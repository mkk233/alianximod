# -*- coding: utf-8 -*-
import json
import re
import zipfile
from xml.etree import ElementTree as ET

LIVE = r"D:\alianximod\docs\术式制作表.xlsx"
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"


def coln(letters):
    number = 0
    for char in letters:
        number = number * 26 + ord(char) - 64
    return number


def rows_of(text):
    return {int(m.group(1)): m.group(0) for m in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text)}


def cells(row):
    found = {}
    for match in re.finditer(r'<c\b([^>/]*?)(?:/>|>([\s\S]*?)</c>)', row):
        attrs, inner = match.group(1), match.group(2) or ""
        ref = re.search(r'r="([A-Z]+)(\d+)"', attrs)
        if not ref:
            continue
        kind = re.search(r't="([^"]+)"', attrs)
        kind_name = kind.group(1) if kind else "n"
        value = None
        if kind_name == "inlineStr":
            value = "".join(ET.fromstring("<root>" + inner + "</root>").itertext())
        else:
            raw = re.search(r"<v>([\s\S]*?)</v>", inner)
            value = raw.group(1) if raw else None
        style = re.search(r's="(\d+)"', attrs)
        found[ref.group(1)] = {"value": value, "type": kind_name, "style": style.group(1) if style else None}
    return found


with zipfile.ZipFile(LIVE) as book:
    rels = ET.fromstring(book.read("xl/_rels/workbook.xml.rels"))
    targets = {}
    for node in rels:
        target = "xl/" + node.attrib["Target"].lstrip("/")
        targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
    work = ET.fromstring(book.read("xl/workbook.xml"))
    sheets = {node.attrib["name"]: targets[node.attrib[REL]] for node in work.find("s:sheets", NS)}
    archive = book.read(sheets["待制作原始数据"]).decode("utf-8")
    data = book.read(sheets["数据表"]).decode("utf-8")
    pending = book.read(sheets["待制作"]).decode("utf-8")

archive_rows = rows_of(archive)
report = {"archiveHeaders": {}, "archiveLastMeta": {}, "dataRows": [], "pendingCells": [], "validations": []}
for row_number in (1, 2):
    found = cells(archive_rows[row_number])
    report["archiveHeaders"][str(row_number)] = {
        col: cell["value"] for col, cell in found.items() if coln(col) >= 115
    }
last = max(archive_rows)
last_cells = cells(archive_rows[last])
report["archiveLast"] = last
report["archiveLastMeta"] = {col: last_cells[col] for col in last_cells if coln(col) >= 115}
report["archiveRowAttrs"] = re.search(r"<row\b[^>]*r=\"33\"[^>]*>", archive).group(0)

data_rows = rows_of(data)
for number in range(1, 18):
    if number not in data_rows:
        report["dataRows"].append({"row": number, "missing": True})
        continue
    found = cells(data_rows[number])
    report["dataRows"].append({
        "row": number,
        "attrs": re.search(r"<row\b[^>]*>", data_rows[number]).group(0),
        "cells": {col: found.get(col) for col in ("A", "B", "C", "D", "E", "F", "G", "H")},
    })

pending_rows = rows_of(pending)
for number in range(2, 11):
    found = cells(pending_rows[number])
    report["pendingCells"].append({
        "row": number,
        "attrs": re.search(r"<row\b[^>]*>", pending_rows[number]).group(0),
        "cells": {
            col: found.get(col)
            for col in ("A", "B", "E", "F", "G", "H", "I", "K", "L", "N", "O", "R", "S", "V", "DX", "EC", "EJ", "EK", "DV")
        },
    })

for match in re.finditer(r"<dataValidation\b[\s\S]*?</dataValidation>", pending):
    block = match.group(0)
    formula = re.search(r"<formula1>([\s\S]*?)</formula1>", block)
    sq = re.search(r'sqref="([^"]+)"', block)
    formula_text = formula.group(1) if formula else ""
    sqref = sq.group(1) if sq else ""
    if "R" in sqref or "S" in sqref or "弹道" in formula_text or "链上角色" in formula_text:
        report["validations"].append({"sqref": sqref, "formula1": formula_text, "type": re.search(r'type="([^"]+)"', block).group(1)})

out = r"D:\alianximod\docs\开发记录\术式批量制作\queue-values-20261111\sheet-work\meta.json"
with open(out, "w", encoding="utf-8") as handle:
    json.dump(report, handle, ensure_ascii=True, indent=2)
print("rows", len(report["pendingCells"]), "valid", len(report["validations"]))
