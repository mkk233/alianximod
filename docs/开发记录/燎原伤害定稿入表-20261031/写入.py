# -*- coding: utf-8 -*-
"""Write the confirmed 燎原 damage spec into the current status row."""
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
NEW_EJ = (
    "有效目标接触火场且余额大于0才挂附着燃烧。无火种不影响本体伤害。"
    "持续接触不每tick重复挂同一火场的附着燃烧。资源燃烧不要求独占。"
    "同次真实释放不重复挂层。满8条计划在扣款前拒绝。"
)
NEW_EK = (
    "附着燃烧完整段伤害4×(1+0.1n+F)。n和F只在启动时取，F不再把整段额外乘1+F，扣燃料后不重算n。"
    "完整段请求10点，周期20tick。同tick按需求比例分配不足燃料，实付不足按实付/10缩放该段一次。"
    "最长(3+n)×20tick，燃料尽则提前结束。不预扣全部，不复制私有燃料。"
)
DV_APPEND = (
    "2026-10-31已确认的新设计，待代码落实。本轮只改制作表规格，没有改游戏，也没有实测通过。"
    "当前功能描述和特效路径仍是此前实现展示。功能描述里的按段燃烧和每段10点火种是已上表的展示句，不是这次8点本体伤害的说明。"
    "下面的新参数是后续改代码的依据。完整规格见 docs/开发记录/燎原伤害定稿入表-20261031/任务.md。\n"
    "基础伤害列改为8，表示火场本体基础火伤，不是360。场地维持周期改为10，只表示本体伤害周期："
    "每10tick对仍接触的目标结算一次，首次有效接触立即结算。火种消费仍是燃烧结算、数值10、周期20tick，不能和本体的10tick混填。"
    "火场寿命仍为200tick。同一目标站满全程，本体在第0、10、20至190tick结算，共20次，结束时不额外多打一段。"
    "本体火伤接受一次炉心加成，每次8×(1+F)；过载满热F=0.60时为12.8点。\n"
    "附着燃烧完整段为4×(1+0.1n+F)。n是启动时floor(目标火种余额/10)，F是启动时炉心加成快照。"
    "公式里的F已经是炉心增幅，不再把整段额外乘1+F。n不在每次扣燃料后重算。"
    "每20tick结算一次，每完整段仍请求10点火种；实付不足10时按实付/10缩放该段一次。"
    "最长有效期仍是(3+n)×20tick，实际段数还受燃料限制。不整笔预付，不复制私有燃料，不同存活燃烧争用唯一余额。"
    "补种不刷新已锁定倍率和期限。燃料耗尽或到期结束。\n"
    "有火种时，场内本体伤害与附着燃烧同时存在。无火种时仍有本体伤害。"
    "离开火场停止本体伤害，已挂上的火种燃烧继续按自己的周期消耗余额并造成伤害。"
    "火场结束也不强制取消尚未结束的独立附着燃烧。不蔓延，不破坏地形。"
    "场内无火种者离场后，不能再额外给一段离场仍持续的原版点燃。\n"
    "视觉半径约6格，近似圆形火焰。实际判定是对应大小的方形水平覆盖，随各处地形高度贴合的低矮接触体。"
    "用实体完整碰撞盒与火场接触体相交，不只检查脚底单点，不用圆盘裁掉方形角落。"
    "每tick检查接触，生成时立即检查。接触检测频率与10tick本体伤害周期分开。"
    "重复进出不重置同目标本体伤害计时，不能刷首次伤害。持续接触不每tick重复挂同一火场的附着燃烧。\n"
    "本体伤害与附着燃烧同tick到期时，两部分都要兑现。被无敌帧吞掉的一份不算已兑现。"
    "不靠修改全局无敌帧解决。这是待落实核对，本轮没有检查或修复代码。\n"
    "360点只在以下条件同时成立时成立：目标初始100点火种，即10档；施法时过载已满热F=0.60；"
    "目标从生成起一直接触火场200tick；期间不补种、无其他燃烧争抢余额；忽略目标减伤与免疫，并假定每次伤害正确兑现。"
    "本体8×1.60×20＝256。附着燃烧4×(1+0.1×10+0.60)＝10.4/段，100÷10＝10段，共104。"
    "附着层首段在第20tick，末段在第200tick，末段可在火场结束时由独立附着层正常完成。"
    "总计256+104＝360点理论火伤，平均36点/秒。刚开启过载而未满热不能套这个数值。"
    "这是新设计计算，不是实测已通过。"
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
backup = batch / "备份" / f"燎原伤害-术式制作表-{stamp}-1.xlsx"
output = batch / "candidate-preserved.xlsx"
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
    fail(f"燎原位置不是待验收唯一行: {[(item[0], item[2]) for item in locations]}")
_title, part_name, number, row = locations[0]
if cell_text(cell_xml(row, "A", number)[0]) != "燎原" or cell_text(cell_xml(row, "B", number)[0]) != TARGET:
    fail("名称和编号不匹配")
if cell_text(cell_xml(row, "E", number)[0]) != "2" or cell_text(cell_xml(row, "BV", number)[0]) != "20":
    fail("基础伤害或本体周期已不是2和20，停止以免重复翻倍")
if cell_text(cell_xml(row, "EC", number)[0]) != "10" or cell_text(cell_xml(row, "EG", number)[0]) != "20":
    fail("火种段请求或燃烧周期已变")
if cell_text(cell_xml(row, "BW", number)[0]) != "200" or cell_text(cell_xml(row, "N", number)[0]) != OLD_N:
    fail("寿命或现行展示句已变")
old_dv = cell_text(cell_xml(row, "DV", number)[0])
if "2026-10-31已确认的新设计" in old_dv:
    fail("本次确认已经写过")
old_o = cell_text(cell_xml(row, "O", number)[0])
updated = set_cell(row, "E", number, "8")
updated = set_cell(updated, "BV", number, "10")
updated = set_cell(updated, "EJ", number, NEW_EJ)
updated = set_cell(updated, "EK", number, NEW_EK)
updated = set_cell(updated, "DV", number, old_dv + "\n" + DV_APPEND)
if cell_text(cell_xml(updated, "N", number)[0]) != OLD_N or cell_text(cell_xml(updated, "O", number)[0]) != old_o:
    fail("展示句或特效路径被改掉")
if cell_text(cell_xml(updated, "EC", number)[0]) != "10" or cell_text(cell_xml(updated, "EG", number)[0]) != "20":
    fail("火种周期被混填")
if cell_text(cell_xml(updated, "E", number)[0]) == "360":
    fail("基础伤害写成了360")

source_xml = parts[part_name].decode("utf-8")
new_rows = []
for item in rows_of(source_xml):
    new_rows.append(updated if row_number(item) == number else item)
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
    counts = {}
    ids = []
    for title, name in status_parts:
        page = [code for code, _number, _row in sheet_ids(dst.read(name).decode("utf-8"))]
        counts[title] = len(page)
        ids.extend(page)
    before_counts = {}
    before_ids = []
    for title, name in status_parts:
        page = [code for code, _number, _row in sheet_ids(parts[name].decode("utf-8"))]
        before_counts[title] = len(page)
        before_ids.extend(page)
    if counts != before_counts or ids != before_ids or len(ids) != len(set(ids)):
        fail(f"行数或编号变化 {before_counts} -> {counts}")
    pending = dst.read(part_name).decode("utf-8")
    written = [item for item in rows_of(pending) if row_number(item) == number][0]
    if cell_text(cell_xml(written, "E", number)[0]) != "8" or cell_text(cell_xml(written, "BV", number)[0]) != "10":
        fail("规格列没有写上")
    if "256+104＝360" not in cell_text(cell_xml(written, "DV", number)[0]):
        fail("360条件没有写上")
    if "4×(1+0.1n+F)" not in cell_text(cell_xml(written, "EK", number)[0]):
        fail("附着公式没有写上")
    if TARGET in dst.read("xl/worksheets/sheet6.xml").decode("utf-8"):
        fail("资源明细出现了新的燎原行")

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
(batch / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"baseline": baseline, "candidate": summary["candidateSha256"], "row": number}, ensure_ascii=False))
