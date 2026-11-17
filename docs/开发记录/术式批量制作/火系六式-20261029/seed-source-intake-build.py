# -*- coding: utf-8 -*-
"""Read the live workbook for the seed-source intake. Does not write the workbook."""
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
process_path = root / "docs" / "统一开发与术式批量制作流程.md"
output_path = batch / "seed-source-input.json"
names = {"点火": None, "热光束": None, "火球": None}
six = [
    "fire_liaoyuan",
    "fire_liaoli",
    "fire_fentian",
    "fire_heat_up",
    "fire_burn",
    "fire_cool",
]
status_order = ["已完成", "待制作", "制作中", "待验收"]


def fail(message):
    raise SystemExit(message)


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
        if not ref or attrs.endswith("/"):
            continue
        column = ref.group(1)
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


def row_cells(sheet_xml, number):
    for row_xml in rows_of(sheet_xml):
        if row_number_of(row_xml) == number:
            return cells_of(row_xml)
    return {}


def column_index(column):
    value = 0
    for char in column:
        value = value * 26 + ord(char) - 64
    return value


def merges_of(sheet_xml):
    found = []
    for match in re.finditer(r'<(?:[\w]+:)?mergeCell\b[^>]*\bref="([A-Z]+)(\d+):([A-Z]+)(\d+)"', sheet_xml):
        found.append((match.group(1), int(match.group(2)), match.group(3), int(match.group(4))))
    return found


def group_for(column, row1, merges):
    index = column_index(column)
    for start_column, start_row, end_column, end_row in merges:
        if start_row != 1 or end_row != 1:
            continue
        if column_index(start_column) <= index <= column_index(end_column):
            return row1.get(start_column, {}).get("text")
    return row1.get(column, {}).get("text")


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


source_bytes = source.read_bytes()
source_stat = source.stat()
prior = json.loads((batch / "unlock-intake.json").read_text(encoding="utf-8"))
prior_by_id = {item["id"]: item for item in prior["items"]}
with zipfile.ZipFile(source) as archive:
    workbook = archive.read("xl/workbook.xml").decode("utf-8")
    sheet_refs = re.findall(r'<sheet [^>]*name="([^"]+)"[^>]*r:id="([^"]+)"', workbook)
    rels = archive.read("xl/_rels/workbook.xml.rels").decode("utf-8")
    rel_map = {}
    for match in re.finditer(r"<Relationship\b([^>]*)/?>", rels):
        attrs = match.group(1)
        rel_id = re.search(r'\bId="([^"]+)"', attrs).group(1)
        target = re.search(r'\bTarget="([^"]+)"', attrs).group(1)
        rel_map[rel_id] = target
    loaded = {}
    for name, rel in sheet_refs:
        target = rel_map[rel]
        if target.startswith("/"):
            path = target.lstrip("/")
        elif target.startswith("xl/"):
            path = target
        else:
            path = "xl/" + target
        loaded[name] = archive.read(path).decode("utf-8")

missing = [name for name in status_order + ["资源状态明细", "待制作原始数据"] if name not in loaded]
if missing:
    fail(f"Missing sheets {missing}")

headers_sheet = loaded["制作中"]
row1 = row_cells(headers_sheet, 1)
row2 = row_cells(headers_sheet, 2)
merges = merges_of(headers_sheet)
headers = {column: payload["text"] for column, payload in row2.items()}

counts = {}
located = []
by_sheet = {}
for title in status_order:
    records = sheet_records(loaded[title])
    counts[title] = len(records)
    by_sheet[title] = records
    located.extend({"id": code, "name": cells.get("A", {}).get("text"), "sheet": title, "row": number} for number, code, cells in records)
codes = [item["id"] for item in located]
duplicates = sorted({code for code in codes if codes.count(code) > 1})

exact = []
for item in located:
    if item["name"] in names:
        exact.append(item)
name_counts = {}
for item in exact:
    name_counts[item["name"]] = name_counts.get(item["name"], 0) + 1

archive_headers = {column: payload["text"] for column, payload in row_cells(loaded["待制作原始数据"], 2).items()}
archive_records = sheet_records(loaded["待制作原始数据"])
archive_hits = []
for number, code, cells in archive_records:
    label = cells.get("A", {}).get("text")
    if code in {item["id"] for item in exact} or label in names:
        archive_hits.append({"id": code, "name": label, "row": number})

detail_headers = {column: payload["text"] for column, payload in row_cells(loaded["资源状态明细"], 2).items()}
detail_hits = []
detail_inventory = []
for row_xml in rows_of(loaded["资源状态明细"]):
    number = row_number_of(row_xml)
    if number < 3:
        continue
    cells = cells_of(row_xml)
    technique = cells.get("A", {}).get("text", "").strip()
    if not technique:
        continue
    detail_inventory.append({"row": number, "id": technique, "sequence": cells.get("B", {}).get("text")})
    if technique in {item["id"] for item in exact} or technique in six:
        detail_hits.append({
            "row": number,
            "id": technique,
            "cells": {
                column: {"header": detail_headers.get(column), **payload}
                for column, payload in cells.items()
            },
        })

