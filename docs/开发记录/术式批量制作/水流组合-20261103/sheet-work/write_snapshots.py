import json

src = r"D:\alianximod\docs\开发记录\术式批量制作\水流组合-20261103\sheet-work\extract.json"
out_dir = r"D:\alianximod\docs\开发记录\术式批量制作\水流组合-20261103\sheet-work"
d = json.load(open(src, encoding="utf-8"))

row = None
for t in d["targets"]:
    if t["sheet"] == "已完成" and t["row"] == 26:
        row = t
        break
snap = {
    "sheet": "已完成",
    "row": 26,
    "range": "A:EN",
    "columnCount": len(row["cells"]),
    "B": "water_deep_pressure",
    "A": "深海重压",
    "cells": row["cells"],
}
json.dump(snap, open(out_dir + r"\water_deep_pressure-A-EN.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)

details = [t for t in d["targets"] if t["sheet"] == "资源状态明细"]
json.dump(details, open(out_dir + r"\water_deep_pressure-资源明细.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)

# Named-range values by exact rows from data sheet nonempty cells.
wanted = {n["name"]: n["ref"] for n in d["definedNames"] if not n["name"].startswith("_")}
lists = {item["col"]: {cell["row"]: cell["value"] for cell in item["nonempty"]} for item in d["dataLists"]}

def col_of(ref):
    # 数据表!$A$4:$A$10
    body = ref.split("!")[-1].replace("$", "")
    start = body.split(":")[0]
    col = "".join(ch for ch in start if ch.isalpha())
    rows = []
    parts = body.split(":")
    for part in parts:
        digits = "".join(ch for ch in part if ch.isdigit())
        if digits:
            rows.append(int(digits))
    return col, rows

named = []
for name, ref in wanted.items():
    if "INDEX" in ref or "COUNTA" in ref:
        # continuous from the start cell downward until blank, using stored nonempty
        body = ref.split("!")[1]
        start = body.split(":")[0].replace("$", "")
        col = "".join(ch for ch in start if ch.isalpha())
        start_row = int("".join(ch for ch in start if ch.isdigit()))
        colmap = lists.get(col, {})
        values = []
        r = start_row
        while r in colmap and colmap[r] != "":
            values.append({"row": r, "value": colmap[r]})
            r += 1
        named.append({"name": name, "ref": ref, "values": values, "note": "continuous until blank; INDEX/COUNTA"})
        continue
    col, rows = col_of(ref)
    if len(rows) == 2:
        colmap = lists.get(col, {})
        values = [{"row": r, "value": colmap[r]} for r in range(rows[0], rows[1] + 1) if r in colmap]
    else:
        values = []
    named.append({"name": name, "ref": ref, "values": values})

json.dump(named, open(out_dir + r"\数据表下拉.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)
for item in named:
    vals = " | ".join(v["value"] for v in item["values"])
    print(item["name"], "=>", vals)
