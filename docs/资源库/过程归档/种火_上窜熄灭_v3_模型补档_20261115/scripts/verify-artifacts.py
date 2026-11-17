import json, hashlib, struct
from pathlib import Path
from PIL import Image
d=Path(r"D:/alianximod/docs/资源库/过程归档/种火_上窜熄灭_v3_模型补档_20261115")
before=json.loads((d/"inputs-before.json").read_text(encoding="utf-8-sig"))
rows=[]
for f in before["files"]:
 p=Path(f["path"])
 rows.append({"path":str(p).replace("\\","/"),"beforeSha256":f["sha256"],"afterSha256":hashlib.sha256(p.read_bytes()).hexdigest().upper(),"sizeBefore":f["size"],"sizeAfter":p.stat().st_size})
strip=Image.open(Path(r"D:/alianximod/docs/资源库/特效/火焰/种火_上窜_v3/seed_sow_v3-strip.png")).convert("RGBA")
frames=[]
for i in range(16):
 im=Image.open(d/"frames"/f"frame_{i+1:02}.png").convert("RGBA")
 a=im.getchannel("A")
 frames.append({"frame":i+1,"durationMs":50,"matchesStripRGBA":im.tobytes()==strip.crop((0,i*64,64,(i+1)*64)).tobytes(),"nontransparentPixels":sum(v>0 for v in a.getdata()),"alphaValues":sorted(set(a.getdata()))})
spr=Path(r"D:/alianximod/docs/资源库/特效/火焰/种火_上窜_v3/seed_sow_v3.aseprite").read_bytes()
pos=128; durations=[]
for i in range(struct.unpack_from("<H",spr,6)[0]):
 size,magic,chunks,duration=struct.unpack_from("<IHHH",spr,pos)
 durations.append(duration); pos+=size
shots=[]
for name in ["01-hero-3quarter.png","02-front-billboard.png","03-side-offset-thickness.png","04-closeup-burnout-phases.png"]:
 p=d/name; im=Image.open(p)
 shots.append({"path":str(p).replace("\\","/"),"size":p.stat().st_size,"dimensions":list(im.size),"sha256":hashlib.sha256(p.read_bytes()).hexdigest().upper()})
result={"inputCount":len(rows),"allInputsByteUnchanged":all(r["beforeSha256"]==r["afterSha256"] and r["sizeBefore"]==r["sizeAfter"] for r in rows),"inputs":rows,"stripDimensions":list(strip.size),"asepriteDurationsMs":durations,"frames":frames,"allFramesMatchStrip":all(f["matchesStripRGBA"] for f in frames),"frame16FullyTransparent":frames[-1]["nontransparentPixels"]==0,"screenshots":shots}
(d/"verification.json").write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding="utf8")
print(json.dumps({k:v for k,v in result.items() if k not in ("inputs","frames")},ensure_ascii=False))

