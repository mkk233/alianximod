# -*- coding: utf-8 -*-
"""Append type options 附魔 and 领域. Do not change filled cells."""
import hashlib
import importlib.util
import json
import re
import sys
import zipfile
from datetime import datetime
from pathlib import Path

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
WANTED = ("附魔", "领域")
spec = importlib.util.spec_from_file_location("archive_write", HERE / "archive-write.py")
aw = importlib.util.module_from_spec(spec)
spec.loader.exec_module(aw)


def load(path):
    payload = path.read_bytes()
    with zipfile.ZipFile(path) as archive:
        order = archive.infolist()
        parts = {info.filename: archive.read(info.filename) for info in order}
    shared = []
    if "xl/sharedStrings.xml" in parts:
        root = __import__("xml.etree.ElementTree", fromlist=["ElementTree"]).ElementTree.fromstring(parts["xl/sharedStrings.xml"])
        for item in root:
            if aw.local(item.tag) == "si":
                texts = []
                for node in item.iter():
                    if aw.local(node.tag) == "t" and node.text:
                        texts.append(node.text)
                shared.append("".join(texts))
    names = aw.sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
    return payload, order, parts, shared, names


def type_rows(parts, names, shared):
    xml = parts[names["数据表"]].decode("utf-8")
    rows = {aw.row_number(row): row for row in aw.rows_of(xml)}
    values = []
    number = 4
    while number in rows:
        value = aw.read_at(rows[number], "Q", number, shared)
        if not value:
            return values, number, xml, rows
        values.append({
            "row": number,
            "Q": value,
            "R": aw.read_at(rows[number], "R", number, shared),
            "S": aw.read_at(rows[number], "S", number, shared),
        })
        number += 1
    return values, None, xml, rows


def defined_hits(workbook):
    hits = []
    for match in re.finditer(r"<definedName\b[^>]*>[^<]*</definedName>", workbook):
        text = match.group(0)
        if "术式类型" in text or "$Q$" in text:
            hits.append(text)
    return hits


def validation_hits(parts, names):
    hits = []
    for title, path in names.items():
        xml = parts[path].decode("utf-8")
        for match in re.finditer(r"<dataValidation\b([^>]*)>([\s\S]*?)</dataValidation>", xml):
            body = match.group(0)
            if "术式类型" not in body and "$Q$" not in body:
                continue
            formula = re.search(r"<formula1>([^<]*)</formula1>", body)
            hits.append({
                "sheet": title,
                "attrs": match.group(1).strip(),
                "formula1": formula.group(1) if formula else None,
            })
    return hits


def status_snapshot(parts, names, shared):
    pages = {}
    gloom = []
    for title in ("已完成", "待制作", "制作中", "待验收"):
        page = aw.ids_of(parts[names[title]].decode("utf-8"), shared)
        pages[title] = len(page)
        for code, number, row in page:
            if code == "fire_nether_gloom":
                gloom.append({
                    "sheet": title,
                    "row": number,
                    "C": aw.read_at(row, "C", number, shared),
                    "N": aw.read_at(row, "N", number, shared),
                })
    return pages, gloom


