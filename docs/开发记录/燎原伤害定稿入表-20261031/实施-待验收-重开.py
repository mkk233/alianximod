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
result = {"sha256": hashlib.sha256(payload).hexdigest().upper(), "bytes": live.stat().st_size}
with zipfile.ZipFile(live) as archive:
    counts = {}
    ids = []
    located = []
    pages = {}
    for title, name in parts:
        xml = archive.read(name).decode("utf-8")
        page = []
        for row in rows_of(xml):
            number = row_number(row)
            if number < 3:
                continue
            code = cell_text(row, "B", number)
            if not code:
                continue
            page.append({"row": number, "id": code, "name": cell_text(row, "A", number)})
            ids.append(code)
            if code == "fire_liaoyuan":
                note = cell_text(row, "DV", number) or ""
                located.append({
                    "sheet": title,
                    "row": number,
                    "E": cell_text(row, "E", number),
                    "AS": cell_text(row, "AS", number),
                    "BV": cell_text(row, "BV", number),
                    "BW": cell_text(row, "BW", number),
                    "EC": cell_text(row, "EC", number),
                    "EG": cell_text(row, "EG", number),
                    "EH": cell_text(row, "EH", number),
                    "n": cell_text(row, "N", number),
                    "o": cell_text(row, "O", number),
                    "compiled": "BUILD SUCCESSFUL in 25s" in note,
                    "quota": "本阶段额度已用完，返修结果未追加复审" in note,
                    "awaitingPlaytest": "用户游戏实测仍待做" in note,
                })
        counts[title] = len(page)
        pages[title] = page
    result.update({
        "counts": counts,
        "unique": len(set(ids)),
        "total": len(ids),
        "pages": pages,
        "located": located,
        "makingHas": "fire_liaoyuan" in archive.read("xl/worksheets/sheet3.xml").decode("utf-8"),
        "detailHas": "fire_liaoyuan" in archive.read("xl/worksheets/sheet6.xml").decode("utf-8"),
    })
out = Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031\实施-待验收-reopen.json")
out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
print(result["sha256"], result["counts"], result["unique"], [(item["sheet"], item["row"]) for item in located], result["makingHas"])
