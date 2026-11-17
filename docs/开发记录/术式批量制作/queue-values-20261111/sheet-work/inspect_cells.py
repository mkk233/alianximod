# -*- coding: utf-8 -*-
import json
import re
import zipfile
from xml.etree import ElementTree as ET

LIVE = r"D:\alianximod\docs\术式制作表.xlsx"
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"


def rows_of(text):
    return {int(m.group(1)): m.group(0) for m in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text)}


def raw_cells(row):
    found = {}
    for match in re.finditer(r'<c\b[^>]*r="([A-Z]+)\d+"[\s\S]*?(?:/>|</c>)', row):
        found[match.group(1)] = match.group(0)
    return found


with zipfile.ZipFile(LIVE) as book:
    rels = ET.fromstring(book.read("xl/_rels/workbook.xml.rels"))
    targets = {}
    for node in rels:
        target = "xl/" + node.attrib["Target"].lstrip("/")
        targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
    work = ET.fromstring(book.read("xl/workbook.xml"))
    sheets = {node.attrib["name"]: targets[node.attrib[REL]] for node in work.find("s:sheets", NS)}
    pending = book.read(sheets["待制作"]).decode("utf-8")
    done = book.read(sheets["已完成"]).decode("utf-8")
    data = book.read(sheets["数据表"]).decode("utf-8")
    archive = book.read(sheets["待制作原始数据"]).decode("utf-8")

pending_rows = rows_of(pending)
report = {"pending": [], "examples": [], "dataTail": {}, "archiveFilter": "autoFilter" in archive, "dataMerges": []}
for number in range(3, 11):
    raw = raw_cells(pending_rows[number])
    report["pending"].append({
        "row": number,
        "attrs": re.search(r"<row\b[^>]*>", pending_rows[number]).group(0),
        "present": sorted(raw),
        "focus": {col: raw.get(col) for col in ("E", "F", "G", "H", "I", "K", "L", "N", "O", "R", "S", "V", "DX", "EC", "EJ", "EK", "DV")},
    })

done_rows = rows_of(done)
count = 0
for number, row in sorted(done_rows.items()):
    if number < 3:
        continue
    raw = raw_cells(row)
    if "EJ" in raw or "EK" in raw or "EC" in raw:
        text = "".join(ET.fromstring("<root>" + raw.get("A", "<c/>") + raw.get("EJ", "<c/>") + raw.get("EK", "<c/>") + raw.get("EC", "<c/>") + "</root>").itertext())
        report["examples"].append({
            "row": number,
            "A": "".join(ET.fromstring("<root>" + raw.get("A", "<c/>") + "</root>").itertext())[:20],
            "EC": raw.get("EC"),
            "EJ": raw.get("EJ"),
            "EK": raw.get("EK"),
            "DX": raw.get("DX"),
        })
        count += 1
        if count >= 8:
            break

report["dataMerges"] = re.findall(r"<mergeCell[^/]*/>", data)[:20]
report["dataFilter"] = re.search(r"<autoFilter\b[^/]*/>", data).group(0) if re.search(r"<autoFilter\b[^/]*/>", data) else None
report["pendingFilter"] = re.search(r"<autoFilter\b[^/]*/>", pending).group(0) if re.search(r"<autoFilter\b[^/]*/>", pending) else None
report["archiveDimension"] = re.search(r"<dimension[^/]*/>", archive).group(0)
report["dataDimension"] = re.search(r"<dimension[^/]*/>", data).group(0)
tail = data[data.rfind("<row r=\"16\""):data.find("</sheetData>")]
report["dataTail"] = tail[-500:]
out = r"D:\alianximod\docs\开发记录\术式批量制作\queue-values-20261111\sheet-work\cells.json"
with open(out, "w", encoding="utf-8") as handle:
    json.dump(report, handle, ensure_ascii=True, indent=2)
print("pending", len(report["pending"]), "examples", len(report["examples"]))
