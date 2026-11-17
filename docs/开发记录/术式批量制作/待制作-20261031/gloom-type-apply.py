# -*- coding: utf-8 -*-
"""Set fire_nether_gloom type to 领域附魔. Do not move rows."""
import hashlib
import importlib.util
import json
import re
import sys
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
CODE = "fire_nether_gloom"
NEW = "领域附魔"
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
        root = ET.fromstring(parts["xl/sharedStrings.xml"])
        for item in root:
            if aw.local(item.tag) == "si":
                texts = []
                for node in item.iter():
                    if aw.local(node.tag) == "t" and node.text:
                        texts.append(node.text)
                shared.append("".join(texts))
    names = aw.sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
    return payload, order, parts, shared, names


def locate(parts, names, shared):
    found = []
    pages = {}
    for title in ("已完成", "待制作", "制作中", "待验收"):
        page = aw.ids_of(parts[names[title]].decode("utf-8"), shared)
        pages[title] = [(code, number) for code, number, _row in page]
        for code, number, row in page:
            if code == CODE:
                found.append((title, number, row))
    return found, pages


def type_names(parts, names, shared):
    xml = parts[names["数据表"]].decode("utf-8")
    values = []
    for row in aw.rows_of(xml):
        number = aw.row_number(row)
        if number < 4:
            continue
        value = aw.read_at(row, "Q", number, shared)
        if not value:
            return values, number, xml
        values.append((number, value))
    return values, None, xml


