# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029")
summary = json.loads((batch / "liaoyuan-visual-return-preserve-summary.json").read_text(encoding="utf-8"))
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
live_sha = hashlib.sha256(live.read_bytes()).hexdigest().upper()
if live_sha != summary["candidateSha256"]:
    raise SystemExit(live_sha)

def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)

def text_of(row_xml, column, number):
    match = re.search(rf'<(?:[\w]+:)?c r="{column}{number}"[^>]*>([\s\S]*?)</(?:[\w]+:)?c>', row_xml)
    if not match:
        return None
    inner = match.group(1)
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", inner)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<(?:[\w]+:)?v>([\s\S]*)</(?:[\w]+:)?v>", inner)
    return html.unescape(value.group(1)) if value else None

def ids_of(xml):
    found = []
    for row in rows_of(xml):
        number = int(re.search(r'\br="(\d+)"', row).group(1))
        if number < 3:
            continue
        code = text_of(row, "B", number)
        if code:
            found.append(code)
    return found

with zipfile.ZipFile(summary["backup"]) as old, zipfile.ZipFile(live) as new:
    different = [name for name in old.namelist() if old.read(name) != new.read(name)]
    counts = {}
    all_ids = []
    parts = {
        "已完成": "xl/worksheets/sheet1.xml",
        "待制作": "xl/worksheets/sheet2.xml",
        "制作中": "xl/worksheets/sheet3.xml",
        "待验收": "xl/worksheets/sheet4.xml",
    }
    for title, name in parts.items():
        codes = ids_of(new.read(name).decode("utf-8"))
        counts[title] = len(codes)
        all_ids.extend(codes)
    making = {int(re.search(r'\br="(\d+)"', row).group(1)): row for row in rows_of(new.read(parts["制作中"]).decode("utf-8"))}
    pending = {int(re.search(r'\br="(\d+)"', row).group(1)): row for row in rows_of(new.read(parts["待验收"]).decode("utf-8"))}
    feature = text_of(making[6], "N", 6)
    print(json.dumps({
        "sha256": live_sha,
        "different": different,
        "counts": counts,
        "unique": len(all_ids) == 46 and len(set(all_ids)) == 46,
        "making": [text_of(making[n], "B", n) for n in (3, 4, 5, 6)],
        "pending": [text_of(pending[n], "B", n) for n in (3, 4)],
        "sharedFuel": "不预扣全部余额" in feature and "一次消耗全部" not in feature,
        "returned": "退回制作中" in text_of(making[6], "DV", 6),
    }, ensure_ascii=False))
