# -*- coding: utf-8 -*-
"""Compare the preserved workbook with the original and the migration spec."""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path
from zipfile import ZipFile

from lxml import etree as ET

ROOT = Path(__file__).resolve().parent
SOURCE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
PRESERVED = ROOT / "candidate-preserved.xlsx"
SPEC = json.loads((ROOT / "migration-spec.json").read_text(encoding="utf-8"))
NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
OLD_NAMES = {
    "弹道": "数据表!$A$4:$A$8",
    "链上角色": "数据表!$E$4:$E$12",
    "释放种类": "数据表!$I$4:$I$6",
    "释放标签": "数据表!$M$4:$M$5",
    "术式类型": "数据表!$Q$4:$Q$7",
    "属性": "数据表!$U$4:$U$9",
    "外形": "数据表!$Y$4:$Y$5",
    "是否": "数据表!$AC$4:$AC$5",
    "领域边界": "数据表!$AG$4:$AG$5",
    "领域外观": "数据表!$AK$4:$AK$5",
    "曲线函数": "数据表!$AO$4:$AO$4",
    "附魔效果键": "数据表!$AS$4:$AS$8",
}


def q(tag: str) -> str:
    return f"{{{NS}}}{tag}"


def col_index(column: str) -> int:
    number = 0
    for char in column:
        number = number * 26 + ord(char) - 64
    return number


def column_of(ref: str) -> str:
    return re.sub(r"\d", "", ref)


def cell_text(cell):
    kind = cell.get("t")
    if kind == "inlineStr":
        return ("inlineStr", "".join(node.text or "" for node in cell.iter(q("t"))), cell.get("s"))
    value = cell.find(q("v"))
    return (kind, None if value is None else value.text, cell.get("s"))


def load(path: Path):
    archive = ZipFile(path)
    sheets = {}
    for name, filename in SPEC["sheetFiles"].items():
        root = ET.fromstring(archive.read(filename))
        cells = {}
        heights = {}
        for row in root.find(q("sheetData")):
            heights[row.get("r")] = (row.get("ht"), row.get("customHeight"))
            for cell in row:
                cells[cell.get("r")] = cell_text(cell)
        sheets[name] = {
            "root": root,
            "cells": cells,
            "heights": heights,
        }
    return archive, sheets


