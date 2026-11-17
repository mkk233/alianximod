# -*- coding: utf-8 -*-
"""Return fire_meteorite from 待验收 to 制作中 after the playtest report."""
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
PRIOR = ROOT / "docs" / "开发记录" / "陨岩岩浆材质接入-20261102" / "快照" / "修订需求-20261102-022635-1.md"
TARGET = "fire_meteorite"
SOURCE_ROW = 9
NEW_ROW = 16
SHORT_N = "撞到实体后沿当前弹道继续飞行，触地才变成陨石方块。"
DV_APPEND = (
    "2026-11-02 用户实测：“一分为三+陨石会在同一个地点生成三个陨岩，并且这个链条上我还装配了落点指定,"
    "并且刚释放时没有正确出现三个陨石一起落下，落地后才能看出来三个陨岩”。"
    "截图 C:/Users/baishang/AppData/Local/Temp/codex-clipboard-73c3459d-b583-44c6-9fd1-d7255cb60508.png。"
    "此处只记录观察。没有新的间距或落点设计。返修进行中。"
    "本阶段额度已用完，返修结果未追加复审。本次退回未编译。\n"
    "制作中 | 陨岩三分落点返修-20261102 | docs/开发记录/陨岩三分落点返修-20261102/调度记录.md"
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


def section(text, start, end):
    marker = start + "\n"
    if text.count(marker) != 1:
        fail("快照标题不唯一: " + start)
    return text.split(marker, 1)[1].split("\n" + end, 1)[0].strip("\n")


def main():
    prior = PRIOR.read_text(encoding="utf-8")
    full_n = section(prior, "## 转换前 N", "## 转换后 N")
    full_o = section(prior, "## 转换前 O", "## 转换后 O")
    if "当前弹道继续飞行，直到触地才落块" not in full_n or "随后残骸无伤坠落" in full_n:
        fail("详细功能不是续飞到触地")
    if "meteor-magma-crust-v5" not in full_o or "熔岩" not in full_o:
        fail("详细视觉不是已确认的 v5")
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
    if len(hits) != 1:
        fail(f"fire_meteorite 出现 {len(hits)} 次")
    if hits[0][0] == "制作中":
        fail("已在制作中，本脚本不重复迁移")
    if hits[0][0] != "待验收" or hits[0][1] != SOURCE_ROW:
        fail(f"fire_meteorite 不在待验收第9行: {hits[0][0]} {hits[0][1]}")
    if counts != {"已完成": 40, "待制作": 0, "制作中": 13, "待验收": 7}:
        fail(f"状态页计数异常 {counts}")
    if [number for _code, number, _row in ids_of(making_xml, shared)] != list(range(3, 16)):
        fail("制作中行号异常")
    source_row = hits[0][2]
    original = column_values(source_row, SOURCE_ROW, shared)
    if original["N"] != SHORT_N:
        fail("待验收功能句已变，停止对账")
    if "同一个地点生成三个陨岩" in (original["DV"] or ""):
        fail("本次实测反馈已经写过")
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
        fail("资源状态明细已有关联行，停止以免漏搬")

    moved = renumber_row(source_row, SOURCE_ROW, NEW_ROW)
    moved = replace_text_cell(moved, "N", NEW_ROW, full_n)
    moved = replace_text_cell(moved, "O", NEW_ROW, full_o)
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
    if read_at(moved, "N", NEW_ROW, shared) != full_n or read_at(moved, "O", NEW_ROW, shared) != full_o:
        fail("详细规格没有恢复")
    if not read_at(moved, "DV", NEW_ROW, shared).startswith(original["DV"] + "\n"):
        fail("原备注丢失")

    pending_out = replace_data(pending_xml, [row for row in rows_of(pending_xml) if row_number(row) != SOURCE_ROW])
    pending_out = replace_once(pending_out, 'ref="A1:EN9"', 'ref="A1:EN8"', "待验收维度")
    making_out = replace_data(making_xml, rows_of(making_xml) + [moved])
    making_out = replace_once(making_out, 'ref="A1:EN15"', 'ref="A1:EN16"', "制作中维度")
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    workbook = replace_once(workbook, "制作中!$A$2:$EN$15", "制作中!$A$2:$EN$16", "制作中数据范围")
    if 'ref="A2:EN2"' not in pending_out or 'ref="A2:EN5"' not in making_out or "待验收!$A$2:$EN$2" not in workbook:
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
        if [code for code, _number, _row in making_ids][:-1] != [code for code, _number, _row in ids_of(making_xml, shared)]:
            fail("制作中原有顺序被改动")
        if [code for code, _number, _row in making_ids][-1] != TARGET or len(making_ids) != 14:
            fail("制作中没有把陨岩放在末行")
        if TARGET in [code for code, _number, _row in pending_ids] or len(pending_ids) != 6:
            fail("待验收仍有陨岩或行数不对")
        outside_making = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml).replace('ref="A1:EN15"', 'ref="A1:EN16"')
        if outside_making != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_making):
            fail("制作中筛选或验证被额外改动")
        outside_pending = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_xml).replace('ref="A1:EN9"', 'ref="A1:EN8"')
        if outside_pending != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_pending):
            fail("待验收筛选或验证被额外改动")
        seen = []
        after = {"已完成": 40, "待制作": 0, "制作中": 14, "待验收": 6}
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
    revised_path = snap_dir / f"当前需求-{stamp}-{sequence}.md"
    aen_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "sheet": "待验收",
        "row": SOURCE_ROW,
        "id": TARGET,
        "columnCount": 144,
        "detailRows": [],
        "cells": cells,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    nodv_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "id": TARGET,
        "N": original["N"],
        "O": original["O"],
        "DV": original["DV"],
        "restored": {"N": full_n, "O": full_o, "dvAppend": DV_APPEND},
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    revised_path.write_text(
        "# fire_meteorite 当前需求\n\n"
        "功能仍是实体命中后按当前弹道续飞直到触地，首次结算不重复。"
        "外观仍是已确认的 meteor-magma-crust-v5，不退回旧直坠，也不退回旧材质。\n\n"
        "用户 2026-11-02 实测原话：“一分为三+陨石会在同一个地点生成三个陨岩，并且这个链条上我还装配了落点指定,"
        "并且刚释放时没有正确出现三个陨石一起落下，落地后才能看出来三个陨岩”。\n\n"
        "截图：C:/Users/baishang/AppData/Local/Temp/codex-clipboard-73c3459d-b583-44c6-9fd1-d7255cb60508.png。"
        "只记录这次观察。没有据此规定三颗间距或落点。\n\n"
        "## 功能\n\n" + full_n + "\n\n"
        "## 视觉\n\n" + full_o + "\n\n"
        "正式目录仍是 docs/资源库/材质/方块/陨岩_熔岩岩壳_v5/。"
        "运行贴图仍是 textures/block/meteorite_v1.png，内容为 v5。模型仍是 minecraft:block/cube_all。\n\n"
        "资源状态明细没有本编号的关联行。本次退回未编译，未追加复审。\n",
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
        "snapshots": {"aen": str(aen_path), "nodv": str(nodv_path), "current": str(revised_path)},
        "from": {"sheet": "待验收", "row": SOURCE_ROW},
        "to": {"sheet": "制作中", "row": NEW_ROW},
        "countsBefore": counts,
        "countsAfter": after,
        "live": str(LIVE),
        "affectedParts": ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml", "xl/workbook.xml"],
    }
    (BATCH / "sheet-return-prepare.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"baseline": baseline, "candidate": result["candidateSha256"], "affected": result["affectedParts"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
