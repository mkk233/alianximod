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


with zipfile.ZipFile(live) as archive:
    sheets = {name: archive.read(name).decode("utf-8") for name in (
        "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml",
        "xl/worksheets/sheet4.xml", "xl/worksheets/sheet6.xml",
    )}
rows = {row_number(row): row for row in rows_of(sheets["xl/worksheets/sheet2.xml"])}
left = sum(xml.count("震环") for xml in sheets.values())
stat = live.stat()
report = {
    "sha256": sha256(data),
    "bytes": len(data),
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "name": cell_text(rows[6], "A", 6),
    "id": cell_text(rows[6], "B", 6),
    "element": cell_text(rows[6], "D", 6),
    "damage": cell_text(rows[6], "E", 6),
    "cooling": cell_text(rows[6], "F", 6),
    "anchor": cell_text(rows[7], "A", 7),
    "anchorId": cell_text(rows[7], "B", 7),
    "anchorType": cell_text(rows[7], "C", 7),
    "anchorElement": cell_text(rows[7], "D", 7),
    "oldNameLeft": left,
    "meteor": "热波" in cell_text(rows[5], "N", 5) and "震环" not in cell_text(rows[5], "N", 5),
    "continue": "热波" in cell_text(rows[10], "N", 10) and "震环" not in cell_text(rows[10], "N", 10),
    "kindle": "热波" in cell_text(rows[12], "N", 12) and "震环" not in cell_text(rows[12], "N", 12),
    "detonate": "热波" in cell_text(rows[13], "N", 13) and "震环" not in cell_text(rows[13], "N", 13),
    "note": "热波" in cell_text(rows[6], "DV", 6) and "震环" not in cell_text(rows[6], "DV", 6),
}
Path(r"D:\alianximod\docs\开发记录\灼地产种设计入表-20261030\热波-reopen.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8"
)
print(json.dumps(report, ensure_ascii=False))
