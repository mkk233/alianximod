# -*- coding: utf-8 -*-
"""Move fire_spark from 待验收 back to 制作中 after the crossed-launch repair."""
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
snapshot_path = root / "docs" / "开发记录" / "弹丸命中消耗-20261111" / "fire_spark-前置快照.md"
repair_path = batch / "spark-crossed-repair-snapshot-20261113.json"
output = batch / "spark-crossed-writeback-candidate-preserved.xlsx"
script_path = batch / "spark-crossed-writeback-20261113.py"
expected_baseline = "D91475C0B9BCB01878FDF74DDFAEAE975F19241968E2ACC6E3189C299DECEE60"
expected_bytes = 177691
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
facing_sentence = (
    "不声明持续面向玩家（CameraFacingTag.NONE），按发射方向一次性锁定生成原版箭矢式±45°十字穿插双面片"
    "（火头朝前、火尾朝后，渲染层保留眼下0.245格视觉偏置保证第一人称清晰可见），飞行全程直射不随视角转圈。"
)
dv_addition = (
    "[设计反馈 2026-11-13] 用户游戏内反馈：“修改朝向后我释放火苗发现火苗的特效现在会转圈，但是我的预期是火苗直射出去，同时为了让玩家第一人称可以看清特效做成箭矢那样的十字穿插，按照发射方向生成而不是持续面向玩家”。SPARK 已从持续面向镜头登记移出（CameraFacingTag.NONE），改为按发射方向一次性锁定生成 ±45° 十字穿插双面片，并在渲染层对齐眼下 0.245 格纯视觉偏置消除第一人称针孔共面退化（不改碰撞盒、弹道或留焰采样）。源码实现（impl-01/impl-02）与统一编译（compile-02 退出码 0）已完成；本阶段额度已用完（设计 2/2、实现 3/3），返修结果未追加复审。\n"
    "[状态纠正 / 美术进度 2026-11-13] 功能已实现并编译通过；二维像素复用已确认 v3（docs/资源库/特效/火焰/火苗_飞行火舌_v3/fire_spark_v3_strip.png）；十字穿插直射真实 Blockbench 三维视口示意已生成于 docs/资源库/过程归档/火苗_v3.1_十字穿插直射示意_20261113/，待用户视觉确认。因空间朝向与结构变化且新版三维示意尚未经用户确认，按流程第 5.1、8、9 节从待验收退回制作中；下一步：待用户确认 v3.1 三维示意后归档正式目录并按第 8 节迁回待验收。\n"
    "制作中 | 待制作-20261031 | docs/开发记录/术式批量制作/待制作-20261031/记录.md"
)
facing_registry = {
    "CAMERA_PLANE": [
        "SEED",
        "KINDLE",
        "DETONATE",
        "DUST",
        "FENTIAN_SKY_FLAME",
        "FIRE_SEED_ORBIT_FLAME",
        "WATER_PRISON_SHELL",
    ],
    "UPRIGHT_CAMERA_YAW": ["SWORD_RETURN_ALL_VORTEX"],
    "NONE": ["SPARK", "TRAIL", "SURFACE", "SHOCK", "NONE"],
}
dependencies = [
    {
        "attemptId": "20261113-impl-spark-cross-01",
        "agentId": "01a1d2d4-8b13-7ca2-a0a8-d24fe037bb1c",
        "receipt": "docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-launch-impl-20261113.md",
        "state": "已完成并关闭",
    },
    {
        "attemptId": "20261113-art-spark-cross-01",
        "agentId": "01a1d2d4-8bfb-7c61-9daa-72263eab534c",
        "receipt": "docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-3d-preview-20261113.md",
        "state": "已完成并关闭",
    },
    {
        "attemptId": "20261113-compile-spark-cross-01",
        "agentId": "01a1d2df-2b3f-7452-889c-8d14d2cbf6fd",
        "receipt": "docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-compile-20261113.md",
        "state": "已完成并关闭；退出码 1，外部 ResponseWindowService 阻塞，未改本项源码",
    },
    {
        "attemptId": "20261113-impl-spark-cross-02",
        "agentId": "01a1d30f-c537-7dd0-8b4f-a5dc7b7110e4",
        "receipt": "docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-launch-impl-02-20261113.md",
        "state": "已完成并关闭；渲染层眼下 0.245 格视觉偏置",
    },
    {
        "attemptId": "20261113-compile-spark-cross-02",
        "agentId": "01a1d311-b1b5-7a33-8295-e2010c6d0f5c",
        "receipt": "docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-compile-02-20261113.md",
        "state": "已完成并关闭；compileJava --offline --no-daemon 退出码 0",
    },
]


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
    escaped = html.escape(new_text, quote=False)
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
    updated, row_count = re.subn(
        rf'(<row\b[^>]*\br="){old}(")',
        lambda match: match.group(1) + str(new) + match.group(2),
        row_xml,
        count=1,
    )
    updated, ref_count = re.subn(
        rf'\br="([A-Z]+){old}"',
        lambda match: 'r="' + match.group(1) + str(new) + '"',
        updated,
    )
    if row_count != 1 or ref_count < 1 or row_number(updated) != new:
        fail(f"Row {old} did not become {new} cleanly")
    if re.search(rf'\br="{old}\b"', updated) or re.search(rf'\br="[A-Z]+{old}(?!\d)"', updated):
        fail(f"Row {old} left a stale reference while becoming {new}")
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


