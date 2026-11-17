# -*- coding: utf-8 -*-
import zipfile
from pathlib import Path
xml = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx").read("xl/worksheets/sheet5.xml").decode("utf-8")
for needle in ("首次带资源接触", "范围选取", "命中成功", "每段", "周期"):
    print(needle, xml.find(needle))
index = xml.find("触发时机")
print(xml[index:index + 2500])
