# -*- coding: utf-8 -*-
"""Move fire_seed_sow from making row 7 to acceptance without rewriting unrelated workbook XML."""
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
output = batch / "seed-sow-v3-writeback-candidate-preserved.xlsx"
snapshot_path = batch / "seed-sow-v3-requirements-snapshot-20261115.json"
script_path = batch / "seed-sow-v3-writeback-20261115.py"
status_parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}
n_pending = "命中被接受后并入本体基数，基础产种10"
o_pending = "assets/xueximod/textures/effect/seed_sow_v3/seed_sow.png"
dv_addition = (
    "[美术进度 2026-11-15] 2026-11-11 用户确认“按照这版接入”。同版 v3 已接入运行贴图 assets/xueximod/textures/effect/seed_sow_v3/seed_sow.png（2776 字节，SHA256 A50EC7ED6A8434F50987A86036201DABFF3AB8A70CFF9D82A31CAC9F12B03EEE）及同目录 seed_sow.png.mcmeta（64 字节，blur=false、clamp=true，SHA256 B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A）。正式归档 docs/资源库/特效/火焰/种火_上窜_v3/。2026-11-15 补齐真实 Blockbench 模型 docs/资源库/过程归档/种火_上窜熄灭_v3_模型补档_20261115/seed_sow_v3_preview.bbmodel（804998 字节，SHA256 CE7AC65C0406AE3A338FBB5CD7AE98E70D005A245520C7B4603245715DB9852C），22:06:38.950+08 复开，43/43 贴图就绪；四张视口与 visual-handoff.md 同目录。模型留在示意归档，不进游戏。无目标参考边长 1.75 格，有目标按碰撞高度缩放；运行朝向仍是 CAMERA_PLANE，向镜头平移 0.08 格。侧图是冻结面，线框假人不代表 Forge 遮挡。旧 v1 保留为历史，不再作为运行贴图。本次不改运行参数，不提出新版本，不重复要求同版视觉确认。现行 src/main 1456 个文件、13766184 字节，树 SHA256 351eb181c4eb75b138b9370a8b2fce8caeaba71026d408a240c627f62bf408d4，与 2026-11-15 03:25:54.673–03:26:22.708 退出码 0 的 compileJava 输入相同；本次未改 src，未重跑 Gradle。未游戏实测，不能迁已完成。设计 2/2、实现 3/3 已用完，未追加复审。需求快照 docs/开发记录/术式批量制作/待制作-20261031/seed-sow-v3-requirements-snapshot-20261115.json。\n"
    "待验收 | 待制作-20261031 | docs/开发记录/术式批量制作/待制作-20261031/记录.md"
)
expected_n_sha = "0F82B940BCE07C2F698520C31F1C10ABBFB14AC8F33585D6CEC9B7FD3255B00B"
expected_o_sha = "18A102B22C06AF9040F51B590BB0D056494C87388840565D46131154AE7277DB"
expected_dv_sha = "70A5CD2E4CB9EA6DC0832B40CF2AE2039CC804CC7044816D2C0B0E915963101B"
expected_lengths = {"N": 288, "O": 25, "DV": 1090}
expected_baseline = "8CFA3C2599EC08A531B3624522551791E208D2425892930BC6C1616DAD9CE938"

def fail(message):
    raise SystemExit(message)

def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()

def read_shared(path):
    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    kernel.CreateFileW.argtypes = [wintypes.LPCWSTR, wintypes.DWORD, wintypes.DWORD, wintypes.LPVOID, wintypes.DWORD, wintypes.DWORD, wintypes.HANDLE]
    kernel.CreateFileW.restype = wintypes.HANDLE
    kernel.GetFileSizeEx.argtypes = [wintypes.HANDLE, ctypes.POINTER(ctypes.c_longlong)]
    kernel.GetFileSizeEx.restype = wintypes.BOOL
    kernel.ReadFile.argtypes = [wintypes.HANDLE, wintypes.LPVOID, wintypes.DWORD, ctypes.POINTER(wintypes.DWORD), wintypes.LPVOID]
    kernel.ReadFile.restype = wintypes.BOOL
    kernel.CloseHandle.argtypes = [wintypes.HANDLE]
    kernel.CloseHandle.restype = wintypes.BOOL
    handle = kernel.CreateFileW(str(path), 0x80000000, 0x00000001 | 0x00000002 | 0x00000004, None, 3, 0x80, None)
    if handle is None or handle == ctypes.c_void_p(-1).value:
        fail("Shared read failed with Win32 " + str(ctypes.get_last_error()))
    try:
        size = ctypes.c_longlong()
        if not kernel.GetFileSizeEx(handle, ctypes.byref(size)):
            fail("GetFileSizeEx failed with Win32 " + str(ctypes.get_last_error()))
        total = int(size.value)
        buffer = ctypes.create_string_buffer(total)
        read = wintypes.DWORD()
        if not kernel.ReadFile(handle, buffer, total, ctypes.byref(read), None) or int(read.value) != total:
            fail("ReadFile failed with Win32 " + str(ctypes.get_last_error()))
        return buffer.raw
    finally:
        kernel.CloseHandle(handle)

