# -*- coding: utf-8 -*-
"""Add the eight release rows, then archive and move them.

The candidate is a surgical copy of the original package. Unchanged worksheet
parts stay byte-identical so validations, comments, and existing rows survive.
"""
import hashlib
import html
import json
import re
import shutil
import sys
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parent
LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
BACKUP_DIR = ROOT / "备份"
CANDIDATE_DIR = ROOT / "候选"
BASELINE = "137F6BC91F1AA2F61A5D28A051F316348DB84E8C845BFDA8D2BF3FFC6F411145"
QUOTE = (
    "帮我做几个术式\n"
    "扩展多重释放和一分为N\n"
    "三重释放、四重释放、8重释放16重释放、穷尽释放\n"
    "一分为二、一分为5，一分为8\n"
    "他们应该用同样的底座功能只是数值不一样而已，记得遵循流程"
)
DV_BASE = (
    QUOTE
    + "\n来源：用户2026-11-09直接需求。输入版本 user-release-variants-20261109-v1。\n"
    + "记录：docs/开发记录/术式批量制作/多重与分裂扩展-20261109/记录.md\n"
    + "本批方案默认沿用同类冷却/耗魔，数量由声明决定；开发中，尚未编译/游戏验收。"
)
MARKER = "制作中 | 多重与分裂扩展-20261109 | docs/开发记录/术式批量制作/多重与分裂扩展-20261109/记录.md"
O_TEXT = "规则自身不新增独立特效或美术，沿用现有通用规则图标；宿主视觉继续由原宿主和共享多重/复制底座生成。"
ITEMS = [
    ("rule_triple_release", "三重释放", "multi", 3),
    ("rule_quadruple_release", "四重释放", "multi", 4),
    ("rule_octuple_release", "八重释放", "multi", 8),
    ("rule_sixteen_release", "十六重释放", "multi", 16),
    ("rule_exhaustive_release", "穷尽释放", "remain", None),
    ("rule_double_cast", "一分为二", "split", 2),
    ("rule_quintuple_cast", "一分为五", "split", 5),
    ("rule_octuple_cast", "一分为八", "split", 8),
]


def sha256(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def multicast_n(count):
    return (
        f"沿用双重释放的共享多重释放机制，从自身之后的原始装配格开始，按原始格顺序向后读取最多{count}格；"
        f"修饰、附魔、复制声明与内层多重仍按既有解析机制处理，{count}不是成功攻击次数。"
        "遇链尾停止，不回绕、不补足；实际成员释放间隔沿用既有3tick。"
        "与同类术式仅数量参数不同，不建立专属执行分支。"
    )


def split_n(count):
    return (
        f"沿用一分为三的共享复制机制，通过ReplicationSpec.of({count})把后续第一个接受复制的宿主展开为{count}份；"
        "作用范围、复制与多重的组合顺序、嵌套乘积、生成几何及命中结算均沿用共享复制底座，"
        "与同类术式仅份数不同，不按术式ID单独实现。"
        "现有底座接受屏障/领域/护体/场地/条件释放等宿主，完全沿用既有资格。"
    )


def requirement(kind, count):
    if kind == "multi":
        return multicast_n(count)
    if kind == "remain":
        return (
            "沿用同一多重释放底座，使用remaining模式，从自身下一个原始装配格读取到当前链尾；"
            "包含的各类规则仍走共享解析机制，实际成员间隔沿用既有3tick。"
            "不回绕、不无限循环，也不表示持续释放直到魔力耗尽。"
        )
    return split_n(count)


def load_package(payload):
    with zipfile.ZipFile(Path(payload) if isinstance(payload, Path) else __import__("io").BytesIO(payload)) as book:
        entries = book.infolist()
        data = {info.filename: book.read(info.filename) for info in entries}
    return entries, data


def load_bytes(payload):
    if isinstance(payload, (bytes, bytearray)):
        import io
        with zipfile.ZipFile(io.BytesIO(payload)) as book:
            entries = book.infolist()
            data = {info.filename: book.read(info.filename) for info in entries}
        return entries, data
    return load_package(payload)


def sheet_map(data):
    ns = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
    rel = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"
    rels = ET.fromstring(data["xl/_rels/workbook.xml.rels"])
    targets = {}
    for node in rels:
        target = "xl/" + node.attrib["Target"].lstrip("/")
        targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
    work = ET.fromstring(data["xl/workbook.xml"])
    names = {}
    for node in work.find("s:sheets", ns):
        names[node.attrib["name"]] = targets[node.attrib[rel]]
    return names


def shared_items(sst):
    return ["".join(ET.fromstring(body).itertext())
            for body in re.findall(r"<si\b[^>]*>[\s\S]*?</si>", sst)]


def rows_of(text):
    return {int(match.group(1)): match.group(0) for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text)}


