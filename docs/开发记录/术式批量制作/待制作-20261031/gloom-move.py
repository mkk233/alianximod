# -*- coding: utf-8 -*-
"""Move fire_nether_gloom to pending acceptance after the standalone repair compiled."""
import hashlib
import importlib.util
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
spec = importlib.util.spec_from_file_location("archive_write", HERE / "archive-write.py")
aw = importlib.util.module_from_spec(spec)
spec.loader.exec_module(aw)

CODE = "fire_nether_gloom"
MARKER = "[设计反馈 2026-10-31] 用户覆盖：幽冥单独装配即可打开自身领域。"
NEW_N = (
    "幽冥单独装配即可在脚下打开自身领域。水平 7、纵深 7、寿命 200 沿用雾雨和深压的共同值，高度沿用体积领域已登记的 8。"
    "满 20 刻后每 20 刻给范围内敌对生物增加基础 5 火种，乘主人当时产种倍率后取整；重叠时同一全局窗口只取最高一次。"
    "蓝火只是表现。不减速，不附加伤害，不放置原版灵魂火。也可以附在已有领域上，但单独释放不再要求前置。"
)
NEW_O = "assets/xueximod/textures/effect/nether_gloom_v1/nether_gloom.png"
NEW_EJ = "单独释放时作用于自己打开的领域内敌对生物，范围和时长用这块领域。附在已有领域上时仍作用于该领域。不再要求前置载体。"
NEW_EK = "每20tick基础5，乘领域主人当时产种倍率后取整。完整周期后首笔。同一目标在全局20tick窗口只发最高的一次。不减速，不生成原版火。单独释放打开自身领域。附在灼地或雾雨上时不另开第二块。"
NEW_EM = "不减速，不另建魂系资源。单独释放打开自身领域。历史“不自建领域”不再作为现行限制。"
KNOWN = {
    "EJ": "只作用于领域载体内的敌对生物。范围和时长继承载体。",
    "EK": "每20tick基础5，乘领域主人当时产种倍率后取整。完整周期后首笔。同一目标在全局20tick窗口只发最高的一次。不减速，不自建领域，不生成原版火。",
    "EM": "不自建领域，不减速，不另建魂系资源",
}


def squash(text):
    return re.sub(r"\s+", "", text or "")


def load_shared(parts):
    shared = []
    if "xl/sharedStrings.xml" not in parts:
        return shared
    root = ET.fromstring(parts["xl/sharedStrings.xml"])
    for item in root:
        if aw.local(item.tag) == "si":
            texts = []
            for node in item.iter():
                if aw.local(node.tag) == "t" and node.text:
                    texts.append(node.text)
            shared.append("".join(texts))
    return shared


def renumber(row, old, new):
    if old == new:
        return row
    updated = re.sub(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row, count=1)
    updated = re.sub(rf'r="([A-Z]+){old}(?!\d)"', rf'r="\g<1>{new}"', updated)
    if aw.row_number(updated) != new:
        aw.fail(f"行号 {old} 没有变成 {new}")
    return updated


def merges(xml):
    rows = set()
    for ref in re.findall(r'<mergeCell\b[^>]*ref="([A-Z]+)(\d+):([A-Z]+)(\d+)"', xml):
        rows.update(range(int(ref[1]), int(ref[3]) + 1))
    return rows


def cells_of(row, number, shared):
    found = {}
    for match in re.finditer(rf'<c r="([A-Z]+){number}(?!\d)"', row):
        column = match.group(1)
        span = aw.cell_span(row, column, number)
        found[column] = aw.cell_text(span[0], shared) if span else None
    return found


def enum_values(data_xml, column, shared):
    values = []
    for row in aw.rows_of(data_xml):
        number = aw.row_number(row)
        if number < 4:
            continue
        value = aw.read_at(row, column, number, shared)
        if not value:
            break
        values.append(value)
    return values


def sheet_comments(parts, names, title):
    rel_name = "xl/worksheets/_rels/" + Path(names[title]).name + ".rels"
    blob = parts.get(rel_name, b"").decode("utf-8", errors="ignore")
    hits = []
    for target in re.findall(r'Target="([^"]+)"', blob):
        if "comment" not in target.lower():
            continue
        path = "xl/" + target.replace("../", "")
        text = parts.get(path, b"").decode("utf-8", errors="ignore")
        hits.extend(re.findall(r'\b[A-Z]{1,3}(\d+)\b', text))
    return [int(item) for item in hits]


