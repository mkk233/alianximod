# -*- coding: utf-8 -*-
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parent
BASE = next((ROOT / "备份").glob("*-1.xlsx"))
PRE = next((ROOT / "备份").glob("*-2.xlsx"))
CANDIDATE = ROOT / "候选" / "candidate-move.xlsx"
SPEC = json.loads((ROOT / "move-rows.json").read_text(encoding="utf-8"))


def load(path):
    with zipfile.ZipFile(path) as book:
        return {name: book.read(name) for name in book.namelist()}


def rows(text):
    return {int(match.group(1)): match.group(0) for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text)}


def cell(xml, items, column, number):
    match = re.search(rf'<c r="{column}{number}"([^>]*)(?:/>|>([\s\S]*?)</c>)', xml)
    attrs, inner = match.group(1), match.group(2) or ""
    value = re.search(r"<v>([\s\S]*?)</v>", inner)
    if value is None:
        return None
    if 't="s"' in attrs:
        return items[int(value.group(1))]
    return value.group(1)


base, pre, candidate = load(BASE), load(PRE), load(CANDIDATE)
report = {
    "pendingRestored": candidate["xl/worksheets/sheet2.xml"] == base["xl/worksheets/sheet2.xml"],
    "unchangedSheets": {
        part: candidate[part] == pre[part] == base[part]
        for part in [
            "xl/worksheets/sheet1.xml",
            "xl/worksheets/sheet4.xml",
            "xl/worksheets/sheet5.xml",
            "xl/worksheets/sheet6.xml",
        ]
    },
    "commentsIdentical": all(candidate[name] == base[name] for name in candidate if "comment" in name.lower()),
}
making_pre = pre["xl/worksheets/sheet3.xml"].decode("utf-8")
making = candidate["xl/worksheets/sheet3.xml"].decode("utf-8")
archive_pre = pre["xl/worksheets/sheet7.xml"].decode("utf-8")
archive = candidate["xl/worksheets/sheet7.xml"].decode("utf-8")
old_making, new_making = rows(making_pre), rows(making)
old_archive, new_archive = rows(archive_pre), rows(archive)
report["makingOldIdentical"] = all(old_making[number] == new_making[number] for number in range(1, 20))
report["makingNewRows"] = sorted(number for number in new_making if number >= 20)
report["archiveOldIdentical"] = all(old_archive[number] == new_archive[number] for number in old_archive)
report["archiveNewRows"] = sorted(number for number in new_archive if number >= 26)
report["makingFilter"] = re.search(r"<autoFilter[^>]*/>", making).group(0)
report["makingValidationsIdentical"] = re.findall(r"<dataValidation\b[\s\S]*?</dataValidation>", making_pre) == re.findall(r"<dataValidation\b[\s\S]*?</dataValidation>", making)
book = candidate["xl/workbook.xml"].decode("utf-8")
report["filters"] = {
    "pending": "待制作!$A$2:$EN$2" in book,
    "making": "制作中!$A$2:$EN$27" in book,
    "oldMakingGone": "制作中!$A$2:$EN$19" not in book,
}
items = ["".join(ET.fromstring(body).itertext()) for body in re.findall(r"<si\b[^>]*>[\s\S]*?</si>", candidate["xl/sharedStrings.xml"].decode("utf-8"))]
marker = "制作中 | 多重与分裂扩展-20261109 | docs/开发记录/术式批量制作/多重与分裂扩展-20261109/记录.md"
checked = []
for row in SPEC["rows"]:
    archive_note = cell(archive, items, "DV", row["archiveRow"])
    making_note = cell(making, items, "DV", row["makingRow"])
    checked.append({
        "id": row["id"],
        "archiveId": cell(archive, items, "B", row["archiveRow"]) == row["id"],
        "archiveN": cell(archive, items, "N", row["archiveRow"]) == row["N"],
        "archiveO": cell(archive, items, "O", row["archiveRow"]) == row["O"],
        "archiveOriginal": archive_note == row["DV"] and marker not in archive_note,
        "makingId": cell(making, items, "B", row["makingRow"]) == row["id"],
        "makingN": cell(making, items, "N", row["makingRow"]) == row["N"],
        "makingO": cell(making, items, "O", row["makingRow"]) == row["O"],
        "makingMarker": making_note.endswith("\n" + marker) and row["DV"] in making_note,
        "source": cell(archive, items, "EQ", row["archiveRow"]),
        "sourceRow": cell(archive, items, "ER", row["archiveRow"]),
        "resource": cell(archive, items, "EX", row["archiveRow"]),
    })
report["rows"] = checked
report["allPassed"] = report["pendingRestored"] and all(report["unchangedSheets"].values()) and report["commentsIdentical"] and report["makingOldIdentical"] and report["archiveOldIdentical"] and report["makingValidationsIdentical"] and all(item["archiveOriginal"] and item["makingMarker"] and item["makingN"] and item["archiveN"] for item in checked)
print(json.dumps(report, ensure_ascii=False, indent=2))
if not report["allPassed"]:
    raise SystemExit(1)
