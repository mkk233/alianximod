# -*- coding: utf-8 -*-
"""Archive the eight pending rows, then update confirmed values in place."""
import hashlib
import html
import io
import json
import re
import zipfile
from copy import deepcopy
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parent
LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
BACKUP = Path(r"D:\alianximod\docs\开发记录\术式批量制作\queue-values-20261111\备份\术式制作表-20261111-142913-1.xlsx")
CANDIDATE = ROOT / "候选" / "术式制作表-queue-values-20261111.xlsx"
BASELINE = "59640cbc39cc00ecfc41c07ea5bcffecc1ead7622252432e3f6dd510a1f6c23f"
BACKUP_TIME = "2026-11-11 14:29:13"
COLLECT_TIME = datetime.now().astimezone().strftime("%Y-%m-%d %H:%M:%S")
SOURCE_FILE = "docs/术式制作表.xlsx"
SOURCE_PAGE = "待制作"
SNAPSHOT = "2026-11-11 写入确认设计前的原始待制作行，仅供对比，不是迁移。"
TARGETS = [
    "fire_pick_lamp",
    "trajectory_parallel",
    "water_wave",
    "water_disperse",
    "sword_guard",
    "sword_array",
    "phantom_sword_surge",
    "sword_flight",
]
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"
ET.register_namespace("", NS["s"])
ET.register_namespace("r", "http://schemas.openxmlformats.org/officeDocument/2006/relationships")

