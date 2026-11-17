# 只生成本次方向草稿，不读玩法表，也不写游戏源码。
import base64
import json
import math
import struct
import uuid
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent
MODEL_PATH = ROOT / "liaoli-hilt-near-tip-far.bbmodel"

# 与 LiaoliV6Layout.FAN_DEGREES 相同的左右镜像扇角。深度按本次预览反过来。
FAN_DEGREES = [-180.0, -154.0, -128.0, -102.0, -78.0, -52.0, -26.0, 0.0]
SWORD_NAMES = [
    "sword_01_right_level",
    "sword_02_right_26",
    "sword_03_right_52",
    "sword_04_right_78",
    "sword_05_left_78",
    "sword_06_left_52",
    "sword_07_left_26",
    "sword_08_left_level",
]

CENTER_Y = 18.0
HILT_RADIUS = 7.0
TIP_RADIUS = 18.0
HILT_Z = 5.5
TIP_Z = 17.5


def new_id():
    return str(uuid.uuid4())


def solid_png(red, green, blue, size=16):
    row = b"\x00" + bytes((red, green, blue)) * size
    raw = row * size

    def chunk(tag, payload):
        return struct.pack(">I", len(payload)) + tag + payload + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", size, size, 8, 2, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    return "data:image/png;base64," + base64.b64encode(png).decode("ascii")


def fan_point(angle_degrees, radius, depth):
    radians = math.radians(angle_degrees)
    lateral = math.cos(radians) * radius
    rise = -math.sin(radians) * radius
    return (lateral, CENTER_Y + rise, depth)


def add(a, b, scale):
    return (a[0] + (b[0] - a[0]) * scale, a[1] + (b[1] - a[1]) * scale, a[2] + (b[2] - a[2]) * scale)


def cube(name, center, size, texture, tint):
    half = size / 2.0
    origin = [round(center[0], 4), round(center[1], 4), round(center[2], 4)]
    start = [round(center[0] - half, 4), round(center[1] - half, 4), round(center[2] - half, 4)]
    end = [round(center[0] + half, 4), round(center[1] + half, 4), round(center[2] + half, 4)]
    face = {"uv": [0, 0, 16, 16], "texture": texture}
    return {
        "name": name,
        "box_uv": False,
        "render_order": "default",
        "locked": False,
        "export": True,
        "scope": 0,
        "allow_mirror_modeling": True,
        "from": start,
        "to": end,
        "autouv": 1,
        "color": tint,
        "origin": origin,
        "rotation": [0, 0, 0],
        "faces": {
            "north": dict(face),
            "east": dict(face),
            "south": dict(face),
            "west": dict(face),
            "up": dict(face),
            "down": dict(face),
        },
        "type": "cube",
        "uuid": new_id(),
    }


def body_cube(name, start, end, front_texture, back_texture, side_texture, tint):
    def face(texture):
        return {"uv": [0, 0, 16, 16], "texture": texture}

    return {
        "name": name,
        "box_uv": False,
        "render_order": "default",
        "locked": False,
        "export": True,
        "scope": 0,
        "allow_mirror_modeling": True,
        "from": start,
        "to": end,
        "autouv": 1,
        "color": tint,
        "origin": [
            round((start[0] + end[0]) / 2.0, 4),
            round((start[1] + end[1]) / 2.0, 4),
            round((start[2] + end[2]) / 2.0, 4),
        ],
        "rotation": [0, 0, 0],
        "faces": {
            "north": face(front_texture),
            "east": face(side_texture),
            "south": face(back_texture),
            "west": face(side_texture),
            "up": face(side_texture),
            "down": face(side_texture),
        },
        "type": "cube",
        "uuid": new_id(),
    }


def texture_entry(name, source):
    return {
        "name": name,
        "path": "",
        "folder": "",
        "namespace": "",
        "id": "0",
        "group": "",
        "scope": 0,
        "width": 16,
        "height": 16,
        "uv_width": 16,
        "uv_height": 16,
        "particle": False,
        "use_as_default": False,
        "layers_enabled": False,
        "sync_to_project": "",
        "file_format": "png",
        "render_mode": "default",
        "render_sides": "auto",
        "wrap_mode": "limited",
        "pbr_channel": "color",
        "fps": 1,
        "force_translucent": False,
        "frame_time": 1,
        "frame_order_type": "loop",
        "frame_order": "",
        "frame_interpolate": False,
        "visible": True,
        "internal": True,
        "saved": False,
        "uuid": new_id(),
        "source": source,
    }


def group(name, origin, is_open=True):
    return {
        "name": name,
        "uuid": new_id(),
        "export": True,
        "locked": False,
        "scope": 0,
        "selected": False,
        "visibility": True,
        "origin": origin,
        "rotation": [0, 0, 0],
        "color": 0,
        "children": [],
        "reset": False,
        "shade": True,
        "mirror_uv": False,
        "autouv": 0,
        "isOpen": is_open,
        "primary_selected": False,
    }


