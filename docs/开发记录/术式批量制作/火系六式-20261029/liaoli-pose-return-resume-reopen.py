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
backup = batch / "备份" / "liaoli-pose-return-resume-术式制作表-20261030-003559-1.xlsx"
intake = json.loads((batch / "liaoli-pose-intake.json").read_text(encoding="utf-8"))
expected = "69FD9B9BCC47885D0449CB135F84523894F48962745AA41BAED4FD1F05400C02"
baseline = "0D72B6D07C7E604943AC8DC71AA6EEB783F686EC0DAE1CEB1F69A13ED30FA6E7"


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


data = live.read_bytes()
stat = live.stat()
if sha256(data) != expected:
    raise SystemExit("Live hash is not the candidate")
if sha256(backup.read_bytes()) != baseline:
    raise SystemExit("Backup is not the baseline")
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(live) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"]:
        raise SystemExit(f"Unexpected parts: {different}")
    ids = []
    located = {}
    counts = {}
    for title, part in (
        ("已完成", "xl/worksheets/sheet1.xml"),
        ("待制作", "xl/worksheets/sheet2.xml"),
        ("制作中", "xl/worksheets/sheet3.xml"),
        ("待验收", "xl/worksheets/sheet4.xml"),
    ):
        page = ids_of(dst.read(part).decode("utf-8"))
        counts[title] = len(page)
        for number, code in page:
            ids.append(code)
            located.setdefault(code, []).append((title, number))
    making = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")
    row = next(item for item in rows_of(making) if re.search(r'<row r="6"', item))
    remark = text_of(row, "DV", 6)
    ok = (
        counts == {"已完成": 40, "待制作": 13, "制作中": 4, "待验收": 2}
        and len(ids) == 59 and len(set(ids)) == 59
        and located["fire_liaoli"] == [("制作中", 6)]
        and "fire_liaoli" not in dst.read("xl/worksheets/sheet4.xml").decode("utf-8")
        and text_of(row, "N", 6) == intake["restored"]["n"]
        and text_of(row, "O", 6) == intake["restored"]["o"]
        and remark.startswith(intake["currentDisplay"]["dv"])
        and "背后八剑姿态" in remark
        and located["fire_heat_up"] == [("制作中", 3)]
        and located["fire_burn"] == [("制作中", 4)]
        and located["fire_cool"] == [("制作中", 5)]
        and located["fire_fentian"] == [("待验收", 3)]
        and located["fire_liaoyuan"] == [("待验收", 4)]
    )
    if not ok:
        raise SystemExit(json.dumps({"counts": counts, "liaoli": located.get("fire_liaoli")}, ensure_ascii=False))
print(json.dumps({
    "liveSha256": sha256(data),
    "liveMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "liveBytes": len(data),
    "counts": counts,
    "unique": 59,
    "liaoli": located["fire_liaoli"],
    "different": different,
}, ensure_ascii=False))
