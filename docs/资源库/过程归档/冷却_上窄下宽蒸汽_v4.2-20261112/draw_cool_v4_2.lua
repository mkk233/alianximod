local out=args.out
app.fs.makeAllDirectories(out)
app.fs.makeAllDirectories(out.."/frames")
local rgba=app.pixelColor.rgba
local floor,sin,pi=math.floor,math.sin,math.pi
local function round(v)return floor(v+0.5)end
local function clamp(v,a,b)return math.max(a,math.min(b,v))end
local function px(im,x,y,c)
  x,y=round(x),round(y)
  if x>=0 and y>=0 and x<im.width and y<im.height then im:drawPixel(x,y,c)end
end
local function rect(im,x0,y0,x1,y1,c)
  for y=round(y0),round(y1)do for x=round(x0),round(x1)do px(im,x,y,c)end end
end
local function oval(im,cx,cy,rx,ry,c)
  for y=floor(cy-ry),math.ceil(cy+ry)do for x=floor(cx-rx),math.ceil(cx+rx)do
    if ((x-cx)/rx)^2+((y-cy)/ry)^2<=1 then px(im,x,y,c)end
  end end
end
local function put(s,l,fi,im)
  local cel=l:cel(fi)
  if cel then cel.image=im;cel.position=Point(0,0)
  else s:newCel(l,fi,im,Point(0,0))end
end
local W,H,N=64,64,16
local rearConfig={
  {29,20,-3,5,0,11,0.70,0.3,0.60},{35,20,3,5,0,11,0.70,2.1,0.60},
  {27,28,-5,5,2,10,0.80,1.0,0.78},{37,28,5,5,2,10,0.80,3.2,0.78},
  {24,37,-7,5,4,9,0.90,2.0,0.94},{40,37,7,5,4,9,0.90,4.1,0.94},
  {20,45,-9,4,6,8,0.94,3.0,1.08},{44,45,9,4,6,8,0.94,5.2,1.08},
  {16,53,-11,3,8,7,0.96,4.0,1.16},{48,53,11,3,8,7,0.96,6.1,1.16}
}
local frontConfig={
  {30,23,-2,4,0,11,0.74,2.1,0.72},{34,23,2,4,0,11,0.74,4.1,0.72},
  {28,31,-3.5,4,2,10,0.58,1.2,0.76},{36,31,3.5,4,2,10,0.58,3.2,0.76},
  {26,40,-5,4,4,9,0.56,0.3,0.90},{38,40,5,4,4,9,0.56,2.3,0.90},
  {22,48,-7,3,6,8,0.52,3.4,1.02},{42,48,7,3,6,8,0.52,5.4,1.02},
  {19,55,-8,2,8,7,0.48,2.5,1.12},{45,55,8,2,8,7,0.48,4.5,1.12}
}
local function vent(im,fragments,f,cfg)
  local x0,y0,drift,drop,delay,life,strength,seed,scale=table.unpack(cfg)
  local t=(f-delay)/life
  if t<=0 or t>=1 then return end
  local side=drift<0 and -1 or 1
  local fade=clamp(t*4.5,0,1)*(1-clamp((t-0.44)/0.56,0,1)^1.4)*strength
  local alpha=round(220*fade)
  if alpha<2 then return end
  local x=x0+drift*t^1.15
  local y=y0+drop*t
  local rx,ry=(2.0+2.6*t)*scale,(1.9+0.55*sin(t*pi))*scale
  local rim=rgba(127,175,187,round(alpha*0.76))
  local mid=rgba(190,220,226,round(alpha*0.91))
  local lit=rgba(236,247,246,round(alpha*0.90))
  local tmp=Image(W,H,ColorMode.RGB)
  if t<0.76 then
    for k=0,round(math.abs(x-x0))do
      local q=k/math.max(1,math.abs(x-x0))
      local xx=x0+side*k
      local yy=y0+(y-y0)*q+sin(q*pi+seed)*0.5
      oval(tmp,xx,yy,0.85,0.6+q*0.8,rim)
      if k%4~=0 then px(tmp,xx,yy,mid)end
    end
  end
  oval(tmp,x,y,rx,ry,rim)
  oval(tmp,x+side*rx*0.60,y-ry*0.50,rx*0.65,ry*0.79,rim)
  oval(tmp,x-side*rx*0.45,y+ry*0.42,rx*0.65,ry*0.75,rim)
  oval(tmp,x+side*0.5,y-0.2,rx*0.84,ry*0.64,mid)
  oval(tmp,x+side*rx*0.60,y-ry*0.50,rx*0.46,ry*0.53,lit)
  oval(tmp,x-side*rx*0.15,y-ry*0.30,rx*0.52,ry*0.55,lit)
  if t>0.54 then
    local gap=round(x-side*rx*0.30)
    rect(tmp,gap,y,gap,y+ry,rgba(0,0,0,0))
  end
  im:drawImage(tmp,Point(0,0))
  if t>0.45 then
    local a=round(alpha*0.5)
    rect(fragments,x+side*(rx+1.8),y+0.7,
      x+side*(rx+1.8)+1,y+0.7,rgba(207,233,236,a))
    if t>0.65 then px(fragments,x-side*rx*1.5,y+2,rgba(174,212,223,a))end
  end
