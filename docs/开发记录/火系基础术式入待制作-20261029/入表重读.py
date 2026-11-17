# -*- coding: utf-8 -*-
import hashlib
import html
import re
import zipfile
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
old = Path(r"D:\alianximod\docs\开发记录\火种共享燃料改造-20261029\final-writeback-candidate-preserved.xlsx")
data = live.read_bytes()
stat = live.stat()
print("LIVE", hashlib.sha256(data).hexdigest().upper(), datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"), len(data))
print("OLD_EXISTS", old.exists(), hashlib.sha256(old.read_bytes()).hexdigest().upper() if old.exists() else "")

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
    value = re.search(r"<v>([\s\S]*)</v>", inner)
    return html.unescape(value.group(1)) if value else None

with zipfile.ZipFile(live) as new, zipfile.ZipFile(old) as prev:
    print("PARTS", new.namelist() == prev.namelist())
    different = [name for name in prev.namelist() if prev.read(name) != new.read(name)]
    print("DIFF", different)
    for part, title in (
        ("xl/worksheets/sheet1.xml", "已完成"),
        ("xl/worksheets/sheet2.xml", "待制作"),
        ("xl/worksheets/sheet3.xml", "制作中"),
        ("xl/worksheets/sheet4.xml", "待验收"),
    ):
        xml = new.read(part).decode("utf-8")
        print("PAGE", title, "rows", re.findall(r'<row r="(\d+)"', xml))
        for row in rows_of(xml):
            number = int(re.search(r'\br="(\d+)"', row).group(1))
            if number < 3:
                continue
            print(title, number, text_of(row, "A", number), text_of(row, "B", number))
