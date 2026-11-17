# -*- coding: utf-8 -*-
import json
from pathlib import Path

source = Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029\liaoyuan-visual-transition-snapshot.json")
data = json.loads(source.read_text(encoding="utf-8"))
detail = data.get("currentDetailed") or data.get("approvedDetail") or {}
out = Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031\实施-退回-旧详细.txt")
lines = [
    "keys " + ",".join(data.keys()),
    "detail keys " + ",".join(detail.keys()) if isinstance(detail, dict) else type(detail).__name__,
]
if isinstance(detail, dict):
    for key in ("n", "o", "name"):
        text = detail.get(key, "")
        lines.append(f"\n## {key} len={len(text)}\n{text}")
out.write_text("\n".join(lines), encoding="utf-8")
print(out.stat().st_size)
