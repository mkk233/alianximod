# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
data = live.read_bytes()
stat = live.stat()
archive = zipfile.ZipFile(live)
xml = archive.read("xl/worksheets/sheet3.xml").decode("utf-8")
rows = re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)
row = next(item for item in rows if re.search(r'\br="25"', item))
meta = json.loads(Path(r"D:\alianximod\docs\开发记录\燎原本体伤害-20261112\candidate-meta.json").read_text(encoding="utf-8"))


def text(column):
    cell = re.search(rf'<c r="{column}25"[\s\S]*?</c>', row).group(0)
    texts = re.findall(r"<t(?: [^>]*)?>([\s\S]*?)</t>", cell)
    if texts:
        return html.unescape("".join(texts))
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", cell)
    return html.unescape(value.group(1)) if value else ""


n = text("N")
o = text("O")
dv = text("DV")
print("live", hashlib.sha256(data).hexdigest().upper(), len(data),
      datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="milliseconds"))
print("N equal", n == meta["nAfter"], len(n))
print("O equal", o == meta["oAfter"], len(o))
print("DV prefix", dv.startswith("用户原话：“这样的设计好"))
print("DV addition", dv.endswith(meta["dvAddition"]))
print("body4", "本体基础火伤4点" in n)
print("burn formula", "4×(1+0.1n+F)" in n)
print("period", "周期20tick" in n)
print("life", "200tick" in n)
print("radius", "半径10" in n)
print("other skill", "焚天" in n)
print("old body absent", "本体基础火伤8点" not in n)
print("pending absent", b"fire_liaoyuan" not in archive.read("xl/worksheets/sheet4.xml"))
print("resource absent", b"fire_liaoyuan" not in archive.read("xl/worksheets/sheet6.xml"))
