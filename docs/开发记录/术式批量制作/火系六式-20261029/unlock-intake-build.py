# -*- coding: utf-8 -*-
"""Read the live workbook for unlock intake. Does not write the workbook."""
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
prior_path = batch / "test-stage-input.json"
process_path = root / "docs" / "统一开发与术式批量制作流程.md"
output_path = batch / "unlock-intake.json"
wanted = [
    "fire_liaoyuan",
    "fire_liaoli",
    "fire_fentian",
    "fire_heat_up",
    "fire_burn",
    "fire_cool",
]
focus = ["fire_liaoyuan", "fire_liaoli", "fire_fentian"]
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
keywords = ("等级", "学习", "解锁", "刻印", "熟练", "学会", "可印")


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
        if attrs.endswith("/"):
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


def row_cells(sheet_xml, number):
    for row_xml in rows_of(sheet_xml):
        if row_number_of(row_xml) == number:
            return cells_of(row_xml)
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


def column_index(column):
    value = 0
    for char in column:
        value = value * 26 + ord(char) - 64
    return value


def column_name(index):
    letters = []
    while index:
        index, remainder = divmod(index - 1, 26)
        letters.append(chr(65 + remainder))
    return "".join(reversed(letters))


def merges_of(sheet_xml):
    found = []
    for match in re.finditer(r'<(?:[\w]+:)?mergeCell\b[^>]*\bref="([A-Z]+)(\d+):([A-Z]+)(\d+)"', sheet_xml):
        found.append({
            "startColumn": match.group(1),
            "startRow": int(match.group(2)),
            "endColumn": match.group(3),
            "endRow": int(match.group(4)),
        })
    return found


def group_for(column, row1, merges):
    index = column_index(column)
    for merge in merges:
        if merge["startRow"] != 1 or merge["endRow"] != 1:
            continue
        if column_index(merge["startColumn"]) <= index <= column_index(merge["endColumn"]):
            return row1.get(merge["startColumn"], {}).get("text")
    return row1.get(column, {}).get("text")


def comment_map(archive):
    mapped = {}
    for name in archive.namelist():
        if not name.startswith("xl/comments") or not name.endswith(".xml"):
            continue
        xml = archive.read(name).decode("utf-8")
        for match in re.finditer(r'<(?:[\w]+:)?comment\b[^>]*\bref="([A-Z]+\d+)"[^>]*>([\s\S]*?)</(?:[\w]+:)?comment>', xml):
            texts = re.findall(r"<(?:[\w]+:)?t\b[^>]*>([\s\S]*?)</(?:[\w]+:)?t>", match.group(2))
            mapped[match.group(1)] = unescape("".join(texts))
    return mapped


source_bytes = source.read_bytes()
source_stat = source.stat()
prior = json.loads(prior_path.read_text(encoding="utf-8"))
with zipfile.ZipFile(source) as archive:
    sheets = {name: archive.read(part).decode("utf-8") for name, part in status_parts.items()}
    detail_xml = archive.read("xl/worksheets/sheet6.xml").decode("utf-8")
    comments = comment_map(archive)

making = sheets["制作中"]
row1 = row_cells(making, 1)
row2 = row_cells(making, 2)
merges = merges_of(making)
headers = {column: payload["text"] for column, payload in row2.items()}
header_fields = []
for column, header in sorted(headers.items(), key=lambda item: column_index(item[0])):
    group = group_for(column, row1, merges)
    blob = f"{group or ''} {header}"
    if any(word in blob for word in keywords):
        header_fields.append({
            "column": column,
            "group": group,
            "header": header,
            "comment": comments.get(f"{column}2") or comments.get(f"{column}1"),
        })

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

detail_headers = {column: payload["text"] for column, payload in row_cells(detail_xml, 2).items()}
detail_row1 = row_cells(detail_xml, 1)
detail_merges = merges_of(detail_xml)
detail_fields = []
for column, header in sorted(detail_headers.items(), key=lambda item: column_index(item[0])):
    group = group_for(column, detail_row1, detail_merges)
    blob = f"{group or ''} {header}"
    if any(word in blob for word in keywords):
        detail_fields.append({"column": column, "group": group, "header": header})
detail_inventory = []
detail_matches = []
for row_xml in rows_of(detail_xml):
    number = row_number_of(row_xml)
    if number < 3:
        continue
    cells = cells_of(row_xml)
    technique = cells.get("A", {}).get("text", "").strip()
    if not technique:
        continue
    entry = {
        "row": number,
        "id": technique,
        "sequence": cells.get("B", {}).get("text"),
    }
    detail_inventory.append(entry)
    if technique in wanted:
        detail_matches.append(entry)