def check():
    payload, _order, parts, shared, names = load(LIVE)
    found, pages = locate(parts, names, shared)
    types, next_row, _xml = type_names(parts, names, shared)
    info = {
        "sha256": hashlib.sha256(payload).hexdigest().upper(),
        "bytes": len(payload),
        "mtime": datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
        "found": [{"sheet": title, "row": number, "type": aw.read_at(row, "C", number, shared)} for title, number, row in found],
        "pages": {title: len(rows) for title, rows in pages.items()},
        "typeNames": [value for _number, value in types],
        "nextTypeRow": next_row,
    }
    (HERE / "gloom-type-comment-20261101-check.json").write_text(
        json.dumps(info, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(info, ensure_ascii=False))
    if len(found) != 1:
        return 2
    if info["found"][0]["type"] == NEW:
        result = {
            "submitted": False,
            "alreadySatisfied": True,
            "sheet": info["found"][0]["sheet"],
            "cell": "C" + str(info["found"][0]["row"]),
            "id": CODE,
            "old": NEW,
            "new": NEW,
            "enumAction": "not-needed",
            "baselineSha256": info["sha256"],
            "liveSha256": info["sha256"],
            "mtime": info["mtime"],
            "bytes": info["bytes"],
            "counts": info["pages"],
            "reopenMatches": True,
        }
        (HERE / "gloom-type-comment-20261101.json").write_text(
            json.dumps(result, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        return 10
    if info["found"][0]["type"] not in ("通用", None, ""):
        return 3
    return 0


def apply():
    payload, order, parts, shared, names = load(LIVE)
    baseline = hashlib.sha256(payload).hexdigest().upper()
    check_info = json.loads((HERE / "gloom-type-comment-20261101-check.json").read_text(encoding="utf-8"))
    if baseline != check_info["sha256"]:
        aw.fail("读取后源表变了")
    found, pages = locate(parts, names, shared)
    if len(found) != 1:
        aw.fail("幽冥不是唯一状态行")
    title, number, row = found[0]
    current = aw.read_at(row, "C", number, shared)
    if current == NEW:
        aw.fail("已经是领域附魔")
    if current not in ("通用", None, ""):
        aw.fail("类型已是其他值: " + str(current))
    types, next_row, data_xml = type_names(parts, names, shared)
    names_only = [value for _row_number, value in types]
    if any(value == NEW for value in names_only):
        enum_action = "already-present"
    else:
        if next_row is None:
            aw.fail("类型枚举后面没有空行")
        enum_action = "append-Q" + str(next_row)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = HERE / "备份" / f"术式制作表-gloom-type-{stamp}-1.xlsx"
    output = HERE / "gloom-type-comment-20261101-candidate-preserved.xlsx"
    backup.parent.mkdir(exist_ok=True)
    backup.write_bytes(payload)
    sample = aw.cell_span(row, "C", number) or aw.cell_span(row, "A", number)
    style = aw.style_of(sample[0]) if sample else None
    row, action = aw.force_text(row, "C", number, NEW, {current}, shared, style)
    if aw.read_at(row, "B", number, shared) != CODE or aw.read_at(row, "N", number, shared) != aw.read_at(found[0][2], "N", number, shared):
        aw.fail("编号或功能描述被改动")
    source_xml = parts[names[title]].decode("utf-8")
    new_rows = []
    for item in aw.rows_of(source_xml):
        new_rows.append(row if aw.row_number(item) == number else item)
    changed = {names[title]: aw.replace_data(source_xml, new_rows).encode("utf-8")}
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    if enum_action.startswith("append"):
        data_rows = aw.rows_of(data_xml)
        data_style = None
        sample_row = next(item for item in data_rows if aw.row_number(item) == 4)
        sample_cell = aw.cell_span(sample_row, "Q", 4)
        if sample_cell:
            data_style = aw.style_of(sample_cell[0])
        placed = False
        for index, item in enumerate(data_rows):
            if aw.row_number(item) == next_row:
                if aw.read_at(item, "Q", next_row, shared) not in (None, ""):
                    aw.fail("类型空行被占用")
                data_rows[index], _written = aw.write_value(item, "Q", next_row, NEW, "text", shared, data_style)
                placed = True
                break
        if not placed:
            aw.fail("没有找到类型枚举的下一行")
        changed[names["数据表"]] = aw.replace_data(data_xml, data_rows).encode("utf-8")
        old_range = "数据表!$Q$4:$Q$" + str(next_row - 1)
        new_range = "数据表!$Q$4:$Q$" + str(next_row)
        matches = list(re.finditer(re.escape(old_range) + r"(?!\d)", workbook))
        if len(matches) != 1:
            aw.fail("类型命名范围出现 %s 次: %s" % (len(matches), old_range))
        match = matches[0]
        workbook = workbook[:match.start()] + new_range + workbook[match.end():]
        changed["xl/workbook.xml"] = workbook.encode("utf-8")
    with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
    with zipfile.ZipFile(output) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if set(different) != set(changed):
            aw.fail("意外改动: " + ",".join(different))
        if dst.read(names["待制作原始数据"]) != parts[names["待制作原始数据"]]:
            aw.fail("档案被改动")
        new_parts = dict(parts)
        new_parts.update({key: dst.read(key) for key in changed})
        after_found, after_pages = locate(new_parts, names, shared)
        if [(item[0], item[1]) for item in after_found] != [(title, number)]:
            aw.fail("幽冥位置变了")
        _title, _number, after_row = after_found[0]
        if aw.read_at(after_row, "C", number, shared) != NEW:
            aw.fail("类型没有写成领域附魔")
        if {key: len(value) for key, value in after_pages.items()} != {key: len(value) for key, value in pages.items()}:
            aw.fail("状态页行数变了")
        ET.fromstring(dst.read(names[title]))
    summary = {
        "baselineSha256": baseline,
        "candidate": str(output),
        "candidateSha256": hashlib.sha256(output.read_bytes()).hexdigest().upper(),
        "backup": str(backup),
        "sheet": title,
        "cell": "C" + str(number),
        "id": CODE,
        "old": current,
        "new": NEW,
        "enumAction": enum_action,
        "submitted": False,
    }
    (HERE / "gloom-type-comment-20261101-build.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(summary, ensure_ascii=False))


def report():
    submit = json.loads((HERE / "gloom-standalone-submit.json").read_text(encoding="utf-8")) if False else None
    submit_path = HERE / "gloom-type-comment-20261101-submit.json"
    submit = json.loads(submit_path.read_text(encoding="utf-8"))
    payload, _order, parts, shared, names = load(LIVE)
    found, pages = locate(parts, names, shared)
    live_type = aw.read_at(found[0][2], "C", found[0][1], shared) if len(found) == 1 else None
    build = json.loads((HERE / "gloom-type-comment-20261101-build.json").read_text(encoding="utf-8"))
    result = {
        "submitted": bool(submit.get("submitted")),
        "sheet": build["sheet"],
        "cell": build["cell"],
        "id": CODE,
        "old": build["old"],
        "new": live_type,
        "enumAction": build["enumAction"],
        "baselineSha256": build["baselineSha256"],
        "liveSha256": hashlib.sha256(payload).hexdigest().upper(),
        "bytes": len(payload),
        "mtime": submit.get("mtime"),
        "counts": {title: len(rows) for title, rows in pages.items()},
        "reopenMatches": live_type == NEW and hashlib.sha256(payload).hexdigest().upper() == submit.get("sha256"),
    }
    (HERE / "gloom-type-comment-20261101.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    mode = sys.argv[1]
    if mode == "check":
        raise SystemExit(check())
    if mode == "apply":
        apply()
    elif mode == "report":
        report()
    else:
        raise SystemExit("unknown mode")
