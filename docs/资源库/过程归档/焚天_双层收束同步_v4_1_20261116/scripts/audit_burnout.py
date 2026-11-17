import base64
import hashlib
import io
import json
from pathlib import Path

from PIL import Image

root = Path("D:/alianximod/docs/资源库/过程归档/焚天_双层收束同步_v4_1_20261116")
archive = Path("D:/alianximod/docs/资源库/特效/火焰/焚天_双层叠加_v4")
old_model_path = Path("D:/alianximod/docs/资源库/过程归档/焚天_双层叠加_v4_20261116/blockbench/fentian_dual_layer_v4.bbmodel")
receipt = Path("D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/fentian-burnout-sync-art-20261116.md")
expected = {
    "background_fentian_v2-strip.png": "3AE6A95E41D92991B828EDC7491CBB40A18D503CDCF700281A50D901D6AA6A76",
    "background_fentian_v2.aseprite": "8A9E64C672466E5C1F764860307EF6E24E66A5545A71D725172DDD1BF52F6A11",
    "foreground_fentian_v3-strip.png": "EF3476EB01225D2E7478F9BBA5430F28BCF2E1D12198F3F482A634C155A04070",
    "foreground_fentian_v3.aseprite": "3EC04641BBBC573BAAABCF088DA61B6B79B7418159C2D1F6F3287D5B8B06BB42",
}
for name, digest in expected.items():
    assert hashlib.sha256((archive / name).read_bytes()).hexdigest().upper() == digest
assert hashlib.sha256(old_model_path.read_bytes()).hexdigest().upper() == "7C320BBDCBCDB7B756384C34F961BFC927C733DA33E768073E4EEE4F79E46089"
old_model = json.loads(old_model_path.read_text(encoding="utf-8"))
model = json.loads((root / "blockbench/fentian_dual_layer_burnout_v4_1.bbmodel").read_text(encoding="utf-8"))
def geometry(document):
    return [{key: element.get(key) for key in ("uuid", "name", "from", "to", "origin", "rotation", "shade")} for element in document["elements"]]
assert geometry(old_model) == geometry(model)
assert old_model["outliner"] == model["outliner"]
with Image.open(root / "textures/background_burnout_9tick_preview.png") as image:
    alpha_sums = [sum(image.crop((0, index * 144, 128, (index + 1) * 144)).convert("RGBA").getchannel("A").getdata()) for index in range(9)]
    assert all(first > second for first, second in zip(alpha_sums, alpha_sums[1:]))
    assert alpha_sums[-1] == 0
with Image.open(root / "previews/burnout_9tick.gif") as animation:
    durations = []
    for index in range(animation.n_frames):
        animation.seek(index)
        durations.append(animation.info.get("duration"))
    assert animation.n_frames == 9 and set(durations) == {50}
embedded = []
for texture in model["textures"]:
    if texture["name"] == "aux_mannequin_palette.png":
        continue
    source_path = root / "textures" / texture["name"] if texture["name"].startswith("background_burnout") else archive / texture["name"]
    encoded = base64.b64decode(texture["source"].split(",", 1)[1])
    with Image.open(io.BytesIO(encoded)) as image, Image.open(source_path) as original:
        pixels = image.convert("RGBA").tobytes()
        source_pixels = original.convert("RGBA").tobytes()
        assert image.size == original.size
        deltas = [abs(first - second) for first, second in zip(pixels, source_pixels)]
        visible = [deltas[offset + channel] for offset in range(0, len(pixels), 4) if source_pixels[offset + 3] != 0 for channel in range(3)]
        embedded.append({"name": texture["name"], "same_rgba": pixels == source_pixels, "changed_channels": sum(delta != 0 for delta in deltas), "max_visible_rgb_delta": max(visible, default=0), "changed_alpha_pixels": sum(deltas[offset] != 0 for offset in range(3, len(deltas), 4))})
samples = []
for tick in (0, 3, 6, 8):
    with Image.open(root / f"previews/gui_burnout_t{tick}.png") as image:
        samples.append({"tick": tick, "background_sample_xy": [300, 420], "rgba": list(image.convert("RGBA").getpixel((300, 420)))})
print(json.dumps({"originals_unchanged": True, "old_model_unchanged": True, "element_geometry_and_outliner_equal": True, "alpha_sums": alpha_sums, "gif_frames": 9, "gif_frame_ms": 50, "embedded": embedded, "gui_alpha_samples": samples}, ensure_ascii=False))
paths = [path for path in root.rglob("*") if path.is_file() and path.name != "manifest.json"]
if receipt.exists():
    paths.append(receipt)
for path in sorted(paths):
    print(json.dumps({"path": path.as_posix(), "bytes": path.stat().st_size, "sha256": hashlib.sha256(path.read_bytes()).hexdigest().upper()}, ensure_ascii=False))
