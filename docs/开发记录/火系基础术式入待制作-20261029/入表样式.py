# -*- coding: utf-8 -*-
import re
import zipfile
xml = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx").read("xl/worksheets/sheet4.xml").decode("utf-8")
row = re.search(r'<row r="3"[\s\S]*?</row>', xml).group(0)
print(re.match(r"<row\b[^>]*>", row).group(0))
for col in ("A", "B", "C", "D", "E", "J", "M", "N", "O", "P", "U", "AS", "DV", "DW", "EC", "EG"):
    match = re.search(rf'<c r="{col}3"[^>]*>', row)
    print(col, match.group(0) if match else "missing")
sheet2 = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx").read("xl/worksheets/sheet2.xml").decode("utf-8")
print("sheet2 val sample", "EB3:EB1000" in sheet2, "C3:C1000" in sheet2)
