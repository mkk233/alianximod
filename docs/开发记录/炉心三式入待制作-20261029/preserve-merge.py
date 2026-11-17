# -*- coding: utf-8 -*-
"""Transplant artifact-authored cells into a byte copy of the original package.

Does not use a workbook save API. Existing cells stay byte-for-byte.
"""
import hashlib
import json
import zipfile
from pathlib import Path
from xml.sax.saxutils import escape

from openpyxl import load_workbook
from openpyxl.utils import column_index_from_string

root = Path(r"D:\alianximod\docs\开发记录\炉心三式入待制作-20261029")
source = Path(r"D:\alianximod\docs\术式制作表.xlsx")
snapshot = json.loads((root / "input-snapshot.json").read_text(encoding="utf-8-sig"))
baseline = json.loads((root / "baseline.json").read_text(encoding="utf-8-sig"))
source_bytes = source.read_bytes()
source_hash = hashlib.sha256(source_bytes).hexdigest().upper()
if source_hash != baseline["sha256"] or source_hash != snapshot["baselineSha256"]:
    raise SystemExit(f"Source hash changed before merge: {source_hash}")

artifact = load_workbook(root / "candidate-artifact.xlsx", data_only=False, read_only=False)
pending = artifact["待制作"]
catalog = artifact["数据表"]
for entry in snapshot["rows"]:
    for column, expected in entry["values"].items():
        actual = pending[f"{column}{entry['row']}"].value
        if isinstance(expected, str):
            if actual != expected:
                raise SystemExit(f"Artifact mismatch {column}{entry['row']}")
        elif actual != expected and not (isinstance(actual, (int, float)) and float(actual) == float(expected)):
            raise SystemExit(f"Artifact number mismatch {column}{entry['row']}: {actual!r}")
for address, expected in snapshot["enumCells"].items():
    if catalog[address].value != expected:
        raise SystemExit(f"Artifact enum mismatch {address}: {catalog[address].value!r}")
artifact.close()


def column_width(column):
    total = 0
    for char in column:
        total = total * 26 + ord(char) - 64
    return total


def cjk_capacity(width):
    return max(8, int(width / 2.3))


