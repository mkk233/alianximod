# -*- coding: utf-8 -*-
"""Return fire_meteorite from 待验收 to 制作中 for the v5 texture repair."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(r"D:\alianximod")
BATCH = ROOT / "docs" / "开发记录" / "陨岩岩浆材质接入-20261102"
LIVE = ROOT / "docs" / "术式制作表.xlsx"
PRIOR = ROOT / "docs" / "开发记录" / "陨岩实体命中续飞-20261101" / "快照" / "修订需求-20261102-002838-1.md"
TARGET = "fire_meteorite"
SOURCE_ROW = 9
NEW_ROW = 16
SHORT_N = "撞到实体后沿当前弹道继续飞行，触地才变成陨石方块。"
NEW_O = (
    "平整无孔洞的暗色岩壳之间有大片橙红熔岩、局部亮黄，约1/3表面熔岩，"
    "MC像素32x32静态全不透明，六面同贴图，一格立方体保持。采用 meteor-magma-crust-v5。"
)
DV_APPEND = (
    "2026-11-02 用户确认采用 meteor-magma-crust-v5。原话“采用这个版本”；此前“火焰纹理太少了要更像岩浆块”。"
    "本轮仅换材质，命中实体后沿当前弹道续飞直到触地的功能不改。"
    "历史 compileJava 通过不能代表本次编译。编译待当前运行客户端退出，且材质接入完成后再进行。\n"
    "制作中 | 陨岩岩浆材质接入-20261102 | docs/开发记录/陨岩岩浆材质接入-20261102/调度记录.md"
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
    return {col_name(index): read_at(row, col_name(index), number, shared) for index in range(1, 145)}


def revised_function():
    text = PRIOR.read_text(encoding="utf-8")
    marker = "## 完整修订后的功能需求\n"
    if text.count(marker) != 1:
        fail("上一版完整功能需求标题不唯一")
    body = text.split(marker, 1)[1].split("\n## 待验收展示", 1)[0].strip("\n")
    if "当前弹道继续飞行，直到触地才落块" not in body or "随后残骸无伤坠落" in body:
        fail("上一版功能需求不是续飞到触地")
    return body


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

    full_n = revised_function()
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
    if len(all_ids) != len(set(all_ids)):
        fail("四页编号重复")
    hits = located.get(TARGET, [])
    if len(hits) != 1:
        fail(f"fire_meteorite 出现 {len(hits)} 次")
    if hits[0][0] != "待验收" or hits[0][1] != SOURCE_ROW:
        fail(f"fire_meteorite 不在待验收第9行: {hits[0][0]} {hits[0][1]}")
    if counts != {"已完成": 40, "待制作": 0, "制作中": 13, "待验收": 7}:
        fail(f"状态页计数异常 {counts}")
    source_row = hits[0][2]
    original = column_values(source_row, SOURCE_ROW, shared)
    if original["N"] != SHORT_N:
        fail("待验收功能句已变，停止以免覆盖新需求")
    if "meteor-magma-crust-v5" in (original["DV"] or ""):
        fail("本次视觉确认已经写过")
    header = {}
    for row in rows_of(making_xml):
        if row_number(row) == 2:
            header = column_values(row, 2, shared)
            break
    if header.get("N") != "功能描述" or header.get("O") != "特效" or header.get("DV") != "备注":
        fail("表头变化")
    archive_ids = [code for code, _number, _row in ids_of(parts[PARTS["待制作原始数据"]].decode("utf-8"), shared)]
    if archive_ids.count(TARGET) != 1:
        fail("待制作原始数据首次快照不是一条")
    detail_xml = parts[PARTS["资源状态明细"]].decode("utf-8")
    if f"<v>{shared.index(TARGET)}</v>" in detail_xml:
        fail("资源状态明细已有 fire_meteorite")
    making_numbers = [number for _code, number, _row in ids_of(making_xml, shared)]
    if making_numbers != list(range(3, 16)):
        fail(f"制作中行号异常 {making_numbers}")

    moved = renumber_row(source_row, SOURCE_ROW, NEW_ROW)
    moved = replace_text_cell(moved, "N", NEW_ROW, full_n)
    moved = replace_text_cell(moved, "O", NEW_ROW, NEW_O)
    moved = replace_text_cell(moved, "DV", NEW_ROW, original["DV"] + "\n" + DV_APPEND)
    prefix = source_row[:source_row.find("<c ")].replace(f'r="{SOURCE_ROW}"', f'r="{NEW_ROW}"', 1)
    if not moved.startswith(prefix):
        fail("移动行的行高或行属性丢失")
    for index in range(1, 145):
        column = col_name(index)
        if column in ("N", "O", "DV"):
            continue
        if read_at(moved, column, NEW_ROW, shared) != original[column]:
            fail(f"移动时改到了 {column}")
    written_n = read_at(moved, "N", NEW_ROW, shared)
    if written_n != full_n or "直到触地才落块" not in written_n:
        fail("完整功能需求没有恢复")
    if not read_at(moved, "DV", NEW_ROW, shared).startswith(original["DV"] + "\n"):
        fail("原备注丢失")

    pending_rows = [row for row in rows_of(pending_xml) if row_number(row) != SOURCE_ROW]
    pending_out = replace_data(pending_xml, pending_rows)
    pending_out = replace_once(pending_out, 'ref="A1:EN9"', 'ref="A1:EN8"', "待验收维度")
    making_out = replace_data(making_xml, rows_of(making_xml) + [moved])
    making_out = replace_once(making_out, 'ref="A1:EN15"', 'ref="A1:EN16"', "制作中维度")
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    workbook = replace_once(workbook, "制作中!$A$2:$EN$15", "制作中!$A$2:$EN$16", "制作中数据范围")
    if 'ref="A2:EN5"' not in making_out or 'ref="A2:EN2"' not in pending_out:
        fail("筛选范围不在预期位置")
    if "待验收!$A$2:$EN$2" not in workbook:
        fail("待验收筛选名称丢失")
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
        check_making = dst.read(PARTS["制作中"]).decode("utf-8")
        check_pending = dst.read(PARTS["待验收"]).decode("utf-8")
        making_ids = ids_of(check_making, shared)
        pending_ids = ids_of(check_pending, shared)
        if [code for code, _number, _row in making_ids][:-1] != [code for code, _number, _row in ids_of(making_xml, shared)]:
            fail("制作中原有顺序被改动")
        if [code for code, _number, _row in making_ids][-1] != TARGET or len(making_ids) != 14:
            fail("制作中没有把陨岩放在末行")
        if TARGET in [code for code, _number, _row in pending_ids] or len(pending_ids) != 6:
            fail("待验收仍有陨岩或行数不对")
        for code, number, row in ids_of(making_xml, shared):
            kept = [item for item_code, _item_number, item in making_ids if item_code == code][0]
            if kept != row:
                fail(f"制作中原行被改动 {code}")
        outside_making = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml).replace('ref="A1:EN15"', 'ref="A1:EN16"')
        if outside_making != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_making):
            fail("制作中筛选或验证被额外改动")
        outside_pending = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_xml).replace('ref="A1:EN9"', 'ref="A1:EN8"')
        if outside_pending != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_pending):
            fail("待验收筛选或验证被额外改动")
        after = {"已完成": 40, "待制作": 0, "制作中": 14, "待验收": 6}
        seen = []
        for title in STATUS:
            page = [code for code, _number, _row in ids_of(dst.read(PARTS[title]).decode("utf-8"), shared)]
            if len(page) != after[title]:
                fail(f"{title} 计数 {len(page)}")
            seen.extend(page)
        if len(seen) != len(set(seen)) or len(seen) != 60:
            fail("四页总数或唯一性变化")

    cells = [{"column": col_name(index), "header": header.get(col_name(index)), "text": original[col_name(index)]} for index in range(1, 145)]
    aen_path = snap_dir / f"变更前-AEN-{stamp}-{sequence}.json"
    nodv_path = snap_dir / f"变更前-NODV-{stamp}-{sequence}.json"
    revised_path = snap_dir / f"修订需求-{stamp}-{sequence}.md"
    aen_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "sheet": "待验收",
        "row": SOURCE_ROW,
        "id": TARGET,
        "columnCount": 144,
        "cells": cells,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    nodv_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "id": TARGET,
        "from": {"sheet": "待验收", "row": SOURCE_ROW},
        "N": original["N"],
        "O": original["O"],
        "DV": original["DV"],
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    revised_path.write_text(
        "# fire_meteorite 岩浆材质返修需求\n\n"
        "功能全文沿用 2026-11-02 续飞修订，不回滚。本次只改视觉。\n\n"
        "用户 2026-11-02 对 v5 实际 Blockbench 预览回复“采用这个版本”。"
        "视觉修改原话“火焰纹理太少了要更像岩浆块”。"
        "版本 meteor-magma-crust-v5。原尺寸 PNG SHA256 "
        "a0f7027b8b39c4f9f3aaabb9e05713c42e5b0e86697c89d9294848a9b228e1d8。"
        "过程目录 docs/资源库/过程归档/陨岩_熔岩岩壳_v5-20261102/。"
        "计划正式目录 docs/资源库/材质/方块/陨岩_熔岩岩壳_v5/。"
        "运行引用仍为 textures/block/meteorite_v1.png。本次写回不接入、不编译。\n\n"
        "## 完整功能需求\n\n"
        + full_n + "\n\n"
        "## 文字视觉要求\n\n"
        + NEW_O + "\n\n"
        "## 现行备注与本次追加\n\n"
        + original["DV"] + "\n" + DV_APPEND + "\n\n"
        "历史 compileJava 通过只覆盖续飞那次源码，不能代表这次换材质后的编译。"
        "编译待当前运行客户端退出，且材质接入完成后再进行。"
        "本阶段额度已用完，返修结果未追加复审。\n",
        encoding="utf-8",
    )
    result = {
        "ready": True,
        "submitted": False,
        "baselineSha256": baseline,
        "baselineMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "backup": str(backup),
        "candidate": str(candidate),
        "candidateSha256": sha256_file(candidate),
        "snapshots": {"aen": str(aen_path), "nodv": str(nodv_path), "revised": str(revised_path)},
        "from": {"sheet": "待验收", "row": SOURCE_ROW},
        "to": {"sheet": "制作中", "row": NEW_ROW},
        "countsBefore": counts,
        "countsAfter": {"已完成": 40, "待制作": 0, "制作中": 14, "待验收": 6},
        "live": str(LIVE),
    }
    (BATCH / "sheet-prep-prepare.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"baseline": baseline, "candidate": result["candidateSha256"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
