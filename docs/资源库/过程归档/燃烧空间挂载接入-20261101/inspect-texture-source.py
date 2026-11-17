# -*- coding: utf-8 -*-
import json

PATH = r"D:\alianximod\docs\资源库\过程归档\燃烧v3_Blockbench挂载示意-20261101\燃烧v3_空间挂载示意_v1.bbmodel"
with open(PATH, "r", encoding="utf-8") as handle:
    data = json.load(handle)
for texture in data["textures"]:
    source = texture.get("source") or ""
    print(texture.get("id"), texture.get("name"), "source_len", len(source), "prefix", source[:30].replace("\n", ""))
print("--- groups ---")
for group in data["groups"]:
    print(group.get("name"), "origin", group.get("origin"), "rot", group.get("rotation"))
