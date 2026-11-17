# -*- coding: utf-8 -*-
"""Append the round-1 test result to six DV cells. Does not move rows or change N/O."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "术式批量制作" / "火系六式-20261029"
live = root / "docs" / "术式制作表.xlsx"
expected = "CE0AF918E0A99DC586232E76B156465B12AEDF8A5C0047CAD8268BEF6A3D899D"
review = "docs/开发记录/术式批量制作/火系六式-20261029/test-stage-review-r1.md"
wanted = [
    (3, "燎原", "fire_liaoyuan"),
    (4, "燎里", "fire_liaoli"),
    (5, "焚天", "fire_fentian"),
    (6, "升温", "fire_heat_up"),
    (7, "燃烧", "fire_burn"),
    (8, "冷却", "fire_cool"),
]
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
additions = {
    "fire_liaoyuan": (
        "[设计反馈 2026-10-29] 测试正式结果核对第1轮（1/2）决定留在制作中，不转换N/O。"
        "已执行功能子集通过，不是整项测试失败。未迁是因为缺六式适用的正式实现复核通过记录，"
        "以及共用底座R2/R3返修未追加复审；可见地表按外露碰撞顶面的解释只有作者反馈。"
        "火种来源缺失和尚未游戏实测不单独作为未迁原因。"
        "已测：注册200tick、魔力60、火种分档与20tick调度、100点13段每段4（F=0.30时4.6）、同施法者排他和一次消费、几何边界。"
        "覆盖缺口：真实地火每20tick的2点与附着互斥、权限先于消费、离场继续、规则复制份。"
        "下一步须用户明确授权后再补适用实现复核，并补共享批次一次消费与地火/附着接入。"
        "贴地、视觉持续和实体运行列用户实测。"
        "同日纠正：本式原始需求只消费火种、不产火种。正常玩法没有授予入口是准备限制，不是本轮离线计划或账本失败，也不授权新增来源。"
        f"详见 {review}。"
    ),
    "fire_liaoli": (
        "[设计反馈 2026-10-29] 测试正式结果核对第1轮（1/2）决定留在制作中，不转换N/O。"
        "已执行功能子集通过，不是整项测试失败。未迁是因为缺六式适用的正式实现复核通过记录，以及共用R2/R3返修未追加复审。"
        "公式和视觉几何通过不能代表整次剑技生命周期。火种来源缺失和尚未游戏实测不单独作为未迁原因。"
        "已测：持续/跟随准星/持剑、寿命40tick、魔力40、0.36×S线性火伤（100点36，F=0.30时46.8）、走廊和v6布局函数。"
        "覆盖缺口：物理6点与火伤分开结算、每目标一次、权限先于消费、松开/换剑/死亡收招、40tick不自动重发、规则复制收益。"
        "下一步须用户明确授权后再补适用实现复核，并补共享批次与持续输入接入。"
        "第一人称裁切和侧面厚度列用户实测。"
        "同日纠正：本式原始需求只消费火种、不产火种。正常玩法没有授予入口是准备限制，不是本轮离线公式失败，也不授权新增来源。"
        f"详见 {review}。"
    ),
    "fire_fentian": (
        "[设计反馈 2026-10-29] 测试正式结果核对第1轮（1/2）决定留在制作中，不转换N/O。"
        "已执行功能子集通过，不是整项测试失败。未迁是因为缺六式适用的正式实现复核通过记录，以及共用R2/R3返修未追加复审。"
        "字节码检查不是游戏内Mixin织入，也不能补签实现通过。火种来源缺失和尚未游戏实测不单独作为未迁原因。"
        "已测：寿命40tick、魔力100、0/9/54/100计划、余量尾段、4tick间隔、100点10段锁定指数、炉心同层一次、目标排他。"
        "覆盖缺口：8格内真实存活和权限筛选、已有燃烧不先扣火种、离场继续、死亡终止、4tick实际伤害和防御、规则复制收益。"
        "下一步须用户明确授权后再补适用实现复核，并补共享批次与实体序列。有火种分支的游戏准备方式另行解决，不擅增来源。"
        "同日纠正：本式原始需求只消费火种、不产火种。正常玩法没有授予入口是准备限制，不是本轮离线计划失败。"
        f"详见 {review}。"
    ),
    "fire_heat_up": (
        "[设计反馈 2026-10-29] 测试正式结果核对第1轮（1/2）决定留在制作中，不转换N/O。"
        "已执行功能子集通过，不是整项测试失败。未迁是因为缺六式适用的正式实现复核通过记录；"
        "炉心扣费和失败恢复只有作者返修反馈，玩家事务未覆盖。不是因为没有火种生产入口。"
        "已测：自身状态/动作、魔力20、装配1、无炉心进普通、已有普通或过载拒绝、150拍到F=0.30、产率1.12–1.60、真实混链拒绝。"
        "覆盖缺口：真实扣20魔力、500tick独立冷却、安装失败退款且状态不变，以及生命/护甲/韧性、治疗和水冰承伤、抗性、牛奶、保存/死亡/登录。"
        "下一步须用户明确授权后再补适用实现复核，并用真实玩家事务测正常、魔力不足和安装失败恢复。"
        "属性和生命周期列真实Forge或用户实测。"
        "同日纠正：本式不依赖火种余额。"
        f"详见 {review}。"
    ),
    "fire_burn": (
        "[设计反馈 2026-10-29] 测试正式结果核对第1轮（1/2）决定留在制作中，不转换N/O。"
        "已执行功能子集通过，不是整项测试失败。未迁是因为缺六式适用的正式实现复核通过记录；炉心原子返修未追加复审。"
        "安装规划通过不证明玩家升级失败能完整回滚。不是因为没有火种生产入口。"
        "已测：直接过载、普通升级保留F=0.30、已过载拒绝、不能降普通、再100拍到0.60、产率2.2、真实混链拒绝。"
        "覆盖缺口：升级只装一份属性、当前生命只向下钳、真实扣费和独立冷却、失败时效果/热量/魔力恢复，以及水冰/治疗/抗性和存档事件。"
        "下一步须用户明确授权后再补适用实现复核，并补普通升级、已过载拒绝和安装失败恢复的真实事务。"
        "同日纠正：本式不依赖火种余额。"
        f"详见 {review}。"
    ),
    "fire_cool": (
        "[设计反馈 2026-10-29] 测试正式结果核对第1轮（1/2）决定留在制作中，不转换N/O。"
        "已执行功能子集通过，不是整项测试失败。未迁是因为缺六式适用的正式实现复核通过记录；"
        "共用伤害R2/R3和炉心事务返修未追加复审。空玩家拒绝不证明成功cool只喷一次蒸汽。不是因为没有火种生产入口。"
        "已测：零魔力/动作注册、普通和过载主动清热量、无炉心拒绝、真实混链拒绝。"
        "覆盖缺口：成功cool一次蒸汽、被动移除或死亡不喷、半径3敌我筛选和每目标固定2点、属性恢复不加当前生命、另外两式冷却保留。"
        "击退距离和碰撞列真实运行验证，不以缺游戏画面单独拦迁。"
        "下一步须用户明确授权后再补适用实现复核，并补成功、无炉心和非主动结束的事务。"
        "同日纠正：本式不依赖火种余额。"
        f"详见 {review}。"
    ),
}


def fail(message):
    raise SystemExit(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()


def cell_xml(row_xml, column, row_number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{row_number}"', row_xml))
    if len(matches) != 1:
        fail(f"Cell {column}{row_number} count is {len(matches)}")
    start = matches[0].start()
    opener_end = row_xml.find(">", start)
    if opener_end < 0:
        fail(f"Cell {column}{row_number} has no tag end")
    if row_xml[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</(?:[\w]+:)?c>", row_xml[opener_end:])
        if not close:
            fail(f"Cell {column}{row_number} is not closed")
        end = opener_end + close.end()
    return row_xml[start:end], start, end


def cell_text(cell):
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<(?:[\w]+:)?v>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if value:
        return html.unescape(value.group(1))
    fail(f"No text in {cell[:80]}")


def rows_of(sheet_xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet_xml)


def sheet_ids(sheet_xml, id_column):
    found = []
    for row_xml in rows_of(sheet_xml):
        row_number = int(re.search(r'\br="(\d+)"', row_xml).group(1))
        if row_number < 3:
            continue
        token = f'r="{id_column}{row_number}"'
        if token not in row_xml:
            continue
        code = cell_text(cell_xml(row_xml, id_column, row_number)[0]).strip()
        if code:
            found.append((code, row_number))
    return found


live_bytes = live.read_bytes()
live_stat = live.stat()
live_hash = sha256(live_bytes)
if live_hash != expected:
    fail(f"Live workbook hash changed before writeback: {live_hash}")

stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"test-stage-writeback-术式制作表-{stamp}-1.xlsx"
output = batch / "test-stage-writeback-candidate-preserved.xlsx"
notes_path = batch / "test-stage-writeback-notes.json"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)
if sha256(backup.read_bytes()) != expected:
    fail("Backup hash is not the baseline")
notes_path.write_text(json.dumps({
    "backup": str(backup),
    "baselineSha256": expected,
    "additions": additions,
}, ensure_ascii=False, indent=2), encoding="utf-8")

snapshot = json.loads((batch / "test-stage-transition-snapshot.json").read_text(encoding="utf-8"))
snapshot_by_id = {item["id"]: item for item in snapshot["items"]}

with zipfile.ZipFile(backup) as archive:
    parts = {name: archive.read(name) for name in archive.namelist()}
    order = archive.infolist()

original = parts["xl/worksheets/sheet3.xml"]
sheet = original.decode("utf-8")
row_map = {}
for row_xml in rows_of(sheet):
    row_map[int(re.search(r'\br="(\d+)"', row_xml).group(1))] = row_xml
if set(row_map) != {1, 2, 3, 4, 5, 6, 7, 8}:
    fail(f"Making rows are {sorted(row_map)}")

detail_ids = sheet_ids(parts["xl/worksheets/sheet6.xml"].decode("utf-8"), "A")
fire_details = [item for item in detail_ids if item[0] in additions]
if fire_details:
    fail(f"Unexpected resource rows: {fire_details}")

updated_rows = {}
reconcile = []
for row_number, name, code in wanted:
    row_xml = row_map[row_number]
    if cell_text(cell_xml(row_xml, "A", row_number)[0]) != name:
        fail(f"Name mismatch on row {row_number}")
    if cell_text(cell_xml(row_xml, "B", row_number)[0]) != code:
        fail(f"Id mismatch on row {row_number}")
    feature = cell_text(cell_xml(row_xml, "N", row_number)[0])
    effect = cell_text(cell_xml(row_xml, "O", row_number)[0])
    cell, start, end = cell_xml(row_xml, "DV", row_number)
    old_text = cell_text(cell)
    source = snapshot_by_id[code]
    if feature != source["n"] or effect != source["o"] or old_text != source["dv"]:
        fail(f"Current N/O/DV no longer matches the transition snapshot for {code}")
    if feature != source["conversion"]["nBefore"] or effect != source["conversion"]["oBefore"]:
        fail(f"Conversion before-values differ for {code}")
    if "测试正式结果核对第1轮" in old_text:
        fail(f"DV{row_number} already has this review note")
    addition = "\n" + additions[code]
    escaped = html.escape(addition, quote=False)
    if 't="str"' in cell[:80] and cell.endswith("</v></c>"):
        updated_cell = cell[: -len("</v></c>")] + escaped + "</v></c>"
    elif 't="inlineStr"' in cell[:180] and cell.endswith("</t></is></c>"):
        updated_cell = cell[: -len("</t></is></c>")] + escaped + "</t></is></c>"
    else:
        fail(f"Unsupported DV cell on row {row_number}: {cell[:120]}")
    updated_row = row_xml[:start] + updated_cell + row_xml[end:]
    if cell_text(cell_xml(updated_row, "DV", row_number)[0]) != old_text + addition:
        fail(f"DV{row_number} text was not appended cleanly")
    if cell_text(cell_xml(updated_row, "N", row_number)[0]) != feature:
        fail(f"N changed for {code}")
    if cell_text(cell_xml(updated_row, "O", row_number)[0]) != effect:
        fail(f"O changed for {code}")
    updated_rows[row_number] = updated_row
    reconcile.append({"id": code, "row": row_number, "nKept": True, "oKept": True, "dvPrefixKept": True})

sheet_out = sheet
for row_number, _name, _code in wanted:
    if sheet_out.count(row_map[row_number]) != 1:
        fail(f"Row {row_number} is not unique")
    sheet_out = sheet_out.replace(row_map[row_number], updated_rows[row_number], 1)
restored = sheet_out
for row_number, _name, code in wanted:
    if restored.count(updated_rows[row_number]) != 1:
        fail(f"Updated row {row_number} for {code} is not unique")
    restored = restored.replace(updated_rows[row_number], row_map[row_number], 1)
if restored.encode("utf-8") != original:
    fail("Sheet differs beyond the six DV additions")

changed = {"xl/worksheets/sheet3.xml": sheet_out.encode("utf-8")}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    if src.namelist() != dst.namelist():
        fail("Zip part list changed")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml"]:
        fail(f"Unexpected part changes: {different}")
    counts = {}
    located = []
    for title, name in status_parts.items():
        ids = sheet_ids(dst.read(name).decode("utf-8"), "B")
        counts[title] = len(ids)
        located.extend((code, title, row_number) for code, row_number in ids)
    if counts != {"已完成": 40, "待制作": 0, "制作中": 6, "待验收": 0}:
        fail(f"Counts {counts}")
    codes = [item[0] for item in located]
    if len(codes) != len(set(codes)) or len(codes) != 46:
        fail(f"Id total {len(codes)} unique {len(set(codes))}")
    claimed = [(code, title, row) for code, title, row in located if code in additions]
    expected_locations = [(code, "制作中", row) for row, _name, code in wanted]
    if claimed != expected_locations:
        fail(f"Locations changed: {claimed}")

summary = {
    "submitted": False,
    "baselineSha256": expected,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "changedParts": different,
    "counts": counts,
    "resourceDetailIds": [{"id": code, "row": row} for code, row in detail_ids],
    "snapshotReconcile": reconcile,
}
(batch / "test-stage-writeback-preserve-summary.json").write_text(
    json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "backup": backup.name,
    "candidateSha256": summary["candidateSha256"],
    "counts": counts,
    "changedParts": different,
    "resourceDetails": summary["resourceDetailIds"],
}, ensure_ascii=False))
