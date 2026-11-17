# -*- coding: utf-8 -*-
"""Read-only inventory of the technique workbook. Does not write the source."""
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
OUT = Path(__file__).with_name("inventory.json")
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"

def col_letters(index):
    letters = ""
    n = index
    while n:
        n, rem = divmod(n - 1, 26)
        letters = chr(65 + rem) + letters
    return letters

def col_index(letters):
    n = 0
    for ch in letters:
        n = n * 26 + ord(ch) - 64
    return n

with zipfile.ZipFile(LIVE) as book:
    names = book.namelist()
    data = {name: book.read(name) for name in names}

rels = ET.fromstring(data["xl/_rels/workbook.xml.rels"])
targets = {}
for node in rels:
    target = "xl/" + node.attrib["Target"].lstrip("/")
    targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
work = ET.fromstring(data["xl/workbook.xml"])
sheets = []
for node in work.find("s:sheets", NS):
    sheets.append({
        "name": node.attrib["name"],
        "part": targets[node.attrib[REL]],
        "sheetId": node.attrib.get("sheetId"),
    })

sst_xml = data["xl/sharedStrings.xml"].decode("utf-8")
items = []
for body in re.findall(r"<si\b[^>]*>[\s\S]*?</si>", sst_xml):
    items.append("".join(ET.fromstring(body).itertext()))

def cell_value(cell_xml):
    ref = re.search(r'\br="([A-Z]+)(\d+)"', cell_xml)
    if not ref:
        return None
    kind = re.search(r'\bt="([^"]+)"', cell_xml)
    raw = re.search(r"<v>([\s\S]*?)</v>", cell_xml)
    formula = re.search(r"<f[^>]*>([\s\S]*?)</f>", cell_xml)
    value = None
    if raw:
        value = raw.group(1)
        if kind and kind.group(1) == "s":
            value = items[int(value)]
        elif kind and kind.group(1) == "inlineStr":
            value = "".join(ET.fromstring("<x xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" + cell_xml + "</x>").itertext())
    style = re.search(r'\bs="(\d+)"', cell_xml)
    return {
        "ref": ref.group(1) + ref.group(2),
        "col": ref.group(1),
        "row": int(ref.group(2)),
        "type": kind.group(1) if kind else "n",
        "value": value,
        "formula": formula.group(1) if formula else None,
        "style": style.group(1) if style else None,
    }

def rows_of(text):
    found = {}
    for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text):
        found[int(match.group(1))] = match.group(0)
    return found

defined = []
for node in work.findall("s:definedNames/s:definedName", NS):
    defined.append({"name": node.attrib.get("name"), "text": (node.text or "").strip(), "localSheetId": node.attrib.get("localSheetId")})

wanted = [
    "rule_triple_release", "rule_quadruple_release", "rule_octuple_release", "rule_sixteen_release",
    "rule_exhaustive_release", "rule_double_cast", "rule_quintuple_cast", "rule_octuple_cast",
    "rule_dual_release", "rule_triple_cast", "fire_burn",
]
report = {
    "parts": len(names),
    "partNames": names,
    "sheets": [],
    "definedNames": defined,
    "hits": {key: [] for key in wanted},
    "comments": [],
    "validations": [],
}

for sheet in sheets:
    text = data[sheet["part"]].decode("utf-8")
    row_map = rows_of(text)
    dimension = re.search(r'<dimension[^>]*ref="([^"]+)"', text)
    ids = []
    nonempty_other = []
    for number, row in row_map.items():
        if number < 3:
            continue
        cells = [cell_value(piece) for piece in re.findall(r"<c\b[^>]*>[\s\S]*?</c>|<c\b[^>]*/>", row)]
        cells = [cell for cell in cells if cell]
        values = {cell["col"]: cell["value"] for cell in cells}
        bid = values.get("B")
        if bid not in (None, ""):
            ids.append({"row": number, "id": str(bid).strip(), "name": values.get("A")})
            if str(bid).strip() in report["hits"]:
                report["hits"][str(bid).strip()].append({"sheet": sheet["name"], "row": number})
        elif any(value not in (None, "") for value in values.values()):
            nonempty_other.append(number)
    header1 = {}
    header2 = {}
    if 1 in row_map:
        for cell in [cell_value(piece) for piece in re.findall(r"<c\b[^>]*>[\s\S]*?</c>|<c\b[^>]*/>", row_map[1])]:
            if cell and cell["value"] not in (None, ""):
                header1[cell["col"]] = cell["value"]
    if 2 in row_map:
        for cell in [cell_value(piece) for piece in re.findall(r"<c\b[^>]*>[\s\S]*?</c>|<c\b[^>]*/>", row_map[2])]:
            if cell and cell["value"] not in (None, ""):
                header2[cell["col"]] = cell["value"]
    vals = []
    for match in re.finditer(r"<dataValidation\b[\s\S]*?</dataValidation>", text):
        block = match.group(0)
        sq = re.search(r'sqref="([^"]+)"', block)
        typ = re.search(r'type="([^"]+)"', block)
        formula = re.search(r"<formula1>([\s\S]*?)</formula1>", block)
        vals.append({"sqref": sq.group(1) if sq else None, "type": typ.group(1) if typ else None, "formula1": formula.group(1) if formula else None})
    report["validations"].append({"sheet": sheet["name"], "count": len(vals), "items": vals[:80]})
    report["sheets"].append({
        "name": sheet["name"],
        "part": sheet["part"],
        "dimension": dimension.group(1) if dimension else None,
        "rowCount": len(row_map),
        "maxRow": max(row_map) if row_map else 0,
        "dataRows": len(ids),
        "ids": ids,
        "nonemptyWithoutB": nonempty_other,
        "header1Count": len(header1),
        "header2Count": len(header2),
        "header2": header2,
    })

