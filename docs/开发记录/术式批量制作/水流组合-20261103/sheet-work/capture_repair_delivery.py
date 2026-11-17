# -*- coding: utf-8 -*-
"""Save the delivery snapshot before creating a workbook candidate."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

ROOT = Path(r"D:\alianximod")
LIVE = ROOT / "docs" / "术式制作表.xlsx"
WORK = ROOT / "docs" / "开发记录" / "术式批量制作" / "水流组合-20261103" / "sheet-work"
OUT = WORK / "运行故障-20261104待验收快照.json"
BASELINE = "37DC28E3671359455DE1504F96CE10344920D256E8BD971D4350A76888615591"
CODE = "water_deep_pressure"
NEW_N = "低矮的重压领域。范围内暂时积起不流动的水，里面的人变慢、变潮湿，氧气消耗加快"
NEW_O = "assets/xueximod/textures/domain/deep_pressure_bands.png；assets/xueximod/textures/domain/deep_pressure_arcs.png；原版 minecraft:block/water_still、minecraft:block/water_flow、minecraft:block/water_overlay"
APPEND = "\n".join([
    "2026-11-04 区块加载修复已写入。StillWaterLease 的加载回调改为使用本次 ChunkEvent.Load 给出的 ChunkAccess 读方块，回调内不再调用 Level.getBlockState / getChunk。需要改世界的格子改在回调结束后的队列里处理。",
    "修复报告：docs/开发记录/术式批量制作/水流组合-20261103/运行故障-20261104/修复报告.md。该报告只改这一处加载读取，未改功能文案，也未改美术。",
    "最终 compileJava --offline --no-daemon 于 2026-11-04 01:31:43 至 01:32:26 退出码 0，见 docs/开发记录/术式批量制作/水流组合-20261103/运行故障-20261104/编译回执.md。",
    "仍待用户重新进世界并实测术式。",
    "本阶段额度已用完，返修结果未追加复审。",
    "待验收 | 水流组合-20261103 | docs/开发记录/术式批量制作/水流组合-20261103/记录.md",
])


def fail(message):
    raise SystemExit(message)


def col_letter(number):
    letters = ""
    while number:
        number, remainder = divmod(number - 1, 26)
        letters = chr(65 + remainder) + letters
    return letters


def shared_items(xml):
    items = []
    for body in re.findall(r"<si\b[^>]*>(.*?)</si>", xml, re.S):
        parts = re.findall(r"<t\b[^>]*>(.*?)</t>", body, re.S)
        items.append(html.unescape("".join(parts)))
    return items


def row_map(xml):
    found = {}
    for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', xml):
        found[int(match.group(1))] = match.group(0)
    return found


def cell_value(row, column, number, shared):
    match = re.search(rf'<c r="{column}{number}\b"[^>]*(?:/>|>[\s\S]*?</c>)', row)
    if not match:
        return None
    cell = match.group(0)
    if cell.endswith("/>"):
        return ""
    if 't="inlineStr"' in cell or "<is>" in cell:
        return html.unescape("".join(re.findall(r"<t\b[^>]*>(.*?)</t>", cell, re.S)))
    raw = re.search(r"<v>(.*?)</v>", cell, re.S)
    if not raw:
        return ""
    if 't="s"' in cell:
        return shared[int(raw.group(1))]
    return raw.group(1)


def sheet_names(workbook_xml, rels_xml):
    rels = {}
    for match in re.finditer(r'Id="([^"]+)"[^>]*Target="([^"]+)"', rels_xml):
        rels[match.group(1)] = match.group(2)
    names = {}
    for match in re.finditer(r'<sheet[^>]*name="([^"]+)"[^>]*r:id="([^"]+)"', workbook_xml):
        target = rels[match.group(2)]
        if not target.startswith("xl/"):
            target = "xl/" + target.lstrip("/")
        names[match.group(1)] = target.replace("\\", "/")
    return names


def main():
    payload = LIVE.read_bytes()
    source_hash = hashlib.sha256(payload).hexdigest().upper()
    if source_hash != BASELINE:
        fail(f"Source hash is {source_hash}")
    with zipfile.ZipFile(LIVE) as book:
        stored = {name: book.read(name) for name in book.namelist()}
    names = sheet_names(
        stored["xl/workbook.xml"].decode("utf-8"),
        stored["xl/_rels/workbook.xml.rels"].decode("utf-8"),
    )
    shared = shared_items(stored["xl/sharedStrings.xml"].decode("utf-8"))
    located = []
    for title in ("已完成", "待制作", "制作中", "待验收"):
        for number, row in row_map(stored[names[title]].decode("utf-8")).items():
            if number >= 3 and cell_value(row, "B", number, shared) == CODE:
                located.append((title, number))
    if located != [("制作中", 18)]:
        fail(f"Located {located}")
    sheet, number = located[0]
    xml = stored[names[sheet]].decode("utf-8")
    rows = row_map(xml)
    row = rows[number]
    headers = rows[2]
    cells = {}
    for index in range(1, 145):
        column = col_letter(index)
        value = cell_value(row, column, number, shared)
        if value not in (None, ""):
            cells[column] = {"header": cell_value(headers, column, 2, shared), "value": value}
    current = {column: item["value"] for column, item in cells.items()}
    if "StillWaterLease" not in current["DV"] or not current["N"].startswith("深海重压保持现有稳定编号"):
        fail("Current row is not the repaired making-stage text")
    details = []
    detail_xml = stored[names["资源状态明细"]].decode("utf-8")
    for detail_number, detail_row in row_map(detail_xml).items():
        if cell_value(detail_row, "A", detail_number, shared) != CODE:
            continue
        detail_cells = {}
        for index in range(1, 40):
            column = col_letter(index)
            value = cell_value(detail_row, column, detail_number, shared)
            if value not in (None, ""):
                detail_cells[column] = value
        details.append({"row": detail_number, "cells": detail_cells})
    if [(item["row"], item["cells"].get("B")) for item in details] != [(4, "2"), (5, "3")]:
        fail(f"Details changed {details}")
    document = {
        "writtenBeforeWorkbookEdit": True,
        "capturedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "sourceHash": source_hash,
        "sourceMtime": datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
        "sourceSize": len(payload),
        "approvedVersion": "原需求仍是待验收转换前完整需求快照与返修快照里的完整 N。v3.1 采用不变。不用待制作原始档案覆盖现行设计。本次待验收 N/O 只恢复上次游戏内简句和资源路径。",
        "fault": "2026-11-04 用户进世界卡在准备生成区域 4%/5%。三次栈停在 StillWaterLease 区块加载回调重新请求当前区块。HTTP 版本检查超时是旁支。",
        "repair": "修复报告只改 StillWaterLease.java。加载回调改用 ChunkAccess 读方块，改世界的格子延后到回调结束。未改功能文案，未改美术。报告文末当时尚未编译；随后的编译回执已记录通过。",
        "compile": "gradlew.bat compileJava --offline --no-daemon，2026-11-04 01:31:43 至 01:32:26，退出码 0。不是游戏验收，也不是追加正式复审。",
        "currentRow": {"sheet": sheet, "row": number, "cells": cells},
        "relatedDetails": {"policy": "资源状态明细不随主项复制，也不改明细。", "rows": details},
        "originalNODV": {"N": current["N"], "O": current["O"], "DV": current["DV"]},
        "after": {"N": NEW_N, "O": NEW_O, "DV": current["DV"] + "\n" + APPEND},
    }
    OUT.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    if hashlib.sha256(LIVE.read_bytes()).hexdigest().upper() != source_hash:
        fail("Workbook changed while writing snapshot")
    print("SNAPSHOT", OUT)


if __name__ == "__main__":
    main()
