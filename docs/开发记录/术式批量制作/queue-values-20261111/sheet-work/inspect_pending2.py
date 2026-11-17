# -*- coding: utf-8 -*-
import hashlib
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
OUT = Path(__file__).with_name("inspect.json")
TARGETS = [
    "fire_pick_lamp", "trajectory_parallel", "water_wave", "water_disperse",
    "sword_guard", "sword_array", "phantom_sword_surge", "sword_flight",
]
COLS = ["A", "B", "E", "F", "G", "H", "I", "K", "L", "N", "O", "R", "S", "V", "DV", "EC", "EJ", "EK"]
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def rows_of(text):
    return {int(m.group(1)): m.group(0) for m in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text)}


def cell_map(row_xml):
    found = {}
    for match in re.finditer(r'<c\b([^>/]*?)(?:/>|>([\s\S]*?)</c>)', row_xml):
        attrs, inner = match.group(1), match.group(2) or ""
        ref = re.search(r'r="([A-Z]+)\d+"', attrs)
        if not ref:
            continue
        kind = re.search(r't="([^"]+)"', attrs)
        kind_name = kind.group(1) if kind else "n"
        value = re.search(r"<v>([\s\S]*?)</v>", inner)
        parsed = value.group(1) if value else None
        if kind_name == "inlineStr":
            parsed = "".join(ET.fromstring("<root>" + inner + "</root>").itertext())
        style = re.search(r's="(\d+)"', attrs)
        found[ref.group(1)] = {
            "type": kind_name,
            "value": parsed,
            "style": style.group(1) if style else None,
            "xml": match.group(0)[:180],
        }
    return found


def sheet_map(data):
    rels = ET.fromstring(data["xl/_rels/workbook.xml.rels"])
    targets = {}
    for node in rels:
        target = "xl/" + node.attrib["Target"].lstrip("/")
        targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
    work = ET.fromstring(data["xl/workbook.xml"])
    names = {}
    order = []
    for node in work.find("s:sheets", NS):
        names[node.attrib["name"]] = targets[node.attrib[REL]]
        order.append(node.attrib["name"])
    defined = []
    for node in work.findall("s:definedNames/s:definedName", NS):
        defined.append({"name": node.attrib.get("name"), "text": "".join(node.itertext()).strip(), "localSheetId": node.attrib.get("localSheetId")})
    return names, order, defined


def ids_of(text):
    found = []
    for number, row in sorted(rows_of(text).items()):
        if number < 3:
            continue
        cells = cell_map(row)
        value = (cells.get("B") or {}).get("value")
        if value not in (None, ""):
            found.append({"row": number, "id": str(value).strip(), "name": (cells.get("A") or {}).get("value")})
    return found


with zipfile.ZipFile(LIVE) as book:
    data = {name: book.read(name) for name in book.namelist()}
sheets, order, defined = sheet_map(data)
texts = {name: data[part].decode("utf-8") for name, part in sheets.items()}
counts = {name: len(ids_of(texts[name])) for name in ("已完成", "待制作", "制作中", "待验收", "待制作原始数据", "资源状态明细")}
places = {target: [] for target in TARGETS}
for name in counts:
    for item in ids_of(texts[name]):
        if item["id"] in places:
            places[item["id"]].append({"sheet": name, "row": item["row"], "name": item["name"]})

pending_rows = rows_of(texts["待制作"])
header = cell_map(pending_rows[2])
interesting = {}
for col, cell in header.items():
    value = cell.get("value") or ""
    if col in COLS or any(token in value for token in ("资源", "水势", "数值", "上限", "扣", "弹道", "角色", "伤害", "冷却", "魔力", "速度")):
        interesting[col] = value

pending = []
for item in ids_of(texts["待制作"]):
    if item["id"] not in TARGETS:
        continue
    cells = cell_map(pending_rows[item["row"]])
    pending.append({
        "row": item["row"],
        "id": item["id"],
        "name": item["name"],
        "cells": {col: cells.get(col) for col in COLS},
    })

archive_rows = rows_of(texts["待制作原始数据"])
archive_header_row = 1 if "EP" in cell_map(archive_rows[1]) else 2
archive_header = cell_map(archive_rows[archive_header_row])
meta = {col: cell.get("value") for col, cell in sorted(archive_header.items(), key=lambda pair: pair[0]) if col.startswith("E") and len(col) == 2 and col >= "EO" or col.startswith("F")}
last = max(archive_rows)
last_cells = cell_map(archive_rows[last])

data_rows = rows_of(texts["数据表"])
data_dump = []
for number in range(1, 22):
    if number not in data_rows:
        data_dump.append({"row": number, "missing": True})
        continue
    cells = cell_map(data_rows[number])
    data_dump.append({
        "row": number,
        "A": cells.get("A"),
        "B": cells.get("B"),
        "C": cells.get("C"),
        "D": cells.get("D"),
        "E": cells.get("E"),
        "F": cells.get("F"),
        "G": cells.get("G"),
        "H": cells.get("H"),
    })

validations = []
for name in ("待制作", "数据表", "制作中", "待验收", "已完成"):
    for match in re.finditer(r"<dataValidation\b[\s\S]*?</dataValidation>", texts[name]):
        block = match.group(0)
        formula = re.search(r"<formula1>([\s\S]*?)</formula1>", block)
        sq = re.search(r'sqref="([^"]+)"', block)
        formula_text = formula.group(1) if formula else ""
        if any(token in formula_text for token in ("弹道", "链上角色")):
            validations.append({"sheet": name, "sqref": sq.group(1) if sq else None, "formula1": formula_text})

report = {
    "sha256": sha256(LIVE),
    "sheetOrder": order,
    "counts": counts,
    "pendingIds": ids_of(texts["待制作"]),
    "places": places,
    "headers": interesting,
    "pending": pending,
    "defined": [item for item in defined if item["name"] in ("弹道", "链上角色")],
    "data": data_dump,
    "validations": validations,
    "archive": {
        "maxRow": last,
        "headerRow": archive_header_row,
        "meta": meta,
        "lastId": (last_cells.get("B") or {}).get("value"),
        "lastMeta": {col: last_cells.get(col) for col in meta},
        "dimension": re.search(r'<dimension[^>]*ref="([^"]+)"', texts["待制作原始数据"]).group(1),
        "pendingDimension": re.search(r'<dimension[^>]*ref="([^"]+)"', texts["待制作"]).group(1),
        "dataDimension": re.search(r'<dimension[^>]*ref="([^"]+)"', texts["数据表"]).group(1),
    },
}
OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print("wrote", OUT)
