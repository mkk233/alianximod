# -*- coding: utf-8 -*-
"""Submit the remaining type fixes onto the user's newer save."""
import hashlib
import importlib.util
import json
import re
import sys
import zipfile
from datetime import datetime
from pathlib import Path

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
spec = importlib.util.spec_from_file_location("apply", HERE / "reclassify-types-apply.py")
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
aw = mod.aw

REMAINING = (
    ("制作中", "fire_flame_trail", "通用", "附魔"),
    ("制作中", "fire_kindle", "通用", "附魔"),
    ("制作中", "fire_detonate", "通用", "附魔"),
    ("制作中", "earth_break_ground", "通用", "附魔"),
)
PRESERVE = (
    ("已完成", "water_mist", "领域附魔"),
    ("已完成", "water_deep_pressure", "领域附魔"),
    ("已完成", "magic_ignite", "附魔"),
    ("已完成", "vine_mark", "附魔"),
    ("已完成", "tide_mark", "附魔"),
    ("已完成", "sinking_edge", "附魔"),
    ("已完成", "keen_mark", "附魔"),
    ("制作中", "fire_seed_sow", "附魔"),
    ("制作中", "fire_nether_gloom", "领域附魔"),
)


def apply():
    payload, order, parts, shared, names = mod.load(mod.LIVE)
    baseline = hashlib.sha256(payload).hexdigest().upper()
    values, next_row = mod.type_values(parts, names, shared)
    if values != mod.EXPECTED_TYPES or next_row != 10:
        aw.fail("类型列表变了")
    located = mod.page_rows(parts, names, shared)
    edits = []
    for title, code, old, new in REMAINING:
        places = located.get(code, [])
        if len(places) != 1 or places[0][0] != title:
            aw.fail("编号位置不对: " + code)
        _title, number, row = places[0]
        current = aw.read_at(row, "C", number, shared)
        if current == new:
            continue
        if current != old:
            aw.fail(code + " 当前类型是 " + str(current))
        edits.append((number, row, code, old, new))
    if len(edits) != 4:
        aw.fail("待改行不是 4 个")
    for title, code, expected in PRESERVE:
        places = located.get(code, [])
        if len(places) != 1 or places[0][0] != title:
            aw.fail("保留行位置不对: " + code)
        current = aw.read_at(places[0][2], "C", places[0][1], shared)
        if current != expected:
            aw.fail(code + " 不能覆盖为旧映射，当前是 " + str(current))
    xml = parts[names["制作中"]].decode("utf-8")
    rows = aw.rows_of(xml)
    edit_at = {number: (row, code, old, new) for number, row, code, old, new in edits}
    new_rows = []
    for row in rows:
        number = aw.row_number(row)
        if number not in edit_at:
            new_rows.append(row)
            continue
        original, code, old, new = edit_at[number]
        if row != original:
            aw.fail("行内容对不上: " + code)
        sample = aw.cell_span(row, "C", number)
        style = aw.style_of(sample[0]) if sample else None
        updated, action = aw.force_text(row, "C", number, new, {old}, shared, style)
        if action != "forced":
            aw.fail("类型没写上: " + code)
        if aw.read_at(updated, "N", number, shared) != aw.read_at(original, "N", number, shared):
            aw.fail("功能描述被改动: " + code)
        new_rows.append(updated)
    changed = {names["制作中"]: aw.replace_data(xml, new_rows).encode("utf-8")}
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    old_range = "数据表!$Q$4:$Q$10"
    new_range = "数据表!$Q$4:$Q$9"
    matches = list(re.finditer(re.escape(old_range) + r"(?!\d)", workbook))
    if len(matches) != 1:
        aw.fail("类型命名范围出现 %s 次" % len(matches))
    match = matches[0]
    workbook = workbook[:match.start()] + new_range + workbook[match.end():]
    changed["xl/workbook.xml"] = workbook.encode("utf-8")
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = HERE / "备份" / f"术式制作表-reclassify-types-commit-{stamp}-1.xlsx"
    output = HERE / "reclassify-types-20261101-commit-candidate-preserved.xlsx"
    backup.parent.mkdir(exist_ok=True)
    backup.write_bytes(payload)
    with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
    with zipfile.ZipFile(output) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if set(different) != set(changed):
            aw.fail("意外改动: " + ",".join(different))
        new_parts = dict(parts)
        new_parts.update(changed)
        after = mod.page_rows(new_parts, names, shared)
        for _number, _row, code, _old, new in edits:
            places = after[code]
            if aw.read_at(places[0][2], "C", places[0][1], shared) != new:
                aw.fail("写后类型不对: " + code)
        for title, code, expected in PRESERVE:
            places = after[code]
            if aw.read_at(places[0][2], "C", places[0][1], shared) != expected:
                aw.fail("保留值被改: " + code)
        counts = {title: len(aw.ids_of(new_parts[names[title]].decode("utf-8"), shared)) for title in mod.PAGES}
        if counts != {"已完成": 40, "待制作": 0, "制作中": 16, "待验收": 4}:
            aw.fail("行数变了")
        if dst.read(names["待制作原始数据"]) != parts[names["待制作原始数据"]]:
            aw.fail("档案被改动")
        if dst.read(names["数据表"]) != parts[names["数据表"]]:
            aw.fail("类型选项被改动")
    summary = {
        "baselineSha256": baseline,
        "candidateSha256": hashlib.sha256(output.read_bytes()).hexdigest().upper(),
        "backup": str(backup),
        "changedIds": [code for _number, _row, code, _old, _new in edits],
        "oldRange": old_range,
        "newRange": new_range,
        "submitted": False,
    }
    (HERE / "reclassify-types-20261101-commit-build.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(summary, ensure_ascii=False))


def verify():
    submit = json.loads((HERE / "reclassify-types-20261101-commit-submit.json").read_text(encoding="utf-8"))
    payload, _order, parts, shared, names = mod.load(mod.LIVE)
    live = hashlib.sha256(payload).hexdigest().upper()
    values, _next = mod.type_values(parts, names, shared)
    located = mod.page_rows(parts, names, shared)
    defined = re.search(r'<definedName name="术式类型">([^<]*)</definedName>', parts["xl/workbook.xml"].decode("utf-8"))
    current = {}
    invalid = []
    for title in mod.PAGES:
        for code, number, row in aw.ids_of(parts[names[title]].decode("utf-8"), shared):
            value = aw.read_at(row, "C", number, shared)
            current[code] = {"sheet": title, "row": number, "type": value}
            if value not in mod.EXPECTED_TYPES:
                invalid.append(code)
    changed = []
    for title, code, _old, new in REMAINING:
        item = current.get(code)
        if not item or item["type"] != new or item["sheet"] != title:
            aw.fail("重开后未改成附魔: " + code)
        changed.append({"sheet": title, "row": item["row"], "id": code, "new": new})
    preserved = []
    for title, code, expected in PRESERVE:
        item = current[code]
        if item["type"] != expected or item["sheet"] != title:
            aw.fail("重开后保留值变了: " + code)
        preserved.append({"sheet": title, "row": item["row"], "id": code, "type": expected})
    result = {
        "submitted": bool(submit.get("submitted")) and live == submit.get("sha256"),
        "changed": changed,
        "changedCount": len(changed),
        "preservedUserValues": preserved,
        "enum": values,
        "definedName": defined.group(1) if defined else None,
        "invalidRemaining": invalid,
        "counts": {title: len(aw.ids_of(parts[names[title]].decode("utf-8"), shared)) for title in mod.PAGES},
        "checked": sum(len(aw.ids_of(parts[names[title]].decode("utf-8"), shared)) for title in mod.PAGES),
        "baselineSha256": "B2DA801BC9CF03413754040267CA49829859169D4766D54B0FCF62BF255C282D",
        "liveSha256": live,
        "bytes": len(payload),
        "mtime": datetime.fromtimestamp(mod.LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
        "reopenMatches": live == submit.get("sha256") and not invalid,
        "archiveUntouched": True,
        "previousUnsubmitted": True,
    }
    if not result["submitted"]:
        aw.fail("提交结果对不上")
    (HERE / "reclassify-types-20261101-commit-report.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    previous = json.loads((HERE / "reclassify-types-20261101-report.json").read_text(encoding="utf-8"))
    if previous.get("submitted") is not False:
        aw.fail("上一轮未提交事实丢了")
    previous["laterCommit"] = {
        "submitted": True,
        "report": "reclassify-types-20261101-commit-report.json",
        "sha256": live,
        "mtime": result["mtime"],
        "changedIds": [item["id"] for item in changed],
        "note": "上一轮 submitted 仍为 false。这是关闭工作簿后的另一次提交。",
    }
    (HERE / "reclassify-types-20261101-report.json").write_text(
        json.dumps(previous, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    if sys.argv[1] == "apply":
        apply()
    elif sys.argv[1] == "verify":
        verify()
    else:
        aw.fail("未知模式")
