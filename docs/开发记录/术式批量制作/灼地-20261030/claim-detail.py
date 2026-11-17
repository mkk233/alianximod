# -*- coding: utf-8 -*-
"""Read resource-detail rows, archive tail, comments, and duplicate ids."""
import html
import json
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cells(row):
    found = {}
    for match in re.finditer(r"<(?:[\w]+:)?c\b[^>]*?(?:/>|>[\s\S]*?</(?:[\w]+:)?c>)", row):
        cell = match.group(0)
        ref = re.search(r'\br="([A-Z]+)(\d+)"', cell)
        if not ref:
            continue
        kind = re.search(r'\bt="([^"]+)"', cell)
        kind = kind.group(1) if kind else ""
        value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?v>", cell)
        inline = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", cell)
        formula = re.search(r"<(?:[\w]+:)?f\b", cell)
        text = None
        if inline:
            text = html.unescape("".join(inline))
        elif value:
            text = html.unescape(value.group(1))
        found[ref.group(1)] = {"text": text, "type": kind, "formula": bool(formula)}
    return found


def main():
    with zipfile.ZipFile(live) as archive:
        parts = {info.filename: archive.read(info.filename) for info in archive.infolist()}
    detail = parts["xl/worksheets/sheet6.xml"].decode("utf-8")
    archive_xml = parts["xl/worksheets/sheet7.xml"].decode("utf-8")
    pending = parts["xl/worksheets/sheet2.xml"].decode("utf-8")
    detail_rows = []
    for row in rows_of(detail):
        number = row_number(row)
        mapped = cells(row)
        if number < 3:
            continue
        detail_rows.append({
            "row": number,
            "id": (mapped.get("A") or {}).get("text"),
            "seq": (mapped.get("B") or {}).get("text"),
            "cells": {key: value["text"] for key, value in mapped.items() if value["text"] not in (None, "")},
        })
    archive_tail = []
    for row in rows_of(archive_xml):
        number = row_number(row)
        if number < 8:
            continue
        mapped = cells(row)
        archive_tail.append({
            "row": number,
            "nonempty": {key: value["text"] for key, value in mapped.items() if value["text"] not in (None, "")},
        })
    comment_refs = []
    for name in ("xl/comments/comment2.xml", "xl/drawings/commentsDrawing2.vml"):
        text = parts[name].decode("utf-8", errors="replace")
        refs = re.findall(r'\b(?:ref|r)="([A-Z]+\d+)"', text)
        comment_refs.append({"file": name, "refs": refs[:80], "count": len(refs), "hasScorch": "fire_scorch_field" in text or "灼地" in text})
    formulas = []
    for row in rows_of(pending):
        if "<f" in row or "<f " in row:
            formulas.append(row_number(row))
    ids = {"已完成": [], "待制作": [], "制作中": [], "待验收": []}
    paths = {"已完成": "xl/worksheets/sheet1.xml", "待制作": "xl/worksheets/sheet2.xml", "制作中": "xl/worksheets/sheet3.xml", "待验收": "xl/worksheets/sheet4.xml"}
    for name, path in paths.items():
        for row in rows_of(parts[path].decode("utf-8")):
            number = row_number(row)
            if number < 3:
                continue
            mapped = cells(row)
            code = (mapped.get("B") or {}).get("text")
            if code and str(code).strip():
                ids[name].append(str(code).strip())
    flat = [code for group in ids.values() for code in group]
    dupes = sorted({code for code in flat if flat.count(code) > 1})
    scorch_rows = [item for item in detail_rows if item["id"] == "fire_scorch_field"]
    print(json.dumps({
        "detailAll": detail_rows,
        "scorchDetail": scorch_rows,
        "archiveTail": archive_tail,
        "comments": comment_refs,
        "pendingFormulaRows": formulas,
        "dupes": dupes,
        "counts": {name: len(group) for name, group in ids.items()},
        "total": len(flat),
        "pendingHasRow4Comment": any("4" == re.search(r"(\d+)$", ref).group(1) for ref in comment_refs[0]["refs"]),
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
