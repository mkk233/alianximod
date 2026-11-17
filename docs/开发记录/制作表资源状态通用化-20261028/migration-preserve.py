# -*- coding: utf-8 -*-
"""Graft the authored migration onto the original package.

Artifact Tool export normalizes old styles. This step keeps original parts and
writes only the new structure, prefixed headers, and mapped cells.
"""
from __future__ import annotations

import hashlib
import json
import re
import shutil
from datetime import datetime
from pathlib import Path
from zipfile import ZipFile, ZipInfo

from lxml import etree as ET

ROOT = Path(__file__).resolve().parent
SOURCE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
SPEC = json.loads((ROOT / "migration-spec.json").read_text(encoding="utf-8"))
BASELINE = "486D6229C56EB22CEA9BA2BE02E273640BB5F16D77F4E91CDDE34024F30D7A50"
NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
RNS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
XML = "http://www.w3.org/XML/1998/namespace"
ET.register_namespace("r", RNS)


def q(tag: str) -> str:
    return f"{{{NS}}}{tag}"


def col_index(column: str) -> int:
    number = 0
    for char in column:
        number = number * 26 + ord(char) - 64
    return number


def col_name(number: int) -> str:
    letters = ""
    while number:
        number, remainder = divmod(number - 1, 26)
        letters = chr(65 + remainder) + letters
    return letters


def column_of(ref: str) -> str:
    return re.sub(r"\d", "", ref)


def cell_text(cell) -> tuple:
    kind = cell.get("t")
    if kind == "inlineStr":
        return ("inlineStr", "".join(node.text or "" for node in cell.iter(q("t"))), cell.get("s"))
    value = cell.find(q("v"))
    return (kind, None if value is None else value.text, cell.get("s"))


def make_inline(ref: str, style: str, text: str):
    cell = ET.Element(q("c"))
    cell.set("r", ref)
    cell.set("s", style)
    cell.set("t", "inlineStr")
    inline = ET.SubElement(cell, q("is"))
    node = ET.SubElement(inline, q("t"))
    if text[:1].isspace() or text[-1:].isspace():
        node.set(f"{{{XML}}}space", "preserve")
    node.text = text
    return cell


def make_number(ref: str, style: str, value):
    cell = ET.Element(q("c"))
    cell.set("r", ref)
    cell.set("s", style)
    cell.set("t", "n")
    node = ET.SubElement(cell, q("v"))
    if isinstance(value, float) and value.is_integer():
        node.text = str(int(value))
    else:
        node.text = str(int(value) if isinstance(value, int) else value)
    return cell


def insert_cell(row, cell) -> None:
    target = col_index(column_of(cell.get("r")))
    for index, existing in enumerate(list(row)):
        if col_index(column_of(existing.get("r"))) > target:
            row.insert(index, cell)
            return
    row.append(cell)


def rows_by_number(sheet_data):
    return {int(row.get("r")): row for row in sheet_data}


def add_value(row, column: str, row_number: int, value, center_style: str, left_style: str, left_columns: set[str]) -> None:
    style = left_style if column in left_columns else center_style
    ref = f"{column}{row_number}"
    if isinstance(value, bool) or value is None:
        raise ValueError(ref)
    cell = make_number(ref, style, value) if isinstance(value, (int, float)) else make_inline(ref, style, value)
    insert_cell(row, cell)


def name_formula(column: str) -> str:
    return (
        f"'数据表'!${column}$4:INDEX('数据表'!${column}$4:${column}$1000,"
        f"COUNTA('数据表'!${column}$4:${column}$1000))"
    )


def validation(column: str, title: str, formula: str, first: int = 3, last: int = 1000):
    node = ET.Element(q("dataValidation"))
    node.set("sqref", f"{column}{first}:{column}{last}")
    node.set("showDropDown", "0")
    node.set("showInputMessage", "0")
    node.set("showErrorMessage", "0")
    node.set("allowBlank", "1")
    node.set("errorTitle", title)
    node.set("error", "请从数据表的列表里选")
    node.set("promptTitle", "从数据表选择")
    node.set("prompt", title)
    node.set("type", "list")
    formula_node = ET.SubElement(node, q("formula1"))
    formula_node.text = formula
    return node


def snapshot(path: Path) -> dict:
    book = {}
    with ZipFile(path) as archive:
        for name, filename in SPEC["sheetFiles"].items():
            root = ET.fromstring(archive.read(filename))
            cells = {}
            for row in root.find(q("sheetData")):
                for cell in row:
                    cells[cell.get("r")] = cell_text(cell)
            book[name] = cells
    return book


