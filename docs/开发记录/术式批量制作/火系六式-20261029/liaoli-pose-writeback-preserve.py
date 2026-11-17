# -*- coding: utf-8 -*-
"""Move fire_liaoli from 制作中 to 待验收 after the pose repair."""
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
compile_result = json.loads((batch / "liaoli-pose-compile-results.json").read_text(encoding="utf-8"))
display_n = "持剑向前刺击。命中时花掉当时剩余的火种，并打一刀物理"
display_o = (
    "assets/xueximod/textures/effect/liaoli_v6/sword_unlit.png\n"
    "assets/xueximod/textures/effect/liaoli_v6/sword_hot.png\n"
    "assets/xueximod/textures/effect/liaoli_v6/back_ignite.png\n"
    "assets/xueximod/textures/effect/liaoli_v6/back_flow.png\n"
    "assets/xueximod/textures/effect/liaoli_v6/thrust_backflow.png\n"
    "assets/xueximod/textures/effect/liaoli_v6/rear_ring.png\n"
    "docs/资源库/特效/火焰/燎里_剑阵姿态_v7/liaoli-dive-leg-array-forward.bbmodel"
)
addition = (
    "[设计反馈 2026-10-30] 背后八剑姿态已由原作者交回并停止写入。"
    "站立时剑尖朝下、柄靠近玩家、尖更远、左右镜像；身体横置时剑阵随俯仰转到腿部，并采用确认版前移。"
    "火焰锚点跟着新剑。六张 liaoli_v6 贴图、三道环、手持刺击分流、伤害和火种未改。"
    "正式姿态归档 docs/资源库/特效/火焰/燎里_剑阵姿态_v7/。"
    "同一作者随后只同步 src/test/java/com/bslx/XueXiMod/visual/liaoli/LiaoliV6LayoutCheck.java 的过时方向预期，没有运行该检查，没有新增独立测试。"
    "最终 .\\gradlew.bat compileJava --offline --no-daemon 于2026-10-30 00:56:57至00:57:38退出0，compileJava实际执行，晚于生产源码和检查文件。"
    "已知限制：施法者靠0.75格内眼睛位置匹配，识别失败回到站立；滑翔没有完整速度偏航；激流没有持续转圈；游泳没有原版额外平移。"
    "这些没有游戏验证，也不表示完全匹配原版。"
    "设计2/2、实现3/3承接。本阶段额度已用完，返修结果未追加复审。"
    "旧审查通过不能当作这次修复通过。尚未游戏实测，不迁已完成。"
    "详见 docs/开发记录/术式批量制作/火系六式-20261029/记录.md。\n"
    "待验收 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
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
            cell, count=1)
    elif re.search(r"<(?:[\w]+:)?v\b", cell):
        updated, count = re.subn(
            r"(<(?:[\w]+:)?v\b[^>]*>)[\s\S]*(</(?:[\w]+:)?v>)",
            lambda match: match.group(1) + escaped + match.group(2),
            cell, count=1)
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


if compile_result.get("exitCode") != 0 or not compile_result.get("laterThanLastSourceWrite"):
    fail("Compile evidence is not a later successful compile")
live_bytes = live.read_bytes()
live_stat = live.stat()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"liaoli-pose-writeback-术式制作表-{stamp}-1.xlsx"
output = batch / "liaoli-pose-writeback-candidate-preserved.xlsx"
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
if [row_number(row) for row in rows3] != [1, 2, 3, 4, 5, 6]:
    fail("制作中 shape changed")
if [row_number(row) for row in rows4] != [1, 2, 3, 4]:
    fail("待验收 shape changed")
for row, code in ((rows3[2], "fire_heat_up"), (rows3[3], "fire_burn"), (rows3[4], "fire_cool"), (rows3[5], "fire_liaoli")):
    if cell_text(cell_xml(row, "B", row_number(row))[0]) != code:
        fail(f"制作中 identity changed: {code}")
for row, code in ((rows4[2], "fire_fentian"), (rows4[3], "fire_liaoyuan")):
    if cell_text(cell_xml(row, "B", row_number(row))[0]) != code:
        fail(f"待验收 identity changed: {code}")
source = rows3[5]
old_n = cell_text(cell_xml(source, "N", 6)[0])
old_o = cell_text(cell_xml(source, "O", 6)[0])
old_dv = cell_text(cell_xml(source, "DV", 6)[0])
if "0.36×S×(1+F)" not in old_n or "2026-10-30只返修背后八剑" not in old_n:
    fail("Current N is not the approved detailed pose requirement")
if "背后八剑姿态" not in old_dv:
    fail("Pose feedback missing from current DV")
