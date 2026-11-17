# -*- coding: utf-8 -*-
"""Move three consumers to pending acceptance and add the burn-settlement trigger."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "火种共享燃料改造-20261029"
live = root / "docs" / "术式制作表.xlsx"
before = json.loads((batch / "final-writeback-before.json").read_text(encoding="utf-8"))
expected = before["workbook"]["sha256"]
items = {item["id"]: item for item in before["items"]}
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
display_n = {
    "fire_liaoyuan": "铺开地表火层。踩中后按段燃烧，每段消耗10点火种，燃料耗尽或到达期限就结束",
    "fire_liaoli": "持剑向前刺击。命中时花掉当时剩余的火种，并打一刀物理",
    "fire_fentian": "球内有火种的目标挂上焚天。伤害在挂上时锁定，最多40刻，每段消耗10点火种",
}
display_o = {
    "fire_liaoyuan": "\n".join([
        "assets/xueximod/textures/effect/liaoyuan_v2/ground_flame.png",
        "assets/xueximod/textures/effect/liaoyuan_v2/ground_embers.png",
        "assets/xueximod/textures/effect/liaoyuan_v2/spread_wave.png",
    ]),
    "fire_fentian": "assets/xueximod/textures/effect/fentian_v2/conflagration.png",
    "fire_liaoli": "\n".join([
        "assets/xueximod/textures/effect/liaoli_v6/sword_unlit.png",
        "assets/xueximod/textures/effect/liaoli_v6/sword_hot.png",
        "assets/xueximod/textures/effect/liaoli_v6/back_ignite.png",
        "assets/xueximod/textures/effect/liaoli_v6/back_flow.png",
        "assets/xueximod/textures/effect/liaoli_v6/thrust_backflow.png",
        "assets/xueximod/textures/effect/liaoli_v6/rear_ring.png",
    ]),
}
constraints = {
    "fire_liaoyuan": "完整段请求10点，周期20tick。同tick按需求比例分配不足燃料，实付不足按实付/10缩放该段伤害。n和F只在启动时观察。最长(3+n)×20tick，燃料尽则提前结束。不预扣全部。",
    "fire_fentian": "启动锁定n和F。完整段请求10点，周期4tick，伤害2×(1.2^n+F)。同tick按需求比例分配，实付不足按比例缩放，不再另乘余量尾段。最多10次结算、40tick。不预扣全部。",
}
periods = {"fire_liaoyuan": "20", "fire_fentian": "4"}
triggers = {"fire_liaoyuan": "燃烧结算", "fire_fentian": "燃烧结算"}
playtest = (
    "游戏实测尚未做。步骤：单层按周期扣种；两层同时扣；燃料不足时按比例分配并按实付折算伤害；"
    "燃烧未结束时持续补种；引爆先清空则燃烧失去燃料，燃烧先扣则引爆只拿剩余；"
    "余额到0后新火种不复活已结束的旧层；焚天最多40有效tick且指数不因补种提高；"
    "卸载或恢复后旧的已付轨道不重复扣；护甲和抗火仍生效，已付火种不退回。"
)
common = (
    "有效 compileJava --offline --no-daemon 于2026-10-29 21:36:10至21:36:22退出0，compileJava为UP-TO-DATE，"
    "对应21:29已生成且晚于源码的主类。21:29:15启动的第一次包装进程因输出管道卡住，于21:34:44中止，退出码1，"
    "不是源码编译失败，也不是重新编译。SharedFuelCheck、SixFireFunctionalCheck、FireSeedCheck、BurnLedgerCheck、"
    "BurnDispatchCheck、FireReleaseStructureCheck、SixFireLogicCheck于21:38:38至21:38:40全部退出0。"
    "本版没有追加正式复审，这些退出码不是复审通过，也不是游戏验收。"
    "旧存档版本1整份预付回放且不再扣火种；版本2只有shared标记才按段扣，缺标记按预付；其它版本整份丢弃。新挂载只建共享轨道。"
    "同效果键视觉查询只显示第一条轨道。没有实体NBT往返。"
    + playtest
    + "待验收 | 火种共享燃料改造-20261029 | docs/开发记录/火种共享燃料改造-20261029/记录.md。"
    "详见 docs/开发记录/火种共享燃料改造-20261029/final-writeback-conversion.json。"
)
additions = {
    "fire_liaoyuan": "[设计反馈 2026-10-29] 共享燃料最终写回。游戏内一句对齐 technique.xueximod.fire_liaoyuan.desc。资源扣款触发改为燃烧结算，周期20tick，完整段10点。首次带资源接触只保留为挂载时机，写在迁出前的详细N。" + common,
    "fire_fentian": "[设计反馈 2026-10-29] 共享燃料最终写回。游戏内一句对齐 technique.xueximod.fire_fentian.desc。资源扣款触发改为燃烧结算，周期4tick，完整段10点，最多40有效tick。范围选取只保留为挂载时机，写在迁出前的详细N。" + common,
    "fire_liaoli": "[设计反馈 2026-10-29] 共享燃料最终写回。游戏内一句对齐 technique.xueximod.fire_liaoli.desc。仍在命中成功时消费当时全部余额。公式保持6点物理加0.36×S×(1+F)，不改成0.2×S。详细公式留在迁出前快照和本备注。" + common,
}


def fail(message):
    raise SystemExit(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()


def rows_of(sheet_xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet_xml)


def row_number(row_xml):
    return int(re.search(r'\br="(\d+)"', row_xml).group(1))


def cell_xml(row_xml, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row_xml))
    if len(matches) != 1:
        fail(f"Cell {column}{number} count is {len(matches)}")
    start = matches[0].start()
    opener_end = row_xml.find(">", start)
    if row_xml[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</(?:[\w]+:)?c>", row_xml[opener_end:])
        if not close:
            fail(f"Cell {column}{number} is not closed")
        end = opener_end + close.end()
    return row_xml[start:end], start, end


def cell_text(cell):
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<(?:[\w]+:)?v>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if value:
        return html.unescape(value.group(1))
    return None


def replace_text(cell, column, number, new_text):
    escaped = html.escape(new_text, quote=False)
    if re.search(r"<(?:[\w]+:)?t\b", cell):
        updated, count = re.subn(
            r"(<(?:[\w]+:)?t\b[^>]*>)[\s\S]*(</(?:[\w]+:)?t>)",
            lambda match: match.group(1) + escaped + match.group(2),
            cell,
            count=1,
        )
    elif re.search(r"<(?:[\w]+:)?v>", cell):
        updated, count = re.subn(
            r"(<(?:[\w]+:)?v>)[\s\S]*(</(?:[\w]+:)?v>)",
            lambda match: match.group(1) + escaped + match.group(2),
            cell,
            count=1,
        )
    else:
        fail(f"Cell {column}{number} has no text node")
    if count != 1 or cell_text(updated) != new_text:
        fail(f"Cell {column}{number} text was not written cleanly")
    return updated


def set_cell(row_xml, column, number, new_text):
    cell, start, end = cell_xml(row_xml, column, number)
    return row_xml[:start] + replace_text(cell, column, number, new_text) + row_xml[end:]


def insert_period(row_xml, number, ticks):
    if f'r="EG{number}"' in row_xml:
        fail(f"EG{number} already exists")
    marker = f'<c r="EI{number}"'
    if row_xml.count(marker) != 1:
        fail(f"Cannot place EG{number}")
    cell = f'<c r="EG{number}" s="15" t="n"><v>{ticks}</v></c>'
    return row_xml.replace(marker, cell + marker, 1)


def renumber(row_xml, old, new):
    updated = re.sub(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row_xml, count=1)
    updated = re.sub(rf'r="([A-Z]+){old}"', rf'r="\g<1>{new}"', updated)
    if row_number(updated) != new:
        fail(f"Row {old} did not become {new}")
    return updated


def replace_sheet_rows(xml, new_rows):
    match = re.search(r"(<(?:[\w]+:)?sheetData>)([\s\S]*)(</(?:[\w]+:)?sheetData>)", xml)
    if not match:
        fail("sheetData missing")
    return xml[:match.start(2)] + "".join(new_rows) + xml[match.end(2):]


def sheet_ids(sheet_xml):
    found = []
    for row_xml in rows_of(sheet_xml):
        number = row_number(row_xml)
        if number < 3 or f'r="B{number}"' not in row_xml:
            continue
        code = cell_text(cell_xml(row_xml, "B", number)[0])
        if code and code.strip():
            found.append((code.strip(), number))
    return found


live_bytes = live.read_bytes()
if sha256(live_bytes) != expected:
    fail(f"Live workbook hash changed: {sha256(live_bytes)}")
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"final-writeback-术式制作表-{stamp}-1.xlsx"
output = batch / "final-writeback-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original_parts = {info.filename: archive.read(info.filename) for info in order}

for name in original_parts:
    if name.endswith(".xml") and ("BN$11" in original_parts[name].decode("utf-8", "ignore") or "$BN$4:$BN$11" in original_parts[name].decode("utf-8", "ignore")):
        fail(f"Hardcoded trigger range in {name}")

sheet3 = original_parts["xl/worksheets/sheet3.xml"].decode("utf-8")
sheet4 = original_parts["xl/worksheets/sheet4.xml"].decode("utf-8")
sheet5 = original_parts["xl/worksheets/sheet5.xml"].decode("utf-8")
rows3 = rows_of(sheet3)
rows4 = rows_of(sheet4)
if [row_number(row) for row in rows3] != [1, 2, 3, 4, 5, 6, 7, 8]:
    fail("制作中 row numbers changed")
if [row_number(row) for row in rows4] != [1, 2]:
    fail("待验收 is not header-only")

by_id = {}
for row_xml in rows3:
    number = row_number(row_xml)
    if number < 3:
        continue
    by_id[cell_text(cell_xml(row_xml, "B", number)[0])] = row_xml
if set(items) != {"fire_liaoyuan", "fire_liaoli", "fire_fentian"}:
    fail("Snapshot items changed")
for code, row_xml in (
    ("fire_heat_up", rows3[5]),
    ("fire_burn", rows3[6]),
    ("fire_cool", rows3[7]),
):
    if cell_text(cell_xml(row_xml, "B", row_number(row_xml))[0]) != code:
        fail(f"Furnace row is not {code}")
    if "<f" in row_xml:
        fail(f"{code} has a formula")

moved = {}
for code, spec_row in (("fire_liaoyuan", 3), ("fire_liaoli", 4), ("fire_fentian", 5)):
    row_xml = by_id[code]
    number = row_number(row_xml)
    if number != spec_row or items[code]["row"] != number or items[code]["sheet"] != "制作中":
        fail(f"{code} is not at the snapshot row")
    if "<f" in row_xml:
        fail(f"{code} has a formula")
    if cell_text(cell_xml(row_xml, "N", number)[0]) != items[code]["n"]:
        fail(f"N drifted for {code}")
    if cell_text(cell_xml(row_xml, "O", number)[0]) != items[code]["o"]:
        fail(f"O drifted for {code}")
    if cell_text(cell_xml(row_xml, "DV", number)[0]) != items[code]["dv"]:
        fail(f"DV drifted for {code}")
    if "共享燃料最终写回" in items[code]["dv"]:
        fail(f"DV already has the final marker for {code}")
    row_xml = set_cell(row_xml, "N", number, display_n[code])
    row_xml = set_cell(row_xml, "O", number, display_o[code])
    row_xml = set_cell(row_xml, "DV", number, items[code]["dv"] + "\n" + additions[code])
    if code in triggers:
        if cell_text(cell_xml(row_xml, "EB", number)[0]) != items[code]["cells"]["EB"]["text"]:
            fail(f"EB drifted for {code}")
        row_xml = set_cell(row_xml, "EB", number, triggers[code])
        row_xml = set_cell(row_xml, "EK", number, constraints[code])
        row_xml = insert_period(row_xml, number, periods[code])
        if cell_text(cell_xml(row_xml, "EC", number)[0]) != "10":
            fail(f"EC is not 10 for {code}")
    else:
        if cell_text(cell_xml(row_xml, "EB", number)[0]) != "命中成功":
            fail("燎里 trigger changed")
        if cell_text(cell_xml(row_xml, "EC", number)[0]) != "全部":
            fail("燎里 amount changed")
    moved[code] = row_xml

sheet3_out = replace_sheet_rows(sheet3, [
    rows3[0],
    rows3[1],
    renumber(rows3[5], 6, 3),
    renumber(rows3[6], 7, 4),
    renumber(rows3[7], 8, 5),
])
sheet4_out = replace_sheet_rows(sheet4, [
    rows4[0],
    rows4[1],
    moved["fire_liaoyuan"],
    moved["fire_liaoli"],
    moved["fire_fentian"],
])
if sheet3_out.count('ref="A1:EN8"') != 1 or sheet4_out.count('ref="A1:EN2"') != 1:
    fail("Dimension anchors moved")
sheet3_out = sheet3_out.replace('ref="A1:EN8"', 'ref="A1:EN5"', 1)
sheet4_out = sheet4_out.replace('ref="A1:EN2"', 'ref="A1:EN5"', 1)

rows5 = rows_of(sheet5)
row12 = next(row for row in rows5 if row_number(row) == 12)
if 'r="BN12"' in row12 or "燃烧结算" in sheet5:
    fail("燃烧结算 is already present")
kept = [cell_text(cell_xml(sheet5, "BN", number)[0]) for number in range(4, 12)]
if kept != ["成功施放", "出手提交", "命中成功", "潮印命中成功", "领域周期", "首次带资源接触", "范围选取", "主动结束"]:
    fail(f"Trigger list changed: {kept}")
row12_out = row12.replace(
    "</row>",
    '<c r="BN12" s="20" t="inlineStr"><is><t>燃烧结算</t></is></c></row>',
    1,
)
if rows5.count(row12) != 1:
    fail("Row 12 is not unique")
sheet5_out = sheet5.replace(row12, row12_out, 1)

changed = {
    "xl/worksheets/sheet3.xml": sheet3_out.encode("utf-8"),
    "xl/worksheets/sheet4.xml": sheet4_out.encode("utf-8"),
    "xl/worksheets/sheet5.xml": sheet5_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    if src.namelist() != dst.namelist():
        fail("Zip part list changed")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml", "xl/worksheets/sheet5.xml"]:
        fail(f"Unexpected part changes: {different}")
    counts = {}
    all_ids = []
    located = {}
    for title, name in status_parts.items():
        ids = sheet_ids(dst.read(name).decode("utf-8"))
        counts[title] = len(ids)
        for code, number in ids:
            located.setdefault(code, []).append((title, number))
            all_ids.append(code)
    if counts != {"已完成": 40, "待制作": 0, "制作中": 3, "待验收": 3}:
        fail(f"Counts {counts}")
    if len(all_ids) != 46 or len(set(all_ids)) != 46:
        fail("Status ids are not unique")
    expected_rows = {
        "fire_liaoyuan": ("待验收", 3),
        "fire_liaoli": ("待验收", 4),
        "fire_fentian": ("待验收", 5),
        "fire_heat_up": ("制作中", 3),
        "fire_burn": ("制作中", 4),
        "fire_cool": ("制作中", 5),
    }
    for code, place in expected_rows.items():
        if located.get(code) != [place]:
            fail(f"{code} at {located.get(code)}")
    pending = dst.read("xl/worksheets/sheet4.xml").decode("utf-8")
    pending_rows = {row_number(row): row for row in rows_of(pending)}
    for code, number in (("fire_liaoyuan", 3), ("fire_liaoli", 4), ("fire_fentian", 5)):
        row_xml = pending_rows[number]
        if cell_text(cell_xml(row_xml, "N", number)[0]) != display_n[code]:
            fail(f"Display N mismatch for {code}")
        if cell_text(cell_xml(row_xml, "O", number)[0]) != display_o[code]:
            fail(f"Display O mismatch for {code}")
        if additions[code] not in cell_text(cell_xml(row_xml, "DV", number)[0]):
            fail(f"DV addition missing for {code}")
        if not cell_text(cell_xml(row_xml, "DV", number)[0]).startswith(items[code]["dv"]):
            fail(f"Old DV was not kept for {code}")
    if cell_text(cell_xml(pending_rows[3], "EB", 3)[0]) != "燃烧结算":
        fail("燎原 trigger was not updated")
    if cell_text(cell_xml(pending_rows[5], "EB", 5)[0]) != "燃烧结算":
        fail("焚天 trigger was not updated")
    if cell_text(cell_xml(pending_rows[3], "EG", 3)[0]) != "20":
        fail("燎原 period is not 20")
    if cell_text(cell_xml(pending_rows[5], "EG", 5)[0]) != "4":
        fail("焚天 period is not 4")
    if cell_text(cell_xml(pending_rows[4], "EB", 4)[0]) != "命中成功":
        fail("燎里 trigger changed")
    if "0.36×S" not in cell_text(cell_xml(pending_rows[4], "DV", 4)[0]):
        fail("燎里 formula left the remark")
    if "0.2×S" in display_n["fire_liaoli"]:
        fail("燎里 sentence picked up the draft formula")
    data_sheet = dst.read("xl/worksheets/sheet5.xml").decode("utf-8")
    if cell_text(cell_xml(data_sheet, "BN", 12)[0]) != "燃烧结算":
        fail("New trigger option missing")
    if [cell_text(cell_xml(data_sheet, "BN", number)[0]) for number in range(4, 12)] != kept:
        fail("Old trigger options changed")

conversion = {
    "kind": "pending-acceptance-conversion",
    "notVerification": True,
    "notOriginalArchive": True,
    "baselineSha256": expected,
    "fromSheet": "制作中",
    "toSheet": "待验收",
    "displaySource": "src/main/resources/assets/xueximod/lang/zh_cn.json technique.xueximod.*.desc",
    "resourceSource": [
        "src/main/java/com/bslx/XueXiMod/visual/fire/FireVisualLayout.java",
        "src/main/java/com/bslx/XueXiMod/visual/liaoli/LiaoliV6Layout.java",
    ],
    "items": {
        code: {
            "fromRow": items[code]["row"],
            "toRow": {"fire_liaoyuan": 3, "fire_liaoli": 4, "fire_fentian": 5}[code],
            "beforeN": items[code]["n"],
            "beforeO": items[code]["o"],
            "beforeDv": items[code]["dv"],
            "displayN": display_n[code],
            "displayO": display_o[code],
            "dvAppend": additions[code],
            "trigger": triggers.get(code, "命中成功"),
            "periodTick": periods.get(code),
        }
        for code in ("fire_liaoyuan", "fire_liaoli", "fire_fentian")
    },
}
(batch / "final-writeback-conversion.json").write_text(json.dumps(conversion, ensure_ascii=False, indent=2), encoding="utf-8")
summary = {
    "submitted": False,
    "baselineSha256": expected,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "counts": counts,
    "changedParts": different,
    "rows": {code: expected_rows[code][1] for code in display_n},
}
(batch / "final-writeback-preserve-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "backup": backup.name,
    "candidateSha256": summary["candidateSha256"],
    "counts": counts,
    "changedParts": different,
}, ensure_ascii=False))
