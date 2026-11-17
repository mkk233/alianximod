from pathlib import Path
import json, math, zipfile, random
from PIL import Image, ImageDraw, ImageFont
OUT=Path(__file__).resolve().parent
ROOT=Path('D:/alianximod')
DATA=json.loads((OUT/'gpt-layout-export-20261114.json').read_text(encoding='utf-8'))
NODES=DATA['nodes']; ROUTES=DATA['routes']
ASSET=ROOT/'src/main/resources/assets/xueximod/textures/gui/imprint_desk'
sheet=Image.open(ASSET/'tree_nodes_sheet.png').convert('RGBA')
altar=Image.open(ASSET/'altar_hub.png').convert('RGBA')
pip=sheet.crop((114,26,122,34))
colors=['#5cc64c','#f07e26','#3e9ef2','#b0733a','#deb24c','#9474ce','#cc964a']
assets=Path.home()/'.gradle/caches/forge_gradle/assets'
idx=json.loads((assets/'indexes/5.json').read_text(encoding='utf-8'))['objects']
h=idx['minecraft/font/unifont.zip']['hash']
glyphs={}; needed={ord(c) for n in NODES for c in n['name']}|{ord(c) for c in '参悟原胚刻印归位背包'}
with zipfile.ZipFile(assets/'objects'/h[:2]/h) as z:
    for file in z.namelist():
        if not file.endswith('.hex'): continue
        for row in z.read(file).decode().splitlines():
            k,bits=row.split(':'); code=int(k,16)
            if code in needed:
                mask=Image.new('L',(16,16)); d=ImageDraw.Draw(mask); stride=len(bits)//16
                for y in range(16):
                    number=int(bits[y*stride:(y+1)*stride],16)
                    for x in range(16):
                        if number&(1<<(15-x)): d.point((x,y),fill=255)
                glyphs[code]=mask
def label(img,name,x,y,color,scale):
    # Match Minecraft's 16px CJK bitmap at oversample=2, advance=9 GUI px.
    for c in name:
        mask=glyphs[ord(c)].resize((8*scale,8*scale),Image.Resampling.NEAREST)
        img.paste(color,(round(x),round(y)),mask); x+=9*scale
def clip(a,b,box):
    l,t,r,bot=box; dx=b[0]-a[0];dy=b[1]-a[1];u=0;v=1
    for p,q in [(-dx,a[0]-l),(dx,r-a[0]),(-dy,a[1]-t),(dy,bot-a[1])]:
        if abs(p)<1e-9:
            if q<0:return None
        else:
            h=q/p
            if p<0:u=max(u,h)
            else:v=min(v,h)
            if u>v:return None
    return u,v
