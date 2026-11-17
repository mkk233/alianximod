# -*- coding: utf-8 -*-
"""把已确认的燃烧 v3 空间挂载 bbmodel 转成运行网格、贴图和入场分件。

只读示意模型与已有贴图。不重绘、不改色。示意辅助不写出。
"""
import base64
import hashlib
import io
import json
import shutil
from pathlib import Path

from PIL import Image

ROOT = Path(r"D:\alianximod")
SOURCE = ROOT / "docs" / "资源库" / "过程归档" / "燃烧v3_Blockbench挂载示意-20261101"
MODEL_PATH = SOURCE / "燃烧v3_空间挂载示意_v1.bbmodel"
RUNTIME_TEX = ROOT / "src" / "main" / "resources" / "assets" / "xueximod" / "textures" / "effect" / "burn_spatial_v1"
RUNTIME_MODEL = ROOT / "src" / "main" / "resources" / "assets" / "xueximod" / "models" / "effect" / "burn_spatial_v1"
ENTRY_DIR = ROOT / "src" / "main" / "resources" / "assets" / "xueximod" / "textures" / "effect" / "furnace_v2"
PROCESS = ROOT / "docs" / "资源库" / "过程归档" / "燃烧空间挂载接入-20261101"

FRAME_WIDTH = 96
FRAME_HEIGHT = 112
LOOP_FRAMES = 20
ENTRY_FRAMES = 8
SHOULDER_Y = 24.0

BONE_PIVOTS = {
    "body": (0.0, 0.0, 0.0),
    "right_arm": (-5.0, 2.0, 0.0),
    "left_arm": (5.0, 2.0, 0.0),
    "right_leg": (-1.9, 12.0, 0.0),
    "left_leg": (1.9, 12.0, 0.0),
}
BONE_NAMES = set(BONE_PIVOTS)
TEXTURE_KEYS = {
    0: "cape",
    1: "rear",
    3: "hand",
    6: "shoulder_right",
    7: "shoulder_left",
    8: "abdomen_right",
    9: "abdomen_left",
    10: "hip_left",
    11: "shin_right",
    12: "shin_left",
    13: "hand_embers_right",
    14: "hand_embers_left",
    15: "body_embers",
}
ENTRY_FULL = {
    "cape": ENTRY_DIR / "burn_entry_cape.png",
    "rear": ENTRY_DIR / "burn_entry_rear_fire.png",
    "hand": ENTRY_DIR / "burn_entry_hand_fire.png",
}
BODY_PARTS = [
    "shoulder_right",
    "shoulder_left",
    "abdomen_right",
    "abdomen_left",
    "hip_left",
    "shin_right",
    "shin_left",
]
EMBER_PARTS = ["hand_embers_right", "hand_embers_left", "body_embers"]
# 入场像素落在循环遮罩外时，按原稿火舌根部就近归簇。坐标是贴图像素，Y 向下。
BODY_ROOTS = {
    "shoulder_right": (39.0, 54.0),
    "shoulder_left": (57.0, 51.0),
    "abdomen_right": (41.0, 68.0),
    "abdomen_left": (50.0, 66.0),
    "hip_left": (57.0, 76.0),
    "shin_right": (39.0, 94.0),
    "shin_left": (54.0, 94.0),
}


def sha256_file(path):
    digest = hashlib.sha256()
    digest.update(path.read_bytes())
    return digest.hexdigest()


def bb_to_model(point_x, point_y, point_z):
    return (point_x, SHOULDER_Y - point_y, -point_z)


def round_num(number):
    return round(float(number), 5)


def load_model():
    with MODEL_PATH.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def group_index(model):
    names = {}
    parents = {}
    for group in model["groups"]:
        names[group["uuid"]] = group["name"]
    # 父级来自 outliner
    def walk(node, parent_uuid):
        if isinstance(node, list):
            for child in node:
                walk(child, parent_uuid)
            return
        if isinstance(node, str):
            parents.setdefault("element:" + node, parent_uuid)
            return
        group_uuid = node["uuid"]
        parents[group_uuid] = parent_uuid
        for child in node.get("children", []):
            walk(child, group_uuid)

    walk(model["outliner"], None)
    return names, parents


