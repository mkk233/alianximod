# -*- coding: utf-8 -*-
"""Move fire_liaoyuan from 待验收 back to 制作中 and restore the merged requirement."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
here = root / "docs" / "开发记录" / "燎原伤害定稿入表-20261031"
live = root / "docs" / "术式制作表.xlsx"
TARGET = "fire_liaoyuan"
OLD_N = "铺开地表火层。踩中后按段燃烧，每段消耗10点火种，燃料耗尽或到达期限就结束"
NEW_N = (
    "燎原是持续燃烧侧的终极术式。按2026-10-31已批准的伤害翻倍、半径10和灰烬覆盖返修。"
    "触发后在释放原点周围形成视觉半径10格、持续200tick的地表火场。200tick从火场生成起算，包含现有约12tick的向外展开，完全铺开后不重新计时。"
    "默认原点为施法者，落点组合沿用链上原点。\n"
    "实际接触区是半宽10格的方形贴地低矮层，水平约20×20格，随各处真实地表高度贴合。"
    "用实体完整碰撞盒相交，不只查脚底，不用圆盘裁掉方形角落。每tick检查接触，生成时立即检查。"
    "服务端已展开范围与客户端展开进度一致。前沿覆盖到的地表立即参与接触，不等待展开结束，也不提前攻击尚未覆盖的最终整片。"
    "接触检测和10tick本体伤害周期分开。\n"
    "本体基础火伤8点，接受一次炉心加成，每次8×(1+F)。过载满热F=0.60时为12.8点。"
    "目标被已展开接触体首次碰到时立刻结算这一下，它就是该目标本体周期的第1次，同tick不再另叠展开伤害和场地首伤。"
    "之后仍接触则每10tick结算。重复进出不重置同目标本体计时，不能刷首次伤害。离开火场停止本体伤害。"
    "火场在第200tick结束时不额外多打一段。无火种时仍有本体伤害。外围目标要等前沿到达才首次命中，本体次数可能少于20。\n"
    "有可用火种且本次火场允许启动时，首次碰到同时启动附着燃烧并结算首段。首段就是原有各段的第一段，不另加一整段。"
    "启动观察n=floor(当时余额/10)和炉心F，发生在扣这一段燃料之前。足额段伤害为4×(1+0.1n+F)。"
    "公式里的F已经是炉心增幅，不再把整段额外乘1+F。n不在扣燃料后重算。"
    "每个完整段向共享余额请求10点，周期20tick，首次延迟0。实付不足10时按实付/10缩放该段一次，无燃料则该段不造成资源伤害。"
    "最长有效期仍是(3+n)×20tick。不预扣全部，不复制私有燃料。不同存活燃烧争用唯一余额。"
    "补种不刷新已锁定倍率和期限。燃料耗尽或到期结束。火场结束不强制取消尚未结束的附着燃烧。离场后附着燃烧继续按自己的周期消耗并造成伤害。\n"
    "有火种时，场内本体伤害与附着燃烧同时存在，不再用附着燃烧替代并取消本体。"
    "同一次真实释放的复制、重复接触和每帧前沿相交，不重复挂同一层，也不重复首次伤害。"
    "每目标最多8条计划，满额在扣火种之前拒绝。同一tick到期的各层按需求比例分配。"
    "本体和附着燃烧同tick到期时两部分都要兑现。被无敌帧吞掉的一份不算已兑现，不修改全局无敌帧。\n"
    "相对首次接触，满10段燃烧可在第0、20、40至180tick完成，仍只消耗100点，不变成11段。"
    "若目标获得完整20次本体命中，且过载起手满热F=0.60、初始100点火种、期间不补种、无其他燃烧抢资源、忽略减伤和免疫，"
    "本体8×1.60×20＝256，附着10.4×10＝104，合计360点理论火伤。展开首伤含在这20次里，不另加12.8。"
    "刚开启过载而未满热，或本体不足20次，不能套360。这是设计计算，不是实测。\n"
    "火焰不蔓延，不放置原版火方块，不烧毁或替换方块，不破坏掉落物。无火种者离场后不能再给一段原版点燃。"
    "火场尺寸可按已有能量规则变化，火种段数和资源伤害不再被放大。首版实际魔力60、冷却600tick。不产生火种。"
    "现有点火额外2、热光束每次接受命中1、火球每个实际目标3不变。不改燎里和焚天。"
)
NEW_O = (
    "保留现有从中心向外约12tick展开的动画形态和节奏，不改成瞬间铺满。"
    "展开前沿扫过后，地表留下连续的深灰、炭黑与灰烬颗粒，覆盖有效区域各处地表，贴合真实地形，不是浮在空中的大圆片。"
    "原来稀疏的火焰斑点不再作为主体。覆盖层上零散暗红余烬和橙黄火星，疏密与明灭错开，少量火星短暂升起再熄灭。"
    "外缘保持近圆形展开观感，用破碎灰烬边缘、火浪和火星过渡弱化方形轮廓。方形接触区的边角也要有可见灰烬。"
    "结束时火星减弱、灰烬淡去，原方块外观恢复，不留焦土或被改动的方块。"
    "被消耗火种的目标仍可有随档数变化的附着火舌。灰烬正式贴图尚未确认，草稿待预览，不把参考模组贴图或新路径写进本行。"
)
DV_ADD = (
    "[设计反馈 2026-10-31] 用户原话：“按这个版本开始修”，并强调“别忘了伤害翻倍”。"
    "8点本体、4×(1+0.1n+F)燃烧、半径10和灰烬覆盖都已批准，不再是待决定项。本项从待验收退回制作中，返修进行中。"
    "当前N/O改为合并后的详细需求。旧的一句话展示和三张贴图路径留在 docs/开发记录/燎原伤害定稿入表-20261031/实施-退回-现行快照.json。"
    "美术草稿另做，尚未预览确认，本轮不写新资源路径，也不改游戏。"
    "本批设计校验2/2、实现校验3/3已经用完。这次退回不追加、不重置额度，也不把额度用完写成已经复审通过。"
    "记录里既有的追加轮次保持原样，这次不算新批次。\n"
    "制作中 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
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


def cells_of(row, number):
    found = {}
    for match in re.finditer(rf'<(?:[\w]+:)?c r="([A-Z]+){number}"', row):
        text = cell_text(cell_xml(row, match.group(1), number)[0])
        if text is not None:
            found[match.group(1)] = text
    return found


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


def renumber(row, old, new):
    updated = re.sub(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row, count=1)
    updated = re.sub(rf'r="([A-Z]+){old}"', rf'r="\g<1>{new}"', updated)
    if row_number(updated) != new:
        fail(f"Row {old} did not become {new}")
    return updated


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
backup = here / "备份" / f"实施-退回-术式制作表-{stamp}-1.xlsx"
output = here / "实施-退回-candidate-preserved.xlsx"
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
        if code == TARGET:
            locations.append((title, number))
if locations != [("待验收", 4)]:
    fail(f"燎原位置变化: {locations}")
if "fire_liaoyuan" in parts["xl/worksheets/sheet6.xml"].decode("utf-8"):
    fail("资源明细已有燎原，本脚本不搬明细")

sheet3 = parts["xl/worksheets/sheet3.xml"].decode("utf-8")
sheet4 = parts["xl/worksheets/sheet4.xml"].decode("utf-8")
rows3 = rows_of(sheet3)
rows4 = rows_of(sheet4)
making = sheet_ids(sheet3)
pending = sheet_ids(sheet4)
if [code for code, _number, _row in making] != ["fire_heat_up", "fire_burn", "fire_cool"]:
    fail(f"制作中身份变化: {[code for code, _number, _row in making]}")
if [code for code, _number, _row in pending] != ["fire_fentian", TARGET, "fire_liaoli", "fire_scorch_field"]:
    fail(f"待验收身份变化: {[code for code, _number, _row in pending]}")
source = pending[1][2]
number = 4
if cell_text(cell_xml(source, "N", number)[0]) != OLD_N:
    fail("展示句已变，不能按这次退回覆盖")
if "按这个版本开始修" in cell_text(cell_xml(source, "DV", number)[0]):
    fail("本次返修授权已经写过")
expected_numbers = {"E": "8", "AS": "10", "BV": "10", "BW": "200", "EC": "10", "EG": "20", "EH": "0", "EB": "燃烧结算"}
for column, expected in expected_numbers.items():
    if cell_text(cell_xml(source, column, number)[0]) != expected:
        fail(f"{column} 是 {cell_text(cell_xml(source, column, number)[0])}，不是已批准的 {expected}")
if "<f" in source.lower():
    fail("燎原行含公式")
before_cells = cells_of(source, number)
(here / "实施-退回-现行快照.json").write_text(json.dumps({
    "baselineSha256": baseline,
    "sheet": "待验收",
    "row": number,
    "resourceDetail": None,
    "cells": before_cells,
    "mergedFrom": "docs/开发记录/术式批量制作/火系六式-20261029/liaoyuan-visual-transition-snapshot.json",
    "quota": {"design": "2/2", "implementation": "3/3", "reset": False},
}, ensure_ascii=False, indent=2), encoding="utf-8")

edited = set_cell(source, "N", number, NEW_N)
edited = set_cell(edited, "O", number, NEW_O)
edited = set_cell(edited, "DV", number, before_cells["DV"] + "\n" + DV_ADD)
for column, expected in expected_numbers.items():
    if cell_text(cell_xml(edited, column, number)[0]) != expected:
        fail(f"迁移时改动了 {column}")
moved = renumber(edited, 4, 6)
sheet3_out = sheet3.replace("</sheetData>", moved + "</sheetData>", 1)
sheet4_out_rows = [rows4[0], rows4[1], rows4[2], renumber(rows4[4], 5, 4), renumber(rows4[5], 6, 5)]
match = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", sheet4)
sheet4_out = sheet4[:match.start(2)] + "".join(sheet4_out_rows) + sheet4[match.end(2):]
if sheet3_out.count('ref="A1:EN5"') != 1 or sheet4_out.count('ref="A1:EN6"') != 1:
    fail("范围锚点缺失")
sheet3_out = sheet3_out.replace('ref="A1:EN5"', 'ref="A1:EN6"', 1)
sheet4_out = sheet4_out.replace('ref="A1:EN6"', 'ref="A1:EN5"', 1)
changed = {
    "xl/worksheets/sheet3.xml": sheet3_out.encode("utf-8"),
    "xl/worksheets/sheet4.xml": sheet4_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"]:
        fail(f"Unexpected parts: {different}")
    counts = {}
    ids = []
    for title, name in status_parts:
        page = [code for code, _number, _row in sheet_ids(dst.read(name).decode("utf-8"))]
        counts[title] = len(page)
        ids.extend(page)
    if counts != {"已完成": 40, "待制作": 13, "制作中": 4, "待验收": 3} or len(ids) != 60 or len(set(ids)) != 60:
        fail(f"Counts {counts} unique {len(set(ids))}")
    making_out = sheet_ids(dst.read("xl/worksheets/sheet3.xml").decode("utf-8"))
    pending_out = sheet_ids(dst.read("xl/worksheets/sheet4.xml").decode("utf-8"))
    if [code for code, _number, _row in making_out] != ["fire_heat_up", "fire_burn", "fire_cool", TARGET]:
        fail("制作中顺序不对")
    if [code for code, _number, _row in pending_out] != ["fire_fentian", "fire_liaoli", "fire_scorch_field"]:
        fail("待验收还留着燎原或邻居错位")
    row = making_out[3][2]
    if cell_text(cell_xml(row, "N", 6)[0]) != NEW_N or "灰烬" not in cell_text(cell_xml(row, "O", 6)[0]):
        fail("详细需求没有恢复")
    if "assets/" in cell_text(cell_xml(row, "O", 6)[0]):
        fail("O仍是旧资源路径")
    if TARGET in dst.read("xl/worksheets/sheet4.xml").decode("utf-8"):
        fail("待验收仍有燎原编号")

summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "from": "待验收第4行",
    "to": "制作中第6行",
    "counts": counts,
}
(here / "实施-退回-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"baseline": baseline, "candidate": summary["candidateSha256"], "to": "制作中第6行"}, ensure_ascii=False))
