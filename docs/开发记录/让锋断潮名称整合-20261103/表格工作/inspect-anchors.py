# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")


def odd_refs(xml):
    refs = re.findall(r'sqref="([^"]+)"', xml)
    odd = []
    for ref in refs:
        if re.search(r"(80|1000)$", ref):
            continue
        odd.append(ref)
    return odd


def main():
    with zipfile.ZipFile(LIVE) as book:
        names = book.namelist()
        print("comment-like", [name for name in names if "comment" in name or "vml" in name or "drawing" in name])
        for label, part in (("已完成", "xl/worksheets/sheet1.xml"), ("制作中", "xl/worksheets/sheet3.xml")):
            xml = book.read(part).decode("utf-8")
            print("====", label, "odd sqref", odd_refs(xml))
            merges = re.findall(r'<mergeCell ref="([^"]+)"', xml)
            print("merges", merges)
            drawing = re.search(r"<legacyDrawing[^>]*/>", xml)
            print("legacy", drawing.group(0) if drawing else None)
        # vml anchors
        for name in names:
            if name.endswith(".vml") or "comments" in name:
                text = book.read(name).decode("utf-8", errors="replace")
                print("FILE", name, "len", len(text))
                anchors = re.findall(r"<x:Anchor>([^<]*)</x:Anchor>", text)
                print(" anchors", anchors[:30], "count", len(anchors))
                rows = re.findall(r'\br="([A-Z]+\d+)"', text)
                print(" cell refs sample", rows[:20], "count", len(rows))


if __name__ == "__main__":
    main()
