# 导出 v8 水环运行时网格，并对照 Blockbench 金标准做渲染检查。不启动 Gradle。
import hashlib
import json
import math
import shutil
import struct
from pathlib import Path

ROOT = Path(r"D:\alianximod")
TASK = ROOT / "docs" / "资源库" / "过程归档" / "水环接入-20261027"
MODEL = ROOT / "docs" / "资源库" / "特效" / "水流" / "周身水环_细碎毛边_v8" / "周身水环_细碎水膜毛边_v8.bbmodel"
ATLAS = TASK / "water_ring_v8_atlas.png"
REFERENCE = TASK / "blockbench-native-transform-reference.json"
GAME_TEXTURE = ROOT / "src" / "main" / "resources" / "assets" / "xueximod" / "textures" / "entity" / "xueximod" / "water_ring_v8.png"
GAME_MESH = ROOT / "src" / "main" / "resources" / "assets" / "xueximod" / "waterring" / "water_ring_v8_mesh.json"
RUNTIME = ROOT / "docs" / "资源库" / "特效" / "水流" / "周身水环_细碎毛边_v8" / "runtime"
PIVOT = (0.0, 15.0, 0.0)
LAYERS = (
    ("破碎流转主体_平滑载体", "body", 0, 24, 576, 8),
    ("整圈外缘_20簇逆时针锯齿水花", "outer", 256, 40, 96, 6),
    ("整圈内缘_16簇逆时针锯齿水花", "inner", 512, 40, 96, 7),
)


def fail(message):
    raise SystemExit(message)


def png_size(path):
    with path.open("rb") as handle:
        header = handle.read(24)
    if header[:8] != b"\x89PNG\r\n\x1a\n":
        fail(f"不是 PNG: {path}")
    return struct.unpack(">II", header[16:24])


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def spin_tilt(local, ticks):
    angle = ticks / 48.0 * math.tau
    cosine = math.cos(angle)
    sine = math.sin(angle)
    spun_x = local[0] * cosine + local[2] * sine
    spun_y = local[1]
    spun_z = -local[0] * sine + local[2] * cosine
    tilt = math.radians(3.0)
    tilt_cosine = math.cos(tilt)
    tilt_sine = math.sin(tilt)
    return (
        spun_x * tilt_cosine - spun_y * tilt_sine,
        spun_x * tilt_sine + spun_y * tilt_cosine,
        spun_z,
    )


