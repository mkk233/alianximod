# -*- coding: utf-8 -*-
import hashlib
import html
import json
import os
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
task = root / "docs" / "开发记录" / "燎原本体伤害-20261112"
live = root / "docs" / "术式制作表.xlsx"
expected = "0C24BFA4EC405190964E9A3938FE21866112A694E8CE730B492B986B4C8BBF57"
old = "没有待确认项。源码和编译尚未开始，不写编译通过，不迁待验收或已完成。"
new = "没有待确认项。本写回不记录编译结论，不写编译通过，不迁待验收或已完成。"


def fail(message):
    raise SystemExit(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()


live_bytes = live.read_bytes()
if sha256(live_bytes) != expected:
    fail("Live hash changed; note correction stopped")
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = task / "备份" / f"术式制作表-{stamp}-2.xlsx"
if backup.exists():
    fail("Backup already exists")
backup.write_bytes(live_bytes)

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}
name = "xl/worksheets/sheet3.xml"
sheet = parts[name].decode("utf-8")
escaped_old = html.escape(old, quote=False)
escaped_new = html.escape(new, quote=False)
if sheet.count(escaped_old) != 1:
    fail(f"Target sentence count is {sheet.count(escaped_old)}")
parts[name] = sheet.replace(escaped_old, escaped_new, 1).encode("utf-8")
candidate = task / "术式制作表-备注修正候选.xlsx"
with zipfile.ZipFile(candidate, "w") as archive:
    for info in order:
        archive.writestr(info, parts[info.filename])

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate) as dst:
    if src.namelist() != dst.namelist():
        fail("Zip part list changed")
    different = [item for item in src.namelist() if src.read(item) != dst.read(item)]
    if different != [name]:
        fail(f"Unexpected changes: {different}")
    xml = dst.read(name).decode("utf-8")
    rows = re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)
    row = next(item for item in rows if re.search(r'\br="25"', item))
    cell = re.search(r'<c r="DV25"[\s\S]*?</c>', row).group(0)
    text = html.unescape("".join(re.findall(r"<t(?: [^>]*)?>([\s\S]*?)</t>", cell)))
    if old in text or new not in text or not text.startswith("用户原话：“这样的设计好"):
        fail("Corrected note is not intact")
    number = re.search(r'<c r="E25"[\s\S]*?</c>', row).group(0)
    if "<v>4</v>" not in number:
        fail("E changed")

if sha256(live.read_bytes()) != expected:
    fail("Live changed before replacement")
os.replace(candidate, live)
after = live.read_bytes()
result = {
    "beforeSha256": expected,
    "afterSha256": sha256(after),
    "bytes": len(after),
    "mtime": datetime.fromtimestamp(live.stat().st_mtime).isoformat(timespec="milliseconds"),
    "backup": str(backup),
    "old": old,
    "new": new,
}
(task / "note-correction.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(result, ensure_ascii=False))
