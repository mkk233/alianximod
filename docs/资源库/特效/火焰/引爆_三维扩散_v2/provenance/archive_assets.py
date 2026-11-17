from pathlib import Path
from datetime import datetime, timezone
import copy
import hashlib
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
CANDIDATE = Path("D:/alianximod/docs/资源库/过程归档/引爆_三维扩散爆炸_v2_20261116")
RECEIPT = Path("D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/detonate-v2-3d-art-approved-archive-20261116.md")


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def record(path):
    return {"path": path.relative_to(ROOT).as_posix(), "bytes": path.stat().st_size, "sha256": sha(path)}


candidate_manifest_hash = sha(CANDIDATE / "manifest.json")
candidate_manifest = json.loads((CANDIDATE / "manifest.json").read_text(encoding="utf-8"))
candidate_index = {item["path"]: item for item in candidate_manifest["files"]}
assert candidate_manifest["version"] == "fire_detonate_v2_3d_candidate_20261116"
for name, item in candidate_index.items():
    assert sha(CANDIDATE / name) == item["sha256"], f"Candidate changed: {name}"

asset_names = [
    "source/explosion_puff_orange.aseprite",
    "source/explosion_puff_soul.aseprite",
    "textures/explosion_puff_orange-strip.png",
    "textures/explosion_puff_soul-strip.png",
    "blockbench/fire_detonate_v2_3d_candidate.bbmodel",
    "previews/detonate_v2_3d_main.gif",
    "previews/detonate_v2_3d_side.gif",
    "previews/detonate_v2_3d_strength.gif",
    "previews/detonate_v2_3d_soul.gif",
    "previews/puff_keyframes.png",
    "previews/gui_peak_threequarter.png",
    "previews/gui_peak_side_top.png",
    "previews/gui_breakup_threequarter.png",
    "previews/gui_peak_soul.png",
    "previews/strength_comparison_detail.png",
]
mapping = {name: name for name in asset_names}
for name in ["input-snapshot-used.json", "particle-seeds.json", "asset-verification.json", "reopen-validation.json", "native-gif-encoding.json"]:
    mapping[f"provenance/{name}"] = f"evidence/{name}"
mapping["provenance/candidate-manifest.json"] = "manifest.json"

for destination in mapping:
    if (ROOT / destination).exists():
        raise RuntimeError(f"Destination exists; refuse overwrite: {destination}")
if (ROOT / "manifest.json").exists() or (ROOT / "provenance/archive-verification.json").exists():
    raise RuntimeError("Archive outputs already exist; refuse overwrite")

matches = []
for destination, original in mapping.items():
    source = CANDIDATE / original
    target = ROOT / destination
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)
    assert source.read_bytes() == target.read_bytes(), f"Byte mismatch: {destination}"
    expected = candidate_manifest_hash if original == "manifest.json" else candidate_index[original]["sha256"]
    assert sha(target) == expected
    matches.append({"formalPath": destination, "candidatePath": str(source), "bytes": target.stat().st_size, "candidateSHA256": expected, "formalSHA256": sha(target), "byteIdentical": True})

for name, item in candidate_index.items():
    assert sha(CANDIDATE / name) == item["sha256"], f"Candidate changed after copy: {name}"
assert sha(CANDIDATE / "manifest.json") == candidate_manifest_hash

verification = {
    "attemptId": "20261116-art-detonate-3d-archive-02",
    "checkedAtUtc": datetime.now(timezone.utc).isoformat(),
    "method": "verify candidate SHA256; binary copy; byte-for-byte comparison; verify candidate again",
    "copiedFiles": len(matches),
    "allCopiedFilesByteIdentical": True,
    "candidateRegisteredFilesCheckedBeforeAndAfter": len(candidate_index),
    "candidateManifestSHA256BeforeAndAfter": candidate_manifest_hash,
    "candidateUnchanged": True,
    "mcpStartedThisAttempt": False,
    "spatialParametersChanged": False,
    "runtimeFilesChecked": False,
    "matches": matches,
}
(ROOT / "provenance/archive-verification.json").write_text(json.dumps(verification, ensure_ascii=False, indent=2), encoding="utf-8")

