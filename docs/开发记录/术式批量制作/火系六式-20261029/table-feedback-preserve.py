# -*- coding: utf-8 -*-
"""Append one dated design note to six DV cells inside a byte copy."""
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path

root = Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029")
backup = root / "备份" / "table-feedback-术式制作表-20261029-032140-1.xlsx"
output = root / "table-feedback-candidate-preserved.xlsx"
result_path = root / "table-feedback-result.json"
baseline = "053366B785B3C9B25C4E43B75F36F93F68C142655740EFAB1CF5F79FBF43B3B6"
record_link = "docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
process_marker = f"制作中 | 火系六式-20261029 | {record_link}"
feedback = {
    "fire_liaoyuan": f"[设计反馈 2026-10-29] 用户已确认燎原v2。从原点向外铺火浪，之后维持贴地火海/低矮火焰/临时炭纹。原N数值与不改地形约束保留。确认版归档docs/资源库/特效/火焰/燎原_v2/。详细原话与过程见 {record_link}。",
    "fire_liaoli": f"[设计反馈 2026-10-29] 用户已确认燎里v6。八剑左右镜像，剑柄向后倾；火从玩家背后逐渐覆盖剑柄与剑身；手持刺击双流朝玩家身后；同时3道环不断增大并后移。透明视觉不新增攻击。确认版归档docs/资源库/特效/火焰/燎里_v6/。详细原话与过程见 {record_link}。",
    "fire_fentian": f"[设计反馈 2026-10-29] 用户强调“特效是用在敌人身上的”，已确认焚天v2：金白火柱/赤红爆燃/4tick脉冲/余烬，按各敌人实际计划段数播放。确认版归档docs/资源库/特效/火焰/焚天_v2/。详细原话与过程见 {record_link}。",
    "fire_heat_up": f"[设计反馈 2026-10-29] 用户已确认炉心三式v1。升温是普通炉心低密度暖光。确认版归档docs/资源库/特效/火焰/炉心三式_v1/。详细原话与过程见 {record_link}。",
    "fire_burn": f"[设计反馈 2026-10-29] 用户已确认炉心三式v1。燃烧是过载较强橙红反馈。确认版归档docs/资源库/特效/火焰/炉心三式_v1/。详细原话与过程见 {record_link}。",
    "fire_cool": f"[设计反馈 2026-10-29] 用户已确认炉心三式v1。冷却只在成功主动cool时播放一次蒸汽。确认版归档docs/资源库/特效/火焰/炉心三式_v1/。详细原话与过程见 {record_link}。",
}
wanted = [
    (3, "燎原", "fire_liaoyuan"),
    (4, "燎里", "fire_liaoli"),
    (5, "焚天", "fire_fentian"),
    (6, "升温", "fire_heat_up"),
    (7, "燃烧", "fire_burn"),
    (8, "冷却", "fire_cool"),
]
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}


def fail(message):
    raise SystemExit(message)


