# -*- coding: utf-8 -*-
"""Append the approved square contact decision to the in-progress 灼地 note."""
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
baseline = "2F354F20D8F3A001F95908DAB4F83856C2E76E5056ACA411DD1ACFDDFDC2E6E4"
feedback = (
    "[设计反馈 2026-10-30] 用户原话：“灼地也按这个方形接触体实现”。"
    "已批准接触体为随地形贴合的方形低矮检测盒，并持续检测实体碰撞。"
    "表内半径2格对应中心向四边各延伸2格。产种数值和周期保持原要求。"
    "第一轮圆盘脚点方案被本决定替代，不再是当前几何。"
    "详见 docs/开发记录/术式批量制作/灼地-20261030/记录.md。"
)


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
    if 't="inlineStr"' in piece:
        parts = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", piece)
        return html.unescape("".join(parts)) if parts else None
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?v>", piece)
    return html.unescape(value.group(1)) if value else None


def replace_text(cell, new_text):
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
        fail("DV cell has no text")
    updated, count = re.subn(pattern, lambda match: opener(match.group(1)) + escaped + match.group(2), cell, count=1)
    if count != 1 or cell_text(updated) != new_text:
        fail("DV text was not written cleanly")
    return updated


live_bytes = live.read_bytes()
if sha256(live_bytes) != baseline:
    fail("Live workbook hash is not the shape baseline")
live_stat = live.stat()
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup_dir = batch / "备份"
candidate_dir = batch / "候选"
backup_dir.mkdir(exist_ok=True)
candidate_dir.mkdir(exist_ok=True)
sequence = 1
while (backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx").exists():
    sequence += 1
backup = backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx"
candidate = candidate_dir / "术式制作表-shape-candidate.xlsx"
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}
sheet3 = parts["xl/worksheets/sheet3.xml"].decode("utf-8")
rows = rows_of(sheet3)
by_number = {row_number(row): row for row in rows}
if [row_number(row) for row in rows] != [1, 2, 3, 4, 5, 6]:
    fail("制作中 row shape changed")
source = by_number[6]
if cell_text(cell_xml(source, "A", 6)) != "灼地" or cell_text(cell_xml(source, "B", 6)) != "fire_scorch_field":
    fail("制作中第6行不是灼地")
if cell_text(cell_xml(source, "G", 6)) != "20" or cell_text(cell_xml(source, "H", 6)) != "0":
    fail("Approved level cap or complexity changed")
if cell_text(cell_xml(source, "AS", 6)) != "2" or cell_text(cell_xml(source, "E", 6)) != "1":
    fail("Radius or damage changed before this edit")
original_n = cell_text(cell_xml(source, "N", 6))
original_o = cell_text(cell_xml(source, "O", 6))
original_dv = cell_text(cell_xml(source, "DV", 6))
if "方形接触体" in original_dv or "方形低矮检测盒" in original_dv:
    fail("Square decision is already in DV")
if not original_dv.endswith("docs/开发记录/术式批量制作/灼地-20261030/记录.md。"):
    fail("Existing DV does not end at the parameter feedback")
dv_cell = cell_xml(source, "DV", 6)
new_dv = original_dv + "\n" + feedback
updated = source[:dv_cell[1]] + replace_text(dv_cell[0], new_dv) + source[dv_cell[2]:]
if cell_text(cell_xml(updated, "N", 6)) != original_n or cell_text(cell_xml(updated, "O", 6)) != original_o:
    fail("N/O changed")
if cell_text(cell_xml(updated, "DV", 6)) != new_dv or not new_dv.startswith(original_dv):
    fail("DV did not keep the previous text")
if cell_text(cell_xml(updated, "G", 6)) != "20" or cell_text(cell_xml(updated, "H", 6)) != "0":
    fail("Parameters changed")
new_rows = [updated if row_number(row) == 6 else row for row in rows]
match = re.search(r"(<(?:[\w]+:)?sheetData>)([\s\S]*)(</(?:[\w]+:)?sheetData>)", sheet3)
sheet3_out = sheet3[:match.start(2)] + "".join(new_rows) + sheet3[match.end(2):]
changed = {"xl/worksheets/sheet3.xml": sheet3_out.encode("utf-8")}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml"]:
        fail(f"Unexpected parts changed: {different}")
    if "方形低矮检测盒" in dst.read("xl/worksheets/sheet7.xml").decode("utf-8"):
        fail("Archive received the new decision")
    making = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")
    row6 = next(row for row in rows_of(making) if row_number(row) == 6)
    if cell_text(cell_xml(row6, "DV", 6)) != new_dv:
        fail("Reopen lost the feedback")
    for number, code in ((3, "fire_heat_up"), (4, "fire_burn"), (5, "fire_cool")):
        row = next(item for item in rows_of(making) if row_number(item) == number)
        if row != by_number[number]:
            fail(f"{code} xml changed")

summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "sourceLastWriteTime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "backup": str(backup),
    "backupName": backup.name,
    "candidate": str(candidate),
    "candidateSha256": sha256(candidate.read_bytes()),
    "feedback": feedback,
    "dvBefore": original_dv,
    "dvAfter": new_dv,
}
(batch / "shape-snapshot.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"backup": backup.name, "candidateSha256": summary["candidateSha256"]}, ensure_ascii=False))