def bone_for_element(element_uuid, names, parents):
    current = parents.get("element:" + element_uuid)
    while current:
        name = names.get(current, "")
        if name in BONE_NAMES:
            return name
        current = parents.get(current)
    return None


def decode_texture(texture):
    source = texture["source"]
    marker = "base64,"
    payload = source[source.index(marker) + len(marker):]
    return Image.open(io.BytesIO(base64.b64decode(payload))).convert("RGBA")


def union_mask(image):
    width, height = image.size
    frames = height // FRAME_HEIGHT
    mask = Image.new("L", (FRAME_WIDTH, FRAME_HEIGHT), 0)
    mask_px = mask.load()
    image_px = image.load()
    counts = [[0 for _ in range(FRAME_WIDTH)] for _ in range(FRAME_HEIGHT)]
    for frame_cursor in range(frames):
        origin_y = frame_cursor * FRAME_HEIGHT
        for row in range(FRAME_HEIGHT):
            for column in range(FRAME_WIDTH):
                if image_px[column, origin_y + row][3] > 0:
                    counts[row][column] += 1
                    mask_px[column, row] = 255
    return mask, counts


def nearest_part(column, row, roots):
    best_name = None
    best_distance = None
    for name, (root_x, root_y) in roots.items():
        distance = (column - root_x) ** 2 + (row - root_y) ** 2
        if best_distance is None or distance < best_distance:
            best_distance = distance
            best_name = name
    return best_name


def split_entry(source_path, part_images, part_counts, roots, height):
    source = Image.open(source_path).convert("RGBA")
    if source.size != (FRAME_WIDTH, height):
        raise SystemExit(f"入场尺寸不符 {source_path} {source.size}")
    sheets = {name: Image.new("RGBA", (FRAME_WIDTH, height), (0, 0, 0, 0)) for name in part_images}
    source_px = source.load()
    sheet_px = {name: sheet.load() for name, sheet in sheets.items()}
    assigned = {name: 0 for name in part_images}
    unmatched = 0
    overlap_pixels = 0
    owners = {}
    for name, counts in part_counts.items():
        for row in range(FRAME_HEIGHT):
            for column in range(FRAME_WIDTH):
                if counts[row][column] <= 0:
                    continue
                owners.setdefault((column, row), []).append((counts[row][column], name))
    for key, listed in owners.items():
        if len(listed) > 1:
            overlap_pixels += 1
    chosen = {}
    for key, listed in owners.items():
        listed.sort(key=lambda item: (-item[0], item[1]))
        chosen[key] = listed[0][1]
    for frame_cursor in range(ENTRY_FRAMES):
        origin_y = frame_cursor * FRAME_HEIGHT
        for row in range(FRAME_HEIGHT):
            for column in range(FRAME_WIDTH):
                pixel = source_px[column, origin_y + row]
                if pixel[3] == 0:
                    continue
                owner = chosen.get((column, row))
                if owner is None:
                    unmatched += 1
                    owner = nearest_part(column, row, roots)
                sheet_px[owner][column, origin_y + row] = pixel
                assigned[owner] += 1
    return sheets, assigned, unmatched, overlap_pixels


