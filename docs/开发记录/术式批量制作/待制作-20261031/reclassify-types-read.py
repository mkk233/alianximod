# -*- coding: utf-8 -*-
"""Read the live type list and every current status row. No writes to the workbook."""
import hashlib
import importlib.util
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
spec = importlib.util.spec_from_file_location("archive_write", HERE / "archive-write.py")
aw = importlib.util.module_from_spec(spec)
spec.loader.exec_module(aw)


def load(path):
    payload = path.read_bytes()
    with zipfile.ZipFile(path) as archive:
        parts = {info.filename: archive.read(info.filename) for info in archive.infolist()}
    shared = []
    if "xl/sharedStrings.xml" in parts:
        root = ET.fromstring(parts["xl/sharedStrings.xml"])
        for item in root:
            if aw.local(item.tag) == "si":
                texts = []
                for node in item.iter():
                    if aw.local(node.tag) == "t" and node.text:
                        texts.append(node.text)
                shared.append("".join(texts))
    names = aw.sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
    return payload, parts, shared, names


def letters(index):
    text = ""
    while index:
        index, remainder = divmod(index - 1, 26)
        text = chr(65 + remainder) + text
    return text


def row_map(xml, shared):
    found = {}
    for row in aw.rows_of(xml):
        number = aw.row_number(row)
        cells = {}
        for match in re.finditer(r'<(?:[\w]+:)?c r="([A-Z]+)(\d+)"', row):
            column, row_number = match.group(1), int(match.group(2))
            if row_number != number:
                continue
            span = aw.cell_span(row, column, number)
            value = aw.cell_text(span[0], shared) if span else None
            if value not in (None, ""):
                cells[column] = value
        if cells:
            found[number] = cells
    return found


def comments_of(parts):
    notes = []
    for name, payload in parts.items():
        if "comment" not in name.lower():
            continue
        text = payload.decode("utf-8", errors="replace")
        for match in re.finditer(r'ref="([A-Z]+\d+)"[\s\S]{0,800}?<t[^>]*>([\s\S]*?)</t>', text):
            notes.append({"part": name, "ref": match.group(1), "text": match.group(2)})
    return notes


def main():
    payload, parts, shared, names = load(LIVE)
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    defined = [
        match.group(0)
        for match in re.finditer(r"<definedName\b[^>]*>[^<]*</definedName>", workbook)
        if "术式类型" in match.group(0) or "$Q$" in match.group(0)
    ]
    data = row_map(parts[names["数据表"]].decode("utf-8"), shared)
    type_rows = []
    for number in range(1, 30):
        cells = data.get(number, {})
        picked = {column: cells[column] for column in ("P", "Q", "R", "S", "T") if column in cells}
        if picked:
            type_rows.append({"row": number, **picked})
    pages = {}
    duplicates = []
    seen = {}
    for title in ("已完成", "待制作", "制作中", "待验收"):
        xml = parts[names[title]].decode("utf-8")
        rows = row_map(xml, shared)
        header = {}
        for number in (1, 2):
            for column, value in rows.get(number, {}).items():
                header[column] = header.get(column, "")
                header[column] = (header[column] + " " + value).strip()
        entries = []
        for number, cells in rows.items():
            if number < 3 or "B" not in cells:
                continue
            code = cells["B"]
            entry = {"row": number, "cells": cells}
            entries.append(entry)
            seen.setdefault(code, []).append({"sheet": title, "row": number})
        validations = []
        for match in re.finditer(r"<dataValidation\b([^>]*)>([\s\S]*?)</dataValidation>", xml):
            body = match.group(0)
            if "术式类型" in body or "$Q$" in body:
                formula = re.search(r"<formula1>([^<]*)</formula1>", body)
                validations.append({"attrs": match.group(1).strip(), "formula1": formula.group(1) if formula else None})
        pages[title] = {"header": header, "rows": entries, "validations": validations, "count": len(entries)}
    for code, places in seen.items():
        if len(places) != 1:
            duplicates.append({"id": code, "places": places})
    info = {
        "sha256": hashlib.sha256(payload).hexdigest().upper(),
        "bytes": len(payload),
        "mtime": datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
        "definedNames": defined,
        "typeArea": type_rows,
        "comments": comments_of(parts),
        "pages": pages,
        "duplicates": duplicates,
        "sheetNames": list(names),
    }
    (HERE / "reclassify-types-20261101-read.json").write_text(
        json.dumps(info, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps({
        "sha256": info["sha256"],
        "mtime": info["mtime"],
        "bytes": info["bytes"],
        "definedNames": defined,
        "typeArea": type_rows,
        "counts": {title: pages[title]["count"] for title in pages},
        "duplicates": duplicates,
        "commentCount": len(info["comments"]),
        "headers": pages["已完成"]["header"],
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
