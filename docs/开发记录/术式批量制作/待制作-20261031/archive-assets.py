# -*- coding: utf-8 -*-
"""Copy adopted v1 art into the resource library. Do not edit pixels."""
import hashlib
import json
import shutil
import struct
import zlib
from pathlib import Path

ROOT = Path(r"D:\alianximod\docs\资源库")
SRC = ROOT / "过程归档" / "新十三式二维草稿-20261031"
OUT = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")

ITEMS = [
    {
        "id": "fire_spark",
        "name": "火苗",
        "folder": ROOT / "特效" / "火焰" / "火苗_v1",
        "object": "飞行火苗条带",
        "files": ["fire_spark.aseprite", "fire_spark-strip.png"],
        "runtime": "assets/xueximod/textures/effect/fire_spark_v1/fire_spark.png",
        "runtime_note": "运行时使用竖条 PNG，不要把 aseprite 直接放进 src",
    },
    {
        "id": "fire_flame_trail",
        "name": "留焰",
        "folder": ROOT / "特效" / "火焰" / "留焰_v1",
        "object": "短焰采样条带",
        "files": ["flame_trail.aseprite", "flame_trail-strip.png"],
        "runtime": "assets/xueximod/textures/effect/flame_trail_v1/flame_trail.png",
        "runtime_note": "运行时使用竖条 PNG",
    },
    {
        "id": "fire_meteorite",
        "name": "陨岩",
        "folder": ROOT / "材质" / "方块" / "陨岩_v1",
        "object": "陨岩方块",
        "files": [
            "meteorite-block-texture.aseprite",
            "meteorite-block-texture.png",
            "meteorite.aseprite",
            "meteorite-strip.png",
        ],
        "runtime": "assets/xueximod/textures/block/meteorite_v1.png",
        "runtime_note": "六面只接 32×32 的 meteorite-block-texture.png。64×64 条带只是外观预览，不能贴到立方体六个面",
    },
    {
        "id": "fire_shock_ring",
        "name": "热波",
        "folder": ROOT / "特效" / "火焰" / "热波_v1",
        "object": "热波环条带",
        "files": ["shock_ring.aseprite", "shock_ring-strip.png"],
        "runtime": "assets/xueximod/textures/effect/shock_ring_v1/shock_ring.png",
        "runtime_note": "运行时使用竖条 PNG",
    },
    {
        "id": "rule_surface_anchor",
        "name": "地表定位",
        "folder": ROOT / "特效" / "标记" / "地表定位_v1",
        "object": "地表锚点条带",
        "files": ["surface_anchor.aseprite", "surface_anchor-strip.png"],
        "runtime": "assets/xueximod/textures/effect/surface_anchor_v1/surface_anchor.png",
        "runtime_note": "运行时使用竖条 PNG",
    },
    {
        "id": "fire_seed_sow",
        "name": "种火",
        "folder": ROOT / "特效" / "火焰" / "种火_v1",
        "object": "播种火种条带",
        "files": ["seed_sow.aseprite", "seed_sow-strip.png"],
        "runtime": "assets/xueximod/textures/effect/seed_sow_v1/seed_sow.png",
        "runtime_note": "运行时使用竖条 PNG",
    },
    {
        "id": "fire_kindle",
        "name": "引燃",
        "folder": ROOT / "特效" / "火焰" / "引燃_v1",
        "object": "引燃条带",
        "files": ["kindle.aseprite", "kindle-strip.png"],
        "runtime": "assets/xueximod/textures/effect/kindle_v1/kindle.png",
        "runtime_note": "运行时使用竖条 PNG",
    },
    {
        "id": "fire_detonate",
        "name": "引爆",
        "folder": ROOT / "特效" / "火焰" / "引爆_v1",
        "object": "引爆条带",
        "files": ["detonate.aseprite", "detonate-strip.png"],
        "runtime": "assets/xueximod/textures/effect/detonate_v1/detonate.png",
        "runtime_note": "运行时使用竖条 PNG",
    },
    {
        "id": "fire_nether_gloom",
        "name": "幽冥",
        "folder": ROOT / "特效" / "火焰" / "幽冥_v1",
        "object": "幽冥领域条带",
        "files": ["nether_gloom.aseprite", "nether_gloom-strip.png"],
        "runtime": "assets/xueximod/textures/effect/nether_gloom_v1/nether_gloom.png",
        "runtime_note": "运行时使用竖条 PNG。蓝色只是视觉，不生成原版火",
    },
    {
        "id": "earth_break_ground",
        "name": "崩地",
        "folder": ROOT / "特效" / "大地" / "崩地_v1",
        "object": "土尘条带",
        "files": ["ground_dust.aseprite", "ground_dust-strip.png"],
        "runtime": "assets/xueximod/textures/effect/ground_dust_v1/ground_dust.png",
        "runtime_note": "只有土尘条带。没有坑形贴图，不能把土尘贴成碗形地形",
    },
]

