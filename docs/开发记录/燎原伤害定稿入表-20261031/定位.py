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
        if text is not None:
            found[match.group(1)] = text
    return found


status = {
    "xl/worksheets/sheet1.xml": "已完成",
    "xl/worksheets/sheet2.xml": "待制作",
    "xl/worksheets/sheet3.xml": "制作中",
    "xl/worksheets/sheet4.xml": "待验收",
}
report = {
    "sha256": sha256(data),
    "mtime": datetime.fromtimestamp(live.stat().st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": len(data),
}
located = []
counts = {}
with zipfile.ZipFile(live) as archive:
    sheet2 = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
    header = cells(rows_of(sheet2)[1])
    for name, title in status.items():
        xml = archive.read(name).decode("utf-8")
        page = []
        for row in rows_of(xml):
            number = row_number(row)
            if number < 3:
                continue
            info = cells(row)
            code = info.get("B")
            if code:
                page.append(code)
            if code == "fire_liaoyuan" or info.get("A") == "燎原":
                located.append({"sheet": title, "row": number, "cells": info})
        counts[title] = len(page)
    detail = []
    sheet6 = archive.read("xl/worksheets/sheet6.xml").decode("utf-8")
    for row in rows_of(sheet6):
        info = cells(row)
        blob = " ".join(info.values())
        if "燎原" in blob or "fire_liaoyuan" in blob or row_number(row) <= 2:
            detail.append({"row": row_number(row), "cells": info})
report["counts"] = counts
report["located"] = located
report["detail"] = detail
report["headers"] = header
out = Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031\定位.json")
out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "sha": report["sha256"],
    "counts": counts,
    "where": [(item["sheet"], item["row"], item["cells"].get("A"), item["cells"].get("B")) for item in located],
    "detailRows": [item["row"] for item in detail],
}, ensure_ascii=False))