end
local vapour=Sprite(W,H,ColorMode.RGB)
vapour.layers[1].name="01 rear vents - shoulders to ankles"
vapour:newLayer().name="02 front vents - descending release"
vapour:newLayer().name="03 outward and downward fragments"
local vframes={{},{},{}}
for f=0,N-1 do
  local frame=f==0 and vapour.frames[1]or vapour:newEmptyFrame()
  frame.duration=0.075
  local ims={Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB)}
  for _,cfg in ipairs(rearConfig)do vent(ims[1],ims[3],f,cfg)end
  for _,cfg in ipairs(frontConfig)do vent(ims[2],ims[3],f,cfg)end
  for li=1,3 do put(vapour,vapour.layers[li],f+1,ims[li]);vframes[li][f+1]=ims[li]end
end
vapour:saveAs(out.."/cool_v4_2_vapour.aseprite")
local oldWave=app.open(args.waveBase)
local wave=Sprite(96,96,ColorMode.RGB)
wave.layers[1].name="01 translucent trailing vapour"
wave:newLayer().name="02 broken pale pressure rim"
wave:newLayer().name="03 outward dispersing flecks"
local ringFrames={}
for f=0,N-1 do
  local frame=f==0 and wave.frames[1]or wave:newEmptyFrame()
  frame.duration=0.075
  for li=1,3 do
    local im=Image(96,96,ColorMode.RGB)
    if f>=6 and f<=14 then
      local cel=oldWave.layers[li]:cel(f-4)
      if cel then im:drawImage(cel.image,cel.position)end
    end
    put(wave,wave.layers[li],f+1,im)
  end
  local im=Image(96,96,ColorMode.RGB);im:drawSprite(wave,f+1)
  ringFrames[f+1]=im
  im:saveAs(out..string.format("/frames/wave_%02d.png",f+1))
end
wave:saveAs(out.."/cool_v4_2_shockwave.aseprite")
local function export(s,slug)
  local strip=Image(s.width,s.height*N,ColorMode.RGB)
  for fi=1,N do
    local im=Image(s.width,s.height,ColorMode.RGB);im:drawSprite(s,fi)
    strip:drawImage(im,Point(0,(fi-1)*s.height))
  end
  strip:saveAs(out.."/cool_v4_2_"..slug.."_strip.png")
end
export(vapour,"vapour");export(wave,"shockwave")
for li,slug in ipairs({"rear","front","fragments"})do
  local strip=Image(W,H*N,ColorMode.RGB)
  for fi=1,N do
    strip:drawImage(vframes[li][fi],Point(0,(fi-1)*H))
    vframes[li][fi]:saveAs(out..string.format("/frames/%s_%02d.png",slug,fi))
  end
  strip:saveAs(out.."/cool_v4_2_"..slug.."_strip.png")
