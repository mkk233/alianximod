from pathlib import Path
from PIL import Image
import json, hashlib
root=Path("D:/alianximod/docs/资源库/过程归档/幽冥_v2领域定稿预览_20261116")
paths=sorted((root/"frames").glob("domain_*.png"))
assert len(paths)==96
frames=[Image.open(p).convert("RGB") for p in paths]
pal=frames[0].quantize(colors=256,method=Image.Quantize.MEDIANCUT)
encoded=[im.quantize(palette=pal,dither=Image.Dither.NONE) for im in frames]
encoded[0].save(root/"domain-animation-4p8s.gif",save_all=True,append_images=encoded[1:],duration=50,loop=0,disposal=2,optimize=False)
im=Image.open(root/"domain-animation-4p8s.gif")
dur=0
for i in range(im.n_frames):
 im.seek(i)
 dur+=im.info.get("duration",0)
result={"frames":im.n_frames,"size":im.size,"duration_ms":dur,"source":"96 native Blockbench main viewport PNG captures; GIF palette encoding only; no composited/synthetic/interpolated frames","png_frame_hashes":[{"file":p.name,"sha256":hashlib.sha256(p.read_bytes()).hexdigest()} for p in paths]}
(root/"evidence/gif-encoding.json").write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding="utf-8")
print(json.dumps({k:v for k,v in result.items() if k!="png_frame_hashes"},ensure_ascii=False))

