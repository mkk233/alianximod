import ctypes
import hashlib
import html
import json
import re
import shutil
import sys
import zipfile
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path

BASE = Path(r"D:\alianximod")
SRC = BASE / "docs" / "术式制作表.xlsx"
BATCH = BASE / "docs" / "开发记录" / "术式批量制作" / "待制作-20261031"
BACKUP_DIR = BATCH / "备份"
EXPECTED = "744A693B267A357D0CFF1D3F57598C01130B2C5559A9FAAA433C60DC2A115679"
M = "{http://schemas.openxmlformats.org/spreadsheetml/2006/main}"
QUOTE = (
    "这个效果很好但是不适合作为燃烧的特效，因为这个火比较猛不像是初级术式这样你把现在焚天的特效先拿来给引燃用，把这个效果给焚天"
)
KINDLE_PIXEL = "3AE6A95E41D92991B828EDC7491CBB40A18D503CDCF700281A50D901D6AA6A76"
FENTIAN_PIXEL = "EF3476EB01225D2E7478F9BBA5430F28BCF2E1D12198F3F482A634C155A04070"
KINDLE_RUNTIME = "assets/xueximod/textures/effect/kindle_v4/kindle.png"
FENTIAN_RUNTIME = "assets/xueximod/textures/effect/fentian_v3/conflagration.png"
RECEIPT = "docs/开发记录/术式批量制作/待制作-20261031/kindle-fentian-swap-approval-20261116.md"


def die(code, message):
    print(message)
    sys.exit(code)


def sha(path):
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def meta(path):
    stat = path.stat()
    return {"bytes": stat.st_size, "sha256": sha(path), "mtime_ns": stat.st_mtime_ns}


def text_sha(value):
    return hashlib.sha256(value.encode("utf-8")).hexdigest().upper()


def escape(value):
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def load_approved():
    root = BASE / "docs" / "开发记录" / "火种共享燃料改造-20261029"
    approved = json.loads((root / "approved-requirements.json").read_text(encoding="utf-8"))
    conversion = json.loads((root / "final-writeback-conversion.json").read_text(encoding="utf-8"))
    item = conversion["items"]["fire_fentian"]
    approved_n = approved["items"]["fire_fentian"]["n"]
    if approved_n != item["beforeN"] or item["beforeO"] is None:
        die(2, "ABORT_APPROVED_MISMATCH")
    return approved_n, item["beforeO"], item["displayN"], item["displayO"]


def cell_map(xml_bytes):
    root = ET.fromstring(xml_bytes)
    result = {}
    for cell in root.iter(M + "c"):
        ref = cell.get("r")
        kind = cell.get("t") or ""
        style = cell.get("s") or ""
        if kind == "inlineStr":
            value = "".join((node.text or "") for node in cell.iter(M + "t"))
        else:
            formula = cell.find(M + "f")
            raw = cell.find(M + "v")
            value = (formula.text or "" if formula is not None else "") + "|" + (raw.text or "" if raw is not None else "")
        result[ref] = (kind, style, value)
    return result


def column_row(ref):
    return "".join(ch for ch in ref if ch.isalpha()), int("".join(ch for ch in ref if ch.isdigit()))


def set_inline_text(xml, ref, value):
    start = xml.find(f'<c r="{ref}"')
    if start < 0:
        die(30, f"ABORT_CELL_MISSING {ref}")
    end = xml.find("</c>", start) + 4
    cell = xml[start:end]
    if cell.count("<t") != 1 or cell.count("</t>") != 1:
        die(31, f"ABORT_TEXT_NODE {ref}")
    open_end = cell.find(">", cell.find("<t")) + 1
    close = cell.find("</t>")
    updated = cell[:open_end] + escape(value) + cell[close:]
    return xml[:start] + updated + xml[end:]


