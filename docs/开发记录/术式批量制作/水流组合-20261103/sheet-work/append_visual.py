# -*- coding: utf-8 -*-
"""Append the latest visual intent to three in-progress rows. No page move."""
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
SNAPSHOT = WORK / "本次领取需求快照.json"
SNAPSHOT_V1 = WORK / "本次领取需求快照-v1.json"
BASELINE = "14A7833B277D6A6D2B5417EFA20F6284E17EA52F4DF3E9A505F97C5CC3AC1771"
MAKING = "xl/worksheets/sheet3.xml"

TARGETS = {
    18: "water_vortex",
    19: "water_bubble_column",
    20: "water_deep_pressure",
}

NEW_O = {
    "water_vortex": "独立平面旋流保持饱满回旋水带；组成水龙卷后外形明确下窄上宽，水面下有分层回旋流向底部窄口并有气泡，水面上有饱满错相上升水带；方块实体仍需有可见间隙，8格吸引不变。当前v3实际Blockbench草稿待用户确认。",
    "water_bubble_column": "独立水中可见上升泡膜；附加旋流时，气泡穿过水下回旋层并连续进入下窄上宽的上升水流；仅有独立气泡时不凭空制造旋转水龙卷。当前组合v3待确认。",
    "water_deep_pressure": "保留现有重压领域效果，增加真实水的正常水面/水下外观；与旋流、气泡形成组合时，水域内部可看见回旋和上升气泡。深海单独释放不自动获得另两术式的水龙卷外观。当前组合v3待确认。",
}

APPEND = "\n".join([
    "2026-11-03 用户后续原话：「水流需要更饱满，并且水域中也应该可以看到水流的特效」。",
    "随后用户原话：「漩涡应该下窄上宽」。",
    "O 已按这两句写成当前文字意图。当前草稿是 v3，尚未用户确认，不写已采用，也不换成资源名。",
])


def fail(message):
    raise SystemExit(message)


def sha256_bytes(payload):
    return hashlib.sha256(payload).hexdigest().upper()


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


def cell_xml(row, column, number):
    match = re.search(rf'<c r="{column}{number}\b"[^>]*(?:/>|>[\s\S]*?</c>)', row)
    if not match:
        fail(f"Missing {column}{number}")
    return match.group(0)


def cell_value(row, column, number, shared):
    cell = cell_xml(row, column, number)
    if cell.endswith("/>"):
        return ""
    if 't="inlineStr"' in cell or "<is>" in cell:
        texts = re.findall(r"<t\b[^>]*>(.*?)</t>", cell, re.S)
        return html.unescape("".join(texts))
    raw = re.search(r"<v>(.*?)</v>", cell, re.S)
    if not raw:
        return ""
    if 't="s"' in cell:
        return shared[int(raw.group(1))]
    return raw.group(1)


def xml_text(value):
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def status_ids(xml, shared):
    found = []
    for number, row in row_map(xml).items():
        if number < 3:
            continue
        code = cell_value(row, "B", number, shared)
        if code:
            found.append((number, code))
    return found


def replace_index(sheet, column, number, new_index):
    pattern = rf'(<c r="{column}{number}\b"[^>]*t="s"[^>]*>)<v>\d+</v>'
    match = re.search(pattern, sheet)
    if not match:
        fail(f"Cannot retarget {column}{number}")
    if len(re.findall(pattern, sheet)) != 1:
        fail(f"{column}{number} is not unique")
    return sheet[: match.start()] + match.group(1) + f"<v>{new_index}</v>" + sheet[match.end() :]


