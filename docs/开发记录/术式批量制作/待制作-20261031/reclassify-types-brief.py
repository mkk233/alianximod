# -*- coding: utf-8 -*-
import json
from pathlib import Path

data = json.loads(Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031\reclassify-types-20261101-read.json").read_text(encoding="utf-8"))
keys = ["A", "C", "D", "J", "K", "L", "M", "P", "Q", "R", "S", "AB", "AM", "BX", "CH", "CR", "DB", "DN", "EJ", "EK"]
for title, page in data["pages"].items():
    print("##", title, page["count"])
    for entry in page["rows"]:
        cells = entry["cells"]
        bits = [f"{key}={cells[key]}" for key in keys if key in cells]
        text = cells.get("N", "").replace("\n", " / ")
        note = cells.get("DV", "").replace("\n", " / ")
        print(f"{entry['row']}\t{cells.get('B')}\t" + " | ".join(bits))
        print("  N:", text[:240])
        if note:
            print("  DV:", note[:180])