def approved_text(marker, nxt):
    source = snapshot_path.read_text(encoding="utf-8")
    block = source.split(marker, 1)[1].split(nxt, 1)[0]
    return block.strip("\r\n")


n_original = approved_text("### N 功能描述", "### O 特效")
o_original = approved_text("### O 特效", "### DV 备注")
if len(n_original) != 455 or "\n" not in n_original or "\r" in n_original:
    fail(f"Approved N changed: {len(n_original)}")
if len(o_original) != 57 or "\n" in o_original or "\r" in o_original:
    fail(f"Approved O changed: {len(o_original)}")
o_merged = o_original + "\n" + facing_sentence

live_bytes = live.read_bytes()
live_stat = live.stat()
baseline = sha256(live_bytes)
if baseline != expected_baseline or len(live_bytes) != expected_bytes:
    fail(f"Live baseline changed before candidate build: {baseline} {len(live_bytes)}")

stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"spark-crossed-writeback-术式制作表-{stamp}-1.xlsx"
if backup.exists() or output.exists() or repair_path.exists():
    fail("Output path already exists")
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)
if sha256(backup.read_bytes()) != baseline or backup.stat().st_size != len(live_bytes):
    fail("Backup hash mismatch")

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original_parts = {info.filename: archive.read(info.filename) for info in order}

if original_parts["xl/worksheets/sheet6.xml"].decode("utf-8").count("fire_spark") != 0:
    fail("Resource detail unexpectedly contains fire_spark")

sheet3 = original_parts[status_parts["制作中"]].decode("utf-8")
sheet4 = original_parts[status_parts["待验收"]].decode("utf-8")
ids3 = sheet_ids(sheet3)
ids4 = sheet_ids(sheet4)
flame = [item for item in ids4 if item[0] == "fire_spark"]
if len(flame) != 1 or flame[0][1] != 14 or any(item[0] == "fire_spark" for item in ids3):
    fail(f"fire_spark location mismatch: {[(item[0], item[1]) for item in flame]}")

source_number = 14
source = flame[0][2]
if "<f" in source:
    fail("fire_spark row contains a formula")
if cell_text(cell_xml(source, "A", source_number)[0]) != "火苗":
    fail("Spark name mismatch")
old_n = cell_text(cell_xml(source, "N", source_number)[0])
old_o = cell_text(cell_xml(source, "O", source_number)[0])
old_dv = cell_text(cell_xml(source, "DV", source_number)[0])
if old_n != "命中后火苗消失，并造成1点基础火伤、2点火种和原版燃烧。":
    fail("Pending N is not the acceptance sentence")
if old_o != "assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png":
    fail("Pending O is not the v3 resource id")
if not old_dv.startswith("2026-10-30用户确认：“把火苗写入待制作表，同样只有5tickCD”") or "待验收 | 弹丸命中消耗-20261111" not in old_dv:
    fail("Pending DV prefix is not intact")
if "十字穿插" in old_dv or "0.245" in old_dv:
    fail("Pending DV already contains the crossed-launch note")

content_rows3 = [number for _, number, _ in ids3]
target_number = max(content_rows3) + 1
if target_number != 28:
    fail(f"Expected target row 28, found {target_number}")
edited = set_cell(source, "N", source_number, n_original)
edited = set_cell(edited, "O", source_number, o_merged)
edited = set_cell(edited, "DV", source_number, old_dv + "\n" + dv_addition)
moved = renumber(edited, source_number, target_number)
if cell_text(cell_xml(moved, "B", target_number)[0]) != "fire_spark":
    fail("Moved id changed")
if cell_text(cell_xml(moved, "N", target_number)[0]) != n_original:
    fail("Moved N mismatch")
if cell_text(cell_xml(moved, "O", target_number)[0]) != o_merged:
    fail("Moved O mismatch")
moved_dv = cell_text(cell_xml(moved, "DV", target_number)[0])
if moved_dv != old_dv + "\n" + dv_addition or not moved_dv.startswith(old_dv):
    fail("Moved DV prefix was not preserved")
if len(re.findall(r"<c\b", moved)) != len(re.findall(r"<c\b", source)):
    fail("Moved row cell count changed")
if row_without_text(moved) != row_without_text(renumber(source, source_number, target_number)):
    fail("Moved row changed outside N/O/DV text")

rows3 = rows_of(sheet3)
rows4 = rows_of(sheet4)
source_position = next(index for index, row in enumerate(rows4) if row_number(row) == source_number)
rows4_out = rows4[:source_position] + rows4[source_position + 1:]
if any(row_number(row) == source_number for row in rows4_out):
    fail("Pending source row was not removed")
