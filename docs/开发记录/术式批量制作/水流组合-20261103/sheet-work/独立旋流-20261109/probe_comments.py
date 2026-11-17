# -*- coding: utf-8 -*-
import re
import zipfile
z = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx")
for name in z.namelist():
    if "comment" in name.lower() or name.endswith(".vml"):
        text = z.read(name).decode("utf-8", "replace")
        hits = re.findall(r'ref="[A-Z]+18"', text)
        print(name, hits[:8], "count", len(hits))
