# -*- coding: utf-8 -*-
import zipfile
import xml.etree.ElementTree as ET

PATH = r"D:\alianximod\docs\术式制作表.xlsx"
NS = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}


def col_index(cell_ref):
    letters = "".join(ch for ch in cell_ref if ch.isalpha())
    value = 0
    for ch in letters:
        value = value * 26 + (ord(ch) - 64)
    return value


def col_name(index):
    letters = ""
    while index:
        index, remainder = divmod(index - 1, 26)
        letters = chr(65 + remainder) + letters
    return letters


def load_strings(archive):
    root = ET.fromstring(archive.read("xl/sharedStrings.xml"))
    strings = []
    for item in root.findall("m:si", NS):
        strings.append("".join(node.text or "" for node in item.iter("{http://schemas.openxmlformats.org/spreadsheetml/2006/main}t")))
    return strings


def cell_text(cell, strings):
    kind = cell.attrib.get("t")
    node = cell.find("m:v", NS)
    if node is None or node.text is None:
        inline = cell.find("m:is", NS)
        if inline is None:
            return ""
        return "".join(part.text or "" for part in inline.iter("{http://schemas.openxmlformats.org/spreadsheetml/2006/main}t"))
    if kind == "s":
        return strings[int(node.text)]
    return node.text


def dump_sheet(archive, strings, sheet_path, wanted):
    root = ET.fromstring(archive.read(sheet_path))
    for row in root.findall("m:sheetData/m:row", NS):
        values = {}
        for cell in row.findall("m:c", NS):
            ref = cell.attrib.get("r", "")
            values[col_index(ref)] = cell_text(cell, strings)
        if values.get(2) == wanted:
            print("ROW", row.attrib.get("r"), "sheet", sheet_path)
            for index in (14, 15, 126):
                text = values.get(index, "")
                print("---", col_name(index), "len", len(text))
                print(text[:4000])
            return True
    return False


def main():
    with zipfile.ZipFile(PATH) as archive:
        strings = load_strings(archive)
        found = dump_sheet(archive, strings, "xl/worksheets/sheet3.xml", "fire_burn")
        print("found_making", found)
        # 资源状态明细：找出包含 fire_burn 的行，打印前几列
        root = ET.fromstring(archive.read("xl/worksheets/sheet6.xml"))
        hits = 0
        for row in root.findall("m:sheetData/m:row", NS):
            values = {}
            for cell in row.findall("m:c", NS):
                ref = cell.attrib.get("r", "")
                values[col_index(ref)] = cell_text(cell, strings)
            blob = " ".join(values.values())
            if "fire_burn" in blob or "burn_cape" in blob or "燃烧" in blob and "披风" in blob:
                hits += 1
                if hits <= 8:
                    preview = {col_name(key): values[key][:180] for key in sorted(values)[:8]}
                    print("DETAIL", row.attrib.get("r"), preview)
        print("detail_hits", hits)


if __name__ == "__main__":
    main()
