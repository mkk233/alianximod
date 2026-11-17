from pathlib import Path
from PIL import Image, ImageDraw
import json

ROOT = Path(__file__).resolve().parents[1]
frames = [Image.open(path).convert("RGBA") for path in sorted((ROOT / "exports/frames").glob("dust_*.png"))]
previews = []
stats = []
for number, frame in enumerate(frames):
    background = Image.new("RGBA", frame.size, (43, 47, 52, 255))
    background.alpha_composite(frame)
    previews.append(background.convert("RGB").resize((864, 288), Image.Resampling.NEAREST))
    alpha = frame.getchannel("A")
    stats.append({"frame": number + 1, "time": number * 0.05, "bbox": alpha.getbbox(), "alphaMax": alpha.getextrema()[1]})
previews[0].save(ROOT / "previews/ground_airborne_v2r2-2d.gif", save_all=True, append_images=previews[1:], duration=50, loop=0, disposal=2)
contact = Image.new("RGB", (576, 6 * 212), (43, 47, 52))
draw = ImageDraw.Draw(contact)
for row, number in enumerate([0, 3, 7, 12, 19, 23]):
    contact.paste(frames[number].convert("RGBA").resize((576, 192), Image.Resampling.NEAREST), (0, row * 212 + 20), frames[number].getchannel("A").resize((576, 192), Image.Resampling.NEAREST))
    draw.text((8, row * 212 + 4), f"frame {number + 1:02d} / {number * 0.05:.2f}s", fill=(225, 226, 228))
contact.save(ROOT / "previews/2d-keyframes.png")
(ROOT / "evidence/2d-frame-inspection.json").write_text(json.dumps(stats, indent=2), encoding="utf-8")
print(json.dumps({"frames": len(frames), "first": stats[0], "penultimate": stats[-2], "last": stats[-1]}))
