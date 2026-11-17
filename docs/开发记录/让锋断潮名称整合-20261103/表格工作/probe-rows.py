# -*- coding: utf-8 -*-
"""Read-only locate water_yielding_ribbon and tide_sever."""
import hashlib
import html
import json
import re
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(r"D:\alianximod")
LIVE = ROOT / "docs" / "术式制作表.xlsx"
OUT = Path(__file__).resolve().parent / "probe-rows.json"
NS = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
      "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
      "pr": "http://schemas.openxmlformats.org/package/2006/relationships"}


def local(tag):
    return tag.rsplit("}", 1)[-1]


def col_name(index):
    name = ""
    while index:
        index, rem = divmod(index - 1, 26)
        name = chr(65 + rem) + name
    return name


def col_index(name):
    value = 0
    for char in name:
        value = value * 26 + (ord(char) - 64)
    return value


def sha256_file(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def shared_list(xml):
    root = ET.fromstring(xml)
    found = []
    for item in root:
        if local(item.tag) != "si":
            continue
        texts = []
        for node in item.iter():
            if local(node.tag) == "t" and node.text:
                texts.append(html.unescape(node.text))
        found.append("".join(texts))
    return found


def cell_text(cell, shared):
    if cell is None:
        return None
    kind = cell.attrib.get("t")
    if kind == "inlineStr":
        texts = []
        for node in cell.iter():
            if local(node.tag) == "t" and node.text:
                texts.append(html.unescape(node.text))
        return "".join(texts)
    value_node = None
    for child in list(cell):
        if local(child.tag) == "v":
            value_node = child
            break
    if value_node is None or value_node.text is None:
        return None
    raw = html.unescape(value_node.text)
    if kind == "s" and raw.isdigit():
        return shared[int(raw)]
    return raw


def parse_sheet(xml, shared):
    root = ET.fromstring(xml)
    rows = {}
    for node in root.iter():
        if local(node.tag) != "row":
            continue
        number = int(node.attrib["r"])
        cells = {}
        for cell in list(node):
            if local(cell.tag) != "c":
                continue
            ref = cell.attrib.get("r", "")
            match = re.match(r"([A-Z]+)", ref)
            if not match:
                continue
            cells[match.group(1)] = cell_text(cell, shared)
        rows[number] = cells
    return rows


def main():
    import zipfile
    stat = LIVE.stat()
    with zipfile.ZipFile(LIVE) as book:
        names = book.namelist()
        shared = shared_list(book.read("xl/sharedStrings.xml")) if "xl/sharedStrings.xml" in names else []
        workbook = ET.fromstring(book.read("xl/workbook.xml"))
        rels = ET.fromstring(book.read("xl/_rels/workbook.xml.rels"))
        rel_map = {node.attrib["Id"]: node.attrib["Target"] for node in rels}
        sheets = []
        for node in workbook.iter():
            if local(node.tag) != "sheet":
                continue
            target = rel_map[node.attrib["{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"]]
            path = "xl/" + target.lstrip("/") if not target.startswith("xl/") else target
            sheets.append({"name": node.attrib["name"], "path": path})
        parsed = {}
        for sheet in sheets:
            parsed[sheet["name"]] = parse_sheet(book.read(sheet["path"]), shared)

    targets = ("water_yielding_ribbon", "tide_sever")
    name_needles = ("让锋", "断潮")
    status = ["已完成", "待制作", "制作中", "待验收"]
    summary = []
    hits = []
    counts = {}
    duplicate = []
    seen = {}
    for sheet_name in status:
        rows = parsed.get(sheet_name, {})
        data_rows = []
        for number, cells in sorted(rows.items()):
            if number < 3:
                continue
            code = (cells.get("B") or "").strip()
            if not code and not any(cells.get(col_name(index)) for index in range(1, 145)):
                continue
            data_rows.append(number)
            if code:
                seen.setdefault(code, []).append({"sheet": sheet_name, "row": number})
        counts[sheet_name] = len(data_rows)
        for number, cells in sorted(rows.items()):
            if number < 3:
                continue
            code = (cells.get("B") or "").strip()
            name = cells.get("A") or ""
            if code in targets or any(needle in name for needle in name_needles):
                full = {col_name(index): cells.get(col_name(index)) for index in range(1, 145)}
                hits.append({"sheet": sheet_name, "row": number, "A": name, "B": code, "full": full})
    for code, places in seen.items():
        if len(places) > 1:
            duplicate.append({"B": code, "places": places})

    archive_hits = []
    resource_hits = []
    for sheet_name in ("待制作原始数据", "资源状态明细", "数据表"):
        rows = parsed.get(sheet_name, {})
        for number, cells in sorted(rows.items()):
            blob = " ".join(str(value) for value in cells.values() if value)
            if any(token in blob for token in targets + name_needles):
                if sheet_name == "资源状态明细":
                    resource_hits.append({"sheet": sheet_name, "row": number, "cells": cells})
                elif sheet_name == "待制作原始数据":
                    archive_hits.append({
                        "sheet": sheet_name,
                        "row": number,
                        "A": cells.get("A"),
                        "B": cells.get("B"),
                        "N": cells.get("N"),
                        "O": cells.get("O"),
                        "DV": cells.get("DV"),
                    })
                else:
                    archive_hits.append({"sheet": sheet_name, "row": number, "A": cells.get("A"), "B": cells.get("B")})

    headers = {}
    for sheet_name in status[:1]:
        rows = parsed[sheet_name]
        headers = {
            "row1": {key: rows.get(1, {}).get(key) for key in ("A", "B", "N", "O", "P", "DV", "DW", "EN")},
            "row2": {col_name(index): rows.get(2, {}).get(col_name(index)) for index in range(1, 145)},
        }

    payload = {
        "path": str(LIVE),
        "sha256": sha256_file(LIVE),
        "mtime": datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="milliseconds"),
        "size": stat.st_size,
        "sheets": [item["name"] for item in sheets],
        "counts": counts,
        "duplicateB": duplicate,
        "headerSample": headers,
        "hits": hits,
        "archiveHits": archive_hits,
        "resourceHits": resource_hits,
    }
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({
        "sha256": payload["sha256"],
        "mtime": payload["mtime"],
        "size": payload["size"],
        "sheets": payload["sheets"],
        "counts": counts,
        "duplicateCount": len(duplicate),
        "hitCount": len(hits),
        "hits": [{"sheet": item["sheet"], "row": item["row"], "A": item["A"], "B": item["B"]} for item in hits],
        "archive": [{"sheet": item["sheet"], "row": item["row"], "A": item.get("A"), "B": item.get("B")} for item in archive_hits],
        "resourceRows": [item["row"] for item in resource_hits],
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