def main():
    original = LIVE.read_bytes()
    start_hash = sha256_bytes(original)
    if start_hash != BASELINE:
        fail(f"Source hash is {start_hash}, baseline {BASELINE}")
    with zipfile.ZipFile(LIVE, "r") as book:
        stored = {name: book.read(name) for name in book.namelist()}
    shared_xml = stored["xl/sharedStrings.xml"].decode("utf-8")
    shared = shared_items(shared_xml)
    making = stored[MAKING].decode("utf-8")
    rows = row_map(making)
    current = {}
    for number, code in TARGETS.items():
        row = rows[number]
        actual = cell_value(row, "B", number, shared)
        if actual != code:
            fail(f"Row {number} is {actual}, expected {code}")
        current[code] = {
            "N": cell_value(row, "N", number, shared),
            "O": cell_value(row, "O", number, shared),
            "DV": cell_value(row, "DV", number, shared),
            "row": row,
        }
        if "v3" in (current[code]["O"] or "") and "下窄上宽" in (current[code]["O"] or ""):
            fail(f"{code} O already contains the v3 narrow-base wording")
    snapshot = json.loads(SNAPSHOT.read_text(encoding="utf-8"))
    for code, values in current.items():
        recorded_o = snapshot["techniques"][code]["O"]
        recorded_dv = snapshot["techniques"][code]["DV"]
        if values["O"] != recorded_o or values["DV"] != recorded_dv:
            fail(f"{code} sheet O/DV differs from the claim snapshot")
        if code != "water_deep_pressure" and values["N"] != snapshot["techniques"][code]["N"]:
            fail(f"{code} N differs from the claim snapshot")
        if code == "water_deep_pressure" and values["N"] != snapshot["techniques"][code]["N"]:
            fail("Deep N differs from the claim snapshot")

    new_dv = {code: values["DV"] + "\n" + APPEND for code, values in current.items()}
    items = list(shared)
    index = {text: pos for pos, text in enumerate(items)}

    def intern(text):
        if text in index:
            return index[text]
        index[text] = len(items)
        items.append(text)
        return index[text]

    updated = making
    new_indexes = {}
    for number, code in TARGETS.items():
        o_index = intern(NEW_O[code])
        dv_index = intern(new_dv[code])
        updated = replace_index(updated, "O", number, o_index)
        updated = replace_index(updated, "DV", number, dv_index)
        new_indexes[code] = {"O": o_index, "DV": dv_index}
    additions = []
    for text in items[len(shared) :]:
        additions.append(f'<si><t xml:space="preserve">{xml_text(text)}</t></si>')
    sst = shared_xml.replace("</sst>", "".join(additions) + "</sst>", 1)
    count_match = re.search(r'count="(\d+)"', sst)
    sst = sst.replace(count_match.group(0), f'count="{int(count_match.group(1)) + 6}"', 1)
    sst = re.sub(r'uniqueCount="\d+"', f'uniqueCount="{len(items)}"', sst, count=1)

    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    readable = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    backup_dir = WORK / "备份"
    candidate_dir = WORK / "候选"
    backup_dir.mkdir(parents=True, exist_ok=True)
    candidate_dir.mkdir(parents=True, exist_ok=True)
    backup = backup_dir / f"术式制作表-视觉补充-{stamp}-1.xlsx"
    candidate = candidate_dir / f"术式制作表-视觉补充-{stamp}-1.xlsx"
    backup.write_bytes(original)
    with zipfile.ZipFile(LIVE, "r") as incoming, zipfile.ZipFile(candidate, "w") as outgoing:
        for info in incoming.infolist():
            payload = incoming.read(info.filename)
            if info.filename == "xl/sharedStrings.xml":
                payload = sst.encode("utf-8")
            elif info.filename == MAKING:
                payload = updated.encode("utf-8")
            outgoing.writestr(info, payload)

    verify(candidate, stored, shared, current, new_dv)
    if sha256_bytes(LIVE.read_bytes()) != start_hash:
        fail("Source changed before replace")
    shutil.copyfile(candidate, LIVE)
    if sha256_bytes(LIVE.read_bytes()) != sha256_bytes(candidate.read_bytes()):
        shutil.copyfile(backup, LIVE)
        fail("Reopen hash mismatch; backup restored")
    verify(LIVE, stored, shared, current, new_dv)
    final_hash = sha256_bytes(LIVE.read_bytes())
    final_mtime = datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
    shutil.copyfile(SNAPSHOT, SNAPSHOT_V1)
    snapshot["v1历史快照"] = {
        "file": str(SNAPSHOT_V1),
        "submittedHash": snapshot.get("submittedHash"),
        "O": {code: current[code]["O"] for code in TARGETS.values()},
        "DV": {code: current[code]["DV"] for code in TARGETS.values()},
    }
    snapshot["visualUpdate"] = {
        "at": readable,
        "sourceHashBefore": start_hash,
        "submittedHash": final_hash,
        "submittedMtime": final_mtime,
        "changed": "制作中18/19/20的O与DV。N、其他列、页和待制作原始数据未改。",
        "draft": "v3待确认，未写已采用资源名",
    }
    snapshot["submittedHash"] = final_hash
    snapshot["submittedMtime"] = final_mtime
    for code in TARGETS.values():
        snapshot["techniques"][code]["O"] = NEW_O[code]
        snapshot["techniques"][code]["DV"] = new_dv[code]
    SNAPSHOT.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8")
    print("OK", final_hash, final_mtime)
    print("BACKUP", backup)
    print("CANDIDATE", candidate)


