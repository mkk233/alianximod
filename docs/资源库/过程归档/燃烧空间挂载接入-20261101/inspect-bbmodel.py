# -*- coding: utf-8 -*-
import json

PATH = r"D:\alianximod\docs\资源库\过程归档\燃烧v3_Blockbench挂载示意-20261101\燃烧v3_空间挂载示意_v1.bbmodel"
with open(PATH, "r", encoding="utf-8") as handle:
    data = json.load(handle)

print("resolution", data.get("resolution"))
print("texture0 keys", sorted(data["textures"][0].keys()))
for texture in data["textures"]:
    print("TEX", texture.get("id"), texture.get("name"), "uv", texture.get("uv_width"), texture.get("uv_height"),
          "size", texture.get("width"), texture.get("height"), "path", (texture.get("relative_path") or texture.get("name")))

elements = {element["uuid"]: element for element in data["elements"]}

def walk(node, depth):
    if isinstance(node, list):
        for child in node:
            walk(child, depth)
        return
    if isinstance(node, str):
        element = elements.get(node)
        name = element.get("name") if element else node
        print("  " * depth + f"ELEM {name} type={element.get('type') if element else '?'}")
        return
    name = node.get("name")
    print("  " * depth + f"GROUP {name} origin={node.get('origin')} rot={node.get('rotation')} uuid={node.get('uuid')}")
    for child in node.get("children", []):
        walk(child, depth + 1)

walk(data["outliner"], 0)

print("--- sample meshes ---")
for element in data["elements"]:
    if element.get("name") not in ("fx_right_fist_core", "fx_cape_curved", "示意辅助_body", "fx_hand_right_front_shell"):
        continue
    print("NAME", element.get("name"), "type", element.get("type"))
    print(" keys", sorted(element.keys()))
    print(" origin", element.get("origin"), "rot", element.get("rotation"), "from", element.get("from"), "to", element.get("to"))
    vertices = element.get("vertices")
    if isinstance(vertices, dict):
        print(" vertex_count", len(vertices))
        for key, value in list(vertices.items())[:3]:
            print("  v", key, value)
    faces = element.get("faces")
    if isinstance(faces, dict):
        print(" face_count", len(faces))
        for key, face in list(faces.items())[:2]:
            print("  face keys", sorted(face.keys()))
            print("  face", {k: face[k] for k in face if k != "uv"})
            print("  uv", face.get("uv"))
    elif isinstance(faces, list):
        print(" faces list", len(faces), faces[:1])
