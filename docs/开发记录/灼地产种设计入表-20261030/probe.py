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


def cells(row):
    found = {}
    for match in re.finditer(r'<(?:[\w]+:)?c r="([A-Z]+)(\d+)"([^>]*?)(?:/>|>([\s\S]*?)</(?:[\w]+:)?c>)', row):
        column, number, attrs, inner = match.group(1), match.group(2), match.group(3), match.group(4)
        text = None
        if inner:
            inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", inner)
            value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", inner)
            if inline:
                text = html.unescape(inline.group(1))
            elif value:
                text = html.unescape(value.group(1))
        found[column] = {"n": number, "t": text, "self": inner is None}
    return found


def col_index(name):
    total = 0
    for char in name:
        total = total * 26 + ord(char) - 64
    return total


with zipfile.ZipFile(live) as archive:
    parts = {name: archive.read(name) for name in archive.namelist()}

report = {
    "sha256": sha256(data),
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": len(data),
}
sheet2 = parts["xl/worksheets/sheet2.xml"].decode("utf-8")
header = cells(rows_of(sheet2)[1])
labels = {column: header[column]["t"] for column in sorted(header, key=col_index)}
report["headers"] = labels
located = []
mentions = []
for title, name in (
    ("已完成", "xl/worksheets/sheet1.xml"),
    ("待制作", "xl/worksheets/sheet2.xml"),
    ("制作中", "xl/worksheets/sheet3.xml"),
    ("待验收", "xl/worksheets/sheet4.xml"),
):
    xml = parts[name].decode("utf-8")
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        info = cells(row)
        code = (info.get("B") or {}).get("t")
        if code == "fire_scorch_field":
            located.append({"sheet": title, "row": number, "cells": {column: info[column]["t"] for column in info if info[column]["t"]}})
        for column, cell in info.items():
            text = cell["t"] or ""
            if "灼地" in text or "fire_scorch_field" in text:
                mentions.append({"sheet": title, "row": number, "column": column, "id": code, "text": text[:180]})
report["located"] = located
report["mentions"] = mentions
sheet6 = parts["xl/worksheets/sheet6.xml"].decode("utf-8")
detail_rows = []
for row in rows_of(sheet6):
    info = cells(row)
    blob = " ".join(cell["t"] or "" for cell in info.values())
    if "灼地" in blob or "fire_scorch" in blob or row_number(row) <= 2:
        detail_rows.append({"row": row_number(row), "cells": {column: info[column]["t"] for column in sorted(info, key=col_index)}})
report["detail"] = detail_rows
(Path(r"D:\alianximod\docs\开发记录\灼地产种设计入表-20261030") / "probe.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8"
)
print(json.dumps({
    "sha": report["sha256"],
    "located": [(item["sheet"], item["row"]) for item in located],
    "mentionCount": len(mentions),
    "detailRows": [item["row"] for item in detail_rows],
}, ensure_ascii=False))
