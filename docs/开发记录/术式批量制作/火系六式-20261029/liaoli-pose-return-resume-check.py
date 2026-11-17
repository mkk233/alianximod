# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029")
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
candidate = batch / "liaoli-pose-return-candidate-preserved.xlsx"
intake = json.loads((batch / "liaoli-pose-intake.json").read_text(encoding="utf-8"))
expected_live = "0D72B6D07C7E604943AC8DC71AA6EEB783F686EC0DAE1CEB1F69A13ED30FA6E7"
expected_candidate = "69FD9B9BCC47885D0449CB135F84523894F48962745AA41BAED4FD1F05400C02"


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


def ids_of(xml):
    found = []
    for row in rows_of(xml):
        number = int(re.search(r'\br="(\d+)"', row).group(1))
        if number >= 3:
            code = text_of(row, "B", number)
            if code:
                found.append((number, code))
    return found


live_bytes = live.read_bytes()
live_stat = live.stat()
live_hash = sha256(live_bytes)
candidate_hash = sha256(candidate.read_bytes())
if live_hash != expected_live:
    raise SystemExit(f"Live hash changed: {live_hash}")
if candidate_hash != expected_candidate:
    raise SystemExit(f"Candidate hash changed: {candidate_hash}")
with zipfile.ZipFile(live) as src, zipfile.ZipFile(candidate) as dst:
    if src.namelist() != dst.namelist():
        raise SystemExit("Part list differs")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"]:
        raise SystemExit(f"Unexpected parts: {different}")
    if src.read("xl/worksheets/sheet6.xml") != dst.read("xl/worksheets/sheet6.xml"):
        raise SystemExit("Resource detail changed")
    if src.read("xl/worksheets/sheet7.xml") != dst.read("xl/worksheets/sheet7.xml"):
        raise SystemExit("Archive changed")
    making = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")
    pending = dst.read("xl/worksheets/sheet4.xml").decode("utf-8")
    live_pending = src.read("xl/worksheets/sheet4.xml").decode("utf-8")
    row = next(item for item in rows_of(making) if re.search(r'<row r="6"', item))
    if text_of(row, "B", 6) != "fire_liaoli":
        raise SystemExit("Candidate row 6 is not 燎里")
    if text_of(row, "N", 6) != intake["restored"]["n"] or text_of(row, "O", 6) != intake["restored"]["o"]:
        raise SystemExit("Restored N/O drifted")
    if not text_of(row, "DV", 6).startswith(intake["currentDisplay"]["dv"]):
        raise SystemExit("DV prefix lost")
    if "fire_liaoli" in pending:
        raise SystemExit("Candidate 待验收 still has 燎里")
    live_ids = []
    for part in ("xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"):
        live_ids.extend(code for _number, code in ids_of(src.read(part).decode("utf-8")))
    if live_ids.count("fire_liaoli") != 1:
        raise SystemExit("Live 燎里 is not unique")
    live_row = next(item for item in rows_of(live_pending) if text_of(item, "B", int(re.search(r'\br="(\d+)"', item).group(1))) == "fire_liaoli")
    live_number = int(re.search(r'\br="(\d+)"', live_row).group(1))
print(json.dumps({
    "ready": True,
    "liveSha256": live_hash,
    "liveMtime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "liveBytes": len(live_bytes),
    "liveLiaoliRow": live_number,
    "candidateSha256": candidate_hash,
    "different": different,
}, ensure_ascii=False))