def wrapped_lines(text, capacity):
    count = 0
    for paragraph in str(text).split("\n"):
        count += 1 if paragraph == "" else max(1, (len(paragraph) + capacity - 1) // capacity)
    return count


widths = {"N": 80, "O": 52, "DV": 90, "EJ": 28, "EK": 36, "EM": 28}
row_heights = {}
for entry in snapshot["rows"]:
    needed = 1
    for column, value in entry["values"].items():
        if isinstance(value, str) and ("\n" in value or len(value) > 20):
            needed = max(needed, wrapped_lines(value, cjk_capacity(widths.get(column, 18))))
    row_heights[entry["row"]] = max(225, needed * 16 + 12)


def inline(ref, style, text):
    preserved = ' xml:space="preserve"' if text[:1].isspace() or text[-1:].isspace() or "\n" in text else ""
    return f'<c r="{ref}" s="{style}" t="inlineStr"><is><t{preserved}>{escape(text)}</t></is></c>'


def number(ref, style, value):
    if isinstance(value, float) and value.is_integer():
        value = int(value)
    return f'<c r="{ref}" s="{style}" t="n"><v>{value}</v></c>'


with zipfile.ZipFile(source) as archive:
    sheet2 = archive.read("xl/worksheets/sheet2.xml")
    sheet5 = archive.read("xl/worksheets/sheet5.xml")
    workbook_xml = archive.read("xl/workbook.xml")
    names = archive.namelist()

style_by_column = {}
row5_start = sheet2.find(b'<row r="5"')
row5_end = sheet2.find(b"</row>", row5_start)
row5 = sheet2[row5_start:row5_end]
cursor = 0
while True:
    marker = row5.find(b'<c r="', cursor)
    if marker < 0:
        break
    ref_end = row5.find(b'"', marker + 6)
    ref = row5[marker + 6:ref_end].decode("ascii")
    style_at = row5.find(b' s="', marker)
    style_end = row5.find(b'"', style_at + 4)
    column = "".join(char for char in ref if char.isalpha())
    style_by_column[column] = row5[style_at + 4:style_end].decode("ascii")
    cursor = ref_end

long_columns = {"B", "N", "O", "DV", "EJ", "EK", "EM"}
rows_xml = []
for entry in snapshot["rows"]:
    cells = []
    for column, value in sorted(entry["values"].items(), key=lambda item: column_width(item[0])):
        style = style_by_column.get(column)
        if style is None:
            style = "16" if column in long_columns else "15"
        ref = f"{column}{entry['row']}"
        cells.append(inline(ref, style, value) if isinstance(value, str) else number(ref, style, value))
    height = row_heights[entry["row"]]
    rows_xml.append(f'<row r="{entry["row"]}" ht="{height}" customHeight="1">{"".join(cells)}</row>')
addition = "".join(rows_xml).encode("utf-8")

close = b"</sheetData>"
if sheet2.count(close) != 1:
    raise SystemExit("sheetData close is not unique")
if b'<row r="6"' in sheet2 or b'<row r="7"' in sheet2 or b'<row r="8"' in sheet2:
    raise SystemExit("Pending sheet already has row 6, 7, or 8")
sheet2_out = sheet2.replace(b'<dimension ref="A1:EN5"/>', b'<dimension ref="A1:EN8"/>', 1)
sheet2_out = sheet2_out.replace(b'<autoFilter ref="A2:EN5"/>', b'<autoFilter ref="A2:EN8"/>', 1)
sheet2_out = sheet2_out.replace(close, addition + close, 1)

enum_insert = (
    inline("I7", "21", snapshot["enumCells"]["I7"])
    + inline("K7", "21", snapshot["enumCells"]["K7"])
).encode("utf-8")
anchor = '<c r="G7" s="21" t="inlineStr"><is><t>锁定准星附近的实体</t></is></c>'.encode("utf-8")
if sheet5.count(anchor) != 1:
    raise SystemExit("Data sheet row 7 anchor missing")
if b'r="I7"' in sheet5 or b'r="K7"' in sheet5:
    raise SystemExit("Enum cells already exist")
sheet5_out = sheet5.replace(anchor, anchor + enum_insert, 1)

name_from = snapshot["namedRange"]["from"].encode("utf-8")
name_to = snapshot["namedRange"]["to"].encode("utf-8")
filter_from = "'待制作'!$A$2:$EN$5".encode("utf-8")
filter_to = "'待制作'!$A$2:$EN$8".encode("utf-8")
if workbook_xml.count(name_from) != 1 or workbook_xml.count(filter_from) != 1:
    raise SystemExit("Workbook name replacement is not unique")
workbook_out = workbook_xml.replace(name_from, name_to, 1).replace(filter_from, filter_to, 1)

changed = {
    "xl/worksheets/sheet2.xml": sheet2_out,
    "xl/worksheets/sheet5.xml": sheet5_out,
    "xl/workbook.xml": workbook_out,
}
output = root / "candidate-preserved.xlsx"
with zipfile.ZipFile(source) as src, zipfile.ZipFile(output, "w") as dst:
    for info in src.infolist():
        payload = changed.get(info.filename, src.read(info.filename))
        dst.writestr(info, payload)

with zipfile.ZipFile(source) as src, zipfile.ZipFile(output) as dst:
    if src.namelist() != dst.namelist():
        raise SystemExit("Zip part list changed")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    expected = sorted(changed)
    if sorted(different) != expected:
        raise SystemExit(f"Unexpected part changes: {different}")

check = load_workbook(output, data_only=False, read_only=False)
original = load_workbook(source, data_only=False, read_only=False)
problems = []
for title in original.sheetnames:
    left = original[title]
    right = check[title]
    max_row = max(left.max_row or 1, right.max_row or 1)
    max_col = max(left.max_column or 1, right.max_column or 1)
    for row in range(1, max_row + 1):
        for col in range(1, max_col + 1):
            if title == "待制作" and row in {6, 7, 8}:
                continue
            if title == "数据表" and row == 7 and col in {column_index_from_string("I"), column_index_from_string("K")}:
                continue
            if left.cell(row, col).value != right.cell(row, col).value:
                problems.append(f"{title}!{left.cell(row, col).coordinate}")
if problems:
    raise SystemExit(f"Existing values changed: {problems[:12]}")

ids = []
for title in ["已完成", "待制作", "制作中", "待验收"]:
    sheet = check[title]
    for row in range(3, (sheet.max_row or 2) + 1):
        code = sheet.cell(row, 2).value
        if code:
            ids.append((title, row, code))
wanted = [("待制作", 6, "fire_heat_up"), ("待制作", 7, "fire_burn"), ("待制作", 8, "fire_cool")]
for item in wanted:
    if ids.count(item) != 1:
        raise SystemExit(f"Missing new id {item}")
codes = [item[2] for item in ids]
if len(codes) != len(set(codes)) or len(codes) != 46:
    raise SystemExit(f"Id count {len(codes)} unique {len(set(codes))}")
counts = {title: sum(1 for item in ids if item[0] == title) for title in ["已完成", "待制作", "制作中", "待验收"]}
if counts != {"已完成": 40, "待制作": 6, "制作中": 0, "待验收": 0}:
    raise SystemExit(f"Counts {counts}")
if check["待制作"].auto_filter.ref != "A2:EN8":
    raise SystemExit("Filter not extended")
if check.defined_names["释放种类"].attr_text != "数据表!$I$4:$I$7":
    raise SystemExit("Named range not extended")
if check["数据表"]["I7"].value != "自身状态" or check["数据表"]["J7"].value not in (None, ""):
    raise SystemExit("Enum source unexpected")
for entry in snapshot["rows"]:
    for column, expected in entry["values"].items():
        actual = check["待制作"][f"{column}{entry['row']}"].value
        if actual != expected:
            raise SystemExit(f"Preserved mismatch {column}{entry['row']}")
    if check["待制作"][f"DX{entry['row']}"].value not in (None, ""):
        raise SystemExit("Resource column was filled")
    if check["待制作"][f"R{entry['row']}"].value not in (None, ""):
        raise SystemExit("Trajectory was filled")
original.close()
check.close()
print(json.dumps({
    "candidate": str(output),
    "changed_parts": expected,
    "row_heights": row_heights,
    "counts": counts,
    "ids": 46,
}, ensure_ascii=False))