def bake_geometry(model, names, parents):
    bones = {name: [] for name in BONE_PIVOTS}
    used_textures = set()
    skipped = []
    for element in model["elements"]:
        name = element.get("name", "")
        if name.startswith("示意辅助") or element.get("type") != "mesh":
            skipped.append(name)
            continue
        bone_name = bone_for_element(element["uuid"], names, parents)
        if bone_name is None:
            raise SystemExit("网格没有骨骼: " + name)
        pivot = BONE_PIVOTS[bone_name]
        texture_ids = set()
        faces = []
        for face in element["faces"].values():
            texture_id = int(face["texture"])
            if texture_id not in TEXTURE_KEYS:
                raise SystemExit(f"{name} 使用了未允许贴图 {texture_id}")
            texture_ids.add(texture_id)
            if len(face["vertices"]) != 4:
                raise SystemExit(name + " 不是四边面")
            corners = []
            for vertex_key in face["vertices"]:
                raw = element["vertices"][vertex_key]
                model_point = bb_to_model(raw[0], raw[1], raw[2])
                local = (
                    model_point[0] - pivot[0],
                    model_point[1] - pivot[1],
                    model_point[2] - pivot[2],
                )
                uv = face["uv"][vertex_key]
                corners.append([
                    round_num(local[0]),
                    round_num(local[1]),
                    round_num(local[2]),
                    round_num(uv[0] / FRAME_WIDTH),
                    round_num(uv[1] / FRAME_HEIGHT),
                ])
            faces.append(corners)
        if len(texture_ids) != 1:
            raise SystemExit(name + " 混合了多张贴图")
        texture_key = TEXTURE_KEYS[texture_ids.pop()]
        used_textures.add(texture_key)
        bones[bone_name].append({
            "mesh": name,
            "texture": texture_key,
            "faces": faces,
        })
    return bones, sorted(used_textures), skipped


def bounds_of(bones):
    report = {}
    for bone_name, meshes in bones.items():
        lows = [999.0, 999.0, 999.0]
        highs = [-999.0, -999.0, -999.0]
        face_count = 0
        for mesh in meshes:
            for face in mesh["faces"]:
                face_count += 1
                for corner in face:
                    for axis in range(3):
                        lows[axis] = min(lows[axis], corner[axis])
                        highs[axis] = max(highs[axis], corner[axis])
        report[bone_name] = {
            "meshes": [mesh["mesh"] for mesh in meshes],
            "faces": face_count,
            "min": [round_num(value) for value in lows],
            "max": [round_num(value) for value in highs],
        }
    return report


def write_loop_textures(model):
    loop_dir = RUNTIME_TEX / "loop"
    loop_dir.mkdir(parents=True, exist_ok=True)
    written = {}
    for texture in model["textures"]:
        texture_id = int(texture.get("id"))
        if texture_id not in TEXTURE_KEYS:
            continue
        image = decode_texture(texture)
        if image.size != (FRAME_WIDTH, FRAME_HEIGHT * LOOP_FRAMES):
            raise SystemExit(f"循环贴图尺寸不符 {texture['name']} {image.size}")
        key = TEXTURE_KEYS[texture_id]
        path = loop_dir / f"{key}.png"
        image.save(path, format="PNG")
        written[key] = str(path)
    return written


