# -*- coding: utf-8 -*-
import hashlib
import zipfile
from pathlib import Path

old = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031\approved-record-candidate-preserved.xlsx")
new = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031\blocked-compile-build.json")
import json
build = json.loads(new.read_text(encoding="utf-8"))
backup = Path(build["backup"])
with zipfile.ZipFile(old) as left, zipfile.ZipFile(backup) as right:
    left_names = set(left.namelist())
    right_names = set(right.namelist())
    print("old", hashlib.sha256(old.read_bytes()).hexdigest().upper())
    print("backup", hashlib.sha256(backup.read_bytes()).hexdigest().upper())
    print("only old", sorted(left_names - right_names))
    print("only new", sorted(right_names - left_names))
    for name in sorted(left_names & right_names):
        a = left.read(name)
        b = right.read(name)
        if a != b:
            print("DIFF", name, len(a), len(b))
