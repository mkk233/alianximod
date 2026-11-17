# -*- coding: utf-8 -*-
import html
import re
import zipfile

xml = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx").read("xl/worksheets/sheet5.xml").decode("utf-8")
rows = re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)

def cells(row_xml):
    found = {}
    for match in re.finditer(r'<(?:[\w]+:)?c r="([A-Z]+)(\d+)"([^>]*)>([\s\S]*?)</(?:[\w]+:)?c>', row_xml):
        inner = match.group(4)
        text = re.search(r"<(?:[\w]+:)?t\b[^>]*>([\s\S]*)</(?:[\w]+:)?t>", inner)
        if text:
            found[match.group(1)] = html.unescape(text.group(1))
    return found

header = cells(rows[1])
print("row2", {key: header[key] for key in header if header[key] in ("触发时机", "计量单位", "操作", "数值") or key in ("BN", "BO", "BP", "BR")})
for label, column in (("触发时机", None),):
    pass
for column, title in header.items():
    if title in ("触发时机", "计量单位", "操作"):
        print("COL", column, title)
        values = []
        for row_xml in rows[3:]:
            value = cells(row_xml).get(column)
            if value:
                values.append(value)
        print(values)
