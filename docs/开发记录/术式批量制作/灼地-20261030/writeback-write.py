# -*- coding: utf-8 -*-
"""Move 灼地 from 制作中 to 待验收 and convert N/O for display."""
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
baseline = "4A61BC491A040E2AFB6879F61652BE597521FB10E6A71D524859F895B123BB09"
code = "fire_scorch_field"
display_n = "在脚前合法地表铺开边长4格的贴地火场，持续100刻，接触的敌人按首次和停留周期获得火种。"
display_o = (
    "xueximod:textures/effect/liaoyuan_v2/ground_flame.png；"
    "xueximod:textures/effect/liaoyuan_v2/ground_embers.png；"
    "minecraft:flame（产种汇聚火星）。"
)
marker = "待验收 | 灼地-20261030 | docs/开发记录/术式批量制作/灼地-20261030/记录.md"
fact = "2026-10-30 compileJava 退出码 0，未打包，未游戏。本阶段额度已用完，返修结果未追加复审。"
dv_ending = (
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


def sheet_ids(xml):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        text = cell_text(cell_xml(row, "B", number))
        if text and str(text).strip():
            found.append(str(text).strip())
    return found


live_bytes = live.read_bytes()
if sha256(live_bytes) != baseline:
    fail("Live workbook hash is not the read baseline; writeback paused")
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
candidate = candidate_dir / "术式制作表-accept-candidate.xlsx"
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}

sheet3 = parts["xl/worksheets/sheet3.xml"].decode("utf-8")
sheet4 = parts["xl/worksheets/sheet4.xml"].decode("utf-8")
rows3 = rows_of(sheet3)
rows4 = rows_of(sheet4)
if [row_number(row) for row in rows3] != [1, 2, 3, 4, 5, 6]:
    fail("制作中 row shape changed")
if [row_number(row) for row in rows4] != [1, 2, 3, 4, 5]:
    fail("待验收 row shape changed")
source = rows3[5]
if cell_text(cell_xml(source, "A", 6)) != "灼地" or cell_text(cell_xml(source, "B", 6)) != code:
    fail("制作中第6行不是灼地")
if cell_text(cell_xml(source, "G", 6)) != "20" or cell_text(cell_xml(source, "H", 6)) != "0":
    fail("Approved level cap or complexity changed")
if cell_text(cell_xml(source, "E", 6)) != "1" or cell_text(cell_xml(source, "F", 6)) != "30":
    fail("Damage or cooldown changed")
if cell_text(cell_xml(source, "AS", 6)) != "2" or cell_text(cell_xml(source, "BW", 6)) != "100":
    fail("Radius or life changed")
if cell_xml(source, "K", 6) or cell_xml(source, "L", 6):
    fail("Assembly cost or registered mana is no longer empty")
original_n = cell_text(cell_xml(source, "N", 6))
original_o = cell_text(cell_xml(source, "O", 6))
original_dv = cell_text(cell_xml(source, "DV", 6))
if not original_n or not original_n.startswith("灼地是地面产种术式"):
    fail("N is not the saved requirement text")
if "费用、魔力、复杂度、装配消耗、等级上限和等级成长仍未定" not in original_n:
    fail("N no longer contains the saved unsettled-cost sentence")
if original_o != "贴地火舌。每次产种时，火星从地面向目标身上汇聚，表现积火。不采用持续伤害命中的闪烁或受击反馈来假装周期产种在扣血。装上幽冥后，火场表现为蓝色魂火。":
    fail("O is not the saved visual text")
if "暂用复杂度 0、等级上限 20，之后再调" not in original_dv or "灼地也按这个方形接触体实现" not in original_dv:
    fail("Approved user quotes are missing from DV")
if not original_dv.endswith(dv_ending):
    fail("DV has text after the saved square-contact feedback")
if "待验收 | 灼地-20261030" in original_dv:
    fail("Acceptance marker is already present")
done_ids = sheet_ids(parts["xl/worksheets/sheet1.xml"].decode("utf-8"))
pending_ids = sheet_ids(parts["xl/worksheets/sheet2.xml"].decode("utf-8"))
making_ids = sheet_ids(sheet3)
accept_ids = sheet_ids(sheet4)
if making_ids != ["fire_heat_up", "fire_burn", "fire_cool", code]:
    fail("制作中 order changed")
if accept_ids != ["fire_fentian", "fire_liaoyuan", "fire_liaoli"]:
    fail("待验收 order changed")
flat = done_ids + pending_ids + making_ids + accept_ids
if len(flat) != 60 or len(set(flat)) != 60 or flat.count(code) != 1:
    fail("Status ids are not a unique set of 60")
