# -*- coding: utf-8 -*-
"""Append 火苗 to 待制作. Does not claim, implement, or edit other techniques."""
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
NEW_ID = "fire_spark"
NEW_N = (
    "火苗是独立的火属性攻击术式，单独做一版飞弹载体。严格复用现有攻击、飞弹、索敌、弹道、复制与附魔底座，"
    "不引入攻击附加攻击，也不让附魔自动生成默认载体。\n"
    "释放一枚以小火苗表现的飞弹，复用现有飞弹运动、碰撞和正常目标过滤。默认弹道为直线，允许现有弹道改写。"
    "有效命中时造成1点基础火属性伤害，增加基础2点火种，并施加原版燃烧。基础冷却5tick。"
    "单次命中结算，不做额外爆炸或范围伤害。命中后弹体消失。\n"
    "原版燃烧的后续跳伤不重复产生火种，不消耗火种，也不自动转成资源型引燃或引爆。原版燃烧时长尚未确认。\n"
    "基础2点产种沿用现有炉心产种倍率、取整与目标100点共享上限。同一次释放的重复碰撞和复制对同一目标不能绕过现有去重。\n"
    "本体只提供这2点火种，不内置种火附魔的10点。种火装在火苗上时，设计预期是同一命中的基础2再加种火的基础10，然后按炉心产种规则结算。"
    "12不是火苗的固有数值。\n"
    "接入现有兼容的索敌、弹道、复制和附魔，不按术式编号做新的链路特判。"
    "魔力、装配消耗、飞行速度、射程、寿命、碰撞尺寸、等级上限和等级成长未定。"
)
NEW_O = "一枚可辨认的小火苗沿飞弹路径飞行，可有短尾焰。命中时短暂火星，并表现原版着火。尾焰只作表现，不自动形成留焰判定体。"
NEW_DV = (
    "2026-10-30用户确认：“把火苗写入待制作表，同样只有5tickCD”。基础伤害1，基础冷却5tick，产种基础2，属性火，编号fire_spark。"
    "只进入待制作，尚未领取。\n"
    "原版燃烧时长待定，建议20tick。不把20写成已确认正式时长。表内没有专用点燃时长字段，时长留空，不把原版燃烧记成火种或燃烧结算。\n"
    "索敌、弹道和附魔开关按现有火球飞弹的类别复用：命中体、触发、直线、攻击、移动命中体、接受索敌和弹道改写。"
    "飞行速度、存活tick和碰撞尺寸没有照搬火球的已登记数值。\n"
    "魔力、装配消耗、等级上限和等级成长未定，对应数值格留空，不用0代替。\n"
    "种火与火苗组合时，预期同一命中为基础2加种火基础10，再按炉心规则结算。"
    "制作前需定向确认本体产种与附魔产种能沿现有入口合并一次结算，避免两次独立提交被同次产种去重拦掉一份。"
    "尚未核对，不能认为现有代码已经支持合并。"
)
KEEP = (
    "fire_flame_trail", "fire_scorch_field", "fire_meteorite", "fire_shock_ring", "fire_upwell",
    "rule_surface_anchor", "rule_stagger", "rule_hit_continue", "fire_seed_sow", "earth_break_ground",
)


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


def text_cell(column, number, text, style):
    escaped = html.escape(text, quote=False)
    space = ' xml:space="preserve"' if ("\n" in text or text[:1].isspace() or text[-1:].isspace()) else ""
    return f'<c r="{column}{number}" s="{style}" t="str"><v{space}>{escaped}</v></c>'


def num_cell(column, number, value):
    return f'<c r="{column}{number}" s="15" t="n"><v>{value}</v></c>'


def build_row(number):
    parts = [
        text_cell("A", number, "火苗", "15"),
        text_cell("B", number, NEW_ID, "16"),
        text_cell("C", number, "法术", "15"),
        text_cell("D", number, "火", "15"),
        num_cell("E", number, "1"),
        num_cell("F", number, "5"),
        text_cell("J", number, "否", "15"),
        text_cell("M", number, "是", "15"),
        text_cell("N", number, NEW_N, "16"),
        text_cell("O", number, NEW_O, "16"),
        text_cell("P", number, "命中体", "15"),
        text_cell("Q", number, "触发", "15"),
        text_cell("R", number, "直线", "15"),
        text_cell("S", number, "攻击", "15"),
        text_cell("T", number, "是", "15"),
        text_cell("Y", number, "是", "15"),
        text_cell("Z", number, "是", "15"),
        text_cell("AA", number, "否", "15"),
        text_cell("AB", number, "否", "15"),
        text_cell("AH", number, "是", "15"),
        text_cell("AK", number, "是", "15"),
        text_cell("DV", number, NEW_DV, "16"),
        text_cell("DW", number, "是", "15"),
        text_cell("DX", number, "火种", "15"),
        text_cell("DZ", number, "增加", "15"),
        text_cell("EA", number, "命中目标", "15"),
        text_cell("EB", number, "命中成功", "15"),
        num_cell("EC", number, "2"),
        text_cell("EI", number, "每目标每次施法", "15"),
        text_cell("EJ", number, "有效命中才增加火种。原版燃烧的后续跳伤不产种、不耗种，也不转成引燃或引爆。", "16"),
        text_cell("EK", number, "基础2，再按现有炉心产种倍率与上限100沿用底座取整。同次释放对同一目标沿用现有去重。不含种火的10点。", "16"),
        text_cell("EL", number, "N/DV", "15"),
        text_cell("EM", number, "本体只有基础2。种火的基础10是附魔，不写进本行数值。合并结算尚未核对。", "16"),
    ]
    return f'<row r="{number}" ht="225" customHeight="1">{"".join(parts)}</row>'


