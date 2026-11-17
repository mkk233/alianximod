# -*- coding: utf-8 -*-
"""Reopen the committed workbook and compare it with the pre-change backup."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

BATCH = Path(r"D:\alianximod\docs\开发记录\炉心升温燃烧视觉接入-20261031")
PREP = json.loads((BATCH / "sheet-prepare-result.json").read_text(encoding="utf-8"))
LIVE = Path(PREP["live"])
BACKUP = Path(PREP["backup"])
README = Path(PREP["readme"])
APPEND = "2026-10-31 已采用升温v2/燃烧v3并完成客户端接入，待Cursor结论收束及统一编译；游戏待用户验证；Codex未复审"
STATUS = ("已完成", "待制作", "制作中", "待验收")


def sha256_bytes(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_span(row, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row.find(">", start)
    if row[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</(?:[\w]+:)?c>", row[opener_end:])
        end = opener_end + close.end()
    return row[start:end]


def cell_text(cell):
    if cell is None:
        return None
    inline = re.search(r"<(?:[\w]+:)?is>([\s\S]*)</(?:[\w]+:)?is>", cell)
    if inline:
        texts = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", inline.group(1))
        return html.unescape("".join(texts))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    return html.unescape(value.group(1)) if value else None


def ids_of(xml):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        code = cell_text(cell_span(row, "B", number))
        if code:
            found.append((code, number, row))
    return found


def load(path):
    with zipfile.ZipFile(path) as archive:
        return {name: archive.read(name) for name in archive.namelist()}


def main():
    live_bytes = LIVE.read_bytes()
    backup_parts = load(BACKUP)
    live_parts = load(LIVE)
    different = sorted(name for name in live_parts if live_parts[name] != backup_parts.get(name))
    making = live_parts["xl/worksheets/sheet3.xml"].decode("utf-8")
    old_making = backup_parts["xl/worksheets/sheet3.xml"].decode("utf-8")
    nodv = json.loads(Path(PREP["snapshots"]["nodv"]).read_text(encoding="utf-8"))
    counts = {}
    locations = {}
    all_ids = []
    for title, part in (("已完成", "xl/worksheets/sheet1.xml"), ("待制作", "xl/worksheets/sheet2.xml"), ("制作中", "xl/worksheets/sheet3.xml"), ("待验收", "xl/worksheets/sheet4.xml")):
        page = ids_of(live_parts[part].decode("utf-8"))
        counts[title] = len(page)
        for code, number, _row in page:
            all_ids.append(code)
            if code in ("fire_heat_up", "fire_burn", "fire_cool"):
                locations.setdefault(code, []).append({"sheet": title, "row": number})
    checks = {
        "liveSha256": sha256_bytes(live_bytes),
        "matchesCandidate": sha256_bytes(live_bytes) == PREP["candidateSha256"],
        "backupSha256": sha256_bytes(BACKUP.read_bytes()),
        "differentParts": different,
        "onlyMakingSheet": different == ["xl/worksheets/sheet3.xml"],
        "archiveSame": live_parts["xl/worksheets/sheet7.xml"] == backup_parts["xl/worksheets/sheet7.xml"] if "xl/worksheets/sheet7.xml" in live_parts else False,
        "counts": counts,
        "countsUnchanged": counts == PREP["counts"],
        "unique": len(all_ids) == len(set(all_ids)) == 60,
        "locations": locations,
        "outsideSheetDataSame": re.sub(r"<sheetData>[\s\S]*</sheetData>", "", making) == re.sub(r"<sheetData>[\s\S]*</sheetData>", "", old_making),
        "readmeSha256": sha256_bytes(README.read_bytes()),
        "mtime": datetime.fromtimestamp(LIVE.stat().st_mtime).astimezone().isoformat(timespec="milliseconds"),
    }
    # sheet names may not be sheet7. Compare by filename set from workbook is already covered by differentParts.
    items = {}
    for code in ("fire_heat_up", "fire_burn"):
        number = nodv["items"][code]["row"]
        row = [item for item in rows_of(making) if row_number(item) == number][0]
        old = [item for item in rows_of(old_making) if row_number(item) == number][0]
        text_n = cell_text(cell_span(row, "N", number))
        text_o = cell_text(cell_span(row, "O", number))
        text_dv = cell_text(cell_span(row, "DV", number))
        changed_columns = []
        for column in ("A", "B", "N", "O", "DV", "DW", "DY", "EN"):
            if cell_text(cell_span(row, column, number)) != cell_text(cell_span(old, column, number)):
                changed_columns.append(column)
        items[code] = {
            "row": number,
            "nKept": text_n == nodv["items"][code]["N"],
            "oChanged": text_o != nodv["items"][code]["O"] and "textures/effect/furnace_v2/" in (text_o or ""),
            "dvKeptPrefix": text_dv.startswith(nodv["items"][code]["DV"] + "\n"),
            "dvEndsWithProgress": (text_dv or "").endswith(APPEND),
            "rowOpenKept": row[:row.find("<c ")] == old[:old.find("<c ")],
            "changedSample": changed_columns,
        }
    cool_number = locations["fire_cool"][0]["row"]
    cool = [item for item in rows_of(making) if row_number(item) == cool_number][0]
    old_cool = [item for item in rows_of(old_making) if row_number(item) == cool_number][0]
    checks["fireCoolUnchanged"] = cool == old_cool
    checks["items"] = items
    readme = README.read_text(encoding="utf-8")
    checks["indexHeat"] = "已接入源码，待编译/游戏验收" in readme.splitlines()[8]
    checks["indexBurn"] = "已接入源码，待编译/游戏验收" in readme.splitlines()[10]
    checks["indexV1Kept"] = "已接入textures/effect/furnace_v1，2026-10-29最终compileJava" in readme
    checks["oldPhraseGone"] = "尚未替换游戏中的v1" not in readme
    ok = all([
        checks["matchesCandidate"],
        checks["onlyMakingSheet"],
        checks["countsUnchanged"],
        checks["unique"],
        checks["outsideSheetDataSame"],
        checks["fireCoolUnchanged"],
        checks["indexHeat"],
        checks["indexBurn"],
        checks["indexV1Kept"],
        checks["oldPhraseGone"],
        all(item["nKept"] and item["oChanged"] and item["dvKeptPrefix"] and item["dvEndsWithProgress"] and item["rowOpenKept"] for item in items.values()),
        locations["fire_heat_up"] == [{"sheet": "制作中", "row": 3}],
        locations["fire_burn"] == [{"sheet": "制作中", "row": 4}],
    ])
    checks["ok"] = ok
    (BATCH / "sheet-verify.json").write_text(json.dumps(checks, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"ok": ok, "sha": checks["liveSha256"], "parts": different, "items": items}, ensure_ascii=False))
    if not ok:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
