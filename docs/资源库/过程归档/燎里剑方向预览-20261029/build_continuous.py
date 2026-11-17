# 连续剑身草稿。不覆盖已打开的串珠模型，也不写游戏源码。
import base64
import json
import math
import struct
import uuid
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent
MODEL_PATH = ROOT / "liaoli-hilt-near-tip-far-continuous.bbmodel"

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
HILT_LENGTH = 2.5
HILT_THICK = 0.6
GUARD_WIDTH = 2.5
GUARD_THICK = 0.6
GUARD_LENGTH = 0.45
BLADE_ROOT_WIDTH = 1.4
BLADE_THICK = 0.25
TIP_LENGTH = 2.2


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


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def mul(a, scale):
    return (a[0] * scale, a[1] * scale, a[2] * scale)


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def cross(a, b):
    return (
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    )


def length(a):
    return math.sqrt(dot(a, a))


def norm(a):
    span = length(a)
    if span <= 0.0:
        raise SystemExit("zero vector")
    return mul(a, 1.0 / span)


def fan_point(angle_degrees, radius, depth):
    radians = math.radians(angle_degrees)
    lateral = math.cos(radians) * radius
    rise = -math.sin(radians) * radius
    return (lateral, CENTER_Y + rise, depth)


def along(hilt, direction, distance):
    return add(hilt, mul(direction, distance))


def frame(angle_degrees, hilt, tip):
    direction = norm(sub(tip, hilt))
    radians = math.radians(angle_degrees)
    tangent = norm((-math.sin(radians), -math.cos(radians), 0.0))
    if abs(dot(direction, tangent)) > 1.0e-6:
        raise SystemExit("blade axes are not perpendicular")
    normal = norm(cross(direction, tangent))
    return direction, tangent, normal


def corners(center, tangent, normal, half_tangent, half_normal):
    return [
        sub(sub(center, mul(tangent, half_tangent)), mul(normal, half_normal)),
        add(sub(center, mul(normal, half_normal)), mul(tangent, half_tangent)),
        add(add(center, mul(tangent, half_tangent)), mul(normal, half_normal)),
        sub(add(center, mul(normal, half_normal)), mul(tangent, half_tangent)),
    ]


def r3(point):
    return [round(point[0], 4), round(point[1], 4), round(point[2], 4)]


def quad(vertices, texture):
    return {
        "uv": {
            vertices[0]: [0, 0],
            vertices[1]: [16, 0],
            vertices[2]: [16, 16],
            vertices[3]: [0, 16],
        },
        "vertices": vertices,
        "texture": texture,
    }


def triangle(vertices, texture):
    return {
        "uv": {
            vertices[0]: [0, 16],
            vertices[1]: [16, 16],
            vertices[2]: [8, 0],
        },
        "vertices": vertices,
        "texture": texture,
    }


def mesh(name, origin, texture_hint):
    return {
        "name": name,
        "type": "mesh",
        "uuid": new_id(),
        "color": texture_hint,
        "origin": r3(origin),
        "rotation": [0, 0, 0],
        "visibility": True,
        "locked": False,
        "export": True,
        "render_order": "default",
        "allow_mirror_modeling": True,
        "scope": 0,
        "vertices": {},
        "faces": {},
    }


def put_vertex(element, point):
    key = new_id()
    element["vertices"][key] = r3(point)
    return key


def prism(name, start, end, tangent, normal, half_start, half_end, texture, tint, cap_end=True):
    element = mesh(name, mul(add(start, end), 0.5), tint)
    near = [put_vertex(element, point) for point in corners(start, tangent, normal, half_start[0], half_start[1])]
    far = [put_vertex(element, point) for point in corners(end, tangent, normal, half_end[0], half_end[1])]
    faces = [
        quad([near[0], near[3], near[2], near[1]], texture),
        quad([near[1], near[2], far[2], far[1]], texture),
        quad([near[3], far[3], far[2], near[2]], texture),
        quad([near[0], far[0], far[3], near[3]], texture),
        quad([near[0], near[1], far[1], far[0]], texture),
    ]
    if cap_end:
        faces.append(quad([far[0], far[1], far[2], far[3]], texture))
    for face in faces:
        element["faces"][new_id()] = face
    return element, far


def cone(name, base_keys, base_points, apex, texture, tint):
    element = mesh(name, apex, tint)
    copied = []
    for point in base_points:
        copied.append(put_vertex(element, point))
    apex_key = put_vertex(element, apex)
    order = [
        (0, 1),
        (1, 2),
        (2, 3),
        (3, 0),
    ]
    for first, second in order:
        element["faces"][new_id()] = triangle([copied[first], copied[second], apex_key], texture)
    return element


def cube(name, center, size, texture, tint):
    half = size / 2.0
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
        "origin": r3(center),
        "rotation": [0, 0, 0],
        "visibility": True,
        "faces": {side: dict(face) for side in ("north", "east", "south", "west", "up", "down")},
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
        "visibility": True,
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


