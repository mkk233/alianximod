# -*- coding: utf-8 -*-
"""把已确认的 burn-v3-spatial-v1 原件复制到正式目录，并核对运行贴图像素。"""
import hashlib
import json
import shutil
from pathlib import Path

from PIL import Image

ROOT = Path(r"D:\alianximod")
SOURCE = ROOT / "docs" / "资源库" / "过程归档" / "燃烧v3_Blockbench挂载示意-20261101"
FORMAL = ROOT / "docs" / "资源库" / "特效" / "火焰" / "燃烧_空间挂载_v1"
RUNTIME = ROOT / "src" / "main" / "resources" / "assets" / "xueximod"
FURNACE = RUNTIME / "textures" / "effect" / "furnace_v2"
LOOP = RUNTIME / "textures" / "effect" / "burn_spatial_v1" / "loop"

VISIBLE_STRIPS = [
    "burn-v3-cape-strip.png",
    "burn-v3-rear_right-strip.png",
    "burn-v3-rear_left-strip.png",
    "burn-v3-shoulder_right-strip.png",
    "burn-v3-shoulder_left-strip.png",
    "burn-v3-abdomen_right-strip.png",
    "burn-v3-abdomen_left-strip.png",
    "burn-v3-hip_left-strip.png",
    "burn-v3-shin_right-strip.png",
    "burn-v3-shin_left-strip.png",
    "burn-v3-hand_right-strip.png",
    "burn-v3-hand_left-strip.png",
    "burn-v3-hand_embers_right-strip.png",
    "burn-v3-hand_embers_left-strip.png",
    "burn-v3-body_embers-strip.png",
]
HIDDEN_STRIP = "burn-v3-embers-strip.png"
PREVIEWS = [
    "bb-front-animation.gif",
    "bb-back-animation.gif",
    "bb-side-depth.png",
    "bb-hand-held-detail.png",
    "blockbench-app-proof.png",
]
CORE = [
    "burn-v3-spatial-parts.aseprite",
    "燃烧v3_空间挂载示意_v1.bbmodel",
    "native-mount-parameters.json",
    "视觉交接_待确认.md",
]


def sha256_file(path):
    digest = hashlib.sha256()
    digest.update(path.read_bytes())
    return digest.hexdigest()


def same_pixels(left, right):
    with Image.open(left) as left_image, Image.open(right) as right_image:
        left_rgba = left_image.convert("RGBA")
        right_rgba = right_image.convert("RGBA")
        if left_rgba.size != right_rgba.size:
            return False, f"size {left_rgba.size} vs {right_rgba.size}"
        mismatch = 0
        left_px = left_rgba.load()
        right_px = right_rgba.load()
        width, height = left_rgba.size
        for row in range(height):
            for column in range(width):
                if left_px[column, row] != right_px[column, row]:
                    mismatch += 1
        return mismatch == 0, mismatch


def copy_named(name):
    source = SOURCE / name
    if not source.is_file():
        raise SystemExit("缺少原件 " + name)
    target = FORMAL / name
    shutil.copy2(source, target)
    source_hash = sha256_file(source)
    target_hash = sha256_file(target)
    if source_hash != target_hash:
        raise SystemExit("复制后哈希不一致 " + name)
    return {
        "file": name,
        "bytes": target.stat().st_size,
        "sha256": target_hash,
    }


def main():
    FORMAL.mkdir(parents=True, exist_ok=True)
    records = []
    for name in CORE + VISIBLE_STRIPS + [HIDDEN_STRIP] + PREVIEWS:
        records.append(copy_named(name))
    comparisons = []
    pairs = [
        (LOOP / "cape.png", SOURCE / "burn-v3-cape-strip.png", "loop cape vs part strip"),
        (LOOP / "cape.png", FURNACE / "burn_cape.png", "loop cape vs old furnace sheet"),
        (LOOP / "rear.png", FURNACE / "burn_rear_fire.png", "loop rear vs old furnace sheet"),
        (LOOP / "hand.png", FURNACE / "burn_hand_fire.png", "loop hand vs old furnace sheet"),
        (LOOP / "shoulder_right.png", SOURCE / "burn-v3-shoulder_right-strip.png", "shoulder right"),
        (LOOP / "shoulder_left.png", SOURCE / "burn-v3-shoulder_left-strip.png", "shoulder left"),
        (LOOP / "abdomen_right.png", SOURCE / "burn-v3-abdomen_right-strip.png", "abdomen right"),
        (LOOP / "abdomen_left.png", SOURCE / "burn-v3-abdomen_left-strip.png", "abdomen left"),
        (LOOP / "hip_left.png", SOURCE / "burn-v3-hip_left-strip.png", "hip left"),
        (LOOP / "shin_right.png", SOURCE / "burn-v3-shin_right-strip.png", "shin right"),
        (LOOP / "shin_left.png", SOURCE / "burn-v3-shin_left-strip.png", "shin left"),
        (LOOP / "hand_embers_right.png", SOURCE / "burn-v3-hand_embers_right-strip.png", "hand embers right"),
        (LOOP / "hand_embers_left.png", SOURCE / "burn-v3-hand_embers_left-strip.png", "hand embers left"),
        (LOOP / "body_embers.png", SOURCE / "burn-v3-body_embers-strip.png", "body embers"),
        (RUNTIME / "textures/effect/burn_spatial_v1/entry/cape.png", FURNACE / "burn_entry_cape.png", "entry cape"),
        (RUNTIME / "textures/effect/burn_spatial_v1/entry/rear.png", FURNACE / "burn_entry_rear_fire.png", "entry rear"),
        (RUNTIME / "textures/effect/burn_spatial_v1/entry/hand.png", FURNACE / "burn_entry_hand_fire.png", "entry hand"),
    ]
    for left, right, label in pairs:
        matched, detail = same_pixels(left, right)
        comparisons.append({"label": label, "matched": matched, "detail": detail})
        print(label, matched, detail)
    mount = RUNTIME / "models/effect/burn_spatial_v1/mount.json"
    text = mount.read_text(encoding="utf-8")
    if "示意辅助" in text:
        raise SystemExit("运行网格含示意辅助")
    manifest = {
        "version": "burn-v3-spatial-v1",
        "date": "2026-11-01",
        "status": "用户已确认，已接入源码，待最终编译与游戏实测",
        "approval": "可以就这版接入",
        "arm_direction": "整团火焰随手臂一起转动",
        "source_directory": str(SOURCE),
        "files": records,
    }
    manifest_path = FORMAL / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    report = {
        "copied": len(records),
        "visible_strips": len(VISIBLE_STRIPS),
        "comparisons": comparisons,
        "mount_has_helper": "示意辅助" in text,
    }
    report_path = ROOT / "docs" / "资源库" / "过程归档" / "燃烧空间挂载接入-20261101" / "archive-report.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("copied", len(records))


if __name__ == "__main__":
    main()