def check():
    payload, _order, parts, shared, names = load(LIVE)
    types, next_row, _xml, rows = type_rows(parts, names, shared)
    pages, gloom = status_snapshot(parts, names, shared)
    names_only = [item["Q"] for item in types]
    info = {
        "sha256": hashlib.sha256(payload).hexdigest().upper(),
        "bytes": len(payload),
        "mtime": datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
        "types": types,
        "nextTypeRow": next_row,
        "nextRowExists": next_row in rows if next_row else False,
        "definedNames": defined_hits(parts["xl/workbook.xml"].decode("utf-8")),
        "validations": validation_hits(parts, names),
        "pages": pages,
        "gloom": gloom,
        "missing": [name for name in WANTED if name not in names_only],
    }
    (HERE / "type-options-20261101-check.json").write_text(
        json.dumps(info, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(info, ensure_ascii=False))
    if info["missing"]:
        return 0
    return 10


def apply():
    payload, order, parts, shared, names = load(LIVE)
    baseline = hashlib.sha256(payload).hexdigest().upper()
    check_info = json.loads((HERE / "type-options-20261101-check.json").read_text(encoding="utf-8"))
    if baseline != check_info["sha256"]:
        aw.fail("读取后源表变了")
    types, next_row, data_xml, rows = type_rows(parts, names, shared)
    current_names = [item["Q"] for item in types]
    if current_names != [item["Q"] for item in check_info["types"]]:
        aw.fail("类型列表变了")
    missing = [name for name in WANTED if name not in current_names]
    if missing != list(WANTED):
        aw.fail("缺项与检查不一致: " + ",".join(missing))
    if next_row is None:
        aw.fail("类型枚举后面没有空行")
    targets = []
    for offset, text in enumerate(WANTED):
        number = next_row + offset
        if number not in rows:
            aw.fail("类型下一行不存在: " + str(number))
        if aw.read_at(rows[number], "Q", number, shared) not in (None, ""):
            aw.fail("Q" + str(number) + " 已有值")
        targets.append((number, text))
    sample = aw.cell_span(rows[types[-1]["row"]], "Q", types[-1]["row"])
    style = aw.style_of(sample[0]) if sample else None
    data_rows = aw.rows_of(data_xml)
    index_by_row = {aw.row_number(row): index for index, row in enumerate(data_rows)}
    for number, text in targets:
        data_rows[index_by_row[number]], _written = aw.write_value(
            data_rows[index_by_row[number]], "Q", number, text, "text", shared, style
        )
    data_xml = aw.bump_dimension(aw.replace_data(data_xml, data_rows), targets[-1][0])
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    old_range = "数据表!$Q$4:$Q$" + str(next_row - 1)
    new_range = "数据表!$Q$4:$Q$" + str(targets[-1][0])
    matches = list(re.finditer(re.escape(old_range) + r"(?!\d)", workbook))
    if len(matches) != 1:
        aw.fail("类型命名范围出现 %s 次: %s" % (len(matches), old_range))
    match = matches[0]
    workbook = workbook[:match.start()] + new_range + workbook[match.end():]
    changed = {
        names["数据表"]: data_xml.encode("utf-8"),
        "xl/workbook.xml": workbook.encode("utf-8"),
    }
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = HERE / "备份" / f"术式制作表-type-options-{stamp}-1.xlsx"
    output = HERE / "type-options-20261101-candidate-preserved.xlsx"
    backup.parent.mkdir(exist_ok=True)
    backup.write_bytes(payload)
    with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
    with zipfile.ZipFile(output) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if set(different) != set(changed):
            aw.fail("意外改动: " + ",".join(different))
        for title in ("已完成", "待制作", "制作中", "待验收", "待制作原始数据", "资源状态明细"):
            if dst.read(names[title]) != parts[names[title]]:
                aw.fail(title + " 被改动")
        new_parts = dict(parts)
        new_parts.update(changed)
        after, _next, _xml, _rows = type_rows(new_parts, names, shared)
        expected = current_names + list(WANTED)
        if [item["Q"] for item in after] != expected:
            aw.fail("类型顺序不对")
        for item, previous in zip(after, types):
            if item["R"] != previous["R"] or item["S"] != previous["S"]:
                aw.fail("旧类型代码或说明被改动")
        for item in after[len(types):]:
            if item["R"] not in (None, "") or item["S"] not in (None, ""):
                aw.fail("新选项写了代码或说明")
        pages, gloom = status_snapshot(new_parts, names, shared)
        if pages != check_info["pages"] or gloom != check_info["gloom"]:
            aw.fail("状态页或幽冥变了")
        if defined_hits(workbook) != [
            '<definedName name="术式类型">' + new_range + "</definedName>"
        ]:
            aw.fail("命名范围结果不对")
        if validation_hits(new_parts, names) != check_info["validations"]:
            aw.fail("验证引用被改动")
    summary = {
        "baselineSha256": baseline,
        "candidate": str(output),
        "candidateSha256": hashlib.sha256(output.read_bytes()).hexdigest().upper(),
        "backup": str(backup),
        "added": [{"cell": "Q" + str(number), "value": text} for number, text in targets],
        "oldRange": old_range,
        "newRange": new_range,
        "validationChanged": False,
        "codesInvented": False,
        "submitted": False,
    }
    (HERE / "type-options-20261101-build.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(summary, ensure_ascii=False))


def report():
    submit = json.loads((HERE / "type-options-20261101-submit.json").read_text(encoding="utf-8"))
    payload, _order, parts, shared, names = load(LIVE)
    live = hashlib.sha256(payload).hexdigest().upper()
    types, _next, _xml, _rows = type_rows(parts, names, shared)
    pages, gloom = status_snapshot(parts, names, shared)
    result = {
        "submitted": bool(submit.get("submitted")) and live == submit["sha256"],
        "added": [
            {"cell": "数据表!Q9", "value": "附魔"},
            {"cell": "数据表!Q10", "value": "领域"},
        ],
        "names": [item["Q"] for item in types],
        "definedName": defined_hits(parts["xl/workbook.xml"].decode("utf-8")),
        "validationsUnchanged": all(item["formula1"] == "=术式类型" for item in validation_hits(parts, names)),
        "oldOptionsKept": [item["Q"] for item in types[:5]] == ["剑术", "动作", "法术", "通用", "领域附魔"],
        "baselineSha256": "02FC2FD007D695611615AC9A7F26EAE4EE9562303A06E692FDC016DFA166CCE7",
        "liveSha256": live,
        "bytes": len(payload),
        "mtime": datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
        "counts": pages,
        "gloomType": gloom[0]["C"] if gloom else None,
        "gloomCell": ("C" + str(gloom[0]["row"])) if gloom else None,
        "reopenMatches": live == submit["sha256"],
        "method": "ooxml-part-copy",
        "exportRebuilt": False,
    }
    (HERE / "type-options-20261101.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "check"
    if mode == "check":
        raise SystemExit(check())
    if mode == "apply":
        apply()
    elif mode == "report":
        report()
    else:
        aw.fail("未知模式")
