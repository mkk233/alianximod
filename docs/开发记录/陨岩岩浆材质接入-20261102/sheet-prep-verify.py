# -*- coding: utf-8 -*-
"""Reopen the workbook and confirm fire_meteorite returned to 制作中."""
import json
import zipfile
from pathlib import Path
import importlib.util

BATCH = Path(r"D:\alianximod\docs\开发记录\陨岩岩浆材质接入-20261102")
spec = importlib.util.spec_from_file_location("prepare", BATCH / "sheet-prep-prepare.py")
prepare = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prepare)
prep = json.loads((BATCH / "sheet-prep-prepare.json").read_text(encoding="utf-8"))
snap = json.loads(Path(prep["snapshots"]["nodv"]).read_text(encoding="utf-8"))


def main():
    if prepare.sha256_file(prep["live"]) != prep["candidateSha256"]:
        raise SystemExit("live workbook is not the candidate")
    full_n = prepare.revised_function()
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
        if counts != prep["countsAfter"] or sum(counts.values()) != 60:
            raise SystemExit(str(counts))
        hits = located[prepare.TARGET]
        if len(hits) != 1 or hits[0][0] != "制作中" or hits[0][1] != 16:
            raise SystemExit(str([(item[0], item[1]) for item in hits]))
        row = hits[0][2]
        if prepare.read_at(row, "N", 16, shared) != full_n:
            raise SystemExit("N is not the full revised requirement")
        if prepare.read_at(row, "O", 16, shared) != prepare.NEW_O:
            raise SystemExit("O mismatch")
        dv = prepare.read_at(row, "DV", 16, shared)
        if not dv.startswith(snap["DV"] + "\n") or "历史 compileJava 通过不能代表本次编译" not in dv:
            raise SystemExit("DV mismatch")
        if "直到触地才落块" not in prepare.read_at(row, "N", 16, shared):
            raise SystemExit("continue-flight requirement missing")
    result = {"ok": True, "counts": counts, "sheet": "制作中", "row": 16}
    (BATCH / "sheet-prep-verify.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
