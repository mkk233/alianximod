from pathlib import Path
from zipfile import ZipFile
from xml.etree import ElementTree as ET
import hashlib
import json
import datetime

ROOT = Path(__file__).resolve().parents[1]
OLD = ROOT.parent / "崩地_坑口扬尘_v2_20261116"
BOOK = Path("D:/alianximod/docs/术式制作表.xlsx")
for folder in ["source", "exports/frames", "previews/overall-frames", "previews/side-frames", "evidence"]:
    (ROOT / folder).mkdir(parents=True, exist_ok=True)
protected = [BOOK, *[path for path in OLD.rglob("*") if path.is_file()]]
protected += list(Path("D:/alianximod/docs/资源库/特效/大地/崩地_v1").glob("*"))
protected += [Path("D:/alianximod/src/main/resources/assets/xueximod/textures/effect/ground_dust_v1/ground_dust.png")]
hashes = {str(path): hashlib.sha256(path.read_bytes()).hexdigest() for path in protected if path.is_file()}
(ROOT / "evidence/protected-before.json").write_text(json.dumps(hashes, ensure_ascii=False, indent=2), encoding="utf-8")
ns = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
with ZipFile(BOOK) as archive:
    workbook = ET.fromstring(archive.read("xl/workbook.xml"))
    relations = {item.attrib["Id"]: item.attrib["Target"] for item in ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))}
    strings = ["".join(item.itertext()) for item in ET.fromstring(archive.read("xl/sharedStrings.xml"))] if "xl/sharedStrings.xml" in archive.namelist() else []
    sheet = next(item for item in workbook.find("s:sheets", ns) if item.attrib["name"] == "制作中")
    target = relations[sheet.attrib["{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"]]
    target = target.lstrip("/") if target.startswith("/") else "xl/" + target
    def value(cell):
        if cell.attrib.get("t") == "inlineStr":
            return "".join(cell.find("s:is", ns).itertext())
        content = cell.find("s:v", ns)
        if content is None:
            return ""
        return strings[int(content.text)] if cell.attrib.get("t") == "s" else content.text
    rows = [{cell.attrib["r"]: value(cell) for cell in row} for row in ET.fromstring(archive.read(target)).findall(".//s:sheetData/s:row", ns)]
    row = next(item for item in rows if "earth_break_ground" in item.values())
    row_number = next(key[1:] for key in row if key.startswith("B") and row[key] == "earth_break_ground")
    cells = {column: row.get(column + row_number, "") for column in ["B", "N", "O", "DV"]}
old_snapshot = json.loads((OLD / "evidence/requirements-N-O-DV-original.json").read_text(encoding="utf-8"))
snapshot = {
    "batchId": "待制作-20261031", "taskId": "earth_break_ground_v2_art_preview",
    "attemptId": "20261116-art-ground-v2-04", "capturedAt": datetime.datetime.now().astimezone().isoformat(),
    "workbook": str(BOOK), "sha256": hashes[str(BOOK)], "bytes": BOOK.stat().st_size,
    "sheet": "制作中", "row": int(row_number), "cells": cells,
    "previousWorkbookSha256": old_snapshot["sha256"],
    "changesFromPrevious": {column: cells[column] != old_snapshot["cells"][column] for column in cells},
    "userFeedbackVerbatim": "这个尘土效果不好没有飞扬的感觉",
    "rejectedCandidate": "earth-ground-mouth-dust-v2-20261116",
    "status": "v2 rejected; v2r2 candidate not yet confirmed; workbook read only",
}
(ROOT / "evidence/requirements-N-O-DV-current.json").write_text(json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8")
(ROOT / "evidence/requirements-N-O-DV-current.txt").write_text("\n\n".join(column + " 原文\n" + text for column, text in cells.items()), encoding="utf-8")
(ROOT / "evidence/requirements-N-O-DV-previous.json").write_bytes((OLD / "evidence/requirements-N-O-DV-original.json").read_bytes())
(ROOT / "evidence/preparation-input.json").write_bytes(Path("D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/ground-v2-requirements-snapshot-20261116.json").read_bytes())
(ROOT / "source/preview_auxiliary_palette.aseprite").write_bytes((OLD / "source/preview_auxiliary_palette.aseprite").read_bytes())
(ROOT / "exports/preview_auxiliary_palette.png").write_bytes((OLD / "exports/preview_auxiliary_palette.png").read_bytes())
emitters = [
    [-0.82, -0.20, 1.40, -0.94, -0.30, 1.20, 0.00, 0],
    [0.48, 0.28, 1.55, 0.86, 0.64, 1.46, 0.05, 1],
    [0.10, -1.05, 1.12, 0.10, -1.06, 0.98, 0.00, 2],
    [-1.38, 0.48, 1.08, -0.88, 0.38, 0.67, 0.10, 1],
    [1.26, -0.36, 1.18, 0.94, -0.56, 0.80, 0.05, 0],
    [-0.26, 1.32, 1.02, -0.44, 0.82, 0.56, 0.10, 2],
    [0.16, 0.02, 1.30, -0.16, 0.26, 1.64, 0.00, 2],
    [-0.54, -0.72, 0.88, -0.72, -0.80, 1.08, 0.10, 0],
    [0.82, 0.86, 0.84, 0.80, 0.92, 0.92, 0.05, 1],
]
grit = [
    [-0.48, 0.22, -1.38, 0.46, 1.80, 0.90, 0.13, 0.00],
    [0.28, 0.12, 1.70, 0.78, 2.14, 1.05, 0.16, 0.00],
    [0.00, -0.65, 0.35, -1.80, 1.45, 0.85, 0.10, 0.05],
    [-1.18, -0.20, -1.50, -0.65, 0.94, 0.70, 0.12, 0.05],
    [0.88, -0.52, 1.50, -1.05, 1.28, 0.80, 0.09, 0.00],
    [-0.18, 0.84, -0.75, 1.65, 1.10, 0.75, 0.14, 0.05],
    [0.12, -0.10, -0.80, 0.34, 2.35, 1.10, 0.10, 0.00],
    [-0.60, -0.72, -0.80, -1.48, 1.62, 0.95, 0.07, 0.10],
    [1.15, 0.62, 1.30, 0.88, 0.82, 0.65, 0.08, 0.05],
    [-0.96, 0.75, -0.92, 1.26, 1.32, 0.85, 0.09, 0.10],
    [0.45, -1.20, 0.75, -1.35, 0.70, 0.65, 0.12, 0.05],
    [0.58, 0.48, 0.92, 1.14, 1.76, 0.95, 0.07, 0.10],
]
recipe = {
    "version": "earth-ground-airborne-dust-v2r2-20261116",
    "trigger": 0.4, "previewSeconds": 2.3, "visualSeconds": 1.4,
    "dustLocalLifetime": 1.2, "unitsPerBlock": 16, "groundY": 0.5, "rootY": 0.56,
    "dustFields": ["x", "z", "size", "dx", "dz", "peakRise", "delay", "bank"],
    "dust": emitters,
    "gritFields": ["x", "z", "dx", "dz", "apexRise", "flightSeconds", "size", "delay"],
    "grit": grit,
    "gritFormula": "u=age/flightSeconds; offset=(dx*u,4*apexRise*u*(1-u),dz*u); visual only",
}
(ROOT / "evidence/motion-recipe.json").write_text(json.dumps(recipe, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"workbookHash": snapshot["sha256"], "row": row_number, "changed": snapshot["changesFromPrevious"], "protectedFiles": len(hashes)}, ensure_ascii=False))
