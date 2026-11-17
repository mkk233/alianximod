# -*- coding: utf-8 -*-
"""Move fire_detonate to acceptance with an XML-preserving row copy."""
import importlib.util
import io
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
spec = importlib.util.spec_from_file_location("prior", batch / "detonate-v2-3d-candidate-writeback-20261116.py")
prior = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prior)

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
expected_baseline = "7E2B700C5F83C20964495BED4F7C2D6ED2E27F670FE9892D258C8D4A021F65E4"
expected_bytes = 188379
expected_n = "461AE1A12693676FF94273A432ADD5BA9307F0C93C483E7F9E2F672008A7D329"
expected_o = "34AF39A9173DD98D1DB200DB61E47E2337863414A640ACBAAA55B9146937C66F"
expected_dv = "72B68E6CF722CE798370F0294EF7D76A22F475ECC7C753A4A7945E0FE776991F"
new_n = "按当时余额引爆一次。不附加直接伤害"
new_o = (
    "普通 assets/xueximod/textures/effect/detonate_v2_3d/explosion_puff_orange.png；"
    "幽冥 assets/xueximod/textures/effect/detonate_v2_3d/explosion_puff_soul.png"
)
dv_addition = (
    "[美术进度 2026-11-16] 用户原话：“可以先采用这一版”，绑定 fire_detonate_v2_3d_candidate_20261116。"
    "正式归档 docs/资源库/特效/火焰/引爆_三维扩散_v2/。运行普通 assets/xueximod/textures/effect/detonate_v2_3d/explosion_puff_orange.png，"
    "幽冥 assets/xueximod/textures/effect/detonate_v2_3d/explosion_puff_soul.png，与确认版逐字节一致。"
    "26团按真实spent播放，S0或重复不播；S大于100视觉保持1.35，不封顶消费或伤害。"
    "扫描接触点已传递；扫描无交点沿用载体位置，其他无逐目标交点路径回退目标当时脚底，不表示所有路径都是精确碰撞点。"
    "compileJava --offline --no-daemon 于2026-11-16 16:48:39.021–16:49:00.628退出码0。未游戏实测，不迁已完成。"
    "详见 docs/开发记录/术式批量制作/待制作-20261031/detonate-v2-3d-final-sheet-20261116.md。\n"
    "待验收 | 待制作-20261031 | docs/开发记录/术式批量制作/待制作-20261031/记录.md"
)


def renumber(row_xml, old, new):
    updated, row_count = re.subn(
        rf'(<row\b[^>]*\br="){old}(")',
        lambda match: match.group(1) + str(new) + match.group(2),
        row_xml,
        count=1,
    )
    updated, ref_count = re.subn(
        rf'\br="([A-Z]+){old}"',
        lambda match: 'r="' + match.group(1) + str(new) + '"',
        updated,
    )
    if row_count != 1 or ref_count < 1 or prior.row_number(updated) != new:
        prior.fail(f"Row {old} did not become {new}")
    return updated, ref_count


def counts_of(data):
    found = {}
    codes = []
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        names = archive.namelist()
        if len(names) != 27 or "xl/sharedStrings.xml" in names:
            prior.fail("ZIP inventory changed")
        for label, name in prior.parts.items():
            rows = prior.sheet_ids(archive.read(name).decode("utf-8"))
            found[label] = len(rows)
            codes.extend(code for code, _, _ in rows)
    if len(codes) != 78 or len(set(codes)) != 78:
        prior.fail(f"B identity failed: {found}")
    return found


