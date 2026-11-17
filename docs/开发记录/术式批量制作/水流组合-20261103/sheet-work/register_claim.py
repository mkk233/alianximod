# -*- coding: utf-8 -*-
"""Register two new techniques and move deep pressure. OOXML-preserving edit."""
import hashlib
import html
import json
import re
import shutil
import zipfile
from datetime import datetime
from pathlib import Path

ROOT = Path(r"D:\alianximod")
LIVE = ROOT / "docs" / "术式制作表.xlsx"
BATCH = ROOT / "docs" / "开发记录" / "术式批量制作" / "水流组合-20261103"
WORK = BATCH / "sheet-work"
BASELINE = "BACDC2AE3E552DCCB2A600A9F6C147C3FABF70DF81680E1FB3EAA59DD260AE04"
MARKER = "docs/开发记录/术式批量制作/水流组合-20261103/记录.md"
STAGE_TODO = f"待制作 | 水流组合-20261103 | {MARKER}"
STAGE_DOING = f"制作中 | 水流组合-20261103 | {MARKER}"

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


def col_index(col):
    number = 0
    for char in col:
        number = number * 26 + ord(char) - 64
    return number


def xml_text(value):
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    )


def shared_items(xml):
    items = []
    for body in re.findall(r"<si\b[^>]*>(.*?)</si>", xml, re.S):
        parts = re.findall(r"<t\b[^>]*>(.*?)</t>", body, re.S)
        items.append(html.unescape("".join(parts)))
    return items


def rows_of(xml):
    found = []
    for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', xml):
        found.append((match.group(1), match.group(0)))
    return found


def row_map(xml):
    return {int(number): row for number, row in rows_of(xml)}


def cell_value(row, column, number, shared):
    match = re.search(
        rf'<c r="{column}{number}\b"([^>]*?)(?:/>|>[\s\S]*?</c>)',
        row,
    )
    if not match:
        return None
    cell = match.group(0)
    attrs = match.group(1)
    if cell.endswith("/>"):
        return ""
    if 't="inlineStr"' in attrs or "<is>" in cell:
        texts = re.findall(r"<t\b[^>]*>(.*?)</t>", cell, re.S)
        return html.unescape("".join(texts))
    raw = re.search(r"<v>(.*?)</v>", cell, re.S)
    if not raw:
        return ""
    if 't="s"' in attrs:
        return shared[int(raw.group(1))]
    return raw.group(1)


def require_header(sheet_xml, shared, column, expected):
    row = row_map(sheet_xml)[2]
    actual = cell_value(row, column, 2, shared)
    if actual != expected:
        fail(f"Header {column} is {actual!r}, expected {expected!r}")


class StringTable:
    def __init__(self, xml):
        self.xml = xml
        self.items = shared_items(xml)
        self.index = {text: pos for pos, text in enumerate(self.items)}
        self.added_refs = 0

    def intern(self, text):
        if text in self.index:
            self.added_refs += 1
            return self.index[text]
        self.index[text] = len(self.items)
        self.items.append(text)
        self.added_refs += 1
        return self.index[text]

    def render(self):
        additions = []
        original_count = len(shared_items(self.xml))
        for text in self.items[original_count:]:
            additions.append(f'<si><t xml:space="preserve">{xml_text(text)}</t></si>')
        updated = self.xml.replace("</sst>", "".join(additions) + "</sst>")
        unique = len(self.items)
        count_match = re.search(r'count="(\d+)"', updated)
        old_count = int(count_match.group(1))
        updated = updated.replace(
            count_match.group(0),
            f'count="{old_count + self.added_refs}"',
            1,
        )
        updated = re.sub(
            r'uniqueCount="\d+"',
            f'uniqueCount="{unique}"',
            updated,
            count=1,
        )
        return updated


def text_cell(column, row, style, index):
    return f'<c r="{column}{row}" s="{style}" t="s"><v>{index}</v></c>'


def number_cell(column, row, style, value):
    if isinstance(value, float) and value.is_integer():
        shown = str(int(value))
    else:
        shown = str(value)
    return f'<c r="{column}{row}" s="{style}"><v>{shown}</v></c>'


def build_row(row_number, spans, height, cells):
    body = "".join(cells)
    return (
        f'<row r="{row_number}" spans="{spans}" ht="{height}" customHeight="1" '
        f'x14ac:dyDescent="0.25">{body}</row>'
    )


