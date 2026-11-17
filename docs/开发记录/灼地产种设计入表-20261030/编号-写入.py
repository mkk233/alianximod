# -*- coding: utf-8 -*-
"""Rename pending non-fire technique ids. Does not touch code, archive, or comments."""
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
MAPPING = {
    "fire_surface_anchor": "rule_surface_anchor",
    "fire_stagger": "rule_stagger",
    "fire_hit_continue": "rule_hit_continue",
    "fire_break_ground": "earth_break_ground",
}
EXPECT = {
    "fire_surface_anchor": ("地表定位", "通用", "无", "rule_surface_anchor"),
    "fire_stagger": ("错时", "通用", "无", "rule_stagger"),
    "fire_hit_continue": ("命中接续", "通用", "无", "rule_hit_continue"),
    "fire_break_ground": ("崩地", "通用", "土", "earth_break_ground"),
}
EDITABLE = {
    "xl/worksheets/sheet1.xml",
    "xl/worksheets/sheet2.xml",
    "xl/worksheets/sheet3.xml",
    "xl/worksheets/sheet4.xml",
    "xl/worksheets/sheet5.xml",
    "xl/worksheets/sheet6.xml",
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
            found.append((code, number, row))
    return found


def apply_map(text):
    updated = text
    for old, new in MAPPING.items():
        updated = updated.replace(old, new)
    return updated


def rewrite(xml):
    touched = []
    rebuilt = []
    for row in rows_of(xml):
        number = row_number(row)
        current = row
        columns = re.findall(rf'r="([A-Z]+){number}"', row)
        for column in columns:
            found = cell_xml(current, column, number)
            text = cell_text(found[0]) if found else None
            if not text or not any(old in text for old in MAPPING):
                continue
            current = set_cell(current, column, number, apply_map(text))
            touched.append(f"{column}{number}")
        rebuilt.append(current)
    match = re.search(r"(<(?:[\w]+:)?sheetData>)([\s\S]*)(</(?:[\w]+:)?sheetData>)", xml)
    return xml[:match.start(2)] + "".join(rebuilt) + xml[match.end(2):], touched


live_bytes = live.read_bytes()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"编号-术式制作表-{stamp}-1.xlsx"
output = batch / "编号-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}

presence = {name: {old: parts[name].decode("utf-8").count(old) for old in MAPPING} for name in parts if name.endswith(".xml")}
sheet2 = parts["xl/worksheets/sheet2.xml"].decode("utf-8")
located = {code: (number, row) for code, number, row in sheet_ids(sheet2)}
for old, (name, kind, element, _new) in EXPECT.items():
    if old not in located:
        fail(f"{old} 不在待制作")
    number, row = located[old]
    if cell_text(cell_xml(row, "A", number)[0]) != name or cell_text(cell_xml(row, "C", number)[0]) != kind or cell_text(cell_xml(row, "D", number)[0]) != element:
        fail(f"{old} 的名称、类型或属性已变")
for title, name in (("已完成", "xl/worksheets/sheet1.xml"), ("制作中", "xl/worksheets/sheet3.xml"), ("待验收", "xl/worksheets/sheet4.xml")):
    for code, number, row in sheet_ids(parts[name].decode("utf-8")):
        if code in MAPPING or code in MAPPING.values():
            fail(f"{title} 已有目标编号 {code}")
        element = cell_text(cell_xml(row, "D", number)[0]) if cell_xml(row, "D", number) else None
        if code.startswith("fire_") and element not in (None, "火"):
            fail(f"历史页出现未授权的前缀遗留 {title} {code}")
for code, number, row in sheet_ids(sheet2):
    element = cell_text(cell_xml(row, "D", number)[0]) if cell_xml(row, "D", number) else None
    if code.startswith("fire_") and element not in (None, "火") and code not in MAPPING:
        fail(f"待制作还有未列入的前缀遗留 {code}")
    if code in MAPPING.values():
        fail(f"新编号已存在 {code}")

changed = {}
touched = {}
for name in EDITABLE:
    updated, places = rewrite(parts[name].decode("utf-8"))
    if places:
        changed[name] = updated.encode("utf-8")
        touched[name] = places
if touched != {"xl/worksheets/sheet2.xml": ["B7", "B9", "B10", "B15"]}:
    fail(f"改动范围不是四格编号: {touched}")

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        payload = changed.get(info.filename, src.read(info.filename))
        dst.writestr(info, payload)

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet2.xml"]:
        fail(f"Unexpected parts: {different}")
    counts = {}
    ids = []
    for title, name in (
        ("已完成", "xl/worksheets/sheet1.xml"),
        ("待制作", "xl/worksheets/sheet2.xml"),
        ("制作中", "xl/worksheets/sheet3.xml"),
        ("待验收", "xl/worksheets/sheet4.xml"),
    ):
        page = [code for code, _number, _row in sheet_ids(dst.read(name).decode("utf-8"))]
        counts[title] = len(page)
        ids.extend(page)
    if counts != {"已完成": 40, "待制作": 13, "制作中": 3, "待验收": 3} or len(ids) != 59 or len(set(ids)) != 59:
        fail(f"Counts {counts} unique {len(set(ids))}")
    for new in MAPPING.values():
        if ids.count(new) != 1:
            fail(f"{new} 不是唯一编号")
    pending = dst.read("xl/worksheets/sheet2.xml").decode("utf-8")
    for old in MAPPING:
        if old in pending:
            fail(f"待制作仍有 {old}")
    for name in EDITABLE:
        text = dst.read(name).decode("utf-8")
        for old in MAPPING:
            if old in text:
                fail(f"{name} 仍有 {old}")
    rows = {number: row for code, number, row in sheet_ids(pending)}
    if cell_text(cell_xml(rows[6], "B", 6)[0]) != "fire_shock_ring" or cell_text(cell_xml(rows[6], "A", 6)[0]) != "热波":
        fail("热波编号被改掉")
    if "基础15点火种" not in cell_text(cell_xml(rows[4], "N", 4)[0]) or cell_text(cell_xml(rows[4], "F", 4)[0]) != "30":
        fail("灼地被改掉")
    if cell_text(cell_xml(rows[15], "D", 15)[0]) != "土" or "其他属性术式仍可拼接使用" not in cell_text(cell_xml(rows[15], "N", 15)[0]):
        fail("崩地属性或拼接句被改掉")
    stagger_cooling = cell_xml(rows[9], "F", 9)
    if cell_text(cell_xml(rows[7], "D", 7)[0]) != "无" or stagger_cooling is not None:
        fail("地表定位属性被改，或错时冷却被填上")

comment_hits = {}
for name, payload in parts.items():
    if "comment" not in name.lower():
        continue
    text = payload.decode("utf-8", errors="replace")
    comment_hits[name] = {old: text.count(old) for old in MAPPING if old in text}
summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "mapping": MAPPING,
    "touched": touched,
    "counts": counts,
    "commentHits": comment_hits,
    "partHits": {name: counts_for for name, counts_for in presence.items() if any(counts_for.values())},
}
(batch / "编号-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"baseline": baseline, "candidate": summary["candidateSha256"], "touched": touched, "comments": comment_hits}, ensure_ascii=False))
