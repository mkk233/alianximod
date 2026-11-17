from pathlib import Path
from PIL import Image
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
RECEIPT = Path("D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/detonate-v2-3d-art-preview-20261116.md")
def record(path):
    data = path.read_bytes()
    item = {"path": str(path.relative_to(ROOT)).replace("\\", "/") if path.is_relative_to(ROOT) else str(path), "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest().upper()}
    if path.suffix.lower() in [".png", ".gif"]:
        with Image.open(path) as image:
            item.update(width=image.width, height=image.height, frames=getattr(image, "n_frames", 1))
    return item

manifest = {
    "version": "fire_detonate_v2_3d_candidate_20261116",
    "status": "candidate_pending_user_confirmation",
    "batchId": "待制作-20261031",
    "taskId": "fire_detonate_v2_art",
    "attemptId": "20261116-art-detonate-3d-01",
    "parentId": "01a1ce06-b681-7c90-b11d-260d3c169911",
    "authorAgentId": None,
    "requestedModel": "gpt-6-astra",
    "requestedEffort": "max",
    "actualBackendModel": None,
    "fallback": {"priorModels": ["antigravity/gemini-4-carbon", "antigravity/gemini-4-argon"], "priorError": "Unknown model", "source": "existing parent-confirmed spawn evidence; not retried this attempt", "quotaOr403Reason": False},
    "reviewBudget": {"design": "2/2", "implementation": "3/3", "statement": "本阶段额度已用完，返修结果未追加复审"},
    "input": record(ROOT / "evidence/input-snapshot-used.json"),
    "latestUserWords": "效果不太行，应该参考原版爆炸的效果三维的扩散开而不是简单的二维效果",
    "mechanicsChanged": False,
    "units": {"modelUnitsPerBlock": 16, "targetHeightUnits": 28.8, "targetHeightBlocks": 1.8, "groundGridUnits": 16},
    "axes": {"+X": "target right", "+Y": "up", "+Z": "target front"},
    "anchor": {"previewRelativeToFeet": [0, 16, 1.8], "runtimeProposal": "actual world-space carrier hit point, frozen during one shot", "extraYOffset": 0, "cameraBias": 0},
    "geometry": {"kind": "26 independent world-positioned camera-facing puff planes", "outerPuffs": 24, "centerPuffs": 2, "singlePlaneThickness": 0, "wholeEffectCameraPlane": False, "seedFile": "evidence/particle-seeds.json"},
    "texture": {"framePixels": [32, 32], "stripPixels": [32, 512], "frameCount": 16, "sourceFrameMs": 50, "sourceLayers": 2, "layout": "vertical top to bottom", "filter": "nearest", "alpha": "0 or 255", "lastFrameTransparent": True},
    "timing": {"effectTicks": 20, "effectMs": 1000, "nativeFramesPerSequence": 20, "nativeFrameMs": 50, "previewHoldMs": 650, "loopInGame": False, "loopInPreview": True, "encodedGifFrames": 19, "encodedGifMs": 1650, "localAgeToAtlasFrame": "floor(clamp(age/(lifetime-1),0,1)*15)"},
    "material": {"nativeShade": False, "nativeEmissive": False, "alphaPreserved": True, "transparent": True, "depthTest": True, "depthWrite": False, "sort": "each puff back to front every frame", "runtimeProposal": "lit particle material preserving alpha, nearest, FULL_BRIGHT; not yet implemented"},
    "strength": {"spent0": "no extra particles", "samples": [{"spent": 10, "scale": 0.65}, {"spent": 50, "scale": 1.0}, {"spent": 100, "scale": 1.35}], "scalesAffect": ["world displacement", "individual puff face dimensions"], "damageRadius": False, "values1to9": "nonzero visual scale proposed", "above100": "not previewed; mapping requires confirmation"},
    "variants": {"orange": "textures/explosion_puff_orange-strip.png", "soul": "textures/explosion_puff_soul-strip.png", "sameAlpha": True, "sameGeometryAndTiming": True, "vertexColor": "white"},
    "model": {"path": "blockbench/fire_detonate_v2_3d_candidate.bbmodel", "sourceUuid": "5f702182-73a6-cd40-964d-2ad6a7c2a361", "reopenedUuid": "e9eebb72-fed6-5b89-ab9e-3991002b637d", "savedGlobalFrame": 7, "nativeAnimationTracks": False, "dynamicMethod": "native GUI pose/UV update then capture; scripts and poses archived", "auxiliaryObjectsExcludedFromRuntime": True},
    "previews": {"preferred": "previews/detonate_v2_3d_main.gif", "side": "previews/detonate_v2_3d_side.gif", "strength": "previews/detonate_v2_3d_strength.gif", "soul": "previews/detonate_v2_3d_soul.gif", "nativeSource": "actual Desktop Blockbench main viewport", "encoding": "fixed crop per sequence of native captures; no repaint", "cropRecord": "evidence/native-gif-encoding.json"},
    "protectedExistingProjects": 17,
    "protectedProjectsUnchanged": True,
    "oldRejectedCandidateOverwritten": False,
    "userConfirmed": False,
    "integrated": False,
    "compiled": False,
    "forgeVerified": False,
    "writeScopeClosed": True,
    "handoff": "visual-handoff.md",
    "independentReceipt": record(RECEIPT),
    "verification": json.loads((ROOT / "evidence/asset-verification.json").read_text(encoding="utf-8")),
    "files": [record(path) for path in sorted(ROOT.rglob("*")) if path.is_file() and path.name != "manifest.json"]
}
(ROOT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"status": manifest["status"], "listedFiles": len(manifest["files"]), "receiptSHA256": manifest["independentReceipt"]["sha256"]}, ensure_ascii=False))
