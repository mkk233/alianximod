# -*- coding: utf-8 -*-
"""Move only fire_liaoyuan from 待验收 back to 制作中."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
task = root / "docs" / "开发记录" / "燎原本体伤害-20261112"
batch = root / "docs" / "开发记录" / "术式批量制作" / "火系六式-20261029"
live = root / "docs" / "术式制作表.xlsx"
snapshot_path = root / "docs" / "开发记录" / "燎原伤害定稿入表-20261031" / "实施-交付快照.json"
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}


def fail(message):
    raise SystemExit(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()


def rows_of(sheet_xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet_xml)


def row_number(row_xml):
    return int(re.search(r'\br="(\d+)"', row_xml).group(1))


def cell_bounds(row_xml, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row_xml))
    if len(matches) != 1:
        fail(f"Cell {column}{number} count is {len(matches)}")
    start = matches[0].start()
    opener_end = row_xml.find(">", start)
    if row_xml[opener_end - 1] == "/":
        return start, opener_end + 1
    close = re.search(r"</(?:[\w]+:)?c>", row_xml[opener_end:])
    if not close:
        fail(f"Cell {column}{number} is not closed")
    return start, opener_end + close.end()


def cell_xml(row_xml, column, number):
    start, end = cell_bounds(row_xml, column, number)
    return row_xml[start:end]


def cell_text(cell):
    texts = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", cell)
    if texts:
        return html.unescape("".join(texts))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    return html.unescape(value.group(1)) if value else ""


def xml_text(value):
    return html.escape(value, quote=False).replace("\n", "&#10;")


def replace_inline(cell, column, number, new_text):
    node = re.search(r"<(?:[\w]+:)?t\b([^>]*)>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if not node or cell.count("<t") != 1:
        fail(f"Cell {column}{number} does not have exactly one text node")
    updated = cell[:node.start(2)] + xml_text(new_text) + cell[node.end(2):]
    if cell_text(updated) != new_text:
        fail(f"Cell {column}{number} text was not written cleanly")
    return updated


def replace_number(cell, column, number, new_text):
    node = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if not node or cell.count("<v") != 1 or 't="n"' not in cell:
        fail(f"Cell {column}{number} is not a single numeric value")
    updated = cell[:node.start(1)] + new_text + cell[node.end(1):]
    if cell_text(updated) != new_text:
        fail(f"Cell {column}{number} number was not written cleanly")
    return updated


def set_cell(row_xml, column, number, new_text, numeric=False):
    start, end = cell_bounds(row_xml, column, number)
    cell = row_xml[start:end]
    updated = replace_number(cell, column, number, new_text) if numeric else replace_inline(cell, column, number, new_text)
    return row_xml[:start] + updated + row_xml[end:]


def renumber(row_xml, old, new):
    updated = re.sub(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row_xml, count=1)
    updated = re.sub(rf'r="([A-Z]+){old}"', rf'r="\g<1>{new}"', updated)
    if row_number(updated) != new or re.search(rf'\br="{old}"', updated) or re.search(rf'r="[A-Z]+{old}"', updated):
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
        code = cell_text(cell_xml(row_xml, "B", number)).strip()
        if code:
            found.append((code, number, row_xml))
    return found


def replace_once(xml, old, new, label):
    if xml.count(old) != 1:
        fail(f"{label} count is {xml.count(old)}")
    return xml.replace(old, new, 1)


snapshot = json.loads(snapshot_path.read_text(encoding="utf-8"))
approved_n = snapshot["sheetOriginal"]["N"]
approved_o = snapshot["sheetOriginal"]["O"]
replacements = [
    ("本体基础火伤8点，接受一次炉心加成，每次8×(1+F)。过载满热F=0.60时为12.8点。", "本体基础火伤4点，接受一次炉心加成，每次4×(1+F)。过载满热F=0.60时为6.4点。"),
    ("本体8×1.60×20＝256，附着10.4×10＝104，合计360点理论火伤。展开首伤含在这20次里，不另加12.8。", "本体4×1.60×20＝128，附着10.4×10＝104，合计232点理论火伤。展开首伤含在这20次里，不另加6.4。"),
    ("刚开启过载而未满热，或本体不足20次，不能套360。", "刚开启过载而未满热，或本体不足20次，不能套232。"),
]
merged_n = approved_n
for old, new in replacements:
    if merged_n.count(old) != 1:
        fail(f"Approved phrase missing: {old}")
    merged_n = merged_n.replace(old, new, 1)
lead = "按2026-10-31已批准的伤害翻倍、半径10和灰烬覆盖返修。"
lead_new = "按2026-10-31已批准的半径10和灰烬覆盖返修，并合并2026-11-12本体基础改为每10tick 4点。"
if merged_n.count(lead) != 1:
    fail("Approved lead missing")
merged_n = merged_n.replace(lead, lead_new, 1)
if "本体基础火伤4点" not in merged_n or "每次4×(1+F)" not in merged_n:
    fail("Merged body damage missing")
if any(token in merged_n for token in ("本体基础火伤8点", "每次8×(1+F)", "为12.8点", "本体8×1.60×20", "不能套360", "不另加12.8")):
    fail("Old body-damage phrase survived")
for preserved in ("4×(1+0.1n+F)", "每10tick", "周期20tick", "200tick", "半径10", "最多8条", "焚天"):
    if preserved not in merged_n:
        fail(f"Preserved phrase missing: {preserved}")

live_bytes = live.read_bytes()
live_stat = live.stat()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = task / "备份" / f"术式制作表-{stamp}-1.xlsx"
output = task / "术式制作表-候选.xlsx"
backup.parent.mkdir(parents=True, exist_ok=True)
if backup.exists() or output.exists():
    fail("Output path already exists")
backup.write_bytes(live_bytes)
if sha256(backup.read_bytes()) != baseline:
    fail("Backup hash mismatch")

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original_parts = {info.filename: archive.read(info.filename) for info in order}

if b"fire_liaoyuan" in original_parts["xl/worksheets/sheet6.xml"]:
    fail("Resource detail has fire_liaoyuan; this script does not move detail rows")

sheet3 = original_parts[status_parts["制作中"]].decode("utf-8")
sheet4 = original_parts[status_parts["待验收"]].decode("utf-8")
ids3 = sheet_ids(sheet3)
ids4 = sheet_ids(sheet4)
found = [item for item in ids4 if item[0] == "fire_liaoyuan"]
if len(found) != 1 or any(item[0] == "fire_liaoyuan" for item in ids3):
    fail(f"fire_liaoyuan location is not unique in 待验收: {[(item[0], item[1]) for item in found]}")
source_number, source = found[0][1], found[0][2]
if source_number != 6:
    fail(f"Expected 待验收 row 6, found {source_number}")
if "<f" in source:
    fail("fire_liaoyuan row contains a formula")
if cell_text(cell_xml(source, "A", source_number)) != "燎原":
    fail("Name mismatch")
if cell_text(cell_xml(source, "E", source_number)) != "8":
    fail("Column E is not the expected body base 8")
current_n = cell_text(cell_xml(source, "N", source_number))
current_o = cell_text(cell_xml(source, "O", source_number))
current_dv = cell_text(cell_xml(source, "DV", source_number))
expected_n = snapshot["acceptanceConversion"]["afterN"]
expected_o = "\n".join(snapshot["acceptanceConversion"]["afterO"])
if current_n != expected_n or current_o != expected_o:
    fail("Current acceptance display does not match the 2026-10-31 snapshot")
if not current_dv.startswith("用户原话：“这样的设计好"):
    fail("Current DV prefix changed")

addition = (
    "[设计反馈 2026-11-12] 用户原话要求燎原火场本体伤害由每10tick 8点改为每10tick 4点；"
    "火种附着段、间隔、寿命、半径和焚天不变。问题是待验收展示仍写本体基础8点。"
    "影响是本体基础、炉心加成后的本体值和完整20次示例合计改为4点、6.4点和232点；"
    "附着燃烧仍是4×(1+0.1n+F)、每段请求10点、周期20tick、首次延迟0，"
    "火场寿命200tick、半径10、焚天不变。已按最近一次已批准详细需求恢复N/O并合并这次修改，"
    "E列基础伤害改为4。没有待确认项。源码和编译尚未开始，不写编译通过，不迁待验收或已完成。"
    "本阶段额度已用完，返修结果未追加复审。详见 docs/开发记录/术式批量制作/火系六式-20261029/记录.md。\n"
    "制作中 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
)
merged_dv = current_dv + "\n" + addition

target_number = max(number for _code, number, _row in ids3) + 1
edited = source
edited = set_cell(edited, "E", source_number, "4", numeric=True)
edited = set_cell(edited, "N", source_number, merged_n)
edited = set_cell(edited, "O", source_number, approved_o)
edited = set_cell(edited, "DV", source_number, merged_dv)
edited = re.sub(r'(<row\b[^>]*\bht=")[^"]+(")', r'\g<1>409.6\2', edited, count=1)
moved = renumber(edited, source_number, target_number)
if cell_text(cell_xml(moved, "B", target_number)) != "fire_liaoyuan":
    fail("Moved id changed")
if cell_text(cell_xml(moved, "E", target_number)) != "4":
    fail("Moved E mismatch")
if cell_text(cell_xml(moved, "N", target_number)) != merged_n:
    fail("Moved N mismatch")
if cell_text(cell_xml(moved, "O", target_number)) != approved_o:
    fail("Moved O mismatch")
if cell_text(cell_xml(moved, "DV", target_number)) != merged_dv:
    fail("Moved DV mismatch")
for column in ("A", "B", "C", "D", "F", "AS", "BV", "BW", "EB", "EC", "EG", "EH"):
    if cell_text(cell_xml(moved, column, target_number)) != cell_text(cell_xml(source, column, source_number)):
        fail(f"Column {column} changed")

rows4 = rows_of(sheet4)
source_position = next(index for index, row in enumerate(rows4) if row_number(row) == source_number)
rows4_out = rows4[:source_position] + rows4[source_position + 1:]
rows3_out = rows_of(sheet3) + [moved]
sheet4_out = replace_sheet_rows(sheet4, rows4_out)
sheet3_out = replace_sheet_rows(sheet3, rows3_out)
sheet4_out = replace_once(sheet4_out, 'ref="A1:EN21"', 'ref="A1:EN20"', "待验收 dimension")
sheet3_out = replace_once(sheet3_out, 'ref="A1:EN24"', f'ref="A1:EN{target_number}"', "制作中 dimension")

changed = {
    status_parts["制作中"]: sheet3_out.encode("utf-8"),
    status_parts["待验收"]: sheet4_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

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
        current = sheet_ids(dst.read(name).decode("utf-8"))
        originals[title] = sheet_ids(src.read(name).decode("utf-8"))
        counts[title] = len(current)
        for code, number, _row in current:
            located.setdefault(code, []).append((title, number))
            all_ids.append(code)
    if counts != {"已完成": 37, "待制作": 3, "制作中": 22, "待验收": 16}:
        fail(f"Counts {counts}")
    if len(all_ids) != 78 or len(set(all_ids)) != 78:
        fail("Status ids are not unique")
    if located.get("fire_liaoyuan") != [("制作中", target_number)]:
        fail(f"fire_liaoyuan at {located.get('fire_liaoyuan')}")
    if b"fire_liaoyuan" in dst.read(status_parts["待验收"]):
        fail("待验收 still contains fire_liaoyuan")
    for name in ("xl/worksheets/sheet5.xml", "xl/worksheets/sheet6.xml", "xl/worksheets/sheet7.xml", "xl/styles.xml", "xl/workbook.xml"):
        if dst.read(name) != src.read(name):
            fail(f"{name} changed")
    for index in range(1, 5):
        name = f"xl/comments/comment{index}.xml"
        if dst.read(name) != src.read(name):
            fail(f"{name} changed")
    making_rows = {number: row for _code, number, row in sheet_ids(dst.read(status_parts["制作中"]).decode("utf-8"))}
    for code, number, original_row in originals["制作中"]:
        if making_rows[number] != original_row:
            fail(f"制作中 existing row {code} changed")
    pending_rows = {number: row for _code, number, row in sheet_ids(dst.read(status_parts["待验收"]).decode("utf-8"))}
    for code, number, original_row in originals["待验收"]:
        if code == "fire_liaoyuan":
            continue
        if pending_rows.get(number) != original_row:
            fail(f"待验收 existing row {code} changed")

meta = {
    "capturedBeforeLiveWrite": True,
    "baselineSha256": baseline,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "source": {"sheet": "待验收", "row": source_number, "id": "fire_liaoyuan", "name": "燎原"},
    "target": {"sheet": "制作中", "row": target_number, "id": "fire_liaoyuan", "name": "燎原"},
    "countsBefore": {title: len(items) for title, items in originals.items()},
    "countsCandidate": counts,
    "approvedSnapshot": str(snapshot_path),
    "eBefore": "8",
    "eAfter": "4",
    "nBefore": current_n,
    "oBefore": current_o,
    "nAfter": merged_n,
    "oAfter": approved_o,
    "dvAddition": addition,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "changedParts": different,
    "compiled": False,
    "submitted": False,
}
(task / "candidate-meta.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
(task / "需求快照-合并后.json").write_text(json.dumps({
    "sourceSnapshot": str(snapshot_path),
    "approvedN": approved_n,
    "approvedO": approved_o,
    "mergedN": merged_n,
    "mergedO": approved_o,
    "currentDisplayN": current_n,
    "currentDisplayO": current_o,
    "currentDVPrefix": current_dv[:80],
    "dvAddition": addition,
}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({
    "baseline": baseline,
    "sourceRow": source_number,
    "targetRow": target_number,
    "counts": counts,
    "candidate": str(output),
    "nLength": len(merged_n),
    "oLength": len(approved_o),
}, ensure_ascii=False))
