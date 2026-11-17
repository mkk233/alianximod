# -*- coding: utf-8 -*-
"""Return only fire_fentian from 待验收 to 制作中 and merge the approved change."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
task = root / "docs" / "开发记录" / "焚天定时燃尽-20261112"
live = root / "docs" / "术式制作表.xlsx"
before_path = root / "docs" / "开发记录" / "火种共享燃料改造-20261029" / "final-writeback-before.json"
conversion_path = root / "docs" / "开发记录" / "火种共享燃料改造-20261029" / "final-writeback-conversion.json"
expected_baseline = "9A5960132434A5F6AE5322C32EDF27EE5B25E2A56CE5FF3F5B712B32AF8147B7"
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}


def fail(message):
    raise SystemExit(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()


def rows_of(sheet_xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet_xml)


def row_number(row_xml):
    return int(re.search(r'\br="(\d+)"', row_xml).group(1))


def cell_bounds(row_xml, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row_xml))
    if len(matches) != 1:
        fail(f"Cell {column}{number} count is {len(matches)}")
    start = matches[0].start()
    opener_end = row_xml.find(">", start)
    if row_xml[opener_end - 1] == "/":
        return start, opener_end + 1
    close = re.search(r"</(?:[\w]+:)?c>", row_xml[opener_end:])
    if not close:
        fail(f"Cell {column}{number} is not closed")
    return start, opener_end + close.end()


def cell_xml(row_xml, column, number):
    start, end = cell_bounds(row_xml, column, number)
    return row_xml[start:end]


def cell_text(cell):
    texts = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", cell)
    if texts:
        return html.unescape("".join(texts))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    return html.unescape(value.group(1)) if value else ""


def xml_text(value):
    return html.escape(value, quote=False).replace("\n", "&#10;")


def replace_inline(cell, column, number, new_text):
    node = re.search(r"<(?:[\w]+:)?t\b([^>]*)>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if not node or cell.count("<t") != 1:
        fail(f"Cell {column}{number} does not have exactly one text node")
    updated = cell[:node.start(2)] + xml_text(new_text) + cell[node.end(2):]
    if cell_text(updated) != new_text:
        fail(f"Cell {column}{number} text was not written cleanly")
    return updated


def replace_number(cell, column, number, new_text):
    node = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if not node or cell.count("<v") != 1 or 't="n"' not in cell:
        fail(f"Cell {column}{number} is not a single numeric value")
    updated = cell[:node.start(1)] + new_text + cell[node.end(1):]
    if cell_text(updated) != new_text:
        fail(f"Cell {column}{number} number was not written cleanly")
    return updated


def set_cell(row_xml, column, number, new_text, numeric=False):
    start, end = cell_bounds(row_xml, column, number)
    cell = row_xml[start:end]
    updated = replace_number(cell, column, number, new_text) if numeric else replace_inline(cell, column, number, new_text)
    return row_xml[:start] + updated + row_xml[end:]


def renumber(row_xml, old, new):
    updated = re.sub(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row_xml, count=1)
    updated = re.sub(rf'r="([A-Z]+){old}"', rf'r="\g<1>{new}"', updated)
    if row_number(updated) != new or re.search(rf'\br="{old}"', updated) or re.search(rf'r="[A-Z]+{old}"', updated):
        fail(f"Row {old} did not become {new} cleanly")
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
        code = cell_text(cell_xml(row_xml, "B", number)).strip()
        if code:
            found.append((code, number, row_xml))
    return found


def replace_once(xml, old, new, label):
    if xml.count(old) != 1:
        fail(f"{label} count is {xml.count(old)}")
    return xml.replace(old, new, 1)


before = json.loads(before_path.read_text(encoding="utf-8"))
conversion = json.loads(conversion_path.read_text(encoding="utf-8"))
approved = next(item for item in before["items"] if item.get("id") == "fire_fentian")
converted = conversion["items"]["fire_fentian"]
if approved["n"] != converted["beforeN"] or approved["o"] != converted["beforeO"]:
    fail("Approved before snapshot and conversion snapshot differ")
if approved["cells"]["E"]["header"] != "基础伤害" or approved["cells"]["E"]["text"] != "2":
    fail("Approved column E is not base damage 2")

approved_n = approved["n"]
approved_o = approved["o"]
replacements = [
    ("无共享余额不挂资源燃烧、不受本术式资源伤害。",
     "结算前余额为0时该拍无伤，但固定窗口仍留到40tick截止，期间可接后续补给。"),
    ("启动时只观察n=floor(当时余额/10)和炉心F，并锁定指数与炉心参数。完整段伤害为2×(1.2^n+F)。每个完整段请求10点共享火种；实付不足10点时按实付/10缩放，不再把启动余量r/10再乘一次。点燃后第4、8、12tick结算，最多10次、40有效tick结束。补种只给后续段提供燃料，不提高指数，不延长40tick上限。余额耗尽则结束，不能免费等待。",
     "窗口固定2秒、40tick，在第4、8、12至40tick共10个时点结算，不因补种延长。启动时只快照炉心F，不锁定火种指数。每一拍按结算前当前余额S和含本次的剩余结算数R计算请求C=ceil(S/R)。实际伤害=(实际扣到的火种C/10)×3×(1.2^(S/10)+F)。S下降时指数跟着下降。补种提高后续拍的C和伤害。不再把每段请求固定为10点，也不再把累计消耗限定为100点。最后一拍燃尽当时窗口内已入账的有效余额。目标火种余额上限仍是100。"),
    ("100点观察时n=10，完整段约12.38；50点n=5。这些是燃料充足时的单段，不是预付后保证的总伤。1至9点n=0，完整段为2×(1+F)，按实付比例缩放。",
     "累计150的消耗示例：起手100，前5拍各扣10、合计50，随后补入50，后5拍各扣20。这只说明消耗，不是实测伤害。指数用该拍结算前的S，不再用挂载时锁定的n。"),
    ("引爆若先清余额，本层失去后续燃料；本层先扣部分，引爆只消费余量。",
     "引爆若先清余额，本层后续拍在再次入账前没有燃料；本层先扣部分，引爆只消费余量。"),
    ("4tick间隔、指数档数与40tick上限均不被聚能或多份规则放大。",
     "4tick间隔与40tick窗口均不被聚能或多份规则放大。燎原、幽冥、种火的数值，以及本式冷却、法力和半径，均不改。"),
]
merged_n = approved_n
for old, new in replacements:
    if merged_n.count(old) != 1:
        fail(f"Approved phrase missing: {old}")
    merged_n = merged_n.replace(old, new, 1)
for stale in ("完整段伤害为2×", "约12.38", "2×(1+F)", "锁定指数", "每个完整段请求10点", "余额耗尽则结束"):
    if stale in merged_n:
        fail(f"Stale phrase survived: {stale}")
for required in ("C=ceil(S/R)", "×3×(1.2^(S/10)+F)", "40tick", "后5拍各扣20", "上限仍是100", "燎原、幽冥、种火"):
    if required not in merged_n:
        fail(f"Merged phrase missing: {required}")
balance = 100
taken = []
for index in range(10):
    if index == 5:
        balance += 50
    remaining = 10 - index
    requested = (balance + remaining - 1) // remaining
    paid = min(requested, balance)
    balance -= paid
    taken.append(paid)
if taken != [10, 10, 10, 10, 10, 20, 20, 20, 20, 20] or sum(taken) != 150:
    fail(f"Consumption example does not match the stated 150 case: {taken}")

live_bytes = live.read_bytes()
live_stat = live.stat()
baseline = sha256(live_bytes)
if baseline != expected_baseline or len(live_bytes) != 179165:
    fail(f"Live workbook is not the required baseline: {baseline} {len(live_bytes)}")
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = task / "备份" / f"术式制作表-{stamp}-1.xlsx"
output = task / "术式制作表-候选.xlsx"
backup.parent.mkdir(parents=True, exist_ok=True)
if backup.exists() or output.exists():
    fail("Output path already exists")
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original_parts = {info.filename: archive.read(info.filename) for info in order}
if b"fire_fentian" in original_parts["xl/worksheets/sheet6.xml"]:
    fail("Resource detail has fire_fentian")

sheet3 = original_parts[status_parts["制作中"]].decode("utf-8")
sheet4 = original_parts[status_parts["待验收"]].decode("utf-8")
ids3 = sheet_ids(sheet3)
ids4 = sheet_ids(sheet4)
found = [item for item in ids4 if item[0] == "fire_fentian"]
if len(found) != 1 or any(item[0] == "fire_fentian" for item in ids3):
    fail("fire_fentian is not uniquely in 待验收")
source_number, source = found[0][1], found[0][2]
if source_number != 3 or "<f" in source:
    fail(f"Unexpected source row {source_number}")
if cell_text(cell_xml(source, "A", source_number)) != "焚天":
    fail("Name mismatch")
if cell_text(cell_xml(source, "E", source_number)) != "2":
    fail("Column E is not 2")
current_n = cell_text(cell_xml(source, "N", source_number))
current_o = cell_text(cell_xml(source, "O", source_number))
current_dv = cell_text(cell_xml(source, "DV", source_number))
if current_n != converted["displayN"] or current_o != converted["displayO"]:
    fail("Current display does not match the saved acceptance conversion")
if not current_dv.startswith("用户原话：“至于焚天就按照你说的"):
    fail("DV prefix changed")

addition = (
    "[设计反馈 2026-11-12] 用户已批准焚天改为固定2秒（40tick）、每4tick结算、共10个时点。"
    "每段消耗C=ceil(结算前当前火种S/含本次剩余结算数R)；实际伤害=(实际扣种C/10)×3×(1.2^(S/10)+F)，"
    "F沿用启动时炉心快照。基数由2改为3，并接受S下降带来的指数下降。补种提高后续C和伤害，不延长2秒，"
    "不再累计限定100。最后一段燃尽窗口内有效入账余额；S=0的当拍无伤，窗口留到截止并可接补给。"
    "目标余额上限100不变。累计150示例为100起手、前5段共50、补50、后5段各20。"
    "燎原、幽冥、种火以及本式冷却、法力、半径不改。E列基础伤害由2改为3。"
    "本轮不制作新美术；2026-10-29已确认的敌人身上焚天v2和运行贴图fentian_v2/conflagration.png仍是既有视觉记录，"
    "不因旧待验收视为这次玩法已通过。没有待确认项。本阶段不编译、不写编译通过、不迁待验收或已完成。"
    "设计2/2、实现3/3已用完，不新增正式复审。详见 docs/开发记录/术式批量制作/火系六式-20261029/记录.md。\n"
    "制作中 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
)
edited = set_cell(source, "E", source_number, "3", numeric=True)
edited = set_cell(edited, "N", source_number, merged_n)
edited = set_cell(edited, "O", source_number, approved_o)
edited = set_cell(edited, "DV", source_number, current_dv + "\n" + addition)
if 'ht="' in edited:
    edited = re.sub(r'(<row\b[^>]*\bht=")[^"]+(")', r'\g<1>409.6\2', edited, count=1)
target_number = max(number for _code, number, _row in ids3) + 1
moved = renumber(edited, source_number, target_number)
for column in ("A", "B", "C", "D", "F", "K", "L", "AP", "AS", "AN", "AO"):
    if cell_text(cell_xml(moved, column, target_number)) != cell_text(cell_xml(source, column, source_number)):
        fail(f"Column {column} changed")
if cell_text(cell_xml(moved, "E", target_number)) != "3":
    fail("Moved E mismatch")
if cell_text(cell_xml(moved, "N", target_number)) != merged_n or cell_text(cell_xml(moved, "O", target_number)) != approved_o:
    fail("Moved N/O mismatch")
if not cell_text(cell_xml(moved, "DV", target_number)).startswith(current_dv + "\n"):
    fail("Moved DV was not preserved")

rows4 = rows_of(sheet4)
source_position = next(index for index, row in enumerate(rows4) if row_number(row) == source_number)
sheet4_out = replace_sheet_rows(sheet4, rows4[:source_position] + rows4[source_position + 1:])
sheet3_out = replace_sheet_rows(sheet3, rows_of(sheet3) + [moved])
sheet3_out = replace_once(sheet3_out, 'ref="A1:EN24"', f'ref="A1:EN{target_number}"', "制作中 dimension")

changed = {
    status_parts["制作中"]: sheet3_out.encode("utf-8"),
    status_parts["待验收"]: sheet4_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    if src.namelist() != dst.namelist():
        fail("Zip part list changed")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != [status_parts["制作中"], status_parts["待验收"]]:
        fail(f"Unexpected part changes: {different}")
    counts = {}
    located = {}
    all_ids = []
    originals = {}
    for title, name in status_parts.items():
        current = sheet_ids(dst.read(name).decode("utf-8"))
        originals[title] = sheet_ids(src.read(name).decode("utf-8"))
        counts[title] = len(current)
        for code, number, _row in current:
            located.setdefault(code, []).append((title, number))
            all_ids.append(code)
    if counts != {"已完成": 37, "待制作": 3, "制作中": 22, "待验收": 16}:
        fail(f"Counts {counts}")
    if len(all_ids) != 78 or len(set(all_ids)) != 78 or located.get("fire_fentian") != [("制作中", target_number)]:
        fail(f"Identity check failed: {located.get('fire_fentian')}")
    if b"fire_fentian" in dst.read(status_parts["待验收"]):
        fail("待验收 still contains fire_fentian")
    for name in ("xl/worksheets/sheet5.xml", "xl/worksheets/sheet6.xml", "xl/worksheets/sheet7.xml", "xl/styles.xml", "xl/workbook.xml"):
        if dst.read(name) != src.read(name):
            fail(f"{name} changed")
    for index in range(1, 5):
        if dst.read(f"xl/comments/comment{index}.xml") != src.read(f"xl/comments/comment{index}.xml"):
            fail("Comments changed")
    making_rows = {number: row for _code, number, row in sheet_ids(dst.read(status_parts["制作中"]).decode("utf-8"))}
    for code, number, original_row in originals["制作中"]:
        if making_rows[number] != original_row:
            fail(f"制作中 row {code} changed")
    pending_rows = {number: row for _code, number, row in sheet_ids(dst.read(status_parts["待验收"]).decode("utf-8"))}
    for code, number, original_row in originals["待验收"]:
        if code != "fire_fentian" and pending_rows.get(number) != original_row:
            fail(f"待验收 row {code} changed")

meta = {
    "attempt": "sheet-repair-01",
    "baselineSha256": baseline,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "source": {"sheet": "待验收", "row": source_number},
    "target": {"sheet": "制作中", "row": target_number},
    "countsCandidate": counts,
    "eBefore": "2",
    "eAfter": "3",
    "approvedSnapshot": str(before_path),
    "nAfter": merged_n,
    "oAfter": approved_o,
    "dvAddition": addition,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "changedParts": different,
    "submitted": False,
}
(task / "candidate-meta.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
(task / "需求快照.json").write_text(json.dumps({
    "approvedSource": str(before_path),
    "conversionSource": str(conversion_path),
    "approvedN": approved_n,
    "approvedO": approved_o,
    "currentDisplayN": current_n,
    "currentDisplayO": current_o,
    "mergedN": merged_n,
    "mergedO": approved_o,
    "eBefore": "2",
    "eAfter": "3",
    "unchanged": {"F": "1200", "AP": "100", "AS": "8", "K": "3", "L": "4", "AN": "1", "AO": "1"},
    "consumptionExample": taken,
    "art": {
        "historicalConfirmation": "2026-10-29用户确认敌人身上的焚天v2",
        "runtime": "assets/xueximod/textures/effect/fentian_v2/conflagration.png",
        "thisRound": "不制作新美术，也不把旧待验收当成这次玩法通过",
    },
}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({"baseline": baseline, "sourceRow": source_number, "targetRow": target_number, "counts": counts, "candidateSha256": meta["candidateSha256"], "nLength": len(merged_n)}, ensure_ascii=False))
