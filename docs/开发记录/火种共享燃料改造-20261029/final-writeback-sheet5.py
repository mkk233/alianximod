# -*- coding: utf-8 -*-
import re
import zipfile
xml = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx").read("xl/worksheets/sheet5.xml").decode("utf-8")
print(re.findall(r'<row r="(\d+)"', xml))
row12 = re.search(r'<row r="12"[\s\S]*?</row>', xml)
print("row12", "yes" if row12 else "no")
if row12:
    refs = re.findall(r'r="([A-Z]+)12"', row12.group(0))
    print(refs)
row11 = re.search(r'<row r="11"[\s\S]*?</row>', xml)
print("row11 tail", row11.group(0)[-180:] if row11 else "no")
sheet6 = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx").read("xl/worksheets/sheet6.xml").decode("utf-8")
print(re.findall(r'sqref="([^"]+)"[\s\S]*?<ns0:formula1>([^<]+)</ns0:formula1>', sheet6))
