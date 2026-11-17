# -*- coding: utf-8 -*-
"""Append the imprint-desk feedback to three DV cells. Does not move rows or change N/O."""
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
expected = "8B2CE30DA8263C3FCBB1AFD4E162EE953D7141EFF5FE433B9AB4995511B61054"
report = "docs/开发记录/术式批量制作/火系六式-20261029/unlock-repair-report.md"
marker = "刻印台实测反馈"
targets = [
    (3, "燎原", "fire_liaoyuan"),
    (4, "燎里", "fire_liaoli"),
    (5, "焚天", "fire_fentian"),
]
staying = [
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
tail = (
    "/shu stone 只给物品，不写学习名单。"
    "若当前游戏 /shu list 没有对应编号，运行版本另需排查；本单没有操作或验证游戏。"
    "获取方式原备注仍待用户另定，不自动改为满级授予，也不把作者建议写成用户新批准。"
    "待重测。先前未满足待验收的原因保留：缺适用正式实现复核通过记录，相关关键返修未追加复审。"
    "不因这次前置定位宣称六式通过。"
    f"详见 {report}。"
)
additions = {
    "fire_liaoyuan": (
        "[设计反馈 2026-10-29] 刻印台实测反馈，不是验收通过，本式仍留在制作中，不转换N/O。"
        "用户当时用的是 /shu rank sword 20 和 /shu rank magic 20，刻印台仍没有燎原。"
        "作者定位且未改生产或测试源码：当前实现把等级门槛和已学会分开，刻印台同时要求；"
        "这两条命令提高等级，但不把三式加入已学会名单。本式已注册，是法术，复杂度20。"
        "法术等级到20只打开等级门。定向学习用 /shu learn fire_liaoyuan，然后重新打开刻印台。"
        "只升剑术等级时，即使学会也不会列出。"
        + tail
    ),
    "fire_liaoli": (
        "[设计反馈 2026-10-29] 刻印台实测反馈，不是验收通过，本式仍留在制作中，不转换N/O。"
        "用户当时用的是 /shu rank sword 20 和 /shu rank magic 20，刻印台仍没有燎里。"
        "作者定位且未改生产或测试源码：当前实现把等级门槛和已学会分开，刻印台同时要求；"
        "这两条命令提高等级，但不把三式加入已学会名单。本式已注册，是剑术，复杂度20。"
        "剑术等级到20只打开等级门。定向学习用 /shu learn fire_liaoli，然后重新打开刻印台。"
        "只升法术等级时，即使学会也不会列出。"
        + tail
    ),
    "fire_fentian": (
        "[设计反馈 2026-10-29] 刻印台实测反馈，不是验收通过，本式仍留在制作中，不转换N/O。"
        "用户当时用的是 /shu rank sword 20 和 /shu rank magic 20，刻印台仍没有焚天。"
        "作者定位且未改生产或测试源码：当前实现把等级门槛和已学会分开，刻印台同时要求；"
        "这两条命令提高等级，但不把三式加入已学会名单。本式已注册，是法术，复杂度20。"
        "法术等级到20只打开等级门。定向学习用 /shu learn fire_fentian，然后重新打开刻印台。"
        "只升剑术等级时，即使学会也不会列出。"
        + tail
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
backup = batch / "备份" / f"unlock-writeback-术式制作表-{stamp}-1.xlsx"
output = batch / "unlock-writeback-candidate-preserved.xlsx"
notes_path = batch / "unlock-writeback-notes.json"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)
if sha256(backup.read_bytes()) != expected:
    fail("Backup hash is not the baseline")

intake = json.loads((batch / "unlock-intake.json").read_text(encoding="utf-8"))
intake_by_id = {item["id"]: item for item in intake["items"]}
notes_path.write_text(json.dumps({
    "backup": str(backup),
    "baselineSha256": expected,
    "additions": additions,
}, ensure_ascii=False, indent=2), encoding="utf-8")

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()

original_parts = {}
with zipfile.ZipFile(backup) as archive:
    for info in order:
        original_parts[info.filename] = archive.read(info.filename)

original = original_parts["xl/worksheets/sheet3.xml"]
sheet = original.decode("utf-8")
row_map = {}
for row_xml in rows_of(sheet):
    row_map[int(re.search(r'\br="(\d+)"', row_xml).group(1))] = row_xml
if set(row_map) != {1, 2, 3, 4, 5, 6, 7, 8}:
    fail(f"Making rows are {sorted(row_map)}")

detail_ids = sheet_ids(original_parts["xl/worksheets/sheet6.xml"].decode("utf-8"), "A")
fire_details = [item for item in detail_ids if item[0].startswith("fire_")]
if fire_details:
    fail(f"Unexpected resource rows: {fire_details}")

updated_rows = {}
reconcile = []
for row_number, name, code in targets:
    row_xml = row_map[row_number]
    if cell_text(cell_xml(row_xml, "A", row_number)[0]) != name:
        fail(f"Name mismatch on row {row_number}")
    if cell_text(cell_xml(row_xml, "B", row_number)[0]) != code:
        fail(f"Id mismatch on row {row_number}")
    feature = cell_text(cell_xml(row_xml, "N", row_number)[0])
    effect = cell_text(cell_xml(row_xml, "O", row_number)[0])
    cell, start, end = cell_xml(row_xml, "DV", row_number)
    old_text = cell_text(cell)
    source = intake_by_id[code]
    if feature != source["n"] or effect != source["o"] or old_text != source["dv"]:
        fail(f"Current N/O/DV no longer matches unlock intake for {code}")
    if marker in old_text:
        fail(f"DV{row_number} already has this feedback")
    addition = "\n" + additions[code]
    escaped = html.escape(addition, quote=False)
    if 't="str"' in cell[:80] and cell.endswith("</v></c>"):
        updated_cell = cell[: -len("</v></c>")] + escaped + "</v></c>"
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

for row_number, name, code in staying:
    row_xml = row_map[row_number]
    if cell_text(cell_xml(row_xml, "B", row_number)[0]) != code:
        fail(f"Id mismatch on row {row_number}")
    source = intake_by_id[code]
    if cell_text(cell_xml(row_xml, "DV", row_number)[0]) != source["dv"]:
        fail(f"Furnace DV changed before this writeback: {code}")
    if marker in source["dv"]:
        fail(f"Furnace DV already contains the new marker: {code}")

sheet_out = sheet
for row_number, _name, _code in targets:
    if sheet_out.count(row_map[row_number]) != 1:
        fail(f"Row {row_number} is not unique")
    sheet_out = sheet_out.replace(row_map[row_number], updated_rows[row_number], 1)
restored = sheet_out
for row_number, _name, code in targets:
    if restored.count(updated_rows[row_number]) != 1:
        fail(f"Updated row {row_number} for {code} is not unique")
    restored = restored.replace(updated_rows[row_number], row_map[row_number], 1)
if restored.encode("utf-8") != original:
    fail("Sheet differs beyond the three DV additions")
for row_number, _name, _code in staying:
    if sheet_out.count(row_map[row_number]) != 1:
        fail(f"Furnace row {row_number} changed")

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
    expected_locations = [(code, "制作中", row) for row, _name, code in targets + staying]
    claimed = [(code, title, row) for code, title, row in located if code in additions or code in {item[2] for item in staying}]
    if claimed != expected_locations:
        fail(f"Locations changed: {claimed}")
    for code, addition in additions.items():
        if addition not in dst.read("xl/worksheets/sheet3.xml").decode("utf-8"):
            fail(f"Addition missing from candidate sheet: {code}")

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
    "reconcile": reconcile,
}
(batch / "unlock-writeback-preserve-summary.json").write_text(
    json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "backup": backup.name,
    "candidateSha256": summary["candidateSha256"],
    "counts": counts,
    "changedParts": different,
    "resourceDetails": summary["resourceDetailIds"],
}, ensure_ascii=False))
