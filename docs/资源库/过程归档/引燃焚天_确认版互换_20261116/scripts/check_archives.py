import base64
import hashlib
import io
import json
from pathlib import Path

from PIL import Image

root = Path("D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116")
kindle = Path("D:/alianximod/docs/资源库/特效/火焰/引燃_承接焚天_v4")
fentian = Path("D:/alianximod/docs/资源库/特效/火焰/焚天_全身烈焰_v3")
receipt = Path("D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/kindle-fentian-swap-art-20261116.md")
paths = [path for folder in (root, kindle, fentian) for path in folder.rglob("*") if path.is_file() and path.name != "manifest.json"]
if receipt.exists():
    paths.append(receipt)
entries = []
for path in sorted(paths):
    entry = {
        "path": path.as_posix(),
        "bytes": path.stat().st_size,
        "sha256": hashlib.sha256(path.read_bytes()).hexdigest().upper(),
    }
    if path.suffix.lower() == ".png":
        with Image.open(path) as image:
            entry["size"] = list(image.size)
            alpha_values = sorted(set(image.convert("RGBA").getchannel("A").getdata()))
            entry["alpha_binary"] = set(alpha_values).issubset({0, 255})
    entries.append(entry)
model = json.loads((root / "blockbench/kindle_fentian_approved_swap.bbmodel").read_text(encoding="utf-8"))
origins = {
    "aux_mannequin_palette.png": root / "textures/aux_mannequin_palette.png",
    "kindle_v4-strip.png": kindle / "kindle_v4-strip.png",
    "fentian_v3-strip.png": fentian / "fentian_v3-strip.png",
}
embedded = []
for texture in model["textures"]:
    encoded = base64.b64decode(texture["source"].split(",", 1)[1])
    with Image.open(io.BytesIO(encoded)) as loaded, Image.open(origins[texture["name"]]) as original:
        pixels = loaded.convert("RGBA").tobytes()
        source_pixels = original.convert("RGBA").tobytes()
        channel_deltas = [abs(first - second) for first, second in zip(pixels, source_pixels)]
        visible_deltas = [channel_deltas[offset + channel] for offset in range(0, len(pixels), 4) if source_pixels[offset + 3] != 0 for channel in range(3)]
        transparent_deltas = [channel_deltas[offset + channel] for offset in range(0, len(pixels), 4) if source_pixels[offset + 3] == 0 for channel in range(3)]
        embedded.append({
            "name": texture["name"],
            "embedded_png_sha256": hashlib.sha256(encoded).hexdigest().upper(),
            "rgba_sha256": hashlib.sha256(pixels).hexdigest().upper(),
            "source_rgba_sha256": hashlib.sha256(source_pixels).hexdigest().upper(),
            "same_rgba_pixels": pixels == source_pixels,
            "same_size": loaded.size == original.size,
            "changed_channels": sum(delta != 0 for delta in channel_deltas),
            "maximum_channel_delta": max(channel_deltas),
            "changed_alpha_pixels": sum(pixels[offset] != source_pixels[offset] for offset in range(3, len(pixels), 4)),
            "changed_visible_rgb_channels": sum(delta != 0 for delta in visible_deltas),
            "maximum_visible_rgb_delta": max(visible_deltas, default=0),
            "changed_transparent_rgb_channels": sum(delta != 0 for delta in transparent_deltas),
        })
pairs = [
    (Path("D:/alianximod/docs/资源库/特效/火焰/焚天_v2/fentian-conflagration-v2-strip.png"), kindle / "kindle_v4-strip.png"),
    (Path("D:/alianximod/docs/资源库/特效/火焰/焚天_v2/fentian-conflagration-v2.aseprite"), kindle / "kindle_v4.aseprite"),
    (Path("D:/alianximod/docs/资源库/过程归档/引燃_全身燃烧重绘_v3_20261116/textures/kindle_v3_strip.png"), fentian / "fentian_v3-strip.png"),
    (Path("D:/alianximod/docs/资源库/过程归档/引燃_全身燃烧重绘_v3_20261116/source/kindle_v3.aseprite"), fentian / "fentian_v3.aseprite"),
]
copies = [{"source": source.as_posix(), "target": target.as_posix(), "same_bytes": source.read_bytes() == target.read_bytes()} for source, target in pairs]
assert all(copy["same_bytes"] for copy in copies)
assert all(texture["same_size"] for texture in embedded)
print(json.dumps({"files": entries, "embedded": embedded, "copies": copies}, ensure_ascii=False, indent=2))
