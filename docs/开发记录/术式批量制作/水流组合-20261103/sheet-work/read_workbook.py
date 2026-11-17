# Read-only OOXML extract. Does not modify the workbook or any dependency.
import json
import posixpath
import zipfile
import xml.etree.ElementTree as ET
from collections import defaultdict

XLSX = r"D:\alianximod\docs\术式制作表.xlsx"
OUT = r"D:\alianximod\docs\开发记录\术式批量制作\水流组合-20261103\sheet-work\extract.json"
NS = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = {"r": "http://schemas.openxmlformats.org/package/2006/relationships"}
STATUS = {"已完成", "待制作", "制作中", "待验收"}


def col_row(ref):
    col = ""
    row = ""
    for ch in ref:
        if ch.isalpha():
            col += ch
        else:
            row += ch
    return col, int(row)


def col_index(col):
    n = 0
    for ch in col:
        n = n * 26 + (ord(ch) - 64)
    return n


def col_letter(n):
    s = ""
    while n:
        n, rem = divmod(n - 1, 26)
        s = chr(65 + rem) + s
    return s


def text_of(node):
    parts = []
    for t in node.iter("{http://schemas.openxmlformats.org/spreadsheetml/2006/main}t"):
        parts.append(t.text or "")
    return "".join(parts)


def load_shared(zf):
    root = ET.fromstring(zf.read("xl/sharedStrings.xml"))
    out = []
    for si in root.findall("m:si", NS):
        out.append(text_of(si))
    return out


def cell_value(cell, shared):
    t = cell.get("t")
    if t == "inlineStr":
        return text_of(cell)
    v = cell.find("m:v", NS)
    if v is None or v.text is None:
        return ""
    if t == "s":
        return shared[int(v.text)]
    if t == "b":
        return "TRUE" if v.text == "1" else "FALSE"
    return v.text


def load_sheet(zf, path, shared):
    root = ET.fromstring(zf.read(path))
    rows = defaultdict(dict)
    for c in root.findall(".//m:c", NS):
        ref = c.get("r")
        if not ref:
            continue
        col, row = col_row(ref)
        rows[row][col] = cell_value(c, shared)
    validations = []
    for dv in root.findall(".//m:dataValidation", NS):
        formula = dv.find("m:formula1", NS)
        sqref = dv.get("sqref")
        validations.append({
            "sqref": sqref,
            "type": dv.get("type"),
            "formula1": None if formula is None else (formula.text or ""),
        })
    return rows, validations


def sheet_map(zf):
    wb = ET.fromstring(zf.read("xl/workbook.xml"))
    rels = ET.fromstring(zf.read("xl/_rels/workbook.xml.rels"))
    rid_to_target = {}
    for rel in rels:
        rid_to_target[rel.get("Id")] = rel.get("Target")
    sheets = []
    for sh in wb.findall("m:sheets/m:sheet", NS):
        rid = sh.get("{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id")
        target = rid_to_target[rid]
        if not target.startswith("xl/"):
            target = posixpath.normpath(posixpath.join("xl", target))
        sheets.append({"name": sh.get("name"), "path": target.replace("\\", "/")})
    names = []
    for dn in wb.findall("m:definedNames/m:definedName", NS):
        names.append({
            "name": dn.get("name"),
            "localSheetId": dn.get("localSheetId"),
            "ref": (dn.text or "").strip(),
        })
    return sheets, names


def row_nonempty(rowmap):
    return any((v or "").strip() for v in rowmap.values())


