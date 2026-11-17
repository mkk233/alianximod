# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
with zipfile.ZipFile(live) as archive:
    pending = archive.read("xl/worksheets/sheet2.xml").decode("utf-8")
    making = archive.read("xl/worksheets/sheet3.xml").decode("utf-8")
    stored = archive.read("xl/worksheets/sheet7.xml").decode("utf-8")

def show(label, xml, pattern):
    found = re.findall(pattern, xml)
    print(label, found[:12])

show("pending filter", pending, r"<autoFilter[^>]*>")
show("making filter", making, r"<autoFilter[^>]*>")
show("pending dim", pending, r"<dimension[^>]*>")
show("making dim", making, r"<dimension[^>]*>")
show("archive dim", stored, r"<dimension[^>]*>")
show("pending selection", pending, r"<selection[^>]*>")
show("making selection", making, r"<selection[^>]*>")
show("archive selection", stored, r"<selection[^>]*>")
rows = re.findall(r"<row\b[^>]*>", pending)
print("pending row openers", rows)
rows3 = re.findall(r"<row\b[^>]*>", making)
print("making row openers", rows3)
rows7 = re.findall(r"<row\b[^>]*>", stored)
print("archive row openers", rows7)
# style of EP8
ep = re.search(r'<c r="EP8"[^>]*>[\s\S]*?</c>', stored)
print("EP8", ep.group(0)[:400] if ep else None)
ev = re.search(r'<c r="EV8"[^>]*>[\s\S]*?</c>', stored)
print("EV8 head", ev.group(0)[:180] if ev else None)
dv = re.search(r'<c r="DV4"[^>]*>', pending)
print("DV4 opener", dv.group(0) if dv else None)
