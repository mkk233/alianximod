# -*- coding: utf-8 -*-
"""Rename two rows and move them from 制作中 to 待验收."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(r"D:\alianximod")
WORK = ROOT / "docs" / "开发记录" / "让锋断潮名称整合-20261103" / "表格工作"
LIVE = ROOT / "docs" / "术式制作表.xlsx"
TIDE = "tide_sever"
RIBBON = "water_yielding_ribbon"
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
EXPECTED = {
    TIDE: {
        "sheet": "制作中",
        "row": 16,
        "A": "断潮",
        "newA": "薄水切",
        "N": "一道薄而长的水切",
        "O": "TideSeverEffect",
    },
    RIBBON: {
        "sheet": "制作中",
        "row": 17,
        "A": "让锋",
        "newA": "断潮",
        "N": "先铺开水面，再从中间劈开。水分开没有伤害，只有中间那一刀",
        "O": "WaterYieldingEffect",
    },
}
APPEND = {
    TIDE: (
        "2026-11-03 用户最新选择取代上面的停用计划，该计划不再执行：只改名，显示名由断潮改为薄水切，本行仍可用。"
        "稳定编号 tide_sever、效果 TideSeverEffect、数值、配置键和存档不迁移。水幻形仍适配 tide_sever（薄水切），不适配重斩。"
        "旧自定义组合标题可能保留原名，重新装配后的标题按新名称。"
        "最终 .\\gradlew.bat compileJava --offline --no-daemon 于 2026-11-03T15:39:43.983+08:00 至 2026-11-03T15:40:12.387+08:00 实际执行，退出码 0，BUILD SUCCESSFUL。"
        "未游戏验证，未新增正式复审。\n"
        "待验收 | 让锋断潮名称整合-20261103 | docs/开发记录/让锋断潮名称整合-20261103/记录.md"
    ),
    RIBBON: (
        "2026-11-03 用户最新选择取代上面的暂不使用安排，该计划不再执行：只改名，显示名由让锋改为断潮，本行仍可用。"
        "稳定编号 water_yielding_ribbon、效果 WaterYieldingEffect、数值、配置键和存档不迁移。水幻形仍适配薄水切 tide_sever，不改为适配本行。"
        "旧自定义组合标题可能保留原名，重新装配后的标题按新名称。"
        "最终 .\\gradlew.bat compileJava --offline --no-daemon 于 2026-11-03T15:39:43.983+08:00 至 2026-11-03T15:40:12.387+08:00 实际执行，退出码 0，BUILD SUCCESSFUL。"
        "未游戏验证，未新增正式复审。\n"
        "待验收 | 让锋断潮名称整合-20261103 | docs/开发记录/让锋断潮名称整合-20261103/记录.md"
    ),
}
TARGETS = {TIDE: 10, RIBBON: 11}


def fail(message):
    raise SystemExit(message)


def sha256_bytes(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def sha256_file(path):
    return sha256_bytes(Path(path).read_bytes())


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
    if old == new:
        return row
    updated, count = re.subn(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row, count=1)
    if count != 1:
        fail(f"Row {old} did not renumber")
    updated = re.sub(
        rf'\br="([A-Z]+){old}(?!\d)"',
        lambda match: f'r="{match.group(1)}{new}"',
        updated,
    )
    if row_number(updated) != new or re.search(rf'\br="[A-Z]+{old}(?!\d)"', updated):
        fail(f"Row {old} still has old refs")
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
    return xml[: match.start(2)] + "".join(new_rows) + xml[match.end(2) :]


def column_values(row, number, shared):
    return {col_name(index): read_at(row, col_name(index), number, shared) for index in range(1, 145)}


def main():
    live_bytes = LIVE.read_bytes()
    baseline = sha256_bytes(live_bytes)
    stat = LIVE.stat()
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    sequence = 1
    backup_dir = WORK / "备份"
    candidate_dir = WORK / "候选"
    snap_dir = WORK / "快照"
    for folder in (backup_dir, candidate_dir, snap_dir):
        folder.mkdir(exist_ok=True)
    while (backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx").exists():
        sequence += 1
    backup = backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx"
    candidate = candidate_dir / f"术式制作表-候选-{stamp}-{sequence}.xlsx"
    backup.write_bytes(live_bytes)

    with zipfile.ZipFile(backup) as archive:
        order = archive.infolist()
        parts = {info.filename: archive.read(info.filename) for info in order}
    shared = shared_list(parts["xl/sharedStrings.xml"])
    making_xml = parts[PARTS["制作中"]].decode("utf-8")
    pending_xml = parts[PARTS["待验收"]].decode("utf-8")
    counts = {}
    located = {}
    all_ids = []
    for title in STATUS:
        page = ids_of(parts[PARTS[title]].decode("utf-8"), shared)
        counts[title] = len(page)
        for code, number, row in page:
            all_ids.append(code)
            located.setdefault(code, []).append((title, number, row))
    if len(all_ids) != len(set(all_ids)) or counts != {"已完成": 38, "待制作": 0, "制作中": 15, "待验收": 7}:
        fail(f"四页计数或唯一性异常 {counts}")
    making_ids = ids_of(making_xml, shared)
    pending_ids = ids_of(pending_xml, shared)
    if [number for _code, number, _row in making_ids] != list(range(3, 18)):
        fail("制作中行号不连续")
    if [number for _code, number, _row in pending_ids] != list(range(3, 10)):
        fail("待验收行号不连续")
    header = {}
    for row in rows_of(making_xml):
        if row_number(row) == 2:
            header = column_values(row, 2, shared)
            break
    if header.get("A") != "名称" or header.get("B") != "编号" or header.get("N") != "功能描述" or header.get("O") != "特效" or header.get("DV") != "备注":
        fail("表头变化")
    sources = {}
    originals = {}
    for code, expect in EXPECTED.items():
        hits = located.get(code, [])
        if len(hits) != 1 or hits[0][0] != expect["sheet"] or hits[0][1] != expect["row"]:
            fail(f"{code} 位置异常: {[(item[0], item[1]) for item in hits]}")
        original = column_values(hits[0][2], expect["row"], shared)
        for field in ("A", "N", "O"):
            if original[field] != expect[field]:
                fail(f"{code} 的 {field} 已变")
        note = original.get("DV") or ""
        if not note.startswith("2026-11-03 用户原话："):
            fail(f"{code} 历史备注丢失")
        if "用户最新选择取代" in note:
            fail(f"{code} 已经写过本次选择")
        sources[code] = hits[0]
        originals[code] = original

    moved = {}
    for code, target in TARGETS.items():
        _sheet, number, row = sources[code]
        updated = renumber_row(row, number, target)
        updated = replace_text_cell(updated, "A", target, EXPECTED[code]["newA"])
        updated = replace_text_cell(updated, "DV", target, originals[code]["DV"] + "\n" + APPEND[code])
        for index in range(1, 145):
            column = col_name(index)
            if column in ("A", "DV"):
                continue
            if read_at(updated, column, target, shared) != originals[code][column]:
                fail(f"{code} 改到了 {column}")
        if read_at(updated, "B", target, shared) != code:
            fail(f"{code} 编号变化")
        if read_at(updated, "N", target, shared) != EXPECTED[code]["N"] or read_at(updated, "O", target, shared) != EXPECTED[code]["O"]:
            fail(f"{code} 功能或资源标识变化")
        if not read_at(updated, "DV", target, shared).startswith(originals[code]["DV"] + "\n"):
            fail(f"{code} 原备注丢失")
        if "停用标签" in APPEND[code] or "DEVELOPMENT_ONLY" in APPEND[code]:
            fail("新备注写入了停用标签")
        moved[code] = updated

    making_rows = [row for row in rows_of(making_xml) if row_number(row) not in (16, 17)]
    if [row_number(row) for row in making_rows] != list(range(1, 16)):
        fail("制作中删除后行号不连续")
    making_out = replace_data(making_xml, making_rows)
    making_out = replace_once(making_out, 'ref="A1:EN17"', 'ref="A1:EN15"', "制作中维度")
    pending_out = replace_data(pending_xml, rows_of(pending_xml) + [moved[TIDE], moved[RIBBON]])
    pending_out = replace_once(pending_out, 'ref="A1:EN9"', 'ref="A1:EN11"', "待验收维度")
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    workbook = replace_once(workbook, "制作中!$A$2:$EN$17", "制作中!$A$2:$EN$15", "制作中数据范围")
    if 'ref="A2:EN5"' not in making_out or 'ref="A2:EN2"' not in pending_out:
        fail("原筛选范围丢失")
    if "待验收!$A$2:$EN$2" not in workbook or "已完成!$A$2:$EN$40" not in workbook:
        fail("其它页数据范围被改")
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
            fail("共享字符串被改")
        check_making = dst.read(PARTS["制作中"]).decode("utf-8")
        check_pending = dst.read(PARTS["待验收"]).decode("utf-8")
        if [row for row in rows_of(check_making)] != making_rows:
            fail("制作中保留行发生变化")
        if rows_of(check_pending)[:-2] != rows_of(pending_xml):
            fail("待验收原有行发生变化")
        new_making = [code for code, _number, _row in ids_of(check_making, shared)]
        new_pending = [code for code, _number, _row in ids_of(check_pending, shared)]
        if new_making != [code for code, _number, _row in making_ids][:-2]:
            fail("制作中原有顺序被改动")
        if new_pending[:-2] != [code for code, _number, _row in pending_ids] or new_pending[-2:] != [TIDE, RIBBON]:
            fail("待验收末两行不对")
        outside_making = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml).replace('ref="A1:EN17"', 'ref="A1:EN15"')
        if outside_making != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_making):
            fail("制作中筛选或验证被额外改动")
        outside_pending = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_xml).replace('ref="A1:EN9"', 'ref="A1:EN11"')
        if outside_pending != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_pending):
            fail("待验收筛选或验证被额外改动")
        seen = []
        after = {"已完成": 38, "待制作": 0, "制作中": 13, "待验收": 9}
        for title in STATUS:
            page = [code for code, _number, _row in ids_of(dst.read(PARTS[title]).decode("utf-8"), shared)]
            if len(page) != after[title]:
                fail(f"{title} 计数 {len(page)}")
            seen.extend(page)
        if len(seen) != len(set(seen)) or len(seen) != 60:
            fail("四页总数或唯一性变化")
        if sha256_file(LIVE) != baseline:
            fail("准备期间源表已变化")

    cells = {
        code: [
            {"column": col_name(index), "header": header.get(col_name(index)), "text": originals[code][col_name(index)]}
            for index in range(1, 145)
        ]
        for code in (TIDE, RIBBON)
    }
    aen_path = snap_dir / f"变更前-AEN-{stamp}-{sequence}.json"
    current_path = snap_dir / f"待验收前需求-{stamp}-{sequence}.md"
    aen_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "columnCount": 144,
        "rows": {
            code: {"sheet": "制作中", "row": EXPECTED[code]["row"], "cells": cells[code]}
            for code in (TIDE, RIBBON)
        },
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    current_path.write_text(requirement_text(originals), encoding="utf-8")
    result = {
        "ready": True,
        "baselineSha256": baseline,
        "baselineMtime": datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="milliseconds"),
        "baselineSize": stat.st_size,
        "backup": str(backup),
        "candidate": str(candidate),
        "candidateSha256": sha256_file(candidate),
        "snapshots": {"aen": str(aen_path), "current": str(current_path)},
        "moves": {
            code: {
                "from": {"sheet": "制作中", "row": EXPECTED[code]["row"], "name": EXPECTED[code]["A"]},
                "to": {"sheet": "待验收", "row": TARGETS[code], "name": EXPECTED[code]["newA"]},
            }
            for code in (TIDE, RIBBON)
        },
        "countsBefore": counts,
        "countsAfter": after,
        "live": str(LIVE),
        "affectedParts": list(changed),
    }
    (WORK / "prepare-final.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"baseline": baseline, "candidate": result["candidateSha256"], "moves": result["moves"]}, ensure_ascii=False))


def requirement_text(originals):
    return (
        "# 让锋与断潮：待验收前的名称与需求\n\n"
        "修改前两行都在制作中。本文件保存当时完整功能句、资源标识和备注。N、O 与数值不因这次改名而转换。\n\n"
        "## 用户最新选择\n\n"
        "只改名：让锋改为断潮，原断潮改为薄水切，保留水幻形可用。这取代早先的停用计划。两项仍然可用。\n\n"
        "## 资源映射\n\n"
        "- water_yielding_ribbon：显示名让锋改为断潮。效果标识保持 WaterYieldingEffect。稳定编号、配置键和存档不迁移。\n"
        "- tide_sever：显示名断潮改为薄水切。效果标识保持 TideSeverEffect。稳定编号、配置键和存档不迁移。\n"
        "- 水幻形仍只适配 tide_sever。玩家可见载荷名改为薄水切。重斩 water_yielding_ribbon 不进入水幻形适配。\n"
        "- 旧自定义组合标题可能仍是装配时的旧名。重新装配后的标题按新名称。本表不改那些已保存标题。\n\n"
        "## tide_sever\n\n"
        "制作中第 16 行。迁前名称「断潮」。功能句：一道薄而长的水切。资源标识：TideSeverEffect。\n\n"
        "迁前备注：\n\n"
        + originals[TIDE]["DV"]
        + "\n\n## water_yielding_ribbon\n\n"
        "制作中第 17 行。迁前名称「让锋」。功能句：先铺开水面，再从中间劈开。水分开没有伤害，只有中间那一刀。资源标识：WaterYieldingEffect。\n\n"
        "迁前备注：\n\n"
        + originals[RIBBON]["DV"]
        + "\n\n## 编译\n\n"
        "source-owner 已停写。最终 `.\\gradlew.bat compileJava --offline --no-daemon` 于 2026-11-03T15:39:43.983+08:00 至 2026-11-03T15:40:12.387+08:00 执行，退出码 0。编译开始晚于最后源码写入 2026-11-03T15:33:34.665+08:00。未游戏验证，未新增正式复审。\n"
    )


if __name__ == "__main__":
    main()
