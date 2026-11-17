# -*- coding: utf-8 -*-
import html
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
columns = ["A", "E", "I", "M", "Q", "U", "Y", "AC", "AG", "AK", "AO", "AS", "BJ", "BL", "BN", "BP", "BR", "BT", "BV"]


def local(tag):
    return tag.rsplit("}", 1)[-1]


def text_of(node):
    parts = []
    if node.text:
        parts.append(node.text)
    for child in list(node):
        parts.append(text_of(child))
        if child.tail:
            parts.append(child.tail)
    return html.unescape("".join(parts))


with zipfile.ZipFile(live) as archive:
    shared = []
    root = ET.fromstring(archive.read("xl/sharedStrings.xml")) if "xl/sharedStrings.xml" in archive.namelist() else None
    if root is not None:
        for item in root:
            if local(item.tag) == "si":
                shared.append(text_of(item))
    sheet = ET.fromstring(archive.read("xl/worksheets/sheet5.xml"))
lists = {column: [] for column in columns}
for row in sheet.iter():
    if local(row.tag) != "row":
        continue
    number = int(row.attrib["r"])
    if number < 3:
        continue
    for cell in list(row):
        if local(cell.tag) != "c":
            continue
        match = re.match(r"([A-Z]+)(\d+)", cell.attrib.get("r", ""))
        if not match or match.group(1) not in lists:
            continue
        kind = cell.attrib.get("t")
        raw = None
        for child in list(cell):
            if local(child.tag) in ("v", "is"):
                raw = text_of(child)
        if kind == "s" and raw and raw.isdigit():
            raw = shared[int(raw)]
        if raw:
            lists[match.group(1)].append(raw)
path = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031\intake-lists.json")
path.write_text(json.dumps(lists, ensure_ascii=False, indent=2), encoding="utf-8")
print({key: len(value) for key, value in lists.items()})
