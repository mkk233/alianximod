# -*- coding: utf-8 -*-
import hashlib
import importlib.util
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
spec = importlib.util.spec_from_file_location("archive_write", HERE / "archive-write.py")
aw = importlib.util.module_from_spec(spec)
spec.loader.exec_module(aw)

data = LIVE.read_bytes()
print("SHA", hashlib.sha256(data).hexdigest().upper(), "BYTES", len(data))
with zipfile.ZipFile(LIVE) as archive:
    parts = {name: archive.read(name) for name in archive.namelist()}
shared = []
if "xl/sharedStrings.xml" in parts:
    root = ET.fromstring(parts["xl/sharedStrings.xml"])
    for item in root:
        if aw.local(item.tag) == "si":
            texts = []
            for node in item.iter():
                if aw.local(node.tag) == "t" and node.text:
                    texts.append(node.text)
            shared.append("".join(texts))
names = aw.sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
for title in ("已完成", "待制作", "制作中", "待验收"):
    page = aw.ids_of(parts[names[title]].decode("utf-8"), shared)
    print(title, [(code, number) for code, number, _row in page])
archive = aw.ids_of(parts[names["待制作原始数据"]].decode("utf-8"), shared)
print("ARCHIVE", len(archive), [code for code, _number, _row in archive])
detail = parts[names["资源状态明细"]].decode("utf-8")
print("DETAIL gloom", "fire_nether_gloom" in detail)
data_xml = parts[names["数据表"]].decode("utf-8")
rows = {aw.row_number(row): row for row in aw.rows_of(data_xml)}
for number in range(3, 12):
    row = rows.get(number)
    if not row:
        continue
    bits = []
    for column in ("AG", "AH", "AI", "AK", "AL", "AM"):
        value = aw.read_at(row, column, number, shared)
        if value:
            bits.append(column + "=" + value)
    if bits:
        print("DATA", number, " | ".join(bits))
making = {code: (number, row) for code, number, row in aw.ids_of(parts[names["制作中"]].decode("utf-8"), shared)}
accept = {code: (number, row) for code, number, row in aw.ids_of(parts[names["待验收"]].decode("utf-8"), shared)}
print("GLOOM making", "fire_nether_gloom" in making, "accept", "fire_nether_gloom" in accept)
if "fire_nether_gloom" in making:
    number, row = making["fire_nether_gloom"]
    for column in ("N", "O", "P", "Q", "S", "CR", "CS", "CT", "CU", "CX", "CY", "CZ", "EJ", "EK", "EM"):
        value = aw.read_at(row, column, number, shared)
        print(column, "len" if column in ("N", "O", "DV") else "", (len(value) if column in ("N", "O") else value))
    print("DV len", len(aw.read_at(row, "DV", number, shared) or ""))
for title in ("制作中", "待验收"):
    xml = parts[names[title]].decode("utf-8")
    merges = re_find = __import__("re").findall(r'<mergeCell\b[^>]*ref="[^"]+"', xml)
    print(title, "MERGES", merges)
