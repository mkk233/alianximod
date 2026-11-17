# -*- coding: utf-8 -*-
import math
import os
import random
from PIL import Image, ImageDraw, ImageFont

BASE_DIR = r"D:\alianximod\docs\资源库\过程归档\刻印台UI_v1_黑曜星坛与隐藏背包_20261113"
EXP_DIR = os.path.join(BASE_DIR, "export")
PREV_DIR = os.path.join(BASE_DIR, "preview")
REF_DIR = r"D:\alianximod\docs\资源库\过程归档\刻印台UI重设计_概念图_20261113"
MOD_TEX = r"D:\alianximod\src\main\resources\assets\xueximod\textures"

os.makedirs(PREV_DIR, exist_ok=True)

FONT_PATH = r"C:\Windows\Fonts\simsun.ttc"
FONT_12 = ImageFont.truetype(FONT_PATH, 12)
FONT_14 = ImageFont.truetype(FONT_PATH, 14)
FONT_16 = ImageFont.truetype(FONT_PATH, 16)


def load_rgba(path):
    return Image.open(path).convert("RGBA")


def nearest_scale(im, factor):
    return im.resize((im.width * factor, im.height * factor), Image.Resampling.NEAREST)


def load_icon_16(rel_path, alpha_scale=1.0):
    p = os.path.join(MOD_TEX, rel_path)
    im = load_rgba(p)
    if im.size != (16, 16):
        im = im.resize((16, 16), Image.Resampling.NEAREST)
    if alpha_scale < 0.999:
        r, g, b, a = im.split()
        a = a.point(lambda v: int(v * alpha_scale))
        im = Image.merge("RGBA", (r, g, b, a))
    return im


ALTAR_HUB = load_rgba(os.path.join(EXP_DIR, "altar_hub.png"))
ALTAR_STRIP = load_rgba(os.path.join(EXP_DIR, "altar_hub_glow_strip.png"))
ALTAR_WIDGETS = load_rgba(os.path.join(EXP_DIR, "altar_widgets.png"))
CONFIRM_BTNS = load_rgba(os.path.join(EXP_DIR, "confirm_button_states.png"))
CORNER_BTNS = load_rgba(os.path.join(EXP_DIR, "corner_button_states.png"))
INV_DRAWER = load_rgba(os.path.join(EXP_DIR, "inventory_drawer.png"))
TREE_SHEET = load_rgba(os.path.join(EXP_DIR, "tree_nodes_sheet.png"))
BORDER_9 = load_rgba(os.path.join(EXP_DIR, "screen_border_9slice.png"))
TOOLTIP_9 = load_rgba(os.path.join(EXP_DIR, "tooltip_frame_9slice.png"))

SLIP_MORTAL = load_rgba(os.path.join(EXP_DIR, "slip_mortal_icon.png"))
SLIP_MYSTIC = load_rgba(os.path.join(EXP_DIR, "slip_mystic_icon.png"))
SLIP_TRANSC = load_rgba(os.path.join(EXP_DIR, "slip_transcendent_icon.png"))

ARROW_IDLE = ALTAR_WIDGETS.crop((0, 0, 16, 12))
ARROW_ACTIVE = ALTAR_WIDGETS.crop((16, 0, 32, 12))
PLUS_IDLE = ALTAR_WIDGETS.crop((32, 0, 46, 14))
PLUS_ACTIVE = ALTAR_WIDGETS.crop((48, 0, 62, 14))
BADGE_CHECK = ALTAR_WIDGETS.crop((0, 16, 7, 23))
BADGE_WARN = ALTAR_WIDGETS.crop((8, 16, 15, 23))
BADGE_CROSS = ALTAR_WIDGETS.crop((16, 16, 23, 23))
GHOST_AURA = ALTAR_WIDGETS.crop((32, 16, 48, 32))

TILE_INDEX = {
    "WOOD": 0,
    "FIRE": 1,
    "WATER": 2,
    "EARTH": 3,
    "METAL": 4,
    "NONE": 5,
    "RULE": 6,
    "LOCKED": 7,
}
NODE_TILES = {
    k: TREE_SHEET.crop((idx * 24, 0, idx * 24 + 22, 22))
    for k, idx in TILE_INDEX.items()
}
FRAME_SELECTED = TREE_SHEET.crop((0, 26, 26, 52))
FRAME_STUDYABLE = TREE_SHEET.crop((28, 26, 54, 52))
FRAME_HOVER = TREE_SHEET.crop((56, 26, 82, 52))
BADGE_SCROLL = TREE_SHEET.crop((88, 26, 98, 36))
BADGE_LOCK = TREE_SHEET.crop((102, 26, 110, 35))
NODE_PIP = TREE_SHEET.crop((114, 26, 122, 34))

LINE_COLORS = {
    "WOOD": (92, 198, 76, 255),
    "FIRE": (240, 126, 38, 255),
    "WATER": (62, 158, 242, 255),
    "EARTH": (176, 115, 58, 255),
    "METAL": (222, 178, 76, 255),
    "NONE": (148, 116, 206, 255),
    "RULE": (204, 150, 74, 255),
    "LOCKED": (66, 70, 90, 220),
}


