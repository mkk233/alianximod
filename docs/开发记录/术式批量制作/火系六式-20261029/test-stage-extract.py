# -*- coding: utf-8 -*-
"""Read the live workbook and the formal input snapshot. Does not write the workbook."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "术式批量制作" / "火系六式-20261029"
source = root / "docs" / "术式制作表.xlsx"
snapshot_path = batch / "input-snapshot.json"
process_path = root / "docs" / "统一开发与术式批量制作流程.md"
output_path = batch / "test-stage-input.json"
wanted = [
    "fire_liaoyuan",
    "fire_liaoli",
    "fire_fentian",
    "fire_heat_up",
    "fire_burn",
    "fire_cool",
]
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
key_columns = {
    "A": "name",
    "B": "id",
    "C": "type",
    "D": "attribute",
    "N": "functionText",
    "O": "effectText",
    "P": "releaseKind",
    "Q": "releaseTag",
    "R": "trajectory",
    "S": "chainRole",
    "DV": "note",
}


def fail(message):
    raise SystemExit(message)


def file_sha(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def unescape(text):
    return html.unescape(text)


def rows_of(sheet_xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet_xml)


def row_number_of(row_xml):
    return int(re.search(r'\br="(\d+)"', row_xml).group(1))


def cells_of(row_xml):
    found = {}
    for match in re.finditer(r"<(?:[\w]+:)?c\b([^>]*)>", row_xml):
        attrs = match.group(1)
        ref = re.search(r'\br="([A-Z]+)(\d+)"', attrs)
        if not ref:
            continue
        column = ref.group(1)
        self_closing = attrs.endswith("/")
        if self_closing:
            continue
        close = re.search(r"</(?:[\w]+:)?c>", row_xml[match.end():])
        if not close:
            fail(f"Unclosed cell {column}{ref.group(2)}")
        inner = row_xml[match.end():match.end() + close.start()]
        inline = re.search(r"<(?:[\w]+:)?t\b[^>]*>([\s\S]*)</(?:[\w]+:)?t>", inner)
        value = re.search(r"<(?:[\w]+:)?v\b[^>]*>([\s\S]*)</(?:[\w]+:)?v>", inner)
        if inline:
            text = unescape(inline.group(1))
            kind = "inlineStr"
        elif value:
            text = unescape(value.group(1))
            kind = "str" if 't="str"' in attrs else "n" if 't="n"' in attrs else "v"
        else:
            continue
        if text == "":
            continue
        found[column] = {"kind": kind, "text": text}
    return found


def headers_of(sheet_xml):
    for row_xml in rows_of(sheet_xml):
        if row_number_of(row_xml) == 2:
            return {column: payload["text"] for column, payload in cells_of(row_xml).items()}
    return {}


def sheet_records(sheet_xml):
    records = []
    for row_xml in rows_of(sheet_xml):
        number = row_number_of(row_xml)
        if number < 3:
            continue
        cells = cells_of(row_xml)
        code = cells.get("B", {}).get("text", "").strip()
        if code:
            records.append((number, code, cells))
    return records


def snapshot_rows(data):
    found = {}
    for entry in data["sheets"]["待制作"]:
        cells = entry.get("cells") or {}
        number = entry.get("row")
        code = cells.get(f"B{number}")
        if code in wanted:
            found[code] = {
                "sheet": "待制作",
                "row": number,
                "name": cells.get(f"A{number}"),
                "n": cells.get(f"N{number}"),
                "o": cells.get(f"O{number}"),
                "dv": cells.get(f"DV{number}"),
            }
    listed = [item for item in data.get("ids", []) if item and item[0] in wanted]
    return found, listed


source_bytes = source.read_bytes()
source_stat = source.stat()
with zipfile.ZipFile(source) as archive:
    sheets = {name: archive.read(part).decode("utf-8") for name, part in status_parts.items()}
    detail_xml = archive.read("xl/worksheets/sheet6.xml").decode("utf-8")

counts = {}
located = []
by_sheet = {}
for title, xml in sheets.items():
    records = sheet_records(xml)
    counts[title] = len(records)
    by_sheet[title] = records
    located.extend({"id": code, "sheet": title, "row": number} for number, code, _cells in records)
codes = [item["id"] for item in located]
duplicates = sorted({code for code in codes if codes.count(code) > 1})
headers = headers_of(sheets["制作中"])
detail_headers = headers_of(detail_xml)
detail_inventory = []
detail_rows = []
for row_xml in rows_of(detail_xml):
    number = row_number_of(row_xml)
    if number < 3:
        continue
    cells = cells_of(row_xml)
    technique = cells.get("A", {}).get("text", "").strip()
    if not technique:
        continue
    packed = {
        column: {"header": detail_headers.get(column), **payload}
        for column, payload in cells.items()
    }
    entry = {
        "row": number,
        "id": technique,
        "sequence": cells.get("B", {}).get("text"),
        "cells": packed,
    }
    detail_inventory.append(entry)
    if technique in wanted:
        detail_rows.append(entry)

snapshot_data = json.loads(snapshot_path.read_text(encoding="utf-8-sig"))
snapshot, snapshot_listed = snapshot_rows(snapshot_data)
items = []
for code in wanted:
    hits = [item for item in located if item["id"] == code]
    if len(hits) != 1:
        fail(f"{code} occurrences {hits}")
    hit = hits[0]
    _number, _code, cells = next(record for record in by_sheet[hit["sheet"]] if record[1] == code)
    current = {name: cells.get(column, {}).get("text") for column, name in key_columns.items()}
    source_row = snapshot.get(code)
    if source_row is None:
        comparison = {"present": False}
    else:
        current_dv = current["note"] or ""
        old_dv = source_row["dv"] or ""
        prefix = current_dv.startswith(old_dv)
        comparison = {
            "present": True,
            "sheet": source_row["sheet"],
            "row": source_row["row"],
            "nameEqual": current["name"] == source_row["name"],
            "nEqual": current["functionText"] == source_row["n"],
            "oEqual": current["effectText"] == source_row["o"],
            "dvPrefixEqual": prefix,
            "dvSuffix": current_dv[len(old_dv):] if prefix else None,
        }
    populated = {
        column: {"header": headers.get(column), **payload}
        for column, payload in cells.items()
    }
    items.append({
        "id": code,
        "name": current["name"],
        "sheet": hit["sheet"],
        "row": hit["row"],
        "sheetStage": hit["sheet"],
        "batchStage": "待测试",
        "occurrences": hits,
        "type": current["type"],
        "attribute": current["attribute"],
        "releaseKind": current["releaseKind"],
        "releaseTag": current["releaseTag"],
        "trajectory": current["trajectory"],
        "chainRole": current["chainRole"],
        "n": current["functionText"],
        "o": current["effectText"],
        "dv": current["note"],
        "populatedCells": populated,
        "resourceDetails": [row for row in detail_rows if row["id"] == code],
        "snapshotComparison": comparison,
    })

document = {
    "purpose": "测试阶段输入。来自现行制作表，不是待验收转换，也不是正式测试复审。",
    "readAt": datetime.now().astimezone().isoformat(timespec="seconds"),
    "workbook": {
        "path": "docs/术式制作表.xlsx",
        "sha256": hashlib.sha256(source_bytes).hexdigest().upper(),
        "lastWriteTime": datetime.fromtimestamp(source_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "bytes": len(source_bytes),
        "modified": False,
    },
    "process": {
        "path": "docs/统一开发与术式批量制作流程.md",
        "sha256": file_sha(process_path),
    },
    "inputSnapshot": {
        "path": "docs/开发记录/术式批量制作/火系六式-20261029/input-snapshot.json",
        "sha256": file_sha(snapshot_path),
        "listed": [
            {"id": item[0], "sheet": item[1], "row": item[2]}
            for item in snapshot_listed
        ],
    },
    "counts": counts,
    "total": len(codes),
    "unique": len(set(codes)),
    "duplicateIds": duplicates,
    "allIds": located,
    "resourceDetailIdColumn": "A",
    "resourceDetailInventory": detail_inventory,
    "resourceDetailMatches": [row["id"] for row in detail_rows],
    "items": items,
}
output_path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "sha256": document["workbook"]["sha256"],
    "mtime": document["workbook"]["lastWriteTime"],
    "bytes": document["workbook"]["bytes"],
    "processSha256": document["process"]["sha256"],
    "counts": counts,
    "total": document["total"],
    "unique": document["unique"],
    "duplicates": duplicates,
    "resourceDetailInventory": [
        {"id": row["id"], "row": row["row"], "sequence": row["sequence"]}
        for row in detail_inventory
    ],
    "items": [
        {
            "id": item["id"],
            "name": item["name"],
            "sheet": item["sheet"],
            "row": item["row"],
            "type": item["type"],
            "attribute": item["attribute"],
            "releaseKind": item["releaseKind"],
            "releaseTag": item["releaseTag"],
            "trajectory": item["trajectory"],
            "chainRole": item["chainRole"],
            "nameEqual": item["snapshotComparison"].get("nameEqual"),
            "populated": sorted(item["populatedCells"]),
            "resourceDetails": len(item["resourceDetails"]),
            "nEqual": item["snapshotComparison"].get("nEqual"),
            "oEqual": item["snapshotComparison"].get("oEqual"),
            "dvPrefixEqual": item["snapshotComparison"].get("dvPrefixEqual"),
            "dvSuffixLength": None if item["snapshotComparison"].get("dvSuffix") is None else len(item["snapshotComparison"]["dvSuffix"]),
            "seedNote": "FireSeedState.grant" in (item["snapshotComparison"].get("dvSuffix") or ""),
            "snapshotSheet": item["snapshotComparison"].get("sheet"),
            "snapshotRow": item["snapshotComparison"].get("row"),
        }
        for item in items
    ],
}, ensure_ascii=False, indent=2))
