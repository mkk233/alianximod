from pathlib import Path
from PIL import Image, ImageSequence
import datetime
import hashlib
import json
import struct

ROOT = Path(__file__).resolve().parents[1]
artifacts = []
for path in sorted(ROOT.rglob("*")):
    if not path.is_file() or path.name == "manifest.json":
        continue
    data = path.read_bytes()
    item = {"path": path.relative_to(ROOT).as_posix(), "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest().upper()}
    if path.suffix.lower() in [".png", ".gif"]:
        with Image.open(path) as image:
            item.update({"width": image.width, "height": image.height, "mode": image.mode})
            if path.suffix.lower() == ".gif":
                item["frames"] = image.n_frames
                item["durationMs"] = sum(frame.info.get("duration", 0) for frame in ImageSequence.Iterator(image))
            else:
                item["transparent"] = "A" in image.getbands() and image.getchannel("A").getextrema()[0] < 255
    elif path.suffix.lower() == ".aseprite":
        magic, count, width, height, depth = struct.unpack_from("<5H", data, 4)
        offset, duration = 128, 0
        for frame in range(count):
            length, frame_magic, chunks, milliseconds = struct.unpack_from("<IHHH", data, offset)
            duration += milliseconds
            offset += length
        item.update({"width": width, "height": height, "frames": count, "depth": depth, "durationMs": duration})
    elif path.suffix.lower() == ".bbmodel":
        model = json.loads(data)
        item.update({"elements": len(model["elements"]), "textures": len(model["textures"]), "animations": [{"name": animation["name"], "seconds": animation["length"]} for animation in model["animations"]]})
    if path.name.startswith("inspection-"):
        item["status"] = "working-camera-inspection-not-confirmation"
    elif path.name.startswith("decoded-gif-"):
        item["status"] = "decoded-final-gif-validation-not-new-native-capture"
    artifacts.append(item)
manifest = {
    "version": "earth-ground-airborne-dust-v2r2-20261116",
    "batchId": "待制作-20261031", "taskId": "earth_break_ground_v2_art_preview",
    "attemptId": "20261116-art-ground-v2-04",
    "parent": "01a1ce06-b681-7c90-b11d-260d3c169911",
    "createdAt": datetime.datetime.now().astimezone().isoformat(),
    "status": "candidate-awaiting-user-same-version-confirmation",
    "userFeedbackVerbatim": "这个尘土效果不好没有飞扬的感觉",
    "rejectedVersion": "earth-ground-mouth-dust-v2-20261116",
    "review": {"designUsed": 2, "implementationUsed": 3, "statement": "本阶段额度已用完，返修结果未追加复审"},
    "functionalChanges": False, "integrated": False, "compiled": False, "gameTested": False,
    "children": [],
    "sourceTools": {"aseprite": "magpie MCP batch 1.3.17/API39", "blockbench": "magpie MCP actual desktop 5.2.2/plugin1.10.1"},
    "timing": {"oldCandidateSeconds": 0.8, "proposedRuntimeBudgetSeconds": 1.4, "proposedRuntimeTicks": 28, "dustTextureLifetimeSeconds": 1.2, "staggerSeconds": [0, 0.05, 0.1], "previewSeconds": 2.3, "previewTriggerSeconds": 0.4, "allInvisibleFromPreviewSeconds": 1.65},
    "nativeSaveReopen": {"created": "937be075-0517-53f5-6aa9-c7f18ce35ab8", "reopened": "1aa7c01f-c3a9-2d54-e824-b3331f4094e9", "nativeSaveAndReopenVerified": True},
    "requirementsSnapshot": "evidence/requirements-N-O-DV-current.json",
    "coordinatedWorkbookSha256": "D7681EAAD336B5E4719B9FFE21BEE2652D9EA74303C97E86119A1C0B1ADD3F56",
    "handoff": "visual-handoff.md", "motionRecipe": "evidence/motion-recipe.json",
    "gifChecks": json.loads((ROOT / "evidence/gif-verification.json").read_text(encoding="utf-8")),
    "artifacts": artifacts,
    "selfHash": "Not included to avoid self-reference; receipt records manifest SHA256.",
}
(ROOT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"artifacts": len(artifacts), "manifestSha256": hashlib.sha256((ROOT / "manifest.json").read_bytes()).hexdigest().upper()}, ensure_ascii=False))
