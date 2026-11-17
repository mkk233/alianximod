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
        target = rel_map[rel].lstrip("/")
        if not target.startswith("xl/"):
            target = "xl/" + target
        xml = archive.read(target).decode("utf-8")
        if "首次带资源接触" in xml or "命中成功" in xml:
            print("FOUND", name, target, "len", len(xml))
            texts = re.findall(r"<(?:[\w]+:)?t\b[^>]*>([\s\S]*?)</(?:[\w]+:)?t>", xml)
            values = re.findall(r"<(?:[\w]+:)?v>([\s\S]*?)</(?:[\w]+:)?v>", xml)
            print("t count", len(texts), "v count", len(values))
            joined = "\n".join(html.unescape(item) for item in texts[:80])
            print(joined[:2000])
