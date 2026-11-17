# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
before = json.loads(Path(r"D:\alianximod\docs\开发记录\火种共享燃料改造-20261029\final-writeback-before.json").read_text(encoding="utf-8"))
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
        count = 0
        for row in rows_of(xml):
            number = int(re.search(r'\br="(\d+)"', row).group(1))
            if number < 3:
                continue
            code = text_of(row, "B", number)
            if code:
                count += 1
            if code == "fire_liaoli":
                print("HIT", title, number, text_of(row, "A", number))
                print("NLEN", len(text_of(row, "N", number) or ""))
                print("OLEN", len(text_of(row, "O", number) or ""))
                print("DVLEN", len(text_of(row, "DV", number) or ""))
                print("NHEAD", (text_of(row, "N", number) or "")[:80])
                print("OHEAD", (text_of(row, "O", number) or "")[:180])
                deps = []
                for column in ("C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "P", "Q", "R", "S", "U", "AP", "EB", "EC", "EG"):
                    value = text_of(row, column, number)
                    if value:
                        deps.append(f"{column}={value}")
                print("DEPS", ",".join(deps))
        print("COUNT", title, count)
    detail = archive.read("xl/worksheets/sheet6.xml").decode("utf-8")
    print("DETAIL", "fire_liaoli" in detail)

items = before.get("rows") or before.get("items") or before
if isinstance(before, dict):
    print("BEFORE_KEYS", list(before.keys())[:12])