PREVIEWS = ["review-board.gif", "review-board.png", "review-board.aseprite"]


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def png_info(path):
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        return {"png": False}
    width, height, bit_depth, color_type = struct.unpack(">IIBB", data[16:26])
    return {
        "png": True,
        "width": width,
        "height": height,
        "bitDepth": bit_depth,
        "colorType": color_type,
    }


def paeth(left, up, upper_left):
    estimate = left + up - upper_left
    left_distance = abs(estimate - left)
    up_distance = abs(estimate - up)
    upper_distance = abs(estimate - upper_left)
    if left_distance <= up_distance and left_distance <= upper_distance:
        return left
    if up_distance <= upper_distance:
        return up
    return upper_left


def corner_alpha(path):
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    width, height, bit_depth, color_type = struct.unpack(">IIBB", data[16:26])
    if bit_depth != 8 or color_type not in (2, 6):
        return {"skipped": "not 8-bit RGB or RGBA", "width": width, "height": height}
    channels = 4 if color_type == 6 else 3
    offset = 8
    compressed = b""
    while offset + 8 <= len(data):
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        kind = data[offset + 4:offset + 8]
        chunk = data[offset + 8:offset + 8 + length]
        if kind == b"IDAT":
            compressed += chunk
        if kind == b"IEND":
            break
        offset += 12 + length
    raw = zlib.decompress(compressed)
    stride = width * channels
    rows = []
    cursor = 0
    previous = bytearray(stride)
    for _row_index in range(height):
        filter_type = raw[cursor]
        cursor += 1
        scan = bytearray(raw[cursor:cursor + stride])
        cursor += stride
        current = bytearray(stride)
        for index, value in enumerate(scan):
            left = current[index - channels] if index >= channels else 0
            up = previous[index]
            upper_left = previous[index - channels] if index >= channels else 0
            if filter_type == 0:
                current[index] = value
            elif filter_type == 1:
                current[index] = (value + left) & 255
            elif filter_type == 2:
                current[index] = (value + up) & 255
            elif filter_type == 3:
                current[index] = (value + ((left + up) // 2)) & 255
            elif filter_type == 4:
                current[index] = (value + paeth(left, up, upper_left)) & 255
            else:
                return {"skipped": "unknown filter " + str(filter_type)}
        rows.append(current)
        previous = current
    points = [(0, 0), (width - 1, 0), (0, height - 1), (width - 1, height - 1)]
    alphas = []
    for point_x, point_y in points:
        pixel = rows[point_y][point_x * channels:(point_x + 1) * channels]
        alpha = pixel[3] if channels == 4 else 255
        alphas.append({"x": point_x, "y": point_y, "alpha": alpha})
    return {"corners": alphas}


def main():
    for item in ITEMS:
        folder = item["folder"]
        if folder.exists() and any(folder.iterdir()):
            raise SystemExit("正式目录已有内容，停止以免覆盖: " + str(folder))
        folder.mkdir(parents=True, exist_ok=True)
        for name in item["files"] + PREVIEWS:
            source = SRC / name
            if not source.is_file():
                raise SystemExit("缺少源文件: " + str(source))
            shutil.copy2(source, folder / name)
    manifest_items = []
    for item in ITEMS:
        folder = item["folder"]
        files = []
        for path in sorted(folder.iterdir(), key=lambda entry: entry.name.lower()):
            record = {
                "name": path.name,
                "bytes": path.stat().st_size,
                "sha256": sha256(path),
            }
            if path.suffix.lower() == ".png":
                record.update(png_info(path))
                record["corners"] = corner_alpha(path)
            files.append(record)
        readme = folder / "README.md"
        lines = [
            "# " + item["name"] + " v1",
            "",
            "采用状态：用户 2026-10-31 原话“先保留这个版本，额度不够用了暂时，之后再重置，暂时直接按照这个版本执行”。这是暂用归档，不是游戏实测通过，也没有复制进 `src/main/resources`。",
            "",
            "来源：`docs/资源库/过程归档/新十三式二维草稿-20261031/`。像素未改。",
            "",
            "逻辑对象：" + item["object"] + "。",
            "",
            item["runtime_note"] + "。建议运行时路径：`" + item["runtime"] + "`。",
            "",
            "预览板是最近邻 3 倍，每帧 80ms，不是批准的世界尺寸或伤害节拍。",
            "",
        ]
        if item["id"] == "fire_meteorite":
            lines.append("32×32 的 `meteorite-block-texture.png` 才是方块六面贴图。`meteorite-strip.png` 是 64×64 外观预览。")
            lines.append("")
        readme.write_text("\n".join(lines), encoding="utf-8")
        manifest_items.append({
            "id": item["id"],
            "name": item["name"],
            "logicalObject": item["object"],
            "formalDirectory": str(folder).replace("\\", "/"),
            "recommendedRuntimePath": item["runtime"],
            "runtimeNote": item["runtime_note"],
            "copiedToSrc": False,
            "playtestPassed": False,
            "files": files,
        })
    no_art = [
        {"id": "fire_upwell", "name": "喷涌", "note": "暂用载体运动示意，无独立新素材"},
        {"id": "rule_stagger", "name": "错时", "note": "暂用载体运动示意，无独立新素材"},
        {"id": "rule_hit_continue", "name": "命中接续", "note": "暂用载体运动示意，无独立新素材"},
    ]
    payload = {
        "version": "v1",
        "authorization": "先保留这个版本，额度不够用了暂时，之后再重置，暂时直接按照这个版本执行",
        "playtestPassed": False,
        "copiedToSrc": False,
        "items": manifest_items,
        "noNewArt": no_art,
    }
    (OUT / "resource-manifest-v1.json").write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    md = [
        "# 新十三式二维 v1 资源清单",
        "",
        "用户暂用授权：“先保留这个版本，额度不够用了暂时，之后再重置，暂时直接按照这个版本执行”。未进 src，未做游戏实测。",
        "",
        "| 编号 | 对象 | 正式目录 | 建议运行时路径 |",
        "| --- | --- | --- | --- |",
    ]
    for item in manifest_items:
        md.append("| " + item["id"] + " | " + item["logicalObject"] + " | `" + item["formalDirectory"] + "` | `" + item["recommendedRuntimePath"] + "` |")
    md.append("")
    md.append("喷涌、错时、命中接续没有新的攻击素材。陨岩六面只用 32×32 方块贴图。每项 PNG 的尺寸、帧高和 SHA256 在 `resource-manifest-v1.json`。")
    md.append("")
    (OUT / "resource-manifest-v1.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    print("ITEMS", len(manifest_items))
    for item in manifest_items:
        print(item["id"], item["formalDirectory"])
        for record in item["files"]:
            extra = ""
            if record.get("png"):
                extra = " %sx%s corners=%s" % (record["width"], record["height"], record.get("corners"))
            print(" ", record["name"], record["sha256"][:16], extra)


if __name__ == "__main__":
    main()
