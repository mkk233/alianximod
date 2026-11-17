local out=args.out
app.fs.makeAllDirectories(out)
local W,H,N=64,64,16
local rgba=app.pixelColor.rgba
local floor,sin,pi=math.floor,math.sin,math.pi
local function round(v)return floor(v+0.5)end
local function px(im,x,y,c)
  x,y=round(x),round(y)
  if x>=0 and y>=0 and x<im.width and y<im.height then im:drawPixel(x,y,c)end
end
local function rect(im,x1,y1,x2,y2,c)
  for y=round(y1),round(y2)do for x=round(x1),round(x2)do px(im,x,y,c)end end
end
local function oval(im,cx,cy,rx,ry,c)
  for y=floor(cy-ry),math.ceil(cy+ry)do
    for x=floor(cx-rx),math.ceil(cx+rx)do
      if ((x-cx)/rx)^2+((y-cy)/ry)^2<=1 then px(im,x,y,c)end
    end
  end
end
local function plume(im,f,cfg)
  local x0,y0,rise,drift,delay,strength,seed=table.unpack(cfg)
  local t=(f-delay)/(N-1-delay)
  if t<=0 or t>=1 then return end
  local fade=math.min(1,t*6)*(1-t^1.7)*strength
  local a=round(220*fade)
  if a<2 then return end
  local x=x0+drift*t+sin(t*pi*2+seed)*1.15
  local y=y0-rise*(0.10+0.90*t^0.8)
  local rx=1.7+t*3.3
  local ry=1.6+t*1.8
  local tmp=Image(W,H,ColorMode.RGB)
  local rim=rgba(128,173,184,round(a*0.66))
  local mid=rgba(190,219,224,round(a*0.90))
  local lit=rgba(236,246,245,round(a*0.90))
  local tailY=y0-rise*t*0.36
  local tailX=x0+drift*t*0.35
  if t<0.72 then
    for k=0,round(tailY-y)do
      local q=k/math.max(1,tailY-y)
      local yy=tailY-k
      local xx=tailX+(x-tailX)*q+sin(q*pi*2+seed+t*3)*0.8
      local width=0.7+q*0.8
      oval(tmp,xx,yy,width,1.1,rim)
      if k%3~=0 then px(tmp,xx,yy,mid)end
    end
  end
  oval(tmp,x,y,rx,ry,rim)
  oval(tmp,x-rx*0.60,y-ry*0.45,rx*0.68,ry*0.84,rim)
  oval(tmp,x+rx*0.55,y-ry*0.70,rx*0.64,ry*0.74,rim)
  oval(tmp,x,y-0.4,rx*0.80,ry*0.66,mid)
  oval(tmp,x-rx*0.50,y-ry*0.50,rx*0.48,ry*0.58,lit)
  oval(tmp,x+rx*0.40,y-ry*0.70,rx*0.44,ry*0.47,lit)
  if t>0.54 then
    local gapX=round(x+sin(seed)*rx*0.45)
    rect(tmp,gapX,y,gapX,y+ry,rgba(0,0,0,0))
    if t>0.76 then rect(tmp,x-rx,y+1,x+rx,y+ry,rgba(0,0,0,0))end
  end
  im:drawImage(tmp,Point(0,0))
end
local rearConfig={
  {19,43,25,-4,0,0.85,0.3},
  {45,41,24,4,1,0.85,1.9},
  {26,56,29,-4,2,0.90,3.0},
  {38,56,28,4,3,0.90,4.8},
  {24,29,15,-3,1,0.65,2.5},
  {40,29,16,3,3,0.65,5.2}
}
local frontConfig={
  {23,40,20,-1.5,1,0.60,1.1},
  {41,40,21,1.5,2,0.60,4.2},
  {31,42,21,1.5,4,0.50,2.6},
  {27,54,21,-1,1,0.60,3.8},
  {37,54,20,1,3,0.60,5.5}
}
local s=Sprite(W,H,ColorMode.RGB)
s.layers[1].name='01 rear rising vapour'
local front=s:newLayer();front.name='02 front light wisps'
local specks=s:newLayer();specks.name='03 dissipating fragments'
for f=0,N-1 do
  local frame=f==0 and s.frames[1]or s:newEmptyFrame()
  frame.duration=0.075
  local images={Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB)}
  for _,c in ipairs(rearConfig)do plume(images[1],f,c)end
  for _,c in ipairs(frontConfig)do plume(images[2],f,c)end
  if f>=7 and f<=13 then
    local a=round(95*(14-f)/7)
    for i=1,6 do
      local side=i%2==0 and 1 or -1
      local x=32+side*(13+(i%3)*2)+round(sin(f*0.22+i))
      local y=42-(f-7)*2-(i%3)*7
      rect(images[3],x,y,x+1,y,rgba(208,232,235,a))
    end
  end
  for li=1,3 do
    local cel=s.layers[li]:cel(frame)
    if cel then cel.image=images[li]else s:newCel(s.layers[li],frame,images[li],Point(0,0))end
  end
