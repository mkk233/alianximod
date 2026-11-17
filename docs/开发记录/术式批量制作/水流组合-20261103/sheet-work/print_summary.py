import json

p = r"D:\alianximod\docs\开发记录\术式批量制作\水流组合-20261103\sheet-work\extract.json"
d = json.load(open(p, encoding="utf-8"))
print("=== HITS ===")
for h in d["keywordHits"]:
    print(h["sheet"], h["row"], h["A"], h["B"], ",".join(h["needles"]))
print("=== TARGETS ===")
for t in d["targets"]:
    nonempty = [c for c in t["cells"] if c["value"] != ""]
    print("---", t["sheet"], t["row"], t["needles"], "nonempty", len(nonempty), "cells", len(t["cells"]))
    for c in nonempty:
        h2 = c["header2"] or c["header1"]
        val = c["value"].replace("\n", " / ")
        print(" ", c["col"], h2 + ":", val[:500])
print("=== ARCHIVE ===")
for a in d["archiveB"]:
    print(a["row"], a["B"], a["A"])
print("=== NAMES ===")
for n in d["definedNames"]:
    print(n["name"], "|", n["ref"][:200])
print("=== KEY WATERISH ===")
words = ("水", "潮", "雾", "泡", "旋", "海", "涧", "泼", "幕", "牢", "环")
for row in d["keyRows"]:
    if row["sheet"] not in ("已完成", "待制作", "制作中", "待验收"):
        continue
    fields = row["fields"]
    blob = " ".join(fields.values())
    if any(w in blob for w in words):
        print(row["sheet"], row["row"], fields.get("A"), fields.get("B"), fields.get("C"), fields.get("D"))