def main():
    textures = [
        texture_entry("body_gray_blue", solid_png(138, 160, 181)),
        texture_entry("back_dark", solid_png(92, 110, 128)),
        texture_entry("eye_dark", solid_png(22, 24, 30)),
        texture_entry("chest_mark", solid_png(231, 215, 161)),
        texture_entry("hilt_deep_red", solid_png(142, 28, 36)),
        texture_entry("blade_gold", solid_png(227, 155, 43)),
        texture_entry("tip_bright", solid_png(255, 224, 138)),
    ]
    body, back, eye, chest, hilt, blade, tip = range(7)

    elements = []
    player_ids = []
    fan_children = []

    def remember(node, bucket):
        elements.append(node)
        bucket.append(node["uuid"])

    remember(body_cube("head", [-4, 24, -4], [4, 32, 4], body, back, body, 9), player_ids)
    remember(body_cube("torso", [-4, 12, -2], [4, 24, 2], body, back, body, 9), player_ids)
    remember(body_cube("arm_right", [-8, 12, -2], [-4, 24, 2], body, back, body, 9), player_ids)
    remember(body_cube("arm_left", [4, 12, -2], [8, 24, 2], body, back, body, 9), player_ids)
    remember(body_cube("leg_right", [-4, 0, -2], [0, 12, 2], body, back, body, 9), player_ids)
    remember(body_cube("leg_left", [0, 0, -2], [4, 12, 2], body, back, body, 9), player_ids)
    remember(cube("eye_right", (-1.7, 28.6, -4.45), 1.15, eye, 0), player_ids)
    remember(cube("eye_left", (1.7, 28.6, -4.45), 1.15, eye, 0), player_ids)
    remember(cube("chest_mark", (0.0, 19.0, -2.45), 1.6, chest, 4), player_ids)
    remember(cube("back_ridge", (0.0, 18.0, 2.7), 1.5, back, 8), player_ids)
    # 背脊要竖长一点，单独再补一截，避免只剩一个点。
    remember(body_cube("back_ridge_tall", [-1.1, 13.5, 2.0], [1.1, 23.0, 3.3], back, back, back, 8), player_ids)

    checks = []
    for index, angle in enumerate(FAN_DEGREES):
        hilt_point = fan_point(angle, HILT_RADIUS, HILT_Z)
        tip_point = fan_point(angle, TIP_RADIUS, TIP_Z)
        if not tip_point[2] > hilt_point[2]:
            raise SystemExit("tip is not farther in +Z")
        sword_ids = []
        samples = [
            ("pommel", -0.04, 1.9, hilt),
            ("grip_near", 0.08, 1.55, hilt),
            ("grip_far", 0.16, 1.55, hilt),
            ("guard", 0.24, 2.5, hilt),
            ("blade_1", 0.36, 1.2, blade),
            ("blade_2", 0.48, 1.15, blade),
            ("blade_3", 0.60, 1.1, blade),
            ("blade_4", 0.72, 1.05, blade),
            ("blade_5", 0.84, 0.95, blade),
            ("tip_base", 0.93, 0.75, tip),
            ("tip", 1.0, 0.55, tip),
        ]
        for part_name, portion, size, texture in samples:
            center = add(hilt_point, tip_point, portion)
            remember(cube(f"{SWORD_NAMES[index]}_{part_name}", center, size, texture, 3 if texture == hilt else 4), sword_ids)
        sword_group = group(SWORD_NAMES[index], [round(hilt_point[0], 4), round(hilt_point[1], 4), round(hilt_point[2], 4)], False)
        fan_children.append({"uuid": sword_group["uuid"], "isOpen": False, "children": sword_ids, "group": sword_group})
        checks.append({
            "name": SWORD_NAMES[index],
            "angle": angle,
            "hilt": [round(v, 4) for v in hilt_point],
            "tip": [round(v, 4) for v in tip_point],
        })

    for index in range(4):
        left = checks[index]
        right = checks[7 - index]
        if abs(left["hilt"][0] + right["hilt"][0]) > 0.001 or abs(left["tip"][0] + right["tip"][0]) > 0.001:
            raise SystemExit("x mirror failed")
        if abs(left["hilt"][1] - right["hilt"][1]) > 0.001 or abs(left["tip"][1] - right["tip"][1]) > 0.001:
            raise SystemExit("y mirror failed")
        if abs(left["hilt"][2] - right["hilt"][2]) > 0.001 or abs(left["tip"][2] - right["tip"][2]) > 0.001:
            raise SystemExit("z mirror failed")
        if abs(left["angle"] + right["angle"] + 180.0) > 0.001:
            raise SystemExit("angle mirror failed")

    player_group = group("Player", [0, 12, 0], True)
    fan_group = group("BackFan", [0, CENTER_Y, HILT_Z], True)
    groups = [player_group, fan_group] + [child["group"] for child in fan_children]
    outliner = [
        {"uuid": player_group["uuid"], "isOpen": True, "children": player_ids},
        {
            "uuid": fan_group["uuid"],
            "isOpen": True,
            "children": [
                {"uuid": child["uuid"], "isOpen": False, "children": child["children"]}
                for child in fan_children
            ],
        },
    ]
    for child in fan_children:
        child["group"].pop("children", None)
        child["group"]["children"] = []

    model = {
        "meta": {
            "format_version": "5.0",
            "model_format": "free",
            "box_uv": False,
        },
        "name": "liaoli-hilt-near-tip-far",
        "model_identifier": "liaoli_hilt_near_tip_far",
        "visible_box": [4, 4, 4],
        "variable_placeholders": "",
        "multi_file_ruleset": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 16, "height": 16},
        "elements": elements,
        "groups": groups,
        "outliner": outliner,
        "textures": textures,
        "animations": [],
    }
    MODEL_PATH.write_text(json.dumps(model, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"wrote {MODEL_PATH} elements={len(elements)} swords={len(checks)}")


if __name__ == "__main__":
    main()