end
s:saveAs(out..'/cool_v2.aseprite')
local strip=Image(W,H*N,ColorMode.RGB)
for fi=1,N do
  local im=Image(W,H,ColorMode.RGB)
  im:drawSprite(s,fi)
  strip:drawImage(im,Point(0,(fi-1)*H))
end
strip:saveAs(out..'/cool_v2_strip.png')
for li,slug in ipairs({'rear','front','fragments'})do
  local layerStrip=Image(W,H*N,ColorMode.RGB)
  for fi=1,N do layerStrip:drawImage(s.layers[li]:cel(fi).image,Point(0,(fi-1)*H))end
  layerStrip:saveAs(out..'/cool_v2_'..slug..'_strip.png')
end
local dummy=Image(W,H,ColorMode.RGB)
local skin=rgba(124,145,154,255)
local body=rgba(66,84,99,255)
local limb=rgba(50,68,83,255)
rect(dummy,26,8,37,19,skin)
rect(dummy,26,8,27,19,rgba(101,123,137,255))
rect(dummy,28,13,29,14,rgba(38,53,65,255))
rect(dummy,34,13,35,14,rgba(38,53,65,255))
rect(dummy,26,20,37,37,body)
rect(dummy,20,20,25,33,body)
rect(dummy,38,20,43,33,body)
rect(dummy,20,34,25,37,skin)
rect(dummy,38,34,43,37,skin)
rect(dummy,26,38,30,55,limb)
rect(dummy,33,38,37,55,limb)
rect(dummy,26,38,27,53,rgba(76,95,107,255))
rect(dummy,33,38,34,53,rgba(76,95,107,255))
local glyphs={
 C={'111','100','100','100','111'},O={'111','101','101','101','111'},
 L={'100','100','100','100','111'},V={'101','101','101','101','010'},
 ['2']={'110','001','010','100','111'},['/']={'001','001','010','100','100'},
 D={'110','101','101','101','110'},A={'010','101','111','101','101'},
 R={'110','101','110','101','101'},K={'101','101','110','101','101'},
 I={'111','010','010','010','111'},G={'111','100','101','101','111'},
 H={'101','101','111','101','101'},T={'111','010','010','010','010'}
}
local function text(im,str,x,y,color)
  for i=1,#str do
    local g=glyphs[str:sub(i,i)]
    if g then for yy=1,5 do for xx=1,3 do
      if g[yy]:sub(xx,xx)=='1'then px(im,x+(i-1)*4+xx-1,y+yy-1,color)end
    end end end
  end
end
local pw,ph=176,100
local preview=Sprite(pw,ph,ColorMode.RGB)
preview.layers[1].name='二维审稿板_人物与深浅背景'
for fi=1,N do
  local frame=fi==1 and preview.frames[1]or preview:newEmptyFrame()
  frame.duration=fi==N and 0.45 or 0.075
  local im=Image(pw,ph,ColorMode.RGB)
  rect(im,0,0,pw-1,ph-1,rgba(13,22,31,255))
  text(im,'COOL / V2',8,6,rgba(196,230,236,255))
  for panel=1,2 do
    local x=panel==1 and 4 or 92
    local bg=panel==1 and rgba(27,42,53,255)or rgba(180,197,202,255)
    rect(im,x,17,x+79,95,bg)
    text(im,panel==1 and 'DARK'or 'LIGHT',x+5,21,
      panel==1 and rgba(119,151,165,255)or rgba(65,91,100,255))
    local pos=Point(x+8,32)
    rect(im,x+17,88,x+63,88,panel==1 and rgba(57,77,88,255)or rgba(133,157,165,255))
    im:drawImage(s.layers[1]:cel(fi).image,pos)
    im:drawImage(dummy,pos)
    im:drawImage(s.layers[2]:cel(fi).image,pos)
    im:drawImage(s.layers[3]:cel(fi).image,pos)
  end
  local cel=preview.layers[1]:cel(frame)
  if cel then cel.image=im else preview:newCel(preview.layers[1],frame,im,Point(0,0))end
end
preview:saveAs(out..'/cool_v2_preview.aseprite')
app.sprite=preview
app.command.SpriteSize{ui=false,width=pw*4,height=ph*4,method='nearest'}
local still=Image(preview.width,preview.height,ColorMode.RGB)
still:drawSprite(preview,7)
still:saveAs(out..'/cool_v2_preview.png')
preview:saveAs(out..'/cool_v2_preview.gif')
return {source=out..'/cool_v2.aseprite',preview=out..'/cool_v2_preview.gif',
 still=out..'/cool_v2_preview.png',width=W,height=H,frames=N,frameMs=75,
 sourceDurationMs=1200,previewClearHoldMs=450}
