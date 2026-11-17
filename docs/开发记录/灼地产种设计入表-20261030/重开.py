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
    sheet2 = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
    sheet6 = archive.read("xl/worksheets/sheet6.xml").decode("utf-8")
    counts = {}
    all_ids = []
    for title, name in (
        ("已完成", "xl/worksheets/sheet1.xml"),
        ("待制作", "xl/worksheets/sheet2.xml"),
        ("制作中", "xl/worksheets/sheet3.xml"),
        ("待验收", "xl/worksheets/sheet4.xml"),
    ):
        page = ids(archive.read(name).decode("utf-8"))
        counts[title] = len(page)
        all_ids.extend(page)
row = [item for item in rows_of(sheet2) if row_number(item) == 4][0]
detail = [item for item in rows_of(sheet6) if row_number(item) == 7][0]
stat = live.stat()
report = {
    "sha256": sha256(data),
    "bytes": len(data),
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "counts": counts,
    "unique": len(set(all_ids)),
    "row": 4,
    "id": cell_text(row, "B", 4),
    "damage": cell_text(row, "E", 4),
    "radius": cell_text(row, "AS", 4),
    "life": cell_text(row, "BW", 4),
    "periodGone": cell_text(row, "BV", 4) is None,
    "triggerGone": cell_text(row, "EB", 4) is None,
    "firstAmount": cell_text(row, "EC", 4),
    "resource": cell_text(row, "DX", 4),
    "detailAmount": cell_text(detail, "I", 7),
    "detailPeriod": cell_text(detail, "M", 7),
    "detailTriggerGone": cell_text(detail, "H", 7) is None,
    "detailCount": sheet6.count("fire_scorch_field"),
    "oldSentenceGone": "每20tick造成一次基础2点火伤" not in cell_text(row, "N", 4),
}
(Path(r"D:\alianximod\docs\开发记录\灼地产种设计入表-20261030") / "reopen.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8"
)
print(json.dumps(report, ensure_ascii=False))
