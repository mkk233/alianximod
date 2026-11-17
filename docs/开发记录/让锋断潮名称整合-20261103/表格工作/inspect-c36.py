# -*- coding: utf-8 -*-
import html
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")


def local(tag):
    return tag.rsplit("}", 1)[-1]


def shared_list(xml):
    root = ET.fromstring(xml)
    found = []
    for item in root:
        if local(item.tag) != "si":
            continue
        texts = []
        for node in item.iter():
            if local(node.tag) == "t" and node.text:
                texts.append(html.unescape(node.text))
        found.append("".join(texts))
    return found


def main():
    with zipfile.ZipFile(LIVE) as book:
        shared = shared_list(book.read("xl/sharedStrings.xml"))
        done = book.read("xl/worksheets/sheet1.xml").decode("utf-8")
        index = done.find('sqref="C36"')
        print(done[max(0, index - 500): index + 80])
        print("B36", shared[327])
        print("C36", shared[296])
        print("calcChain", "xl/calcChain.xml" in book.namelist())


if __name__ == "__main__":
    main()
