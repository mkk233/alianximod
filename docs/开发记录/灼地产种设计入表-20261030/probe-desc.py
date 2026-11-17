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
    rows = [cell_map(row) for row in rows_of(archive.read("xl/worksheets/sheet5.xml").decode("utf-8"))]
for index, row in enumerate(rows[2:16], start=3):
    print(index, "BN", row.get("BN"), "| BO", row.get("BO"), "| BP", row.get("BP"), "| BQ", row.get("BQ"))
    print("   BL", row.get("BL"), "| BM", row.get("BM"), "| BR", row.get("BR"), "| BS", row.get("BS"))
