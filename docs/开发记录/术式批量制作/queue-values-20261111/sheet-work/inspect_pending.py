# -*- coding: utf-8 -*-
"""Read-only check of the eight pending rows and lookup tables."""
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


def shared_items(sst):
    return [
        "".join(ET.fromstring(body).itertext())
        for body in re.findall(r"<si\b[^>]*>[\s\S]*?</si>", sst)
    ]


def rows_of(text):
    return {
        int(match.group(1)): match.group(0)
        for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text)
    }


def cell_map(row_xml, items):
    found = {}
    for match in re.finditer(r'<c\b([^>]*?)(?:/>|>([\s\S]*?)</c>)', row_xml):
        attrs = match.group(1)
        inner = match.group(2) or ""
        ref = re.search(r'r="([A-Z]+)\d+"', attrs)
        if not ref:
            continue
        kind = re.search(r't="([^"]+)"', attrs)
        value = re.search(r"<v>([\s\S]*?)</v>", inner)
        parsed = value.group(1) if value else None
        formula = re.search(r"<f[^>]*>([\s\S]*?)</f>", inner)
        if kind and kind.group(1) == "s" and parsed is not None:
            parsed = items[int(parsed)]
        elif kind and kind.group(1) == "inlineStr":
            parsed = "".join(ET.fromstring("<c>" + inner + "</c>").itertext())
        found[ref.group(1)] = {
            "type": kind.group(1) if kind else "n",
            "value": parsed,
            "formula": formula.group(1) if formula else None,
            "style": (re.search(r's="(\d+)"', attrs) or type("x", (), {"group": lambda self, i: None})()).group(1),
        }
    return found


payload = LIVE.read_bytes()
with zipfile.ZipFile(LIVE) as book:
    data = {name: book.read(name) for name in book.namelist()}
rels = ET.fromstring(data["xl/_rels/workbook.xml.rels"])
targets = {}
for node in rels:
    target = "xl/" + node.attrib["Target"].lstrip("/")
    targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
work = ET.fromstring(data["xl/workbook.xml"])
sheets = {}
for node in work.find("s:sheets", NS):
    sheets[node.attrib["name"]] = targets[node.attrib[REL]]
items = shared_items(data["xl/sharedStrings.xml"].decode("utf-8"))
defined = []
for node in work.findall("s:definedNames/s:definedName", NS):
    defined.append({
        "name": node.attrib.get("name"),
        "text": (node.text or "").strip(),
        "localSheetId": node.attrib.get("localSheetId"),
    })


def sheet_text(name):
    return data[sheets[name]].decode("utf-8")


def ids_of(name):
    found = []
    for number, row in sorted(rows_of(sheet_text(name)).items()):
        if number < 3:
            continue
        cells = cell_map(row, items)
        value = (cells.get("B") or {}).get("value")
        if value not in (None, ""):
            found.append({"row": number, "id": str(value).strip(), "name": (cells.get("A") or {}).get("value")})
    return found


status_names = ["已完成", "待制作", "制作中", "待验收", "待制作原始数据", "资源状态明细", "数据表"]
status = {name: ids_of(name) for name in status_names if name in sheets}
pending_rows = rows_of(sheet_text("待制作"))
header = cell_map(pending_rows[2], items)
wanted_headers = {}
for col, cell in header.items():
    if col in COLS or (cell.get("value") and any(token in str(cell["value"]) for token in ("资源", "水势", "数值", "上限", "扣", "弹道", "链"))):
        wanted_headers[col] = cell.get("value")

pending_detail = []
for item in status["待制作"]:
    if item["id"] not in TARGETS:
        continue
    cells = cell_map(pending_rows[item["row"]], items)
    pending_detail.append({
        "row": item["row"],
        "id": item["id"],
        "cells": {col: cells.get(col) for col in COLS},
        "filled": sorted(col for col, cell in cells.items() if cell.get("value") not in (None, "")),
    })

archive_rows = rows_of(sheet_text("待制作原始数据"))
archive_header = cell_map(archive_rows[2], items) if 2 in archive_rows else cell_map(archive_rows[1], items)
meta_headers = {col: cell.get("value") for col, cell in archive_header.items() if col >= "EO"}
archive_last = max(archive_rows)
archive_sample = cell_map(archive_rows[archive_last], items)
archive_meta_sample = {col: archive_sample.get(col) for col in meta_headers}

data_rows = rows_of(sheet_text("数据表"))
data_dump = []
for number, row in sorted(data_rows.items()):
    if number > 25:
        continue
    cells = cell_map(row, items)
    data_dump.append({
        "row": number,
        "values": {col: cell.get("value") for col, cell in cells.items() if col <= "H"},
        "styles": {col: cell.get("style") for col, cell in cells.items() if col <= "H"},
    })

validations = []
for name in ("待制作", "数据表"):
    text = sheet_text(name)
    for match in re.finditer(r"<dataValidation\b[\s\S]*?</dataValidation>", text):
        block = match.group(0)
        formula = re.search(r"<formula1>([\s\S]*?)</formula1>", block)
        sq = re.search(r'sqref="([^"]+)"', block)
        formula_text = formula.group(1) if formula else ""
        if any(token in (formula_text + (sq.group(1) if sq else "")) for token in ("弹道", "链上", "R", "S", "$A$", "$E$")):
            validations.append({"sheet": name, "sqref": sq.group(1) if sq else None, "formula1": formula_text})

resource_hits = [item for item in status.get("资源状态明细", []) if item["id"] in TARGETS]
places = {target: [] for target in TARGETS}
for name, rows in status.items():
    for item in rows:
        if item["id"] in places:
            places[item["id"]].append({"sheet": name, "row": item["row"]})

report = {
    "sha256": sha256(LIVE),
    "bytes": len(payload),
    "counts": {name: len(rows) for name, rows in status.items()},
    "pendingIds": status["待制作"],
    "places": places,
    "resourceHits": resource_hits,
    "headers": wanted_headers,
    "pending": pending_detail,
    "defined": [item for item in defined if item["name"] in ("弹道", "链上角色") or "弹" in (item["name"] or "") or "链" in (item["name"] or "")],
    "allDefinedCount": len(defined),
    "data": data_dump,
    "validations": validations,
    "archive": {
        "maxRow": archive_last,
        "dataRows": len(status["待制作原始数据"]),
        "metaHeaders": meta_headers,
        "lastMeta": archive_meta_sample,
        "dimension": re.search(r'<dimension[^>]*ref="([^"]+)"', sheet_text("待制作原始数据")).group(1),
    },
}
OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(OUT)
