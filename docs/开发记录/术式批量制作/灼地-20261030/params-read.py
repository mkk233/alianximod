# -*- coding: utf-8 -*-
import html
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
with zipfile.ZipFile(live) as archive:
    sheet = archive.read("xl/worksheets/sheet3.xml").decode("utf-8")
    archive_sheet = archive.read("xl/worksheets/sheet7.xml").decode("utf-8")

def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)

def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))

def cell_text(cell):
    if 't="inlineStr"' in cell:
        parts = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", cell)
        return html.unescape("".join(parts))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?v>", cell)
    return html.unescape(value.group(1)) if value else None

rows = {row_number(row): row for row in rows_of(sheet)}
header = rows[2]
for column in ("G", "H", "K", "L"):
    match = re.search(rf'<c r="{column}2"[\s\S]*?</c>', header)
    print("header", column, cell_text(match.group(0)) if match else None)
row = rows[6]
refs = re.findall(r'\br="([A-Z]+)6"', row)
print("row6 refs", refs)
for column in ("A", "B", "E", "F", "G", "H", "K", "L", "N"):
    match = re.search(rf'<c r="{column}6"[\s\S]*?</c>', row)
    print(column, "present" if match else "absent", cell_text(match.group(0))[:40] if match else "")
for number in (3, 4, 5):
    sample = rows[number]
    for column in ("G", "H"):
        match = re.search(rf'<c r="{column}{number}"[\s\S]*?</c>', sample)
        if match:
            print("sample", number, match.group(0)[:180])
archive_rows = {row_number(row): row for row in rows_of(archive_sheet)}
archive_row = archive_rows[9]
print("archive B", cell_text(re.search(r'<c r="B9"[\s\S]*?</c>', archive_row).group(0)))
print("archive G", "G9" in archive_row, "H9" in archive_row)
dv = re.search(r'<c r="DV6"[\s\S]*?</c>', row).group(0)
text = cell_text(dv)
print("dv tail", text[-180:].replace("\n", " | "))
print("has feedback", "[设计反馈" in text)
