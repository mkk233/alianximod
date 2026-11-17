# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
with zipfile.ZipFile(live) as archive:
    sheet2 = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
refs = re.findall(r'sqref="([^"]+)"', sheet2)
print("COUNT", len(refs))
for ref in refs:
    print(ref)
print("DIM", re.search(r'<dimension[^/]*/>', sheet2).group(0))
print("ROWS", re.findall(r'<row r="(\d+)"', sheet2))
