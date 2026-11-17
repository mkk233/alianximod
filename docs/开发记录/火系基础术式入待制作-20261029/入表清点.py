# -*- coding: utf-8 -*-
"""Read-only inventory of names, headers and dropdowns."""
import hashlib
import html
import re
import zipfile
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
data = live.read_bytes()
stat = live.stat()
print("SHA", hashlib.sha256(data).hexdigest().upper())
print("MTIME", datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"), "BYTES", len(data))

def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)

def cells_of(row_xml):
    found = {}
    for match in re.finditer(r"<(?:[\w]+:)?c\b([^>]*?)(/?)>", row_xml):
        attrs = match.group(1)
        ref = re.search(r'\br="([A-Z]+)(\d+)"', attrs)
        if not ref:
            continue
        if match.group(2) == "/" or attrs.rstrip().endswith("/"):
            continue
        close = re.search(r"</(?:[\w]+:)?c>", row_xml[match.end():])
        inner = row_xml[match.end():match.end() + close.start()]
        inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", inner)
        value = re.search(r"<(?:[\w]+:)?v>([\s\S]*)</(?:[\w]+:)?v>", inner)
        text = html.unescape(inline.group(1) if inline else value.group(1) if value else "")
        if text != "":
            found[ref.group(1)] = text
    return found

with zipfile.ZipFile(live) as archive:
    sheet1 = archive.read("xl/worksheets/sheet1.xml").decode("utf-8")
    sheet2 = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
    sheet3 = archive.read("xl/worksheets/sheet3.xml").decode("utf-8")
    sheet4 = archive.read("xl/worksheets/sheet4.xml").decode("utf-8")
    sheet5 = archive.read("xl/worksheets/sheet5.xml").decode("utf-8")
    header = cells_of(rows_of(sheet1)[1])
    print("HEADERS")
    for col in sorted(header, key=lambda c: (len(c), c)):
        print(f"{col}\t{header[col]}")
    print("NAMES")
    for title, xml in (("已完成", sheet1), ("待制作", sheet2), ("制作中", sheet3), ("待验收", sheet4)):
        for row in rows_of(xml):
            number = int(re.search(r'\br="(\d+)"', row).group(1))
            if number < 3:
                continue
            cells = cells_of(row)
            if cells.get("B"):
                print(title, number, cells.get("A"), cells.get("B"), cells.get("C"), cells.get("P"), cells.get("Q"), cells.get("R"), cells.get("S"))
    print("LISTS")
    # print columns that are list names from row 2 of sheet5 around known groups
    for row in rows_of(sheet5):
        number = int(re.search(r'\br="(\d+)"', row).group(1))
        cells = cells_of(row)
        interesting = {k: v for k, v in cells.items() if k in ("A", "B", "E", "I", "M", "Q", "U", "Y", "AC", "AG", "AK", "AO", "AS", "BJ", "BL", "BN", "BP", "BR", "BT")}
        if interesting and number <= 20:
            print(number, interesting)
    print("SHEET2_ROWS", re.findall(r'<row r="(\d+)"', sheet2)[:8])
    print("SHEET2_DIM", re.search(r"<dimension[^/]*/>", sheet2).group(0))
    print("VAL_COUNT", sheet2.count("dataValidation"))
