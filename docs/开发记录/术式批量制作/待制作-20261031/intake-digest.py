# -*- coding: utf-8 -*-
import json
from pathlib import Path

here = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
data = json.loads((here / "intake-cells.json").read_text(encoding="utf-8"))
headers = data["headers"]["待制作"]["row2"] or data["headers"]["待制作"]["row1"]
row1 = data["headers"]["待制作"]["row1"]


def label(column):
    return row1.get(column) or headers.get(column) or column


lines = []
lines.append("STATUS")
for name, items in data["statusIds"].items():
    lines.append(f"## {name} {len(items)}")
    for item in items:
        lines.append(f"{item['row']}\t{item['id']}\t{item['name']}")
lines.append("ARCHIVE " + str(data["archiveCount"]))
lines.append("COMMENTS")
for comment in data["headerComments"]:
    text = comment.get("text", "").replace("\n", " / ")
    lines.append(f"{comment['sheet']} {comment.get('ref')} {text[:240]}")
lines.append("VALIDATIONS")
for name, rules in data["validations"].items():
    formulas = sorted({(rule.get("type"), rule.get("formula1"), rule.get("sqref")) for rule in rules})
    lines.append(f"## {name} {len(rules)}")
    for item in formulas[:8]:
        lines.append(str(item)[:300])
lines.append("DETAIL HEADER")
lines.append(json.dumps(data["detailHeader"], ensure_ascii=False)[:2000])
lines.append("PENDING")
for row in data["pendingRows"]:
    lines.append(f"\n# {row['id']} row {row['row']}")
    for column, cell in row["cells"].items():
        value = cell.get("v")
        if value is None or value == "":
            continue
        text = str(value).replace("\n", "\\n")
        if len(text) > 500:
            text = text[:500] + "…"
        formula = f" FORMULA={cell['f']}" if cell.get("f") else ""
        lines.append(f"{column} {label(column)} [{cell.get('t')}] {text}{formula}")
detail_by_id = []
for item in data["detailRows"]:
    joined = " ".join(str(cell.get("v")) for cell in item["cells"].values() if cell.get("v"))
    detail_by_id.append((item["row"], joined[:400]))
lines.append("\nDETAIL ROWS " + str(len(data["detailRows"])))
for row, text in detail_by_id:
    lines.append(f"{row} {text}")
(here / "intake-digest.txt").write_text("\n".join(lines), encoding="utf-8")
print("lines", len(lines), "pending", len(data["pendingRows"]), "detail", len(data["detailRows"]))
