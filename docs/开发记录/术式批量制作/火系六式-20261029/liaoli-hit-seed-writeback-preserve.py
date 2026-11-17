# -*- coding: utf-8 -*-
"""Move fire_liaoli from 制作中 to 待验收 after the hit-seed compile."""
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
prepared = json.loads((batch / "liaoli-hit-seed-delivery-snapshot.json").read_text(encoding="utf-8"))
display_n = prepared["proposedDisplay"]["n"]
display_o = "\n".join(prepared["proposedDisplay"]["o"])
if "fire_seed_flame.png" not in display_o or "rear_ring.png" not in display_o:
    raise SystemExit("Display O lost a runtime texture")
if display_n != "持剑向前刺击。命中时花掉当时剩余的火种，并打一刀物理":
    raise SystemExit("Display N is not the existing game sentence")
addition = (
    "[设计反馈 2026-10-30] 命中火环、常驻火苗和高速跟随已停写并完成统一编译，从制作中迁入待验收。"
    "不是游戏实测通过，也不是新增审查通过。\n"
    "用户已选所有带火种实体常驻显示，并确认“确认，使用这版火苗”。"
    "正式图 docs/资源库/特效/火焰/火种常驻火苗_v1/fire_seed_flame.png，8帧各32×32，每帧100ms，"
    "SHA256 106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B。"
    "运行路径 assets/xueximod/textures/effect/fire_seed/fire_seed_flame.png。\n"
    "三道后扩环只在有效命中且扣除前共享余额严格大于50时播放现有一轮。50不触发，51才触发。空挥不播。"
    "不改6点物理和0.36×S×(1+F)，也不改命中时消费当时全部火种。\n"
    "火苗数量为floor(当前余额/10)：低于10为0，10为1，20为2，100为10。"
    "增减、消耗或死亡后同步。新跟踪的玩家看到当时数量。这是跨系统视觉，不新增编号。\n"
    "高速移动按同步下来的施法者身份连续跟随，两名玩家靠近时不绑到旁边的人。保留已批准v7姿态和六张v6贴图。\n"
    "实际写入者23ef4660-da4b-47a8-9341-8ec236ec5cc3已停写。1f4155a4-e537-4518-a3f7-893f09c5daff只读未写。"
    "compileJava --offline --no-daemon于2026-10-30 02:15:29至02:15:55退出0，compileJava实际执行，"
    "BUILD SUCCESSFUL in 25s。774个src/main文件编译前后一致，晚于最后一次源码写入。\n"
    "已知限制：绑定身份不可用时仍站立；服务端命中体按tick跟随，画面可领先约一拍；"
    "滑翔没有完整速度偏航；激流没有持续旋转；游泳没有原版额外平移。"
    "名称门禁只剩OriginalHumanoid.java既有的i和to。未游戏实测。\n"
    "设计2/2、实现3/3承接。本阶段额度已用完，返修结果未追加复审。"
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
backup = batch / "备份" / f"liaoli-hit-seed-writeback-术式制作表-{stamp}-1.xlsx"
output = batch / "liaoli-hit-seed-writeback-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)
with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}
sheet3_rels = parts.get("xl/worksheets/_rels/sheet3.xml.rels", b"").decode("utf-8")
comment_names = []
for target in re.findall(r'Target="([^"]+)"', sheet3_rels):
    if "comment" in target.lower() or target.lower().endswith(".vml"):
        comment_names.append(zip_target(target))
comment_blob = "\n".join(parts[name].decode("utf-8", errors="replace") for name in comment_names)
if re.search(r'\br="[A-Z]+6"', comment_blob):
    fail("A sheet3 comment is anchored on row 6")
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
if old_n != prepared["approvedOriginal"]["n"] or old_o != prepared["approvedOriginal"]["o"] or old_dv != prepared["approvedOriginal"]["dv"]:
    fail("Live N/O/DV is not the saved approved original")
if "0.36×S×(1+F)" not in old_n or "严格大于50" not in old_n:
    fail("Detailed requirement lost the fuel formula or ring threshold")
if "2026-10-30 02:15:29" in old_dv:
    fail("Compile writeback note already present")
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
        fail(f"Counts {counts} unique {len(set(ids))}")
    if located.get("fire_liaoli") != [("待验收", 5)]:
        fail(f"燎里 at {located.get('fire_liaoli')}")
    making = {row_number(row): row for row in rows_of(dst.read("xl/worksheets/sheet3.xml").decode("utf-8"))}
    pending = {row_number(row): row for row in rows_of(dst.read("xl/worksheets/sheet4.xml").decode("utf-8"))}
    for number in (3, 4, 5):
        if making[number] != rows3[number - 1]:
            fail(f"制作中 row {number} changed")
    if 6 in making:
        fail("制作中仍有第6行")
    for number in (3, 4):
        if pending[number] != rows4[number - 1]:
            fail(f"待验收 row {number} changed")
    row = pending[5]
    if cell_text(cell_xml(row, "N", 5)[0]) != display_n or cell_text(cell_xml(row, "O", 5)[0]) != display_o:
        fail("Display N/O mismatch")
    if not cell_text(cell_xml(row, "DV", 5)[0]).startswith(old_dv):
        fail("DV prefix lost")
summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "counts": counts,
    "from": {"sheet": "制作中", "row": 6},
    "to": {"sheet": "待验收", "row": 5},
}
(batch / "liaoli-hit-seed-writeback-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
transition = {
    "capturedBeforeLiveWrite": True,
    "baselineSha256": baseline,
    "baselineMtime": summary["baselineMtime"],
    "baselineBytes": len(live_bytes),
    "source": {"sheet": "制作中", "row": 6, "id": "fire_liaoli"},
    "target": {"sheet": "待验收", "row": 5, "id": "fire_liaoli"},
    "currentDetailed": {"n": old_n, "o": old_o, "dv": old_dv},
    "approvedVersion": {
        "n": prepared["n"],
        "o": prepared["o"],
        "dv": prepared["dv"],
        "source": "liaoli-hit-seed-delivery-snapshot.json",
    },
    "resourceMapping": prepared["resourceMapping"],
    "conversion": {"nBefore": old_n, "nAfter": display_n, "oBefore": old_o, "oAfter": display_o, "dvAfter": old_dv + "\n" + addition},
    "quota": "设计2/2、实现3/3承接。本阶段额度已用完，返修结果未追加复审。",
}
(batch / "liaoli-hit-seed-transition-snapshot.json").write_text(json.dumps(transition, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"backup": backup.name, "candidateSha256": summary["candidateSha256"], "baseline": baseline, "counts": counts}, ensure_ascii=False))
