# -*- coding: utf-8 -*-
import json
from pathlib import Path

roots = [
    Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029"),
    Path(r"D:\alianximod\docs\开发记录\火种共享燃料改造-20261029"),
]
needles = ("fire_fentian", "焚天")


def walk(value, path, hits):
    if isinstance(value, dict):
        if value.get("B") == "fire_fentian" or value.get("id") == "fire_fentian":
            n = value.get("N") or value.get("approvedN") or value.get("beforeN") or value.get("detailN")
            hits.append((path, "direct", len(n) if isinstance(n, str) else None, list(value)[:12]))
        for key, child in value.items():
            if key in ("fire_fentian",) or (isinstance(child, dict) and child.get("id") == "fire_fentian"):
                text = child.get("N") if isinstance(child, dict) else None
                hits.append((path + "/" + str(key), type(child).__name__, len(text) if isinstance(text, str) else None, list(child)[:12] if isinstance(child, dict) else None))
            if isinstance(child, (dict, list)) and path.count("/") < 4:
                walk(child, path + "/" + str(key), hits)
    elif isinstance(value, list) and path.count("/") < 3:
        for index, child in enumerate(value[:30]):
            if isinstance(child, (dict, list)):
                walk(child, path + f"[{index}]", hits)


for root in roots:
    for path in root.glob("*.json"):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        raw = path.read_text(encoding="utf-8")
        if "fire_fentian" not in raw and "焚天" not in raw:
            continue
        hits = []
        walk(data, path.name, hits)
        print(path.name, "hits", len(hits))
        for item in hits[:8]:
            print(" ", item)
