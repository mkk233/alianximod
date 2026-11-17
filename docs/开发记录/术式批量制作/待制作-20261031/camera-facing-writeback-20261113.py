# -*- coding: utf-8 -*-
"""Move fire_flame_trail back to 制作中 and note the surface-anchor pause."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "术式批量制作" / "待制作-20261031"
live = root / "docs" / "术式制作表.xlsx"
snapshot_path = batch / "flame-trail-v3-requirements-snapshot-20261113.json"
repair_path = batch / "flame-trail-v3-repair-snapshot-20261113.json"
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
expected_baseline = "06055D2AA6231BD185C48CE89A37C6D3E36E0E9B41821712AFFB19E7C98A1A38"
expected_snapshot = "D9FFAAAE33CF00F16DAD94EEE1E6357B5934C5714EC08049EFD4F50266BA0941"
facing_sentence = (
    "不声明持续面向玩家（CameraFacingTag.NONE），"
    "采样面沿三维弹道切向直立排布并首尾连贯成线，"
    "不随玩家视角旋转成断裂横条。"
)
flame_addition = (
    "[设计反馈 2026-11-13] 问题：用户游戏内截图反馈留焰接入后斜向弹道未连成线，"
    "呈水平锯齿阶梯。根因：BatchCarrierVisualRenderer 对全部 BILLBOARD "
    "套用 cameraOrientation()，0.25格采样点各自面向屏幕横转。"
    "方案与用户决定：按用户指令建立 visual/facing 标签底座，"
    "默认不面向相机；仅既有 8 项平面公告板登记 CAMERA_PLANE、"
    "万剑归宗漩涡单独登记 UPRIGHT_CAMERA_YAW（不改美术），"
    "rule_surface_anchor 搁置与留焰 TRAIL 均为 CameraFacingTag.NONE；"
    "留焰采样面改为沿世界三维弹道切向直立拼接，"
    "微米级法线分层防共面闪烁，玩法数值与判定未改。"
    "2026-11-13 03:31:31–03:31:59 compileJava --offline --no-daemon 退出码 0；"
    "本阶段额度设计2/2、实现3/3已用完，返修结果未追加复审。"
    "详见 docs/开发记录/术式批量制作/待制作-20261031/"
    "camera-facing-tag-and-trail-fix-20261113.md 与 "
    "camera-facing-compile-20261113.md.\n"
    "[状态纠正 2026-11-13] 因游戏实测发现面向相机导致断线，"
    "运行朝向由原文字交接的面向相机改为 CameraFacingTag.NONE "
    "世界弹道切向直立排布，尚未经斜向三维补预览确认与游戏复测，"
    "按流程第 5.1、8、9 节从待验收迁回制作中。\n"
    "制作中 | 待制作-20261031 | "
    "docs/开发记录/术式批量制作/待制作-20261031/记录.md"
)
surface_addition = (
    "[设计反馈 2026-11-13] 用户明确指示「rule_surface_anchor"
    "（地表定位） 特效之后重新做先搁置其他加一下tag」："
    "本次不添加持续面向玩家标签（维持 CameraFacingTag.NONE "
    "世界空间双面四边形），特效重制暂搁置，条目继续留在制作中。"
    "详见 docs/开发记录/术式批量制作/待制作-20261031/记录.md。"
)
user_quotes = [
    "修一下，让只有特殊声明的特效持续面向玩家，"
    "保持其他特效现在的样子只为之前存在持续面向玩家的特效添加tag"
    "用于接入此功能",
    "rule_surface_anchor（地表定位） 特效之后重新做先搁置"
    "其他加一下tag，万剑归宗的朝向比较特殊要单独标记"
    "不要改变现有美术效果仅改为添加tag通过新底座入口实现",
]
facing_registry = {
    "CAMERA_PLANE": [
        "SPARK",
        "SEED",
        "KINDLE",
        "DETONATE",
        "DUST",
        "FENTIAN_SKY_FLAME",
        "FIRE_SEED_ORBIT_FLAME",
        "WATER_PRISON_SHELL",
    ],
    "UPRIGHT_CAMERA_YAW": ["SWORD_RETURN_ALL_VORTEX"],
    "NONE": ["TRAIL", "SURFACE", "SHOCK", "NONE"],
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
    matches = list(re.finditer(rf'<(?:[\w]+:)?c\b[^>]*\br="{column}{number}"', row_xml))
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
    inline = re.search(r"<(?:[\w]+:)?is\b[^>]*>([\s\S]*)</(?:[\w]+:)?is>", cell)
    if inline:
        nodes = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", inline.group(1))
        if len(nodes) != 1:
            fail(f"Inline cell text node count is {len(nodes)}")
        return html.unescape(nodes[0])
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if value:
        return html.unescape(value.group(1))
    return None


def replace_text(cell, column, number, new_text):
    escaped = html.escape(new_text, quote=False).replace("\n", "&#10;")
    if len(re.findall(r"<(?:[\w]+:)?t\b", cell)) != 1:
        fail(f"Cell {column}{number} does not have exactly one text node")
    updated, count = re.subn(
        r"(<(?:[\w]+:)?t\b[^>]*>)[\s\S]*(</(?:[\w]+:)?t>)",
        lambda match: match.group(1) + escaped + match.group(2),
        cell,
        count=1,
    )
    if count != 1 or cell_text(updated) != new_text:
        fail(f"Cell {column}{number} text was not written cleanly")
    return updated


def set_cell(row_xml, column, number, new_text):
    cell, start, end = cell_xml(row_xml, column, number)
    return row_xml[:start] + replace_text(cell, column, number, new_text) + row_xml[end:]


def renumber(row_xml, old, new):
    updated = re.sub(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row_xml, count=1)
    updated = re.sub(rf'r="([A-Z]+){old}"', rf'r="\g<1>{new}"', updated)
    if row_number(updated) != new or f'r="{old}"' in updated or re.search(rf'r="[A-Z]+{old}"', updated):
        fail(f"Row {old} did not become {new} cleanly")
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
            found.append((code.strip(), number, row_xml))
    return found


def replace_once(xml, old, new, label):
    if xml.count(old) != 1:
        fail(f"{label} count is {xml.count(old)}")
    return xml.replace(old, new, 1)


def validation_covers(xml, row):
    uncovered = []
    for ref in re.findall(r'<dataValidation\b[^>]*sqref="([^"]+)"', xml):
        for part in ref.split():
            bounds = re.match(r"([A-Z]+)(\d+):([A-Z]+)(\d+)$", part)
            if bounds and int(bounds.group(2)) <= row <= int(bounds.group(4)):
                continue
            if bounds and int(bounds.group(2)) <= row and int(bounds.group(4)) < row:
                uncovered.append(part)
    if uncovered:
        fail(f"Validation does not cover row {row}: {uncovered}")


def row_without_text(row_xml):
    return re.sub(r"(<(?:[\w]+:)?t\b[^>]*>)[\s\S]*?(</(?:[\w]+:)?t>)", r"\1\2", row_xml)


snapshot = json.loads(snapshot_path.read_text(encoding="utf-8"))
if sha256(snapshot_path.read_bytes()) != expected_snapshot:
    fail("Requirements snapshot hash changed")
n_original = snapshot["nOriginal"]
o_original = snapshot["oOriginal"]
o_merged = o_original + "\n" + facing_sentence
if len(n_original) != 572 or len(o_original) != 97:
    fail("Approved N/O length changed")

live_bytes = live.read_bytes()
live_stat = live.stat()
baseline = sha256(live_bytes)
if baseline != expected_baseline:
    fail(f"Live baseline changed before candidate build: {baseline}")
if len(live_bytes) != 178752:
    fail(f"Live byte count changed: {len(live_bytes)}")

stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"camera-facing-writeback-术式制作表-{stamp}-1.xlsx"
output = batch / "camera-facing-writeback-candidate-preserved.xlsx"
script_path = batch / "camera-facing-writeback-20261113.py"
backup.parent.mkdir(exist_ok=True)
if backup.exists() or output.exists() or repair_path.exists():
    fail("Output path already exists")
backup.write_bytes(live_bytes)
if sha256(backup.read_bytes()) != baseline:
    fail("Backup hash mismatch")

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original_parts = {info.filename: archive.read(info.filename) for info in order}

if original_parts["xl/worksheets/sheet6.xml"].decode("utf-8").count("fire_flame_trail") != 0:
    fail("Resource detail unexpectedly contains fire_flame_trail")

sheet3 = original_parts[status_parts["制作中"]].decode("utf-8")
sheet4 = original_parts[status_parts["待验收"]].decode("utf-8")
rows3 = rows_of(sheet3)
rows4 = rows_of(sheet4)
ids3 = sheet_ids(sheet3)
ids4 = sheet_ids(sheet4)
surface = [item for item in ids3 if item[0] == "rule_surface_anchor"]
flame = [item for item in ids4 if item[0] == "fire_flame_trail"]
if len(surface) != 1 or surface[0][1] != 3 or any(item[0] == "fire_flame_trail" for item in ids3):
    fail(f"rule_surface_anchor location is not row 3: {[(item[0], item[1]) for item in surface]}")
if len(flame) != 1 or flame[0][1] != 23:
    fail(f"fire_flame_trail is not pending row 23: {[(item[0], item[1]) for item in flame]}")

source_number = 23
source = flame[0][2]
if "<f" in source:
    fail("fire_flame_trail row contains a formula")
if cell_text(cell_xml(source, "A", source_number)[0]) != "留焰":
    fail("Flame name mismatch")
old_n = cell_text(cell_xml(source, "N", source_number)[0])
old_o = cell_text(cell_xml(source, "O", source_number)[0])
old_dv = cell_text(cell_xml(source, "DV", source_number)[0])
if old_n != "沿载体留下不产种的火焰轨迹":
    fail("Pending N is not the acceptance sentence")
if old_o != "assets/xueximod/textures/effect/flame_trail_v3/flame_trail.png":
    fail("Pending O is not the v3 resource id")
if not old_dv.startswith("火径寿命、宽度、采样间距") or "待验收 | 待制作-20261031" not in old_dv:
    fail("Pending DV prefix is not intact")
if "CameraFacingTag" in old_dv:
    fail("Pending DV already contains the facing note")

content_rows3 = [number for _, number, _ in ids3]
target_number = max(content_rows3) + 1
if target_number != 24:
    fail(f"Expected target row 24, found {target_number}")
edited = set_cell(source, "N", source_number, n_original)
edited = set_cell(edited, "O", source_number, o_merged)
edited = set_cell(edited, "DV", source_number, old_dv + "\n" + flame_addition)
moved = renumber(edited, source_number, target_number)
if cell_text(cell_xml(moved, "B", target_number)[0]) != "fire_flame_trail":
    fail("Moved id changed")
if cell_text(cell_xml(moved, "N", target_number)[0]) != n_original:
    fail("Moved N mismatch")
if cell_text(cell_xml(moved, "O", target_number)[0]) != o_merged:
    fail("Moved O mismatch")
moved_dv = cell_text(cell_xml(moved, "DV", target_number)[0])
if moved_dv != old_dv + "\n" + flame_addition or not moved_dv.startswith(old_dv):
    fail("Moved DV prefix was not preserved")
if len(re.findall(r"<c\b", moved)) != len(re.findall(r"<c\b", source)):
    fail("Moved row cell count changed")
if row_without_text(moved) != row_without_text(renumber(source, source_number, target_number)):
    fail("Moved row changed outside N/O/DV text")

surface_row = surface[0][2]
surface_n = cell_text(cell_xml(surface_row, "N", 3)[0])
surface_o = cell_text(cell_xml(surface_row, "O", 3)[0])
surface_dv = cell_text(cell_xml(surface_row, "DV", 3)[0])
if not surface_dv or "CameraFacingTag" in surface_dv:
    fail("Surface DV is missing or already annotated")
surface_edited = set_cell(surface_row, "DV", 3, surface_dv + "\n" + surface_addition)
if cell_text(cell_xml(surface_edited, "N", 3)[0]) != surface_n:
    fail("Surface N changed")
if cell_text(cell_xml(surface_edited, "O", 3)[0]) != surface_o:
    fail("Surface O changed")
if row_without_text(surface_edited) != row_without_text(surface_row):
    fail("Surface row changed outside DV text")

source_position = next(index for index, row in enumerate(rows4) if row_number(row) == source_number)
if source_position != len(rows4) - 1:
    fail("fire_flame_trail is not the last pending row")
surface_position = next(index for index, row in enumerate(rows3) if row_number(row) == 3)
rows3_out = rows3[:surface_position] + [surface_edited] + rows3[surface_position + 1:] + [moved]
rows4_out = rows4[:source_position]
sheet3_out = replace_sheet_rows(sheet3, rows3_out)
sheet4_out = replace_sheet_rows(sheet4, rows4_out)
sheet3_out = replace_once(sheet3_out, 'ref="A1:EN23"', 'ref="A1:EN24"', "制作中 dimension")
validation_covers(sheet3_out, target_number)
validation_covers(sheet4_out, 22)

workbook = original_parts["xl/workbook.xml"].decode("utf-8")
old_filter = "'制作中'!$A$2:$EN$3"
new_filter = "'制作中'!$A$2:$EN$4"
if workbook.count(old_filter) != 1:
    fail("制作中 defined filter missing or duplicated")
workbook_out = workbook.replace(old_filter, new_filter, 1)

changed = {
    status_parts["制作中"]: sheet3_out.encode("utf-8"),
    status_parts["待验收"]: sheet4_out.encode("utf-8"),
    "xl/workbook.xml": workbook_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        clone = zipfile.ZipInfo(filename=info.filename, date_time=info.date_time)
        clone.compress_type = info.compress_type
        clone.flag_bits = info.flag_bits
        clone.external_attr = info.external_attr
        clone.internal_attr = info.internal_attr
        clone.create_system = info.create_system
        clone.extra = info.extra
        clone.comment = info.comment
        dst.writestr(clone, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    if src.namelist() != dst.namelist():
        fail("Zip part list changed")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    allowed = [status_parts["制作中"], status_parts["待验收"], "xl/workbook.xml"]
    if different != allowed:
        fail(f"Unexpected part changes: {different}")
    counts = {}
    located = {}
    all_ids = []
    originals = {}
    for title, name in status_parts.items():
        ids = sheet_ids(dst.read(name).decode("utf-8"))
        counts[title] = len(ids)
        originals[title] = sheet_ids(src.read(name).decode("utf-8"))
        for code, number, _row in ids:
            located.setdefault(code, []).append((title, number))
            all_ids.append(code)
    if counts != {"已完成": 37, "待制作": 3, "制作中": 21, "待验收": 17}:
        fail(f"Counts {counts}")
    if len(all_ids) != 78 or len(set(all_ids)) != 78:
        fail("Status ids are not unique")
    if located.get("fire_flame_trail") != [("制作中", target_number)]:
        fail(f"fire_flame_trail at {located.get('fire_flame_trail')}")
    if located.get("rule_surface_anchor") != [("制作中", 3)]:
        fail(f"rule_surface_anchor at {located.get('rule_surface_anchor')}")
    for title in ("已完成", "待制作"):
        if originals[title] != sheet_ids(dst.read(status_parts[title]).decode("utf-8")):
            fail(f"{title} changed")
    if b"fire_flame_trail" in dst.read(status_parts["待验收"]):
        fail("Pending sheet still contains fire_flame_trail")
    for unchanged in (
        "xl/worksheets/sheet1.xml",
        "xl/worksheets/sheet2.xml",
        "xl/worksheets/sheet5.xml",
        "xl/worksheets/sheet6.xml",
        "xl/worksheets/sheet7.xml",
        "xl/comments/comment1.xml",
        "xl/comments/comment2.xml",
        "xl/comments/comment3.xml",
        "xl/comments/comment4.xml",
        "xl/styles.xml",
    ):
        if dst.read(unchanged) != src.read(unchanged):
            fail(f"{unchanged} changed")
    making_out = sheet_ids(dst.read(status_parts["制作中"]).decode("utf-8"))
    expected_codes = [code for code, _number, _row in originals["制作中"]] + ["fire_flame_trail"]
    if [code for code, _number, _row in making_out] != expected_codes:
        fail("制作中 order changed beyond appending fire_flame_trail")
    for code, number, original_row in originals["制作中"]:
        actual = next(row for actual_code, actual_number, row in making_out if actual_code == code)
        if code == "rule_surface_anchor":
            if actual != surface_edited:
                fail("Surface row differs from the DV-only edit")
        elif actual != original_row:
            fail(f"制作中 row for {code} changed")
    pending_out = sheet_ids(dst.read(status_parts["待验收"]).decode("utf-8"))
    expected_pending = [(code, number, row) for code, number, row in originals["待验收"] if code != "fire_flame_trail"]
    if pending_out != expected_pending:
        fail("待验收 rows changed beyond removing the last fire_flame_trail row")
    reopened = next(row for code, number, row in making_out if code == "fire_flame_trail")
    if cell_text(cell_xml(reopened, "DV", target_number)[0]) != old_dv + "\n" + flame_addition:
        fail("Reopened DV mismatch")
    if cell_text(cell_xml(reopened, "O", target_number)[0]) != o_merged:
        fail("Reopened O mismatch")

repair = {
    "schema": "camera-facing-repair-snapshot-20261113",
    "batchId": "待制作-20261031",
    "taskId": "camera_facing_tag_and_trail_fix",
    "attemptId": "20261113-sheet-facing-tag-01",
    "techniqueId": "fire_flame_trail",
    "source": {
        "sheet": "待验收",
        "row": 23,
        "columnsAtoENPresent": True,
        "nBefore": old_n,
        "oBefore": old_o,
        "dvBefore": old_dv,
    },
    "approvedSource": {
        "path": str(snapshot_path),
        "sha256": expected_snapshot,
        "nOriginal": n_original,
        "oOriginal": o_original,
    },
    "restored": {
        "n": n_original,
        "o": o_merged,
        "oRule": facing_sentence,
    },
    "userQuotes20261113": user_quotes,
    "facingRegistry": facing_registry,
    "unchangedExistingFacingEffects": [
        "SPARK",
        "SEED",
        "KINDLE",
        "DETONATE",
        "DUST",
        "FENTIAN_SKY_FLAME",
        "FIRE_SEED_ORBIT_FLAME",
        "WATER_PRISON_SHELL",
        "SWORD_RETURN_ALL_VORTEX",
    ],
    "compileReceipt": "docs/开发记录/术式批量制作/待制作-20261031/camera-facing-compile-20261113.md",
    "implementationReceipt": "docs/开发记录/术式批量制作/待制作-20261031/camera-facing-tag-and-trail-fix-20261113.md",
    "returnReason": (
        "v3 原 visual-handoff.md 写为直立面向相机，游戏实测发现面向相机导致斜向弹道断成水平阶梯；"
        "现改为 CameraFacingTag.NONE 沿世界弹道切向直立排布，朝向规则较原文字交接变化，"
        "且未经新的斜向三维预览确认及游戏复测，按流程第 5.1、8、9 节退回制作中。"
    ),
    "reviewBudget": "设计 2/2、实现 3/3 已用完，返修结果未追加复审",
    "baselineSha256": baseline,
    "backup": str(backup),
    "candidate": str(output),
}
repair_path.write_text(json.dumps(repair, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({
    "baseline": baseline,
    "baselineBytes": len(live_bytes),
    "baselineMtimeMs": int(live_stat.st_mtime * 1000),
    "backup": str(backup),
    "backupSha256": sha256(backup.read_bytes()),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "counts": counts,
    "changedParts": different,
    "targetRow": target_number,
    "nLength": len(n_original),
    "oLength": len(o_merged),
    "dvPrefixLength": len(old_dv),
    "surfaceDvPrefixLength": len(surface_dv),
}, ensure_ascii=False))
