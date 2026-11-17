# -*- coding: utf-8 -*-
"""List non-cell structures that a row move could disturb."""
import re
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
with zipfile.ZipFile(live) as archive:
    names = {
        "pending": archive.read("xl/worksheets/sheet2.xml").decode("utf-8"),
        "making": archive.read("xl/worksheets/sheet3.xml").decode("utf-8"),
        "archive": archive.read("xl/worksheets/sheet7.xml").decode("utf-8"),
        "detail": archive.read("xl/worksheets/sheet6.xml").decode("utf-8"),
    }
    rels = archive.read("xl/worksheets/_rels/sheet2.xml.rels").decode("utf-8") if "xl/worksheets/_rels/sheet2.xml.rels" in archive.namelist() else ""

for label, xml in names.items():
    tags = sorted(set(re.findall(r"<([\w:]+)", xml)))
    merges = re.findall(r"<mergeCell[^/]*/>", xml)
    cfs = re.findall(r'sqref="([^"]+)"', xml)
    filters = "autoFilter" in xml
    tables = "tableParts" in xml
    drawing = "drawing" in xml
    legacy = "legacyDrawing" in xml
    print(label, "tags", ",".join(tags))
    print(label, "merges", len(merges), "cf-sqref", len(cfs), "filter", filters, "tables", tables, "drawing", drawing, "legacy", legacy)
    if merges:
        print(label, "merge sample", merges[:8])
print("sheet2 rels", rels[:1500])