def draw_9slice(dst, nine_img, x, y, w, h, c=8):
    sw, sh = nine_img.size
    mw, mh = sw - 2 * c, sh - 2 * c
    dst.alpha_composite(nine_img.crop((0, 0, c, c)), (x, y))
    dst.alpha_composite(nine_img.crop((sw - c, 0, sw, c)), (x + w - c, y))
    dst.alpha_composite(nine_img.crop((0, sh - c, c, sh)), (x, y + h - c))
    dst.alpha_composite(nine_img.crop((sw - c, sh - c, sw, sh)), (x + w - c, y + h - c))
    if w - 2 * c > 0:
        top_e = nine_img.crop((c, 0, sw - c, c)).resize((w - 2 * c, c), Image.Resampling.NEAREST)
        bot_e = nine_img.crop((c, sh - c, sw - c, sh)).resize((w - 2 * c, c), Image.Resampling.NEAREST)
        dst.alpha_composite(top_e, (x + c, y))
        dst.alpha_composite(bot_e, (x + c, y + h - c))
    if h - 2 * c > 0:
        left_e = nine_img.crop((0, c, c, sh - c)).resize((c, h - 2 * c), Image.Resampling.NEAREST)
        right_e = nine_img.crop((sw - c, c, sw, sh - c)).resize((c, h - 2 * c), Image.Resampling.NEAREST)
        dst.alpha_composite(left_e, (x, y + c))
        dst.alpha_composite(right_e, (x + w - c, y + c))
    if w - 2 * c > 0 and h - 2 * c > 0 and mw > 0 and mh > 0:
        ctr = nine_img.crop((c, c, sw - c, sh - c)).resize((w - 2 * c, h - 2 * c), Image.Resampling.NEAREST)
        dst.alpha_composite(ctr, (x + c, y + c))


