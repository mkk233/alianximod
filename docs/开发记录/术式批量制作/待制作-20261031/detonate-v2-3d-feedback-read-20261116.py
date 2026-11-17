# -*- coding: utf-8 -*-
import hashlib
import html
import io
import re
import zipfile
from pathlib import Path

path = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031\候选\detonate-v2-3d-feedback-术式制作表-20261116-155706-1.xlsx")
data = path.read_bytes()
print("BYTES", len(data))
print("SHA", hashlib.sha256(data).hexdigest().upper())
with zipfile.ZipFile(io.BytesIO(data)) as archive:
    names = archive.namelist()
    print("ZIP", len(names), "SHARED", "xl/sharedStrings.xml" in names)
    sheets = {
        "已完成": "xl/worksheets/sheet1.xml",
        "待制作": "xl/worksheets/sheet2.xml",
        "制作中": "xl/worksheets/sheet3.xml",
        "待验收": "xl/worksheets/sheet4.xml",
    }
    seen = []
    for label, name in sheets.items():
        xml = archive.read(name).decode("utf-8")
        rows = re.findall(r"<row\b[^>]*>[\s\S]*?</row>", xml)
        codes = []
        for row in rows:
            if 'r="B' not in row:
                continue
            number = int(re.search(r'\br="(\d+)"', row).group(1))
            if number < 3:
                continue
            match = re.search(rf'<c\b[^>]*\br="B{number}"[^>]*>[\s\S]*?</c>', row)
            if not match:
                continue
            nodes = re.findall(r"<t(?: [^>]*)?>([\s\S]*?)</t>", match.group(0))
            if not nodes:
                continue
            code = html.unescape(nodes[0]).strip()
            if code:
                codes.append((code, number, row))
                seen.append(code)
        print(label, len(codes), ",".join(code for code, _, _ in codes))
        if label == "制作中":
            target = [item for item in codes if item[0] == "fire_detonate"]
            print("TARGET", len(target), target[0][1] if target else None)
            row = target[0][2]
            number = target[0][1]
            for column in ("N", "O", "EK", "DV"):
                cell = re.search(rf'<c\b[^>]*\br="{column}{number}"[^>]*>([\s\S]*?)</c>', row).group(1)
                nodes = re.findall(r"<t(?: [^>]*)?>([\s\S]*?)</t>", cell)
                value = html.unescape(nodes[0])
                digest = hashlib.sha256(value.encode("utf-8")).hexdigest().upper()
                print(column, len(value), digest)
            print("VALIDATIONS", len(re.findall(r"<dataValidation\b", xml)))
            print("MERGES", len(re.findall(r"<mergeCell\b", xml)))
    print("TOTAL", len(seen), "UNIQUE", len(set(seen)))
