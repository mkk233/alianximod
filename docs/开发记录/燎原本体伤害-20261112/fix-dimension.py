# -*- coding: utf-8 -*-
import hashlib
import zipfile
from pathlib import Path

candidate = Path(r"D:\alianximod\docs\开发记录\燎原本体伤害-20261112\术式制作表-候选.xlsx")
name = "xl/worksheets/sheet4.xml"
with zipfile.ZipFile(candidate) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}
sheet = parts[name].decode("utf-8")
old = 'ref="A1:EN20"'
new = 'ref="A1:EN21"'
if sheet.count(old) != 1:
    raise SystemExit(f"dimension count {sheet.count(old)}")
parts[name] = sheet.replace(old, new, 1).encode("utf-8")
temporary = candidate.with_suffix(".tmp.xlsx")
with zipfile.ZipFile(temporary, "w") as archive:
    for info in order:
        archive.writestr(info, parts[info.filename])
temporary.replace(candidate)
print(hashlib.sha256(candidate.read_bytes()).hexdigest().upper())
