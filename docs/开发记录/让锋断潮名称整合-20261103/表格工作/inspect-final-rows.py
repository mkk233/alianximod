# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
TARGETS = ("tide_sever", "water_yielding_ribbon")
STATUS = ("已完成", "待制作", "制作中", "待验收")
PARTS = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}


def local(tag):
    return tag.rsplit("}", 1)[-1]


def sha256_file(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def shared_list(xml):
    root = ET.fromstring(xml)
    found = []
    for item in root:
        if local(item.tag) != "si":
            continue
        texts = []
        for node in item.iter():
            if local(node.tag) == "t" and node.text:
                texts.append(html.unescape(node.text))
        found.append("".join(texts))
    return found


def cell_text(cell, shared):
    kind = cell.attrib.get("t")
    if kind == "inlineStr":
        return "".join(html.unescape(node.text) for node in cell.iter() if local(node.tag) == "t" and node.text)
    value_node = next((child for child in list(cell) if local(child.tag) == "v"), None)
    if value_node is None or value_node.text is None:
        return None
    raw = html.unescape(value_node.text)
    if kind == "s" and raw.isdigit():
        return shared[int(raw)]
    return raw


def parse_sheet(xml, shared):
    root = ET.fromstring(xml)
    rows = {}
    for node in root.iter():
        if local(node.tag) != "row":
            continue
        number = int(node.attrib["r"])
        cells = {}
        for cell in list(node):
            if local(cell.tag) != "c":
                continue
            match = re.match(r"([A-Z]+)", cell.attrib.get("r", ""))
            if match:
                cells[match.group(1)] = cell_text(cell, shared)
        rows[number] = cells
    return rows


def main():
    stat = LIVE.stat()
    with zipfile.ZipFile(LIVE) as book:
        shared = shared_list(book.read("xl/sharedStrings.xml"))
        workbook = book.read("xl/workbook.xml").decode("utf-8")
        names = re.findall(r"<definedName\b([^>]*)>([\s\S]*?)</definedName>", workbook)
        interesting = [item[1].strip() for item in names if any(token in item[1] for token in ("制作中", "待验收", "已完成"))]
        payload = {
            "sha256": sha256_file(LIVE),
            "mtime": datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="milliseconds"),
            "size": stat.st_size,
            "definedNames": interesting,
        }
        counts = {}
        hits = []
        for title, part in PARTS.items():
            xml = book.read(part).decode("utf-8")
            rows = parse_sheet(xml, shared)
            data = [number for number, cells in sorted(rows.items()) if number >= 3 and (cells.get("B") or "").strip()]
            counts[title] = len(data)
            dim = re.search(r'<dimension ref="([^"]+)"', xml)
            filt = re.search(r'<autoFilter ref="([^"]+)"', xml)
            payload.setdefault("structure", {})[title] = {
                "dimension": dim.group(1) if dim else None,
                "autoFilter": filt.group(1) if filt else None,
                "dataRows": data,
            }
            for number, cells in sorted(rows.items()):
                if number < 3:
                    continue
                code = (cells.get("B") or "").strip()
                if code in TARGETS:
                    hits.append({
                        "sheet": title,
                        "row": number,
                        "A": cells.get("A"),
                        "B": code,
                        "N": cells.get("N"),
                        "O": cells.get("O"),
                        "DV": cells.get("DV"),
                        "E": cells.get("E"),
                        "F": cells.get("F"),
                        "L": cells.get("L"),
                    })
        payload["counts"] = counts
        payload["hits"] = hits
    print(json.dumps(payload, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
