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


def cell_text(row, column, number):
    match = re.search(rf'<(?:[\w]+:)?c r="{column}{number}"[\s\S]*?</(?:[\w]+:)?c>', row)
    if not match:
        return None
    inner = match.group(0)
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", inner)
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", inner)
    raw = inline.group(1) if inline else value.group(1) if value else None
    return html.unescape(raw) if raw is not None else None


with zipfile.ZipFile(live) as archive:
    sheet2 = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
rows = {row_number(row): row for row in rows_of(sheet2)}
lines = []
for number, column in ((5, "N"), (6, "N"), (6, "O"), (6, "DV"), (10, "N"), (12, "N"), (13, "N"), (7, "N"), (7, "DV")):
    text = cell_text(rows[number], column, number) or ""
    lines.append(f"## {column}{number}")
    for part in re.split(r"(?<=。)", text):
        if "震环" in part or column == "O" and number == 6:
            lines.append(part)
    lines.append("")
Path(r"D:\alianximod\docs\开发记录\灼地产种设计入表-20261030\热波-摘句.txt").write_text("\n".join(lines), encoding="utf-8")
print("ok")
