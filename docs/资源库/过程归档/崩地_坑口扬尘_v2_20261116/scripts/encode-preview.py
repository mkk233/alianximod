from pathlib import Path
from PIL import Image,ImageSequence
import json,hashlib
root=Path(__file__).resolve().parents[1]
paths=sorted((root/'previews'/'3d-frames').glob('overall_*.png'))
assert len(paths)==40,len(paths)
frames=[]
raw=[]
for p in paths:
    im=Image.open(p).convert('RGBA')
    assert im.size==(1100,900)
    raw.append(hashlib.sha256(im.tobytes()).hexdigest())
    # Encoding matte only. Every visual pixel comes from the real native viewport PNG.
    matte=Image.new('RGBA',im.size,(26,30,33,255));matte.alpha_composite(im)
    frames.append(matte.convert('RGB'))
palette_sheet=Image.new('RGB',(1100,900*4))
for j,idx in enumerate([0,12,18,28]):palette_sheet.paste(frames[idx],(0,900*j))
palette=palette_sheet.quantize(colors=256,method=Image.Quantize.MEDIANCUT)
encoded=[im.quantize(palette=palette,dither=Image.Dither.NONE) for im in frames]
target=root/'previews'/'earth_break_ground_v2-real-blockbench.gif'
encoded[0].save(target,save_all=True,append_images=encoded[1:],duration=[50]*40,loop=0,optimize=False,disposal=1)
checks={}
for name in ['earth_break_ground_v2-real-blockbench.gif','ground_dust_v2-2d.gif','ground_dust_v2-original.gif']:
    im=Image.open(root/'previews'/name)
    durations=[];hashes=[]
    for f in ImageSequence.Iterator(im):
        durations.append(f.info.get('duration',0)); hashes.append(hashlib.sha256(f.convert('RGBA').tobytes()).hexdigest())
    checks[name]={'size':im.size,'encoded_frames':len(durations),'unique_decoded_frames':len(set(hashes)),'duration_ms':sum(durations),'frame_durations_ms':durations,'loop':im.info.get('loop')}
assert checks[target.name]['duration_ms']==2000
assert checks[target.name]['unique_decoded_frames']>=16
evidence={'source':'40 direct native Blockbench Preview.screenshot PNGs, 20fps. No offline rendering or collaging. Solid dark matte and common palette applied only for GIF encoding.','png_frames':40,'unique_png_frames':len(set(raw)),'checks':checks}
(root/'evidence'/'gif-verification.json').write_text(json.dumps(evidence,indent=2),encoding='utf-8')
print(json.dumps(evidence))
