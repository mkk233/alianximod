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


def text_of(row, column, number):
    match = re.search(rf'<(?:[\w]+:)?c r="{column}{number}"[\s\S]*?</(?:[\w]+:)?c>', row)
    if not match:
        return None
    inner = match.group(0)
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", inner)
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", inner)
    raw = inline.group(1) if inline else value.group(1) if value else None
    return html.unescape(raw) if raw else None


with zipfile.ZipFile(live) as archive:
    names = archive.namelist()
    for sheet, title in (("xl/worksheets/sheet3.xml", "制作中"), ("xl/worksheets/sheet4.xml", "待验收")):
        xml = archive.read(sheet).decode("utf-8")
        print("SHEET", title, re.search(r'ref="[^"]+"', xml).group(0))
        print("ROWS", [row_number(row) for row in rows_of(xml)])
        for row in rows_of(xml):
            number = row_number(row)
            if number >= 3:
                print(" ", number, text_of(row, "A", number), text_of(row, "B", number))
        refs = sorted(set(re.findall(r'sqref="([^"]+)"', xml)))
        print("VAL", " | ".join(refs[:12]), "...", len(refs))
    rel = archive.read("xl/worksheets/_rels/sheet4.xml.rels").decode("utf-8") if "xl/worksheets/_rels/sheet4.xml.rels" in names else ""
    print("REL4", rel[:500])
    rel3 = archive.read("xl/worksheets/_rels/sheet3.xml.rels").decode("utf-8") if "xl/worksheets/_rels/sheet3.xml.rels" in names else ""
    print("REL3", rel3[:400])
