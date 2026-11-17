# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
with zipfile.ZipFile(live) as archive:
    xml = archive.read("xl/worksheets/sheet6.xml").decode("utf-8")
print("LEN", len(xml))
print(xml[:1500])
print("---TAIL---")
print(xml[-1800:])
print("---VAL---")
for match in re.finditer(r"<dataValidation\b[^>]*>[\s\S]*?</dataValidation>", xml):
    text = match.group(0)
    print(text[:400])
    print("---")
