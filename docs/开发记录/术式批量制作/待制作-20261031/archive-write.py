# -*- coding: utf-8 -*-
"""Fill approved v1 numbers into the 13 making rows and append data-sheet enums."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
IDS = [
    "fire_flame_trail",
    "fire_meteorite",
    "fire_shock_ring",
    "rule_surface_anchor",
    "fire_upwell",
    "rule_stagger",
    "rule_hit_continue",
    "fire_seed_sow",
    "fire_kindle",
    "fire_detonate",
    "fire_nether_gloom",
    "earth_break_ground",
    "fire_spark",
]
MARKER = "[设计反馈 2026-10-31] 已采用首版参数。"
NETHER_EK = "每20tick基础5，乘领域主人当时产种倍率后取整。完整周期后首笔。同一目标在全局20tick窗口只发最高的一次。不减速，不自建领域，不生成原版火。"


def fail(message):
    raise SystemExit(message)


def sha256_bytes(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def local(tag):
    return tag.rsplit("}", 1)[-1]


def col_num(column):
    value = 0
    for char in column:
        value = value * 26 + ord(char) - 64
    return value


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_span(row, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row.find(">", start)
    if row[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</(?:[\w]+:)?c>", row[opener_end:])
        if not close:
            fail(f"Cell {column}{number} is not closed")
        end = opener_end + close.end()
    return row[start:end], start, end


def cell_text(cell, shared):
    if cell is None:
        return None
    inline = re.search(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?t>", cell)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if not value:
        return None
    raw = html.unescape(value.group(1))
    if re.search(r'\bt="s"', cell) and raw.isdigit():
        return shared[int(raw)]
    return raw


def sheet_map(workbook_xml, rel_xml):
    targets = {}
    rels = ET.fromstring(rel_xml)
    for rel in rels:
        if local(rel.tag) != "Relationship":
            continue
        target = rel.attrib["Target"].lstrip("/")
        if not target.startswith("xl/"):
            target = "xl/" + target
        targets[rel.attrib["Id"]] = target
    found = {}
    root = ET.fromstring(workbook_xml)
    for sheet in root.iter():
        if local(sheet.tag) != "sheet":
            continue
        rid = sheet.attrib["{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"]
        found[sheet.attrib["name"]] = targets[rid]
    return found


def ids_of(xml, shared):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        found_cell = cell_span(row, "B", number)
        code = cell_text(found_cell[0], shared) if found_cell else None
        if code:
            found.append((code, number, row))
    return found


def text_cell(column, number, text, style):
    escaped = html.escape(text, quote=False)
    space = ""
    if "\n" in text or text[:1].isspace() or text[-1:].isspace():
        space = ' xml:space="preserve"'
    style_attr = f' s="{style}"' if style else ""
    return f'<c r="{column}{number}" t="inlineStr"{style_attr}><is><t{space}>{escaped}</t></is></c>'


def number_cell(column, number, text, style):
    if not re.fullmatch(r"-?\d+(?:\.\d+)?", text):
        fail(f"Not a number: {column}{number}={text}")
    style_attr = f' s="{style}"' if style else ""
    return f'<c r="{column}{number}"{style_attr}><v>{text}</v></c>'


def style_of(cell):
    if not cell:
        return None
    match = re.search(r'\ss="(\d+)"', cell)
    return match.group(1) if match else None


def insert_cell(row, column, number, payload):
    pattern = re.compile(r'<(?:[\w]+:)?c r="([A-Z]+)(\d+)"')
    target = col_num(column)
    for match in pattern.finditer(row):
        if int(match.group(2)) != number:
            fail(f"Row {number} contains {match.group(0)}")
        if col_num(match.group(1)) == target:
            fail(f"{column}{number} already exists")
        if col_num(match.group(1)) > target:
            return row[:match.start()] + payload + row[match.start():]
    close = row.rfind("</row>")
    if close < 0:
        fail(f"Row {number} has no close tag")
    return row[:close] + payload + row[close:]


def write_value(row, column, number, text, kind, shared, style):
    found = cell_span(row, column, number)
    current = cell_text(found[0], shared) if found else None
    if current == text:
        return row, "kept"
    if found and current not in (None, ""):
        fail(f"{column}{number} already has {current!r}, refused {text!r}")
    payload = text_cell(column, number, text, style) if kind == "text" else number_cell(column, number, text, style_of(found[0]) if found else style)
    if found:
        _cell, start, end = found
        return row[:start] + payload + row[end:], "replaced-empty"
    return insert_cell(row, column, number, payload), "inserted"


def force_text(row, column, number, text, allowed, shared, style):
    found = cell_span(row, column, number)
    current = cell_text(found[0], shared) if found else None
    if current == text:
        return row, "kept"
    if current not in allowed:
        return row, "conflict:" + ("" if current is None else current)
    payload = text_cell(column, number, text, style_of(found[0]) if found else style)
    if found:
        _cell, start, end = found
        return row[:start] + payload + row[end:], "forced"
    return insert_cell(row, column, number, payload), "inserted"


def replace_data(xml, new_rows):
    match = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", xml)
    if not match:
        fail("sheetData missing")
    return xml[:match.start(2)] + "".join(new_rows) + xml[match.end(2):]


def bump_dimension(xml, minimum_row):
    match = re.search(r'(<dimension\b[^>]*\bref=")([A-Z]+)(\d+):([A-Z]+)(\d+)(")', xml)
    if not match:
        fail("dimension missing")
    end_row = int(match.group(5))
    if end_row >= minimum_row:
        return xml
    return xml[:match.start(5)] + str(minimum_row) + xml[match.end(5):]


def read_at(row, column, number, shared):
    found = cell_span(row, column, number)
    return cell_text(found[0], shared) if found else None


def note_for(code):
    common = (
        "这不是新的正式复审，设计仍为2/2，实现仍为0/3，也不是游戏实测通过。"
        "单术战斗成长为0，没有成长专列。基础法杖保持6槽；32槽测试法杖是公共物品，不在本行。"
    )
    details = {
        "fire_flame_trail": "魔力2，冷却5，复杂度2，装配消耗2，等级上限20。寿命40tick，直径0.5，视觉采样不超过0.25格，每载体最多256个表现点。链上角色不适用：附魔按所属段绑定，不登记攻击或动作。资源：docs/资源库/特效/火焰/留焰_v1/flame_trail-strip.png，建议运行时 assets/xueximod/textures/effect/flame_trail_v1/flame_trail.png。",
        "fire_meteorite": "魔力48，冷却40，复杂度6，装配消耗1，等级上限20。物理24，弹道抛物线，速度0.8，仰角15度，重力0.08，有伤飞行100tick，碰撞1立方。硬度3、抗爆6、石镐及以上掉自身。六面只用32×32的 meteorite-block-texture.png；64×64条带只是外观预览。资源：docs/资源库/材质/方块/陨岩_v1/。建议运行时 assets/xueximod/textures/block/meteorite_v1.png。",
        "fire_shock_ring": "魔力8，冷却16，复杂度2，装配消耗1，等级上限20。物理4，无运动弹道，不填直线。内半径1.5、外半径5、总高3，这些不是场地半径。无击退，视觉12tick，判定一次。资源：docs/资源库/特效/火焰/热波_v1/shock_ring-strip.png，建议运行时 assets/xueximod/textures/effect/shock_ring_v1/shock_ring.png。",
        "rule_surface_anchor": "魔力0，冷却0，复杂度1，装配消耗1，等级上限20。释放种类改为规则，链上角色地表定位，无弹道。视线32格，失败整组拒绝、不扣费不冷却。资源：docs/资源库/特效/标记/地表定位_v1/surface_anchor-strip.png，建议运行时 assets/xueximod/textures/effect/surface_anchor_v1/surface_anchor.png。",
        "fire_upwell": "魔力0，冷却0，复杂度2，装配消耗1，等级上限20。释放种类改为规则，链上角色喷涌改写，弹道喷涌。初速1，重力0.08，锥面25度，运动最多100tick。与垂落或风暴同分支则拒绝。无独立新素材，暂用载体运动示意。",
        "rule_stagger": "魔力0，冷却0，复杂度2，装配消耗1，等级上限20。链上角色错时，无弹道。第i份在组开始后10i tick出生。无独立新素材，暂用载体运动示意。",
        "rule_hit_continue": "魔力0，冷却0，复杂度3，装配消耗1，等级上限20。链上角色命中接续，无弹道。无独立新素材，暂用载体运动示意。",
        "fire_seed_sow": "魔力2，冷却5，复杂度1，装配消耗2，等级上限20。基础产种10已在数值列。链上角色不适用：附魔按所属段绑定。资源：docs/资源库/特效/火焰/种火_v1/seed_sow-strip.png，建议运行时 assets/xueximod/textures/effect/seed_sow_v1/seed_sow.png。",
        "fire_kindle": "魔力2，冷却5，复杂度2，装配消耗2，等级上限20。首跳延迟10tick写入首次延迟。原结算公式保留。链上角色不适用：附魔按所属段绑定。资源：docs/资源库/特效/火焰/引燃_v1/kindle-strip.png，建议运行时 assets/xueximod/textures/effect/kindle_v1/kindle.png。",
        "fire_detonate": "魔力2，冷却5，复杂度2，装配消耗2，等级上限20。公式仍是0.2×当时余额×(1+当时F)。链上角色不适用：附魔按所属段绑定。资源：docs/资源库/特效/火焰/引爆_v1/detonate-strip.png，建议运行时 assets/xueximod/textures/effect/detonate_v1/detonate.png。",
        "fire_nether_gloom": "魔力2，冷却5，复杂度3，装配消耗2，等级上限20。数值5、周期20、首次延迟20。原“产种待定”改成批准口径。链上角色不适用：附魔按所属段绑定。蓝火只是视觉。资源：docs/资源库/特效/火焰/幽冥_v1/nether_gloom-strip.png，建议运行时 assets/xueximod/textures/effect/nether_gloom_v1/nether_gloom.png。",
        "earth_break_ground": "魔力2，冷却5，复杂度3，装配消耗2，等级上限20。浅碗半径2、深度2、最多32块。链上角色不适用：附魔按所属段绑定。只有土尘，没有坑形贴图。资源：docs/资源库/特效/大地/崩地_v1/ground_dust-strip.png，建议运行时 assets/xueximod/textures/effect/ground_dust_v1/ground_dust.png。",
        "fire_spark": "魔力2，冷却5，复杂度0，装配消耗1，等级上限20。火伤1、直线、攻击、基础产种2保持。速度0.8，寿命30tick，碰撞0.25立方，原版燃烧20tick没有专列。资源：docs/资源库/特效/火焰/火苗_v1/fire_spark-strip.png，建议运行时 assets/xueximod/textures/effect/fire_spark_v1/fire_spark.png。",
    }
    return (
        "\n" + MARKER
        + "见 docs/开发记录/术式批量制作/待制作-20261031/approved-v1.md。"
        + "用户暂用二维原话：先保留这个版本，额度不够用了暂时，之后再重置，暂时直接按照这个版本执行。"
        + common + details[code]
        + "\n制作中 | 待制作-20261031 | docs/开发记录/术式批量制作/待制作-20261031/记录.md"
    )


NUMBERS = {
    "fire_flame_trail": {"F": "5", "G": "20", "H": "2", "K": "2", "L": "2"},
    "fire_meteorite": {"G": "20", "H": "6", "I": "0.8", "K": "1", "L": "48", "U": "100", "V": "0.8"},
    "fire_shock_ring": {"G": "20", "H": "2", "K": "1", "L": "8"},
    "rule_surface_anchor": {"F": "0", "G": "20", "H": "1", "K": "1", "L": "0"},
    "fire_upwell": {"F": "0", "G": "20", "H": "2", "K": "1", "L": "0"},
    "rule_stagger": {"F": "0", "G": "20", "H": "2", "K": "1", "L": "0"},
    "rule_hit_continue": {"F": "0", "G": "20", "H": "3", "K": "1", "L": "0"},
    "fire_seed_sow": {"G": "20", "H": "1", "K": "2", "L": "2"},
    "fire_kindle": {"F": "5", "G": "20", "H": "2", "K": "2", "L": "2", "EH": "10"},
    "fire_detonate": {"F": "5", "G": "20", "H": "2", "K": "2", "L": "2"},
    "fire_nether_gloom": {"F": "5", "G": "20", "H": "3", "K": "2", "L": "2", "EC": "5", "EG": "20", "EH": "20"},
    "earth_break_ground": {"F": "5", "G": "20", "H": "3", "K": "2", "L": "2"},
    "fire_spark": {"G": "20", "H": "0", "I": "0.8", "K": "1", "L": "2", "U": "30", "V": "0.8"},
}
FORCES = {
    "fire_meteorite": {"R": ("抛物线", {None, ""}), "T": ("是", {None, ""})},
    "rule_surface_anchor": {"P": ("规则", {None, "", "命中体"}), "S": ("地表定位", {None, ""})},
    "fire_upwell": {"P": ("规则", {None, "", "命中体"}), "R": ("喷涌", {None, ""}), "S": ("喷涌改写", {None, ""})},
    "rule_stagger": {"S": ("错时", {None, ""})},
    "rule_hit_continue": {"S": ("命中接续", {None, ""})},
    "fire_nether_gloom": {"EK": (NETHER_EK, None)},
}
ENUMS = [
    (9, [("A", "抛物线"), ("B", "PARABOLA"), ("C", "按初速、仰角和重力飞行，不是垂落")]),
    (10, [("A", "喷涌"), ("B", "UPWELL"), ("C", "从锚点向上喷出，不是直线或垂落")]),
    (13, [("E", "地表定位"), ("F", "SEEK_SURFACE"), ("G", "把后续出生改到视线内第一处向上承托面")]),
    (14, [("E", "喷涌改写"), ("F", "TRAJECTORY_UPWELL"), ("G", "把后续运动改成向上喷出")]),
    (15, [("E", "错时"), ("F", "RULE_STAGGER"), ("G", "第i份在组开始后延迟10i tick出生")]),
    (16, [("E", "命中接续"), ("F", "RULE_HIT_CONTINUE"), ("G", "父段成立后接一段子效果")]),
]


def patch_manifest():
    path = HERE / "resource-manifest-v1.json"
    payload = json.loads(path.read_text(encoding="utf-8"))
    for item in payload["items"]:
        for record in item["files"]:
            name = record["name"]
            if name.endswith("-strip.png") and record.get("width") == 64:
                record["frameWidth"] = 64
                record["frameHeight"] = 64
                record["frameCount"] = record["height"] // 64
            if name == "meteorite-block-texture.png":
                record["frameCount"] = 1
                record["role"] = "block-cube-texture"
            if name == "meteorite-strip.png":
                record["role"] = "appearance-preview-only"
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lines = [
        "# 新十三式二维 v1 资源清单",
        "",
        "用户暂用授权：“先保留这个版本，额度不够用了暂时，之后再重置，暂时直接按照这个版本执行”。未进 src，未做游戏实测。",
        "",
        "| 编号 | 对象 | 尺寸与帧数 | SHA256 | 建议运行时路径 |",
        "| --- | --- | --- | --- | --- |",
    ]
    for item in payload["items"]:
        chosen = next(record for record in item["files"] if record["name"].endswith(".png") and "review-board" not in record["name"] and record.get("role") != "appearance-preview-only")
        if item["id"] == "fire_meteorite":
            chosen = next(record for record in item["files"] if record["name"] == "meteorite-block-texture.png")
        frames = chosen.get("frameCount", 1)
        size = f"{chosen['width']}×{chosen['height']}，{frames}帧"
        if item["id"] == "fire_meteorite":
            size = "方块贴图32×32、1帧；外观条带64×1024、16帧，不能贴六面"
        lines.append("| " + item["id"] + " | " + item["logicalObject"] + " | " + size + " | `" + chosen["sha256"] + "` | `" + item["recommendedRuntimePath"] + "` |")
    lines.append("")
    lines.append("正式目录见 JSON 的 formalDirectory。竖条角点 alpha 为 0。预览板是不透明底，不是术式贴图。喷涌、错时、命中接续没有新素材。")
    lines.append("")
    (HERE / "resource-manifest-v1.md").write_text("\n".join(lines), encoding="utf-8")


def main():
    patch_manifest()
    live_bytes = LIVE.read_bytes()
    baseline = sha256_bytes(live_bytes)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = HERE / "备份" / f"术式制作表-archive-record-{stamp}-1.xlsx"
    output = HERE / "approved-record-candidate-preserved.xlsx"
    backup.parent.mkdir(exist_ok=True)
    backup.write_bytes(live_bytes)
    with zipfile.ZipFile(backup) as archive:
        order = archive.infolist()
        parts = {info.filename: archive.read(info.filename) for info in order}
    shared = []
    shared_xml = parts.get("xl/sharedStrings.xml")
    if shared_xml:
        root = ET.fromstring(shared_xml)
        for item in root:
            if local(item.tag) == "si":
                texts = []
                for node in item.iter():
                    if local(node.tag) == "t" and node.text:
                        texts.append(html.unescape(node.text))
                shared.append("".join(texts))
    names = sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
    making_xml = parts[names["制作中"]].decode("utf-8")
    data_xml = parts[names["数据表"]].decode("utf-8")
    archive_xml = parts[names["待制作原始数据"]]
    making_rows = rows_of(making_xml)
    located = {code: (number, row) for code, number, row in ids_of(making_xml, shared)}
    missing = [code for code in IDS if code not in located]
    if missing:
        fail("制作中缺少: " + ",".join(missing))
    changes = []
    edited = {}
    for code in IDS:
        number, row = located[code]
        original_n = read_at(row, "N", number, shared)
        original_o = read_at(row, "O", number, shared)
        original_dv = read_at(row, "DV", number, shared)
        if not original_n or not original_dv:
            fail(f"{code} 缺少原文或备注")
        if MARKER in original_dv:
            fail(f"{code} 已经写过这次采用反馈")
        sample = cell_span(row, "A", number)
        text_style = style_of(sample[0]) if sample else None
        number_style = None
        actions = []
        for column, text in NUMBERS[code].items():
            row, action = write_value(row, column, number, text, "number", shared, number_style)
            actions.append(column + ":" + action)
        for column, (text, allowed) in FORCES.get(code, {}).items():
            if column == "EK":
                current = read_at(row, "EK", number, shared) or ""
                if current == text:
                    actions.append("EK:kept")
                    continue
                if "待定" not in current:
                    actions.append("EK:conflict")
                    continue
                allowed = {current}
            row, action = force_text(row, column, number, text, allowed, shared, text_style)
            actions.append(column + ":" + action)
        row, action = force_text(row, "DV", number, original_dv + note_for(code), {original_dv}, shared, text_style)
        actions.append("DV:" + action)
        if read_at(row, "N", number, shared) != original_n or read_at(row, "O", number, shared) != original_o:
            fail(f"{code} 的 N 或 O 被改动")
        if original_dv not in read_at(row, "DV", number, shared):
            fail(f"{code} 原备注丢失")
        edited[number] = row
        changes.append({"id": code, "row": number, "actions": actions})
    new_making = []
    for row in making_rows:
        number = row_number(row)
        new_making.append(edited.get(number, row))
    making_out = bump_dimension(replace_data(making_xml, new_making), 18)
    data_rows = rows_of(data_xml)
    before_names = {}
    for number in (4, 5, 6, 7, 8, 9, 10, 11, 12):
        for column in ("A", "E"):
            matched = next((row for row in data_rows if row_number(row) == number), None)
            before_names[column + str(number)] = read_at(matched, column, number, shared) if matched else None
    data_style = None
    sample_row = next(row for row in data_rows if row_number(row) == 4)
    sample_cell = cell_span(sample_row, "A", 4)
    if sample_cell:
        data_style = style_of(sample_cell[0])

    def place(rows, number, pairs):
        def editor(existing):
            current = existing if existing is not None else f'<row r="{number}"></row>'
            for column, text in pairs:
                if read_at(current, column, number, shared) not in (None, ""):
                    fail(f"数据表 {column}{number} 已有内容")
                current, _action = write_value(current, column, number, text, "text", shared, data_style)
            return current
        for index, row in enumerate(rows):
            if row_number(row) == number:
                rows[index] = editor(row)
                return
            if row_number(row) > number:
                rows.insert(index, editor(None))
                return
        rows.append(editor(None))

    for number, pairs in ENUMS:
        place(data_rows, number, pairs)
    data_rows.sort(key=row_number)
    data_out = bump_dimension(replace_data(data_xml, data_rows), 16)
    for key, old in before_names.items():
        column, number_text = re.match(r"([A-Z]+)(\d+)", key).groups()
        number = int(number_text)
        row = next(candidate for candidate in data_rows if row_number(candidate) == number)
        if read_at(row, column, number, shared) != old and old is not None:
            fail(f"旧枚举被改写: {key}")
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    replacements = {
        "数据表!$A$4:$A$8": "数据表!$A$4:$A$10",
        "数据表!$E$4:$E$12": "数据表!$E$4:$E$16",
        "'制作中'!$A$2:$EN$8": "'制作中'!$A$2:$EN$18",
    }
    for old, new in replacements.items():
        matches = list(re.finditer(re.escape(old) + r"(?!\d)", workbook))
        if len(matches) != 1:
            fail(f"命名范围出现 {len(matches)} 次: {old}")
        match = matches[0]
        workbook = workbook[:match.start()] + new + workbook[match.end():]
    changed = {
        names["制作中"]: making_out.encode("utf-8"),
        names["数据表"]: data_out.encode("utf-8"),
        "xl/workbook.xml": workbook.encode("utf-8"),
    }
    with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
    with zipfile.ZipFile(output) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if set(different) != set(changed):
            fail("意外改动的部件: " + ",".join(different))
        if dst.read(names["待制作原始数据"]) != archive_xml:
            fail("档案表被改动")
        ET.fromstring(dst.read(names["制作中"]))
        ET.fromstring(dst.read(names["数据表"]))
        ET.fromstring(dst.read("xl/workbook.xml"))
        check_making = dst.read(names["制作中"]).decode("utf-8")
        check_ids = ids_of(check_making, shared)
        if [code for code, _number, _row in check_ids if code in IDS] != IDS:
            fail("13项顺序或所在状态变了")
        for code, number, row in check_ids:
            if code in IDS:
                if MARKER not in (read_at(row, "DV", number, shared) or ""):
                    fail(f"{code} 没有采用反馈")
            else:
                original = next(source for source_code, _source_number, source in ids_of(making_xml, shared) if source_code == code)
                if row != original and code not in IDS:
                    fail(f"其他制作中行被改动: {code}")
    summary = {
        "baselineSha256": baseline,
        "candidate": str(output),
        "candidateSha256": sha256_bytes(output.read_bytes()),
        "backup": str(backup),
        "changes": changes,
        "definedNames": replacements,
        "submitted": False,
    }
    (HERE / "approved-record-archive-build.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps({"baseline": baseline, "candidate": summary["candidateSha256"], "rows": [item["row"] for item in changes]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
