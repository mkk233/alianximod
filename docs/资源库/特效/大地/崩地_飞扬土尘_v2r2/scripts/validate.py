from pathlib import Path
from PIL import Image, ImageSequence
import hashlib
import json
import struct

ROOT = Path(__file__).resolve().parents[1]
validation = {}
for name in ["ground_airborne_v2r2", "visual_grit_v2r2"]:
    content = (ROOT / f"source/{name}.aseprite").read_bytes()
    magic, count, width, height, depth = struct.unpack_from("<5H", content, 4)
    assert magic == 0xA5E0 and depth == 32
    durations = []
    offset = 128
    for index in range(count):
        length, frame_magic, chunks, duration = struct.unpack_from("<IHHH", content, offset)
        assert frame_magic == 0xF1FA
        durations.append(duration)
        offset += length
    validation[name] = {"frames": count, "width": width, "height": height, "durationMs": sum(durations), "frameDurationsMs": durations}
assert validation["ground_airborne_v2r2"]["durationMs"] == 1200
with Image.open(ROOT / "previews/ground_airborne_v2r2-2d.gif") as animation:
    frames = [frame.convert("RGBA").copy() for frame in ImageSequence.Iterator(animation)]
    validation["gif2d"] = {"encodedFrames": animation.n_frames, "uniqueFrames": len({hashlib.sha256(frame.tobytes()).hexdigest() for frame in frames}), "durationMs": sum(frame.info.get("duration", 0) for frame in ImageSequence.Iterator(animation))}
    assert validation["gif2d"]["encodedFrames"] == 24
for name in ["overall", "side"]:
    with Image.open(ROOT / f"previews/earth_ground_v2r2-{name}-real-blockbench.gif") as animation:
        elapsed = 0
        for frame in ImageSequence.Iterator(animation):
            duration = frame.info["duration"]
            for label, target in [("apex", 850), ("tail", 1500), ("clear", 1850)]:
                if elapsed <= target < elapsed + duration:
                    frame.convert("RGB").save(ROOT / f"previews/decoded-gif-{name}-{label}.png")
            elapsed += duration
        assert elapsed == 2300
model = json.loads((ROOT / "source/earth_ground_airborne_v2r2_preview.bbmodel").read_text(encoding="utf-8"))
assert len(model["animations"]) == 1 and model["animations"][0]["length"] == 2.3
validation["nativeModel"] = {"elements": len(model["elements"]), "textures": len(model["textures"]), "animations": len(model["animations"]), "length": model["animations"][0]["length"], "sha256": hashlib.sha256((ROOT / "source/earth_ground_airborne_v2r2_preview.bbmodel").read_bytes()).hexdigest()}
build = json.loads((ROOT / "evidence/native-build.json").read_text(encoding="utf-8"))
assert len(build["removedCells"]) == 23
reopened = json.loads((ROOT / "evidence/reopened-render.json").read_text(encoding="utf-8"))
assert all(texture["error"] == 0 for texture in reopened["textures"])
current_projects = {item["uuid"]: item for item in reopened["projects"]}
assert all(current_projects[item["uuid"]] == item for item in build["previousProjects"])
validation["protectedOpenProjects"] = {"count": len(build["previousProjects"]), "allRetainedWithSameNamesAndSavedStates": True}
capture = json.loads((ROOT / "evidence/native-capture-side.json").read_text(encoding="utf-8"))
grain = [{"time": sample["time"], "heightAboveGroundBlocks": (sample["grains"][0]["positionModel"][1] - 8) / 16} for sample in capture["trajectory"]]
assert grain[0]["heightAboveGroundBlocks"] < grain[3]["heightAboveGroundBlocks"]
assert grain[3]["heightAboveGroundBlocks"] > grain[5]["heightAboveGroundBlocks"] > grain[6]["heightAboveGroundBlocks"]
validation["nativeMeasuredGrain01"] = grain
validation["visibleCountsAtStages"] = [capture["samples"][index] for index in [4, 8, 10, 17, 24, 30, 33, 37, 45]]
bounds = {}
for name in ["overall", "side"]:
    frame_bounds = []
    for path in sorted((ROOT / f"previews/{name}-frames").glob("*.png")):
        image = Image.open(path)
        bbox = image.getchannel("A").getbbox()
        assert bbox and bbox[0] > 0 and bbox[1] > 0 and bbox[2] < image.width and bbox[3] < image.height, (path, bbox)
        frame_bounds.append(bbox)
    bounds[name] = {"minX": min(item[0] for item in frame_bounds), "minY": min(item[1] for item in frame_bounds), "maxX": max(item[2] for item in frame_bounds), "maxY": max(item[3] for item in frame_bounds), "allObjectsWithinViewport": True}
validation["viewportBounds"] = bounds
before = json.loads((ROOT / "evidence/protected-before.json").read_text(encoding="utf-8"))
after = {path: hashlib.sha256(Path(path).read_bytes()).hexdigest() for path in before}
assert after == before
(ROOT / "evidence/protected-after.json").write_text(json.dumps(after, ensure_ascii=False, indent=2), encoding="utf-8")
validation["protectedFiles"] = {"count": len(after), "allUnchangedSinceCoordinatedDvUpdate": True}
(ROOT / "evidence/final-validation.json").write_text(json.dumps(validation, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(validation, ensure_ascii=False, indent=2))
