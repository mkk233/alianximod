# -*- coding: utf-8 -*-
import re
import zipfile

z = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx")
xml = z.read("xl/worksheets/sheet4.xml").decode("utf-8")
rows = re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)
row = next(item for item in rows if re.search(r'\br="6"', item))
for column in ("N", "O", "DV"):
    match = re.search(rf'<c r="{column}6"[\s\S]*?</c>', row)
    cell = match.group(0)
    print(column, "raw_nl", "\n" in cell, "entity10", "&#10;" in cell, "space", "xml:space" in cell, "amp", "&amp;" in cell)
    text_node = re.search(r"<t(?: [^>]*)?>([\s\S]*?)</t>", cell).group(1)
    print("  t_len", len(text_node), "head", text_node[:40].replace("\n", "\\n"))

sheet3 = z.read("xl/worksheets/sheet3.xml").decode("utf-8")
rows3 = re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet3)
print("MAKING")
for item in rows3:
    number = int(re.search(r'\br="(\d+)"', item).group(1))
    b = re.search(rf'<c r="B{number}"[\s\S]*?</c>', item)
    if not b:
        print(number, "NO-B", "cells", item.count("<c "))
        continue
    texts = re.findall(r"<t(?: [^>]*)?>([\s\S]*?)</t>", b.group(0))
    print(number, "".join(texts) if texts else "")
