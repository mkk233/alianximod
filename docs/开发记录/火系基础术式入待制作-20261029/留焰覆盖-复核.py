# -*- coding: utf-8 -*-
import hashlib
import html
import re
import zipfile
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
data = live.read_bytes()
stat = live.stat()

def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)

def text_of(row, column, number):
    match = re.search(rf'<c r="{column}{number}"[^>]*>([\s\S]*?)</c>', row)
    if not match:
        return None
    inner = match.group(1)
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", inner)
    if value:
        return html.unescape(value.group(1))
    inline = re.search(r"<t(?: [^>]*)?>([\s\S]*)</t>", inner)
    return html.unescape(inline.group(1)) if inline else None

ids = []
located = []
with zipfile.ZipFile(live) as archive:
    pages = {
        "已完成": "xl/worksheets/sheet1.xml",
        "待制作": "xl/worksheets/sheet2.xml",
        "制作中": "xl/worksheets/sheet3.xml",
        "待验收": "xl/worksheets/sheet4.xml",
    }
    counts = {}
    pending = archive.read(pages["待制作"]).decode("utf-8")
    for title, part in pages.items():
        xml = archive.read(part).decode("utf-8")
        page = 0
        for row in rows_of(xml):
            number = int(re.search(r'\br="(\d+)"', row).group(1))
            if number < 3:
                continue
            code = text_of(row, "B", number)
            if not code:
                continue
            page += 1
            ids.append(code)
            if code in ("fire_flame_flow", "fire_flame_trail") or text_of(row, "A", number) in ("焰流", "留焰"):
                located.append((title, number, text_of(row, "A", number), code))
        counts[title] = page
    row = next(item for item in rows_of(pending) if text_of(item, "B", int(re.search(r'\br="(\d+)"', item).group(1))) == "fire_flame_trail")
    number = int(re.search(r'\br="(\d+)"', row).group(1))
    cleared = {column: text_of(row, column, number) for column in ("E", "R", "S", "T", "U", "AH", "AK")}
    detail = archive.read("xl/worksheets/sheet6.xml").decode("utf-8")
    print("HASH", hashlib.sha256(data).hexdigest().upper())
    print("MTIME", datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"))
    print("BYTES", len(data))
    print("COUNTS", counts, "UNIQUE", len(set(ids)), "TOTAL", len(ids))
    print("LOCATED", located)
    print("CLEARED", cleared)
    print("CLASS", text_of(row, "C", number), text_of(row, "J", number), text_of(row, "P", number), text_of(row, "Q", number), text_of(row, "DW", number))
    print("OLD_ID_PENDING", "fire_flame_flow" in pending)
    print("OLD_ID_DETAIL", "fire_flame_flow" in detail)
    print("UPWELL", "原先用独立焰流充当中央火柱的引用已失效" in pending)
    print("CONTINUE", "旧焰流作为独立载体的引用已失效" in pending)
    print("ROWS", re.findall(r'<row r="(\d+)"', pending))
