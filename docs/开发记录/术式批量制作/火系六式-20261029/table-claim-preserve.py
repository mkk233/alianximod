# -*- coding: utf-8 -*-
"""Move six claimed rows inside a byte copy of the saved workbook.

Writes the candidate by copying zip parts. Does not save through a workbook library.
"""
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path

root = Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029")
backup = root / "备份" / "术式制作表-20261029-023143-1.xlsx"
output = root / "table-candidate-preserved.xlsx"
result_path = root / "table-claim-result.json"
baseline = "35CC4218A03152A4C9880A44EC20F99485E884141027909055603FB04D6E4803"
marker = "制作中 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
wanted = [
    (3, "燎原", "fire_liaoyuan"),
    (4, "燎里", "fire_liaoli"),
    (5, "焚天", "fire_fentian"),
    (6, "升温", "fire_heat_up"),
    (7, "燃烧", "fire_burn"),
    (8, "冷却", "fire_cool"),
]
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}


def fail(message):
    raise SystemExit(message)


def column_index(letters):
    total = 0
    for char in letters:
        total = total * 26 + ord(char) - 64
    return total


def unescape(text):
    return html.unescape(text)


def cell_xml(row_xml, column, row_number):
    token = f'<c r="{column}{row_number}"'
    start = row_xml.find(token)
    if start < 0 or row_xml.find(token, start + len(token)) >= 0:
        fail(f"Cell {column}{row_number} is not unique")
    opener_end = row_xml.find(">", start)
    if opener_end < 0:
        fail(f"Cell {column}{row_number} has no tag end")
    if row_xml[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = row_xml.find("</c>", opener_end)
        if close < 0:
            fail(f"Cell {column}{row_number} is not closed")
        end = close + 4
    return row_xml[start:end], start, end


def cell_text(cell):
    inline = re.search(r"<t(?: [^>]*)?>([\s\S]*)</t>", cell)
    if inline:
        return unescape(inline.group(1))
    value = re.search(r"<v>([\s\S]*)</v>", cell)
    if value:
        return unescape(value.group(1))
    fail(f"No text in {cell[:80]}")


def append_marker(row_xml, row_number):
    cell, start, end = cell_xml(row_xml, "DV", row_number)
    if marker in cell:
        fail(f"DV{row_number} already has the marker")
    addition = "\n" + marker
    if 't="str"' in cell[:80] and cell.count("<v>") == 1 and cell.endswith("</v></c>"):
        updated = cell[: -len("</v></c>")] + addition + "</v></c>"
    elif 't="inlineStr"' in cell[:140] and cell.count("</t>") == 1 and cell.endswith("</t></is></c>"):
        updated = cell[: -len("</t></is></c>")] + addition + "</t></is></c>"
    else:
        fail(f"Unsupported DV cell {cell[:120]}")
    if cell_text(updated) != cell_text(cell) + addition:
        fail(f"DV{row_number} text was not appended cleanly")
    return row_xml[:start] + updated + row_xml[end:]


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        fail(f"{label} count is {count}")
    return text.replace(old, new, 1)


def rows_of(sheet_xml):
    return re.findall(r'<row r="\d+"[^>]*>[\s\S]*?</row>', sheet_xml)


def sheet_ids(sheet_xml):
    found = []
    for row_xml in rows_of(sheet_xml):
        row_number = int(re.search(r'<row r="(\d+)"', row_xml).group(1))
        if row_number < 3:
            continue
        token = f'<c r="B{row_number}"'
        if token not in row_xml:
            continue
        code = cell_text(cell_xml(row_xml, "B", row_number)[0]).strip()
        if code:
            found.append((code, row_number))
    return found


def detail_ids(sheet_xml):
    found = []
    for match in re.finditer(r'r="A(\d+)"[\s\S]*?</[^>]*c>', sheet_xml):
        row_number = int(match.group(1))
        if row_number < 3:
            continue
        value = re.search(r"<[^>]*t(?: [^>]*)?>([\s\S]*?)</[^>]*t>", match.group(0))
        if value is None:
            value = re.search(r"<[^>]*v>([\s\S]*?)</[^>]*v>", match.group(0))
        if value and value.group(1).strip():
            found.append((row_number, unescape(value.group(1).strip())))
    return found


source_bytes = backup.read_bytes()
source_hash = hashlib.sha256(source_bytes).hexdigest().upper()
if source_hash != baseline:
    fail(f"Backup hash changed: {source_hash}")

with zipfile.ZipFile(backup) as archive:
    parts = {name: archive.read(name) for name in archive.namelist()}
    order = archive.infolist()

for name in ("xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml", "xl/workbook.xml"):
    raw = parts[name]
    if raw.decode("utf-8").encode("utf-8") != raw:
        fail(f"{name} is not stable utf-8")

pending = parts["xl/worksheets/sheet2.xml"].decode("utf-8")
making = parts["xl/worksheets/sheet3.xml"].decode("utf-8")
workbook = parts["xl/workbook.xml"].decode("utf-8")
detail = parts["xl/worksheets/sheet6.xml"]
for row_number, _name, code in wanted:
    if detail.find(code.encode("utf-8")) >= 0:
        fail(f"Detail sheet unexpectedly contains {code}")

pending_rows = {int(re.search(r'<row r="(\d+)"', row).group(1)): row for row in rows_of(pending)}
making_rows = {int(re.search(r'<row r="(\d+)"', row).group(1)): row for row in rows_of(making)}
if set(pending_rows) != {1, 2, 3, 4, 5, 6, 7, 8}:
    fail(f"Pending rows are {sorted(pending_rows)}")
if set(making_rows) != {1, 2}:
    fail(f"Making rows are {sorted(making_rows)}")

moved = []
for row_number, name, code in wanted:
    row_xml = pending_rows[row_number]
    if "<f" in row_xml:
        fail(f"Row {row_number} contains a formula")
    for ref, ref_row in re.findall(r'<c r="([A-Z]+)(\d+)"', row_xml):
        if int(ref_row) != row_number or column_index(ref) > column_index("EN"):
            fail(f"Cell {ref}{ref_row} is outside row {row_number} A:EN")
    if cell_text(cell_xml(row_xml, "A", row_number)[0]) != name:
        fail(f"Name mismatch on row {row_number}")
    if cell_text(cell_xml(row_xml, "B", row_number)[0]) != code:
        fail(f"Id mismatch on row {row_number}")
    original_cells = {
        ref: cell_xml(row_xml, ref, row_number)[0]
        for ref, ref_row in re.findall(r'<c r="([A-Z]+)(\d+)"', row_xml)
    }
    updated = append_marker(row_xml, row_number)
    updated_cells = {
        ref: cell_xml(updated, ref, row_number)[0]
        for ref, ref_row in re.findall(r'<c r="([A-Z]+)(\d+)"', updated)
    }
    if set(original_cells) != set(updated_cells):
        fail(f"Cell set changed on row {row_number}")
    for ref, cell in original_cells.items():
        if ref != "DV" and cell != updated_cells[ref]:
            fail(f"{ref}{row_number} changed")
    note_cell = original_cells["N"]
    effect_cell = original_cells["O"]
    if cell_text(cell_xml(updated, "N", row_number)[0]) != cell_text(note_cell):
        fail(f"N{row_number} text changed")
    if cell_text(cell_xml(updated, "O", row_number)[0]) != cell_text(effect_cell):
        fail(f"O{row_number} text changed")
    moved.append(updated)

making_out = making
for old, new, label in (
    ('<dimension ref="A1:EN2"/>', '<dimension ref="A1:EN8"/>', "making dimension"),
    ('<autoFilter ref="A2:EN2"/>', '<autoFilter ref="A2:EN8"/>', "making filter"),
    ('<col width="42" customWidth="1" min="14" max="14"/>', '<col width="80" customWidth="1" min="14" max="14"/>', "making N width"),
    ('<col width="26" customWidth="1" min="15" max="15"/>', '<col width="52" customWidth="1" min="15" max="15"/>', "making O width"),
    ('<col width="42" customWidth="1" min="126" max="126"/>', '<col width="90" customWidth="1" min="126" max="126"/>', "making DV width"),
):
    making_out = replace_once(making_out, old, new, label)
if "</sheetData>" not in making_out or making_out.count("</sheetData>") != 1:
    fail("Making sheetData close is not unique")
making_out = replace_once(making_out, "</sheetData>", "".join(moved) + "</sheetData>", "making row insert")
if [int(re.search(r'<row r="(\d+)"', row).group(1)) for row in rows_of(making_out)] != [1, 2, 3, 4, 5, 6, 7, 8]:
    fail("Making row order is wrong before source deletion")
if making_rows[1] not in making_out or making_rows[2] not in making_out:
    fail("Making header rows changed")
for row_number, _name, code in wanted:
    if making_out.count(f"<c r=\"B{row_number}\"") != 1 or code not in making_out:
        fail(f"Target row {row_number} is incomplete")

pending_out = pending
for row_number in range(3, 9):
    pending_out = replace_once(pending_out, pending_rows[row_number], "", f"delete pending row {row_number}")
for old, new, label in (
    ('<dimension ref="A1:EN8"/>', '<dimension ref="A1:EN2"/>', "pending dimension"),
    ('<autoFilter ref="A2:EN8"/>', '<autoFilter ref="A2:EN2"/>', "pending filter"),
):
    pending_out = replace_once(pending_out, old, new, label)
if [int(re.search(r'<row r="(\d+)"', row).group(1)) for row in rows_of(pending_out)] != [1, 2]:
    fail("Pending rows remain after deletion")
for _row_number, _name, code in wanted:
    if code in pending_out:
        fail(f"{code} still remains on pending sheet")
if pending_rows[1] not in pending_out or pending_rows[2] not in pending_out:
    fail("Pending header rows changed")

validations = re.search(r"<dataValidations[\s\S]*?</dataValidations>", pending).group(0)
if validations not in pending_out or validations not in making_out or validations not in making:
    fail("Data validations changed")
merges = re.search(r"<mergeCells[\s\S]*?</mergeCells>", pending).group(0)
if merges not in pending_out or merges not in making_out:
    fail("Merged ranges changed")

workbook_out = workbook
workbook_out = replace_once(workbook_out, "'待制作'!$A$2:$EN$8", "'待制作'!$A$2:$EN$2", "pending named filter")
workbook_out = replace_once(workbook_out, "'制作中'!$A$2:$EN$2", "'制作中'!$A$2:$EN$8", "making named filter")
defined = re.findall(r"<definedName\b[^>]*>[\s\S]*?</definedName>", workbook)
defined_out = re.findall(r"<definedName\b[^>]*>[\s\S]*?</definedName>", workbook_out)
if len(defined) != len(defined_out):
    fail("Defined name count changed")
changed_names = [
    (left, right)
    for left, right in zip(defined, defined_out)
    if left != right
]
if len(changed_names) != 2:
    fail(f"Defined name changes {len(changed_names)}")

changed = {
    "xl/worksheets/sheet2.xml": pending_out.encode("utf-8"),
    "xl/worksheets/sheet3.xml": making_out.encode("utf-8"),
    "xl/workbook.xml": workbook_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        payload = changed.get(info.filename, src.read(info.filename))
        dst.writestr(info, payload)

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    if src.namelist() != dst.namelist():
        fail("Zip part list changed")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if sorted(different) != sorted(changed):
        fail(f"Unexpected part changes: {different}")

with zipfile.ZipFile(output) as dst:
    saved = {name: dst.read(name) for name in status_parts.values()}
    saved["xl/worksheets/sheet6.xml"] = dst.read("xl/worksheets/sheet6.xml")
    saved["xl/workbook.xml"] = dst.read("xl/workbook.xml")

counts = {}
located = []
for title, name in status_parts.items():
    ids = sheet_ids(saved[name].decode("utf-8"))
    counts[title] = len(ids)
    located.extend((code, title, row_number) for code, row_number in ids)
codes = [item[0] for item in located]
if len(codes) != len(set(codes)):
    fail("Ids are not unique after reopen")
claimed = [item for item in located if item[0] in {code for _row, _name, code in wanted}]
if [item[0] for item in claimed] != [code for _row, _name, code in wanted]:
    fail(f"Claimed order changed: {claimed}")
if any(item[1] != "制作中" or item[2] != wanted[index][0] for index, item in enumerate(claimed)):
    fail(f"Claimed location changed: {claimed}")
if counts["待制作"] != 0 or counts["制作中"] != 6:
    fail(f"Status counts {counts}")
if len(codes) != 46:
    fail(f"Total ids {len(codes)}; source total must stay 46 for this saved baseline")

detail_found = detail_ids(saved["xl/worksheets/sheet6.xml"].decode("utf-8"))
result = {
    "submitted": False,
    "baselineSha256": baseline,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": hashlib.sha256(output.read_bytes()).hexdigest().upper(),
    "changedParts": sorted(different),
    "counts": counts,
    "total": len(codes),
    "unique": len(set(codes)),
    "claimed": [
        {"id": code, "name": name, "sourceSheet": "待制作", "sourceRow": row_number, "targetSheet": "制作中", "targetRow": row_number}
        for row_number, name, code in wanted
    ],
    "detailIds": detail_found,
    "detailUnchanged": saved["xl/worksheets/sheet6.xml"] == parts["xl/worksheets/sheet6.xml"],
    "namedRangeChanges": [right for _left, right in changed_names],
    "columnWidthsCarriedToMaking": {"N": 80, "O": 52, "DV": 90},
}
result_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(result, ensure_ascii=False, indent=2))