def build_detail() -> bytes:
    root = ET.Element(q("worksheet"))
    sheet_pr = ET.SubElement(root, q("sheetPr"))
    ET.SubElement(sheet_pr, q("tabColor")).set("rgb", "000F6E6B")
    outline = ET.SubElement(sheet_pr, q("outlinePr"))
    outline.set("summaryBelow", "1")
    outline.set("summaryRight", "1")
    ET.SubElement(sheet_pr, q("pageSetUpPr"))
    ET.SubElement(root, q("dimension")).set("ref", "A1:T6")
    views = ET.SubElement(root, q("sheetViews"))
    view = ET.SubElement(views, q("sheetView"))
    view.set("showGridLines", "0")
    view.set("workbookViewId", "0")
    pane = ET.SubElement(view, q("pane"))
    pane.set("xSplit", "2")
    pane.set("ySplit", "2")
    pane.set("topLeftCell", "C3")
    pane.set("activePane", "bottomRight")
    pane.set("state", "frozen")
    for pane_name in ("topRight", "bottomLeft", "bottomRight"):
        selection = ET.SubElement(view, q("selection"))
        selection.set("pane", pane_name)
        selection.set("activeCell", "A1")
        selection.set("sqref", "A1")
    ET.SubElement(root, q("sheetFormatPr")).set("baseColWidth", "8")
    root.find(q("sheetFormatPr")).set("defaultRowHeight", "15")
    cols = ET.SubElement(root, q("cols"))
    for column, width in SPEC["detailWidths"].items():
        node = ET.SubElement(cols, q("col"))
        node.set("width", str(width))
        node.set("customWidth", "1")
        number = str(col_index(column))
        node.set("min", number)
        node.set("max", number)
    data = ET.SubElement(root, q("sheetData"))
    row1 = ET.SubElement(data, q("row"))
    row1.set("r", "1")
    row1.set("ht", "22")
    row1.set("customHeight", "1")
    insert_cell(row1, make_inline("A1", "22", "资源状态明细"))
    row2 = ET.SubElement(data, q("row"))
    row2.set("r", "2")
    row2.set("ht", "36")
    row2.set("customHeight", "1")
    for column, header in zip(SPEC["detailColumns"], SPEC["detailHeaders"]):
        insert_cell(row2, make_inline(f"{column}2", "23", header))
    left = set(SPEC["detailLeft"])
    for entry in SPEC["detailRows"]:
        row = ET.SubElement(data, q("row"))
        row.set("r", str(entry["row"]))
        row.set("ht", "32")
        row.set("customHeight", "1")
        for column in SPEC["detailColumns"]:
            if column not in entry["values"]:
                continue
            add_value(row, column, entry["row"], entry["values"][column], "15", "16", left)
    ET.SubElement(root, q("autoFilter")).set("ref", "A2:T6")
    merges = ET.SubElement(root, q("mergeCells"))
    merges.set("count", "1")
    ET.SubElement(merges, q("mergeCell")).set("ref", "A1:T1")
    validations = ET.SubElement(root, q("dataValidations"))
    validations.set("count", str(len(SPEC["detailValidations"])))
    for entry in SPEC["detailValidations"]:
        validations.append(validation(entry["col"], entry["title"], entry["formula"]))
    margins = ET.SubElement(root, q("pageMargins"))
    for key, value in (("left", "0.75"), ("right", "0.75"), ("top", "1"), ("bottom", "1"), ("header", "0.5"), ("footer", "0.5")):
        margins.set(key, value)
    return ET.tostring(root, xml_declaration=False, encoding="UTF-8")


def update_status_sheet(original: bytes) -> bytes:
    root = ET.fromstring(original)
    root.find(q("dimension")).set("ref", None)
    return root