def ordered_cells(table, row_number, fields, styles):
    cells = []
    for column, kind, value in sorted(fields, key=lambda item: col_index(item[0])):
        style = styles[kind]
        if kind == "text":
            cells.append(text_cell(column, row_number, style, table.intern(value)))
        elif kind == "id":
            cells.append(text_cell(column, row_number, styles["id"], table.intern(value)))
        elif kind == "long":
            cells.append(text_cell(column, row_number, styles["long"], table.intern(value)))
        else:
            cells.append(number_cell(column, row_number, style, value))
    return cells


def technique_fields(name, code, cooldown, mana, width, depth, height, body_n, body_o, body_dv):
    return [
        ("A", "text", name),
        ("B", "id", code),
        ("C", "text", "领域附魔"),
        ("D", "text", "水"),
        ("E", "num", 0),
        ("F", "num", cooldown),
        ("J", "text", "否"),
        ("L", "num", mana),
        ("M", "text", "是"),
        ("N", "long", body_n),
        ("O", "long", body_o),
        ("P", "text", "命中体"),
        ("Q", "text", "触发"),
        ("R", "text", "直线"),
        ("S", "text", "攻击"),
        ("U", "num", 200),
        ("CR", "num", width),
        ("CS", "num", depth),
        ("CT", "num", height),
        ("CU", "num", mana),
        ("CV", "num", 0),
        ("CW", "num", 0),
        ("CX", "num", 200),
        ("CY", "text", "开放"),
        ("CZ", "text", "体积"),
        ("DV", "long", body_dv),
    ]


VORTEX_N = """旋流是独立的水属性旋转牵引场，可以在干地单独释放。它自带可见水流，但不生成可游泳、也不生成能支撑气泡柱的完整水域。作用是把范围内的对象吸入、向中心收束，并绕轴分开排布。落点、方向和分体都继承链条已经算出的空间结果，不固定在玩家脚下，也不按这个术式的编号另算落点、一分为三或交错。

水平吸引半径默认 8 格，按水平圆距计算，不按方盒。竖直只看当前本体高度。通向本体的路径被方块挡住就不吸引；水本身不算墙。靠近中心时吸引变快，但有速度上限，不瞬移，不穿墙。只有整个身体进入本体才结算入卷，外围经过不扣入卷水势。

单独存在时，生物和玩家的入卷费用固定，潮湿等级不打折、不返还。只有上升气泡已经挂到这个旋流上，并且基部有实际水撑着，形成水龙卷的那一次结算，才读取潮湿：无潮湿照常消耗，一二级少消耗，三四级先付出再返还，净增加水势。高湿也要先付得起这次毛费用。一次释放里同一个实体只结算一次，分出来的几份共用这份记录；离开后再进来不重新扣、也不补返。水势不够，或者本体已经装不下，这个旋流实例立刻结束，卷里的对象开始下落。不是停在外面排队。其他独立领域不跟着消失。

掉落物可以卷进来，不扣也不返水势，但占体积。船、矿车这类有实际运动的非生物按基础入卷费，没有潮湿返还。没有碰撞或已经移除的展示体不推。主人、其他玩家、盟友和敌人都受运动影响；这和深海只对敌人生效的减速、潮湿、窒息不是同一套关系。弹射物不进长期占位，只有真正被偏转时才付该弹的标准处理成本乘 0.2，继续飞，不吞掉。没有旧报价的弹，标准兜底按下面的可调默认，实付仍是兜底乘 0.2，不取整成免费。

没有气泡时只在平面上绕。气泡挂上之后沿螺旋升到顶部，再向外并略向上抛出；同一次已经抛出的不再吸回。结束时还没抛出的停在原地下落，落地按原版摔落结算一次，不另外做一份伤害，也不取消落水或免摔。旋流不继承深海的减速、潮湿和窒息。

下面这些是设计裁决-1给出的可调初始默认，不是用户逐个数确认的原话：魔力 20，冷却 80 刻，寿命 200 刻，干地本体半径 2.5、高 3。表内领域宽和深按直径写 5，高写 3；吸引半径 8 格没有单独的列，不能拿本体半径代替。挂载后升限 8。生物基础入卷 10；形成水龙卷后一档 8、二档 5、三档付 10 返 13、四档付 10 返 16。无旧报价弹射物的标准兜底 5，偏转实付 1。吸引速度、环绕角速度和抛出速度也只在设计裁决里，不另造列。"""

