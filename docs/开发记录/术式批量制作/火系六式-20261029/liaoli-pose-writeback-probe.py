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
print("HASH", hashlib.sha256(data).hexdigest().upper())
print("MTIME", datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"))
print("BYTES", len(data))

def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)

def text_of(row, column, number):
    match = re.search(rf'<c r="{column}{number}"[^>]*>([\s\S]*?)</c>', row)
    if not match:
        return None
    inner = match.group(1)
    inline = re.search(r"<t(?: [^>]*)?>([\s\S]*)</t>", inner)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", inner)
    return html.unescape(value.group(1)) if value else None

pages = [
    ("已完成", "xl/worksheets/sheet1.xml"),
    ("待制作", "xl/worksheets/sheet2.xml"),
    ("制作中", "xl/worksheets/sheet3.xml"),
    ("待验收", "xl/worksheets/sheet4.xml"),
]
with zipfile.ZipFile(live) as archive:
    for title, part in pages:
        xml = archive.read(part).decode("utf-8")
        print("DIM", title, re.search(r'ref="([^"]+)"', xml).group(1))
        for row in rows_of(xml):
            number = int(re.search(r'\br="(\d+)"', row).group(1))
            if number < 3:
                continue
            code = text_of(row, "B", number)
            if code:
                print(title, number, text_of(row, "A", number), code)
            if code == "fire_liaoli":
                print("NLEN", len(text_of(row, "N", number) or ""))
                print("OLEN", len(text_of(row, "O", number) or ""))
                print("DVLEN", len(text_of(row, "DV", number) or ""))
                print("NHEAD", (text_of(row, "N", number) or "")[:40])
                print("POSE", "2026-10-30只返修背后八剑" in (text_of(row, "N", number) or ""))
                print("DVPOSE", "背后八剑姿态" in (text_of(row, "DV", number) or ""))
    print("DETAIL", "fire_liaoli" in archive.read("xl/worksheets/sheet6.xml").decode("utf-8"))
