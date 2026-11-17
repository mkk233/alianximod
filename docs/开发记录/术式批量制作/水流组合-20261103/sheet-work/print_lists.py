import json

p = r"D:\alianximod\docs\开发记录\术式批量制作\水流组合-20261103\sheet-work\extract.json"
d = json.load(open(p, encoding="utf-8"))

print("=== DATA LISTS ===")
for item in d["dataLists"]:
    cont = item["continuousFromFirst"]
    if not cont:
        continue
    print(item["col"], "|", " || ".join(cont))

print("=== RELATED NODV ===")
want = {
    "water_deep_pressure", "water_curtain", "water_mist", "water_ring",
    "magic_create_water", "trajectory_storm", "tide_sever",
    "water_yielding_ribbon", "water_blade", "water_weeping_arc", "tide_mark",
}
for row in d["keyRows"]:
    b = row["fields"].get("B", "")
    if b in want and row["sheet"] in ("已完成", "待制作", "制作中", "待验收"):
        f = row["fields"]
        print("---", row["sheet"], row["row"], f.get("A"), b)
        for col in ("C", "D", "N", "O", "P", "Q", "R", "S", "DV"):
            print(" ", col, (f.get(col) or "").replace("\n", " / "))

print("=== IN PROGRESS ===")
for row in d["keyRows"]:
    if row["sheet"] in ("制作中", "待验收", "待制作"):
        f = row["fields"]
        if f.get("B"):
            print(row["sheet"], row["row"], f.get("A"), f.get("B"), f.get("C"))

print("=== VALIDATION SAMPLE 已完成 count ===")
for name, vals in d["statusValidations"].items():
    print(name, len(vals))
    if name == "已完成":
        for v in vals:
            print(" ", v["sqref"], v["type"], v["formula1"])

print("=== RESOURCE ALL A/B ===")
for row in d["resourceRows"]:
    cells = {c["col"]: c["value"] for c in row["cells"]}
    print(row["row"], cells.get("A"), cells.get("B"), cells.get("C"), cells.get("D"), cells.get("E"))

print("=== DEEP EMPTY COLS ===")
for t in d["targets"]:
    if t["sheet"] == "已完成" and t["row"] == 26:
        empty = [c["col"] for c in t["cells"] if c["value"] == ""]
        print("empty", len(empty), " ".join(empty))
        dv = [c for c in t["cells"] if c["col"] == "DV"]
        print("DV header", dv[0]["header1"], dv[0]["header2"], "value", repr(dv[0]["value"]))
