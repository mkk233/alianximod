# -*- coding: utf-8 -*-
"""Move the three water rows to pending acceptance. Preserve the rest of the package."""
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
WORK = ROOT / "docs" / "开发记录" / "术式批量制作" / "水流组合-20261103" / "sheet-work"
BASELINE = "D66BE8B7006A70C7231B8726C6785E65F64441B192460E125E5D05A1C440A57D"
MARKER = "待验收 | 水流组合-20261103 | docs/开发记录/术式批量制作/水流组合-20261103/记录.md"
QUOTA = "本阶段额度已用完，返修与新接入结果未追加复审；最终compileJava通过，游戏待用户实测。"

NEW_N = {
    "water_vortex": "干地水平牵引。入卷收取水势；与上升气泡和真实水组成龙卷后才按湿度扣返",
    "water_bubble_column": "真实水里的上升水柱。底部与旋流重合时挂上去，父旋流关闭后不再独立续挂",
    "water_deep_pressure": "低矮的重压领域。范围内暂时积起不流动的水，里面的人变慢、变潮湿，氧气消耗加快",
}
NEW_O = {
    "water_vortex": "assets/xueximod/textures/domain/water_flow_v3_1.png；挂载后的气泡膜 assets/xueximod/textures/domain/water_bubble_v3_1.png",
    "water_bubble_column": "assets/xueximod/textures/domain/water_bubble_v3_1.png",
    "water_deep_pressure": "assets/xueximod/textures/domain/deep_pressure_bands.png；assets/xueximod/textures/domain/deep_pressure_arcs.png；原版 minecraft:block/water_still、minecraft:block/water_flow、minecraft:block/water_overlay",
}
APPEND = "\n".join([
    "2026-11-03 用户明确采用：对 v3「这版下窄上宽、水下水流汇入底部的水龙卷，是否采用接入？」答「采用这版继续接入」。随后对 v3.1「采用漏斗主体、水下回旋，以及宽目标周围局部携带水带这一完整版本（v3.1）接入吗？本次替代上一条v3外观确认。」答「采用这版继续接入」。",
    "正式归档 docs/资源库/特效/水流/旋流与水龙卷_v3_1/。运行资源见本行 O。批次 docs/开发记录/术式批量制作/水流组合-20261103/记录.md。编译回执为第三次 compileJava --offline --no-daemon，退出码 0。",
    MARKER,
    QUOTA,
])
ORDER = ["water_vortex", "water_bubble_column", "water_deep_pressure"]


def fail(message):
    raise SystemExit(message)


