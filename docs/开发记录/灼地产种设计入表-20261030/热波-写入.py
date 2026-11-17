# -*- coding: utf-8 -*-
"""Rename 震环 to 热波 on current status text, and clear 地表定位 attribute."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "灼地产种设计入表-20261030"
live = root / "docs" / "术式制作表.xlsx"
NAME_CELLS = {
    5: ("N", "fire_meteorite", 3),
    6: ("DV", "fire_shock_ring", 1),
    10: ("N", "fire_hit_continue", 3),
    12: ("N", "fire_kindle", 1),
    13: ("N", "fire_detonate", 1),
}


def fail(message):
    raise SystemExit(message)


def sha256(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_xml(row, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row.find(">", start)
    if row[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</(?:[\w]+:)?c>", row[opener_end:])
        if not close:
            fail(f"Cell {column}{number} is not closed")
        end = opener_end + close.end()
    return row[start:end], start, end


def cell_text(cell):
    if cell is None:
        return None
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if value:
        return html.unescape(value.group(1))
    return None


def replace_text(cell, column, number, new_text):
    escaped = html.escape(new_text, quote=False)
    needs_space = "\n" in new_text or new_text[:1].isspace() or new_text[-1:].isspace()

    def opener(tag):
        if needs_space and "xml:space" not in tag:
            return tag[:-1] + ' xml:space="preserve">'
        return tag

    if re.search(r"<(?:[\w]+:)?t\b", cell):
        pattern = r"(<(?:[\w]+:)?t\b[^>]*>)[\s\S]*(</(?:[\w]+:)?t>)"
    elif re.search(r"<(?:[\w]+:)?v\b", cell):
        pattern = r"(<(?:[\w]+:)?v\b[^>]*>)[\s\S]*(</(?:[\w]+:)?v>)"
    else:
        fail(f"Cell {column}{number} has no text")
    updated, count = re.subn(pattern, lambda match: opener(match.group(1)) + escaped + match.group(2), cell, count=1)
    if count != 1 or cell_text(updated) != new_text:
        fail(f"Cell {column}{number} was not written cleanly")
    return updated


def set_cell(row, column, number, new_text):
    found = cell_xml(row, column, number)
    if not found:
        fail(f"Missing {column}{number}")
    cell, start, end = found
    return row[:start] + replace_text(cell, column, number, new_text) + row[end:]


def sheet_ids(xml):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        found_cell = cell_xml(row, "B", number)
        code = cell_text(found_cell[0]) if found_cell else None
        if code:
            found.append((code, number))
    return found


live_bytes = live.read_bytes()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"热波-术式制作表-{stamp}-1.xlsx"
output = batch / "热波-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}

sheet2 = parts["xl/worksheets/sheet2.xml"].decode("utf-8")
locations = {code: number for code, number in sheet_ids(sheet2)}
if locations.get("fire_shock_ring") != 6 or locations.get("fire_surface_anchor") != 7:
    fail(f"位置变化: 热波原行{locations.get('fire_shock_ring')} 地表定位{locations.get('fire_surface_anchor')}")
for title, name in (
    ("已完成", "xl/worksheets/sheet1.xml"),
    ("制作中", "xl/worksheets/sheet3.xml"),
    ("待验收", "xl/worksheets/sheet4.xml"),
):
    foreign = [code for code, _number in sheet_ids(parts[name].decode("utf-8")) if code in ("fire_shock_ring", "fire_surface_anchor")]
    if foreign:
        fail(f"{title} 已有目标编号")
if "震环" in parts["xl/worksheets/sheet6.xml"].decode("utf-8"):
    fail("资源明细有震环，需要先对账再改")

rows = rows_of(sheet2)
by_number = {row_number(row): row for row in rows}
ring = by_number[6]
anchor = by_number[7]
if cell_text(cell_xml(ring, "A", 6)[0]) != "震环" or cell_text(cell_xml(ring, "B", 6)[0]) != "fire_shock_ring":
    fail("第6行不是震环")
if cell_text(cell_xml(ring, "D", 6)[0]) != "火" or cell_text(cell_xml(ring, "E", 6)[0]) != "4" or cell_text(cell_xml(ring, "F", 6)[0]) != "16":
    fail("热波原行的属性、伤害或冷却已变")
if cell_text(cell_xml(anchor, "A", 7)[0]) != "地表定位" or cell_text(cell_xml(anchor, "C", 7)[0]) != "通用" or cell_text(cell_xml(anchor, "D", 7)[0]) != "火":
    fail("地表定位类型或属性已变")
edited = {6: set_cell(ring, "A", 6, "热波"), 7: set_cell(anchor, "D", 7, "无")}
for number, (column, code, expected) in NAME_CELLS.items():
    row = edited.get(number, by_number[number])
    if cell_text(cell_xml(row, "B", number)[0]) != code:
        fail(f"第{number}行不是{code}")
    old = cell_text(cell_xml(row, column, number)[0])
    if old.count("震环") != expected:
        fail(f"{column}{number} 的震环出现{old.count('震环')}次")
    edited[number] = set_cell(row, column, number, old.replace("震环", "热波"))

new_rows = [edited.get(row_number(row), row) for row in rows]
match = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", sheet2)
sheet2_out = sheet2[:match.start(2)] + "".join(new_rows) + sheet2[match.end(2):]
if "震环" in sheet2_out:
    fail("待制作仍有未替换的震环")
changed = {"xl/worksheets/sheet2.xml": sheet2_out.encode("utf-8")}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet2.xml"]:
        fail(f"Unexpected parts: {different}")
    pending = dst.read("xl/worksheets/sheet2.xml").decode("utf-8")
    counts = {}
    ids = []
    for title, name in (
        ("已完成", "xl/worksheets/sheet1.xml"),
        ("待制作", "xl/worksheets/sheet2.xml"),
        ("制作中", "xl/worksheets/sheet3.xml"),
        ("待验收", "xl/worksheets/sheet4.xml"),
    ):
        page = [code for code, _number in sheet_ids(dst.read(name).decode("utf-8"))]
        counts[title] = len(page)
        ids.extend(page)
    if counts != {"已完成": 40, "待制作": 13, "制作中": 3, "待验收": 3} or len(ids) != 59 or len(set(ids)) != 59:
        fail(f"Counts {counts}")
    check_rows = {row_number(row): row for row in rows_of(pending)}
    if cell_text(cell_xml(check_rows[6], "A", 6)[0]) != "热波" or cell_text(cell_xml(check_rows[6], "B", 6)[0]) != "fire_shock_ring":
        fail("热波编号或名称不对")
    if cell_text(cell_xml(check_rows[7], "D", 7)[0]) != "无" or cell_text(cell_xml(check_rows[7], "A", 7)[0]) != "地表定位":
        fail("地表定位没有改成无属性")
    kept = {
        4: ("fire_scorch_field", "火", "30"),
        5: ("fire_meteorite", "火", "40"),
        9: ("fire_stagger", "无", None),
        10: ("fire_hit_continue", "无", None),
        11: ("fire_seed_sow", "火", "5"),
        15: ("fire_break_ground", "土", None),
    }
    for number, (code, attribute, cooling) in kept.items():
        row = check_rows[number]
        if cell_text(cell_xml(row, "B", number)[0]) != code or cell_text(cell_xml(row, "D", number)[0]) != attribute:
            fail(f"前次批注被改掉 {code}")
        if cooling and cell_text(cell_xml(row, "F", number)[0]) != cooling:
            fail(f"{code} 冷却被改掉")
    scorch = cell_text(cell_xml(check_rows[4], "N", 4)[0])
    if "基础15点火种" not in scorch or cell_text(cell_xml(check_rows[4], "EC", 4)[0]) != "20":
        fail("灼地产种被改掉")
    if "其他属性术式仍可拼接使用" not in cell_text(cell_xml(check_rows[15], "N", 15)[0]):
        fail("崩地拼接句被改掉")
left_behind = {}
for name in ("xl/worksheets/sheet5.xml", "xl/worksheets/sheet7.xml"):
    left_behind[name] = parts[name].decode("utf-8").count("震环")

summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "counts": counts,
    "archiveNameHits": left_behind,
}
(batch / "热波-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"baseline": baseline, "candidate": summary["candidateSha256"], "left": left_behind}, ensure_ascii=False))
