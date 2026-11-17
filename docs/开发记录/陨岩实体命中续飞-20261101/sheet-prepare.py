# -*- coding: utf-8 -*-
"""Build the fire_meteorite candidate workbook and design-text candidates."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(r"D:\alianximod")
BATCH = ROOT / "docs" / "开发记录" / "陨岩实体命中续飞-20261101"
LIVE = ROOT / "docs" / "术式制作表.xlsx"
DESIGN = ROOT / "docs" / "火属性基础术式与组合设计.md"
AMENDMENT = BATCH / "requirement-amendment.md"
TARGET = "fire_meteorite"
SOURCE_ROW = 6
NEW_ROW = 9
OLD_SENTENCE = (
    "先撞到空中目标时，撞击和命中接续在该碰撞点完成，随后残骸无伤坠落。"
    "接地只处理崩地和方块安放，不再产生中心伤害、热波或资源消费。"
)
REVISED_SENTENCE = (
    "先撞到空中目标时，撞击和命中接续在该碰撞点按原语义只结算一次。"
    "随后同一颗陨岩保持命中瞬间的位置、实际步长和当前弹道继续飞行，直到触地才落块；"
    "不在接触点生成直坠残骸，不清零水平速度，也不另生成第二颗。"
    "沿途再次碰到实体不再造成伤害，也不再触发中心、热波、命中接续或再次消费。"
    "已经续飞的这一支不会被原来的攻击寿命提前清掉或在空中变成方块。"
    "没有提前命中实体就撞块的，仍是一次撞击、一次接续和一次落块。"
    "全程没有合法实体命中的超时仍只留无伤残骸，不叫撞地。"
    "触地只处理崩地和方块安放，不再产生中心伤害、热波或资源消费。"
)
NEW_N = "撞到实体后沿当前弹道继续飞行，触地才变成陨石方块。"
NEW_O = (
    "陨岩_v1\n"
    "正式源：docs/资源库/材质/方块/陨岩_v1/\n"
    "运行：blockstates/meteorite.json、models/block/meteorite.json、models/item/meteorite.json\n"
    "运行贴图：textures/block/meteorite_v1.png\n"
    "游戏内方块名：陨石。残骸实体名：陨石残骸。本次没有新美术版。"
)
DV_APPEND = (
    "2026-11-02 用户授权返修：撞到实体后按当前弹道继续移动，直到触地才变成方块。"
    "该决定替代旧的“命中实体后立即转方块式残骸直坠”。首次结算仍只发生一次，续飞和触地不重复伤害、热波或接续。"
    "实现已收束。.\\gradlew.bat compileJava --offline --no-daemon 于 2026-11-02 00:22:13–00:22:49 退出码 0，"
    "compileJava 实际执行，BUILD SUCCESSFUL。上文 2026-10-31 待统一编译及客户端占用是被本次结果覆盖的历史，不是当前阻塞。"
    "本阶段额度已用完，返修结果未追加复审。游戏待用户实测。\n"
    "待验收 | 陨岩实体命中续飞-20261101 | docs/开发记录/陨岩实体命中续飞-20261101/调度记录.md"
)
DESIGN_EDITS = (
    (
        "如果先撞到空中目标，撞击与命中接续在该碰撞点完成。随后保留可见的陨岩残骸无伤坠落，接地时只处理崩地和方块安放，不再产生中心伤害、震环或资源消费。这个默认处理使首次命中接续和落地方块残留同时成立。",
        "如果先撞到空中目标，撞击与命中接续在该碰撞点完成一次。随后同一颗陨岩按命中瞬间的当前弹道继续飞行，直到触地才落块；不在接触点转成直坠残骸。接地时只处理崩地和方块安放，不再产生中心伤害、震环或资源消费。这个处理使首次命中接续和落地方块残留同时成立。",
    ),
    (
        "附在陨岩上时，每颗实际陨岩只执行一次；先撞空中实体的残骸，等真正接地后执行。",
        "附在陨岩上时，每颗实际陨岩只执行一次；先撞空中实体的陨岩按当前弹道续飞，等真正触地后执行。",
    ),
    (
        "陨岩先撞到空中目标时，两轮判定围绕第一次实体碰撞点完成。残骸继续无伤落地，接地只处理一次崩地与安放方块。最终陨石方块坐标可能与首次碰撞点不同，不能据此重新生成震环。",
        "陨岩先撞到空中目标时，两轮判定围绕第一次实体碰撞点完成。同一颗陨岩按命中时的当前弹道续飞到触地，接地只处理一次崩地与安放方块。最终陨石方块坐标可能与首次碰撞点不同，不能据此重新生成震环。",
    ),
)
AMENDMENT_ADD = (
    "\n## 覆盖关系\n\n"
    "2026-11-02 写回。`docs/开发记录/术式批量制作/待制作-20261031/approved-v1.md` 不修改。"
    "其中「空中撞击后只无伤坠落」，以及本文件「被替代的旧行为」所列句子，"
    "由本修订和同目录快照中的完整修订需求替代。"
    "制作表 `fire_meteorite` 的现行详细需求不再要求命中实体后立即直坠残骸。"
    "`docs/火属性基础术式与组合设计.md` 只改了陨岩命中实体后的运动描述："
    "第三节陨岩、第五节崩地的对应句，以及第七节「首次命中实体与直接落地」。\n"
)
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


def sha256_bytes(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def sha256_file(path):
    return sha256_bytes(path.read_bytes())


def local(tag):
    return tag.rsplit("}", 1)[-1]


def col_name(index):
    name = ""
    while index:
        index, rem = divmod(index - 1, 26)
        name = chr(65 + rem) + name
    return name


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


def renumber_row(row, old, new):
    updated, count = re.subn(rf'(<row\b[^>]*\br="){old}(")', rf"\g<1>{new}\2", row, count=1)
    if count != 1 or row_number(updated) != new:
        fail(f"Row {old} did not renumber")
    updated = re.sub(rf'\br="([A-Z]+){old}"', lambda match: f'r="{match.group(1)}{new}"', updated)
    if re.search(rf'\br="[A-Z]+{old}"', updated):
        fail(f"Row {old} still has old cell refs")
    return updated


def replace_once(xml, old, new, label):
    count = xml.count(old)
    if count != 1:
        fail(f"{label} appeared {count} times")
    return xml.replace(old, new, 1)


def replace_data(xml, new_rows):
    match = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", xml)
    if not match:
        fail("sheetData missing")
    return xml[:match.start(2)] + "".join(new_rows) + xml[match.end(2):]


def column_values(row, number, shared):
    found = {}
    for index in range(1, 145):
        column = col_name(index)
        found[column] = read_at(row, column, number, shared)
    return found


def main():
    live_bytes = LIVE.read_bytes()
    baseline = sha256_bytes(live_bytes)
    stat = LIVE.stat()
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup_dir = BATCH / "备份"
    backup_dir.mkdir(exist_ok=True)
    sequence = 1
    while (backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx").exists():
        sequence += 1
    backup = backup_dir / f"术式制作表-{stamp}-{sequence}.xlsx"
    candidate_dir = BATCH / "候选"
    candidate_dir.mkdir(exist_ok=True)
    candidate = candidate_dir / f"术式制作表-候选-{stamp}-{sequence}.xlsx"
    snap_dir = BATCH / "快照"
    snap_dir.mkdir(exist_ok=True)
    backup.write_bytes(live_bytes)

    with zipfile.ZipFile(backup) as archive:
        order = archive.infolist()
        parts = {info.filename: archive.read(info.filename) for info in order}
    shared = shared_list(parts["xl/sharedStrings.xml"])
    making_xml = parts[PARTS["制作中"]].decode("utf-8")
    pending_xml = parts[PARTS["待验收"]].decode("utf-8")
    counts = {}
    all_ids = []
    located = {}
    for title in STATUS:
        page = ids_of(parts[PARTS[title]].decode("utf-8"), shared)
        counts[title] = len(page)
        for code, number, row in page:
            all_ids.append(code)
            located.setdefault(code, []).append((title, number, row))
    if len(all_ids) != len(set(all_ids)) or counts != {"已完成": 40, "待制作": 0, "制作中": 14, "待验收": 6}:
        fail(f"状态页计数异常 {counts}")
    hits = located.get(TARGET, [])
    if len(hits) != 1 or hits[0][0] != "制作中" or hits[0][1] != SOURCE_ROW:
        fail(f"fire_meteorite 位置异常: {[(item[0], item[1]) for item in hits]}")
    pending_numbers = [number for _code, number, _row in ids_of(pending_xml, shared)]
    if pending_numbers != [3, 4, 5, 6, 7, 8]:
        fail(f"待验收行号异常 {pending_numbers}")
    source_row = hits[0][2]
    original = column_values(source_row, SOURCE_ROW, shared)
    if original["N"].count(OLD_SENTENCE) != 1:
        fail("现行功能描述里的直坠句不唯一")
    if "沿当前弹道继续飞行" in original["DV"]:
        fail("本次进度已经写过")
    revised_n = original["N"].replace(OLD_SENTENCE, REVISED_SENTENCE, 1)
    if "残骸无伤坠落" in revised_n or "直坠残骸" not in revised_n:
        fail("修订后的详细需求仍要求旧行为，或没有写明替代")
    header = {}
    for row in rows_of(making_xml):
        if row_number(row) == 2:
            header = column_values(row, 2, shared)
            break
    if header.get("N") != "功能描述" or header.get("O") != "特效" or header.get("DV") != "备注":
        fail("表头变化")
    archive_ids = [code for code, _number, _row in ids_of(parts[PARTS["待制作原始数据"]].decode("utf-8"), shared)]
    if archive_ids.count(TARGET) != 1:
        fail("待制作原始数据里的首次快照不是一条，停止")
    detail_xml = parts[PARTS["资源状态明细"]].decode("utf-8")
    target_index = shared.index(TARGET)
    if f"<v>{target_index}</v>" in detail_xml:
        fail("资源状态明细已有 fire_meteorite，停止以免改配置语义")

    moved = renumber_row(source_row, SOURCE_ROW, NEW_ROW)
    moved = replace_text_cell(moved, "N", NEW_ROW, NEW_N)
    moved = replace_text_cell(moved, "O", NEW_ROW, NEW_O)
    moved = replace_text_cell(moved, "DV", NEW_ROW, original["DV"] + "\n" + DV_APPEND)
    prefix = source_row[:source_row.find("<c ")].replace(f'r="{SOURCE_ROW}"', f'r="{NEW_ROW}"', 1)
    if not moved.startswith(prefix):
        fail("移动行的行高或行属性丢失")
    for index in range(1, 145):
        column = col_name(index)
        if column in ("N", "O", "DV"):
            continue
        if read_at(moved, column, NEW_ROW, shared) != original[column]:
            fail(f"移动时改到了 {column}")
    if read_at(moved, "N", NEW_ROW, shared) != NEW_N:
        fail("待验收一句没有写上")
    if not read_at(moved, "DV", NEW_ROW, shared).startswith(original["DV"] + "\n"):
        fail("原备注丢失")
    if "compileJava没有执行" not in read_at(moved, "DV", NEW_ROW, shared):
        fail("历史编译阻塞被删掉了")

    making_rows = []
    for row in rows_of(making_xml):
        number = row_number(row)
        if number == SOURCE_ROW:
            continue
        if number > SOURCE_ROW:
            making_rows.append(renumber_row(row, number, number - 1))
        else:
            making_rows.append(row)
    making_out = replace_data(making_xml, making_rows)
    making_out = replace_once(making_out, 'ref="A1:EN16"', 'ref="A1:EN15"', "制作中维度")
    making_out = replace_once(making_out, 'ref="A2:EN6"', 'ref="A2:EN5"', "制作中筛选")
    pending_out = replace_data(pending_xml, rows_of(pending_xml) + [moved])
    pending_out = replace_once(pending_out, 'ref="A1:EN8"', 'ref="A1:EN9"', "待验收维度")
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    workbook = replace_once(workbook, "制作中!$A$2:$EN$16", "制作中!$A$2:$EN$15", "制作中筛选名称")
    if "待验收!$A$2:$EN$2" not in workbook:
        fail("待验收筛选名称不在预期位置")
    ET.fromstring(making_out.encode("utf-8"))
    ET.fromstring(pending_out.encode("utf-8"))
    ET.fromstring(workbook.encode("utf-8"))
    changed = {
        PARTS["制作中"]: making_out.encode("utf-8"),
        PARTS["待验收"]: pending_out.encode("utf-8"),
        "xl/workbook.xml": workbook.encode("utf-8"),
    }
    with zipfile.ZipFile(backup) as src, zipfile.ZipFile(candidate, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
    with zipfile.ZipFile(candidate) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if set(different) != set(changed):
            fail("候选改到了意外部件: " + ",".join(different))
        for title in ("已完成", "待制作", "数据表", "资源状态明细", "待制作原始数据"):
            if dst.read(PARTS[title]) != parts[PARTS[title]]:
                fail(f"{title} 被改动")
        check_making = dst.read(PARTS["制作中"]).decode("utf-8")
        check_pending = dst.read(PARTS["待验收"]).decode("utf-8")
        making_ids = ids_of(check_making, shared)
        pending_ids = ids_of(check_pending, shared)
        expected_making = [code for code, number, _row in ids_of(making_xml, shared) if number != SOURCE_ROW]
        if [code for code, _number, _row in making_ids] != expected_making:
            fail("制作中剩余编号或顺序不对")
        if [code for code, _number, _row in pending_ids][-1] != TARGET or len(pending_ids) != 7:
            fail("待验收没有把陨岩放在末行")
        for code, number, row in ids_of(making_xml, shared):
            if number < SOURCE_ROW:
                kept = [item for item_code, _item_number, item in making_ids if item_code == code][0]
                if kept != row:
                    fail(f"{code} 在删除行之前却被改动")
        outside_making = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml)
        outside_check = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_making)
        outside_making = outside_making.replace('ref="A1:EN16"', 'ref="A1:EN15"').replace('ref="A2:EN6"', 'ref="A2:EN5"')
        if outside_making != outside_check:
            fail("制作中筛选、验证或合并区被额外改动")
        outside_pending = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_xml).replace('ref="A1:EN8"', 'ref="A1:EN9"')
        if outside_pending != re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_pending):
            fail("待验收验证或筛选被额外改动")
        after_counts = {"已完成": 40, "待制作": 0, "制作中": 13, "待验收": 7}
        seen = []
        for title in STATUS:
            page = [code for code, _number, _row in ids_of(dst.read(PARTS[title]).decode("utf-8"), shared)]
            if len(page) != after_counts[title]:
                fail(f"{title} 计数 {len(page)}")
            seen.extend(page)
        if len(seen) != len(set(seen)):
            fail("四页编号重复")

    design = DESIGN.read_bytes().decode("utf-8")
    design_new = design
    for old, new in DESIGN_EDITS:
        if design_new.count(old) != 1:
            fail("设计文档目标句不唯一: " + old[:24])
        design_new = design_new.replace(old, new, 1)
    if design_new.count("残骸继续无伤落地") or design_new.count("保留可见的陨岩残骸无伤坠落"):
        fail("设计文档仍保留直坠残骸句")
    design_candidate = candidate_dir / "火属性设计-候选.md"
    design_candidate.write_bytes(design_new.encode("utf-8"))

    amendment = AMENDMENT.read_bytes().decode("utf-8")
    if "覆盖关系" in amendment:
        fail("需求修订里已经有覆盖关系")
    if not amendment.endswith("\n"):
        amendment += "\n"
    amendment_new = amendment + AMENDMENT_ADD
    amendment_candidate = candidate_dir / "requirement-amendment-候选.md"
    amendment_candidate.write_bytes(amendment_new.encode("utf-8"))

    cells = [{"column": col_name(index), "header": header.get(col_name(index)), "text": original[col_name(index)]} for index in range(1, 145)]
    aen_path = snap_dir / f"变更前-AEN-{stamp}-{sequence}.json"
    nodv_path = snap_dir / f"变更前-NODV-{stamp}-{sequence}.json"
    revised_path = snap_dir / f"修订需求-{stamp}-{sequence}.md"
    aen_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "sheet": "制作中",
        "row": SOURCE_ROW,
        "id": TARGET,
        "columnCount": 144,
        "cells": cells,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    nodv_path.write_text(json.dumps({
        "baselineSha256": baseline,
        "id": TARGET,
        "from": {"sheet": "制作中", "row": SOURCE_ROW},
        "N": original["N"],
        "O": original["O"],
        "DV": original["DV"],
        "revisedN": revised_n,
        "pendingDisplay": {"N": NEW_N, "O": NEW_O, "dvAppend": DV_APPEND},
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    revised_path.write_text(
        "# fire_meteorite 修订需求\n\n"
        "用户原话：接下来修正一下陨岩，陨岩现在命中后会在命中位置生成石块并落下这个效果不好，"
        "撞到实体应该继续以目前弹道移动直到触地才变成方块。\n\n"
        "旧“命中实体后立即转方块式残骸直坠”已由“按命中时当前弹道续飞到触地”替代。"
        "`docs/开发记录/术式批量制作/待制作-20261031/approved-v1.md` 不修改。"
        "覆盖关系写在本目录 requirement-amendment.md 和本快照。\n\n"
        "## 完整修订后的功能需求\n\n"
        + revised_n + "\n\n"
        "## 待验收展示\n\n"
        "N：" + NEW_N + "\n\n"
        + NEW_O + "\n\n"
        "现行运行资源是已有陨岩方块，不是新美术版。"
        "compileJava --offline --no-daemon 于 2026-11-02 00:22:13–00:22:49 退出码 0，compileJava 实际执行。"
        "本阶段额度已用完，返修结果未追加复审。游戏尚未由用户实测。\n\n"
        "## 设计文档被替换的三句\n\n"
        + "\n\n".join(f"旧：{old}\n\n新：{new}" for old, new in DESIGN_EDITS) + "\n",
        encoding="utf-8",
    )
    result = {
        "ready": True,
        "submitted": False,
        "baselineSha256": baseline,
        "baselineMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "backup": str(backup),
        "candidate": str(candidate),
        "candidateSha256": sha256_file(candidate),
        "snapshots": {"aen": str(aen_path), "nodv": str(nodv_path), "revised": str(revised_path)},
        "from": {"sheet": "制作中", "row": SOURCE_ROW},
        "to": {"sheet": "待验收", "row": NEW_ROW},
        "countsBefore": counts,
        "countsAfter": {"已完成": 40, "待制作": 0, "制作中": 13, "待验收": 7},
        "live": str(LIVE),
        "design": str(DESIGN),
        "designSha256": sha256_file(DESIGN),
        "designCandidate": str(design_candidate),
        "designCandidateSha256": sha256_file(design_candidate),
        "amendment": str(AMENDMENT),
        "amendmentSha256": sha256_file(AMENDMENT),
        "amendmentCandidate": str(amendment_candidate),
        "amendmentCandidateSha256": sha256_file(amendment_candidate),
    }
    (BATCH / "sheet-prepare-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"baseline": baseline, "candidate": result["candidateSha256"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
