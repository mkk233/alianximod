# -*- coding: utf-8 -*-
import importlib.util
import json
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
spec = importlib.util.spec_from_file_location("archive_write", HERE / "archive-write.py")
aw = importlib.util.module_from_spec(spec)
spec.loader.exec_module(aw)
old = HERE / "approved-record-candidate-preserved.xlsx"
build = json.loads((HERE / "blocked-compile-build.json").read_text(encoding="utf-8"))
backup = Path(build["backup"])

def sheet3(path):
    with zipfile.ZipFile(path) as archive:
        parts = {name: archive.read(name) for name in archive.namelist()}
    shared = []
    if "xl/sharedStrings.xml" in parts:
        root = ET.fromstring(parts["xl/sharedStrings.xml"])
        for item in root:
            if aw.local(item.tag) == "si":
                texts = []
                for node in item.iter():
                    if aw.local(node.tag) == "t" and node.text:
                        texts.append(node.text)
                shared.append("".join(texts))
    names = aw.sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
    xml = parts[names["制作中"]].decode("utf-8")
    return {code: (number, row) for code, number, row in aw.ids_of(xml, shared)}, shared

left, shared_left = sheet3(old)
right, shared_right = sheet3(backup)
print("ids", list(left), list(right))
for code in right:
    if code not in left:
        print("NEW", code)
        continue
    number_l, row_l = left[code]
    number_r, row_r = right[code]
    if row_l == row_r and number_l == number_r:
        continue
    print("CHANGED", code, "row", number_l, "->", number_r, "sameXml", row_l == row_r)
    if number_l == number_r:
        for column in ["N", "O", "DV", "L", "F", "P", "S", "R"]:
            a = aw.read_at(row_l, column, number_l, shared_left)
            b = aw.read_at(row_r, column, number_r, shared_right)
            if a != b:
                print(" ", column, "len", len(a or ""), len(b or ""))
