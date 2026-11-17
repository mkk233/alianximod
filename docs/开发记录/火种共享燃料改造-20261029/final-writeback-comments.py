# -*- coding: utf-8 -*-
import re
import zipfile
archive = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx")
for name in ("xl/comments/comment3.xml", "xl/comments/comment4.xml", "xl/worksheets/sheet6.xml"):
    xml = archive.read(name).decode("utf-8")
    print("==", name, "len", len(xml))
    if "comment" in name:
        refs = re.findall(r'\bref="([A-Z]+\d+)"', xml)
        print("refs", refs[:30], "count", len(refs))
    else:
        idx = xml.find("dataValidation")
        print(xml[max(0, idx - 80):idx + 500] if idx >= 0 else "no validation")
sheet3 = archive.read("xl/worksheets/sheet3.xml").decode("utf-8")
match = re.search(r'<c r="BV3"[^>]*>[\s\S]*?</c>', sheet3)
print("BV3", match.group(0)[:200] if match else "missing")
match = re.search(r'<c r="EE3"[^>]*>[\s\S]*?</c>', sheet3)
print("EE3", match.group(0)[:220] if match else "missing")
match = re.search(r'<c r="EI3"[^>]*>[\s\S]*?</c>', sheet3)
print("EI3", match.group(0)[:180] if match else "missing")
