# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
data = live.read_bytes()
want = {
    "fire_meteorite", "fire_shock_ring", "fire_scorch_field", "fire_seed_sow",
    "fire_hit_continue", "fire_stagger", "fire_break_ground",
}


def sha256(payload):
    return hashlib.sha256(payload).hexdigest().upper()


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
    names = archive.namelist()
    sheet2 = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
    sheet5 = archive.read("xl/worksheets/sheet5.xml").decode("utf-8")
    comments = [name for name in names if "comment" in name.lower()]
rows = rows_of(sheet2)
header = cells(rows[1])
located = []
for row in rows:
    number = row_number(row)
    info = cells(row)
    code = info.get("B")
    if code in want or (number <= 16 and number >= 3):
        located.append({
            "row": number,
            "A": info.get("A"),
            "B": code,
            "C": info.get("C"),
            "D": info.get("D"),
            "F": info.get("F"),
        })
attrs = []
for row in rows_of(sheet5):
    info = cells(row)
    if info.get("U"):
        attrs.append({"row": row_number(row), "U": info.get("U"), "V": info.get("V"), "W": info.get("W")})
report = {
    "sha256": sha256(data),
    "mtime": datetime.fromtimestamp(live.stat().st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": len(data),
    "F": header.get("F"),
    "D": header.get("D"),
    "C": header.get("C"),
    "rows": located,
    "attrs": attrs,
    "comments": comments,
}
out = Path(r"D:\alianximod\docs\开发记录\灼地产种设计入表-20261030\批注-定位.json")
out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"sha": report["sha256"], "rows": located, "attrs": attrs, "comments": comments}, ensure_ascii=False))
