import json
from pathlib import Path

model = json.loads(Path(r"D:\alianximod\docs\资源库\特效\水流\水幻形_Steve水线_v2_1\water_phantom_steve_v2_1.bbmodel").read_text(encoding="utf-8"))
groups = {group["uuid"]: group for group in model["groups"]}
elements = {element["uuid"]: element for element in model["elements"]}


def walk(node, depth):
    if isinstance(node, str):
        if node in groups:
            group = groups[node]
            print("  " * depth + f"GROUP {group.get('name')} origin={group.get('origin')} rot={group.get('rotation')}")
            for child in group.get("children") or []:
                walk(child, depth + 1)
            return
        element = elements.get(node)
        if element is None:
            print("  " * depth + f"MISSING {node}")
            return
        print(
            "  " * depth
            + f"CUBE {element.get('name')} from={element.get('from')} to={element.get('to')} "
            + f"origin={element.get('origin')} rot={element.get('rotation')}"
        )
        return
    print("  " * depth + f"NODE {node.get('name')} origin={node.get('origin')} rot={node.get('rotation')}")
    for child in node.get("children") or []:
        walk(child, depth + 1)


for element in model["elements"]:
    name = element.get("name") or ""
    if not name.startswith("steve_"):
        continue
    print(name, "box_uv", element.get("box_uv"), "mirror", element.get("mirror_uv") or element.get("mirror"))
    faces = element.get("faces") or {}
    for face_name, face in faces.items():
        print(" ", face_name, "uv", face.get("uv"), "rot", face.get("rotation"), "tex", face.get("texture"))
