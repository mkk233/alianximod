"""Check every opaque pixel of the runtime strips falls inside a body window."""
from PIL import Image

ROOT = r"D:/alianximod/src/main/resources/assets/xueximod/textures/effect/furnace_v2"
FRAME = 112

GROUPS = {
    "heat_rear": (["heat_rear.png", "heat_entry_rear.png"], [(22, 38, 73, 95)]),
    "heat_rear_tongues": (
        ["heat_rear_tongues.png", "heat_entry_rear_tongues.png"],
        [(25, 49, 34, 71), (62, 49, 71, 71), (25, 72, 47, 98), (48, 72, 71, 98)],
    ),
    "heat_front": (
        ["heat_front_tongues.png", "heat_entry_front_tongues.png"],
        [(28, 46, 35, 70), (61, 46, 67, 70), (36, 42, 60, 93)],
    ),
    "heat_embers": (["heat_embers.png", "heat_entry_embers.png"], [(23, 31, 72, 96)]),
    "burn_cape": (["burn_cape.png", "burn_entry_cape.png"], [(24, 39, 77, 103)]),
    "burn_rear": (["burn_rear_fire.png", "burn_entry_rear_fire.png"], [(32, 40, 65, 58)]),
    "burn_body": (
        ["burn_body_fire.png", "burn_entry_body_fire.png"],
        [(36, 42, 59, 77), (36, 78, 46, 94), (47, 78, 59, 94)],
    ),
    "burn_hand": (
        ["burn_hand_fire.png", "burn_entry_hand_fire.png"],
        [(22, 45, 40, 72), (56, 45, 74, 72)],
    ),
    "burn_embers": (["burn_embers.png", "burn_entry_embers.png"], [(19, 25, 79, 98)]),
}


def covered(pixel_x, pixel_y, windows):
    for left, top, right, bottom in windows:
        if left <= pixel_x <= right and top <= pixel_y <= bottom:
            return True
    return False


def frame_alpha(path, frame_index):
    image = Image.open(ROOT + "/" + path).convert("RGBA")
    pixels = image.load()
    total = 0
    y_base = frame_index * FRAME
    for pixel_y in range(FRAME):
        for pixel_x in range(image.size[0]):
            total += pixels[pixel_x, y_base + pixel_y][3]
    return total


failed = False
for label, (paths, windows) in GROUPS.items():
    missed = 0
    sample = None
    for path in paths:
        image = Image.open(ROOT + "/" + path).convert("RGBA")
        pixels = image.load()
        frames = image.size[1] // FRAME
        for frame_index in range(frames):
            y_base = frame_index * FRAME
            for pixel_y in range(FRAME):
                for pixel_x in range(image.size[0]):
                    if pixels[pixel_x, y_base + pixel_y][3] > 0 and not covered(pixel_x, pixel_y, windows):
                        missed += 1
                        if sample is None:
                            sample = (path, frame_index, pixel_x, pixel_y)
        first = frame_alpha(path, 0)
        last = frame_alpha(path, frames - 1)
        print(f"{path} frames {frames} first_alpha {first} last_alpha {last}")
    if missed:
        failed = True
        print(f"MISS {label} {missed} sample {sample}")
    else:
        print(f"COVERED {label}")

raise SystemExit(1 if failed else 0)
