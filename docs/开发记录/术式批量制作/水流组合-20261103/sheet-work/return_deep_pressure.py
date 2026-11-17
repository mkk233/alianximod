# -*- coding: utf-8 -*-
"""Return water_deep_pressure from acceptance to making. Snapshot must already exist."""
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
SNAPSHOT = WORK / "运行故障-20261104返修快照.json"
BASELINE = "4FF10BA2F31A435318911777CA4C96D547AF1550DF7544BC9FB07E1CF25F9164"
CODE = "water_deep_pressure"


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
        fail("Repair snapshot is missing")
    snapshot = json.loads(SNAPSHOT.read_text(encoding="utf-8"))
    if snapshot.get("sourceHash") != BASELINE or snapshot.get("writtenBeforeWorkbookEdit") is not True:
        fail("Repair snapshot is not the pre-edit capture")
    restore = snapshot["restore"]
    original = LIVE.read_bytes()
    start_hash = sha256_bytes(original)
    if start_hash != BASELINE:
        fail(f"Source hash is {start_hash}")
    with zipfile.ZipFile(LIVE) as book:
        stored = {name: book.read(name) for name in book.namelist()}
    shared = shared_items(stored["xl/sharedStrings.xml"].decode("utf-8"))
    making_xml = stored["xl/worksheets/sheet3.xml"].decode("utf-8")
    accept_xml = stored["xl/worksheets/sheet4.xml"].decode("utf-8")
    accept_rows = row_map(accept_xml)
    making_rows = row_map(making_xml)
    source = [
        number for number, row in accept_rows.items()
        if number >= 3 and cell_value(row, "B", number, shared) == CODE
    ]
    if source != [12]:
        fail(f"Acceptance row is {source}")
    if 18 in making_rows or max(making_rows) != 17:
        fail(f"Making rows end at {max(making_rows)}")
    source_row = accept_rows[12]
    if cell_value(source_row, "N", 12, shared) != snapshot["originalNODV"]["N"]:
        fail("Live N changed after snapshot")
    if cell_value(source_row, "O", 12, shared) != snapshot["originalNODV"]["O"]:
        fail("Live O changed after snapshot")
    if cell_value(source_row, "DV", 12, shared) != snapshot["originalNODV"]["DV"]:
        fail("Live DV changed after snapshot")
    items = list(shared)
    index = {text: pos for pos, text in enumerate(items)}

    def intern(text):
        if text not in index:
            index[text] = len(items)
            items.append(text)
        return index[text]

    moved = retarget(source_row, 12, 18)
    moved = replace_index(moved, "N", 18, intern(restore["N"]))
    moved = replace_index(moved, "O", 18, intern(restore["O"]))
    moved = replace_index(moved, "DV", 18, intern(restore["DV"]))
    making_new = replace_once(making_xml, "</sheetData>", moved + "</sheetData>", "append making")
    accept_new = replace_once(accept_xml, source_row, "", "remove acceptance")
    making_new = replace_once(making_new, 'ref="A1:EN17"', 'ref="A1:EN18"', "making dimension")
    accept_new = replace_once(accept_new, 'ref="A1:EN12"', 'ref="A1:EN11"', "acceptance dimension")
    workbook = replace_once(
        stored["xl/workbook.xml"].decode("utf-8"),
        "制作中!$A$2:$EN$17",
        "制作中!$A$2:$EN$18",
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
    backup = WORK / "备份" / f"术式制作表-返修回迁-{stamp}-1.xlsx"
    candidate = WORK / "候选" / f"术式制作表-返修回迁-{stamp}-1.xlsx"
    backup.parent.mkdir(parents=True, exist_ok=True)
    candidate.parent.mkdir(parents=True, exist_ok=True)
    backup.write_bytes(original)
    replacements = {
        "xl/sharedStrings.xml": sst.encode("utf-8"),
        "xl/workbook.xml": workbook.encode("utf-8"),
        "xl/worksheets/sheet3.xml": making_new.encode("utf-8"),
        "xl/worksheets/sheet4.xml": accept_new.encode("utf-8"),
    }
    with zipfile.ZipFile(LIVE) as incoming, zipfile.ZipFile(candidate, "w") as outgoing:
        for info in incoming.infolist():
            outgoing.writestr(info, replacements.get(info.filename, incoming.read(info.filename)))
    verify(candidate, stored, restore, snapshot)
    if sha256_bytes(LIVE.read_bytes()) != start_hash:
        fail("Source changed before replace")
    shutil.copyfile(candidate, LIVE)
    if sha256_bytes(LIVE.read_bytes()) != sha256_bytes(candidate.read_bytes()):
        shutil.copyfile(backup, LIVE)
        fail("Reopen mismatch; backup restored")
    verify(LIVE, stored, restore, snapshot)
    final_hash = sha256_bytes(LIVE.read_bytes())
    final_mtime = datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
    print("OK", final_hash, final_mtime)
    print("BACKUP", backup)
    print("CANDIDATE", candidate)


def verify(path, stored, restore, snapshot):
    with zipfile.ZipFile(path) as book:
        now = {name: book.read(name) for name in book.namelist()}
    if set(now) != set(stored):
        fail("Zip parts changed")
    allowed = {"xl/sharedStrings.xml", "xl/workbook.xml", "xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml"}
    for name, payload in stored.items():
        if name not in allowed and now[name] != payload:
            fail(f"Unexpected part change: {name}")
    old_book = stored["xl/workbook.xml"].decode("utf-8")
    new_book = now["xl/workbook.xml"].decode("utf-8")
    if new_book.replace("制作中!$A$2:$EN$18", "制作中!$A$2:$EN$17") != old_book:
        fail("Workbook changed beyond the making filter")
    shared = shared_items(now["xl/sharedStrings.xml"].decode("utf-8"))
    old_shared = shared_items(stored["xl/sharedStrings.xml"].decode("utf-8"))
    if shared[: len(old_shared)] != old_shared:
        fail("Existing shared strings changed")
    names = {
        "已完成": "xl/worksheets/sheet1.xml",
        "待制作": "xl/worksheets/sheet2.xml",
        "制作中": "xl/worksheets/sheet3.xml",
        "待验收": "xl/worksheets/sheet4.xml",
    }
    counts = {}
    seen = {}
    for title, part in names.items():
        ids = status_ids(now[part].decode("utf-8"), shared)
        counts[title] = len(ids)
        for number, code in ids:
            seen.setdefault(code, []).append((title, number))
    if counts != {"已完成": 37, "待制作": 0, "制作中": 16, "待验收": 9}:
        fail(f"Counts {counts}")
    duplicates = {code: places for code, places in seen.items() if len(places) != 1}
    if duplicates:
        fail(f"Duplicates {duplicates}")
    if seen.get(CODE) != [("制作中", 18)]:
        fail(f"{CODE} at {seen.get(CODE)}")
    if seen.get("water_vortex") != [("待验收", 10)] or seen.get("water_bubble_column") != [("待验收", 11)]:
        fail("Vortex or bubble moved")
    if seen.get("tide_sever") != [("制作中", 16)] or seen.get("water_yielding_ribbon") != [("制作中", 17)]:
        fail("Rename rows moved")
    old_making = row_map(stored["xl/worksheets/sheet3.xml"].decode("utf-8"))
    new_making = row_map(now["xl/worksheets/sheet3.xml"].decode("utf-8"))
    for number, row in old_making.items():
        if new_making.get(number) != row:
            fail(f"Making row {number} changed")
    if 18 not in new_making:
        fail("Target row missing")
    old_accept = row_map(stored["xl/worksheets/sheet4.xml"].decode("utf-8"))
    new_accept = row_map(now["xl/worksheets/sheet4.xml"].decode("utf-8"))
    for number, row in old_accept.items():
        if number == 12:
            if number in new_accept:
                fail("Source row remains")
            continue
        if new_accept.get(number) != row:
            fail(f"Acceptance row {number} changed")
    row = new_making[18]
    if cell_value(row, "N", 18, shared) != restore["N"]:
        fail("Restored N mismatch")
    if cell_value(row, "O", 18, shared) != restore["O"]:
        fail("Restored O mismatch")
    if cell_value(row, "DV", 18, shared) != restore["DV"]:
        fail("Restored DV mismatch")
    if not restore["DV"].startswith(snapshot["originalNODV"]["DV"]):
        fail("DV lost original wording")
    if "当前组合v3待确认」不再表示当前状态" not in restore["O"]:
        fail("O does not mark the draft sentence as history")
    if "已游戏验收" in restore["DV"] or "游戏验收通过" in restore["DV"] or "整批复审通过" in restore["DV"]:
        fail("DV overclaims")
    if now["xl/worksheets/sheet6.xml"] != stored["xl/worksheets/sheet6.xml"]:
        fail("Details changed")
    if now["xl/worksheets/sheet7.xml"] != stored["xl/worksheets/sheet7.xml"]:
        fail("Archive changed")
    print("verified", Path(path).name, counts)


if __name__ == "__main__":
    main()
