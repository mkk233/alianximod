# -*- coding: utf-8 -*-
"""Return fire_liaoli to 制作中 and merge the confirmed sword-array pose."""
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
before = json.loads((root / "docs" / "开发记录" / "火种共享燃料改造-20261029" / "final-writeback-before.json").read_text(encoding="utf-8"))
conversion = json.loads((root / "docs" / "开发记录" / "火种共享燃料改造-20261029" / "final-writeback-conversion.json").read_text(encoding="utf-8"))
detail = next(item for item in before["items"] if item["id"] == "fire_liaoli")
approved_n = detail["n"]
approved_o = detail["o"]
if approved_n != conversion["items"]["fire_liaoli"]["beforeN"]:
    raise SystemExit("Detailed N is not the shared-fuel snapshot")
if "0.36×S×(1+F)" not in approved_n or "不改成0.2×S" not in approved_n:
    raise SystemExit("Detailed N lost the current fuel formula")
pose_n = (
    "2026-10-30只返修背后八剑的位置、朝向和随角色姿态跟随。"
    "站立时剑尖朝下，剑柄靠近玩家，剑尖更远，左右镜像；横身俯冲时剑阵随身体转动并整体移到腿部，采用最后微微前移的确认版。"
    "火焰、流光和三道火环沿用现有游戏效果。不新增玩法，不改上面的伤害、火种、魔力和冷却。"
)
pose_o = (
    "背后八剑沿用现有游戏内的火焰、流光和三道火环，不更换这些贴图。"
    "站立时剑尖朝下，剑柄靠近玩家，剑尖更远，左右镜像。"
    "横身俯冲时剑阵随身体转动，整体移到腿部，并采用确认版微微前移的位置。"
    "灰蓝角色和红柄金刃只是方向示意，不进入游戏，也不新增攻击。"
)
restored_n = approved_n + "\n" + pose_n
restored_o = approved_o + "\n" + pose_o
addition = (
    "[设计反馈 2026-10-30] 用户确认背后八剑姿态，不是游戏实测通过。本项从待验收退回制作中。"
    "用户原话依次是：剑柄更靠近玩家，剑尖更远；剑尖应该朝下只有角色横过来俯冲时才会是现在的角度；"
    "横置是剑阵真题位置应该移动到腿部；往前一点点；ok就用这一版特效还用现在游戏内的。"
    "确认版 docs/资源库/过程归档/燎里俯冲腿部剑阵预览-20261030/liaoli-dive-leg-array-forward.bbmodel"
    " 与 dive-leg-forward-side.png。站立参考 docs/资源库/过程归档/燎里站立与俯冲预览-20261030/standing-side.png。"
    "只修八剑位置、朝向和姿态跟随。现有六张 liaoli_v6 运行贴图继续使用，不替换素材。"
    "玩法恢复共享燃料详细需求，不回滚初版燃料规则。设计2/2、实现3/3已用完，本次不追加复审。源码尚未开始写入。"
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
    if re.search(r"<(?:[\w]+:)?t\b", cell):
        updated, count = re.subn(
            r"(<(?:[\w]+:)?t\b[^>]*>)[\s\S]*(</(?:[\w]+:)?t>)",
            lambda match: match.group(1) + escaped + match.group(2),
            cell,
            count=1,
        )
    elif re.search(r"<(?:[\w]+:)?v\b", cell):
        updated, count = re.subn(
            r"(<(?:[\w]+:)?v\b[^>]*>)[\s\S]*(</(?:[\w]+:)?v>)",
            lambda match: match.group(1) + escaped + match.group(2),
            cell,
            count=1,
        )
    else:
        fail(f"Cell {column}{number} has no text")
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
backup = batch / "备份" / f"liaoli-pose-return-术式制作表-{stamp}-1.xlsx"
output = batch / "liaoli-pose-return-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)
with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}
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
for row, code in ((rows4[2], "fire_liaoli"), (rows4[3], "fire_fentian"), (rows4[4], "fire_liaoyuan")):
    if cell_text(cell_xml(row, "B", row_number(row))[0]) != code:
        fail(f"Pending changed: {code}")
source = rows4[2]
old_n = cell_text(cell_xml(source, "N", 3)[0])
old_o = cell_text(cell_xml(source, "O", 3)[0])
old_dv = cell_text(cell_xml(source, "DV", 3)[0])
if old_n != conversion["items"]["fire_liaoli"]["displayN"]:
    fail("Live N is not the current one-sentence display")
