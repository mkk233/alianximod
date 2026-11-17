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


def sha256(payload):
    return hashlib.sha256(payload).hexdigest().upper()


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


def ids(xml):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        code = cell_text(row, "B", number) if number >= 3 else None
        if code:
            found.append(code)
    return found


with zipfile.ZipFile(live) as archive:
    sheets = {name: archive.read(name).decode("utf-8") for name in (
        "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml",
    )}
counts = {
    "已完成": len(ids(sheets["xl/worksheets/sheet1.xml"])),
    "待制作": len(ids(sheets["xl/worksheets/sheet2.xml"])),
    "制作中": len(ids(sheets["xl/worksheets/sheet3.xml"])),
    "待验收": len(ids(sheets["xl/worksheets/sheet4.xml"])),
}
all_ids = [code for xml in sheets.values() for code in ids(xml)]
rows = {row_number(row): row for row in rows_of(sheets["xl/worksheets/sheet4.xml"])}
note = cell_text(rows[4], "DV", 4)
stat = live.stat()
report = {
    "sha256": sha256(data),
    "bytes": len(data),
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "counts": counts,
    "unique": len(set(all_ids)),
    "id": cell_text(rows[4], "B", 4),
    "radius": cell_text(rows[4], "AS", 4),
    "damage": cell_text(rows[4], "E", 4),
    "bodyPeriod": cell_text(rows[4], "BV", 4),
    "life": cell_text(rows[4], "BW", 4),
    "seed": cell_text(rows[4], "EC", 4),
    "burnPeriod": cell_text(rows[4], "EG", 4),
    "delay": cell_text(rows[4], "EH", 4),
    "trigger": cell_text(rows[4], "EB", 4),
    "mark": "待美术与代码落实" in note,
    "oldPath": "ground_flame.png" in cell_text(rows[4], "O", 4),
    "noNewPath": "burnt_grass" not in cell_text(rows[4], "O", 4),
}
Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031\灰烬-reopen.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8"
)
print(json.dumps(report, ensure_ascii=False))
