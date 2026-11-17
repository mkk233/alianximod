# -*- coding: utf-8 -*-
import html
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cells(row):
    found = {}
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
        found[match.group(1)] = text
    return found


with zipfile.ZipFile(live) as archive:
    for name, title in (
        ("xl/worksheets/sheet1.xml", "已完成"),
        ("xl/worksheets/sheet2.xml", "待制作"),
        ("xl/worksheets/sheet3.xml", "制作中"),
        ("xl/worksheets/sheet4.xml", "待验收"),
    ):
        for row in rows_of(archive.read(name).decode("utf-8")):
            number = row_number(row)
            if number < 3:
                continue
            info = cells(row)
            element = info.get("D")
            code = info.get("B") or ""
            if element in ("土", "无") or code.startswith(("rule_", "earth_", "stone_", "sinking_", "universal_")):
                print(title, number, info.get("A"), code, info.get("C"), element)
