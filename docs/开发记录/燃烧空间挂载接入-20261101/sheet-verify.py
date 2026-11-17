# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

BATCH = Path(r"D:\alianximod\docs\开发记录\燃烧空间挂载接入-20261101")
PREP = json.loads((BATCH / "sheet-prepare-result.json").read_text(encoding="utf-8"))
LIVE = Path(PREP["live"])
BACKUP = Path(PREP["backup"])
NEW_N = "进入或升级超频炉心，并保留已有加成"


def sha256_bytes(payload):
    return hashlib.sha256(payload).hexdigest().upper()


def local(tag):
    return tag.rsplit("}", 1)[-1]


def rows_of(xml):
    return re.findall(r"<row\b[^>]*>[\s\S]*?</row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def shared_list(xml):
    root = ET.fromstring(xml)
    found = []
    for item in root:
        if local(item.tag) != "si":
            continue
        texts = []
        for node in item.iter():
            if local(node.tag) == "t" and node.text:
                texts.append(html.unescape(node.text))
        found.append("".join(texts))
    return found


def cell_text(row, column, number, shared):
    match = re.search(rf'<c r="{column}{number}"[\s\S]*?</c>', row)
    if not match:
        return None
    cell = match.group(0)
    if "<is>" in cell:
        texts = re.findall(r"<t[^>]*>([\s\S]*?)</t>", cell)
        return html.unescape("".join(texts))
    value = re.search(r"<v>([\s\S]*?)</v>", cell)
    if not value:
        return None
    raw = html.unescape(value.group(1))
    if 't="s"' in cell and raw.isdigit():
        return shared[int(raw)]
    return raw


def ids_of(xml, shared):
    found = []
    for row in rows_of(xml):
        number = row_number(row)
        if number < 3:
            continue
        code = cell_text(row, "B", number, shared)
        if code:
            found.append((code, number))
    return found


def main():
    live = LIVE.read_bytes()
    with zipfile.ZipFile(BACKUP) as archive:
        backup_parts = {name: archive.read(name) for name in archive.namelist()}
    with zipfile.ZipFile(LIVE) as archive:
        live_parts = {name: archive.read(name) for name in archive.namelist()}
    shared = shared_list(live_parts["xl/sharedStrings.xml"])
    different = sorted(name for name in live_parts if live_parts[name] != backup_parts.get(name))
    pages = {
        "已完成": ids_of(live_parts["xl/worksheets/sheet1.xml"].decode("utf-8"), shared),
        "待制作": ids_of(live_parts["xl/worksheets/sheet2.xml"].decode("utf-8"), shared),
        "制作中": ids_of(live_parts["xl/worksheets/sheet3.xml"].decode("utf-8"), shared),
        "待验收": ids_of(live_parts["xl/worksheets/sheet4.xml"].decode("utf-8"), shared),
    }
    counts = {title: len(page) for title, page in pages.items()}
    all_ids = [code for page in pages.values() for code, _number in page]
    pending = live_parts["xl/worksheets/sheet4.xml"].decode("utf-8")
    row = next(item for item in rows_of(pending) if row_number(item) == 7)
    nodv = json.loads(Path(PREP["snapshots"]["nodv"]).read_text(encoding="utf-8"))
    dv = cell_text(row, "DV", 7, shared)
    report = {
        "liveSha256": sha256_bytes(live),
        "matchesCandidate": sha256_bytes(live) == PREP["candidateSha256"],
        "differentParts": different,
        "counts": counts,
        "unique": len(all_ids) == len(set(all_ids)) == 60,
        "fireBurn": [item for title, page in pages.items() for item in page if item[0] == "fire_burn"],
        "fireHeat": [item for item in pages["制作中"] if item[0] == "fire_heat_up"],
        "fireCool": [item for item in pages["制作中"] if item[0] == "fire_cool"],
        "n": cell_text(row, "N", 7, shared) == NEW_N,
        "oHasMount": "burn_spatial_v1/mount.json" in (cell_text(row, "O", 7, shared) or ""),
        "dvKept": dv.startswith(nodv["DV"] + "\n") and dv.endswith("调度记录.md"),
        "archiveSame": live_parts["xl/worksheets/sheet7.xml"] == backup_parts["xl/worksheets/sheet7.xml"],
        "detailSame": live_parts["xl/worksheets/sheet6.xml"] == backup_parts["xl/worksheets/sheet6.xml"],
        "sharedSame": live_parts["xl/sharedStrings.xml"] == backup_parts["xl/sharedStrings.xml"],
        "mtime": datetime.fromtimestamp(LIVE.stat().st_mtime).astimezone().isoformat(timespec="milliseconds"),
    }
    report["ok"] = all([
        report["matchesCandidate"],
        set(different) == {"xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml", "xl/workbook.xml"},
        counts == {"已完成": 40, "待制作": 0, "制作中": 15, "待验收": 5},
        report["unique"],
        report["fireBurn"] == [("fire_burn", 7)],
        report["fireHeat"] == [("fire_heat_up", 3)],
        report["fireCool"] == [("fire_cool", 4)],
        report["n"] and report["oHasMount"] and report["dvKept"],
        report["archiveSame"] and report["detailSame"] and report["sharedSame"],
    ])
    (BATCH / "sheet-verify.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"ok": report["ok"], "sha": report["liveSha256"], "counts": counts}, ensure_ascii=False))
    if not report["ok"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
