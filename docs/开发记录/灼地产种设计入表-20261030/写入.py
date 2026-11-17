# -*- coding: utf-8 -*-
"""Update only fire_scorch_field on 待制作 and add its second seed production."""
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
TARGET = "fire_scorch_field"

NEW_N = (
    "灼地是地面产种术式。在合法地表形成固定火场，敌人停留在地表火场中积累火种，供玩家之后选择引燃或引爆。"
    "原有持续2点伤害、每20tick一次、120tick寿命不再适用。\n"
    "基础半径2格，持续100tick（5秒）。只有接触火场实际低矮判定体积的合法敌对目标受影响，不自动覆盖整根竖直柱。"
    "单独使用时默认脚前合法地表；装配地表定位后可投到远处。火场位置固定，不被弹道改成飞行火场。"
    "保留地面定位、合法表面、固定区域，并继续作为领域载体。不放置原版火或岩浆，不蔓延、不破坏地形、不烧掉落物。"
    "火场到期清理。留在目标身上的其他独立燃烧按各自生命周期运行。已经挂上的火种遵循底座正常规则，不因离场清除。\n"
    "同一次灼地对同一合法敌对目标的首次接触：造成1点火伤，并增加基础20点火种。这份首次接触奖励在同一次灼地、同一目标只给一次，反复进出不能再刷20点。"
    "每次真实重新释放，其首次接触奖励仍可正常生效。\n"
    "后续持续接触每20tick增加基础15点火种，不再造成直接伤害。后续周期产种是独立资源效果，不依赖造成伤害，不制造伤害命中，"
    "不因此重复触发种火、引燃或引爆。重新进入不重置产种计时。离开火场停止获得火种，离场错过的周期不追补。"
    "周期计时不能被进出或重叠火场切换绕过。\n"
    "目标在火场生成时即接触且一直停留：第0tick首次20，第20、40、60、80tick各15，第100tick结束不额外结算。"
    "没有炉心增幅、也没有其他生产和消费时，总计80点火种、1点直接伤害；停留到第40tick时已有50点火种。"
    "80与50是该条件下的资源产量，不是任何环境下的剩余余额保证。\n"
    "同一施法者的多个灼地重叠时，对同一目标的持续产种只取一份，不按火场数量倍增。\n"
    "正在燃烧的目标也可以获得火种，直接加入唯一共享余额。现有存活燃烧层逐段消耗，多层加快消耗。"
    "不复制私有燃料，不刷新燃烧层已有的启动倍率或时长，不设燃烧时禁止补种或禁止引爆的锁。\n"
    "产种量接受现有炉心产种倍率和火种上限100，沿用底座正常取整规则。\n"
    "与幽冥或其他不同来源的产种如何叠加，此次未定。本次已确定的重叠限制只针对同一施法者的多个灼地。\n"
    "灵魂火地带组合仍是地表定位、幽冥、灼地，不另建组合行。灼地作为领域载体。幽冥把火场表现为蓝火；"
    "蓝火不改变伤害归属或免疫，不给减速，不放置灵魂沙或原版灵魂火方块。费用、冷却、魔力、复杂度、装配消耗、等级上限和等级成长仍未定。"
)
NEW_O = (
    "贴地火舌。每次产种时，火星从地面向目标身上汇聚，表现积火。"
    "不采用持续伤害命中的闪烁或受击反馈来假装周期产种在扣血。装上幽冥后，火场表现为蓝色魂火。"
)
DV_APPEND = (
    "2026-10-30用户确认：“嗯按照这版写入待制作表”。灼地改为地面产种。"
    "基础伤害改为1，只在同一次灼地、同一合法敌对目标首次接触时结算一次，没有伤害周期。"
    "场地半径仍为2。场地寿命改为100tick。旧的持续2点伤害、每20tick伤害、120tick寿命和场地维持周期20不再适用；"
    "20tick只是后续产种周期，不留在场地维持周期。"
    "首条资源是首次接触基础增加20点火种，写在本行DW:EN。第二条及以后是火场第20、40、60、80tick各基础增加15点火种，"
    "写在资源状态明细配置序号2，第100tick不额外结算。"
    "触发时机列表没有能准确表达地表首次接触或停留接触周期的选项，两处触发时机留空；"
    "不填命中成功、燃烧结算、首次带资源接触或领域周期。"
    "作用对象使用现有“领域内敌对活物”，低矮体积和敌对限制写在功能描述里。"
    "费用、冷却、魔力、复杂度、装配消耗、等级上限、等级成长，以及与幽冥或其他不同来源产种的叠加细则仍未定。"
    "无炉心且无其他生产消费时，一直停留到结束为80点火种加1点直接伤害，停留到40tick为50点火种；这是产量示例，不是剩余余额保证。"
    "本条只录入已确认需求，保持待制作，不领取。"
)
MAIN_EJ = "同一施法者、同一次真实释放、同一合法敌对目标只记首次接触一次。反复进出不再给20。只有低矮体积内的接触才算，不打整根竖直柱。"
MAIN_EK = "基础20，再按现有炉心产种倍率与上限100沿用底座取整，直接加入唯一共享余额。同时有1点火伤，但产种不依赖伤害命中，不因此触发种火、引燃或引爆。"
MAIN_EM = "首条：首次接触基础+20。周期+15只在资源状态明细序号2，不在本行重复。"
DETAIL_P = "目标仍接触该施法者灼地的低矮体积。离开即停。重新进入不重置计时，错过的周期不追补。同一施法者多块重叠只取一份。"
DETAIL_Q = "按火场自身第20、40、60、80tick各基础增加15。第100tick结束不额外结算。不造成直接伤害，不制造伤害命中，不重复触发种火、引燃或引爆。不包含首次接触的20。新的真实释放另计首次接触。炉心倍率与上限100沿用底座取整。"
DETAIL_S = "灼地第二条及以后的周期产种。不重复主行首次接触。"


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


