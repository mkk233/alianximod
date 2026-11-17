# -*- coding: utf-8 -*-
import json
import re
import zipfile
from pathlib import Path

z = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx")
snap = json.loads(Path(r"D:\alianximod\docs\开发记录\燃烧空间挂载接入-20261101\快照\变更前-NODV-20261101-214704-1.json").read_text(encoding="utf-8"))
current = json.loads(Path(r"D:\alianximod\docs\开发记录\燃烧空间挂载接入-20261101\第一人称空手返修-20261109\表格\inspect-current.json").read_text(encoding="utf-8"))
hit = current["hits"][0]
append = snap["pendingDisplay"]["dvAppend"]
print("N equals snapshot", hit["N"] == snap["N"])
print("N equals display", hit["N"] == snap["pendingDisplay"]["N"])
print("O equals snapshot", hit["O"] == snap["O"])
print("O equals display", hit["O"] == snap["pendingDisplay"]["O"])
print("DV equals snapshot", hit["DV"] == snap["DV"])
print("DV equals snapshot+append", hit["DV"] == snap["DV"] + "\n" + append)
print("DV len", len(hit["DV"]), "snap", len(snap["DV"]))
xml = z.read("xl/worksheets/sheet4.xml").decode("utf-8")
row = re.search(r'<row\b[^>]*r="7"[^>]*>[\s\S]*?</row>', xml).group(0)
print("row head", row[:180])
for col in ("N", "O", "DV", "B", "A"):
    cell = re.search(r'<c r="' + col + r'7"[^>]*>[\s\S]*?</c>', row)
    print(col, "FOUND" if cell else "MISSING", (cell.group(0)[:220] if cell else ""))
print("formula count", row.count("<f"))
print("sheet4 conditional", "conditionalFormatting" in xml)
print("sheet3 conditional", "conditionalFormatting" in z.read("xl/worksheets/sheet3.xml").decode("utf-8"))
wb = z.read("xl/workbook.xml").decode("utf-8")
print("defined count", wb.count("制作中!$A$2:$EN$18"))
print("pending defined", wb.count("待验收!$A$2:$EN$2"))