def rows_of(sheet_xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*/>|<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet_xml)

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
    escaped = html.escape(new_text, quote=False).replace("\n", "&#10;")
    if len(re.findall(r"<(?:[\w]+:)?t\b", cell)) != 1:
        fail(f"Cell {column}{number} does not have exactly one text node")
    updated, count = re.subn(r"(<(?:[\w]+:)?t\b[^>]*>)[\s\S]*(</(?:[\w]+:)?t>)", lambda match: match.group(1) + escaped + match.group(2), cell, count=1)
    if count != 1 or cell_text(updated) != new_text:
        fail(f"Cell {column}{number} text was not written cleanly")
    return updated

def set_cell(row_xml, column, number, new_text):
    cell, start, end = cell_xml(row_xml, column, number)
    return row_xml[:start] + replace_text(cell, column, number, new_text) + row_xml[end:]

def renumber(row_xml, old, new):
    updated, row_count = re.subn(rf'(<row\b[^>]*\br="){old}(")', lambda match: match.group(1) + str(new) + match.group(2), row_xml, count=1)
    updated, ref_count = re.subn(rf'\br="([A-Z]+){old}"', lambda match: 'r="' + match.group(1) + str(new) + '"', updated)
    if row_count != 1 or ref_count < 1 or row_number(updated) != new:
        fail(f"Row {old} did not become {new} cleanly")
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
            uncovered.append(part)
    if uncovered:
        fail(f"Validation does not cover row {row}: {uncovered}")

live_bytes = read_shared(live)
if sha256(live_bytes) != expected_baseline or len(live_bytes) != 179938:
    fail("Live workbook baseline changed before candidate build")
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"seed-sow-v3-writeback-术式制作表-{stamp}-1.xlsx"
if backup.exists() or output.exists() or snapshot_path.exists():
    fail("Output path already exists")
backup.parent.mkdir(parents=True, exist_ok=True)
backup.write_bytes(live_bytes)
if sha256(backup.read_bytes()) != expected_baseline:
    fail("Backup hash mismatch")

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original_parts = {info.filename: archive.read(info.filename) for info in order}

if original_parts["xl/worksheets/sheet6.xml"].decode("utf-8").count("fire_seed_sow") != 0:
    fail("Resource detail unexpectedly contains fire_seed_sow")
archive_xml = original_parts["xl/worksheets/sheet7.xml"].decode("utf-8")
archive_hits = [item for item in sheet_ids(archive_xml) if item[0] == "fire_seed_sow"]
if len(archive_hits) != 1 or archive_hits[0][1] != 18:
    fail(f"Archive row mismatch: {[(item[0], item[1]) for item in archive_hits]}")

sheet3 = original_parts[status_parts["制作中"]].decode("utf-8")
sheet4 = original_parts[status_parts["待验收"]].decode("utf-8")
ids3 = sheet_ids(sheet3)
ids4 = sheet_ids(sheet4)
anchor = [item for item in ids3 if item[0] == "fire_seed_sow"]
if len(anchor) != 1 or anchor[0][1] != 7 or any(item[0] == "fire_seed_sow" for item in ids4):
    fail(f"fire_seed_sow location mismatch: {[(item[0], item[1]) for item in anchor]}")
original_counts = {title: len(sheet_ids(original_parts[name].decode("utf-8"))) for title, name in status_parts.items()}
if original_counts != {"已完成": 37, "待制作": 0, "制作中": 23, "待验收": 18}:
    fail(f"Unexpected source counts: {original_counts}")
all_codes = [code for title, name in status_parts.items() for code, _number, _row in sheet_ids(original_parts[name].decode("utf-8"))]
if len(all_codes) != 78 or len(set(all_codes)) != 78:
    fail("Source status ids are not unique")

