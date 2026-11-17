# -*- coding: utf-8 -*-
"""Reopen the committed workbook and confirm only fire_meteorite moved."""
import json
import zipfile
from pathlib import Path
import importlib.util

BATCH = Path(r"D:\alianximod\docs\开发记录\陨岩实体命中续飞-20261101")
spec = importlib.util.spec_from_file_location("prepare", BATCH / "sheet-prepare.py")
prepare = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prepare)
prep = json.loads((BATCH / "sheet-prepare-result.json").read_text(encoding="utf-8"))
snap = json.loads(Path(prep["snapshots"]["nodv"]).read_text(encoding="utf-8"))


def sha256_file(path):
    return prepare.sha256_file(Path(path))


def main():
    if sha256_file(prep["live"]) != prep["candidateSha256"]:
        raise SystemExit("live workbook is not the candidate")
    if sha256_file(prep["design"]) != prep["designCandidateSha256"]:
        raise SystemExit("design hash mismatch")
    if sha256_file(prep["amendment"]) != prep["amendmentCandidateSha256"]:
        raise SystemExit("amendment hash mismatch")
    design = Path(prep["design"]).read_bytes().decode("utf-8")
    if "保留可见的陨岩残骸无伤坠落" in design or "残骸继续无伤落地" in design:
        raise SystemExit("old drop sentence remains in the design doc")
    if "按命中瞬间的当前弹道继续飞行" not in design or "续飞到触地" not in design:
        raise SystemExit("new motion sentence missing")
    if "震环中间为空" not in design:
        raise SystemExit("unrelated design text missing")
    amendment = Path(prep["amendment"]).read_bytes().decode("utf-8")
    if amendment.count("## 覆盖关系") != 1 or "approved-v1.md" not in amendment:
        raise SystemExit("override section missing")
    approved = prepare.ROOT / "docs" / "开发记录" / "术式批量制作" / "待制作-20261031" / "approved-v1.md"
    if not approved.exists():
        raise SystemExit("approved-v1 missing")
    with zipfile.ZipFile(prep["backup"]) as backup, zipfile.ZipFile(prep["live"]) as current:
        shared = prepare.shared_list(current.read("xl/sharedStrings.xml"))
        if current.read("xl/sharedStrings.xml") != backup.read("xl/sharedStrings.xml"):
            raise SystemExit("shared strings changed")
        different = [name for name in current.namelist() if current.read(name) != backup.read(name)]
        if set(different) != {"xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml", "xl/workbook.xml"}:
            raise SystemExit("unexpected parts " + ",".join(different))
        if current.read(prepare.PARTS["待制作原始数据"]) != backup.read(prepare.PARTS["待制作原始数据"]):
            raise SystemExit("archive changed")
        counts = {}
        located = {}
        for title in prepare.STATUS:
            page = prepare.ids_of(current.read(prepare.PARTS[title]).decode("utf-8"), shared)
            counts[title] = len(page)
            for code, number, row in page:
                located.setdefault(code, []).append((title, number, row))
        if counts != prep["countsAfter"]:
            raise SystemExit(str(counts))
        hits = located[prepare.TARGET]
        if len(hits) != 1 or hits[0][0] != "待验收" or hits[0][1] != 9:
            raise SystemExit(str([(item[0], item[1]) for item in hits]))
        row = hits[0][2]
        if prepare.read_at(row, "N", 9, shared) != prepare.NEW_N:
            raise SystemExit("N mismatch")
        if prepare.read_at(row, "O", 9, shared) != prepare.NEW_O:
            raise SystemExit("O mismatch")
        dv = prepare.read_at(row, "DV", 9, shared)
        if not dv.startswith(snap["DV"] + "\n") or "本阶段额度已用完，返修结果未追加复审" not in dv:
            raise SystemExit("DV mismatch")
        if "compileJava没有执行" not in dv or "残骸无伤坠落" in prepare.read_at(row, "N", 9, shared):
            raise SystemExit("history or old behavior check failed")
        archive = [code for code, _number, _row in prepare.ids_of(current.read(prepare.PARTS["待制作原始数据"]).decode("utf-8"), shared)]
        if archive.count(prepare.TARGET) != 1:
            raise SystemExit("archive count")
    if "直坠残骸" not in snap["revisedN"] or "残骸无伤坠落" in snap["revisedN"]:
        raise SystemExit("revised requirement snapshot is wrong")
    result = {"ok": True, "counts": counts, "sheet": "待验收", "row": 9}
    (BATCH / "sheet-verify.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
