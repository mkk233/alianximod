# -*- coding: utf-8 -*-
"""Move the eight release rows from 制作中 to 待验收 without touching other rows."""
import json
import shutil
import sys
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import apply_rows as gate

ROOT = Path(__file__).resolve().parent
LIVE = gate.LIVE
BASELINE = "EF60BADAF1FA9A4F0EC12DD87DD7F465A043D2214864182F546418978B8AE63E"
ICON = "xueximod:textures/gui/technique/other_none.png"
MARKER = "待验收 | 多重与分裂扩展-20261109 | docs/开发记录/术式批量制作/多重与分裂扩展-20261109/记录.md"
COMPILE = (
    "compileJava --offline --no-daemon 退出码 0。"
    "2026-11-09 19:08:43 至 19:09:07（Asia/Shanghai），BUILD SUCCESSFUL in 22s。"
    "日志 docs/开发记录/术式批量制作/多重与分裂扩展-20261109/compile-evidence/compileJava.stdout.txt。"
    "游戏未测。"
)
FEEDBACK = (
    "[设计反馈] 2026-11-09\n"
    "问题：早先误把每条装配链都理解成只有8格，并由此认为八重、十六重最多再读后续7格。这是写回前的误判，不是用户要求。\n"
    "用户原话：更高格子数量是为法杖准备的，测试法杖有32格装配槽，8格的上限仅限剑术，但是剑术有四个释放入口四条8格的链路，不允许剑术使用八重释放及以上的就可以了\n"
    "用户原话：穷尽释放不用限制\n"
    "影响：测试法杖保持32格。剑术是四条各自8格的独立链，多重规则自身占一格。剑术只拒绝固定数量大于等于8的多重，当前就是八重和十六重；装配与施放都在扣费前拒绝，而且不跨另外三条剑链。穷尽是remaining，剑术可以使用，只读当前这一条链的剩余格。三重、四重，以及一分为二、一分为五、一分为八不因此被禁止。一分为八的8份不属于这道禁令。\n"
    "建议：扩到17格只是早先的可选想法，用户没有批准。\n"
    "待确认项：无。\n"
    "后续用户决定：本批按上面两句原话实现，未追加扩容授权。固定8及以上禁剑术已经实施，不是待确认建议。\n"
    "讨论：docs/开发记录/术式批量制作/多重与分裂扩展-20261109/记录.md\n"
    "实测清单：docs/开发记录/术式批量制作/多重与分裂扩展-20261109/用户实测清单.md"
)
AFTER_N = {
    "rule_triple_release": "按多重释放规则，依次释放后续最多3个装配格的内容。",
    "rule_quadruple_release": "按多重释放规则，依次释放后续最多4个装配格的内容。",
    "rule_octuple_release": "按多重释放规则，依次释放后续最多8个装配格的内容，不能用于剑术。",
    "rule_sixteen_release": "按多重释放规则，依次释放后续最多16个装配格的内容，不能用于剑术。",
    "rule_exhaustive_release": "按多重释放规则，依次释放后续直到链尾的装配内容。",
    "rule_double_cast": "将后续首个接受复制的宿主复制为2份。",
    "rule_quintuple_cast": "将后续首个接受复制的宿主复制为5份。",
    "rule_octuple_cast": "将后续首个接受复制的宿主复制为8份。",
}
SOURCE_ROWS = {
    "rule_triple_release": 20,
    "rule_quadruple_release": 21,
    "rule_octuple_release": 22,
    "rule_sixteen_release": 23,
    "rule_exhaustive_release": 24,
    "rule_double_cast": 25,
    "rule_quintuple_cast": 26,
    "rule_octuple_cast": 27,
}


def note_for(original):
    return original + "\n" + FEEDBACK + "\n" + MARKER + "\n" + COMPILE


