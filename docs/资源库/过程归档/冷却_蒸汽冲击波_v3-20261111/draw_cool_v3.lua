local out=args.out
app.fs.makeAllDirectories(out)
app.fs.makeAllDirectories(out.."/frames")
local rgba=app.pixelColor.rgba
local floor,sin,cos,pi=math.floor,math.sin,math.cos,math.pi
local function round(x)return floor(x+0.5)end
local function clamp(x,a,b)return math.max(a,math.min(b,x))end
local function rect(im,x0,y0,x1,y1,c)
  for y=math.max(0,round(y0)),math.min(im.height-1,round(y1))do
    for x=math.max(0,round(x0)),math.min(im.width-1,round(x1))do im:drawPixel(x,y,c)end
  end
end
local function put(s,layer,fi,im)
  local cel=layer:cel(fi)
  if cel then cel.image=im;cel.position=Point(0,0)
  else s:newCel(layer,fi,im,Point(0,0))end
end
local N=16
local vapour=app.open(args.base)
vapour:saveAs(out.."/cool_v3_vapour.aseprite")
local wave=Sprite(96,96,ColorMode.RGB)
wave.layers[1].name="01 translucent trailing vapour"
wave:newLayer().name="02 broken pale pressure rim"
wave:newLayer().name="03 outward dispersing flecks"
local ringFrames={}
for f=0,N-1 do
  local frame=f==0 and wave.frames[1]or wave:newEmptyFrame()
  frame.duration=0.075
  local ims={Image(96,96,ColorMode.RGB),Image(96,96,ColorMode.RGB),Image(96,96,ColorMode.RGB)}
  if f>=1 and f<=9 then
    local t=(f-1)/9
    local r=6+39*(1-(1-t)^1.55)
    local fade=(1-t)^0.65
    local thick=2.7-1.4*t
    for y=1,94 do for x=1,94 do
      local dx,dy=x-47.5,y-47.5
      local a=math.atan(dy,dx)
      local dist=math.sqrt(dx*dx+dy*dy)
      local section=floor((a+pi)/(2*pi)*72)
      local noise=(section*37+section*section*11)%101/100
      local breakage=clamp((t-0.28)*0.9,0,0.62)
      local scallop=0.65*sin(a*11+t*3)+0.4*sin(a*23-t*2)
      local d=dist-(r+scallop)
      if noise>breakage and d<0.65 and d> -thick then
        local alpha=round(235*fade)
        local col=rgba(226,250,250,alpha)
        if d< -thick*0.56 then col=rgba(139,200,217,round(alpha*0.88))end
        if d>0.0 then col=rgba(90,158,180,round(alpha*0.72))end
        ims[2]:drawPixel(x,y,col)
      end
      local trail=dist-(r-3.8-0.8*sin(a*8))
      if noise>breakage+0.12 and trail<0 and trail> -2.8 then
        ims[1]:drawPixel(x,y,rgba(172,214,225,round(85*fade*(0.65+0.35*sin(a*5)^2))))
      end
    end end
    if f>=3 then
      for k=0,13 do
        local a=k*2*pi/14+0.08*sin(k*8)
        local rr=r+2.5+t*4+(k%3)*0.6
        local x,y=47.5+cos(a)*rr,47.5+sin(a)*rr
        if (k+f)%4~=0 then
          local c=rgba(204,238,241,round(135*fade))
          rect(ims[3],x,y,x+(k%2),y,c)
        end
      end
    end
  end
  for i=1,3 do put(wave,wave.layers[i],f+1,ims[i])end
  local im=Image(96,96,ColorMode.RGB);im:drawSprite(wave,f+1)
  ringFrames[f+1]=im
  im:saveAs(out..string.format("/frames/wave_%02d.png",f+1))
end
wave:saveAs(out.."/cool_v3_shockwave.aseprite")
local function export(s,name)
  local strip=Image(s.width,s.height*N,ColorMode.RGB)
  for fi=1,N do
    local im=Image(s.width,s.height,ColorMode.RGB);im:drawSprite(s,fi)
    strip:drawImage(im,Point(0,(fi-1)*s.height))
  end
  strip:saveAs(out.."/"..name.."_strip.png")
end
export(wave,"cool_v3_shockwave")
export(vapour,"cool_v3_vapour")
local vframes={}
for li,slug in ipairs({"rear","front","fragments"})do
  local strip=Image(64,64*N,ColorMode.RGB)
  vframes[li]={}
  for fi=1,N do
    local im=Image(64,64,ColorMode.RGB)
    local cel=vapour.layers[li]:cel(fi)
    if cel then im:drawImage(cel.image,cel.position)end
    vframes[li][fi]=im
    strip:drawImage(im,Point(0,(fi-1)*64))
    im:saveAs(out..string.format("/frames/%s_%02d.png",slug,fi))
  end
  strip:saveAs(out.."/cool_v3_"..slug.."_strip.png")
