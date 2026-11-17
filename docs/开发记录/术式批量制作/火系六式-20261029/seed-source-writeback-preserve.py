# -*- coding: utf-8 -*-
"""Append the seed-source result to three completed DV cells. Does not move rows."""
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
expected = "5F8B37C4A45673CF320F4885EB5BDBF1961960013151EE082EF2C6DFF7FADA28"
record = "docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
marker = "用户已授权本式作为火种来源"
targets = {
    "magic_ignite": "点火",
    "heat_beam": "热光束",
    "fire_orb": "火球",
}
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
tail = (
    "点数固定，生产倍率1，不随伤害、炉心或聚能变化。"
    "100上限、生命周期、原伤害、范围、轨迹和视觉保持既有行为。没有新增获取或学习规则。"
    "未跑测试，未启动游戏，游戏实测待做。"
    f"详见 {record}。"
)
additions = {
    "magic_ignite": (
        "[设计反馈 2026-10-29] 用户已授权本式作为火种来源。"
        "点火附魔后的术式或武器攻击，在伤害被接受时额外挂2点火种；与热光束同次结算共3，与火球同次结算共5。视线盒子不产种。"
        "本次火种增量已实现。compileJava --offline --no-daemon于2026-10-29 13:08:23至13:08:48退出0。"
        "实现核对3/3通过。已完成仍是旧功能状态，不是新火种功能的用户验收。"
        "缺旧详细需求快照，本轮不迁出已完成。"
        + tail
    ),
    "heat_beam": (
        "[设计反馈 2026-10-29] 用户已授权本式作为火种来源。"
        "热光束每次伤害被接受的命中挂1点火种；带点火则一次挂3。"
        "本次火种增量已实现。compileJava --offline --no-daemon于2026-10-29 13:08:23至13:08:48退出0。"
        "实现核对3/3通过。已完成仍是旧功能状态，不是新火种功能的用户验收。"
        "缺旧详细需求快照，本轮不迁出已完成。"
        + tail
    ),
    "fire_orb": (
        "[设计反馈 2026-10-29] 用户已授权本式作为火种来源。"
        "火球每个实际命中目标挂3点火种；带点火则一次挂5。仍是直线穿透，不改成爆炸。"
        "本次火种增量已实现。compileJava --offline --no-daemon于2026-10-29 13:08:23至13:08:48退出0。"
        "实现核对3/3通过。已完成仍是旧功能状态，不是新火种功能的用户验收。"
        "缺旧详细需求快照，本轮不迁出已完成。"
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
    return None


def rows_of(sheet_xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet_xml)


def sheet_ids(sheet_xml, id_column):
    found = []
    for row_xml in rows_of(sheet_xml):
        row_number = int(re.search(r'\br="(\d+)"', row_xml).group(1))
        if row_number < 3:
            continue
        if f'r="{id_column}{row_number}"' not in row_xml:
            continue
        code = cell_text(cell_xml(row_xml, id_column, row_number)[0])
        if code and code.strip():
            found.append((code.strip(), row_number))
    return found


live_bytes = live.read_bytes()
live_stat = live.stat()
if sha256(live_bytes) != expected:
    fail(f"Live workbook hash changed before writeback: {sha256(live_bytes)}")

stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"seed-source-writeback-术式制作表-{stamp}-1.xlsx"
output = batch / "seed-source-writeback-candidate-preserved.xlsx"
notes_path = batch / "seed-source-writeback-notes.json"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)
if sha256(backup.read_bytes()) != expected:
    fail("Backup hash is not the baseline")

intake = json.loads((batch / "seed-source-input.json").read_text(encoding="utf-8"))
intake_by_id = {item["id"]: item for item in intake["items"]}

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original_parts = {info.filename: archive.read(info.filename) for info in order}

original = original_parts["xl/worksheets/sheet1.xml"]
sheet = original.decode("utf-8")
located = {code: row for code, row in sheet_ids(sheet, "B") if code in targets}
if set(located) != set(targets):
    fail(f"Completed locations {located}")
row_map = {}
for row_xml in rows_of(sheet):
    row_map[int(re.search(r'\br="(\d+)"', row_xml).group(1))] = row_xml

updated_rows = {}
found_rows = {}
for code, name in targets.items():
    row_number = located[code]
    row_xml = row_map[row_number]
    if cell_text(cell_xml(row_xml, "A", row_number)[0]) != name:
        fail(f"Name mismatch for {code}")
    feature = cell_text(cell_xml(row_xml, "N", row_number)[0])
    effect = cell_text(cell_xml(row_xml, "O", row_number)[0])
    source = intake_by_id[code]
    if feature != source["n"] or effect != source["o"] or source["dv"] is not None:
        fail(f"Current N/O/DV no longer matches intake for {code}")
    cell, start, end = cell_xml(row_xml, "DV", row_number)
    if not cell.endswith("/>") or cell_text(cell) is not None:
        fail(f"DV{row_number} is no longer an empty cell: {cell}")
    if marker in (cell_text(cell) or ""):
        fail(f"DV{row_number} already has this feedback")
    style = re.search(r'\bs="(\d+)"', cell)
    if not style:
        fail(f"DV{row_number} has no style")
    escaped = html.escape(additions[code], quote=False)
    updated_cell = (
        f'<c r="DV{row_number}" s="{style.group(1)}" t="inlineStr">'
        f'<is><t xml:space="preserve">{escaped}</t></is></c>'
    )
    updated_row = row_xml[:start] + updated_cell + row_xml[end:]
    if cell_text(cell_xml(updated_row, "DV", row_number)[0]) != additions[code]:
        fail(f"DV{row_number} text was not written cleanly")
    if cell_text(cell_xml(updated_row, "N", row_number)[0]) != feature:
        fail(f"N changed for {code}")
    if cell_text(cell_xml(updated_row, "O", row_number)[0]) != effect:
        fail(f"O changed for {code}")
    updated_rows[row_number] = updated_row
    found_rows[code] = row_number

sheet_out = sheet
for row_number, updated in updated_rows.items():
    if sheet_out.count(row_map[row_number]) != 1:
        fail(f"Row {row_number} is not unique")
    sheet_out = sheet_out.replace(row_map[row_number], updated, 1)
restored = sheet_out
for row_number, updated in updated_rows.items():
    if restored.count(updated) != 1:
        fail(f"Updated row {row_number} is not unique")
    restored = restored.replace(updated, row_map[row_number], 1)
if restored.encode("utf-8") != original:
    fail("Sheet differs beyond the three DV additions")

changed = {"xl/worksheets/sheet1.xml": sheet_out.encode("utf-8")}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    if src.namelist() != dst.namelist():
        fail("Zip part list changed")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet1.xml"]:
        fail(f"Unexpected part changes: {different}")
    counts = {}
    all_ids = []
    for title, name in status_parts.items():
        ids = sheet_ids(dst.read(name).decode("utf-8"), "B")
        counts[title] = len(ids)
        all_ids.extend(code for code, _row in ids)
    if counts != {"已完成": 40, "待制作": 0, "制作中": 6, "待验收": 0}:
        fail(f"Counts {counts}")
    if len(all_ids) != len(set(all_ids)) or len(all_ids) != 46:
        fail("Status ids are not unique")
    for code, addition in additions.items():
        if addition not in dst.read("xl/worksheets/sheet1.xml").decode("utf-8"):
            fail(f"Addition missing for {code}")

notes_path.write_text(json.dumps({
    "backup": str(backup),
    "baselineSha256": expected,
    "rows": found_rows,
    "additions": additions,
}, ensure_ascii=False, indent=2), encoding="utf-8")
summary = {
    "submitted": False,
    "baselineSha256": expected,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "rows": found_rows,
    "changedParts": different,
    "counts": counts,
}
(batch / "seed-source-writeback-preserve-summary.json").write_text(
    json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "backup": backup.name,
    "candidateSha256": summary["candidateSha256"],
    "rows": found_rows,
    "counts": counts,
    "changedParts": different,
}, ensure_ascii=False))