def remove_cell(row, column, number):
    found = cell_xml(row, column, number)
    if not found:
        fail(f"Missing {column}{number}")
    _, start, end = found
    return row[:start] + row[end:]


def str_cell(prefix, column, number, text, style):
    escaped = html.escape(text, quote=False)
    space = ' xml:space="preserve"' if ("\n" in text or text[:1].isspace() or text[-1:].isspace()) else ""
    return f'<{prefix}c r="{column}{number}" s="{style}" t="str"><{prefix}v{space}>{escaped}</{prefix}v></{prefix}c>'


def num_cell(prefix, column, number, value, style="15"):
    return f'<{prefix}c r="{column}{number}" s="{style}" t="n"><{prefix}v>{value}</{prefix}v></{prefix}c>'


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


def old_damage_cite(text):
    if not text or "灼地" not in text:
        return False
    return any(token in text for token in ("每20tick造成", "120tick", "基础2点火伤", "持续2点"))


live_bytes = live.read_bytes()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"灼地产种-术式制作表-{stamp}-1.xlsx"
output = batch / "candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}

status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
locations = []
for title, name in status_parts.items():
    for code, number, _row in sheet_ids(parts[name].decode("utf-8")):
        if code == TARGET:
            locations.append((title, number))
if locations != [("待制作", 4)]:
    fail(f"灼地位置变化，停止覆盖: {locations}")

sheet2 = parts["xl/worksheets/sheet2.xml"].decode("utf-8")
rows = rows_of(sheet2)
target = None
for row in rows:
    if row_number(row) == 4:
        target = row
        break
if cell_text(cell_xml(target, "A", 4)[0]) != "灼地":
    fail("待制作第4行名称不是灼地")
if cell_text(cell_xml(target, "E", 4)[0]) != "2" or cell_text(cell_xml(target, "BV", 4)[0]) != "20":
    fail("旧伤害或旧周期已变，停止覆盖")
if cell_text(cell_xml(target, "BW", 4)[0]) != "120" or cell_text(cell_xml(target, "AS", 4)[0]) != "2":
    fail("旧寿命或半径已变，停止覆盖")
old_n = cell_text(cell_xml(target, "N", 4)[0])
old_dv = cell_text(cell_xml(target, "DV", 4)[0])
if "每20tick造成一次基础2点火伤" not in old_n or not old_dv.startswith("魂焰的40tick"):
    fail("待制作需求已不是旧持续伤害稿，停止覆盖")