end
local dummy=Image(W,H,ColorMode.RGB)
local skin,body,limb=rgba(124,145,154,255),rgba(66,84,99,255),rgba(50,68,83,255)
rect(dummy,26,8,37,19,skin);rect(dummy,26,8,27,19,rgba(101,123,137,255))
rect(dummy,28,13,29,14,rgba(38,53,65,255));rect(dummy,34,13,35,14,rgba(38,53,65,255))
rect(dummy,26,20,37,37,body)
rect(dummy,20,20,25,33,body);rect(dummy,38,20,43,33,body)
rect(dummy,20,34,25,37,skin);rect(dummy,38,34,43,37,skin)
rect(dummy,26,38,30,55,limb);rect(dummy,33,38,37,55,limb)
rect(dummy,26,38,27,53,rgba(76,95,107,255));rect(dummy,33,38,34,53,rgba(76,95,107,255))
local glyphs={ ["2"]={"110","001","010","100","111"}, ["1"]={"010","110","010","010","111"}, ["."]={"000","000","000","000","010"},
 C={"111","100","100","100","111"},O={"111","101","101","101","111"},
 L={"100","100","100","100","111"},V={"101","101","101","101","010"},
 ["4"]={"101","101","111","001","001"},["/"]={"001","001","010","100","100"},
 D={"110","101","101","101","110"},A={"010","101","111","101","101"},
 R={"110","101","110","101","101"},K={"101","101","110","101","101"},
 I={"111","010","010","010","111"},G={"111","100","101","101","111"},
 H={"101","101","111","101","101"},T={"111","010","010","010","010"},
 W={"101","101","101","111","101"},E={"111","100","110","100","111"},
 S={"111","100","111","001","111"},P={"110","101","110","100","100"},
 M={"101","111","111","101","101"}
}
local function text(im,str,x,y,c)
  for i=1,#str do
    local g=glyphs[str:sub(i,i)]
    if g then for yy=1,5 do for xx=1,3 do
      if g[yy]:sub(xx,xx)=="1"then px(im,x+(i-1)*4+xx-1,y+yy-1,c)end
    end end end
  end
end
local function projectedRing(src,front)
  local im=Image(104,38,ColorMode.RGB)
  for y=0,37 do for x=0,103 do
    local sy=floor((y-18)/0.36+47.5)
    local sx=x-4
    if sx>=0 and sx<96 and sy>=0 and sy<96 and((front and sy>=48)or(not front and sy<48))then
      im:drawPixel(x,y,src:getPixel(sx,sy))
    end
  end end
  return im
end
local pw,ph=252,116
local preview=Sprite(pw,ph,ColorMode.RGB)
preview.layers[1].name="2D review - steam first, top-to-bottom vents, delayed wave"
for fi=1,N do
  local frame=fi==1 and preview.frames[1]or preview:newEmptyFrame()
  frame.duration=0.075
  local im=Image(pw,ph,ColorMode.RGB)
  rect(im,0,0,pw-1,ph-1,rgba(12,22,30,255))
  text(im,"COOL / V4.2",8,6,rgba(201,233,239,255))
  text(im,fi<=6 and "STEAM"or(fi<=15 and "WAVE"or " "),pw-29,6,rgba(111,175,193,255))
  for panel=1,2 do
    local x=panel==1 and 4 or 130
    rect(im,x,17,x+117,ph-5,panel==1 and rgba(27,42,53,255)or rgba(180,197,202,255))
    text(im,panel==1 and "DARK"or "LIGHT",x+6,23,
      panel==1 and rgba(119,151,165,255)or rgba(65,91,100,255))
    local pos=Point(x+27,37)
    rect(im,x+19,93,x+99,93,panel==1 and rgba(57,77,88,255)or rgba(133,157,165,255))
    local wavePos=Point(x+7,73)
    im:drawImage(projectedRing(ringFrames[fi],false),wavePos)
    im:drawImage(vframes[1][fi],pos);im:drawImage(dummy,pos)
    im:drawImage(vframes[2][fi],pos);im:drawImage(vframes[3][fi],pos)
    im:drawImage(projectedRing(ringFrames[fi],true),wavePos)
  end
  put(preview,preview.layers[1],fi,im)
end
preview:saveAs(out.."/cool_v4_2_preview.aseprite")
local board=Image(pw*3,ph*2,ColorMode.RGB)
for k,fi in ipairs({3,5,7,9,12,15})do
  local im=Image(pw,ph,ColorMode.RGB);im:drawSprite(preview,fi)
  board:drawImage(im,Point(((k-1)%3)*pw,floor((k-1)/3)*ph))
end
board:resize{width=pw*6,height=ph*4,method="nearest"}
board:saveAs(out.."/cool_v4_2_keyframes.png")
app.sprite=preview
app.command.SpriteSize{ui=false,width=pw*4,height=ph*4,method="nearest"}
local still=Image(preview.width,preview.height,ColorMode.RGB);still:drawSprite(preview,9)
still:saveAs(out.."/cool_v4_2_preview.png")
for i,frame in ipairs(preview.frames)do frame.duration=i==16 and 0.45 or(i%2==1 and 0.07 or 0.08)end
preview:saveAs(out.."/cool_v4_2_preview.gif")
return{frames=16,frameMs=75,durationMs=1200,steamFirstVisibleMs=75,
 waveFirstVisibleMs=450,steamDirection="top-to-bottom, outward and downward, narrow top and wide triangular base",
 preview=out.."/cool_v4_2_preview.gif"}



