# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

here = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
backup = here / "备份" / "术式制作表-claim-commit-20261031-025700-1.xlsx"
intake = json.loads((here / "intake.json").read_text(encoding="utf-8"))
EXPECTED = [item["id"] for item in intake["pending"]]


def local(tag):
    return tag.rsplit("}", 1)[-1]


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
    end = opener_end + 1 if row[opener_end - 1] == "/" else opener_end + re.search(r"</(?:[\w]+:)?c>", row[opener_end:]).end()
    return row[start:end]


def cell_text(cell):
    if cell is None:
        return None
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    return html.unescape(value.group(1)) if value else None


def sheet_names(archive):
    rels = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
    targets = {}
    for rel in rels:
        if local(rel.tag) != "Relationship":
            continue
        target = rel.attrib["Target"].lstrip("/")
        targets[rel.attrib["Id"]] = target if target.startswith("xl/") else "xl/" + target
    root = ET.fromstring(archive.read("xl/workbook.xml"))
    found = {}
    for sheet in root.iter():
        if local(sheet.tag) != "sheet":
            continue
        rid = sheet.attrib["{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"]
        found[sheet.attrib["name"]] = targets[rid]
    return found


def ids_of(xml):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        code = cell_text(cell_xml(row, "B", number))
        if code:
            found.append((code, number, row))
    return found


payload = live.read_bytes()
with zipfile.ZipFile(live) as current, zipfile.ZipFile(backup) as previous:
    names = sheet_names(current)
    different = [name for name in previous.namelist() if previous.read(name) != current.read(name)]
    counts = {}
    located = {code: [] for code in EXPECTED}
    all_ids = []
    for title in ("已完成", "待制作", "制作中", "待验收"):
        page = ids_of(current.read(names[title]).decode("utf-8"))
        counts[title] = len(page)
        for code, number, _row in page:
            all_ids.append(code)
            if code in located:
                located[code].append({"sheet": title, "row": number})
    archive_ids = [code for code, _number, _row in ids_of(current.read(names["待制作原始数据"]).decode("utf-8"))]
    making = ids_of(current.read(names["制作中"]).decode("utf-8"))
    checks = []
    for item in intake["pending"]:
        code = item["id"]
        row = next(source for found, _number, source in making if found == code)
        number = next(found_number for found, found_number, _source in making if found == code)
        live_n = cell_text(cell_xml(row, "N", number))
        live_o = cell_text(cell_xml(row, "O", number))
        live_dv = cell_text(cell_xml(row, "DV", number))
        intake_n = item["cells"]["N"]["v"]
        intake_o = item["cells"].get("O", {}).get("v")
        intake_dv = item["cells"]["DV"]["v"]
        checks.append({
            "id": code,
            "row": number,
            "nKept": live_n == intake_n,
            "oKept": live_o == intake_o,
            "dvOriginalKept": intake_dv in (live_dv or ""),
            "claimed": "已领取" in (live_dv or "") and "待制作-20261031" in (live_dv or ""),
        })
    detail_same = current.read(names["资源状态明细"]) == previous.read(names["资源状态明细"])

result = {
    "sha256": hashlib.sha256(payload).hexdigest().upper(),
    "bytes": live.stat().st_size,
    "counts": counts,
    "unique": len(set(all_ids)),
    "total": len(all_ids),
    "archiveCount": len(archive_ids),
    "archiveUnique": len(set(archive_ids)),
    "archiveIds": archive_ids,
    "located": located,
    "checks": checks,
    "differentParts": different,
    "detailSame": detail_same,
    "pendingEmpty": counts["待制作"] == 0,
}
(here / "claim-commit-reopen.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "sha256": result["sha256"],
    "counts": counts,
    "unique": result["unique"],
    "archive": result["archiveCount"],
    "archiveUnique": result["archiveUnique"],
    "allKept": all(item["nKept"] and item["oKept"] and item["dvOriginalKept"] and item["claimed"] for item in checks),
    "different": different,
    "detailSame": detail_same,
}, ensure_ascii=False))
