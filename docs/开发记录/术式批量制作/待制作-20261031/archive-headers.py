# -*- coding: utf-8 -*-
import html
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
here = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")


def local(tag):
    return tag.rsplit("}", 1)[-1]


def col_index(letters):
    value = 0
    for char in letters:
        value = value * 26 + ord(char) - 64
    return value


def col_letters(index):
    letters = ""
    while index:
        index, remainder = divmod(index - 1, 26)
        letters = chr(65 + remainder) + letters
    return letters


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


archive = zipfile.ZipFile(live)
rels = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
targets = {}
for rel in rels:
    if local(rel.tag) != "Relationship":
        continue
    target = rel.attrib["Target"].lstrip("/")
    targets[rel.attrib["Id"]] = target if target.startswith("xl/") else "xl/" + target
root = ET.fromstring(archive.read("xl/workbook.xml"))
sheets = {}
for sheet in root.iter():
    if local(sheet.tag) != "sheet":
        continue
    rid = sheet.attrib["{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"]
    sheets[sheet.attrib["name"]] = archive.read(targets[rid]).decode("utf-8")

making = sheets["制作中"]
headers = {}
for row in rows_of(making):
    number = row_number(row)
    if number > 2:
        break
    headers[number] = {}
    for index in range(1, 145):
        column = col_letters(index)
        text = cell_text(row, column, number)
        if text:
            headers[number][column] = text

wanted = [
    "fire_flame_trail", "fire_meteorite", "fire_shock_ring", "rule_surface_anchor",
    "fire_upwell", "rule_stagger", "rule_hit_continue", "fire_seed_sow", "fire_kindle",
    "fire_detonate", "fire_nether_gloom", "earth_break_ground", "fire_spark",
]
rows = {}
for row in rows_of(making):
    number = row_number(row)
    code = cell_text(row, "B", number)
    if code in wanted:
        cells = {}
        for index in range(1, 145):
            column = col_letters(index)
            text = cell_text(row, column, number)
            if text not in (None, ""):
                cells[column] = text
        rows[code] = {"row": number, "cells": cells}

data = sheets["数据表"]
data_rows = []
for row in rows_of(data):
    number = row_number(row)
    if number > 40:
        break
    values = []
    for index in range(1, 30):
        column = col_letters(index)
        text = cell_text(row, column, number)
        if text:
            values.append(f"{column}{number}={text}")
    if values:
        data_rows.append(" | ".join(values))

validations = re.findall(r'<dataValidation\b([^>]*)>([\s\S]*?)</dataValidation>', making)
val_lines = []
for attrs, body in validations:
    formula = re.search(r"<formula1>([\s\S]*?)</formula1>", body)
    sqref = re.search(r'sqref="([^"]+)"', attrs)
    val_lines.append((sqref.group(1) if sqref else "", formula.group(1) if formula else ""))

out = here / "archive-headers.txt"
lines = ["# headers row1", json.dumps(headers.get(1), ensure_ascii=False), "# headers row2", json.dumps(headers.get(2), ensure_ascii=False)]
for code, item in rows.items():
    lines.append(f"\n# {code} row {item['row']}")
    for column, value in item["cells"].items():
        if column in ("N", "O", "DV"):
            lines.append(f"{column} len={len(value)}")
        else:
            label = headers.get(2, {}).get(column) or headers.get(1, {}).get(column) or ""
            lines.append(f"{column} {label} = {value}")
lines.append("\n# data")
lines.extend(data_rows)
lines.append("\n# validations")
for sqref, formula in val_lines:
    if any(token in formula for token in ("数据表", "角色", "弹道", "种类")) or sqref.startswith(("P", "Q", "R", "S", "C", "D")):
        lines.append(f"{sqref} {formula}")
out.write_text("\n".join(lines), encoding="utf-8")
print(out.stat().st_size, "rows", len(rows))