sheet6 = parts["xl/worksheets/sheet6.xml"].decode("utf-8")
detail_hits = []
for row in rows_of(sheet6):
    number = row_number(row)
    if number >= 3 and cell_text(cell_xml(row, "A", number)) == code:
        detail_hits.append((number, cell_text(cell_xml(row, "B", number))))
if detail_hits != [(7, "2")]:
    fail("Resource detail is not the single row 7 sequence 2")

moved = set_cell(source, "N", 6, display_n)
moved = set_cell(moved, "O", 6, display_o)
new_dv = original_dv + "\n" + marker + "\n" + fact
moved = set_cell(moved, "DV", 6, new_dv)
if cell_text(cell_xml(moved, "N", 6)) != display_n or cell_text(cell_xml(moved, "O", 6)) != display_o:
    fail("Display N/O was not written")
if not cell_text(cell_xml(moved, "DV", 6)).startswith(original_dv) or marker not in cell_text(cell_xml(moved, "DV", 6)):
    fail("DV did not keep the previous text")
if cell_text(cell_xml(moved, "G", 6)) != "20" or cell_text(cell_xml(moved, "B", 6)) != code:
    fail("Moved row lost identity or parameters")
for column in ("A", "C", "D", "E", "F", "H", "P", "Q", "S", "AS", "BW"):
    if cell_text(cell_xml(moved, column, 6)) != cell_text(cell_xml(source, column, 6)):
        fail(f"Column {column} changed")

sheet3_out = replace_once(
    replace_sheet_rows(sheet3, rows3[:5]),
    'ref="A1:EN6"',
    'ref="A1:EN5"',
    "制作中 dimension",
)
sheet4_out = replace_once(
    replace_sheet_rows(sheet4, rows4 + [moved]),
    'ref="A1:EN5"',
    'ref="A1:EN6"',
    "待验收 dimension",
)
if 'ref="A2:EN8"' not in sheet3_out or 'ref="A2:EN2"' not in sheet4_out:
    fail("AutoFilter changed")
changed = {
    "xl/worksheets/sheet3.xml": sheet3_out.encode("utf-8"),
    "xl/worksheets/sheet4.xml": sheet4_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"]:
        fail(f"Unexpected parts changed: {different}")
    making = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")
    accept = dst.read("xl/worksheets/sheet4.xml").decode("utf-8")
    if sheet_ids(making) != ["fire_heat_up", "fire_burn", "fire_cool"]:
        fail("Source still has or lost a neighbor")
    if sheet_ids(accept) != ["fire_fentian", "fire_liaoyuan", "fire_liaoli", code]:
        fail("Target row is incomplete")
    target = next(row for row in rows_of(accept) if row_number(row) == 6)
    if cell_text(cell_xml(target, "N", 6)) != display_n or cell_text(cell_xml(target, "O", 6)) != display_o:
        fail("Reopen lost the display text")
    if not cell_text(cell_xml(target, "DV", 6)).startswith(original_dv):
        fail("Reopen lost the original DV")
    for number, neighbor in ((3, "fire_heat_up"), (4, "fire_burn"), (5, "fire_cool")):
        row = next(item for item in rows_of(making) if row_number(item) == number)
        if row != rows3[number - 1]:
            fail(f"{neighbor} xml changed")
    for number, neighbor in ((3, "fire_fentian"), (4, "fire_liaoyuan"), (5, "fire_liaoli")):
        row = next(item for item in rows_of(accept) if row_number(item) == number)
        if row != rows4[number - 1]:
            fail(f"{neighbor} xml changed")
    if dst.read("xl/worksheets/sheet6.xml") != src.read("xl/worksheets/sheet6.xml"):
        fail("Resource detail changed")
    if dst.read("xl/worksheets/sheet7.xml") != src.read("xl/worksheets/sheet7.xml"):
        fail("Archive changed")
    if code.encode("utf-8") not in dst.read("xl/worksheets/sheet3.xml") and code.encode("utf-8") in dst.read("xl/worksheets/sheet4.xml"):
        pass
    else:
        fail("Id was not removed from 制作中 or not present on 待验收")

summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "sourceLastWriteTime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "sourceBytes": len(live_bytes),
    "backup": str(backup),
    "backupName": backup.name,
    "candidate": str(candidate),
    "candidateSha256": sha256(candidate.read_bytes()),
    "displayN": display_n,
    "displayO": display_o,
    "dvBefore": original_dv,
    "dvAfter": new_dv,
}
(batch / "writeback-snapshot.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"backup": backup.name, "candidateSha256": summary["candidateSha256"]}, ensure_ascii=False))
