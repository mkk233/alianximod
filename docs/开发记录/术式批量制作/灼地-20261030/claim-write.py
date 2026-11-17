# -*- coding: utf-8 -*-
"""Archive fire_scorch_field, then move that pending row into 制作中."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "术式批量制作" / "灼地-20261030"
live = root / "docs" / "术式制作表.xlsx"
baseline = "83D13762D627FF953A184F5B7F6247E88A972ED9FA139099D6FFA5A12DFEFB9E"
marker = "制作中 | 灼地-20261030 | docs/开发记录/术式批量制作/灼地-20261030/记录.md"
code = "fire_scorch_field"


def fail(message):
    raise SystemExit(message)


def sha256(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_xml(row, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row.find(">", start)
    if row[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</(?:[\w]+:)?c>", row[opener_end:])
        if not close:
            fail(f"Cell {column}{number} is not closed")
        end = opener_end + close.end()
    return row[start:end], start, end


def cell_text(cell):
    if cell is None:
        return None
    piece = cell[0] if isinstance(cell, tuple) else cell
    inline = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", piece)
    if inline and 't="inlineStr"' in piece:
        return html.unescape("".join(inline))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?v>", piece)
    if value:
        return html.unescape(value.group(1))
    if inline:
        return html.unescape("".join(inline))
    return None


def replace_text(cell, column, number, new_text):
    escaped = html.escape(new_text, quote=False)
    needs_space = "\n" in new_text or new_text[:1].isspace() or new_text[-1:].isspace()

    def opener(tag):
        if needs_space and "xml:space" not in tag:
            return tag[:-1] + ' xml:space="preserve">'
        return tag

    if 't="inlineStr"' in cell and re.search(r"<(?:[\w]+:)?t\b", cell):
        pattern = r"(<(?:[\w]+:)?t\b[^>]*>)[\s\S]*(</(?:[\w]+:)?t>)"
    elif re.search(r"<(?:[\w]+:)?v\b", cell):
        pattern = r"(<(?:[\w]+:)?v\b[^>]*>)[\s\S]*(</(?:[\w]+:)?v>)"
    else:
        fail(f"Cell {column}{number} has no text")
    updated, count = re.subn(pattern, lambda match: opener(match.group(1)) + escaped + match.group(2), cell, count=1)
    if count != 1 or cell_text(updated) != new_text:
        fail(f"Cell {column}{number} was not written cleanly")
    return updated


def set_cell(row, column, number, new_text):
    found = cell_xml(row, column, number)
    if not found:
        fail(f"Missing {column}{number}")
    cell, start, end = found
    return row[:start] + replace_text(cell, column, number, new_text) + row[end:]


def renumber(row, old, new):
    updated = re.sub(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row, count=1)
    updated = re.sub(rf'r="([A-Z]+){old}"', rf'r="\g<1>{new}"', updated)
    if row_number(updated) != new:
        fail(f"Row {old} did not become {new}")
    addresses = [int(item) for item in re.findall(r'\br="[A-Z]+(\d+)"', updated)]
    if not addresses or any(item != new for item in addresses):
        fail(f"Row {old} cell addresses did not all become {new}")
    return updated


def replace_sheet_rows(xml, new_rows):
    match = re.search(r"(<(?:[\w]+:)?sheetData>)([\s\S]*)(</(?:[\w]+:)?sheetData>)", xml)
    if not match:
        fail("sheetData missing")
    return xml[:match.start(2)] + "".join(new_rows) + xml[match.end(2):]


def sheet_ids(xml):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        found_cell = cell_xml(row, "B", number)
        text = cell_text(found_cell)
        if text and str(text).strip():
            found.append((str(text).strip(), number, row))
    return found


def detail_ids(xml):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        found_cell = cell_xml(row, "A", number)
        text = cell_text(found_cell)
        if text and str(text).strip():
            found.append((str(text).strip(), number, row))
    return found


def inline_cell(column, number, text):
    escaped = html.escape(text, quote=False)
    space = ' xml:space="preserve"' if ("\n" in text or text[:1].isspace() or text[-1:].isspace()) else ""
    return f'<c r="{column}{number}" s="16" t="inlineStr"><is><t{space}>{escaped}</t></is></c>'


def replace_once(xml, old, new, label):
    count = xml.count(old)
    if count != 1:
        fail(f"{label} count is {count}")
    return xml.replace(old, new, 1)


live_bytes = live.read_bytes()
live_stat = live.stat()
if sha256(live_bytes) != baseline:
    fail("Live workbook hash is not the read baseline")
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup_dir = batch / "备份"
candidate_dir = batch / "候选"
backup_dir.mkdir(exist_ok=True)
candidate_dir.mkdir(exist_ok=True)
sequence = 1
while (backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx").exists():
    sequence += 1
backup = backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx"
candidate = candidate_dir / "术式制作表-candidate.xlsx"
backup.write_bytes(live_bytes)
captured_at = datetime.now().astimezone().isoformat(timespec="milliseconds")
source_mtime = datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds")

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}

sheet2 = parts["xl/worksheets/sheet2.xml"].decode("utf-8")
sheet3 = parts["xl/worksheets/sheet3.xml"].decode("utf-8")
sheet7 = parts["xl/worksheets/sheet7.xml"].decode("utf-8")
sheet6 = parts["xl/worksheets/sheet6.xml"].decode("utf-8")
rows2 = rows_of(sheet2)
rows3 = rows_of(sheet3)
rows7 = rows_of(sheet7)
if [row_number(row) for row in rows2] != list(range(1, 17)):
    fail("待制作 rows are not 1..16")
if [row_number(row) for row in rows3] != [1, 2, 3, 4, 5]:
    fail("制作中 rows changed")
if [row_number(row) for row in rows7] != [1, 2, 3, 4, 5, 6, 7, 8, 10]:
    fail("档案 rows changed")
pending_ids = sheet_ids(sheet2)
making_ids = sheet_ids(sheet3)
done_ids = sheet_ids(parts["xl/worksheets/sheet1.xml"].decode("utf-8"))
accept_ids = sheet_ids(parts["xl/worksheets/sheet4.xml"].decode("utf-8"))
archive_ids = sheet_ids(sheet7)
if [item[0] for item in pending_ids][1] != code or pending_ids[1][1] != 4:
    fail("灼地 is not 待制作 row 4")
flat = [item[0] for item in done_ids + pending_ids + making_ids + accept_ids]
if len(flat) != 60 or len(set(flat)) != 60 or flat.count(code) != 1:
    fail("Status ids are not a unique set of 60")
if any(item[0] == code for item in archive_ids):
    fail("Archive already has this id")
if [item[0] for item in making_ids] != ["fire_heat_up", "fire_burn", "fire_cool"]:
    fail("制作中 neighbors changed")
source = rows2[3]
if cell_text(cell_xml(source, "A", 4)) != "灼地" or cell_text(cell_xml(source, "B", 4)) != code:
    fail("Source identity mismatch")
original_n = cell_text(cell_xml(source, "N", 4))
original_o = cell_text(cell_xml(source, "O", 4))
original_dv = cell_text(cell_xml(source, "DV", 4))
if not original_n or not original_o or not original_dv:
    fail("N/O/DV missing")
if marker in original_dv or "[设计反馈" in original_dv:
    fail("DV already has this batch marker")
if cell_xml(source, "R", 4):
    fail("弹道 was not empty")
details = [item for item in detail_ids(sheet6) if item[0] == code]
if len(details) != 1 or details[0][1] != 7:
    fail("Resource detail row is not the single row 7")
detail_row = details[0][2]
if cell_text(cell_xml(detail_row, "B", 7)) != "2":
    fail("Detail sequence is not 2")
detail_columns = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T"]
detail_values = {column: cell_text(cell_xml(detail_row, column, 7)) for column in detail_columns}
expected_detail = {
    "A": code, "B": "2", "C": "是", "D": "火种", "F": "增加", "G": "领域内敌对活物",
    "I": "15", "K": "不适用", "M": "20", "O": "每次触发", "R": "N/DV",
    "S": "灼地第二条及以后的周期产种。不重复主行首次接触。",
}
for column, expected in expected_detail.items():
    if detail_values[column] != expected:
        fail(f"Detail {column} changed")
for column in ("E", "H", "J", "L", "N", "T"):
    if detail_values[column] not in (None, ""):
        fail(f"Detail {column} was empty and is now filled")

source_hash_lower = baseline.lower()
detail_sentence = (
    "资源状态明细第7行，配置序号2。"
    "启用是，资源火种，操作增加，作用对象领域内敌对活物，数值15，寿命策略不适用，周期tick 20，计量单位每次触发。"
    "触发时机、状态、状态amplifier、寿命tick、首次延迟tick和附加明细键为空。"
)
snapshot_note = (
    "2026-10-30领取前首次快照。来源是当时的现行工作簿 docs/术式制作表.xlsx，页「待制作」第4行，不是用制作中或更早备份反推。"
    f"源文件 LastWriteTime {source_mtime}，SHA256 {baseline}。"
    f"本批备份 docs/开发记录/术式批量制作/灼地-20261030/备份/{backup.name} 与该源哈希相同。"
    "本行 A:EN 是领取前原文。N、O、DV 未转换，也没有写入本批流程标记。"
    "同编号资源状态明细只有这一条，配置序号仍是2，不复制制作状态，活页明细不搬动。"
    f"明细生效条件：{detail_values['P']}\n"
    f"明细结算约束：{detail_values['Q']}"
)
provenance = {
    "EP": "docs/术式制作表.xlsx",
    "EQ": "待制作",
    "ER": "4",
    "ES": source_mtime,
    "ET": captured_at,
    "EU": source_hash_lower,
    "EV": snapshot_note,
    "EX": detail_sentence,
    "EY": "docs/术式制作表.xlsx",
    "EZ": "7",
    "FA": source_hash_lower,
}

archive_row = renumber(source, 4, 9)
archive_row = archive_row[:-6] + "".join(inline_cell(column, 9, text) for column, text in provenance.items()) + "</row>"
if cell_text(cell_xml(archive_row, "DV", 9)) != original_dv:
    fail("Archive DV changed")
if cell_text(cell_xml(archive_row, "N", 9)) != original_n or cell_text(cell_xml(archive_row, "O", 9)) != original_o:
    fail("Archive N/O changed")

making_row = renumber(source, 4, 6)
making_dv = original_dv + "\n" + marker
making_row = set_cell(making_row, "DV", 6, making_dv)
if cell_text(cell_xml(making_row, "N", 6)) != original_n or cell_text(cell_xml(making_row, "O", 6)) != original_o:
    fail("制作中 N/O changed")
if not cell_text(cell_xml(making_row, "DV", 6)).startswith(original_dv) or marker not in cell_text(cell_xml(making_row, "DV", 6)):
    fail("制作中 DV marker missing")
if "[设计反馈" in cell_text(cell_xml(making_row, "DV", 6)):
    fail("Design feedback was added")

shifted = [rows2[0], rows2[1], rows2[2]]
for old_number in range(5, 17):
    shifted.append(renumber(rows2[old_number - 1], old_number, old_number - 1))
if [row_number(row) for row in shifted] != list(range(1, 16)):
    fail("Shifted 待制作 rows are not 1..15")
sheet2_out = replace_once(replace_sheet_rows(sheet2, shifted), 'ref="A1:EN16"', 'ref="A1:EN15"', "待制作 dimension")
row10 = rows7[-1]
if row_number(row10) != 10 or "使用说明" not in row10:
    fail("Archive note row moved")
sheet7_out = sheet7.replace(row10, archive_row + row10, 1)
if sheet7_out.count(archive_row) != 1:
    fail("Archive row was not inserted once")
sheet3_out = replace_once(
    replace_sheet_rows(sheet3, rows3 + [making_row]),
    'ref="A1:EN5"',
    'ref="A1:EN6"',
    "制作中 dimension",
)
changed = {
    "xl/worksheets/sheet2.xml": sheet2_out.encode("utf-8"),
    "xl/worksheets/sheet3.xml": sheet3_out.encode("utf-8"),
    "xl/worksheets/sheet7.xml": sheet7_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml", "xl/worksheets/sheet7.xml"]:
        fail(f"Unexpected parts changed: {different}")
    pending = dst.read("xl/worksheets/sheet2.xml").decode("utf-8")
    making = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")
    archived = dst.read("xl/worksheets/sheet7.xml").decode("utf-8")
    detail = dst.read("xl/worksheets/sheet6.xml").decode("utf-8")
    if detail != sheet6:
        fail("Resource detail changed")
    if dst.read("xl/worksheets/sheet5.xml") != parts["xl/worksheets/sheet5.xml"]:
        fail("数据表 changed")
    counts = {}
    ids = []
    for title, name in (
        ("已完成", "xl/worksheets/sheet1.xml"),
        ("待制作", "xl/worksheets/sheet2.xml"),
        ("制作中", "xl/worksheets/sheet3.xml"),
        ("待验收", "xl/worksheets/sheet4.xml"),
    ):
        page = [item[0] for item in sheet_ids(dst.read(name).decode("utf-8"))]
        counts[title] = len(page)
        ids.extend(page)
    if counts != {"已完成": 40, "待制作": 13, "制作中": 4, "待验收": 3} or len(ids) != 60 or len(set(ids)) != 60:
        fail(f"Counts failed {counts}")
    if code in [item[0] for item in sheet_ids(pending)] or pending.count(code) != 0:
        fail("待制作 still contains the id")
    making_now = sheet_ids(making)
    if [item[0] for item in making_now] != ["fire_heat_up", "fire_burn", "fire_cool", code]:
        fail(f"制作中 order changed: {[item[0] for item in making_now]}")
    moved = making_now[3][2]
    if cell_text(cell_xml(moved, "A", 6)) != "灼地":
        fail("制作中 name changed")
    if cell_text(cell_xml(moved, "E", 6)) != "1" or cell_text(cell_xml(moved, "F", 6)) != "30":
        fail("Damage or cooldown changed")
    if cell_text(cell_xml(moved, "EC", 6)) != "20" or cell_text(cell_xml(moved, "AS", 6)) != "2" or cell_text(cell_xml(moved, "BW", 6)) != "100":
        fail("Radius, life, or first seed value changed")
    if cell_xml(moved, "R", 6):
        fail("弹道 was filled")
    archive_now = sheet_ids(archived)
    if [item[0] for item in archive_now] != [item[0] for item in archive_ids] + [code]:
        fail("Archive ids were overwritten or reordered")
    stored = archive_now[-1][2]
    if row_number(stored) != 9 or cell_text(cell_xml(stored, "DV", 9)) != original_dv:
        fail("Archive row is not the original snapshot")
    if "使用说明" not in archived or row_number(rows_of(archived)[-1]) != 10:
        fail("Archive note was not kept on row 10")
    pending_now = sheet_ids(pending)
    expected_pending = [item[0] for item in pending_ids if item[0] != code]
    if [item[0] for item in pending_now] != expected_pending:
        fail("Other 待制作 ids changed")
    if [item[1] for item in pending_now] != list(range(3, 16)):
        fail("待制作 rows were not closed up")
    old_by_code = {item[0]: item[2] for item in pending_ids if item[0] != code}
    for item in pending_now:
        old_row = old_by_code[item[0]]
        old_number = row_number(old_row)
        if item[2] != renumber(old_row, old_number, item[1]):
            fail(f"Pending row {item[0]} was not a pure shift")
    for kept, number in (("fire_heat_up", 3), ("fire_burn", 4), ("fire_cool", 5)):
        old = next(item[2] for item in making_ids if item[0] == kept)
        new = next(item[2] for item in making_now if item[0] == kept)
        if old != new:
            fail(f"{kept} changed")
    if cell_text(cell_xml(next(item[2] for item in pending_now if item[0] == "fire_spark"), "F", 15)) != "5":
        fail("火苗 cooldown changed")
    if cell_text(cell_xml(next(item[2] for item in pending_now if item[0] == "fire_seed_sow"), "F", 10)) != "5":
        fail("种火 cooldown changed")
    detail_now = detail_ids(detail)
    if [(item[0], item[1]) for item in detail_now] != [(item[0], item[1]) for item in detail_ids(sheet6)]:
        fail("Detail association changed")

snapshot = {
    "submitted": False,
    "baselineSha256": baseline,
    "sourceLastWriteTime": source_mtime,
    "sourceBytes": live_stat.st_size,
    "capturedAt": captured_at,
    "backup": str(backup),
    "backupName": backup.name,
    "candidate": str(candidate),
    "candidateSha256": sha256(candidate.read_bytes()),
    "sourceSheet": "待制作",
    "sourceRow": 4,
    "targetSheet": "制作中",
    "targetRow": 6,
    "archiveSheet": "待制作原始数据",
    "archiveRow": 9,
    "id": code,
    "name": "灼地",
    "n": original_n,
    "o": original_o,
    "dv": original_dv,
    "dvAfter": making_dv,
    "marker": marker,
    "detailRow": 7,
    "detail": detail_values,
    "provenance": provenance,
    "counts": counts,
    "pendingIds": [item[0] for item in pending_now],
    "makingIds": [item[0] for item in making_now],
    "archiveIds": [item[0] for item in archive_now],
}
(batch / "claim-snapshot.json").write_text(json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "backup": backup.name,
    "candidateSha256": snapshot["candidateSha256"],
    "counts": counts,
    "archiveRow": 9,
    "targetRow": 6,
}, ensure_ascii=False))