for name in names:
    if "comment" in name.lower() and name.endswith(".xml"):
        text = data[name].decode("utf-8", errors="replace")
        refs = re.findall(r'ref="([A-Z]+\d+)"', text)
        report["comments"].append({"part": name, "refs": len(refs), "sample": refs[:12]})

# Template full cells
status = {item["name"]: item for item in report["sheets"]}
templates = {}
for sheet_name, wanted_id in (("已完成", "rule_dual_release"), ("已完成", "rule_triple_cast")):
    sheet = next(item for item in report["sheets"] if item["name"] == sheet_name)
    text = data[sheet["part"]].decode("utf-8")
    row_map = rows_of(text)
    hit = next(row for row in sheet["ids"] if row["id"] == wanted_id)
    row = row_map[hit["row"]]
    cells = [cell_value(piece) for piece in re.findall(r"<c\b[^>]*>[\s\S]*?</c>|<c\b[^>]*/>", row)]
    templates[wanted_id] = {
        "row": hit["row"],
        "cells": [{
            "col": cell["col"],
            "header": status[sheet_name]["header2"].get(cell["col"]),
            "type": cell["type"],
            "value": cell["value"],
            "formula": cell["formula"],
            "style": cell["style"],
        } for cell in cells if cell],
    }

# Data sheet role names
data_sheet = next(item for item in report["sheets"] if item["name"] == "数据表")
text = data[data_sheet["part"]].decode("utf-8")
row_map = rows_of(text)
roles = []
for number, row in sorted(row_map.items()):
    cells = [cell_value(piece) for piece in re.findall(r"<c\b[^>]*>[\s\S]*?</c>|<c\b[^>]*/>", row)]
    values = {cell["col"]: cell["value"] for cell in cells if cell}
    blob = " ".join(str(value) for value in values.values() if value)
    if any(token in blob for token in ("双重", "一分为", "RULE_", "通用", "规则", "触发", "直线")):
        roles.append({"row": number, "values": values})

archive = next((item for item in report["sheets"] if item["name"] == "待制作原始数据"), None)
archive_detail = None
if archive:
    text = data[archive["part"]].decode("utf-8")
    row_map = rows_of(text)
    sample_ids = archive["ids"][:3] + archive["ids"][-2:]
    archive_detail = {
        "dataRows": archive["dataRows"],
        "maxRow": archive["maxRow"],
        "dimension": archive["dimension"],
        "header2": archive["header2"],
        "sample": sample_ids,
        "duplicateCheckReady": True,
    }

resource = next(item for item in report["sheets"] if item["name"] == "资源状态明细")
resource_ids = [row["id"] for row in resource["ids"]]

OUT.write_text(json.dumps({
    "templates": templates,
    "rolesSample": roles[:40],
    "archive": archive_detail,
    "resourceIdCount": len(resource_ids),
    "resourceHasTargets": [item for item in wanted if item in resource_ids],
    "summary": [{
        "name": item["name"],
        "dimension": item["dimension"],
        "dataRows": item["dataRows"],
        "maxRow": item["maxRow"],
        "nonemptyWithoutB": item["nonemptyWithoutB"],
        "validationCount": next(v["count"] for v in report["validations"] if v["sheet"] == item["name"]),
    } for item in report["sheets"]],
    "hits": report["hits"],
    "comments": report["comments"],
    "definedNames": defined,
    "validations": report["validations"],
    "header2BySheet": {item["name"]: item["header2"] for item in report["sheets"] if item["name"] in ("已完成", "待制作", "制作中", "待验收", "待制作原始数据", "资源状态明细")},
    "ids": {item["name"]: item["ids"] for item in report["sheets"] if item["name"] in ("已完成", "待制作", "制作中", "待验收", "待制作原始数据")},
}, ensure_ascii=False, indent=2), encoding="utf-8")
print(OUT)
