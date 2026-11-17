# -*- coding: utf-8 -*-
import hashlib
import json
import os
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
task = Path(r"D:\alianximod\docs\开发记录\焚天定时燃尽-20261112")
source = task / "术式制作表-候选.xlsx"
expected = "9A5960132434A5F6AE5322C32EDF27EE5B25E2A56CE5FF3F5B712B32AF8147B7"
candidate = "9CF556DFCB06311CA0A17E3104A72483860C60B92207710025845919566B61E4"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


if digest(live) != expected or live.stat().st_size != 179165:
    raise SystemExit("Live workbook changed; candidate preserved")
if digest(source) != candidate:
    raise SystemExit("Candidate changed")


def replace():
    os.replace(source, live)


excel_reopened = False
try:
    replace()
except PermissionError:
    raise SystemExit("Workbook is locked; candidate preserved and no process was closed")

after = digest(live)
if after != candidate:
    raise SystemExit(f"Submitted hash mismatch: {after}")
stat = live.stat()
result = {
    "submitted": True,
    "beforeSha256": expected,
    "afterSha256": after,
    "bytes": stat.st_size,
    "mtime": datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="milliseconds"),
    "excelReopened": excel_reopened,
}
(task / "submit-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(result, ensure_ascii=False))
