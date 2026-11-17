# -*- coding: utf-8 -*-
import json
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
cells = json.loads(Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031\intake-cells.json").read_text(encoding="utf-8"))
print("ARCHIVE")
for item in cells["archiveIds"]:
    print(item["row"], item["id"], item["name"])
print("FORMULAS", sum(1 for row in cells["pendingRows"] for cell in row["cells"].values() if cell.get("f")))
with zipfile.ZipFile(live) as archive:
    root = ET.fromstring(archive.read("xl/workbook.xml"))
    for node in root.iter():
        if node.tag.rsplit("}", 1)[-1] != "definedName":
            continue
        print("NAME", node.attrib.get("name"), (node.text or "")[:180])
