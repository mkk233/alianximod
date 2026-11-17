# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
ns = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
rel = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"
with zipfile.ZipFile(live) as book:
    wb = book.read("xl/workbook.xml").decode("utf-8")
    rels = ET.fromstring(book.read("xl/_rels/workbook.xml.rels"))
    targets = {}
    for node in rels:
        target = "xl/" + node.attrib["Target"].lstrip("/")
        targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
    work = ET.fromstring(wb.encode("utf-8"))
    names = {node.attrib["name"]: targets[node.attrib[rel]] for node in work.find("s:sheets", ns)}
    making = book.read(names["制作中"]).decode("utf-8")
    acceptance = book.read(names["待验收"]).decode("utf-8")
print("defined")
for match in re.finditer(r"<definedName\b[^>]*>[^<]*</definedName>", wb):
    if "制作中" in match.group(0) or "待验收" in match.group(0):
        print(match.group(0))
print("making auto", re.findall(r"<autoFilter\b[^>]*/>", making))
print("making dim", re.search(r"<dimension\b[^>]*/>", making).group(0))
print("accept auto", re.findall(r"<autoFilter\b[^>]*/>", acceptance))
print("accept dim", re.search(r"<dimension\b[^>]*/>", acceptance).group(0))
