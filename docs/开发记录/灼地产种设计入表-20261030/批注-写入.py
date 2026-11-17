# -*- coding: utf-8 -*-
"""Apply the seven confirmed sheet annotations. Does not move techniques."""
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
FOOTER = "魔力、冷却、复杂度、装配消耗、等级上限和等级成长未定，对应数值格留空，不用0代替。"
COOLDOWNS = {
    "fire_scorch_field": 30,
    "fire_meteorite": 40,
    "fire_shock_ring": 16,
    "fire_seed_sow": 5,
}
ATTRIBUTES = {
    "fire_stagger": "无",
    "fire_hit_continue": "无",
    "fire_break_ground": "土",
}
EXPECTED_ROWS = {
    "fire_scorch_field": 4,
    "fire_meteorite": 5,
    "fire_shock_ring": 6,
    "fire_stagger": 9,
    "fire_hit_continue": 10,
    "fire_seed_sow": 11,
    "fire_break_ground": 15,
}
EARTH_SENTENCE = "地形修改是土属性效果，其他属性术式仍可拼接使用。"


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


def num_cell(column, number, value):
    return f'<c r="{column}{number}" s="15" t="n"><v>{value}</v></c>'


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


def cooling_sentence(ticks):
    return f"基础冷却{ticks}tick。魔力、复杂度、装配消耗、等级上限和等级成长未定，对应数值格留空，不用0代替。"


live_bytes = live.read_bytes()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"批注-术式制作表-{stamp}-1.xlsx"
output = batch / "批注-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}

sheet2 = parts["xl/worksheets/sheet2.xml"].decode("utf-8")
if 'sqref="F' in sheet2 and 'type="list"' in sheet2:
    for block in re.findall(r"<dataValidation\b[^>]*>[\s\S]*?</dataValidation>", sheet2):
        if re.search(r'sqref="F', block) and 'type="list"' in block:
            fail("冷却列是下拉列表，不能直接写数字")
locations = {code: number for code, number in sheet_ids(sheet2)}
for code, number in EXPECTED_ROWS.items():
    if locations.get(code) != number:
        fail(f"{code} 不在待制作第{number}行，当前是{locations.get(code)}")
for title, name in (
    ("已完成", "xl/worksheets/sheet1.xml"),
    ("制作中", "xl/worksheets/sheet3.xml"),
    ("待验收", "xl/worksheets/sheet4.xml"),
):
    foreign = [code for code, _number in sheet_ids(parts[name].decode("utf-8")) if code in EXPECTED_ROWS]
    if foreign:
        fail(f"{title} 也有这些编号: {foreign}")

rows = rows_of(sheet2)
by_number = {row_number(row): row for row in rows}
edited = {}
for code, ticks in COOLDOWNS.items():
    number = EXPECTED_ROWS[code]
    row = by_number[number]
    if cell_text(cell_xml(row, "D", number)[0]) != "火":
        fail(f"{code} 属性已不是火")
    if cell_xml(row, "F", number):
        fail(f"{code} 冷却格已有值")
    dv = cell_text(cell_xml(row, "DV", number)[0])
    if FOOTER not in dv:
        fail(f"{code} 备注里没有待定冷却句")
    row = set_cell(row, "DV", number, dv.replace(FOOTER, cooling_sentence(ticks), 1))
    if code == "fire_scorch_field":
        text_n = cell_text(cell_xml(row, "N", number)[0])
        old_n = "费用、冷却、魔力、复杂度、装配消耗、等级上限和等级成长仍未定。"
        old_append = "费用、冷却、魔力、复杂度、装配消耗、等级上限、等级成长，以及与幽冥或其他不同来源产种的叠加细则仍未定。"
        new_append = "费用、魔力、复杂度、装配消耗、等级上限、等级成长，以及与幽冥或其他不同来源产种的叠加细则仍未定。"
        if old_n not in text_n or text_n.count(old_n) != 1:
            fail("灼地需求里的待定冷却句数量不对")
        row = set_cell(row, "N", number, text_n.replace(old_n, "基础冷却30tick。费用、魔力、复杂度、装配消耗、等级上限和等级成长仍未定。", 1))
        dv_now = cell_text(cell_xml(row, "DV", number)[0])
        if old_append not in dv_now or dv_now.count(old_append) != 1:
            fail("灼地备注后半的待定冷却句数量不对")
        row = set_cell(row, "DV", number, dv_now.replace(old_append, new_append, 1))
        if "基础20点火种" not in cell_text(cell_xml(row, "N", number)[0]):
            fail("灼地产种需求被覆盖")
    anchor = "E" if cell_xml(row, "E", number) else "D"
    found = cell_xml(row, anchor, number)
    if not found:
        fail(f"{code} 无法插入冷却列")
    row = row[:found[2]] + num_cell("F", number, ticks) + row[found[2]:]
    edited[number] = row