def patch_status(original: bytes, sheet_name: str) -> bytes:
    root = ET.fromstring(original)
    root.find(q("dimension")).set("ref", SPEC["dimensionRef"][sheet_name])
    root.find(q("autoFilter")).set("ref", SPEC["filterRef"][sheet_name])
    cols = root.find(q("cols"))
    for column, width in SPEC["columnWidths"].items():
        node = ET.SubElement(cols, q("col"))
        node.set("width", str(width))
        node.set("customWidth", "1")
        number = str(col_index(column))
        node.set("min", number)
        node.set("max", number)
    data = root.find(q("sheetData"))
    rows = rows_by_number(data)
    insert_cell(rows[1], make_inline("DW1", "22", SPEC["groupTitle"]))
    for column, header in zip(SPEC["newColumns"], SPEC["newHeaders"]):
        insert_cell(rows[2], make_inline(f"{column}2", "23", header))
    for column in SPEC["prefixColumns"]:
        cell = next(item for item in rows[2] if item.get("r") == f"{column}2")
        texts = list(cell.iter(q("t")))
        if len(texts) != 1:
            raise RuntimeError(f"{sheet_name} {column}2 rich text")
        texts[0].text = "旧档·" + (texts[0].text or "")
    left = set(SPEC["leftColumns"])
    for entry in SPEC["mainRows"]:
        if entry["sheet"] != sheet_name:
            continue
        row = rows[entry["row"]]
        actual = cell_text(next(item for item in row if column_of(item.get("r")) == "B"))[1]
        if actual != entry["id"]:
            raise RuntimeError(f"id drift {sheet_name} {entry['row']} {actual}")
        for column, value in entry["values"].items():
            add_value(row, column, entry["row"], value, "15", "16", left)
    merges = root.find(q("mergeCells"))
    merges.set("count", str(int(merges.get("count")) + 1))
    ET.SubElement(merges, q("mergeCell")).set("ref", "DW1:EN1")
    validations = root.find(q("dataValidations"))
    validations.set("count", str(int(validations.get("count")) + len(SPEC["validations"])))
    for entry in SPEC["validations"]:
        validations.append(validation(entry["col"], entry["title"], entry["formula"]))
    return ET.tostring(root, xml_declaration=False, encoding="UTF-8")


def patch_catalog(original: bytes) -> bytes:
    root = ET.fromstring(original)
    root.find(q("dimension")).set("ref", "A1:BV12")
    cols = root.find(q("cols"))
    widths = dict(SPEC["dictionaryWidths"])
    for item in SPEC["lists"]:
        widths[item["col"]] = item["width"]
    for column, width in widths.items():
        node = ET.SubElement(cols, q("col"))
        node.set("width", str(width))
        node.set("customWidth", "1")
        number = str(col_index(column))
        node.set("min", number)
        node.set("max", number)
    rows = rows_by_number(root.find(q("sheetData")))
    insert_cell(rows[1], make_inline("AW1", "18", "资源与状态字典"))
    insert_cell(rows[2], make_inline("AW2", "18", "本体字典"))
    for column, header in zip(SPEC["dictionaryColumns"], SPEC["dictionaryHeaders"]):
        insert_cell(rows[3], make_inline(f"{column}3", "19", header))
    for row_offset, values in enumerate(SPEC["dictionaryRows"]):
        row_number = row_offset + 4
        style = "20" if row_number % 2 == 0 else "21"
        for column, value in zip(SPEC["dictionaryColumns"], values):
            if value is None:
                continue
            ref = f"{column}{row_number}"
            cell = make_number(ref, style, value) if isinstance(value, (int, float)) else make_inline(ref, style, value)
            insert_cell(rows[row_number], cell)
    for item in SPEC["lists"]:
        insert_cell(rows[2], make_inline(f"{item['col']}2", "18", item["title"]))
        insert_cell(rows[3], make_inline(f"{item['col']}3", "19", "名称"))
        for index, value in enumerate(item["values"]):
            row_number = index + 4
            style = "20" if row_number % 2 == 0 else "21"
            insert_cell(rows[row_number], make_inline(f"{item['col']}{row_number}", style, value))
    merges = root.find(q("mergeCells"))
    merges.set("count", str(int(merges.get("count")) + 1))
    ET.SubElement(merges, q("mergeCell")).set("ref", "AW1:BH1")
    return ET.tostring(root, xml_declaration=False, encoding="UTF-8")


def patch_styles(original: str) -> str:
    fills = '<fills count="11">'
    xfs = '<cellXfs count="22">'
    if original.count(fills) != 1 or original.count(xfs) != 1:
        raise RuntimeError("style anchors moved")
    fill = '<fill><patternFill patternType="solid"><fgColor rgb="00D5EDE8"/></patternFill></fill>'
    extra = (
        '<xf numFmtId="0" fontId="1" fillId="11" borderId="0" applyAlignment="1" pivotButton="0" quotePrefix="0" xfId="0">'
        '<alignment horizontal="center" vertical="center" wrapText="1"/></xf>'
        '<xf numFmtId="0" fontId="2" fillId="11" borderId="1" applyAlignment="1" pivotButton="0" quotePrefix="0" xfId="0">'
        '<alignment horizontal="center" vertical="center" wrapText="1"/></xf>'
    )
    updated = original.replace(fills, '<fills count="12">', 1)
    updated = updated.replace("</fills>", fill + "</fills>", 1)
    updated = updated.replace(xfs, '<cellXfs count="24">', 1)
    updated = updated.replace("</cellXfs>", extra + "</cellXfs>", 1)
    return updated


