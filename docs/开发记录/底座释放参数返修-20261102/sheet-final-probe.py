# -*- coding: utf-8 -*-
import importlib.util
import re
import zipfile
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "prepare", Path(r"D:\alianximod\docs\开发记录\底座释放参数返修-20261102\sheet-return-prepare.py")
)
prepare = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prepare)
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
print("hash", prepare.sha256_file(live))
print("lock", Path(r"D:\alianximod\docs\~$术式制作表.xlsx").exists())
with zipfile.ZipFile(live) as archive:
    shared = prepare.shared_list(archive.read("xl/sharedStrings.xml"))
    for title in prepare.STATUS:
        xml = archive.read(prepare.PARTS[title]).decode("utf-8")
        ids = prepare.ids_of(xml, shared)
        hits = [(code, number) for code, number, _row in ids if code in ("fire_shock_ring", "fire_meteorite")]
        print(title, len(ids), hits)
        print("dimension", re.findall(r'<dimension[^/]*/>', xml))
        print("filter", re.findall(r'<autoFilter[^/]*/>', xml))
    workbook = archive.read("xl/workbook.xml").decode("utf-8")
    for match in re.findall(r"<definedName[^>]*>[^<]+</definedName>", workbook):
        if "制作中" in match or "待验收" in match:
            print(match)
