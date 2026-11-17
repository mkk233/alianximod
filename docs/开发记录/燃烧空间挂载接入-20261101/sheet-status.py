# -*- coding: utf-8 -*-
import hashlib
import json
import re
from pathlib import Path

folder = Path(r"D:\alianximod\docs\资源库\特效\火焰\燃烧_空间挂载_v1")
readme_path = folder / "README.md"
manifest_path = folder / "manifest.json"
old_readme = "当前状态：用户已确认，已接入源码，`compileJava` 已通过，待用户游戏实测。"
new_readme = "当前状态：用户已确认，已接入源码，`compileJava` 已通过，制作表已迁待验收，待用户游戏实测。"
old_status = '"status": "用户已确认，已接入源码，compileJava 已通过，待用户游戏实测"'
new_status = '"status": "用户已确认，已接入源码，compileJava 已通过，制作表已迁待验收，待用户游戏实测"'
art_suffix = {".aseprite", ".bbmodel", ".png", ".gif"}
before = {
    path.name: hashlib.sha256(path.read_bytes()).hexdigest()
    for path in folder.iterdir()
    if path.suffix.lower() in art_suffix
}
manifest = manifest_path.read_text(encoding="utf-8")
hashes = re.findall(r'"sha256": "([0-9a-f]{64})"', manifest)
readme = readme_path.read_text(encoding="utf-8")
if readme.count(old_readme) != 1 or manifest.count(old_status) != 1:
    raise SystemExit("status text not unique")
readme_path.write_text(readme.replace(old_readme, new_readme, 1), encoding="utf-8")
manifest_path.write_text(manifest.replace(old_status, new_status, 1), encoding="utf-8")
after_manifest = manifest_path.read_text(encoding="utf-8")
after_hashes = re.findall(r'"sha256": "([0-9a-f]{64})"', after_manifest)
after = {
    path.name: hashlib.sha256(path.read_bytes()).hexdigest()
    for path in folder.iterdir()
    if path.suffix.lower() in art_suffix
}
parsed = json.loads(after_manifest)
if before != after or hashes != after_hashes:
    raise SystemExit("source hash changed")
for record in parsed["files"]:
    digest = hashlib.sha256((folder / record["file"]).read_bytes()).hexdigest()
    if digest != record["sha256"]:
        raise SystemExit("mismatch " + record["file"])
record_path = Path(r"D:\alianximod\docs\开发记录\燃烧空间挂载接入-20261101\调度记录.md")
text = record_path.read_text(encoding="utf-8")
if "2026-11-01 制作表写回：" in text:
    raise SystemExit("record already appended")
if not text.rstrip().endswith("不修改其他术式，不标已完成。"):
    raise SystemExit("record ending changed")
extra = (
    "\n\n2026-11-01 制作表写回：fire_burn 从制作中第4行迁到待验收第7行。"
    "N 改为游戏内一句，O 改为 burn-v3-spatial-v1 的正式源和运行资源。"
    "DV 保留原文并追加编译结果。升温、冷却和其他术式未改。"
    "待制作原始数据未覆盖。游戏待用户实测，未标已完成，Codex未复审。"
    "详见 sheet-writer-report.md。\n"
)
record_path.write_text(text.rstrip() + extra, encoding="utf-8")
print("status_ok", len(parsed["files"]), len(before))
