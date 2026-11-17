# -*- coding: utf-8 -*-
"""Move fire_liaoyuan from 制作中 to 待验收 and convert N/O for display."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
here = root / "docs" / "开发记录" / "燎原伤害定稿入表-20261031"
live = root / "docs" / "术式制作表.xlsx"
TARGET = "fire_liaoyuan"
NEW_N = "铺开半径10格的贴地火场，展开前沿即可命中。本体基础8点、每10刻一次并受一次炉心加成；有火种时足额段为4×(1+0.1n+F)，每段请求10点火种"
NEW_O = "\n".join([
    "xueximod:textures/effect/liaoyuan_v3/ground_ash.png",
    "xueximod:textures/effect/liaoyuan_v3/ground_ash_b.png",
    "xueximod:textures/effect/liaoyuan_v3/ground_ash_c.png",
    "xueximod:textures/effect/liaoyuan_v3/ground_ash_d.png",
    "xueximod:textures/effect/liaoyuan_v3/ground_embers.png",
    "xueximod:textures/effect/liaoyuan_v2/spread_wave.png",
    "xueximod:textures/effect/liaoyuan_v2/ground_flame.png",
])
DV_ADD = (
    "[设计反馈 2026-10-31] 本次返修已按批准版写入，并完成编译。"
    "命令 .\\gradlew.bat compileJava --offline --no-daemon 于 2026-10-31 02:40:32.528 至 02:40:58.358 退出 0，BUILD SUCCESSFUL in 25s。"
    "src/main 842 个文件前后一致，覆盖最后相关写入 2026-10-31 02:27:19。"
    "先前空缓存导致的编译失败保留为历史，不再作为现行阻塞。"
    "更早备注里的待美术与代码落实，已被这次实施结果取代。"
    "最近批准美术是过程 v5 草方块像素尺度的离散灰烬，用户原话“可以，使用这版”，正式归档 docs/资源库/特效/火焰/燎原_v3/。"
    "用户游戏实测仍待做。已知限制只引用作者结果：生成年龄 0 有效范围 0，最早年龄 1 才接触伤害；外围命中次数可能少于中心 20 次；"
    "360 只是满热且初始 100 点火种等条件下的理论值；抗火和减伤仍然适用；"
    "旧 KnownLimitsCheck 与 FireVisualLayoutCheck 仍期待半径 6 和本体 2，未更新未运行，不算通过。"
    "名称门禁里未改的 OriginalHumanoid 的 i 与 to 仍在，本次改动未新增。"
    "本阶段额度已用完，返修结果未追加复审。\n"
    "待验收 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
)
KEEP = {"E": "8", "AS": "10", "BV": "10", "BW": "200", "EC": "10", "EG": "20", "EH": "0", "EB": "燃烧结算"}


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
    found = cell_xml(row, column, number)
    if not found:
        fail(f"Missing {column}{number}")
    cell, start, end = found
    return row[:start] + replace_text(cell, column, number, new_text) + row[end:]


def renumber(row, old, new):
    if old == new:
        return row
    updated = re.sub(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row, count=1)
    updated = re.sub(rf'r="([A-Z]+){old}"', rf'r="\g<1>{new}"', updated)
    if row_number(updated) != new:
        fail(f"Row {old} did not become {new}")
    return updated


def sheet_ids(xml):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        found_cell = cell_xml(row, "B", number)
        code = cell_text(found_cell[0]) if found_cell else None
        if code:
            found.append((code, number, row))
    return found


def dimension(xml):
    match = re.search(r"<dimension\b[^>]*\bref=\"([^\"]+)\"", xml)
    if not match:
        fail("Missing dimension")
    return match.group(1)


live_bytes = live.read_bytes()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = here / "备份" / f"实施-待验收-术式制作表-{stamp}-1.xlsx"
output = here / "实施-待验收-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}

status_parts = (
    ("已完成", "xl/worksheets/sheet1.xml"),
    ("待制作", "xl/worksheets/sheet2.xml"),
    ("制作中", "xl/worksheets/sheet3.xml"),
    ("待验收", "xl/worksheets/sheet4.xml"),
)
locations = []
for title, name in status_parts:
    for code, number, _row in sheet_ids(parts[name].decode("utf-8")):
        if code == TARGET:
            locations.append((title, number))
if len(locations) != 1 or locations[0][0] != "制作中":
    fail(f"燎原位置变化: {locations}")
detail_xml = parts["xl/worksheets/sheet6.xml"]
if b"fire_liaoyuan" in detail_xml:
    fail("资源明细已有燎原，本脚本不改明细")

sheet3 = parts["xl/worksheets/sheet3.xml"].decode("utf-8")
sheet4 = parts["xl/worksheets/sheet4.xml"].decode("utf-8")
making = sheet_ids(sheet3)
pending = sheet_ids(sheet4)
source_number = locations[0][1]
source = next(row for code, number, row in making if code == TARGET)
if [code for code, _number, _row in making if code != TARGET] != ["fire_heat_up", "fire_burn", "fire_cool"]:
    fail("制作中除燎原外的身份变了")
if [code for code, _number, _row in pending] != ["fire_fentian", "fire_liaoli", "fire_scorch_field"]:
    fail("待验收邻居变了")
for column, expected in KEEP.items():
    if cell_text(cell_xml(source, column, source_number)[0]) != expected:
        fail(f"{column} 不是已批准的 {expected}")
old_n = cell_text(cell_xml(source, "N", source_number)[0])
old_o = cell_text(cell_xml(source, "O", source_number)[0])
old_dv = cell_text(cell_xml(source, "DV", source_number)[0])
if "基础火伤8点" not in old_n or "半径10" not in old_n:
    fail("制作中详细需求已变")
if old_n == NEW_N or old_o.startswith("xueximod:"):
    fail("展示转换已经写过")
if "BUILD SUCCESSFUL in 25s" in old_dv:
    fail("编译结果已经写过")
if "<f" in source.lower():
    fail("燎原行含公式")
(here / "实施-待验收-转换前.json").write_text(json.dumps({
    "baselineSha256": baseline,
    "from": {"sheet": "制作中", "row": source_number},
    "n": old_n,
    "o": old_o,
    "dv": old_dv,
    "kept": KEEP,
}, ensure_ascii=False, indent=2), encoding="utf-8")

edited = set_cell(source, "N", source_number, NEW_N)
edited = set_cell(edited, "O", source_number, NEW_O)
edited = set_cell(edited, "DV", source_number, old_dv + "\n" + DV_ADD)
for column, expected in KEEP.items():
    if cell_text(cell_xml(edited, column, source_number)[0]) != expected:
        fail(f"转换时改动了 {column}")
target_number = max(number for _code, number, _row in pending) + 1
moved = renumber(edited, source_number, target_number)
kept_making = [row for _code, number, row in making if number != source_number]
header_rows = [row for row in rows_of(sheet3) if row_number(row) < 3]
making_numbers = [number for _code, number, _row in making if number != source_number]
renumbered_making = []
for row, number in zip(kept_making, making_numbers):
    new_number = number - 1 if number > source_number else number
    renumbered_making.append(renumber(row, number, new_number))
sheet3_body = header_rows + renumbered_making
sheet4_body = [row for row in rows_of(sheet4) if row_number(row) < 3 or cell_xml(row, "B", row_number(row))] + [moved]
match3 = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", sheet3)
match4 = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", sheet4)
sheet3_out = sheet3[:match3.start(2)] + "".join(sheet3_body) + sheet3[match3.end(2):]
sheet4_out = sheet4[:match4.start(2)] + "".join([row for row in rows_of(sheet4)] + [moved]) + sheet4[match4.end(2):]
old_dim3 = dimension(sheet3)
old_dim4 = dimension(sheet4)
new_dim3 = f"A1:EN{max(row_number(row) for row in sheet3_body)}"
new_dim4 = f"A1:EN{target_number}"
if sheet3_out.count(f'ref="{old_dim3}"') != 1 or sheet4_out.count(f'ref="{old_dim4}"') != 1:
    fail(f"范围锚点缺失 {old_dim3} {old_dim4}")
sheet3_out = sheet3_out.replace(f'ref="{old_dim3}"', f'ref="{new_dim3}"', 1)
sheet4_out = sheet4_out.replace(f'ref="{old_dim4}"', f'ref="{new_dim4}"', 1)
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
    ids = []
    for title, name in status_parts:
        page = [code for code, _number, _row in sheet_ids(dst.read(name).decode("utf-8"))]
        counts[title] = len(page)
        ids.extend(page)
    if counts != {"已完成": 40, "待制作": 13, "制作中": 3, "待验收": 4} or len(ids) != 60 or len(set(ids)) != 60:
        fail(f"Counts {counts} unique {len(set(ids))}")
    making_out = sheet_ids(dst.read("xl/worksheets/sheet3.xml").decode("utf-8"))
    pending_out = sheet_ids(dst.read("xl/worksheets/sheet4.xml").decode("utf-8"))
    if [code for code, _number, _row in making_out] != ["fire_heat_up", "fire_burn", "fire_cool"]:
        fail("制作中还留着燎原或炉心错位")
    if [code for code, _number, _row in pending_out] != ["fire_fentian", "fire_liaoli", "fire_scorch_field", TARGET]:
        fail("待验收顺序不对")
    row = pending_out[3][2]
    landed = pending_out[3][1]
    if cell_text(cell_xml(row, "N", landed)[0]) != NEW_N:
        fail("游戏内文案不对")
    written_o = cell_text(cell_xml(row, "O", landed)[0])
    for token in NEW_O.split("\n"):
        if token not in written_o:
            fail(f"资源缺失 {token}")
    if TARGET in dst.read("xl/worksheets/sheet3.xml").decode("utf-8"):
        fail("制作中仍有燎原编号")

summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "from": f"制作中第{source_number}行",
    "to": f"待验收第{landed}行",
    "targetRow": landed,
    "counts": counts,
}
(here / "实施-待验收-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"baseline": baseline, "to": summary["to"], "candidate": summary["candidateSha256"]}, ensure_ascii=False))
