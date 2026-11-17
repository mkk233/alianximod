# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
with zipfile.ZipFile(live) as archive:
    xml = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
row = re.search(r'<row r="4"[^>]*>[\s\S]*?</row>', xml).group(0)
for column in ("AB", "N", "E", "BV"):
    match = re.search(rf'<c r="{column}4"[\s\S]*?</c>', row)
    text = match.group(0)
    print(column, text[:220].replace("\n", "\\n"))
    print("---")
