# -*- coding: utf-8 -*-
"""Move fire_burn to 待验收 in a candidate workbook. Does not replace the live sheet."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(r"D:\alianximod")
BATCH = ROOT / "docs" / "开发记录" / "燃烧空间挂载接入-20261101"
LIVE = ROOT / "docs" / "术式制作表.xlsx"
README = ROOT / "docs" / "资源库" / "README.md"
TARGET = "fire_burn"
NEW_ROW = 7
NEW_N = "进入或升级超频炉心，并保留已有加成"
NEW_O = (
    "燃烧_空间挂载_v1 / burn-v3-spatial-v1\n"
    "正式源：docs/资源库/特效/火焰/燃烧_空间挂载_v1/\n"
    "运行：models/effect/burn_spatial_v1/mount.json\n"
    "运行贴图：textures/effect/burn_spatial_v1/loop 与 entry 的 cape.png、rear.png、hand.png、"
    "shoulder_right.png、shoulder_left.png、abdomen_right.png、abdomen_left.png、hip_left.png、"
    "shin_right.png、shin_left.png、hand_embers_right.png、hand_embers_left.png、body_embers.png"
)
DV_APPEND = (
    "2026-11-01 已按确认模型 burn-v3-spatial-v1 接入。"
    ".\\gradlew.bat compileJava --offline --no-daemon 于 2026-11-01 21:37:58–21:38:35 退出码 0，BUILD SUCCESSFUL。"
    "游戏待用户实测。Codex未复审。\n"
    "待验收 | 燃烧空间挂载接入-20261101 | docs/开发记录/燃烧空间挂载接入-20261101/调度记录.md"
)
STATUS = ("已完成", "待制作", "制作中", "待验收")
PARTS = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
    "数据表": "xl/worksheets/sheet5.xml",
    "资源状态明细": "xl/worksheets/sheet6.xml",
    "待制作原始数据": "xl/worksheets/sheet7.xml",
}


def fail(message):
    raise SystemExit(message)


def sha256_bytes(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def local(tag):
    return tag.rsplit("}", 1)[-1]


def col_name(index):
    name = ""
    while index:
        index, rem = divmod(index - 1, 26)
        name = chr(65 + rem) + name
    return name


def rows_of(xml):
    return re.findall(r"<row\b[^>]*>[\s\S]*?</row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def shared_list(xml):
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


def cell_span(row, column, number):
    matches = list(re.finditer(rf'<c r="{column}{number}"', row))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row.find(">", start)
    if row[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</c>", row[opener_end:])
        if not close:
            fail(f"Cell {column}{number} is not closed")
        end = opener_end + close.end()
    return row[start:end], start, end


def cell_text(cell, shared):
    if cell is None:
        return None
    if 't="inlineStr"' in cell or "<is>" in cell:
        texts = re.findall(r"<t[^>]*>([\s\S]*?)</t>", cell)
        return html.unescape("".join(texts))
    value = re.search(r"<v>([\s\S]*?)</v>", cell)
    if not value:
        return None
    raw = html.unescape(value.group(1))
    if 't="s"' in cell and raw.isdigit():
        return shared[int(raw)]
    return raw


def read_at(row, column, number, shared):
    found = cell_span(row, column, number)
    return cell_text(found[0], shared) if found else None


def ids_of(xml, shared):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        code = read_at(row, "B", number, shared)
        if code:
            found.append((code, number, row))
    return found


def text_cell(column, number, text, style):
    escaped = html.escape(text, quote=False)
    space = ' xml:space="preserve"' if ("\n" in text or text[:1].isspace() or text[-1:].isspace()) else ""
    style_attr = f' s="{style}"' if style else ""
    return f'<c r="{column}{number}"{style_attr} t="inlineStr"><is><t{space}>{escaped}</t></is></c>'


def style_of(cell):
    match = re.search(r'\ss="(\d+)"', cell or "")
    return match.group(1) if match else None


def replace_text_cell(row, column, number, text):
    found = cell_span(row, column, number)
    if not found:
        fail(f"Missing {column}{number}")
    payload = text_cell(column, number, text, style_of(found[0]))
    _cell, start, end = found
    return row[:start] + payload + row[end:]


def renumber_row(row, old, new):
    updated, count = re.subn(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row, count=1)
    if count != 1 or row_number(updated) != new:
        fail(f"Row {old} did not renumber")
    updated = re.sub(rf'\br="([A-Z]+){old}"', lambda match: f'r="{match.group(1)}{new}"', updated)
    if re.search(rf'\br="[A-Z]+{old}"', updated):
        fail(f"Row {old} still has old cell refs")
    return updated


def replace_once(xml, old, new, label):
    count = xml.count(old)
    if count != 1:
        fail(f"{label} appeared {count} times")
    return xml.replace(old, new, 1)


def replace_data(xml, new_rows):
    match = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", xml)
    if not match:
        fail("sheetData missing")
    return xml[:match.start(2)] + "".join(new_rows) + xml[match.end(2):]


def column_values(row, number, shared):
    found = {}
    for index in range(1, 145):
        column = col_name(index)
        found[column] = read_at(row, column, number, shared)
    return found


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

    with zipfile.ZipFile(backup) as archive:
        order = archive.infolist()
        parts = {info.filename: archive.read(info.filename) for info in order}
    shared = shared_list(parts["xl/sharedStrings.xml"])
    making_xml = parts[PARTS["制作中"]].decode("utf-8")
    pending_xml = parts[PARTS["待验收"]].decode("utf-8")
    counts = {}
    all_ids = []
    located = {}
    for title in STATUS:
        page = ids_of(parts[PARTS[title]].decode("utf-8"), shared)
        counts[title] = len(page)
        for code, number, row in page:
            all_ids.append(code)
            located.setdefault(code, []).append((title, number, row))
    if len(all_ids) != len(set(all_ids)) or counts != {"已完成": 40, "待制作": 0, "制作中": 16, "待验收": 4}:
        fail(f"状态页计数异常 {counts}")
    hits = located.get(TARGET, [])
    if len(hits) != 1 or hits[0][0] != "制作中" or hits[0][1] != 4:
        fail(f"fire_burn 不在制作中第4行: {[(item[0], item[1]) for item in hits]}")
    if located["fire_heat_up"][0][1] != 3 or located["fire_cool"][0][1] != 5:
        fail("升温或冷却位置已变，停止")
    source_row = hits[0][2]
    original = column_values(source_row, 4, shared)
    if not original["N"] or not original["O"] or not original["DV"]:
        fail("缺少 N/O/DV")
    if "burn-v3-spatial-v1 接入" in original["DV"]:
        fail("本次进度已经写过")
    if original["N"][:2] != "燃烧":
        fail("功能描述不是燃烧原文")
    header = {}
    for row in rows_of(making_xml):
        if row_number(row) == 2:
            header = column_values(row, 2, shared)
            break
    if header.get("N") != "功能描述" or header.get("O") != "特效" or header.get("DV") != "备注":
        fail("表头变化")
    archive_ids = [code for code, _number, _row in ids_of(parts[PARTS["待制作原始数据"]].decode("utf-8"), shared)]
    if archive_ids.count(TARGET) != 1:
        fail("待制作原始数据里的首次快照不是一条，停止")
    detail_ids = [code for code, _number, _row in ids_of(parts[PARTS["资源状态明细"]].decode("utf-8"), shared)]
    detail_text = parts[PARTS["资源状态明细"]].decode("utf-8")
    detail_has_target = any(shared[int(index)] == TARGET for index in re.findall(r"<v>(\d+)</v>", detail_text) if int(index) < len(shared))
    if detail_has_target or TARGET in detail_ids:
        fail("资源状态明细已有 fire_burn，停止以免改成第二条配置")

    moved = renumber_row(source_row, 4, NEW_ROW)
    moved = replace_text_cell(moved, "N", NEW_ROW, NEW_N)
    moved = replace_text_cell(moved, "O", NEW_ROW, NEW_O)
    moved = replace_text_cell(moved, "DV", NEW_ROW, original["DV"] + "\n" + DV_APPEND)
    if not moved.startswith(source_row[:source_row.find("<c ")].replace('r="4"', f'r="{NEW_ROW}"', 1)):
        fail("移动行的行高或行属性丢失")
    for index in range(1, 145):
        column = col_name(index)
        if column in ("N", "O", "DV"):
            continue
        if read_at(moved, column, NEW_ROW, shared) != original[column]:
            fail(f"移动时改到了 {column}")
    if read_at(moved, "N", NEW_ROW, shared) != NEW_N or read_at(moved, "O", NEW_ROW, shared) != NEW_O:
        fail("待验收展示没有写上")
    if not read_at(moved, "DV", NEW_ROW, shared).startswith(original["DV"] + "\n"):
        fail("原备注丢失")

    making_rows = []
    for row in rows_of(making_xml):
        number = row_number(row)
        if number == 4:
            continue
        if number > 4:
            making_rows.append(renumber_row(row, number, number - 1))
        else:
            making_rows.append(row)
    making_out = replace_data(making_xml, making_rows)
    making_out = replace_once(making_out, 'ref="A1:EN18"', 'ref="A1:EN17"', "制作中维度")
    making_out = replace_once(making_out, 'ref="A2:EN8"', 'ref="A2:EN7"', "制作中筛选")
    pending_rows = rows_of(pending_xml) + [moved]
    pending_out = replace_data(pending_xml, pending_rows)
    pending_out = replace_once(pending_out, 'ref="A1:EN6"', 'ref="A1:EN7"', "待验收维度")
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    workbook = replace_once(workbook, "制作中!$A$2:$EN$18", "制作中!$A$2:$EN$17", "制作中筛选名称")
    ET.fromstring(making_out.encode("utf-8"))
    ET.fromstring(pending_out.encode("utf-8"))
    ET.fromstring(workbook.encode("utf-8"))

    changed = {
        PARTS["制作中"]: making_out.encode("utf-8"),
        PARTS["待验收"]: pending_out.encode("utf-8"),
        "xl/workbook.xml": workbook.encode("utf-8"),
    }
    with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

    with zipfile.ZipFile(candidate) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if set(different) != set(changed):
            fail("候选改到了意外部件: " + ",".join(different))
        for title in ("已完成", "待制作", "数据表", "资源状态明细", "待制作原始数据"):
            if dst.read(PARTS[title]) != parts[PARTS[title]]:
                fail(f"{title} 被改动")
        if dst.read("xl/sharedStrings.xml") != parts["xl/sharedStrings.xml"]:
            fail("共享字符串被改动")
        check_making = dst.read(PARTS["制作中"]).decode("utf-8")
        check_pending = dst.read(PARTS["待验收"]).decode("utf-8")
        making_ids = ids_of(check_making, shared)
        pending_ids = ids_of(check_pending, shared)
        if [code for code, _number, _row in making_ids] != [code for code, number, _row in located_ids(making_xml, shared) if code != TARGET]:
            fail("制作中剩余编号或顺序不对")
        if [code for code, _number, _row in pending_ids][-1] != TARGET or len(pending_ids) != 5:
            fail("待验收没有把燃烧放在末行")
        heat = [row for _code, number, row in making_ids if number == 3][0]
        old_heat = located["fire_heat_up"][0][2]
        if heat != old_heat:
            fail("升温行被改动")
        cool = [row for code, _number, row in making_ids if code == "fire_cool"][0]
        if column_values(cool, 4, shared) != column_values(located["fire_cool"][0][2], 5, shared):
            fail("冷却内容被改动")
        written = [row for code, _number, row in pending_ids if code == TARGET][0]
        if read_at(written, "B", NEW_ROW, shared) != TARGET:
            fail("待验收编号不对")
        outside_making = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml)
        outside_check = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_making)
        outside_making = outside_making.replace('ref="A1:EN18"', 'ref="A1:EN17"').replace('ref="A2:EN8"', 'ref="A2:EN7"')
        if outside_making != outside_check:
            fail("制作中筛选、验证或合并区被额外改动")
        outside_pending = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_xml).replace('ref="A1:EN6"', 'ref="A1:EN7"')
        outside_pending_check = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_pending)
        if outside_pending != outside_pending_check:
            fail("待验收验证或筛选被额外改动")
        after_counts = {}
        after_ids = []
        for title, part in (("已完成", PARTS["已完成"]), ("待制作", PARTS["待制作"]), ("制作中", PARTS["制作中"]), ("待验收", PARTS["待验收"])):
            page = [code for code, _number, _row in ids_of(dst.read(part).decode("utf-8"), shared)]
            after_counts[title] = len(page)
            after_ids.extend(page)
        if after_counts != {"已完成": 40, "待制作": 0, "制作中": 15, "待验收": 5} or len(after_ids) != len(set(after_ids)):
            fail(f"迁移后计数异常 {after_counts}")

    cells = []
    for index in range(1, 145):
        column = col_name(index)
        cells.append({"column": column, "header": header.get(column), "text": original[column]})
    aen_path = snap_dir / f"变更前-AEN-{stamp}-{sequence}.json"
    nodv_path = snap_dir / f"变更前-NODV-{stamp}-{sequence}.json"
    visual_path = snap_dir / f"批准要求与资源映射-{stamp}-{sequence}.md"
    aen_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "sheet": "制作中",
        "row": 4,
        "columnCount": 144,
        "cells": cells,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    nodv_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "id": TARGET,
        "from": {"sheet": "制作中", "row": 4},
        "N": original["N"],
        "O": original["O"],
        "DV": original["DV"],
        "pendingDisplay": {"N": NEW_N, "O": NEW_O, "dvAppend": DV_APPEND},
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    visual_path.write_text(
        "# fire_burn 批准要求与资源映射\n\n"
        "用户于 2026-11-01 看过真实 Blockbench 正背动画后确认：「可以就这版接入」。"
        "版本 burn-v3-spatial-v1。整团手焰与拳头火星随手臂转动。\n\n"
        "正式目录：docs/资源库/特效/火焰/燃烧_空间挂载_v1/\n\n"
        "游戏内一句：进入或升级超频炉心，并保留已有加成。\n\n"
        + NEW_O + "\n\n"
        "compileJava --offline --no-daemon 于 2026-11-01 21:37:58–21:38:35 退出码 0。"
        "游戏尚未由用户实测。Codex 未复审。旧的完整 N/O/DV 留在同目录变更前快照，不由待验收短句替代。\n",
        encoding="utf-8",
    )
    result = {
        "ready": True,
        "submitted": False,
        "baselineSha256": baseline,
        "baselineBytes": len(live_bytes),
        "baselineMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "backup": str(backup),
        "candidate": str(candidate),
        "candidateSha256": sha256_bytes(candidate.read_bytes()),
        "snapshots": {"aen": str(aen_path), "nodv": str(nodv_path), "mapping": str(visual_path)},
        "from": {"sheet": "制作中", "row": 4},
        "to": {"sheet": "待验收", "row": NEW_ROW},
        "countsBefore": counts,
        "countsAfter": {"已完成": 40, "待制作": 0, "制作中": 15, "待验收": 5},
        "live": str(LIVE),
        "readme": str(README),
        "readmeSha256": sha256_bytes(README.read_bytes()),
    }
    (BATCH / "sheet-prepare-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"baseline": baseline, "candidate": result["candidateSha256"]}, ensure_ascii=False))


def located_ids(xml, shared):
    return ids_of(xml, shared)


if __name__ == "__main__":
    main()
