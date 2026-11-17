from pathlib import Path
from datetime import datetime, timezone, timedelta
import hashlib
import json
from PIL import Image

root = Path(__file__).resolve().parents[1]
prior = root.parent / "幽冥_v2领域定稿预览_20261116"
receipt = Path("D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/gloom-v2-atmosphere-revision-preview-20261116.md")

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()

def save_json(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

prior_manifest = json.loads((prior / "manifest.json").read_text(encoding="utf-8-sig"))
prior_checks = []
for item in prior_manifest["files"]:
    path = prior / item["path"]
    actual = digest(path)
    assert actual == item["sha256"], str(path)
    prior_checks.append({"path": str(path), "sha256": actual, "unchanged": True})
protected = json.loads((prior / "evidence/input-preservation-final.json").read_text(encoding="utf-8-sig"))
protected_checks = []
for item in protected:
    path = Path(item["Path"])
    actual = digest(path)
    protected_checks.append({"path": str(path), "previous_attempt_sha256": item["After"], "sha256": actual, "unchanged_since_previous_attempt": actual == item["After"], "written_by_this_attempt": False})
assert digest(prior / "manifest.json") == "DD378114EC0C99F9F2A2DD0D7D3C48F92ED9FB389EABA797D1772B73EF8FC779"
save_json(root / "evidence/input-preservation.json", {
    "prior_manifest_sha256": digest(prior / "manifest.json"),
    "prior_artifact_count_unchanged": len(prior_checks),
    "prior_artifacts": prior_checks,
    "comparison_basis": "Previous attempt final hashes, not this attempt start. Workbook and shared README differ; this art attempt did not write either. External change author/time not investigated.",
    "protected": protected_checks
})
reopen = json.loads((root / "evidence/reopen-proof.json").read_text(encoding="utf-8"))
assert all(item["stillOpen"] and item["savedBefore"] == item["savedAfter"] for item in reopen["protectedProjects"])
with Image.open(root / "01-inside-eye-sky-and-fire.png") as before_view, Image.open(root / "evidence/reopened-actual-viewport.png") as after_view:
    assert before_view.size == after_view.size == (1280, 912)
    reopened_pixel_difference = sum(before != after for before, after in zip(before_view.convert("RGBA").getdata(), after_view.convert("RGBA").getdata()))
tool_evidence = {
    "tools": [
        {"name": "mcp__magpie.aseprite__status", "result": "1.3.17, batch ready, live.connected=false"},
        {"name": "mcp__magpie.aseprite__launch_aseprite", "result": "connected=true; existing installed native GUI"},
        {"name": "mcp__magpie.aseprite__run_lua", "count": 2, "response_each": "Aseprite disconnected before answering", "native_operations_observed": ["new palette-only editable source and sprite sheet saved", "native source reopened and GIF exported"], "file_evidence": ["evidence/aseprite-native.txt", "evidence/aseprite-reopen.txt", "evidence/encoding-and-pixels.json"]},
        {"name": "mcp__magpie.blockbench__get_capabilities", "result": "desktop 5.2.2; plugin 1.10.1; active old project 0ff57af5-a0c2-9110-a30c-aa29bf43c939"},
        {"name": "mcp__magpie.blockbench__risky_eval", "result": "native clone/load, editable constructors and scoped Undo, Codecs.project.write/load, synchronous Screencam.screenshotPreview successful"}
    ],
    "aseprite_palette_mapping_applied_to_existing_nontransparent_cel_pixels": [
        {"condition": "oldG >= 220 and oldR >= 100", "rgb": [204, 250, 245]},
        {"condition": "else oldG >= 185", "rgb": [80, 219, 222]},
        {"condition": "else oldG >= 130", "rgb": [14, 139, 158]},
        {"condition": "else", "rgb": [7, 66, 83]}
    ],
    "alpha_untouched_in_Aseprite": True,
    "outside_view_visibility_correction": "Initial group-only visibility did not hide proxy in actual screenshot. Corrected to child cube visibility, recaptured, inspected, restored before save.",
    "hidden_window_raf": "Not used during this attempt",
    "desktop_control": False,
    "imagegen": False,
    "headless": False,
    "alternate_instance": False,
    "global_configuration_changed": False,
    "reopened_viewport_byte_identical": False,
    "reopened_viewport_changed_pixels": reopened_pixel_difference,
    "reopened_viewport_pixel_count": 1280 * 912,
    "reopened_viewport_inspection": "Same scene, camera and materials visually verified. Small pixel differences are recorded, not claimed byte-identical; cause not independently verified.",
    "images_inspected_with_view_image": ["compare-old-same-camera.png", "01-inside-eye-sky-and-fire.png", "02-overall-three-quarter.png", "03-side-person-height.png", "v2-palette-preview.gif", "inside-soulvalley-4s.gif", "evidence/reopened-actual-viewport.png", "frames/inside_027.png", "frames/inside_054.png"],
    "original_projects_preserved": len(reopen["protectedProjects"]),
    "forge_validation": "not run"
}
save_json(root / "evidence/tool-evidence.json", tool_evidence)
files = [{"path": path.relative_to(root).as_posix(), "bytes": path.stat().st_size, "sha256": digest(path)} for path in sorted(root.rglob("*")) if path.is_file() and path.name != "manifest.json"]
manifest = {
    "version": "gloom-v2-soulvalley-danger-rev01-20261116",
    "status": "candidate_awaiting_user_same_version_confirmation",
    "batchId": "待制作-20261031",
    "taskId": "fire_nether_gloom_v2_domain_preview",
    "attemptId": "20261116-art-gloom-rev-01",
    "created_at": datetime.now(timezone(timedelta(hours=8))).isoformat(),
    "role": "art_author_not_reviewer",
    "native_agent_id": "01a1e3a5-bf00-7532-978b-a0d7284fcc0f",
    "native_agent_id_source": "parent resume task",
    "direct_parent": "current main conversation; actual id unknown",
    "requested_model": "gpt-6-astra",
    "requested_effort": "max",
    "actual_served_backend": "unknown_not_independently_exposed",
    "fallback": [{"model": model, "result": "Unknown model; no agent id/artifact; parent task evidence"} for model in ["antigravity/gemini-4-carbon", "antigravity/gemini-4-argon"]],
    "review": {"design": "2/2", "implementation": "3/3", "note": "本阶段额度已用完，返修结果未追加复审"},
    "input_root": str(prior),
    "input_manifest_sha256": digest(prior / "manifest.json"),
    "input_requirement_snapshot_sha256": digest(prior / "需求快照-row10.json"),
    "geometry": {"blocks": [7, 8, 7], "axis_order": "X,Y,Z", "model_units_per_block": 16, "cluster_count": 49, "sample_uuid": "20261116-0000-4000-8000-000000000049", "anchor_y_blocks": 0.15, "follows": "world-fixed", "person_blocks": 1.8, "eye_blocks": 1.62, "candidate_width_multiplier": 0.94, "candidate_height_multiplier": 1.16, "candidate_pulse_width": [0.82, 1], "candidate_pulse_height": [0.52, 1], "sparse_visual_embers": 8},
    "texture": {"frame_size": [64, 64], "strip_size": [64, 1024], "frames": 16, "frame_ms": 100, "alpha_mask": "exactly identical to runtime v2", "rgb": "Aseprite palette-only revision, not identical to runtime", "v3_adopted": False},
    "viewport_animation": {"source": "actual Blockbench main viewport screenshots", "frames": 80, "duration_ms": 4000, "frame_ms": 50, "size": [960, 684], "interpolation": False, "transition_demonstrated": False, "not_spell_duration": True},
    "atmosphere": {"native_preview_proxy": "far sky enclosure gradient and fog-colored distant auxiliary scenery", "runtime_sky_or_fog_implemented": False, "camera_inside_detection_implemented": False, "transition_suggestion_only": {"enter_seconds": 0.6, "exit_seconds": 0.8, "curve": "u*u*(3-2*u)", "user_approved_parameter": False, "demonstrated": False}},
    "native_blockbench": {"created": reopen["createdProject"], "reopened": reopen["reopenedProject"], "saved": reopen["saved"], "cubes": reopen["cubes"], "groups": reopen["groups"], "textures": reopen["textures"], "animations": reopen["animations"], "original_projects_preserved": len(reopen["protectedProjects"])},
    "confirmation": False,
    "integration": False,
    "compile": False,
    "game_test": False,
    "java_changes": False,
    "write_scope_closed": True,
    "receipt": {"path": str(receipt), "sha256": digest(receipt)},
    "files": files
}
save_json(root / "manifest.json", manifest)
for item in files:
    assert digest(root / item["path"]) == item["sha256"]
print(json.dumps({"files_verified": len(files), "old_artifacts_unchanged": len(prior_checks), "protected_inputs_same_as_prior_attempt": sum(item["unchanged_since_previous_attempt"] for item in protected_checks), "protected_inputs_different_from_prior_attempt": sum(not item["unchanged_since_previous_attempt"] for item in protected_checks), "original_projects_preserved": len(reopen["protectedProjects"]), "manifest_sha256": digest(root / "manifest.json"), "receipt_sha256": digest(receipt), "native_reopen_changed_pixels": reopened_pixel_difference}, ensure_ascii=True))