if "2026-10-30用户确认" in old_dv:
    fail("本次确认已经写过")
outside = []
for title, name in status_parts.items():
    xml = parts[name].decode("utf-8")
    for row in rows_of(xml):
        number = row_number(row)
        for match in re.finditer(r'<(?:[\w]+:)?c r="([A-Z]+)(\d+)"[\s\S]*?</(?:[\w]+:)?c>', row):
            text = cell_text(match.group(0))
            if old_damage_cite(text) and not (title == "待制作" and number == 4 and match.group(1) in ("N", "DV")):
                outside.append(f"{title}:{match.group(1)}{number}")
if outside:
    fail(f"其他单元格仍把灼地写成旧持续伤害: {outside}")

updated = set_cell(target, "E", 4, "1")
updated = set_cell(updated, "N", 4, NEW_N)
updated = set_cell(updated, "O", 4, NEW_O)
updated = set_cell(updated, "BW", 4, "100")
updated = set_cell(updated, "DV", 4, old_dv + "\n" + DV_APPEND)
updated = remove_cell(updated, "BV", 4)
extra = "".join([
    str_cell("", "DW", 4, "是", "15"),
    str_cell("", "DX", 4, "火种", "15"),
    str_cell("", "DZ", 4, "增加", "15"),
    str_cell("", "EA", 4, "领域内敌对活物", "15"),
    num_cell("", "EC", 4, "20"),
    str_cell("", "EE", 4, "不适用", "15"),
    str_cell("", "EI", 4, "每目标每次施法", "15"),
    str_cell("", "EJ", 4, MAIN_EJ, "16"),
    str_cell("", "EK", 4, MAIN_EK, "16"),
    str_cell("", "EL", 4, "N/DV", "15"),
    str_cell("", "EM", 4, MAIN_EM, "16"),
])
if not updated.endswith("</row>"):
    fail("待制作行没有正常结束")
updated = updated[:-6] + extra + "</row>"
if cell_xml(updated, "BV", 4) or cell_xml(updated, "EB", 4) or cell_xml(updated, "EG", 4):
    fail("旧周期或周期触发被写进主行")
if cell_text(cell_xml(updated, "E", 4)[0]) != "1" or cell_text(cell_xml(updated, "EC", 4)[0]) != "20":
    fail("主行数值没有写上")
if cell_text(cell_xml(updated, "B", 4)[0]) != TARGET or cell_text(cell_xml(updated, "AS", 4)[0]) != "2":
    fail("编号或半径被改动")
new_rows = []
for row in rows:
    new_rows.append(updated if row_number(row) == 4 else row)
match = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", sheet2)
sheet2_out = sheet2[:match.start(2)] + "".join(new_rows) + sheet2[match.end(2):]

sheet6 = parts["xl/worksheets/sheet6.xml"].decode("utf-8")
if TARGET in sheet6:
    fail("资源状态明细已有灼地，停止追加")
if sheet6.count('ref="A1:T6"') != 1:
    fail("资源明细范围不是A1:T6")
detail_rows = rows_of(sheet6)
if [row_number(row) for row in detail_rows] != [1, 2, 3, 4, 5, 6]:
    fail("资源明细行数已变")
if cell_text(cell_xml(detail_rows[2], "A", 3)[0]) != "water_mist":
    fail("资源明细原行已变")
detail = (
    '<ns0:row r="7" ht="96" customHeight="1">'
    + str_cell("ns0:", "A", 7, TARGET, "15")
    + num_cell("ns0:", "B", 7, "2")
    + str_cell("ns0:", "C", 7, "是", "15")
    + str_cell("ns0:", "D", 7, "火种", "15")
    + str_cell("ns0:", "F", 7, "增加", "15")
    + str_cell("ns0:", "G", 7, "领域内敌对活物", "15")
    + num_cell("ns0:", "I", 7, "15")
    + str_cell("ns0:", "K", 7, "不适用", "15")
    + num_cell("ns0:", "M", 7, "20")
    + str_cell("ns0:", "O", 7, "每次触发", "15")
    + str_cell("ns0:", "P", 7, DETAIL_P, "16")
    + str_cell("ns0:", "Q", 7, DETAIL_Q, "16")
    + str_cell("ns0:", "R", 7, "N/DV", "15")
    + str_cell("ns0:", "S", 7, DETAIL_S, "16")
    + "</ns0:row>"
)
if cell_xml(detail, "H", 7) or "首次接触奖励" in detail:
    fail("明细重复了首条或填了触发时机")
