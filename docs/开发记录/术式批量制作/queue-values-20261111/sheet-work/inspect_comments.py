# -*- coding: utf-8 -*-
import re
import zipfile

with zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx") as book:
    text = book.read("xl/comments/comment2.xml").decode("utf-8")
    vml = book.read("xl/drawings/commentsDrawing2.vml").decode("utf-8", errors="replace")
refs = re.findall(r'ref="([A-Z]+\d+)"', text)
print("comment refs", len(refs))
print(" ".join(refs))
rows = sorted({int(re.search(r"\d+", ref).group(0)) for ref in refs})
print("rows", rows)
