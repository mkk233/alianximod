# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\灼地-20261030")
snapshot = json.loads((batch / "params-snapshot.json").read_text(encoding="utf-8"))
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
            if code:
                found.append(str(code).strip())
    return found


with zipfile.ZipFile(live) as archive:
    sheets = {name: archive.read(name) for name in (
        "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml",
        "xl/worksheets/sheet4.xml", "xl/worksheets/sheet5.xml", "xl/worksheets/sheet6.xml",
        "xl/worksheets/sheet7.xml",
    )}
with zipfile.ZipFile(snapshot["backup"]) as backup:
    backup_parts = {name: backup.read(name) for name in sheets}
making = sheets["xl/worksheets/sheet3.xml"].decode("utf-8")
archive_xml = sheets["xl/worksheets/sheet7.xml"].decode("utf-8")
row6 = next(row for row in rows_of(making) if row_number(row) == 6)
archive_row = next(row for row in rows_of(archive_xml) if row_number(row) == 9)
status = {
    "已完成": ids(sheets["xl/worksheets/sheet1.xml"].decode("utf-8")),
    "待制作": ids(sheets["xl/worksheets/sheet2.xml"].decode("utf-8")),
    "制作中": ids(making),
    "待验收": ids(sheets["xl/worksheets/sheet4.xml"].decode("utf-8")),
}
flat = [code for rows in status.values() for code in rows]
unchanged = [name for name in sheets if name != "xl/worksheets/sheet3.xml" and sheets[name] == backup_parts[name]]
stat = live.stat()
report = {
    "liveEqualsCandidate": data == candidate,
    "sha256": sha256(data),
    "bytes": len(data),
    "mtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "counts": {name: len(rows) for name, rows in status.items()},
    "unique": len(set(flat)),
    "total": len(flat),
    "name": cell_text(row6, "A", 6),
    "id": cell_text(row6, "B", 6),
    "levelCap": cell_text(row6, "G", 6),
    "complexity": cell_text(row6, "H", 6),
    "damage": cell_text(row6, "E", 6),
    "cooldown": cell_text(row6, "F", 6),
    "costEmpty": cell_text(row6, "K", 6) is None and cell_text(row6, "L", 6) is None,
    "dvKept": cell_text(row6, "DV", 6) == snapshot["dvAfter"],
    "archiveLevelEmpty": cell_text(archive_row, "G", 9) is None and cell_text(archive_row, "H", 9) is None,
    "archiveUntouched": "xl/worksheets/sheet7.xml" in unchanged,
    "otherSheetsUntouched": len(unchanged) == 6,
    "makingOrder": status["制作中"],
}
ok = all([
    report["liveEqualsCandidate"], report["counts"] == {"已完成": 40, "待制作": 13, "制作中": 4, "待验收": 3},
    report["unique"] == 60, report["total"] == 60, report["name"] == "灼地", report["id"] == "fire_scorch_field",
    report["levelCap"] == "20", report["complexity"] == "0", report["damage"] == "1", report["cooldown"] == "30",
    report["costEmpty"], report["dvKept"], report["archiveLevelEmpty"], report["archiveUntouched"],
    report["otherSheetsUntouched"], report["makingOrder"] == ["fire_heat_up", "fire_burn", "fire_cool", "fire_scorch_field"],
    report["sha256"] != snapshot["baselineSha256"],
])
(batch / "params-reopen.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
if not ok:
    raise SystemExit(json.dumps(report, ensure_ascii=False, indent=2))
print(json.dumps({k: report[k] for k in ("sha256", "mtime", "levelCap", "complexity", "counts", "unique")}, ensure_ascii=False))
