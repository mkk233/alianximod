from pathlib import Path
import hashlib
import json
from datetime import datetime

root = Path(__file__).resolve().parent.parent
previous = root.parent / "幽冥_魂谷天空危险魂火_v2修订_20261116"
workspace = Path("D:/alianximod")
receipt = workspace / "docs/开发记录/术式批量制作/待制作-20261031/gloom-v2-sway-batch-preview-20261116.md"

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()

old_manifest = json.loads((previous / "manifest.json").read_text(encoding="utf-8"))
failures = []
for item in old_manifest["files"]:
    path = previous / item["path"]
    if not path.exists() or digest(path) != item["sha256"].upper():
        failures.append(item["path"])
assert not failures, failures
runtime_png = workspace / "src/main/resources/assets/xueximod/textures/effect/nether_gloom_v2/nether_gloom.png"
runtime_java = workspace / "src/main/java/com/bslx/XueXiMod/client/NetherDomainWisps.java"
assert digest(runtime_png) == "C4FA3B1B82BB29E8BD5040FDE4527BC9DBD8451083608DBB619C2AE02C9CB66F"
assert digest(runtime_java) == "5435DD96C5E9126726AC7E23B2D42C110BB53EBC3D4E1723648F9FC26CCE4791"
preservation = {
    "previousManifestFilesChecked": len(old_manifest["files"]),
    "previousFilesUnchanged": True,
    "previousManifestSha256": digest(previous / "manifest.json"),
    "runtimePngSha256": digest(runtime_png),
    "runtimeJavaSha256": digest(runtime_java),
    "oldSway03NotWrittenOrCounted": True,
    "oldLuaOutputRoot": str(root.parent / "幽冥_魂火摇曳_v2修订2_20261116"),
    "newOutputRoot": str(root),
}
(root / "evidence/input-preservation.json").write_text(json.dumps(preservation, ensure_ascii=False, indent=2), encoding="utf-8")
manifest = {
    "version": "gloom-v2-fixed-root-local-sway-batch-20261116",
    "status": "candidate_awaiting_user_same_version_confirmation",
    "batchId": "待制作-20261031",
    "taskId": "fire_nether_gloom_v2_domain_preview",
    "attemptId": "20261116-art-gloom-sway-04",
    "createdAt": datetime.now().astimezone().isoformat(),
    "author": "Avicenna",
    "role": "art_author_not_reviewer",
    "nativeAgentId": "01a1e3a5-bf00-7532-978b-a0d7284fcc0f",
    "directParent": "current resume/send_input main conversation; actual ID unknown",
    "requestedModel": "gpt-6-astra",
    "requestedEffort": "max",
    "servedBackend": "unknown_not_independently_exposed",
    "fallback": "Inherited actual Carbon and Argon spawn Unknown model; no returned agent/artifact; no new spawn this attempt",
    "review": {"design": "2/2", "implementation": "3/3", "note": "本阶段额度已用完，返修结果未追加复审"},
    "inputRoot": str(previous),
    "inputManifestSha256": digest(previous / "manifest.json"),
    "inputAsepriteSha256": digest(previous / "youming_v2_soulvalley_palette.aseprite"),
    "inputBbmodelSha256": digest(previous / "youming-v2-soulvalley-danger-candidate.bbmodel"),
    "geometry": {"modelUnitsPerBlock": 16, "widthX": 7, "depthZ": 7, "heightY": 8, "clusters": 49, "anchorYBlocks": 0.15, "clusterScale": [1, 1, 1], "wispScaleKeys": 0, "fixedWorldPositionAndYaw": True, "baseWidthBlocks": [0.6486, 0.9964], "baseHeightBlocks": [0.58, 1.2296]},
    "texture": {"frameWidth": 64, "frameHeight": 64, "stripHeight": 1024, "frames": 16, "frameMs": 100, "loopMs": 1600, "layers": 5, "fixedRootRowsZeroBased": [44, 63], "localTongueFrameOffsets": [0, 5, 9, 13], "clusterPhaseFrames": [0, 4, 8, 12], "maxLocalShiftPixels": 4, "previewAlpha": 210},
    "nativeAseprite": {"version": "1.3.17", "backend": "batch", "ui": False, "savedAndReopened": True, "productionResult": {"frames": 16, "layers": 5}, "boundedScriptCorrection": "dofile scope nil sprite; corrected once by loadfile explicit argument, success"},
    "nativeBlockbench": {"version": "5.2.2", "plugin": "1.10.1", "environment": "desktop", "createdProject": "e88e38bc-306f-4e1c-b4de-8a15f93dadd2", "reopenedProject": "bd0ab5c3-e2ff-a2d3-b619-3ba215a682d2", "viewport": "main", "capture": "native synchronous Screencam.screenshotPreview", "originalProjectsRetained": 24},
    "animations": [{"file": "inside-sway-3p2s.gif", "nativeFrames": 64, "gifFrames": 64, "durationMs": 3200}, {"file": "close-sway-3p2s.gif", "nativeFrames": 64, "gifFrames": 32, "durationMs": 3200, "note": "identical adjacent samples merged; not static"}],
    "confirmation": "pending",
    "integration": "not_done",
    "compile": "not_run",
    "gameTest": "not_run",
    "skyFog": "unchanged native proxy; Forge implementation absent; transition not recaptured in sway04",
    "runtimeClusterPulse": "unchanged in Java; cancelled only in this native candidate",
    "writeScopeClosed": True,
    "receipt": {"path": str(receipt), "sha256": digest(receipt)},
    "files": [{"path": str(path.relative_to(root)).replace("\\", "/"), "bytes": path.stat().st_size, "sha256": digest(path)} for path in sorted(root.rglob("*")) if path.is_file() and path.name != "manifest.json"],
}
(root / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"files": len(manifest["files"]), "inputPreservation": preservation, "manifestSha256": digest(root / "manifest.json"), "receiptSha256": digest(receipt)}, ensure_ascii=False))
