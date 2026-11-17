# -*- coding: utf-8 -*-
"""Append the v2-3D candidate note to fire_detonate DV without rewriting unrelated workbook XML."""
import hashlib
import html
import io
import json
import re
import sys
import zipfile
import ctypes
from ctypes import wintypes
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "术式批量制作" / "待制作-20261031"
live = root / "docs" / "术式制作表.xlsx"
expected_baseline = "2596AC4DD0F01B14A96A5C37AAC6EC658958B1772CDC47214B0C703447112E94"
expected_bytes = 188165
expected_n_sha = "461AE1A12693676FF94273A432ADD5BA9307F0C93C483E7F9E2F672008A7D329"
expected_o_sha = "34AF39A9173DD98D1DB200DB61E47E2337863414A640ACBAAA55B9146937C66F"
expected_dv_sha = "A47BEC489642B56919446D9696FF73591A30928D7F4D7A98EAB822BD070B7F39"
expected_dv_length = 1512
dv_addition = (
    "[美术进度 2026-11-16] 三维候选 fire_detonate_v2_3d_candidate_20261116 状态 candidate_pending_user_confirmation："
    "26团不同xyz中心像素云错时径向扩散，橙/蓝同形，20tick单次；真实Aseprite源与GUI Blockbench已交付。"
    "模型只保存峰值可编辑姿态，动图为真实GUI逐帧捕获，不是原生动画轨道。"
    "用户尚未同版确认，未接入、未编译、未游戏实测；现运行v1暂用保留，仍留制作中。"
    "详见 docs/开发记录/术式批量制作/待制作-20261031/detonate-v2-3d-candidate-sheet-20261116.md。"
)
parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
}


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
    return {column: values.get(column) for column in columns}


def row_without_text(row_xml):
    return re.sub(r"(<(?:[\w]+:)?t\b[^>]*>)[\s\S]*?(</(?:[\w]+:)?t>)", r"\1\2", row_xml)


def inspect(data):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        names = archive.namelist()
        if len(names) != 27 or "xl/sharedStrings.xml" in names:
            fail(f"ZIP inventory changed: {len(names)}")
        sheets = {name: archive.read(name).decode("utf-8") for name in parts.values()}
        infos = archive.infolist()
    counts = {}
    codes = []
    for label, name in parts.items():
        found = sheet_ids(sheets[name])
        counts[label] = len(found)
        codes.extend(code for code, _, _ in found)
    if counts != {"已完成": 37, "待制作": 0, "制作中": 22, "待验收": 19} or len(codes) != 78 or len(set(codes)) != 78:
        fail(f"Identity check failed: {counts} total={len(codes)} unique={len(set(codes))}")
    return sheets, counts, infos


def build():
    live_bytes = read_shared(live)
    if sha256(live_bytes) != expected_baseline or len(live_bytes) != expected_bytes:
        fail("Live workbook baseline changed before candidate build")
    sheets, counts, _ = inspect(live_bytes)
    matches = [item for item in sheet_ids(sheets[parts["制作中"]]) if item[0] == "fire_detonate"]
    if len(matches) != 1:
        fail(f"fire_detonate count is {len(matches)}")
    _, number, source_row = matches[0]
    values = full_row(row_values(source_row))
    old_n, old_o, old_dv = values.get("N") or "", values.get("O") or "", values.get("DV") or ""
    if sha256(old_n.encode("utf-8")) != expected_n_sha or sha256(old_o.encode("utf-8")) != expected_o_sha:
        fail("N/O changed")
    if sha256(old_dv.encode("utf-8")) != expected_dv_sha or len(old_dv) != expected_dv_length:
        fail("DV baseline changed")
    if "fire_detonate_v2_3d_candidate_20261116" in old_dv:
        fail("3D candidate note already present")
    new_dv = old_dv + "\n" + dv_addition
    edited_row = set_cell(source_row, "DV", number, new_dv)
    if row_without_text(edited_row) != row_without_text(source_row):
        fail("XML outside DV text changed")
    edited_values = full_row(row_values(edited_row))
    for column, value in values.items():
        if column != "DV" and edited_values.get(column) != value:
            fail(f"Column {column} changed")
    source_sheet = sheets[parts["制作中"]]
    if source_sheet.count(source_row) != 1:
        fail("Source row is not unique")
    edited_sheet = source_sheet.replace(source_row, edited_row, 1)
    if len(re.findall(r"<dataValidation\b", edited_sheet)) != len(re.findall(r"<dataValidation\b", source_sheet)):
        fail("Validation count changed")
    if len(re.findall(r"<mergeCell\b", edited_sheet)) != len(re.findall(r"<mergeCell\b", source_sheet)):
        fail("Merge count changed")
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = batch / "备份" / f"detonate-v2-3d-candidate-术式制作表-{stamp}-1.xlsx"
    output = batch / "候选" / f"detonate-v2-3d-candidate-术式制作表-{stamp}-1.xlsx"
    if backup.exists() or output.exists():
        fail("Timestamped output already exists")
    backup.parent.mkdir(parents=True, exist_ok=True)
    output.parent.mkdir(parents=True, exist_ok=True)
    backup.write_bytes(live_bytes)
    if sha256(backup.read_bytes()) != expected_baseline:
        fail("Backup hash differs")
    with zipfile.ZipFile(io.BytesIO(live_bytes)) as src, zipfile.ZipFile(output, "w") as dst:
        for info in src.infolist():
            payload = edited_sheet.encode("utf-8") if info.filename == parts["制作中"] else src.read(info.filename)
            cloned = zipfile.ZipInfo(filename=info.filename, date_time=info.date_time)
            cloned.compress_type = info.compress_type
            cloned.external_attr = info.external_attr
            cloned.flag_bits = info.flag_bits
            cloned.create_system = info.create_system
            cloned.comment = info.comment
            dst.writestr(cloned, payload)
    candidate = output.read_bytes()
    reopened, reopened_counts, _ = inspect(candidate)
    target = [item for item in sheet_ids(reopened[parts["制作中"]]) if item[0] == "fire_detonate"]
    if len(target) != 1 or target[0][1] != number or cell_text(cell_xml(target[0][2], "DV", number)[0]) != new_dv:
        fail("Reopened candidate mismatch")
    if cell_text(cell_xml(target[0][2], "N", number)[0]) != old_n or cell_text(cell_xml(target[0][2], "O", number)[0]) != old_o:
        fail("Reopened N/O mismatch")
    with zipfile.ZipFile(io.BytesIO(live_bytes)) as src, zipfile.ZipFile(io.BytesIO(candidate)) as dst:
        if src.namelist() != dst.namelist():
            fail("ZIP order changed")
        for name in src.namelist():
            if name != parts["制作中"] and src.read(name) != dst.read(name):
                fail(f"Unrelated ZIP entry changed: {name}")
    print(json.dumps({
        "backup": str(backup),
        "candidate": str(output),
        "candidateSha256": sha256(candidate),
        "candidateBytes": len(candidate),
        "row": number,
        "oldDvLength": len(old_dv),
        "newDvLength": len(new_dv),
        "newDvSha256": sha256(new_dv.encode("utf-8")),
        "counts": reopened_counts,
    }, ensure_ascii=False))


if __name__ == "__main__":
    build()
