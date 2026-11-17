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


def cell_text(xml):
    if "</c>" not in xml:
        return ""
    inner = re.search(r">(.*)</c>", xml, re.S).group(1)
    return "".join(ET.fromstring("<root>" + inner + "</root>").itertext())


def cells(row):
    found = {}
    for match in re.finditer(r'<c\b[^>]*r="([A-Z]+)\d+"[\s\S]*?(?:/>|</c>)', row):
        found[match.group(1)] = cell_text(match.group(0))
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
    resource = book.read(sheets["资源状态明细"]).decode("utf-8")
    archive = book.read(sheets["待制作原始数据"]).decode("utf-8")

wanted = ("火种", "水势")
examples = []
for number, row in sorted(rows_of(done).items()):
    found = cells(row)
    if found.get("DX") in wanted and found.get("EC"):
        examples.append({col: found.get(col) for col in ("A", "B", "DX", "DZ", "EA", "EB", "EC", "EI", "EJ", "EK", "EL", "EM")})
        if len(examples) >= 6:
            break

pending_examples = []
for number, row in sorted(rows_of(pending).items()):
    found = cells(row)
    if found.get("DX"):
        pending_examples.append({col: found.get(col) for col in ("A", "B", "DW", "DX", "DZ", "EA", "EB", "EC", "EE", "EI", "EJ", "EK", "EL", "EM")})

resource_rows = rows_of(resource)
resource_header = {"row1": cells(resource_rows[1]), "row2": cells(resource_rows[2])}
resource_ids = []
for number, row in sorted(resource_rows.items()):
    if number < 3:
        continue
    found = cells(row)
    if found.get("B"):
        resource_ids.append({"row": number, "id": found.get("B"), "name": found.get("A")})

archive_rows = rows_of(archive)
archive_row3 = cells(archive_rows[3])
report = {
    "doneExamples": examples,
    "pendingResource": pending_examples,
    "resourceIds": resource_ids,
    "resourceHeader": resource_header,
    "archiveRow3Meta": {col: archive_row3.get(col) for col in ("A", "B", "EP", "EQ", "ER", "ES", "ET", "EU", "EV", "EX")},
    "archiveCols3": sorted(k for k, v in archive_row3.items() if v not in (None, "")),
}
out = r"D:\alianximod\docs\开发记录\术式批量制作\queue-values-20261111\sheet-work\resource.json"
json.dump(report, open(out, "w", encoding="utf-8"), ensure_ascii=True, indent=2)
print(len(examples), len(pending_examples), len(resource_ids))
