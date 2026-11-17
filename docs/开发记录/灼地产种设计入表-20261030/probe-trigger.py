# -*- coding: utf-8 -*-
import html
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def cell_map(row):
    found = {}
    for match in re.finditer(r'<(?:[\w]+:)?c r="([A-Z]+)(\d+)"([^>]*?)(?:/>|>([\s\S]*?)</(?:[\w]+:)?c>)', row):
        inner = match.group(4)
        text = ""
        if inner:
            inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", inner)
            value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", inner)
            if inline:
                text = html.unescape(inline.group(1))
            elif value:
                text = html.unescape(value.group(1))
        found[match.group(1)] = text
    return found


with zipfile.ZipFile(live) as archive:
    sheet5 = archive.read("xl/worksheets/sheet5.xml").decode("utf-8")
rows = [cell_map(row) for row in rows_of(sheet5)]
header = rows[1] if len(rows) > 1 else rows[0]
print("ROW1", rows[0])
print("ROW2 keys", header)
for column, text in header.items():
    if text in ("触发时机", "作用对象", "计量单位", "操作", "寿命策略", "资源"):
        print("COL", column, text)
        for index, row in enumerate(rows[2:40], start=3):
            value = row.get(column, "")
            neighbor = row.get(chr(ord(column) + 1), "") if len(column) == 1 else ""
            if value or neighbor:
                print(index, value, "|", neighbor[:80])
