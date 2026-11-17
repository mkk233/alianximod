from pathlib import Path
from PIL import Image, ImageSequence
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
results = []
for view in ["overall", "side"]:
    paths = sorted((ROOT / f"previews/{view}-frames").glob("*.png"))
    frames = []
    for path in paths:
        native = Image.open(path).convert("RGBA")
        background = Image.new("RGBA", native.size, (38, 42, 47, 255))
        background.alpha_composite(native)
        frames.append(background.convert("RGB"))
    palette_sheet = Image.new("RGB", (550 * 6, 450))
    for column, index in enumerate([0, 10, 14, 18, 24, 32]):
        palette_sheet.paste(frames[index].resize((550, 450)), (column * 550, 0))
    palette = palette_sheet.quantize(colors=256)
    quantized = [frame.quantize(palette=palette, dither=Image.Dither.NONE) for frame in frames]
    output = ROOT / f"previews/earth_ground_v2r2-{view}-real-blockbench.gif"
    quantized[0].save(output, save_all=True, append_images=quantized[1:], duration=50, loop=0, optimize=False, disposal=1)
    with Image.open(output) as animation:
        decoded = [frame.convert("RGB").copy() for frame in ImageSequence.Iterator(animation)]
        durations = [frame.info.get("duration", 0) for frame in ImageSequence.Iterator(animation)]
        results.append({"file": str(output.relative_to(ROOT)), "dimensions": animation.size, "nativeSamples": len(paths), "encodedFrames": animation.n_frames, "uniqueDecodedFrames": len({hashlib.sha256(frame.tobytes()).hexdigest() for frame in decoded}), "durationMs": sum(durations), "frameDurationsMs": durations})
assert all(result["durationMs"] == 2300 and result["uniqueDecodedFrames"] > 20 for result in results)
(ROOT / "evidence/gif-verification.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
print(json.dumps(results, indent=2))
