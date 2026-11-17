# -*- coding: utf-8 -*-
"""Reopen the committed workbook and confirm only fire_shock_ring moved."""
import hashlib
import json
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

import importlib.util

BATCH = Path(r"D:\alianximod\docs\开发记录\热波正圆精修-20261101")
spec = importlib.util.spec_from_file_location("prepare", BATCH / "sheet-prepare.py")
prepare = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prepare)
prep = json.loads((BATCH / "sheet-prepare-result.json").read_text(encoding="utf-8"))
snap = json.loads(Path(prep["snapshots"]["nodv"]).read_text(encoding="utf-8"))


def sha256_file(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest().upper()


def main():
    live = Path(prep["live"])
    if sha256_file(live) != prep["candidateSha256"]:
        raise SystemExit("live workbook is not the candidate")
    if sha256_file(prep["readme"]) != prep["readmeCandidateSha256"]:
        raise SystemExit("index hash mismatch")
    if sha256_file(prep["v1Readme"]) != prep["v1CandidateSha256"]:
        raise SystemExit("v1 readme hash mismatch")
    if sha256_file(prep["v2Readme"]) != prep["v2ReadmeCandidateSha256"]:
        raise SystemExit("v2 readme hash mismatch")
    if sha256_file(prep["v2Manifest"]) != prep["v2ManifestCandidateSha256"]:
        raise SystemExit("manifest hash mismatch")
    for relative, digest in prep["v1ArtSha256"].items():
        if sha256_file(prepare.ROOT / relative) != digest:
            raise SystemExit("v1 art changed " + relative)
    for relative, digest in prep["v2ArtSha256"].items():
        if sha256_file(prepare.ROOT / relative) != digest:
            raise SystemExit("v2 art changed " + relative)
    with zipfile.ZipFile(prep["backup"]) as backup, zipfile.ZipFile(live) as current:
        shared = prepare.shared_list(current.read("xl/sharedStrings.xml"))
        if current.read("xl/sharedStrings.xml") != backup.read("xl/sharedStrings.xml"):
            raise SystemExit("shared strings changed")
        different = [name for name in current.namelist() if current.read(name) != backup.read(name)]
        if set(different) != {"xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml", "xl/workbook.xml"}:
            raise SystemExit("unexpected workbook parts " + ",".join(different))
        counts = {}
        located = {}
        for title in prepare.STATUS:
            page = prepare.ids_of(current.read(prepare.PARTS[title]).decode("utf-8"), shared)
            counts[title] = len(page)
            for code, number, row in page:
                located.setdefault(code, []).append((title, number, row))
        if counts != prep["countsAfter"]:
            raise SystemExit(f"counts {counts}")
        hits = located.get(prepare.TARGET, [])
        if len(hits) != 1 or hits[0][0] != "待验收" or hits[0][1] != 8:
            raise SystemExit(f"location {[(item[0], item[1]) for item in hits]}")
        row = hits[0][2]
        if prepare.read_at(row, "N", 8, shared) != prepare.NEW_N:
            raise SystemExit("N mismatch")
        if prepare.read_at(row, "O", 8, shared) != prepare.NEW_O:
            raise SystemExit("O mismatch")
        dv = prepare.read_at(row, "DV", 8, shared)
        if not dv.startswith(snap["DV"] + "\n") or "本阶段额度已用完，返修结果未追加复审" not in dv:
            raise SystemExit("DV mismatch")
        if "待统一编译" not in dv:
            raise SystemExit("historical compile note removed")
        archive = prepare.ids_of(current.read(prepare.PARTS["待制作原始数据"]).decode("utf-8"), shared)
        if [code for code, _number, _row in archive].count(prepare.TARGET) != 1:
            raise SystemExit("archive snapshot changed")
        ET.fromstring(current.read("xl/worksheets/sheet3.xml"))
        ET.fromstring(current.read("xl/worksheets/sheet4.xml"))
    index = Path(prep["readme"]).read_bytes().decode("utf-8")
    if index.count("热波 · 水平正圆 v2（已确认并接入运行贴图）") != 1:
        raise SystemExit("index bullet missing")
    if "除热波外，这些暂用素材尚未复制进游戏资源" not in index:
        raise SystemExit("old batch status was not carved out")
    if "升温 · 贴身炽热 v2（已采用）" not in index or "燃烧 · 空间挂载 v1（已确认）" not in index:
        raise SystemExit("other index entries missing")
    manifest = json.loads(Path(prep["v2Manifest"]).read_bytes().decode("utf-8"))
    recorded = {item["file"]: item["sha256"].lower() for item in manifest["files"] if item["file"] in prep["manifestArtSha256"]}
    if recorded != prep["manifestArtSha256"]:
        raise SystemExit("manifest art hashes changed")
    for name, digest in recorded.items():
        actual = hashlib.sha256((prepare.V2_DIR / name).read_bytes()).hexdigest()
        if actual != digest:
            raise SystemExit("art bytes changed " + name)
    result = {"ok": True, "counts": counts, "row": 8, "sheet": "待验收", "different": different}
    (BATCH / "sheet-verify.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
