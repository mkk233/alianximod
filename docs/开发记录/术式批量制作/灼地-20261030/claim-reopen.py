# -*- coding: utf-8 -*-
"""Reopen the submitted workbook and check the claim result."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\灼地-20261030")
snapshot = json.loads((batch / "claim-snapshot.json").read_text(encoding="utf-8"))
data = live.read_bytes()
candidate = Path(snapshot["candidate"]).read_bytes()


def sha256(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_text(row, column, number):
    match = re.search(rf'<(?:[\w]+:)?c r="{column}{number}"[\s\S]*?</(?:[\w]+:)?c>', row)
    if not match:
        return None
    inner = match.group(0)
    if 't="inlineStr"' in inner:
        parts = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", inner)
        return html.unescape("".join(parts)) if parts else None
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?v>", inner)
    return html.unescape(value.group(1)) if value else None


def ids(xml, column="B"):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number >= 3:
            code = cell_text(row, column, number)
            if code and str(code).strip():
                found.append((str(code).strip(), number))
    return found


with zipfile.ZipFile(live) as archive:
    sheets = {
        "已完成": archive.read("xl/worksheets/sheet1.xml").decode("utf-8"),
        "待制作": archive.read("xl/worksheets/sheet2.xml").decode("utf-8"),
        "制作中": archive.read("xl/worksheets/sheet3.xml").decode("utf-8"),
        "待验收": archive.read("xl/worksheets/sheet4.xml").decode("utf-8"),
        "数据表": archive.read("xl/worksheets/sheet5.xml"),
        "资源状态明细": archive.read("xl/worksheets/sheet6.xml").decode("utf-8"),
        "待制作原始数据": archive.read("xl/worksheets/sheet7.xml").decode("utf-8"),
    }
with zipfile.ZipFile(snapshot["backup"]) as backup:
    backup_data = backup.read("xl/worksheets/sheet5.xml")
status = {name: ids(sheets[name]) for name in ("已完成", "待制作", "制作中", "待验收")}
flat = [code for rows in status.values() for code, _number in rows]
making_rows = {row_number(row): row for row in rows_of(sheets["制作中"])}
archive_rows = {row_number(row): row for row in rows_of(sheets["待制作原始数据"])}
detail_rows = {row_number(row): row for row in rows_of(sheets["资源状态明细"])}
pending_codes = [code for code, _number in status["待制作"]]
stat = live.stat()
report = {
    "liveEqualsCandidate": data == candidate,
    "sha256": sha256(data),
    "bytes": len(data),
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "counts": {name: len(rows) for name, rows in status.items()},
    "unique": len(set(flat)),
    "total": len(flat),
    "pendingHasScorch": "fire_scorch_field" in pending_codes,
    "making": status["制作中"],
    "archive": ids(sheets["待制作原始数据"]),
    "makingName": cell_text(making_rows[6], "A", 6),
    "makingN": cell_text(making_rows[6], "N", 6) == snapshot["n"],
    "makingO": cell_text(making_rows[6], "O", 6) == snapshot["o"],
    "makingDv": cell_text(making_rows[6], "DV", 6) == snapshot["dvAfter"],
    "archiveN": cell_text(archive_rows[9], "N", 9) == snapshot["n"],
    "archiveO": cell_text(archive_rows[9], "O", 9) == snapshot["o"],
    "archiveDvOriginal": cell_text(archive_rows[9], "DV", 9) == snapshot["dv"],
    "archiveHasMarker": snapshot["marker"] in (cell_text(archive_rows[9], "DV", 9) or ""),
    "detailId": cell_text(detail_rows[7], "A", 7),
    "detailSeq": cell_text(detail_rows[7], "B", 7),
    "detailValue": cell_text(detail_rows[7], "I", 7),
    "detailPeriod": cell_text(detail_rows[7], "M", 7),
    "dataSheetUnchanged": sheets["数据表"] == backup_data,
    "noteKept": "使用说明" in (cell_text(archive_rows[10], "A", 10) or ""),
    "damage": cell_text(making_rows[6], "E", 6),
    "cooldown": cell_text(making_rows[6], "F", 6),
    "trajectoryEmpty": cell_text(making_rows[6], "R", 6) is None,
}
checks = [
    report["liveEqualsCandidate"], report["makingN"], report["makingO"], report["makingDv"],
    report["archiveN"], report["archiveO"], report["archiveDvOriginal"], not report["archiveHasMarker"],
    report["counts"] == {"已完成": 40, "待制作": 13, "制作中": 4, "待验收": 3},
    report["unique"] == 60, report["total"] == 60, not report["pendingHasScorch"],
    report["makingName"] == "灼地", report["detailId"] == "fire_scorch_field", report["detailSeq"] == "2",
    report["detailValue"] == "15", report["detailPeriod"] == "20", report["dataSheetUnchanged"],
    report["noteKept"], report["damage"] == "1", report["cooldown"] == "30", report["trajectoryEmpty"],
    report["sha256"] != snapshot["baselineSha256"],
]
if not all(checks):
    raise SystemExit(json.dumps(report, ensure_ascii=False, indent=2))
(batch / "claim-reopen.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({k: report[k] for k in ("sha256", "mtime", "bytes", "counts", "unique", "makingName")}, ensure_ascii=False))
