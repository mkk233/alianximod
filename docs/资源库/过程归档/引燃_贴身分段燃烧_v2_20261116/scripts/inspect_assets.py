from pathlib import Path
import json, hashlib, base64, io
from PIL import Image

root=Path(__file__).resolve().parents[1]
rows=[]
for p in sorted(root.rglob('*')):
    if not p.is_file() or p.name=='manifest.json': continue
    data=p.read_bytes()
    row={'path':p.as_posix(),'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest().upper()}
    if p.suffix.lower() in {'.png','.gif'}:
        with Image.open(p) as im:
            row.update(width=im.width,height=im.height,frames=getattr(im,'n_frames',1))
            if p.suffix.lower()=='.png':
                values=sorted(set(im.convert('RGBA').getchannel('A').getdata()))
                row['alpha_binary']=all(v in {0,255} for v in values)
                if len(values)<=2: row['alpha_values']=values
            else:
                durations=[]
                for frame in range(im.n_frames):
                    im.seek(frame);durations.append(im.info.get('duration',0))
                row['durations_ms']=durations
                row['total_duration_ms']=sum(durations)
    rows.append(row)
book=Path('D:/alianximod/docs/术式制作表.xlsx')
model=json.loads((root/'blockbench/fire_kindle_v2_candidate_preview.bbmodel').read_text(encoding='utf-8'))
embedded=[]
for tex in model.get('textures',[]):
    source=tex.get('source','')
    item={'name':tex.get('name'),'path':tex.get('path'),'width':tex.get('width'),'height':tex.get('height')}
    if source.startswith('data:'):
        raw=base64.b64decode(source.split(',',1)[1]);item['embedded_sha256']=hashlib.sha256(raw).hexdigest().upper()
        item['embedded_bytes']=len(raw)
        with Image.open(io.BytesIO(raw)) as embedded_image, Image.open(root/'textures'/tex['name']) as exported_image:
            e=embedded_image.convert('RGBA').tobytes();f=exported_image.convert('RGBA').tobytes()
            item['embedded_rgba_sha256']=hashlib.sha256(e).hexdigest().upper()
            item['export_rgba_sha256']=hashlib.sha256(f).hexdigest().upper()
            item['same_rgba_pixels']=e==f
    embedded.append(item)
print(json.dumps({'files':rows,'workbook_sha256_after':hashlib.sha256(book.read_bytes()).hexdigest().upper(),'model_embedded_textures':embedded},ensure_ascii=False,indent=2))
