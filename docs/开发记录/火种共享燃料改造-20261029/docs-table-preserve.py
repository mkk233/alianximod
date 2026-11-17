# -*- coding: utf-8 -*-
"""Update three in-progress consumer rows. Does not move rows or touch the archive sheet."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "火种共享燃料改造-20261029"
live = root / "docs" / "术式制作表.xlsx"
marker = "共享燃料实施中"
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
archive_part = "xl/worksheets/sheet7.xml"
detail_part = "xl/worksheets/sheet6.xml"


def fail(message):
    raise SystemExit(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()


def cell_xml(row_xml, column, row_number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{row_number}"', row_xml))
    if len(matches) != 1:
        fail(f"Cell {column}{row_number} count is {len(matches)}")
    start = matches[0].start()
    opener_end = row_xml.find(">", start)
    if opener_end < 0:
        fail(f"Cell {column}{row_number} has no tag end")
    if row_xml[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</(?:[\w]+:)?c>", row_xml[opener_end:])
        if not close:
            fail(f"Cell {column}{row_number} is not closed")
        end = opener_end + close.end()
    return row_xml[start:end], start, end


def cell_text(cell):
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<(?:[\w]+:)?v>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if value:
        return html.unescape(value.group(1))
    return None


def replace_text(cell, column, row_number, new_text):
    escaped = html.escape(new_text, quote=False)
    if re.search(r"<(?:[\w]+:)?t\b", cell):
        updated, count = re.subn(
            r"(<(?:[\w]+:)?t\b[^>]*>)[\s\S]*(</(?:[\w]+:)?t>)",
            lambda match: match.group(1) + escaped + match.group(2),
            cell,
            count=1,
        )
    elif re.search(r"<(?:[\w]+:)?v>", cell):
        updated, count = re.subn(
            r"(<(?:[\w]+:)?v>)[\s\S]*(</(?:[\w]+:)?v>)",
            lambda match: match.group(1) + escaped + match.group(2),
            cell,
            count=1,
        )
    else:
        fail(f"Cell {column}{row_number} has no text node")
    if count != 1 or cell_text(updated) != new_text:
        fail(f"Cell {column}{row_number} text was not written cleanly")
    return updated


def rows_of(sheet_xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet_xml)


def sheet_ids(sheet_xml, id_column):
    found = []
    for row_xml in rows_of(sheet_xml):
        row_number = int(re.search(r'\br="(\d+)"', row_xml).group(1))
        if row_number < 3:
            continue
        if f'r="{id_column}{row_number}"' not in row_xml:
            continue
        code = cell_text(cell_xml(row_xml, id_column, row_number)[0])
        if code and code.strip():
            found.append((code.strip(), row_number))
    return found


approved = json.loads((batch / "approved-requirements.json").read_text(encoding="utf-8"))
current = json.loads((batch / "current-input.json").read_text(encoding="utf-8"))
expected = current["workbook"]["sha256"]
items = {item["id"]: item for item in current["items"]}
live_bytes = live.read_bytes()
live_stat = live.stat()
if sha256(live_bytes) != expected:
    fail(f"Live workbook hash changed before writeback: {sha256(live_bytes)}")

stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"docs-table-术式制作表-{stamp}-1.xlsx"
output = batch / "docs-table-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)
if sha256(backup.read_bytes()) != expected:
    fail("Backup hash is not the baseline")

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original_parts = {info.filename: archive.read(info.filename) for info in order}

sheet_name = "xl/worksheets/sheet3.xml"
original = original_parts[sheet_name]
sheet = original.decode("utf-8")
located = {code: row for code, row in sheet_ids(sheet, "B") if code in approved["items"]}
if set(located) != set(approved["items"]):
    fail(f"In-progress locations {located}")
for code, spec in approved["items"].items():
    if items[code]["sheet"] != "制作中" or items[code]["row"] != located[code]:
        fail(f"Snapshot location differs for {code}")

row_map = {}
for row_xml in rows_of(sheet):
    row_map[int(re.search(r'\br="(\d+)"', row_xml).group(1))] = row_xml

updated_rows = {}
notes_rows = {}
for code, spec in approved["items"].items():
    row_number = located[code]
    row_xml = row_map[row_number]
    if cell_text(cell_xml(row_xml, "A", row_number)[0]) != spec["name"]:
        fail(f"Name mismatch for {code}")
    feature = cell_text(cell_xml(row_xml, "N", row_number)[0])
    effect = cell_text(cell_xml(row_xml, "O", row_number)[0])
    remark = cell_text(cell_xml(row_xml, "DV", row_number)[0]) or ""
    if feature != items[code]["n"] or effect != items[code]["o"] or remark != items[code]["dv"]:
        fail(f"Current N/O/DV no longer matches the pre-edit snapshot for {code}")
    if marker in remark:
        fail(f"DV{row_number} already has this feedback")
    if spec["nMode"] == "replace":
        new_feature = spec["n"]
    elif spec["nMode"] == "append":
        if "0.36" not in feature or "6" not in feature:
            fail(f"Liaoli formula is missing from N{row_number}")
        new_feature = feature + "\n" + spec["nAppend"]
    else:
        fail(f"Unknown nMode for {code}")
    cell, start, end = cell_xml(row_xml, "N", row_number)
    row_xml = row_xml[:start] + replace_text(cell, "N", row_number, new_feature) + row_xml[end:]
    for column, new_value in spec["fields"].items():
        old_value = cell_text(cell_xml(row_xml, column, row_number)[0])
        snapshot = items[code]["cells"][column]["text"]
        if old_value != snapshot:
            fail(f"{column}{row_number} no longer matches the snapshot")
        cell, start, end = cell_xml(row_xml, column, row_number)
        row_xml = row_xml[:start] + replace_text(cell, column, row_number, new_value) + row_xml[end:]
    addition = approved["dvAppend"][code]
    new_remark = remark + "\n" + addition
    cell, start, end = cell_xml(row_xml, "DV", row_number)
    row_xml = row_xml[:start] + replace_text(cell, "DV", row_number, new_remark) + row_xml[end:]
    if cell_text(cell_xml(row_xml, "O", row_number)[0]) != effect:
        fail(f"O changed for {code}")
    if cell_text(cell_xml(row_xml, "N", row_number)[0]) != new_feature:
        fail(f"N was not stored for {code}")
    updated_rows[row_number] = row_xml
    notes_rows[code] = row_number

sheet_out = sheet
for row_number, updated in updated_rows.items():
    if sheet_out.count(row_map[row_number]) != 1:
        fail(f"Row {row_number} is not unique")
    sheet_out = sheet_out.replace(row_map[row_number], updated, 1)
restored = sheet_out
for row_number, updated in updated_rows.items():
    if restored.count(updated) != 1:
        fail(f"Updated row {row_number} is not unique")
    restored = restored.replace(updated, row_map[row_number], 1)
if restored.encode("utf-8") != original:
    fail("Sheet differs beyond the three consumer rows")

changed = {sheet_name: sheet_out.encode("utf-8")}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    if src.namelist() != dst.namelist():
        fail("Zip part list changed")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != [sheet_name]:
        fail(f"Unexpected part changes: {different}")
    if src.read(archive_part) != dst.read(archive_part):
        fail("Archive sheet changed")
    if src.read(detail_part) != dst.read(detail_part):
        fail("Resource detail changed")
    counts = {}
    all_ids = []
    for title, name in status_parts.items():
        ids = sheet_ids(dst.read(name).decode("utf-8"), "B")
        counts[title] = len(ids)
        all_ids.extend(code for code, _row in ids)
    if counts != {"已完成": 40, "待制作": 0, "制作中": 6, "待验收": 0}:
        fail(f"Counts {counts}")
    if len(all_ids) != len(set(all_ids)) or len(all_ids) != 46:
        fail("Status ids are not unique")
    written = dst.read(sheet_name).decode("utf-8")
    for code, addition in approved["dvAppend"].items():
        if addition not in written:
            fail(f"Addition missing for {code}")
        if "0.2×S" in cell_text(cell_xml(updated_rows[notes_rows["fire_liaoli"]], "N", notes_rows["fire_liaoli"])[0]).split("不改成")[-1]:
            pass
    liaoli_n = cell_text(cell_xml(updated_rows[notes_rows["fire_liaoli"]], "N", notes_rows["fire_liaoli"])[0])
    if "0.36×S" not in liaoli_n or "0.2×S" in liaoli_n.replace("不改成0.2×S。", ""):
        fail("Liaoli formula changed")

summary = {
    "submitted": False,
    "baselineSha256": expected,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "rows": notes_rows,
    "changedParts": different,
    "counts": counts,
}
(batch / "docs-table-preserve-summary.json").write_text(
    json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "backup": backup.name,
    "candidateSha256": summary["candidateSha256"],
    "rows": notes_rows,
    "counts": counts,
    "changedParts": different,
}, ensure_ascii=False))
