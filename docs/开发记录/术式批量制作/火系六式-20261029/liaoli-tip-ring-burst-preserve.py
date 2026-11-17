# -*- coding: utf-8 -*-
"""Return fire_liaoli to 制作中 and merge the tip-ring and pending burst notes."""
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
delivery = json.loads((batch / "liaoli-hit-seed-delivery-snapshot.json").read_text(encoding="utf-8"))
transition = json.loads((batch / "liaoli-hit-seed-transition-snapshot.json").read_text(encoding="utf-8"))
if delivery["approvedOriginal"]["n"] != transition["currentDetailed"]["n"]:
    raise SystemExit("Delivery and transition detailed N differ")
if not delivery["n"].startswith(delivery["approvedOriginal"]["n"]) or "确认，使用这版火苗" not in delivery["n"]:
    raise SystemExit("Latest approved N lost the flame confirmation")
if "0.36×S×(1+F)" not in delivery["n"] or "不改成0.2×S" not in delivery["n"]:
    raise SystemExit("Detailed N lost the shared-fuel formula")
change_n = (
    "2026-10-30用户实测修改。火焰环从刺出的手持剑剑尖生成，持续一路变大并向玩家身后移动，最终大于背后整个剑阵后才消散。"
    "现有有效命中且火种严格大于50才触发、三道环和rear_ring贴图继续作为已批准基础。此次修正发生位置、运动、最终尺度与收尾。不改上面的伤害和消耗。\n"
    "用户感觉收割100火种的目标伤害偏低、缺少爆发，并要求按玩家移动速度增强攻击，类似MC26.3长矛。"
    "伤害系数、移速采样和作用范围尚未批准，只记录待确认，不改现有6点物理和0.36×S×(1+F)。"
)
change_o = (
    "火焰环从刺出的手持剑剑尖发出，持续变大并向玩家身后移动，大过背后整个剑阵后消散。"
    "仍是三道环，仍用rear_ring贴图，仍只在有效命中且扣除前火种严格大于50时出现。\n"
    "移速增强和爆发的具体数值尚未确认，不写成已批准效果。"
)
restored_n = delivery["n"] + "\n" + change_n
restored_o = delivery["o"] + "\n" + change_o
addition = (
    "[设计反馈 2026-10-30] 用户实测后要求返修，本项从待验收退回制作中，不是验收通过，也不是实现或审核通过。"
    "火焰环改为从刺出的手持剑剑尖生成，持续一路变大并向玩家身后移动，最终大于背后整个剑阵后才消散。"
    "触发仍是有效命中且扣除前火种严格大于50，三道环和rear_ring贴图保留。\n"
    "用户感觉收割100火种的目标伤害偏低、缺少爆发，并要求按玩家移动速度增强攻击，类似MC26.3长矛。"
    "伤害/移速具体系数待用户决定，不把建议写成已确认，也不先改伤害。火环修复不因此等待。\n"
    "共享燃料、已确认火苗和v7姿态保留。本次实现尚未开始，待统一编译。上次命中火环编译不作为本次完成。"
    "设计2/2、实现3/3已用完。本阶段额度已用完，返修结果未追加复审。"
    "详见 docs/开发记录/术式批量制作/火系六式-20261029/记录.md。\n"
    "制作中 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
)


def fail(message):
    raise SystemExit(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_xml(row, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row))
    if len(matches) != 1:
        fail(f"Cell {column}{number} count is {len(matches)}")
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
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if value:
        return html.unescape(value.group(1))
    return None


def replace_text(cell, column, number, new_text):
    escaped = html.escape(new_text, quote=False)
    needs_space = "\n" in new_text or new_text[:1].isspace() or new_text[-1:].isspace()

    def opener(tag):
        if needs_space and "xml:space" not in tag:
            return tag[:-1] + ' xml:space="preserve">'
        return tag

    if re.search(r"<(?:[\w]+:)?t\b", cell):
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
    cell, start, end = cell_xml(row, column, number)
    return row[:start] + replace_text(cell, column, number, new_text) + row[end:]


def renumber(row, old, new):
    updated = re.sub(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row, count=1)
    updated = re.sub(rf'r="([A-Z]+){old}"', rf'r="\g<1>{new}"', updated)
    if row_number(updated) != new:
        fail(f"Row {old} did not become {new}")
    return updated


def replace_sheet_rows(xml, new_rows):
    match = re.search(r"(<(?:[\w]+:)?sheetData>)([\s\S]*)(</(?:[\w]+:)?sheetData>)", xml)
    return xml[:match.start(2)] + "".join(new_rows) + xml[match.end(2):]