def main():
    with zipfile.ZipFile(XLSX, "r") as zf:
        shared = load_shared(zf)
        sheets, defined = sheet_map(zf)
        parsed = {}
        for sh in sheets:
            rows, validations = load_sheet(zf, sh["path"], shared)
            parsed[sh["name"]] = {"rows": rows, "validations": validations, "path": sh["path"]}

    inventory = []
    b_locations = defaultdict(list)
    problem_rows = []
    page_counts = {}
    headers = {}
    key_rows = []
    key_cols = ["A", "B", "C", "D", "N", "O", "P", "Q", "R", "S", "DV"]

    for name, data in parsed.items():
        rows = data["rows"]
        if not rows:
            page_counts[name] = {"maxRow": 0, "rowsWithContentFrom3": 0, "bNonEmpty": 0}
            continue
        max_row = max(rows)
        header1 = rows.get(1, {})
        header2 = rows.get(2, {})
        headers[name] = {
            "row1": {k: header1[k] for k in sorted(header1, key=col_index)},
            "row2": {k: header2[k] for k in sorted(header2, key=col_index)},
        }
        content = 0
        bcount = 0
        for r in range(3, max_row + 1):
            rowmap = rows.get(r, {})
            if not row_nonempty(rowmap):
                continue
            content += 1
            b = (rowmap.get("B") or "").strip()
            a = (rowmap.get("A") or "").strip()
            if name in STATUS:
                if b:
                    bcount += 1
                    b_locations[b].append({"sheet": name, "row": r, "name": a})
                else:
                    problem_rows.append({"sheet": name, "row": r, "reason": "B空但同行有内容", "A": a})
                key_rows.append({
                    "sheet": name,
                    "row": r,
                    "fields": {col: rowmap.get(col, "") for col in key_cols},
                })
            elif name == "资源状态明细":
                key_rows.append({
                    "sheet": name,
                    "row": r,
                    "fields": {col: rowmap.get(col, "") for col in sorted(rowmap, key=col_index)},
                })
        if name in STATUS:
            page_counts[name] = {
                "maxRow": max_row,
                "rowsWithContentFrom3": content,
                "bNonEmpty": bcount,
            }
        else:
            page_counts[name] = {
                "maxRow": max_row,
                "rowsWithContentFrom3": content,
            }

    duplicates = []
    for b, locs in sorted(b_locations.items()):
        status_locs = [loc for loc in locs if loc["sheet"] in STATUS]
        if len(status_locs) > 1:
            duplicates.append({"B": b, "locations": status_locs})

    needles = [
        "旋流", "气泡", "深海", "水龙卷", "龙卷", "泼潮", "穿涧", "水幕",
        "tide_sever", "water_yielding_ribbon", "water_deep_pressure",
        "vortex", "bubble", "deep_pressure", "水势", "潮湿",
    ]

    def row_hit(rowmap):
        blob = "\n".join(rowmap.values())
        return [n for n in needles if n in blob]

    hits = []
    for name, data in parsed.items():
        for r, rowmap in sorted(data["rows"].items()):
            if r < 3 and name in STATUS:
                continue
            found = row_hit(rowmap)
            if found:
                hits.append({
                    "sheet": name,
                    "row": r,
                    "needles": found,
                    "A": rowmap.get("A", ""),
                    "B": rowmap.get("B", ""),
                })

    # Full A:EN for rows whose A/B or any cell mentions the three targets.
    target_needles = ["旋流", "气泡柱", "气泡", "深海重压", "water_deep_pressure", "水龙卷"]
    targets = []
    for name, data in parsed.items():
        header2 = data["rows"].get(2, {})
        max_col = 144 if name in STATUS or name == "待制作原始数据" else 0
        if max_col == 0:
            cols_present = set()
            for rowmap in data["rows"].values():
                cols_present.update(rowmap)
            ordered = sorted(cols_present, key=col_index)
        else:
            ordered = [col_letter(i) for i in range(1, max_col + 1)]
        for r, rowmap in sorted(data["rows"].items()):
            if r < 3:
                continue
            blob = "\n".join(rowmap.values())
            found = [n for n in target_needles if n in blob]
            if not found:
                continue
            cells = []
            for col in ordered:
                value = rowmap.get(col, "")
                if name in STATUS or name == "待制作原始数据":
                    cells.append({
                        "col": col,
                        "header1": data["rows"].get(1, {}).get(col, ""),
                        "header2": header2.get(col, ""),
                        "value": value,
                    })
                elif value != "":
                    cells.append({
                        "col": col,
                        "header1": data["rows"].get(1, {}).get(col, ""),
                        "header2": header2.get(col, ""),
                        "value": value,
                    })
            targets.append({
                "sheet": name,
                "row": r,
                "needles": found,
                "cells": cells,
            })

    # Data sheet continuous lists: each column until first blank after a value, plus full nonempty.
    data_sheet = parsed.get("数据表", {}).get("rows", {})
    lists = []
    if data_sheet:
        cols = set()
        for rowmap in data_sheet.values():
            cols.update(rowmap)
        for col in sorted(cols, key=col_index):
            max_r = max(data_sheet)
            values = []
            for r in range(1, max_r + 1):
                values.append({"row": r, "value": data_sheet.get(r, {}).get(col, "")})
            # continuous from first nonempty downward until blank
            start = None
            for item in values:
                if item["value"] != "":
                    start = item["row"]
                    break
            continuous = []
            if start is not None:
                for item in values:
                    if item["row"] < start:
                        continue
                    if item["value"] == "":
                        break
                    continuous.append(item["value"])
            lists.append({
                "col": col,
                "header": data_sheet.get(1, {}).get(col, "") or data_sheet.get(2, {}).get(col, ""),
                "continuousFromFirst": continuous,
                "nonempty": [item for item in values if item["value"] != ""],
            })

    archive_bs = []
    archive = parsed.get("待制作原始数据")
    if archive:
        for r, rowmap in sorted(archive["rows"].items()):
            if r < 3:
                continue
            b = (rowmap.get("B") or "").strip()
            if b:
                archive_bs.append({"row": r, "B": b, "A": rowmap.get("A", "")})

    resource_rows = []
    resource = parsed.get("资源状态明细")
    if resource:
        header2 = resource["rows"].get(2, {})
        header1 = resource["rows"].get(1, {})
        for r, rowmap in sorted(resource["rows"].items()):
            if r < 3 or not row_nonempty(rowmap):
                continue
            resource_rows.append({
                "row": r,
                "cells": [
                    {
                        "col": col,
                        "header1": header1.get(col, ""),
                        "header2": header2.get(col, ""),
                        "value": rowmap.get(col, ""),
                    }
                    for col in sorted(set(list(header2) + list(rowmap)), key=col_index)
                    if rowmap.get(col, "") != "" or col in ("A", "B")
                ],
            })

    status_validations = {
        name: parsed[name]["validations"]
        for name in parsed
        if name in STATUS
    }

    payload = {
        "pageCounts": page_counts,
        "statusBUnique": len(duplicates) == 0,
        "statusDuplicateB": duplicates,
        "problemRows": problem_rows,
        "bInventory": [
            {"B": b, "locations": locs}
            for b, locs in sorted(b_locations.items())
        ],
        "archiveB": archive_bs,
        "keywordHits": hits,
        "keyRows": key_rows,
        "targets": targets,
        "definedNames": defined,
        "dataLists": lists,
        "statusValidations": status_validations,
        "resourceRows": resource_rows,
        "statusHeaders": {name: headers.get(name, {}) for name in STATUS if name in headers},
        "otherHeaders": {name: headers[name] for name in headers if name not in STATUS},
    }
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)
    print("sheets", [s["name"] for s in sheets])
    print("status counts", {k: page_counts.get(k) for k in ["已完成", "待制作", "制作中", "待验收"]})
    print("unique", len(duplicates) == 0, "dups", len(duplicates), "problems", len(problem_rows))
    print("b", len(b_locations), "archive", len(archive_bs), "targets", len(targets), "hits", len(hits))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
