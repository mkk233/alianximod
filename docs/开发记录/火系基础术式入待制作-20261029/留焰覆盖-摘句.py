# -*- coding: utf-8 -*-
import html
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")

def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)

def text_of(row, column, number):
    match = re.search(rf'<c r="{column}{number}"[^>]*>([\s\S]*?)</c>', row)
    if not match:
        return ""
    inner = match.group(1)
    inline = re.search(r"<t(?: [^>]*)?>([\s\S]*)</t>", inner)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", inner)
    return html.unescape(value.group(1)) if value else ""

with zipfile.ZipFile(live) as archive:
    xml = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
    for row in rows_of(xml):
        number = int(re.search(r'\br="(\d+)"', row).group(1))
        if number not in (8, 10):
            continue
        for column in ("N", "O", "DV"):
            text = text_of(row, column, number)
            start = 0
            while True:
                index = text.find("焰流", start)
                if index < 0:
                    break
                print(f"--- r{number} {column} {index}")
                print(text[max(0, index - 60):index + 80].replace("\n", " / "))
                start = index + 2
