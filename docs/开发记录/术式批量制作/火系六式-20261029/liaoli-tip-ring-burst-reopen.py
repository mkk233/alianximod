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
backup = batch / "备份" / "liaoli-tip-ring-burst-submit-术式制作表-20261030-025101-1.xlsx"
intake = json.loads((batch / "liaoli-tip-ring-burst-intake.json").read_text(encoding="utf-8"))
expected = "410458A81EB7F37B5E1016072162BD31263532A92D34F2F52AB39F2574220F2C"
baseline = "798D366A9B901B9F9145891FAC3B2108ABFC16F0386303C5511458179B1B8CFC"


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
    located = {}
    counts = {}
    ids = []
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
    if different != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"]:
        raise SystemExit(str(different))
    if counts != {"已完成": 40, "待制作": 13, "制作中": 4, "待验收": 2} or len(set(ids)) != 59:
        raise SystemExit(str(counts))
    if located["fire_liaoli"] != [("制作中", 6)] or "fire_liaoli" in dst.read("xl/worksheets/sheet4.xml").decode("utf-8"):
        raise SystemExit("location")
    if text_of(row, "N", 6) != intake["restored"]["n"] or text_of(row, "O", 6) != intake["restored"]["o"]:
        raise SystemExit("restored")
    if not text_of(row, "DV", 6).startswith(intake["currentDisplay"]["dv"]):
        raise SystemExit("dv")
    if located["fire_heat_up"] != [("制作中", 3)] or located["fire_fentian"] != [("待验收", 3)] or located["fire_liaoyuan"] != [("待验收", 4)]:
        raise SystemExit("neighbors")
    if located["fire_flame_trail"] != [("待制作", 3)]:
        raise SystemExit("trail")
intake["submitted"] = True
intake["final"] = {
    "sheet": "制作中",
    "row": 6,
    "sha256": expected,
    "mtime": "2026-10-30 02:51:01.753",
    "bytes": len(data),
    "backup": "docs/开发记录/术式批量制作/火系六式-20261029/备份/liaoli-tip-ring-burst-submit-术式制作表-20261030-025101-1.xlsx",
}
(batch / "liaoli-tip-ring-burst-intake.json").write_text(json.dumps(intake, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "sha256": expected,
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": len(data),
    "counts": counts,
    "unique": 59,
    "liaoli": located["fire_liaoli"],
}, ensure_ascii=False))
