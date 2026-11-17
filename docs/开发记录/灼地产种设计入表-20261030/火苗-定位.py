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
        found[match.group(1)] = text
    return found


status = {
    "xl/worksheets/sheet1.xml": "已完成",
    "xl/worksheets/sheet2.xml": "待制作",
    "xl/worksheets/sheet3.xml": "制作中",
    "xl/worksheets/sheet4.xml": "待验收",
}
found = []
counts = {}
with zipfile.ZipFile(live) as archive:
    sheet2 = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
    header_row = rows_of(sheet2)[1]
    header = cells(header_row)
    ignite_headers = {column: text for column, text in header.items() if text and any(word in text for word in ("燃", "点燃", "着火", "火", "飞弹", "弹"))}
    dimension = re.search(r'ref="([^"]+)"', sheet2).group(1)
    validations = []
    for match in re.finditer(r'<dataValidation\b([^>]*)>', sheet2):
        attrs = match.group(1)
        sqref = re.search(r'sqref="([^"]+)"', attrs)
        if sqref and re.search(r"(\d+)", sqref.group(1)):
            validations.append(sqref.group(1))
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
            blob = " ".join(value or "" for value in info.values())
            if code in ("fire_spark", "fire_orb", "magic_ignite", "magic_arcane_missile") or "火苗" in blob or (info.get("A") in ("火球", "点火", "奥术飞弹")):
                found.append({"sheet": title, "row": number, "cells": {column: info[column] for column in info if info[column]}})
        counts[title] = len(page)
report = {
    "sha256": sha256(data),
    "mtime": datetime.fromtimestamp(live.stat().st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": len(data),
    "counts": counts,
    "dimension": dimension,
    "validationSample": validations[:8],
    "validationCount": len(validations),
    "igniteHeaders": ignite_headers,
    "found": [{key: item[key] for key in ("sheet", "row")} | {"A": item["cells"].get("A"), "B": item["cells"].get("B")} for item in found],
}
Path(r"D:\alianximod\docs\开发记录\灼地产种设计入表-20261030\火苗-定位.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8"
)
print(json.dumps({k: report[k] for k in ("sha256", "counts", "dimension", "validationCount", "found")}, ensure_ascii=False))
print("HEADERS", json.dumps(ignite_headers, ensure_ascii=False))
