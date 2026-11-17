# -*- coding: utf-8 -*-
"""Move fire_spark from 制作中 to 待验收 after the confirmed v3.1 archive."""
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
output = batch / "spark-crossed-v31-writeback-candidate-preserved.xlsx"
snapshot_path = batch / "spark-crossed-v31-requirements-snapshot-20261113.json"
script_path = batch / "spark-crossed-v31-writeback-20261113.py"
expected_baseline = "9F81EFB3A5E614F180444CB7F51858A7CD1D8C5275106CDE4D4B5BB735835C21"
expected_bytes = 178433
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
n_pending = "命中后火苗消失，并造成1点基础火伤、2点火种和原版燃烧。"
o_pending = "assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png"
dv_addition = (
    "[阶段 2026-11-13] 用户确认采用火苗v3.1十字穿插直射三维视口预览（原话：“采用”），已正式归档于 docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/，运行资源核对完成并补齐 fire_spark.png.mcmeta；compile-02（12:04:01–12:04:26 javac执行）与 compile-03（12:56:27–12:56:39 UP-TO-DATE）退出码均为0；本阶段额度已用完（设计2/2、实现3/3），返修结果未追加复审；待用户游戏实测。需求快照 docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-v31-requirements-snapshot-20261113.json。\n"
    "待验收 | 待制作-20261031 | docs/开发记录/术式批量制作/待制作-20261031/记录.md"
)