NOTES = {
    "fire_pick_lamp": (
        "[2026-11-11确认] 首轮数值：基础伤害 5，其中剑击 4、附加火伤 1；每个有效命中目标火种 +5；"
        "身前约 2.5 格、横向约 1.5 格；可击退目标向上速度约 0.35 格/秒，不长时间浮空。"
        "法力 10，冷却 30 tick，装配消耗 2，复杂度 2，等级上限 20。不能被挑起的目标仍受伤并加火种。"
        "不消费火种。复杂度只是当前代码兼容值，不把通玄写成等级门槛。尚未领取。"
    ),
    "trajectory_parallel": (
        "[2026-11-11确认] 弹道名称定为平行，链上角色定为平行改写。法力 0，冷却 20 tick，装配消耗 0，"
        "复杂度 0，等级上限 20。只改后续允许改写的术式为水平飞行，保留生成位置；不改写拒绝弹道改写的斩击；"
        "不与垂落、喷涌、龙卷叠成多套运动。表内名称已加入数据表，代码尚未实现，尚未领取。"
    ),
    "water_wave": (
        "[2026-11-11确认] 一次消耗当前可用水势，单次最多 20 点；没有水势不产生有效浪，不扣本术式法力。"
        "按实际扣除的水势线性放大：1 点时长约 2 格、宽约 3 格、伤害 1；20 点时长约 4 格、宽约 5 格、伤害 3。"
        "E=3 是上限，不是每次都打 3 点。击退固定约 2 格，不随水势放大。多目标只扣一次水势。"
        "法力 8，冷却 40 tick，装配 1，复杂度 0，等级上限 20。尚未领取。"
    ),
    "water_disperse": (
        "[2026-11-11确认数值，名称仍暂定] 每次固定消耗 15 点水势，不足则不产生有效效果。"
        "通路身前 8 格长、3 格宽、高 2.5 格；基础伤害 2；主要向两侧推约 2.5 格。"
        "法力 12，冷却 60 tick，装配 1，复杂度 2，等级上限 20。水势按一次施放扣除。"
        "若后来改名，只改名称，不改编号和效果。尚未领取。"
    ),
    "sword_guard": (
        "[2026-11-11确认] 招架窗口 10 tick，身前 120 度。近战和直接命中的箭减伤 50%，"
        "爆炸、持续伤害和环境伤害暂不纳入。成立时优先于水环的水势偏转，这次不扣偏转水势；"
        "未成立则交给水环原规则。水环常驻 20% 减伤是否继续叠加，未确认。"
        "法力 4，冷却 30 tick，装配 1，复杂度 0，等级上限 20。尚未领取。"
    ),
    "sword_array": (
        "[2026-11-11确认] 取消半径 2 格、8 格的自动布阵。搜索半径 16 格，最多 8 把，原地升起 2 格；"
        "已经悬停的剑进入停驻，不反复叠加高度。停驻上限 30 秒，剑来或幻剑纵横可提前结束。"
        "法力 10，冷却 40 tick，装配 1，复杂度 2，等级上限 20。现有链上角色没有「停驻/布置」，"
        "确认本次不造成伤害，所以链上角色留空，不改记成攻击。尚未领取。"
    ),
    "phantom_sword_surge": (
        "[2026-11-11确认] 目标分配为每把剑选离自己最近的合格目标。以玩家为中心，水平 16×16 格，上下各 8 格；"
        "最多 8 把；近远分界 6 格；地面剑原地升高 2 格。单剑基础伤害 2，飞行速度 1.5 格/tick。"
        "法力 12，冷却 60 tick，装配 2，复杂度 3，等级上限 20。多把剑打中同一目标时仍遵守现有无敌帧，"
        "不保证伤害简单相加。尚未领取。"
    ),
    "sword_flight": (
        "[2026-11-11确认] 启动法力 10，维持每秒 10 点，结束后冷却 100 tick，装配 2，复杂度 3，等级上限 20。"
        "选剑顺序、真剑物品转移和公共右键回收已确认。主动结束操作，以及法力回复叠高后如何保证仍有净消耗，"
        "仍未定，不要把每秒 10 点写成已经解决永续飞行。尚未领取。"
    ),
}
ARRAY_N = (
    "调动玩家已有的剑实体，保持各自原来的水平位置并原地升起、停驻，不凭空生成新剑，也不自动搬到指定阵位。"
    "靠近玩家的剑停驻后就是周身布阵，距离玩家较远的剑就是外围布阵。列剑本次不造成伤害，不把玩家传送到剑旁。"
    "出剑由幻剑纵横承担，召回继续使用剑来。"
)
ARRAY_O = "已有剑实体在原来位置升起并停驻，不自动围成新阵形，本次不播放剑群出击。"
SURGE_N = (
    "操纵周围可由玩家操纵的已有剑实体发起攻击，玩家留在原地，不随剑瞬移。插在地上的剑先原地升起；"
    "已经悬停或列阵的剑直接发剑。每把剑分别选择离自己最近的合格敌对目标，只决定出手方向，默认不持续追踪。"
    "没有合格目标时，距玩家水平距离 6 格以内的剑朝外，超过 6 格的剑朝玩家方向，再由三维交错错开各自方向。"
    "飞出的剑仍可通过剑来回收。本招不改变基础幻剑术的生成逻辑。"
)
FLIGHT_APPEND = (
    "承载使用背包中的真实剑物品：先查快捷栏从左到右的第一把剑；快捷栏没有，再按主背包从上到下、每行从左到右的第一把剑。"
    "没有真剑不能启动。幻剑不能承载飞行。成功启动时从原槽取出这把剑并交给承载实体，完整保留耐久、附魔、名称和其他物品数据；"
    "启动失败不能吞剑。真剑丢出后形成的实体新增公共右键回收：主手为空则直接回主手；主手有物品时先把原主手物品移入背包，"
    "再把剑放到主手；背包放不下则本次回收失败，剑实体保留，不吞物品、不丢到地上。御剑承载剑使用同一套归还。"
    "幻剑不因此变成能收进背包的真实物品。"
)
WAVE_EJ = "一次消耗当前可用水势，单次最多 20 点。没有可用水势时不产生有效浪，不扣本术式法力。按实际扣除的水势线性放大范围和伤害。"
WAVE_EK = "E=3 是 20 点水势时的伤害上限，不是每次固定扣 20 或固定打 3 点。多目标只扣一次水势。击退固定约 2 格，不随水势放大。"


