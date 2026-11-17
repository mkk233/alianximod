# -*- coding: utf-8 -*-
import json
from pathlib import Path

data = json.loads(Path(r"D:\alianximod\docs\开发记录\焚天定时燃尽-20261112\inspect-location.json").read_text(encoding="utf-8"))
hit = data["hits"]["待验收"][0]
cells = hit["cells"]
for column in ("A", "B", "E", "F", "N", "O", "AP", "AS", "AN", "AO", "L", "K"):
    item = cells[column]
    text = item["text"] or ""
    print("==", column, "len", len(text), "head", item["head"])
    print(text[:500])
print("== DV len", len(cells["DV"]["text"] or ""))
print((cells["DV"]["text"] or "")[:240])
print("---TAIL---")
print((cells["DV"]["text"] or "")[-700:])
