# -*- coding: utf-8 -*-
import html
import re
import zipfile

xml = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx").read("xl/worksheets/sheet3.xml").decode("utf-8")
rows = re.findall(r"<row\b[^>]*>[\s\S]*?</row>", xml)
print("row tags:")
for row in rows:
    print(re.match(r"<row\b[^>]*>", row).group(0)[:180])

def cells(row):
    found = {}
    for match in re.finditer(r'<c r="([A-Z]+)(\d+)"([^>/]*)(/?)>', row):
        found[match.group(1)] = "self" if match.group(4) == "/" else "open"
    return found

for row in rows[2:5]:
    number = re.search(r'\br="(\d+)"', row).group(1)
    present = cells(row)
    print("ROW", number, {key: present.get(key) for key in ("EB", "EC", "EE", "EF", "EG", "EH", "EI", "EJ", "EK")})

# hardcoded BN ranges
blob = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx")
for name in blob.namelist():
    if not name.endswith(".xml"):
        continue
    text = blob.read(name).decode("utf-8", errors="ignore")
    if "BN$11" in text or "BN4:BN11" in text or "$BN$4:$BN$11" in text:
        print("HARD", name)
print("sheet6 validations snippet", "dataValidation" in blob.read("xl/worksheets/sheet6.xml").decode("utf-8"))
# comments
for name in blob.namelist():
    if "comment" in name or "vml" in name:
        print("PART", name)