sheet6_out = sheet6.replace('ref="A1:T6"', 'ref="A1:T7"', 1)
close = sheet6_out.rfind("</ns0:sheetData>")
if close < 0:
    fail("资源明细没有sheetData结束")
sheet6_out = sheet6_out[:close] + detail + sheet6_out[close:]

changed = {
    "xl/worksheets/sheet2.xml": sheet2_out.encode("utf-8"),
    "xl/worksheets/sheet6.xml": sheet6_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet2.xml", "xl/worksheets/sheet6.xml"]:
        fail(f"Unexpected parts: {different}")
    pending = dst.read("xl/worksheets/sheet2.xml").decode("utf-8")
    detail_xml = dst.read("xl/worksheets/sheet6.xml").decode("utf-8")
    ids = []
    counts = {}
    for title, name in status_parts.items():
        xml = dst.read(name).decode("utf-8")
        page = [code for code, _number, _row in sheet_ids(xml)]
        counts[title] = len(page)
        ids.extend(page)
    if counts != {"已完成": 40, "待制作": 13, "制作中": 3, "待验收": 3}:
        fail(f"Counts {counts}")
    if len(ids) != 59 or len(set(ids)) != 59 or ids.count(TARGET) != 1:
        fail("状态页编号不再唯一")
    row = [item for item in rows_of(pending) if row_number(item) == 4][0]
    checks = {
        "A": "灼地", "B": TARGET, "E": "1", "AS": "2", "BW": "100",
        "DW": "是", "DX": "火种", "DZ": "增加", "EA": "领域内敌对活物",
        "EC": "20", "EE": "不适用", "EI": "每目标每次施法", "EL": "N/DV",
    }
    for column, expected in checks.items():
        if cell_text(cell_xml(row, column, 4)[0]) != expected:
            fail(f"{column} is {cell_text(cell_xml(row, column, 4)[0])}")
    if cell_xml(row, "BV", 4) or cell_xml(row, "EB", 4) or cell_xml(row, "EG", 4) or cell_xml(row, "EH", 4):
        fail("主行仍有旧周期或触发")
    written_n = cell_text(cell_xml(row, "N", 4)[0])
    written_o = cell_text(cell_xml(row, "O", 4)[0])
    written_dv = cell_text(cell_xml(row, "DV", 4)[0])
    if written_n != NEW_N or written_o != NEW_O or not written_dv.endswith(DV_APPEND):
        fail("N/O/DV mismatch")
    if "每20tick造成一次基础2点火伤" in written_n or "持续120tick，每20tick" in written_n:
        fail("旧持续伤害句子仍在")
    detail_row = [item for item in rows_of(detail_xml) if row_number(item) == 7][0]
    detail_checks = {
        "A": TARGET, "B": "2", "C": "是", "D": "火种", "F": "增加",
        "G": "领域内敌对活物", "I": "15", "K": "不适用", "M": "20", "O": "每次触发",
    }
    for column, expected in detail_checks.items():
        if cell_text(cell_xml(detail_row, column, 7)[0]) != expected:
            fail(f"明细{column}不对")
    if cell_xml(detail_row, "H", 7) or cell_xml(detail_row, "N", 7):
        fail("明细填了触发或延迟")
    if detail_xml.count(TARGET) != 1:
        fail("明细出现多条灼地")
    neighbors = [code for code, _number, _row in sheet_ids(pending)]
    if neighbors[0] != "fire_flame_trail" or neighbors[1] != TARGET:
        fail(f"待制作顺序变化: {neighbors[:3]}")

summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "sheet": "待制作",
    "row": 4,
    "detailSheet": "资源状态明细",
    "detailRow": 7,
    "counts": counts,
}
(batch / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"baseline": baseline, "candidate": summary["candidateSha256"], "row": 4, "detailRow": 7}, ensure_ascii=False))
