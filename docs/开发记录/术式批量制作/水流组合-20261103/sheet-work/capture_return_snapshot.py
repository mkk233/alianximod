# -*- coding: utf-8 -*-
"""Write the repair snapshot before any workbook candidate is created."""
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
PRIOR = WORK / "待验收转换前完整需求快照.json"
OUT = WORK / "运行故障-20261104返修快照.json"
BASELINE = "4FF10BA2F31A435318911777CA4C96D547AF1550DF7544BC9FB07E1CF25F9164"
CODE = "water_deep_pressure"

RESTORE_O = (
    "保留现有重压领域效果，增加真实水的正常水面/水下外观；与旋流、气泡形成组合时，水域内部可看见回旋和上升气泡。"
    "深海单独释放不自动获得另两术式的水龙卷外观。"
    "2026-11-03 已采用 v3.1，美术没有改版。"
    "历史草稿句「当前组合v3待确认」不再表示当前状态。"
)
APPEND = "\n".join([
    "2026-11-04 用户实测反馈：进入世界卡在准备生成区域 4%/5%。日志最后进度是「准备生成区域中：5%」，三次线程栈相同。",
    "明确根因：深海临时水 StillWaterLease 在区块加载回调里重新同步请求当前区块。ReleaseBaseEvents.onChunkLoad 第 191 行调用 StillWaterLease.onChunkLoad，LeaseData.reconcile 第 253 行进入 sweepOrphans，第 263 行 level.getBlockState 再进 ServerChunkCache.getChunk，对尚未完成的区块 future 再次 managedBlock。",
    "HttpConnectTimeoutException 是 Forge 版本检查线程访问 files.minecraftforge.net 超时，与这次停住不是同一条调用链。旋流和气泡不在栈上。出生区域加载就会进这个回调，不需要先施放术式。世界因此进不去，三项都还不能在这局里实测。待验收不能当成已经玩过。",
    "本行退回制作中，只修该临时水底层。美术没有改版。",
    "报告：docs/开发记录/术式批量制作/水流组合-20261103/运行故障-20261104/线程现场诊断.md",
    "制作中 | 水流组合-20261103 | docs/开发记录/术式批量制作/水流组合-20261103/记录.md",
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


def main():
    payload = LIVE.read_bytes()
    source_hash = hashlib.sha256(payload).hexdigest().upper()
    if source_hash != BASELINE:
        fail(f"Source hash is {source_hash}")
    prior = json.loads(PRIOR.read_text(encoding="utf-8"))
    approved = prior["rows"][CODE]
    with zipfile.ZipFile(LIVE) as book:
        shared = shared_items(book.read("xl/sharedStrings.xml").decode("utf-8"))
        accept = book.read("xl/worksheets/sheet4.xml").decode("utf-8")
        detail_xml = book.read("xl/worksheets/sheet6.xml").decode("utf-8")
        making = book.read("xl/worksheets/sheet3.xml").decode("utf-8")
    accept_rows = row_map(accept)
    located = []
    for number, row in accept_rows.items():
        if number >= 3 and cell_value(row, "B", number, shared) == CODE:
            located.append(number)
    making_hits = [
        number for number, row in row_map(making).items()
        if number >= 3 and cell_value(row, "B", number, shared) == CODE
    ]
    if located != [12] or making_hits:
        fail(f"Located acceptance {located}, making {making_hits}")
    number = located[0]
    row = accept_rows[number]
    headers = row_map(accept)[2]
    cells = {}
    for index in range(1, 145):
        column = col_letter(index)
        value = cell_value(row, column, number, shared)
        if value not in (None, ""):
            cells[column] = {
                "header": cell_value(headers, column, 2, shared),
                "value": value,
            }
    current = {column: item["value"] for column, item in cells.items()}
    if current["N"] != approved["after"]["N"] or current["O"] != approved["after"]["O"] or current["DV"] != approved["after"]["DV"]:
        fail("Live N/O/DV no longer match the acceptance snapshot")
    if approved["before"]["N"] != approved["fields"]["N"] or approved["before"]["O"] != approved["fields"]["O"]:
        fail("Prior snapshot before/fields disagree")
    details = []
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
    if len(details) != 2:
        fail(f"Expected 2 detail rows, found {len(details)}")
    restore_n = approved["before"]["N"]
    restore_dv = current["DV"] + "\n" + APPEND
    if "当前组合v3待确认" not in approved["before"]["O"]:
        fail("Historical unconfirmed sentence missing")
    if RESTORE_O.endswith("待确认。"):
        fail("Restored O still ends unconfirmed")
    document = {
        "writtenBeforeWorkbookEdit": True,
        "capturedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "sourceHash": source_hash,
        "sourceMtime": datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
        "sourceSize": len(payload),
        "approvedVersion": "最近完整需求是待验收转换前完整需求快照的 before.N/before.O。批准版本为需求快照.md与设计裁决-1.md，并合并 2026-11-03 用户对 v3.1 的明确采用。不从第一次原始档案或待验收一句话反推。",
        "art": "美术没有改版。v3.1 采用仍然有效。",
        "currentAcceptanceRow": {
            "sheet": "待验收",
            "row": number,
            "cells": cells,
        },
        "relatedDetails": {
            "policy": "资源状态明细按编号保留，不随主项复制，也不改明细。",
            "rows": details,
        },
        "originalNODV": {"N": current["N"], "O": current["O"], "DV": current["DV"]},
        "fullRequirementNODV": approved["before"],
        "restore": {"N": restore_n, "O": RESTORE_O, "DV": restore_dv},
        "plannedMove": {"from": "待验收第12行", "to": "制作中下一空行", "expectedCounts": "37/0/16/9"},
    }
    OUT.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    if hashlib.sha256(LIVE.read_bytes()).hexdigest().upper() != source_hash:
        fail("Workbook changed while writing snapshot")
    print("SNAPSHOT", OUT)
    print("DETAILS", [(item["row"], item["cells"].get("B"), item["cells"].get("C")) for item in details])


if __name__ == "__main__":
    main()