manifest = {
    "version": candidate_manifest["version"],
    "status": "approved_for_adoption",
    "implementationStatus": "integration_in_progress",
    "batchId": "待制作-20261031",
    "taskId": "fire_detonate_v2_art",
    "attemptId": "20261116-art-detonate-3d-archive-02",
    "parentId": "01a1ce06-b681-7c90-b11d-260d3c169911",
    "authorAgentId": None,
    "requestedModel": "gpt-6-astra",
    "requestedEffort": "max",
    "actualBackendModel": None,
    "confirmation": {
        "userWords": "可以先采用这一版",
        "date": "2026-11-16",
        "timezone": "Asia/Shanghai",
        "preciseTimestamp": None,
        "relayedByParent": "01a1ce06-b681-7c90-b11d-260d3c169911",
        "boundVersion": candidate_manifest["version"],
        "boundPreviews": [copy.deepcopy(candidate_index[name]) for name in ["previews/detonate_v2_3d_main.gif", "previews/detonate_v2_3d_side.gif"]],
    },
    "candidateOrigin": {"path": str(CANDIDATE), "manifestSHA256": candidate_manifest_hash, "historicalManifestCopy": "provenance/candidate-manifest.json", "historyUnmodified": True},
    "integrator": {"name": "Harvey", "agentId": "01a1e377-93fa-7cc3-ac7f-4bd275c32916", "scope": "src; final runtime byte comparison remains integrator responsibility"},
    "reviewBudget": copy.deepcopy(candidate_manifest["reviewBudget"]),
    "userConfirmed": True,
    "integrated": False,
    "integrationCompletionVerified": False,
    "compiled": False,
    "forgeVerified": False,
    "mechanicsChanged": False,
    "spatialParametersChanged": False,
    "mcpStartedThisAttempt": False,
    "writeScopeClosed": True,
    "input": record(ROOT / "provenance/input-snapshot-used.json"),
    "handoff": "visual-handoff.md",
    "readme": "README.md",
    "archiveVerification": "provenance/archive-verification.json",
    "independentReceipt": {"path": str(RECEIPT), "bytes": RECEIPT.stat().st_size, "sha256": sha(RECEIPT)},
    "copiedAssetCount": len(asset_names),
    "copiedHistoricalEvidenceCount": len(matches) - len(asset_names),
    "allCopiedFilesByteIdentical": True,
    "historicalCandidateNamesPreservedForByteIdentity": True,
    "processLinks": {"scripts": str(CANDIDATE / "scripts"), "pixelFrames": str(CANDIDATE / "frames"), "guiFramesRoot": str(CANDIDATE / "previews"), "captureEvidenceRoot": str(CANDIDATE / "evidence")},
}
for name in ["units", "axes", "anchor", "geometry", "texture", "timing", "material", "strength", "variants", "model", "previews"]:
    manifest[name] = copy.deepcopy(candidate_manifest[name])
manifest["geometry"]["seedFile"] = "provenance/particle-seeds.json"
manifest["previews"]["cropRecord"] = "provenance/native-gif-encoding.json"
manifest["files"] = []
for path in sorted(ROOT.rglob("*")):
    if not path.is_file() or path.name == "manifest.json":
        continue
    item = record(path)
    original = mapping.get(item["path"])
    if original:
        item["candidateSource"] = original
        item["byteIdenticalToCandidate"] = True
        if original in candidate_index:
            for key in ["width", "height", "frames"]:
                if key in candidate_index[original]:
                    item[key] = candidate_index[original][key]
    manifest["files"].append(item)
(ROOT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
for item in manifest["files"]:
    assert sha(ROOT / item["path"]) == item["sha256"]
assert sha(RECEIPT) == manifest["independentReceipt"]["sha256"]
print(json.dumps({"status": manifest["status"], "implementationStatus": manifest["implementationStatus"], "copiedFiles": len(matches), "allByteIdentical": True, "candidateFilesUnchanged": len(candidate_index), "formalManifestFiles": len(manifest["files"]), "manifestSHA256": sha(ROOT / "manifest.json"), "receiptSHA256": sha(RECEIPT)}, ensure_ascii=False))
