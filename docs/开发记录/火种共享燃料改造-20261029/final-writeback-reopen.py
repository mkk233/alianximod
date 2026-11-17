# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\火种共享燃料改造-20261029")
summary = json.loads((batch / "final-writeback-preserve-summary.json").read_text(encoding="utf-8"))
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
live_sha = hashlib.sha256(live.read_bytes()).hexdigest().upper()
if live_sha != summary["candidateSha256"]:
    raise SystemExit(f"Live {live_sha} is not the candidate")


def rows_of(sheet_xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet_xml)


def cell_text(row_xml, column, number):
    match = re.search(rf'<(?:[\w]+:)?c r="{column}{number}"[^>]*>([\s\S]*?)</(?:[\w]+:)?c>', row_xml)
    if not match:
        return None
    inner = match.group(1)
    text = re.search(r"<(?:[\w]+:)?t\b[^>]*>([\s\S]*)</(?:[\w]+:)?t>", inner)
    if text:
        return html.unescape(text.group(1))
    value = re.search(r"<(?:[\w]+:)?v>([\s\S]*)</(?:[\w]+:)?v>", inner)
    return html.unescape(value.group(1)) if value else None


def ids_of(sheet_xml):
    found = []
    for row_xml in rows_of(sheet_xml):
        number = int(re.search(r'\br="(\d+)"', row_xml).group(1))
        if number < 3:
            continue
        code = cell_text(row_xml, "B", number)
        if code:
            found.append(code)
    return found


with zipfile.ZipFile(summary["backup"]) as old, zipfile.ZipFile(live) as new:
    different = [name for name in old.namelist() if old.read(name) != new.read(name)]
    counts = {}
    all_ids = []
    parts = {
        "已完成": "xl/worksheets/sheet1.xml",
        "待制作": "xl/worksheets/sheet2.xml",
        "制作中": "xl/worksheets/sheet3.xml",
        "待验收": "xl/worksheets/sheet4.xml",
    }
    for title, name in parts.items():
        codes = ids_of(new.read(name).decode("utf-8"))
        counts[title] = len(codes)
        all_ids.extend(codes)
    making = new.read(parts["制作中"]).decode("utf-8")
    pending = new.read(parts["待验收"]).decode("utf-8")
    data = new.read("xl/worksheets/sheet5.xml").decode("utf-8")
    making_rows = {int(re.search(r'\br="(\d+)"', row).group(1)): row for row in rows_of(making)}
    pending_rows = {int(re.search(r'\br="(\d+)"', row).group(1)): row for row in rows_of(pending)}
    check = {
        "sha256": live_sha,
        "different": different,
        "counts": counts,
        "unique": len(set(all_ids)) == 46 and len(all_ids) == 46,
        "making": [cell_text(making_rows[number], "B", number) for number in (3, 4, 5)],
        "pending": [cell_text(pending_rows[number], "B", number) for number in (3, 4, 5)],
        "triggers": [
            cell_text(pending_rows[3], "EB", 3),
            cell_text(pending_rows[4], "EB", 4),
            cell_text(pending_rows[5], "EB", 5),
        ],
        "periods": [cell_text(pending_rows[3], "EG", 3), cell_text(pending_rows[5], "EG", 5)],
        "option": "燃烧结算" in data,
    }
print(json.dumps(check, ensure_ascii=False))
if different != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml", "xl/worksheets/sheet5.xml"]:
    raise SystemExit("parts")
if not check["unique"] or check["making"] != ["fire_heat_up", "fire_burn", "fire_cool"]:
    raise SystemExit("making")
if check["pending"] != ["fire_liaoyuan", "fire_liaoli", "fire_fentian"]:
    raise SystemExit("pending")
if check["triggers"] != ["燃烧结算", "命中成功", "燃烧结算"] or check["periods"] != ["20", "4"]:
    raise SystemExit("fields")