def write_entry_textures(loop_dir):
    entry_dir = RUNTIME_TEX / "entry"
    entry_dir.mkdir(parents=True, exist_ok=True)
    notes = {"full": {}, "body": {}, "embers": {}}
    for key, source in ENTRY_FULL.items():
        image = Image.open(source).convert("RGBA")
        if image.size != (FRAME_WIDTH, FRAME_HEIGHT * ENTRY_FRAMES):
            raise SystemExit(f"入场整层尺寸不符 {source} {image.size}")
        path = entry_dir / f"{key}.png"
        image.save(path, format="PNG")
        notes["full"][key] = {"source": str(source), "sha256": sha256_file(path)}
    body_source = ENTRY_DIR / "burn_entry_body_fire.png"
    body_images = {}
    body_counts = {}
    for key in BODY_PARTS:
        image = Image.open(loop_dir / f"{key}.png").convert("RGBA")
        _mask, counts = union_mask(image)
        body_images[key] = image
        body_counts[key] = counts
    body_sheets, body_assigned, body_unmatched, body_overlap = split_entry(
        body_source, body_images, body_counts, BODY_ROOTS, FRAME_HEIGHT * ENTRY_FRAMES)
    for key, sheet in body_sheets.items():
        sheet.save(entry_dir / f"{key}.png", format="PNG")
    notes["body"] = {
        "source": str(body_source),
        "assigned": body_assigned,
        "unmatched_copied_by_nearest_root": body_unmatched,
        "union_overlap_pixels": body_overlap,
    }
    ember_source = ENTRY_DIR / "burn_entry_embers.png"
    ember_images = {}
    ember_counts = {}
    for key in EMBER_PARTS:
        image = Image.open(loop_dir / f"{key}.png").convert("RGBA")
        _mask, counts = union_mask(image)
        ember_images[key] = image
        ember_counts[key] = counts
    ember_roots = {
        "hand_embers_right": (32.0, 40.0),
        "hand_embers_left": (66.0, 40.0),
        "body_embers": (48.0, 60.0),
    }
    ember_sheets, ember_assigned, ember_unmatched, ember_overlap = split_entry(
        ember_source, ember_images, ember_counts, ember_roots, FRAME_HEIGHT * ENTRY_FRAMES)
    for key, sheet in ember_sheets.items():
        sheet.save(entry_dir / f"{key}.png", format="PNG")
    notes["embers"] = {
        "source": str(ember_source),
        "assigned": ember_assigned,
        "unmatched_copied_by_nearest_root": ember_unmatched,
        "union_overlap_pixels": ember_overlap,
        "basis": "循环分件并集遮罩逐像素拷贝原入场 RGBA；遮罩外像素不改色，按原稿火舌根部就近归入已有分件",
    }
    return notes


def main():
    model = load_model()
    names, parents = group_index(model)
    for group in model["groups"]:
        rotation = group.get("rotation") or [0, 0, 0]
        if any(abs(float(value)) > 1e-6 for value in rotation):
            raise SystemExit("休息姿态旋转不是 0: " + group["name"])
    bones, textures, skipped = bake_geometry(model, names, parents)
    right_anchor = bb_to_model(-6.0, 13.2, 0.0)
    left_anchor = bb_to_model(6.0, 13.2, 0.0)
    right_pivot = BONE_PIVOTS["right_arm"]
    left_pivot = BONE_PIVOTS["left_arm"]
    anchors = {
        "right_arm": [round_num(right_anchor[axis] - right_pivot[axis]) for axis in range(3)],
        "left_arm": [round_num(left_anchor[axis] - left_pivot[axis]) for axis in range(3)],
    }
    payload = {
        "version": "burn-v3-spatial-v1",
        "frame_width": FRAME_WIDTH,
        "frame_height": FRAME_HEIGHT,
        "loop_frames": LOOP_FRAMES,
        "entry_frames": ENTRY_FRAMES,
        "model_to_block": 0.0625,
        "anchors": anchors,
        "bones": bones,
    }
    RUNTIME_MODEL.mkdir(parents=True, exist_ok=True)
    model_path = RUNTIME_MODEL / "mount.json"
    with model_path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, ensure_ascii=False, separators=(",", ":"))
        handle.write("\n")
    written = write_loop_textures(model)
    entry_notes = write_entry_textures(RUNTIME_TEX / "loop")
    summary = {
        "skipped_helpers": skipped,
        "textures": textures,
        "bounds": bounds_of(bones),
        "anchors": anchors,
        "loop_textures": written,
        "entry": entry_notes,
        "mount_sha256": sha256_file(model_path),
        "mount_bytes": model_path.stat().st_size,
    }
    report_path = PROCESS / "bake-report.json"
    with report_path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(summary, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    print(json.dumps({
        "skipped": skipped,
        "bounds": summary["bounds"],
        "anchors": anchors,
        "entry_body_unmatched": entry_notes["body"]["unmatched_copied_by_nearest_root"],
        "entry_ember_unmatched": entry_notes["embers"]["unmatched_copied_by_nearest_root"],
        "body_overlap": entry_notes["body"]["union_overlap_pixels"],
        "ember_overlap": entry_notes["embers"]["union_overlap_pixels"],
        "mount_bytes": summary["mount_bytes"],
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
