# -*- coding: utf-8 -*-
"""Move tide_sever and water_yielding_ribbon into 制作中 without changing A/N/O."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(r"D:\alianximod")
BATCH = ROOT / "docs" / "开发记录" / "让锋断潮名称整合-20261103"
WORK = BATCH / "表格工作"
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
PLAN = {
    TIDE: (
        "2026-11-03 用户原话：先只修改让锋和断潮错位的问题，新设计之后再考虑落地。"
        "计划：原薄水切断潮退出现役可用列表，保留旧数据兼容及本行现有功能、数值、冷却、成本、素材，稳定编号仍为 tide_sever。"
        "A、N、O 本阶段不改。旧版停用标签待源码与编译成功后再写。\n"
        "制作中 | 让锋断潮名称整合-20261103 | docs/开发记录/让锋断潮名称整合-20261103/记录.md"
    ),
    RIBBON: (
        "2026-11-03 用户原话：先只修改让锋和断潮错位的问题，新设计之后再考虑落地。"
        "计划：本行显示名改为断潮，保留重斩与真实分水及本行现有功能、数值、冷却、成本、素材，稳定编号仍为 water_yielding_ribbon。"
        "A、N、O 本阶段不改。让锋名称暂不使用。现役新名待源码与编译成功后再写。\n"
        "制作中 | 让锋断潮名称整合-20261103 | docs/开发记录/让锋断潮名称整合-20261103/记录.md"
    ),
}


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


def destination_row(old):
    if old in (7, 9):
        return None
    shift = (1 if old > 7 else 0) + (1 if old > 9 else 0)
    return old - shift


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
    done_xml = parts[PARTS["已完成"]].decode("utf-8")
    making_xml = parts[PARTS["制作中"]].decode("utf-8")
    counts = {}
    located = {}
    all_ids = []
    for title in STATUS:
        page = ids_of(parts[PARTS[title]].decode("utf-8"), shared)
        counts[title] = len(page)
        for code, number, row in page:
            all_ids.append(code)
            located.setdefault(code, []).append((title, number, row))
    if len(all_ids) != len(set(all_ids)) or counts != {"已完成": 40, "待制作": 0, "制作中": 13, "待验收": 7}:
        fail(f"四页计数或唯一性异常 {counts}")
    if [number for _code, number, _row in ids_of(done_xml, shared)] != list(range(3, 43)):
        fail("已完成行号不连续")
    if [number for _code, number, _row in ids_of(making_xml, shared)] != list(range(3, 16)):
        fail("制作中行号不连续")
    header = {}
    for row in rows_of(done_xml):
        if row_number(row) == 2:
            header = column_values(row, 2, shared)
            break
    if header.get("A") != "名称" or header.get("B") != "编号" or header.get("N") != "功能描述" or header.get("O") != "特效" or header.get("DV") != "备注":
        fail("表头变化")
    sources = {}
    for code, expected_name, expected_row in ((TIDE, "断潮", 7), (RIBBON, "让锋", 9)):
        hits = located.get(code, [])
        if len(hits) != 1 or hits[0][0] != "已完成" or hits[0][1] != expected_row:
            fail(f"{code} 位置异常: {[(item[0], item[1]) for item in hits]}")
        if read_at(hits[0][2], "A", expected_row, shared) != expected_name:
            fail(f"{code} 名称与编号不对应")
        sources[code] = hits[0]
    archive_ids = [code for code, _number, _row in ids_of(parts[PARTS["待制作原始数据"]].decode("utf-8"), shared)]
    detail_xml = parts[PARTS["资源状态明细"]].decode("utf-8")
    if TIDE in archive_ids or RIBBON in archive_ids:
        fail("档案已有这两项，停止以免改写档案")
    if f"<v>{shared.index(TIDE)}</v>" in detail_xml or f"<v>{shared.index(RIBBON)}</v>" in detail_xml:
        fail("资源明细已有这两项，停止以免漏搬")
    originals = {}
    for code, (_sheet, number, row) in sources.items():
        original = column_values(row, number, shared)
        if original["DV"] not in (None, ""):
            fail(f"{code} 已有备注，停止以免覆盖")
        if "名称整合" in (original["DV"] or ""):
            fail(f"{code} 的本次计划已经写过")
        originals[code] = original
    if originals[TIDE]["N"] != "一道薄而长的水切" or originals[TIDE]["O"] != "TideSeverEffect":
        fail("断潮现行功能或资源标识已变")
    if originals[RIBBON]["N"] != "先铺开水面，再从中间劈开。水分开没有伤害，只有中间那一刀" or originals[RIBBON]["O"] != "WaterYieldingEffect":
        fail("让锋现行功能或资源标识已变")

    targets = {TIDE: 16, RIBBON: 17}
    moved = {}
    for code in (TIDE, RIBBON):
        _sheet, number, row = sources[code]
        updated = renumber_row(row, number, targets[code])
        updated = replace_text_cell(updated, "DV", targets[code], PLAN[code])
        for index in range(1, 145):
            column = col_name(index)
            if column == "DV":
                continue
            if read_at(updated, column, targets[code], shared) != originals[code][column]:
                fail(f"{code} 改到了 {column}")
        if read_at(updated, "A", targets[code], shared) != originals[code]["A"]:
            fail(f"{code} 名称被改")
        if read_at(updated, "DV", targets[code], shared) != PLAN[code]:
            fail(f"{code} 备注未按计划写入")
        moved[code] = updated

    done_rows = []
    for row in rows_of(done_xml):
        old = row_number(row)
        new = destination_row(old)
        if new is None:
            continue
        done_rows.append(renumber_row(row, old, new))
    if [row_number(row) for row in done_rows] != list(range(1, 41)):
        fail("已完成重排后行号不连续")
    done_out = replace_data(done_xml, done_rows)
    done_out = replace_once(done_out, 'ref="A1:EN42"', 'ref="A1:EN40"', "已完成维度")
    done_out = replace_once(done_out, 'ref="A2:EN42"', 'ref="A2:EN40"', "已完成筛选")
    making_out = replace_data(making_xml, rows_of(making_xml) + [moved[TIDE], moved[RIBBON]])
    making_out = replace_once(making_out, 'ref="A1:EN15"', 'ref="A1:EN17"', "制作中维度")
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    workbook = replace_once(workbook, "已完成!$A$2:$EN$42", "已完成!$A$2:$EN$40", "已完成数据范围")
    workbook = replace_once(workbook, "制作中!$A$2:$EN$15", "制作中!$A$2:$EN$17", "制作中数据范围")
    if 'ref="A2:EN5"' not in making_out:
        fail("制作中原筛选范围丢失")
    ET.fromstring(done_out.encode("utf-8"))
    ET.fromstring(making_out.encode("utf-8"))
    ET.fromstring(workbook.encode("utf-8"))
    changed = {
        PARTS["已完成"]: done_out.encode("utf-8"),
        PARTS["制作中"]: making_out.encode("utf-8"),
        "xl/workbook.xml": workbook.encode("utf-8"),
    }
    with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))

    with zipfile.ZipFile(candidate) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if set(different) != set(changed):
            fail("候选改到了意外部件: " + ",".join(different))
        for title in ("待制作", "待验收", "数据表", "资源状态明细", "待制作原始数据"):
            if dst.read(PARTS[title]) != parts[PARTS[title]]:
                fail(f"{title} 被改动")
        check_done = dst.read(PARTS["已完成"]).decode("utf-8")
        check_making = dst.read(PARTS["制作中"]).decode("utf-8")
        done_ids = ids_of(check_done, shared)
        making_ids = ids_of(check_making, shared)
        if [code for code, _number, _row in making_ids][:-2] != [code for code, _number, _row in ids_of(making_xml, shared)]:
            fail("制作中原有顺序被改动")
        if [code for code, _number, _row in making_ids][-2:] != [TIDE, RIBBON]:
            fail("末两行不是断潮和让锋")
        if TIDE in [code for code, _number, _row in done_ids] or RIBBON in [code for code, _number, _row in done_ids]:
            fail("已完成仍有这两项")
        outside_done = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", done_xml)
        outside_done = outside_done.replace('ref="A1:EN42"', 'ref="A1:EN40"').replace('ref="A2:EN42"', 'ref="A2:EN40"')
        if outside_done != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_done):
            fail("已完成筛选、验证或视图被额外改动")
        outside_making = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml).replace('ref="A1:EN15"', 'ref="A1:EN17"')
        if outside_making != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_making):
            fail("制作中筛选或验证被额外改动")
        kept = {}
        for row in rows_of(done_xml):
            old = row_number(row)
            new = destination_row(old)
            if new is None:
                continue
            kept[new] = renumber_row(row, old, new)
        for row in rows_of(check_done):
            number = row_number(row)
            if kept[number] != row:
                fail(f"已完成第 {number} 行内容变化")
        seen = []
        after = {"已完成": 38, "待制作": 0, "制作中": 15, "待验收": 7}
        for title in STATUS:
            page = [code for code, _number, _row in ids_of(dst.read(PARTS[title]).decode("utf-8"), shared)]
            if len(page) != after[title]:
                fail(f"{title} 计数 {len(page)}")
            seen.extend(page)
        if len(seen) != len(set(seen)) or len(seen) != 60:
            fail("四页总数或唯一性变化")
        if sha256_file(LIVE) != baseline:
            fail("准备期间源表已变化")

    cells = {}
    for code in (TIDE, RIBBON):
        cells[code] = [
            {"column": col_name(index), "header": header.get(col_name(index)), "text": originals[code][col_name(index)]}
            for index in range(1, 145)
        ]
    aen_path = snap_dir / f"变更前-AEN-{stamp}-{sequence}.json"
    current_path = snap_dir / f"现行需求-{stamp}-{sequence}.md"
    aen_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "columnCount": 144,
        "rows": {
            code: {"sheet": "已完成", "row": sources[code][1], "cells": cells[code]}
            for code in (TIDE, RIBBON)
        },
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    current_path.write_text(requirement_text(originals, header), encoding="utf-8")
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
            TIDE: {"from": {"sheet": "已完成", "row": 7}, "to": {"sheet": "制作中", "row": 16}, "name": "断潮"},
            RIBBON: {"from": {"sheet": "已完成", "row": 9}, "to": {"sheet": "制作中", "row": 17}, "name": "让锋"},
        },
        "countsBefore": counts,
        "countsAfter": after,
        "live": str(LIVE),
        "affectedParts": list(changed),
        "archiveUntouched": True,
        "resourceDetailUntouched": True,
        "nameAndRequirementColumnsUnchanged": ["A", "B", "N", "O"],
    }
    (WORK / "prepare-preview.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({
        "baseline": baseline,
        "candidate": result["candidateSha256"],
        "moves": result["moves"],
        "countsAfter": after,
    }, ensure_ascii=False))


def requirement_text(originals, header):
    def line(code, column):
        return f"- {column} {header.get(column) or ''}：{originals[code].get(column)}"

    return (
        "# 让锋与断潮名称整合：迁制作中前的现行需求\n\n"
        "本文件只留档。不改功能、数值、冷却、成本、素材或稳定编号。A、N、O 保持表内原文。\n\n"
        "## 用户本次原话\n\n"
        "2026-11-03 记录原文：先只修改让锋和断潮错位的问题，新设计之后再考虑落地。"
        "承接方案：现有让锋（水体分开与重斩）改名断潮；旧薄水切断潮退出可用列表；让锋名称暂不承载新功能。\n\n"
        "本阶段已批准的机械范围：water_yielding_ribbon 保留重斩与真实分水，显示名改为断潮；"
        "tide_sever 退出现役可用列表并保留旧数据兼容。现役新名和旧版停用标签待源码与编译成功后再写入。\n\n"
        "## tide_sever\n\n"
        "表位置：已完成第 7 行。名称「断潮」。编号 tide_sever。类型剑术，属性水，链上角色攻击。\n\n"
        "现行功能句（已完成展示，本阶段不改）：一道薄而长的水切。\n\n"
        "现行资源标识（本阶段不改）：TideSeverEffect。\n\n"
        "备注原为空。\n\n"
        "可追溯的现行设计摘要：docs/水属性术式.md 攻击表与核对表。"
        "断潮为剑术，伤害 6，冷却 20 tick，一道薄而长的水切，弹道不可改，只允许索敌改生成位置。核对表写明已实现。\n\n"
        "表内对应列：\n\n"
        + "\n".join(line(TIDE, column) for column in ("E", "F", "G", "L", "P", "Q", "R", "S", "U", "W"))
        + "\n\n"
        "缺口：没有待制作原始数据行，也没有进入待验收前保存的 N/O/DV 原文快照。不从档案回滚，不补写猜测的长需求。\n\n"
        "## water_yielding_ribbon\n\n"
        "表位置：已完成第 9 行。名称「让锋」。编号 water_yielding_ribbon。类型剑术，属性水，链上角色攻击。\n\n"
        "现行功能句（已完成展示，本阶段不改）：先铺开水面，再从中间劈开。水分开没有伤害，只有中间那一刀。\n\n"
        "现行资源标识（本阶段不改）：WaterYieldingEffect。\n\n"
        "备注原为空。\n\n"
        "可追溯的现行设计摘要：docs/水属性术式.md。"
        "让锋为剑术，伤害 40，冷却 600 tick，魔力 80；先铺水面，10 tick 后中刀生效。该文写「两侧分开只是画面」。核对表写明已实现。\n\n"
        "用户本次要求保留的是重斩与真实分水，不是一套新数值。docs/让锋真实分水修改与实现指导-2026-10-26.md "
        "写明该轮未改功能，第 4 节规则当时用户尚未单独确认。该指导不覆盖表内 N/O，也不当作已经实现的验收原文。\n\n"
        "表内对应列：\n\n"
        + "\n".join(line(RIBBON, column) for column in ("E", "F", "G", "L", "P", "Q", "R", "S", "U", "W"))
        + "\n\n"
        "缺口：没有待制作原始数据行，也没有进入待验收前保存的 N/O/DV 原文快照。"
        "表内功能句与「真实分水」的措辞并不相同。本阶段不改 N，不把未确认的分水规则补进功能描述。\n"
    )


if __name__ == "__main__":
    main()