live_bytes = live.read_bytes()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"火苗-术式制作表-{stamp}-1.xlsx"
output = batch / "火苗-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}

status_parts = (
    ("已完成", "xl/worksheets/sheet1.xml"),
    ("待制作", "xl/worksheets/sheet2.xml"),
    ("制作中", "xl/worksheets/sheet3.xml"),
    ("待验收", "xl/worksheets/sheet4.xml"),
)
for title, name in status_parts:
    for code, number, row in sheet_ids(parts[name].decode("utf-8")):
        name_text = cell_text(cell_xml(row, "A", number)[0])
        if code == NEW_ID or name_text == "火苗":
            fail(f"已有火苗: {title} 第{number}行 {code}")
sheet2 = parts["xl/worksheets/sheet2.xml"].decode("utf-8")
pending_ids = sheet_ids(sheet2)
pending_codes = [code for code, _number, _row in pending_ids]
if any(code not in pending_codes for code in KEEP):
    fail("待制作缺少应保留的编号")
if sheet2.count('ref="A1:EN15"') != 1:
    fail("待制作范围不是A1:EN15")
if [row_number(row) for row in rows_of(sheet2)] != list(range(1, 16)):
    fail("待制作行号不连续")
new_number = 16
row_xml = build_row(new_number)
if "12点" in row_xml or ">12<" in row_xml:
    fail("把12写进了数值")
close = sheet2.rfind("</sheetData>")
sheet2_out = sheet2[:close] + row_xml + sheet2[close:]
sheet2_out = sheet2_out.replace('ref="A1:EN15"', 'ref="A1:EN16"', 1)
changed = {"xl/worksheets/sheet2.xml": sheet2_out.encode("utf-8")}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet2.xml"]:
        fail(f"Unexpected parts: {different}")
    counts = {}
    ids = []
    for title, name in status_parts:
        page = [code for code, _number, _row in sheet_ids(dst.read(name).decode("utf-8"))]
        counts[title] = len(page)
        ids.extend(page)
    if counts != {"已完成": 40, "待制作": 14, "制作中": 3, "待验收": 3} or len(ids) != 60 or len(set(ids)) != 60:
        fail(f"Counts {counts} unique {len(set(ids))}")
    pending = dst.read("xl/worksheets/sheet2.xml").decode("utf-8")
    rows = {number: row for _code, number, row in sheet_ids(pending)}
    added = rows[16]
    checks = {
        "A": "火苗", "B": NEW_ID, "C": "法术", "D": "火", "E": "1", "F": "5",
        "P": "命中体", "Q": "触发", "R": "直线", "S": "攻击",
        "DX": "火种", "DZ": "增加", "EB": "命中成功", "EC": "2", "EI": "每目标每次施法",
    }
    for column, expected in checks.items():
        if cell_text(cell_xml(added, column, 16)[0]) != expected:
            fail(f"{column} 不是 {expected}")
    if cell_xml(added, "U", 16) or cell_xml(added, "V", 16) or cell_xml(added, "L", 16) or cell_xml(added, "EF", 16):
        fail("未定数值被填上")
    text_n = cell_text(cell_xml(added, "N", 16)[0])
    text_dv = cell_text(cell_xml(added, "DV", 16)[0])
    if "建议20tick" not in text_dv or "已确认正式时长" not in text_dv:
        fail("燃烧时长备注不对")
    if "基础2点火种" not in text_n or "12不是火苗的固有数值" not in text_n:
        fail("产种说明不对")
    for code in KEEP:
        if pending_codes.count(code) != 1 or code not in [item for item, _number, _row in sheet_ids(pending)]:
            fail(f"保留编号丢失 {code}")
    if NEW_ID in dst.read("xl/worksheets/sheet6.xml").decode("utf-8"):
        fail("资源明细重复了首条")
    old_rows = {number: row for _code, number, row in pending_ids}
    new_rows = {number: row for _code, number, row in sheet_ids(pending) if number < 16}
    for number, row in old_rows.items():
        if new_rows[number] != row:
            fail(f"原第{number}行被改动")

summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "row": 16,
    "counts": counts,
}
(batch / "火苗-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"baseline": baseline, "candidate": summary["candidateSha256"], "row": 16}, ensure_ascii=False))
