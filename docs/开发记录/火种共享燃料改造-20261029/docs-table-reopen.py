# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\火种共享燃料改造-20261029")
summary = json.loads((batch / "docs-table-preserve-summary.json").read_text(encoding="utf-8"))
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
backup = Path(summary["backup"])
live_bytes = live.read_bytes()
live_sha = hashlib.sha256(live_bytes).hexdigest().upper()
if live_sha != summary["candidateSha256"]:
    raise SystemExit(f"Live hash {live_sha} is not the candidate")


def cell_xml(row_xml, column, row_number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{row_number}"', row_xml))
    if len(matches) != 1:
        raise SystemExit(f"Cell {column}{row_number} count {len(matches)}")
    start = matches[0].start()
    opener_end = row_xml.find(">", start)
    if row_xml[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</(?:[\w]+:)?c>", row_xml[opener_end:])
        end = opener_end + close.end()
    return row_xml[start:end]


def cell_text(cell):
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<(?:[\w]+:)?v>([\s\S]*)</(?:[\w]+:)?v>", cell)
    return html.unescape(value.group(1)) if value else None


def rows_of(sheet_xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet_xml)


def ids_of(sheet_xml):
    found = []
    for row_xml in rows_of(sheet_xml):
        row_number = int(re.search(r'\br="(\d+)"', row_xml).group(1))
        if row_number < 3 or f'r="B{row_number}"' not in row_xml:
            continue
        code = cell_text(cell_xml(row_xml, "B", row_number))
        if code and code.strip():
            found.append(code.strip())
    return found


with zipfile.ZipFile(backup) as old, zipfile.ZipFile(live) as new:
    if old.namelist() != new.namelist():
        raise SystemExit("Part list changed")
    different = [name for name in old.namelist() if old.read(name) != new.read(name)]
    if different != ["xl/worksheets/sheet3.xml"]:
        raise SystemExit(f"Unexpected parts {different}")
    sheet = new.read("xl/worksheets/sheet3.xml").decode("utf-8")
    old_sheet = old.read("xl/worksheets/sheet3.xml").decode("utf-8")
    counts = {}
    all_ids = []
    for title, name in {
        "已完成": "xl/worksheets/sheet1.xml",
        "待制作": "xl/worksheets/sheet2.xml",
        "制作中": "xl/worksheets/sheet3.xml",
        "待验收": "xl/worksheets/sheet4.xml",
    }.items():
        codes = ids_of(new.read(name).decode("utf-8"))
        counts[title] = len(codes)
        all_ids.extend(codes)
    if counts != {"已完成": 40, "待制作": 0, "制作中": 6, "待验收": 0} or len(set(all_ids)) != 46:
        raise SystemExit(f"Counts {counts} unique {len(set(all_ids))}")
    row_map = {int(re.search(r'\br="(\d+)"', row).group(1)): row for row in rows_of(sheet)}
    old_map = {int(re.search(r'\br="(\d+)"', row).group(1)): row for row in rows_of(old_sheet)}
    for row_number in (6, 7, 8):
        if row_map[row_number] != old_map[row_number]:
            raise SystemExit(f"Furnace row {row_number} changed")
    checks = {}
    for code, row_number in summary["rows"].items():
        checks[code] = {
            "id": cell_text(cell_xml(row_map[row_number], "B", row_number)),
            "ec": cell_text(cell_xml(row_map[row_number], "EC", row_number)),
            "hasMarker": "共享燃料实施中" in (cell_text(cell_xml(row_map[row_number], "DV", row_number)) or ""),
            "keepsFormula": "0.36×S" in (cell_text(cell_xml(row_map[row_number], "N", row_number)) or ""),
        }
print(json.dumps({"sha256": live_sha, "counts": counts, "different": different, "checks": checks}, ensure_ascii=False))
