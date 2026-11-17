from PIL import Image,ImageDraw
from pathlib import Path
import json,hashlib
root=Path(__file__).resolve().parents[1]
frames=[Image.open(root/'exports'/'frames'/f'dust_{i:02d}.png').convert('RGBA') for i in range(1,17)]
bg=Image.new('RGBA',(96,32))
for y in range(32):
    for x in range(96): bg.putpixel((x,y),(43,48,51,255) if (x//4+y//4)%2 else (56,61,63,255))
preview=[Image.alpha_composite(bg,im).convert('RGB').resize((768,256),Image.Resampling.NEAREST) for im in frames]
preview[0].save(root/'previews'/'ground_dust_v2-2d.gif',save_all=True,append_images=preview[1:],duration=[50]*16,loop=0,optimize=False)
preview[4].save(root/'previews'/'ground_dust_v2-2d-peak.png')
sheet=Image.new('RGB',(768,256*4),(36,41,43))
for i,f in enumerate([1,4,9,14]): sheet.paste(preview[f],(0,256*i))
sheet.save(root/'previews'/'ground_dust_v2-2d-contact.png')
stats=[{'frame':i+1,'start_ms':50*i,'alpha_pixels':sum(1 for a in im.getchannel('A').getdata() if a),'max_alpha':max(im.getchannel('A').getdata()),'bbox':im.getbbox(),'sha256':hashlib.sha256(im.tobytes()).hexdigest()} for i,im in enumerate(frames)]
(root/'evidence'/'2d-frame-inspection.json').write_text(json.dumps({'frames':stats,'frame_count':16,'duration_ms':800,'last_frame_clear':frames[-1].getbbox() is None,'unique_rgba_frames':len({s['sha256'] for s in stats})},indent=2),encoding='utf-8')
print(json.dumps(stats))
