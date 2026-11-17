# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
payload = live.read_bytes()


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_text(row, column, number):
    match = re.search(rf'<(?:[\w]+:)?c r="{column}{number}"[\s\S]*?</(?:[\w]+:)?c>', row)
    if not match:
        return None
    inner = match.group(0)
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", inner)
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", inner)
    raw = inline.group(1) if inline else value.group(1) if value else None
    return html.unescape(raw) if raw else None


parts = (
    ("已完成", "xl/worksheets/sheet1.xml"),
    ("待制作", "xl/worksheets/sheet2.xml"),
    ("制作中", "xl/worksheets/sheet3.xml"),
    ("待验收", "xl/worksheets/sheet4.xml"),
)
stat = live.stat()
result = {
    "sha256": hashlib.sha256(payload).hexdigest().upper(),
    "bytes": stat.st_size,
    "mtime": stat.st_mtime,
}
with zipfile.ZipFile(live) as archive:
    counts = {}
    located = []
    ids = []
    for title, name in parts:
        xml = archive.read(name).decode("utf-8")
        page = []
        for row in rows_of(xml):
            number = row_number(row)
            if number < 3:
                continue
            code = cell_text(row, "B", number)
            if code:
                page.append(code)
                if code == "fire_liaoyuan":
                    located.append({
                        "sheet": title,
                        "row": number,
                        "name": cell_text(row, "A", number),
                        "E": cell_text(row, "E", number),
                        "AS": cell_text(row, "AS", number),
                        "BV": cell_text(row, "BV", number),
                        "EH": cell_text(row, "EH", number),
                        "EG": cell_text(row, "EG", number),
                        "EC": cell_text(row, "EC", number),
                        "nHasRadius": "半径10" in (cell_text(row, "N", number) or ""),
                        "nHasEight": "基础火伤8点" in (cell_text(row, "N", number) or ""),
                        "oHasAsh": "灰烬" in (cell_text(row, "O", number) or ""),
                        "oIsPath": (cell_text(row, "O", number) or "").startswith("assets/"),
                        "authorized": "按这个版本开始修" in (cell_text(row, "DV", number) or ""),
                        "notUndecided": "不再是待决定项" in (cell_text(row, "DV", number) or ""),
                    })
        counts[title] = len(page)
        ids.extend(page)
    result["counts"] = counts
    result["unique"] = len(set(ids))
    result["total"] = len(ids)
    result["located"] = located
    result["pendingHasId"] = "fire_liaoyuan" in archive.read("xl/worksheets/sheet4.xml").decode("utf-8")
    result["detailHasId"] = "fire_liaoyuan" in archive.read("xl/worksheets/sheet6.xml").decode("utf-8")
out = Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031\实施-退回-reopen.json")
out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
print(out.read_text(encoding="utf-8"))
