# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029")
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
backup = batch / "备份" / "seed-source-writeback-resume-术式制作表-20261029-141057-1.xlsx"
notes = json.loads((batch / "seed-source-writeback-notes.json").read_text(encoding="utf-8"))
status = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}


def cell_text(row_xml, column, row_number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{row_number}"', row_xml))
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


stat = live.stat()
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(live) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    counts = {title: len(ids_of(dst.read(part).decode("utf-8"))) for title, part in status.items()}
    ids = []
    for part in status.values():
        ids.extend(ids_of(dst.read(part).decode("utf-8")))
    sheet = dst.read("xl/worksheets/sheet1.xml").decode("utf-8")
    old = src.read("xl/worksheets/sheet1.xml").decode("utf-8")
    rows = {int(re.search(r'\br="(\d+)"', row).group(1)): row for row in rows_of(sheet)}
    old_rows = {int(re.search(r'\br="(\d+)"', row).group(1)): row for row in rows_of(old)}
    kept = {}
    for code, addition in notes["additions"].items():
        row_number = notes["rows"][code]
        kept[code] = {
            "n": cell_text(rows[row_number], "N", row_number) == cell_text(old_rows[row_number], "N", row_number),
            "o": cell_text(rows[row_number], "O", row_number) == cell_text(old_rows[row_number], "O", row_number),
            "note": cell_text(rows[row_number], "DV", row_number) == addition,
        }
print(json.dumps({
    "sha256": hashlib.sha256(live.read_bytes()).hexdigest().upper(),
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": stat.st_size,
    "different": different,
    "counts": counts,
    "unique": len(set(ids)) == 46 and len(ids) == 46,
    "kept": kept,
}, ensure_ascii=False, indent=2))
