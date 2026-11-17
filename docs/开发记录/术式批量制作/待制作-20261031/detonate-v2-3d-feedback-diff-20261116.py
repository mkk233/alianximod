# -*- coding: utf-8 -*-
import difflib
import io
import re
import zipfile
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
left = batch / "备份" / "detonate-v2-3d-feedback-术式制作表-20261116-155706-1.xlsx"
right = batch / "候选" / "detonate-v2-3d-feedback-术式制作表-20261116-155706-1.xlsx"
with zipfile.ZipFile(left) as a, zipfile.ZipFile(right) as b:
    old = a.read("xl/worksheets/sheet3.xml").decode("utf-8")
    new = b.read("xl/worksheets/sheet3.xml").decode("utf-8")
def ids(xml):
    found = []
    for row in re.findall(r"<row\b[^>]*>[\s\S]*?</row>", xml):
        number = int(re.search(r'\br="(\d+)"', row).group(1))
        match = re.search(rf'<c\b[^>]*\br="B{number}"[^>]*>[\s\S]*?</c>', row)
        if not match:
            found.append((number, "NOCELL"))
            continue
        nodes = re.findall(r"<t(?: [^>]*)?>([\s\S]*?)</t>", match.group(0))
        if nodes:
            found.append((number, html.unescape(nodes[0]).strip()))
        else:
            found.append((number, "NOTEXT"))
    return found

import html
for label, xml in (("OLD", old), ("NEW", new)):
    index = xml.find('r="29"')
    print(label, "INDEX", index)
    if index >= 0:
        print(xml[max(0, index - 300):index + 700])
old_rows = re.findall(r"<row\b[^>]*>[\s\S]*?</row>", old)
new_rows = re.findall(r"<row\b[^>]*>[\s\S]*?</row>", new)
print("ROWS", len(old_rows), len(new_rows), "XML", len(old), len(new))
for index, (before, after) in enumerate(zip(old_rows, new_rows), start=1):
    if before != after:
        number = re.search(r'\br="(\d+)"', before).group(1)
        print("CHANGED_ROW_INDEX", index, "NUMBER", number, "DELTA", len(after) - len(before))
if len(old_rows) != len(new_rows):
    print("ROW_COUNT_DIFFERS")
