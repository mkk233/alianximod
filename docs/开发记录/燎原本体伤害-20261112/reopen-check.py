# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path

candidate = Path(r"D:\alianximod\docs\术式制作表.xlsx")
meta = json.loads(Path(r"D:\alianximod\docs\开发记录\燎原本体伤害-20261112\candidate-meta.json").read_text(encoding="utf-8"))
z = zipfile.ZipFile(candidate)


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_text(row, column, number):
    match = re.search(rf'<c r="{column}{number}"[\s\S]*?</c>', row)
    if not match:
        return None
    cell = match.group(0)
    texts = re.findall(r"<t(?: [^>]*)?>([\s\S]*?)</t>", cell)
    if texts:
        return html.unescape("".join(texts))
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", cell)
    return html.unescape(value.group(1)) if value else ""


def ids(xml):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        code = cell_text(row, "B", number)
        if number >= 3 and code and code.strip():
            found.append((code.strip(), number, cell_text(row, "A", number)))
    return found


parts = {"已完成": "sheet1", "待制作": "sheet2", "制作中": "sheet3", "待验收": "sheet4"}
counts = {}
located = {}
for title, sheet in parts.items():
    xml = z.read(f"xl/worksheets/{sheet}.xml").decode("utf-8")
    print(title, "dimension", re.search(r'<dimension[^>]*ref="([^"]+)"', xml).group(1),
          "rows", [row_number(row) for row in rows_of(xml)])
    for code, number, name in ids(xml):
        counts[title] = counts.get(title, 0) + 1
        located.setdefault(code, []).append((title, number, name))

print("counts", counts)
print("unique", len(located), "total", sum(counts.values()))
print("target", located.get("fire_liaoyuan"))
making = z.read("xl/worksheets/sheet3.xml").decode("utf-8")
row = next(item for item in rows_of(making) if row_number(item) == 25)
print("E", cell_text(row, "E", 25))
print("N head", cell_text(row, "N", 25)[:80])
print("N has 4", "本体基础火伤4点" in cell_text(row, "N", 25))
print("N has old 8", "本体基础火伤8点" in cell_text(row, "N", 25))
print("O head", cell_text(row, "O", 25)[:40])
print("DV ends", cell_text(row, "DV", 25).endswith(meta["dvAddition"]))
print("DV starts", cell_text(row, "DV", 25).startswith("用户原话：“这样的设计好"))
print("formula errors", len(re.findall(r"#REF!|#DIV/0!|#VALUE!|#NAME\?|#N/A|#NUM!|#NULL!", making)))
print("candidate hash", hashlib.sha256(candidate.read_bytes()).hexdigest().upper())
