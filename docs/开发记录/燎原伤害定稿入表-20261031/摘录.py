# -*- coding: utf-8 -*-
import json
from pathlib import Path

report = json.loads(Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031\定位.json").read_text(encoding="utf-8"))
cells = report["located"][0]["cells"]
lines = []
for column, text in cells.items():
    if column in ("N", "O", "DV", "EJ", "EK", "EM"):
        continue
    lines.append(f"{column}={text}")
for column in ("N", "O", "EJ", "EK", "EM"):
    lines.append(f"\n## {column}\n{cells.get(column, '')}")
text = cells.get("DV", "")
lines.append("\n## DV head\n" + text[:500])
lines.append("\n## DV tail\n" + text[-800:])
Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031\摘录.txt").write_text("\n".join(lines), encoding="utf-8")
print("cells", len(cells), "dv", len(text))
