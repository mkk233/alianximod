# -*- coding: utf-8 -*-
import hashlib
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
payload = live.read_bytes()
print("BYTES", len(payload))
print("SHA", hashlib.sha256(payload).hexdigest().upper())
with zipfile.ZipFile(live) as archive:
    bad = archive.testzip()
    print("ZIP", "ok" if bad is None else bad)
    print("PARTS", len(archive.namelist()))
