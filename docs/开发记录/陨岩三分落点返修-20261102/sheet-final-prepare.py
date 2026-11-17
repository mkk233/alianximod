# -*- coding: utf-8 -*-
"""Move fire_meteorite from 制作中 back to 待验收 after the base-fix compile."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(r"D:\alianximod")
BATCH = ROOT / "docs" / "开发记录" / "陨岩三分落点返修-20261102"
LIVE = ROOT / "docs" / "术式制作表.xlsx"
PRIOR = BATCH / "快照" / "当前需求-20261102-024237-1.md"
TARGET = "fire_meteorite"
SOURCE_ROW = 16
NEW_ROW = 9
NEW_N = "撞到实体后沿当前弹道继续飞行，触地才变成陨石方块。"
NEW_O = (
    "陨岩·熔岩岩壳v5 / meteor-magma-crust-v5\n"
    "正式目录：docs/资源库/材质/方块/陨岩_熔岩岩壳_v5/\n"
    "运行贴图：textures/block/meteorite_v1.png（内容为 meteor-magma-crust-v5）\n"
    "模型：minecraft:block/cube_all"
)
DV_APPEND = (
    "2026-11-02 用户补充原话“装了垂落”；“原底座上落点指定的空间位置就是可以被垂落基层的才对看看是底座问题还是术式问题”。"
    "“基层”是错别字，正确含义是垂落继承落点指定选定的位置。不是新增陨岩强制天降，也不是新散布设计。\n"
    "作者报告：三份父攻击存在，取样用的 copyIndex 全是 0，生成与垂落轨迹重叠，落地空位后备形成竖列。"
    "已在几何取样前把 BirthLayout 的份序号 0/1/2 传入落点及垂落，每次仍提交 1 份。旧路径没有 BirthLayout 时保持原帧。"
    "安放搜索未改。狭窄地形仍可能把残留块补到附近同列，这次改动不保证任何地形三块不共列。\n"
    ".\\gradlew.bat compileJava --offline --no-daemon 于 2026-11-02 02:56:32–02:57:01 退出码 0，"
    "compileJava 实际执行。源文件前后哈希保持 827a767db942ce2e58dd9fade5dc77694e217e37df55cb63a6e594d4b4fb488c。"
    "本阶段额度已用完，返修结果未追加复审。游戏待用户实测。\n"
    "待验收 | 陨岩三分落点返修-20261102 | docs/开发记录/陨岩三分落点返修-20261102/调度记录.md"
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


def approved_function():
    text = PRIOR.read_text(encoding="utf-8")
    marker = "## 功能\n"
    if text.count(marker) != 1:
        fail("当前需求快照的功能标题不唯一")
    body = text.split(marker, 1)[1].split("\n## 视觉", 1)[0].strip("\n")
    if "当前弹道继续飞行，直到触地才落块" not in body:
        fail("当前需求快照缺少续飞功能")
    return body


def main():
    full_n = approved_function()
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
    if len(all_ids) != len(set(all_ids)):
        fail("四页编号重复")
    hits = located.get(TARGET, [])
    if len(hits) != 1 or hits[0][0] != "制作中" or hits[0][1] != SOURCE_ROW:
        fail(f"fire_meteorite 位置异常: {[(item[0], item[1]) for item in hits]}")
    if counts != {"已完成": 40, "待制作": 0, "制作中": 14, "待验收": 6}:
        fail(f"状态页计数异常 {counts}")
    if [number for _code, number, _row in ids_of(pending_xml, shared)] != [3, 4, 5, 6, 7, 8]:
        fail("待验收行号异常")
    source_row = hits[0][2]
    original = column_values(source_row, SOURCE_ROW, shared)
    if original["N"] != full_n:
        fail("制作中功能需求与退回快照不一致，停止对账")
    if "meteor-magma-crust-v5" not in (original["O"] or ""):
        fail("现行视觉不是已确认的 v5")
    if "02:56:32" in (original["DV"] or ""):
        fail("本次编译结果已经写过")
    header = {}
    for row in rows_of(making_xml):
        if row_number(row) == 2:
            header = column_values(row, 2, shared)
            break
    if header.get("N") != "功能描述" or header.get("O") != "特效" or header.get("DV") != "备注":
        fail("表头变化")
    if [code for code, _number, _row in ids_of(parts[PARTS["待制作原始数据"]].decode("utf-8"), shared)].count(TARGET) != 1:
        fail("待制作原始数据首次快照不是一条")
    if f"<v>{shared.index(TARGET)}</v>" in parts[PARTS["资源状态明细"]].decode("utf-8"):
        fail("资源状态明细已有关联行")

    moved = renumber_row(source_row, SOURCE_ROW, NEW_ROW)
    moved = replace_text_cell(moved, "N", NEW_ROW, NEW_N)
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
    if read_at(moved, "N", NEW_ROW, shared) != NEW_N or not read_at(moved, "DV", NEW_ROW, shared).startswith(original["DV"] + "\n"):
        fail("展示或原备注不正确")

    making_out = replace_data(making_xml, [row for row in rows_of(making_xml) if row_number(row) != SOURCE_ROW])
    making_out = replace_once(making_out, 'ref="A1:EN16"', 'ref="A1:EN15"', "制作中维度")
    pending_out = replace_data(pending_xml, rows_of(pending_xml) + [moved])
    pending_out = replace_once(pending_out, 'ref="A1:EN8"', 'ref="A1:EN9"', "待验收维度")
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    workbook = replace_once(workbook, "制作中!$A$2:$EN$16", "制作中!$A$2:$EN$15", "制作中数据范围")
    if 'ref="A2:EN5"' not in making_out or "待验收!$A$2:$EN$2" not in workbook:
        fail("筛选范围不在预期位置")
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
        if [code for code, _number, _row in making_ids] != [code for code, number, _row in ids_of(making_xml, shared) if number != SOURCE_ROW]:
            fail("制作中剩余顺序不对")
        if [code for code, _number, _row in pending_ids][-1] != TARGET or len(pending_ids) != 7:
            fail("待验收没有把陨岩放在末行")
        outside_making = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml).replace('ref="A1:EN16"', 'ref="A1:EN15"')
        if outside_making != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_making):
            fail("制作中筛选或验证被额外改动")
        outside_pending = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_xml).replace('ref="A1:EN8"', 'ref="A1:EN9"')
        if outside_pending != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_pending):
            fail("待验收筛选或验证被额外改动")
        seen = []
        after = {"已完成": 40, "待制作": 0, "制作中": 13, "待验收": 7}
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
    revised_path = snap_dir / f"待验收需求-{stamp}-{sequence}.md"
    aen_path.write_text(json.dumps({
        "baselineSha256": baseline, "sheet": "制作中", "row": SOURCE_ROW, "id": TARGET,
        "columnCount": 144, "cells": cells,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    nodv_path.write_text(json.dumps({
        "baselineSha256": baseline, "id": TARGET,
        "N": original["N"], "O": original["O"], "DV": original["DV"],
        "pendingDisplay": {"N": NEW_N, "O": NEW_O, "dvAppend": DV_APPEND},
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    revised_path.write_text(
        "# fire_meteorite 待验收转换\n\n"
        "用户补充原话“装了垂落”；“原底座上落点指定的空间位置就是可以被垂落基层的才对看看是底座问题还是术式问题”。"
        "“基层”是错别字，正确含义是垂落继承落点指定选定的位置。不是新增强制天降或新散布。\n\n"
        "批准外观 meteor-magma-crust-v5。运行贴图 textures/block/meteorite_v1.png，"
        "SHA256 a0f7027b8b39c4f9f3aaabb9e05713c42e5b0e86697c89d9294848a9b228e1d8，本次未改。\n\n"
        "## 转换前 N\n\n" + original["N"] + "\n\n"
        "## 转换后 N\n\n" + NEW_N + "\n\n"
        "## 转换前 O\n\n" + original["O"] + "\n\n"
        "## 转换后 O\n\n" + NEW_O + "\n\n"
        "compileJava --offline --no-daemon 于 2026-11-02 02:56:32.906–02:57:01.895 退出码 0，"
        "compileJava 实际执行。源文件哈希 827a767db942ce2e58dd9fade5dc77694e217e37df55cb63a6e594d4b4fb488c 前后保持。"
        "本阶段额度已用完，返修结果未追加复审。游戏尚未由用户实测。\n",
        encoding="utf-8",
    )
    result = {
        "ready": True, "submitted": False,
        "baselineSha256": baseline,
        "baselineMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "backup": str(backup), "candidate": str(candidate), "candidateSha256": sha256_file(candidate),
        "snapshots": {"aen": str(aen_path), "nodv": str(nodv_path), "pending": str(revised_path)},
        "from": {"sheet": "制作中", "row": SOURCE_ROW},
        "to": {"sheet": "待验收", "row": NEW_ROW},
        "countsBefore": counts, "countsAfter": after, "live": str(LIVE),
        "affectedParts": list(changed),
    }
    (BATCH / "sheet-final-prepare.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"baseline": baseline, "candidate": result["candidateSha256"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