source_number = 7
source = anchor[0][2]
if "<f" in source:
    fail("fire_seed_sow row contains a formula")
source_values = row_values(source, source_number)
if source_values.get("A") != "种火" or source_values.get("B") != "fire_seed_sow":
    fail("Seed identity mismatch")
old_n = source_values.get("N")
old_o = source_values.get("O")
old_dv = source_values.get("DV")
if (len(old_n or ""), sha256((old_n or "").encode("utf-8"))) != (expected_lengths["N"], expected_n_sha):
    fail("Making N changed before conversion")
if (len(old_o or ""), sha256((old_o or "").encode("utf-8"))) != (expected_lengths["O"], expected_o_sha):
    fail("Making O changed before conversion")
if (len(old_dv or ""), sha256((old_dv or "").encode("utf-8"))) != (expected_lengths["DV"], expected_dv_sha):
    fail("Making DV changed before conversion")
if "[美术进度 2026-11-15]" in old_dv or "待验收 | 待制作-20261031" in old_dv:
    fail("Making DV already contains the acceptance marker")

archive_values = row_values(archive_hits[0][2], 18)
if archive_values.get("N") != old_n or archive_values.get("O") != old_o:
    fail("Archive N/O does not match the original making demand")
if not old_dv.startswith(archive_values.get("DV") or "\0"):
    fail("Archive DV is not the preserved prefix")

content_rows4 = [number for _code, number, _row in ids4]
target_number = max(content_rows4) + 1
if target_number != 25 or any(row_number(row) == target_number for row in rows_of(sheet4)):
    fail(f"Expected empty target row 25, found {target_number}")
edited = set_cell(source, "N", source_number, n_pending)
edited = set_cell(edited, "O", source_number, o_pending)
edited = set_cell(edited, "DV", source_number, old_dv + "\n" + dv_addition)
moved = renumber(edited, source_number, target_number)
if cell_text(cell_xml(moved, "B", target_number)[0]) != "fire_seed_sow":
    fail("Moved id changed")
if cell_text(cell_xml(moved, "N", target_number)[0]) != n_pending or cell_text(cell_xml(moved, "O", target_number)[0]) != o_pending:
    fail("Moved N/O mismatch")
moved_dv = cell_text(cell_xml(moved, "DV", target_number)[0])
if moved_dv != old_dv + "\n" + dv_addition:
    fail("Moved DV prefix was not preserved")
if len(re.findall(r"<c\b", moved)) != len(re.findall(r"<c\b", source)):
    fail("Moved row cell count changed")
if row_without_text(moved) != row_without_text(renumber(source, source_number, target_number)):
    fail("Moved row changed outside N/O/DV text")

sheet3_out = sheet3.replace(source, "", 1)
if sheet3.count(source) != 1 or "fire_seed_sow" in sheet3_out:
    fail("Making source row was not removed cleanly")
sheet4_out = replace_once(sheet4, "</sheetData>", moved + "</sheetData>", "待验收 sheetData close")
sheet4_out = replace_once(sheet4_out, 'ref="A1:EN24"', 'ref="A1:EN25"', "待验收 dimension")
if 'ref="A1:EN27"' not in sheet3_out or 'ref="A2:EN4"' not in sheet3_out:
    fail("制作中 dimension or filter changed unexpectedly")
validation_covers(sheet4_out, target_number)

changed = {status_parts["制作中"]: sheet3_out.encode("utf-8"), status_parts["待验收"]: sheet4_out.encode("utf-8")}
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
    if src.testzip() is not None or dst.testzip() is not None:
        fail("Zip test failed")
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
    if counts != {"已完成": 37, "待制作": 0, "制作中": 22, "待验收": 19}:
        fail(f"Counts {counts}")
    if len(all_ids) != 78 or len(set(all_ids)) != 78:
        fail("Status ids are not unique")
    if located.get("fire_seed_sow") != [("待验收", target_number)]:
        fail(f"fire_seed_sow at {located.get('fire_seed_sow')}")
    making_ids = sheet_ids(dst.read(status_parts["制作中"]).decode("utf-8"))
    expected_codes = [(code, number) for code, number, _row in originals["制作中"] if code != "fire_seed_sow"]
    if [(code, number) for code, number, _row in making_ids] != expected_codes:
        fail("制作中 order changed beyond removing fire_seed_sow")
    original_making = {code: row for code, _number, row in originals["制作中"]}
    for code, number, actual in making_ids:
        if actual != original_making[code]:
            fail(f"制作中 row for {code} changed")
    pending_out = sheet_ids(dst.read(status_parts["待验收"]).decode("utf-8"))
    if [(code, number, row) for code, number, row in pending_out if code != "fire_seed_sow"] != originals["待验收"]:
        fail("待验收 existing rows changed")
    reopened = next(row for code, number, row in pending_out if code == "fire_seed_sow")
    if cell_text(cell_xml(reopened, "DV", target_number)[0]) != old_dv + "\n" + dv_addition:
        fail("Reopened DV mismatch")
    if cell_text(cell_xml(reopened, "O", target_number)[0]) != o_pending or cell_text(cell_xml(reopened, "N", target_number)[0]) != n_pending:
        fail("Reopened N/O mismatch")
    for unchanged in ("xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet5.xml", "xl/worksheets/sheet6.xml", "xl/worksheets/sheet7.xml", "xl/workbook.xml", "xl/styles.xml", "xl/comments/comment3.xml", "xl/comments/comment4.xml"):
        if dst.read(unchanged) != src.read(unchanged):
            fail(f"{unchanged} changed")

