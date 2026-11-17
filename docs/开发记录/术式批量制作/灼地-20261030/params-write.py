# -*- coding: utf-8 -*-
"""Write the confirmed level cap and complexity onto the in-progress 灼地 row."""
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
baseline = "9E79A331524B770F79CC74A311ADDCACB5C63C0E0B57FB85A0D802E8BFECD2F9"
feedback = (
    "[设计反馈 2026-10-30] 用户原话：“暂用复杂度 0、等级上限 20，之后再调”。"
    "制作中等级上限写 20，复杂度写 0。这是用户明确的暂定数值，之后再调；"
    "费用、魔力、装配消耗等其余未定项仍留空，不用 0 填。"
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
    fail("Live workbook hash is not the parameter baseline")
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
candidate = candidate_dir / "术式制作表-params-candidate.xlsx"
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}
sheet3 = parts["xl/worksheets/sheet3.xml"].decode("utf-8")
rows = rows_of(sheet3)
by_number = {row_number(row): row for row in rows}
if [row_number(row) for row in rows] != [1, 2, 3, 4, 5, 6]:
    fail("制作中 row shape changed")
header = by_number[2]
if cell_text(cell_xml(header, "G", 2)) != "等级上限" or cell_text(cell_xml(header, "H", 2)) != "复杂度":
    fail("Headers are not 等级上限 / 复杂度")
source = by_number[6]
if cell_text(cell_xml(source, "A", 6)) != "灼地" or cell_text(cell_xml(source, "B", 6)) != "fire_scorch_field":
    fail("制作中第6行不是灼地")
if cell_xml(source, "G", 6) or cell_xml(source, "H", 6) or cell_xml(source, "K", 6) or cell_xml(source, "L", 6):
    fail("A blank parameter cell is already filled")
original_n = cell_text(cell_xml(source, "N", 6))
original_o = cell_text(cell_xml(source, "O", 6))
original_dv = cell_text(cell_xml(source, "DV", 6))
if "[设计反馈" in original_dv or "暂用复杂度" in original_dv:
    fail("This decision is already in DV")
if not original_dv.endswith("docs/开发记录/术式批量制作/灼地-20261030/记录.md"):
    fail("Process marker is not at the end of DV")
found = cell_xml(source, "F", 6)
if cell_text(found) != "30":
    fail("Cooldown is not 30")
updated = source[:found[2]] + '<c r="G6" s="15" t="n"><v>20</v></c><c r="H6" s="15" t="n"><v>0</v></c>' + source[found[2]:]
if not updated[found[2]:].startswith('<c r="G6"') or '<c r="J6"' not in updated:
    fail("G/H were not inserted before J")
dv_cell = cell_xml(updated, "DV", 6)
new_dv = original_dv + "\n" + feedback
updated = updated[:dv_cell[1]] + replace_text(dv_cell[0], new_dv) + updated[dv_cell[2]:]
if cell_text(cell_xml(updated, "G", 6)) != "20" or cell_text(cell_xml(updated, "H", 6)) != "0":
    fail("Numbers were not stored")
if cell_text(cell_xml(updated, "N", 6)) != original_n or cell_text(cell_xml(updated, "O", 6)) != original_o:
    fail("N/O changed")
if cell_text(cell_xml(updated, "DV", 6)) != new_dv:
    fail("DV is not the original plus one feedback")
if cell_xml(updated, "K", 6) or cell_xml(updated, "L", 6):
    fail("Unset cost cells were filled")
new_rows = []
for row in rows:
    new_rows.append(updated if row_number(row) == 6 else row)
match = re.search(r"(<(?:[\w]+:)?sheetData>)([\s\S]*)(</(?:[\w]+:)?sheetData>)", sheet3)
sheet3_out = sheet3[:match.start(2)] + "".join(new_rows) + sheet3[match.end(2):]
if "#REF!" in sheet3_out or "#VALUE!" in sheet3_out:
    fail("Formula error text appeared")
changed = {"xl/worksheets/sheet3.xml": sheet3_out.encode("utf-8")}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml"]:
        fail(f"Unexpected parts changed: {different}")
    if dst.read("xl/worksheets/sheet7.xml") != src.read("xl/worksheets/sheet7.xml"):
        fail("Archive changed")
    if "暂用复杂度" in dst.read("xl/worksheets/sheet7.xml").decode("utf-8"):
        fail("Feedback leaked into the archive")
    making = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")
    row6 = next(row for row in rows_of(making) if row_number(row) == 6)
    if cell_text(cell_xml(row6, "G", 6)) != "20" or cell_text(cell_xml(row6, "H", 6)) != "0":
        fail("Reopen lost the numbers")
    if cell_text(cell_xml(row6, "E", 6)) != "1" or cell_text(cell_xml(row6, "F", 6)) != "30":
        fail("Damage or cooldown changed")
    for number, code in ((3, "fire_heat_up"), (4, "fire_burn"), (5, "fire_cool")):
        row = next(item for item in rows_of(making) if row_number(item) == number)
        if cell_text(cell_xml(row, "B", number)) != code:
            fail(f"{code} moved")
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
    "levelCap": 20,
    "complexity": 0,
    "feedback": feedback,
    "dvBefore": original_dv,
    "dvAfter": new_dv,
}
(batch / "params-snapshot.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"backup": backup.name, "candidateSha256": summary["candidateSha256"]}, ensure_ascii=False))
