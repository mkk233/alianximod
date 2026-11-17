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
stat = live.stat()


def sha256(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_xml(row, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row.find(">", start)
    if row[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</(?:[\w]+:)?c>", row[opener_end:])
        end = opener_end + close.end()
    return row[start:end]


def cell_text(cell):
    if not cell:
        return None
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    return html.unescape(value.group(1)) if value else None


with zipfile.ZipFile(live) as archive:
    parts = {name: archive.read(name) for name in archive.namelist()}
report = {
    "sha256": sha256(data),
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": len(data),
    "pages": {},
}
for title, name in (
    ("已完成", "xl/worksheets/sheet1.xml"),
    ("待制作", "xl/worksheets/sheet2.xml"),
    ("制作中", "xl/worksheets/sheet3.xml"),
    ("待验收", "xl/worksheets/sheet4.xml"),
):
    xml = parts[name].decode("utf-8")
    ids = []
    for row in rows_of(xml):
        number = row_number(row)
        if number >= 3:
            code = cell_text(cell_xml(row, "B", number))
            if code:
                ids.append([number, code])
    report["pages"][title] = {
        "rows": [row_number(row) for row in rows_of(xml)],
        "ids": ids,
        "dimensions": re.findall(r'ref="A1:EN\d+"', xml),
        "validations": re.findall(r'sqref="[^"]+"', xml),
    }
sheet4 = parts["xl/worksheets/sheet4.xml"].decode("utf-8")
row = next(item for item in rows_of(sheet4) if row_number(item) == 5)
deps = {}
for column in ("A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "P", "Q", "R", "S", "U", "AP", "EB", "EC"):
    deps[column] = cell_text(cell_xml(row, column, 5))
n = cell_text(cell_xml(row, "N", 5))
o = cell_text(cell_xml(row, "O", 5))
dv = cell_text(cell_xml(row, "DV", 5))
report["liaoli"] = {
    "deps": deps,
    "n": n,
    "oStarts": o[:80] if o else None,
    "oLen": len(o or ""),
    "dvLen": len(dv or ""),
    "hasFormula": "<f" in row,
    "hasKnown": "已知限制" in (dv or ""),
    "hasRings": "严格大于50" in (n or "") or "严格大于50" in (dv or ""),
}
report["resourceHasLiaoli"] = "fire_liaoli" in parts["xl/worksheets/sheet6.xml"].decode("utf-8")
print(json.dumps(report, ensure_ascii=False, indent=2))
