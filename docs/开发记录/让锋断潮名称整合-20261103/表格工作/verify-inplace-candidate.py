# -*- coding: utf-8 -*-
import html
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

WORK = Path(r"D:\alianximod\docs\开发记录\让锋断潮名称整合-20261103\表格工作")
PREP = json.loads((WORK / "prepare-inplace-commit.json").read_text(encoding="utf-8"))
SENTENCE = "2026-11-03 用户最新决定：仅改名，原停用计划取消；保留功能及水幻形可用，不迁待验收，保持当前子表。"


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


def cell_map(xml, shared):
    root = ET.fromstring(xml)
    found = {}
    for row in root.iter():
        if local(row.tag) != "row":
            continue
        for cell in list(row):
            if local(cell.tag) != "c":
                continue
            ref = cell.attrib.get("r")
            kind = cell.attrib.get("t")
            if kind == "inlineStr":
                text = "".join(html.unescape(node.text or "") for node in cell.iter() if local(node.tag) == "t")
            else:
                value = next((child.text for child in list(cell) if local(child.tag) == "v"), None)
                text = None if value is None else html.unescape(value)
                if kind == "s" and text and text.isdigit():
                    text = shared[int(text)]
            found[ref] = text
    return found


def main():
    live = Path(PREP["live"])
    candidate = Path(PREP["candidate"])
    with zipfile.ZipFile(live) as left, zipfile.ZipFile(candidate) as right:
        changed = [name for name in left.namelist() if left.read(name) != right.read(name)]
        if changed != ["xl/worksheets/sheet3.xml"]:
            raise SystemExit("部件差异不是只有制作中: " + ",".join(changed))
        shared = shared_list(left.read("xl/sharedStrings.xml"))
        old_cells = cell_map(left.read(changed[0]), shared)
        new_cells = cell_map(right.read(changed[0]), shared)
        diffs = [ref for ref in sorted(set(old_cells) | set(new_cells)) if old_cells.get(ref) != new_cells.get(ref)]
        if diffs != ["A16", "A17", "DV16", "DV17"]:
            raise SystemExit("单元格差异异常: " + ",".join(diffs))
        if new_cells["A16"] != "薄水切" or new_cells["A17"] != "断潮":
            raise SystemExit("新名称不对")
        for ref in ("DV16", "DV17"):
            if not str(new_cells[ref]).startswith(str(old_cells[ref]) + "\n" + SENTENCE):
                raise SystemExit(ref + " 备注追加不完整")
        old_xml = left.read(changed[0]).decode("utf-8")
        new_xml = right.read(changed[0]).decode("utf-8")
        old_outside = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", old_xml)
        new_outside = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", new_xml)
        if old_outside != new_outside:
            raise SystemExit("制作中表外结构变化")
        if "autoFilter" not in new_xml or "dataValidations" not in new_xml:
            raise SystemExit("筛选或验证丢失")
    print(json.dumps({"changedParts": changed, "changedCells": diffs, "names": {"A16": new_cells["A16"], "A17": new_cells["A17"]}}, ensure_ascii=False))


if __name__ == "__main__":
    main()