def main():
    live_bytes = prior.read_shared(live)
    if prior.sha256(live_bytes) != expected_baseline or len(live_bytes) != expected_bytes:
        prior.fail("Baseline changed before candidate build")
    sheets, before_counts, _ = prior.inspect(live_bytes)
    making_name = prior.parts["制作中"]
    accept_name = prior.parts["待验收"]
    matches = [item for item in prior.sheet_ids(sheets[making_name]) if item[0] == "fire_detonate"]
    if len(matches) != 1:
        prior.fail("fire_detonate is not unique in making")
    _, source_number, source_row = matches[0]
    if source_number != 9:
        prior.fail(f"fire_detonate row is {source_number}, not 9")
    values = prior.full_row(prior.row_values(source_row))
    old_n, old_o, old_dv = values.get("N") or "", values.get("O") or "", values.get("DV") or ""
    if prior.sha256(old_n.encode("utf-8")) != expected_n or prior.sha256(old_o.encode("utf-8")) != expected_o:
        prior.fail("N/O changed")
    if prior.sha256(old_dv.encode("utf-8")) != expected_dv or "可以先采用这一版" in old_dv:
        prior.fail("DV baseline changed or approval already written")
    edited = prior.set_cell(source_row, "N", source_number, new_n)
    edited = prior.set_cell(edited, "O", source_number, new_o)
    edited = prior.set_cell(edited, "DV", source_number, old_dv + "\n" + dv_addition)
    edited_values = prior.full_row(prior.row_values(edited))
    for column, value in values.items():
        if column not in {"N", "O", "DV"} and edited_values.get(column) != value:
            prior.fail(f"Column {column} changed")
    if not edited_values.get("DV").startswith(old_dv):
        prior.fail("DV prefix lost")
    moved, ref_count = renumber(edited, source_number, 27)
    moved_values = prior.full_row(prior.row_values(moved))
    if moved_values != {**values, "N": new_n, "O": new_o, "DV": old_dv + "\n" + dv_addition}:
        prior.fail("Moved row values changed outside N/O/DV")
    making = sheets[making_name]
    accept = sheets[accept_name]
    if making.count(source_row) != 1 or "<dimension ref=\"A1:EN28\"/>" not in making:
        prior.fail("Making sheet is not in the expected shape")
    if "<dimension ref=\"A1:EN26\"/>" not in accept or "<row r=\"27\"" in accept or accept.count("</sheetData>") != 1:
        prior.fail("Acceptance sheet is not in the expected shape")
    new_making = making.replace(source_row, "", 1)
    if "fire_detonate" in new_making or "<row r=\"9\"" in new_making:
        prior.fail("Source row was not removed cleanly")
    if new_making.count("<dataValidation ") != making.count("<dataValidation ") or new_making.count("<mergeCell ") != making.count("<mergeCell "):
        prior.fail("Making validation or merge changed")
    new_accept = accept.replace("<dimension ref=\"A1:EN26\"/>", "<dimension ref=\"A1:EN27\"/>", 1)
    new_accept = new_accept.replace("</sheetData>", moved + "</sheetData>", 1)
    if new_accept.count("<dataValidation ") != accept.count("<dataValidation ") or new_accept.count("<mergeCell ") != accept.count("<mergeCell "):
        prior.fail("Acceptance validation or merge changed")
    inserted = [item for item in prior.sheet_ids(new_accept) if item[0] == "fire_detonate"]
    if len(inserted) != 1 or inserted[0][1] != 27 or new_accept.count('<row r="27"') != 1:
        prior.fail("Target row was not inserted once")

    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = batch / "备份" / f"detonate-v2-3d-final-术式制作表-{stamp}-1.xlsx"
    output = batch / "候选" / f"detonate-v2-3d-final-术式制作表-{stamp}-1.xlsx"
    snapshot = batch / "detonate-v2-3d-final-requirements-20261116.json"
    if backup.exists() or output.exists() or snapshot.exists():
        prior.fail("Output already exists")
    backup.parent.mkdir(parents=True, exist_ok=True)
    output.parent.mkdir(parents=True, exist_ok=True)
    backup.write_bytes(live_bytes)
    replacements = {making_name: new_making.encode("utf-8"), accept_name: new_accept.encode("utf-8")}
    with zipfile.ZipFile(io.BytesIO(live_bytes)) as src, zipfile.ZipFile(output, "w") as dst:
        for info in src.infolist():
            payload = replacements.get(info.filename, src.read(info.filename))
            cloned = zipfile.ZipInfo(filename=info.filename, date_time=info.date_time)
            cloned.compress_type = info.compress_type
            cloned.external_attr = info.external_attr
            cloned.flag_bits = info.flag_bits
            cloned.create_system = info.create_system
            cloned.comment = info.comment
            dst.writestr(cloned, payload)
    candidate = output.read_bytes()
    after_counts = counts_of(candidate)
    if before_counts != {"已完成": 37, "待制作": 0, "制作中": 22, "待验收": 19}:
        prior.fail("Before counts changed")
    if after_counts != {"已完成": 37, "待制作": 0, "制作中": 21, "待验收": 20}:
        prior.fail(f"After counts are {after_counts}")
    with zipfile.ZipFile(io.BytesIO(live_bytes)) as src, zipfile.ZipFile(io.BytesIO(candidate)) as dst:
        if src.namelist() != dst.namelist():
            prior.fail("ZIP order changed")
        for name in src.namelist():
            if name not in replacements and src.read(name) != dst.read(name):
                prior.fail(f"Unrelated ZIP entry changed: {name}")
        target = [item for item in prior.sheet_ids(dst.read(accept_name).decode("utf-8")) if item[0] == "fire_detonate"]
        if len(target) != 1 or target[0][1] != 27:
            prior.fail("Reopened target mismatch")
        reopened = prior.full_row(prior.row_values(target[0][2]))
        if reopened.get("N") != new_n or reopened.get("O") != new_o or reopened.get("DV") != old_dv + "\n" + dv_addition:
            prior.fail("Reopened N/O/DV mismatch")
    payload = {
        "batchId": "待制作-20261031",
        "taskId": "fire_detonate_v2_progress",
        "attemptId": "20261116-sheet-detonate-3d-ready-07",
        "sourceSheet": "制作中",
        "sourceRow": 9,
        "targetSheet": "待验收",
        "targetRow": 27,
        "baselineSha256": expected_baseline,
        "candidateSha256": prior.sha256(candidate),
        "candidateBytes": len(candidate),
        "before": {"N": old_n, "O": old_o, "DV": old_dv},
        "after": {"N": new_n, "O": new_o, "DV": old_dv + "\n" + dv_addition},
        "cellSha256Before": {"N": expected_n, "O": expected_o, "DV": expected_dv},
        "approvedRequirements": "docs/开发记录/术式批量制作/待制作-20261031/detonate-v2-3d-approved-requirements-20261116.json",
        "resourceMapping": {
            "orange": "assets/xueximod/textures/effect/detonate_v2_3d/explosion_puff_orange.png",
            "soul": "assets/xueximod/textures/effect/detonate_v2_3d/explosion_puff_soul.png",
        },
        "countsBefore": before_counts,
        "countsAfter": after_counts,
        "movedCellRefs": ref_count,
    }
    snapshot.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps({"backup": str(backup), "candidate": str(output), "candidateSha256": payload["candidateSha256"], "candidateBytes": len(candidate), "counts": after_counts, "dvLength": len(payload["after"]["DV"])}, ensure_ascii=False))


if __name__ == "__main__":
    main()
