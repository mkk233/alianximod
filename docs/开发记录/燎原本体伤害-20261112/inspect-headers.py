# -*- coding: utf-8 -*-
import html
import re
import zipfile

z = zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx")
xml = z.read("xl/worksheets/sheet4.xml").decode("utf-8")
rows = re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cells(row):
    found = []
    pattern = re.compile(r"<(?:[\w]+:)?c\b[^>]*>[\s\S]*?</(?:[\w]+:)?c>|<(?:[\w]+:)?c\b[^>]*/>")
    for match in pattern.finditer(row):
        cell = match.group(0)
        ref = re.search(r'\br="([A-Z]+)(\d+)"', cell)
        texts = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", cell)
        if texts:
            text = html.unescape("".join(texts))
        else:
            value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
            text = html.unescape(value.group(1)) if value else ""
        found.append((ref.group(1), text))
    return found


for row in rows:
    number = row_number(row)
    if number not in (1, 2):
        continue
    print("ROW", number, "cells", len(cells(row)))
    for ref, text in cells(row):
        if text:
            print(f"{ref}\t{text}")

print("---TAIL---")
for tag in ["mergeCell", "autoFilter", "conditionalFormatting", "hyperlink", "legacyDrawing"]:
    print(tag, xml.count(tag))
print("dimension", re.search(r"<dimension[^/]*/>", xml).group(0))

comments = z.read("xl/comments/comment4.xml").decode("utf-8")
blocks = re.findall(r'<comment\b([^>]*)>([\s\S]*?)</comment>', comments)
print("COMMENT COUNT", len(blocks))
for attrs, body in blocks:
    ref = re.search(r'ref="([^"]+)"', attrs).group(1)
    text = html.unescape("".join(re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", body)))
    print(ref, text[:400].replace("\n", " / "))