def cell_map(row_xml, items):
    found = {}
    for match in re.finditer(r'<c\b([^>]*?)(?:/>|>([\s\S]*?)</c>)', row_xml):
        attrs = match.group(1)
        inner = match.group(2) or ""
        ref = re.search(r'r="([A-Z]+)\d+"', attrs)
        if not ref:
            continue
        column = ref.group(1)
        kind = re.search(r't="([^"]+)"', attrs)
        value = re.search(r"<v>([\s\S]*?)</v>", inner)
        parsed = value.group(1) if value else None
        if kind and kind.group(1) == "s" and parsed is not None:
            parsed = items[int(parsed)]
        found[column] = parsed
    return found


def retarget(row_xml, old, new, height):
    row_xml = row_xml.replace(f'<row r="{old}"', f'<row r="{new}"', 1)
    row_xml = re.sub(rf'r="([A-Z]+){old}"', lambda match: f'r="{match.group(1)}{new}"', row_xml)
    row_xml, count = re.subn(r'ht="[\d.]+"', f'ht="{height}"', row_xml, count=1)
    if count != 1:
        raise SystemExit(f"Row {new} has no height")
    return row_xml


def replace_string_cell(row_xml, column, row_number, index):
    pattern = rf'<c r="{column}{row_number}"([^>]*?)(?:/>|>(?:[\s\S]*?)</c>)'

    def replacer(match):
        style = re.search(r's="(\d+)"', match.group(1))
        style_id = style.group(1) if style else "9"
        return f'<c r="{column}{row_number}" s="{style_id}" t="s"><v>{index}</v></c>'

    updated, count = re.subn(pattern, replacer, row_xml, count=1)
    if count != 1:
        raise SystemExit(f"Could not replace {column}{row_number}")
    return updated


