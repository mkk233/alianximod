# -*- coding: utf-8 -*-
"""Move rule_surface_anchor from 制作中 to 待验收 without touching the locked live workbook."""
import ctypes
import hashlib
import html
import json
import re
import zipfile
from ctypes import wintypes
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "术式批量制作" / "待制作-20261031"
live = root / "docs" / "术式制作表.xlsx"
output = batch / "surface-anchor-v2-writeback-candidate-preserved.xlsx"
snapshot_path = batch / "surface-anchor-v2-requirements-snapshot-20261114.json"
script_path = batch / "surface-anchor-v2-writeback-20261114.py"
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
n_pending = "视线内第一个向上承托面上出现一个悬空倒三角。失败则整组不释放"
o_pending = "assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png"
dv_addition = (
    "[阶段 2026-11-14] 用户确认采用地表定位 v2 棱面像素倒三角（原话：「你先继续，我忘了地面定位之前你给出的版本我还没说采用，之前那个版本的效果可以接入」）。正式归档 docs/资源库/特效/标记/地表定位_倒三角_v2/。运行贴图 assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png 与确认版竖条逐字节一致（SHA256 4434BDC7B0311E863277E9BD23761139E77489AD6B548FBB48AC202C4FB35A20），同目录 surface_anchor.png.mcmeta 为 blur=false、clamp=true。统一编译 20261114-compile-surface-anchor-v2-02 于 2026-11-14 04:54:01.291 开始、04:54:14.077 结束，晚于最后相关写入 BatchCarrierVisual.java 2026-11-14 04:48:11.855，退出码 0；:compileJava 为 UP-TO-DATE，四个目标 class 已于 04:48:47至04:48:48 生成，本次未重跑 javac。串台修复后仅在 needsSurface 且选点成功时于落点播一次。本阶段额度已用完（设计 2/2、实现 3/3），返修结果未追加复审。待用户游戏实测。需求快照 docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-requirements-snapshot-20261114.json。\n"
    "待验收 | 待制作-20261031 | docs/开发记录/术式批量制作/待制作-20261031/记录.md"
)
expected_n = (
    "沿准星选择有遮挡约束的有效地表，给出表面上方能容纳完整攻击本体的锚点。它明确选地面，不同于在目标附近空间取点的落点指定。不把弹丸变成落物，不代替垂落或喷涌。没有合法表面或足够空间时拒绝该次释放，不隔墙找点，不把攻击塞进实心方块。用于向上喷发时，锚点留出本体与支撑面的间距，避免出生就撞地。出生时的支撑关系不是一次新的命中接续。\n"
    "灼地可改用这个锚点。火山喷发以它作为喷口。不另建组合行。"
)
expected_o = "准星落在可见地表上的锚点标记，位置在表面上方，能看出不是隔墙选点。"


