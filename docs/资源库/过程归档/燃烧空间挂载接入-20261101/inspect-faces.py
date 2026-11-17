# -*- coding: utf-8 -*-
import json

PATH = r"D:\alianximod\docs\资源库\过程归档\燃烧v3_Blockbench挂载示意-20261101\燃烧v3_空间挂载示意_v1.bbmodel"
with open(PATH, "r", encoding="utf-8") as handle:
    data = json.load(handle)

print("groups field type", type(data.get("groups")), "len", len(data.get("groups") or []))
node = data["outliner"][0]
print("root keys", sorted(node.keys()))
print("root raw name", node.get("name"), "origin", node.get("origin"))
print("child0 type", type(node["children"][0]))
child = node["children"][0]
if isinstance(child, dict):
    print("child keys", sorted(child.keys()))
    print({k: child[k] for k in child if k != "children"})

print("--- per mesh ---")
for element in data["elements"]:
    if element.get("type") != "mesh":
        continue
    textures = set()
    sides = set()
    vertex_count = 0
    min_u = 999
    min_v = 999
    max_u = -1
    max_v = -1
    poly = []
    for face in element["faces"].values():
        textures.add(face.get("texture"))
        poly.append(len(face.get("vertices", [])))
        uv = face.get("uv") or {}
        for pair in uv.values():
            min_u = min(min_u, pair[0])
            max_u = max(max_u, pair[0])
            min_v = min(min_v, pair[1])
            max_v = max(max_v, pair[1])
    print(f"{element['name']}: faces={len(element['faces'])} verts={len(element['vertices'])} tex={textures} poly={set(poly)} uv=({min_u:.1f}-{max_u:.1f},{min_v:.1f}-{max_v:.1f}) rot={element.get('rotation')}")
