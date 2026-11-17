# -*- coding: utf-8 -*-
import html
import re
import zipfile
from xml.etree import ElementTree as ET

z = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx")
root = ET.fromstring(z.read("xl/sharedStrings.xml"))
shared = []
for item in root:
    texts = []
    for node in item.iter():
        if node.tag.endswith("}t") and node.text:
            texts.append(html.unescape(node.text))
    shared.append("".join(texts))

def rows_of(xml):
    return re.findall(r"<row\b[^>]*>[\s\S]*?</row>", xml)

def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))

def cell_text(row, column, number):
    match = re.search(rf'<c r="{column}{number}"[\s\S]*?</c>', row)
    if not match:
        return None
    cell = match.group(0)
    if "<is>" in cell:
        return html.unescape("".join(re.findall(r"<t[^>]*>([\s\S]*?)</t>", cell)))
    value = re.search(r"<v>([\s\S]*?)</v>", cell)
    if not value:
        return None
    raw = html.unescape(value.group(1))
    if 't="s"' in cell and raw.isdigit():
        return shared[int(raw)]
    return raw

pages = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
for title, part in pages.items():
    xml = z.read(part).decode("utf-8")
    ids = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        code = cell_text(row, "B", number)
        if code:
            ids.append((code, number))
    print(title, len(ids))
    for code, number in ids:
        if code == "fire_shock_ring":
            row = next(item for item in rows_of(xml) if row_number(item) == number)
            print("FOUND", title, number)
            print("OPEN", row[:140])
            print("---N---")
            print(cell_text(row, "N", number))
            print("---O---")
            print(cell_text(row, "O", number))
            print("---DVTAIL---")
            dv = cell_text(row, "DV", number) or ""
            print(dv[-400:])
            print("A", cell_text(row, "A", number))
print("archive", "fire_shock_ring" in "".join(shared))
print("detail shared hit", any(text == "fire_shock_ring" for text in shared))
