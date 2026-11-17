# -*- coding: utf-8 -*-
"""Check the existing 燎原 candidate against the live workbook. Does not write the workbook."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "术式批量制作" / "火系六式-20261029"
live = root / "docs" / "术式制作表.xlsx"
candidate = batch / "liaoyuan-visual-writeback-candidate-preserved.xlsx"
snapshot = json.loads((batch / "liaoyuan-visual-transition-snapshot.json").read_text(encoding="utf-8"))
expected_live = "FEFBC4B30087C08C6E8B9B6C482FEEEBC8DB6F6F3E65F6B88117E4D3D630C070"
expected_candidate = "FF2D4227625B4C1E19ADD49BC34BFCFD6B9CA98F0C8D221FF6AA98AB0B75E0E8"
pending_ids = [
    "fire_flame_flow", "fire_scorch_field", "fire_meteorite", "fire_shock_ring",
    "fire_surface_anchor", "fire_upwell", "fire_stagger", "fire_hit_continue",
    "fire_seed_sow", "fire_kindle", "fire_detonate", "fire_nether_gloom", "fire_break_ground",
]


def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def text_of(row, column, number):
    match = re.search(rf'<c r="{column}{number}"[^>]*>([\s\S]*?)</c>', row)
    if not match:
        return None
    inner = match.group(1)
    inline = re.search(r"<t(?: [^>]*)?>([\s\S]*)</t>", inner)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", inner)
    return html.unescape(value.group(1)) if value else None


def sheet_ids(xml):
    found = []
    for row in rows_of(xml):
        number = int(re.search(r'\br="(\d+)"', row).group(1))
        if number >= 3:
            code = text_of(row, "B", number)
            if code:
                found.append((number, code, text_of(row, "A", number)))
    return found


candidate_bytes = candidate.read_bytes()
candidate_hash = sha256(candidate_bytes)
if candidate_hash != expected_candidate:
    raise SystemExit(f"Candidate hash {candidate_hash}")
live_bytes = live.read_bytes()
live_stat = live.stat()
live_hash = sha256(live_bytes)
if live_hash != expected_live:
    raise SystemExit(f"Live hash changed: {live_hash}")
with zipfile.ZipFile(live) as src, zipfile.ZipFile(candidate) as dst:
    if src.namelist() != dst.namelist():
        raise SystemExit("Zip part list differs")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"]:
        raise SystemExit(f"Unexpected parts: {different}")
    pending = dst.read("xl/worksheets/sheet4.xml").decode("utf-8")
    making = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")
    queued = dst.read("xl/worksheets/sheet2.xml").decode("utf-8")
    done = dst.read("xl/worksheets/sheet1.xml").decode("utf-8")
    counts = {}
    ids = []
    located = {}
    for title, xml in (("已完成", done), ("待制作", queued), ("制作中", making), ("待验收", pending)):
        page = sheet_ids(xml)
        counts[title] = len(page)
        for number, code, name in page:
            ids.append(code)
            located.setdefault(code, []).append((title, number, name))
    if counts != {"已完成": 40, "待制作": 13, "制作中": 3, "待验收": 3}:
        raise SystemExit(f"Counts {counts}")
    if len(ids) != 59 or len(set(ids)) != 59:
        raise SystemExit("Ids are not 59 unique")
    if located["fire_liaoyuan"] != [("待验收", 5, "燎原")]:
        raise SystemExit(f"燎原 at {located['fire_liaoyuan']}")
    if "fire_liaoyuan" in making:
        raise SystemExit("制作中 still contains 燎原")
    for number, code in ((3, "fire_heat_up"), (4, "fire_burn"), (5, "fire_cool")):
        if located[code] != [("制作中", number, {"fire_heat_up": "升温", "fire_burn": "燃烧", "fire_cool": "冷却"}[code])]:
            raise SystemExit(f"{code} at {located[code]}")
    if located["fire_liaoli"][0][0:2] != ("待验收", 3) or located["fire_fentian"][0][0:2] != ("待验收", 4):
        raise SystemExit("燎里或焚天位置变了")
    queued_ids = [code for number, code, name in sheet_ids(queued)]
    if queued_ids != pending_ids:
        raise SystemExit(f"待制作 ids {queued_ids}")
    row = [item for item in rows_of(pending) if 'r="5"' in item[:80]][0]
    if text_of(row, "N", 5) != snapshot["conversion"]["nAfter"]:
        raise SystemExit("N does not match the saved snapshot")
    if text_of(row, "O", 5) != snapshot["conversion"]["oAfter"]:
        raise SystemExit("O does not match the saved snapshot")
    remark = text_of(row, "DV", 5)
    if not remark.startswith(snapshot["currentDetailed"]["dv"]):
        raise SystemExit("DV prefix lost")
    if "两项视觉问题已由原作者交回并停止写入" not in remark:
        raise SystemExit("Repair note missing")
    if "本阶段额度已用完，返修结果未追加复审" not in remark:
        raise SystemExit("Quota note missing")
    if "fire_liaoyuan" in dst.read("xl/worksheets/sheet7.xml").decode("utf-8"):
        archive_same = True
    else:
        archive_same = False
    if src.read("xl/worksheets/sheet6.xml") != dst.read("xl/worksheets/sheet6.xml"):
        raise SystemExit("Resource detail changed")
    if src.read("xl/worksheets/sheet7.xml") != dst.read("xl/worksheets/sheet7.xml"):
        raise SystemExit("Archive changed")
print(json.dumps({
    "ready": True,
    "liveSha256": live_hash,
    "liveMtime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "liveBytes": len(live_bytes),
    "candidateSha256": candidate_hash,
    "different": different,
    "counts": counts,
    "unique": 59,
    "archiveStillHasSameId": archive_same,
}, ensure_ascii=False))
