# -*- coding: utf-8 -*-
"""Move only fire_burn from 待验收 back to 制作中 inside a candidate workbook."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(r"D:\alianximod")
LIVE = ROOT / "docs" / "术式制作表.xlsx"
WORK = Path(r"D:\alianximod\docs\开发记录\燃烧空间挂载接入-20261101\第一人称空手返修-20261109\表格")
SNAP = ROOT / "docs" / "开发记录" / "燃烧空间挂载接入-20261101" / "快照" / "变更前-NODV-20261101-214704-1.json"
TARGET = "fire_burn"
STATUS = ("已完成", "待制作", "制作中", "待验收")
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"


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
        return row[start: opener_end + 1], start, opener_end + 1
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
        if code and str(code).strip():
            found.append((str(code).strip(), number, row))
    return found


def column_values(row, number, shared):
    found = {}
    for index in range(1, 145):
        column = col_name(index)
        found[column] = read_at(row, column, number, shared)
    return found


def sheet_map(parts):
    rels = ET.fromstring(parts["xl/_rels/workbook.xml.rels"])
    targets = {}
    for node in rels:
        target = "xl/" + node.attrib["Target"].lstrip("/")
        targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
    work = ET.fromstring(parts["xl/workbook.xml"])
    found = {}
    for node in work.find("s:sheets", NS):
        found[node.attrib["name"]] = targets[node.attrib[REL]]
    return found


def text_cell(column, number, text, style):
    escaped = html.escape(text, quote=False)
    space = ' xml:space="preserve"' if ("\n" in text or (text[:1].isspace() if text else False) or (text[-1:].isspace() if text else False)) else ""
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


def main():
    live_bytes = LIVE.read_bytes()
    baseline = sha256_bytes(live_bytes)
    stat = LIVE.stat()
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup_dir = WORK / "备份"
    candidate_dir = WORK / "候选"
    snap_dir = WORK / "快照"
    for folder in (backup_dir, candidate_dir, snap_dir):
        folder.mkdir(exist_ok=True)
    sequence = 1
    while (backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx").exists():
        sequence += 1
    backup = backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx"
    candidate = candidate_dir / f"术式制作表-候选-{stamp}-{sequence}.xlsx"
    backup.write_bytes(live_bytes)
    if sha256_bytes(backup.read_bytes()) != baseline:
        fail("备份哈希与读取基线不一致")

    approved = json.loads(SNAP.read_text(encoding="utf-8"))
    feedback = (
        "[设计反馈 2026-11-09] 用户确认第三人称与预览完全一致。第一人称空手时火焰落在手下方。"
        "问题只在该视角的运行挂点。已确认的第三人称外形和素材保持不变。建议只修正挂接偏移，不改玩法，也不另做外形。"
        "完整术式尚不能据此判为已完成。详见 docs/开发记录/燃烧空间挂载接入-20261101/第一人称空手返修-20261109/记录.md\n"
        "制作中 | heat-burn-20261031 | docs/开发记录/燃烧空间挂载接入-20261101/第一人称空手返修-20261109/记录.md"
    )
    visual_merge = (
        "2026-11-09 用户确认第三人称与 burn-v3-spatial-v1 预览完全一致，该已批准空间挂载保持为当前外观，"
        "不回退更早平面挂载，也不改素材。第一人称空手时火焰落在手下方，本次只修正该运行挂接偏移。"
        "完整术式尚未验收完成。"
    )

    with zipfile.ZipFile(backup) as archive:
        order = archive.infolist()
        parts = {info.filename: archive.read(info.filename) for info in order}
    names = sheet_map(parts)
    for title in list(STATUS) + ["资源状态明细", "待制作原始数据", "数据表"]:
        if title not in names:
            fail(f"缺少工作表 {title}")
    shared = shared_list(parts["xl/sharedStrings.xml"])
    pages = {}
    located = {}
    counts = {}
    all_ids = []
    for title in STATUS:
        page = ids_of(parts[names[title]].decode("utf-8"), shared)
        pages[title] = page
        counts[title] = len(page)
        for code, number, row in page:
            all_ids.append(code)
            located.setdefault(code, []).append((title, number, row))
    if len(all_ids) != len(set(all_ids)):
        fail("四张状态页编号重复")
    if counts != {"已完成": 37, "待制作": 0, "制作中": 16, "待验收": 9}:
        fail(f"状态页计数不是读取时的 37/0/16/9：{counts}")
    hits = located.get(TARGET, [])
    if len(hits) != 1 or hits[0][0] != "待验收" or hits[0][1] != 7:
        fail(f"fire_burn 不在待验收第7行: {[(item[0], item[1]) for item in hits]}")
    source_row = hits[0][2]
    original = column_values(source_row, 7, shared)
    if original["N"] != approved["pendingDisplay"]["N"] or original["O"] != approved["pendingDisplay"]["O"]:
        fail("当前待验收 N/O 已不是 2026-11-01 写入的展示句，停止以免覆盖后来的改动")
    if original["DV"] != approved["DV"] + "\n" + approved["pendingDisplay"]["dvAppend"]:
        fail("当前 DV 与已批准原文加 2026-11-01 追加不一致")
    if not approved["N"] or not approved["O"]:
        fail("已批准完整 N/O 为空")
    archive_ids = [code for code, _number, _row in ids_of(parts[names["待制作原始数据"]].decode("utf-8"), shared)]
    if archive_ids.count(TARGET) != 1:
        fail("待制作原始数据中的 fire_burn 不是一条")
    detail_xml = parts[names["资源状态明细"]].decode("utf-8")
    detail_ids = []
    for row in rows_of(detail_xml):
        number = row_number(row)
        if number < 3:
            continue
        code = read_at(row, "A", number, shared)
        if code and str(code).strip():
            detail_ids.append(str(code).strip())
    if TARGET in detail_ids:
        fail("资源状态明细已有 fire_burn")
    pending_xml = parts[names["待验收"]].decode("utf-8")
    making_xml = parts[names["制作中"]].decode("utf-8")
    if "<row r=\"19\"" in making_xml or 'r="19"' in making_xml.split("</sheetData>", 1)[0]:
        fail("制作中已经有第19行")
    if "conditionalFormatting" in pending_xml or "conditionalFormatting" in making_xml:
        fail("出现条件格式，停止")
    if re.search(r"<comment\b[^>]*ref=\"[A-Z]+7\"", pending_xml):
        fail("待验收第7行有批注锚点")
    header = {}
    for row in rows_of(making_xml):
        if row_number(row) == 2:
            header = column_values(row, 2, shared)
            break
    if header.get("N") != "功能描述" or header.get("O") != "特效" or header.get("DV") != "备注":
        fail("表头变化")

    new_n = approved["N"]
    new_o = approved["O"] + "\n" + visual_merge
    new_dv = original["DV"] + "\n" + feedback
    moved = renumber_row(source_row, 7, 19)
    moved = replace_text_cell(moved, "N", 19, new_n)
    moved = replace_text_cell(moved, "O", 19, new_o)
    moved = replace_text_cell(moved, "DV", 19, new_dv)
    for index in range(1, 145):
        column = col_name(index)
        if column in ("N", "O", "DV"):
            continue
        if read_at(moved, column, 19, shared) != original[column]:
            fail(f"移动时改到了 {column}")
    if read_at(moved, "N", 19, shared) != new_n or read_at(moved, "O", 19, shared) != new_o or read_at(moved, "DV", 19, shared) != new_dv:
        fail("恢复后的 N/O/DV 读回不一致")
    if source_row not in pending_xml or pending_xml.count(source_row) != 1:
        fail("来源行不唯一")
    pending_out = pending_xml.replace(source_row, "", 1)
    if "<row r=\"7\"" in pending_out:
        fail("待验收仍留有第7行")
    making_out = making_xml.replace("</sheetData>", moved + "</sheetData>", 1)
    making_out = replace_once(making_out, 'ref="A1:EN18"', 'ref="A1:EN19"', "制作中维度")
    if making_xml.count('<autoFilter ref="A2:EN5"') != 1 or making_out.count('<autoFilter ref="A2:EN5"') != 1:
        fail("制作中筛选范围与读取时不同")
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    workbook = replace_once(workbook, "制作中!$A$2:$EN$18", "制作中!$A$2:$EN$19", "制作中筛选名称")
    ET.fromstring(making_out.encode("utf-8"))
    ET.fromstring(pending_out.encode("utf-8"))
    ET.fromstring(workbook.encode("utf-8"))
    outside_making = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml).replace('ref="A1:EN18"', 'ref="A1:EN19"')
    outside_check = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_out)
    if outside_making != outside_check:
        fail("制作中筛选、验证或合并区被额外改动")
    if re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_xml).replace(source_row, "") != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_out):
        # source row is inside sheetData, so outside parts must match exactly
        pass
    if re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_xml) != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_out):
        fail("待验收验证、筛选或合并区被改动")

    changed = {
        names["制作中"]: making_out.encode("utf-8"),
        names["待验收"]: pending_out.encode("utf-8"),
        "xl/workbook.xml": workbook.encode("utf-8"),
    }
    with zipfile.ZipFile(candidate, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, parts[info.filename]))
    with zipfile.ZipFile(candidate) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if set(different) != set(changed):
            fail("候选改到了意外部件: " + ",".join(different))
        check_shared = shared_list(dst.read("xl/sharedStrings.xml"))
        if check_shared != shared:
            fail("共享字符串被改动")
        after_counts = {}
        after_ids = []
        after_located = {}
        for title in STATUS:
            page = ids_of(dst.read(names[title]).decode("utf-8"), check_shared)
            after_counts[title] = len(page)
            for code, number, row in page:
                after_ids.append(code)
                after_located.setdefault(code, []).append((title, number, row))
        if after_counts != {"已完成": 37, "待制作": 0, "制作中": 17, "待验收": 8} or len(after_ids) != len(set(after_ids)) or len(after_ids) != 62:
            fail(f"迁移后计数异常 {after_counts} {len(after_ids)}")
        placed = after_located[TARGET]
        if placed != [("制作中", 19, placed[0][2])]:
            fail("fire_burn 没有只落在制作中第19行")
        written = placed[0][2]
        if read_at(written, "N", 19, check_shared) != new_n or read_at(written, "O", 19, check_shared) != new_o or read_at(written, "DV", 19, check_shared) != new_dv:
            fail("候选读回 N/O/DV 不一致")
        for code, number, row in pages["制作中"]:
            match = [item for item in after_located[code] if item[0] == "制作中"]
            if len(match) != 1 or match[0][1] != number:
                fail(f"制作中原行位置变化 {code}")
            if match[0][2] != row:
                fail(f"制作中原行内容变化 {code}")
        for code, number, row in pages["待验收"]:
            if code == TARGET:
                continue
            match = [item for item in after_located[code] if item[0] == "待验收"]
            if len(match) != 1 or match[0][1] != number or match[0][2] != row:
                fail(f"待验收其他行变化 {code}")
        if dst.read(names["待制作原始数据"]) != parts[names["待制作原始数据"]]:
            fail("原始档案被改动")
        if dst.read(names["资源状态明细"]) != parts[names["资源状态明细"]]:
            fail("资源状态明细被改动")
        if dst.read(names["数据表"]) != parts[names["数据表"]]:
            fail("数据表被改动")
        if dst.read(names["已完成"]) != parts[names["已完成"]] or dst.read(names["待制作"]) != parts[names["待制作"]]:
            fail("已完成或待制作被改动")

    cells = []
    for index in range(1, 145):
        column = col_name(index)
        cells.append({"column": column, "header": header.get(column), "text": original[column]})
    aen_path = snap_dir / f"变更前-AEN-{stamp}-{sequence}.json"
    nodv_path = snap_dir / f"迁回前-NODV-{stamp}-{sequence}.json"
    aen_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "sheet": "待验收",
        "row": 7,
        "columnCount": 144,
        "cells": cells,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    nodv_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "id": TARGET,
        "from": {"sheet": "待验收", "row": 7},
        "to": {"sheet": "制作中", "row": 19},
        "currentN": original["N"],
        "currentO": original["O"],
        "currentDV": original["DV"],
        "approvedSnapshot": str(SNAP),
        "restoredN": new_n,
        "restoredO": new_o,
        "restoredDV": new_dv,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    result = {
        "ready": True,
        "submitted": False,
        "baselineSha256": baseline,
        "baselineBytes": len(live_bytes),
        "baselineMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "backup": str(backup),
        "candidate": str(candidate),
        "candidateSha256": sha256_bytes(candidate.read_bytes()),
        "snapshots": {"aen": str(aen_path), "nodv": str(nodv_path), "approved": str(SNAP)},
        "from": {"sheet": "待验收", "row": 7},
        "to": {"sheet": "制作中", "row": 19},
        "countsBefore": counts,
        "countsAfter": after_counts,
        "live": str(LIVE),
        "detailRowsMoved": 0,
        "archiveUntouched": True,
    }
    (WORK / "move-prepare-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"baseline": baseline, "candidate": result["candidateSha256"], "toRow": 19}, ensure_ascii=False))


if __name__ == "__main__":
    main()
