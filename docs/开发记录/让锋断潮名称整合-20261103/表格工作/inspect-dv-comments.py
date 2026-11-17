# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")


def main():
    with zipfile.ZipFile(LIVE) as book:
        print("sheet1 rels")
        print(book.read("xl/worksheets/_rels/sheet1.xml.rels").decode("utf-8"))
        print("--- comments1 head ---")
        text = book.read("xl/comments1.xml").decode("utf-8")
        print(text[:1500])
        done = book.read("xl/worksheets/sheet1.xml").decode("utf-8")
        for match in re.finditer(r"<dataValidation\b[\s\S]*?</dataValidation>", done):
            block = match.group(0)
            if "C36" in block or 'sqref="C36"' in block:
                print("--- C36 validation ---")
                print(block[:800])
        row = re.search(r'<row\b[^>]*\br="7".*?</row>', done).group(0)
        cell = re.search(r'<c r="DV7"[\s\S]*?(?:/>|</c>)', row)
        print("--- DV7 ---")
        print(cell.group(0) if cell else "MISSING")
        cell_a = re.search(r'<c r="A7"[\s\S]*?(?:/>|</c>)', row)
        print("--- A7 ---")
        print(cell_a.group(0) if cell_a else "MISSING")
        # which B is on row 36
        row36 = re.search(r'<row\b[^>]*\br="36".*?</row>', done).group(0)
        b36 = re.search(r'<c r="B36"[\s\S]*?(?:/>|</c>)', row36)
        c36 = re.search(r'<c r="C36"[\s\S]*?(?:/>|</c>)', row36)
        print("--- B36 C36 ---")
        print(b36.group(0) if b36 else "no B")
        print(c36.group(0) if c36 else "no C")


if __name__ == "__main__":
    main()
