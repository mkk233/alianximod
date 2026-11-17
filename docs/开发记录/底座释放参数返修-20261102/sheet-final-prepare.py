# -*- coding: utf-8 -*-
"""Move fire_shock_ring and fire_meteorite from 制作中 back to 待验收."""
import hashlib
import importlib.util
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

BATCH = Path(r"D:\alianximod\docs\开发记录\底座释放参数返修-20261102")
SPEC = importlib.util.spec_from_file_location("sheet_return", BATCH / "sheet-return-prepare.py")
base = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(base)

HEAT = "fire_shock_ring"
METEOR = "fire_meteorite"
PRIOR_AEN = BATCH / "快照" / "变更前-AEN-20261102-034454-1.json"
COMPILE = json.loads((BATCH / "compile-result.json").read_text(encoding="utf-8"))
IMPLEMENTATION = json.loads((BATCH / "implementation-result.json").read_text(encoding="utf-8"))
DV_APPEND = (
    "2026-11-02 底座释放参数返修已实现。父段没有接触点、也没有地表冻结点时，几何上下文只传给选点中心和锚点。"
    "份序号在建帧前写入。热波按各份 origin。强制接触点和地表冻结原点仍优先。没有新增平衡键。"
    ".\\gradlew.bat compileJava --offline --no-daemon 于 2026-11-02 03:55:41.329–03:56:10.197 退出码 0，compileJava 实际执行。"
    "待游戏验收。本阶段额度已用完，返修结果未追加复审。\n"
    "待验收 | 底座释放参数返修-20261102 | docs/开发记录/底座释放参数返修-20261102/compile-report.md"
)


def fail(message):
    raise SystemExit(message)


def cell_map(row_entry):
    return {item["column"]: item["text"] for item in row_entry["cells"]}


def gate():
    if IMPLEMENTATION.get("blockers") != [] or IMPLEMENTATION.get("writes_stopped") is not True:
        fail("实现交付仍有阻塞或尚未停写")
    if COMPILE.get("status") != "compile_successful" or COMPILE.get("exitCode") != 0:
        fail("本轮编译未成功")
    if COMPILE.get("compileJavaOutcome") != "EXECUTED" or COMPILE.get("source_unchanged") is not True:
        fail("本轮 compileJava 不是实际执行或输入不稳定")
    if COMPILE.get("blockers") != [] or COMPILE.get("writes_stopped") is not True:
        fail("编译结果仍有阻塞")
    if COMPILE.get("startedAt") != "2026-11-02T03:55:41.3293566+08:00":
        fail("编译时间与本轮证据不一致")
    if not COMPILE.get("compile_started_after_last_write"):
        fail("编译早于最后一次源码写入")


