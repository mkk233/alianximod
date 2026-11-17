# -*- coding: utf-8 -*-
import html
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
want = {
    "fire_meteorite", "fire_shock_ring", "fire_scorch_field", "fire_seed_sow",
    "fire_hit_continue", "fire_stagger", "fire_break_ground",
}


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cells(row):
    found = {}
    for match in re.finditer(r'<(?:[\w]+:)?c r="([A-Z]+)(\d+)"([^>]*?)(?:/>|>([\s\S]*?)</(?:[\w]+:)?c>)', row):
        inner = match.group(4)
        text = None
        kind = "empty"
        if inner:
            if re.search(r't="n"', match.group(0)[:80]) or 't="n"' in match.group(0)[:120]:
                kind = "n"
            inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", inner)
            value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", inner)
            if inline:
                text = html.unescape(inline.group(1))
                kind = "inline"
            elif value:
                text = html.unescape(value.group(1))
        found[match.group(1)] = (kind, text, match.group(0)[:80])
    return found


with zipfile.ZipFile(live) as archive:
    sheet2 = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
out = Path(r"D:\alianximod\docs\开发记录\灼地产种设计入表-20261030\批注-摘句.txt")
lines = []
for row in rows_of(sheet2):
    info = cells(row)
    code = (info.get("B") or (None, None, None))[1]
    if code not in want:
        continue
    number = row_number(row)
    lines.append(f"## {number} {code}")
    lines.append("ORDER " + " ".join(info))
    for column in ("C", "D", "E", "F"):
        if column in info:
            lines.append(f"{column} {info[column][0]} {info[column][2]}")
    for column in ("N", "DV"):
        text = info[column][1]
        lines.append(f"---{column}---")
        lines.append(text)
    lines.append("")
out.write_text("\n".join(lines), encoding="utf-8")
print("wrote", len(lines))