prior_items = {item["id"]: item for item in prior["items"]}
items = []
requirement_changed = []
for code in wanted:
    hits = [item for item in located if item["id"] == code]
    if len(hits) != 1:
        fail(f"{code} occurrences {hits}")
    hit = hits[0]
    _number, _code, cells = next(record for record in by_sheet[hit["sheet"]] if record[1] == code)
    prior_item = prior_items[code]
    prior_cells = prior_item["populatedCells"]
    current_keys = set(cells)
    prior_keys = set(prior_cells)
    cell_diffs = []
    for column in sorted(current_keys | prior_keys, key=column_index):
        current_text = cells.get(column, {}).get("text")
        prior_text = prior_cells.get(column, {}).get("text")
        if current_text != prior_text:
            cell_diffs.append({
                "column": column,
                "header": headers.get(column) or prior_cells.get(column, {}).get("header"),
                "equal": False,
            })
    n_text = cells.get("N", {}).get("text")
    o_text = cells.get("O", {}).get("text")
    dv_text = cells.get("DV", {}).get("text") or ""
    prior_dv = prior_item["dv"] or ""
    dv_prefix = dv_text.startswith(prior_dv)
    non_dv_diffs = [diff for diff in cell_diffs if diff["column"] != "DV"]
    changed = (n_text != prior_item["n"]) or (o_text != prior_item["o"]) or (not dv_prefix) or bool(non_dv_diffs)
    if changed:
        requirement_changed.append(code)
    unlock_fields = []
    for field in header_fields:
        column = field["column"]
        payload = cells.get(column)
        unlock_fields.append({
            **field,
            "present": payload is not None,
            "kind": None if payload is None else payload["kind"],
            "text": None if payload is None else payload["text"],
        })
    item = {
        "id": code,
        "name": cells.get("A", {}).get("text"),
        "sheet": hit["sheet"],
        "row": hit["row"],
        "occurrences": hits,
        "type": cells.get("C", {}).get("text"),
        "attribute": cells.get("D", {}).get("text"),
        "releaseKind": cells.get("P", {}).get("text"),
        "releaseTag": cells.get("Q", {}).get("text"),
        "trajectory": cells.get("R", {}).get("text"),
        "chainRole": cells.get("S", {}).get("text"),
        "n": n_text,
        "o": o_text,
        "dv": dv_text,
        "unlockFields": unlock_fields,
        "resourceDetails": [row for row in detail_matches if row["id"] == code],
        "comparisonToTestStageInput": {
            "sameSheet": hit["sheet"] == prior_item["sheet"],
            "sameRow": hit["row"] == prior_item["row"],
            "nEqual": n_text == prior_item["n"],
            "oEqual": o_text == prior_item["o"],
            "dvPrefixEqual": dv_prefix,
            "dvSuffix": dv_text[len(prior_dv):] if dv_prefix else None,
            "nonDvCellDiffs": non_dv_diffs,
            "userRequirementChanged": changed,
        },
    }
    if code in focus:
        item["populatedCells"] = {
            column: {"header": headers.get(column), **payload}
            for column, payload in cells.items()
        }
    items.append(item)

document = {
    "purpose": "刻印台缺失反馈的现行表输入。只读对账和用户反馈采集，不是正确性或验收复审，不迁移。",
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
    "comparedWith": {
        "path": "docs/开发记录/术式批量制作/火系六式-20261029/test-stage-input.json",
        "sha256": file_sha(prior_path),
        "workbookSha256": prior["workbook"]["sha256"],
    },
    "userFeedback": {
        "at": "2026-10-29",
        "text": "我启动游戏看了一下，发现用命令修改熟练度等级到20魔法和剑等级都升了但是没正确解锁术式，在刻印台中没有焚天燎原燎里。",
        "acceptancePassed": False,
        "sheetEditedByThisFeedback": False,
    },
    "counts": counts,
    "total": len(codes),
    "unique": len(set(codes)),
    "duplicateIds": duplicates,
    "allIds": located,
    "unlockHeaderFields": header_fields,
    "resourceDetailUnlockFields": detail_fields,
    "resourceDetailIdColumn": "A",
    "resourceDetailInventory": detail_inventory,
    "resourceDetailMatches": detail_matches,
    "userRequirementChangedIds": requirement_changed,
    "items": items,
}
output_path.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "sha256": document["workbook"]["sha256"],
    "mtime": document["workbook"]["lastWriteTime"],
    "bytes": document["workbook"]["bytes"],
    "processSha256": document["process"]["sha256"],
    "counts": counts,
    "unique": document["unique"],
    "duplicates": duplicates,
    "unlockHeaders": header_fields,
    "detailUnlockHeaders": detail_fields,
    "detailInventory": detail_inventory,
    "requirementChanged": requirement_changed,
    "items": [
        {
            "id": item["id"],
            "name": item["name"],
            "sheet": item["sheet"],
            "row": item["row"],
            "nEqual": item["comparisonToTestStageInput"]["nEqual"],
            "oEqual": item["comparisonToTestStageInput"]["oEqual"],
            "dvPrefixEqual": item["comparisonToTestStageInput"]["dvPrefixEqual"],
            "dvSuffixLength": None if item["comparisonToTestStageInput"]["dvSuffix"] is None else len(item["comparisonToTestStageInput"]["dvSuffix"]),
            "nonDvDiffs": item["comparisonToTestStageInput"]["nonDvCellDiffs"],
            "unlock": [
                {"column": field["column"], "header": field["header"], "text": field["text"]}
                for field in item["unlockFields"]
            ],
        }
        for item in items
    ],
}, ensure_ascii=False, indent=2))