def main():
    gate()
    prior = json.loads(PRIOR_AEN.read_text(encoding="utf-8"))
    display = {code: cell_map(prior["rows"][code]) for code in (HEAT, METEOR)}
    full = base.requirements()
    for code, version in ((HEAT, "heat-circle-v2"), (METEOR, "meteor-magma-crust-v5")):
        if display[code]["N"] != full[code]["shortN"] or version not in (display[code]["O"] or ""):
            fail(code + " 的原待验收展示值不匹配")
        if display[code]["O"] not in full[code]["O"]:
            fail(code + " 的资源映射不在现行特效里")
    live_bytes = base.LIVE.read_bytes()
    baseline = hashlib.sha256(live_bytes).hexdigest().upper()
    stat = base.LIVE.stat()
    with zipfile.ZipFile(base.LIVE) as archive:
        order = archive.infolist()
        parts = {info.filename: archive.read(info.filename) for info in order}
    shared = base.shared_list(parts["xl/sharedStrings.xml"])
    making_xml = parts[base.PARTS["制作中"]].decode("utf-8")
    pending_xml = parts[base.PARTS["待验收"]].decode("utf-8")
    counts = {}
    located = {}
    seen = []
    for title in base.STATUS:
        page = base.ids_of(parts[base.PARTS[title]].decode("utf-8"), shared)
        counts[title] = len(page)
        for code, number, row in page:
            located.setdefault(code, []).append((title, number, row))
            seen.append(code)
    if counts != {"已完成": 40, "待制作": 0, "制作中": 15, "待验收": 5} or len(seen) != len(set(seen)):
        fail("四页计数或唯一性异常 " + str(counts))
    making_numbers = [base.row_number(row) for row in base.rows_of(making_xml)]
    pending_numbers = [base.row_number(row) for row in base.rows_of(pending_xml)]
    if making_numbers != list(range(1, 18)) or pending_numbers != list(range(1, 8)):
        fail("行号不是连续末行")
    sources = {}
    expected_rows = {HEAT: 16, METEOR: 17}
    for code, expected in expected_rows.items():
        hits = located.get(code, [])
        if len(hits) != 1 or hits[0][0] != "制作中" or hits[0][1] != expected:
            fail(code + " 不在制作中预期行")
        sources[code] = hits[0]
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    if workbook.count("制作中!$A$2:$EN$17") != 1 or 'ref="A1:EN17"' not in making_xml or 'ref="A1:EN7"' not in pending_xml:
        fail("范围引用已变化")
    if 'ref="A2:EN5"' not in making_xml or 'ref="A2:EN2"' not in pending_xml:
        fail("筛选范围已变化")
    originals = {}
    for code, (_sheet, number, row) in sources.items():
        current = base.column_values(row, number, shared)
        if current["N"] != full[code]["N"] or current["O"] != full[code]["O"]:
            fail(code + " 的现行需求已变，先对账")
        if not (current["DV"] or "").startswith(display[code]["DV"] + "\n"):
            fail(code + " 的历史备注不完整")
        if "03:55:41.329" in (current["DV"] or ""):
            fail(code + " 已经写过本轮编译")
        originals[code] = current

    targets = {HEAT: 8, METEOR: 9}
    moved = {}
    for code, target in targets.items():
        _sheet, number, row = sources[code]
        updated = base.renumber_row(row, number, target)
        updated = base.replace_text_cell(updated, "N", target, display[code]["N"])
        updated = base.replace_text_cell(updated, "O", target, display[code]["O"])
        updated = base.replace_text_cell(updated, "DV", target, originals[code]["DV"] + "\n" + DV_APPEND)
        for index in range(1, 145):
            column = base.col_name(index)
            if column in ("N", "O", "DV"):
                continue
            if base.read_at(updated, column, target, shared) != originals[code][column]:
                fail(code + " 改到了 " + column)
        note = base.read_at(updated, "DV", target, shared)
        if not note.startswith(originals[code]["DV"] + "\n") or "份序号在建帧前写入" not in note:
            fail(code + " 备注追加失败")
        moved[code] = updated

    making_rows = [row for row in base.rows_of(making_xml) if base.row_number(row) not in (16, 17)]
    making_out = base.replace_data(making_xml, making_rows)
    making_out = base.replace_once(making_out, 'ref="A1:EN17"', 'ref="A1:EN15"', "制作中维度")
    pending_out = base.replace_data(pending_xml, base.rows_of(pending_xml) + [moved[HEAT], moved[METEOR]])
    pending_out = base.replace_once(pending_out, 'ref="A1:EN7"', 'ref="A1:EN9"', "待验收维度")
    workbook_out = base.replace_once(workbook, "制作中!$A$2:$EN$17", "制作中!$A$2:$EN$15", "制作中末行")
    if 'ref="A2:EN5"' not in making_out or "待验收!$A$2:$EN$2" not in workbook_out:
        fail("筛选被改动")
    ET.fromstring(making_out.encode("utf-8"))
    ET.fromstring(pending_out.encode("utf-8"))
    ET.fromstring(workbook_out.encode("utf-8"))
    changed = {
        base.PARTS["制作中"]: making_out.encode("utf-8"),
        base.PARTS["待验收"]: pending_out.encode("utf-8"),
        "xl/workbook.xml": workbook_out.encode("utf-8"),
    }
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    sequence = 1
    backup_dir = BATCH / "备份"
    candidate_dir = BATCH / "候选"
    snap_dir = BATCH / "快照"
    backup_dir.mkdir(exist_ok=True)
    candidate_dir.mkdir(exist_ok=True)
    snap_dir.mkdir(exist_ok=True)
    while (backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx").exists():
        sequence += 1
    backup = backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx"
    candidate = candidate_dir / f"术式制作表-候选-{stamp}-{sequence}.xlsx"
    backup.write_bytes(live_bytes)
    with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
    with zipfile.ZipFile(candidate) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if set(different) != set(changed):
            fail("候选改到了意外部件")
        for title in ("已完成", "待制作", "数据表", "资源状态明细", "待制作原始数据"):
            if dst.read(base.PARTS[title]) != parts[base.PARTS[title]]:
                fail(title + " 被改动")
        check_making = dst.read(base.PARTS["制作中"]).decode("utf-8")
        check_pending = dst.read(base.PARTS["待验收"]).decode("utf-8")
        if [base.row_number(row) for row in base.rows_of(check_making)] != list(range(1, 16)):
            fail("制作中删行后行号变化")
        pending_ids = base.ids_of(check_pending, shared)
        if [code for code, _number, _row in pending_ids][-2:] != [HEAT, METEOR]:
            fail("待验收末两行不对")
        if HEAT in [code for code, _number, _row in base.ids_of(check_making, shared)]:
            fail("制作中仍有热波")
        outside_making = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml).replace('ref="A1:EN17"', 'ref="A1:EN15"')
        if outside_making != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_making):
            fail("制作中筛选或验证被额外改动")
        outside_pending = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_xml).replace('ref="A1:EN7"', 'ref="A1:EN9"')
        if outside_pending != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_pending):
            fail("待验收筛选或验证被额外改动")
        after_seen = []
        after = {"已完成": 40, "待制作": 0, "制作中": 13, "待验收": 7}
        for title in base.STATUS:
            page = [code for code, _number, _row in base.ids_of(dst.read(base.PARTS[title]).decode("utf-8"), shared)]
            if len(page) != after[title]:
                fail(title + " 计数错误")
            after_seen.extend(page)
        if len(after_seen) != 60 or len(set(after_seen)) != 60:
            fail("总数或唯一性变化")

    header = {}
    for row in base.rows_of(making_xml):
        if base.row_number(row) == 2:
            header = base.column_values(row, 2, shared)
    aen_path = snap_dir / f"变更前-AEN-{stamp}-{sequence}.json"
    nodv_path = snap_dir / f"待验收前-NODV-{stamp}-{sequence}.json"
    aen_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "columnCount": 144,
        "rows": {
            code: {
                "sheet": "制作中",
                "row": sources[code][1],
                "cells": [
                    {"column": base.col_name(index), "header": header.get(base.col_name(index)), "text": originals[code][base.col_name(index)]}
                    for index in range(1, 145)
                ],
            }
            for code in (HEAT, METEOR)
        },
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    nodv_path.write_text(json.dumps({
        "userRepair": "2026-11-02 用户明确修",
        "approvedResources": {HEAT: "heat-circle-v2", METEOR: "meteor-magma-crust-v5"},
        "rows": {
            code: {
                "currentN": originals[code]["N"],
                "currentO": originals[code]["O"],
                "currentDV": originals[code]["DV"],
                "pendingDisplayN": display[code]["N"],
                "pendingDisplayO": display[code]["O"],
            }
            for code in (HEAT, METEOR)
        },
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    result = {
        "ready": True,
        "baselineSha256": baseline,
        "baselineMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "backup": str(backup),
        "candidate": str(candidate),
        "candidateSha256": base.sha256_file(candidate),
        "snapshots": {"aen": str(aen_path), "nodv": str(nodv_path)},
        "moves": {
            HEAT: {"from": {"sheet": "制作中", "row": 16}, "to": {"sheet": "待验收", "row": 8}},
            METEOR: {"from": {"sheet": "制作中", "row": 17}, "to": {"sheet": "待验收", "row": 9}},
        },
        "countsBefore": counts,
        "countsAfter": after,
        "live": str(base.LIVE),
        "affectedParts": list(changed),
        "display": {code: {"N": display[code]["N"], "O": display[code]["O"]} for code in (HEAT, METEOR)},
    }
    (BATCH / "sheet-final-prepare.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"baseline": baseline, "candidate": result["candidateSha256"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
