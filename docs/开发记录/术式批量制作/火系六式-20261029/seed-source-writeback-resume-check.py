# -*- coding: utf-8 -*-
"""Confirm the retained candidate still matches the live baseline. Does not write the workbook."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "术式批量制作" / "火系六式-20261029"
live = root / "docs" / "术式制作表.xlsx"
candidate = batch / "seed-source-writeback-candidate-preserved.xlsx"
notes = json.loads((batch / "seed-source-writeback-notes.json").read_text(encoding="utf-8"))
baseline = "5F8B37C4A45673CF320F4885EB5BDBF1961960013151EE082EF2C6DFF7FADA28"
candidate_expected = "8F9ED61F2EBBB7C1EEE389412249793825FCE1B76EB96D30737D18FB125FF0D4"
status = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}


def fail(message):
    raise SystemExit(message)


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def cell_text(row_xml, column, row_number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{row_number}"', row_xml))
    if len(matches) != 1:
        fail(f"Cell {column}{row_number} count is {len(matches)}")
    start = matches[0].start()
    opener_end = row_xml.find(">", start)
    if row_xml[opener_end - 1] == "/":
        return None
    close = re.search(r"</(?:[\w]+:)?c>", row_xml[opener_end:])
    cell = row_xml[start:opener_end + close.end()]
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
        if row_number < 3:
            continue
        code = cell_text(row_xml, "B", row_number)
        if code:
            found.append(code.strip())
    return found


live_hash = sha256(live)
candidate_hash = sha256(candidate)
if live_hash != baseline:
    fail(f"LIVE_CHANGED {live_hash}")
if candidate_hash != candidate_expected:
    fail(f"CANDIDATE_CHANGED {candidate_hash}")
stat = live.stat()
with zipfile.ZipFile(live) as src, zipfile.ZipFile(candidate) as dst:
    if src.namelist() != dst.namelist():
        fail("Zip part list differs")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet1.xml"]:
        fail(f"Unexpected differences {different}")
    counts = {}
    all_ids = []
    for title, part in status.items():
        ids = ids_of(dst.read(part).decode("utf-8"))
        counts[title] = len(ids)
        all_ids.extend(ids)
    if counts != {"已完成": 40, "待制作": 0, "制作中": 6, "待验收": 0}:
        fail(f"Counts {counts}")
    if len(all_ids) != 46 or len(set(all_ids)) != 46:
        fail("Status ids are not unique")
    live_sheet = src.read("xl/worksheets/sheet1.xml").decode("utf-8")
    cand_sheet = dst.read("xl/worksheets/sheet1.xml").decode("utf-8")
    live_rows = {int(re.search(r'\br="(\d+)"', row).group(1)): row for row in rows_of(live_sheet)}
    cand_rows = {int(re.search(r'\br="(\d+)"', row).group(1)): row for row in rows_of(cand_sheet)}
    checked = {}
    for code, addition in notes["additions"].items():
        row_number = notes["rows"][code]
        live_row = live_rows[row_number]
        cand_row = cand_rows[row_number]
        if cell_text(live_row, "B", row_number) != code or cell_text(cand_row, "B", row_number) != code:
            fail(f"Id moved for {code}")
        if cell_text(live_row, "N", row_number) != cell_text(cand_row, "N", row_number):
            fail(f"N differs for {code}")
        if cell_text(live_row, "O", row_number) != cell_text(cand_row, "O", row_number):
            fail(f"O differs for {code}")
        if cell_text(live_row, "DV", row_number) is not None:
            fail(f"Live DV is no longer empty for {code}")
        if cell_text(cand_row, "DV", row_number) != addition:
            fail(f"Candidate DV mismatch for {code}")
        checked[code] = row_number
print(json.dumps({
    "live": live_hash,
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": stat.st_size,
    "candidate": candidate_hash,
    "different": different,
    "counts": counts,
    "rows": checked,
}, ensure_ascii=False))
