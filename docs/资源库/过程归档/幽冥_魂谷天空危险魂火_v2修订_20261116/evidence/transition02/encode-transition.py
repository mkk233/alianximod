from pathlib import Path
from PIL import Image
import hashlib
import json

root = Path(__file__).resolve().parents[2]
paths = sorted((root / "frames/transition02").glob("tone_*.png"))
assert len(paths) == 76
frames = []
frame_hashes = []
for path in paths:
    with Image.open(path) as image:
        assert image.size == (960, 684)
        frames.append(image.convert("RGB").quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE))
    frame_hashes.append({"path": path.relative_to(root).as_posix(), "sha256": hashlib.sha256(path.read_bytes()).hexdigest().upper()})
output = root / "inside-tone-transition-3p8s.gif"
frames[0].save(output, save_all=True, append_images=frames[1:], duration=50, loop=0, optimize=False, disposal=2)
with Image.open(output) as image:
    assert image.n_frames == 76
    duration = 0
    for index in range(image.n_frames):
        image.seek(index)
        duration += image.info["duration"]
    assert duration == 3800
proof = {
    "source": "76 actual Blockbench main viewport captures, native material shader mixing before rendering",
    "encoder_only": "Pillow GIF palette/container encoding; no pixel filter, compositing, 3D rendering or interpolation",
    "frames": 76,
    "frame_ms": 50,
    "duration_ms": duration,
    "size": [960, 684],
    "gif_sha256": hashlib.sha256(output.read_bytes()).hexdigest().upper(),
    "frame_hashes": frame_hashes
}
(root / "evidence/transition02/encoding.json").write_text(json.dumps(proof, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({key: value for key, value in proof.items() if key != "frame_hashes"}))
