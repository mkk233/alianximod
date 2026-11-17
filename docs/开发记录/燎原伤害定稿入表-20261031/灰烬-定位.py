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
with zipfile.ZipFile(live) as archive:
    header = cells(rows_of(archive.read("xl/worksheets/sheet2.xml").decode("utf-8"))[1])
    located = []
    counts = {}
    for name, title in status.items():
        xml = archive.read(name).decode("utf-8")
        page = []
        for row in rows_of(xml):
            number = row_number(row)
            if number < 3:
                continue
            info = cells(row)
            if info.get("B"):
                page.append(info["B"])
            if info.get("B") == "fire_liaoyuan" or info.get("A") == "燎原":
                located.append({"sheet": title, "row": number, "cells": info})
        counts[title] = len(page)
size_headers = {column: text for column, text in header.items() if text and any(word in text for word in ("半径", "宽", "直径", "范围", "延迟", "寿命", "周期"))}
report = {
    "sha256": sha256(data),
    "mtime": datetime.fromtimestamp(live.stat().st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": len(data),
    "counts": counts,
    "where": [(item["sheet"], item["row"]) for item in located],
    "sizeHeaders": size_headers,
}
if located:
    cells_now = located[0]["cells"]
    report["picked"] = {column: cells_now.get(column) for column in size_headers}
    report["picked"].update({
        "A": cells_now.get("A"), "B": cells_now.get("B"), "E": cells_now.get("E"),
        "AS": cells_now.get("AS"), "BV": cells_now.get("BV"), "BW": cells_now.get("BW"),
        "EC": cells_now.get("EC"), "EG": cells_now.get("EG"), "EH": cells_now.get("EH"),
        "EB": cells_now.get("EB"), "N": cells_now.get("N"),
        "hasAsh": "灰烬覆盖" in (cells_now.get("DV") or ""),
        "hasRadius6": "半径约6格" in (cells_now.get("DV") or ""),
        "ek": cells_now.get("EK"),
    })
Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031\灰烬-定位.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8"
)
print(json.dumps(report, ensure_ascii=False))
