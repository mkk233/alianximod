# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\火系基础术式入待制作-20261029")
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
summary = json.loads((batch / "入表-summary.json").read_text(encoding="utf-8"))
backup = Path(summary["backup"])
specs = json.loads((batch / "入表-rows.json").read_text(encoding="utf-8"))

def sha256(blob):
    return hashlib.sha256(blob).hexdigest().upper()

def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)

def text_of(source, column, number):
    match = re.search(rf'<c r="{column}{number}"[^>]*>([\s\S]*?)</c>', source)
    if not match:
        return None
    inner = match.group(1)
    inline = re.search(r"<t(?: [^>]*)?>([\s\S]*)</t>", inner)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", inner)
    return html.unescape(value.group(1)) if value else None

live_bytes = live.read_bytes()
report = {"liveSha256": sha256(live_bytes), "bytes": len(live_bytes), "backupSha256": sha256(backup.read_bytes())}
with zipfile.ZipFile(live) as now, zipfile.ZipFile(backup) as prev:
    report["partsEqual"] = now.namelist() == prev.namelist()
    report["differentParts"] = [name for name in prev.namelist() if prev.read(name) != now.read(name)]
    pending = now.read("xl/worksheets/sheet2.xml").decode("utf-8")
    report["dimension"] = re.search(r'ref="([^"]+)"', pending).group(1)
    report["rows"] = []
    for index, spec in enumerate(specs):
        number = index + 3
        report["rows"].append({
            "row": number,
            "name": text_of(pending, "A", number),
            "id": text_of(pending, "B", number),
            "n": text_of(pending, "N", number) == spec["n"],
            "o": bool(text_of(pending, "O", number)),
            "dv": bool(text_of(pending, "DV", number)),
        })
    ids = []
    counts = {}
    for title, part in (
        ("已完成", "xl/worksheets/sheet1.xml"),
        ("待制作", "xl/worksheets/sheet2.xml"),
        ("制作中", "xl/worksheets/sheet3.xml"),
        ("待验收", "xl/worksheets/sheet4.xml"),
    ):
        xml = now.read(part).decode("utf-8")
        page = []
        for row in rows_of(xml):
            number = int(re.search(r'\br="(\d+)"', row).group(1))
            if number >= 3:
                code = text_of(row, "B", number)
                if code:
                    page.append(code)
        counts[title] = len(page)
        ids.extend(page)
    report["counts"] = counts
    report["unique"] = len(set(ids))
    report["duplicates"] = sorted(code for code in set(ids) if ids.count(code) > 1)
    uncovered = []
    covered = []
    for validation in re.findall(r'<dataValidation\b[^>]*sqref="([^"]+)"', pending):
        covered.append(validation)
        for ref in validation.split():
            bounds = re.match(r"([A-Z]+)(\d+):([A-Z]+)(\d+)$", ref)
            if bounds and int(bounds.group(2)) <= 15 and int(bounds.group(4)) < 15:
                uncovered.append(ref)
    report["validationCount"] = len(covered)
    report["uncovered"] = uncovered
    report["kindlePeriod"] = text_of(pending, "EG", 12)
    report["detonateAmount"] = text_of(pending, "EC", 13)
    report["gloomAmount"] = text_of(pending, "EC", 14)
    report["seedAmount"] = text_of(pending, "EC", 11)
    report["names"] = [text_of(pending, "A", index + 3) for index in range(13)]
print(json.dumps(report, ensure_ascii=False, indent=2))
