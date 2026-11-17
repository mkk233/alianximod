# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
intake = json.loads(Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029\liaoyuan-visual-intake.json").read_text(encoding="utf-8"))
data = live.read_bytes()
stat = live.stat()

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

print("HASH", hashlib.sha256(data).hexdigest().upper())
print("MTIME", datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"))
print("BYTES", len(data))
with zipfile.ZipFile(live) as archive:
    for title, part in (
        ("已完成", "xl/worksheets/sheet1.xml"),
        ("待制作", "xl/worksheets/sheet2.xml"),
        ("制作中", "xl/worksheets/sheet3.xml"),
        ("待验收", "xl/worksheets/sheet4.xml"),
    ):
        xml = archive.read(part).decode("utf-8")
        print("PAGE", title, re.search(r'ref="([^"]+)"', xml).group(1))
        for row in rows_of(xml):
            number = int(re.search(r'\br="(\d+)"', row).group(1))
            if number >= 3:
                print(title, number, text_of(row, "A", number), text_of(row, "B", number))
    sheet3 = archive.read("xl/worksheets/sheet3.xml").decode("utf-8")
    for row in rows_of(sheet3):
        number = int(re.search(r'\br="(\d+)"', row).group(1))
        if text_of(row, "B", number) == "fire_liaoyuan":
            current_n = text_of(row, "N", number)
            current_o = text_of(row, "O", number)
            current_dv = text_of(row, "DV", number)
            print("N_MATCH", current_n == intake["approvedDetail"]["n"])
            print("O_MATCH", current_o == intake["approvedDetail"]["o"])
            print("N_LEN", len(current_n or ""), "O_LEN", len(current_o or ""), "DV_LEN", len(current_dv or ""))
            print("HAS_PREPAY", "一次消耗全部" in (current_n or ""))
            print("HAS_SHARED", "不预扣全部余额" in (current_n or ""))
            print("DV_HAS_RETURN", "退回制作中" in (current_dv or ""))
            print("CELL_N", re.search(r'<c r="N%d"[^>]*>[\s\S]{0,80}' % number, row).group(0))
    sheet4 = archive.read("xl/worksheets/sheet4.xml").decode("utf-8")
    uncovered = []
    for validation in re.findall(r'<dataValidation\b[^>]*sqref="([^"]+)"', sheet4):
        for ref in validation.split():
            bounds = re.match(r"([A-Z]+)(\d+):([A-Z]+)(\d+)$", ref)
            if bounds and int(bounds.group(2)) <= 5 and int(bounds.group(4)) < 5:
                uncovered.append(ref)
    print("SHEET4_UNCOVERED", uncovered)
    print("DETAIL", "fire_liaoyuan" in archive.read("xl/worksheets/sheet6.xml").decode("utf-8"))
