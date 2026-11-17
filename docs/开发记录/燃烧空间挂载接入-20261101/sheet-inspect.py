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
xml = z.read("xl/worksheets/sheet6.xml").decode("utf-8")
rows = re.findall(r"<row\b[^>]*>[\s\S]*?</row>", xml)
print("detail rows", [re.search(r'\br="(\d+)"', row).group(1) for row in rows])
row2 = next(row for row in rows if re.search(r'\br="2"', row))
for match in re.finditer(r'<c r="([A-Z]+)2"[^>]*>[\s\S]*?</c>', row2):
    body = match.group(0)
    if 't="s"' in body:
        index = int(re.search(r"<v>(\d+)</v>", body).group(1))
        print(match.group(1), shared[index])
print("detail sqref", re.findall(r'sqref="([^"]+)"', xml))
wb = z.read("xl/workbook.xml").decode("utf-8")
for match in re.finditer(r"<definedName\b[^>]*>[\s\S]*?</definedName>", wb):
    if "制作中" in match.group(0):
        print(match.group(0))
print("fire_burn indexes", [index for index, text in enumerate(shared) if text == "fire_burn"])
