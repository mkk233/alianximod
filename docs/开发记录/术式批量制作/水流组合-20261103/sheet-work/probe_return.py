# -*- coding: utf-8 -*-
import hashlib
import html
import re
import zipfile
from datetime import datetime
from pathlib import Path

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
payload = LIVE.read_bytes()
print("HASH", hashlib.sha256(payload).hexdigest().upper())
print("SIZE", len(payload))
print("MTIME", datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3])
with zipfile.ZipFile(LIVE) as book:
    stored = {name: book.read(name) for name in book.namelist()}

def shared_items(xml):
    items = []
    for body in re.findall(r"<si\b[^>]*>(.*?)</si>", xml, re.S):
        parts = re.findall(r"<t\b[^>]*>(.*?)</t>", body, re.S)
        items.append(html.unescape("".join(parts)))
    return items

def row_map(xml):
    found = {}
    for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', xml):
        found[int(match.group(1))] = match.group(0)
    return found

def cell_value(row, column, number, shared):
    match = re.search(rf'<c r="{column}{number}\b"[^>]*(?:/>|>[\s\S]*?</c>)', row)
    if not match:
        return None
    cell = match.group(0)
    if cell.endswith("/>"):
        return ""
    if 't="inlineStr"' in cell or "<is>" in cell:
        return html.unescape("".join(re.findall(r"<t\b[^>]*>(.*?)</t>", cell, re.S)))
    raw = re.search(r"<v>(.*?)</v>", cell, re.S)
    if not raw:
        return ""
    if 't="s"' in cell:
        return shared[int(raw.group(1))]
    return raw.group(1)

shared = shared_items(stored["xl/sharedStrings.xml"].decode("utf-8"))
names = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
    "资源状态明细": "xl/worksheets/sheet6.xml",
}
targets = {"water_vortex", "water_bubble_column", "water_deep_pressure", "tide_sever", "water_yielding_ribbon"}
for title, part in names.items():
    if title == "资源状态明细":
        continue
    xml = stored[part].decode("utf-8")
    dim = re.search(r'<dimension ref="([^"]+)"', xml)
    print("DIM", title, dim.group(1) if dim else None, "rows", sorted(row_map(xml)))
    for number, row in row_map(xml).items():
        if number < 3:
            continue
        code = cell_value(row, "B", number, shared)
        if code in targets:
            print("HIT", title, number, code, cell_value(row, "A", number, shared))
wb = stored["xl/workbook.xml"].decode("utf-8")
for token in ("制作中!$A$2:$EN$17", "待验收!$A$2:$EN$2", "制作中!$A$2:$EN$"):
    print("FILTER", token, token in wb)
detail = stored["xl/worksheets/sheet6.xml"].decode("utf-8")
print("DETAIL_HITS", detail.count("water_deep_pressure"))
