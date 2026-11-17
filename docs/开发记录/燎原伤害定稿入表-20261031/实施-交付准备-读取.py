# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
payload = live.read_bytes()


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
    return html.unescape(raw) if raw else None


parts = (
    ("已完成", "xl/worksheets/sheet1.xml"),
    ("待制作", "xl/worksheets/sheet2.xml"),
    ("制作中", "xl/worksheets/sheet3.xml"),
    ("待验收", "xl/worksheets/sheet4.xml"),
)
stat = live.stat()
located = []
counts = {}
ids = []
with zipfile.ZipFile(live) as archive:
    for title, name in parts:
        xml = archive.read(name).decode("utf-8")
        page = []
        for row in rows_of(xml):
            number = row_number(row)
            if number < 3:
                continue
            code = cell_text(row, "B", number)
            if not code:
                continue
            page.append({"id": code, "row": number, "name": cell_text(row, "A", number)})
            if code == "fire_liaoyuan":
                located.append({
                    "sheet": title,
                    "row": number,
                    "cells": {
                        column: cell_text(row, column, number)
                        for column in ("A", "B", "C", "D", "E", "F", "AS", "BV", "BW", "EB", "EC", "EG", "EH", "N", "O", "DV")
                    },
                })
        counts[title] = len(page)
        ids.extend(item["id"] for item in page)
out = {
    "sha256": hashlib.sha256(payload).hexdigest().upper(),
    "bytes": stat.st_size,
    "mtimeNs": stat.st_mtime_ns,
    "counts": counts,
    "unique": len(set(ids)),
    "total": len(ids),
    "located": located,
    "making": None,
    "pending": None,
}
with zipfile.ZipFile(live) as archive:
    making = []
    pending = []
    for title, name, bucket in (
        ("制作中", "xl/worksheets/sheet3.xml", making),
        ("待验收", "xl/worksheets/sheet4.xml", pending),
    ):
        xml = archive.read(name).decode("utf-8")
        for row in rows_of(xml):
            number = row_number(row)
            if number < 3:
                continue
            code = cell_text(row, "B", number)
            if code:
                bucket.append({"row": number, "id": code, "name": cell_text(row, "A", number)})
    out["making"] = making
    out["pending"] = pending
path = Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031\实施-交付准备-读取.json")
path.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
print(out["sha256"], out["bytes"], out["counts"], [(item["sheet"], item["row"]) for item in located])