def sha256(payload):
    return hashlib.sha256(payload).hexdigest()


def load(payload):
    if isinstance(payload, (bytes, bytearray)):
        raw = bytes(payload)
    else:
        raw = payload.read_bytes()
    with zipfile.ZipFile(io.BytesIO(raw)) as book:
        entries = book.infolist()
        data = {item.filename: book.read(item.filename) for item in entries}
    return raw, entries, data


def sheet_parts(data):
    rels = ET.fromstring(data["xl/_rels/workbook.xml.rels"])
    targets = {}
    for node in rels:
        target = "xl/" + node.attrib["Target"].lstrip("/")
        targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
    work = ET.fromstring(data["xl/workbook.xml"])
    names = {}
    for node in work.find("s:sheets", NS):
        names[node.attrib["name"]] = targets[node.attrib[REL]]
    return names


def rows_of(text):
    return {
        int(match.group(1)): match
        for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text)
    }


def cell_blocks(row_xml):
    return list(re.finditer(r'<c\b[^>]*r="([A-Z]+)(\d+)"[^>]*(?:/>|>[\s\S]*?</c>)', row_xml))


def cell_value(block):
    if "</c>" not in block:
        return ""
    inner = re.search(r">(.*)</c>", block, re.S).group(1)
    return "".join(ET.fromstring("<root>" + inner + "</root>").itertext())


def cell_map(row_xml):
    found = {}
    for match in cell_blocks(row_xml):
        found[match.group(1)] = cell_value(match.group(0))
    return found


def col_index(letters):
    number = 0
    for char in letters:
        number = number * 26 + ord(char) - 64
    return number


def inline(column, row_number, style, text):
    escaped = html.escape(text, quote=False)
    return f'<c r="{column}{row_number}" s="{style}" t="inlineStr"><is><t>{escaped}</t></is></c>'


def number_cell(column, row_number, style, value):
    return f'<c r="{column}{row_number}" s="{style}"><v>{value}</v></c>'


def replace_cell(row_xml, column, row_number, replacement):
    pattern = rf'<c\b[^>]*r="{column}{row_number}"[^>]*(?:/>|>[\s\S]*?</c>)'
    updated, count = re.subn(pattern, lambda _match: replacement, row_xml, count=1)
    if count != 1:
        raise SystemExit(f"Missing cell {column}{row_number}")
    return updated


def insert_cell(row_xml, column, row_number, replacement):
    if re.search(rf'<c\b[^>]*r="{column}{row_number}"', row_xml):
        raise SystemExit(f"Cell already exists {column}{row_number}")
    target = col_index(column)
    for match in cell_blocks(row_xml):
        if col_index(match.group(1)) > target:
            return row_xml[:match.start()] + replacement + row_xml[match.start():]
    return row_xml.replace("</row>", replacement + "</row>", 1)


def set_cell(row_xml, column, row_number, replacement):
    if re.search(rf'<c\b[^>]*r="{column}{row_number}"', row_xml):
        return replace_cell(row_xml, column, row_number, replacement)
    return insert_cell(row_xml, column, row_number, replacement)


def remove_cell(row_xml, column, row_number):
    pattern = rf'<c\b[^>]*r="{column}{row_number}"[^>]*(?:/>|>[\s\S]*?</c>)'
    updated, count = re.subn(pattern, "", row_xml, count=1)
    if count != 1:
        raise SystemExit(f"Cannot clear missing cell {column}{row_number}")
    return updated


def append_note(original, addition):
    original = original or ""
    if addition in original:
        raise SystemExit("Note already present")
    if not original.endswith("\n") and original:
        original += "\n"
    return original + addition


def retarget_row(row_xml, old, new):
    updated = row_xml.replace(f'<row r="{old}"', f'<row r="{new}"', 1)
    return re.sub(rf'r="([A-Z]+){old}"', lambda match: f'r="{match.group(1)}{new}"', updated)


