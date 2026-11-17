# -*- coding: utf-8 -*-
"""Read-only inventory of the live technique workbook. Does not write the workbook."""
import html
import json
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
out = Path(r"D:\alianximod\docs\开发记录\术式批量制作\灼地-20261030\claim-inventory.json")


def col_letter(index):
    letters = ""
    while index:
        index, remainder = divmod(index - 1, 26)
        letters = chr(65 + remainder) + letters
    return letters


def col_index(letters):
    value = 0
    for char in letters:
        value = value * 26 + ord(char) - 64
    return value


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def load_shared(parts):
    raw = parts.get("xl/sharedStrings.xml")
    if not raw:
        return []
    xml = raw.decode("utf-8")
    items = re.findall(r"<(?:[\w]+:)?si\b[^>]*>([\s\S]*?)</(?:[\w]+:)?si>", xml)
    strings = []
    for item in items:
        texts = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", item)
        strings.append(html.unescape("".join(texts)))
    return strings


def cell_map(row, shared):
    number = row_number(row)
    found = {}
    for match in re.finditer(r"<(?:[\w]+:)?c\b[^>]*?(?:/>|>[\s\S]*?</(?:[\w]+:)?c>)", row):
        cell = match.group(0)
        ref = re.search(r'\br="([A-Z]+)(\d+)"', cell)
        if not ref:
            continue
        column = ref.group(1)
        kind = re.search(r'\bt="([^"]+)"', cell)
        kind = kind.group(1) if kind else ""
        style = re.search(r'\bs="([^"]+)"', cell)
        formula = re.search(r"<(?:[\w]+:)?f\b[^>]*>([\s\S]*?)</(?:[\w]+:)?f>", cell)
        value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?v>", cell)
        inline = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", cell)
        text = None
        if kind == "s" and value:
            text = shared[int(value.group(1))]
        elif kind == "inlineStr" or inline:
            text = html.unescape("".join(inline))
        elif value:
            text = html.unescape(value.group(1))
        found[column] = {
            "text": text,
            "type": kind,
            "style": style.group(1) if style else None,
            "formula": html.unescape(formula.group(1)) if formula else None,
        }
    return number, found


def sheet_targets(workbook_xml, rels_xml):
    rels = {}
    for tag in re.findall(r"<Relationship\b[^>]*>", rels_xml):
        rid = re.search(r'\bId="([^"]+)"', tag).group(1)
        target = re.search(r'\bTarget="([^"]+)"', tag).group(1)
        rels[rid] = target
    sheets = []
    for tag in re.findall(r"<sheet\b[^>]*>", workbook_xml):
        name = html.unescape(re.search(r'\bname="([^"]+)"', tag).group(1))
        rid = re.search(r'\br:id="([^"]+)"', tag).group(1)
        target = rels[rid]
        if target.startswith("/"):
            path = target.lstrip("/")
        elif target.startswith("worksheets/"):
            path = "xl/" + target
        else:
            path = "xl/" + target.lstrip("/")
        sheets.append({"name": name, "rid": rid, "path": path})
    return sheets


def defined_names(workbook_xml):
    return re.findall(r'<definedName[^>]*name="([^"]+)"[^>]*>([\s\S]*?)</definedName>', workbook_xml)


