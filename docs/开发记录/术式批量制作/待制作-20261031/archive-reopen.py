# -*- coding: utf-8 -*-
import hashlib
import json
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
BACKUP = HERE / "备份" / "术式制作表-archive-submit-20261031-1.xlsx"
# Reuse the writer's helpers by import.
import importlib.util
spec = importlib.util.spec_from_file_location("archive_write", HERE / "archive-write.py")
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

live = LIVE.read_bytes()
locked = BACKUP.read_bytes()
with zipfile.ZipFile(LIVE) as archive:
    parts = {name: archive.read(name) for name in archive.namelist()}
shared = []
if "xl/sharedStrings.xml" in parts:
    root = ET.fromstring(parts["xl/sharedStrings.xml"])
    for item in root:
        if mod.local(item.tag) == "si":
            texts = []
            for node in item.iter():
                if mod.local(node.tag) == "t" and node.text:
                    texts.append(node.text)
            shared.append("".join(texts))
names = mod.sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
counts = {}
all_ids = []
making = None
for title in ("已完成", "待制作", "制作中", "待验收"):
    page = mod.ids_of(parts[names[title]].decode("utf-8"), shared)
    counts[title] = len(page)
    all_ids.extend(code for code, _number, _row in page)
    if title == "制作中":
        making = page
data = parts[names["数据表"]].decode("utf-8")
data_rows = {mod.row_number(row): row for row in mod.rows_of(data)}
enums = {
    "A9": mod.read_at(data_rows[9], "A", 9, shared),
    "B9": mod.read_at(data_rows[9], "B", 9, shared),
    "A10": mod.read_at(data_rows[10], "A", 10, shared),
    "B10": mod.read_at(data_rows[10], "B", 10, shared),
    "E13": mod.read_at(data_rows[13], "E", 13, shared),
    "F13": mod.read_at(data_rows[13], "F", 13, shared),
    "E14": mod.read_at(data_rows[14], "E", 14, shared),
    "E15": mod.read_at(data_rows[15], "E", 15, shared),
    "E16": mod.read_at(data_rows[16], "E", 16, shared),
    "F16": mod.read_at(data_rows[16], "F", 16, shared),
    "A4": mod.read_at(data_rows[4], "A", 4, shared),
    "E12": mod.read_at(data_rows[12], "E", 12, shared),
}
sample = {}
for code, number, row in making:
    if code not in mod.IDS:
        continue
    sample[code] = {
        "row": number,
        "P": mod.read_at(row, "P", number, shared),
        "R": mod.read_at(row, "R", number, shared),
        "S": mod.read_at(row, "S", number, shared),
        "L": mod.read_at(row, "L", number, shared),
        "F": mod.read_at(row, "F", number, shared),
        "H": mod.read_at(row, "H", number, shared),
        "K": mod.read_at(row, "K", number, shared),
        "G": mod.read_at(row, "G", number, shared),
        "E": mod.read_at(row, "E", number, shared),
        "Nlen": len(mod.read_at(row, "N", number, shared) or ""),
        "Olen": len(mod.read_at(row, "O", number, shared) or ""),
        "dvHasMarker": mod.MARKER in (mod.read_at(row, "DV", number, shared) or ""),
    }
workbook = parts["xl/workbook.xml"].decode("utf-8")
print(json.dumps({
    "liveSha": hashlib.sha256(live).hexdigest().upper(),
    "lockSha": hashlib.sha256(locked).hexdigest().upper(),
    "bytes": len(live),
    "counts": counts,
    "unique": len(set(all_ids)),
    "makingIds": [code for code, _number, _row in making],
    "enums": enums,
    "namesOk": "数据表!$A$4:$A$10" in workbook and "数据表!$E$4:$E$16" in workbook,
    "sample": sample,
}, ensure_ascii=False, indent=2))
