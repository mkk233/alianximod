# -*- coding: utf-8 -*-
import html
import re
import zipfile

with zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx") as book:
    xml = book.read("xl/worksheets/sheet6.xml").decode("utf-8")
    shared_xml = book.read("xl/sharedStrings.xml").decode("utf-8")
items = []
for body in re.findall(r"<si\b[^>]*>(.*?)</si>", shared_xml, re.S):
    parts = re.findall(r"<t\b[^>]*>(.*?)</t>", body, re.S)
    items.append(html.unescape("".join(parts)))
indexes = [i for i, text in enumerate(items) if text == "water_deep_pressure"]
print("indexes", indexes)
for index in indexes:
    print("refs", xml.count(f"<v>{index}</v>"))
rows = list(re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', xml))
print("detail rows", [int(m.group(1)) for m in rows])