def pack(cells):
    packed = {}
    for column, payload in cells.items():
        packed[column] = {
            "group": group_for(column, row1, merges),
            "header": headers.get(column),
            **payload,
        }
    return dict(sorted(packed.items(), key=lambda item: column_index(item[0])))


items = []
for item in exact:
    _number, _code, cells = next(record for record in by_sheet[item["sheet"]] if record[1] == item["id"])
    populated = pack(cells)
    items.append({
        "id": item["id"],
        "name": item["name"],
        "sheet": item["sheet"],
        "row": item["row"],
        "n": cells.get("N", {}).get("text"),
        "o": cells.get("O", {}).get("text"),
        "dv": cells.get("DV", {}).get("text"),
        "releaseKind": cells.get("P", {}).get("text"),
        "releaseTag": cells.get("Q", {}).get("text"),
        "trajectory": cells.get("R", {}).get("text"),
        "chainRole": cells.get("S", {}).get("text"),
        "populatedColumns": sorted(populated, key=column_index),
        "cells": populated,
        "resourceDetails": [row for row in detail_hits if row["id"] == item["id"]],
        "archiveRows": [row for row in archive_hits if row["id"] == item["id"]],
    })

six_now = []
for code in six:
    hits = [item for item in located if item["id"] == code]
    prior_item = prior_by_id[code]
    if len(hits) != 1:
        six_now.append({"id": code, "occurrences": hits})
        continue
    hit = hits[0]
    _number, _code, cells = next(record for record in by_sheet[hit["sheet"]] if record[1] == code)
    six_now.append({
        "id": code,
        "name": hit["name"],
        "sheet": hit["sheet"],
        "row": hit["row"],
        "nEqualToUnlockIntake": cells.get("N", {}).get("text") == prior_item["n"],
        "oEqualToUnlockIntake": cells.get("O", {}).get("text") == prior_item["o"],
        "dvEqualToUnlockIntake": cells.get("DV", {}).get("text") == prior_item["dv"],
        "hasImprintFeedback": "刻印台实测反馈" in (cells.get("DV", {}).get("text") or ""),
    })

document = {
    "purpose": "火种来源新增的需求与表对账。只采集，不改表，不迁移，不判断实现是否正确。",
    "readAt": datetime.now().astimezone().isoformat(timespec="seconds"),
    "workbook": {
        "path": "docs/术式制作表.xlsx",
        "sha256": hashlib.sha256(source_bytes).hexdigest().upper(),
        "lastWriteTime": datetime.fromtimestamp(source_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "bytes": len(source_bytes),
        "modified": False,
        "previousWritebackSha256": "B7461BA4578F7E7AF3C549E58FFAE0746D5221C28C4E106AEEBC6F62776F6435",
    },
    "process": {
        "path": "docs/统一开发与术式批量制作流程.md",
        "sha256": hashlib.sha256(process_path.read_bytes()).hexdigest().upper(),
    },
    "sheetNames": [name for name, _rel in sheet_refs],
    "counts": counts,
    "totalStatusRows": len(codes),
    "uniqueStatusIds": len(set(codes)),
    "duplicateStatusIds": duplicates,
    "archiveRowCount": len(archive_records),
    "nameMatchCounts": name_counts,
    "items": items,
    "archiveHits": archive_hits,
    "resourceDetailInventory": detail_inventory,
    "six": six_now,
}
output_path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "sha256": document["workbook"]["sha256"],
    "mtime": document["workbook"]["lastWriteTime"],
    "bytes": document["workbook"]["bytes"],
    "processSha256": document["process"]["sha256"],
    "hashUnchangedSinceUnlockWriteback": document["workbook"]["sha256"] == document["workbook"]["previousWritebackSha256"],
    "counts": counts,
    "unique": document["uniqueStatusIds"],
    "duplicates": duplicates,
    "archiveRows": document["archiveRowCount"],
    "archiveHits": archive_hits,
    "detailInventory": detail_inventory,
    "items": [
        {
            "id": item["id"],
            "name": item["name"],
            "sheet": item["sheet"],
            "row": item["row"],
            "n": item["n"],
            "o": item["o"],
            "dv": item["dv"],
            "releaseKind": item["releaseKind"],
            "releaseTag": item["releaseTag"],
            "trajectory": item["trajectory"],
            "chainRole": item["chainRole"],
            "columns": item["populatedColumns"],
            "resourceDetails": len(item["resourceDetails"]),
            "archiveRows": item["archiveRows"],
        }
        for item in items
    ],
    "six": six_now,
}, ensure_ascii=False, indent=2))