def group(name, origin):
    return {
        "name": name,
        "uuid": new_id(),
        "export": True,
        "locked": False,
        "scope": 0,
        "selected": False,
        "visibility": True,
        "origin": r3(origin),
        "rotation": [0, 0, 0],
        "color": 0,
        "children": [],
        "reset": False,
        "shade": True,
        "mirror_uv": False,
        "autouv": 0,
        "isOpen": True,
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
    body, back, eye, chest, hilt_tex, blade_tex, tip_tex = range(7)
    elements = []
    player_ids = []

    def keep(node, bucket):
        elements.append(node)
        bucket.append(node["uuid"])

    keep(body_cube("head", [-4, 24, -4], [4, 32, 4], body, back, body, 9), player_ids)
    keep(body_cube("torso", [-4, 12, -2], [4, 24, 2], body, back, body, 9), player_ids)
    keep(body_cube("arm_right", [-8, 12, -2], [-4, 24, 2], body, back, body, 9), player_ids)
    keep(body_cube("arm_left", [4, 12, -2], [8, 24, 2], body, back, body, 9), player_ids)
    keep(body_cube("leg_right", [-4, 0, -2], [0, 12, 2], body, back, body, 9), player_ids)
    keep(body_cube("leg_left", [0, 0, -2], [4, 12, 2], body, back, body, 9), player_ids)
    keep(cube("eye_right", (-1.7, 28.6, -4.45), 1.15, eye, 0), player_ids)
    keep(cube("eye_left", (1.7, 28.6, -4.45), 1.15, eye, 0), player_ids)
    keep(cube("chest_mark", (0.0, 19.0, -2.45), 1.6, chest, 4), player_ids)
    keep(cube("back_ridge", (0.0, 18.0, 2.7), 1.5, back, 8), player_ids)
    keep(body_cube("back_ridge_tall", [-1.1, 13.5, 2.0], [1.1, 23.0, 3.3], back, back, back, 8), player_ids)

    player_group = group("Player", (0.0, 12.0, 0.0))
    fan_group = group("BackFan", (0.0, CENTER_Y, HILT_Z))
    sword_groups = []
    fan_outliner = []
    for index, angle in enumerate(FAN_DEGREES):
        hilt = fan_point(angle, HILT_RADIUS, HILT_Z)
        tip = fan_point(angle, TIP_RADIUS, TIP_Z)
        direction, tangent, normal = frame(angle, hilt, tip)
        span = length(sub(tip, hilt))
        if span <= HILT_LENGTH + TIP_LENGTH:
            raise SystemExit("sword is too short")
        hilt_end = along(hilt, direction, HILT_LENGTH)
        guard_start = along(hilt, direction, HILT_LENGTH - GUARD_LENGTH * 0.5)
        guard_end = along(hilt, direction, HILT_LENGTH + GUARD_LENGTH * 0.5)
        blade_end = along(hilt, direction, span - TIP_LENGTH)
        sword_ids = []
        hilt_mesh, _ = prism(
            f"{SWORD_NAMES[index]}_hilt",
            hilt,
            hilt_end,
            tangent,
            normal,
            (HILT_THICK * 0.5, HILT_THICK * 0.5),
            (HILT_THICK * 0.5, HILT_THICK * 0.5),
            hilt_tex,
            3,
        )
        guard_mesh, _ = prism(
            f"{SWORD_NAMES[index]}_guard",
            guard_start,
            guard_end,
            tangent,
            normal,
            (GUARD_WIDTH * 0.5, GUARD_THICK * 0.5),
            (GUARD_WIDTH * 0.5, GUARD_THICK * 0.5),
            hilt_tex,
            3,
        )
        blade_mesh, far_keys = prism(
            f"{SWORD_NAMES[index]}_blade",
            hilt_end,
            blade_end,
            tangent,
            normal,
            (BLADE_ROOT_WIDTH * 0.5, BLADE_THICK * 0.5),
            (0.16, 0.04),
            blade_tex,
            4,
            cap_end=False,
        )
        far_points = [tuple(blade_mesh["vertices"][key]) for key in far_keys]
        tip_mesh = cone(f"{SWORD_NAMES[index]}_tip", far_keys, far_points, tip, tip_tex, 4)
        for part in (hilt_mesh, guard_mesh, blade_mesh, tip_mesh):
            keep(part, sword_ids)
        sword_group = group(SWORD_NAMES[index], hilt)
        sword_group["isOpen"] = False
        sword_groups.append(sword_group)
        fan_outliner.append({"uuid": sword_group["uuid"], "isOpen": False, "children": sword_ids})

    model = {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": "liaoli-hilt-near-tip-far-continuous",
        "model_identifier": "liaoli_hilt_near_tip_far_continuous",
        "visible_box": [4, 4, 4],
        "variable_placeholders": "",
        "multi_file_ruleset": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 16, "height": 16},
        "elements": elements,
        "groups": [player_group, fan_group] + sword_groups,
        "outliner": [
            {"uuid": player_group["uuid"], "isOpen": True, "children": player_ids},
            {"uuid": fan_group["uuid"], "isOpen": True, "children": fan_outliner},
        ],
        "textures": textures,
        "animations": [],
    }
    if MODEL_PATH.name == "liaoli-hilt-near-tip-far.bbmodel":
        raise SystemExit("refusing to overwrite the open preview")
    MODEL_PATH.write_text(json.dumps(model, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"{MODEL_PATH} elements={len(elements)}")


if __name__ == "__main__":
    main()
