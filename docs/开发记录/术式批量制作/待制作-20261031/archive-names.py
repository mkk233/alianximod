# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
xml = zipfile.ZipFile(live).read("xl/workbook.xml").decode("utf-8")
for match in re.finditer(r"<definedName\b([^>]*)>([\s\S]*?)</definedName>", xml):
    print(match.group(1).strip(), "=>", match.group(2)[:180])
