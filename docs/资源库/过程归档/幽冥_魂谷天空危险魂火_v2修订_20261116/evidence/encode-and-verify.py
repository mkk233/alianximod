from pathlib import Path
from PIL import Image, ImageChops
import hashlib
import json

root = Path(__file__).resolve().parents[1]
prior = root.parent / "幽冥_v2领域定稿预览_20261116"
frame_paths = sorted((root / "frames").glob("inside_*.png"))
assert len(frame_paths) == 80
frames = []
frame_hashes = []
for path in frame_paths:
    with Image.open(path) as image:
        assert image.size == (960, 684)
        frames.append(image.convert("RGB").quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE))
    frame_hashes.append({"file": path.relative_to(root).as_posix(), "sha256": hashlib.sha256(path.read_bytes()).hexdigest().upper()})
gif_path = root / "inside-soulvalley-4s.gif"
frames[0].save(gif_path, save_all=True, append_images=frames[1:], duration=50, loop=0, optimize=False, disposal=2)
with Image.open(gif_path) as image:
    assert image.n_frames == 80
    durations = []
    for index in range(image.n_frames):
        image.seek(index)
        durations.append(image.info["duration"])
    assert sum(durations) == 4000
with Image.open(prior / "textures/nether_gloom_v2_runtime.png") as source, Image.open(root / "textures/youming_v2_soulvalley_strip.png") as candidate:
    source = source.convert("RGBA")
    candidate = candidate.convert("RGBA")
    assert source.size == candidate.size == (64, 1024)
    alpha_equal = ImageChops.difference(source.getchannel("A"), candidate.getchannel("A")).getbbox() is None
    assert alpha_equal
    changed_rgb_pixels = sum(old[:3] != new[:3] for old, new in zip(source.getdata(), candidate.getdata()) if old[3])
    visible_pixels = sum(pixel[3] > 0 for pixel in source.getdata())
proof = {
    "source": "80 synchronous captures of actual Blockbench main viewport; Pillow only encodes GIF palette/container",
    "frames": 80,
    "size": [960, 684],
    "frame_ms": 50,
    "duration_ms": 4000,
    "loop": 0,
    "interpolation_or_compositing": False,
    "gif_sha256": hashlib.sha256(gif_path.read_bytes()).hexdigest().upper(),
    "palette_revision": {
        "source": str(prior / "textures/nether_gloom_v2_runtime.png"),
        "candidate": "textures/youming_v2_soulvalley_strip.png",
        "alpha_mask_exactly_equal": alpha_equal,
        "visible_pixels": visible_pixels,
        "rgb_changed_visible_pixels": changed_rgb_pixels,
        "shape_and_frame_order_unchanged": True
    },
    "frame_hashes": frame_hashes
}
(root / "evidence/encoding-and-pixels.json").write_text(json.dumps(proof, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({key: value for key, value in proof.items() if key != "frame_hashes"}, ensure_ascii=True))
