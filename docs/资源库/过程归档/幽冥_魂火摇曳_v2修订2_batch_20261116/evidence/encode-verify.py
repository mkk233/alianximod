from pathlib import Path
from PIL import Image, ImageSequence
import hashlib
import json

root = Path(__file__).resolve().parent.parent
previous = root.parent / "幽冥_魂谷天空危险魂火_v2修订_20261116"
strip = Image.open(root / "textures/youming_v2_sway_strip.png").convert("RGBA")
frames = [strip.crop((0, index * 64, 64, (index + 1) * 64)) for index in range(16)]
root_bytes = [frame.crop((0, 44, 64, 64)).tobytes() for frame in frames]
assert all(value == root_bytes[0] for value in root_bytes)
assert len({frame.tobytes() for frame in frames}) == 16
old_strip = Image.open(previous / "textures/youming_v2_soulvalley_strip.png").convert("RGBA")
assert frames[0].crop((0, 44, 64, 64)).tobytes() == old_strip.crop((0, 44, 64, 64)).tobytes()
palette = sorted({pixel for frame in frames for pixel in frame.getdata() if pixel[3]})
assert set(palette).issubset(set(old_strip.getdata()))
model = json.loads((root / "youming-v2-fixed-root-sway-candidate.bbmodel").read_text(encoding="utf-8"))
old_model = json.loads((previous / "youming-v2-soulvalley-danger-candidate.bbmodel").read_text(encoding="utf-8"))
assert model["elements"] == old_model["elements"]
def without_ui_expansion(value):
    if isinstance(value, list):
        return [without_ui_expansion(item) for item in value]
    if isinstance(value, dict):
        return {key: without_ui_expansion(item) for key, item in value.items() if key != "isOpen"}
    return value

assert without_ui_expansion(model["outliner"]) == without_ui_expansion(old_model["outliner"])
assert without_ui_expansion(model.get("groups")) == without_ui_expansion(old_model.get("groups"))
animation = model["animations"][0]
wisp_animators = [value for value in animation["animators"].values() if value.get("name", "").startswith("WISP_")]
assert len(wisp_animators) == 49
assert all(not value.get("keyframes") for value in wisp_animators)
encoded = []
for mode in ("inside", "close"):
    paths = sorted((root / "frames" / mode).glob("frame_*.png"))
    assert len(paths) == 64
    native = [Image.open(path).convert("RGB") for path in paths]
    hashes = [hashlib.sha256(frame.tobytes()).hexdigest() for frame in native]
    assert len(set(hashes)) >= 16
    indexed = [frame.quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE) for frame in native]
    output = root / (mode + "-sway-3p2s.gif")
    indexed[0].save(output, save_all=True, append_images=indexed[1:], duration=50, loop=0, disposal=2, optimize=False)
    gif = Image.open(output)
    durations = [frame.info.get("duration") for frame in ImageSequence.Iterator(gif)]
    evidence = json.loads((root / "evidence" / ("capture-" + mode + ".json")).read_text(encoding="utf-8"))
    assert evidence["allWispScalesIdentity"]
    baseline = evidence["frames"][0]["transforms"]
    assert all(frame["transforms"] == baseline for frame in evidence["frames"])
    encoded.append({"file": output.name, "frames": gif.n_frames, "durationMs": sum(durations), "uniqueNativeFrames": len(set(hashes)), "source": "unmodified native Screencam PNGs; GIF palette quantization only", "fixedWispTransformsAllFrames": True})
proof = {
    "rootRows44to63IdenticalAllFrames": True,
    "rootMatchesPreviousFrame0": True,
    "localTongueFramesUnique": 16,
    "rgbaPalette": palette,
    "frameBounds": [frame.getbbox() for frame in frames],
    "all273ElementsGroupsAndHierarchyUnchanged": True,
    "outlinerUiExpansionMayDiffer": True,
    "all49WispAnimatorsEmpty": True,
    "encoded": encoded,
}
(root / "evidence/encoding-and-pixel-proof.json").write_text(json.dumps(proof, ensure_ascii=False, indent=2), encoding="utf-8")
snapshot = json.loads((root.parent / "幽冥_v2领域定稿预览_20261116/需求快照-row10.json").read_text(encoding="utf-8-sig"))
snapshot["attemptId"] = "20261116-art-gloom-sway-04"
snapshot["latestUserFeedback"] = "火焰特效一直在变化大小的这个效果有点蠢幽冥的火焰应该摇曳而不是变大"
snapshot["precedence"] = "保留N/O/DV历史原文；approved_override与最新反馈优先，不能回退旧N限制；魂谷天空候选继续保留。"
(root / "需求快照-row10.json").write_text(json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(proof, ensure_ascii=False))