rows3_out = rows3 + [moved]
sheet3_out = replace_sheet_rows(sheet3, rows3_out)
sheet4_out = replace_sheet_rows(sheet4, rows4_out)
sheet3_out = sheet3_out.replace('<dimension ref="A1:EN27"/>', '<dimension ref="A1:EN28"/>', 1)
sheet4_out = sheet4_out.replace('<dimension ref="A1:EN22"/>', '<dimension ref="A1:EN20"/>', 1)
if sheet3_out.count('<dimension ref="A1:EN28"/>') != 1 or sheet4_out.count('<dimension ref="A1:EN20"/>') != 1:
    fail("Dimension replacement failed")
validation_covers(sheet3_out, target_number)
validation_covers(sheet4_out, 20)

changed = {
    status_parts["制作中"]: sheet3_out.encode("utf-8"),
    status_parts["待验收"]: sheet4_out.encode("utf-8"),
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
    allowed = [status_parts["制作中"], status_parts["待验收"]]
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
    if counts != {"已完成": 37, "待制作": 0, "制作中": 24, "待验收": 16}:
        fail(f"Counts {counts}")
    if len(all_ids) != 77 or len(set(all_ids)) != 77:
        fail("Status ids are not unique")
    if located.get("fire_spark") != [("制作中", target_number)]:
        fail(f"fire_spark at {located.get('fire_spark')}")
    for title in ("已完成", "待制作"):
        if originals[title] != sheet_ids(dst.read(status_parts[title]).decode("utf-8")):
            fail(f"{title} changed")
    if b"fire_spark" in dst.read(status_parts["待验收"]):
        fail("Pending sheet still contains fire_spark")
    for unchanged in (
        "xl/worksheets/sheet1.xml",
        "xl/worksheets/sheet2.xml",
        "xl/worksheets/sheet5.xml",
        "xl/worksheets/sheet6.xml",
        "xl/worksheets/sheet7.xml",
        "xl/workbook.xml",
        "xl/comments/comment1.xml",
        "xl/comments/comment2.xml",
        "xl/comments/comment3.xml",
        "xl/comments/comment4.xml",
        "xl/styles.xml",
    ):
        if dst.read(unchanged) != src.read(unchanged):
            fail(f"{unchanged} changed")
    making_out = sheet_ids(dst.read(status_parts["制作中"]).decode("utf-8"))
    expected_codes = [code for code, _number, _row in originals["制作中"]] + ["fire_spark"]
    if [code for code, _number, _row in making_out] != expected_codes:
        fail("制作中 order changed beyond appending fire_spark")
    for code, _number, original_row in originals["制作中"]:
        actual = next(row for actual_code, _actual_number, row in making_out if actual_code == code)
        if actual != original_row:
            fail(f"制作中 row for {code} changed")
    pending_out = sheet_ids(dst.read(status_parts["待验收"]).decode("utf-8"))
    expected_pending = [item for item in originals["待验收"] if item[0] != "fire_spark"]
    if pending_out != expected_pending:
        fail("待验收 rows changed beyond removing fire_spark")
    reopened = next(row for code, number, row in making_out if code == "fire_spark")
    if cell_text(cell_xml(reopened, "DV", target_number)[0]) != old_dv + "\n" + dv_addition:
        fail("Reopened DV mismatch")
    if cell_text(cell_xml(reopened, "O", target_number)[0]) != o_merged:
        fail("Reopened O mismatch")
    if cell_text(cell_xml(reopened, "N", target_number)[0]) != n_original:
        fail("Reopened N mismatch")

columns = {}
for column in re.findall(r'\br="([A-Z]+)14"', source):
    columns[column] = cell_text(cell_xml(source, column, 14)[0])

repair = {
    "schema": "spark-crossed-repair-snapshot-20261113",
    "batchId": "待制作-20261031",
    "taskId": "fire_spark_crossed_launch",
    "attemptId": "20261113-sheet-spark-cross-01",
    "techniqueId": "fire_spark",
    "source": {
        "sheet": "待验收",
        "row": 14,
        "columnsAtoENPresent": True,
        "valuesAtoEN": columns,
        "nBefore": old_n,
        "oBefore": old_o,
        "dvBefore": old_dv,
    },
    "approvedSource": {
        "path": str(snapshot_path),
        "sha256": sha256(snapshot_path.read_bytes()),
        "nOriginal": n_original,
        "oOriginal": o_original,
    },
    "restored": {
        "n": n_original,
        "o": o_merged,
        "oAddition": facing_sentence,
        "dvPrefix": old_dv,
        "dvAddition": dv_addition,
    },
    "facingRegistry": facing_registry,
    "dependencies": dependencies,
    "resourceDetail": "资源状态明细无 fire_spark，原样未动",
    "reviewBudget": "设计 2/2、实现 3/3 已用完，返修结果未追加复审",
    "baselineSha256": baseline,
    "baselineBytes": len(live_bytes),
    "baselineMtimeMs": int(live_stat.st_mtime * 1000),
    "backup": str(backup),
    "candidate": str(output),
    "script": str(script_path),
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
    "columnCount": len(columns),
}, ensure_ascii=False))