if "已知限制：施法者靠0.75格内眼睛位置匹配" in old_dv:
    fail("Writeback note already present")
if "<f" in source:
    fail("燎里 row has a formula")
edited = set_cell(source, "N", 6, display_n)
edited = set_cell(edited, "O", 6, display_o)
edited = set_cell(edited, "DV", 6, old_dv + "\n" + addition)
if cell_text(cell_xml(edited, "E", 6)[0]) != "6" or cell_text(cell_xml(edited, "EC", 6)[0]) != "全部":
    fail("Damage or resource amount changed")
moved = renumber(edited, 6, 5)
sheet3_out = replace_sheet_rows(sheet3, rows3[:5])
sheet4_out = replace_sheet_rows(sheet4, rows4 + [moved])
if sheet3_out.count('ref="A1:EN6"') != 1 or sheet4_out.count('ref="A1:EN4"') != 1:
    fail("Dimension anchor missing")
sheet3_out = sheet3_out.replace('ref="A1:EN6"', 'ref="A1:EN5"', 1)
sheet4_out = sheet4_out.replace('ref="A1:EN4"', 'ref="A1:EN5"', 1)
uncovered = []
for validation in re.findall(r'<dataValidation\b[^>]*sqref="([^"]+)"', sheet4_out):
    for ref in validation.split():
        bounds = re.match(r"([A-Z]+)(\d+):([A-Z]+)(\d+)$", ref)
        if bounds and int(bounds.group(2)) <= 5 and int(bounds.group(4)) < 5:
            uncovered.append(ref)
if uncovered:
    fail(f"Validation does not cover row 5: {uncovered}")
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
    if counts != {"已完成": 40, "待制作": 13, "制作中": 3, "待验收": 3} or len(set(ids)) != 59:
        fail(f"Counts {counts}")
    if located.get("fire_liaoli") != [("待验收", 5)]:
        fail(f"燎里 at {located.get('fire_liaoli')}")
    making = {row_number(row): row for row in rows_of(dst.read("xl/worksheets/sheet3.xml").decode("utf-8"))}
    pending = {row_number(row): row for row in rows_of(dst.read("xl/worksheets/sheet4.xml").decode("utf-8"))}
    for number in (3, 4, 5):
        if making[number] != rows3[number - 1]:
            fail(f"制作中 row {number} changed")
    for number in (3, 4):
        if pending[number] != rows4[number - 1]:
            fail(f"待验收 row {number} changed")
    row = pending[5]
    if cell_text(cell_xml(row, "N", 5)[0]) != display_n or cell_text(cell_xml(row, "O", 5)[0]) != display_o:
        fail("Display N/O mismatch")
    if not cell_text(cell_xml(row, "DV", 5)[0]).startswith(old_dv):
        fail("DV prefix lost")
    if "fire_liaoli" in dst.read("xl/worksheets/sheet3.xml").decode("utf-8"):
        fail("制作中 still contains 燎里")
snapshot = {
    "capturedBeforeLiveWrite": True,
    "baselineSha256": baseline,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "source": {"sheet": "制作中", "row": 6, "id": "fire_liaoli"},
    "target": {"sheet": "待验收", "row": 5, "id": "fire_liaoli"},
    "currentDetailed": {"n": old_n, "o": old_o, "dv": old_dv},
    "approvedPose": {
        "archive": "docs/资源库/特效/火焰/燎里_剑阵姿态_v7/",
        "model": "liaoli-dive-leg-array-forward.bbmodel",
        "userQuote": "ok就用这一版特效还用现在游戏内的",
    },
    "resourceMapping": display_o.split("\n"),
    "conversion": {"nBefore": old_n, "nAfter": display_n, "oBefore": old_o, "oAfter": display_o},
    "knownLimits": [
        "施法者靠0.75格内眼睛位置匹配，识别失败回到站立",
        "滑翔没有完整速度偏航",
        "激流没有持续转圈",
        "游泳没有原版额外平移",
    ],
    "quota": "设计2/2、实现3/3承接。本阶段额度已用完，返修结果未追加复审。",
}
(batch / "liaoli-pose-transition-snapshot.json").write_text(json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8")
summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "baselineMtime": snapshot["baselineMtime"],
    "baselineBytes": len(live_bytes),
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "counts": counts,
    "from": {"sheet": "制作中", "row": 6},
    "to": {"sheet": "待验收", "row": 5},
}
(batch / "liaoli-pose-writeback-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"backup": backup.name, "candidateSha256": summary["candidateSha256"], "counts": counts}, ensure_ascii=False))
