import base64
import hashlib
import io
import json
from pathlib import Path

from PIL import Image

root = Path("D:/alianximod/docs/资源库/过程归档/焚天_双层叠加_v4_20261116")
archive = Path("D:/alianximod/docs/资源库/特效/火焰/焚天_双层叠加_v4")
receipt = Path("D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/fentian-dual-layer-art-20261116.md")
manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
for layer in manifest["layers"]:
    assert (archive / layer["archive_png"]).read_bytes() == Path(layer["source_png"]).read_bytes()
    assert (archive / layer["archive_aseprite"]).read_bytes() == Path(layer["source_aseprite"]).read_bytes()
model = json.loads((root / "blockbench/fentian_dual_layer_v4.bbmodel").read_text(encoding="utf-8"))
embedded = []
for texture in model["textures"]:
    original = root / "textures" / texture["name"] if texture["name"].startswith("aux_") else archive / texture["name"]
    encoded = base64.b64decode(texture["source"].split(",", 1)[1])
    with Image.open(io.BytesIO(encoded)) as decoded, Image.open(original) as source:
        pixels = decoded.convert("RGBA").tobytes()
        source_pixels = source.convert("RGBA").tobytes()
        assert decoded.size == source.size
        deltas = [abs(first - second) for first, second in zip(pixels, source_pixels)]
        visible = [deltas[offset + channel] for offset in range(0, len(pixels), 4) if source_pixels[offset + 3] != 0 for channel in range(3)]
        embedded.append({
            "name": texture["name"],
            "same_rgba": pixels == source_pixels,
            "changed_channels": sum(delta != 0 for delta in deltas),
            "max_delta": max(deltas),
            "changed_alpha_pixels": sum(deltas[offset] != 0 for offset in range(3, len(deltas), 4)),
            "changed_visible_rgb_channels": sum(delta != 0 for delta in visible),
            "max_visible_rgb_delta": max(visible, default=0),
            "embedded_png_sha256": hashlib.sha256(encoded).hexdigest().upper(),
        })
with Image.open(root / "previews/dual_layer_demo.gif") as animation:
    durations = []
    for index in range(animation.n_frames):
        animation.seek(index)
        durations.append(animation.info.get("duration"))
    print(json.dumps({"gif_frames": animation.n_frames, "gif_size": animation.size, "durations_ms": sorted(set(durations))}))
print(json.dumps({"embedded": embedded}, ensure_ascii=False))
files = [path for folder in (root, archive) for path in folder.rglob("*") if path.is_file() and path.name != "manifest.json"]
files.append(receipt)
for path in sorted(files):
    print(json.dumps({"path": path.as_posix(), "bytes": path.stat().st_size, "sha256": hashlib.sha256(path.read_bytes()).hexdigest().upper()}, ensure_ascii=False))
