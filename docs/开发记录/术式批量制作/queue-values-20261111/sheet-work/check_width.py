# -*- coding: utf-8 -*-
import zipfile
path = r"D:\alianximod\docs\开发记录\术式批量制作\queue-values-20261111\sheet-work\候选\术式制作表-queue-values-20261111.xlsx"
with zipfile.ZipFile(path) as book:
    text = book.read("xl/worksheets/sheet5.xml").decode("utf-8")
print("width32", text.count('width="32"'))
print("old6", '<col width="24" customWidth="1" style="19" min="6" max="6"/>' in text)
print("new6", '<col width="32" customWidth="1" style="19" min="6" max="6"/>' in text)
