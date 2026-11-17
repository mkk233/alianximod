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
status = {
    "xl/worksheets/sheet1.xml": "已完成",
    "xl/worksheets/sheet2.xml": "待制作",
    "xl/worksheets/sheet3.xml": "制作中",
    "xl/worksheets/sheet4.xml": "待验收",
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


prefixes = {}
mismatches = []
id_hits = []
targets = ("fire_surface_anchor", "fire_stagger", "fire_hit_continue", "fire_break_ground")
with zipfile.ZipFile(live) as archive:
    names = archive.namelist()
    for name, title in list(status.items()) + [("xl/worksheets/sheet6.xml", "资源状态明细")]:
        xml = archive.read(name).decode("utf-8")
        for row in rows_of(xml):
            number = row_number(row)
            info = cells(row)
            code = info.get("B") if title != "资源状态明细" else info.get("A")
            if title != "资源状态明细" and code and number >= 3:
                prefix = code.split("_", 1)[0]
                prefixes.setdefault(prefix, 0)
                prefixes[prefix] += 1
                element = info.get("D")
                if code.startswith("fire_") and element not in (None, "火"):
                    mismatches.append({"sheet": title, "row": number, "A": info.get("A"), "B": code, "C": info.get("C"), "D": element})
            for column, text in info.items():
                if text and any(token in text for token in targets):
                    id_hits.append({"sheet": title, "row": number, "column": column, "id": code, "count": {token: text.count(token) for token in targets if token in text}})
    sheet5 = archive.read("xl/worksheets/sheet5.xml").decode("utf-8")
    sheet7 = archive.read("xl/worksheets/sheet7.xml").decode("utf-8") if "xl/worksheets/sheet7.xml" in names else ""
report = {
    "sha256": sha256(data),
    "mtime": datetime.fromtimestamp(live.stat().st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "bytes": len(data),
    "prefixes": prefixes,
    "mismatches": mismatches,
    "idHits": id_hits,
    "sheet5Hits": {token: sheet5.count(token) for token in targets},
    "sheet7Hits": {token: sheet7.count(token) for token in targets},
}
Path(r"D:\alianximod\docs\开发记录\灼地产种设计入表-20261030\编号-定位.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8"
)
print(json.dumps({"sha": report["sha256"], "prefixes": prefixes, "mismatches": mismatches, "hits": id_hits, "sheet5": report["sheet5Hits"], "sheet7": report["sheet7Hits"]}, ensure_ascii=False))
