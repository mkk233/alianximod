# -*- coding: utf-8 -*-
import json
import zipfile
from pathlib import Path
import importlib.util

BATCH = Path(r"D:\alianximod\docs\开发记录\底座释放参数返修-20261102")
spec = importlib.util.spec_from_file_location("final_prepare", BATCH / "sheet-final-prepare.py")
prepare = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prepare)
prep = json.loads((BATCH / "sheet-final-prepare.json").read_text(encoding="utf-8"))


def main():
    if prepare.base.sha256_file(prep["live"]) != prep["candidateSha256"]:
        raise SystemExit("live workbook is not the candidate")
    with zipfile.ZipFile(prep["backup"]) as backup, zipfile.ZipFile(prep["live"]) as current:
        shared = prepare.base.shared_list(current.read("xl/sharedStrings.xml"))
        different = [name for name in current.namelist() if current.read(name) != backup.read(name)]
        if set(different) != set(prep["affectedParts"]):
            raise SystemExit("unexpected parts")
        if current.read(prepare.base.PARTS["待制作原始数据"]) != backup.read(prepare.base.PARTS["待制作原始数据"]):
            raise SystemExit("archive changed")
        counts = {}
        located = {}
        seen = []
        for title in prepare.base.STATUS:
            page = prepare.base.ids_of(current.read(prepare.base.PARTS[title]).decode("utf-8"), shared)
            counts[title] = len(page)
            for code, number, row in page:
                located.setdefault(code, []).append((title, number, row))
                seen.append(code)
        if counts != prep["countsAfter"] or len(seen) != 60 or len(set(seen)) != 60:
            raise SystemExit(str(counts))
        expected = {"fire_shock_ring": 8, "fire_meteorite": 9}
        for code, row_number in expected.items():
            hits = located[code]
            if len(hits) != 1 or hits[0][0] != "待验收" or hits[0][1] != row_number:
                raise SystemExit(code)
            row = hits[0][2]
            if prepare.base.read_at(row, "N", row_number, shared) != prep["display"][code]["N"]:
                raise SystemExit(code + " N")
            if prepare.base.read_at(row, "O", row_number, shared) != prep["display"][code]["O"]:
                raise SystemExit(code + " O")
            note = prepare.base.read_at(row, "DV", row_number, shared)
            if "份序号在建帧前写入" not in note or "03:55:41.329" not in note:
                raise SystemExit(code + " DV")
    result = {"ok": True, "counts": counts, "uniqueIds": 60}
    (BATCH / "sheet-final-verify.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
