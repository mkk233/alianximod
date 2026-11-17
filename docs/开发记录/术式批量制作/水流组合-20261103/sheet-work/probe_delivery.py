# -*- coding: utf-8 -*-
import hashlib
import zipfile
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
backup = Path(r"D:\alianximod\docs\开发记录\术式批量制作\水流组合-20261103\sheet-work\备份\术式制作表-待验收-20261103-223840-1.xlsx")
payload = live.read_bytes()
print("LIVE", hashlib.sha256(payload).hexdigest().upper(), live.stat().st_size)
print("BACKUP", hashlib.sha256(backup.read_bytes()).hexdigest().upper())
with zipfile.ZipFile(live) as now, zipfile.ZipFile(backup) as old:
    names = now.namelist()
    changed = [name for name in names if now.read(name) != old.read(name)]
print("CHANGED", changed)
text = zipfile.ZipFile(live).read("xl/worksheets/sheet4.xml").decode("utf-8")
for token in (
    "干地水平牵引",
    "真实水里的上升水柱",
    "低矮的重压领域。范围内暂时积起不流动的水",
    "water_flow_v3_1.png",
    "water_bubble_v3_1.png",
    "deep_pressure_bands.png",
    "采用这版继续接入",
    "本阶段额度已用完，返修与新接入结果未追加复审；最终compileJava通过，游戏待用户实测。",
    "待验收 | 水流组合-20261103 |",
):
    print(token, text.count(token) if token in text else "SHARED-OR-ABSENT")
archive = zipfile.ZipFile(live).read("xl/worksheets/sheet7.xml")
print("ARCHIVE_SAME", archive == zipfile.ZipFile(backup).read("xl/worksheets/sheet7.xml"))
print("DETAIL_SAME", zipfile.ZipFile(live).read("xl/worksheets/sheet6.xml") == zipfile.ZipFile(backup).read("xl/worksheets/sheet6.xml"))
print("DATA_SAME", zipfile.ZipFile(live).read("xl/worksheets/sheet5.xml") == zipfile.ZipFile(backup).read("xl/worksheets/sheet5.xml"))
making = zipfile.ZipFile(live).read("xl/worksheets/sheet3.xml").decode("utf-8")
print("TARGETS_GONE", "water_vortex" not in making and "water_bubble_column" not in making)
print("RENAME_INLINE", "薄水切" in making and "断潮" in making)
