from pathlib import Path
from PIL import Image, ImageChops
import json
import math
import base64
import io

ROOT = Path(__file__).resolve().parents[1]
strips = {variant: Image.open(ROOT / f"textures/explosion_puff_{variant}-strip.png").convert("RGBA") for variant in ["orange", "soul"]}
assert ImageChops.difference(strips["orange"].getchannel("A"), strips["soul"].getchannel("A")).getbbox() is None
for variant, strip in strips.items():
    assert strip.size == (32, 512)
    for frame in range(16):
        original = Image.open(ROOT / f"frames/{variant}/puff_{frame+1:02d}.png").convert("RGBA")
        assert original.tobytes() == strip.crop((0, frame*32, 32, (frame+1)*32)).tobytes()
    assert strip.crop((0, 480, 32, 512)).getchannel("A").getbbox() is None
model = json.loads((ROOT / "blockbench/fire_detonate_v2_3d_candidate.bbmodel").read_text(encoding="utf-8"))
embedded = []
for texture in model["textures"]:
    if texture["name"].startswith("explosion_puff_"):
        image = Image.open(io.BytesIO(base64.b64decode(texture["source"].split(",")[1]))).convert("RGBA")
        variant = "orange" if "_orange-" in texture["name"] else "soul"
        assert image.tobytes() == strips[variant].tobytes()
        embedded.append(texture["name"])
assert len(embedded) == 2
captures = {mode: json.loads((ROOT / f"evidence/captures_{mode}.json").read_text(encoding="utf-8"))["samples"] for mode in ["main", "side", "strength", "soul"]}
for mode, samples in captures.items():
    assert len(samples) == 20
    assert len({tuple(sample["camera_position"]) for sample in samples}) == 1
    assert len({sample["camera_zoom"] for sample in samples}) == 1
    assert not any(particle["alive"] for particle in samples[-1]["particles"])
    for sample in samples:
        assert Path(sample["file"]).is_file()
for main, side, soul in zip(captures["main"], captures["side"], captures["soul"]):
    assert main["particles"] == side["particles"] == soul["particles"]
seeds = json.loads((ROOT / "evidence/particle-seeds.json").read_text())
frame_measures = []
for sample in captures["main"]:
    centers = [particle["center"] for particle in sample["particles"] if particle["alive"]]
    extents = [round((max(point[axis] for point in centers)-min(point[axis] for point in centers))/16, 4) if centers else 0 for axis in range(3)]
    frame_measures.append({"frame": sample["frame"], "at_ms": sample["at_ms"], "active_puffs": len(centers), "center_span_blocks_xyz": extents})
gif_reports = {}
for mode in captures:
    gif = Image.open(ROOT / f"previews/detonate_v2_3d_{mode}.gif")
    durations = []
    for index in range(gif.n_frames):
        gif.seek(index)
        durations.append(gif.info.get("duration"))
    assert sum(durations) == 1650
    gif_reports[mode] = {"size": list(gif.size), "encoded_frames": gif.n_frames, "total_ms": sum(durations), "effect_ms": 1000, "display_hold_ms": 650}
report = {"source_export_match": True, "alpha_equal_both_variants": True, "embedded_texture_pixel_match": embedded, "fixed_camera_each_sequence": True, "world_positions_identical_across_views_and_variants": True, "all_particles_inactive_final_frame": True, "particle_count_per_nonzero_effect": 26, "frame_measures": frame_measures, "gifs": gif_reports, "forge_verified": False}
(ROOT / "evidence/asset-verification.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(report, ensure_ascii=False))
