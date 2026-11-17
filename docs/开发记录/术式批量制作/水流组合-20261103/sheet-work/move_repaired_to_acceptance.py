# -*- coding: utf-8 -*-
"""Move repaired water_deep_pressure back to pending acceptance."""
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
SNAPSHOT = WORK / "运行故障-20261104待验收快照.json"
BASELINE = "37DC28E3671359455DE1504F96CE10344920D256E8BD971D4350A76888615591"
CODE = "water_deep_pressure"
QUOTA = "本阶段额度已用完，返修结果未追加复审。"


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
            found.append((number, code))
    return found


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


def main():
    if not SNAPSHOT.is_file():
        fail("Delivery snapshot is missing")
    snapshot = json.loads(SNAPSHOT.read_text(encoding="utf-8"))
    if snapshot.get("sourceHash") != BASELINE or snapshot.get("writtenBeforeWorkbookEdit") is not True:
        fail("Delivery snapshot is not the pre-edit capture")
    after = snapshot["after"]
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
    shared = shared_items(stored["xl/sharedStrings.xml"].decode("utf-8"))
    making_part = names["制作中"]
    accept_part = names["待验收"]
    making_xml = stored[making_part].decode("utf-8")
    accept_xml = stored[accept_part].decode("utf-8")
    making_rows = row_map(making_xml)
    accept_rows = row_map(accept_xml)
    source_number = snapshot["currentRow"]["row"]
    if cell_value(making_rows[source_number], "B", source_number, shared) != CODE:
        fail("Snapshot row no longer matches B")
    if cell_value(making_rows[source_number], "DV", source_number, shared) != snapshot["originalNODV"]["DV"]:
        fail("Live DV changed after snapshot")
    target_number = max(accept_rows) + 1
    if target_number != 12:
        fail(f"Acceptance next row is {target_number}")
    items = list(shared)
    index = {text: pos for pos, text in enumerate(items)}

    def intern(text):
        if text not in index:
            index[text] = len(items)
            items.append(text)
        return index[text]

    source_row = making_rows[source_number]
    moved = retarget(source_row, source_number, target_number)
    moved = replace_index(moved, "N", target_number, intern(after["N"]))
    moved = replace_index(moved, "O", target_number, intern(after["O"]))
    moved = replace_index(moved, "DV", target_number, intern(after["DV"]))
    making_new = replace_once(making_xml, source_row, "", "remove making")
    accept_new = replace_once(accept_xml, "</sheetData>", moved + "</sheetData>", "append acceptance")
    making_new = replace_once(making_new, 'ref="A1:EN18"', 'ref="A1:EN17"', "making dimension")
    accept_new = replace_once(accept_new, 'ref="A1:EN11"', 'ref="A1:EN12"', "acceptance dimension")
    workbook = replace_once(
        stored["xl/workbook.xml"].decode("utf-8"),
        "制作中!$A$2:$EN$18",
        "制作中!$A$2:$EN$17",
        "making filter",
    )
    additions = [
        f'<si><t xml:space="preserve">{xml_text(text)}</t></si>'
        for text in items[len(shared):]
    ]
    sst = stored["xl/sharedStrings.xml"].decode("utf-8")
    if additions:
        sst = sst.replace("</sst>", "".join(additions) + "</sst>", 1)
    sst = re.sub(r'uniqueCount="\d+"', f'uniqueCount="{len(items)}"', sst, count=1)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = WORK / "备份" / f"术式制作表-修复交付-{stamp}-1.xlsx"
    candidate = WORK / "候选" / f"术式制作表-修复交付-{stamp}-1.xlsx"
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
    verify(candidate, stored, names, snapshot, source_number, target_number)
    if sha256_bytes(LIVE.read_bytes()) != start_hash:
        fail("Source changed before replace")
    shutil.copyfile(candidate, LIVE)
    if sha256_bytes(LIVE.read_bytes()) != sha256_bytes(candidate.read_bytes()):
        shutil.copyfile(backup, LIVE)
        fail("Reopen mismatch; backup restored")
    verify(LIVE, stored, names, snapshot, source_number, target_number)
    print("OK", sha256_bytes(LIVE.read_bytes()), datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3])
    print("BACKUP", backup)
    print("CANDIDATE", candidate)
    print("MOVED", source_number, target_number)


def verify(path, stored, names, snapshot, source_number, target_number):
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
    if new_book.replace("制作中!$A$2:$EN$17", "制作中!$A$2:$EN$18") != old_book:
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
        for number, code in ids:
            seen.setdefault(code, []).append((title, number))
    if counts != {"已完成": 37, "待制作": 0, "制作中": 15, "待验收": 10}:
        fail(f"Counts {counts}")
    duplicates = {code: places for code, places in seen.items() if len(places) != 1}
    if duplicates:
        fail(f"Duplicates {duplicates}")
    if seen.get(CODE) != [("待验收", target_number)]:
        fail(f"{CODE} at {seen.get(CODE)}")
    if seen.get("water_vortex") != [("待验收", 10)] or seen.get("water_bubble_column") != [("待验收", 11)]:
        fail("Vortex or bubble moved")
    if seen.get("tide_sever") != [("制作中", 16)] or seen.get("water_yielding_ribbon") != [("制作中", 17)]:
        fail("Rename rows moved")
    old_making = row_map(stored[names["制作中"]].decode("utf-8"))
    new_making = row_map(now[names["制作中"]].decode("utf-8"))
    for number, row in old_making.items():
        if number == source_number:
            if number in new_making:
                fail("Source row remains")
            continue
        if new_making.get(number) != row:
            fail(f"Making row {number} changed")
    old_accept = row_map(stored[names["待验收"]].decode("utf-8"))
    new_accept = row_map(now[names["待验收"]].decode("utf-8"))
    for number, row in old_accept.items():
        if new_accept.get(number) != row:
            fail(f"Acceptance row {number} changed")
    row = new_accept[target_number]
    after = snapshot["after"]
    if cell_value(row, "N", target_number, shared) != after["N"]:
        fail("N mismatch")
    if cell_value(row, "O", target_number, shared) != after["O"]:
        fail("O mismatch")
    if cell_value(row, "DV", target_number, shared) != after["DV"]:
        fail("DV mismatch")
    if not after["DV"].startswith(snapshot["originalNODV"]["DV"]):
        fail("DV lost original wording")
    if QUOTA not in after["DV"] or "仍待用户重新进世界并实测术式" not in after["DV"]:
        fail("DV missing delivery note")
    if "已游戏验收" in after["DV"] or "游戏验收通过" in after["DV"] or "整批复审通过" in after["DV"]:
        fail("DV overclaims")
    print("verified", Path(path).name, counts)


if __name__ == "__main__":
    main()