def sha256_bytes(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def col_letter(number):
    letters = ""
    while number:
        number, remainder = divmod(number - 1, 26)
        letters = chr(65 + remainder) + letters
    return letters


def shared_items(xml):
    items = []
    for body in re.findall(r"<si\b[^>]*>(.*?)</si>", xml, re.S):
        parts = re.findall(r"<t\b[^>]*>(.*?)</t>", body, re.S)
        items.append(html.unescape("".join(parts)))
    return items


def row_map(xml):
    found = {}
    for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', xml):
        found[int(match.group(1))] = match.group(0)
    return found


def cell_value(row, column, number, shared):
    match = re.search(rf'<c r="{column}{number}\b"[^>]*(?:/>|>[\s\S]*?</c>)', row)
    if not match:
        return None
    cell = match.group(0)
    if cell.endswith("/>"):
        return ""
    if 't="inlineStr"' in cell or "<is>" in cell:
        return html.unescape("".join(re.findall(r"<t\b[^>]*>(.*?)</t>", cell, re.S)))
    raw = re.search(r"<v>(.*?)</v>", cell, re.S)
    if not raw:
        return ""
    if 't="s"' in cell:
        return shared[int(raw.group(1))]
    return raw.group(1)


def status_ids(xml, shared):
    found = []
    for number, row in row_map(xml).items():
        if number < 3:
            continue
        code = cell_value(row, "B", number, shared)
        if code:
            found.append((number, code, cell_value(row, "A", number, shared)))
    return found


def xml_text(value):
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def retarget(row_xml, old, new):
    row_xml = re.sub(
        rf'r="([A-Z]+){old}\b"',
        lambda match: f'r="{match.group(1)}{new}"',
        row_xml,
    )
    row_xml = re.sub(rf'\br="{old}"', f'r="{new}"', row_xml, count=1)
    if re.search(rf'r="[A-Z]*{old}\b"', row_xml):
        fail(f"Row still references {old}")
    return row_xml


def replace_index(row_xml, column, number, new_index):
    pattern = rf'(<c r="{column}{number}\b"[^>]*t="s"[^>]*>)<v>\d+</v>'
    matches = list(re.finditer(pattern, row_xml))
    if len(matches) != 1:
        fail(f"{column}{number} shared cell count {len(matches)}")
    match = matches[0]
    return row_xml[: match.start()] + match.group(1) + f"<v>{new_index}</v>" + row_xml[match.end() :]


def replace_once(xml, old, new, label):
    if xml.count(old) != 1:
        fail(f"{label} count {xml.count(old)}")
    return xml.replace(old, new, 1)


def sheet_names(workbook_xml, rels_xml):
    rels = {}
    for match in re.finditer(r'Id="([^"]+)"[^>]*Target="([^"]+)"', rels_xml):
        rels[match.group(1)] = match.group(2)
    names = {}
    for match in re.finditer(r'<sheet[^>]*name="([^"]+)"[^>]*r:id="([^"]+)"', workbook_xml):
        target = rels[match.group(2)]
        if not target.startswith("xl/"):
            target = "xl/" + target.lstrip("/")
        names[match.group(1)] = target.replace("\\", "/")
    return names


def main():
    original = LIVE.read_bytes()
    start_hash = sha256_bytes(original)
    if start_hash != BASELINE:
        fail(f"Source hash is {start_hash}")
    with zipfile.ZipFile(LIVE) as book:
        stored = {name: book.read(name) for name in book.namelist()}
    names = sheet_names(
        stored["xl/workbook.xml"].decode("utf-8"),
        stored["xl/_rels/workbook.xml.rels"].decode("utf-8"),
    )
    making_part = names["制作中"]
    accept_part = names["待验收"]
    shared = shared_items(stored["xl/sharedStrings.xml"].decode("utf-8"))
    making_xml = stored[making_part].decode("utf-8")
    accept_xml = stored[accept_part].decode("utf-8")
    located = {}
    for title, part in names.items():
        if title not in {"已完成", "待制作", "制作中", "待验收"}:
            continue
        for number, code, label in status_ids(stored[part].decode("utf-8"), shared):
            if code in ORDER:
                located.setdefault(code, []).append((title, number, label))
    for code in ORDER:
        if located.get(code) != [("制作中", { "water_vortex": 18, "water_bubble_column": 19, "water_deep_pressure": 20 }[code], {"water_vortex": "旋流", "water_bubble_column": "气泡柱", "water_deep_pressure": "深海重压"}[code])]:
            fail(f"{code} located at {located.get(code)}")
    making_rows = row_map(making_xml)
    headers = row_map(making_xml)[2]
    header_names = {col_letter(i): cell_value(headers, col_letter(i), 2, shared) for i in range(1, 145)}
    before = {}
    for code, places in located.items():
        number = places[0][1]
        row = making_rows[number]
        cells = {}
        for index in range(1, 145):
            column = col_letter(index)
            value = cell_value(row, column, number, shared)
            if value not in (None, ""):
                cells[column] = {"header": header_names.get(column), "value": value}
        before[code] = {"row": number, "cells": cells, "xml": row}
        for column in ("N", "O", "DV"):
            if not cells[column]["value"]:
                fail(f"{code} {column} empty")
    new_dv = {code: before[code]["cells"]["DV"]["value"] + "\n" + APPEND for code in ORDER}
    items = list(shared)
    index = {text: pos for pos, text in enumerate(items)}

    def intern(text):
        if text not in index:
            index[text] = len(items)
            items.append(text)
        return index[text]

    accept_rows = row_map(accept_xml)
    next_row = max(accept_rows) + 1
    if next_row != 10:
        fail(f"Acceptance next row is {next_row}")
    moved = {}
    accept_new = accept_xml
    making_new = making_xml
    for offset, code in enumerate(ORDER):
        old_number = before[code]["row"]
        new_number = next_row + offset
        row_xml = retarget(before[code]["xml"], old_number, new_number)
        row_xml = replace_index(row_xml, "N", new_number, intern(NEW_N[code]))
        row_xml = replace_index(row_xml, "O", new_number, intern(NEW_O[code]))
        row_xml = replace_index(row_xml, "DV", new_number, intern(new_dv[code]))
        moved[code] = new_number
        making_new = replace_once(making_new, before[code]["xml"], "", f"remove {code}")
        accept_new = accept_new.replace("</sheetData>", row_xml + "</sheetData>", 1)
    making_new = replace_once(making_new, 'ref="A1:EN20"', 'ref="A1:EN17"', "making dimension")
    accept_new = replace_once(accept_new, 'ref="A1:EN9"', 'ref="A1:EN12"', "acceptance dimension")
    workbook = stored["xl/workbook.xml"].decode("utf-8")
    workbook = replace_once(workbook, "制作中!$A$2:$EN$20", "制作中!$A$2:$EN$17", "making filter")
    additions = [
        f'<si><t xml:space="preserve">{xml_text(text)}</t></si>'
        for text in items[len(shared):]
    ]
    if len(additions) != 9:
        fail(f"Expected 9 new strings, got {len(additions)}")
    sst = stored["xl/sharedStrings.xml"].decode("utf-8").replace("</sst>", "".join(additions) + "</sst>", 1)
    count_match = re.search(r'count="(\d+)"', sst)
    sst = sst.replace(count_match.group(0), f'count="{int(count_match.group(1)) + 9}"', 1)
    sst = re.sub(r'uniqueCount="\d+"', f'uniqueCount="{len(items)}"', sst, count=1)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    readable = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    backup = WORK / "备份" / f"术式制作表-待验收-{stamp}-1.xlsx"
    candidate = WORK / "候选" / f"术式制作表-待验收-{stamp}-1.xlsx"
    backup.parent.mkdir(parents=True, exist_ok=True)
    candidate.parent.mkdir(parents=True, exist_ok=True)
    backup.write_bytes(original)
    replacements = {
        "xl/sharedStrings.xml": sst.encode("utf-8"),
        "xl/workbook.xml": workbook.encode("utf-8"),
        making_part: making_new.encode("utf-8"),
        accept_part: accept_new.encode("utf-8"),
    }
    with zipfile.ZipFile(LIVE) as incoming, zipfile.ZipFile(candidate, "w") as outgoing:
        for info in incoming.infolist():
            outgoing.writestr(info, replacements.get(info.filename, incoming.read(info.filename)))
    verify(candidate, stored, names, before, new_dv, moved)
    if sha256_bytes(LIVE.read_bytes()) != start_hash:
        fail("Source changed before replace")
    shutil.copyfile(candidate, LIVE)
    if sha256_bytes(LIVE.read_bytes()) != sha256_bytes(candidate.read_bytes()):
        shutil.copyfile(backup, LIVE)
        fail("Reopen mismatch; backup restored")
    verify(LIVE, stored, names, before, new_dv, moved)
    final_hash = sha256_bytes(LIVE.read_bytes())
    final_mtime = datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
    snapshot = {
        "capturedAt": readable,
        "sourceHashBefore": start_hash,
        "submittedHash": final_hash,
        "submittedMtime": final_mtime,
        "requirementVersion": "需求快照.md + 设计裁决-1.md；2026-11-03 用户采用 v3.1 完整版本，替代此前 v3 外观确认",
        "compile": "第三次 .\\gradlew.bat compileJava --offline --no-daemon，2026-11-03 22:31:36 至 22:32:15，退出码 0。不是游戏验收，也不是追加正式复审。",
        "quota": QUOTA,
        "archivePolicy": "待制作原始数据未改。深海重压不补历史原档。",
        "resourceMapping": {
            "water_vortex": {
                "runtime": ["assets/xueximod/textures/domain/water_flow_v3_1.png", "assets/xueximod/textures/domain/water_bubble_v3_1.png"],
                "archive": ["docs/资源库/特效/水流/旋流与水龙卷_v3_1/water_flow_v3_1.png", "docs/资源库/特效/水流/旋流与水龙卷_v3_1/bubble_v3_1.png"],
                "notIntegrated": "线框、地面、模型和动图没有进游戏",
            },
            "water_bubble_column": {
                "runtime": ["assets/xueximod/textures/domain/water_bubble_v3_1.png"],
                "archive": ["docs/资源库/特效/水流/旋流与水龙卷_v3_1/bubble_v3_1.png"],
            },
            "water_deep_pressure": {
                "runtime": ["assets/xueximod/textures/domain/deep_pressure_bands.png", "assets/xueximod/textures/domain/deep_pressure_arcs.png", "minecraft:block/water_still", "minecraft:block/water_flow", "minecraft:block/water_overlay"],
                "note": "没有新的深海确认贴图。临时水用原版水材质。",
            },
        },
        "rows": {},
    }
    for code in ORDER:
        cells = {column: item["value"] for column, item in before[code]["cells"].items()}
        snapshot["rows"][code] = {
            "from": f"制作中第{before[code]['row']}行",
            "to": f"待验收第{moved[code]}行",
            "before": {"N": cells["N"], "O": cells["O"], "DV": cells["DV"]},
            "after": {"N": NEW_N[code], "O": NEW_O[code], "DV": new_dv[code]},
            "fields": cells,
        }
    (WORK / "待验收转换前完整需求快照.json").write_text(
        json.dumps(snapshot, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print("OK", final_hash, final_mtime)
    print("BACKUP", backup)
    print("CANDIDATE", candidate)
    print("MOVED", moved)


def verify(path, stored, names, before, new_dv, moved):
    with zipfile.ZipFile(path) as book:
        now = {name: book.read(name) for name in book.namelist()}
    if set(now) != set(stored):
        fail("Zip parts changed")
    allowed = {"xl/sharedStrings.xml", "xl/workbook.xml", names["制作中"], names["待验收"]}
    for name, payload in stored.items():
        if name not in allowed and now[name] != payload:
            fail(f"Unexpected part change: {name}")
    old_book = stored["xl/workbook.xml"].decode("utf-8")
    new_book = now["xl/workbook.xml"].decode("utf-8")
    if new_book.replace("制作中!$A$2:$EN$17", "制作中!$A$2:$EN$20") != old_book:
        fail("Workbook changed beyond the making filter")
    shared = shared_items(now["xl/sharedStrings.xml"].decode("utf-8"))
    old_shared = shared_items(stored["xl/sharedStrings.xml"].decode("utf-8"))
    if shared[: len(old_shared)] != old_shared:
        fail("Existing shared strings changed")
    counts = {}
    seen = {}
    for title in ("已完成", "待制作", "制作中", "待验收"):
        ids = status_ids(now[names[title]].decode("utf-8"), shared)
        counts[title] = len(ids)
        for number, code, _label in ids:
            seen.setdefault(code, []).append((title, number))
    if counts != {"已完成": 37, "待制作": 0, "制作中": 15, "待验收": 10}:
        fail(f"Counts {counts}")
    duplicates = {code: places for code, places in seen.items() if len(places) != 1}
    if duplicates:
        fail(f"Duplicates {duplicates}")
    if seen.get("tide_sever") != [("制作中", 16)] or seen.get("water_yielding_ribbon") != [("制作中", 17)]:
        fail("Rename rows moved")
    old_making = row_map(stored[names["制作中"]].decode("utf-8"))
    new_making = row_map(now[names["制作中"]].decode("utf-8"))
    for number, row in old_making.items():
        if number in {item["row"] for item in before.values()}:
            if number in new_making:
                fail(f"Source row {number} still present")
            continue
        if new_making.get(number) != row:
            fail(f"Making row {number} changed")
    old_accept = row_map(stored[names["待验收"]].decode("utf-8"))
    new_accept = row_map(now[names["待验收"]].decode("utf-8"))
    for number, row in old_accept.items():
        if new_accept.get(number) != row:
            fail(f"Acceptance row {number} changed")
    for code in ORDER:
        number = moved[code]
        row = new_accept[number]
        if cell_value(row, "B", number, shared) != code:
            fail(f"{code} id mismatch")
        if cell_value(row, "N", number, shared) != NEW_N[code]:
            fail(f"{code} N mismatch")
        if cell_value(row, "O", number, shared) != NEW_O[code]:
            fail(f"{code} O mismatch")
        if cell_value(row, "DV", number, shared) != new_dv[code]:
            fail(f"{code} DV mismatch")
        if not new_dv[code].startswith(before[code]["cells"]["DV"]["value"]):
            fail(f"{code} lost original DV")
        if QUOTA not in new_dv[code] or MARKER not in new_dv[code]:
            fail(f"{code} missing delivery marker")
        if "上轮六项" in new_dv[code] or "整批复审通过" in new_dv[code]:
            fail("DV overclaims review")
    if "water_deep_pressure" in stored["xl/worksheets/sheet7.xml"].decode("utf-8"):
        pass
    print("verified", Path(path).name, counts)


if __name__ == "__main__":
    main()