def sheet_ids(xml):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number >= 3 and f'r="B{number}"' in row:
            code = cell_text(cell_xml(row, "B", number)[0])
            if code:
                found.append((code, number))
    return found


def zip_target(target):
    if target.startswith("/"):
        return target.lstrip("/")
    stack = ["xl", "worksheets"]
    for part in target.split("/"):
        if part == "..":
            stack.pop()
        elif part and part != ".":
            stack.append(part)
    return "/".join(stack)


live_bytes = live.read_bytes()
live_stat = live.stat()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"liaoli-tip-ring-burst-术式制作表-{stamp}-1.xlsx"
output = batch / "liaoli-tip-ring-burst-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)
with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}
sheet4_rels = parts.get("xl/worksheets/_rels/sheet4.xml.rels", b"").decode("utf-8")
comment_names = [zip_target(target) for target in re.findall(r'Target="([^"]+)"', sheet4_rels) if "comment" in target.lower() or target.lower().endswith(".vml")]
comment_blob = "\n".join(parts[name].decode("utf-8", errors="replace") for name in comment_names)
if re.search(r'\br="[A-Z]+5"', comment_blob):
    fail("A sheet4 comment is anchored on row 5")
if "fire_liaoli" in parts["xl/worksheets/sheet6.xml"].decode("utf-8"):
    fail("Resource detail has fire_liaoli")
sheet3 = parts["xl/worksheets/sheet3.xml"].decode("utf-8")
sheet4 = parts["xl/worksheets/sheet4.xml"].decode("utf-8")
rows3 = rows_of(sheet3)
rows4 = rows_of(sheet4)
if [row_number(row) for row in rows3] != [1, 2, 3, 4, 5]:
    fail("制作中 shape changed")
if [row_number(row) for row in rows4] != [1, 2, 3, 4, 5]:
    fail("待验收 shape changed")
for row, code in ((rows3[2], "fire_heat_up"), (rows3[3], "fire_burn"), (rows3[4], "fire_cool")):
    if cell_text(cell_xml(row, "B", row_number(row))[0]) != code:
        fail(f"Furnace changed: {code}")
for row, code in ((rows4[2], "fire_fentian"), (rows4[3], "fire_liaoyuan"), (rows4[4], "fire_liaoli")):
    if cell_text(cell_xml(row, "B", row_number(row))[0]) != code:
        fail(f"Pending changed: {code}")
source = rows4[4]
old_n = cell_text(cell_xml(source, "N", 5)[0])
old_o = cell_text(cell_xml(source, "O", 5)[0])
old_dv = cell_text(cell_xml(source, "DV", 5)[0])
if old_n != transition["conversion"]["nAfter"] or old_o != transition["conversion"]["oAfter"]:
    fail("Live display N/O is not the hit-seed writeback")
if not old_dv.startswith(transition["currentDetailed"]["dv"]) or "2026-10-30 02:15:29" not in old_dv:
    fail("Live DV lost the approved prefix or compile note")
if "剑尖生成" in old_dv:
    fail("Tip-ring note already present")
if "<f" in source:
    fail("燎里 row has a formula")
edited = set_cell(source, "N", 5, restored_n)
edited = set_cell(edited, "O", 5, restored_o)
edited = set_cell(edited, "DV", 5, old_dv + "\n" + addition)
if cell_text(cell_xml(edited, "E", 5)[0]) != "6" or cell_text(cell_xml(edited, "EC", 5)[0]) != "全部":
    fail("Damage or resource amount changed")
moved = renumber(edited, 5, 6)
sheet3_out = replace_sheet_rows(sheet3, rows3 + [moved])
sheet4_out = replace_sheet_rows(sheet4, rows4[:4])
if sheet3_out.count('ref="A1:EN5"') != 1 or sheet4_out.count('ref="A1:EN5"') != 1:
    fail("Dimension anchor missing")
sheet3_out = sheet3_out.replace('ref="A1:EN5"', 'ref="A1:EN6"', 1)
sheet4_out = sheet4_out.replace('ref="A1:EN5"', 'ref="A1:EN4"', 1)
uncovered = []
for validation in re.findall(r'<dataValidation\b[^>]*sqref="([^"]+)"', sheet3_out):
    for ref in validation.split():
        bounds = re.match(r"([A-Z]+)(\d+):([A-Z]+)(\d+)$", ref)
        if bounds and int(bounds.group(2)) <= 6 and int(bounds.group(4)) < 6:
            uncovered.append(ref)
