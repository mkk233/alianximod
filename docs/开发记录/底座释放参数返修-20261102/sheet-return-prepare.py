# -*- coding: utf-8 -*-
"""Return fire_meteorite and fire_shock_ring from 待验收 to 制作中."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(r"D:\alianximod")
BATCH = ROOT / "docs" / "开发记录" / "底座释放参数返修-20261102"
LIVE = ROOT / "docs" / "术式制作表.xlsx"
METEOR_PRIOR = ROOT / "docs" / "开发记录" / "陨岩三分落点返修-20261102" / "快照" / "待验收需求-20261102-030207-1.md"
HEAT_PRIOR = ROOT / "docs" / "开发记录" / "热波正圆精修-20261101" / "快照" / "变更前-NODV-20261101-234901-1.json"
METEOR = "fire_meteorite"
HEAT = "fire_shock_ring"
DV_APPEND = (
    "2026-11-02 用户明确“修”。已确认问题：批量路径丢落点中心；建帧份序号未继承；热波优先共享 anchor 导致重合。"
    "完整根因 docs/开发记录/底座释放同类排查-20261102/Cursor检查报告.md。"
    "本阶段未实现，不标通过。上一轮追加实现检查 1 轮已完成，累计实现 4 轮。此次返修不追加正式复审。\n"
    "制作中 | 底座释放参数返修-20261102 | docs/开发记录/底座释放同类排查-20261102/Cursor检查报告.md"
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


def requirements():
    meteor_text = METEOR_PRIOR.read_text(encoding="utf-8")
    meteor_n = section(meteor_text, "## 转换前 N", "## 转换后 N")
    meteor_visual = section(meteor_text, "## 转换前 O", "## 转换后 O")
    meteor_resource = section(meteor_text, "## 转换后 O", "compileJava")
    if "直到触地才落块" not in meteor_n or "meteor-magma-crust-v5" not in meteor_visual:
        fail("陨岩完整需求不是续飞和 v5")
    meteor_o = meteor_visual + "\n" + meteor_resource.strip()
    heat = json.loads(HEAT_PRIOR.read_text(encoding="utf-8"))
    heat_n = heat["N"]
    heat_resource = heat["pendingDisplay"]["O"]
    if "外半径5格" not in heat_n or "内半径1.5格" not in heat_n:
        fail("热波完整功能缺少内外半径")
    if "heat-circle-v2" not in heat_resource or "半径 10" in heat_resource:
        fail("热波资源映射不是正圆 v2")
    heat_o = (
        "水平正圆，中心留空 1.5 格，外缘对齐外半径 5 格。载体边长 10 格是直径。一次结算。\n"
        + heat_resource
    )
    return {
        METEOR: {"N": meteor_n, "O": meteor_o, "shortN": "撞到实体后沿当前弹道继续飞行，触地才变成陨石方块。"},
        HEAT: {"N": heat_n, "O": heat_o, "shortN": "在锚点周围产生一次环形冲击，中心留空，只结算一次。"},
    }


def main():
    wanted = requirements()
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
    sources = {}
    for code in (HEAT, METEOR):
        hits = located.get(code, [])
        if len(hits) != 1 or hits[0][0] != "待验收":
            fail(f"{code} 不在待验收: {[(item[0], item[1]) for item in hits]}")
        sources[code] = hits[0]
    if sources[HEAT][1] != 8 or sources[METEOR][1] != 9:
        fail(f"行号变化 热波{sources[HEAT][1]} 陨岩{sources[METEOR][1]}")
    if [number for _code, number, _row in ids_of(making_xml, shared)] != list(range(3, 16)):
        fail("制作中行号异常")
    header = {}
    for row in rows_of(making_xml):
        if row_number(row) == 2:
            header = column_values(row, 2, shared)
            break
    if header.get("N") != "功能描述" or header.get("O") != "特效" or header.get("DV") != "备注":
        fail("表头变化")
    archive_ids = [code for code, _number, _row in ids_of(parts[PARTS["待制作原始数据"]].decode("utf-8"), shared)]
    detail_xml = parts[PARTS["资源状态明细"]].decode("utf-8")
    originals = {}
    for code, (_sheet, number, row) in sources.items():
        if archive_ids.count(code) != 1:
            fail(f"{code} 的首次档案不是一条")
        if f"<v>{shared.index(code)}</v>" in detail_xml:
            fail(f"{code} 已有资源明细，停止以免漏搬")
        original = column_values(row, number, shared)
        if original["N"] != wanted[code]["shortN"]:
            fail(f"{code} 的现行功能句已变")
        if "批量路径丢落点中心" in (original["DV"] or ""):
            fail(f"{code} 的本次反馈已经写过")
        originals[code] = original

    targets = {HEAT: 16, METEOR: 17}
    moved = {}
    for code in (HEAT, METEOR):
        _sheet, number, row = sources[code]
        updated = renumber_row(row, number, targets[code])
        updated = replace_text_cell(updated, "N", targets[code], wanted[code]["N"])
        updated = replace_text_cell(updated, "O", targets[code], wanted[code]["O"])
        updated = replace_text_cell(updated, "DV", targets[code], originals[code]["DV"] + "\n" + DV_APPEND)
        prefix = row[:row.find("<c ")].replace(f'r="{number}"', f'r="{targets[code]}"', 1)
        if not updated.startswith(prefix):
            fail(f"{code} 行属性丢失")
        for index in range(1, 145):
            column = col_name(index)
            if column in ("N", "O", "DV"):
                continue
            if read_at(updated, column, targets[code], shared) != originals[code][column]:
                fail(f"{code} 改到了 {column}")
        if not read_at(updated, "DV", targets[code], shared).startswith(originals[code]["DV"] + "\n"):
            fail(f"{code} 原备注丢失")
        moved[code] = updated

    pending_rows = [row for row in rows_of(pending_xml) if row_number(row) not in (8, 9)]
    pending_out = replace_data(pending_xml, pending_rows)
    pending_out = replace_once(pending_out, 'ref="A1:EN9"', 'ref="A1:EN7"', "待验收维度")
    making_out = replace_data(making_xml, rows_of(making_xml) + [moved[HEAT], moved[METEOR]])
    making_out = replace_once(making_out, 'ref="A1:EN15"', 'ref="A1:EN17"', "制作中维度")
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    workbook = replace_once(workbook, "制作中!$A$2:$EN$15", "制作中!$A$2:$EN$17", "制作中数据范围")
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
        if [code for code, _number, _row in making_ids][:-2] != [code for code, _number, _row in ids_of(making_xml, shared)]:
            fail("制作中原有顺序被改动")
        if [code for code, _number, _row in making_ids][-2:] != [HEAT, METEOR]:
            fail("末两行不是热波和陨岩")
        if HEAT in [code for code, _number, _row in pending_ids] or METEOR in [code for code, _number, _row in pending_ids]:
            fail("待验收仍有这两项")
        outside_making = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml).replace('ref="A1:EN15"', 'ref="A1:EN17"')
        if outside_making != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_making):
            fail("制作中筛选或验证被额外改动")
        outside_pending = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_xml).replace('ref="A1:EN9"', 'ref="A1:EN7"')
        if outside_pending != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_pending):
            fail("待验收筛选或验证被额外改动")
        seen = []
        after = {"已完成": 40, "待制作": 0, "制作中": 15, "待验收": 5}
        for title in STATUS:
            page = [code for code, _number, _row in ids_of(dst.read(PARTS[title]).decode("utf-8"), shared)]
            if len(page) != after[title]:
                fail(f"{title} 计数 {len(page)}")
            seen.extend(page)
        if len(seen) != len(set(seen)) or len(seen) != 60:
            fail("四页总数或唯一性变化")

    cells = {}
    for code, (_sheet, number, row) in sources.items():
        cells[code] = [{"column": col_name(index), "header": header.get(col_name(index)), "text": originals[code][col_name(index)]} for index in range(1, 145)]
    aen_path = snap_dir / f"变更前-AEN-{stamp}-{sequence}.json"
    current_path = snap_dir / f"返修需求-{stamp}-{sequence}.md"
    aen_path.write_text(json.dumps({
        "baselineSha256": baseline, "columnCount": 144, "rows": {
            code: {"sheet": "待验收", "row": sources[code][1], "cells": cells[code]} for code in (HEAT, METEOR)
        },
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    current_path.write_text(
        "# 底座释放参数返修：当前完整需求\n\n"
        "供源码作者使用。只含 fire_shock_ring 与 fire_meteorite。其它术式不在本次返修范围。\n\n"
        "2026-11-02 用户明确“修”。已确认：批量路径丢落点中心；建帧份序号未继承；热波优先共享 anchor 导致重合。"
        "根因全文：docs/开发记录/底座释放同类排查-20261102/Cursor检查报告.md。本阶段未实现。\n\n"
        "## fire_shock_ring\n\n"
        "来源快照：docs/开发记录/热波正圆精修-20261101/快照/变更前-NODV-20261101-234901-1.json 的完整 N，"
        "以及同目录 批准要求与资源映射-20261101-234901-1.md 的 heat-circle-v2。不退回更早的环形描述。\n\n"
        "### 功能\n\n" + wanted[HEAT]["N"] + "\n\n"
        "### 视觉与资源\n\n" + wanted[HEAT]["O"] + "\n\n"
        "## fire_meteorite\n\n"
        "来源快照：docs/开发记录/陨岩三分落点返修-20261102/快照/待验收需求-20261102-030207-1.md 的转换前全文。"
        "功能是实体命中后沿当前弹道续飞，触地留下陨石块。外观保持 meteor-magma-crust-v5。\n\n"
        "### 功能\n\n" + wanted[METEOR]["N"] + "\n\n"
        "### 视觉与资源\n\n" + wanted[METEOR]["O"] + "\n",
        encoding="utf-8",
    )
    result = {
        "ready": True,
        "baselineSha256": baseline,
        "baselineMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "backup": str(backup),
        "candidate": str(candidate),
        "candidateSha256": sha256_file(candidate),
        "snapshots": {"aen": str(aen_path), "current": str(current_path)},
        "moves": {
            HEAT: {"from": {"sheet": "待验收", "row": 8}, "to": {"sheet": "制作中", "row": 16}},
            METEOR: {"from": {"sheet": "待验收", "row": 9}, "to": {"sheet": "制作中", "row": 17}},
        },
        "countsBefore": counts,
        "countsAfter": after,
        "live": str(LIVE),
        "affectedParts": list(changed),
        "requirementSources": {
            HEAT: str(HEAT_PRIOR),
            METEOR: str(METEOR_PRIOR),
        },
    }
    (BATCH / "sheet-return-prepare.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"baseline": baseline, "candidate": result["candidateSha256"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
