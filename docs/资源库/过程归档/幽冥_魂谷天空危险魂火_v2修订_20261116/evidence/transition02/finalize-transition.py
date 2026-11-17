from pathlib import Path
from datetime import datetime, timezone, timedelta
from PIL import Image
import hashlib
import json

root = Path(__file__).resolve().parents[2]
evidence = root / "evidence/transition02"
receipt = Path("D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/gloom-v2-atmosphere-transition-preview-20261116.md")
old_receipt = receipt.with_name("gloom-v2-atmosphere-revision-preview-20261116.md")

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()

def read_json(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))

def write_json(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")

baseline = read_json(evidence / "manifest-rev01.json")
unchanged = []
for item in baseline["files"]:
    if item["path"] == "visual-handoff.md":
        continue
    actual = digest(root / item["path"])
    assert actual == item["sha256"], item["path"]
    unchanged.append(item["path"])
assert (root / "visual-handoff.md").read_bytes().startswith((evidence / "visual-handoff-rev01.md").read_bytes().rstrip())
assert digest(old_receipt) == "64D727690D78AC6E6B084ABDD6D288A9FD6E03F654A99EA3F722841AC0755B82"
capture = read_json(evidence / "native-capture.json")
assert len(capture["frames"]) == 76
assert capture["frames"][14]["domainBlend"] > .4999
assert capture["frames"][14]["domainBlend"] < .5001
proof = read_json(evidence / "final-project-proof.json")
assert proof["modelDeepEqualToSavedFile"] and proof["saved"] and proof["allPriorProjectsRetained"] and proof["shaderBlendRemoved"]
samples = {}
for label, index in [("normal", 0), ("enter_half", 14), ("domain", 28), ("exit_half", 52), ("restored", 68)]:
    with Image.open(root / f"frames/transition02/tone_{index:03}.png") as image:
        samples[label] = list(image.convert("RGBA").getpixel((100, 20)))
assert samples["normal"] == samples["restored"]
for label in ["enter_half", "exit_half"]:
    assert all(abs(samples[label][channel] - (samples["normal"][channel] + samples["domain"][channel]) / 2) <= 2 for channel in range(3))
verification = {
    "previous_artifacts_unchanged_excluding_appended_handoff": len(unchanged),
    "unchanged_paths": unchanged,
    "handoff_append_only": True,
    "rev01_receipt_unchanged": True,
    "model_hash_unchanged": digest(root / "youming-v2-soulvalley-danger-candidate.bbmodel"),
    "old_sustained_gif_hash_unchanged": digest(root / "inside-soulvalley-4s.gif"),
    "baseline_manifest_sha256": digest(evidence / "manifest-rev01.json"),
    "key_frames_view_image_checked": ["transition-normal.png", "transition-domain.png", "transition-restored.png"],
    "gif_view_image_checked": True,
    "actual_sky_pixel_check": {"position": [100, 20], "values": samples, "restored_equals_normal": True, "half_weights_match_native_color_interpolation_within_2_levels": True},
    "native_model_deep_equal": True,
    "model_written": False,
    "shader_state_restored": True
}
write_json(evidence / "verification.json", verification)
manifest = baseline
manifest["latest_attemptId"] = "20261116-art-gloom-transition-02"
manifest["last_supplement_at"] = datetime.now(timezone(timedelta(hours=8))).isoformat()
manifest["historical_metadata_note"] = "Original attemptId/viewport_animation/atmosphere fields describe rev01 and are preserved. transition_supplement describes this added delivery; prior manifest is archived byte-for-byte."
manifest["transition_supplement"] = {
    "attemptId": "20261116-art-gloom-transition-02",
    "status": "candidate_same_version_delivery_supplement",
    "gif": "inside-tone-transition-3p8s.gif",
    "gif_sha256": digest(root / "inside-tone-transition-3p8s.gif"),
    "native_project": "0160f5df-5ae4-58e0-f638-74fa0e1dcca0",
    "existing_model_unchanged": True,
    "native_reproducible_script": "evidence/transition02/capture-transition.native.js",
    "frames": 76,
    "fps": 20,
    "duration_seconds": 3.8,
    "entry_seconds": 0.6,
    "exit_seconds": 0.8,
    "curve": "u*u*(3-2*u)",
    "transition_demonstrated": True,
    "camera_moves": False,
    "volume_test_run": False,
    "fire_remains_lit_during_exit": True,
    "expiry_or_afterglow_demonstrated": False,
    "normal_environment": "neutral daytime material reference, not Minecraft capture",
    "runtime_implemented": False,
    "user_confirmed": False,
    "compilation_or_game_run": False,
    "review_note": "本阶段额度已用完，返修结果未追加复审",
    "receipt": {"path": str(receipt), "sha256": digest(receipt)}
}
manifest["files"] = [{"path": path.relative_to(root).as_posix(), "bytes": path.stat().st_size, "sha256": digest(path)} for path in sorted(root.rglob("*")) if path.is_file() and path != root / "manifest.json"]
write_json(root / "manifest.json", manifest)
for item in manifest["files"]:
    assert digest(root / item["path"]) == item["sha256"]
print(json.dumps({"files_verified": len(manifest["files"]), "rev01_unchanged_files": len(unchanged), "handoff_append_only": True, "gif_sha256": digest(root / "inside-tone-transition-3p8s.gif"), "script_sha256": digest(evidence / "capture-transition.native.js"), "manifest_sha256": digest(root / "manifest.json"), "receipt_sha256": digest(receipt), "native_sky_samples": samples}, ensure_ascii=True))
