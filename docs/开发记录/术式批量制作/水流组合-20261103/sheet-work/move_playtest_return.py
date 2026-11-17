# -*- coding: utf-8 -*-
"""Return the three water rows to making. The playtest snapshot must already exist."""
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
SNAPSHOT = WORK / "实测三项-20261104返修快照.json"
BASELINE = "5CD41F680C9E0E98EF8A3BB191B6F2E29BE85F7D9FFE49882BF1224591F83305"
ORDER = ["water_vortex", "water_bubble_column", "water_deep_pressure"]
QUOTE = "水流漩涡的特效完全和你给的预览不一样，现在只有几条直线出现在这里"


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
        fail("Playtest snapshot is missing")
    snapshot = json.loads(SNAPSHOT.read_text(encoding="utf-8"))
    if snapshot.get("sourceHash") != BASELINE or snapshot.get("writtenBeforeWorkbookEdit") is not True:
        fail("Playtest snapshot is not the pre-edit capture")
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
    if max(making_rows) != 17 or any(number in making_rows for number in (18, 19, 20)):
        fail(f"Making rows end at {max(making_rows)}")
    items = list(shared)
    index = {text: pos for pos, text in enumerate(items)}

    def intern(text):
        if text not in index:
            index[text] = len(items)
            items.append(text)
        return index[text]

    moved_rows = {}
    accept_new = accept_xml
    for offset, code in enumerate(ORDER):
        source_number = snapshot["rows"][code]["row"]
        source_row = accept_rows[source_number]
        if cell_value(source_row, "B", source_number, shared) != code:
            fail(f"{code} is not at row {source_number}")
        if cell_value(source_row, "DV", source_number, shared) != snapshot["rows"][code]["originalNODV"]["DV"]:
            fail(f"{code} DV changed after snapshot")
        target_number = 18 + offset
        moved = retarget(source_row, source_number, target_number)
        restore = snapshot["rows"][code]["restore"]
        moved = replace_index(moved, "N", target_number, intern(restore["N"]))
        moved = replace_index(moved, "O", target_number, intern(restore["O"]))
        moved = replace_index(moved, "DV", target_number, intern(restore["DV"]))
        moved_rows[code] = target_number
        accept_new = replace_once(accept_new, source_row, "", f"remove {code}")
        making_xml = replace_once(making_xml, "</sheetData>", moved + "</sheetData>", f"append {code}")
    making_new = replace_once(making_xml, 'ref="A1:EN17"', 'ref="A1:EN20"', "making dimension")
    accept_new = replace_once(accept_new, 'ref="A1:EN12"', 'ref="A1:EN9"', "acceptance dimension")
    workbook = replace_once(
        stored["xl/workbook.xml"].decode("utf-8"),
        "制作中!$A$2:$EN$17",
        "制作中!$A$2:$EN$20",
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
    backup = WORK / "备份" / f"术式制作表-实测回迁-{stamp}-1.xlsx"
    candidate = WORK / "候选" / f"术式制作表-实测回迁-{stamp}-1.xlsx"
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
    verify(candidate, stored, names, snapshot, moved_rows)
    if sha256_bytes(LIVE.read_bytes()) != start_hash:
        fail("Source changed before replace")
    shutil.copyfile(candidate, LIVE)
    if sha256_bytes(LIVE.read_bytes()) != sha256_bytes(candidate.read_bytes()):
        shutil.copyfile(backup, LIVE)
        fail("Reopen mismatch; backup restored")
    verify(LIVE, stored, names, snapshot, moved_rows)
    print("OK", sha256_bytes(LIVE.read_bytes()), datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3])
    print("BACKUP", backup)
    print("CANDIDATE", candidate)
    print("MOVED", moved_rows)


def verify(path, stored, names, snapshot, moved_rows):
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
    if new_book.replace("制作中!$A$2:$EN$20", "制作中!$A$2:$EN$17") != old_book:
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
    if counts != {"已完成": 37, "待制作": 0, "制作中": 18, "待验收": 7}:
        fail(f"Counts {counts}")
    duplicates = {code: places for code, places in seen.items() if len(places) != 1}
    if duplicates:
        fail(f"Duplicates {duplicates}")
    if seen.get("tide_sever") != [("制作中", 16)] or seen.get("water_yielding_ribbon") != [("制作中", 17)]:
        fail("Rename rows moved")
    old_making = row_map(stored[names["制作中"]].decode("utf-8"))
    new_making = row_map(now[names["制作中"]].decode("utf-8"))
    for number, row in old_making.items():
        if new_making.get(number) != row:
            fail(f"Making row {number} changed")
    old_accept = row_map(stored[names["待验收"]].decode("utf-8"))
    new_accept = row_map(now[names["待验收"]].decode("utf-8"))
    removed = {item["row"] for item in snapshot["rows"].values()}
    for number, row in old_accept.items():
        if number in removed:
            if number in new_accept:
                fail(f"Acceptance row {number} remains")
            continue
        if new_accept.get(number) != row:
            fail(f"Acceptance row {number} changed")
    for code in ORDER:
        number = moved_rows[code]
        row = new_making[number]
        restore = snapshot["rows"][code]["restore"]
        if seen.get(code) != [("制作中", number)]:
            fail(f"{code} at {seen.get(code)}")
        if cell_value(row, "N", number, shared) != restore["N"]:
            fail(f"{code} N mismatch")
        if cell_value(row, "O", number, shared) != restore["O"]:
            fail(f"{code} O mismatch")
        if cell_value(row, "DV", number, shared) != restore["DV"]:
            fail(f"{code} DV mismatch")
        if not restore["DV"].startswith(snapshot["rows"][code]["originalNODV"]["DV"]):
            fail(f"{code} lost original DV")
        if QUOTE not in restore["DV"] or "本次无正式复审" not in restore["DV"]:
            fail(f"{code} missing user quote")
        if restore["O"].rstrip().endswith("待确认。"):
            fail(f"{code} O unconfirmed")
    if "不主动替换植物。" in snapshot["rows"]["water_deep_pressure"]["restore"]["N"]:
        fail("Deep plant rule not superseded")
    if "ChunkEvent.Load" not in snapshot["rows"]["water_deep_pressure"]["restore"]["N"]:
        fail("Chunk load fix missing from deep N")
    print("verified", Path(path).name, counts)


if __name__ == "__main__":
    main()
