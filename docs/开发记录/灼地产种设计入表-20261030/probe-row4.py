# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
with zipfile.ZipFile(live) as archive:
    xml = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
row = re.search(r'<row r="4"[^>]*>[\s\S]*?</row>', xml).group(0)
for match in re.finditer(r'<c r="([A-Z]+)4"[^>]*?(?:/>|>)', row):
    tag = match.group(0)
    if tag.endswith(">"):
        tag = tag[:-1] + ">"
    print(match.group(1), tag[:180])