def render(size,zoom,center,scale=2,bounds=False):
    w,h=size; image=Image.new('RGBA',(w*scale,h*scale),'#070a12');draw=ImageDraw.Draw(image)
    rng=random.Random(104)
    for _ in range(w*h//220):
        px=rng.randrange(w)*scale;py=rng.randrange(h)*scale
        draw.rectangle((px,py,px+scale-1,py+scale-1),fill=rng.choice(['#323d5b','#4c5172','#705384']))
    cx,cy=center
    def screen(x,y):return(cx+x*zoom,cy+y*zoom)
    for route in ROUTES:
        pts=list(zip(route['x'],route['y']))
        points=[screen(x,y) for x,y in pts]
        if route['parent']==-1:points[0]=(cx+DATA['anchors'][route['group']][0],cy+DATA['anchors'][route['group']][1])
        color=colors[route['group']]
        boxes=[(cx-88,cy-92,cx+88,cy+92)]
        for i in [route['parent'],route['child']]:
            if i<0:continue
            n=NODES[i];x,y=screen(n['x'],n['y']);hw=max(16,n['width']/2+3)
            boxes.append((x-hw,y-18,x+hw,y+26))
        for a,b in zip(points,points[1:]):
            intervals=[(0,1)]
            for box in boxes:
                hit=clip(a,b,box)
                if not hit:continue
                new=[]
                for u,v in intervals:
                    if hit[1]<=u or hit[0]>=v:new.append((u,v));continue
                    if hit[0]>u:new.append((u,hit[0]))
                    if hit[1]<v:new.append((hit[1],v))
                intervals=new
            for u,v in intervals:
                p=(round((a[0]+(b[0]-a[0])*u)*scale),round((a[1]+(b[1]-a[1])*u)*scale))
                q=(round((a[0]+(b[0]-a[0])*v)*scale),round((a[1]+(b[1]-a[1])*v)*scale))
                draw.line((p,q),fill=color,width=max(1,scale))
        for p in points[1:-1]:
            blocked=False
            for n in NODES:
                nx,ny=screen(n['x'],n['y']); hw=max(17,n['width']/2+4)
                if p[0]+4>=nx-hw and p[0]-4<=nx+hw and p[1]+4>=ny-18 and p[1]-4<=ny+25:blocked=True;break
            if not blocked:image.alpha_composite(pip.resize((8*scale,8*scale),Image.Resampling.NEAREST),(round((p[0]-4)*scale),round((p[1]-4)*scale)))
    for n in NODES:
        x,y=screen(n['x'],n['y']); tile=sheet.crop((n['group']*24,0,n['group']*24+22,22))
        image.alpha_composite(tile.resize((22*scale,22*scale),Image.Resampling.NEAREST),(round((x-11)*scale),round((y-11)*scale)))
        icon=ROOT/('src/main/resources/assets/xueximod/textures/gui/technique/'+('attack' if n['type']=='SWORD' else 'other')+'_'+n['attribute'].lower()+'.png')
        # Resource icon lookup below is replaced by the exact production fallback mapping.
        if icon.exists():image.alpha_composite(Image.open(icon).convert('RGBA').resize((16*scale,16*scale),Image.Resampling.NEAREST),(round((x-8)*scale),round((y-9)*scale)))
        label(image,n['name'],(x-n['width']/2)*scale,(y+13)*scale,'#eceef6',scale)
        if bounds:
            hw=max(17,n['width']/2+4)
            draw.rectangle(((x-hw)*scale,(y-18)*scale,(x+hw)*scale,(y+25)*scale),outline='#ce5c77',width=1)
    image.alpha_composite(altar.resize((184*scale,192*scale),Image.Resampling.NEAREST),(round((cx-92)*scale),round((cy-96)*scale)))
    # Actual v1 overlays, followed by the same 10px content scissor and 8px nine-slice frame.
    button=Image.open(ASSET/'confirm_button_states.png').convert('RGBA').crop((0,40,60,60))
    image.alpha_composite(button.resize((60*scale,20*scale),Image.Resampling.NEAREST),(round((cx-30)*scale),round(cy*scale)))
    label(image,'原胚刻印',(cx-18)*scale,(cy-35)*scale,'#eceef6',scale)
    label(image,'刻印',(cx-9)*scale,(cy+6)*scale,'#828094',scale)
    # The viewport mask also clips names and stone corners at its inner edge.
    draw=ImageDraw.Draw(image)
    for box in [(0,0,w*scale,10*scale),(0,(h-10)*scale,w*scale,h*scale),(0,0,10*scale,h*scale),((w-10)*scale,0,w*scale,h*scale)]:draw.rectangle(box,fill='#070a12')
    border=Image.open(ASSET/'screen_border_9slice.png').convert('RGBA')
    for xi,(px,pw,u,uw) in enumerate([(0,8,0,8),(8,w-16,8,8),(w-8,8,16,8)]):
        for yi,(py,ph,v,vh) in enumerate([(0,8,0,8),(8,h-16,8,8),(h-8,8,16,8)]):
            if xi==1 and yi==1:continue
            image.alpha_composite(border.crop((u,v,u+uw,v+vh)).resize((pw*scale,ph*scale),Image.Resampling.NEAREST),(px*scale,py*scale))
    corner=Image.open(ASSET/'corner_button_states.png').convert('RGBA').crop((0,0,52,18))
    for text,x in [('归位',w-118),('背包',w-60)]:
        image.alpha_composite(corner.resize((52*scale,18*scale),Image.Resampling.NEAREST),(x*scale,8*scale))
        label(image,text,(x+17)*scale,13*scale,'#eceef6',scale)

    return image.convert('RGB')

if __name__=='__main__':
    full=render((1280,1160),1,(660,480),2)
    full.save(OUT/'gpt-layout-overview-20261114.png')
    default=render((640,360),1,(320,180),2)
    default.save(OUT/'gpt-layout-default-20261114.png')
    minimum=render((1000,880),.75,(520,360),2)
    minimum.save(OUT/'gpt-layout-minzoom-20261114.png')
    audit=Image.new('RGB',(full.width+default.width,full.height),'#101322')
    audit.paste(full,(0,0));audit.paste(default,(full.width,44));audit.paste(minimum.resize((1280,1126),Image.Resampling.NEAREST),(full.width,820))
    d=ImageDraw.Draw(audit); f=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',24)
    d.text((full.width+16,8),'实际 Java 布局 / 默认 zoom=1.00 / 640×360 GUI',font=f,fill='white')
    d.text((full.width+16,780),'全览 zoom=0.75 / 1000×880 GUI',font=f,fill='white')
    d.text((full.width+16,2010),'全部 85 个节点；复用实际贴图；离线视口模拟',font=f,fill='white')
    d.text((full.width+16,2050),'不是游戏截图；状态统一显示为已解锁',font=f,fill='white')
    audit.save(OUT/'gpt-layout-audit-20261114.png')
    reserve=render((1440,1440),1,(720,720),2).convert('RGBA'); layer=Image.new('RGBA',reserve.size); rd=ImageDraw.Draw(layer)
    f=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',25)
    counts={}
    def within(g,x,y):
        angle=math.degrees(math.atan2(y,x))
        while angle<DATA['sectorStart'][g]:angle+=360
        return angle<=DATA['sectorEnd'][g]
    for g,title in [(0,'木'),(3,'土'),(4,'金')]:
        start,end=DATA['sectorStart'][g],DATA['sectorEnd'][g]
        def pt(a,r):return (round((720+math.cos(math.radians(a))*r)*2),round((720+math.sin(math.radians(a))*r)*2))
        poly=[pt(a,690) for a in range(int(start),int(end)+1)]+[pt(a,340) for a in range(int(end),int(start)-1,-1)]
        color=tuple(int(colors[g][i:i+2],16) for i in (1,3,5))
        rd.polygon(poly,fill=(*color,22))
        for a in [start,end]:rd.line([pt(a,170),pt(a,690)],fill=(*color,140),width=2)
        count=0
        # Empty diagnostic slots: six-CJK label envelope at MIN_ZOOM, 88px lattice.
        # No fake technique is included in the 85-node data or production code.
        for y in range(-616,617,88):
            for x in range(-616,617,88):
                if not 360<=math.hypot(x,y)<=650:continue
                if not all(within(g,x+dx,y+dy) for dx in [-31/.75,31/.75] for dy in [-18/.75,25/.75]):continue
                if any(not(abs(x-n['x'])*.75>=31+max(17,n['width']/2+4)+4 or (y-n['y'])*.75>=47 or(n['y']-y)*.75>=47) for n in NODES):continue
                sx,sy=(720+x)*2,(720+y)*2
                rd.rectangle((sx-22,sy-22,sx+22,sy+22),outline=(*color,130),width=2)
                rd.line((sx-36,sy+34,sx+36,sy+34),fill=(*color,90),width=2)
                count+=1
        counts[title]=count
        tx,ty=pt((start+end)/2,390)
        rd.rounded_rectangle((tx-160,ty-38,tx+160,ty+40),radius=7,fill=(7,10,18,240))
        rd.text((tx-146,ty-30),f'{title}：{int(end-start)}° 持续扩展扇区',font=f,fill=(*color,255))
        rd.text((tx-146,ty+4),f'示意空位 {count} 个（非实际术式）',font=f,fill=(235,238,248,255))
    reserve=Image.alpha_composite(reserve,layer)
    reserve.convert('RGB').save(OUT/'gpt-layout-reserved-20261114.png')
    (OUT/'gpt-layout-reserved-20261114.txt').write_text(str(counts)+'\nTemplate: 6 CJK characters, 54px Font.width; frame+badge+name padded envelope at zoom=.75; slots are illustrative, not added techniques.\n',encoding='utf-8')
    print(DATA['audit']);print('Reserved illustrative slots:',counts)
