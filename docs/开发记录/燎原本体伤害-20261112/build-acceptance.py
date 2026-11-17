# -*- coding: utf-8 -*-
"""Move only fire_liaoyuan from 制作中 back to 待验收."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
task = root / "docs" / "开发记录" / "燎原本体伤害-20261112"
live = root / "docs" / "术式制作表.xlsx"
snapshot_path = task / "需求快照-合并后.json"
lang_path = root / "src" / "main" / "resources" / "assets" / "xueximod" / "lang" / "zh_cn.json"
expected_baseline = "BD92D6AB57731DCA6D15656FD47AF6022AB8397A5EBCFBB1E746BC9A79EDFA2D"
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
resource_names = [
    "textures/effect/liaoyuan_v3/ground_ash.png",
    "textures/effect/liaoyuan_v3/ground_ash_b.png",
    "textures/effect/liaoyuan_v3/ground_ash_c.png",
    "textures/effect/liaoyuan_v3/ground_ash_d.png",
    "textures/effect/liaoyuan_v3/ground_embers.png",
    "textures/effect/liaoyuan_v2/spread_wave.png",
    "textures/effect/liaoyuan_v2/ground_flame.png",
    "textures/effect/liaoyuan_v2/ground_embers.png",
]


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


def set_cell(row_xml, column, number, new_text):
    start, end = cell_bounds(row_xml, column, number)
    updated = replace_inline(row_xml[start:end], column, number, new_text)
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
lang = json.loads(lang_path.read_text(encoding="utf-8"))
display_n = lang["technique.xueximod.fire_liaoyuan.desc"]
expected_n = "铺开半径10格的贴地火场，展开前沿即可命中。本体基础4点、每10刻一次并受一次炉心加成；有火种时足额段为4×(1+0.1n+F)，每段请求10点火种"
if display_n != expected_n:
    fail("Game text is not the expected one-sentence description")
for relative in resource_names:
    if not (root / "src" / "main" / "resources" / "assets" / "xueximod" / relative).is_file():
        fail(f"Missing resource {relative}")
display_o = "\n".join(f"xueximod:{relative}" for relative in resource_names)

live_bytes = live.read_bytes()
live_stat = live.stat()
baseline = sha256(live_bytes)
if baseline != expected_baseline or len(live_bytes) != 181761:
    fail(f"Live workbook is not the required baseline: {baseline}")
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = task / "备份" / f"术式制作表-{stamp}-3.xlsx"
output = task / "术式制作表-待验收候选.xlsx"
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
    fail("Resource detail has fire_liaoyuan")

sheet3 = original_parts[status_parts["制作中"]].decode("utf-8")
sheet4 = original_parts[status_parts["待验收"]].decode("utf-8")
ids3 = sheet_ids(sheet3)
ids4 = sheet_ids(sheet4)
found = [item for item in ids3 if item[0] == "fire_liaoyuan"]
if len(found) != 1 or any(item[0] == "fire_liaoyuan" for item in ids4):
    fail("fire_liaoyuan is not uniquely in 制作中")
source_number, source = found[0][1], found[0][2]
if source_number != 25:
    fail(f"Expected 制作中 row 25, found {source_number}")
if "<f" in source:
    fail("Row contains a formula")
if cell_text(cell_xml(source, "A", source_number)) != "燎原":
    fail("Name mismatch")
if cell_text(cell_xml(source, "E", source_number)) != "4" or 't="n"' not in cell_xml(source, "E", source_number):
    fail("Column E is not numeric 4")
current_n = cell_text(cell_xml(source, "N", source_number))
current_o = cell_text(cell_xml(source, "O", source_number))
current_dv = cell_text(cell_xml(source, "DV", source_number))
if current_n != snapshot["mergedN"] or current_o != snapshot["mergedO"]:
    fail("Current detailed N/O does not match the saved merged snapshot")
if not current_dv.startswith("用户原话：“这样的设计好"):
    fail("DV prefix changed")
if "待验收 | 火系六式-20261029" in current_dv.split("制作中 | 火系六式-20261029")[-1]:
    fail("Acceptance marker already follows the latest return marker")

addition = (
    "[设计反馈 2026-11-12] 本体基础已按用户要求合并为每10tick 4点。火种附着段仍是4×(1+0.1n+F)、"
    "每段请求10点、周期20tick、首次延迟0；火场寿命200tick，半径10，焚天不变。"
    "N改为与technique.xueximod.fire_liaoyuan.desc一致的游戏内文案，O改为当前运行贴图。"
    "compileJava --offline --no-daemon于2026-11-12 14:42:41.448至14:43:04.937退出0，BUILD SUCCESSFUL in 22s；"
    "src/main共1373个文件，编译前后差异0。本阶段额度已用完，返修结果未追加复审。"
    "未启动游戏，未游戏实测，不迁已完成。详见 docs/开发记录/术式批量制作/火系六式-20261029/记录.md。\n"
    "待验收 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
)
edited = set_cell(source, "N", source_number, display_n)
edited = set_cell(edited, "O", source_number, display_o)
edited = set_cell(edited, "DV", source_number, current_dv + "\n" + addition)
target_number = max(number for _code, number, _row in ids4) + 1
moved = renumber(edited, source_number, target_number)
if cell_text(cell_xml(moved, "E", target_number)) != "4":
    fail("Moved E changed")
if cell_text(cell_xml(moved, "N", target_number)) != display_n:
    fail("Moved N mismatch")
if cell_text(cell_xml(moved, "O", target_number)) != display_o:
    fail("Moved O mismatch")
if not cell_text(cell_xml(moved, "DV", target_number)).startswith(current_dv + "\n"):
    fail("Moved DV did not preserve the original")
for column in ("A", "B", "C", "D", "F", "AS", "BV", "BW", "EB", "EC", "EG", "EH"):
    if cell_text(cell_xml(moved, column, target_number)) != cell_text(cell_xml(source, column, source_number)):
        fail(f"Column {column} changed")

rows3 = rows_of(sheet3)
source_position = next(index for index, row in enumerate(rows3) if row_number(row) == source_number)
if source_position != len(rows3) - 1:
    fail("fire_liaoyuan is not the last 制作中 row")
sheet3_out = replace_sheet_rows(sheet3, rows3[:source_position])
sheet4_out = replace_sheet_rows(sheet4, rows_of(sheet4) + [moved])
sheet3_out = replace_once(sheet3_out, 'ref="A1:EN25"', 'ref="A1:EN24"', "制作中 dimension")
sheet4_out = replace_once(sheet4_out, 'ref="A1:EN21"', f'ref="A1:EN{target_number}"', "待验收 dimension")

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
    if different != [status_parts["制作中"], status_parts["待验收"]]:
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
    if counts != {"已完成": 37, "待制作": 3, "制作中": 21, "待验收": 17}:
        fail(f"Counts {counts}")
    if len(all_ids) != 78 or len(set(all_ids)) != 78:
        fail("Status ids are not unique")
    if located.get("fire_liaoyuan") != [("待验收", target_number)]:
        fail(f"fire_liaoyuan at {located.get('fire_liaoyuan')}")
    if b"fire_liaoyuan" in dst.read(status_parts["制作中"]):
        fail("制作中 still contains fire_liaoyuan")
    for name in ("xl/worksheets/sheet5.xml", "xl/worksheets/sheet6.xml", "xl/worksheets/sheet7.xml", "xl/styles.xml", "xl/workbook.xml"):
        if dst.read(name) != src.read(name):
            fail(f"{name} changed")
    for index in range(1, 5):
        name = f"xl/comments/comment{index}.xml"
        if dst.read(name) != src.read(name):
            fail(f"{name} changed")
    making_rows = {number: row for _code, number, row in sheet_ids(dst.read(status_parts["制作中"]).decode("utf-8"))}
    for code, number, original_row in originals["制作中"]:
        if code == "fire_liaoyuan":
            continue
        if making_rows.get(number) != original_row:
            fail(f"制作中 row {code} changed")
    pending_rows = {number: row for _code, number, row in sheet_ids(dst.read(status_parts["待验收"]).decode("utf-8"))}
    for code, number, original_row in originals["待验收"]:
        if pending_rows.get(number) != original_row:
            fail(f"待验收 row {code} changed")

meta = {
    "attempt": "sheet-acceptance-01",
    "baselineSha256": baseline,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "source": {"sheet": "制作中", "row": source_number},
    "target": {"sheet": "待验收", "row": target_number},
    "countsCandidate": counts,
    "e": "4",
    "nBeforeLength": len(current_n),
    "oBeforeLength": len(current_o),
    "nAfter": display_n,
    "oAfter": display_o,
    "dvAddition": addition,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "changedParts": different,
    "submitted": False,
}
(task / "acceptance-candidate-meta.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
(task / "待验收-转换.json").write_text(json.dumps({
    "mergedSnapshot": str(snapshot_path),
    "beforeN": current_n,
    "beforeO": current_o,
    "afterN": display_n,
    "afterO": display_o,
    "e": "4",
}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({"baseline": baseline, "sourceRow": source_number, "targetRow": target_number, "counts": counts, "candidateSha256": meta["candidateSha256"]}, ensure_ascii=False))
