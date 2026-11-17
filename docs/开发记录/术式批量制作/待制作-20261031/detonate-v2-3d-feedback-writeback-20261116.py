# -*- coding: utf-8 -*-
"""Append one dated visual-feedback note to fire_detonate DV without rewriting unrelated workbook XML."""
import ctypes
import hashlib
import html
import io
import json
import re
import zipfile
from ctypes import wintypes
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "术式批量制作" / "待制作-20261031"
live = root / "docs" / "术式制作表.xlsx"
receipt_path = batch / "detonate-v2-3d-feedback-20261116.md"
expected_baseline = "16927898C86981D6F2CE7FA5E5C2D91678A6F8A9690BD6C1B042B8E99203C2BD"
expected_bytes = 187970
expected_n_sha = "461AE1A12693676FF94273A432ADD5BA9307F0C93C483E7F9E2F672008A7D329"
expected_o_sha = "34AF39A9173DD98D1DB200DB61E47E2337863414A640ACBAAA55B9146937C66F"
expected_dv_sha = "AF59A2B4D8943EE79A245EE9C0930BE585BA62A090C939E852F4B362709E4D51"
expected_dv_length = 1271
dv_addition = (
    "[美术进度 2026-11-16 反馈返修] 用户原话：“效果不太行，应该参考原版爆炸的效果三维的扩散开而不是简单的二维效果”。"
    "旧二维候选 docs/资源库/过程归档/引爆_短促火团_v2_20261116/ 已拒绝，只保留历史；"
    "三维扩散候选正在制作，未确认、未接入。现运行 v1 暂用保留。不改 N/O 的伤害、范围和消耗规则，仍留制作中。"
    "详见 docs/开发记录/术式批量制作/待制作-20261031/detonate-v2-3d-feedback-20261116.md。"
)


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
    invalid = ctypes.c_void_p(-1).value
    if handle is None or handle == invalid:
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


def row_values(row_xml):
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


live_bytes = read_shared(live)
if sha256(live_bytes) != expected_baseline or len(live_bytes) != expected_bytes:
    fail("Live workbook baseline changed before candidate build")

with zipfile.ZipFile(io.BytesIO(live_bytes)) as archive:
    names = archive.namelist()
    if len(names) != 27:
        fail(f"ZIP entry count is {len(names)}")
    if "xl/sharedStrings.xml" in names:
        fail("Workbook unexpectedly uses a shared string table")
    sheets = {name: archive.read(name).decode("utf-8") for name in [
        "xl/worksheets/sheet1.xml",
        "xl/worksheets/sheet2.xml",
        "xl/worksheets/sheet3.xml",
        "xl/worksheets/sheet4.xml",
    ]}
    source_infos = archive.infolist()

counts = {}
all_ids = []
for label, part in {"已完成": "xl/worksheets/sheet1.xml", "待制作": "xl/worksheets/sheet2.xml", "制作中": "xl/worksheets/sheet3.xml", "待验收": "xl/worksheets/sheet4.xml"}.items():
    found = sheet_ids(sheets[part])
    counts[label] = len(found)
    all_ids.extend(code for code, _, _ in found)
if counts != {"已完成": 37, "待制作": 0, "制作中": 22, "待验收": 19} or len(all_ids) != 78 or len(set(all_ids)) != 78:
    fail(f"Workbook identity check failed: {counts}")

matches = [(code, number, row) for code, number, row in sheet_ids(sheets["xl/worksheets/sheet3.xml"]) if code == "fire_detonate"]
if len(matches) != 1:
    fail(f"fire_detonate match count is {len(matches)}")
_, source_number, source_row = matches[0]
source_values = full_row(row_values(source_row))
old_n = source_values.get("N") or ""
old_o = source_values.get("O") or ""
old_dv = source_values.get("DV") or ""
if (
    sha256(old_n.encode("utf-8")) != expected_n_sha
    or sha256(old_o.encode("utf-8")) != expected_o_sha
    or sha256(old_dv.encode("utf-8")) != expected_dv_sha
    or len(old_dv) != expected_dv_length
):
    fail("N/O changed before feedback append")
