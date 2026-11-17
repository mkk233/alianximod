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
parts_wanted = {
    "xl/worksheets/sheet1.xml": "已完成",
    "xl/worksheets/sheet2.xml": "待制作",
    "xl/worksheets/sheet3.xml": "制作中",
    "xl/worksheets/sheet4.xml": "待验收",
    "xl/worksheets/sheet6.xml": "资源状态明细",
}


def sha256(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cells(row):
    found = {}
    for match in re.finditer(r'<(?:[\w]+:)?c r="([A-Z]+)(\d+)"([^>]*?)(?:/>|>([\s\S]*?)</(?:[\w]+:)?c>)', row):
        inner = match.group(4)
        text = None
        if inner:
            inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", inner)
            value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", inner)
            if inline:
                text = html.unescape(inline.group(1))
            elif value:
                text = html.unescape(value.group(1))
        found[match.group(1)] = text
    return found


hits = []
anchors = []
with zipfile.ZipFile(live) as archive:
    names = archive.namelist()
    for name, title in parts_wanted.items():
        xml = archive.read(name).decode("utf-8")
        for row in rows_of(xml):
            number = row_number(row)
            info = cells(row)
            code = info.get("B") or info.get("A")
            if code in ("fire_shock_ring", "fire_surface_anchor") or info.get("A") in ("震环", "地表定位", "热波"):
                anchors.append({"sheet": title, "row": number, "A": info.get("A"), "B": info.get("B"), "C": info.get("C"), "D": info.get("D"), "E": info.get("E"), "F": info.get("F")})
            for column, text in info.items():
                if text and "震环" in text:
                    hits.append({"sheet": title, "row": number, "column": column, "id": info.get("B") or info.get("A"), "text": text})
report = {
    "sha256": sha256(data),
    "mtime": datetime.fromtimestamp(live.stat().st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": len(data),
    "anchors": anchors,
    "hits": hits,
    "commentParts": [name for name in names if "comment" in name.lower()],
}
out = Path(r"D:\alianximod\docs\开发记录\灼地产种设计入表-20261030\热波-定位.json")
out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"sha": report["sha256"], "anchors": anchors, "hitCount": len(hits), "places": [f"{item['sheet']}:{item['column']}{item['row']}:{item['id']}" for item in hits]}, ensure_ascii=False))
