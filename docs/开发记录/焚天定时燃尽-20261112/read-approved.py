# -*- coding: utf-8 -*-
import json
from pathlib import Path

before = json.loads(Path(r"D:\alianximod\docs\开发记录\火种共享燃料改造-20261029\final-writeback-before.json").read_text(encoding="utf-8"))
conversion = json.loads(Path(r"D:\alianximod\docs\开发记录\火种共享燃料改造-20261029\final-writeback-conversion.json").read_text(encoding="utf-8"))
item = next(row for row in before["items"] if row.get("id") == "fire_fentian")
converted = conversion["items"]["fire_fentian"]
print("before keys", list(item))
print("sheet", item.get("sheet"), "row", item.get("row"))
print("N len", len(item.get("n", "")), "O len", len(item.get("o", "")), "DV len", len(item.get("dv", "")))
print("conversion before", len(converted["beforeN"]), len(converted["beforeO"]), len(converted["beforeDv"]))
print("equal N", item.get("n") == converted["beforeN"])
print("equal O", item.get("o") == converted["beforeO"])
print("equal DV", item.get("dv") == converted["beforeDv"])
print("displayN", converted["displayN"])
print("displayO", converted["displayO"])
print("cells", json.dumps(item.get("cells"), ensure_ascii=False)[:2000])
out = Path(r"D:\alianximod\docs\开发记录\焚天定时燃尽-20261112\approved-extract.txt")
out.write_text("N\n" + item["n"] + "\n\nO\n" + item["o"] + "\n\nDV-TAIL\n" + item["dv"][-900:], encoding="utf-8")
