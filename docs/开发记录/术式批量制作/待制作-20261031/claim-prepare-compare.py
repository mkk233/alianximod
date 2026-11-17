# -*- coding: utf-8 -*-
"""Compare the live workbook with intake. Read-only."""
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

here = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
intake = json.loads((here / "intake.json").read_text(encoding="utf-8"))
EXPECTED = [item["id"] for item in intake["pending"]]


def local(tag):
    return tag.rsplit("}", 1)[-1]


def text_of(node):
    parts = []
    if node.text:
        parts.append(node.text)
    for child in list(node):
        parts.append(text_of(child))
        if child.tail:
            parts.append(child.tail)
    return html.unescape("".join(parts))


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_xml(row, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row.find(">", start)
    if row[opener_end - 1] == "/":
        return row[start:opener_end + 1]
    close = re.search(r"</(?:[\w]+:)?c>", row[opener_end:])
    return row[start:opener_end + close.end()]


def cell_value(cell, shared):
    if cell is None:
        return None
    kind = re.search(r'\bt="([^"]+)"', cell)
    kind = kind.group(1) if kind else None
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", cell)
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    formula = re.search(r"<(?:[\w]+:)?f(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?f>", cell)
    raw = html.unescape(inline.group(1) if inline else value.group(1) if value else "")
    if kind == "s" and raw.isdigit():
        raw = shared[int(raw)]
    return raw if raw != "" else None, None if not formula else html.unescape(formula.group(1))


payload = live.read_bytes()
stat = live.stat()
archive = zipfile.ZipFile(live)
shared = []
if "xl/sharedStrings.xml" in archive.namelist():
    root = ET.fromstring(archive.read("xl/sharedStrings.xml"))
    shared = [text_of(item) for item in root if local(item.tag) == "si"]
workbook = ET.fromstring(archive.read("xl/workbook.xml"))
rels = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
targets = {}
for rel in rels:
    if local(rel.tag) != "Relationship":
        continue
    target = rel.attrib["Target"].lstrip("/")
    if not target.startswith("xl/"):
        target = "xl/" + target
    targets[rel.attrib["Id"]] = target
sheets = {}
for sheet in workbook.iter():
    if local(sheet.tag) != "sheet":
        continue
    rid = sheet.attrib["{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"]
    sheets[sheet.attrib["name"]] = archive.read(targets[rid]).decode("utf-8")


def ids_of(xml):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        cell = cell_xml(row, "B", number)
        parsed = cell_value(cell, shared)
        code = parsed[0] if parsed else None
        if code:
            found.append((code, number, row))
    return found


status_names = ["已完成", "待制作", "制作中", "待验收"]
status = {name: [{"id": code, "row": number} for code, number, _row in ids_of(sheets[name])] for name in status_names}
pending = {code: (number, row) for code, number, row in ids_of(sheets["待制作"])}
archive_ids = [code for code, _number, _row in ids_of(sheets["待制作原始数据"])]
detail_hits = []
for code, number, row in ids_of(sheets["资源状态明细"]):
    if code in EXPECTED:
        detail_hits.append({"id": code, "row": number})

diffs = []
for item in intake["pending"]:
    code = item["id"]
    if code not in pending:
        diffs.append({"id": code, "issue": "不在当前待制作"})
        continue
    number, row = pending[code]
    live_cells = {}
    for match in re.finditer(rf'<(?:[\w]+:)?c r="([A-Z]+){number}"', row):
        column = match.group(1)
        value, formula = cell_value(cell_xml(row, column, number), shared)
        if value is not None or formula:
            live_cells[column] = {"v": value, "f": formula}
    intake_cells = {}
    for column, cell in item["cells"].items():
        value = cell.get("v")
        formula = cell.get("f")
        if value is not None and value != "":
            intake_cells[column] = {"v": str(value), "f": formula}
        elif formula:
            intake_cells[column] = {"v": None, "f": formula}
    columns = sorted(set(live_cells) | set(intake_cells), key=lambda name: (len(name), name))
    for column in columns:
        left = intake_cells.get(column)
        right = live_cells.get(column)
        left_v = None if left is None else left["v"]
        right_v = None if right is None else None if right["v"] is None else str(right["v"])
        left_f = None if left is None else left["f"]
        right_f = None if right is None else right["f"]
        if left_v != right_v or left_f != right_f:
            diffs.append({"id": code, "column": column, "intake": left_v, "live": right_v, "intakeFormula": left_f, "liveFormula": right_f})
    if item.get("resourceDetailRows"):
        diffs.append({"id": code, "issue": "intake 有资源明细，当前比对另计", "count": len(item["resourceDetailRows"])})

extra_pending = [code for code in pending if code not in EXPECTED]
missing = [code for code in EXPECTED if code not in pending]
def slim(items):
    return [{"id": item["id"], "row": item["row"]} for item in items]


other_sheet_changed = any(slim(intake["statusIds"][name]) != status[name] for name in ("已完成", "制作中", "待验收"))
result = {
    "liveSha256": hashlib.sha256(payload).hexdigest().upper(),
    "liveBytes": stat.st_size,
    "liveMtimeNs": stat.st_mtime_ns,
    "intakeSha256": intake["workbook"]["sha256"],
    "hashMatchesIntake": hashlib.sha256(payload).hexdigest().upper() == intake["workbook"]["sha256"],
    "status": {name: len(items) for name, items in status.items()},
    "statusIds": status,
    "archiveIds": archive_ids,
    "batchDiffs": diffs,
    "missingFromPending": missing,
    "extraPending": extra_pending,
    "detailHits": detail_hits,
    "otherStatusPagesChanged": other_sheet_changed,
    "batchRowsUnchanged": not diffs and not missing and not extra_pending,
}
(here / "claim-prepare-compare.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "hashMatchesIntake": result["hashMatchesIntake"],
    "live": result["liveSha256"],
    "batchRowsUnchanged": result["batchRowsUnchanged"],
    "diffCount": len(diffs),
    "otherStatusPagesChanged": other_sheet_changed,
    "counts": result["status"],
    "archive": archive_ids,
    "detailHits": detail_hits,
}, ensure_ascii=False))