def draw_starfield_1x(w, h, hub_cx, hub_cy):
    bg = Image.new("RGBA", (w, h), (7, 10, 18, 255))
    px = bg.load()
    for y in range(h):
        for x in range(w):
            dx = (x - hub_cx) / 195.0
            dy = (y - hub_cy) / 145.0
            d2 = dx * dx + dy * dy
            if d2 < 1.35:
                t = max(0.0, 1.0 - math.sqrt(d2 / 1.35))
                r = int(7 + 16 * (t ** 1.6))
                g = int(10 + 11 * (t ** 1.7))
                b = int(18 + 30 * (t ** 1.4))
                px[x, y] = (r, g, b, 255)
    rng = random.Random(20261113)
    star_cols = [
        (58, 72, 102, 255),
        (88, 106, 145, 255),
        (134, 156, 198, 255),
        (196, 212, 245, 255),
        (138, 98, 196, 255),
    ]
    for _ in range(210):
        sx = rng.randint(5, w - 6)
        sy = rng.randint(5, h - 6)
        col = star_cols[rng.randint(0, len(star_cols) - 1)]
        px[sx, sy] = col
        if rng.random() < 0.11:
            dim = (col[0] // 2, col[1] // 2, col[2] // 2, 255)
            if sx + 1 < w - 5:
                px[sx + 1, sy] = dim
            if sy + 1 < h - 5:
                px[sx, sy + 1] = dim
    return bg


def draw_thick_pixel_line(draw, x0, y0, x1, y1, col):
    shadow = (8, 9, 16, 220)
    draw.line([(x0, y0 + 1), (x1, y1 + 1)], fill=shadow, width=1)
    draw.line([(x0, y0), (x1, y1)], fill=col, width=1)


def render_pixel_text(dst_hi, text, x_hi, y_hi, font=FONT_12, scale=2, fill=(246, 244, 255, 255),
                      shadow=(14, 10, 26, 245), align="left"):
    tmp_d = ImageDraw.Draw(Image.new("RGBA", (1, 1)))
    tmp_d.fontmode = "1"
    bbox = tmp_d.textbbox((0, 0), text, font=font)
    tw = max(1, bbox[2] - bbox[0] + 2)
    th = max(1, bbox[3] - bbox[1] + 2)

    txt_im = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
    td = ImageDraw.Draw(txt_im)
    td.fontmode = "1"
    ox, oy = -bbox[0] + 1, -bbox[1] + 1
    if shadow:
        td.text((ox + 1, oy + 1), text, font=font, fill=shadow)
    td.text((ox, oy), text, font=font, fill=fill)

    up = nearest_scale(txt_im, scale)
    if align == "center":
        px = x_hi - up.width // 2
    elif align == "right":
        px = x_hi - up.width
    else:
        px = x_hi
    py = y_hi
    dst_hi.alpha_composite(up, (int(px), int(py)))
    return up.width, up.height


def build_skill_tree_spec():
    nodes = [
        {"id": "vine_mark", "label": "缠枝", "dx": -142, "dy": -108, "elem": "WOOD", "icon": "gui/technique/other_wood.png", "state": "learned"},
        {"id": "sword_guard", "label": "架剑", "dx": -88, "dy": -106, "elem": "WOOD", "icon": "item/skill_sword.png", "state": "learned"},
        {"id": "parasite_seed", "label": "寄生种子", "dx": -186, "dy": -82, "elem": "WOOD", "icon": "item/skill_attack_wood.png", "state": "learned"},
        {"id": "sword_thrust", "label": "刺击", "dx": -128, "dy": -72, "elem": "WOOD", "icon": "gui/technique/attack_wood.png", "state": "learned"},
        {"id": "movement_dash", "label": "冲刺", "dx": -84, "dy": -68, "elem": "RULE", "icon": "gui/technique/other_metal.png", "state": "learned"},
        {"id": "phantom_sword", "label": "幻剑术", "dx": -106, "dy": -28, "elem": "WOOD", "icon": "item/skill_sword.png", "state": "learned"},
        {"id": "sword_come", "label": "剑来", "dx": -154, "dy": -36, "elem": "WOOD", "icon": "gui/technique/attack_wood.png", "state": "learned"},
        {"id": "sword_return_all", "label": "万剑归宗", "dx": -202, "dy": -36, "elem": "LOCKED", "icon": "item/skill_sword.png", "state": "locked"},
        {"id": "sword_array", "label": "列剑", "dx": -142, "dy": 4, "elem": "WOOD", "icon": "item/skill_sword.png", "state": "learned"},
        {"id": "phantom_surge", "label": "幻剑纵横", "dx": -176, "dy": 12, "elem": "WOOD", "icon": "gui/technique/attack_wood.png", "state": "learned"},

        {"id": "movement_leap", "label": "跳跃", "dx": -102, "dy": 38, "elem": "RULE", "icon": "gui/technique/other_none.png", "state": "learned"},
        {"id": "rule_triple_cast", "label": "一分为三", "dx": -152, "dy": 44, "elem": "RULE", "icon": "gui/technique/other_wood.png", "state": "learned"},
        {"id": "trajectory_drop", "label": "垂落术式", "dx": -192, "dy": 52, "elem": "RULE", "icon": "gui/technique/attack_none.png", "state": "learned"},
        {"id": "rule_dual_release", "label": "双重释放", "dx": -118, "dy": 78, "elem": "RULE", "icon": "gui/technique/other_none.png", "state": "learned"},
        {"id": "rule_triple_release", "label": "三重释放", "dx": -166, "dy": 88, "elem": "RULE", "icon": "gui/technique/other_metal.png", "state": "learned"},
        {"id": "rule_oct_release", "label": "八重释放", "dx": -210, "dy": 98, "elem": "LOCKED", "icon": "gui/technique/other_none.png", "state": "locked", "hide_label": True},

        {"id": "stone_hammer", "label": "岩槌", "dx": -76, "dy": 98, "elem": "EARTH", "icon": "gui/technique/attack_earth.png", "state": "learned"},
        {"id": "earth_break", "label": "崩地", "dx": -36, "dy": 106, "elem": "EARTH", "icon": "item/skill_attack_earth.png", "state": "learned"},
        {"id": "keen_mark", "label": "锐锋印", "dx": 36, "dy": 106, "elem": "METAL", "icon": "gui/technique/other_metal.png", "state": "learned"},
        {"id": "metal_needle", "label": "金针", "dx": 78, "dy": 98, "elem": "METAL", "icon": "gui/technique/attack_metal.png", "state": "learned"},

        {"id": "fire_spark", "label": "火苗", "dx": 62, "dy": -108, "elem": "FIRE", "icon": "gui/technique/attack_fire.png", "state": "learned"},
        {"id": "fire_kindle", "label": "引燃", "dx": 108, "dy": -108, "elem": "FIRE", "icon": "gui/technique/other_fire.png", "state": "learned"},
        {"id": "fire_liaoyuan", "label": "燎原", "dx": 158, "dy": -92, "elem": "LOCKED", "icon": "gui/technique/attack_fire.png", "state": "locked"},
        {"id": "fire_seed_sow", "label": "种火", "dx": 78, "dy": -70, "elem": "FIRE", "icon": "gui/technique/other_fire.png", "state": "learned"},
        {"id": "fire_detonate", "label": "引爆", "dx": 116, "dy": -64, "elem": "FIRE", "icon": "item/skill_attack_fire.png", "state": "studyable"},
        {"id": "fire_heat_up", "label": "升温", "dx": 108, "dy": -26, "elem": "FIRE", "icon": "gui/technique/other_fire.png", "state": "learned"},
        {"id": "fire_fentian", "label": "焚天", "dx": 156, "dy": -26, "elem": "LOCKED", "icon": "item/skill_attack_fire.png", "state": "locked"},
        {"id": "magic_ignite", "label": "点火", "dx": 102, "dy": 14, "elem": "FIRE", "icon": "item/embryo_fire.png", "state": "learned"},

        {"id": "tide_mark", "label": "潮印", "dx": 112, "dy": 54, "elem": "WATER", "icon": "gui/technique/other_water.png", "state": "learned"},
        {"id": "water_prison", "label": "水牢", "dx": 156, "dy": 42, "elem": "WATER", "icon": "item/skill_attack_water.png", "state": "learned"},
        {"id": "rule_cast_anchor", "label": "落点指定", "dx": 198, "dy": 32, "elem": "WATER", "icon": "gui/technique/other_none.png", "state": "learned"},
        {"id": "traj_3d", "label": "三维交错", "dx": 118, "dy": 94, "elem": "NONE", "icon": "gui/technique/other_none.png", "state": "learned"},
        {"id": "water_vortex", "label": "水流旋涡", "dx": 162, "dy": 84, "elem": "WATER", "icon": "gui/technique/other_water.png", "state": "learned"},
        {"id": "water_deep", "label": "深海重压", "dx": 204, "dy": 98, "elem": "LOCKED", "icon": "gui/technique/other_water.png", "state": "locked"},
    ]

    edges = [
        (("HUB", -42, -66), "movement_dash", "WOOD"),
        ("movement_dash", "sword_thrust", "WOOD"),
        ("sword_thrust", "vine_mark", "WOOD"),
        ("sword_thrust", "parasite_seed", "WOOD"),
        ("movement_dash", "sword_guard", "WOOD"),
        (("HUB", -64, -26), "phantom_sword", "WOOD"),
        ("phantom_sword", "sword_come", "WOOD"),
        ("phantom_sword", "sword_array", "WOOD"),
        ("sword_array", "phantom_surge", "WOOD"),
        ("sword_come", "sword_return_all", "LOCKED"),

        (("HUB", -56, 36), "movement_leap", "RULE"),
        ("movement_leap", "rule_dual_release", "RULE"),
        ("rule_dual_release", "rule_triple_release", "RULE"),
        ("rule_triple_release", "rule_oct_release", "LOCKED"),
        ("movement_leap", "rule_triple_cast", "RULE"),
        ("rule_triple_cast", "trajectory_drop", "RULE"),

        (("HUB", -24, 68), "earth_break", "EARTH"),
        ("earth_break", "stone_hammer", "EARTH"),
        (("HUB", 24, 66), "keen_mark", "METAL"),
        ("keen_mark", "metal_needle", "METAL"),

        (("HUB", 42, -66), "fire_seed_sow", "FIRE"),
        ("fire_seed_sow", "fire_spark", "FIRE"),
        ("fire_spark", "fire_kindle", "FIRE"),
        ("fire_seed_sow", "fire_detonate", "FIRE"),
        ("fire_kindle", "fire_detonate", "FIRE"),
        ("fire_detonate", "fire_liaoyuan", "LOCKED"),
        (("HUB", 64, -20), "magic_ignite", "FIRE"),
        ("fire_seed_sow", "fire_heat_up", "FIRE"),
        ("magic_ignite", "fire_heat_up", "FIRE"),
        ("fire_heat_up", "fire_fentian", "LOCKED"),

        (("HUB", 64, 14), "tide_mark", "WATER"),
        ("tide_mark", "water_prison", "WATER"),
        ("water_prison", "rule_cast_anchor", "WATER"),
        ("tide_mark", "water_vortex", "WATER"),
        ("water_vortex", "water_deep", "LOCKED"),
        ("tide_mark", "traj_3d", "NONE"),
    ]

    pips = [
        (-114, -92), (-130, -52), (-132, 60), (-56, 96), (56, 96),
        (86, -92), (134, -84), (134, 48), (140, 70),
    ]
    return nodes, edges, pips


def render_tree_and_altar_1x(w, h, hub_cx, hub_cy, mode="study", glow_frame=0, breath=0.0,
                             hide_nodes_under_rect=None):
    canvas = draw_starfield_1x(w, h, hub_cx, hub_cy)
    draw = ImageDraw.Draw(canvas)

    nodes, edges, pips = build_skill_tree_spec()
    nmap = {n["id"]: n for n in nodes}

    def visible_pos(dx, dy):
        px, py = hub_cx + dx, hub_cy + dy
        if px < -14 or px > w + 14 or py < -14 or py > h + 14:
            return None
        return (px, py)

    for src, dst_id, ckey in edges:
        dn = nmap[dst_id]
        p2 = visible_pos(dn["dx"], dn["dy"])
        if p2 is None:
            continue
        if isinstance(src, tuple) and src[0] == "HUB":
            p1 = (hub_cx + src[1], hub_cy + src[2])
        else:
            sn = nmap[src]
            p1 = visible_pos(sn["dx"], sn["dy"])
            if p1 is None:
                continue
        draw_thick_pixel_line(draw, p1[0], p1[1], p2[0], p2[1], LINE_COLORS[ckey])

    for pdx, pdy in pips:
        pp = visible_pos(pdx, pdy)
        if pp and 8 <= pp[0] <= w - 12 and 8 <= pp[1] <= h - 12:
            if hide_nodes_under_rect:
                rx0, ry0, rx1, ry1 = hide_nodes_under_rect
                if rx0 - 6 <= pp[0] <= rx1 + 6 and ry0 - 6 <= pp[1] <= ry1 + 6:
                    continue
            canvas.alpha_composite(NODE_PIP, (pp[0] - 4, pp[1] - 4))

    screen_nodes = {}
    for n in nodes:
        pos = visible_pos(n["dx"], n["dy"])
        if pos is None:
            continue
        nx, ny = pos
        if nx < 14 or nx > w - 14 or ny < 14 or ny > h - 16:
            continue
        if hide_nodes_under_rect:
            rx0, ry0, rx1, ry1 = hide_nodes_under_rect
            if rx0 - 11 <= nx <= rx1 + 11 and ry0 - 11 <= ny <= ry1 + 11:
                continue

        screen_nodes[n["id"]] = (nx, ny, n)
        tile = NODE_TILES[n["elem"]]
        tx, ty = nx - 11, ny - 11
        canvas.alpha_composite(tile, (tx, ty))

        icon_alpha = 0.35 if n["state"] == "locked" else 1.0
        ic = load_icon_16(n["icon"], alpha_scale=icon_alpha)
        canvas.alpha_composite(ic, (nx - 8, ny - 9))

        if n["state"] == "locked":
            canvas.alpha_composite(BADGE_LOCK, (tx + 14, ty + 12))

        if mode == "study" and n["id"] == "fire_detonate":
            if breath > 0.02:
                ga = int(45 + breath * 65)
                glow_box = Image.new("RGBA", (30, 30), (0, 0, 0, 0))
                gd = ImageDraw.Draw(glow_box)
                gd.rectangle([0, 0, 29, 29], outline=(194, 96, 255, ga), width=2)
                canvas.alpha_composite(glow_box, (nx - 15, ny - 15))
            canvas.alpha_composite(FRAME_STUDYABLE, (nx - 13, ny - 13))
            canvas.alpha_composite(BADGE_SCROLL, (nx + 5, ny - 15))
        elif mode == "imprint" and n["id"] == "magic_ignite":
            canvas.alpha_composite(FRAME_SELECTED, (nx - 13, ny - 13))
        elif n["state"] == "studyable" and mode == "imprint":
            canvas.alpha_composite(FRAME_STUDYABLE, (nx - 13, ny - 13))
            canvas.alpha_composite(BADGE_SCROLL, (nx + 5, ny - 15))

    ax, ay = hub_cx - 92, hub_cy - 96
    altar_frame = ALTAR_STRIP.crop((0, glow_frame * 192, 184, (glow_frame + 1) * 192))
    canvas.alpha_composite(altar_frame, (ax, ay))

    slot0_xy = (ax + 64, ay + 74)
    slot1_xy = (ax + 104, ay + 74)
    btn_xy = (ax + 62, ay + 96)

    if mode == "study":
        canvas.alpha_composite(load_icon_16("item/embryo_fire.png"), slot0_xy)
        canvas.alpha_composite(BADGE_CHECK, (slot0_xy[0] + 10, slot0_xy[1] + 10))
        canvas.alpha_composite(PLUS_ACTIVE, (ax + 85, ay + 75))
        canvas.alpha_composite(SLIP_MYSTIC, slot1_xy)
        canvas.alpha_composite(CONFIRM_BTNS.crop((0, 20, 60, 40)), btn_xy)
    else:
        canvas.alpha_composite(load_icon_16("item/embryo_wood.png"), slot0_xy)
        canvas.alpha_composite(ARROW_ACTIVE, (ax + 84, ay + 76))
        canvas.alpha_composite(GHOST_AURA, slot1_xy)
        canvas.alpha_composite(load_icon_16("item/skill_attack_fire.png", alpha_scale=0.58), slot1_xy)
        canvas.alpha_composite(CONFIRM_BTNS.crop((0, 20, 60, 40)), btn_xy)

    return canvas, screen_nodes


def build_fullscreen_study_collapsed():
    W, H, SCALE = 480, 270, 4
    hub_cx, hub_cy = 236, 136
    c1x, snodes = render_tree_and_altar_1x(W, H, hub_cx, hub_cy, mode="study", glow_frame=2, breath=0.5)

    hi = nearest_scale(c1x, SCALE)

    for nid, (nx, ny, n) in snodes.items():
        if n.get("hide_label") or nx < 24 or nx > W - 24:
            continue
        col = (145, 150, 170, 235) if n["state"] == "locked" else (244, 246, 255, 255)
        if nid == "fire_detonate":
            col = (255, 236, 168, 255)
        render_pixel_text(hi, n["label"], nx * SCALE, (ny + 13) * SCALE, font=FONT_12, scale=2,
                          fill=col, align="center")

    ax, ay = hub_cx - 92, hub_cy - 96
    render_pixel_text(hi, "参悟 · 通玄", (ax + 92) * SCALE, (ay + 61) * SCALE, font=FONT_12, scale=2,
                      fill=(248, 242, 255, 255), align="center")
    render_pixel_text(hi, "1/1", (ax + 121) * SCALE, (ay + 84) * SCALE, font=FONT_12, scale=2,
                      fill=(128, 255, 158, 255), align="right")
    render_pixel_text(hi, "参悟", (ax + 92) * SCALE, (ay + 101) * SCALE, font=FONT_16, scale=2,
                      fill=(255, 248, 255, 255), align="center")

    fg1x = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    btn_normal = CORNER_BTNS.crop((0, 0, 52, 18))
    fg1x.alpha_composite(btn_normal, (362, 8))
    fg1x.alpha_composite(btn_normal, (420, 8))

    det_x, det_y, _ = snodes["fire_detonate"]
    tt_x, tt_y, tt_w, tt_h = det_x + 16, det_y - 6, 108, 44
    if tt_x + tt_w > W - 6:
        tt_x = W - 6 - tt_w
    ImageDraw.Draw(fg1x).rectangle([tt_x + 1, tt_y + 1, tt_x + tt_w - 2, tt_y + tt_h - 2], fill=(14, 11, 26, 248))
    draw_9slice(fg1x, TOOLTIP_9, tt_x, tt_y, tt_w, tt_h, c=8)
    draw_9slice(fg1x, BORDER_9, 0, 0, W, H, c=8)

    hi.alpha_composite(nearest_scale(fg1x, SCALE), (0, 0))

    render_pixel_text(hi, "归位", (362 + 26) * SCALE, (8 + 5) * SCALE, font=FONT_12, scale=2,
                      fill=(228, 224, 245, 255), align="center")
    render_pixel_text(hi, "背包", (420 + 26) * SCALE, (8 + 5) * SCALE, font=FONT_12, scale=2,
                      fill=(228, 224, 245, 255), align="center")

    render_pixel_text(hi, "引爆 【通玄·可参悟】", (tt_x + 6) * SCALE, (tt_y + 6) * SCALE,
                      font=FONT_12, scale=2, fill=(255, 226, 132, 255))
    tw_line2, _ = render_pixel_text(hi, "通用·火 | 前置:种火/引燃", (tt_x + 6) * SCALE, (tt_y + 18) * SCALE,
                      font=FONT_12, scale=2, fill=(214, 236, 255, 255))
    hi.alpha_composite(nearest_scale(BADGE_CHECK, 3), ((tt_x + 6) * SCALE + tw_line2 + 6, (tt_y + 18) * SCALE + 2))
    render_pixel_text(hi, "消耗:火原胚+通玄残简 1/1", (tt_x + 6) * SCALE, (tt_y + 30) * SCALE,
                      font=FONT_12, scale=2, fill=(232, 198, 255, 255))

    out_path = os.path.join(PREV_DIR, "preview_study_collapsed_4x.png")
    hi.save(out_path)
    print("Saved:", out_path)
    return hi


def populate_inventory_drawer(drawer_base):
    im = drawer_base.copy()
    inv_items = [
        (0, 0, load_icon_16("item/embryo_wood.png"), "8"),
        (0, 1, load_icon_16("item/embryo_water.png"), "5"),
        (0, 2, load_icon_16("item/embryo_fire.png"), "6"),
        (0, 3, load_icon_16("item/embryo_earth.png"), "4"),
        (0, 4, load_icon_16("item/embryo_metal.png"), "3"),
        (0, 5, load_icon_16("item/embryo_none.png"), "9"),
        (0, 6, SLIP_MORTAL, "3"),
        (0, 7, SLIP_MYSTIC, "2"),
        (0, 8, SLIP_TRANSC, "1"),
        (1, 0, load_icon_16("item/skill_attack_fire.png"), None),
        (1, 1, load_icon_16("item/skill_attack_water.png"), None),
        (1, 2, load_icon_16("item/skill_attack_wood.png"), None),
        (1, 3, load_icon_16("item/skill_attack_earth.png"), None),
        (1, 4, load_icon_16("item/skill_attack_metal.png"), None),
        (1, 5, load_icon_16("item/skill_attack_none.png"), None),
        (1, 6, load_icon_16("item/shu_embryo.png"), "12"),
        (2, 0, load_icon_16("item/skill_sword.png"), None),
        (2, 1, load_icon_16("item/skill_magic.png"), None),
        (2, 2, load_icon_16("item/ore_sousuo.png"), "16"),
        (2, 3, load_icon_16("item/shu_raw.png"), "7"),
        (2, 4, load_icon_16("item/shu_canzha.png"), "4"),
    ]
    hotbar_items = [
        (0, load_icon_16("item/skill_sword.png"), None),
        (1, load_icon_16("item/skill_magic.png"), None),
        (2, load_icon_16("item/skill_attack_fire.png"), None),
        (3, load_icon_16("item/skill_attack_water.png"), None),
        (5, load_icon_16("item/shu_food.png"), "16"),
        (7, SLIP_MYSTIC, "1"),
        (8, load_icon_16("item/embryo_wood.png"), "4"),
    ]
    counts = []
    for r, c, icon, cnt in inv_items:
        sx, sy = 9 + c * 18, 15 + r * 18
        im.alpha_composite(icon, (sx, sy))
        if cnt:
            counts.append((sx + 16, sy + 9, cnt))
    for c, icon, cnt in hotbar_items:
        sx, sy = 9 + c * 18, 73
        im.alpha_composite(icon, (sx, sy))
        if cnt:
            counts.append((sx + 16, sy + 9, cnt))
    return im, counts


def build_fullscreen_imprint_open_inv():
    W, H, SCALE = 480, 270, 4
    hub_cx, hub_cy = 204, 136
    drawer_x, drawer_y = 296, 28
    drawer_x1, drawer_y1 = drawer_x + INV_DRAWER.width, drawer_y + INV_DRAWER.height

    c1x, snodes = render_tree_and_altar_1x(
        W, H, hub_cx, hub_cy, mode="imprint", glow_frame=3, breath=0.65
    )

    hi = nearest_scale(c1x, SCALE)

    for nid, (nx, ny, n) in snodes.items():
        if n.get("hide_label") or nx < 24 or nx > W - 24:
            continue
        # Avoid half-clipped background labels peeking right at the drawer bottom bezel
        if (drawer_x - 12 <= nx <= drawer_x1 + 12) and (drawer_y - 4 <= ny + 16 <= drawer_y1 + 6):
            continue
        col = (145, 150, 170, 235) if n["state"] == "locked" else (244, 246, 255, 255)
        if nid == "magic_ignite":
            col = (255, 228, 128, 255)
        render_pixel_text(hi, n["label"], nx * SCALE, (ny + 13) * SCALE, font=FONT_12, scale=2,
                          fill=col, align="center")

    ax, ay = hub_cx - 92, hub_cy - 96
    render_pixel_text(hi, "原胚刻印", (ax + 92) * SCALE, (ay + 61) * SCALE, font=FONT_12, scale=2,
                      fill=(248, 242, 255, 255), align="center")
    render_pixel_text(hi, "刻印", (ax + 92) * SCALE, (ay + 101) * SCALE, font=FONT_16, scale=2,
                      fill=(255, 248, 255, 255), align="center")

    fg1x = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    btn_normal = CORNER_BTNS.crop((0, 0, 52, 18))
    btn_active = CORNER_BTNS.crop((0, 36, 52, 54))
    fg1x.alpha_composite(btn_normal, (362, 7))
    fg1x.alpha_composite(btn_active, (420, 7))

    pop_drawer, inv_counts = populate_inventory_drawer(INV_DRAWER)
    fg1x.alpha_composite(pop_drawer, (drawer_x, drawer_y))

    ig_x, ig_y, _ = snodes["magic_ignite"]
    tt_x, tt_y, tt_w, tt_h = ig_x + 15, ig_y - 6, 152, 46
    if tt_x + tt_w > W - 6:
        tt_x = W - 6 - tt_w
    ImageDraw.Draw(fg1x).rectangle([tt_x + 1, tt_y + 1, tt_x + tt_w - 2, tt_y + tt_h - 2], fill=(14, 11, 26, 248))
    draw_9slice(fg1x, TOOLTIP_9, tt_x, tt_y, tt_w, tt_h, c=8)
    mini_fire = load_icon_16("gui/technique/other_fire.png").resize((10, 10), Image.Resampling.NEAREST)
    fg1x.alpha_composite(mini_fire, (tt_x + 6, tt_y + 6))
    draw_9slice(fg1x, BORDER_9, 0, 0, W, H, c=8)

    hi.alpha_composite(nearest_scale(fg1x, SCALE), (0, 0))

    render_pixel_text(hi, "归位", (362 + 26) * SCALE, (7 + 5) * SCALE, font=FONT_12, scale=2,
                      fill=(228, 224, 245, 255), align="center")
    render_pixel_text(hi, "收起背包", (420 + 26) * SCALE, (7 + 5) * SCALE, font=FONT_12, scale=2,
                      fill=(255, 244, 255, 255), align="center")

    for cx_r, cy_t, cnt_str in inv_counts:
        render_pixel_text(hi, cnt_str, (drawer_x + cx_r) * SCALE, (drawer_y + cy_t) * SCALE,
                          font=FONT_12, scale=2, fill=(248, 248, 255, 255), align="right")

    render_pixel_text(hi, "点火 [已学会]", (tt_x + 19) * SCALE, (tt_y + 6) * SCALE,
                      font=FONT_12, scale=2, fill=(255, 224, 122, 255))
    render_pixel_text(hi, "附魔 · 火 | 伤害 2  冷却 40t", (tt_x + 6) * SCALE, (tt_y + 19) * SCALE,
                      font=FONT_12, scale=2, fill=(224, 228, 245, 255))
    render_pixel_text(hi, "木生火：范围 +10% · 燃烧时间 +10%", (tt_x + 6) * SCALE, (tt_y + 31) * SCALE,
                      font=FONT_12, scale=2, fill=(138, 255, 166, 255))

    out_path = os.path.join(PREV_DIR, "preview_imprint_open_inv_4x.png")
    hi.save(out_path)
    print("Saved:", out_path)
    return hi


def build_populated_altar_card(mode="study"):
    im = ALTAR_STRIP.crop((0, 3 * 192, 184, 4 * 192)).copy()
    slot0_xy = (64, 74)
    slot1_xy = (104, 74)
    btn_xy = (62, 96)
    if mode == "study":
        im.alpha_composite(load_icon_16("item/embryo_fire.png"), slot0_xy)
        im.alpha_composite(BADGE_CHECK, (slot0_xy[0] + 10, slot0_xy[1] + 10))
        im.alpha_composite(PLUS_ACTIVE, (85, 75))
        im.alpha_composite(SLIP_MYSTIC, slot1_xy)
        im.alpha_composite(CONFIRM_BTNS.crop((0, 20, 60, 40)), btn_xy)
    else:
        im.alpha_composite(load_icon_16("item/embryo_wood.png"), slot0_xy)
        im.alpha_composite(ARROW_ACTIVE, (84, 76))
        im.alpha_composite(GHOST_AURA, slot1_xy)
        im.alpha_composite(load_icon_16("item/skill_attack_fire.png", alpha_scale=0.58), slot1_xy)
        im.alpha_composite(CONFIRM_BTNS.crop((0, 20, 60, 40)), btn_xy)
    return im


def build_gate_comparison_sheet():
    W, H = 1920, 1080
    board = Image.new("RGBA", (W, H), (10, 12, 22, 255))
    bd = ImageDraw.Draw(board)

    cols = [(18, 52, 578, 1062), (594, 52, 1306, 1062), (1322, 52, 1902, 1062)]
    for x0, y0, x1, y1 in cols:
        bd.rectangle([x0, y0, x1, y1], fill=(16, 19, 34, 255), outline=(66, 56, 104, 255), width=2)

    render_pixel_text(board, "术式刻印台 UI v1 — 1:1 门禁对照图（目标参考 vs Aseprite 1x 原生像素组件最近邻放大）",
                      960, 14, font=FONT_14, scale=2, fill=(242, 232, 255, 255), align="center")

    render_pixel_text(board, "① 用户指定目标视觉基准 (参考图裁切)", 298, 64, font=FONT_12, scale=2,
                      fill=(212, 188, 255, 255), align="center")
    ref_target = load_rgba(os.path.join(REF_DIR, "user_target_ref_altar_tree.jpg"))
    tw, th = ref_target.size
    crop_t = ref_target.crop((int(tw * 0.25), int(th * 0.20), int(tw * 0.75), int(th * 0.82))).resize(
        (524, 448), Image.Resampling.LANCZOS
    )
    board.alpha_composite(crop_t, (36, 98))

    render_pixel_text(board, "concept_E 参悟态祭坛与可参悟节点基准", 298, 560, font=FONT_12, scale=2,
                      fill=(212, 188, 255, 255), align="center")
    ref_e = load_rgba(os.path.join(REF_DIR, "concept_E_altar_study_hidden_inv.png"))
    ew, eh = ref_e.size
    crop_e = ref_e.crop((int(ew * 0.33), int(eh * 0.22), int(ew * 0.78), int(eh * 0.79))).resize(
        (524, 448), Image.Resampling.LANCZOS
    )
    board.alpha_composite(crop_e, (36, 594))

    render_pixel_text(board, "② 本版 Aseprite 手绘像素祭坛 (2x) + 节点砖 (3x) + 三档研习简 (5x)", 950, 64,
                      font=FONT_12, scale=2, fill=(255, 232, 162, 255), align="center")

    a_study_2x = nearest_scale(build_populated_altar_card("study"), 2)
    a_impr_2x = nearest_scale(build_populated_altar_card("imprint"), 2)
    board.alpha_composite(a_study_2x, (604, 108))
    board.alpha_composite(a_impr_2x, (932, 108))

    render_pixel_text(board, "参悟 · 通玄", 604 + 92 * 2, 108 + 61 * 2, font=FONT_12, scale=1,
                      fill=(248, 242, 255, 255), align="center")
    render_pixel_text(board, "1/1", 604 + 120 * 2, 108 + 84 * 2, font=FONT_12, scale=1,
                      fill=(128, 255, 158, 255), align="right")
    render_pixel_text(board, "参悟", 604 + 92 * 2, 108 + 100 * 2, font=FONT_16, scale=1,
                      fill=(255, 248, 255, 255), align="center")

    render_pixel_text(board, "原胚刻印", 932 + 92 * 2, 108 + 61 * 2, font=FONT_12, scale=1,
                      fill=(248, 242, 255, 255), align="center")
    render_pixel_text(board, "刻印", 932 + 92 * 2, 108 + 100 * 2, font=FONT_16, scale=1,
                      fill=(255, 248, 255, 255), align="center")

    render_pixel_text(board, "参悟态 (184×192 @ 2x)", 604 + 184, 502, font=FONT_12, scale=2,
                      fill=(198, 214, 245, 255), align="center")
    render_pixel_text(board, "刻印态 (184×192 @ 2x)", 932 + 184, 502, font=FONT_12, scale=2,
                      fill=(198, 214, 245, 255), align="center")

    render_pixel_text(board, "立体倒角节点石砖 (22×22) 与选中/可参悟/悬停外框 (26×26) @ 3x：", 618, 542,
                      font=FONT_12, scale=2, fill=(224, 212, 255, 255))
    row0_3x = nearest_scale(TREE_SHEET.crop((0, 0, 192, 24)), 3)
    row1_3x = nearest_scale(TREE_SHEET.crop((0, 26, 130, 54)), 3)
    board.alpha_composite(row0_3x, (618, 574))
    tile_names = ["木", "火", "水", "土", "金", "无", "规则", "未解锁"]
    for idx, nm in enumerate(tile_names):
        render_pixel_text(board, nm, 618 + idx * 72 + 33, 646, font=FONT_12, scale=2,
                          fill=(210, 218, 240, 255), align="center")
    board.alpha_composite(row1_3x, (618, 682))
    render_pixel_text(board, "选中框 / 可参悟框 / 悬停框 / 竹简角标 / 锁头 / 节点钉", 618, 768,
                      font=FONT_12, scale=2, fill=(188, 198, 226, 255))

    render_pixel_text(board, "三档研习简专属 16×16 像素图标 @ 5x：", 618, 814,
                      font=FONT_12, scale=2, fill=(224, 212, 255, 255))
    slips_info = [
        (SLIP_MORTAL, "凡胎研习简"),
        (SLIP_MYSTIC, "通玄残简"),
        (SLIP_TRANSC, "化境孤本"),
    ]
    for idx, (sim, sname) in enumerate(slips_info):
        sx = 650 + idx * 210
        board.alpha_composite(nearest_scale(sim, 5), (sx, 850))
        render_pixel_text(board, sname, sx + 40, 940, font=FONT_12, scale=2,
                          fill=(242, 236, 255, 255), align="center")

    render_pixel_text(board, "③ 隐藏背包面板与三态按钮 @ 3x", 1612, 64,
                      font=FONT_12, scale=2, fill=(180, 245, 210, 255), align="center")

    pop_drw, _ = populate_inventory_drawer(INV_DRAWER)
    drw_3x = nearest_scale(pop_drw, 3)
    board.alpha_composite(drw_3x, (1345, 106))
    render_pixel_text(board, "inventory_drawer.png (178×104 · 3×9+1×9)", 1612, 428,
                      font=FONT_12, scale=2, fill=(198, 214, 245, 255), align="center")

    render_pixel_text(board, "主操作钮 (60×20×3态)", 1455, 480, font=FONT_12, scale=2,
                      fill=(224, 212, 255, 255), align="center")
    cbtn_3x = nearest_scale(CONFIRM_BTNS, 3)
    board.alpha_composite(cbtn_3x, (1365, 516))
    for i, lbl in enumerate(["常态", "悬停", "禁用"]):
        col = (130, 128, 148, 255) if i == 2 else (255, 248, 255, 255)
        render_pixel_text(board, lbl, 1365 + 90, 516 + i * 60 + 16, font=FONT_12, scale=2,
                          fill=col, align="center")

    render_pixel_text(board, "右上角钮 (52×18×3态)", 1745, 480, font=FONT_12, scale=2,
                      fill=(224, 212, 255, 255), align="center")
    kbtn_3x = nearest_scale(CORNER_BTNS, 3)
    board.alpha_composite(kbtn_3x, (1667, 516))
    for i, lbl in enumerate(["背包", "背包", "收起背包"]):
        render_pixel_text(board, lbl, 1667 + 78, 516 + i * 54 + 14, font=FONT_12, scale=2,
                          fill=(252, 244, 255, 255), align="center")

    render_pixel_text(board, "连接符 · 状态角标 · 9-Slice 边框 (@4x)", 1612, 738,
                      font=FONT_12, scale=2, fill=(224, 212, 255, 255), align="center")
    board.alpha_composite(nearest_scale(ALTAR_WIDGETS, 4), (1352, 780))
    board.alpha_composite(nearest_scale(BORDER_9, 4), (1650, 788))
    board.alpha_composite(nearest_scale(TOOLTIP_9, 4), (1772, 788))
    render_pixel_text(board, "钢石外框", 1698, 896, font=FONT_12, scale=2,
                      fill=(198, 214, 245, 255), align="center")
    render_pixel_text(board, "紫晶提示框", 1820, 896, font=FONT_12, scale=2,
                      fill=(226, 192, 255, 255), align="center")

    out_path = os.path.join(PREV_DIR, "ref_vs_v1_altar_gate.png")
    board.save(out_path)
    print("Saved:", out_path)
    return board


def build_subtle_glow_gif():
    LW, LH, SCALE = 280, 236, 3
    hub_cx, hub_cy = LW // 2, LH // 2 + 4
    frames_rgb = []

    for f in range(8):
        phase = (f / 8.0) * math.pi * 2.0
        breath = (1.0 - math.cos(phase)) * 0.5
        c1x, snodes = render_tree_and_altar_1x(
            LW, LH, hub_cx, hub_cy, mode="study", glow_frame=f, breath=breath
        )
        draw_9slice(c1x, BORDER_9, 0, 0, LW, LH, c=8)
        hi = nearest_scale(c1x, SCALE)

        for nid, (nx, ny, n) in snodes.items():
            if n.get("hide_label") or ny + 18 >= LH - 6:
                continue
            col = (255, 236, 168, 255) if nid == "fire_detonate" else (240, 242, 255, 255)
            render_pixel_text(hi, n["label"], nx * SCALE, (ny + 13) * SCALE, font=FONT_12, scale=1,
                              fill=col, align="center")

        ax, ay = hub_cx - 92, hub_cy - 96
        render_pixel_text(hi, "参悟 · 通玄", (ax + 92) * SCALE, (ay + 62) * SCALE, font=FONT_12, scale=1,
                          fill=(248, 242, 255, 255), align="center")
        render_pixel_text(hi, "1/1", (ax + 120) * SCALE, (ay + 85) * SCALE, font=FONT_12, scale=1,
                          fill=(128, 255, 158, 255), align="right")
        render_pixel_text(hi, "参悟", (ax + 92) * SCALE, (ay + 101) * SCALE, font=FONT_16, scale=1,
                          fill=(255, 248, 255, 255), align="center")

        frames_rgb.append(
            hi.convert("RGB").quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
        )

    out_path = os.path.join(PREV_DIR, "altar_subtle_glow_3x.gif")
    frames_rgb[0].save(
        out_path,
        save_all=True,
        append_images=frames_rgb[1:],
        duration=120,
        loop=0,
        optimize=False,
    )
    print("Saved:", out_path)


if __name__ == "__main__":
    build_gate_comparison_sheet()
    build_fullscreen_study_collapsed()
    build_fullscreen_imprint_open_inv()
    build_subtle_glow_gif()
    print("ALL_4_PREVIEWS_BUILT_OK")