def fail(message):
    raise SystemExit(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()


def rows_of(sheet_xml):
    return re.findall(
        r"<(?:[\w]+:)?row\b[^>]*/>|<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>",
        sheet_xml,
    )


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


def replace_once(xml, old, new, label):
    count = xml.count(old)
    if count != 1:
        fail(f"{label} count is {count}")
    return xml.replace(old, new, 1)


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


def column_index(column):
    value = 0
    for character in column:
        value = value * 26 + ord(character) - 64
    return value


live_bytes = live.read_bytes()
live_stat = live.stat()
baseline = sha256(live_bytes)
if baseline != expected_baseline or len(live_bytes) != expected_bytes:
    fail(f"Live baseline changed before candidate build: {baseline} {len(live_bytes)}")

stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"spark-crossed-v31-writeback-术式制作表-{stamp}-1.xlsx"
if backup.exists() or output.exists() or snapshot_path.exists():
    fail("Output path already exists")
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
spark = [item for item in ids3 if item[0] == "fire_spark"]
if len(spark) != 1 or spark[0][1] != 28 or any(item[0] == "fire_spark" for item in ids4):
    fail(f"fire_spark location mismatch: {[(item[0], item[1]) for item in spark]}")
if len(ids3) != 25 or len(ids4) != 16:
    fail(f"Unexpected source counts: making={len(ids3)} pending={len(ids4)}")

source_number = 28
source = spark[0][2]
if "<f" in source:
    fail("fire_spark row contains a formula")
if cell_text(cell_xml(source, "A", source_number)[0]) != "火苗":
    fail("Spark name mismatch")
old_n = cell_text(cell_xml(source, "N", source_number)[0])
old_o = cell_text(cell_xml(source, "O", source_number)[0])
old_dv = cell_text(cell_xml(source, "DV", source_number)[0])
if len(old_n) != 455 or "火苗是独立的火属性攻击术式" not in old_n:
    fail(f"Making N is not the detailed requirement: {len(old_n)}")
if len(old_o) != 171 or "不声明持续面向玩家" not in old_o:
    fail(f"Making O is not the crossed-launch intent: {len(old_o)}")
if not old_dv.startswith("2026-10-30用户确认：“把火苗写入待制作表，同样只有5tickCD”") or "制作中 | 待制作-20261031" not in old_dv:
    fail("Making DV prefix is not intact")
if "[阶段 2026-11-13]" in old_dv or "待验收 | 待制作-20261031" in old_dv:
    fail("Making DV already contains the acceptance marker")

content_rows4 = [number for _code, number, _row in ids4]
target_number = max(content_rows4) + 1
if target_number != 23:
    fail(f"Expected target row 23, found {target_number}")
edited = set_cell(source, "N", source_number, n_pending)
edited = set_cell(edited, "O", source_number, o_pending)
edited = set_cell(edited, "DV", source_number, old_dv + "\n" + dv_addition)
moved = renumber(edited, source_number, target_number)
if cell_text(cell_xml(moved, "B", target_number)[0]) != "fire_spark":
    fail("Moved id changed")
if cell_text(cell_xml(moved, "N", target_number)[0]) != n_pending:
    fail("Moved N mismatch")
if cell_text(cell_xml(moved, "O", target_number)[0]) != o_pending:
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
source_position = next(index for index, row in enumerate(rows3) if row_number(row) == source_number)
if source_position != len(rows3) - 1:
    fail("fire_spark is not the last making row")
rows3_out = rows3[:source_position]
if any(row_number(row) == source_number for row in rows3_out):
    fail("Making source row was not removed")
rows4_out = rows4 + [moved]
sheet3_out = replace_sheet_rows(sheet3, rows3_out)
sheet4_out = replace_sheet_rows(sheet4, rows4_out)
sheet3_out = replace_once(sheet3_out, 'ref="A1:EN28"', 'ref="A1:EN27"', "制作中 dimension")
sheet4_out = replace_once(sheet4_out, 'ref="A1:EN20"', 'ref="A1:EN23"', "待验收 dimension")
validation_covers(sheet3_out, 27)
validation_covers(sheet4_out, target_number)

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
    if counts != {"已完成": 37, "待制作": 0, "制作中": 24, "待验收": 17}:
        fail(f"Counts {counts}")
    if len(all_ids) != 78 or len(set(all_ids)) != 78:
        fail("Status ids are not unique")
    if located.get("fire_spark") != [("待验收", target_number)]:
        fail(f"fire_spark at {located.get('fire_spark')}")
    for title in ("已完成", "待制作"):
        if originals[title] != sheet_ids(dst.read(status_parts[title]).decode("utf-8")):
            fail(f"{title} changed")
    if b"fire_spark" in dst.read(status_parts["制作中"]):
        fail("Making sheet still contains fire_spark")
    for unchanged in (
        "xl/worksheets/sheet1.xml",
        "xl/worksheets/sheet2.xml",
        "xl/worksheets/sheet5.xml",
        "xl/worksheets/sheet6.xml",
        "xl/worksheets/sheet7.xml",
        "xl/workbook.xml",
        "xl/styles.xml",
    ):
        if dst.read(unchanged) != src.read(unchanged):
            fail(f"{unchanged} changed")
    making_out = sheet_ids(dst.read(status_parts["制作中"]).decode("utf-8"))
    expected_codes = [code for code, _number, _row in originals["制作中"] if code != "fire_spark"]
    if [code for code, _number, _row in making_out] != expected_codes:
        fail("制作中 order changed beyond removing fire_spark")
    for code, number, original_row in originals["制作中"]:
        if code == "fire_spark":
            continue
        actual = next(row for actual_code, actual_number, row in making_out if actual_code == code)
        if actual != original_row or row_number(actual) != number:
            fail(f"制作中 row for {code} changed")
    pending_out = sheet_ids(dst.read(status_parts["待验收"]).decode("utf-8"))
    if pending_out[:-1] != originals["待验收"]:
        fail("待验收 existing rows changed")
    reopened = next(row for code, number, row in pending_out if code == "fire_spark")
    if cell_text(cell_xml(reopened, "DV", target_number)[0]) != old_dv + "\n" + dv_addition:
        fail("Reopened DV mismatch")
    if cell_text(cell_xml(reopened, "O", target_number)[0]) != o_pending:
        fail("Reopened O mismatch")
    if cell_text(cell_xml(reopened, "N", target_number)[0]) != n_pending:
        fail("Reopened N mismatch")

columns = {}
for column in sorted(set(re.findall(r'\br="([A-Z]+)28"', source)), key=column_index):
    columns[column] = cell_text(cell_xml(source, column, 28)[0])
if list(columns)[0] != "A" or list(columns)[-1] != "EM" or len(columns) != 40:
    fail(f"Unexpected source columns: {list(columns)}")

snapshot = {
    "schema": "spark-crossed-v31-requirements-snapshot-20261113",
    "batchId": "待制作-20261031",
    "taskId": "fire_spark_crossed_launch",
    "attemptId": "20261113-sheet-spark-cross-02",
    "techniqueId": "fire_spark",
    "source": {
        "sheet": "制作中",
        "row": 28,
        "columnsAtoENPresent": True,
        "valuesAtoEN": columns,
        "nBefore": old_n,
        "oBefore": old_o,
        "dvBefore": old_dv,
    },
    "userConfirmation": {
        "date": "2026-11-13",
        "verbatim": "采用",
        "subject": "火苗 v3.1 十字穿插直射真实 Blockbench 三维视口预览",
    },
    "formalArchive": "docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/",
    "runtimeResources": [
        "assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png",
        "assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png.mcmeta",
    ],
    "converted": {
        "targetSheet": "待验收",
        "targetRow": target_number,
        "n": n_pending,
        "o": o_pending,
        "dvPrefix": old_dv,
        "dvAddition": dv_addition,
    },
    "configuration": {
        "standard": [
            {"key": "damageMultiplier", "baseDamage": 1},
            {"key": "cooldownTicks", "default": 5, "unit": "tick", "range": [0, 1200000]},
            {"key": "manaCost", "default": 2, "range": [0, 1000000]},
        ],
        "carrier": {
            "declaration": "ListedRelease.declareCarrier(spark(), true)",
            "keys": [
                {"key": "lifetimeTicks", "default": 30, "unit": "tick"},
                {"key": "speed", "default": 0.8, "unit": "格/tick"},
                {"key": "thickness", "default": 0.25, "unit": "格"},
                {"key": "birthRadius", "default": 0.125, "unit": "格"},
                {"key": "seedCount", "default": 2, "kind": "整数计数", "range": [0, 100]},
            ],
            "consumption": "施放及命中结算实时消费",
        },
    },
    "resourceDetail": "资源状态明细无 fire_spark，原样未动",
    "reviewBudget": "设计 2/2、实现 3/3 已用完，返修结果未追加复审",
    "baselineSha256": baseline,
    "baselineBytes": len(live_bytes),
    "baselineMtimeMs": int(live_stat.st_mtime * 1000),
    "backup": str(backup),
    "candidate": str(output),
    "script": str(script_path),
}
snapshot_path.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({
    "baseline": baseline,
    "baselineBytes": len(live_bytes),
    "baselineMtimeMs": int(live_stat.st_mtime * 1000),
    "backup": str(backup),
    "backupSha256": sha256(backup.read_bytes()),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "snapshot": str(snapshot_path),
    "snapshotSha256": sha256(snapshot_path.read_bytes()),
    "counts": counts,
    "changedParts": different,
    "targetRow": target_number,
    "nBeforeLength": len(old_n),
    "oBeforeLength": len(old_o),
    "dvPrefixLength": len(old_dv),
    "columnCount": len(columns),
}, ensure_ascii=False))
