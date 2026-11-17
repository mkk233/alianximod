# -*- coding: utf-8 -*-
import hashlib
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
CANDIDATE = Path(__file__).with_name("候选") / "术式制作表-queue-values-20261111.xlsx"
BASELINE = "59640cbc39cc00ecfc41c07ea5bcffecc1ead7622252432e3f6dd510a1f6c23f"
TARGETS = [
    "fire_pick_lamp", "trajectory_parallel", "water_wave", "water_disperse",
    "sword_guard", "sword_array", "phantom_sword_surge", "sword_flight",
]
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def package(path):
    with zipfile.ZipFile(path) as book:
        return {name: book.read(name) for name in book.namelist()}


def sheets(data):
    rels = ET.fromstring(data["xl/_rels/workbook.xml.rels"])
    targets = {}
    for node in rels:
        target = "xl/" + node.attrib["Target"].lstrip("/")
        targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
    work = ET.fromstring(data["xl/workbook.xml"])
    return {node.attrib["name"]: targets[node.attrib[REL]] for node in work.find("s:sheets", NS)}


def rows(text):
    return {int(m.group(1)): m.group(0) for m in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text)}


def cells(row):
    found = {}
    for match in re.finditer(r'<c\b[^>]*r="([A-Z]+)\d+"[^>]*(?:/>|>[\s\S]*?</c>)', row):
        block = match.group(0)
        value = ""
        if "</c>" in block:
            inner = re.search(r">(.*)</c>", block, re.S).group(1)
            value = "".join(ET.fromstring("<root>" + inner + "</root>").itertext())
        found[match.group(1)] = value
    return found


def ids(text):
    found = []
    for number, row in sorted(rows(text).items()):
        if number < 3:
            continue
        value = cells(row).get("B", "").strip()
        if value:
            found.append((number, value))
    return found


base = package(LIVE)
cand = package(CANDIDATE)
base_sheets = sheets(base)
cand_sheets = sheets(cand)
changed = sorted(name for name in base if base[name] != cand.get(name))
names = ["已完成", "待制作", "制作中", "待验收", "待制作原始数据", "资源状态明细", "数据表"]
counts = {}
for name in names:
    if name == "数据表":
        continue
    counts[name] = len(ids(cand[cand_sheets[name]].decode("utf-8")))

base_pending = rows(base[base_sheets["待制作"]].decode("utf-8"))
cand_pending = rows(cand[cand_sheets["待制作"]].decode("utf-8"))
base_archive = rows(base[base_sheets["待制作原始数据"]].decode("utf-8"))
cand_archive = rows(cand[cand_sheets["待制作原始数据"]].decode("utf-8"))
checks = []
focus = ["A", "B", "E", "F", "G", "H", "I", "K", "L", "N", "O", "R", "S", "V", "DV", "EC", "EJ", "EK"]
for stable in TARGETS:
    source = next(number for number, row in base_pending.items() if cells(row).get("B") == stable)
    original = cells(base_pending[source])
    current = cells(cand_pending[source])
    archive_row = next(number for number, row in cand_archive.items() if cells(row).get("B") == stable)
    archived = cells(cand_archive[archive_row])
    same = {col: archived.get(col) == original.get(col) for col in focus}
    checks.append({
        "id": stable,
        "pendingRow": source,
        "archiveRow": archive_row,
        "archiveMatchesOriginal": all(same.values()),
        "archiveDiffs": [col for col, ok in same.items() if not ok],
        "current": {col: current.get(col) for col in focus},
        "meta": {col: archived.get(col) for col in ("EP", "EQ", "ER", "ES", "ET", "EU", "EV", "EX")},
        "stillPending": current.get("B") == stable,
    })

lookup_rows = rows(cand[cand_sheets["数据表"]].decode("utf-8"))
workbook = cand["xl/workbook.xml"].decode("utf-8")
report = {
    "liveHash": digest(LIVE),
    "candidateHash": digest(CANDIDATE),
    "baselineUnchanged": digest(LIVE) == BASELINE,
    "changedParts": changed,
    "unchangedPartCount": sum(1 for name in base if base[name] == cand.get(name)),
    "counts": counts,
    "archiveOldRowsUnchanged": all(base_archive[number] == cand_archive[number] for number in range(1, 34)),
    "named": {
        "trajectory": "数据表!$A$4:$A$11" in workbook,
        "role": "数据表!$E$4:$E$17" in workbook,
        "oldTrajectoryGone": "数据表!$A$4:$A$10" not in workbook,
        "oldRoleGone": "数据表!$E$4:$E$16" not in workbook,
    },
    "lookup": {
        "11": {col: cells(lookup_rows[11]).get(col) for col in ("A", "B", "C")},
        "17": {col: cells(lookup_rows[17]).get(col) for col in ("E", "F", "G")},
        "10A": cells(lookup_rows[10]).get("A"),
        "16E": cells(lookup_rows[16]).get("E"),
    },
    "checks": checks,
    "resourceUnchanged": base[base_sheets["资源状态明细"]] == cand[cand_sheets["资源状态明细"]],
    "completedUnchanged": base[base_sheets["已完成"]] == cand[cand_sheets["已完成"]],
    "makingUnchanged": base[base_sheets["制作中"]] == cand[cand_sheets["制作中"]],
    "acceptUnchanged": base[base_sheets["待验收"]] == cand[cand_sheets["待验收"]],
}
out = Path(__file__).with_name("verify.json")
out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"counts": counts, "changed": changed, "archiveOk": report["archiveOldRowsUnchanged"], "allMatch": all(item["archiveMatchesOriginal"] for item in checks)}, ensure_ascii=False))