snapshot = {
    "schema": "seed-sow-v3-requirements-snapshot-20261115",
    "batchId": "待制作-20261031",
    "taskId": "fire_seed_sow_v3_archive_sync",
    "attemptId": "20261115-sync-seed-sow-v3-02",
    "techniqueId": "fire_seed_sow",
    "source": {"sheet": "制作中", "row": 7, "columnsAtoEN": 144, "valuesAtoEN": full_row(source_values), "nBefore": old_n, "oBefore": old_o, "dvBefore": old_dv},
    "archive": {"sheet": "待制作原始数据", "row": 18, "valuesAtoEN": full_row(archive_values), "note": "首次待制作快照，本次不覆盖、不转换"},
    "resourceDetail": "资源状态明细无 fire_seed_sow，原样未动",
    "userConfirmation": {"date": "2026-11-11", "verbatim": "按照这版接入", "subject": "种火_上窜_v3 二维及同版 Blockbench 补档"},
    "approvedVersion": "种火_上窜熄灭_v3；本次模型补档不提出新运行视觉参数",
    "displayCopy": {"techniqueCard": n_pending, "techniqueCardSource": "src/main/java/com/bslx/XueXiMod/shu/ModTechniques.java FIRE_SEED_SOW；zh_cn.json 没有覆盖这句技术卡文案"},
    "formalArchive": "docs/资源库/特效/火焰/种火_上窜_v3/",
    "modelArchive": "docs/资源库/过程归档/种火_上窜熄灭_v3_模型补档_20261115/seed_sow_v3_preview.bbmodel",
    "runtimeResources": [
        {"path": "assets/xueximod/textures/effect/seed_sow_v3/seed_sow.png", "bytes": 2776, "sha256": "A50EC7ED6A8434F50987A86036201DABFF3AB8A70CFF9D82A31CAC9F12B03EEE"},
        {"path": "assets/xueximod/textures/effect/seed_sow_v3/seed_sow.png.mcmeta", "bytes": 64, "sha256": "B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A"},
    ],
    "converted": {"targetSheet": "待验收", "targetRow": target_number, "n": n_pending, "o": o_pending, "dvPrefix": old_dv, "dvAddition": dv_addition},
    "configuration": {
        "stableId": "fire_seed_sow",
        "standard": [{"key": "cooldownTicks", "default": 5, "unit": "tick"}, {"key": "manaCost", "default": 2}],
        "special": [{"key": "seedCount", "default": 10, "unit": "count", "range": [0, 100]}],
        "consumption": "下一次服务器启动或重新打开单机世界时读取标准冷却、法力与种火基数",
    },
    "compileEvidence": {"command": ".\\gradlew.bat compileJava --offline --no-daemon", "started": "2026-11-15 03:25:54.673 +08:00", "ended": "2026-11-15 03:26:22.708 +08:00", "exitCode": 0, "srcMainTreeSha256": "351eb181c4eb75b138b9370a8b2fce8caeaba71026d408a240c627f62bf408d4", "srcMainUnchangedBeforeCommit": True},
    "baseline": {"sha256": expected_baseline, "bytes": 179938, "backup": str(backup)},
}
snapshot_path.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
print(json.dumps({"backup": str(backup), "candidate": str(output), "candidateSha256": sha256(output.read_bytes()), "candidateBytes": output.stat().st_size, "targetRow": target_number, "counts": counts}, ensure_ascii=False))
