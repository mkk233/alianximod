import json, hashlib, math
from pathlib import Path
from datetime import datetime, timezone
d=Path(r"D:/alianximod/docs/资源库/过程归档/种火_上窜熄灭_v3_模型补档_20261115")
receipt=Path(r"D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/seed-sow-v3-model-repair-20261115.md")
def digest(p):
 return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def entry(p):
 return {"path":str(p).replace("\\","/"),"bytes":p.stat().st_size,"sha256":digest(p)}
v=json.loads((d/"verification.json").read_text(encoding="utf8"))
r=json.loads((d/"reopen-validation.json").read_text(encoding="utf8"))
n=json.loads((d/"native-final-state.json").read_text(encoding="utf8"))
model=d/"seed_sow_v3_preview.bbmodel"
assert digest(model)=="CE7AC65C0406AE3A338FBB5CD7AE98E70D005A245520C7B4603245715DB9852C"
assert v["allInputsByteUnchanged"] and v["allFramesMatchStrip"] and v["frame16FullyTransparent"]
assert r["reopen"]["differentNativeProjectObject"] and n["texturesReady"]==43 and not n["missingTextureErrors"]
capture=json.loads((d/"viewport-capture-evidence.json").read_text(encoding="utf8"))
initial=capture.get("initialCompletedBatch",capture)
shots=[]
for index,s in enumerate(initial["shots"]):
 x=dict(s)
 p=d/s["file"]
 x.update(entry(p))
 x["metadataBasis"]="executed native capture script; final PNG independently observed, hashed and viewed"
 if index<2:
  direction=[a-b for a,b in zip(s["camera"],s["target"])]
  length=math.sqrt(sum(a*a for a in direction))
  normal=[a/length for a in direction]
  x["normal"]=normal
  x["pitchYawDegrees"]=[-math.degrees(math.asin(normal[1])),math.degrees(math.atan2(normal[0],normal[2])),0]
  x["normalCameraDot"]=1.0
 shots.append(x)
finalCapture={"attemptId":"20261115-art-seed-sow-05","initialCompletedBatch":initial,"shots":shots,"finalModel":entry(model),"nativeReopenEvidence":"reopen-validation.json","nativeFinalState":"native-final-state.json","finalCaptureToolReturn":"50s timeout; no successful return claimed; PNG files were observed on disk and visually inspected","optionalTimingDemo":"not completed, invalidated; static F08 restored; no continuing playback","pixelEditingOrComposition":False,"realNativeGUIRenderer":True}
(d/"viewport-capture-evidence.json").write_text(json.dumps(finalCapture,ensure_ascii=False,indent=2),encoding="utf8")
outputs=[entry(p) for p in sorted(d.rglob("*")) if p.is_file() and p.name!="manifest.json"]
manifest={"schemaVersion":1,"completedAtUtc":datetime.now(timezone.utc).isoformat(),"status":"completed_authorized_confirmed_v3_model_archive_only","batchId":"待制作-20261031","taskId":"fire_seed_sow_v3_model_repair","attemptId":"20261115-art-seed-sow-05","authorRole":"sole art author","directParent":{"role":"dispatching main session","id":"unknown"},"nativeAgentId":"unknown","requestedModel":"gpt-6-astra","reasoningEffort":"max","actualServiceModel":"unknown","fallbackErrors":[{"model":"antigravity/gemini-4-carbon","error":"Unknown model"},{"model":"antigravity/gemini-4-argon","error":"Unknown model"}],"fallbackEvidenceSource":"explicit dispatching-parent report; not 403; not quota","reviewBudget":{"design":"2/2 exhausted","implementation":"3/3 exhausted","additionalReview":False,"statement":"本阶段额度已用完，返修结果未追加复审"},"confirmation":{"date":"2026-11-11","quote":"按照这版接入","version":"seed_sow_v3","newRuntimeVisualParameters":False},"inputsUnchanged":{"count":65,"allByteIdentical":True,"evidence":["inputs-before.json","verification.json"],"asepriteSha256":"D514877D8AFFFFC6D5D54A9A6E64F46FBB12E41F1CB1C0C84A90C2B9129F1C0D","stripSha256":"A50EC7ED6A8434F50987A86036201DABFF3AB8A70CFF9D82A31CAC9F12B03EEE"},"texture":{"frameSize":[64,64],"stripSize":[64,1024],"frames":16,"frameDurationMs":50,"onePassMs":800,"loop":False,"lastFrameTransparent":True,"exportedFramesRgbaMatch":True},"runtimeFacts":{"cameraFacing":"CAMERA_PLANE including pitch","cameraOffsetBlocks":0.08,"targetHalfSpan":"collisionHeight/2","noTargetHalfSpanBlocks":0.875,"lifetimeTicks":16,"runtimeNotSimulated":["entity collision-height scaling","Youming tint","Forge depth/lighting/transparency","multiplayer cameras","tick scheduling and damage/spawn logic"]},"previewFacts":{"unitsPerBlock":16,"dummyHeightBlocks":1.8,"referenceFaceBlocks":[1.75,1.75],"referenceThicknessUnits":0.12,"bodyCenterUnits":[0,14.4,0],"offsetUnits":1.28,"thicknessIsPreviewOnly":True,"sideViewFrozenForInspection":True,"phaseFrames":[1,6,10,12,15,16],"persistentPlayback":False},"model":entry(model),"screenshots":[entry(d/x["file"]) for x in shots],"nativeReopen":{"success":True,"differentProjectObject":True,"elements":634,"groups":17,"embeddedTextures":43,"texturesReady":43},"toolsEvidence":"tool-evidence.json","limitations":["Two risky_eval calls timed out; final artifacts and successful static save/reopen are separately evidenced","Optional timed demo was invalidated, not delivered","Computer Use stopped by user Escape; no further desktop control","Private native viewport disposal returned onContextMenu is not defined; cleanup status unknown"],"noSubagents":True,"noCompile":True,"noPackaging":True,"noGameLaunch":True,"noGameTest":True,"noSharedIndexOrWorkbookOrSourceWrites":True,"writesClosed":True,"writeScopes":[str(d).replace("\\","/"),str(receipt).replace("\\","/")],"outputs":outputs,"independentReceipt":entry(receipt),"manifestSelfHashExcluded":True}
(d/"manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding="utf8")
print(json.dumps({"manifest":entry(d/"manifest.json"),"model":entry(model),"receipt":entry(receipt),"archiveFilesIncludingManifest":len(outputs)+1,"inputsUnchanged":65,"allFramesMatch":True,"nativeReopen":True},ensure_ascii=True))

