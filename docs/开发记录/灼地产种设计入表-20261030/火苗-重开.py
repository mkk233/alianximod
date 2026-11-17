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
        if number >= 3:
            code = cell_text(row, "B", number)
            if code:
                found.append(code)
    return found


with zipfile.ZipFile(live) as archive:
    sheets = {name: archive.read(name).decode("utf-8") for name in (
        "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml",
    )}
    detail = archive.read("xl/worksheets/sheet6.xml").decode("utf-8")
counts = {title: len(ids(sheets[name])) for title, name in (
    ("已完成", "xl/worksheets/sheet1.xml"), ("待制作", "xl/worksheets/sheet2.xml"),
    ("制作中", "xl/worksheets/sheet3.xml"), ("待验收", "xl/worksheets/sheet4.xml"),
)}
all_ids = [code for xml in sheets.values() for code in ids(xml)]
rows = {row_number(row): row for row in rows_of(sheets["xl/worksheets/sheet2.xml"])}
stat = live.stat()
report = {
    "sha256": sha256(data),
    "bytes": len(data),
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "counts": counts,
    "unique": len(set(all_ids)),
    "name": cell_text(rows[16], "A", 16),
    "id": cell_text(rows[16], "B", 16),
    "damage": cell_text(rows[16], "E", 16),
    "cooling": cell_text(rows[16], "F", 16),
    "seeds": cell_text(rows[16], "EC", 16),
    "trigger": cell_text(rows[16], "EB", 16),
    "durationEmpty": cell_text(rows[16], "EF", 16) is None,
    "durationNote": "原版燃烧时长待定，建议20tick" in cell_text(rows[16], "DV", 16),
    "detailHasSpark": "fire_spark" in detail,
    "seedSowCooling": cell_text(rows[11], "F", 11),
}
Path(r"D:\alianximod\docs\开发记录\灼地产种设计入表-20261030\火苗-reopen.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8"
)
print(json.dumps(report, ensure_ascii=False))
