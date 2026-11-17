# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

WORK = Path(r"D:\alianximod\docs\开发记录\让锋断潮名称整合-20261103\表格工作")
PREP = json.loads((WORK / "prepare-inplace-commit.json").read_text(encoding="utf-8"))
LIVE = Path(PREP["live"])
SENTENCE = "2026-11-03 用户最新决定：仅改名，原停用计划取消；保留功能及水幻形可用，不迁待验收，保持当前子表。"
PARTS = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}


def local(tag):
    return tag.rsplit("}", 1)[-1]


def sha256_file(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest().upper()


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


def rows_of(xml):
    return re.findall(r"<row\b[^>]*>[\s\S]*?</row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_span(row, column, number):
    matches = list(re.finditer(rf'<c r="{column}{number}"', row))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row.find(">", start)
    if row[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</c>", row[opener_end:])
        if not close:
            raise SystemExit(f"Cell {column}{number} is not closed")
        end = opener_end + close.end()
    return row[start:end]


def cell_text(cell, shared):
    if 't="inlineStr"' in cell or "<is>" in cell:
        return html.unescape("".join(re.findall(r"<t[^>]*>([\s\S]*?)</t>", cell)))
    value = re.search(r"<v>([\s\S]*?)</v>", cell)
    if not value:
        return None
    raw = html.unescape(value.group(1))
    if 't="s"' in cell and raw.isdigit():
        return shared[int(raw)]
    return raw


def read_at(row, column, number, shared):
    found = cell_span(row, column, number)
    return cell_text(found, shared) if found else None


def main():
    live_hash = sha256_file(LIVE)
    if live_hash != PREP["candidateSha256"]:
        raise SystemExit("重开哈希不是候选")
    with zipfile.ZipFile(LIVE) as book:
        shared = shared_list(book.read("xl/sharedStrings.xml"))
        counts = {}
        places = {}
        for title, part in PARTS.items():
            xml = book.read(part).decode("utf-8")
            count = 0
            for row in rows_of(xml):
                number = row_number(row)
                if number < 3:
                    continue
                code = read_at(row, "B", number, shared)
                if not code:
                    continue
                count += 1
                if code in ("tide_sever", "water_yielding_ribbon"):
                    places[code] = {
                        "sheet": title,
                        "row": number,
                        "A": read_at(row, "A", number, shared),
                        "N": read_at(row, "N", number, shared),
                        "O": read_at(row, "O", number, shared),
                        "DV": read_at(row, "DV", number, shared),
                    }
            counts[title] = count
        making = book.read(PARTS["制作中"]).decode("utf-8")
        if 'ref="A2:EN5"' not in making or "<dataValidations" not in making:
            raise SystemExit("制作中筛选或验证丢失")
    if counts != {"已完成": 38, "待制作": 0, "制作中": 15, "待验收": 7}:
        raise SystemExit(f"计数变化 {counts}")
    expected = {
        "tide_sever": ("制作中", 16, "薄水切", "一道薄而长的水切", "TideSeverEffect"),
        "water_yielding_ribbon": ("制作中", 17, "断潮", "先铺开水面，再从中间劈开。水分开没有伤害，只有中间那一刀", "WaterYieldingEffect"),
    }
    for code, spec in expected.items():
        found = places[code]
        if (found["sheet"], found["row"], found["A"], found["N"], found["O"]) != spec:
            raise SystemExit(code + " 重开内容不对")
        if SENTENCE not in found["DV"] or "2026-11-03 用户原话：" not in found["DV"]:
            raise SystemExit(code + " 备注不完整")
    stat = LIVE.stat()
    print(json.dumps({
        "reopenSha256": live_hash,
        "mtime": datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="milliseconds"),
        "size": stat.st_size,
        "counts": counts,
        "tide": places["tide_sever"],
        "ribbon": {key: places["water_yielding_ribbon"][key] for key in ("sheet", "row", "A", "N", "O")},
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