def build(source_bytes):
    entries, data = gate.load_bytes(source_bytes)
    names = gate.sheet_map(data)
    making = data[names["制作中"]].decode("utf-8")
    acceptance = data[names["待验收"]].decode("utf-8")
    archive = data[names["待制作原始数据"]]
    sst = data["xl/sharedStrings.xml"].decode("utf-8")
    items = gate.shared_items(sst)
    making_rows = gate.rows_of(making)
    acceptance_rows = gate.rows_of(acceptance)
    if max(acceptance_rows) != 13 or 14 in acceptance_rows:
        raise SystemExit("Acceptance sheet end moved")
    if gate.cell_map(acceptance_rows.get(13, ""), items).get("B") != "water_vortex":
        raise SystemExit("water_vortex is not on acceptance row 13")
    prior = {row["id"]: row for row in json.loads((ROOT / "move-rows.json").read_text(encoding="utf-8"))["rows"]}
    intern, added, _ = gate.intern_factory(sst)
    moved = []
    new_rows = []
    making_new = making
    for offset, stable in enumerate(AFTER_N):
        source_row = SOURCE_ROWS[stable]
        row = making_rows.get(source_row)
        current = gate.cell_map(row, items) if row else {}
        if current.get("B") != stable or current.get("N") != prior[stable]["N"] or current.get("O") != prior[stable]["O"] or current.get("DV") != prior[stable]["makingDV"]:
            raise SystemExit(f"{stable} wording changed before migration")
        destination = 14 + offset
        updated_note = note_for(current["DV"])
        height = max(gate.row_height(updated_note, AFTER_N[stable]), 120)
        moved_row = gate.retarget(row, source_row, destination, height)
        moved_row = gate.replace_string_cell(moved_row, "N", destination, intern(AFTER_N[stable]))
        moved_row = gate.replace_string_cell(moved_row, "O", destination, intern(ICON))
        moved_row = gate.replace_string_cell(moved_row, "DV", destination, intern(updated_note))
        new_rows.append(moved_row)
        making_new = making_new.replace(row, "", 1)
        moved.append({
            "id": stable,
            "name": current.get("A"),
            "fromRow": source_row,
            "toRow": destination,
            "before": current,
            "afterN": AFTER_N[stable],
            "afterO": ICON,
            "afterDV": updated_note,
        })
    if any(gate.rows_of(making_new).get(number) for number in range(20, 28)):
        raise SystemExit("Source rows remain")
    for number, row in making_rows.items():
        if number < 20 and row not in making_new:
            raise SystemExit(f"Existing making row {number} changed")
    making_new = making_new.replace('ref="A1:EN27"', 'ref="A1:EN19"', 1)
    acceptance_new = acceptance.replace("</sheetData>", "".join(new_rows) + "</sheetData>", 1)
    acceptance_new = acceptance_new.replace('ref="A1:EN13"', 'ref="A1:EN21"', 1)
    for number, row in acceptance_rows.items():
        if row not in acceptance_new:
            raise SystemExit(f"Existing acceptance row {number} changed")
    book = data["xl/workbook.xml"].decode("utf-8").replace("制作中!$A$2:$EN$27", "制作中!$A$2:$EN$19", 1)
    if "制作中!$A$2:$EN$27" in book or book.count("制作中!$A$2:$EN$19") != 1:
        raise SystemExit("Making filter did not shrink cleanly")
    if "待验收!$A$2:$EN$2" not in book:
        raise SystemExit("Acceptance filter changed unexpectedly")
    reference_delta = (
        gate.string_refs(making_new) + gate.string_refs(acceptance_new)
        - gate.string_refs(making) - gate.string_refs(acceptance)
    )
    sst_new = gate.append_strings(sst, added, reference_delta)
    final_items = gate.shared_items(sst_new)
    for spec, row in zip(moved, new_rows):
        values = gate.cell_map(row, final_items)
        if values.get("N") != spec["afterN"] or values.get("O") != spec["afterO"] or values.get("DV") != spec["afterDV"]:
            raise SystemExit(f"Converted text mismatch for {spec['id']}")
        if "不能用于剑术" in values["N"] and spec["id"] not in ("rule_octuple_release", "rule_sixteen_release"):
            raise SystemExit(f"Sword limit leaked into {spec['id']}")
        if spec["id"] in ("rule_octuple_release", "rule_sixteen_release") and not values["N"].endswith("不能用于剑术。"):
            raise SystemExit("Sword limit missing from high multicast")
        if "穷尽释放不用限制" not in values["DV"] or "游戏未测" not in values["DV"] or MARKER not in values["DV"]:
            raise SystemExit("DV append incomplete")
        for column, value in spec["before"].items():
            if column in ("N", "O", "DV"):
                continue
            if values.get(column) != value:
                raise SystemExit(f"{spec['id']} column {column} changed")
    patch = {
        names["制作中"]: making_new.encode("utf-8"),
        names["待验收"]: acceptance_new.encode("utf-8"),
        "xl/workbook.xml": book.encode("utf-8"),
        "xl/sharedStrings.xml": sst_new.encode("utf-8"),
    }
    if archive != data[names["待制作原始数据"]]:
        raise SystemExit("Archive bytes changed in memory")
    return entries, data, patch, moved


def main():
    source = LIVE.read_bytes()
    source_hash = gate.sha256(source)
    if source_hash != BASELINE:
        raise SystemExit(f"HASH_CHANGED {source_hash}")
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = ROOT / "备份" / f"术式制作表-{stamp}-3.xlsx"
    candidate = ROOT / "候选" / "candidate-accept.xlsx"
    shutil.copyfile(LIVE, backup)
    entries, data, patch, moved = build(source)
    changed = gate.write_package(entries, data, patch, candidate)
    _, written = gate.load_package(candidate)
    names = gate.sheet_map(written)
    if written[names["待制作原始数据"]] != data[names["待制作原始数据"]]:
        raise SystemExit("Archive part changed")
    for sheet in ("已完成", "待制作", "数据表", "资源状态明细"):
        if written[names[sheet]] != data[names[sheet]]:
            raise SystemExit(f"{sheet} changed")
    summary = gate.assert_conservation(written, "accept", "move")
    counts = summary["counts"]
    if counts != {"已完成": 37, "待制作": 0, "制作中": 16, "待验收": 17}:
        raise SystemExit(f"Unexpected counts {counts}")
    items = gate.shared_items(written["xl/sharedStrings.xml"].decode("utf-8"))
    status = gate.status_ids(written, names, items)
    placed = {item[1]: (name, item[0]) for name in ("已完成", "待制作", "制作中", "待验收") for item in status[name]}
    for stable, source_row in SOURCE_ROWS.items():
        if placed.get(stable) != ("待验收", SOURCE_ROWS[stable] - 6):
            raise SystemExit(f"{stable} landed at {placed.get(stable)}")
    payload = {
        "sourceHash": source_hash,
        "candidateHash": gate.sha256(candidate.read_bytes()),
        "backup": str(backup),
        "candidate": str(candidate),
        "changedParts": changed,
        "counts": counts,
        "rows": moved,
        "approvedVersion": "user-release-variants-20261109-v3-carrier-limits",
        "resource": ICON,
    }
    (ROOT / "acceptance-snapshot.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"candidateHash": payload["candidateHash"], "counts": counts, "changedParts": changed, "backup": backup.name}, ensure_ascii=False))


if __name__ == "__main__":
    main()