def ids_of(text):
    found = []
    for number, match in sorted(rows_of(text).items()):
        if number < 3:
            continue
        value = cell_map(match.group(0)).get("B")
        if value not in (None, ""):
            found.append((number, value.strip(), cell_map(match.group(0)).get("A")))
    return found


def build(source_bytes):
    _payload, entries, data = load(source_bytes)
    parts = sheet_parts(data)
    pending = data[parts["待制作"]].decode("utf-8")
    archive = data[parts["待制作原始数据"]].decode("utf-8")
    lookup = data[parts["数据表"]].decode("utf-8")
    workbook = data["xl/workbook.xml"].decode("utf-8")
    styles = ensure_wrap_style(data["xl/styles.xml"].decode("utf-8"))

    pending_rows = rows_of(pending)
    located = {}
    for number, match in pending_rows.items():
        if number < 3:
            continue
        cells = cell_map(match.group(0))
        value = (cells.get("B") or "").strip()
        if value in TARGETS:
            located[value] = (number, match.group(0), cells)
    if list(located) != TARGETS and set(located) != set(TARGETS):
        raise SystemExit("Pending target mismatch: " + ",".join(located))
    if len(located) != 8:
        raise SystemExit("Expected eight pending targets")

    archive_ids = {item[1] for item in ids_of(archive)}
    overlap = sorted(archive_ids.intersection(TARGETS))
    if overlap:
        raise SystemExit("Archive already has: " + ",".join(overlap))
    other_sheets = ["已完成", "制作中", "待验收", "资源状态明细"]
    for name in other_sheets:
        hits = [item[1] for item in ids_of(data[parts[name]].decode("utf-8")) if item[1] in TARGETS]
        if hits:
            raise SystemExit(f"{name} already has {hits}")

    archive_rows = rows_of(archive)
    if max(archive_rows) != 33 or 34 in archive_rows:
        raise SystemExit("Archive append position changed")
    archive_xml = []
    provenance = []
    for offset, stable in enumerate(TARGETS):
        source_row, row_xml, _cells = located[stable]
        archive_row = 34 + offset
        copied = retarget_row(row_xml, source_row, archive_row)
        copied = copied.replace("<row ", '<row ht="156" customHeight="1" s="19" ', 1)
        extras = [
            inline("EP", archive_row, 9, SOURCE_FILE),
            inline("EQ", archive_row, 9, SOURCE_PAGE),
            number_cell("ER", archive_row, 8, source_row),
            inline("ES", archive_row, 9, BACKUP_TIME),
            inline("ET", archive_row, 9, COLLECT_TIME),
            inline("EU", archive_row, 9, BASELINE.upper()),
            inline("EV", archive_row, 9, SNAPSHOT),
            inline("EX", archive_row, 9, "无独立行"),
        ]
        copied = copied.replace("</row>", "".join(extras) + "</row>", 1)
        if MARKER_FORBIDDEN(copied):
            raise SystemExit("Archive received a confirmation marker")
        archive_xml.append(copied)
        provenance.append({"id": stable, "sourceRow": source_row, "archiveRow": archive_row})

    archive_new = archive.replace("</sheetData>", "".join(archive_xml) + "</sheetData>", 1)
    archive_new = archive_new.replace('ref="A1:FS33"', 'ref="A1:FS41"', 1)

    if "平行" in cell_map(rows_of(lookup)[11].group(0)).get("A", "") or "平行改写" in cell_map(rows_of(lookup)[17].group(0) if 17 in rows_of(lookup) else "").get("E", ""):
        raise SystemExit("Lookup names already exist")
    if 11 in rows_of(lookup) and cell_map(rows_of(lookup)[11].group(0)).get("A") not in (None, ""):
        raise SystemExit("Trajectory slot A11 is occupied")
    if 17 in rows_of(lookup):
        raise SystemExit("Role row 17 already exists")
    trajectory = (
        '<row r="11" ht="32" customHeight="1" s="19">'
        + inline("A", 11, 29, "平行")
        + inline("B", 11, 29, "PARALLEL")
        + inline("C", 11, 29, "沿施放朝向的水平方向飞行，保留原生成位置")
        + "</row>"
    )
    role = (
        '<row r="17" ht="48" customHeight="1" s="19">'
        + inline("E", 17, 29, "平行改写")
        + inline("F", 17, 29, "TRAJECTORY_PARALLEL")
        + inline("G", 17, 29, "把后续允许改写的术式改成水平飞行；不改生成位置，不与垂落、喷涌、龙卷叠成多套运动")
        + "</row>"
    )
    lookup_new = lookup.replace("</sheetData>", trajectory + role + "</sheetData>", 1)
    lookup_new = lookup_new.replace('ref="A1:BV16"', 'ref="A1:BV17"', 1)
    lookup_new = lookup_new.replace('<col width="24" customWidth="1" style="19" min="6" max="6"/>', '<col width="38" customWidth="1" style="19" min="6" max="6"/>', 1)

    workbook_new = workbook.replace("数据表!$A$4:$A$10", "数据表!$A$4:$A$11", 1)
    workbook_new = workbook_new.replace("数据表!$E$4:$E$16", "数据表!$E$4:$E$17", 1)
    if workbook_new.count("数据表!$A$4:$A$11") != 1 or workbook_new.count("数据表!$E$4:$E$17") != 1:
        raise SystemExit("Named range replacement was not unique")

    updates = {}
    changed_columns = {}
    for stable in TARGETS:
        source_row, row_xml, original = located[stable]
        updated = row_xml
        touched = []

        def put_number(column, value, current=updated):
            nonlocal updated
            updated = set_cell(updated, column, source_row, number_cell(column, source_row, 8, value))
            touched.append(column)

        def put_text(column, value):
            nonlocal updated
            updated = set_cell(updated, column, source_row, inline(column, source_row, 9, value))
            touched.append(column)

        if stable == "fire_pick_lamp":
            for column, value in (("E", 5), ("F", 30), ("G", 20), ("H", 2), ("K", 2), ("L", 10), ("EC", 5)):
                put_number(column, value)
        elif stable == "trajectory_parallel":
            put_text("R", "平行")
            put_text("S", "平行改写")
            for column, value in (("F", 20), ("G", 20), ("H", 0), ("K", 0), ("L", 0)):
                put_number(column, value)
        elif stable == "water_wave":
            for column, value in (("E", 3), ("F", 40), ("G", 20), ("H", 0), ("K", 1), ("L", 8), ("EC", 20)):
                put_number(column, value)
            put_text("EJ", WAVE_EJ)
            put_text("EK", WAVE_EK)
        elif stable == "water_disperse":
            for column, value in (("E", 2), ("F", 60), ("G", 20), ("H", 2), ("K", 1), ("L", 12), ("EC", 15)):
                put_number(column, value)
        elif stable == "sword_guard":
            for column, value in (("F", 30), ("G", 20), ("H", 0), ("K", 1), ("L", 4)):
                put_number(column, value)
        elif stable == "sword_array":
            put_text("N", ARRAY_N)
            put_text("O", ARRAY_O)
            updated = remove_cell(updated, "S", source_row)
            touched.append("S")
            for column, value in (("F", 40), ("G", 20), ("H", 2), ("K", 1), ("L", 10)):
                put_number(column, value)
        elif stable == "phantom_sword_surge":
            put_text("N", SURGE_N)
            for column, value in (("E", 2), ("F", 60), ("G", 20), ("H", 3), ("K", 2), ("L", 12)):
                put_number(column, value)
            put_number("I", "1.5")
            put_number("V", "1.5")
        elif stable == "sword_flight":
            put_text("N", append_note(original.get("N"), FLIGHT_APPEND))
            for column, value in (("F", 100), ("G", 20), ("H", 3), ("K", 2), ("L", 10)):
                put_number(column, value)
        put_text("DV", append_note(original.get("DV"), NOTES[stable]))
        if stable not in ("sword_array", "phantom_sword_surge") and cell_map(updated).get("N") != original.get("N"):
            if stable != "sword_flight":
                raise SystemExit(f"N changed unexpectedly for {stable}")
        if stable != "sword_array" and cell_map(updated).get("O") != original.get("O"):
            raise SystemExit(f"O changed unexpectedly for {stable}")
        if stable != "sword_flight" and not (cell_map(updated).get("DV") or "").startswith(original.get("DV") or ""):
            raise SystemExit(f"DV prefix lost for {stable}")
        if stable == "sword_flight" and not (cell_map(updated).get("N") or "").startswith(original.get("N") or ""):
            raise SystemExit("Flight N prefix lost")
        updates[source_row] = updated
        changed_columns[stable] = touched

    pending_new = pending
    for source_row, updated in updates.items():
        old = located_row(pending_rows, source_row)
        if pending_new.count(old) != 1:
            raise SystemExit(f"Pending row {source_row} is not unique")
        pending_new = pending_new.replace(old, updated, 1)

    patch = {
        parts["待制作"]: pending_new.encode("utf-8"),
        parts["待制作原始数据"]: archive_new.encode("utf-8"),
        parts["数据表"]: lookup_new.encode("utf-8"),
        "xl/workbook.xml": workbook_new.encode("utf-8"),
        "xl/styles.xml": styles.encode("utf-8"),
    }
    for name, payload in patch.items():
        ET.fromstring(payload)
    return entries, data, patch, provenance, changed_columns