def cell_xml(row_xml, column, row_number):
    token = f'<c r="{column}{row_number}"'
    start = row_xml.find(token)
    if start < 0 or row_xml.find(token, start + len(token)) >= 0:
        fail(f"Cell {column}{row_number} is not unique")
    opener_end = row_xml.find(">", start)
    if opener_end < 0:
        fail(f"Cell {column}{row_number} has no tag end")
    if row_xml[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = row_xml.find("</c>", opener_end)
        if close < 0:
            fail(f"Cell {column}{row_number} is not closed")
        end = close + 4
    return row_xml[start:end], start, end


def cell_text(cell):
    inline = re.search(r"<t(?: [^>]*)?>([\s\S]*)</t>", cell)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<v>([\s\S]*)</v>", cell)
    if value:
        return html.unescape(value.group(1))
    fail(f"No text in {cell[:80]}")


def rows_of(sheet_xml):
    return re.findall(r'<row r="\d+"[^>]*>[\s\S]*?</row>', sheet_xml)


def sheet_ids(sheet_xml):
    found = []
    for row_xml in rows_of(sheet_xml):
        row_number = int(re.search(r'<row r="(\d+)"', row_xml).group(1))
        if row_number < 3 or f'<c r="B{row_number}"' not in row_xml:
            continue
        code = cell_text(cell_xml(row_xml, "B", row_number)[0]).strip()
        if code:
            found.append((code, row_number))
    return found


source_bytes = backup.read_bytes()
if hashlib.sha256(source_bytes).hexdigest().upper() != baseline:
    fail("Backup hash is not the baseline")

with zipfile.ZipFile(backup) as archive:
    parts = {name: archive.read(name) for name in archive.namelist()}
    order = archive.infolist()

original = parts["xl/worksheets/sheet3.xml"]
if original.decode("utf-8").encode("utf-8") != original:
    fail("Making sheet is not stable utf-8")
sheet = original.decode("utf-8")
row_map = {int(re.search(r'<row r="(\d+)"', row).group(1)): row for row in rows_of(sheet)}
if set(row_map) != {1, 2, 3, 4, 5, 6, 7, 8}:
    fail(f"Making rows are {sorted(row_map)}")

updated_rows = {}
for row_number, name, code in wanted:
    row_xml = row_map[row_number]
    if cell_text(cell_xml(row_xml, "A", row_number)[0]) != name:
        fail(f"Name mismatch on row {row_number}")
    if cell_text(cell_xml(row_xml, "B", row_number)[0]) != code:
        fail(f"Id mismatch on row {row_number}")
    cell, start, end = cell_xml(row_xml, "DV", row_number)
    old_text = cell_text(cell)
    if process_marker not in old_text or "[设计反馈 2026-10-29]" in old_text:
        fail(f"DV{row_number} is not ready for one feedback append")
    if row_number <= 5 and "[设计反馈 2026-10-28]" not in old_text:
        fail(f"Earlier feedback missing on DV{row_number}")
    addition = "\n" + feedback[code]
    escaped = html.escape(addition, quote=False)
    if 't="str"' in cell[:80] and cell.endswith("</v></c>"):
        updated_cell = cell[: -len("</v></c>")] + escaped + "</v></c>"
    elif 't="inlineStr"' in cell[:140] and cell.endswith("</t></is></c>"):
        updated_cell = cell[: -len("</t></is></c>")] + escaped + "</t></is></c>"
    else:
        fail(f"Unsupported DV cell on row {row_number}")
    updated_row = row_xml[:start] + updated_cell + row_xml[end:]
    if cell_text(cell_xml(updated_row, "DV", row_number)[0]) != old_text + addition:
        fail(f"DV{row_number} text was not appended cleanly")
    for column in re.findall(rf'<c r="([A-Z]+){row_number}"', row_xml):
        if column != "DV" and cell_xml(row_xml, column, row_number)[0] != cell_xml(updated_row, column, row_number)[0]:
            fail(f"{column}{row_number} changed")
    updated_rows[row_number] = updated_row

sheet_out = sheet
for row_number, _name, _code in wanted:
    count = sheet_out.count(row_map[row_number])
    if count != 1:
        fail(f"Row {row_number} count is {count}")
    sheet_out = sheet_out.replace(row_map[row_number], updated_rows[row_number], 1)

restored = sheet_out
for _row_number, _name, code in wanted:
    addition = "\n" + feedback[code]
    if restored.count(addition) != 1:
        fail(f"Feedback for {code} is not unique")
    restored = restored.replace(addition, "", 1)
if restored.encode("utf-8") != original:
    fail("Sheet differs beyond the six DV additions")

changed = {"xl/worksheets/sheet3.xml": sheet_out.encode("utf-8")}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    if src.namelist() != dst.namelist():
        fail("Zip part list changed")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet3.xml"]:
        fail(f"Unexpected part changes: {different}")
    saved_sheet = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")

counts = {}
located = []
with zipfile.ZipFile(output) as dst:
    for title, name in status_parts.items():
        ids = sheet_ids(dst.read(name).decode("utf-8"))
        counts[title] = len(ids)
        located.extend((code, title, row_number) for code, row_number in ids)
codes = [item[0] for item in located]
claimed = [item for item in located if item[0] in feedback]
if len(codes) != len(set(codes)) or len(codes) != 46:
    fail(f"Id total {len(codes)} unique {len(set(codes))}")
if counts != {"已完成": 40, "待制作": 0, "制作中": 6, "待验收": 0}:
    fail(f"Counts {counts}")
if [(item[0], item[2]) for item in claimed] != [(code, row) for row, _name, code in wanted]:
    fail(f"Claimed location changed: {claimed}")
if saved_sheet != sheet_out:
    fail("Reopened sheet differs from authored sheet")

result = {
    "submitted": False,
    "baselineSha256": baseline,
    "baselineMtime": "2026-10-29 02:41:03.540",
    "baselineBytes": 77699,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": hashlib.sha256(output.read_bytes()).hexdigest().upper(),
    "changedParts": different,
    "counts": counts,
    "total": len(codes),
    "unique": len(set(codes)),
    "rows": [
        {"id": code, "name": name, "sheet": "制作中", "row": row_number, "feedback": feedback[code]}
        for row_number, name, code in wanted
    ],
}
result_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"candidateSha256": result["candidateSha256"], "counts": counts, "changedParts": different}, ensure_ascii=False))
