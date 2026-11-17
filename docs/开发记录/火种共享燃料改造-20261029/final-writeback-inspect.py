# -*- coding: utf-8 -*-
import hashlib
import html
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
data = live.read_bytes()
print("sha", hashlib.sha256(data).hexdigest().upper(), "bytes", len(data))
with zipfile.ZipFile(live) as archive:
    workbook = archive.read("xl/workbook.xml").decode("utf-8")
    names = re.findall(r"<definedName\b([^>]*)>([\s\S]*?)</definedName>", workbook)
    for attrs, body in names:
        if "触发" in body or "BN" in body or "触发" in attrs:
            print("NAME", attrs, body[:300].replace("\n", " "))
    rels = archive.read("xl/_rels/workbook.xml.rels").decode("utf-8")
    print("--- validations ---")
    for part in ("xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml", "xl/worksheets/sheet6.xml"):
        xml = archive.read(part).decode("utf-8")
        vals = re.findall(r"<dataValidation\b[^>]*>[\s\S]*?</dataValidation>", xml)
        hits = [item for item in vals if "触发" in item or "BN" in item]
        print(part, "validations", len(vals), "trigger-like", len(hits))
        for item in hits[:3]:
            print(item[:400].replace("\n", " "))
    sheet5 = archive.read("xl/worksheets/sheet5.xml").decode("utf-8")
    cells = re.findall(r'<c r="(BN\d+)"([^>]*)>([\s\S]*?)</c>', sheet5)
    print("--- BN cells", len(cells))
    for ref, attrs, inner in cells:
        text = re.search(r"<t[^>]*>([\s\S]*)</t>", inner)
        print(ref, attrs, html.unescape(text.group(1)) if text else inner[:80])
    sheet3 = archive.read("xl/worksheets/sheet3.xml").decode("utf-8")
    sheet4 = archive.read("xl/worksheets/sheet4.xml").decode("utf-8")
    print("sheet3 rows", re.findall(r'<row r="(\d+)"', sheet3))
    print("sheet4 rows", re.findall(r'<row r="(\d+)"', sheet4))
    print("sheet3 dimension", re.search(r"<dimension[^/]*/>", sheet3).group(0) if re.search(r"<dimension[^/]*/>", sheet3) else "none")
    print("sheet4 dimension", re.search(r"<dimension[^/]*/>", sheet4).group(0) if re.search(r"<dimension[^/]*/>", sheet4) else "none")
    header = re.search(r'<row r="2"[\s\S]*?</row>', sheet3).group(0)
    headers = re.findall(r'<c r="([A-Z]+)2"[^>]*>[\s\S]*?<t[^>]*>([\s\S]*?)</t>', header)
    for col, text in headers:
        plain = html.unescape(text)
        if "周期" in plain or col in ("EF", "EG", "EH", "EI", "EB", "EC"):
            print("H", col, plain)
