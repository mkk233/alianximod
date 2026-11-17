# -*- coding: utf-8 -*-
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
PRIOR = Path(__file__).with_name("move-rows.json")
OUT = Path(__file__).with_name("locate-pending.json")
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"
WANTED = [
    "rule_triple_release", "rule_quadruple_release", "rule_octuple_release", "rule_sixteen_release",
    "rule_exhaustive_release", "rule_double_cast", "rule_quintuple_cast", "rule_octuple_cast",
]

with zipfile.ZipFile(LIVE) as book:
    data = {name: book.read(name) for name in book.namelist()}
rels = ET.fromstring(data["xl/_rels/workbook.xml.rels"])
targets = {}
for node in rels:
    target = "xl/" + node.attrib["Target"].lstrip("/")
    targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
work = ET.fromstring(data["xl/workbook.xml"])
names = {}
for node in work.find("s:sheets", NS):
    names[node.attrib["name"]] = targets[node.attrib[REL]]
sst = data["xl/sharedStrings.xml"].decode("utf-8")
items = ["".join(ET.fromstring(body).itertext()) for body in re.findall(r"<si\b[^>]*>[\s\S]*?</si>", sst)]


def rows_of(text):
    return {int(match.group(1)): match.group(0) for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text)}


def cells(row_xml):
    found = {}
    for match in re.finditer(r'<c\b([^>]*?)(?:/>|>([\s\S]*?)</c>)', row_xml):
        attrs = match.group(1)
        inner = match.group(2) or ""
        ref = re.search(r'r="([A-Z]+)\d+"', attrs)
        if not ref:
            continue
        kind = re.search(r't="([^"]+)"', attrs)
        value = re.search(r"<v>([\s\S]*?)</v>", inner)
        parsed = value.group(1) if value else None
        if kind and kind.group(1) == "s" and parsed is not None:
            parsed = items[int(parsed)]
        found[ref.group(1)] = parsed
    return found


status = {}
hits = {key: [] for key in WANTED}
for sheet in ("已完成", "待制作", "制作中", "待验收", "待制作原始数据", "资源状态明细"):
    text = data[names[sheet]].decode("utf-8")
    row_map = rows_of(text)
    ids = []
    for number, row in sorted(row_map.items()):
        if number < 3:
            continue
        values = cells(row)
        bid = values.get("B")
        if sheet == "资源状态明细":
            bid = values.get("A")
        if bid not in (None, ""):
            ids.append({"row": number, "id": str(bid).strip(), "name": values.get("A") if sheet != "资源状态明细" else values.get("B")})
            if str(bid).strip() in hits:
                hits[str(bid).strip()].append({"sheet": sheet, "row": number, "N": values.get("N"), "O": values.get("O"), "DV": values.get("DV")})
    dimension = re.search(r'<dimension[^>]*ref="([^"]+)"', text)
    status[sheet] = {
        "dimension": dimension.group(1) if dimension else None,
        "maxRow": max(row_map) if row_map else 0,
        "dataRows": len(ids),
        "ids": ids,
    }

prior = {row["id"]: row for row in json.loads(PRIOR.read_text(encoding="utf-8"))["rows"]}
compare = []
for stable in WANTED:
    places = [place for place in hits[stable] if place["sheet"] in ("已完成", "待制作", "制作中", "待验收")]
    archived = [place for place in hits[stable] if place["sheet"] == "待制作原始数据"]
    current = places[0] if len(places) == 1 else None
    entry = {"id": stable, "places": places, "archiveCount": len(archived)}
    if current and current["sheet"] == "制作中":
        old = prior[stable]
        entry["sameN"] = current["N"] == old["N"]
        entry["sameO"] = current["O"] == old["O"]
        entry["sameDV"] = current["DV"] == old["makingDV"]
    compare.append(entry)

OUT.write_text(json.dumps({"statusCounts": {key: {"dimension": value["dimension"], "maxRow": value["maxRow"], "dataRows": value["dataRows"]} for key, value in status.items()}, "ids": {key: value["ids"] for key, value in status.items() if key in ("已完成", "待制作", "制作中", "待验收")}, "compare": compare}, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"counts": {key: value["dataRows"] for key, value in status.items()}, "compare": compare}, ensure_ascii=False, indent=2))