if uncovered:
    fail(f"Validation does not cover row 6: {uncovered}")
changed = {
    "xl/worksheets/sheet3.xml": sheet3_out.encode("utf-8"),
    "xl/worksheets/sheet4.xml": sheet4_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"]:
        fail(f"Unexpected parts: {different}")
    counts = {}
    located = {}
    ids = []
    for title, name in (
        ("已完成", "xl/worksheets/sheet1.xml"),
        ("待制作", "xl/worksheets/sheet2.xml"),
        ("制作中", "xl/worksheets/sheet3.xml"),
        ("待验收", "xl/worksheets/sheet4.xml"),
    ):
        page = sheet_ids(dst.read(name).decode("utf-8"))
        counts[title] = len(page)
        for code, number in page:
            ids.append(code)
            located.setdefault(code, []).append((title, number))
    if counts != {"已完成": 40, "待制作": 13, "制作中": 4, "待验收": 2} or len(set(ids)) != 59:
        fail(f"Counts {counts} unique {len(set(ids))}")
    if located.get("fire_liaoli") != [("制作中", 6)]:
        fail(f"燎里 at {located.get('fire_liaoli')}")
    making = {row_number(row): row for row in rows_of(dst.read("xl/worksheets/sheet3.xml").decode("utf-8"))}
    pending = {row_number(row): row for row in rows_of(dst.read("xl/worksheets/sheet4.xml").decode("utf-8"))}
    for number in (3, 4, 5):
        if making[number] != rows3[number - 1]:
            fail(f"制作中 row {number} changed")
    for number in (3, 4):
        if pending[number] != rows4[number - 1]:
            fail(f"待验收 row {number} changed")
    if 5 in pending:
        fail("待验收仍有第5行")
    row = making[6]
    if cell_text(cell_xml(row, "N", 6)[0]) != restored_n or cell_text(cell_xml(row, "O", 6)[0]) != restored_o:
        fail("Restored N/O mismatch")
    if not cell_text(cell_xml(row, "DV", 6)[0]).startswith(old_dv) or "待用户决定" not in cell_text(cell_xml(row, "DV", 6)[0]):
        fail("DV prefix lost")
dependencies = {}
for column in ("A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "P", "Q", "R", "S", "U", "AP", "EB", "EC"):
    dependencies[column] = cell_text(cell_xml(moved, column, 6)[0])
summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "counts": counts,
    "from": {"sheet": "待验收", "row": 5},
    "to": {"sheet": "制作中", "row": 6},
}
(batch / "liaoli-tip-ring-burst-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
intake = {
    "id": "fire_liaoli",
    "name": "燎里",
    "sourceBeforeMove": {"sheet": "待验收", "row": 5},
    "target": {"sheet": "制作中", "row": 6},
    "submitted": False,
    "baselineSha256": baseline,
    "baselineMtime": summary["baselineMtime"],
    "baselineBytes": len(live_bytes),
    "currentDisplay": {"n": old_n, "o": old_o, "dv": old_dv},
    "approvedVersion": {
        "n": delivery["n"],
        "o": delivery["o"],
        "source": "liaoli-hit-seed-delivery-snapshot.json",
        "note": "最近已批准详细需求含共享燃料、v7姿态、火环大于50、常驻火苗和火苗外观确认。待验收一句话不是批准需求。",
    },
    "userChanges": [
        "火焰环从刺出的手持剑剑尖生成，持续一路变大并向玩家身后移动，最终大于背后整个剑阵后才消散。命中且火种大于50、三道环、rear_ring贴图仍是已批准基础。",
        "用户感觉收割100火种伤害偏低、缺爆发，并要求按玩家移动速度增强攻击，类似MC26.3长矛。伤害/移速具体系数待用户决定，不先改伤害。",
    ],
    "restored": {"n": restored_n, "o": restored_o, "dv": old_dv + "\n" + addition},
    "dependencies": dependencies,
    "quota": "设计2/2、实现3/3已用完。本阶段额度已用完，返修结果未追加复审。",
    "implementationStarted": False,
    "damageConfirmed": False,
}
(batch / "liaoli-tip-ring-burst-intake.json").write_text(json.dumps(intake, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"backup": backup.name, "candidateSha256": summary["candidateSha256"], "baseline": baseline, "counts": counts}, ensure_ascii=False))
