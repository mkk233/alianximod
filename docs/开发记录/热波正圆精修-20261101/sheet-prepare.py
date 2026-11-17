# -*- coding: utf-8 -*-
"""Build the fire_shock_ring candidate workbook and index candidates. Does not replace live files."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(r"D:\alianximod")
BATCH = ROOT / "docs" / "开发记录" / "热波正圆精修-20261101"
LIVE = ROOT / "docs" / "术式制作表.xlsx"
README = ROOT / "docs" / "资源库" / "README.md"
V1_README = ROOT / "docs" / "资源库" / "特效" / "火焰" / "热波_v1" / "README.md"
V2_DIR = ROOT / "docs" / "资源库" / "特效" / "火焰" / "热波_水平正圆_v2"
V2_README = V2_DIR / "README.md"
V2_MANIFEST = V2_DIR / "manifest.json"
TARGET = "fire_shock_ring"
SOURCE_ROW = 7
NEW_ROW = 8
NEW_N = "在锚点周围产生一次环形冲击，中心留空，只结算一次。"
NEW_O = (
    "热波_水平正圆_v2 / heat-circle-v2\n"
    "正式源：docs/资源库/特效/火焰/热波_水平正圆_v2/\n"
    "运行贴图：textures/effect/shock_ring_v1/shock_ring.png"
    "（文件名兼容旧引用，内容为 heat-circle-v2）"
)
DV_APPEND = (
    "2026-11-01 用户批准 heat-circle-v2：“按这版接入，同时把范围扩展到这个特效范围”；"
    "最终选择“保留中心1.5格空区，只确保外缘对齐”。外半径 5 格，内半径 1.5 格，一次结算。"
    "运行贴图已替换。.\\gradlew.bat compileJava --offline --no-daemon 于 2026-11-01 23:42:11–23:42:27 退出码 0，"
    "compileJava 为 UP-TO-DATE。上文 2026-10-31 待统一编译及客户端占用是被本次结果覆盖的历史，不是当前阻塞。"
    "本阶段额度已用完，返修结果未追加复审。游戏待用户实测。\n"
    "待验收 | 热波正圆精修-20261101 | docs/开发记录/热波正圆精修-20261101/调度记录.md"
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
OLD_BATCH_END = "喷涌、错时、命中接续没有新素材。尚未复制进游戏资源，尚未游戏实测，不追加设计复审。"
NEW_BATCH_END = (
    "喷涌、错时、命中接续没有新素材。除热波外，这些暂用素材尚未复制进游戏资源，尚未游戏实测，不追加设计复审。"
    "热波 v1 曾已接入运行贴图，本次已被下方水平正圆 v2 替换，上面旧的“尚未复制进游戏资源”不再是热波现状。"
)
NEW_INDEX_BULLET = (
    "\n- [热波 · 水平正圆 v2（已确认并接入运行贴图）](特效/火焰/热波_水平正圆_v2/README.md)："
    "用户 2026-11-01 确认。原话“按这版接入，同时把范围扩展到这个特效范围”；"
    "“保留中心1.5格空区，只确保外缘对齐”。内容版本 heat-circle-v2。"
    "正式目录 docs/资源库/特效/火焰/热波_水平正圆_v2/。"
    "运行文件名仍是 textures/effect/shock_ring_v1/shock_ring.png，当前字节是新竖条。"
    "外半径 5 格，内半径 1.5 格；载体边长 10 格是直径，不是半径 10。一次伤害。"
    ".\\gradlew.bat compileJava --offline --no-daemon 于 2026-11-01 23:42:11–23:42:27 退出码 0，"
    "compileJava 为 UP-TO-DATE。待用户游戏实测，未标已完成，未追加复审。"
    "旧目录 热波_v1 保留为历史。\n"
)
OLD_V1_STATUS = (
    "采用状态：用户 2026-10-31 原话“先保留这个版本，额度不够用了暂时，之后再重置，暂时直接按照这个版本执行”。"
    "这是暂用归档，不是游戏实测通过，也没有复制进 `src/main/resources`。"
)
NEW_V1_STATUS = (
    "采用状态：用户 2026-10-31 原话“先保留这个版本，额度不够用了暂时，之后再重置，暂时直接按照这个版本执行”。"
    "这是暂用历史归档，不是游戏实测通过。该版源文件曾接入运行贴图 "
    "textures/effect/shock_ring_v1/shock_ring.png，2026-11-01 已被水平正圆 v2 的字节替换。"
    "本目录源文件保留，不再是当前运行内容。"
)
V2_STATUS_ADD = "制作表已把 fire_shock_ring 从制作中迁入待验收，未标已完成。"
OLD_MANIFEST_STATUS = "user_confirmed_archived_runtime_replaced_compileJava_up_to_date_exit_0_pending_playtest"
NEW_MANIFEST_STATUS = "user_confirmed_archived_runtime_replaced_compileJava_up_to_date_exit_0_sheet_pending_acceptance_pending_playtest"


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


def art_hashes(directory, skip_names):
    found = {}
    for path in sorted(directory.rglob("*")):
        if path.is_file() and path.name not in skip_names:
            found[str(path.relative_to(ROOT)).replace("\\", "/")] = sha256_file(path)
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
    if len(all_ids) != len(set(all_ids)) or counts != {"已完成": 40, "待制作": 0, "制作中": 15, "待验收": 5}:
        fail(f"状态页计数异常 {counts}")
    hits = located.get(TARGET, [])
    if len(hits) != 1 or hits[0][0] != "制作中" or hits[0][1] != SOURCE_ROW:
        fail(f"fire_shock_ring 位置异常: {[(item[0], item[1]) for item in hits]}")
    pending_numbers = [number for _code, number, _row in ids_of(pending_xml, shared)]
    if pending_numbers != [3, 4, 5, 6, 7]:
        fail(f"待验收行号异常 {pending_numbers}")
    source_row = hits[0][2]
    original = column_values(source_row, SOURCE_ROW, shared)
    if not original["N"] or not original["O"] or not original["DV"]:
        fail("缺少 N/O/DV")
    if "heat-circle-v2" in original["DV"]:
        fail("本次进度已经写过")
    if not original["N"].startswith("在释放锚点周围产生一次环形冲击"):
        fail("功能描述不是热波原文")
    if "半径10" in original["N"] or "半径 10" in original["N"]:
        fail("原文已把半径写成 10，停止")
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
    detail_ids = [code for code, _number, _row in ids_of(detail_xml, shared)]
    target_index = shared.index(TARGET)
    if detail_ids.count(TARGET) or f"<v>{target_index}</v>" in detail_xml:
        fail("资源状态明细已有 fire_shock_ring，停止以免改配置语义")

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
    if read_at(moved, "N", NEW_ROW, shared) != NEW_N or read_at(moved, "O", NEW_ROW, shared) != NEW_O:
        fail("待验收展示没有写上")
    if not read_at(moved, "DV", NEW_ROW, shared).startswith(original["DV"] + "\n"):
        fail("原备注丢失")
    if "半径10" in NEW_N or "半径 10" in NEW_O:
        fail("新展示把外半径写成了 10")

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
    making_out = replace_once(making_out, 'ref="A1:EN17"', 'ref="A1:EN16"', "制作中维度")
    making_out = replace_once(making_out, 'ref="A2:EN7"', 'ref="A2:EN6"', "制作中筛选")
    pending_rows = rows_of(pending_xml) + [moved]
    pending_out = replace_data(pending_xml, pending_rows)
    pending_out = replace_once(pending_out, 'ref="A1:EN7"', 'ref="A1:EN8"', "待验收维度")
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    workbook = replace_once(workbook, "制作中!$A$2:$EN$17", "制作中!$A$2:$EN$16", "制作中筛选名称")
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
        if dst.read("xl/sharedStrings.xml") != parts["xl/sharedStrings.xml"]:
            fail("共享字符串被改动")
        check_making = dst.read(PARTS["制作中"]).decode("utf-8")
        check_pending = dst.read(PARTS["待验收"]).decode("utf-8")
        making_ids = ids_of(check_making, shared)
        pending_ids = ids_of(check_pending, shared)
        expected_making = [code for code, number, _row in ids_of(making_xml, shared) if number != SOURCE_ROW]
        if [code for code, _number, _row in making_ids] != expected_making:
            fail("制作中剩余编号或顺序不对")
        if [code for code, _number, _row in pending_ids][-1] != TARGET or len(pending_ids) != 6:
            fail("待验收没有把热波放在末行")
        for code, number, row in ids_of(making_xml, shared):
            if number < SOURCE_ROW:
                kept = [item for item_code, _item_number, item in making_ids if item_code == code][0]
                if kept != row:
                    fail(f"{code} 在删除行之前却被改动")
        written = [row for code, _number, row in pending_ids if code == TARGET][0]
        if read_at(written, "B", NEW_ROW, shared) != TARGET:
            fail("待验收编号不对")
        if read_at(written, "DW", NEW_ROW, shared) != original["DW"] or read_at(written, "DY", NEW_ROW, shared) != original["DY"]:
            fail("资源组首条被改动")
        outside_making = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making_xml)
        outside_check = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_making)
        outside_making = outside_making.replace('ref="A1:EN17"', 'ref="A1:EN16"').replace('ref="A2:EN7"', 'ref="A2:EN6"')
        if outside_making != outside_check:
            fail("制作中筛选、验证或合并区被额外改动")
        outside_pending = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", pending_xml).replace('ref="A1:EN7"', 'ref="A1:EN8"')
        outside_pending_check = re.sub(r"<sheetData>[\s\S]*</sheetData>", "", check_pending)
        if outside_pending != outside_pending_check:
            fail("待验收验证或筛选被额外改动")
        after_counts = {}
        after_ids = []
        for title in STATUS:
            page = [code for code, _number, _row in ids_of(dst.read(PARTS[title]).decode("utf-8"), shared)]
            after_counts[title] = len(page)
            after_ids.extend(page)
        if after_counts != {"已完成": 40, "待制作": 0, "制作中": 14, "待验收": 6} or len(after_ids) != len(set(after_ids)):
            fail(f"迁移后计数异常 {after_counts}")

    def read_utf8(path):
        return path.read_bytes().decode("utf-8")

    def write_utf8(path, text):
        path.write_bytes(text.encode("utf-8"))

    readme = read_utf8(README)
    if readme.count(OLD_BATCH_END) != 1 or "热波 · 水平正圆 v2（已确认并接入运行贴图）" in readme:
        fail("总索引热波段落不唯一或已经写过")
    if "升温 · 贴身炽热 v2（已采用）" not in readme or "燃烧 · 空间挂载 v1（已确认）" not in readme:
        fail("升温或燃烧索引条目缺失，停止")
    readme_new = readme.replace(OLD_BATCH_END, NEW_BATCH_END + NEW_INDEX_BULLET, 1)
    if readme_new.count("热波 · 水平正圆 v2（已确认并接入运行贴图）") != 1:
        fail("新热波索引没有正好一条")
    if "升温 · 贴身炽热 v2（已采用）" not in readme_new or "燃烧 · 空间挂载 v1（已确认）" not in readme_new:
        fail("索引改动碰到了其他条目")
    readme_candidate = candidate_dir / "资源库-README-候选.md"
    write_utf8(readme_candidate, readme_new)

    v1 = read_utf8(V1_README)
    if v1.count(OLD_V1_STATUS) != 1:
        fail("旧热波 v1 采用状态不唯一")
    v1_new = v1.replace(OLD_V1_STATUS, NEW_V1_STATUS, 1)
    v1_candidate = candidate_dir / "热波v1-README-候选.md"
    write_utf8(v1_candidate, v1_new)

    v2 = read_utf8(V2_README)
    marker = "javac 没有重新执行。"
    if v2.count(marker) != 1 or "迁入待验收" in v2:
        fail("新正式 README 状态锚点不唯一或已经写过")
    v2_new = v2.replace(marker, marker + V2_STATUS_ADD, 1)
    v2_candidate = candidate_dir / "热波v2-README-候选.md"
    write_utf8(v2_candidate, v2_new)

    manifest_text = read_utf8(V2_MANIFEST)
    manifest = json.loads(manifest_text)
    if manifest.get("status") != OLD_MANIFEST_STATUS:
        fail("新正式 manifest 状态不是预期原文")
    art_names = {
        "shock_ring_circle.aseprite",
        "shock_ring_circle-strip.png",
        "shock_ring_circle-frame10.png",
        "circle-2d-preview.gif",
        "热波_水平正圆_v2.bbmodel",
        "bb-top-animation.gif",
        "bb-oblique-animation.gif",
        "bb-top-still.png",
        "bb-oblique-still.png",
        "blockbench-app-proof.png",
    }
    art_before = {}
    for item in manifest.get("files", []):
        name = str(item.get("file", ""))
        if name not in art_names:
            continue
        digest = str(item.get("sha256", "")).lower()
        actual = hashlib.sha256((V2_DIR / name).read_bytes()).hexdigest()
        if actual != digest:
            fail(f"批准艺术哈希已变: {name}")
        art_before[name] = digest
    if set(art_before) != art_names:
        fail("批准艺术哈希条目不齐")
    if manifest_text.count(OLD_MANIFEST_STATUS) != 1:
        fail("manifest 状态字符串不唯一")
    manifest_new = manifest_text.replace(OLD_MANIFEST_STATUS, NEW_MANIFEST_STATUS, 1)
    readme_item = next(item for item in manifest.get("files", []) if item.get("file") == "README.md")
    old_readme_hash = readme_item["sha256"]
    new_readme_hash = hashlib.sha256(v2_new.encode("utf-8")).hexdigest()
    new_readme_bytes = str(len(v2_new.encode("utf-8")))
    manifest_new = manifest_new.replace(
        f'"file": "README.md",\n      "bytes": {readme_item["bytes"]},\n      "sha256": "{old_readme_hash}"',
        f'"file": "README.md",\n      "bytes": {new_readme_bytes},\n      "sha256": "{new_readme_hash}"',
        1,
    )
    for name, digest in art_before.items():
        if manifest_new.count(digest) != manifest_text.count(digest):
            fail(f"候选 manifest 改到了艺术哈希 {name}")
    manifest_candidate = candidate_dir / "热波v2-manifest-候选.json"
    write_utf8(manifest_candidate, manifest_new)

    v1_art = art_hashes(V1_README.parent, {"README.md"})
    v2_art = art_hashes(V2_DIR, {"README.md", "manifest.json"})

    cells = []
    for index in range(1, 145):
        column = col_name(index)
        cells.append({"column": column, "header": header.get(column), "text": original[column]})
    aen_path = snap_dir / f"变更前-AEN-{stamp}-{sequence}.json"
    nodv_path = snap_dir / f"变更前-NODV-{stamp}-{sequence}.json"
    visual_path = snap_dir / f"批准要求与资源映射-{stamp}-{sequence}.md"
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
        "pendingDisplay": {"N": NEW_N, "O": NEW_O, "dvAppend": DV_APPEND},
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    visual_path.write_text(
        "# fire_shock_ring 批准要求与资源映射\n\n"
        "用户 2026-11-01 确认 heat-circle-v2。原话：“按这版接入，同时把范围扩展到这个特效范围”。"
        "最终选择：“保留中心1.5格空区，只确保外缘对齐”。\n\n"
        "外半径 5 格，内半径 1.5 格。载体边长 10 格是直径，不是半径 10。一次结算。\n\n"
        "正式目录：docs/资源库/特效/火焰/热波_水平正圆_v2/\n\n"
        "运行贴图：textures/effect/shock_ring_v1/shock_ring.png，内容版本 heat-circle-v2，"
        "SHA-256 5959d47d14ea8a69394559b13f504686c620230a35505a765731cbb76bda6d0f。\n\n"
        "游戏内没有单独的 fire_shock_ring 描述文案，待验收 N 使用已实现行为的一句话。\n\n"
        "compileJava --offline --no-daemon 于 2026-11-01 23:42:11–23:42:27 退出码 0，compileJava 为 UP-TO-DATE。"
        "本阶段额度已用完，返修结果未追加复审。游戏尚未由用户实测。完整 N/O/DV 留在同目录变更前快照。\n",
        encoding="utf-8",
    )
    result = {
        "ready": True,
        "submitted": False,
        "baselineSha256": baseline,
        "baselineBytes": len(live_bytes),
        "baselineMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
        "backup": str(backup),
        "candidate": str(candidate),
        "candidateSha256": sha256_bytes(candidate.read_bytes()),
        "snapshots": {"aen": str(aen_path), "nodv": str(nodv_path), "mapping": str(visual_path)},
        "from": {"sheet": "制作中", "row": SOURCE_ROW},
        "to": {"sheet": "待验收", "row": NEW_ROW},
        "countsBefore": counts,
        "countsAfter": {"已完成": 40, "待制作": 0, "制作中": 14, "待验收": 6},
        "live": str(LIVE),
        "readme": str(README),
        "readmeSha256": sha256_file(README),
        "readmeCandidate": str(readme_candidate),
        "readmeCandidateSha256": sha256_file(readme_candidate),
        "v1Readme": str(V1_README),
        "v1ReadmeSha256": sha256_file(V1_README),
        "v1Candidate": str(v1_candidate),
        "v1CandidateSha256": sha256_file(v1_candidate),
        "v2Readme": str(V2_README),
        "v2ReadmeSha256": sha256_file(V2_README),
        "v2ReadmeCandidate": str(v2_candidate),
        "v2ReadmeCandidateSha256": sha256_file(v2_candidate),
        "v2Manifest": str(V2_MANIFEST),
        "v2ManifestSha256": sha256_file(V2_MANIFEST),
        "v2ManifestCandidate": str(manifest_candidate),
        "v2ManifestCandidateSha256": sha256_file(manifest_candidate),
        "v1ArtSha256": v1_art,
        "v2ArtSha256": v2_art,
        "manifestArtSha256": art_before,
    }
    (BATCH / "sheet-prepare-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"baseline": baseline, "candidate": result["candidateSha256"], "art": len(art_before)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
