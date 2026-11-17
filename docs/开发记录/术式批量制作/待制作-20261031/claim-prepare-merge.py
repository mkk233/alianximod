# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
archive = zipfile.ZipFile(live)
rels = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
targets = {}
for rel in rels:
    if rel.tag.rsplit("}", 1)[-1] != "Relationship":
        continue
    target = rel.attrib["Target"].lstrip("/")
    targets[rel.attrib["Id"]] = target if target.startswith("xl/") else "xl/" + target
root = ET.fromstring(archive.read("xl/workbook.xml"))
for sheet in root.iter():
    if sheet.tag.rsplit("}", 1)[-1] != "sheet":
        continue
    name = sheet.attrib["name"]
    if name not in ("待制作", "制作中", "待制作原始数据"):
        continue
    rid = sheet.attrib["{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"]
    xml = archive.read(targets[rid]).decode("utf-8")
    refs = re.findall(r"<mergeCell\b[^>]*ref=\"([^\"]+)\"", xml)
    print(name, refs)
