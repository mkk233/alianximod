# -*- coding: utf-8 -*-
import html
import json
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def texts(row):
    found = []
    for match in re.finditer(r'<(?:[\w]+:)?c r="([A-Z]+)(\d+)"([^>]*?)(?:/>|>([\s\S]*?)</(?:[\w]+:)?c>)', row):
        inner = match.group(4)
        text = None
        if inner:
            inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", inner)
            value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", inner)
            if inline:
                text = html.unescape(inline.group(1))
            elif value:
                text = html.unescape(value.group(1))
        if text:
            found.append((match.group(1), text))
    return found


with zipfile.ZipFile(live) as archive:
    sheet5 = archive.read("xl/worksheets/sheet5.xml").decode("utf-8")
    sheet2 = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
wanted = ("触发", "命中", "接触", "周期", "增加", "火种", "计量", "对象", "受伤", "有效伤害")
lines = []
for row in rows_of(sheet5):
    items = texts(row)
    blob = " ".join(text for _, text in items)
    if any(word in blob for word in wanted) and len(blob) < 400:
        lines.append(blob)
valid = re.findall(r'<dataValidation\b[^>]*sqref="(EB[^"]+|EA[^"]+|EI[^"]+|DZ[^"]+|DW[^"]+|DX[^"]+)"[^>]*>[\s\S]*?</dataValidation>', sheet2)
print("VALID", len(valid))
print("---DICT---")
print("\n".join(lines[:80]))