def text_lines(text, width):
    per_line = max(8, int(width / 2))
    total = 0
    for line in text.split("\n"):
        units = 0
        for char in line:
            units += 1 if ord(char) > 127 else 0.55
        total += max(1, int((units + per_line - 1) // per_line))
    return total


def row_height(note, description):
    lines = max(text_lines(description, 80), text_lines(O_TEXT, 52), text_lines(note, 90), text_lines("十六重释放", 14))
    return max(48, int(lines * 18 + 12))


def intern_factory(sst):
    items = shared_items(sst)
    lookup = {}
    for index, value in enumerate(items):
        lookup.setdefault(value, index)
    added = []

    def intern(value):
        if value not in lookup:
            lookup[value] = len(items) + len(added)
            added.append(value)
        return lookup[value]

    return intern, added, items


def append_strings(sst, added, reference_delta):
    header = re.search(r'count="(\d+)"\s+uniqueCount="(\d+)"', sst)
    if not header:
        raise SystemExit("Shared string header missing")
    count = int(header.group(1)) + reference_delta
    unique = int(header.group(2)) + len(added)
    sst = sst.replace(header.group(0), f'count="{count}" uniqueCount="{unique}"', 1)
    if added:
        block = "".join('<si><t xml:space="preserve">' + html.escape(value, quote=False) + "</t></si>" for value in added)
        sst = sst.replace("</sst>", block + "</sst>", 1)
    return sst


def string_refs(xml):
    return len(re.findall(r'\bt="s"', xml))


def write_package(entries, data, patch, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(destination, "w") as book:
        for entry in entries:
            book.writestr(entry, patch.get(entry.filename, data[entry.filename]))
    with zipfile.ZipFile(destination) as book:
        changed = [name for name in data if data[name] != book.read(name)]
    if set(changed) != set(patch):
        raise SystemExit("Unexpected changed parts: " + ",".join(changed))
    return changed


def ids_from(text, items):
    found = []
    for number, row in sorted(rows_of(text).items()):
        if number < 3:
            continue
        cells = cell_map(row, items)
        value = cells.get("B")
        if value not in (None, ""):
            found.append((number, str(value).strip(), cells.get("A")))
    return found


def build_transcribe(source_bytes):
    entries, data = load_bytes(source_bytes)
    names = sheet_map(data)
    done = data[names["已完成"]].decode("utf-8")
    pending = data[names["待制作"]].decode("utf-8")
    sst = data["xl/sharedStrings.xml"].decode("utf-8")
    items = shared_items(sst)
    done_rows = rows_of(done)
    pending_rows = rows_of(pending)
    if max(pending_rows) != 2 or 3 in pending_rows:
        raise SystemExit("待制作 is not empty")
    dual = cell_map(done_rows[29], items)
    triple = cell_map(done_rows[32], items)
    if dual["B"] != "rule_dual_release" or triple["B"] != "rule_triple_cast":
        raise SystemExit("Template rows moved")
    intern, added, _ = intern_factory(sst)
    built = []
    new_rows = []
    for offset, (stable, title, kind, count) in enumerate(ITEMS):
        source_row = 29 if kind != "split" else 32
        row_number = 3 + offset
        description = requirement(kind, count)
        height = row_height(DV_BASE, description)
        row = retarget(done_rows[source_row], source_row, row_number, height)
        row = replace_string_cell(row, "A", row_number, intern(title))
        row = replace_string_cell(row, "B", row_number, intern(stable))
        row = replace_string_cell(row, "N", row_number, intern(description))
        row = replace_string_cell(row, "O", row_number, intern(O_TEXT))
        row = replace_string_cell(row, "DV", row_number, intern(DV_BASE))
        new_rows.append(row)
        built.append({
            "row": row_number,
            "id": stable,
            "name": title,
            "kind": kind,
            "count": count,
            "templateRow": source_row,
            "height": height,
            "N": description,
            "O": O_TEXT,
            "DV": DV_BASE,
        })
    pending_new = pending.replace("</sheetData>", "".join(new_rows) + "</sheetData>", 1)
    pending_new = pending_new.replace('ref="A1:EN2"', 'ref="A1:EN10"', 1)
    pending_new = pending_new.replace('ref="A2:EN2"', 'ref="A2:EN10"', 1)
    book = data["xl/workbook.xml"].decode("utf-8").replace("待制作!$A$2:$EN$2", "待制作!$A$2:$EN$10", 1)
    if book.count("待制作!$A$2:$EN$10") != 1 or "待制作!$A$2:$EN$2" in book:
        raise SystemExit("Pending filter name was not unique")
    reference_delta = string_refs(pending_new) - string_refs(pending)
    sst_new = append_strings(sst, added, reference_delta)
    final_items = shared_items(sst_new)
    for spec, row in zip(built, new_rows):
        cells = cell_map(row, final_items)
        expected_role = "一分为三" if spec["kind"] == "split" else "双重释放"
        expected_cool = "0" if spec["kind"] == "split" else "20"
        expected_pull = "0" if spec["kind"] == "split" else "1"
        checks = {
            "A": spec["name"], "B": spec["id"], "C": "通用", "D": "无", "E": "0",
            "F": expected_cool, "G": "20", "L": "0", "N": spec["N"], "O": spec["O"],
            "P": "规则", "Q": "触发", "R": "直线", "S": expected_role, "X": expected_pull,
            "DV": spec["DV"],
        }
        for column, expected in checks.items():
            if cells.get(column) != expected:
                raise SystemExit(f"{spec['id']} {column} is {cells.get(column)!r}")
        if "TripleCastEffect" in (cells.get("O") or "") or cells.get("N") == dual["N"]:
            raise SystemExit("Old template wording leaked")
        if QUOTE not in cells["DV"] or "已验收" in cells["DV"] or "已复审" in cells["DV"]:
            raise SystemExit("DV wording failed")
    patch = {
        names["待制作"]: pending_new.encode("utf-8"),
        "xl/workbook.xml": book.encode("utf-8"),
        "xl/sharedStrings.xml": sst_new.encode("utf-8"),
    }
    for name in ("已完成", "制作中", "待验收", "数据表", "资源状态明细", "待制作原始数据"):
        if names[name] in patch:
            raise SystemExit("Unexpected sheet patch")
    return entries, data, patch, built


def build_move(source_bytes, source_hash, captured_at):
    entries, data = load_bytes(source_bytes)
    names = sheet_map(data)
    pending = data[names["待制作"]].decode("utf-8")
    making = data[names["制作中"]].decode("utf-8")
    archive = data[names["待制作原始数据"]].decode("utf-8")
    sst = data["xl/sharedStrings.xml"].decode("utf-8")
    items = shared_items(sst)
    pending_rows = rows_of(pending)
    making_rows = rows_of(making)
    archive_rows = rows_of(archive)
    if max(making_rows) != 19 or 20 in making_rows or max(archive_rows) != 25 or 26 in archive_rows:
        raise SystemExit("Destination rows are not at the expected end")
    intern, added, _ = intern_factory(sst)
    archive_xml = []
    making_xml = []
    moved = []
    for offset, (stable, title, kind, count) in enumerate(ITEMS):
        source_row = 3 + offset
        row = pending_rows.get(source_row)
        if row is None or cell_map(row, items).get("B") != stable:
            raise SystemExit(f"Pending row {source_row} is not {stable}")
        original_cells = cell_map(row, items)
        archive_row_number = 26 + offset
        making_row_number = 20 + offset
        archived = retarget(row, source_row, archive_row_number, original_cells and row_height(original_cells["DV"], original_cells["N"]))
        archived = archived.replace('spans="1:144"', 'spans="1:175"', 1)
        provenance = [
            ("EP", intern("docs/术式制作表.xlsx"), "s"),
            ("EQ", intern("待制作"), "s"),
            ("ER", str(source_row), "n"),
            ("ES", intern(captured_at), "s"),
            ("ET", intern(captured_at), "s"),
            ("EU", intern(source_hash), "s"),
            ("EV", intern("2026-11-09首次从空白待制作转录用户直接需求，并在迁入制作中之前留下这份首次快照。原文是转录时的待制作整行，不是从已完成或制作中反推。无适用资源状态明细。输入版本 user-release-variants-20261109-v1。"), "s"),
            ("EX", intern("无独立行"), "s"),
        ]
        extra = []
        for column, value, kind_cell in provenance:
            if kind_cell == "n":
                extra.append(f'<c r="{column}{archive_row_number}" s="8"><v>{value}</v></c>')
            else:
                extra.append(f'<c r="{column}{archive_row_number}" s="9" t="s"><v>{value}</v></c>')
        archived = archived.replace("</row>", "".join(extra) + "</row>", 1)
        making_note = original_cells["DV"] + "\n" + MARKER
        moved_row = retarget(row, source_row, making_row_number, row_height(making_note, original_cells["N"]))
        moved_row = replace_string_cell(moved_row, "DV", making_row_number, intern(making_note))
        archive_xml.append(archived)
        making_xml.append(moved_row)
        moved.append({
            "id": stable,
            "name": title,
            "from": f"待制作第{source_row}行",
            "archiveRow": archive_row_number,
            "makingRow": making_row_number,
            "N": original_cells["N"],
            "O": original_cells["O"],
            "DV": original_cells["DV"],
            "makingDV": making_note,
        })
    pending_new = pending
    for number in range(3, 11):
        pending_new = pending_new.replace(pending_rows[number], "", 1)
    pending_new = pending_new.replace('ref="A1:EN10"', 'ref="A1:EN2"', 1)
    pending_new = pending_new.replace('ref="A2:EN10"', 'ref="A2:EN2"', 1)
    if rows_of(pending_new).keys() != {1, 2}:
        raise SystemExit("Pending sheet did not return to the header")
    making_new = making.replace("</sheetData>", "".join(making_xml) + "</sheetData>", 1)
    making_new = making_new.replace('ref="A1:EN19"', 'ref="A1:EN27"', 1)
    archive_new = archive.replace("</sheetData>", "".join(archive_xml) + "</sheetData>", 1)
    archive_new = archive_new.replace('ref="A1:FS25"', 'ref="A1:FS33"', 1)
    book = data["xl/workbook.xml"].decode("utf-8")
    book = book.replace("待制作!$A$2:$EN$10", "待制作!$A$2:$EN$2", 1)
    book = book.replace("制作中!$A$2:$EN$19", "制作中!$A$2:$EN$27", 1)
    if "待制作!$A$2:$EN$10" in book or book.count("制作中!$A$2:$EN$27") != 1:
        raise SystemExit("Filter names did not move cleanly")
    reference_delta = (
        string_refs(pending_new) + string_refs(making_new) + string_refs(archive_new)
        - string_refs(pending) - string_refs(making) - string_refs(archive)
    )
    sst_new = append_strings(sst, added, reference_delta)
    final_items = shared_items(sst_new)
    for spec, archived, moved_row in zip(moved, archive_xml, making_xml):
        archived_cells = cell_map(archived, final_items)
        moved_cells = cell_map(moved_row, final_items)
        if archived_cells["B"] != spec["id"] or archived_cells["N"] != spec["N"] or archived_cells["O"] != spec["O"] or archived_cells["DV"] != spec["DV"]:
            raise SystemExit(f"Archive changed wording for {spec['id']}")
        if MARKER in archived_cells["DV"]:
            raise SystemExit("Archive received the making marker")
        if moved_cells["N"] != spec["N"] or moved_cells["O"] != spec["O"] or moved_cells["DV"] != spec["makingDV"]:
            raise SystemExit(f"Making row changed wording for {spec['id']}")
        if not moved_cells["DV"].endswith("\n" + MARKER) or QUOTE not in moved_cells["DV"]:
            raise SystemExit("Making marker was not appended")
        if archived_cells.get("EQ") != "待制作" or archived_cells.get("EX") != "无独立行":
            raise SystemExit("Provenance missing")
    for number, row in making_rows.items():
        if row not in making_new:
            raise SystemExit(f"Existing making row {number} changed")
    patch = {
        names["待制作"]: pending_new.encode("utf-8"),
        names["制作中"]: making_new.encode("utf-8"),
        names["待制作原始数据"]: archive_new.encode("utf-8"),
        "xl/workbook.xml": book.encode("utf-8"),
        "xl/sharedStrings.xml": sst_new.encode("utf-8"),
    }
    return entries, data, patch, moved


def status_ids(data, names, items):
    result = {}
    for name in ("已完成", "待制作", "制作中", "待验收", "待制作原始数据"):
        result[name] = ids_from(data[names[name]].decode("utf-8"), items)
    return result


def assert_conservation(data, label, stage):
    names = sheet_map(data)
    items = shared_items(data["xl/sharedStrings.xml"].decode("utf-8"))
    status = status_ids(data, names, items)
    flat = []
    for name in ("已完成", "待制作", "制作中", "待验收"):
        flat.extend(item[1] for item in status[name])
    counts = {name: len(status[name]) for name in ("已完成", "待制作", "制作中", "待验收")}
    if len(flat) != 70 or len(set(flat)) != 70:
        raise SystemExit(f"{label} conservation failed {counts} unique={len(set(flat))}")
    archive_ids = [item[1] for item in status["待制作原始数据"]]
    new_ids = [item[0] for item in ITEMS]
    if len(archive_ids) != len(set(archive_ids)):
        raise SystemExit("Archive has a duplicate id")
    if stage == "transcribe":
        if any(stable in archive_ids or flat.count(stable) != 1 for stable in new_ids):
            raise SystemExit("New ids are not exactly once on the status pages")
    if stage == "move":
        if any(archive_ids.count(stable) != 1 or flat.count(stable) != 1 for stable in new_ids):
            raise SystemExit("Archive first-snapshot is not unique")
    return {"counts": counts, "total": len(flat), "unique": len(set(flat)), "archive": len(archive_ids), "status": status}


def save_candidate(source_bytes, patch_builder, destination, stage):
    entries, data, patch, detail = patch_builder(source_bytes)
    changed = write_package(entries, data, patch, destination)
    _, written = load_package(destination)
    summary = assert_conservation(written, destination.name, stage)
    return changed, detail, summary, sha256(destination.read_bytes())


def main():
    mode = sys.argv[1]
    source = LIVE.read_bytes()
    source_hash = sha256(source)
    expected = sys.argv[2] if len(sys.argv) > 2 else BASELINE
    if source_hash != expected:
        raise SystemExit(f"HASH_CHANGED {source_hash}")
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    CANDIDATE_DIR.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    if mode == "transcribe":
        backup = BACKUP_DIR / f"术式制作表-{stamp}-1.xlsx"
        candidate = CANDIDATE_DIR / "candidate-transcribe.xlsx"
        shutil.copyfile(LIVE, backup)
        changed, detail, summary, candidate_hash = save_candidate(source, build_transcribe, candidate, "transcribe")
        counts = {key: value for key, value in summary["counts"].items()}
        if counts != {"已完成": 37, "待制作": 8, "制作中": 17, "待验收": 8}:
            raise SystemExit(f"Transcribe counts {counts}")
        payload = {
            "phase": "transcribe",
            "sourceHash": source_hash,
            "candidateHash": candidate_hash,
            "backup": str(backup),
            "candidate": str(candidate),
            "changedParts": changed,
            "counts": counts,
            "rows": detail,
        }
        (ROOT / "transcription-rows.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps({"phase": "transcribe", "candidateHash": candidate_hash, "counts": counts, "changedParts": changed}, ensure_ascii=False))
        return
    if mode == "move":
        captured = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        backup = BACKUP_DIR / f"术式制作表-{stamp}-2.xlsx"
        candidate = CANDIDATE_DIR / "candidate-move.xlsx"
        shutil.copyfile(LIVE, backup)

        def builder(payload):
            return build_move(payload, source_hash, captured)

        changed, detail, summary, candidate_hash = save_candidate(source, builder, candidate, "move")
        counts = summary["counts"]
        if counts != {"已完成": 37, "待制作": 0, "制作中": 25, "待验收": 8} or summary["archive"] != 30:
            raise SystemExit(f"Move counts {counts} archive={summary['archive']}")
        payload = {
            "phase": "move",
            "sourceHash": source_hash,
            "candidateHash": candidate_hash,
            "capturedAt": captured,
            "backup": str(backup),
            "candidate": str(candidate),
            "changedParts": changed,
            "counts": counts,
            "archiveRows": summary["archive"],
            "rows": detail,
        }
        (ROOT / "move-rows.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps({"phase": "move", "candidateHash": candidate_hash, "counts": counts, "archive": summary["archive"], "capturedAt": captured, "changedParts": changed}, ensure_ascii=False))
        return
    if mode == "commit":
        candidate = Path(sys.argv[3])
        if sha256(LIVE.read_bytes()) != expected:
            raise SystemExit(f"HASH_CHANGED_BEFORE_COMMIT {sha256(LIVE.read_bytes())}")
        shutil.copyfile(candidate, LIVE)
        live_hash = sha256(LIVE.read_bytes())
        if live_hash != sha256(candidate.read_bytes()):
            raise SystemExit("COMMIT_MISMATCH")
        print(json.dumps({"committed": True, "hash": live_hash, "mtime": datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S")}, ensure_ascii=False))
        return
    raise SystemExit("Unknown mode")


if __name__ == "__main__":
    main()
