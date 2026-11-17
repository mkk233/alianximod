# -*- coding: utf-8 -*-
"""Read-only intake of the current workbook. Does not write the workbook."""
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
out = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031\intake-cells.json")
NS = {
    "m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
    "pr": "http://schemas.openxmlformats.org/package/2006/relationships",
}


def col_index(letters):
    value = 0
    for char in letters:
        value = value * 26 + ord(char) - 64
    return value


def col_letters(index):
    letters = ""
    while index:
        index, remainder = divmod(index - 1, 26)
        letters = chr(65 + remainder) + letters
    return letters


def local(tag):
    return tag.rsplit("}", 1)[-1]


def text_of(node):
    parts = []
    if node.text:
        parts.append(node.text)
    for child in list(node):
        parts.append(text_of(child))
        if child.tail:
            parts.append(child.tail)
    return html.unescape("".join(parts))


payload = live.read_bytes()
stat = live.stat()
archive = zipfile.ZipFile(live)
names = set(archive.namelist())

shared = []
if "xl/sharedStrings.xml" in names:
    root = ET.fromstring(archive.read("xl/sharedStrings.xml"))
    for item in root:
        if local(item.tag) == "si":
            shared.append(text_of(item))

workbook = ET.fromstring(archive.read("xl/workbook.xml"))
rels = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
rel_target = {}
for rel in rels:
    if local(rel.tag) == "Relationship":
        target = rel.attrib["Target"].lstrip("/")
        if not target.startswith("xl/"):
            target = "xl/" + target
        rel_target[rel.attrib["Id"]] = target

sheets = []
for sheet in workbook.iter():
    if local(sheet.tag) != "sheet":
        continue
    rid = sheet.attrib.get("{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id")
    sheets.append({"name": sheet.attrib["name"], "path": rel_target[rid]})


def parse_sheet(path):
    root = ET.fromstring(archive.read(path))
    rows = []
    validations = []
    dimension = None
    for node in root.iter():
        kind = local(node.tag)
        if kind == "dimension":
            dimension = node.attrib.get("ref")
        elif kind == "dataValidation":
            formula = None
            for child in list(node):
                if local(child.tag) == "formula1":
                    formula = text_of(child)
            validations.append({
                "sqref": node.attrib.get("sqref"),
                "type": node.attrib.get("type"),
                "formula1": formula,
            })
        elif kind == "row":
            number = int(node.attrib["r"])
            cells = {}
            for cell in list(node):
                if local(cell.tag) != "c":
                    continue
                ref = cell.attrib.get("r", "")
                match = re.match(r"([A-Z]+)(\d+)", ref)
                if not match:
                    continue
                column = match.group(1)
                kind_attr = cell.attrib.get("t")
                formula = None
                raw = None
                for child in list(cell):
                    child_kind = local(child.tag)
                    if child_kind == "f":
                        formula = text_of(child)
                    elif child_kind == "v":
                        raw = text_of(child)
                    elif child_kind == "is":
                        raw = text_of(child)
                        kind_attr = kind_attr or "inlineStr"
                value = raw
                if kind_attr == "s" and raw is not None and raw.isdigit():
                    value = shared[int(raw)]
                elif kind_attr == "b" and raw is not None:
                    value = raw == "1"
                cells[column] = {"t": kind_attr, "v": value, "f": formula}
            rows.append({"r": number, "cells": cells})
    return {"dimension": dimension, "rows": rows, "validations": validations}


def comments_for(sheet_path):
    rel_path = "xl/worksheets/_rels/" + Path(sheet_path).name + ".rels"
    if rel_path not in names:
        return []
    root = ET.fromstring(archive.read(rel_path))
    found = []
    for rel in root:
        if local(rel.tag) != "Relationship":
            continue
        if "comments" not in rel.attrib.get("Type", ""):
            continue
        raw = rel.attrib["Target"]
        if raw.startswith("/"):
            target = raw.lstrip("/")
        elif raw.startswith(".."):
            target = "xl/" + raw.split("../")[-1]
        elif raw.startswith("xl/"):
            target = raw
        else:
            target = "xl/worksheets/" + raw
        if target not in names:
            # resolve relative to the worksheet directory
            base = Path(sheet_path).parent
            resolved = str((base / rel.attrib["Target"]).as_posix()).replace("/xl/worksheets/../", "/xl/")
            target = resolved[1:] if resolved.startswith("/") else resolved
        if target not in names:
            found.append({"missing": rel.attrib["Target"]})
            continue
        comment_root = ET.fromstring(archive.read(target))
        for comment in comment_root.iter():
            if local(comment.tag) != "comment":
                continue
            ref = comment.attrib.get("ref")
            texts = []
            for child in comment.iter():
                if local(child.tag) == "t" and child.text:
                    texts.append(html.unescape(child.text))
            found.append({"ref": ref, "text": "".join(texts)})
    return found


parsed = []
for sheet in sheets:
    body = parse_sheet(sheet["path"])
    body["name"] = sheet["name"]
    body["path"] = sheet["path"]
    body["comments"] = comments_for(sheet["path"])
    parsed.append(body)

