# -*- coding: utf-8 -*-
"""Move only fire_cool from 制作中 to 待验收, preserving the workbook package."""
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
body = root / "src" / "main" / "java" / "com" / "bslx" / "XueXiMod" / "client" / "furnace" / "FurnaceBodyVisual.java"
layout = root / "src" / "main" / "java" / "com" / "bslx" / "XueXiMod" / "visual" / "fire" / "FireVisualLayout.java"
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
display_n = "主动冷却炉心，并放出一次蒸汽"
display_o = (
    "src/main/resources/assets/xueximod/textures/effect/cool_v41/cool_v4_1_rear_strip.png、"
    "src/main/resources/assets/xueximod/textures/effect/cool_v41/cool_v4_1_front_strip.png、"
    "src/main/resources/assets/xueximod/textures/effect/cool_v41/cool_v4_1_fragments_strip.png、"
    "src/main/resources/assets/xueximod/textures/effect/cool_v41/cool_v4_1_shockwave_strip.png"
    "（各配同名.png.mcmeta，blur=false、clamp=true；归档 docs/资源库/特效/火焰/冷却_贴地蒸汽冲击波_v4.1/）"
)
addition = (
    "[设计反馈 2026-11-12] 用户已采用冷却 v4.1。四张运行条带已接入 textures/effect/cool_v41/，"
    "与归档哈希一致。最终 .\\gradlew.bat compileJava --offline --no-daemon 于2026-11-12 13:21:15至13:21:41退出0，"
    "BUILD SUCCESSFUL，compileJava实际执行。本阶段额度已用完，返修结果未追加复审。游戏未实测，不迁已完成。"
    "详见 docs/开发记录/术式批量制作/火系六式-20261029/记录.md。\n"
    "待验收 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
)


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
    inline = re.search(r"<(?:[\w]+:)?is\b[^>]*>([\s\S]*)</(?:[\w]+:)?is>", cell)
    if inline:
        return html.unescape("".join(re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", inline.group(1))))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
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
    else:
        fail(f"Cell {column}{number} has no inline text node")
    if count != 1 or cell_text(updated) != new_text:
        fail(f"Cell {column}{number} text was not written cleanly")
    return updated


def set_cell(row_xml, column, number, new_text):
    cell, start, end = cell_xml(row_xml, column, number)
    return row_xml[:start] + replace_text(cell, column, number, new_text) + row_xml[end:]


def renumber(row_xml, old, new):
    updated = re.sub(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row_xml, count=1)
    updated = re.sub(rf'r="([A-Z]+){old}"', rf'r="\g<1>{new}"', updated)
    if row_number(updated) != new or updated.count(f'r="{old}"') or re.search(rf'r="[A-Z]+{old}"', updated):
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


def replace_dimension(xml, old_ref, new_ref):
    token = f'ref="{old_ref}"'
    if xml.count(token) != 1:
        fail(f"Dimension {old_ref} count is {xml.count(token)}")
    return xml.replace(token, f'ref="{new_ref}"', 1)


def validation_covers(xml, row):
    uncovered = []
    for ref in re.findall(r'<dataValidation\b[^>]*sqref="([^"]+)"', xml):
        for part in ref.split():
            bounds = re.match(r"([A-Z]+)(\d+):([A-Z]+)(\d+)$", part)
            if bounds and int(bounds.group(2)) <= row and int(bounds.group(4)) < row:
                uncovered.append(part)
    if uncovered:
        fail(f"Validation does not cover row {row}: {uncovered}")


if sha256(body.read_bytes()) != "689062B8CC9282655F1039B35D520C3277F5ED564DAFDDCFA526928142D3797C":
    fail("FurnaceBodyVisual hash changed")
if sha256(layout.read_bytes()) != "3845368C24FC0571AD507F17F0820DB4A587BF77FF12118EF91E29C082796ADB":
    fail("FireVisualLayout hash changed")

live_bytes = live.read_bytes()
live_stat = live.stat()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"cool-v41-writeback-术式制作表-{stamp}-1.xlsx"
output = batch / "cool-v41-writeback-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
if backup.exists() or output.exists():
    fail("Output path already exists")
backup.write_bytes(live_bytes)
if sha256(backup.read_bytes()) != baseline:
    fail("Backup hash mismatch")

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original_parts = {info.filename: archive.read(info.filename) for info in order}

if b"fire_cool" in original_parts["xl/worksheets/sheet6.xml"]:
    fail("Resource detail has fire_cool; this script does not move detail rows")

sheet3 = original_parts[status_parts["制作中"]].decode("utf-8")
sheet4 = original_parts[status_parts["待验收"]].decode("utf-8")
rows3 = rows_of(sheet3)
rows4 = rows_of(sheet4)
ids3 = sheet_ids(sheet3)
ids4 = sheet_ids(sheet4)
cool = [item for item in ids3 if item[0] == "fire_cool"]
if len(cool) != 1 or any(item[0] == "fire_cool" for item in ids4):
    fail(f"fire_cool location is not unique in 制作中: {cool}")
source_number = cool[0][1]
source = cool[0][2]
if "<f" in source:
    fail("fire_cool row contains a formula")
if cell_text(cell_xml(source, "A", source_number)[0]) != "冷却":
    fail("Name mismatch")
old_n = cell_text(cell_xml(source, "N", source_number)[0])
old_o = cell_text(cell_xml(source, "O", source_number)[0])
old_dv = cell_text(cell_xml(source, "DV", source_number)[0])
if not old_n.startswith("冷却是独立的火属性法术") or len(old_n) != 709:
    fail("Live N changed")
if not old_o.startswith("成功退出炉心时") or len(old_o) != 76:
    fail("Live O changed")
if not old_dv.startswith("本行是冷却 / fire_cool。") or len(old_dv) != 1895:
    fail("Live DV changed")
if "待验收 | 火系六式-20261029" in old_dv:
    fail("Acceptance marker already present")

target_number = max(number for _, number, _ in ids4) + 1
edited = set_cell(source, "N", source_number, display_n)
edited = set_cell(edited, "O", source_number, display_o)
edited = set_cell(edited, "DV", source_number, old_dv + "\n" + addition)
moved = renumber(edited, source_number, target_number)
if cell_text(cell_xml(moved, "B", target_number)[0]) != "fire_cool":
    fail("Moved id changed")
if cell_text(cell_xml(moved, "N", target_number)[0]) != display_n:
    fail("Moved N mismatch")
if cell_text(cell_xml(moved, "O", target_number)[0]) != display_o:
    fail("Moved O mismatch")
remark = cell_text(cell_xml(moved, "DV", target_number)[0])
if not remark.startswith(old_dv + "\n") or not remark.endswith(addition):
    fail("Moved DV was not preserved")

source_position = next(index for index, row in enumerate(rows3) if row_number(row) == source_number)
rows3_out = rows3[:source_position] + [
    renumber(row, row_number(row), row_number(row) - 1)
    for row in rows3[source_position + 1:]
]
rows4_out = rows4 + [moved]
sheet3_out = replace_sheet_rows(sheet3, rows3_out)
sheet4_out = replace_sheet_rows(sheet4, rows4_out)
sheet3_out = replace_dimension(sheet3_out, "A1:EN25", "A1:EN24")
sheet4_out = replace_dimension(sheet4_out, "A1:EN20", f"A1:EN{target_number}")
validation_covers(sheet4_out, target_number)

workbook = original_parts["xl/workbook.xml"].decode("utf-8")
old_filter = "'制作中'!$A$2:$EN$5"
new_filter = "'制作中'!$A$2:$EN$4"
if workbook.count(old_filter) != 1:
    fail("制作中 filter range missing or duplicated")
workbook_out = workbook.replace(old_filter, new_filter, 1)

changed = {
    status_parts["制作中"]: sheet3_out.encode("utf-8"),
    status_parts["待验收"]: sheet4_out.encode("utf-8"),
    "xl/workbook.xml": workbook_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

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
    if located.get("fire_cool") != [("待验收", target_number)]:
        fail(f"fire_cool at {located.get('fire_cool')}")
    for title in ("已完成", "待制作"):
        if [(code, number) for code, number, _row in originals[title]] != [
            (code, number) for code, number, _row in sheet_ids(dst.read(status_parts[title]).decode("utf-8"))
        ]:
            fail(f"{title} ids moved")
    if b"fire_cool" in dst.read("xl/worksheets/sheet3.xml"):
        fail("Source sheet still contains fire_cool")
    if dst.read("xl/worksheets/sheet5.xml") != src.read("xl/worksheets/sheet5.xml"):
        fail("Data sheet changed")
    if dst.read("xl/worksheets/sheet6.xml") != src.read("xl/worksheets/sheet6.xml"):
        fail("Resource detail changed")
    if dst.read("xl/worksheets/sheet7.xml") != src.read("xl/worksheets/sheet7.xml"):
        fail("Archive sheet changed")
    if dst.read("xl/comments/comment3.xml") != src.read("xl/comments/comment3.xml"):
        fail("Comments changed")
    making_out = {number: row for _code, number, row in sheet_ids(dst.read(status_parts["制作中"]).decode("utf-8"))}
    expected_making = [(code, number - 1 if number > source_number else number, row) for code, number, row in originals["制作中"] if code != "fire_cool"]
    actual_making = sheet_ids(dst.read(status_parts["制作中"]).decode("utf-8"))
    if [(code, number) for code, number, _row in actual_making] != [(code, number) for code, number, _row in expected_making]:
        fail("制作中 order changed beyond closing the fire_cool gap")
    for code, number, original_row in expected_making:
        actual_row = making_out[number]
        original_number = number + 1 if number >= source_number else number
        if actual_row != renumber(original_row, original_number, number):
            fail(f"制作中 row for {code} changed beyond renumbering")
    pending_out = {number: row for _code, number, row in sheet_ids(dst.read(status_parts["待验收"]).decode("utf-8"))}
    for code, number, original_row in originals["待验收"]:
        if pending_out[number] != original_row:
            fail(f"待验收 existing row {code} changed")

meta = {
    "capturedBeforeLiveWrite": True,
    "baselineSha256": baseline,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "source": {"sheet": "制作中", "row": source_number, "id": "fire_cool", "name": "冷却"},
    "target": {"sheet": "待验收", "row": target_number, "id": "fire_cool", "name": "冷却"},
    "countsBefore": {title: len(items) for title, items in originals.items()},
    "countsCandidate": counts,
    "nBefore": old_n,
    "oBefore": old_o,
    "dvBefore": old_dv,
    "nAfter": display_n,
    "oAfter": display_o,
    "dvAddition": addition,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "changedParts": different,
    "filter": {"before": old_filter, "after": new_filter},
}
(batch / "cool-v41-writeback-candidate-meta-20261112.json").write_text(
    json.dumps(meta, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)
print(json.dumps({
    "baseline": baseline,
    "sourceRow": source_number,
    "targetRow": target_number,
    "counts": counts,
    "backup": str(backup),
    "candidate": str(output),
}, ensure_ascii=False))
