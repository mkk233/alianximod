from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json
import hashlib
import sys

ROOT = Path(__file__).resolve().parents[1]
FONT = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 18)
board = Image.new("RGB", (1280, 364), "#202b36")
draw = ImageDraw.Draw(board)
draw.text((20, 12), "DETONATE v2-3D | INDIVIDUAL PUFF / NOT THE ENTIRE EXPLOSION", font=FONT, fill="#f0ead7")
for row, variant in enumerate(["orange", "soul"]):
    for index in range(16):
        frame = Image.open(ROOT / f"frames/{variant}/puff_{index+1:02d}.png").convert("RGBA")
        scaled = frame.resize((64, 64), Image.Resampling.NEAREST)
        board.paste(scaled, (index * 80 + 8, 78 + row * 145), scaled)
        draw.text((index * 80 + 8, 151 + row * 145), f"{index*50}ms", font=FONT, fill="#b7c8d4")
    draw.text((16, 48 + row * 145), variant.upper(), font=FONT, fill="#dfebef")
board.save(ROOT / "previews/puff_keyframes.png")
if "--initial" in sys.argv:
    print("2D board ready")
    raise SystemExit
crop_records = {}
for mode in ["main", "side", "strength", "soul"]:
    paths = sorted((ROOT / f"previews/{mode}").glob("frame_*.png"))
    if paths:
        originals = [Image.open(path).convert("RGBA") for path in paths]
        bounds = [image.getchannel("A").getbbox() for image in originals]
        crop = (min(box[0] for box in bounds), min(box[1] for box in bounds), max(box[2] for box in bounds), max(box[3] for box in bounds))
        crop_records[mode] = {"source": "actual native GUI PNG frames", "fixed_pixel_crop_all_frames": crop, "repaint": False}
        frames = [image.crop(crop).convert("RGB") for image in originals]
        frames[0].save(ROOT / f"previews/detonate_v2_3d_{mode}.gif", save_all=True, append_images=frames[1:], duration=[50] * (len(frames)-1)+[700], loop=0, disposal=2, optimize=False)
        if mode == "strength":
            frames[6].save(ROOT / "previews/strength_comparison_detail.png")
        panel = Image.new("RGB", (1440, 650), "#202b36")
        paneldraw = ImageDraw.Draw(panel)
        for column, number in enumerate([1, 4, 7, 10, 14, 18]):
            image = frames[number-1]
            image.thumbnail((480, 285), Image.Resampling.LANCZOS)
            xcoord, ycoord = (column % 3) * 480, (column // 3) * 325
            panel.paste(image, (xcoord + (480-image.width)//2, ycoord+28))
            paneldraw.text((xcoord+14, ycoord+8), f"Native GUI / {number:02d} / {(number-1)*50}ms", font=FONT, fill="white")
        panel.save(ROOT / f"previews/{mode}_temporal_contact.png")
(ROOT / "evidence/native-gif-encoding.json").write_text(json.dumps(crop_records, ensure_ascii=False, indent=2), encoding="utf-8")
print("Native captures encoded; no rendered pixels repainted")
