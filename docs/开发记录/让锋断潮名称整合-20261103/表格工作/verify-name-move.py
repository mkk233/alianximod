# -*- coding: utf-8 -*-
"""Reopen the live workbook and check the name-alignment move."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

WORK = Path(r"D:\alianximod\docs\开发记录\让锋断潮名称整合-20261103\表格工作")
PREP = json.loads((WORK / "prepare-preview.json").read_text(encoding="utf-8"))
LIVE = Path(PREP["live"])
BACKUP = Path(PREP["backup"])
STATUS = ("已完成", "待制作", "制作中", "待验收")
PARTS = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
    "数据表": "xl/worksheets/sheet5.xml",
    "资源状态明细": "xl/worksheets/sheet6.xml",
    "待制作原始数据": "xl/worksheets/sheet7.xml",
}


def fail(message):
    raise SystemExit(message)


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
            fail(f"Cell {column}{number} is not closed")
        end = opener_end + close.end()
    return row[start:end], start, end


def cell_text(cell, shared):
    if cell is None:
        return None
    if 't="inlineStr"' in cell or "<is>" in cell:
        texts = re.findall(r"<t[^>]*>([\s\S]*?)</t>", cell)
        return html.unescape("".join(texts))
    value = re.search(r"<v>([\s\S]*?)</v>", cell)
    if not value:
        return None
    raw = html.unescape(value.group(1))
    if 't="s"' in cell and raw.isdigit():
        return shared[int(raw)]
    return raw


def read_at(row, column, number, shared):
    found = cell_span(row, column, number)
    return cell_text(found[0], shared) if found else None


def ids_of(xml, shared):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        code = read_at(row, "B", number, shared)
        if code:
            found.append((code, number))
    return found


def main():
    live_hash = sha256_file(LIVE)
    if live_hash != PREP["candidateSha256"]:
        fail("重开哈希不是候选")
    if sha256_file(BACKUP) != PREP["baselineSha256"]:
        fail("备份不是读取基线")
    with zipfile.ZipFile(LIVE) as live, zipfile.ZipFile(BACKUP) as backup:
        shared = shared_list(live.read("xl/sharedStrings.xml"))
        if live.read("xl/sharedStrings.xml") != backup.read("xl/sharedStrings.xml"):
            fail("共享字符串被改")
        for title in ("待制作", "待验收", "数据表", "资源状态明细", "待制作原始数据"):
            if live.read(PARTS[title]) != backup.read(PARTS[title]):
                fail(f"{title} 与备份不同")
        counts = {}
        places = {}
        seen = []
        for title in STATUS:
            page = ids_of(live.read(PARTS[title]).decode("utf-8"), shared)
            counts[title] = len(page)
            for code, number in page:
                seen.append(code)
                places.setdefault(code, []).append({"sheet": title, "row": number})
        if counts != PREP["countsAfter"] or len(seen) != 60 or len(seen) != len(set(seen)):
            fail(f"重开计数或唯一性失败 {counts}")
        making = live.read(PARTS["制作中"]).decode("utf-8")
        located = {}
        for code, number in ids_of(making, shared):
            located[code] = number
        if located.get("tide_sever") != 16 or located.get("water_yielding_ribbon") != 17:
            fail(f"目标行不对 {located.get('tide_sever')} {located.get('water_yielding_ribbon')}")
        if "tide_sever" in dict(ids_of(live.read(PARTS["已完成"]).decode("utf-8"), shared)) or "water_yielding_ribbon" in dict(ids_of(live.read(PARTS["已完成"]).decode("utf-8"), shared)):
            fail("已完成仍能找到这两项")
        expected = {
            "tide_sever": ("断潮", "一道薄而长的水切", "TideSeverEffect"),
            "water_yielding_ribbon": ("让锋", "先铺开水面，再从中间劈开。水分开没有伤害，只有中间那一刀", "WaterYieldingEffect"),
        }
        for row in rows_of(making):
            number = row_number(row)
            code = read_at(row, "B", number, shared)
            if code not in expected:
                continue
            name, function_text, visual = expected[code]
            if read_at(row, "A", number, shared) != name:
                fail(code + " 名称变化")
            if read_at(row, "N", number, shared) != function_text:
                fail(code + " 功能变化")
            if read_at(row, "O", number, shared) != visual:
                fail(code + " 资源标识变化")
            note = read_at(row, "DV", number, shared) or ""
            if not note.startswith("2026-11-03 用户原话：") or "制作中 | 让锋断潮名称整合-20261103 |" not in note:
                fail(code + " 备注不是本次计划")
        workbook = live.read("xl/workbook.xml").decode("utf-8")
        if "已完成!$A$2:$EN$40" not in workbook or "制作中!$A$2:$EN$17" not in workbook:
            fail("数据范围未更新")
        if "已完成!$A$2:$EN$42" in workbook or "制作中!$A$2:$EN$15" in workbook:
            fail("旧数据范围仍在")
    stat = LIVE.stat()
    print(json.dumps({
        "reopenSha256": live_hash,
        "mtime": datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="milliseconds"),
        "size": stat.st_size,
        "counts": counts,
        "tide": places["tide_sever"],
        "ribbon": places["water_yielding_ribbon"],
        "unique": len(seen) == len(set(seen)) == 60,
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
