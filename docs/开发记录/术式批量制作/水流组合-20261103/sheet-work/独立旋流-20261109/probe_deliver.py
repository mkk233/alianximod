# -*- coding: utf-8 -*-
import html
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")

def shared_items(xml):
    items = []
    for body in re.findall(r"<si\b[^>]*>(.*?)</si>", xml, re.S):
        parts = re.findall(r"<t\b[^>]*>(.*?)</t>", body, re.S)
        items.append(html.unescape("".join(parts)))
    return items

def rows_of(text):
    return {int(m.group(1)): m.group(0) for m in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text)}

def cell(row_xml, column, number, shared):
    match = re.search(rf'<c r="{column}{number}\b"[^>]*(?:/>|>[\s\S]*?</c>)', row_xml)
    if not match:
        return None
    cell_xml = match.group(0)
    if 't="inlineStr"' in cell_xml or "<is>" in cell_xml:
        return html.unescape("".join(re.findall(r"<t\b[^>]*>(.*?)</t>", cell_xml, re.S)))
    raw = re.search(r"<v>(.*?)</v>", cell_xml, re.S)
    if not raw:
        return ""
    if 't="s"' in cell_xml:
        return shared[int(raw.group(1))]
    return raw.group(1)

with zipfile.ZipFile(live) as book:
    stored = {name: book.read(name) for name in book.namelist()}
shared = shared_items(stored["xl/sharedStrings.xml"].decode("utf-8"))
wanted = {"water_vortex", "water_bubble_column", "water_deep_pressure", "tide_sever", "water_yielding_ribbon"}
for part, title in (
    ("xl/worksheets/sheet1.xml", "已完成"),
    ("xl/worksheets/sheet2.xml", "待制作"),
    ("xl/worksheets/sheet3.xml", "制作中"),
    ("xl/worksheets/sheet4.xml", "待验收"),
):
    text = stored[part].decode("utf-8")
    found = rows_of(text)
    dim = re.search(r'<dimension ref="([^"]+)"', text)
    ids = []
    hits = []
    for number, row_xml in found.items():
        if number < 3:
            continue
        code = cell(row_xml, "B", number, shared)
        if code:
            ids.append(code)
        if code in wanted:
            hits.append((number, code, cell(row_xml, "A", number, shared)))
    print(title, dim.group(1) if dim else None, "max", max(found), "count", len(ids), hits)
wb = stored["xl/workbook.xml"].decode("utf-8")
print("FILTERS", re.findall(r"(?:制作中|待验收)!\$A\$2:\$EN\$\d+", wb))
# vortex detail
detail = stored["xl/worksheets/sheet6.xml"].decode("utf-8")
print("detail rows", sorted(rows_of(detail)))
