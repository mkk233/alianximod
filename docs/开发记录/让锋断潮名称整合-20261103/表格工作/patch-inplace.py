# -*- coding: utf-8 -*-
"""Patch only A and DV on the two located rows. Do not submit the artifact-tool export."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(r"D:\alianximod")
WORK = ROOT / "docs" / "开发记录" / "让锋断潮名称整合-20261103" / "表格工作"
LIVE = ROOT / "docs" / "术式制作表.xlsx"
SENTENCE = "2026-11-03 用户最新决定：仅改名，原停用计划取消；保留功能及水幻形可用，不迁待验收，保持当前子表。"
EXPECTED = {
    "tide_sever": {"old": "断潮", "new": "薄水切", "n": "一道薄而长的水切", "o": "TideSeverEffect"},
    "water_yielding_ribbon": {"old": "让锋", "new": "断潮", "n": "先铺开水面，再从中间劈开。水分开没有伤害，只有中间那一刀", "o": "WaterYieldingEffect"},
}
PARTS = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}


def fail(message):
    raise SystemExit(message)


def sha256_bytes(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def local(tag):
    return tag.rsplit("}", 1)[-1]


def rows_of(xml):
    return re.findall(r"<row\b[^>]*>[\s\S]*?</row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


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


def text_cell(column, number, text, style):
    escaped = html.escape(text, quote=False)
    space = ' xml:space="preserve"' if ("\n" in text or text[:1].isspace() or text[-1:].isspace()) else ""
    style_attr = f' s="{style}"' if style else ""
    return f'<c r="{column}{number}"{style_attr} t="inlineStr"><is><t{space}>{escaped}</t></is></c>'


def style_of(cell):
    match = re.search(r'\ss="(\d+)"', cell or "")
    return match.group(1) if match else None


def replace_text_cell(row, column, number, text):
    found = cell_span(row, column, number)
    if not found:
        fail(f"Missing {column}{number}")
    payload = text_cell(column, number, text, style_of(found[0]))
    _cell, start, end = found
    return row[:start] + payload + row[end:]


def ids_of(xml, shared):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        code = read_at(row, "B", number, shared)
        if code:
            found.append((code, number, row))
    return found


def replace_data(xml, new_rows):
    match = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", xml)
    if not match:
        fail("sheetData missing")
    return xml[: match.start(2)] + "".join(new_rows) + xml[match.end(2) :]


def main():
    live_bytes = LIVE.read_bytes()
    baseline = sha256_bytes(live_bytes)
    stat = LIVE.stat()
    with zipfile.ZipFile(LIVE) as archive:
        order = archive.infolist()
        parts = {info.filename: archive.read(info.filename) for info in order}
    shared = shared_list(parts["xl/sharedStrings.xml"])
    counts = {}
    located = {}
    for title, part in PARTS.items():
        page = ids_of(parts[part].decode("utf-8"), shared)
        counts[title] = len(page)
        for code, number, row in page:
            if code in EXPECTED:
                located.setdefault(code, []).append((title, number, row))
    if counts != {"已完成": 38, "待制作": 0, "制作中": 15, "待验收": 7}:
        fail(f"四页计数异常 {counts}")
    making_xml = parts[PARTS["制作中"]].decode("utf-8")
    originals = {}
    for code, spec in EXPECTED.items():
        hits = located.get(code, [])
        if len(hits) != 1 or hits[0][0] != "制作中":
            fail(f"{code} 位置异常")
        _title, number, row = hits[0]
        current = {
            "row": number,
            "A": read_at(row, "A", number, shared),
            "N": read_at(row, "N", number, shared),
            "O": read_at(row, "O", number, shared),
            "DV": read_at(row, "DV", number, shared) or "",
        }
        if current["A"] != spec["old"] or current["N"] != spec["n"] or current["O"] != spec["o"]:
            fail(f"{code} 的名称、功能或资源标识已变")
        if not current["DV"].startswith("2026-11-03 用户原话：") or SENTENCE in current["DV"]:
            fail(f"{code} 的备注状态异常")
        originals[code] = current
    updated_rows = []
    for row in rows_of(making_xml):
        number = row_number(row)
        code = read_at(row, "B", number, shared)
        if code not in EXPECTED:
            updated_rows.append(row)
            continue
        spec = EXPECTED[code]
        current = originals[code]
        row = replace_text_cell(row, "A", number, spec["new"])
        row = replace_text_cell(row, "DV", number, current["DV"] + "\n" + SENTENCE)
        if read_at(row, "A", number, shared) != spec["new"]:
            fail(f"{code} 名称未写上")
        if read_at(row, "B", number, shared) != code or read_at(row, "N", number, shared) != spec["n"] or read_at(row, "O", number, shared) != spec["o"]:
            fail(f"{code} 改到了编号、功能或资源标识")
        note = read_at(row, "DV", number, shared)
        if not note.startswith(current["DV"] + "\n") or not note.endswith(SENTENCE):
            fail(f"{code} 备注没有原样追加")
        updated_rows.append(row)
    making_out = replace_data(making_xml, updated_rows)
    outside_old = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml)
    outside_new = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_out)
    if outside_old != outside_new:
        fail("制作中筛选、验证或视图被改动")
    ET.fromstring(making_out.encode("utf-8"))
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    candidate = WORK / "候选" / f"术式制作表-候选-原位保真-{stamp}-1.xlsx"
    changed = {PARTS["制作中"]: making_out.encode("utf-8")}
    with zipfile.ZipFile(LIVE) as src, zipfile.ZipFile(candidate, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
    with zipfile.ZipFile(candidate) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if different != [PARTS["制作中"]]:
            fail("候选改到了其它部件: " + ",".join(different))
        if sha256_bytes(LIVE.read_bytes()) != baseline:
            fail("准备期间源表已变化")
    result = {
        "ready": True,
        "baselineSha256": baseline,
        "baselineMtime": datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="milliseconds"),
        "baselineSize": stat.st_size,
        "candidate": str(candidate),
        "candidateSha256": sha256_bytes(candidate.read_bytes()),
        "live": str(LIVE),
        "backup": str(WORK / "备份" / "术式制作表-原位-20261103-172443-1.xlsx"),
        "rows": {
            code: {"sheet": "制作中", "row": originals[code]["row"], "from": originals[code]["A"], "to": EXPECTED[code]["new"]}
            for code in EXPECTED
        },
        "artifactExportRejected": "D:/alianximod/docs/开发记录/让锋断潮名称整合-20261103/表格工作/候选/术式制作表-候选-原位-20261103-172443-1.xlsx",
    }
    (WORK / "prepare-inplace-commit.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"baseline": baseline, "candidate": result["candidateSha256"], "rows": result["rows"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
