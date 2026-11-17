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
backup = batch / "备份" / "liaoli-pose-writeback-submit-术式制作表-20261030-010230-1.xlsx"
snapshot = json.loads((batch / "liaoli-pose-transition-snapshot.json").read_text(encoding="utf-8"))
expected = "EB3E24375BC4198F11C122E99523431C8B79BB9E32849E414C5913116AC2A7CA"
baseline = "69FD9B9BCC47885D0449CB135F84523894F48962745AA41BAED4FD1F05400C02"


def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def text_of(row, column, number):
    match = re.search(rf'<c r="{column}{number}"[^>]*>([\s\S]*?)</c>', row)
    if not match:
        return None
    inner = match.group(1)
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", inner)
    if value:
        return html.unescape(value.group(1))
    inline = re.search(r"<t(?: [^>]*)?>([\s\S]*)</t>", inner)
    return html.unescape(inline.group(1)) if inline else None


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
if sha256(data) != expected or sha256(backup.read_bytes()) != baseline:
    raise SystemExit("Hash mismatch")
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(live) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
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
    pending = dst.read("xl/worksheets/sheet4.xml").decode("utf-8")
    row = next(item for item in rows_of(pending) if re.search(r'<row r="5"', item))
    remark = text_of(row, "DV", 5)
    if different != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"]:
        raise SystemExit(different)
    if counts != {"已完成": 40, "待制作": 13, "制作中": 3, "待验收": 3} or len(set(ids)) != 59:
        raise SystemExit(counts)
    if located["fire_liaoli"] != [("待验收", 5)] or "fire_liaoli" in dst.read("xl/worksheets/sheet3.xml").decode("utf-8"):
        raise SystemExit("location")
    if text_of(row, "N", 5) != snapshot["conversion"]["nAfter"] or text_of(row, "O", 5) != snapshot["conversion"]["oAfter"]:
        raise SystemExit("display")
    if not remark.startswith(snapshot["currentDetailed"]["dv"]) or "眼睛位置匹配" not in remark:
        raise SystemExit("dv")
print(json.dumps({
    "sha256": sha256(data),
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": len(data),
    "counts": counts,
    "unique": 59,
    "liaoli": located["fire_liaoli"],
}, ensure_ascii=False))
