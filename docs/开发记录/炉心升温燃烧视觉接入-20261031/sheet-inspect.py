# -*- coding: utf-8 -*-
"""Read-only locate fire_heat_up and fire_burn before sheet writeback."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
OUT = Path(r"D:\alianximod\docs\开发记录\炉心升温燃烧视觉接入-20261031\sheet-inspect.json")
TARGETS = ("fire_heat_up", "fire_burn", "fire_cool")


def local(tag):
    return tag.rsplit("}", 1)[-1]


def sha256_bytes(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def col_num(column):
    value = 0
    for char in column:
        value = value * 26 + ord(char) - 64
    return value


def col_name(index):
    name = ""
    while index:
        index, rem = divmod(index - 1, 26)
        name = chr(65 + rem) + name
    return name


def cell_span(row, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row.find(">", start)
    if row[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</(?:[\w]+:)?c>", row[opener_end:])
        if not close:
            raise SystemExit(f"Cell {column}{number} is not closed")
        end = opener_end + close.end()
    return row[start:end], start, end


def shared_list(xml):
    if not xml:
        return []
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
    inline = re.search(r"<(?:[\w]+:)?is>([\s\S]*)</(?:[\w]+:)?is>", cell)
    if inline:
        texts = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", inline.group(1))
        return html.unescape("".join(texts))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if not value:
        return None
    raw = html.unescape(value.group(1))
    if re.search(r'\bt="s"', cell) and raw.isdigit():
        return shared[int(raw)]
    return raw


def sheet_map(workbook_xml, rel_xml):
    targets = {}
    rels = ET.fromstring(rel_xml)
    for rel in rels:
        if local(rel.tag) != "Relationship":
            continue
        target = rel.attrib["Target"].lstrip("/")
        if not target.startswith("xl/"):
            target = "xl/" + target
        targets[rel.attrib["Id"]] = target
    found = {}
    root = ET.fromstring(workbook_xml)
    for sheet in root.iter():
        if local(sheet.tag) != "sheet":
            continue
        rid = sheet.attrib["{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"]
        found[sheet.attrib["name"]] = targets[rid]
    return found


def cells_of(row, number, shared):
    found = {}
    for match in re.finditer(r'<(?:[\w]+:)?c r="([A-Z]+)(\d+)"', row):
        column = match.group(1)
        if int(match.group(2)) != number:
            continue
        span = cell_span(row, column, number)
        cell = span[0]
        kind = "inline" if 't="inlineStr"' in cell else ("shared" if 't="s"' in cell else "other")
        found[column] = {
            "text": cell_text(cell, shared),
            "kind": kind,
            "style": (re.search(r'\ss="(\d+)"', cell) or [None, None])[1],
        }
    return found


def main():
    data = LIVE.read_bytes()
    stat = LIVE.stat()
    with zipfile.ZipFile(LIVE) as archive:
        parts = {name: archive.read(name) for name in archive.namelist()}
    shared = shared_list(parts.get("xl/sharedStrings.xml"))
    names = sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
    status = ("已完成", "待制作", "制作中", "待验收")
    counts = {}
    locations = {code: [] for code in TARGETS}
    all_ids = []
    for title in status:
        xml = parts[names[title]].decode("utf-8")
        page_ids = []
        for row in rows_of(xml):
            number = row_number(row)
            if number < 3:
                continue
            span = cell_span(row, "B", number)
            code = cell_text(span[0], shared) if span else None
            if not code:
                continue
            page_ids.append(code)
            all_ids.append(code)
            if code in locations:
                locations[code].append({"sheet": title, "row": number, "part": names[title]})
        counts[title] = len(page_ids)
    archive_name = names.get("待制作原始数据")
    detail_name = names.get("资源状态明细")
    archive_hits = []
    if archive_name:
        xml = parts[archive_name].decode("utf-8")
        for row in rows_of(xml):
            number = row_number(row)
            if number < 3:
                continue
            span = cell_span(row, "B", number)
            code = cell_text(span[0], shared) if span else None
            if code in ("fire_heat_up", "fire_burn", "fire_cool"):
                archive_hits.append({"id": code, "row": number})
    detail_hits = []
    if detail_name:
        xml = parts[detail_name].decode("utf-8")
        for row in rows_of(xml):
            number = row_number(row)
            span = cell_span(row, "A", number)
            code = cell_text(span[0], shared) if span else None
            if code in ("fire_heat_up", "fire_burn", "fire_cool"):
                detail_hits.append({"id": code, "row": number, "cells": list(cells_of(row, number, shared).keys())})
    making = parts[names["制作中"]].decode("utf-8")
    header_row = None
    for row in rows_of(making):
        if row_number(row) == 2:
            header_row = cells_of(row, 2, shared)
            break
    items = {}
    for code in ("fire_heat_up", "fire_burn"):
        hits = [item for item in locations[code] if item["sheet"] == "制作中"]
        if len(hits) != 1:
            items[code] = {"error": "not unique in 制作中", "locations": locations[code]}
            continue
        hit = hits[0]
        xml = parts[hit["part"]].decode("utf-8")
        target = None
        for row in rows_of(xml):
            if row_number(row) == hit["row"]:
                target = row
                break
        cells = cells_of(target, hit["row"], shared)
        ordered = []
        for index in range(1, 145):
            column = col_name(index)
            cell = cells.get(column)
            ordered.append({
                "column": column,
                "header": (header_row or {}).get(column, {}).get("text"),
                "text": None if cell is None else cell["text"],
                "kind": None if cell is None else cell["kind"],
                "style": None if cell is None else cell["style"],
            })
        items[code] = {
            "sheet": hit["sheet"],
            "row": hit["row"],
            "columnCount": len(ordered),
            "filled": sum(1 for cell in ordered if cell["text"] not in (None, "")),
            "N": next(cell["text"] for cell in ordered if cell["column"] == "N"),
            "O": next(cell["text"] for cell in ordered if cell["column"] == "O"),
            "DV": next(cell["text"] for cell in ordered if cell["column"] == "DV"),
            "Okind": next(cell["kind"] for cell in ordered if cell["column"] == "O"),
            "DVkind": next(cell["kind"] for cell in ordered if cell["column"] == "DV"),
            "row": ordered,
        }
    report = {
        "sha256": sha256_bytes(data),
        "bytes": len(data),
        "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "sheets": names,
        "counts": counts,
        "uniqueStatusIds": len(set(all_ids)),
        "statusIdCount": len(all_ids),
        "locations": locations,
        "archiveHits": archive_hits,
        "detailHits": detail_hits,
        "items": {
            code: {
                "sheet": payload.get("sheet"),
                "rowNumber": payload.get("row") if isinstance(payload.get("row"), int) else None,
                "filled": payload.get("filled"),
                "Okind": payload.get("Okind"),
                "DVkind": payload.get("DVkind"),
                "N": payload.get("N"),
                "O": payload.get("O"),
                "DV": payload.get("DV"),
                "nonempty": [
                    {"column": cell["column"], "header": cell["header"], "text": cell["text"]}
                    for cell in payload.get("row", [])
                    if isinstance(payload.get("row"), list) and cell["text"] not in (None, "")
                ] if isinstance(payload.get("row"), list) else payload,
            }
            for code, payload in items.items()
        },
    }
    # Fix row number: the loop overwrote. Recompute from locations.
    for code in ("fire_heat_up", "fire_burn"):
        hits = [item for item in locations[code] if item["sheet"] == "制作中"]
        report["items"][code]["rowNumber"] = hits[0]["row"] if len(hits) == 1 else None
    OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({
        "sha256": report["sha256"],
        "counts": counts,
        "locations": locations,
        "archiveHits": archive_hits,
        "detailHits": detail_hits,
        "rows": {code: report["items"][code]["rowNumber"] for code in ("fire_heat_up", "fire_burn")},
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
