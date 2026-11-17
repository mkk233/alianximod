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
backup = batch / "备份" / "unlock-writeback-术式制作表-20261029-114250-1.xlsx"
notes = json.loads((batch / "unlock-writeback-notes.json").read_text(encoding="utf-8"))
stat = live.stat()


def cell_text(sheet, column, row):
    row_xml = re.search(rf'<(?:[\w]+:)?row\b[^>]*\br="{row}"[^>]*>[\s\S]*?</(?:[\w]+:)?row>', sheet).group(0)
    cell = re.search(rf'<(?:[\w]+:)?c r="{column}{row}"[\s\S]*?</(?:[\w]+:)?c>', row_xml).group(0)
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<(?:[\w]+:)?v>([\s\S]*)</(?:[\w]+:)?v>", cell)
    return html.unescape(value.group(1))


with zipfile.ZipFile(backup) as src, zipfile.ZipFile(live) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    sheet = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")
    old = src.read("xl/worksheets/sheet3.xml").decode("utf-8")
checks = {}
for code, addition in notes["additions"].items():
    checks[code] = addition in sheet
furnace = {}
for row, code in ((6, "fire_heat_up"), (7, "fire_burn"), (8, "fire_cool")):
    furnace[code] = cell_text(sheet, "DV", row) == cell_text(old, "DV", row) and "刻印台实测反馈" not in cell_text(sheet, "DV", row)
kept = {}
for row, code in ((3, "fire_liaoyuan"), (4, "fire_liaoli"), (5, "fire_fentian")):
    kept[code] = {
        "n": cell_text(sheet, "N", row) == cell_text(old, "N", row),
        "o": cell_text(sheet, "O", row) == cell_text(old, "O", row),
        "dvPrefix": cell_text(sheet, "DV", row).startswith(cell_text(old, "DV", row)),
        "marker": "刻印台实测反馈" in cell_text(sheet, "DV", row),
    }
print(json.dumps({
    "sha256": hashlib.sha256(live.read_bytes()).hexdigest().upper(),
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": stat.st_size,
    "different": different,
    "additions": checks,
    "furnaceUnchanged": furnace,
    "kept": kept,
}, ensure_ascii=False, indent=2))