if "效果不太行" in old_dv:
    fail("Feedback is already present")
new_dv = old_dv + "\n" + dv_addition
edited_row = set_cell(source_row, "DV", source_number, new_dv)
if row_without_text(edited_row) != row_without_text(source_row):
    fail("DV append changed XML outside the text node")
edited_values = full_row(row_values(edited_row))
for column, value in source_values.items():
    if column != "DV" and edited_values.get(column) != value:
        fail(f"Column {column} changed")
if edited_values.get("N") != old_n or edited_values.get("O") != old_o or not new_dv.startswith(old_dv):
    fail("N/O/DV prefix was not preserved")

source_sheet = sheets["xl/worksheets/sheet3.xml"]
if source_sheet.count(source_row) != 1:
    fail("Source row is not unique in sheet XML")
edited_sheet = source_sheet.replace(source_row, edited_row, 1)
if edited_sheet.count(f'r="B{source_number}"') != source_sheet.count(f'r="B{source_number}"'):
    fail("Edited sheet changed the target row identity")

stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"detonate-v2-3d-feedback-术式制作表-{stamp}-1.xlsx"
output = batch / "候选" / f"detonate-v2-3d-feedback-术式制作表-{stamp}-1.xlsx"
if backup.exists() or output.exists() or receipt_path.exists():
    fail("Timestamped output already exists")
backup.parent.mkdir(parents=True, exist_ok=True)
output.parent.mkdir(parents=True, exist_ok=True)
backup.write_bytes(live_bytes)
if sha256(backup.read_bytes()) != expected_baseline:
    fail("Backup hash differs from baseline")

with zipfile.ZipFile(io.BytesIO(live_bytes)) as src, zipfile.ZipFile(output, "w") as dst:
    for info in src.infolist():
        payload = edited_sheet.encode("utf-8") if info.filename == "xl/worksheets/sheet3.xml" else src.read(info.filename)
        cloned = zipfile.ZipInfo(filename=info.filename, date_time=info.date_time)
        cloned.compress_type = info.compress_type
        cloned.external_attr = info.external_attr
        cloned.flag_bits = info.flag_bits
        cloned.create_system = info.create_system
        cloned.comment = info.comment
        dst.writestr(cloned, payload)

with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    if src.namelist() != dst.namelist() or len(dst.namelist()) != 27:
        fail("Candidate ZIP inventory changed")
    for name in src.namelist():
        if name == "xl/worksheets/sheet3.xml":
            continue
        if src.read(name) != dst.read(name):
            fail(f"Unrelated ZIP entry changed: {name}")
    reopened = dst.read("xl/worksheets/sheet3.xml").decode("utf-8")
    reopened_ids = sheet_ids(reopened)
    reopened_match = [item for item in reopened_ids if item[0] == "fire_detonate"]
    if len(reopened_match) != 1 or reopened_match[0][1] != source_number:
        fail("Reopened target row changed")
    reopened_values = full_row(row_values(reopened_match[0][2]))
    if reopened_values.get("N") != old_n or reopened_values.get("O") != old_o or reopened_values.get("DV") != new_dv:
        fail("Reopened N/O/DV mismatch")
    validation_count = len(re.findall(r"<dataValidation\b", reopened))
    if validation_count != len(re.findall(r"<dataValidation\b", source_sheet)):
        fail("Validation count changed")

candidate_bytes = output.read_bytes()
summary = {
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(candidate_bytes),
    "candidateBytes": len(candidate_bytes),
    "row": source_number,
    "oldDvLength": len(old_dv),
    "newDvLength": len(new_dv),
    "oldDvSha256": sha256(old_dv.encode("utf-8")),
    "newDvSha256": sha256(new_dv.encode("utf-8")),
    "counts": counts,
}
print(json.dumps(summary, ensure_ascii=False))
