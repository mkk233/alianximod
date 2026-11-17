# -*- coding: utf-8 -*-
"""Return fire_liaoyuan from pending acceptance to in progress. Does not touch other techniques."""
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
intake = json.loads((batch / "liaoyuan-visual-intake.json").read_text(encoding="utf-8"))
detail = intake["approvedDetail"]
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
addition = (
    "[设计反馈 2026-10-29] 用户实测燎原未通过。用户原话：只有释放第一下有特效，后续持续伤害无特效，周围地面未正确点燃；先修燎原，其余以后反馈。"
    "本项从待验收退回制作中，恢复最近一次已批准的共享燃料详细需求，不改燃料设计。"
    "目前是返修中，尚未重新编译，也未重新实测通过。"
    "不使用待制作原始数据，也不恢复更早的一次消费需求。"
    "详见 docs/开发记录/术式批量制作/火系六式-20261029/记录.md。\n"
    "制作中 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
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
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<(?:[\w]+:)?v>([\s\S]*)</(?:[\w]+:)?v>", cell)
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
    elif re.search(r"<(?:[\w]+:)?v>", cell):
        updated, count = re.subn(
            r"(<(?:[\w]+:)?v>)[\s\S]*(</(?:[\w]+:)?v>)",
            lambda match: match.group(1) + escaped + match.group(2),
            cell,
            count=1,
        )
    else:
        fail(f"Cell {column}{number} has no text node")
    if count != 1 or cell_text(updated) != new_text:
        fail(f"Cell {column}{number} text was not written cleanly")
    return updated


def set_cell(row_xml, column, number, new_text):
    cell, start, end = cell_xml(row_xml, column, number)
    return row_xml[:start] + replace_text(cell, column, number, new_text) + row_xml[end:]


def renumber(row_xml, old, new):
    updated = re.sub(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row_xml, count=1)
    updated = re.sub(rf'r="([A-Z]+){old}"', rf'r="\g<1>{new}"', updated)
    if row_number(updated) != new:
        fail(f"Row {old} did not become {new}")
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
            found.append((code.strip(), number))
    return found


if "不预扣全部余额" not in detail["n"] or "一次消耗全部" in detail["n"]:
    fail("approvedDetail is not the shared-fuel requirement")
if detail["o"].startswith("assets/"):
    fail("approvedDetail O is a resource path, not the detailed visual intent")

live_bytes = live.read_bytes()
live_stat = live.stat()
expected = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"liaoyuan-visual-return-术式制作表-{stamp}-1.xlsx"
output = batch / "liaoyuan-visual-return-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original_parts = {info.filename: archive.read(info.filename) for info in order}

detail_xml = original_parts["xl/worksheets/sheet6.xml"].decode("utf-8")
if "fire_liaoyuan" in detail_xml:
    fail("Resource detail has fire_liaoyuan; this script does not move detail rows")

sheet3 = original_parts["xl/worksheets/sheet3.xml"].decode("utf-8")
sheet4 = original_parts["xl/worksheets/sheet4.xml"].decode("utf-8")
rows3 = rows_of(sheet3)
rows4 = rows_of(sheet4)
if [row_number(row) for row in rows3] != [1, 2, 3, 4, 5]:
    fail(f"制作中 rows {[row_number(row) for row in rows3]}")
if [row_number(row) for row in rows4] != [1, 2, 3, 4, 5]:
    fail(f"待验收 rows {[row_number(row) for row in rows4]}")
for row_xml, code in ((rows3[2], "fire_heat_up"), (rows3[3], "fire_burn"), (rows3[4], "fire_cool")):
    if cell_text(cell_xml(row_xml, "B", row_number(row_xml))[0]) != code:
        fail(f"Furnace identity changed: {code}")
for row_xml, code in ((rows4[3], "fire_liaoli"), (rows4[4], "fire_fentian")):
    if cell_text(cell_xml(row_xml, "B", row_number(row_xml))[0]) != code:
        fail(f"Pending identity changed: {code}")

source = rows4[2]
if cell_text(cell_xml(source, "B", 3)[0]) != "fire_liaoyuan":
    fail("待验收第3行不是燎原")
if cell_text(cell_xml(source, "A", 3)[0]) != "燎原":
    fail("Name mismatch")
old_n = cell_text(cell_xml(source, "N", 3)[0])
old_o = cell_text(cell_xml(source, "O", 3)[0])
old_dv = cell_text(cell_xml(source, "DV", 3)[0])
if old_n != intake["n"] or old_o != intake["o"] or old_dv != intake["dv"]:
    fail("Live N/O/DV no longer matches the intake snapshot")
if "退回制作中" in old_dv:
    fail("Return note already present")
if "<f" in source:
    fail("燎原 row has a formula")

edited = set_cell(source, "N", 3, detail["n"])
edited = set_cell(edited, "O", 3, detail["o"])
edited = set_cell(edited, "DV", 3, old_dv + "\n" + addition)
if cell_text(cell_xml(edited, "EC", 3)[0]) != "10":
    fail("Resource amount changed")
if cell_text(cell_xml(edited, "EB", 3)[0]) != "燃烧结算":
    fail("Trigger changed")
moved = renumber(edited, 3, 6)

sheet3_out = replace_sheet_rows(sheet3, rows3 + [moved])
sheet4_out = replace_sheet_rows(sheet4, [
    rows4[0],
    rows4[1],
    renumber(rows4[3], 4, 3),
    renumber(rows4[4], 5, 4),
])
if sheet3_out.count('ref="A1:EN5"') != 1 or sheet4_out.count('ref="A1:EN5"') != 1:
    fail("Dimension anchor missing")
sheet3_out = sheet3_out.replace('ref="A1:EN5"', 'ref="A1:EN6"', 1)
sheet4_out = sheet4_out.replace('ref="A1:EN5"', 'ref="A1:EN4"', 1)

changed = {
    "xl/worksheets/sheet3.xml": sheet3_out.encode("utf-8"),
    "xl/worksheets/sheet4.xml": sheet4_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    if src.namelist() != dst.namelist():
        fail("Zip part list changed")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"]:
        fail(f"Unexpected part changes: {different}")
    counts = {}
    located = {}
    all_ids = []
    for title, name in status_parts.items():
        ids = sheet_ids(dst.read(name).decode("utf-8"))
        counts[title] = len(ids)
        for code, number in ids:
            located.setdefault(code, []).append((title, number))
            all_ids.append(code)
    if counts != {"已完成": 40, "待制作": 0, "制作中": 4, "待验收": 2}:
        fail(f"Counts {counts}")
    if len(all_ids) != 46 or len(set(all_ids)) != 46:
        fail("Status ids are not unique")
    expected_rows = {
        "fire_liaoyuan": ("制作中", 6),
        "fire_heat_up": ("制作中", 3),
        "fire_burn": ("制作中", 4),
        "fire_cool": ("制作中", 5),
        "fire_liaoli": ("待验收", 3),
        "fire_fentian": ("待验收", 4),
    }
    for code, place in expected_rows.items():
        if located.get(code) != [place]:
            fail(f"{code} at {located.get(code)}")
    making = {row_number(row): row for row in rows_of(dst.read("xl/worksheets/sheet3.xml").decode("utf-8"))}
    pending = {row_number(row): row for row in rows_of(dst.read("xl/worksheets/sheet4.xml").decode("utf-8"))}
    row_xml = making[6]
    if cell_text(cell_xml(row_xml, "N", 6)[0]) != detail["n"]:
        fail("Detailed N was not restored")
    if cell_text(cell_xml(row_xml, "O", 6)[0]) != detail["o"]:
        fail("Detailed O was not restored")
    remark = cell_text(cell_xml(row_xml, "DV", 6)[0])
    if not remark.startswith(old_dv) or addition not in remark:
        fail("DV was not preserved and appended")
    if "一次消耗全部" in cell_text(cell_xml(row_xml, "N", 6)[0]):
        fail("Old prepaid requirement was restored")
    for old, new, code in ((4, 3, "fire_liaoli"), (5, 4, "fire_fentian")):
        original = rows4[old - 1]
        current = pending[new]
        for column in ("A", "B", "N", "O", "DV"):
            if cell_text(cell_xml(current, column, new)[0]) != cell_text(cell_xml(original, column, old)[0]):
                fail(f"{code} {column} changed")
    for number, code in ((3, "fire_heat_up"), (4, "fire_burn"), (5, "fire_cool")):
        if making[number] != rows3[number - 1]:
            fail(f"{code} row bytes changed")

summary = {
    "submitted": False,
    "baselineSha256": expected,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "counts": counts,
    "changedParts": different,
    "from": {"sheet": "待验收", "row": 3},
    "to": {"sheet": "制作中", "row": 6},
}
(batch / "liaoyuan-visual-return-preserve-summary.json").write_text(
    json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "backup": backup.name,
    "baseline": expected,
    "candidateSha256": summary["candidateSha256"],
    "counts": counts,
    "changedParts": different,
}, ensure_ascii=False))
