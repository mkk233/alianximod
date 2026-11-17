# -*- coding: utf-8 -*-
"""Snapshot the two furnace rows and build a candidate workbook. Does not replace the live sheet."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(r"D:\alianximod")
BATCH = ROOT / "docs" / "开发记录" / "炉心升温燃烧视觉接入-20261031"
LIVE = ROOT / "docs" / "术式制作表.xlsx"
README = ROOT / "docs" / "资源库" / "README.md"
STATUS = ("已完成", "待制作", "制作中", "待验收")
TARGETS = ("fire_heat_up", "fire_burn")
APPEND = "2026-10-31 已采用升温v2/燃烧v3并完成客户端接入，待Cursor结论收束及统一编译；游戏待用户验证；Codex未复审"
HEAT_O = (
    "已采用升温贴身炽热 v2。贴身橙热边缘，前臂和躯干侧部向上热流，少量余烬；头和中心保持清晰，不做地面火圈。"
    "维持 96×112、20帧×80ms 循环；入场 8帧×60ms，成功进入普通炉心时只播一次再循环。\n"
    "源：docs/资源库/特效/火焰/升温_贴身炽热_v2/（heat_up-v2.aseprite、heat_up-v2-strip.png、heat_up-v2-back-strip.png、"
    "heat_up-v2-front-strip.png、heat_up-v2-entry.aseprite、heat_up-v2-entry-strip.png）。预览与人偶不加载。\n"
    "接入：textures/effect/furnace_v2/heat_rear.png、heat_rear_tongues.png、heat_front_tongues.png、heat_embers.png、"
    "heat_entry_rear.png、heat_entry_rear_tongues.png、heat_entry_front_tongues.png、heat_entry_embers.png。"
)
BURN_O = (
    "已采用燃烧双手附焰与火焰披风 v3。双手附火：两个拳部最亮金白核心，向上橙色火舌，跟随左右手骨骼、持物、攻击和行走。"
    "肩、躯干、髋、腿局部附焰。肩背火焰披风向下垂并飘动，有破碎火焰边和内部火流；披风随身体背部，不是总朝相机的平面，也不是地面圆环。"
    "维持 96×112、20帧×80ms；入场 8帧×60ms。从普通炉心升级只播燃烧入场，不重播升温。\n"
    "源：docs/资源库/特效/火焰/燃烧_双手附焰与火焰披风_v3/（overdrive-v3.aseprite，以及 cape、rear-fire、body-fire、hand-fire、embers 各自的 aseprite 与 strip.png；"
    "overdrive-v3-entry.aseprite、overdrive-v3-entry-strip.png）。正背预览、合成条和过程归档里的空 combined 源不加载。\n"
    "接入：textures/effect/furnace_v2/burn_cape.png、burn_rear_fire.png、burn_body_fire.png、burn_hand_fire.png、burn_embers.png、"
    "burn_entry_cape.png、burn_entry_rear_fire.png、burn_entry_body_fire.png、burn_entry_hand_fire.png、burn_entry_embers.png。"
)
NEW_O = {"fire_heat_up": HEAT_O, "fire_burn": BURN_O}


def fail(message):
    raise SystemExit(message)


def sha256_bytes(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def local(tag):
    return tag.rsplit("}", 1)[-1]


def col_num(column):
    value = 0
    for char in column:
        value = value * 26 + ord(char) - 64
    return value


def col_name(index):
    name = ""
    while index:
        index, rem = divmod(index - 1, 26)
        name = chr(65 + rem) + name
    return name


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_span(row, column, number):
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


def shared_list(xml):
    if not xml:
        return []
    root = ET.fromstring(xml)
    found = []
    for item in root:
        if local(item.tag) != "si":
            continue
        texts = []
        for node in item.iter():
            if local(node.tag) == "t" and node.text:
                texts.append(html.unescape(node.text))
        found.append("".join(texts))
    return found


def cell_text(cell, shared):
    if cell is None:
        return None
    inline = re.search(r"<(?:[\w]+:)?is>([\s\S]*)</(?:[\w]+:)?is>", cell)
    if inline:
        texts = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", inline.group(1))
        return html.unescape("".join(texts))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if not value:
        return None
    raw = html.unescape(value.group(1))
    if re.search(r'\bt="s"', cell) and raw.isdigit():
        return shared[int(raw)]
    return raw


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


def read_at(row, column, number, shared):
    found = cell_span(row, column, number)
    return cell_text(found[0], shared) if found else None


def style_of(cell):
    match = re.search(r'\ss="(\d+)"', cell or "")
    return match.group(1) if match else None


def text_cell(column, number, text, style):
    escaped = html.escape(text, quote=False)
    space = ' xml:space="preserve"' if ("\n" in text or text[:1].isspace() or text[-1:].isspace()) else ""
    style_attr = f' s="{style}"' if style else ""
    return f'<c r="{column}{number}" t="inlineStr"{style_attr}><is><t{space}>{escaped}</t></is></c>'


def set_text(row, column, number, text, shared):
    found = cell_span(row, column, number)
    if not found:
        fail(f"Missing {column}{number}")
    payload = text_cell(column, number, text, style_of(found[0]))
    _cell, start, end = found
    return row[:start] + payload + row[end:]


def ids_of(xml, shared):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        span = cell_span(row, "B", number)
        code = cell_text(span[0], shared) if span else None
        if code:
            found.append((code, number, row))
    return found


def replace_data(xml, new_rows):
    match = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", xml)
    if not match:
        fail("sheetData missing")
    return xml[:match.start(2)] + "".join(new_rows) + xml[match.end(2):]


def main():
    live_bytes = LIVE.read_bytes()
    baseline = sha256_bytes(live_bytes)
    stat = LIVE.stat()
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup_dir = BATCH / "备份"
    backup_dir.mkdir(exist_ok=True)
    sequence = 1
    while (backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx").exists():
        sequence += 1
    backup = backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx"
    candidate_dir = BATCH / "候选"
    candidate_dir.mkdir(exist_ok=True)
    candidate = candidate_dir / f"术式制作表-候选-{stamp}-{sequence}.xlsx"
    snap_dir = BATCH / "快照"
    snap_dir.mkdir(exist_ok=True)
    backup.write_bytes(live_bytes)
    if sha256_bytes(backup.read_bytes()) != baseline:
        fail("备份哈希与源表不一致")

    with zipfile.ZipFile(backup) as archive:
        order = archive.infolist()
        parts = {info.filename: archive.read(info.filename) for info in order}
    shared = shared_list(parts.get("xl/sharedStrings.xml"))
    names = sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
    for title in STATUS + ("数据表", "资源状态明细", "待制作原始数据"):
        if title not in names:
            fail(f"缺少工作表 {title}")
    making_name = names["制作中"]
    making_xml = parts[making_name].decode("utf-8")
    located = {}
    counts = {}
    all_ids = []
    for title in STATUS:
        page = ids_of(parts[names[title]].decode("utf-8"), shared)
        counts[title] = len(page)
        for code, number, row in page:
            all_ids.append((title, code, number))
            if code in TARGETS or code == "fire_cool":
                located.setdefault(code, []).append((title, number, row))
    if len(all_ids) != len({code for _title, code, _number in all_ids}):
        fail("四张状态页编号不唯一")
    if counts["待制作"] != 0:
        fail("待制作不是空队列，停止以免误领")
    for code in TARGETS:
        hits = located.get(code, [])
        if len(hits) != 1 or hits[0][0] != "制作中":
            fail(f"{code} 不在制作中的唯一行")
    cool_hits = located.get("fire_cool", [])
    if len(cool_hits) != 1 or cool_hits[0][0] != "制作中":
        fail("fire_cool 位置异常，停止")

    archive_xml = parts[names["待制作原始数据"]]
    archive_ids = [code for code, _number, _row in ids_of(archive_xml.decode("utf-8"), shared)]
    for code in TARGETS:
        if archive_ids.count(code) != 1:
            fail(f"待制作原始数据里 {code} 不是唯一首次快照，停止以免覆盖")
    detail_xml = parts[names["资源状态明细"]]
    detail_text = detail_xml.decode("utf-8")
    for code in TARGETS:
        if code in detail_text:
            fail(f"资源状态明细出现了 {code}，与主表无附加明细不一致，停止")

    header = {}
    for row in rows_of(making_xml):
        if row_number(row) == 2:
            for index in range(1, 145):
                column = col_name(index)
                header[column] = read_at(row, column, 2, shared)
            break
    if header.get("O") != "特效" or header.get("DV") != "备注" or header.get("N") != "功能描述":
        fail(f"表头不是 N/O/DV：{header.get('N')}/{header.get('O')}/{header.get('DV')}")

    snapshots = {}
    edited = {}
    originals = {}
    for code in TARGETS:
        _title, number, row = located[code][0]
        original_n = read_at(row, "N", number, shared)
        original_o = read_at(row, "O", number, shared)
        original_dv = read_at(row, "DV", number, shared)
        if not original_n or not original_o or not original_dv:
            fail(f"{code} 缺少 N/O/DV")
        if APPEND in original_dv:
            fail(f"{code} 已经写过本次进度")
        if "这里只记录制作意图" not in original_o:
            fail(f"{code} 的现行特效不是待替换的制作意图，停止")
        cells = []
        for index in range(1, 145):
            column = col_name(index)
            cells.append({
                "column": column,
                "header": header.get(column),
                "text": read_at(row, column, number, shared),
            })
        if len(cells) != 144:
            fail("A:EN 不是 144 列")
        snapshots[code] = {
            "sheet": "制作中",
            "row": number,
            "columns": cells,
        }
        originals[code] = {"N": original_n, "O": original_o, "DV": original_dv, "row": number}
        updated = set_text(row, "O", number, NEW_O[code], shared)
        updated = set_text(updated, "DV", number, original_dv + "\n" + APPEND, shared)
        if read_at(updated, "N", number, shared) != original_n:
            fail(f"{code} 的 N 被改动")
        if not updated.startswith(row[:row.find("<c ")]):
            fail(f"{code} 行高或行属性被改动")
        for index in range(1, 145):
            column = col_name(index)
            if column in ("O", "DV"):
                continue
            if cell_span(row, column, number) is None and cell_span(updated, column, number) is None:
                continue
            old_cell = cell_span(row, column, number)
            new_cell = cell_span(updated, column, number)
            if old_cell is None or new_cell is None or old_cell[0] != new_cell[0]:
                fail(f"{code} 的 {column} 被改动")
        if read_at(updated, "O", number, shared) != NEW_O[code]:
            fail(f"{code} 的 O 回读不一致")
        if read_at(updated, "DV", number, shared) != original_dv + "\n" + APPEND:
            fail(f"{code} 的 DV 回读不一致")
        if style_of(cell_span(updated, "O", number)[0]) != "16" or style_of(cell_span(updated, "DV", number)[0]) != "16":
            fail(f"{code} 样式丢失")
        edited[number] = updated

    new_rows = []
    for row in rows_of(making_xml):
        number = row_number(row)
        new_rows.append(edited.get(number, row))
    making_out = replace_data(making_xml, new_rows)
    ET.fromstring(making_out.encode("utf-8"))
    changed = {making_name: making_out.encode("utf-8")}
    with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

    with zipfile.ZipFile(candidate) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if different != [making_name]:
            fail("候选改到了制作中以外的部件: " + ",".join(different))
        if dst.read(names["待制作原始数据"]) != archive_xml:
            fail("档案页被改动")
        if dst.read(names["资源状态明细"]) != detail_xml:
            fail("资源状态明细被改动")
        for title in ("已完成", "待制作", "待验收", "数据表"):
            if dst.read(names[title]) != parts[names[title]]:
                fail(f"{title} 被改动")
        check_xml = dst.read(making_name).decode("utf-8")
        ET.fromstring(check_xml.encode("utf-8"))
        check_ids = ids_of(check_xml, shared)
        if [code for code, _number, _row in check_ids] != [code for code, _number, _row in ids_of(making_xml, shared)]:
            fail("制作中编号或顺序变化")
        cool_number = cool_hits[0][1]
        cool_row = [row for row in rows_of(check_xml) if row_number(row) == cool_number][0]
        if cool_row != cool_hits[0][2]:
            fail("fire_cool 行被改动")
        for code in TARGETS:
            number = originals[code]["row"]
            written = [row for row in rows_of(check_xml) if row_number(row) == number][0]
            if read_at(written, "N", number, shared) != originals[code]["N"]:
                fail(f"{code} 提交前 N 不一致")
            if read_at(written, "B", number, shared) != code:
                fail(f"{code} 编号变化")
            if originals[code]["DV"] not in read_at(written, "DV", number, shared):
                fail(f"{code} 原备注丢失")
            if not read_at(written, "DV", number, shared).endswith(APPEND):
                fail(f"{code} 进度没有写在备注末尾")
        outside_before = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml)
        outside_after = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_xml)
        if outside_before != outside_after:
            fail("筛选、数据验证或表维度被改动")
        after_counts = {}
        after_ids = []
        for title in STATUS:
            page = [code for code, _number, _row in ids_of(dst.read(names[title]).decode("utf-8"), shared)]
            after_counts[title] = len(page)
            after_ids.extend(page)
        if after_counts != counts or len(after_ids) != len(set(after_ids)):
            fail(f"四页计数变化 {counts} -> {after_counts}")

    nodv_path = snap_dir / f"变更前-NODV-{stamp}-{sequence}.json"
    aen_path = snap_dir / f"变更前-AEN-{stamp}-{sequence}.json"
    visual_path = snap_dir / f"用户批准新视觉-{stamp}-{sequence}.md"
    nodv_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "sourceMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "items": originals,
        "note": "这是写回前制作中原文。待制作原始数据未用这些现行行覆盖。",
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    aen_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "columnCount": 144,
        "items": snapshots,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    visual_path.write_text(
        "# 用户批准的升温 v2 与燃烧 v3\n\n"
        "写回前留存。用户已采用升温贴身炽热 v2 与燃烧双手附焰与火焰披风 v3。此处只记批准视觉、正式源和实际接入路径，不代替 N 的完整游戏逻辑。\n\n"
        "## fire_heat_up\n\n" + HEAT_O + "\n\n## fire_burn\n\n" + BURN_O + "\n",
        encoding="utf-8",
    )
    readme_bytes = README.read_bytes()
    result = {
        "ready": True,
        "submitted": False,
        "baselineSha256": baseline,
        "baselineBytes": len(live_bytes),
        "baselineMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "backup": str(backup),
        "backupSha256": sha256_bytes(backup.read_bytes()),
        "candidate": str(candidate),
        "candidateSha256": sha256_bytes(candidate.read_bytes()),
        "snapshots": {
            "aen": str(aen_path),
            "nodv": str(nodv_path),
            "approvedVisual": str(visual_path),
        },
        "rows": {code: originals[code]["row"] for code in TARGETS},
        "counts": counts,
        "archiveUntouched": True,
        "detailUntouched": True,
        "changedPart": making_name,
        "readmeSha256": sha256_bytes(readme_bytes),
        "readmeBytes": len(readme_bytes),
    }
    (BATCH / "sheet-prepare-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"baseline": baseline, "candidate": result["candidateSha256"], "rows": result["rows"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
