# -*- coding: utf-8 -*-
import html
import json
import re
import zipfile
from pathlib import Path

z = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx")
snap = json.loads(Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031\实施-交付快照.json").read_text(encoding="utf-8"))


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_xml(row, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row.find(">", start)
    if row[opener_end - 1] == "/":
        return row[start:opener_end + 1]
    close = re.search(r"</(?:[\w]+:)?c>", row[opener_end:])
    return row[start:opener_end + close.end()]


def cell_text(cell):
    if not cell:
        return None
    texts = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", cell)
    if texts:
        return html.unescape("".join(texts))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    return html.unescape(value.group(1)) if value else ""


sheet4 = z.read("xl/worksheets/sheet4.xml").decode("utf-8")
sheet3 = z.read("xl/worksheets/sheet3.xml").decode("utf-8")
target = next(row for row in rows_of(sheet4) if row_number(row) == 6)
print("ROW6 OPEN", target[:220])
print("E CELL", cell_xml(target, "E", 6))
print("N OPEN", cell_xml(target, "N", 6)[:180])
print("O OPEN", cell_xml(target, "O", 6)[:180])
dv = cell_xml(target, "DV", 6)
print("DV OPEN", dv[:220])
print("T COUNT N", cell_xml(target, "N", 6).count("<t"))
print("T COUNT DV", dv.count("<t"))
print("CURRENT N", cell_text(cell_xml(target, "N", 6)))
print("N MATCH AFTER", cell_text(cell_xml(target, "N", 6)) == snap["acceptanceConversion"]["afterN"])
print("O MATCH AFTER", cell_text(cell_xml(target, "O", 6)) == "\n".join(snap["acceptanceConversion"]["afterO"]))
print("APPROVED N LEN", len(snap["sheetOriginal"]["N"]), "O LEN", len(snap["sheetOriginal"]["O"]))
print("--- 8 phrases in approved N ---")
text = snap["sheetOriginal"]["N"]
for token in ["8点", "8×", "12.8", "256", "360", "最多8条", "180tick", "火伤8", "每10tick", "20tick", "200tick", "半径10", "焚天", "4×(1+0.1n+F)"]:
    print(token, text.count(token))
print("--- making rows without B ---")
for row in rows_of(sheet3):
    number = row_number(row)
    code = cell_text(cell_xml(row, "B", number))
    if number >= 3 and not (code and code.strip()):
        print(number, "cells", len(re.findall(r"<c ", row)), "head", row[:160])
print("making max", max(row_number(row) for row in rows_of(sheet3)))
print("pending ids")
for row in rows_of(sheet4):
    number = row_number(row)
    code = cell_text(cell_xml(row, "B", number))
    if number >= 3 and code:
        print(number, code, cell_text(cell_xml(row, "A", number)))
print("sheet3 dimension", re.search(r"<dimension[^/]*/>", sheet3).group(0))
print("sheet3 autofilter", re.findall(r"<autoFilter[^/]*/>", sheet3))
print("sheet4 autofilter", re.findall(r"<autoFilter[^/]*/>", sheet4))
print("resource has id", b"fire_liaoyuan" in z.read("xl/worksheets/sheet6.xml"))
