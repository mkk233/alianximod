# -*- coding: utf-8 -*-
import hashlib
import importlib.util
import json
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
spec = importlib.util.spec_from_file_location("archive_write", HERE / "archive-write.py")
aw = importlib.util.module_from_spec(spec)
spec.loader.exec_module(aw)
block = importlib.util.spec_from_file_location("blocked", HERE / "blocked-compile-write.py")
bw = importlib.util.module_from_spec(block)
block.loader.exec_module(bw)

live = LIVE.read_bytes()
with zipfile.ZipFile(LIVE) as archive:
    parts = {name: archive.read(name) for name in archive.namelist()}
shared = bw.load_shared(parts)
names = aw.sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
counts, unique, pages, archive_ids = bw.status_counts(parts, names, shared)
rows = {}
for code, number, row in pages["制作中"]:
    rows[code] = {
        "row": number,
        "N": len(aw.read_at(row, "N", number, shared) or ""),
        "O": len(aw.read_at(row, "O", number, shared) or ""),
        "marker": bw.MARKER in (aw.read_at(row, "DV", number, shared) or ""),
        "gloom": "不改色" in (aw.read_at(row, "DV", number, shared) or ""),
    }
print(json.dumps({
    "sha": hashlib.sha256(live).hexdigest().upper(),
    "bytes": len(live),
    "counts": counts,
    "unique": unique,
    "archive": len(archive_ids),
    "making": [code for code, _number, _row in pages["制作中"]],
    "accept": [code for code, _number, _row in pages["待验收"]],
    "rows": rows,
}, ensure_ascii=False, indent=2))
