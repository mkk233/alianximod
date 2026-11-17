# -*- coding: utf-8 -*-
"""Move fire_liaoyuan from 制作中 to 待验收 after the visual repair delivery."""
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
evidence = json.loads((batch / "liaoyuan-visual-current-evidence.json").read_text(encoding="utf-8"))
compile_result = json.loads((batch / "liaoyuan-visual-compile-results.json").read_text(encoding="utf-8"))
detail = intake["approvedDetail"]
display_n = intake["gameDisplay"]["n"]
display_o = intake["gameDisplay"]["o"]
expected = "FEFBC4B30087C08C6E8B9B6C482FEEEBC8DB6F6F3E65F6B88117E4D3D630C070"
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
addition = (
    "[设计反馈 2026-10-29] 两项视觉问题已由原作者交回并停止写入。"
    "地表火场画面改用世界时间，起手之后的火海、低火和炭纹留到火场寿命结束；"
    "附着燃烧按当前还活着的最高档画火舌和炭纹，目标移动或离开火场后随活跃层持续，燃料耗尽或该层结束就停。"
    "最终命令 .\\gradlew.bat compileJava --offline --no-daemon 于2026-10-29 23:18:01至23:18:26退出0，compileJava实际执行，覆盖这三份最终源字节。"
    "名称门禁退出码1，只剩 OriginalHumanoid 原有两处，不在本次文件。三张运行贴图未新增。"
    "设计2/2、实现3/3承接。本阶段额度已用完，返修结果未追加复审。"
    "旧火种来源复审通过不能当作这次修复通过。尚未游戏重测，不迁已完成。"
    "详见 docs/开发记录/术式批量制作/火系六式-20261029/记录.md。\n"
    "待验收 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
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


if compile_result.get("exitCode") != 0:
    fail("Compile evidence is not exit 0")
if "不预扣全部余额" not in detail["n"] or "一次消耗全部" in detail["n"]:
    fail("Approved detail is not the shared-fuel requirement")
if display_n != evidence["gameSentence"]:
    fail("Game sentence drifted from the author handback")
if set(evidence["resources"]) != {
    "textures/effect/liaoyuan_v2/ground_flame.png",
    "textures/effect/liaoyuan_v2/ground_embers.png",
    "textures/effect/liaoyuan_v2/spread_wave.png",
}:
    fail("Resource list drifted")

live_bytes = live.read_bytes()
live_stat = live.stat()
if sha256(live_bytes) != expected:
    fail(f"Live hash changed: {sha256(live_bytes)}")
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"liaoyuan-visual-writeback-术式制作表-{stamp}-1.xlsx"
output = batch / "liaoyuan-visual-writeback-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original_parts = {info.filename: archive.read(info.filename) for info in order}

if "fire_liaoyuan" in original_parts["xl/worksheets/sheet6.xml"].decode("utf-8"):
    fail("Resource detail has fire_liaoyuan; this script does not move detail rows")

sheet3 = original_parts["xl/worksheets/sheet3.xml"].decode("utf-8")
sheet4 = original_parts["xl/worksheets/sheet4.xml"].decode("utf-8")
rows3 = rows_of(sheet3)
rows4 = rows_of(sheet4)
if [row_number(row) for row in rows3] != [1, 2, 3, 4, 5, 6]:
    fail(f"制作中 rows {[row_number(row) for row in rows3]}")
if [row_number(row) for row in rows4] != [1, 2, 3, 4]:
    fail(f"待验收 rows {[row_number(row) for row in rows4]}")
for row_xml, code in ((rows3[2], "fire_heat_up"), (rows3[3], "fire_burn"), (rows3[4], "fire_cool")):
    if cell_text(cell_xml(row_xml, "B", row_number(row_xml))[0]) != code:
        fail(f"Furnace identity changed: {code}")
for row_xml, code in ((rows4[2], "fire_liaoli"), (rows4[3], "fire_fentian")):
    if cell_text(cell_xml(row_xml, "B", row_number(row_xml))[0]) != code:
        fail(f"Pending identity changed: {code}")

source = rows3[5]
if cell_text(cell_xml(source, "B", 6)[0]) != "fire_liaoyuan":
    fail("制作中第6行不是燎原")
if cell_text(cell_xml(source, "A", 6)[0]) != "燎原":
    fail("Name mismatch")
old_n = cell_text(cell_xml(source, "N", 6)[0])
old_o = cell_text(cell_xml(source, "O", 6)[0])
old_dv = cell_text(cell_xml(source, "DV", 6)[0])
if old_n != detail["n"] or old_o != detail["o"]:
    fail("Live N/O is not the approved shared-fuel detail")
if "退回制作中" not in old_dv:
    fail("Return feedback is missing from DV")
if "两项视觉问题已由原作者交回并停止写入" in old_dv:
    fail("Writeback note already present")
if "<f" in source:
    fail("燎原 row has a formula")

edited = set_cell(source, "N", 6, display_n)
edited = set_cell(edited, "O", 6, display_o)
edited = set_cell(edited, "DV", 6, old_dv + "\n" + addition)
if cell_text(cell_xml(edited, "EC", 6)[0]) != "10":
    fail("Resource amount changed")
if cell_text(cell_xml(edited, "EB", 6)[0]) != "燃烧结算":
    fail("Trigger changed")
if cell_text(cell_xml(edited, "EG", 6)[0]) != "20":
    fail("Period changed")
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
    fail(f"Validation does not cover the moved row: {uncovered}")

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
    if counts != {"已完成": 40, "待制作": 13, "制作中": 3, "待验收": 3}:
        fail(f"Counts {counts}")
    if len(all_ids) != 59 or len(set(all_ids)) != 59:
        fail("Status ids are not the current 59")
    if located.get("fire_liaoyuan") != [("待验收", 5)]:
        fail(f"燎原 at {located.get('fire_liaoyuan')}")
    making = {row_number(row): row for row in rows_of(dst.read("xl/worksheets/sheet3.xml").decode("utf-8"))}
    pending = {row_number(row): row for row in rows_of(dst.read("xl/worksheets/sheet4.xml").decode("utf-8"))}
    for number in (3, 4, 5):
        if making[number] != rows3[number - 1]:
            fail(f"制作中 row {number} bytes changed")
    for number in (3, 4):
        if pending[number] != rows4[number - 1]:
            fail(f"待验收 row {number} bytes changed")
    row_xml = pending[5]
    if cell_text(cell_xml(row_xml, "N", 5)[0]) != display_n:
        fail("Game sentence was not written")
    if cell_text(cell_xml(row_xml, "O", 5)[0]) != display_o:
        fail("Resource paths were not written")
    remark = cell_text(cell_xml(row_xml, "DV", 5)[0])
    if not remark.startswith(old_dv) or addition not in remark:
        fail("DV was not preserved and appended")
    if "fire_liaoyuan" in dst.read("xl/worksheets/sheet3.xml").decode("utf-8"):
        fail("Source still contains 燎原")

snapshot = {
    "capturedBeforeLiveWrite": True,
    "baselineSha256": expected,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "source": {"sheet": "制作中", "row": 6, "id": "fire_liaoyuan", "name": "燎原"},
    "target": {"sheet": "待验收", "row": 5, "id": "fire_liaoyuan", "name": "燎原"},
    "currentDetailed": {"n": old_n, "o": old_o, "dv": old_dv},
    "approvedSharedFuel": {"n": detail["n"], "o": detail["o"], "source": "docs/开发记录/火种共享燃料改造-20261029/final-writeback-before.json via liaoyuan-visual-intake.json approvedDetail"},
    "userFeedback": intake["userFeedback"],
    "repair": {
        "author": evidence["author"],
        "stoppedWriting": True,
        "rootCauses": [item["id"] for item in evidence["rootCauses"]],
        "sources": evidence["sources"],
        "nameGate": evidence["nameGate"],
        "compile": {
            "command": compile_result["command"],
            "start": compile_result["start"],
            "end": compile_result["end"],
            "exitCode": compile_result["exitCode"],
            "laterThanLastSourceWrite": compile_result["laterThanLastSourceWrite"],
        },
    },
    "resourceMapping": intake["resourceMapping"]["runtime"],
    "conversion": {"nBefore": old_n, "nAfter": display_n, "oBefore": old_o, "oAfter": display_o},
    "quota": "设计2/2、实现3/3承接。本阶段额度已用完，返修结果未追加复审。",
    "playtestPassed": False,
}
(batch / "liaoyuan-visual-transition-snapshot.json").write_text(
    json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8")
summary = {
    "submitted": False,
    "baselineSha256": expected,
    "baselineMtime": snapshot["baselineMtime"],
    "baselineBytes": len(live_bytes),
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "counts": counts,
    "changedParts": different,
    "from": {"sheet": "制作中", "row": 6},
    "to": {"sheet": "待验收", "row": 5},
}
(batch / "liaoyuan-visual-writeback-summary.json").write_text(
    json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "backup": backup.name,
    "candidateSha256": summary["candidateSha256"],
    "counts": counts,
    "changedParts": different,
}, ensure_ascii=False))