VORTEX_O = """用户要求水流更饱满，在水域里面也要看得见回旋和气泡。旋流本体是一条清楚的旋转水带；干地时水带留在本体高度里，挂上气泡并升起来之后，水带和水里的回旋仍然看得出来。美术 v2 已按这个方向做过示意，还没有经过用户同版本确认，所以这里不填已经确认的资源名。"""

BUBBLE_N = """气泡柱单独就能放。它只在自然水或术式造出的实际水里，沿自己的柱体把水中的对象往上送。干地没有实际水时，不产生有效上升。不按深海重压的编号判断有没有水。亡灵不会游泳也能被抬上去。离开柱体之后，别的规则照旧，不删掉别人已有的状态。

它和旋流不是一套按编号绑定的组合技。上升可以单独存在，也可以挂到同一个施法者、底部靠在一起的旋流上。挂上之后，原独立上升停掉，改由那份旋流带着螺旋上升，并且可以升到深海那层浅水以上；旋流自己没有气泡就不会升、也不会向外抛。一份气泡同一时刻只挂一个旋流。旋流先放或气泡先放都可以。旋流结束时，挂在它上面的气泡也结束，不退回成独立柱，也不去找下一个旋流。没挂上的气泡和深海水域各自留着。

气泡的上升优先于潮湿造成的下压。水被移走、来源到期或挂载失效时，停止这份上升；已经在高处的对象按正常重力落下。仅仅组合消失，不单独变成溃败。

可调初始默认（设计裁决-1，不是用户逐项原话）：魔力 12，冷却 60 刻，寿命 200 刻，半径 1.25，柱高 8，上升速度 0.12 格/刻。表内领域宽和深按直径写 2.5，高写 8。独立柱的实际有效高度还受连通水体限制；挂到旋流上之后，顶部用旋流挂载后的升限，不把两份高度相乘。不继承深海的减速、潮湿、窒息，也不造水。维持消耗按现行领域不扣维护写 0，不恢复旧的 8/20。"""

BUBBLE_O = """用户要求水流更饱满，水域里面要看得见回旋和气泡。气泡柱在水中是一串清楚上升的气泡，干地没有水时不假装有水柱。和旋流叠在一起时，气泡留在升高的水带里面，仍然看得见。美术 v2 待用户同版本确认，这里不填已确认资源名。"""

DEEP_N = """深海重压保持现有稳定编号 water_deep_pressure，以及当前已经实现的低矮开放领域：默认 7×7×3，寿命 200 刻，开域魔力 40，冷却 80 刻，开放边界。域内敌向目标变慢、变潮湿，氧气消耗按现有倍率加快；原生溺水不产水势。存在期间不收维护费。不恢复已经移除的旧压强和到期爆发，也不把旧场地那套接回来。

2026-11-03 批准的新增修改：领域开启时，在几何内部完整落下的空气格生成临时的实际水。这些水要能被浸水、游泳、呼吸、熄火和气泡的水支撑认出来，但是静止、不向外流、不额外生出水源。不覆盖实心方块、已有的天然水、其他流体或方块实体，不主动替换植物。到期、关闭或租约结束时，只撤走仍由这份术式持有的临时水；天然水和玩家后来的改动留下。不能只靠水面贴图或专属编号冒充水域。主人是否吃到减速、潮湿和窒息仍按现有敌向规则；真实水的游泳和浮力则对进入的人生效。这是给现有编号增加的功能，不是从已完成队列另领的新任务。

临时水使用同一份领域几何和寿命，不另造一套半径。表里原先写着的维持消耗 8 和维持周期 20 与现行不扣维护不符，本次改为 0。"""

DEEP_O = """现有重压领域的外观保留。用户这次要求水流更饱满，并且在这片真实水域里面看得见回旋和气泡。新增的水要在领域里看得出来，而不是只有一层表面。美术 v2 待用户同版本确认，这里不填写已确认的新资源名。"""

QUOTE = "2026-11-03 用户原话：「这个版本不错，按照现在的去实现」。"
WET_QUOTE = "实施准备期间用户原话：「旋流自身聚怪不具备水势消耗衰减的规则，只有组成水龙卷才具备这个根据潮湿等级削减消耗补充水势的能力」。"
VISUAL = "视觉要求来自用户否定 v1 的饱满度，并要求水域内可见水流；v2 增加了水带、水下内旋和气泡，尚未同版本确认。O 因此写水流更饱满、水域内可见回旋与气泡，不写已确认资源。"
VERSION = "批准版本：本目录需求快照.md与设计裁决-1.md，2026-11-03。设计裁决给出的数值是可调初始默认，不是用户逐项原话。"