def status_counts(path):
    with zipfile.ZipFile(path) as workbook:
        book = ET.fromstring(workbook.read("xl/workbook.xml"))
        rels = ET.fromstring(workbook.read("xl/_rels/workbook.xml.rels"))
        rid = {rel.get("Id"): rel.get("Target") for rel in rels}
        ns = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"
        sheets = []
        for sheet in book.find(M + "sheets"):
            target = rid[sheet.get(ns)].lstrip("/")
            sheets.append((sheet.get("name"), target if target.startswith("xl/") else "xl/" + target))
        counts = {}
        locations = []
        for name, target in sheets[:4]:
            root = ET.fromstring(workbook.read(target))
            rows = {}
            for cell in root.iter(M + "c"):
                ref = cell.get("r") or ""
                column, row = column_row(ref)
                if column != "B":
                    continue
                value = "".join((node.text or "") for node in cell.iter(M + "t"))
                if value:
                    rows[row] = value
                    if row != 2:
                        locations.append((name, row, value))
            counts[name] = len([rows[row] for row in rows if row != 2])
        return counts, locations


def main():
    approved_n, approved_o, display_n, display_o = load_approved()
    kindle_add = (
        "\n[设计反馈 2026-11-16] 用户原话：「" + QUOTE + "」。这是同版素材用途互换并授权接入，不是等待同一素材再确认。"
        "引燃承接原焚天v2，批准像素SHA256 " + KINDLE_PIXEL + "，计划运行路径 " + KINDLE_RUNTIME + "；强烈v3改归焚天。"
        "运行文件尚未按实现回执证明已复制。保持制作中。详见 " + RECEIPT + "。"
    )
    o_merge = (
        "\n[2026-11-16 用户明确修改] 原话：「" + QUOTE + "」。本术式改用强烈全身燃烧v3，批准像素SHA256 "
        + FENTIAN_PIXEL + "，计划运行路径 " + FENTIAN_RUNTIME + "。原焚天v2改归引燃。本次不改已批准的每段10点、伤害2×(1.2^n+F)。"
    )
    fentian_add = (
        "\n[状态纠正 2026-11-16] 新确认的素材互换尚未接入，本行从待验收返回制作中。用户原话：「" + QUOTE + "」。"
        "这是同版用途互换并授权接入，不是等待同一素材再确认，也不把旧待验收当成新素材已接入。"
        "强烈v3归焚天，像素SHA256 " + FENTIAN_PIXEL + "，计划运行 " + FENTIAN_RUNTIME + "；原焚天v2归引燃。"
        "N/O按2026-10-29最近已批准详细需求恢复。2026-11-12定时燃尽合并稿没有提交源表，本次不采用。"
        "代码与表内结算仍待对账，本次不改公式、不改EK。详见 " + RECEIPT + "。"
        "\n制作中 | 待制作-20261031 | docs/开发记录/术式批量制作/待制作-20261031/记录.md"
    )
    before = meta(SRC)
    print("BEFORE", json.dumps(before, ensure_ascii=True))
    if before["sha256"] != EXPECTED or before["bytes"] != 180915:
        die(3, "ABORT_BASELINE")
    lock = SRC.with_name("~$" + SRC.name)
    if lock.exists():
        die(4, "ABORT_LOCK")
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    seq = 1
    while True:
        backup = BACKUP_DIR / f"kindle-fentian-swap-术式制作表-{stamp}-{seq}.xlsx"
        if not backup.exists():
            break
        seq += 1
    shutil.copy2(SRC, backup)
    if meta(backup)["sha256"] != EXPECTED:
        die(5, "ABORT_BACKUP")

    with zipfile.ZipFile(backup) as workbook:
        names = [info.filename for info in workbook.infolist()]
        sheet3 = workbook.read("xl/worksheets/sheet3.xml").decode("utf-8")
        sheet4 = workbook.read("xl/worksheets/sheet4.xml").decode("utf-8")
    if len(names) != 27:
        die(6, "ABORT_ZIP_COUNT")
    old3 = cell_map(sheet3.encode("utf-8"))
    old4 = cell_map(sheet4.encode("utf-8"))
    if old3["B8"][2] != "fire_kindle" or old3["N8"][2] is None:
        die(7, "ABORT_KINDLE")
    if text_sha(old3["N8"][2]) != "75F6ED304CD49A509224F4E9D117DBDE581D415A30ED54D0FAF0ED8E656DB8C1":
        die(8, "ABORT_KINDLE_N")
    if text_sha(old3["O8"][2]) != "933CC168D8E153AA87437E5CFDBD52C6030A05FB4392CD85379E66285F6F9F08":
        die(9, "ABORT_KINDLE_O")
    if text_sha(old3["DV8"][2]) != "DA65890C94427018477AB1FC399DAE0A2A898626E758985E9C13A232F23BF3B6":
        die(10, "ABORT_KINDLE_DV")
    if old4["B3"][2] != "fire_fentian" or old4["N3"][2] != display_n or old4["O3"][2] != display_o:
        die(11, "ABORT_FENTIAN_DISPLAY")
    if "28" in {column_row(ref)[1] for ref in old3}:
        die(12, "ABORT_ROW28_OCCUPIED")

    match = re.search(r'<row\b[^>]*\br="3"[^>]*>.*?</row>', sheet4, re.S)
    if not match or sheet4.count('<row r="3"') != 1:
        die(13, "ABORT_SOURCE_ROW")
    source_row = match.group(0)
    if "<f" in source_row or "<f>" in source_row:
        die(14, "ABORT_FORMULA")
    moved = re.sub(r'\br="([A-Z]+)3"', lambda item: f'r="{item.group(1)}28"', source_row, count=0)
    moved = moved.replace('<row r="3"', '<row r="28"', 1)
    if 'r="3"' in moved or moved.count('<row r="28"') != 1:
        die(15, "ABORT_RENUMBER")
    new_n = approved_n
    new_o = approved_o + o_merge
    new_dv = old4["DV3"][2] + fentian_add
    if not new_o.startswith(approved_o) or not new_dv.startswith(old4["DV3"][2]):
        die(16, "ABORT_PREFIX")
    for ref, value in (("N28", new_n), ("O28", new_o), ("DV28", new_dv)):
        moved = set_inline_text(moved, ref, value)
    moved_map = cell_map(("<root xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" + moved + "</root>").encode("utf-8"))
    source_refs = {ref for ref in old4 if column_row(ref)[1] == 3}
    for ref in source_refs:
        column, _ = column_row(ref)
        target = f"{column}28"
        if target not in moved_map or moved_map[target][0] != old4[ref][0] or moved_map[target][1] != old4[ref][1]:
            die(17, f"ABORT_MOVED_STYLE {ref}")
        if column not in {"N", "O", "DV"} and moved_map[target][2] != old4[ref][2]:
            die(18, f"ABORT_MOVED_VALUE {ref}")
    if moved_map["N28"][2] != new_n or moved_map["O28"][2] != new_o or moved_map["DV28"][2] != new_dv:
        die(19, "ABORT_MOVED_TEXT")
    if moved_map["EK28"][2] != old4["EK3"][2] or moved_map["E28"][2] != old4["E3"][2]:
        die(20, "ABORT_FORMULA_CELLS")

    new_sheet4 = sheet4[:match.start()] + sheet4[match.end():]
    if '<row r="3"' in new_sheet4 or "fire_fentian" in new_sheet4:
        die(21, "ABORT_SOURCE_REMAINS")
    if new_sheet4.count("<dataValidation ") != 20 or new_sheet4.count("<mergeCell ref=") != 8:
        die(22, "ABORT_SHEET4_FEATURES")
    new_sheet3 = set_inline_text(sheet3, "DV8", old3["DV8"][2] + kindle_add)
    marker = "</sheetData>"
    pos = new_sheet3.find(marker)
    if pos < 0 or new_sheet3.find("<dimension ref=\"A1:EN27\"/>") < 0:
        die(23, "ABORT_SHEET3_STRUCTURE")
    new_sheet3 = new_sheet3.replace("<dimension ref=\"A1:EN27\"/>", "<dimension ref=\"A1:EN28\"/>", 1)
    new_sheet3 = new_sheet3[:pos] + moved + new_sheet3[pos:]
    if new_sheet3.count("<dataValidation ") != 20 or new_sheet3.count("<mergeCell ref=") != 8:
        die(24, "ABORT_SHEET3_FEATURES")
    check3 = cell_map(new_sheet3.encode("utf-8"))
    if check3["DV8"][2] != old3["DV8"][2] + kindle_add or check3["N8"][2] != old3["N8"][2] or check3["O8"][2] != old3["O8"][2]:
        die(25, "ABORT_KINDLE_RESULT")
    if check3["B28"][2] != "fire_fentian":
        die(26, "ABORT_TARGET_B")

    candidate = BATCH / "kindle-fentian-swap-candidate-20261116.xlsx"
    if candidate.exists():
        die(27, "ABORT_CANDIDATE_EXISTS")
    replacements = {
        "xl/worksheets/sheet3.xml": new_sheet3.encode("utf-8"),
        "xl/worksheets/sheet4.xml": new_sheet4.encode("utf-8"),
    }
    with zipfile.ZipFile(backup, "r") as source, zipfile.ZipFile(candidate, "w") as target:
        for info in source.infolist():
            data = replacements.get(info.filename, source.read(info.filename))
            copied = zipfile.ZipInfo(filename=info.filename, date_time=info.date_time)
            copied.compress_type = info.compress_type
            copied.external_attr = info.external_attr
            copied.flag_bits = info.flag_bits & ~0x8
            target.writestr(copied, data)

    def parts(path):
        with zipfile.ZipFile(path) as workbook:
            return [(info.filename, hashlib.sha256(workbook.read(info.filename)).hexdigest().upper()) for info in workbook.infolist()]

    old_parts = dict(parts(backup))
    new_parts = parts(candidate)
    if [name for name, _digest in new_parts] != names:
        die(28, "ABORT_ORDER")
    changed = [name for name, digest in new_parts if old_parts[name] != digest]
    if changed != ["xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"]:
        die(29, "ABORT_PARTS " + ",".join(changed))

    with zipfile.ZipFile(backup) as old_book, zipfile.ZipFile(candidate) as new_book:
        diffs = []
        for name in names:
            if not (name.startswith("xl/worksheets/sheet") and name.endswith(".xml")):
                continue
            left = cell_map(old_book.read(name))
            right = cell_map(new_book.read(name))
            for ref in sorted(set(left) | set(right), key=lambda item: (column_row(item)[1], column_row(item)[0])):
                if left.get(ref) != right.get(ref):
                    diffs.append((name, ref))
    removed = sorted(ref for name, ref in diffs if name.endswith("sheet4.xml"))
    added = sorted(ref for name, ref in diffs if name.endswith("sheet3.xml") and ref not in old3)
    changed_existing = sorted(ref for name, ref in diffs if name.endswith("sheet3.xml") and ref in old3)
    if set(removed) != source_refs or changed_existing != ["DV8"]:
        die(32, "ABORT_DIFFS " + json.dumps({"removed": removed, "changed": changed_existing}, ensure_ascii=True))
    if {column_row(ref)[0] + "28" for ref in source_refs} != set(added):
        die(33, "ABORT_ADDED")

    if meta(SRC)["sha256"] != EXPECTED or lock.exists():
        die(34, "HOLD_SOURCE_CHANGED")
    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    kernel.CreateFileW.restype = ctypes.c_void_p
    kernel.CreateFileW.argtypes = [ctypes.c_wchar_p, ctypes.c_uint32, ctypes.c_uint32, ctypes.c_void_p, ctypes.c_uint32, ctypes.c_uint32, ctypes.c_void_p]
    handle = kernel.CreateFileW(str(SRC), 0x80000000 | 0x40000000, 0, None, 3, 0x80, None)
    if handle is None or handle == ctypes.c_void_p(-1).value:
        die(35, "HOLD_EXCLUSIVE " + str(ctypes.get_last_error()))
    kernel.CloseHandle(handle)
    shutil.copyfile(candidate, SRC)
    after = meta(SRC)
    if after["sha256"] != meta(candidate)["sha256"]:
        die(36, "ABORT_AFTER")
    counts, locations = status_counts(SRC)
    duplicates = {}
    for name, row, value in locations:
        duplicates.setdefault(value, []).append(f"{name}:{row}")
    duplicates = {key: value for key, value in duplicates.items() if len(value) > 1}
    if duplicates or counts != {"已完成": 37, "待制作": 0, "制作中": 23, "待验收": 18}:
        die(37, "ABORT_COUNTS " + json.dumps({"counts": counts, "duplicates": duplicates}, ensure_ascii=True))
    snapshot = {
        "kind": "fire-kindle-fentian-approved-swap-and-repair-return",
        "batchId": "待制作-20261031",
        "taskId": "fire_kindle_v2_progress",
        "attemptId": "20261116-record-kindle-fentian-swap-04",
        "quote_verbatim": QUOTE,
        "approved_sources": {
            "approved_requirements": "docs/开发记录/火种共享燃料改造-20261029/approved-requirements.json",
            "approved_requirements_sha256": "E5C903294E9DF63A8552119BC6624EEC7521FA9636FE9F9003CE38FD0C53F922",
            "conversion": "docs/开发记录/火种共享燃料改造-20261029/final-writeback-conversion.json",
            "conversion_sha256": "B73C3BA211EEA174D922578F310C8A2795D70A3B1431E30117DDDD14C5CCB653",
            "not_used": "docs/开发记录/焚天定时燃尽-20261112/需求快照.json",
            "not_used_reason": "2026-11-12 candidate merge was not submitted because the source workbook was locked; its own record identifies the 2026-10-29 detailed N/O as the latest approved snapshot.",
        },
        "fire_kindle": {
            "sheet_before": "制作中",
            "row_before": 8,
            "sheet_after": "制作中",
            "row_after": 8,
            "n_unchanged_sha256": text_sha(old3["N8"][2]),
            "o_unchanged_sha256": text_sha(old3["O8"][2]),
            "dv_before": old3["DV8"][2],
            "dv_after": check3["DV8"][2],
        },
        "fire_fentian": {
            "sheet_before": "待验收",
            "row_before": 3,
            "sheet_after": "制作中",
            "row_after": 28,
            "display_n_before": old4["N3"][2],
            "display_o_before": old4["O3"][2],
            "dv_before": old4["DV3"][2],
            "restored_n": new_n,
            "restored_o_prefix": approved_o,
            "o_after": new_o,
            "dv_after": new_dv,
            "e_unchanged": old4["E3"][2],
            "ek_unchanged": old4["EK3"][2],
        },
        "approved_mapping": {
            "old_fentian_v2_pixel_sha256": KINDLE_PIXEL,
            "old_fentian_v2_runtime_plan": KINDLE_RUNTIME,
            "intense_v3_pixel_sha256": FENTIAN_PIXEL,
            "intense_v3_runtime_plan": FENTIAN_RUNTIME,
            "runtime_copy_verified": False,
        },
    }
    snapshot_path = BATCH / "kindle-fentian-swap-requirements-20261116.json"
    snapshot_path.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("BACKUP", str(backup))
    print("CANDIDATE", str(candidate))
    print("CANDIDATE_META", json.dumps(meta(candidate), ensure_ascii=True))
    print("AFTER", json.dumps(after, ensure_ascii=True))
    print("COUNTS", json.dumps(counts, ensure_ascii=True))
    print("KINDLE", json.dumps([item for item in locations if item[2] == "fire_kindle"], ensure_ascii=True))
    print("FENTIAN", json.dumps([item for item in locations if item[2] == "fire_fentian"], ensure_ascii=True))
    print("KINDLE_DV", len(check3["DV8"][2]), text_sha(check3["DV8"][2]))
    print("FENTIAN_N", len(new_n), text_sha(new_n))
    print("FENTIAN_O", len(new_o), text_sha(new_o))
    print("FENTIAN_DV", len(new_dv), text_sha(new_dv))
    print("SNAPSHOT", str(snapshot_path), json.dumps(meta(snapshot_path), ensure_ascii=True))
    print("DONE")


if __name__ == "__main__":
    main()