def main():
    with zipfile.ZipFile(live) as archive:
        parts = {info.filename: archive.read(info.filename) for info in archive.infolist()}
    shared = load_shared(parts)
    workbook_xml = parts["xl/workbook.xml"].decode("utf-8")
    rels_xml = parts["xl/_rels/workbook.xml.rels"].decode("utf-8")
    sheets = sheet_targets(workbook_xml, rels_xml)
    parsed = {}
    for sheet in sheets:
        xml = parts[sheet["path"]].decode("utf-8")
        rows = []
        for row in rows_of(xml):
            number, cells = cell_map(row, shared)
            rows.append({"row": number, "cells": cells})
        validations = re.findall(r"<dataValidation\b[\s\S]*?</dataValidation>", xml)
        validation_brief = []
        for item in validations:
            sqref = re.search(r'sqref="([^"]+)"', item)
            formula = re.search(r"<(?:[\w]+:)?formula1[^>]*>([\s\S]*?)</(?:[\w]+:)?formula1>", item)
            validation_brief.append({
                "sqref": sqref.group(1) if sqref else None,
                "formula1": html.unescape(formula.group(1)) if formula else None,
                "type": re.search(r'type="([^"]+)"', item).group(1) if re.search(r'type="([^"]+)"', item) else None,
            })
        rel_path = sheet["path"].replace("xl/worksheets/", "xl/worksheets/_rels/") + ".rels"
        rel_xml = parts.get(rel_path, b"").decode("utf-8")
        parsed[sheet["name"]] = {
            "path": sheet["path"],
            "rowCount": len(rows),
            "rowNumbers": [item["row"] for item in rows],
            "validations": validation_brief,
            "rels": rel_xml,
            "dimension": re.search(r'<dimension ref="([^"]+)"', xml).group(1) if re.search(r'<dimension ref="([^"]+)"', xml) else None,
            "rows": rows,
        }
    comments = {}
    for name, blob in parts.items():
        if "comment" in name.lower() or name.endswith(".vml"):
            comments[name] = blob.decode("utf-8", errors="replace")[:20000]
    status_names = ["已完成", "待制作", "制作中", "待验收"]
    ids = {}
    for name in status_names + ["待制作原始数据", "资源状态明细", "数据表"]:
        sheet = parsed.get(name)
        if not sheet:
            ids[name] = None
            continue
        found = []
        for row in sheet["rows"]:
            if row["row"] < 3:
                continue
            text = (row["cells"].get("B") or {}).get("text")
            if text and str(text).strip():
                found.append({"row": row["row"], "id": str(text).strip(), "name": (row["cells"].get("A") or {}).get("text")})
        ids[name] = found

    def headers(name):
        sheet = parsed[name]
        by_row = {item["row"]: item["cells"] for item in sheet["rows"]}
        result = {}
        for column, cell in by_row.get(2, {}).items():
            result[column] = {"row2": cell.get("text"), "row1": (by_row.get(1, {}).get(column) or {}).get("text")}
        return result

    target = None
    for name in status_names:
        for item in ids[name] or []:
            if item["name"] == "灼地" or item["id"] == "fire_scorch_field":
                target = {"sheet": name, **item}
    detail_headers = headers("资源状态明细") if "资源状态明细" in parsed else {}
    archive_headers = headers("待制作原始数据") if "待制作原始数据" in parsed else {}
    pending_headers = headers("待制作")
    payload = {
        "sheets": [{"name": item["name"], "path": item["path"]} for item in sheets],
        "definedNames": [{"name": name, "ref": ref[:300]} for name, ref in defined_names(workbook_xml)],
        "counts": {name: None if ids[name] is None else len(ids[name]) for name in ids},
        "ids": ids,
        "target": target,
        "pendingHeaders": pending_headers,
        "detailHeaders": detail_headers,
        "archiveHeaders": archive_headers,
        "commentFiles": list(comments),
        "sharedCount": len(shared),
    }
    if target:
        sheet = parsed[target["sheet"]]
        row = next(item for item in sheet["rows"] if item["row"] == target["row"])
        nonempty = []
        for column, cell in row["cells"].items():
            if cell["text"] not in (None, ""):
                header = pending_headers.get(column, {})
                nonempty.append({
                    "column": column,
                    "row1": header.get("row1"),
                    "row2": header.get("row2"),
                    "text": cell["text"],
                    "type": cell["type"],
                    "style": cell["style"],
                    "formula": cell["formula"],
                })
        payload["targetCells"] = nonempty
        payload["targetValidations"] = [
            item for item in sheet["validations"]
            if item["sqref"] and (f"{target['row']}" in item["sqref"].split() or ":" in item["sqref"])
        ]
    if "资源状态明细" in parsed and target:
        detail_rows = []
        for row in parsed["资源状态明细"]["rows"]:
            code = (row["cells"].get("B") or {}).get("text")
            if code and str(code).strip() == target["id"]:
                cells = []
                for column, cell in row["cells"].items():
                    if cell["text"] not in (None, ""):
                        header = detail_headers.get(column, {})
                        cells.append({
                            "column": column,
                            "row1": header.get("row1"),
                            "row2": header.get("row2"),
                            "text": cell["text"],
                            "type": cell["type"],
                            "style": cell["style"],
                            "formula": cell["formula"],
                        })
                detail_rows.append({"row": row["row"], "cells": cells})
        payload["detailRows"] = detail_rows
    if "待制作原始数据" in parsed:
        payload["archiveRowNumbers"] = parsed["待制作原始数据"]["rowNumbers"]
        payload["archiveDimension"] = parsed["待制作原始数据"]["dimension"]
        payload["archiveValidations"] = parsed["待制作原始数据"]["validations"]
    making = parsed.get("制作中")
    if making:
        payload["makingRowNumbers"] = making["rowNumbers"]
        payload["makingDimension"] = making["dimension"]
        payload["makingValidations"] = making["validations"]
    pending = parsed.get("待制作")
    if pending:
        payload["pendingRowNumbers"] = pending["rowNumbers"]
        payload["pendingDimension"] = pending["dimension"]
        payload["pendingValidationCount"] = len(pending["validations"])
    # comment anchors mentioning the target row or id
    anchors = []
    if target:
        for name, text in comments.items():
            if target["id"] in text or f'r="{target["row"]}"' in text or "灼地" in text:
                anchors.append({"file": name, "hit": True, "length": len(text)})
    payload["commentHits"] = anchors
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({
        "sheets": payload["sheets"],
        "counts": payload["counts"],
        "target": payload["target"],
        "detailRowCount": len(payload.get("detailRows") or []),
        "archiveIds": [item["id"] for item in (ids.get("待制作原始数据") or [])],
        "makingIds": [item["id"] for item in (ids.get("制作中") or [])],
        "pendingIds": [item["id"] for item in (ids.get("待制作") or [])],
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
