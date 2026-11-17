# -*- coding: utf-8 -*-
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parent
BACKUP = next((ROOT / "备份").glob("*-3.xlsx"))
CANDIDATE = ROOT / "候选" / "candidate-accept.xlsx"
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"


def load(path):
    with zipfile.ZipFile(path) as book:
        return {name: book.read(name) for name in book.namelist()}


def rows(text):
    return {int(match.group(1)): match.group(0) for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text)}


base, candidate = load(BACKUP), load(CANDIDATE)
rels = ET.fromstring(base["xl/_rels/workbook.xml.rels"])
targets = {}
for node in rels:
    target = "xl/" + node.attrib["Target"].lstrip("/")
    targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
names = {node.attrib["name"]: targets[node.attrib[REL]] for node in ET.fromstring(base["xl/workbook.xml"]).find("s:sheets", NS)}
making_old, making_new = rows(base[names["制作中"]].decode("utf-8")), rows(candidate[names["制作中"]].decode("utf-8"))
accept_old, accept_new = rows(base[names["待验收"]].decode("utf-8")), rows(candidate[names["待验收"]].decode("utf-8"))
report = {
    "makingKept": all(making_old[number] == making_new[number] for number in making_old if number < 20),
    "makingRemoved": all(number not in making_new for number in range(20, 28)),
    "acceptanceKept": all(accept_old[number] == accept_new[number] for number in accept_old),
    "acceptanceAdded": sorted(number for number in accept_new if number >= 14),
    "validations": re.findall(r"<dataValidation\b[\s\S]*?</dataValidation>", base[names["制作中"]].decode("utf-8")) == re.findall(r"<dataValidation\b[\s\S]*?</dataValidation>", candidate[names["制作中"]].decode("utf-8")) and re.findall(r"<dataValidation\b[\s\S]*?</dataValidation>", base[names["待验收"]].decode("utf-8")) == re.findall(r"<dataValidation\b[\s\S]*?</dataValidation>", candidate[names["待验收"]].decode("utf-8")),
    "comments": all(candidate[name] == base[name] for name in candidate if "comment" in name.lower()),
    "archive": candidate[names["待制作原始数据"]] == base[names["待制作原始数据"]],
    "makingFilter": "制作中!$A$2:$EN$19" in candidate["xl/workbook.xml"].decode("utf-8") and "制作中!$A$2:$EN$27" not in candidate["xl/workbook.xml"].decode("utf-8"),
    "acceptanceFilterUntouched": "待验收!$A$2:$EN$2" in candidate["xl/workbook.xml"].decode("utf-8"),
    "makingAuto": re.search(r"<autoFilter\b[^>]*/>", candidate[names["制作中"]].decode("utf-8")).group(0),
    "acceptanceAuto": re.search(r"<autoFilter\b[^>]*/>", candidate[names["待验收"]].decode("utf-8")).group(0),
}
report["passed"] = all(report[key] for key in ("makingKept", "makingRemoved", "acceptanceKept", "validations", "comments", "archive", "makingFilter", "acceptanceFilterUntouched")) and report["acceptanceAdded"] == list(range(14, 22))
print(json.dumps(report, ensure_ascii=False, indent=2))
if not report["passed"]:
    raise SystemExit(1)
