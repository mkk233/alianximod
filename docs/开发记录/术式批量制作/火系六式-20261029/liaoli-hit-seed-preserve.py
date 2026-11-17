# -*- coding: utf-8 -*-
"""Return fire_liaoli to 制作中 and merge the hit-ring, seed-flame, and follow changes."""
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
snapshot = json.loads((batch / "liaoli-pose-transition-snapshot.json").read_text(encoding="utf-8"))
intake = json.loads((batch / "liaoli-pose-intake.json").read_text(encoding="utf-8"))
approved_n = snapshot["currentDetailed"]["n"]
approved_o = snapshot["currentDetailed"]["o"]
if approved_n != intake["restored"]["n"] or approved_o != intake["restored"]["o"]:
    raise SystemExit("Pose snapshot and intake detailed N/O differ")
if "0.36×S×(1+F)" not in approved_n or "不改成0.2×S" not in approved_n:
    raise SystemExit("Detailed N lost the current fuel formula")
change_n = (
    "2026-10-30用户明确修改，不改上面的伤害、火种消耗、魔力和冷却。\n"
    "现有三道向后扩散火焰环只在有效命中时出现，并且目标在命中前、火种扣除前的火种值严格大于50。"
    "50不触发，51才触发。环仍用已确认的现有后扩环特效。\n"
    "另加火种常驻提示，这不是新术式，不新增编号。所有带火种的实体平时显示，每满10点当前火种余额一朵火苗，"
    "数量为floor(S/10)，低于10为0，100为10。消耗、余额变化、死亡或实体离开后同步减少或消失。"
    "用户已选所有带火种实体常驻显示。新二维火苗预览尚未确认，未确认素材不算批准。\n"
    "用户原话：“告诉移动时燎里特效更随不流畅明显看到特效闪现到身后”。"
    "按高速移动时跟随不流畅、特效跳到身后处理。特效连续跟随玩家，保留已批准v7剑阵姿态和现有特效贴图。"
)
change_o = (
    "三道后扩环只在有效命中、且命中前与火种扣除前余额严格大于50时出现，沿用已确认的现有后扩环，不换贴图。"
    "火种常驻火苗覆盖所有带火种实体，数量为floor(S/10)，低于10不显示，100为10朵；余额变化、死亡或离开后同步。"
    "新二维火苗预览未确认，这里不写未确认素材路径。\n"
    "高速移动时剑阵和现有特效连续跟随玩家，不跳到身后。姿态仍是已批准v7，贴图仍是现有燎里效果，不更换。"
)
restored_n = approved_n + "\n" + change_n
restored_o = approved_o + "\n" + change_o
addition = (
    "[设计反馈 2026-10-30] 用户要求返修，本项从待验收退回制作中，不是游戏实测通过。"
    "火环只在有效命中、且命中前与火种扣除前火种严格大于50时出现，50不触发，51才触发；沿用现有后扩环，不改伤害与消耗。"
    "火种常驻火苗是跨系统视觉：所有带火种实体显示，数量floor(S/10)，低于10为0，100为10，消耗、余额变化、死亡或离开后同步。"
    "用户已选所有带火种实体常驻显示。新二维火苗预览待确认，未确认素材不算批准。不新增编号，不把其他已完成行返修。"
    "用户原话：“告诉移动时燎里特效更随不流畅明显看到特效闪现到身后”。按高速移动跟随不流畅、闪到身后处理，连续跟随玩家，保留已批准v7姿态和现有贴图。"
    "本次实现尚未开始，新火苗待预览确认，待统一编译。上次姿态编译不作为本次完成。"
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
    updated, count = re.subn(
        pattern,
        lambda match: opener(match.group(1)) + escaped + match.group(2),
        cell,
        count=1,
    )
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


live_bytes = live.read_bytes()
live_stat = live.stat()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"liaoli-hit-seed-术式制作表-{stamp}-1.xlsx"
output = batch / "liaoli-hit-seed-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)
with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}
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


sheet4_rels = parts.get("xl/worksheets/_rels/sheet4.xml.rels", b"").decode("utf-8")
comment_names = []
for target in re.findall(r'Target="([^"]+)"', sheet4_rels):
    if "comment" in target.lower() or target.lower().endswith(".vml"):
        comment_names.append(zip_target(target))
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
if old_n != snapshot["conversion"]["nAfter"] or old_o != snapshot["conversion"]["oAfter"]:
    fail("Live display N/O is not the pose writeback")
if not old_dv.startswith(snapshot["currentDetailed"]["dv"]):
    fail("Live DV lost the approved prefix")
if "严格大于50" in old_dv or "火种常驻" in old_n:
    fail("Hit-seed note already present")
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
    if not cell_text(cell_xml(row, "DV", 6)[0]).startswith(old_dv):
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
(batch / "liaoli-hit-seed-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
new_intake = {
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
        "n": approved_n,
        "o": approved_o,
        "snapshot": "docs/开发记录/术式批量制作/火系六式-20261029/liaoli-pose-transition-snapshot.json",
        "intake": "docs/开发记录/术式批量制作/火系六式-20261029/liaoli-pose-intake.json",
        "note": "最新已批准详细需求是姿态快照里的共享燃料正文加已确认八剑姿态。待验收一句话和资源路径不是批准需求。",
    },
    "userChanges": [
        "三道后扩环只在有效命中且命中前、扣除前火种严格大于50时出现。50不触发，51才触发。沿用现有环，不改伤害与消耗。",
        "所有带火种实体常驻火苗，数量floor(S/10)，低于10为0，100为10。这是跨系统视觉，不新增编号。新二维火苗预览未确认。",
        "高速移动时燎里特效跟随不流畅并闪到身后。连续跟随玩家，保留已批准v7姿态和现有贴图。",
    ],
    "restored": {"n": restored_n, "o": restored_o, "dv": old_dv + "\n" + addition},
    "dependencies": dependencies,
    "crossSystem": "火种常驻火苗覆盖所有带火种实体，不新增术式编号，不返修其他已完成行。",
    "quota": "设计2/2、实现3/3已用完。本阶段额度已用完，返修结果未追加复审。",
    "implementationStarted": False,
    "flamePreviewConfirmed": False,
    "compile": "待统一编译。上次姿态编译不作为本次完成。",
}
(batch / "liaoli-hit-seed-intake.json").write_text(json.dumps(new_intake, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"backup": backup.name, "candidateSha256": summary["candidateSha256"], "counts": counts}, ensure_ascii=False))