def main():
    live_bytes = LIVE.read_bytes()
    baseline = hashlib.sha256(live_bytes).hexdigest().upper()
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = HERE / "备份" / f"术式制作表-gloom-standalone-{stamp}-1.xlsx"
    output = HERE / "gloom-standalone-candidate-preserved.xlsx"
    backup.parent.mkdir(exist_ok=True)
    backup.write_bytes(live_bytes)
    with zipfile.ZipFile(backup) as archive:
        order = archive.infolist()
        parts = {info.filename: archive.read(info.filename) for info in order}
    shared = load_shared(parts)
    names = aw.sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
    pages = {}
    for title in ("已完成", "待制作", "制作中", "待验收"):
        pages[title] = aw.ids_of(parts[names[title]].decode("utf-8"), shared)
    locations = [(title, number) for title, page in pages.items() for code, number, _row in page if code == CODE]
    if len(locations) != 1:
        aw.fail("幽冥所在状态页不是唯一的: " + json.dumps(locations, ensure_ascii=False))
    source_title, source_row = locations[0]
    if source_title == "已完成":
        aw.fail("幽冥已在已完成，本单不迁")
    archive_before = parts[names["待制作原始数据"]]
    detail_has = b"fire_nether_gloom" in parts[names["资源状态明细"]]
    source_xml = parts[names[source_title]].decode("utf-8")
    source_entry = next(item for item in pages[source_title] if item[0] == CODE)
    _code, number, row = source_entry
    before_cells = cells_of(row, number, shared)
    old_n = before_cells.get("N") or ""
    old_o = before_cells.get("O") or ""
    old_dv = before_cells.get("DV") or ""
    if not old_n or not old_dv:
        aw.fail("幽冥缺少功能描述或备注")
    if MARKER in old_dv and source_title == "待验收" and before_cells.get("N") == NEW_N:
        aw.fail("待验收已经写过这次覆盖，停止以免重复")
    comment_rows = sheet_comments(parts, names, "制作中")
    if any(item >= 3 for item in comment_rows):
        aw.fail("制作中数据行有批注，不能改行号: " + str(comment_rows))
    if number in merges(source_xml):
        aw.fail("幽冥落在合并单元格里")
    data_xml = parts[names["数据表"]].decode("utf-8")
    boundaries = enum_values(data_xml, "AG", shared)
    visuals = enum_values(data_xml, "AK", shared)
    sample = aw.cell_span(row, "A", number)
    text_style = aw.style_of(sample[0]) if sample else None
    actions = []
    row, action = aw.force_text(row, "N", number, NEW_N, {old_n, NEW_N}, shared, text_style)
    actions.append("N:" + action)
    row, action = aw.force_text(row, "O", number, NEW_O, {old_o, NEW_O}, shared, text_style)
    actions.append("O:" + action)
    for column, new_text in (("EJ", NEW_EJ), ("EK", NEW_EK), ("EM", NEW_EM)):
        current = aw.read_at(row, column, number, shared) or ""
        if current == new_text or squash(current) == squash(new_text):
            actions.append(column + ":kept")
            continue
        if squash(current) != squash(KNOWN[column]):
            actions.append(column + ":kept-user")
            continue
        row, action = aw.force_text(row, column, number, new_text, {current}, shared, text_style)
        actions.append(column + ":" + action)
    for column, text in (("CR", "7"), ("CS", "7"), ("CT", "8"), ("CU", "0"), ("CX", "200")):
        current = aw.read_at(row, column, number, shared)
        if current in (None, ""):
            row, action = aw.write_value(row, column, number, text, "number", shared, None)
            actions.append(column + ":" + action)
        elif current == text:
            actions.append(column + ":kept")
        else:
            actions.append(column + ":kept-user")
    if aw.read_at(row, "CY", number, shared) in (None, "") and "开放" in boundaries:
        row, action = aw.force_text(row, "CY", number, "开放", {None, ""}, shared, text_style)
        actions.append("CY:" + action)
    else:
        actions.append("CY:left")
    if aw.read_at(row, "CZ", number, shared) in (None, "") and "安静" in visuals:
        row, action = aw.force_text(row, "CZ", number, "安静", {None, ""}, shared, text_style)
        actions.append("CZ:" + action)
    else:
        actions.append("CZ:left")
    history = ""
    if old_n not in old_dv:
        history += "\n历史功能描述，其中「不自建领域、必须已有载体」不再作为现行限制：\n" + old_n
    if old_o and old_o not in old_dv and old_o != NEW_O:
        history += "\n历史特效：\n" + old_o
    feedback = (
        "\n" + MARKER
        + "无须前置领域或灼地。旧句只留作历史。"
        + "本阶段额度已用完，返修结果未追加复审。"
        + "领域宽7、深7、高8、寿命200、声明初始消耗0。高度8取体积领域已登记值，不是新半径。边界按开放理解；数据表没有安静档时外观格留空，代码用安静档，不复制雾雨封印。"
        + "魔力仍按登记价2预扣一次，打开失败退回。"
        + "compileJava在独立副本退出码0，见 docs/开发记录/术式批量制作/待制作-20261031/gloom-standalone-compile-report.md。"
        + "原项目正在运行的客户端没有换上这次class。未游戏实测，未打包。"
        + "蓝火贴图 " + NEW_O + "。"
        + "\n待验收 | 待制作-20261031 | docs/开发记录/术式批量制作/待制作-20261031/记录.md"
    )
    if MARKER not in old_dv:
        row, action = aw.force_text(row, "DV", number, old_dv + history + feedback, {old_dv}, shared, text_style)
        actions.append("DV:" + action)
    else:
        actions.append("DV:kept")
    if old_dv not in (aw.read_at(row, "DV", number, shared) or ""):
        aw.fail("原备注丢失")
    if aw.read_at(row, "P", number, shared) != before_cells.get("P") or aw.read_at(row, "L", number, shared) != before_cells.get("L"):
        aw.fail("释放种类或魔力被改动")
    changed = {}
    moved_to = source_row
    if source_title == "制作中":
        accept_xml = parts[names["待验收"]].decode("utf-8")
        accept_rows = aw.rows_of(accept_xml)
        blocked = merges(accept_xml)
        target = max(aw.row_number(item) for item in accept_rows) + 1
        while target in blocked:
            target += 1
        moved = renumber(row, number, target)
        making_rows = []
        for item in aw.rows_of(source_xml):
            item_number = aw.row_number(item)
            if item_number == number:
                continue
            if item_number > number:
                item = renumber(item, item_number, item_number - 1)
            making_rows.append(item)
        accept_rows.append(moved)
        making_out = aw.bump_dimension(aw.replace_data(source_xml, making_rows), max(aw.row_number(item) for item in making_rows))
        accept_out = aw.bump_dimension(aw.replace_data(accept_xml, accept_rows), target)
        changed[names["制作中"]] = making_out.encode("utf-8")
        changed[names["待验收"]] = accept_out.encode("utf-8")
        moved_to = target
    else:
        accept_rows = []
        for item in aw.rows_of(source_xml):
            accept_rows.append(row if aw.row_number(item) == number else item)
        accept_out = aw.replace_data(source_xml, accept_rows)
        changed[names["待验收"]] = accept_out.encode("utf-8")
    with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
    with zipfile.ZipFile(output) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if set(different) != set(changed):
            aw.fail("意外改动: " + ",".join(different))
        if dst.read(names["待制作原始数据"]) != archive_before:
            aw.fail("档案被改动")
        if dst.read(names["资源状态明细"]) != parts[names["资源状态明细"]]:
            aw.fail("资源明细被改动")
        new_parts = dict(parts)
        new_parts.update(changed)
        for title in ("制作中", "待验收"):
            ET.fromstring(new_parts[names[title]])
        after = {}
        all_ids = []
        for title in ("已完成", "待制作", "制作中", "待验收"):
            page = aw.ids_of(new_parts[names[title]].decode("utf-8"), shared)
            after[title] = [code for code, _number, _row in page]
            all_ids.extend(after[title])
        if after["制作中"].count(CODE) + after["待验收"].count(CODE) != 1:
            aw.fail("幽冥没有唯一落在一张状态页")
        if CODE in after["制作中"]:
            aw.fail("幽冥仍在制作中")
        if len(all_ids) != len(set(all_ids)):
            aw.fail("状态页编号重复")
        before_counts = {title: len(pages[title]) for title in pages}
        after_counts = {title: len(after[title]) for title in after}
        if source_title == "制作中":
            if after_counts["制作中"] != before_counts["制作中"] - 1 or after_counts["待验收"] != before_counts["待验收"] + 1:
                aw.fail(f"计数不对 {before_counts} -> {after_counts}")
        if sum(after_counts.values()) != sum(before_counts.values()) or len(set(all_ids)) != len(all_ids):
            aw.fail("四页总数或唯一性变了")
    snapshot = {
        "baselineSha256": baseline,
        "sourceSheet": source_title,
        "sourceRow": source_row,
        "targetSheet": "待验收",
        "targetRow": moved_to,
        "before": before_cells,
        "afterN": NEW_N,
        "afterO": NEW_O,
        "actions": actions,
        "boundaries": boundaries,
        "visuals": visuals,
        "resourceDetailHadGloom": detail_has,
        "override": "幽冥本身是领域术式，单独装配即可通过现有领域底座建立自身领域，无须前置领域或灼地。",
        "resource": NEW_O,
        "compile": "gloom-standalone-compile-report.md exit 0, isolated copy, client class not updated",
    }
    (HERE / "gloom-standalone-row-snapshot.json").write_text(
        json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    summary = {
        "baselineSha256": baseline,
        "candidate": str(output),
        "candidateSha256": hashlib.sha256(output.read_bytes()).hexdigest().upper(),
        "backup": str(backup),
        "from": source_title,
        "to": "待验收",
        "sourceRow": source_row,
        "targetRow": moved_to,
        "beforeCounts": {title: len(pages[title]) for title in pages},
        "actions": actions,
        "resourceDetailHadGloom": detail_has,
        "submitted": False,
    }
    (HERE / "gloom-standalone-build.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(summary, ensure_ascii=False))


if __name__ == "__main__":
    main()
