# -*- coding: utf-8 -*-
import json
import re
from xml.etree import ElementTree as ET

SRC = r"D:\alianximod\docs\开发记录\术式批量制作\queue-values-20261111\sheet-work\cells.json"
OUT = r"D:\alianximod\docs\开发记录\术式批量制作\queue-values-20261111\sheet-work\cells-summary.json"


def text(xml):
    if not xml:
        return None
    if "</c>" not in xml:
        return ""
    inner = re.search(r">(.*)</c>", xml, re.S).group(1)
    return "".join(ET.fromstring("<root>" + inner + "</root>").itertext())


data = json.load(open(SRC, encoding="utf-8"))
pending = []
for item in data["pending"]:
    values = {key: text(item["focus"].get(key)) for key in item["focus"]}
    pending.append({"row": item["row"], "attrs": item["attrs"], "present": item["present"], "values": values, "raw": {key: item["focus"].get(key) for key in ("E", "F", "EC", "N")}})
examples = []
for item in data["examples"]:
    examples.append({key: text(item.get(key)) if key != "row" and key != "A" else item.get(key) for key in ("row", "A", "DX", "EC", "EJ", "EK")})
report = {
    "filters": {"pending": data["pendingFilter"], "data": data["dataFilter"], "archive": data["archiveFilter"]},
    "dims": {"archive": data["archiveDimension"], "data": data["dataDimension"]},
    "merges": data["dataMerges"],
    "tail": data["dataTail"],
    "examples": examples,
    "pending": pending,
}
json.dump(report, open(OUT, "w", encoding="utf-8"), ensure_ascii=True, indent=2)
print("ok")