def main() -> None:
    problems = []

    def fail(message: str) -> None:
        problems.append(message)

    old_zip, old = load(SOURCE)
    new_zip, new = load(PRESERVED)
    prefix = {f"{column}2" for column in SPEC["prefixColumns"]}
    for sheet_name in SPEC["statusSheets"]:
        old_cells = old[sheet_name]["cells"]
        new_cells = new[sheet_name]["cells"]
        for ref, old_value in old_cells.items():
            if col_index(column_of(ref)) > 126:
                fail(f"unexpected old cell {sheet_name} {ref}")
                continue
            got = new_cells.get(ref)
            if ref in prefix:
                if got is None or got[0] != old_value[0] or got[2] != old_value[2] or got[1] != "旧档·" + old_value[1]:
                    fail(f"prefix {sheet_name} {ref} {old_value} -> {got}")
            elif got != old_value:
                fail(f"cell {sheet_name} {ref} {old_value} -> {got}")
        for ref in new_cells:
            if col_index(column_of(ref)) <= 126 and ref not in old_cells:
                fail(f"added old-range cell {sheet_name} {ref}")
        for row_number, height in old[sheet_name]["heights"].items():
            if new[sheet_name]["heights"].get(row_number) != height:
                fail(f"height {sheet_name} {row_number}")
        old_root = old[sheet_name]["root"]
        new_root = new[sheet_name]["root"]
        if ET.tostring(old_root.find(q("sheetViews"))) != ET.tostring(new_root.find(q("sheetViews"))):
            fail(f"freeze changed {sheet_name}")
        old_merges = [node.get("ref") for node in old_root.find(q("mergeCells"))]
        new_merges = [node.get("ref") for node in new_root.find(q("mergeCells"))]
        if old_merges != new_merges[: len(old_merges)] or new_merges[-1] != "DW1:EN1":
            fail(f"merges {sheet_name} {new_merges}")
        old_cols = [(node.get("min"), node.get("max"), node.get("width")) for node in old_root.find(q("cols"))]
        new_cols = [(node.get("min"), node.get("max"), node.get("width")) for node in new_root.find(q("cols"))]
        if new_cols[: len(old_cols)] != old_cols:
            fail(f"old widths changed {sheet_name}")
        old_rules = [(node.get("sqref"), node.find(q("formula1")).text) for node in old_root.find(q("dataValidations"))]
        new_rules = [(node.get("sqref"), node.find(q("formula1")).text) for node in new_root.find(q("dataValidations"))]
        if new_rules[: len(old_rules)] != old_rules or len(old_rules) != 38:
            fail(f"old validations changed {sheet_name} {len(old_rules)}")
        expected_rules = [(f"{item['col']}3:{item['col']}1000", item["formula"]) for item in SPEC["validations"]]
        if new_rules[len(old_rules):] != expected_rules:
            fail(f"new validations {sheet_name} {new_rules[len(old_rules):]}")
        if new_root.find(q("dimension")).get("ref") != SPEC["dimensionRef"][sheet_name]:
            fail(f"dimension {sheet_name}")
        if new_root.find(q("autoFilter")).get("ref") != SPEC["filterRef"][sheet_name]:
            fail(f"filter {sheet_name}")
        legacy = new_root.find(q("legacyDrawing"))
        if legacy.get("{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id") != "anysvml":
            fail(f"comment anchor {sheet_name}")

    old_catalog = old["数据表"]["cells"]
    new_catalog = new["数据表"]["cells"]
    for ref, old_value in old_catalog.items():
        if col_index(column_of(ref)) > 47:
            fail(f"catalog beyond AU {ref}")
        if new_catalog.get(ref) != old_value:
            fail(f"catalog cell {ref}")
    for row_number, height in old["数据表"]["heights"].items():
        if new["数据表"]["heights"].get(row_number) != height:
            fail(f"catalog height {row_number}")
    if ET.tostring(old["数据表"]["root"].find(q("sheetViews"))) != ET.tostring(new["数据表"]["root"].find(q("sheetViews"))):
        fail("catalog freeze changed")

    ids = []
    for sheet_name in SPEC["statusSheets"]:
        for ref, value in new[sheet_name]["cells"].items():
            if column_of(ref) == "B" and ref != "B1" and ref != "B2" and value[1]:
                ids.append((sheet_name, ref, value[1]))
    id_values = [item[2] for item in ids]
    counts = {name: sum(1 for sheet, _, _ in ids if sheet == name) for name in SPEC["statusSheets"]}
    if counts != {"已完成": 40, "待制作": 3, "制作中": 0, "待验收": 0} or len(set(id_values)) != 43:
        fail(f"id census {counts} {len(id_values)}")

    for entry in SPEC["mainRows"]:
        cells = new[entry["sheet"]]["cells"]
        if cells.get(f"B{entry['row']}")[1] != entry["id"]:
            fail(f"main id {entry}")
        for column in SPEC["newColumns"]:
            got = cells.get(f"{column}{entry['row']}")
            expected = entry["values"].get(column)
            if expected is None:
                if got is not None:
                    fail(f"extra {entry['id']} {column} {got}")
            elif isinstance(expected, (int, float)):
                if got != ("n", str(int(expected)), "15" if column not in SPEC["leftColumns"] else "16"):
                    fail(f"number {entry['id']} {column} {got}")
            else:
                style = "16" if column in SPEC["leftColumns"] else "15"
                if got != ("inlineStr", expected, style):
                    fail(f"text {entry['id']} {column} {got}")
        resource = entry["values"].get("DX")
        state = entry["values"].get("DY")
        if bool(resource) == bool(state):
            fail(f"resource/state {entry['id']}")

    configured_rows = {(entry["sheet"], entry["row"]) for entry in SPEC["mainRows"]}
    for sheet_name in SPEC["statusSheets"]:
        for ref in new[sheet_name]["cells"]:
            column = column_of(ref)
            if column not in SPEC["newColumns"]:
                continue
            row_number = int(re.sub(r"\D", "", ref))
            if row_number <= 2:
                continue
            if (sheet_name, row_number) not in configured_rows:
                fail(f"unexpected config {sheet_name} {ref}")

    detail_root = ET.fromstring(new_zip.read("xl/worksheets/sheet6.xml"))
    detail_cells = {}
    for row in detail_root.find(q("sheetData")):
        for cell in row:
            detail_cells[cell.get("r")] = cell_text(cell)
    for entry in SPEC["detailRows"]:
        for column in SPEC["detailColumns"]:
            got = detail_cells.get(f"{column}{entry['row']}")
            expected = entry["values"].get(column)
            if column == "T" and got is not None:
                fail("detail T not empty")
            if expected is None:
                if got is not None and column != "T":
                    fail(f"detail extra {entry['row']} {column}")
            elif isinstance(expected, (int, float)):
                style = "16" if column in SPEC["detailLeft"] else "15"
                if got != ("n", str(int(expected)), style):
                    fail(f"detail number {entry['row']} {column} {got}")
            else:
                style = "16" if column in SPEC["detailLeft"] else "15"
                if column in ("A", "B"):
                    style = "15"
                if got != ("inlineStr", expected, style):
                    fail(f"detail text {entry['row']} {column} {got}")
    detail_rules = [(node.get("sqref"), node.find(q("formula1")).text) for node in detail_root.find(q("dataValidations"))]
    expected_detail_rules = [(f"{item['col']}3:{item['col']}1000", item["formula"]) for item in SPEC["detailValidations"]]
    if detail_rules != expected_detail_rules:
        fail(f"detail validations {detail_rules}")

    workbook = new_zip.read("xl/workbook.xml").decode("utf-8")
    old_workbook = old_zip.read("xl/workbook.xml").decode("utf-8")
    for name, formula in OLD_NAMES.items():
        token = f'<definedName name="{name}">{formula}</definedName>'
        if token not in workbook or token not in old_workbook:
            fail(f"old name {name}")
    for item in SPEC["lists"]:
        formula = (
            f"'数据表'!${item['col']}$4:INDEX('数据表'!${item['col']}$4:${item['col']}$1000,"
            f"COUNTA('数据表'!${item['col']}$4:${item['col']}$1000))"
        )
        if f'<definedName name="{item["name"]}">{formula}</definedName>' not in workbook:
            fail(f"new name {item['name']}")
    if "资源状态明细" not in workbook or "rId8" not in workbook:
        fail("detail sheet missing from workbook")

    unchanged = [
        "xl/comments/comment1.xml", "xl/comments/comment2.xml", "xl/comments/comment3.xml", "xl/comments/comment4.xml",
        "xl/drawings/commentsDrawing1.vml", "xl/drawings/commentsDrawing2.vml",
        "xl/drawings/commentsDrawing3.vml", "xl/drawings/commentsDrawing4.vml",
        "xl/worksheets/_rels/sheet1.xml.rels", "xl/worksheets/_rels/sheet2.xml.rels",
        "xl/worksheets/_rels/sheet3.xml.rels", "xl/worksheets/_rels/sheet4.xml.rels",
        "xl/theme/theme1.xml", "docProps/core.xml", "docProps/app.xml",
    ]
    for name in unchanged:
        if old_zip.read(name) != new_zip.read(name):
            fail(f"part changed {name}")

    kept = ["AS", "BW", "U", "N", "O", "DV", "AR", "BQ", "BR", "DM"]
    for sheet_name in SPEC["statusSheets"]:
        for column in kept:
            for ref, old_value in old[sheet_name]["cells"].items():
                if column_of(ref) == column and new[sheet_name]["cells"].get(ref) != old_value and ref not in prefix:
                    fail(f"kept column {sheet_name} {ref}")

    print(json.dumps({"problems": problems, "idCounts": counts, "ids": len(id_values)}, ensure_ascii=False))
    if problems:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
