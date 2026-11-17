# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
with zipfile.ZipFile(live) as archive:
    for name in ("xl/comments/comment3.xml", "xl/comments/comment4.xml"):
        xml = archive.read(name).decode("utf-8")
        refs = re.findall(r'ref="([A-Z]+)(\d+)"', xml)
        print(name, refs[:40], "count", len(refs))
