# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")


def show(label, xml):
    print("====", label)
    dim = re.search(r"<dimension[^>]*/>", xml)
    filt = re.search(r"<autoFilter[^>]*/>", xml)
    dv = re.search(r"<dataValidations[^>]*>", xml)
    print("DIM", dim.group(0) if dim else None)
    print("FILTER", filt.group(0) if filt else None)
    print("DVTAG", dv.group(0) if dv else None)
    for pat in ("conditionalFormatting", "mergeCell", "tableParts", "hyperlink", "legacyDrawing"):
        print(pat, xml.count(pat))
    rows = re.findall(r'<row[^>]*\br="(\d+)"', xml)
    print("rows", rows[:3], "...", rows[-3:], "count", len(rows))
    # sample DV cell existence on target rows
    for number in (7, 9, 3):
        row = re.search(rf'<row\b[^>]*\br="{number}".*?</row>', xml)
        if not row:
            print("missing row", number)
            continue
        text = row.group(0)
        has_dv = f'r="DV{number}"' in text
        style = re.search(r'\bs="(\d+)"', text)
        spans = re.search(r'\bspans="([^"]+)"', text)
        print(f"row {number} len={len(text)} hasDV={has_dv} firstStyle={style.group(1) if style else None} spans={spans.group(1) if spans else None}")


def main():
    with zipfile.ZipFile(LIVE) as book:
        wb = book.read("xl/workbook.xml").decode("utf-8")
        print("--- workbook refs ---")
        for match in re.finditer(r"<definedName\b([^>]*)>([\s\S]*?)</definedName>", wb):
            print(match.group(1).strip(), "=>", match.group(2).strip())
        show("已完成", book.read("xl/worksheets/sheet1.xml").decode("utf-8"))
        show("制作中", book.read("xl/worksheets/sheet3.xml").decode("utf-8"))
        # validation sqref sample
        done = book.read("xl/worksheets/sheet1.xml").decode("utf-8")
        refs = sorted(set(re.findall(r'sqref="([^"]+)"', done)))
        print("done sqref count", len(refs))
        print("done sqref sample", refs[:12])
        making = book.read("xl/worksheets/sheet3.xml").decode("utf-8")
        refs2 = sorted(set(re.findall(r'sqref="([^"]+)"', making)))
        print("making sqref count", len(refs2))
        print("making sqref sample", refs2[:12])


if __name__ == "__main__":
    main()
