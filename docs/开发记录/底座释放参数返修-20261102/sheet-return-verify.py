# -*- coding: utf-8 -*-
import json
import zipfile
from pathlib import Path
import importlib.util

BATCH = Path(r"D:\alianximod\docs\开发记录\底座释放参数返修-20261102")
spec = importlib.util.spec_from_file_location("prepare", BATCH / "sheet-return-prepare.py")
prepare = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prepare)
prep = json.loads((BATCH / "sheet-return-prepare.json").read_text(encoding="utf-8"))


def main():
    if prepare.sha256_file(prep["live"]) != prep["candidateSha256"]:
        raise SystemExit("live workbook is not the candidate")
    wanted = prepare.requirements()
    with zipfile.ZipFile(prep["backup"]) as backup, zipfile.ZipFile(prep["live"]) as current:
        shared = prepare.shared_list(current.read("xl/sharedStrings.xml"))
        different = [name for name in current.namelist() if current.read(name) != backup.read(name)]
        if set(different) != set(prep["affectedParts"]):
            raise SystemExit("unexpected parts " + ",".join(different))
        if current.read(prepare.PARTS["待制作原始数据"]) != backup.read(prepare.PARTS["待制作原始数据"]):
            raise SystemExit("archive changed")
        if current.read(prepare.PARTS["资源状态明细"]) != backup.read(prepare.PARTS["资源状态明细"]):
            raise SystemExit("detail changed")
        counts = {}
        located = {}
        seen = []
        for title in prepare.STATUS:
            page = prepare.ids_of(current.read(prepare.PARTS[title]).decode("utf-8"), shared)
            counts[title] = len(page)
            for code, number, row in page:
                located.setdefault(code, []).append((title, number, row))
                seen.append(code)
        if counts != prep["countsAfter"] or len(seen) != 60 or len(set(seen)) != 60:
            raise SystemExit(str(counts))
        expected = {"fire_shock_ring": 16, "fire_meteorite": 17}
        for code, row_number in expected.items():
            hits = located[code]
            if len(hits) != 1 or hits[0][0] != "制作中" or hits[0][1] != row_number:
                raise SystemExit(code + " " + str([(item[0], item[1]) for item in hits]))
            row = hits[0][2]
            if prepare.read_at(row, "N", row_number, shared) != wanted[code]["N"]:
                raise SystemExit(code + " N")
            if prepare.read_at(row, "O", row_number, shared) != wanted[code]["O"]:
                raise SystemExit(code + " O")
            if "批量路径丢落点中心" not in prepare.read_at(row, "DV", row_number, shared):
                raise SystemExit(code + " DV")
    result = {"ok": True, "counts": counts, "uniqueIds": 60}
    (BATCH / "sheet-return-verify.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