status_names = ["已完成", "待制作", "制作中", "待验收"]
header_columns = [col_letters(index) for index in range(1, 145)]


def row_map(sheet):
    return {row["r"]: row["cells"] for row in sheet["rows"]}


def cell_value(cells, column):
    item = cells.get(column)
    return None if item is None else item.get("v")


status = {}
anomalies = []
all_ids = []
for sheet in parsed:
    if sheet["name"] not in status_names:
        continue
    rows = row_map(sheet)
    items = []
    for number, cells in rows.items():
        if number < 3:
            continue
        code = cell_value(cells, "B")
        name = cell_value(cells, "A")
        if code is None and name is None and not cells:
            continue
        items.append({"row": number, "id": code, "name": name})
        if not code:
            anomalies.append({"sheet": sheet["name"], "row": number, "issue": "空编号", "name": name})
        else:
            all_ids.append((sheet["name"], number, str(code).strip()))
    status[sheet["name"]] = items

seen = {}
for sheet_name, number, code in all_ids:
    seen.setdefault(code, []).append({"sheet": sheet_name, "row": number})
duplicates = {code: places for code, places in seen.items() if len(places) > 1}

archive = next(sheet for sheet in parsed if sheet["name"] == "待制作原始数据")
archive_ids = []
for number, cells in row_map(archive).items():
    if number < 3:
        continue
    code = cell_value(cells, "B")
    if code:
        archive_ids.append({"row": number, "id": str(code).strip(), "name": cell_value(cells, "A")})
archive_seen = {}
for item in archive_ids:
    archive_seen.setdefault(item["id"], []).append(item["row"])
archive_duplicates = {code: rows for code, rows in archive_seen.items() if len(rows) > 1}

pending = next(sheet for sheet in parsed if sheet["name"] == "待制作")
pending_rows = []
for number, cells in row_map(pending).items():
    if number < 3 or not cell_value(cells, "B"):
        continue
    full = {}
    for column in header_columns:
        if column in cells:
            full[column] = cells[column]
    pending_rows.append({
        "row": number,
        "id": str(cell_value(cells, "B")).strip(),
        "cells": full,
    })

detail = next(sheet for sheet in parsed if sheet["name"] == "资源状态明细")
detail_rows = row_map(detail)
detail_header = {
    "row1": {column: cell_value(detail_rows.get(1, {}), column) for column in header_columns if column in detail_rows.get(1, {})},
    "row2": {column: cell_value(detail_rows.get(2, {}), column) for column in header_columns if column in detail_rows.get(2, {})},
}
detail_items = []
for number, cells in detail_rows.items():
    if number < 3:
        continue
    detail_items.append({
        "row": number,
        "cells": {column: cells[column] for column in cells},
    })

data_sheet = next(sheet for sheet in parsed if sheet["name"] == "数据表")
data_rows = row_map(data_sheet)
headers = {}
for sheet in parsed:
    if sheet["name"] not in status_names and sheet["name"] != "待制作原始数据":
        continue
    rows = row_map(sheet)
    headers[sheet["name"]] = {
        "row1": {column: cell_value(rows.get(1, {}), column) for column in header_columns if column in rows.get(1, {})},
        "row2": {column: cell_value(rows.get(2, {}), column) for column in header_columns if column in rows.get(2, {})},
    }

header_comments = []
for sheet in parsed:
    for comment in sheet["comments"]:
        ref = comment.get("ref") or ""
        match = re.match(r"[A-Z]+(\d+)", ref)
        if match and int(match.group(1)) <= 2:
            header_comments.append({"sheet": sheet["name"], **comment})

result = {
    "sha256": hashlib.sha256(payload).hexdigest().upper(),
    "bytes": stat.st_size,
    "mtimeNs": stat.st_mtime_ns,
    "sheets": [{"name": sheet["name"], "dimension": sheet["dimension"], "dataRows": sum(1 for row in sheet["rows"] if row["r"] >= 3 and row["cells"])} for sheet in parsed],
    "statusCounts": {name: len(items) for name, items in status.items()},
    "statusIds": status,
    "duplicateStatusIds": duplicates,
    "emptyIdAnomalies": anomalies,
    "archiveCount": len(archive_ids),
    "archiveDuplicateIds": archive_duplicates,
    "archiveIds": archive_ids,
    "headers": headers,
    "headerComments": header_comments,
    "validations": {sheet["name"]: sheet["validations"] for sheet in parsed},
    "pendingRows": pending_rows,
    "detailHeader": detail_header,
    "detailRows": detail_items,
    "dataSheetSample": {
        "dimension": data_sheet["dimension"],
        "rowCount": len(data_sheet["rows"]),
    },
}
out.write_text(json.dumps(result, ensure_ascii=False), encoding="utf-8")
print(json.dumps({
    "sha256": result["sha256"],
    "bytes": result["bytes"],
    "counts": result["statusCounts"],
    "pending": [item["id"] for item in pending_rows],
    "duplicates": duplicates,
    "empty": anomalies,
    "archiveDuplicates": archive_duplicates,
    "comments": len(header_comments),
}, ensure_ascii=False))
