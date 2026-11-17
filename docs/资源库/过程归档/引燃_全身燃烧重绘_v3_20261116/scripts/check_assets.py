import base64
import hashlib
import io
import json
from pathlib import Path

from PIL import Image

root = Path("D:/alianximod/docs/资源库/过程归档/引燃_全身燃烧重绘_v3_20261116")
entries = []
for path in sorted(root.rglob("*")):
    if not path.is_file() or path.name == "manifest.json":
        continue
    entry = {
        "path": path.relative_to(root).as_posix(),
        "bytes": path.stat().st_size,
        "sha256": hashlib.sha256(path.read_bytes()).hexdigest().upper(),
    }
    if path.suffix.lower() in {".png", ".gif"}:
        with Image.open(path) as image:
            entry["size"] = list(image.size)
            entry["frames"] = getattr(image, "n_frames", 1)
            if path.suffix == ".gif":
                durations = []
                for frame in range(image.n_frames):
                    image.seek(frame)
                    durations.append(image.info.get("duration"))
                entry["duration_ms"] = sum(durations)
                entry["unique_frame_ms"] = sorted(set(durations))
            if path.parent.name == "textures":
                entry["alpha_values"] = sorted(set(image.convert("RGBA").getchannel("A").getdata()))
    entries.append(entry)
model = json.loads((root / "blockbench/fire_kindle_v3_redraw_candidate_preview.bbmodel").read_text(encoding="utf-8"))
embedded = []
for texture in model["textures"]:
    data = base64.b64decode(texture["source"].split(",", 1)[1])
    with Image.open(io.BytesIO(data)) as image, Image.open(root / "textures" / texture["name"]) as exported:
        pixels = image.convert("RGBA").tobytes()
        embedded.append({
            "name": texture["name"],
            "embedded_png_sha256": hashlib.sha256(data).hexdigest().upper(),
            "rgba_sha256": hashlib.sha256(pixels).hexdigest().upper(),
            "same_rgba": pixels == exported.convert("RGBA").tobytes(),
        })
print(json.dumps({"files": entries, "embedded": embedded}, ensure_ascii=False, indent=2))
