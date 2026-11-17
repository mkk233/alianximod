# -*- coding: utf-8 -*-
import hashlib
import json
import zipfile
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029")
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
backup = batch / "备份" / "test-stage-writeback-术式制作表-20261029-112003-1.xlsx"
notes = json.loads((batch / "test-stage-writeback-notes.json").read_text(encoding="utf-8"))
# Reuse the preserve parser by importing its functions after execution would re-run it.
# Compare zip parts and confirm each addition is present in sheet3 only.
live_bytes = live.read_bytes()
print(hashlib.sha256(live_bytes).hexdigest().upper())
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(live) as dst:
    print("names", src.namelist() == dst.namelist())
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    print("different", different)
    sheet = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")
for code, addition in notes["additions"].items():
    print(code, addition in sheet)