def patch_workbook(original: str) -> str:
    sheet = (
        '<sheet xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" '
        'name="资源状态明细" sheetId="6" state="visible" r:id="rId8"/>'
    )
    if "</sheets>" not in original or original.count("</sheets>") != 1:
        raise RuntimeError("sheets anchor missing")
    updated = original.replace("</sheets>", sheet + "</sheets>", 1)
    replacements = {
        "'已完成'!$A$2:$DV$2": "'已完成'!$A$2:$EN$42",
        "'待制作'!$A$2:$DV$2": "'待制作'!$A$2:$EN$5",
        "'制作中'!$A$2:$DV$2": "'制作中'!$A$2:$EN$2",
        "'待验收'!$A$2:$DV$2": "'待验收'!$A$2:$EN$2",
    }
    for old, new in replacements.items():
        if updated.count(old) != 1:
            raise RuntimeError(f"filter name missing {old}")
        updated = updated.replace(old, new, 1)
    names = []
    for item in SPEC["lists"]:
        names.append(f'<definedName name="{item["name"]}">{name_formula(item["col"])}</definedName>')
    names.append('<definedName name="_xlnm._FilterDatabase" localSheetId="5" hidden="1">\'资源状态明细\'!$A$2:$T$6</definedName>')
    updated = updated.replace("</definedNames>", "".join(names) + "</definedNames>", 1)
    return updated


def patch_content_types(original: str) -> str:
    override = (
        '<Override PartName="/xl/worksheets/sheet6.xml" '
        'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
    )
    return original.replace("</Types>", override + "</Types>", 1)


def patch_rels(original: str) -> str:
    relationship = (
        '<Relationship Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" '
        'Target="/xl/worksheets/sheet6.xml" Id="rId8"/>'
    )
    return original.replace("</Relationships>", relationship + "</Relationships>", 1)


def write_zip(source: Path, output: Path, parts: dict[str, bytes]) -> None:
    with ZipFile(source) as src, ZipFile(output, "w") as dst:
        seen = set()
        for info in src.infolist():
            payload = parts.get(info.filename, src.read(info.filename))
            cloned = ZipInfo(filename=info.filename, date_time=info.date_time)
            cloned.compress_type = info.compress_type
            cloned.external_attr = info.external_attr
            cloned.flag_bits = info.flag_bits
            dst.writestr(cloned, payload)
            seen.add(info.filename)
        for filename, payload in parts.items():
            if filename not in seen:
                dst.writestr(filename, payload)


def main() -> None:
    digest = hashlib.sha256(SOURCE.read_bytes()).hexdigest().upper()
    if digest != BASELINE:
        raise SystemExit(f"source hash changed: {digest}")
    before = snapshot(SOURCE)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup_dir = ROOT / "备份"
    backup_dir.mkdir(exist_ok=True)
    backup = backup_dir / f"术式制作表-{stamp}-1.xlsx"
    shutil.copy2(SOURCE, backup)
    with ZipFile(SOURCE) as src:
        parts = {
            "xl/styles.xml": patch_styles(src.read("xl/styles.xml").decode("utf-8")).encode("utf-8"),
            "xl/workbook.xml": patch_workbook(src.read("xl/workbook.xml").decode("utf-8")).encode("utf-8"),
            "[Content_Types].xml": patch_content_types(src.read("[Content_Types].xml").decode("utf-8")).encode("utf-8"),
            "xl/_rels/workbook.xml.rels": patch_rels(src.read("xl/_rels/workbook.xml.rels").decode("utf-8")).encode("utf-8"),
            "xl/worksheets/sheet6.xml": build_detail(),
        }
        for sheet_name, filename in SPEC["sheetFiles"].items():
            raw = src.read(filename)
            if sheet_name == "数据表":
                parts[filename] = patch_catalog(raw)
            else:
                parts[filename] = patch_status(raw, sheet_name)
    output = ROOT / "candidate-preserved.xlsx"
    write_zip(SOURCE, output, parts)
    (ROOT / "migration-cell-baseline.json").write_text(
        json.dumps({"backup": str(backup), "sheets": {name: len(cells) for name, cells in before.items()}}, ensure_ascii=False),
        encoding="utf-8",
    )
    print(json.dumps({"backup": str(backup), "candidate": str(output), "baselineCells": {name: len(cells) for name, cells in before.items()}}, ensure_ascii=False))


if __name__ == "__main__":
    main()
