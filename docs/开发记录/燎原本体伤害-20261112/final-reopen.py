# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
data = live.read_bytes()
stat = live.stat()
archive = zipfile.ZipFile(live)
parts = {"已完成": "sheet1", "待制作": "sheet2", "制作中": "sheet3", "待验收": "sheet4"}


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def text_of(row, column, number):
    match = re.search(rf'<c r="{column}{number}"[\s\S]*?</c>', row)
    if not match:
        return None
    cell = match.group(0)
    texts = re.findall(r"<t(?: [^>]*)?>([\s\S]*?)</t>", cell)
    if texts:
        return html.unescape("".join(texts))
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", cell)
    return html.unescape(value.group(1)) if value else ""


counts = {}
located = {}
for title, sheet in parts.items():
    xml = archive.read(f"xl/worksheets/{sheet}.xml").decode("utf-8")
    dimension = re.search(r'ref="([^"]+)"', xml).group(1)
    numbers = [row_number(row) for row in rows_of(xml)]
    print(title, dimension, numbers)
    for row in rows_of(xml):
        number = row_number(row)
        code = text_of(row, "B", number)
        if number >= 3 and code and code.strip():
            counts[title] = counts.get(title, 0) + 1
            located.setdefault(code.strip(), []).append((title, number))

meta = json.loads(Path(r"D:\alianximod\docs\开发记录\燎原本体伤害-20261112\candidate-meta.json").read_text(encoding="utf-8"))
row = next(item for item in rows_of(archive.read("xl/worksheets/sheet3.xml").decode("utf-8")) if row_number(item) == 25)
n = text_of(row, "N", 25)
o = text_of(row, "O", 25)
dv = text_of(row, "DV", 25)
result = {
    "sha256": hashlib.sha256(data).hexdigest().upper(),
    "mtime": datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="milliseconds"),
    "bytes": len(data),
    "counts": counts,
    "unique": len(located),
    "total": sum(counts.values()),
    "target": located.get("fire_liaoyuan"),
    "E": text_of(row, "E", 25),
    "NEqual": n == meta["nAfter"],
    "OEqual": o == meta["oAfter"],
    "oldSentenceAbsent": "源码和编译尚未开始" not in dv,
    "newSentencePresent": "本写回不记录编译结论" in dv,
    "prefix": dv.startswith("用户原话：“这样的设计好"),
    "marker": dv.endswith("制作中 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md"),
    "pendingAbsent": b"fire_liaoyuan" not in archive.read("xl/worksheets/sheet4.xml"),
    "resourceAbsent": b"fire_liaoyuan" not in archive.read("xl/worksheets/sheet6.xml"),
    "body4": "本体基础火伤4点" in n,
    "oldBodyAbsent": "本体基础火伤8点" not in n,
    "burnFormula": "4×(1+0.1n+F)" in n,
    "period": "周期20tick" in n,
    "life": "200tick" in n,
    "radius": "半径10" in n,
    "otherSkill": "焚天" in n,
}
Path(r"D:\alianximod\docs\开发记录\燎原本体伤害-20261112\final-reopen.json").write_text(
    json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(result, ensure_ascii=False, indent=2))
