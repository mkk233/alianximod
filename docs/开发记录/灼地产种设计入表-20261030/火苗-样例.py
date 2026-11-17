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


want = {16, 22, 29}
with zipfile.ZipFile(live) as archive:
    sheet1 = archive.read("xl/worksheets/sheet1.xml").decode("utf-8")
    sheet2 = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
lines = []
for row in rows_of(sheet1):
    number = row_number(row)
    if number not in want:
        continue
    info = cells(row)
    lines.append(f"## completed {number} {info.get('A')} {info.get('B')}")
    lines.append(" ".join(f"{column}={info[column]}" for column in info if column not in ("N", "O", "DV") and info[column]))
    lines.append("N=" + (info.get("N") or "")[:400])
    lines.append("O=" + (info.get("O") or "")[:200])
    lines.append("")
pending = [row for row in rows_of(sheet2) if row_number(row) == 15][0]
lines.append("ROW15 OPEN " + pending[:180])
short = []
for match in re.finditer(r'sqref="([^"]+)"', sheet2):
    ref = match.group(1)
    if re.search(r"1[0-5]", ref) or ref.endswith("15") or ":EN" in ref:
        short.append(ref)
lines.append("VALS " + " | ".join(short[:30]))
Path(r"D:\alianximod\docs\开发记录\灼地产种设计入表-20261030\火苗-样例.txt").write_text("\n".join(lines), encoding="utf-8")
print("ok", len(short))
