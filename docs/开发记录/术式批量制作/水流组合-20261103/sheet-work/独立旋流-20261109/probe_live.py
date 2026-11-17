# -*- coding: utf-8 -*-
import hashlib, html, json, re, zipfile
from datetime import datetime
from pathlib import Path
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
payload = live.read_bytes()
print("HASH", hashlib.sha256(payload).hexdigest().upper())
print("MTIME", datetime.fromtimestamp(live.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3])
print("SIZE", len(payload))
snap = json.loads(Path(r"D:\alianximod\docs\开发记录\术式批量制作\水流组合-20261103\sheet-work\实测三项-20261104待验收快照.json").read_text(encoding="utf-8"))
row = next(item for item in snap["rows"] if item["id"] == "water_vortex")
print("SNAP_FROM", row["fromRow"], "TO", row["toRow"])
print("BEFORE_N_HEAD", row["before"][13][:40])
print("BEFORE_O_TAIL", row["before"][14][-80:])
print("AFTER_N", row["after"][13])
print("AFTER_O", row["after"][14][:80])
print("AFTER_DV_TAIL", row["after"][125][-120:])

def shared_items(xml):
    items = []
    for body in re.findall(r"<si\b[^>]*>(.*?)</si>", xml, re.S):
        parts = re.findall(r"<t\b[^>]*>(.*?)</t>", body, re.S)
        items.append(html.unescape("".join(parts)))
    return items

def rows_of(xml):
    return {int(m.group(1)): m.group(0) for m in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', xml)}

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
names = {"sheet1": "已完成", "sheet2": "待制作", "sheet3": "制作中", "sheet4": "待验收", "sheet6": "资源状态明细"}
for part, title in (("xl/worksheets/sheet1.xml", "已完成"), ("xl/worksheets/sheet2.xml", "待制作"), ("xl/worksheets/sheet3.xml", "制作中"), ("xl/worksheets/sheet4.xml", "待验收")):
    found = rows_of(stored[part].decode("utf-8"))
    dim = re.search(r'<dimension ref="([^"]+)"', stored[part].decode("utf-8"))
    hits = []
    for number, row_xml in found.items():
        if number < 3:
            continue
        code = cell(row_xml, "B", number, shared)
        if code in {"water_vortex", "water_bubble_column", "water_deep_pressure", "tide_sever", "water_yielding_ribbon"}:
            hits.append((number, code, cell(row_xml, "A", number, shared)))
    print("PAGE", title, dim.group(1) if dim else None, "max", max(found), hits)
wb = stored["xl/workbook.xml"].decode("utf-8")
print("FILTERS", re.findall(r"制作中!\$A\$2:\$EN\$\d+|待验收!\$A\$2:\$EN\$\d+", wb))
detail = stored["xl/worksheets/sheet6.xml"].decode("utf-8")
print("DETAIL_VORTEX_LITERAL", "water_vortex" in detail)
