# -*- coding: utf-8 -*-
"""Append this round's execution result to six DV cells inside a byte copy."""
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path

root = Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029")
backup = root / "备份" / "table-result-术式制作表-20261029-043043-1.xlsx"
output = root / "table-result-candidate-preserved.xlsx"
result_path = root / "table-result-result.json"
snapshot_path = root / "input-snapshot.json"
baseline = "01819BBEA99BB4CD6A9F9E398C9779E39D09962140A372E84FFDD9B8A874DF0C"
record_link = "docs/开发记录/术式批量制作/火系六式-20261029/记录.md"
compile_link = "docs/开发记录/术式批量制作/火系六式-20261029/compile-final-report.md"
result = (
    "2026-10-29本轮执行结果：已确认素材接入。compile-final报告的确认命令."
    "\\gradlew.bat compileJava --offline --no-daemon于2026-10-29 04:24:17至04:24:31退出0，"
    "BUILD SUCCESSFUL，compileJava为UP-TO-DATE。04:17同命令包装进程因日志管道堵住被终止，没有交回退出码，不当成成功。"
    "定向检查javac --release 17退出0；SixFireLogicCheck、LiaoliV6LayoutCheck、FireVisualLayoutCheck、"
    "KnownLimitsCheck、FurnaceAtomicCheck退出0；FurnaceStructureCheck断言19失败0、FurnaceRepairCheck断言15失败0、"
    "FireReleaseStructureCheck断言41失败0，均退出0。参数被拆开的java启动退出1，不是断言失败。"
    "未跑全量compileTestJava，未打包，未启动游戏；Mixin、持续伤害、炉心生命周期、视觉、关系过滤和击退都未游戏验证。"
    "这些退出码不是正式复审通过。设计/实现额度2/2已用完，本阶段额度已用完，返修结果未追加复审。游戏未实测，保持制作中。"
    f"详见 {record_link} 与 {compile_link}。"
)
seed_note = {
    "fire_liaoyuan": (
        "[设计反馈 2026-10-29] compile-final报告写明FireSeedState.grant在主源码没有调用方，也没有给目标加火种的现成命令。"
        "燎原无火种的地表火伤仍可按现有入口准备，但有火种的一次消费和附着燃烧目前不能按正常玩法准备。"
        f"该缺口未解决，这里不新增产火种途径或调试命令。详见 {compile_link}。"
    ),
    "fire_liaoli": (
        "[设计反馈 2026-10-29] compile-final报告写明FireSeedState.grant在主源码没有调用方，也没有给目标加火种的现成命令。"
        "燎里无火种的物理刺击仍可按现有入口准备，但火种大于0时的追加火伤和一次消费目前不能按正常玩法准备。"
        f"该缺口未解决，这里不新增产火种途径或调试命令。详见 {compile_link}。"
    ),
    "fire_fentian": (
        "[设计反馈 2026-10-29] compile-final报告写明FireSeedState.grant在主源码没有调用方，也没有给目标加火种的现成命令。"
        "焚天只处理火种大于0的目标，因此本式点燃和段伤害目前不能按正常玩法准备。"
        f"该缺口未解决，这里不新增产火种途径或调试命令。详见 {compile_link}。"
    ),
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


def addition_for(code):
    if code in seed_note:
        return result + "\n" + seed_note[code]
    return result


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


def snapshot_rows():
    data = json.loads(snapshot_path.read_text(encoding="utf-8-sig"))
    found = {}
    for entry in data["sheets"]["待制作"]:
        cells = entry.get("cells")
        if not cells:
            continue
        row_number = entry["row"]
        code = cells.get(f"B{row_number}")
        if code in {item[2] for item in wanted}:
            found[code] = {
                "row": row_number,
                "name": cells.get(f"A{row_number}"),
                "n": cells.get(f"N{row_number}"),
                "o": cells.get(f"O{row_number}"),
                "dv": cells.get(f"DV{row_number}"),
            }
    id_hits = [item for item in data.get("ids", []) if item[0] in found]
    return found, id_hits


if hashlib.sha256(backup.read_bytes()).hexdigest().upper() != baseline:
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

snapshot, snapshot_ids = snapshot_rows()
if set(snapshot) != {code for _row, _name, code in wanted}:
    fail(f"Snapshot ids {sorted(snapshot)}")
if sorted(item[0] for item in snapshot_ids) != sorted(code for _row, _name, code in wanted):
    fail("Snapshot id list does not match the six rows")
if any(item[1] != "待制作" for item in snapshot_ids):
    fail(f"Snapshot sheets changed: {snapshot_ids}")

reconcile = []
updated_rows = {}
for row_number, name, code in wanted:
    row_xml = row_map[row_number]
    if cell_text(cell_xml(row_xml, "A", row_number)[0]) != name:
        fail(f"Name mismatch on row {row_number}")
    if cell_text(cell_xml(row_xml, "B", row_number)[0]) != code:
        fail(f"Id mismatch on row {row_number}")
    feature = cell_text(cell_xml(row_xml, "N", row_number)[0])
    effect = cell_text(cell_xml(row_xml, "O", row_number)[0])
    cell, start, end = cell_xml(row_xml, "DV", row_number)
    old_text = cell_text(cell)
    source = snapshot[code]
    if feature != source["n"] or effect != source["o"]:
        fail(f"N/O no longer matches input snapshot for {code}")
    if not old_text.startswith(source["dv"]):
        fail(f"DV prefix no longer matches input snapshot for {code}")
    if "2026-10-29本轮执行结果" in old_text:
        fail(f"DV{row_number} already has this execution result")
    reconcile.append({"id": code, "snapshotRow": source["row"], "currentRow": row_number, "n": True, "o": True, "dvPrefix": True})
    addition = "\n" + addition_for(code)
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
    if sheet_out.count(row_map[row_number]) != 1:
        fail(f"Row {row_number} is not unique")
    sheet_out = sheet_out.replace(row_map[row_number], updated_rows[row_number], 1)
restored = sheet_out
for row_number, _name, code in wanted:
    if restored.count(updated_rows[row_number]) != 1:
        fail(f"Updated row {row_number} for {code} is not unique")
    restored = restored.replace(updated_rows[row_number], row_map[row_number], 1)
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

counts = {}
located = []
with zipfile.ZipFile(output) as dst:
    saved_sheet = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")
    for title, name in status_parts.items():
        ids = sheet_ids(dst.read(name).decode("utf-8"))
        counts[title] = len(ids)
        located.extend((code, title, row_number) for code, row_number in ids)
codes = [item[0] for item in located]
if len(codes) != len(set(codes)) or len(codes) != 46:
    fail(f"Id total {len(codes)} unique {len(set(codes))}")
if counts != {"已完成": 40, "待制作": 0, "制作中": 6, "待验收": 0}:
    fail(f"Counts {counts}")
claimed = [(code, row) for code, _title, row in located if code in {item[2] for item in wanted}]
if claimed != [(code, row) for row, _name, code in wanted]:
    fail(f"Locations changed: {claimed}")
if saved_sheet != sheet_out:
    fail("Reopened sheet differs")

summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "baselineMtime": "2026-10-29 03:23:58.136",
    "baselineBytes": 78137,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": hashlib.sha256(output.read_bytes()).hexdigest().upper(),
    "changedParts": different,
    "counts": counts,
    "total": len(codes),
    "unique": len(set(codes)),
    "snapshotReconcile": reconcile,
    "rows": [
        {
            "id": code,
            "name": name,
            "sheet": "制作中",
            "row": row_number,
            "seedFeedback": code in seed_note,
        }
        for row_number, name, code in wanted
    ],
}
result_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"candidateSha256": summary["candidateSha256"], "counts": counts, "changedParts": different, "snapshot": reconcile}, ensure_ascii=False))