def frame_index(age_ticks):
    whole = math.floor(age_ticks)
    return (whole // 2) % 16


def map_uv(origin_x, frame_height, frame_slot, source_u, source_v):
    frame = frame_slot % 16
    pixel_u = origin_x + 0.5 + (source_u / 256.0) * 255.0
    pixel_v = frame * frame_height + 0.5 + (source_v / frame_height) * (frame_height - 1.0)
    return pixel_u / 768.0, pixel_v / 640.0


def near_eye_alpha(fade, distance):
    if not fade:
        return 255
    if distance < 0.35:
        return 0
    if distance < 0.7:
        return int(255.0 * (distance - 0.35) / 0.35)
    return 255


def align_up(point, normal):
    length = math.sqrt(sum(component * component for component in normal))
    unit = tuple(component / length for component in normal)
    axis = (unit[2], 0.0, -unit[0])
    sine = math.sqrt(sum(component * component for component in axis))
    cosine = unit[1]
    if sine < 1.0e-8:
        if cosine >= 0.0:
            return point
        return (point[0], -point[1], -point[2])
    axis = tuple(component / sine for component in axis)
    radians = math.atan2(sine, cosine)
    cosine = math.cos(radians)
    sine = math.sin(radians)
    dot = sum(axis[index] * point[index] for index in range(3))
    cross = (
        axis[1] * point[2] - axis[2] * point[1],
        axis[2] * point[0] - axis[0] * point[2],
        axis[0] * point[1] - axis[1] * point[0],
    )
    preserved = 1.0 - cosine
    return tuple(point[index] * cosine + cross[index] * sine + axis[index] * dot * preserved for index in range(3))


def old_side(normal):
    helper = (1.0, 0.0, 0.0) if abs(normal[1]) > 0.9 else (0.0, 1.0, 0.0)
    side = (
        helper[1] * normal[2] - helper[2] * normal[1],
        helper[2] * normal[0] - helper[0] * normal[2],
        helper[0] * normal[1] - helper[1] * normal[0],
    )
    length = math.sqrt(sum(component * component for component in side))
    return tuple(component / length for component in side)


def export_layers(model):
    elements = {element["name"]: element for element in model["elements"] if element.get("type") == "mesh"}
    layers = []
    for source_name, runtime_name, origin_x, frame_height, expected_faces, texture_id in LAYERS:
        element = elements.get(source_name)
        if element is None:
            fail(f"缺少网格 {source_name}")
        faces = element["faces"]
        if len(faces) != expected_faces:
            fail(f"{source_name} 面数 {len(faces)} != {expected_faces}")
        quads = []
        for face in faces.values():
            if face.get("texture") != texture_id or len(face["vertices"]) != 4:
                fail(f"{source_name} 面贴图或顶点数不对")
            corners = []
            for vertex_id in face["vertices"]:
                position = element["vertices"][vertex_id]
                texture = face["uv"][vertex_id]
                if not (0.0 <= texture[0] <= 256.0 and 0.0 <= texture[1] <= frame_height):
                    fail(f"{source_name} UV 超出源帧 {texture}")
                corners.append([
                    position[0] - PIVOT[0],
                    position[1] - PIVOT[1],
                    position[2] - PIVOT[2],
                    texture[0],
                    texture[1],
                ])
            quads.append(corners)
        layers.append({
            "name": runtime_name,
            "sourceName": source_name,
            "atlasOriginX": origin_x,
            "frameHeight": frame_height,
            "quadCount": len(quads),
            "quads": quads,
        })
    return layers


def check_reference(reference):
    checked = 0
    worst = 0.0
    for sample_group in reference:
        ticks = sample_group["ticks"]
        for sample in sample_group["samples"]:
            for point in sample["points"]:
                local = (
                    point["source"][0] - PIVOT[0],
                    point["source"][1] - PIVOT[1],
                    point["source"][2] - PIVOT[2],
                )
                placed = spin_tilt(local, ticks)
                target = (
                    point["world"][0] - PIVOT[0],
                    point["world"][1] - PIVOT[1],
                    point["world"][2] - PIVOT[2],
                )
                error = max(abs(placed[index] - target[index]) for index in range(3))
                worst = max(worst, error)
                checked += 1
                if error > 1.0e-6:
                    fail(f"金标准偏差 {error} tick={ticks} id={point['id']}")
    return checked, worst


def check_timing():
    origin = (11.8, 1.5, 0.0)
    at_zero = spin_tilt(origin, 0)
    at_loop = spin_tilt(origin, 96)
    if max(abs(at_zero[index] - at_loop[index]) for index in range(3)) > 1.0e-8:
        fail("0 与 96 tick 旋转不一致")
    if frame_index(0) != 0 or frame_index(96) != 0 or frame_index(32) != 0 or frame_index(30) != 15:
        fail("贴图帧时序不对")
    turned = spin_tilt((1.0, 0.0, 0.0), 12)
    if abs(turned[0]) > 1.0e-9 or abs(turned[1]) > 1.0e-9 or abs(turned[2] + 1.0) > 1.0e-9:
        fail(f"12 tick 后 +X 没有到 -Z: {turned}")


def check_uv():
    body_left, body_top = map_uv(0, 24, 0, 0.0, 0.0)
    body_right, body_bottom = map_uv(0, 24, 0, 256.0, 24.0)
    if abs(body_left - 0.5 / 768.0) > 1.0e-12 or abs(body_top - 0.5 / 640.0) > 1.0e-12:
        fail("主体帧左上没有落在半像素")
    if abs(body_right - 255.5 / 768.0) > 1.0e-12 or abs(body_bottom - 23.5 / 640.0) > 1.0e-12:
        fail("主体帧右下越过了帧边界")
    _, outer_top = map_uv(256, 40, 1, 0.0, 0.0)
    if not (40.5 / 640.0 - 1.0e-12 <= outer_top <= 79.5 / 640.0):
        fail("外缘第二帧 V 不在本帧内")
    _, inner_bottom = map_uv(512, 40, 15, 256.0, 40.0)
    pixel_u = 512.0 + 255.5
    pixel_v = 15 * 40 + 39.5
    if abs(inner_bottom - pixel_v / 640.0) > 1.0e-12 or pixel_u >= 768.0 or pixel_v >= 640.0:
        fail("内缘末帧越过图集")
    first = map_uv(0, 24, 0, 0.0, 0.0)[0]
    last = map_uv(0, 24, 0, 256.0, 0.0)[0]
    middle = map_uv(0, 24, 0, 128.0, 0.0)[0]
    if abs(middle - (first + last) * 0.5) > 1.0e-12:
        fail("UV 不是整帧线性映射")


def check_near_and_far():
    if near_eye_alpha(False, 0.1) != 255 or near_eye_alpha(True, 0.2) != 0:
        fail("近眼透明度阈值变了")
    if near_eye_alpha(True, 0.525) != 127 or near_eye_alpha(True, 1.0) != 255:
        fail("近眼衰减曲线变了")
    world = 30000000.25
    camera = 29999999.5
    good = struct.unpack("f", struct.pack("f", world - camera))[0]
    broken = struct.unpack("f", struct.pack("f", struct.unpack("f", struct.pack("f", world))[0] - struct.unpack("f", struct.pack("f", camera))[0]))[0]
    if abs(good - 0.75) > 1.0e-6 or abs(broken - good) < 1.0e-3:
        fail(f"远坐标先转 float 会丢精度 good={good} broken={broken}")


def check_normal_continuity():
    def unit_xy(above):
        horizontal = math.sqrt(1.0 - above * above)
        return (horizontal, above, 0.0)

    previous = None
    crossed = False
    above = 0.95
    while above >= 0.80 - 1.0e-9:
        normal = unit_xy(above)
        image = align_up((1.0, 0.0, 0.0), normal)
        if previous is not None:
            dot = sum(previous[index] * image[index] for index in range(3))
            if dot < 0.99:
                fail(f"法线连续对齐跳变 dot={dot} y={above}")
            if previous_above > 0.9 >= above:
                crossed = True
        previous = image
        previous_above = above
        above -= 0.01
    if not crossed:
        fail("连续性检查没有越过 0.9")
    low = old_side(unit_xy(0.89))
    high = old_side(unit_xy(0.91))
    old_dot = sum(low[index] * high[index] for index in range(3))
    if old_dot > -0.5:
        fail("旧 0.9 基底没有跳相，检查无效")


def main():
    width, height = png_size(ATLAS)
    if (width, height) != (768, 640):
        fail(f"图集尺寸 {width}x{height}")
    with MODEL.open(encoding="utf-8") as handle:
        model = json.load(handle)
    cube_names = [element["name"] for element in model["elements"] if element.get("type") != "mesh"]
    if "头部" not in cube_names:
        fail("模型里没有占位人物，无法确认导出时排除了它")
    layers = export_layers(model)
    document = {
        "format": 1,
        "pivot": [0, 15, 0],
        "tiltDegrees": 3,
        "modelRadius": 11.775,
        "atlasWidth": 768,
        "atlasHeight": 640,
        "logicalWidth": 256,
        "frameCount": 16,
        "layers": layers,
    }
    payload = json.dumps(document, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    GAME_MESH.parent.mkdir(parents=True, exist_ok=True)
    RUNTIME.mkdir(parents=True, exist_ok=True)
    GAME_MESH.write_bytes(payload)
    (RUNTIME / "water_ring_v8_mesh.json").write_bytes(payload)
    GAME_TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(ATLAS, GAME_TEXTURE)
    shutil.copy2(ATLAS, RUNTIME / "water_ring_v8.png")
    mcmeta = GAME_TEXTURE.with_name("water_ring_v8.png.mcmeta")
    if mcmeta.is_file():
        shutil.copy2(mcmeta, RUNTIME / "water_ring_v8.png.mcmeta")
    if sha256(GAME_TEXTURE) != sha256(ATLAS) or sha256(RUNTIME / "water_ring_v8.png") != sha256(ATLAS):
        fail("运行时 PNG 与合并图集不一致")
    if png_size(GAME_TEXTURE) != (768, 640):
        fail("游戏贴图尺寸不对")
    with REFERENCE.open(encoding="utf-8") as handle:
        reference = json.load(handle)
    checked, worst = check_reference(reference)
    check_timing()
    check_uv()
    check_near_and_far()
    check_normal_continuity()
    quad_total = sum(layer["quadCount"] for layer in layers)
    if quad_total != 768:
        fail(f"总面数 {quad_total}")
    print(f"quads={quad_total}")
    print(f"reference_points={checked} worst_error={worst:.3e}")
    print(f"atlas_sha256={sha256(ATLAS)}")
    print(f"mesh_bytes={len(payload)}")
    print("render-check-ok")


if __name__ == "__main__":
    main()
