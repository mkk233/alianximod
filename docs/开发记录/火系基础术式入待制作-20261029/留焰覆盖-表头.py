# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
with zipfile.ZipFile(live) as archive:
    xml = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
for column in ("A", "C", "E", "J", "M", "P", "Q", "R", "S", "U", "AB", "AH", "AK"):
    for number in (1, 2):
        match = re.search(rf'<c r="{column}{number}"[\s\S]{{0,180}}', xml)
        print(column, number, "NONE" if not match else match.group(0)[:160])
