# -*- coding: utf-8 -*-
import re
import zipfile

z = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx")
for part in ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"]:
    xml = z.read(part).decode("utf-8")
    print("====", part, "len", len(xml))
    rows = re.findall(r'<row\b[^>]*r="(\d+)"', xml)
    print("rows", rows)
    for pat in ["dimension", "autoFilter", "dataValidations", "mergeCell", "legacyDrawing", "drawing"]:
        found = re.findall(r"<" + pat + r"\b[^>]*>", xml)
        print(pat, found[:6])
    index = xml.find("autoFilter")
    print("auto slice", xml[max(0, index - 80): index + 240] if index >= 0 else "none")
    index = xml.find("dataValidations")
    print("dv head", xml[index: index + 500] if index >= 0 else "none")
print("--- comments ---")
for part in z.namelist():
    lower = part.lower()
    if "comment" in lower or lower.endswith(".vml"):
        text = z.read(part).decode("utf-8", errors="replace")
        refs = re.findall(r'ref="([A-Z]+\d+)"', text)
        print(part, "refs", len(refs), refs[:40], "has_fire", "fire_burn" in text)