def vortex_dv(stage_line):
    return "\n".join([
        QUOTE,
        WET_QUOTE,
        "弹射物消耗削减 80%，即标准处理成本×0.2，是用户明确的规则，不是×0.8。入卷分档、本体尺寸、速度和兜底水势是设计裁决-1的可调初始默认。",
        VISUAL,
        VERSION,
        stage_line,
    ])


def bubble_dv(stage_line):
    return "\n".join([
        QUOTE,
        "气泡只在实际水里上升，并在组成水龙卷时把旋流往上送，来自已批准的需求快照。分档水势加减属于旋流在形成水龙卷之后的结算，不是气泡自己的收费。",
        "尺寸、魔力、冷却和上升速度是设计裁决-1的可调初始默认，不是用户逐项原话。",
        VISUAL,
        VERSION,
        stage_line,
    ])


def deep_dv():
    return "\n".join([
        QUOTE,
        "本次明确给深海重压增加临时真实水域，用来支撑气泡柱；不是从已完成队列另领一条任务，也不新开编号。历史待制作原文不存在，本行不伪造首次历史档案。变更前的完成页短句和原资源标识保存在本批 sheet-work 变更前快照。旧水属性术式里的到期爆发不恢复。维持费 8/20 按现行不扣维护改为 0。",
        VISUAL,
        VERSION,
        STAGE_DOING,
    ])


def archive_extra(table, row_number, source_row, stamp, baseline):
    note = (
        "2026-11-03首次登记到空白待制作，并在迁入制作中之前留下这份首次快照。"
        "原文来自需求快照.md与设计裁决-1.md，不是从已完成或制作中反推的历史档案。"
        "无适用资源状态明细。源表基线哈希见本行源哈希。"
    )
    fields = [
        ("EP", "long", "docs/术式制作表.xlsx"),
        ("EQ", "long", "待制作"),
        ("ER", "num", source_row),
        ("ES", "long", stamp),
        ("ET", "long", stamp),
        ("EU", "long", baseline.lower()),
        ("EV", "long", note),
        ("EX", "long", "无独立行"),
    ]
    return ordered_cells(table, row_number, fields, {"text": "9", "id": "9", "long": "9", "num": "8"})


def insert_row(xml, row_xml):
    if "</sheetData>" not in xml:
        fail("sheetData close tag missing")
    return xml.replace("</sheetData>", row_xml + "</sheetData>", 1)


def replace_once(xml, old, new, label):
    count = xml.count(old)
    if count != 1:
        fail(f"{label} found {count} times")
    return xml.replace(old, new, 1)


def status_b(xml, shared):
    found = []
    for number, row in rows_of(xml):
        if int(number) < 3:
            continue
        code = cell_value(row, "B", int(number), shared)
        if code:
            found.append((int(number), code, cell_value(row, "A", int(number), shared)))
    return found


def retarget(row_xml, old, new):
    row_xml = re.sub(
        rf'r="([A-Z]+){old}"',
        lambda match: f'r="{match.group(1)}{new}"',
        row_xml,
    )
    row_xml = re.sub(rf'\br="{old}"', f'r="{new}"', row_xml, count=1)
    row_xml = row_xml.replace('ht="31.95"', 'ht="150"', 1)
    return row_xml


def set_number(row_xml, column, row_number, expected, updated):
    pattern = rf'(<c r="{column}{row_number}"[^>]*>)(?:<v>){expected}(?:</v>)(</c>)'
    match = re.search(pattern, row_xml)
    if not match:
        fail(f"Did not find {column}{row_number} value {expected}")
    return row_xml[: match.start()] + match.group(1) + f"<v>{updated}</v>" + match.group(2) + row_xml[match.end() :]


def set_shared(row_xml, column, row_number, index):
    pattern = rf'<c r="{column}{row_number}"([^>]*?)><v>\d+</v></c>'
    match = re.search(pattern, row_xml)
    if not match:
        fail(f"Shared cell {column}{row_number} missing")
    attrs = match.group(1)
    if 't="s"' not in attrs:
        fail(f"{column}{row_number} is not a shared string")
    replacement = f'<c r="{column}{row_number}"{attrs}><v>{index}</v></c>'
    return row_xml[: match.start()] + replacement + row_xml[match.end() :]


