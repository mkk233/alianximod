# -*- coding: utf-8 -*-
import hashlib
import json
from datetime import datetime
from pathlib import Path
import importlib.util

batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029")
spec = importlib.util.spec_from_file_location("check", batch / "liaoyuan-visual-writeback-resume-check.py")
# Reuse parsers by executing the same comparisons with the roles swapped:
# live is now the submitted workbook; backup is the pre-image.
import html
import re
import zipfile

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
backup = Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029\备份\liaoyuan-visual-writeback-resume-术式制作表-20261029-233150-1.xlsx")
snapshot = json.loads((batch / "liaoyuan-visual-transition-snapshot.json").read_text(encoding="utf-8"))
expected = "FF2D4227625B4C1E19ADD49BC34BFCFD6B9CA98F0C8D221FF6AA98AB0B75E0E8"
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
                found.append((number, code))
    return found


data = live.read_bytes()
stat = live.stat()
live_hash = sha256(data)
if live_hash != expected:
    raise SystemExit(f"Reopen hash {live_hash}")
if sha256(backup.read_bytes()) != "FEFBC4B30087C08C6E8B9B6C482FEEEBC8DB6F6F3E65F6B88117E4D3D630C070":
    raise SystemExit("Backup is not the baseline")
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(live) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"]:
        raise SystemExit(f"Unexpected parts: {different}")
    counts = {}
    ids = []
    located = {}
    for title, part in (
        ("已完成", "xl/worksheets/sheet1.xml"),
        ("待制作", "xl/worksheets/sheet2.xml"),
        ("制作中", "xl/worksheets/sheet3.xml"),
        ("待验收", "xl/worksheets/sheet4.xml"),
    ):
        page = sheet_ids(dst.read(part).decode("utf-8"))
        counts[title] = len(page)
        for number, code in page:
            ids.append(code)
            located.setdefault(code, []).append((title, number))
    making = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")
    pending = dst.read("xl/worksheets/sheet4.xml").decode("utf-8")
    queued = [code for number, code in sheet_ids(dst.read("xl/worksheets/sheet2.xml").decode("utf-8"))]
    row = [item for item in rows_of(pending) if re.search(r'<row r="5"', item)][0]
    remark = text_of(row, "DV", 5)
    checks = {
        "counts": counts,
        "unique": len(set(ids)),
        "total": len(ids),
        "duplicates": sorted(code for code in set(ids) if ids.count(code) > 1),
        "liaoyuan": located.get("fire_liaoyuan"),
        "makingHasLiaoyuan": "fire_liaoyuan" in making,
        "queued": queued == pending_ids,
        "n": text_of(row, "N", 5) == snapshot["conversion"]["nAfter"],
        "o": text_of(row, "O", 5) == snapshot["conversion"]["oAfter"],
        "dvPrefix": remark.startswith(snapshot["currentDetailed"]["dv"]),
        "dvRepair": "两项视觉问题已由原作者交回并停止写入" in remark,
        "dvQuota": "本阶段额度已用完，返修结果未追加复审" in remark,
        "furnace": [located[code] for code in ("fire_heat_up", "fire_burn", "fire_cool")],
        "others": [located[code] for code in ("fire_liaoli", "fire_fentian")],
        "different": different,
    }
if checks["counts"] != {"已完成": 40, "待制作": 13, "制作中": 3, "待验收": 3}:
    raise SystemExit(checks)
if checks["unique"] != 59 or checks["total"] != 59 or checks["duplicates"] or checks["makingHasLiaoyuan"]:
    raise SystemExit(checks)
if not all((checks["queued"], checks["n"], checks["o"], checks["dvPrefix"], checks["dvRepair"], checks["dvQuota"])):
    raise SystemExit(checks)
print(json.dumps({
    "liveSha256": live_hash,
    "liveMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "liveBytes": len(data),
    "checks": checks,
}, ensure_ascii=False))
