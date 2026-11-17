# -*- coding: utf-8 -*-
"""Append the blocked-compile note to the 13 making rows. Do not migrate them."""
import importlib.util
import json
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET
import zipfile

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
spec = importlib.util.spec_from_file_location("archive_write", HERE / "archive-write.py")
aw = importlib.util.module_from_spec(spec)
spec.loader.exec_module(aw)

MARKER = "[阶段 2026-10-31] 实现与确认v1资源已接入。"
COMMON = (
    "\n" + MARKER
    + "正式复审累计设计2/2、实现3/3，额度已用完，不能自动再审。"
    + "待统一编译：本仓开发客户端和runClient仍占用Gradle，GradleDaemon PID 12896，Forge客户端 PID 45212。"
    + "compileJava没有执行。这不是编译器报错，也不是编译通过。"
    + "关闭该开发客户端并结束对应runClient后，重新核对源码再编译；编译成功后才按规则迁待验收。"
    + "证据 docs/开发记录/术式批量制作/待制作-20261031/final-compile-report.md。"
    + "记录 docs/开发记录/术式批量制作/待制作-20261031/记录.md。"
)
GLOOM = (
    "D已声明：原版火焰粒子、燎系剑火、炉身、雾雨和重压水面不改色。"
    "切片超过3072时这一发不染蓝，产种仍走原来的接触判定。不能写成全部火系通道都已染蓝。"
)
TAIL = "\n制作中 | 待制作-20261031 | docs/开发记录/术式批量制作/待制作-20261031/记录.md"


def note_for(code):
    extra = GLOOM if code == "fire_nether_gloom" else ""
    return COMMON + extra + TAIL


def load_shared(parts):
    shared = []
    if "xl/sharedStrings.xml" not in parts:
        return shared
    root = ET.fromstring(parts["xl/sharedStrings.xml"])
    for item in root:
        if aw.local(item.tag) == "si":
            texts = []
            for node in item.iter():
                if aw.local(node.tag) == "t" and node.text:
                    texts.append(node.text)
            shared.append("".join(texts))
    return shared


def status_counts(parts, names, shared):
    counts = {}
    all_ids = []
    pages = {}
    for title in ("已完成", "待制作", "制作中", "待验收"):
        page = aw.ids_of(parts[names[title]].decode("utf-8"), shared)
        counts[title] = len(page)
        all_ids.extend(code for code, _number, _row in page)
        pages[title] = page
    archive = aw.ids_of(parts[names["待制作原始数据"]].decode("utf-8"), shared)
    return counts, len(set(all_ids)), pages, archive


def main():
    live_bytes = LIVE.read_bytes()
    baseline = aw.sha256_bytes(live_bytes)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = HERE / "备份" / f"术式制作表-blocked-compile-{stamp}-1.xlsx"
    output = HERE / "blocked-compile-candidate-preserved.xlsx"
    backup.parent.mkdir(exist_ok=True)
    backup.write_bytes(live_bytes)
    with zipfile.ZipFile(backup) as archive:
        order = archive.infolist()
        parts = {info.filename: archive.read(info.filename) for info in order}
    shared = load_shared(parts)
    names = aw.sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
    before_counts, before_unique, pages, archive_ids = status_counts(parts, names, shared)
    making_xml = parts[names["制作中"]].decode("utf-8")
    located = {code: (number, row) for code, number, row in pages["制作中"]}
    missing = [code for code in aw.IDS if code not in located]
    if missing:
        aw.fail("制作中缺少: " + ",".join(missing))
    intake = json.loads((HERE / "intake.json").read_text(encoding="utf-8"))
    intake_rows = {item["id"]: item for item in intake["pending"]}
    user_edits = []
    edited = {}
    changes = []
    for code in aw.IDS:
        number, row = located[code]
        original_n = aw.read_at(row, "N", number, shared)
        original_o = aw.read_at(row, "O", number, shared)
        original_dv = aw.read_at(row, "DV", number, shared) or ""
        if not original_n or not original_dv:
            aw.fail(f"{code} 缺少原文或备注")
        intake_n = intake_rows[code]["cells"]["N"]["v"]
        intake_o = intake_rows[code]["cells"].get("O", {}).get("v")
        if original_n != intake_n or original_o != intake_o:
            user_edits.append({
                "id": code,
                "row": number,
                "n": original_n,
                "o": original_o,
                "intakeNChanged": original_n != intake_n,
                "intakeOChanged": original_o != intake_o,
            })
        if MARKER in original_dv:
            aw.fail(f"{code} 已经写过这次阻塞记录")
        sample = aw.cell_span(row, "A", number)
        text_style = aw.style_of(sample[0]) if sample else None
        row, action = aw.force_text(row, "DV", number, original_dv + note_for(code), {original_dv}, shared, text_style)
        if aw.read_at(row, "N", number, shared) != original_n or aw.read_at(row, "O", number, shared) != original_o:
            aw.fail(f"{code} 的 N 或 O 被改动")
        if original_dv not in (aw.read_at(row, "DV", number, shared) or ""):
            aw.fail(f"{code} 原备注丢失")
        edited[number] = row
        changes.append({"id": code, "row": number, "from": "制作中", "to": "制作中", "dv": action})
    new_rows = []
    for row in aw.rows_of(making_xml):
        number = aw.row_number(row)
        new_rows.append(edited.get(number, row))
    making_out = aw.replace_data(making_xml, new_rows)
    changed = {names["制作中"]: making_out.encode("utf-8")}
    with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
    with zipfile.ZipFile(output) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if different != [names["制作中"]]:
            aw.fail("意外改动的部件: " + ",".join(different))
        if dst.read(names["待制作原始数据"]) != parts[names["待制作原始数据"]]:
            aw.fail("档案表被改动")
        ET.fromstring(dst.read(names["制作中"]))
        new_parts = dict(parts)
        new_parts[names["制作中"]] = dst.read(names["制作中"])
        after_counts, after_unique, after_pages, after_archive = status_counts(new_parts, names, shared)
        if after_counts != before_counts or after_unique != before_unique:
            aw.fail(f"行数变化 {before_counts} -> {after_counts}")
        if [code for code, _number, _row in after_archive] != [code for code, _number, _row in archive_ids]:
            aw.fail("档案编号变化")
        making_ids = [code for code, _number, _row in after_pages["制作中"]]
        if [code for code in making_ids if code in aw.IDS] != aw.IDS:
            aw.fail("13项不在制作中或顺序变了")
        for code, number, row in after_pages["制作中"]:
            if code in aw.IDS and MARKER not in (aw.read_at(row, "DV", number, shared) or ""):
                aw.fail(f"{code} 没有阻塞记录")
            if code not in aw.IDS:
                original = next(source for source_code, _source_number, source in pages["制作中"] if source_code == code)
                if row != original:
                    aw.fail(f"其他制作中行被改动: {code}")
    summary = {
        "baselineSha256": baseline,
        "candidate": str(output),
        "candidateSha256": aw.sha256_bytes(output.read_bytes()),
        "backup": str(backup),
        "beforeCounts": before_counts,
        "beforeUnique": before_unique,
        "archiveCount": len(archive_ids),
        "changes": changes,
        "userEdits": user_edits,
        "submitted": False,
    }
    (HERE / "blocked-compile-build.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps({
        "baseline": baseline,
        "candidate": summary["candidateSha256"],
        "counts": before_counts,
        "unique": before_unique,
        "archive": len(archive_ids),
        "userEdits": len(user_edits),
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