def verify(path, stored, old_shared, current, new_dv):
    with zipfile.ZipFile(path, "r") as book:
        now = {name: book.read(name) for name in book.namelist()}
    if set(now) != set(stored):
        fail("Zip parts changed")
    for name, payload in stored.items():
        if name in {"xl/sharedStrings.xml", MAKING}:
            continue
        if now[name] != payload:
            fail(f"Unexpected part change: {name}")
    shared = shared_items(now["xl/sharedStrings.xml"].decode("utf-8"))
    old_making = stored[MAKING].decode("utf-8")
    new_making = now[MAKING].decode("utf-8")
    old_rows = row_map(old_making)
    new_rows = row_map(new_making)
    for number, row in old_rows.items():
        if number in TARGETS:
            continue
        if new_rows.get(number) != row:
            fail(f"Making row {number} changed")
    counts = {}
    seen = {}
    for title, part in {
        "已完成": "xl/worksheets/sheet1.xml",
        "待制作": "xl/worksheets/sheet2.xml",
        "制作中": MAKING,
        "待验收": "xl/worksheets/sheet4.xml",
    }.items():
        ids = status_ids(now[part].decode("utf-8"), shared)
        counts[title] = len(ids)
        for number, code in ids:
            seen.setdefault(code, []).append((title, number))
    if counts != {"已完成": 37, "待制作": 0, "制作中": 18, "待验收": 7}:
        fail(f"Counts {counts}")
    duplicates = {code: places for code, places in seen.items() if len(places) != 1}
    if duplicates:
        fail(f"Duplicates {duplicates}")
    if seen.get("tide_sever") != [("制作中", 16)] or seen.get("water_yielding_ribbon") != [("制作中", 17)]:
        fail("Rename rows moved")
    for number, code in TARGETS.items():
        row = new_rows[number]
        if cell_value(row, "N", number, shared) != current[code]["N"]:
            fail(f"{code} N changed")
        if cell_value(row, "O", number, shared) != NEW_O[code]:
            fail(f"{code} O mismatch")
        if cell_value(row, "DV", number, shared) != new_dv[code]:
            fail(f"{code} DV mismatch")
        if not new_dv[code].startswith(current[code]["DV"]):
            fail(f"{code} DV lost original text")
        if "水流需要更饱满，并且水域中也应该可以看到水流的特效" not in new_dv[code]:
            fail("Fullness quote missing")
        if "漩涡应该下窄上宽" not in new_dv[code]:
            fail("Shape quote missing")
        if "已采用" in cell_value(row, "O", number, shared):
            fail("O says the draft was adopted")
        old_without_targets = re.sub(
            rf'(<c r="(?:O|DV){number}\b"[^>]*>)<v>\d+</v>',
            r"\1<v>0</v>",
            old_rows[number],
        )
        new_without_targets = re.sub(
            rf'(<c r="(?:O|DV){number}\b"[^>]*>)<v>\d+</v>',
            r"\1<v>0</v>",
            row,
        )
        if old_without_targets != new_without_targets:
            fail(f"{code} row changed outside O/DV")
    archive = now["xl/worksheets/sheet7.xml"]
    if archive != stored["xl/worksheets/sheet7.xml"]:
        fail("Archive changed")
    if any(old_shared[i] != shared[i] for i in range(len(old_shared))):
        fail("Existing shared strings changed")
    print("verified", Path(path).name, counts)


if __name__ == "__main__":
    main()
