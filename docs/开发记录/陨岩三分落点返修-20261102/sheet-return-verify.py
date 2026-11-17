# -*- coding: utf-8 -*-
"""Reopen the workbook and confirm fire_meteorite is back in 制作中."""
import json
import zipfile
from pathlib import Path
import importlib.util

BATCH = Path(r"D:\alianximod\docs\开发记录\陨岩三分落点返修-20261102")
spec = importlib.util.spec_from_file_location("prepare", BATCH / "sheet-return-prepare.py")
prepare = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prepare)
prep = json.loads((BATCH / "sheet-return-prepare.json").read_text(encoding="utf-8"))
snap = json.loads(Path(prep["snapshots"]["nodv"]).read_text(encoding="utf-8"))


def main():
    if prepare.sha256_file(prep["live"]) != prep["candidateSha256"]:
        raise SystemExit("live workbook is not the candidate")
    with zipfile.ZipFile(prep["backup"]) as backup, zipfile.ZipFile(prep["live"]) as current:
        shared = prepare.shared_list(current.read("xl/sharedStrings.xml"))
        different = [name for name in current.namelist() if current.read(name) != backup.read(name)]
        if set(different) != set(prep["affectedParts"]):
            raise SystemExit("unexpected parts " + ",".join(different))
        if current.read(prepare.PARTS["待制作原始数据"]) != backup.read(prepare.PARTS["待制作原始数据"]):
            raise SystemExit("archive changed")
        counts = {}
        located = {}
        seen = []
        for title in prepare.STATUS:
            page = prepare.ids_of(current.read(prepare.PARTS[title]).decode("utf-8"), shared)
            counts[title] = len(page)
            for code, number, row in page:
                located.setdefault(code, []).append((title, number, row))
                seen.append(code)
        if counts != prep["countsAfter"] or len(seen) != len(set(seen)) or len(seen) != 60:
            raise SystemExit(str(counts))
        hits = located[prepare.TARGET]
        if len(hits) != 1 or hits[0][0] != "制作中" or hits[0][1] != 16:
            raise SystemExit(str([(item[0], item[1]) for item in hits]))
        row = hits[0][2]
        if prepare.read_at(row, "N", 16, shared) != snap["restored"]["N"]:
            raise SystemExit("N mismatch")
        if prepare.read_at(row, "O", 16, shared) != snap["restored"]["O"]:
            raise SystemExit("O mismatch")
        dv = prepare.read_at(row, "DV", 16, shared)
        if not dv.startswith(snap["DV"] + "\n") or "同一个地点生成三个陨岩" not in dv or "返修进行中" not in dv:
            raise SystemExit("DV mismatch")
        if "直到触地才落块" not in prepare.read_at(row, "N", 16, shared):
            raise SystemExit("continue-flight text missing")
        if "meteor-magma-crust-v5" not in prepare.read_at(row, "O", 16, shared):
            raise SystemExit("v5 visual missing")
    result = {"ok": True, "counts": counts, "uniqueIds": len(seen), "sheet": "制作中", "row": 16}
    (BATCH / "sheet-return-verify.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
