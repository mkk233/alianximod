# -*- coding: utf-8 -*-
import hashlib
import json
import zipfile
from pathlib import Path

import importlib.util

WORK = Path(r"D:\alianximod\docs\开发记录\燃烧空间挂载接入-20261101\第一人称空手返修-20261109\表格")
spec = importlib.util.spec_from_file_location("move_fire_burn", WORK / "move-fire-burn.py")
move = importlib.util.module_from_spec(spec)
spec.loader.exec_module(move)
prep = json.loads((WORK / "move-prepare-result.json").read_text(encoding="utf-8"))
nodv = json.loads(Path(prep["snapshots"]["nodv"]).read_text(encoding="utf-8"))
live = Path(prep["live"]).read_bytes()
backup = Path(prep["backup"]).read_bytes()
candidate = Path(prep["candidate"]).read_bytes()
live_sha = hashlib.sha256(live).hexdigest().upper()
print("live", live_sha)
print("backup", hashlib.sha256(backup).hexdigest().upper())
print("candidate", hashlib.sha256(candidate).hexdigest().upper())
print("live_eq_candidate", live == candidate)
print("backup_eq_baseline", hashlib.sha256(backup).hexdigest().upper() == prep["baselineSha256"])

def load(payload):
    book = zipfile.ZipFile(__import__("io").BytesIO(payload))
    parts = {name: book.read(name) for name in book.namelist()}
    return parts

live_parts = load(live)
backup_parts = load(backup)
names = move.sheet_map(live_parts)
shared = move.shared_list(live_parts["xl/sharedStrings.xml"])
counts = {}
located = {}
for title in move.STATUS:
    page = move.ids_of(live_parts[names[title]].decode("utf-8"), shared)
    counts[title] = len(page)
    for code, number, _row in page:
        located.setdefault(code, []).append((title, number))
print("counts", counts)
print("fire", located.get("fire_burn"))
print("unique", len(located), "dupes", [key for key, value in located.items() if len(value) > 1])
row = [item for item in move.ids_of(live_parts[names["制作中"]].decode("utf-8"), shared) if item[0] == "fire_burn"][0][2]
print("N_ok", move.read_at(row, "N", 19, shared) == nodv["restoredN"])
print("O_ok", move.read_at(row, "O", 19, shared) == nodv["restoredO"])
print("DV_ok", move.read_at(row, "DV", 19, shared) == nodv["restoredDV"])
print("pending_has", "fire_burn" in [code for code, _n, _r in move.ids_of(live_parts[names["待验收"]].decode("utf-8"), shared)])
archive = move.ids_of(live_parts[names["待制作原始数据"]].decode("utf-8"), shared)
print("archive_rows", [(code, number) for code, number, _row in archive if code == "fire_burn"])
changed = [name for name in live_parts if live_parts[name] != backup_parts.get(name)]
print("changed_parts", changed)
print("strings_same", live_parts["xl/sharedStrings.xml"] == backup_parts["xl/sharedStrings.xml"])
print("detail_same", live_parts[names["资源状态明细"]] == backup_parts[names["资源状态明细"]])
print("archive_same", live_parts[names["待制作原始数据"]] == backup_parts[names["待制作原始数据"]])
print("styles_same", live_parts.get("xl/styles.xml") == backup_parts.get("xl/styles.xml"))
print("comments_same", all(live_parts[name] == backup_parts[name] for name in live_parts if "comment" in name.lower() or name.lower().endswith(".vml")))
