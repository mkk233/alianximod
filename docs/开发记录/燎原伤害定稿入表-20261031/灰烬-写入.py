# -*- coding: utf-8 -*-
"""Append the ash-cover and radius-10 spec to the current 燎原 row."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "燎原伤害定稿入表-20261031"
live = root / "docs" / "术式制作表.xlsx"
TARGET = "fire_liaoyuan"
OLD_N = "铺开地表火层。踩中后按段燃烧，每段消耗10点火种，燃料耗尽或到达期限就结束"
EK_ADD = "首段在首次被已展开接触体碰到时结算，首次延迟0，这一段就是原有各段里的第一段，不另加一段。之后仍每20tick。扣款触发仍是燃烧结算，不在接触时预付全部余额。"
EJ_ADD = "首次接触以已展开前沿碰到为准。重复进出和每帧前沿相交不重复挂同一燃烧层。"
DV_APPEND = (
    "2026-10-31追加设计：半径10、灰烬覆盖、展开即伤害与首段燃烧，待美术与代码落实。"
    "本轮没有新贴图，也没有改游戏。此条取代刚确认方案里的视觉半径约6格，以及附着层首段固定在第20tick的说法。"
    "完整追加见 docs/开发记录/燎原伤害定稿入表-20261031/灰烬特效与范围任务.md。\n"
    "场地半径改为10，填的是半径，不是直径。视觉半径10格，视觉直径约20格。"
    "实际方形贴地接触区半宽10格，水平约20×20格。低矮接触层仍随各处地表高度，用实体完整碰撞盒相交。\n"
    "保留现有从中心向外展开的动画，沿用已有12tick展开节奏，不改成瞬间铺满。"
    "服务端已展开范围与客户端进度一致。前沿扫过的地表立即参加接触检测，每tick检查。"
    "不等待展开结束才伤害，也不提前打尚未覆盖的整片最终范围。"
    "200tick从火场生成起算，包含展开过程，完全铺开后不重新计200tick。\n"
    "展开前沿扫过后，地表留下连续的深灰、炭黑与灰烬颗粒覆盖，盖住有效区域各处地表。"
    "原来稀疏的火焰斑点不再作为主体。覆盖层上零散暗红余烬和橙黄火星，疏密与明灭错开，少量火星短暂升起再熄灭。"
    "重点是大面积阴燃灰烬地表，不是每一格都立起一团同样的大火。表现贴合真实地形，不是浮在空中的大圆片。"
    "外缘保持近圆形展开观感，用破碎灰烬边缘、火浪和火星过渡弱化方形轮廓。"
    "方形接触区的边角也要有可见灰烬，避免完全看不见的伤害区。"
    "灰烬层是术式临时地表覆盖，结束时火星减弱、灰烬淡去，原方块外观恢复。"
    "不蔓延，不破坏地形，不留下永久烧焦方块，不放置原版火。"
    "参考只采用素材库 burnt 的整面炭灰与稀疏热点方向，不把该模组贴图接入，本轮也没有新的资源路径。\n"
    "目标被已展开接触体首次碰到时，立刻结算一次本体基础8点火伤，并接受一次炉心加成。"
    "这一下就是该目标本体周期的第1次，同tick不再另叠一次展开伤害和一次场地首伤。之后仍接触则每10tick结算。"
    "若目标有可用火种且本次火场允许启动附着燃烧，则立即启动并结算首个燃烧段，"
    "经共享燃料的同一结算入口请求10点火种，足额伤害仍为4×(1+0.1n+F)。"
    "这是把首段提前到接触时，不是在原有段数外赠送额外燃烧段。启动时观察n和F发生在扣这一段燃料之前。"
    "后续仍每20tick结算，按实付/10缩放。首次延迟记为0。周期仍是20。扣款触发仍是燃烧结算，不是接触时预付全部余额。\n"
    "每个目标由同一火场的首次接触机会触发一次起手。重复进出、每帧前沿相交不重复首次伤害，也不重复挂同一燃烧层。"
    "本体后续周期仍保留同目标计时。离场只停止本体伤害，已有附着燃烧独立持续。"
    "同tick的本体伤害和燃烧伤害都要兑现，不能重复提交，不能把被无敌帧吞掉的一份算成已兑现，也不修改全局无敌帧。"
    "本轮没有检查代码。\n"
    "8点/10tick本体、4×(1+0.1n+F)燃烧、每段10燃料/20tick、200tick寿命都保留。"
    "若目标获得完整20次本体命中，并且过载起手满热F=0.60、初始100点火种、无补种、无其他燃烧抢资源、无减伤和免疫，"
    "本体256加燃烧104等于360的理论上限仍成立。燃烧首段改为首次接触立即结算，"
    "满10段可在相对首次接触的第0、20、40至180tick完成，仍只消耗100点，不变成11段。"
    "展开首伤包含在20次本体里，不另加12.8。"
    "外围目标要等前沿到达才首次命中，本体结算次数可能少于20，不能把360写成范围内每个目标必得。"
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


live_bytes = live.read_bytes()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"灰烬-术式制作表-{stamp}-1.xlsx"
output = batch / "灰烬-candidate-preserved.xlsx"
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
locations = []
for title, name in status_parts:
    for code, number, row in sheet_ids(parts[name].decode("utf-8")):
        if code == TARGET or cell_text(cell_xml(row, "A", number)[0]) == "燎原":
            locations.append((title, name, number, row))
if len(locations) != 1 or locations[0][0] != "待验收":
    fail(f"燎原不在待验收的唯一行: {[(item[0], item[2]) for item in locations]}")
_title, part_name, number, row = locations[0]
if cell_text(cell_xml(row, "AS", number)[0]) != "6":
    fail("场地半径已不是6，停止以免把直径写进半径")
if cell_text(cell_xml(row, "E", number)[0]) != "8" or cell_text(cell_xml(row, "BV", number)[0]) != "10":
    fail("本体8点或10tick周期已变")
if cell_text(cell_xml(row, "EC", number)[0]) != "10" or cell_text(cell_xml(row, "EG", number)[0]) != "20" or cell_text(cell_xml(row, "EB", number)[0]) != "燃烧结算":
    fail("火种请求、周期或触发已变")
if cell_text(cell_xml(row, "N", number)[0]) != OLD_N:
    fail("展示句已变")
old_dv = cell_text(cell_xml(row, "DV", number)[0])
old_o = cell_text(cell_xml(row, "O", number)[0])
old_ek = cell_text(cell_xml(row, "EJ", number)[0])
old_constraint = cell_text(cell_xml(row, "EK", number)[0])
if "2026-10-31追加设计：半径10" in old_dv:
    fail("灰烬追加已经写过")
if "ground_flame.png" not in old_o:
    fail("旧特效路径不在")
if cell_xml(row, "EH", number):
    fail("首次延迟格已有值，需要先对账")
updated = set_cell(row, "AS", number, "10")
updated = set_cell(updated, "EJ", number, old_ek + EJ_ADD)
updated = set_cell(updated, "EK", number, old_constraint + EK_ADD)
updated = set_cell(updated, "DV", number, old_dv + "\n" + DV_APPEND)
found = cell_xml(updated, "EG", number)
delay = f'<c r="EH{number}" s="15" t="n"><v>0</v></c>'
updated = updated[:found[2]] + delay + updated[found[2]:]
if cell_text(cell_xml(updated, "N", number)[0]) != OLD_N or cell_text(cell_xml(updated, "O", number)[0]) != old_o:
    fail("展示句或旧贴图路径被改掉")
if cell_text(cell_xml(updated, "EH", number)[0]) != "0" or cell_text(cell_xml(updated, "EG", number)[0]) != "20":
    fail("延迟或燃烧周期不对")
if cell_text(cell_xml(updated, "AS", number)[0]) != "10" or cell_text(cell_xml(updated, "E", number)[0]) != "8":
    fail("半径或本体伤害不对")

source_xml = parts[part_name].decode("utf-8")
new_rows = [updated if row_number(item) == number else item for item in rows_of(source_xml)]
match = re.search(r"(<(?:[\w]+:)?sheetData>)([\s\S]*)(</(?:[\w]+:)?sheetData>)", source_xml)
sheet_out = source_xml[:match.start(2)] + "".join(new_rows) + source_xml[match.end(2):]
changed = {part_name: sheet_out.encode("utf-8")}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != [part_name]:
        fail(f"Unexpected parts: {different}")
    before = []
    after = []
    counts = {}
    for title, name in status_parts:
        page = [code for code, _number, _row in sheet_ids(dst.read(name).decode("utf-8"))]
        counts[title] = len(page)
        after.extend(page)
        before.extend(code for code, _number, _row in sheet_ids(parts[name].decode("utf-8")))
    if before != after or len(after) != len(set(after)):
        fail("编号或行数变化")
    written = [item for item in rows_of(dst.read(part_name).decode("utf-8")) if row_number(item) == number][0]
    note = cell_text(cell_xml(written, "DV", number)[0])
    if "待美术与代码落实" not in note or "burnt_grass.png" in note:
        fail("备注没有待落实标记，或写了参考贴图路径")
    if "相对首次接触的第0、20、40至180tick" not in note:
        fail("首段提前后的360条件不完整")

summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "sheet": "待验收",
    "row": number,
    "counts": counts,
}
(batch / "灰烬-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"baseline": baseline, "candidate": summary["candidateSha256"], "row": number}, ensure_ascii=False))