end
local dummy=Image(64,64,ColorMode.RGB)
local skin,body,limb=rgba(124,145,154,255),rgba(66,84,99,255),rgba(50,68,83,255)
rect(dummy,26,8,37,19,skin)
rect(dummy,26,8,27,19,rgba(101,123,137,255))
rect(dummy,28,13,29,14,rgba(38,53,65,255))
rect(dummy,34,13,35,14,rgba(38,53,65,255))
rect(dummy,26,20,37,37,body)
rect(dummy,20,20,25,33,body);rect(dummy,38,20,43,33,body)
rect(dummy,20,34,25,37,skin);rect(dummy,38,34,43,37,skin)
rect(dummy,26,38,30,55,limb);rect(dummy,33,38,37,55,limb)
rect(dummy,26,38,27,53,rgba(76,95,107,255));rect(dummy,33,38,34,53,rgba(76,95,107,255))
local glyphs={
 C={"111","100","100","100","111"},O={"111","101","101","101","111"},
 L={"100","100","100","100","111"},V={"101","101","101","101","010"},
 ["3"]={"110","001","010","001","110"},["/"]={"001","001","010","100","100"},
 D={"110","101","101","101","110"},A={"010","101","111","101","101"},
 R={"110","101","110","101","101"},K={"101","101","110","101","101"},
 I={"111","010","010","010","111"},G={"111","100","101","101","111"},
 H={"101","101","111","101","101"},T={"111","010","010","010","010"},
 W={"101","101","101","111","101"},E={"111","100","110","100","111"},
 S={"111","100","111","001","111"},P={"110","101","110","100","100"}
}
local function text(im,s,x,y,c)
  for i=1,#s do
    local g=glyphs[s:sub(i,i)]
    if g then for yy=1,5 do for xx=1,3 do
      if g[yy]:sub(xx,xx)=="1"then im:drawPixel(x+(i-1)*4+xx-1,y+yy-1,c)end
    end end end
  end
end
local function projectedRing(src,front)
  local im=Image(104,38,ColorMode.RGB)
  for y=0,37 do for x=0,103 do
    local sy=floor((y-18)/0.36+47.5)
    local sx=x-4
    if sx>=0 and sx<96 and sy>=0 and sy<96 and ((front and sy>=48)or(not front and sy<48))then
      im:drawPixel(x,y,src:getPixel(sx,sy))
    end
  end end
  return im
end
local pw,ph=252,116
local preview=Sprite(pw,ph,ColorMode.RGB)
preview.layers[1].name="2D review only - projected wave, player reference"
for fi=1,N do
  local frame=fi==1 and preview.frames[1]or preview:newEmptyFrame()
  frame.duration=fi==N and 0.45 or 0.075
  local im=Image(pw,ph,ColorMode.RGB)
  rect(im,0,0,pw-1,ph-1,rgba(12,22,30,255))
  text(im,"COOL / V3",8,6,rgba(201,233,239,255))
  text(im,"WAVE",pw-24,6,rgba(111,175,193,255))
  for panel=1,2 do
    local x=panel==1 and 4 or 130
    rect(im,x,17,x+117,ph-5,panel==1 and rgba(27,42,53,255)or rgba(180,197,202,255))
    text(im,panel==1 and "DARK"or "LIGHT",x+6,23,
      panel==1 and rgba(119,151,165,255)or rgba(65,91,100,255))
    local pos=Point(x+27,37)
    rect(im,x+19,93,x+99,93,panel==1 and rgba(57,77,88,255)or rgba(133,157,165,255))
    local wavePos=Point(x+7,60)
    im:drawImage(projectedRing(ringFrames[fi],false),wavePos)
    im:drawImage(vframes[1][fi],pos)
    im:drawImage(dummy,pos)
    im:drawImage(vframes[2][fi],pos);im:drawImage(vframes[3][fi],pos)
    im:drawImage(projectedRing(ringFrames[fi],true),wavePos)
  end
  put(preview,preview.layers[1],fi,im)
end
preview:saveAs(out.."/cool_v3_preview.aseprite")
app.sprite=preview
app.command.SpriteSize{ui=false,width=pw*4,height=ph*4,method="nearest"}
local still=Image(preview.width,preview.height,ColorMode.RGB);still:drawSprite(preview,6)
still:saveAs(out.."/cool_v3_preview.png")
preview:saveAs(out.."/cool_v3_preview.gif")
return {source=out.."/cool_v3_vapour.aseprite",wave=out.."/cool_v3_shockwave.aseprite",
 preview=out.."/cool_v3_preview.gif",frames=N,frameMs=75,durationMs=1200,
 waveVisibleMs=675,baseVapourUnchanged=true}
