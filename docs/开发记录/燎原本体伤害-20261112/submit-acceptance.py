# -*- coding: utf-8 -*-
import hashlib
import json
import os
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
task = Path(r"D:\alianximod\docs\开发记录\燎原本体伤害-20261112")
source = task / "术式制作表-待验收候选.xlsx"
expected = "BD92D6AB57731DCA6D15656FD47AF6022AB8397A5EBCFBB1E746BC9A79EDFA2D"
candidate = "9A5960132434A5F6AE5322C32EDF27EE5B25E2A56CE5FF3F5B712B32AF8147B7"


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


if sha256(live) != expected or live.stat().st_size != 181761:
    raise SystemExit("Live workbook changed; candidate preserved")
if sha256(source) != candidate:
    raise SystemExit("Candidate hash changed")

def replace():
    os.replace(source, live)

try:
    replace()
    closed = False
except PermissionError:
    import win32com.client
    excel = win32com.client.GetActiveObject("Excel.Application")
    workbook = None
    for item in excel.Workbooks:
        if str(item.FullName).lower() == str(live).lower():
            workbook = item
    if workbook is None or not bool(workbook.Saved) or bool(workbook.ReadOnly):
        raise SystemExit("Workbook is missing, dirty, or read-only; candidate preserved")
    if sha256(live) != expected:
        raise SystemExit("Live workbook changed before close; candidate preserved")
    alerts = bool(excel.DisplayAlerts)
    excel.DisplayAlerts = False
    workbook.Close(SaveChanges=False)
    excel.DisplayAlerts = alerts
    if sha256(live) != expected:
        raise SystemExit("Live workbook changed during close; candidate preserved")
    replace()
    excel.Workbooks.Open(str(live), UpdateLinks=0, ReadOnly=False)
    closed = True

after = sha256(live)
if after != candidate:
    raise SystemExit(f"Submitted hash mismatch: {after}")
stat = live.stat()
result = {
    "submitted": True,
    "beforeSha256": expected,
    "afterSha256": after,
    "bytes": stat.st_size,
    "mtime": datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="milliseconds"),
    "excelReopened": closed,
}
(task / "acceptance-submit.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(result, ensure_ascii=False))