def fail(message):
    raise SystemExit(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()


def read_shared(path):
    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    kernel.CreateFileW.argtypes = [
        wintypes.LPCWSTR, wintypes.DWORD, wintypes.DWORD, wintypes.LPVOID,
        wintypes.DWORD, wintypes.DWORD, wintypes.HANDLE,
    ]
    kernel.CreateFileW.restype = wintypes.HANDLE
    kernel.GetFileSizeEx.argtypes = [wintypes.HANDLE, ctypes.POINTER(ctypes.c_longlong)]
    kernel.GetFileSizeEx.restype = wintypes.BOOL
    kernel.ReadFile.argtypes = [
        wintypes.HANDLE, wintypes.LPVOID, wintypes.DWORD,
        ctypes.POINTER(wintypes.DWORD), wintypes.LPVOID,
    ]
    kernel.ReadFile.restype = wintypes.BOOL
    kernel.CloseHandle.argtypes = [wintypes.HANDLE]
    kernel.CloseHandle.restype = wintypes.BOOL
    handle = kernel.CreateFileW(str(path), 0x80000000, 0x00000001 | 0x00000002 | 0x00000004, None, 3, 0x80, None)
    invalid = ctypes.c_void_p(-1).value
    if handle is None or handle == invalid:
        fail("Shared read failed with Win32 " + str(ctypes.get_last_error()))
    try:
        size = ctypes.c_longlong()
        if not kernel.GetFileSizeEx(handle, ctypes.byref(size)):
            fail("GetFileSizeEx failed with Win32 " + str(ctypes.get_last_error()))
        total = int(size.value)
        if total < 0 or total > 0xFFFFFFFF:
            fail("Unexpected workbook size " + str(total))
        buffer = ctypes.create_string_buffer(total)
        read = wintypes.DWORD()
        if not kernel.ReadFile(handle, buffer, total, ctypes.byref(read), None) or int(read.value) != total:
            fail("ReadFile failed with Win32 " + str(ctypes.get_last_error()))
        return buffer.raw
    finally:
        kernel.CloseHandle(handle)


def rows_of(sheet_xml):
    return re.findall(
        r"<(?:[\w]+:)?row\b[^>]*/>|<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>",
        sheet_xml,
    )


def row_number(row_xml):
    return int(re.search(r'\br="(\d+)"', row_xml).group(1))


def cell_xml(row_xml, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c\b[^>]*\br="{column}{number}"', row_xml))
    if len(matches) != 1:
        fail(f"Cell {column}{number} count is {len(matches)}")
    start = matches[0].start()
    opener_end = row_xml.find(">", start)
    if row_xml[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</(?:[\w]+:)?c>", row_xml[opener_end:])
        if not close:
            fail(f"Cell {column}{number} is not closed")
        end = opener_end + close.end()
    return row_xml[start:end], start, end


def cell_text(cell):
    inline = re.search(r"<(?:[\w]+:)?is\b[^>]*>([\s\S]*)</(?:[\w]+:)?is>", cell)
    if inline:
        nodes = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", inline.group(1))
        if len(nodes) != 1:
            fail(f"Inline cell text node count is {len(nodes)}")
        return html.unescape(nodes[0])
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if value:
        return html.unescape(value.group(1))
    return None


def replace_text(cell, column, number, new_text):
    escaped = html.escape(new_text, quote=False)
    if len(re.findall(r"<(?:[\w]+:)?t\b", cell)) != 1:
        fail(f"Cell {column}{number} does not have exactly one text node")
    updated, count = re.subn(
        r"(<(?:[\w]+:)?t\b[^>]*>)[\s\S]*(</(?:[\w]+:)?t>)",
        lambda match: match.group(1) + escaped + match.group(2),
        cell,
        count=1,
    )
    if count != 1 or cell_text(updated) != new_text:
        fail(f"Cell {column}{number} text was not written cleanly")
    return updated


def set_cell(row_xml, column, number, new_text):
    cell, start, end = cell_xml(row_xml, column, number)
    return row_xml[:start] + replace_text(cell, column, number, new_text) + row_xml[end:]


def renumber(row_xml, old, new):
    updated, row_count = re.subn(
        rf'(<row\b[^>]*\br="){old}(")',
        lambda match: match.group(1) + str(new) + match.group(2),
        row_xml,
        count=1,
    )
    updated, ref_count = re.subn(
        rf'\br="([A-Z]+){old}"',
        lambda match: 'r="' + match.group(1) + str(new) + '"',
        updated,
    )
    if row_count != 1 or ref_count < 1 or row_number(updated) != new:
        fail(f"Row {old} did not become {new} cleanly")
    if re.search(rf'\br="{old}\b"', updated) or re.search(rf'\br="[A-Z]+{old}(?!\d)"', updated):
        fail(f"Row {old} left a stale reference while becoming {new}")
    return updated


def replace_once(xml, old, new, label):
    count = xml.count(old)
    if count != 1:
        fail(f"{label} count is {count}")
    return xml.replace(old, new, 1)


def sheet_ids(sheet_xml):
    found = []
    for row_xml in rows_of(sheet_xml):
        number = row_number(row_xml)
        if number < 3 or f'r="B{number}"' not in row_xml:
            continue
        code = cell_text(cell_xml(row_xml, "B", number)[0])
        if code and code.strip():
            found.append((code.strip(), number, row_xml))
    return found


def row_values(row_xml, number):
    values = {}
    for match in re.finditer(r"<(?:[\w]+:)?c\b[^>]*/>|<(?:[\w]+:)?c\b[^>]*>[\s\S]*?</(?:[\w]+:)?c>", row_xml):
        cell = match.group(0)
        ref = re.search(r'\br="([A-Z]+)\d+"', cell)
        if not ref:
            fail("Cell reference missing")
        values[ref.group(1)] = cell_text(cell)
    return values


def column_name(index):
    name = ""
    while index:
        index, remainder = divmod(index - 1, 26)
        name = chr(65 + remainder) + name
    return name


def full_row(values):
    columns = [column_name(index) for index in range(1, 145)]
    if columns[0] != "A" or columns[-1] != "EN" or len(columns) != 144:
        fail("A:EN column span is not 144")
    return {column: values.get(column) for column in columns}


def row_without_text(row_xml):
    return re.sub(r"(<(?:[\w]+:)?t\b[^>]*>)[\s\S]*?(</(?:[\w]+:)?t>)", r"\1\2", row_xml)


def validation_covers(xml, row):
    uncovered = []
    for ref in re.findall(r'<dataValidation\b[^>]*sqref="([^"]+)"', xml):
        for part in ref.split():
            bounds = re.match(r"([A-Z]+)(\d+):([A-Z]+)(\d+)$", part)
            if bounds and int(bounds.group(2)) <= row <= int(bounds.group(4)):
                continue
            if bounds and int(bounds.group(2)) <= row and int(bounds.group(4)) < row:
                uncovered.append(part)
    if uncovered:
        fail(f"Validation does not cover row {row}: {uncovered}")


live_bytes = read_shared(live)
live_stat = live.stat()
baseline = sha256(live_bytes)
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"surface-anchor-v2-writeback-术式制作表-{stamp}-1.xlsx"
if backup.exists() or output.exists() or snapshot_path.exists():
    fail("Output path already exists")
backup.parent.mkdir(parents=True, exist_ok=True)
backup.write_bytes(live_bytes)
if sha256(backup.read_bytes()) != baseline or backup.stat().st_size != len(live_bytes):
    fail("Backup hash mismatch")

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original_parts = {info.filename: archive.read(info.filename) for info in order}

if original_parts["xl/worksheets/sheet6.xml"].decode("utf-8").count("rule_surface_anchor") != 0:
    fail("Resource detail unexpectedly contains rule_surface_anchor")
archive_xml = original_parts["xl/worksheets/sheet7.xml"].decode("utf-8")
archive_hits = [item for item in sheet_ids(archive_xml) if item[0] == "rule_surface_anchor"]
if len(archive_hits) != 1 or archive_hits[0][1] != 14:
    fail(f"Archive row mismatch: {[(item[0], item[1]) for item in archive_hits]}")

sheet3 = original_parts[status_parts["制作中"]].decode("utf-8")
sheet4 = original_parts[status_parts["待验收"]].decode("utf-8")
ids3 = sheet_ids(sheet3)
ids4 = sheet_ids(sheet4)
anchor = [item for item in ids3 if item[0] == "rule_surface_anchor"]
if len(anchor) != 1 or anchor[0][1] != 3 or any(item[0] == "rule_surface_anchor" for item in ids4):
    fail(f"rule_surface_anchor location mismatch: {[(item[0], item[1]) for item in anchor]}")
original_counts = {
    title: len(sheet_ids(original_parts[name].decode("utf-8")))
    for title, name in status_parts.items()
}
if original_counts != {"已完成": 37, "待制作": 0, "制作中": 24, "待验收": 17}:
    fail(f"Unexpected source counts: {original_counts}")

source_number = 3
source = anchor[0][2]
if "<f" in source:
    fail("rule_surface_anchor row contains a formula")
source_values = row_values(source, source_number)
if source_values.get("A") != "地表定位" or source_values.get("B") != "rule_surface_anchor":
    fail("Anchor identity mismatch")
old_n = source_values.get("N")
old_o = source_values.get("O")
old_dv = source_values.get("DV")
if old_n != expected_n or old_o != expected_o:
    fail(f"Making N/O changed before conversion: {len(old_n or '')} {len(old_o or '')}")
if not old_dv or not old_dv.startswith("现有链上角色只有「落点指定」") or len(old_dv) != 1284:
    fail(f"Making DV prefix is not intact: {len(old_dv or '')}")
if not old_dv.endswith("详见 docs/开发记录/术式批量制作/待制作-20261031/记录.md。"):
    fail("Making DV suffix changed")
if "[阶段 2026-11-14]" in old_dv or "待验收 | 待制作-20261031" in old_dv:
    fail("Making DV already contains the acceptance marker")

archive_values = row_values(archive_hits[0][2], 14)
if archive_values.get("N") != old_n or archive_values.get("O") != old_o:
    fail("Archive N/O does not match the original making demand")
if not old_dv.startswith(archive_values.get("DV") or "\0"):
    fail("Archive DV is not the preserved prefix")

content_rows4 = [number for _code, number, _row in ids4]
target_number = max(content_rows4) + 1
if target_number != 24 or any(row_number(row) == target_number for row in rows_of(sheet4)):
    fail(f"Expected empty target row 24, found {target_number}")
edited = set_cell(source, "N", source_number, n_pending)
edited = set_cell(edited, "O", source_number, o_pending)
edited = set_cell(edited, "DV", source_number, old_dv + "\n" + dv_addition)
moved = renumber(edited, source_number, target_number)
if cell_text(cell_xml(moved, "B", target_number)[0]) != "rule_surface_anchor":
    fail("Moved id changed")
if cell_text(cell_xml(moved, "N", target_number)[0]) != n_pending:
    fail("Moved N mismatch")
if cell_text(cell_xml(moved, "O", target_number)[0]) != o_pending:
    fail("Moved O mismatch")
moved_dv = cell_text(cell_xml(moved, "DV", target_number)[0])
if moved_dv != old_dv + "\n" + dv_addition or not moved_dv.startswith(old_dv):
    fail("Moved DV prefix was not preserved")
if len(re.findall(r"<c\b", moved)) != len(re.findall(r"<c\b", source)):
    fail("Moved row cell count changed")
if row_without_text(moved) != row_without_text(renumber(source, source_number, target_number)):
    fail("Moved row changed outside N/O/DV text")

rows3 = rows_of(sheet3)
source_matches = [row for row in rows3 if row_number(row) == source_number]
if source_matches != [source]:
    fail("Source row is not unique")
sheet3_out = sheet3.replace(source, "", 1)
if sheet3.count(source) != 1 or "rule_surface_anchor" in cell_text(cell_xml(next(row for row in rows_of(sheet3_out) if row_number(row) == 24), "B", 24)[0]):
    fail("Making source row was not removed cleanly")
sheet4_out = replace_once(sheet4, "</sheetData>", moved + "</sheetData>", "待验收 sheetData close")
sheet4_out = replace_once(sheet4_out, 'ref="A1:EN23"', 'ref="A1:EN24"', "待验收 dimension")
if 'ref="A1:EN27"' not in sheet3_out:
    fail("制作中 dimension changed unexpectedly")
validation_covers(sheet4_out, target_number)

changed = {
    status_parts["制作中"]: sheet3_out.encode("utf-8"),
    status_parts["待验收"]: sheet4_out.encode("utf-8"),
}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        clone = zipfile.ZipInfo(filename=info.filename, date_time=info.date_time)
        clone.compress_type = info.compress_type
        clone.flag_bits = info.flag_bits
        clone.external_attr = info.external_attr
        clone.internal_attr = info.internal_attr
        clone.create_system = info.create_system
        clone.extra = info.extra
        clone.comment = info.comment
        dst.writestr(clone, changed.get(info.filename, src.read(info.filename)))

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    if src.namelist() != dst.namelist():
        fail("Zip part list changed")
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    allowed = [status_parts["制作中"], status_parts["待验收"]]
    if different != allowed:
        fail(f"Unexpected part changes: {different}")
    counts = {}
    located = {}
    all_ids = []
    originals = {}
    for title, name in status_parts.items():
        ids = sheet_ids(dst.read(name).decode("utf-8"))
        counts[title] = len(ids)
        originals[title] = sheet_ids(src.read(name).decode("utf-8"))
        for code, number, _row in ids:
            located.setdefault(code, []).append((title, number))
            all_ids.append(code)
    if counts != {"已完成": 37, "待制作": 0, "制作中": 23, "待验收": 18}:
        fail(f"Counts {counts}")
    if len(all_ids) != 78 or len(set(all_ids)) != 78:
        fail("Status ids are not unique")
    if located.get("rule_surface_anchor") != [("待验收", target_number)]:
        fail(f"rule_surface_anchor at {located.get('rule_surface_anchor')}")
    for title in ("已完成", "待制作"):
        if originals[title] != sheet_ids(dst.read(status_parts[title]).decode("utf-8")):
            fail(f"{title} changed")
    making_ids = sheet_ids(dst.read(status_parts["制作中"]).decode("utf-8"))
    if any(code == "rule_surface_anchor" for code, _number, _row in making_ids):
        fail("制作中 still contains rule_surface_anchor")
    for unchanged in (
        "xl/worksheets/sheet1.xml",
        "xl/worksheets/sheet2.xml",
        "xl/worksheets/sheet5.xml",
        "xl/worksheets/sheet6.xml",
        "xl/worksheets/sheet7.xml",
        "xl/workbook.xml",
        "xl/styles.xml",
        "xl/comments/comment3.xml",
        "xl/comments/comment4.xml",
    ):
        if dst.read(unchanged) != src.read(unchanged):
            fail(f"{unchanged} changed")
    expected_codes = [(code, number) for code, number, _row in originals["制作中"] if code != "rule_surface_anchor"]
    if [(code, number) for code, number, _row in making_ids] != expected_codes:
        fail("制作中 order changed beyond removing rule_surface_anchor")
    original_making = {code: row for code, _number, row in originals["制作中"]}
    for code, number, actual in making_ids:
        if actual != original_making[code] or row_number(actual) != number:
            fail(f"制作中 row for {code} changed")
    pending_out = sheet_ids(dst.read(status_parts["待验收"]).decode("utf-8"))
    if [(code, number, row) for code, number, row in pending_out if code != "rule_surface_anchor"] != originals["待验收"]:
        fail("待验收 existing rows changed")
    reopened = next(row for code, number, row in pending_out if code == "rule_surface_anchor")
    if cell_text(cell_xml(reopened, "DV", target_number)[0]) != old_dv + "\n" + dv_addition:
        fail("Reopened DV mismatch")
    if cell_text(cell_xml(reopened, "O", target_number)[0]) != o_pending:
        fail("Reopened O mismatch")
    if cell_text(cell_xml(reopened, "N", target_number)[0]) != n_pending:
        fail("Reopened N mismatch")
    if cell_text(cell_xml(reopened, "A", target_number)[0]) != "地表定位":
        fail("Reopened name mismatch")

snapshot = {
    "schema": "surface-anchor-v2-requirements-snapshot-20261114",
    "batchId": "待制作-20261031",
    "taskId": "rule_surface_anchor_v2_sheet_write",
    "attemptId": "20261114-sheet-surface-anchor-v2-01",
    "techniqueId": "rule_surface_anchor",
    "source": {
        "sheet": "制作中",
        "row": 3,
        "columnsAtoEN": 144,
        "presentColumns": sorted(source_values, key=lambda column: [column_name(index) for index in range(1, 145)].index(column)),
        "valuesAtoEN": full_row(source_values),
        "nBefore": old_n,
        "oBefore": old_o,
        "dvBefore": old_dv,
    },
    "archive": {
        "sheet": "待制作原始数据",
        "row": 14,
        "valuesAtoEN": full_row(archive_values),
        "note": "首次待制作快照，本次不覆盖、不转换",
    },
    "resourceDetail": "资源状态明细无 rule_surface_anchor，原样未动",
    "userConfirmation": {
        "date": "2026-11-14",
        "verbatim": "你先继续，我忘了地面定位之前你给出的版本我还没说采用，之前那个版本的效果可以接入",
        "subject": "地表定位_倒三角_v2 二维与真实 Blockbench 三维视口预览",
    },
    "approvedVersion": {
        "parameters": "魔力 0，冷却 0，复杂度 1，装配消耗 1，等级上限 20；释放种类规则，链上角色地表定位，无弹道；视线 32 格，失败整组拒绝、不扣费不冷却",
        "visual": "地表定位_倒三角_v2_20261114",
    },
    "displayCopy": {
        "zhCn": {
            "technique.xueximod.rule_surface_anchor": None,
            "technique.xueximod.rule_surface_anchor.desc": None,
            "gui.xueximod.role.seek_surface": "承托面",
            "gui.xueximod.batch.surface_rejected": "视线里没有可用的承托面，这次没有释放",
        },
        "techniqueCard": "视线内第一个向上承托面。失败则整组不释放",
        "techniqueCardSource": "src/main/java/com/bslx/XueXiMod/shu/ModTechniques.java RULE_SURFACE_ANCHOR；zh_cn.json 没有覆盖这句技术卡文案",
        "alignment": "待验收 N 沿技术卡原句补上已接入的悬空倒三角，失败句与技术卡一致；失败提示仍是 zh_cn 的 surface_rejected，本次不改语言文件",
    },
    "formalArchive": "docs/资源库/特效/标记/地表定位_倒三角_v2/",
    "runtimeResources": [
        {
            "path": "assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png",
            "bytes": 4741,
            "sha256": "4434BDC7B0311E863277E9BD23761139E77489AD6B548FBB48AC202C4FB35A20",
        },
        {
            "path": "assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png.mcmeta",
            "bytes": 64,
            "sha256": "B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A",
        },
    ],
    "converted": {
        "targetSheet": "待验收",
        "targetRow": target_number,
        "n": n_pending,
        "o": o_pending,
        "dvPrefix": old_dv,
        "dvAddition": dv_addition,
    },
    "configuration": {
        "stableId": "rule_surface_anchor",
        "standard": [
            {"key": "cooldownTicks", "default": 0, "unit": "tick", "range": [0, 1200000]},
            {"key": "manaCost", "default": 0, "range": [0, 1000000]},
        ],
        "damageMultiplier": "无；基础伤害 0，不在伤害倍率名单",
        "special": "ListedRelease.declareSpecialBalances 只登记陨岩、火苗和热波。本行 lifetimeTicks=1、speed=0、bodySeedBase=0，读取时在进入专项配置前返回，没有专项键。",
        "assemblyCost": {"value": 1, "configurable": False, "reason": "装配消耗首版不开放"},
        "visualConstants": {
            "halfSpan": 0.40,
            "size": "0.80×0.80",
            "y": [0.04, 0.84],
            "lifetimeTicks": 16,
            "configurable": False,
        },
        "consumption": "下一次服务器启动或重新打开单机世界时读取标准冷却与法力；选点失败在扣费和冷却前返回",
    },
    "reviewBudget": "设计 2/2、实现 3/3 已用完，返修结果未追加复审",
    "baselineSha256": baseline,
    "baselineBytes": len(live_bytes),
    "baselineMtimeMs": int(live_stat.st_mtime * 1000),
    "backup": str(backup),
    "candidate": str(output),
    "script": str(script_path),
    "liveReplacement": "本脚本只构建候选副本，不替换正在打开的源表",
}
snapshot_path.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({
    "baseline": baseline,
    "baselineBytes": len(live_bytes),
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).strftime("%Y-%m-%d %H:%M:%S"),
    "backup": str(backup),
    "backupSha256": sha256(backup.read_bytes()),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "candidateBytes": output.stat().st_size,
    "snapshot": str(snapshot_path),
    "snapshotSha256": sha256(snapshot_path.read_bytes()),
    "counts": counts,
    "changedParts": different,
    "targetRow": target_number,
    "nBeforeLength": len(old_n),
    "oBeforeLength": len(old_o),
    "dvPrefixLength": len(old_dv),
    "presentColumns": len(source_values),
}, ensure_ascii=False))