def insert_cell(row_xml, cell_xml, column, row_number):
    cells = re.findall(r"<c\b[^>]*/>|<c\b[\s\S]*?</c>", row_xml)
    open_tag = re.match(r"<row\b[^>]*>", row_xml).group(0)
    parsed = []
    for cell in cells:
        ref = re.search(r'r="([A-Z]+)\d+"', cell).group(1)
        parsed.append((col_index(ref), cell))
    parsed.append((col_index(column), cell_xml))
    parsed.sort(key=lambda item: item[0])
    return open_tag + "".join(cell for _, cell in parsed) + "</row>"


def write_zip(source_bytes, replacements, destination):
    source_path = WORK / "_source_for_zip.xlsx"
    source_path.write_bytes(source_bytes)
    with zipfile.ZipFile(source_path, "r") as incoming, zipfile.ZipFile(destination, "w") as outgoing:
        for info in incoming.infolist():
            payload = replacements.get(info.filename, incoming.read(info.filename))
            outgoing.writestr(info, payload)
    source_path.unlink()


def main():
    original = LIVE.read_bytes()
    live_hash = sha256_bytes(original)
    if live_hash != BASELINE:
        fail(f"Source hash changed before edit: {live_hash}")
    stamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    file_stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup_dir = WORK / "备份"
    candidate_dir = WORK / "候选"
    backup_dir.mkdir(parents=True, exist_ok=True)
    candidate_dir.mkdir(parents=True, exist_ok=True)
    backup = backup_dir / f"术式制作表-{file_stamp}-1.xlsx"
    stage_todo = candidate_dir / f"术式制作表-待制作登记-{file_stamp}-1.xlsx"
    stage_doing = candidate_dir / f"术式制作表-制作中领取-{file_stamp}-1.xlsx"
    backup.write_bytes(original)

    with zipfile.ZipFile(LIVE, "r") as book:
        stored = {name: book.read(name) for name in book.namelist()}
    shared_xml = stored["xl/sharedStrings.xml"].decode("utf-8")
    shared = shared_items(shared_xml)
    done_xml = stored[PARTS["已完成"]].decode("utf-8")
    making_xml = stored[PARTS["制作中"]].decode("utf-8")
    archive_xml = stored[PARTS["待制作原始数据"]].decode("utf-8")
    for column, expected in {
        "C": "类型",
        "N": "功能描述",
        "O": "特效",
        "P": "释放种类",
        "S": "链上角色",
        "CR": "领域宽",
        "CS": "领域深",
        "CT": "领域高",
        "CU": "领域初始消耗",
        "CV": "领域维持消耗",
        "CW": "领域维持周期",
        "BU": "场地维持消耗",
        "BV": "场地维持周期",
        "DV": "备注",
        "EP": "来源文件",
        "EX": "关联资源状态明细",
    }.items():
        target_xml = archive_xml if column in {"EP", "EX"} else done_xml
        require_header(target_xml, shared, column, expected)

    pre_rows = row_map(done_xml)
    deep = pre_rows[26]
    if cell_value(deep, "B", 26, shared) != "water_deep_pressure":
        fail("Completed row 26 is not water_deep_pressure")
    pre_snapshot = {
        "sheet": "已完成",
        "row": 26,
        "capturedAt": stamp,
        "sourceHash": live_hash,
        "note": "变更前快照。不用这一短行伪造待制作原始数据。",
        "cells": {},
    }
    for column in re.findall(r'<c r="([A-Z]+)26"', deep):
        pre_snapshot["cells"][column] = cell_value(deep, column, 26, shared)
    (WORK / "变更前-已完成26-water_deep_pressure.json").write_text(
        json.dumps(pre_snapshot, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    existing_ids = []
    for title in ("已完成", "待制作", "制作中", "待验收"):
        existing_ids.extend(code for _, code, _ in status_b(stored[PARTS[title]].decode("utf-8"), shared))
    if "water_vortex" in existing_ids or "water_bubble_column" in existing_ids:
        fail("New id already exists")
    if existing_ids.count("water_deep_pressure") != 1:
        fail("Deep pressure id count is not 1")

    styles = {"text": "8", "id": "9", "long": "9", "num": "8"}
    vortex_base = "\n".join(vortex_dv(STAGE_TODO).split("\n")[:-1])
    bubble_base = "\n".join(bubble_dv(STAGE_TODO).split("\n")[:-1])
    vortex_todo = vortex_base + "\n" + STAGE_TODO
    bubble_todo = bubble_base + "\n" + STAGE_TODO
    vortex_doing = vortex_todo + "\n" + STAGE_DOING
    bubble_doing = bubble_todo + "\n" + STAGE_DOING

    def new_fields(stage):
        vortex_text = vortex_todo if stage == "todo" else vortex_doing
        bubble_text = bubble_todo if stage == "todo" else bubble_doing
        return {
            "water_vortex": technique_fields("旋流", "water_vortex", 80, 20, 5, 5, 3, VORTEX_N, VORTEX_O, vortex_text),
            "water_bubble_column": technique_fields("气泡柱", "water_bubble_column", 60, 12, 2.5, 2.5, 8, BUBBLE_N, BUBBLE_O, bubble_text),
        }

    table_todo = StringTable(shared_xml)
    fields_todo = new_fields("todo")
    vortex_row_todo = build_row(3, "1:144", "150", ordered_cells(table_todo, 3, fields_todo["water_vortex"], styles))
    bubble_row_todo = build_row(4, "1:144", "150", ordered_cells(table_todo, 4, fields_todo["water_bubble_column"], styles))
    archive_vortex = build_row(
        24,
        "1:175",
        "160",
        ordered_cells(table_todo, 24, fields_todo["water_vortex"], styles)
        + archive_extra(table_todo, 24, 3, stamp, live_hash),
    )
    archive_bubble = build_row(
        25,
        "1:175",
        "160",
        ordered_cells(table_todo, 25, fields_todo["water_bubble_column"], styles)
        + archive_extra(table_todo, 25, 4, stamp, live_hash),
    )
    todo_xml = stored[PARTS["待制作"]].decode("utf-8")
    if "x14ac" not in todo_xml or "x14ac" not in archive_xml:
        fail("x14ac namespace missing")
    stage1_todo = insert_row(insert_row(todo_xml, vortex_row_todo), bubble_row_todo)
    stage1_todo = replace_once(stage1_todo, 'ref="A1:EN2"', 'ref="A1:EN4"', "todo dimension")
    stage1_archive = insert_row(insert_row(archive_xml, archive_vortex), archive_bubble)
    stage1_archive = replace_once(stage1_archive, 'ref="A1:FS23"', 'ref="A1:FS25"', "archive dimension")
    stage1_book = stored["xl/workbook.xml"].decode("utf-8")
    stage1_book = replace_once(
        stage1_book,
        "待制作!$A$2:$EN$2",
        "待制作!$A$2:$EN$4",
        "todo filter name",
    )
    write_zip(
        original,
        {
            "xl/sharedStrings.xml": table_todo.render().encode("utf-8"),
            PARTS["待制作"]: stage1_todo.encode("utf-8"),
            PARTS["待制作原始数据"]: stage1_archive.encode("utf-8"),
            "xl/workbook.xml": stage1_book.encode("utf-8"),
        },
        stage_todo,
    )

    table_do = StringTable(shared_xml)
    fields_do = new_fields("doing")
    vortex_row = build_row(18, "1:144", "150", ordered_cells(table_do, 18, fields_do["water_vortex"], styles))
    bubble_row = build_row(19, "1:144", "150", ordered_cells(table_do, 19, fields_do["water_bubble_column"], styles))
    archive_vortex = build_row(
        24,
        "1:175",
        "160",
        ordered_cells(table_do, 24, fields_todo["water_vortex"], styles)
        + archive_extra(table_do, 24, 3, stamp, live_hash),
    )
    archive_bubble = build_row(
        25,
        "1:175",
        "160",
        ordered_cells(table_do, 25, fields_todo["water_bubble_column"], styles)
        + archive_extra(table_do, 25, 4, stamp, live_hash),
    )
    moved = retarget(deep, 26, 20)
    moved = set_shared(moved, "N", 20, table_do.intern(DEEP_N))
    moved = set_shared(moved, "O", 20, table_do.intern(DEEP_O))
    moved = set_number(moved, "BU", 20, "8", "0")
    moved = set_number(moved, "BV", 20, "20", "0")
    moved = set_number(moved, "CV", 20, "8", "0")
    moved = set_number(moved, "CW", 20, "20", "0")
    moved = insert_cell(moved, text_cell("DV", 20, "9", table_do.intern(deep_dv())), "DV", 20)
    if 'r="26"' in moved or re.search(r'r="[A-Z]+26"', moved):
        fail("Moved row still points at row 26")

    stage2_making = insert_row(insert_row(insert_row(making_xml, vortex_row), bubble_row), moved)
    stage2_making = replace_once(stage2_making, 'ref="A1:EN17"', 'ref="A1:EN20"', "making dimension")
    stage2_done = done_xml.replace(deep, "", 1)
    if stage2_done.count(deep) != 0:
        fail("Deep row was not unique")
    stage2_archive = insert_row(insert_row(archive_xml, archive_vortex), archive_bubble)
    stage2_archive = replace_once(stage2_archive, 'ref="A1:FS23"', 'ref="A1:FS25"', "archive dimension 2")
    stage2_book = replace_once(
        stored["xl/workbook.xml"].decode("utf-8"),
        "制作中!$A$2:$EN$17",
        "制作中!$A$2:$EN$20",
        "making filter name",
    )
    write_zip(
        original,
        {
            "xl/sharedStrings.xml": table_do.render().encode("utf-8"),
            PARTS["已完成"]: stage2_done.encode("utf-8"),
            PARTS["制作中"]: stage2_making.encode("utf-8"),
            PARTS["待制作原始数据"]: stage2_archive.encode("utf-8"),
            "xl/workbook.xml": stage2_book.encode("utf-8"),
        },
        stage_doing,
    )

    verify_candidate(stage_doing, original, stored)
    if sha256_bytes(LIVE.read_bytes()) != BASELINE:
        fail("Source changed before replace")
    shutil.copyfile(stage_doing, LIVE)
    reopened = LIVE.read_bytes()
    if sha256_bytes(reopened) != sha256_bytes(stage_doing.read_bytes()):
        shutil.copyfile(backup, LIVE)
        fail("Reopen hash mismatch; backup restored")
    verify_candidate(LIVE, original, stored)
    final_hash = sha256_bytes(LIVE.read_bytes())
    final_mtime = datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S")
    snapshot = {
        "sourceBaseline": live_hash,
        "submittedHash": final_hash,
        "submittedMtime": final_mtime,
        "backup": str(backup),
        "stageTodo": str(stage_todo),
        "stageDoing": str(stage_doing),
        "archivePolicy": "两新项有首次待制作快照。深海重压没有历史原档，不写入待制作原始数据。",
        "rows": {
            "water_vortex": {"from": "新登记待制作第3行", "to": "制作中第18行", "archive": "待制作原始数据第24行"},
            "water_bubble_column": {"from": "新登记待制作第4行", "to": "制作中第19行", "archive": "待制作原始数据第25行"},
            "water_deep_pressure": {"from": "已完成第26行", "to": "制作中第20行", "archive": "不新增"},
        },
        "techniques": {
            "water_vortex": field_dump(fields_do["water_vortex"]),
            "water_bubble_column": field_dump(fields_do["water_bubble_column"]),
            "water_deep_pressure": {
                "kept": "除 N/O/DV 与四处维持列外，原已完成整行保留",
                "N": DEEP_N,
                "O": DEEP_O,
                "DV": deep_dv(),
                "BU": 0,
                "BV": 0,
                "CV": 0,
                "CW": 0,
            },
        },
    }
    (WORK / "本次领取需求快照.json").write_text(
        json.dumps(snapshot, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print("OK", final_hash, final_mtime)
    print("BACKUP", backup)
    print("STAGE", stage_todo)
    print("FINAL_CANDIDATE", stage_doing)


def field_dump(fields):
    return {column: value for column, _, value in fields}


def verify_candidate(path, original, stored):
    with zipfile.ZipFile(path, "r") as book:
        current = {name: book.read(name) for name in book.namelist()}
    if set(current) != set(stored):
        fail(f"Zip parts changed: {set(current) ^ set(stored)}")
    for name in stored:
        if name in {
            "xl/sharedStrings.xml",
            "xl/workbook.xml",
            PARTS["已完成"],
            PARTS["制作中"],
            PARTS["待制作原始数据"],
        }:
            continue
        if current[name] != stored[name]:
            fail(f"Unexpected part change: {name}")
    book_xml = current["xl/workbook.xml"].decode("utf-8")
    old_book = stored["xl/workbook.xml"].decode("utf-8")
    if book_xml.replace("制作中!$A$2:$EN$20", "制作中!$A$2:$EN$17") != old_book:
        fail("Workbook differs beyond the making filter name")
    shared = shared_items(current["xl/sharedStrings.xml"].decode("utf-8"))
    counts = {}
    locations = {}
    for title in ("已完成", "待制作", "制作中", "待验收"):
        found = status_b(current[PARTS[title]].decode("utf-8"), shared)
        counts[title] = len(found)
        for row_number, code, name in found:
            locations.setdefault(code, []).append((title, row_number, name))
    if counts != {"已完成": 37, "待制作": 0, "制作中": 18, "待验收": 7}:
        fail(f"Counts {counts}")
    duplicates = {code: places for code, places in locations.items() if len(places) != 1}
    if duplicates:
        fail(f"Duplicate ids {duplicates}")
    expected = {
        "water_vortex": ("制作中", 18, "旋流"),
        "water_bubble_column": ("制作中", 19, "气泡柱"),
        "water_deep_pressure": ("制作中", 20, "深海重压"),
        "tide_sever": ("制作中", 16, "薄水切"),
        "water_yielding_ribbon": ("制作中", 17, "断潮"),
    }
    for code, place in expected.items():
        if locations.get(code) != [place]:
            fail(f"{code} at {locations.get(code)}")
    making = row_map(current[PARTS["制作中"]].decode("utf-8"))
    old_making = row_map(stored[PARTS["制作中"]].decode("utf-8"))
    for number in range(1, 18):
        if making[number] != old_making[number]:
            fail(f"Making row {number} changed")
    done = row_map(current[PARTS["已完成"]].decode("utf-8"))
    old_done = row_map(stored[PARTS["已完成"]].decode("utf-8"))
    if 26 in done:
        fail("Completed row 26 still exists")
    for number, row in old_done.items():
        if number == 26:
            continue
        if done.get(number) != row:
            fail(f"Completed row {number} changed")
    deep_row = making[20]
    for column, expected_value in {
        "B": "water_deep_pressure",
        "CR": "7",
        "CS": "7",
        "CT": "3",
        "CU": "40",
        "CX": "200",
        "CY": "开放",
        "CZ": "重压",
        "BU": "0",
        "BV": "0",
        "CV": "0",
        "CW": "0",
        "AZ": "0",
        "BF": "0",
        "DM": "2",
        "F": "80",
        "L": "40",
    }.items():
        actual = cell_value(deep_row, column, 20, shared)
        if actual != expected_value:
            fail(f"Deep {column} is {actual!r}")
    if "旧爆发" in (cell_value(deep_row, "N", 20, shared) or ""):
        pass
    if "到期爆发" not in cell_value(deep_row, "N", 20, shared):
        fail("Deep N lost the burst constraint")
    archive = current[PARTS["待制作原始数据"]].decode("utf-8")
    archive_ids = []
    for number, row in rows_of(archive):
        if int(number) < 3:
            continue
        code = cell_value(row, "B", int(number), shared)
        if code:
            archive_ids.append(code)
    if archive_ids.count("water_vortex") != 1 or archive_ids.count("water_bubble_column") != 1:
        fail(f"Archive ids missing {archive_ids}")
    if "water_deep_pressure" in archive_ids:
        fail("Deep pressure was forged into the archive")
    old_archive_rows = row_map(stored[PARTS["待制作原始数据"]].decode("utf-8"))
    new_archive_rows = row_map(archive)
    for number, row in old_archive_rows.items():
        if new_archive_rows.get(number) != row:
            fail(f"Old archive row changed: {number}")
    vortex = making[18]
    bubble = making[19]
    for row, number, code, width in (
        (vortex, 18, "water_vortex", "5"),
        (bubble, 19, "water_bubble_column", "2.5"),
    ):
        if cell_value(row, "B", number, shared) != code:
            fail(code)
        if cell_value(row, "C", number, shared) != "领域附魔":
            fail("type")
        if cell_value(row, "R", number, shared) != "直线":
            fail("trajectory")
        if cell_value(row, "S", number, shared) != "攻击":
            fail("role")
        if cell_value(row, "CR", number, shared) != width:
            fail(f"width {code}")
        if STAGE_TODO not in cell_value(row, "DV", number, shared):
            fail("todo marker missing on doing row")
        if STAGE_DOING not in cell_value(row, "DV", number, shared):
            fail("doing marker missing")
        if "WaterFormEffect" in (cell_value(row, "O", number, shared) or ""):
            fail("O contains a resource id")
    archive_rows = row_map(archive)
    if STAGE_DOING in (cell_value(archive_rows[24], "DV", 24, shared) or ""):
        fail("Archive vortex already says 制作中")
    if cell_value(archive_rows[24], "EX", 24, shared) != "无独立行":
        fail("Archive resource note missing")
    if "8/20" in (cell_value(vortex, "N", 18, shared) or ""):
        pass
    print("verified", path.name, counts)


if __name__ == "__main__":
    main()
