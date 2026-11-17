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
old_ids = ("fire_surface_anchor", "fire_stagger", "fire_hit_continue", "fire_break_ground")
new_ids = ("rule_surface_anchor", "rule_stagger", "rule_hit_continue", "earth_break_ground")


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
    sheets = {
        name: archive.read(name).decode("utf-8")
        for name in (
            "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml",
            "xl/worksheets/sheet4.xml", "xl/worksheets/sheet5.xml", "xl/worksheets/sheet6.xml",
        )
    }
rows = {row_number(row): row for row in rows_of(sheets["xl/worksheets/sheet2.xml"])}
stat = live.stat()
report = {
    "sha256": sha256(data),
    "bytes": len(data),
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "cells": {
        number: {"A": cell_text(rows[number], "A", number), "B": cell_text(rows[number], "B", number), "D": cell_text(rows[number], "D", number)}
        for number in (7, 9, 10, 15)
    },
    "oldLeft": sum(xml.count(old) for xml in sheets.values() for old in old_ids),
    "newCounts": {new: sheets["xl/worksheets/sheet2.xml"].count(new) for new in new_ids},
    "heatWave": cell_text(rows[6], "B", 6),
}
Path(r"D:\alianximod\docs\开发记录\灼地产种设计入表-20261030\编号-reopen.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8"
)
print(json.dumps(report, ensure_ascii=False))
