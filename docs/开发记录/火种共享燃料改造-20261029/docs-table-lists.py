# -*- coding: utf-8 -*-
import html
import re
import zipfile
from pathlib import Path

source = Path(r"D:\alianximod\docs\术式制作表.xlsx")
with zipfile.ZipFile(source) as archive:
    workbook = archive.read("xl/workbook.xml").decode("utf-8")
    rels = archive.read("xl/_rels/workbook.xml.rels").decode("utf-8")
    names = re.findall(r'<sheet [^>]*name="([^"]+)"[^>]*r:id="([^"]+)"', workbook)
    rel_map = {}
    for match in re.finditer(r"<Relationship\b([^>]*)/?>", rels):
        attrs = match.group(1)
        rel_map[re.search(r'\bId="([^"]+)"', attrs).group(1)] = re.search(r'\bTarget="([^"]+)"', attrs).group(1)
    for name, rel in names:
        if name not in ("触发时机列表", "操作列表", "计量单位列表", "数据表"):
            continue
        target = rel_map[rel]
        path = target if target.startswith("xl/") or target.startswith("/xl/") else "xl/" + target
        path = path.lstrip("/")
        xml = archive.read(path).decode("utf-8")
        texts = [html.unescape(item) for item in re.findall(r"<(?:[\w]+:)?t\b[^>]*>([\s\S]*?)</(?:[\w]+:)?t>", xml)]
        print("==", name)
        for text in texts[:40]:
            print(text.replace("\n", " | "))
