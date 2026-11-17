# -*- coding: utf-8 -*-
import re
import zipfile
from xml.etree import ElementTree as ET

LIVE = r"D:\alianximod\docs\术式制作表.xlsx"
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"

with zipfile.ZipFile(LIVE) as book:
    rels = ET.fromstring(book.read("xl/_rels/workbook.xml.rels"))
    targets = {}
    for node in rels:
        target = "xl/" + node.attrib["Target"].lstrip("/")
        targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
    work = ET.fromstring(book.read("xl/workbook.xml"))
    sheets = {node.attrib["name"]: targets[node.attrib[REL]] for node in work.find("s:sheets", NS)}
    text = book.read(sheets["待制作原始数据"]).decode("utf-8")

view = re.search(r"<sheetViews[\s\S]*?</sheetViews>", text)
print(view.group(0)[:1000] if view else "no view")
row = re.search(r'<row r="33"[\s\S]*?</row>', text).group(0)
refs = re.findall(r'r="([A-Z]+)33"', row)
print("cell count", len(refs))
print("first", refs[:8], "last", refs[-12:])
print("has FS", "FS" in refs)
print("dimension", re.search(r"<dimension[^/]*/>", text).group(0))