if "2026-10-30" in old_dv and "背后八剑姿态" in old_dv:
    fail("Pose note already present")
if "<f" in source:
    fail("燎里 row has a formula")
edited = set_cell(source, "N", 3, restored_n)
edited = set_cell(edited, "O", 3, restored_o)
edited = set_cell(edited, "DV", 3, old_dv + "\n" + addition)
if cell_text(cell_xml(edited, "E", 3)[0]) != "6" or cell_text(cell_xml(edited, "EC", 3)[0]) != "全部":
    fail("Damage or resource amount changed")
moved = renumber(edited, 3, 6)
sheet3_out = replace_sheet_rows(sheet3, rows3 + [moved])
sheet4_out = replace_sheet_rows(sheet4, [rows4[0], rows4[1], renumber(rows4[3], 4, 3), renumber(rows4[4], 5, 4)])
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
    row = making[6]
    if cell_text(cell_xml(row, "N", 6)[0]) != restored_n or cell_text(cell_xml(row, "O", 6)[0]) != restored_o:
        fail("Restored N/O mismatch")
    if not cell_text(cell_xml(row, "DV", 6)[0]).startswith(old_dv):
        fail("DV prefix lost")
    for old, new, code in ((4, 3, "fire_fentian"), (5, 4, "fire_liaoyuan")):
        original = rows4[old - 1]
        current = pending[new]
        for column in ("A", "B", "N", "O", "DV"):
            if cell_text(cell_xml(current, column, new)[0]) != cell_text(cell_xml(original, column, old)[0]):
                fail(f"{code} {column} changed")
summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "counts": counts,
    "from": {"sheet": "待验收", "row": 3},
    "to": {"sheet": "制作中", "row": 6},
}
(batch / "liaoli-pose-return-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
intake = {
    "id": "fire_liaoli",
    "name": "燎里",
    "sourceBeforeMove": {"sheet": "待验收", "row": 3},
    "target": {"sheet": "制作中", "row": 6},
    "submitted": False,
    "baselineSha256": baseline,
    "baselineMtime": summary["baselineMtime"],
    "baselineBytes": len(live_bytes),
    "currentDisplay": {"n": old_n, "o": old_o, "dv": old_dv},
    "approvedDetail": {
        "n": approved_n,
        "o": approved_o,
        "source": "docs/开发记录/火种共享燃料改造-20261029/final-writeback-before.json",
        "conversion": "docs/开发记录/火种共享燃料改造-20261029/final-writeback-conversion.json",
        "laterApprovedSnapshot": "无更晚的详细玩法快照。待验收一句话和资源路径不是批准需求。",
    },
    "restored": {"n": restored_n, "o": restored_o},
    "dependencies": {
        "C": "剑术", "D": "火", "E": "6", "F": "300", "G": "20", "H": "20",
        "J": "否", "K": "3", "L": "12", "M": "是", "P": "命中体", "Q": "持续",
        "R": "跟随准星", "S": "攻击", "U": "40", "AP": "40", "EB": "命中成功", "EC": "全部",
    },
    "pose": {
        "confirmedModel": "docs/资源库/过程归档/燎里俯冲腿部剑阵预览-20261030/liaoli-dive-leg-array-forward.bbmodel",
        "confirmedImage": "docs/资源库/过程归档/燎里俯冲腿部剑阵预览-20261030/dive-leg-forward-side.png",
        "standingReference": "docs/资源库/过程归档/燎里站立与俯冲预览-20261030/standing-side.png",
        "note": "docs/资源库/过程归档/燎里俯冲腿部剑阵预览-20261030/预览说明.txt",
    },
    "ownership": {
        "sourceAuthor": "1f4155a4-e537-4518-a3f7-893f09c5daff",
        "compileAuthor": "b2d5b359-42e3-4a90-9d24-b26acec6d5f8",
        "tableAuthor": "d6f84d0b-4ea1-4368-a576-8acc20cc2afd",
        "sourceWriteStarted": False,
    },
    "quota": "设计2/2、实现3/3已用完。本次不追加复审。",
    "nextInput": "只修背后八剑的位置、朝向和姿态跟随。火焰、流光、三道火环沿用现有游戏效果。不新增玩法，不改伤害和火种。",
}
(batch / "liaoli-pose-intake.json").write_text(json.dumps(intake, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"backup": backup.name, "candidateSha256": summary["candidateSha256"], "counts": counts}, ensure_ascii=False))
