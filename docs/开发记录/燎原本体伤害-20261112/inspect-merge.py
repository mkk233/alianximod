# -*- coding: utf-8 -*-
import html
import json
import re
import zipfile
from pathlib import Path

z = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx")
snap = json.loads(Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031\实施-交付快照.json").read_text(encoding="utf-8"))
text = snap["sheetOriginal"]["N"]
for match in re.finditer(r".{0,24}8.{0,24}", text):
    print(match.start(), match.group(0).replace("\n", "\\n"))
print("---O 8---")
other = snap["sheetOriginal"]["O"]
print("O hits", len(re.findall("8", other)))
print("---merges sheet4---")
xml = z.read("xl/worksheets/sheet4.xml").decode("utf-8")
print(re.findall(r"<mergeCell[^/]*/>", xml))
print("---cols N width---")
for sheet in ("sheet3", "sheet4"):
    body = z.read(f"xl/worksheets/{sheet}.xml").decode("utf-8")
    cols = re.findall(r"<col\b[^/]*/>", body)
    print(sheet, "col count", len(cols))
    for col in cols:
        if "min=\"14\"" in col or "min=\"5\"" in col or "min=\"126\"" in col:
            print(col)
print("---making long rows---")
rows = re.findall(r"<(?:[\w]+:)?row\b[^>]*>", z.read("xl/worksheets/sheet3.xml").decode("utf-8"))
for row in rows:
    print(row)