for code, attribute in ATTRIBUTES.items():
    number = EXPECTED_ROWS[code]
    row = by_number[number]
    if cell_text(cell_xml(row, "C", number)[0]) != "通用" or cell_text(cell_xml(row, "D", number)[0]) != "火":
        fail(f"{code} 类型或属性已变")
    row = set_cell(row, "D", number, attribute)
    if code == "fire_break_ground":
        text_n = cell_text(cell_xml(row, "N", number)[0])
        if "火属性能力" in text_n or "仅限火" in text_n:
            fail("崩地需求有未处理的火属性限制")
        if EARTH_SENTENCE not in text_n:
            row = set_cell(row, "N", number, text_n + EARTH_SENTENCE)
    else:
        text_n = cell_text(cell_xml(row, "N", number)[0])
        if any(token in text_n for token in ("仅限火", "只限火", "火系专用", "火属性专用")):
            fail(f"{code} 需求仍写明只限火系")
    edited[number] = row

new_rows = []
for row in rows:
    number = row_number(row)
    new_rows.append(edited.get(number, row))
match = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", sheet2)
sheet2_out = sheet2[:match.start(2)] + "".join(new_rows) + sheet2[match.end(2):]
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
    if counts != {"已完成": 40, "待制作": 13, "制作中": 3, "待验收": 3} or len(set(ids)) != 59:
        fail(f"Counts {counts}")
    for row in rows_of(pending):
        number = row_number(row)
        code = cell_text(cell_xml(row, "B", number)[0]) if cell_xml(row, "B", number) else None
        if code in COOLDOWNS:
            if cell_text(cell_xml(row, "F", number)[0]) != str(COOLDOWNS[code]):
                fail(f"{code} 冷却不对")
            if "、冷却" in cell_text(cell_xml(row, "DV", number)[0]) or "冷却、" in cell_text(cell_xml(row, "N", number)[0] or ""):
                fail(f"{code} 仍把冷却列为待定")
        if code in ATTRIBUTES and cell_text(cell_xml(row, "D", number)[0]) != ATTRIBUTES[code]:
            fail(f"{code} 属性不对")
        if code == "fire_scorch_field":
            text_n = cell_text(cell_xml(row, "N", number)[0])
            if "基础15点火种" not in text_n or cell_text(cell_xml(row, "E", number)[0]) != "1":
                fail("灼地产种或伤害被改掉")
            if cell_text(cell_xml(row, "EC", number)[0]) != "20" or cell_xml(row, "BV", number):
                fail("灼地资源或旧周期被改掉")
        if code == "fire_break_ground" and EARTH_SENTENCE not in cell_text(cell_xml(row, "N", number)[0]):
            fail("崩地兼容句没有写入")
        if code in ("fire_stagger", "fire_hit_continue") and cell_text(cell_xml(row, "C", number)[0]) != "通用":
            fail(f"{code} 类型被改掉")

summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "rows": EXPECTED_ROWS,
    "counts": counts,
}
(batch / "批注-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"baseline": baseline, "candidate": summary["candidateSha256"]}, ensure_ascii=False))