def MARKER_FORBIDDEN(row_xml):
    return "[2026-11-11确认" in cell_value_from_row(row_xml, "DV")


def cell_value_from_row(row_xml, column):
    return cell_map(row_xml).get(column, "")


def located_row(pending_rows, number):
    return pending_rows[number].group(0)


def write_package(entries, data, patch, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED) as book:
        for entry in entries:
            payload = patch.get(entry.filename, data[entry.filename])
            book.writestr(entry, payload)
    with zipfile.ZipFile(destination) as book:
        changed = [name for name in data if data[name] != book.read(name)]
    if set(changed) != set(patch):
        raise SystemExit("Unexpected changed parts: " + ",".join(sorted(changed)))



def ensure_wrap_style(styles):
    if styles.count("<xf ") != 32:
        raise SystemExit("Style count changed")
    marker = '</cellXfs>'
    added = (
        '<xf numFmtId="0" fontId="3" fillId="10" borderId="0" applyAlignment="1" '
        'pivotButton="0" quotePrefix="0" xfId="0"><alignment horizontal="center" '
        'vertical="center" wrapText="1"/></xf></cellXfs>'
    )
    styles = styles.replace(marker, added, 1)
    styles = re.sub(
        r'(<cellXfs[^>]*count=")(\d+)(")',
        lambda match: match.group(1) + str(int(match.group(2)) + 1) + match.group(3),
        styles,
        count=1,
    )
    if styles.count("<xf ") != 33:
        raise SystemExit("Wrapped style was not appended")
    return styles


def main():
    source = LIVE.read_bytes()
    if sha256(source) != BASELINE or sha256(BACKUP.read_bytes()) != BASELINE:
        raise SystemExit("Baseline changed before candidate build")
    entries, data, patch, provenance, changed = build(source)
    write_package(entries, data, patch, CANDIDATE)
    report = {
        "candidate": str(CANDIDATE),
        "candidateHash": sha256(CANDIDATE.read_bytes()),
        "baseline": BASELINE,
        "collectedAt": COLLECT_TIME,
        "provenance": provenance,
        "changedColumns": changed,
    }
    (ROOT / "candidate-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"candidateHash": report["candidateHash"], "rows": provenance}, ensure_ascii=False))


if __name__ == "__main__":
    main()
