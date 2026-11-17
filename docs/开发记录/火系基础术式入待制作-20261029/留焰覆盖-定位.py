# -*- coding: utf-8 -*-
"""Locate 焰流 and references. Prints positions only; does not save old text."""
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

def cells_of(row):
    return re.findall(r"<(?:[\w]+:)?c r=\"([A-Z]+)(\d+)\"[^>]*?(?:/>|>([\s\S]*?)</(?:[\w]+:)?c>)", row)

def text_of(inner):
    if not inner:
        return ""
    inline = re.search(r"<t(?: [^>]*)?>([\s\S]*)</t>", inner)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", inner)
    return html.unescape(value.group(1)) if value else ""

pages = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
    "资源明细": "xl/worksheets/sheet6.xml",
}
with zipfile.ZipFile(live) as archive:
    for title, part in pages.items():
        xml = archive.read(part).decode("utf-8")
        print("PAGE", title, re.search(r'ref="([^"]+)"', xml).group(1))
        for row in rows_of(xml):
            number = int(re.search(r'\br="(\d+)"', row).group(1))
            if number < 3:
                continue
            pairs = cells_of(row)
            values = {col: text_of(inner) for col, _num, inner in pairs}
            name = values.get("A", "")
            code = values.get("B", "")
            if code in ("fire_flame_flow", "fire_flame_trail") or name in ("焰流", "留焰"):
                print("HIT", title, number, name, code)
                print("COLS", ",".join(f"{col}={values[col][:40].replace(chr(10), ' ')}" for col in values if col not in ("N", "O", "DV")))
            blob = "\n".join(values.get(col, "") for col in ("N", "O", "DV"))
            if "焰流" in blob or "fire_flame_flow" in blob or "留焰" in blob or "fire_flame_trail" in blob:
                print("REF", title, number, name, code)
    detail = archive.read("xl/worksheets/sheet6.xml").decode("utf-8")
    print("DETAIL_OLD", "fire_flame_flow" in detail)
    print("DETAIL_NEW", "fire_flame_trail" in detail)
    archive_xml = archive.read("xl/worksheets/sheet7.xml").decode("utf-8")
    print("ARCHIVE_OLD", "fire_flame_flow" in archive_xml, "ARCHIVE_NAME", "焰流" in archive_xml)
