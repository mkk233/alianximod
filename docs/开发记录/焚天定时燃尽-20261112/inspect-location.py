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
archive = zipfile.ZipFile(live)
parts = {
    "已完成": "sheet1",
    "待制作": "sheet2",
    "制作中": "sheet3",
    "待验收": "sheet4",
    "资源状态明细": "sheet6",
    "待制作原始数据": "sheet7",
}


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def text_of(row, column, number):
    match = re.search(rf'<c r="{column}{number}"[\s\S]*?</c>', row)
    if not match:
        return None
    cell = match.group(0)
    texts = re.findall(r"<t(?: [^>]*)?>([\s\S]*?)</t>", cell)
    if texts:
        return html.unescape("".join(texts))
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", cell)
    return html.unescape(value.group(1)) if value else ""


result = {
    "sha256": hashlib.sha256(data).hexdigest().upper(),
    "mtime": datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="milliseconds"),
    "bytes": len(data),
    "counts": {},
    "hits": {},
}
for title, sheet in parts.items():
    xml = archive.read(f"xl/worksheets/{sheet}.xml").decode("utf-8")
    count = 0
    hits = []
    for row in rows_of(xml):
        number = row_number(row)
        code = text_of(row, "B", number)
        if number >= 3 and code and code.strip():
            count += 1
            if code.strip() == "fire_fentian":
                cells = {}
                for column in ("A", "B", "C", "D", "E", "F", "N", "O", "DV", "AS", "AP", "AN", "AO", "L", "K"):
                    cell_match = re.search(rf'<c r="{column}{number}"[\s\S]*?</c>', row)
                    cell = cell_match.group(0) if cell_match else ""
                    cells[column] = {
                        "text": text_of(row, column, number),
                        "head": cell[:160],
                    }
                hits.append({"row": number, "cells": cells, "formula": "<f" in row})
    result["counts"][title] = count
    result["hits"][title] = hits
out = Path(r"D:\alianximod\docs\开发记录\焚天定时燃尽-20261112\inspect-location.json")
out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
print(result["sha256"], result["bytes"], result["mtime"])
print(result["counts"])
for title, hits in result["hits"].items():
    print(title, [(item["row"], item["cells"]["A"]["text"], item["cells"]["E"]["text"]) for item in hits])
