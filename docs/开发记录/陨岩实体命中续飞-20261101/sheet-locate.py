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
    print("PAGE", title)
    for token in ("dimension", "autoFilter"):
        for found in re.findall(r'ref="[^"]+"', xml):
            if token == "dimension" and "<dimension" in xml:
                pass
        dims = re.findall(r"<dimension[^/]*/>", xml)
        filters = re.findall(r"<autoFilter[^/]*/>", xml)
    print(" dim", re.findall(r'<dimension[^>]*/>', xml))
    print(" filter", re.findall(r'<autoFilter[^>]*/>', xml))
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        code = cell_text(row, "B", number)
        if code == "fire_meteorite":
            print("FOUND", title, number, "ht", re.search(r'\bht="[^"]+"', row).group(0) if 'ht="' in row else None)
            print("---A---")
            print(cell_text(row, "A", number))
            print("---N---")
            print(cell_text(row, "N", number))
            print("---O---")
            print(cell_text(row, "O", number))
            print("---DV---")
            print(cell_text(row, "DV", number))
wb = z.read("xl/workbook.xml").decode("utf-8")
for name in re.findall(r"<definedName[^>]*>[^<]+</definedName>", wb):
    if "制作中" in name or "待验收" in name:
        print(name)
detail = z.read("xl/worksheets/sheet6.xml").decode("utf-8")
archive = z.read("xl/worksheets/sheet7.xml").decode("utf-8")
index = shared.index("fire_meteorite")
print("detail uses id", f"<v>{index}</v>" in detail)
print("archive uses id", f"<v>{index}</v>" in archive)
