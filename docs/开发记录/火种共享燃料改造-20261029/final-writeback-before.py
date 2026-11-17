# -*- coding: utf-8 -*-
"""Read the three consumer rows. Does not write the workbook."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
out_dir = root / "docs" / "开发记录" / "火种共享燃料改造-20261029"
source = root / "docs" / "术式制作表.xlsx"
wanted = ["fire_liaoyuan", "fire_fentian", "fire_liaoli"]
status = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}


def unescape(text):
    return html.unescape(text)


def rows_of(sheet_xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet_xml)


def cells_of(row_xml):
    found = {}
    for match in re.finditer(r"<(?:[\w]+:)?c\b([^>]*)>", row_xml):
        attrs = match.group(1)
        ref = re.search(r'\br="([A-Z]+)(\d+)"', attrs)
        if not ref or attrs.endswith("/"):
            continue
        column = ref.group(1)
        close = re.search(r"</(?:[\w]+:)?c>", row_xml[match.end():])
        inner = row_xml[match.end():match.end() + close.start()]
        inline = re.search(r"<(?:[\w]+:)?t\b[^>]*>([\s\S]*)</(?:[\w]+:)?t>", inner)
        value = re.search(r"<(?:[\w]+:)?v\b[^>]*>([\s\S]*)</(?:[\w]+:)?v>", inner)
        if inline:
            text, kind = unescape(inline.group(1)), "inlineStr"
        elif value:
            text = unescape(value.group(1))
            kind = "str" if 't="str"' in attrs else "n" if 't="n"' in attrs else "v"
        else:
            continue
        if text != "":
            found[column] = {"kind": kind, "text": text}
    return found


def row_cells(sheet_xml, number):
    for row_xml in rows_of(sheet_xml):
        if int(re.search(r'\br="(\d+)"', row_xml).group(1)) == number:
            return cells_of(row_xml)
    return {}


data = source.read_bytes()
stat = source.stat()
with zipfile.ZipFile(source) as archive:
    sheets = {name: archive.read(part).decode("utf-8") for name, part in status.items()}
    detail = archive.read("xl/worksheets/sheet6.xml").decode("utf-8")
    archive_xml = archive.read("xl/worksheets/sheet7.xml").decode("utf-8")
headers = {column: payload["text"] for column, payload in row_cells(sheets["制作中"], 2).items()}
located = []
items = []
for title, xml in sheets.items():
    for row_xml in rows_of(xml):
        number = int(re.search(r'\br="(\d+)"', row_xml).group(1))
        if number < 3:
            continue
        cells = cells_of(row_xml)
        code = cells.get("B", {}).get("text", "").strip()
        if not code:
            continue
        located.append({"id": code, "sheet": title, "row": number})
        if code in wanted:
            items.append({
                "id": code,
                "name": cells.get("A", {}).get("text"),
                "sheet": title,
                "row": number,
                "n": cells.get("N", {}).get("text"),
                "o": cells.get("O", {}).get("text"),
                "dv": cells.get("DV", {}).get("text"),
                "cells": {
                    column: {"header": headers.get(column), **payload}
                    for column, payload in cells.items()
                },
            })
detail_hits = []
for row_xml in rows_of(detail):
    number = int(re.search(r'\br="(\d+)"', row_xml).group(1))
    if number < 3:
        continue
    cells = cells_of(row_xml)
    code = cells.get("A", {}).get("text", "").strip()
    if code in wanted:
        detail_hits.append({"row": number, "id": code, "cells": cells})
archive_hits = []
for row_xml in rows_of(archive_xml):
    number = int(re.search(r'\br="(\d+)"', row_xml).group(1))
    if number < 3:
        continue
    cells = cells_of(row_xml)
    code = cells.get("B", {}).get("text", "").strip()
    if code in wanted:
        archive_hits.append({"row": number, "id": code})
counts = {}
for title in status:
    counts[title] = sum(1 for item in located if item["sheet"] == title)
document = {
    "purpose": "迁待验收前的现行三消费者原文。含 A:EN。不是待制作原始档案。",
    "readAt": datetime.now().astimezone().isoformat(timespec="seconds"),
    "workbook": {
        "path": "docs/术式制作表.xlsx",
        "sha256": hashlib.sha256(data).hexdigest().upper(),
        "lastWriteTime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "bytes": len(data),
    },
    "counts": counts,
    "uniqueIds": len({item["id"] for item in located}),
    "duplicateIds": sorted({item["id"] for item in located if sum(1 for other in located if other["id"] == item["id"]) > 1}),
    "items": items,
    "resourceDetailHits": detail_hits,
    "archiveHits": archive_hits,
}
(out_dir / "final-writeback-before.json").write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "sha": document["workbook"]["sha256"],
    "mtime": document["workbook"]["lastWriteTime"],
    "bytes": document["workbook"]["bytes"],
    "counts": counts,
    "unique": document["uniqueIds"],
    "duplicates": document["duplicateIds"],
    "items": [
        {
            "id": item["id"],
            "name": item["name"],
            "sheet": item["sheet"],
            "row": item["row"],
            "nLen": len(item["n"] or ""),
            "oLen": len(item["o"] or ""),
            "dvLen": len(item["dv"] or ""),
            "resourceColumns": [
                column for column, cell in item["cells"].items()
                if (cell.get("header") or "").find("火") >= 0 or column >= "DW"
            ],
        }
        for item in items
    ],
    "detail": detail_hits,
    "archive": archive_hits,
}, ensure_ascii=False, indent=2))
