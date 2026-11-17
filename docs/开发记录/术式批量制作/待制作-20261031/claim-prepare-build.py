# -*- coding: utf-8 -*-
"""Prepare an unsubmitted candidate that archives and claims the 13 pending rows."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

here = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
compare = json.loads((here / "claim-prepare-compare.json").read_text(encoding="utf-8"))
if not compare["batchRowsUnchanged"]:
    raise SystemExit("本批13行已变化，不生成覆盖候选")
EXPECTED = [item["id"] for item in compare["statusIds"]["待制作"]]
DV_ADD = (
    "[设计反馈 2026-10-31] 已领取。首版数值和链上角色仍有待定，正在核对底座，视觉待预览确认。具体未定项参照本项原文。\n"
    "制作中 | 待制作-20261031 | docs/开发记录/术式批量制作/待制作-20261031/记录.md"
)


def fail(message):
    raise SystemExit(message)


def sha256(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def local(tag):
    return tag.rsplit("}", 1)[-1]


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
    if not value:
        return None
    raw = html.unescape(value.group(1))
    if re.search(r'\bt="s"', cell) and raw.isdigit():
        return shared[int(raw)]
    return raw


def replace_text(cell, column, number, new_text):
    escaped = html.escape(new_text, quote=False)
    needs_space = "\n" in new_text or new_text[:1].isspace() or new_text[-1:].isspace()

    def opener(tag):
        if needs_space and "xml:space" not in tag:
            return tag[:-1] + ' xml:space="preserve">'
        return tag

    if re.search(r"<(?:[\w]+:)?t\b", cell):
        pattern = r"(<(?:[\w]+:)?t\b[^>]*>)[\s\S]*(</(?:[\w]+:)?t>)"
    elif re.search(r"<(?:[\w]+:)?v\b", cell) and not re.search(r'\bt="s"', cell):
        pattern = r"(<(?:[\w]+:)?v\b[^>]*>)[\s\S]*(</(?:[\w]+:)?v>)"
    else:
        fail(f"Cell {column}{number} cannot be patched in place")
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
    if row_number(updated) != new or f'r="A{old}"' in updated:
        fail(f"Row {old} did not become {new}")
    return updated


def sheet_map(workbook_xml, rel_xml):
    targets = {}
    rels = ET.fromstring(rel_xml)
    for rel in rels:
        if local(rel.tag) != "Relationship":
            continue
        target = rel.attrib["Target"].lstrip("/")
        if not target.startswith("xl/"):
            target = "xl/" + target
        targets[rel.attrib["Id"]] = target
    found = {}
    root = ET.fromstring(workbook_xml)
    for sheet in root.iter():
        if local(sheet.tag) != "sheet":
            continue
        rid = sheet.attrib["{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"]
        found[sheet.attrib["name"]] = targets[rid]
    return found


def ids_of(xml):
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


def replace_data(xml, new_rows):
    match = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", xml)
    if not match:
        fail("sheetData missing")
    return xml[:match.start(2)] + "".join(new_rows) + xml[match.end(2):]


def replace_dimension(xml, new_ref):
    updated, count = re.subn(r'(<dimension\b[^>]*\bref=")[^"]+(")', rf"\g<1>{new_ref}\2", xml, count=1)
    if count != 1:
        fail("dimension was not updated")
    return updated


live_bytes = live.read_bytes()
baseline = sha256(live_bytes)
if baseline != compare["liveSha256"]:
    fail(f"读取后的源表又变了: {baseline}")
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = here / "备份" / f"术式制作表-{stamp}-1.xlsx"
output = here / "claim-prepare-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}
    shared_xml = parts.get("xl/sharedStrings.xml")

shared = []
if shared_xml:
    root = ET.fromstring(shared_xml)
    for item in root:
        if local(item.tag) == "si":
            texts = []
            for node in item.iter():
                if local(node.tag) == "t" and node.text:
                    texts.append(html.unescape(node.text))
            shared.append("".join(texts))

names = sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
pending_xml = parts[names["待制作"]].decode("utf-8")
making_xml = parts[names["制作中"]].decode("utf-8")
archive_xml = parts[names["待制作原始数据"]].decode("utf-8")
def merge_rows(xml):
    rows = set()
    for ref in re.findall(r'<mergeCell\b[^>]*ref="([A-Z]+)(\d+):([A-Z]+)(\d+)"', xml):
        rows.update(range(int(ref[1]), int(ref[3]) + 1))
    for ref in re.findall(r'<mergeCell\b[^>]*ref="([A-Z]+)(\d+)"', xml):
        rows.add(int(ref[1]))
    return rows


archive_merge_rows = merge_rows(archive_xml)
making_merge_rows = merge_rows(making_xml)

pending = ids_of(pending_xml)
making = ids_of(making_xml)
archived = ids_of(archive_xml)
if [code for code, _number, _row in pending] != EXPECTED:
    fail("待制作编号或顺序已变")
if [code for code, _number, _row in making] != ["fire_heat_up", "fire_burn", "fire_cool"]:
    fail("制作中现有行已变，停止以免覆盖别人的写入")
if [code for code, _number, _row in archived] != ["fire_liaoyuan", "fire_liaoli", "fire_fentian", "fire_heat_up", "fire_burn", "fire_cool", "fire_scorch_field"]:
    fail("原有档案不是那7项")
if any(code in EXPECTED for code, _number, _row in archived):
    fail("档案里已经有本批编号")
if b"fire_flame_trail" in parts[names["资源状态明细"]]:
    fail("资源明细出现了本批编号，脚本没有另档明细")

archive_rows = []
making_rows = []
notes = []
def first_clear(start, blocked, count):
    while any(number in blocked for number in range(start, start + count)):
        start += 1
    return start


archive_start = first_clear(max(number for _code, number, _row in archived) + 1, archive_merge_rows, len(EXPECTED))
making_start = first_clear(max(number for _code, number, _row in making) + 1, making_merge_rows, len(EXPECTED))
if any(number in merge_rows(pending_xml) for _code, number, _row in pending):
    fail("待制作数据行落在合并区域内")
for offset, (code, number, row) in enumerate(pending):
    if "<f" in row.lower():
        fail(f"{code} 含公式")
    old_n = cell_text(cell_xml(row, "N", number)[0])
    old_o = cell_text(cell_xml(row, "O", number)[0]) if cell_xml(row, "O", number) else None
    old_dv = cell_text(cell_xml(row, "DV", number)[0])
    role = cell_text(cell_xml(row, "S", number)[0]) if cell_xml(row, "S", number) else None
    if not old_n:
        fail(f"{code} 没有功能描述")
    archive_number = archive_start + offset
    making_number = making_start + offset
    archive_rows.append(renumber(row, number, archive_number))
    edited = set_cell(row, "DV", number, old_dv + "\n" + DV_ADD)
    if cell_text(cell_xml(edited, "N", number)[0]) != old_n:
        fail(f"{code} 的功能描述被改动")
    making_rows.append(renumber(edited, number, making_number))
    undecided = []
    if not role:
        undecided.append("链上角色空")
    blob = "\n".join(part for part in (old_n, old_o, old_dv) if part)
    if "待定" in blob or "未定" in blob:
        undecided.append("原文含待定或未定")
    notes.append({
        "id": code,
        "name": cell_text(cell_xml(row, "A", number)[0]),
        "fromRow": number,
        "archiveRow": archive_number,
        "makingRow": making_number,
        "chainRole": role,
        "clarification": undecided,
    })

pending_out = replace_dimension(replace_data(pending_xml, [row for row in rows_of(pending_xml) if row_number(row) < 3]), "A1:EN2")
making_body = rows_of(making_xml) + making_rows
archive_body = rows_of(archive_xml) + archive_rows
making_out = replace_dimension(replace_data(making_xml, making_body), f"A1:EN{making_start + len(EXPECTED) - 1}")
archive_out = replace_dimension(replace_data(archive_xml, archive_body), f"A1:EN{archive_start + len(EXPECTED) - 1}")
changed = {
    names["待制作"]: pending_out.encode("utf-8"),
    names["制作中"]: making_out.encode("utf-8"),
    names["待制作原始数据"]: archive_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != [names["待制作"], names["制作中"], names["待制作原始数据"]]:
        fail(f"Unexpected parts: {different}")
    pending_ids = [code for code, _number, _row in ids_of(dst.read(names["待制作"]).decode("utf-8"))]
    making_ids = ids_of(dst.read(names["制作中"]).decode("utf-8"))
    archive_ids = [code for code, _number, _row in ids_of(dst.read(names["待制作原始数据"]).decode("utf-8"))]
    if pending_ids:
        fail(f"待制作仍有编号: {pending_ids}")
    if [code for code, _number, _row in making_ids] != ["fire_heat_up", "fire_burn", "fire_cool"] + EXPECTED:
        fail("制作中顺序不对")
    if archive_ids != ["fire_liaoyuan", "fire_liaoli", "fire_fentian", "fire_heat_up", "fire_burn", "fire_cool", "fire_scorch_field"] + EXPECTED:
        fail("档案顺序不对")
    if len(archive_ids) != len(set(archive_ids)):
        fail("档案编号重复")
    status_names = ["已完成", "待制作", "制作中", "待验收"]
    counts = {}
    all_ids = []
    for title in status_names:
        page = [code for code, _number, _row in ids_of(dst.read(names[title]).decode("utf-8"))]
        counts[title] = len(page)
        all_ids.extend(page)
    if counts != {"已完成": 40, "待制作": 0, "制作中": 16, "待验收": 4} or len(all_ids) != 60 or len(set(all_ids)) != 60:
        fail(f"Counts {counts} unique {len(set(all_ids))}")
    for item in notes:
        row = next(row for code, _number, row in making_ids if code == item["id"])
        number = item["makingRow"]
        original = next(source for code, _source_number, source in pending if code == item["id"])
        source_number = item["fromRow"]
        if cell_text(cell_xml(row, "N", number)[0]) != cell_text(cell_xml(original, "N", source_number)[0]):
            fail(f"{item['id']} N 被转换了")
        if "已领取" not in cell_text(cell_xml(row, "DV", number)[0]):
            fail(f"{item['id']} 没有领取反馈")
        if cell_text(cell_xml(original, "DV", source_number)[0]) not in cell_text(cell_xml(row, "DV", number)[0]):
            fail(f"{item['id']} 原备注丢失")
        archived_row = ids_of(dst.read(names["待制作原始数据"]).decode("utf-8"))
        stored = next(candidate for code, _number, candidate in archived_row if code == item["id"])
        if cell_text(cell_xml(stored, "DV", item["archiveRow"])[0]) != cell_text(cell_xml(original, "DV", source_number)[0]):
            fail(f"{item['id']} 档案写入了设计反馈")

live_after = sha256(live.read_bytes())
summary = {
    "submitted": False,
    "statement": "候选已准备但原表未提交",
    "baselineSha256": baseline,
    "liveSha256After": live_after,
    "liveUnchanged": live_after == baseline,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "counts": counts,
    "uniqueStatusIds": 60,
    "moved": notes,
    "archivePreserved": ["fire_liaoyuan", "fire_liaoli", "fire_fentian", "fire_heat_up", "fire_burn", "fire_cool", "fire_scorch_field"],
    "resourceDetailArchived": [],
    "quota": {"design": "0/2", "implementation": "0/3", "countsAsReview": False},
    "fidelity": "候选按 OOXML 原部件复制。只改待制作、制作中、待制作原始数据三个工作表的行和范围。不用整表重建，避免改写样式、批注、验证和公式。",
}
(here / "claim-prepare.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"candidate": summary["candidateSha256"], "liveUnchanged": summary["liveUnchanged"], "counts": counts}, ensure_ascii=False))
