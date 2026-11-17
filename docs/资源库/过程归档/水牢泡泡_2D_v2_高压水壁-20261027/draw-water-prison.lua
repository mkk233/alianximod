
local root=app.params["out"] or "D:/alianximod/docs/资源库/过程归档/水牢泡泡_2D_v2_高压水壁-20261027/"
app.fs.makeAllDirectories(root)
local W,H,N=96,96,16
local pc=app.pixelColor
local function col(r,g,b,a) return pc.rgba(r,g,b,a or 255) end
local function round(x) return math.floor(x+.5) end
local function px(im,x,y,c)
 x=round(x);y=round(y)
 if x>=0 and y>=0 and x<im.width and y<im.height then im:drawPixel(x,y,c) end
end
local function disc(im,cx,cy,rx,ry,c)
 for y=math.floor(cy-ry),math.ceil(cy+ry) do
  for x=math.floor(cx-rx),math.ceil(cx+rx) do
   if ((x-cx)/rx)^2+((y-cy)/ry)^2<=1 then px(im,x,y,c) end
  end
 end
end
local function stroke(im,points,width,c)
 for i=1,#points-1 do
  local a,b=points[i],points[i+1]
  local n=math.max(1,math.ceil(math.max(math.abs(b[1]-a[1]),math.abs(b[2]-a[2]))*2))
  for j=0,n do
   local t=j/n
   local x=a[1]+(b[1]-a[1])*t;local y=a[2]+(b[2]-a[2])*t
   disc(im,x,y,width/2,width/2,c)
  end
 end
end
local function arc(im,rx,ry,first,last,width,c,dx,dy)
 local pts={}
 for k=0,math.ceil((last-first)*2) do
  local a=(first+(last-first)*k/math.ceil((last-first)*2))*math.pi/180
  pts[#pts+1]={47.5+(dx or 0)+rx*math.cos(a),47.5+(dy or 0)+ry*math.sin(a)}
 end
 stroke(im,pts,width,c)
end
local function curve(im,p,width,c)
 local pts={}
 for i=0,56 do
  local t=i/56;local q=1-t
  pts[#pts+1]={q*q*q*p[1][1]+3*q*q*t*p[2][1]+3*q*t*t*p[3][1]+t*t*t*p[4][1],
   q*q*q*p[1][2]+3*q*q*t*p[2][2]+3*q*t*t*p[3][2]+t*t*t*p[4][2]}
 end
 stroke(im,pts,width,c)
end
local s=Sprite(W,H,ColorMode.RGB)
local layers={s.layers[1],s:newLayer(),s:newLayer(),s:newLayer(),s:newLayer(),s:newLayer()}
local names={"01 透明封闭水体","02 厚重高压水壁","03 下缘积水与回流","04 交错束缚水流","05 水壁受压亮边","06 流动短纹"}
for i,l in ipairs(layers) do l.name=names[i] end
local atlas=Image(W,H*N,ColorMode.RGB)
local frames={}
local function ribbonPart(im,p,t0,t1,width,c)
 local pts={}
 for j=0,24 do
  local t=t0+(t1-t0)*j/24; local q=1-t
  pts[#pts+1]={q*q*q*p[1][1]+3*q*q*t*p[2][1]+3*q*t*t*p[3][1]+t*t*t*p[4][1],
    q*q*q*p[1][2]+3*q*q*t*p[2][2]+3*q*t*t*p[3][2]+t*t*t*p[4][2]}
 end
 stroke(im,pts,width,c)
end
for f=0,N-1 do
 if f>0 then s:newEmptyFrame() end
 s.frames[f+1].duration=.1
 local phase=f*2*math.pi/N
 local ims={}
 for i=1,6 do ims[i]=Image(W,H,ColorMode.RGB) end
 -- The enclosing silhouette never inflates. Dense liquid has a dark outer
 -- pressure edge, a saturated core, and a pale inner lip.
 for y=0,H-1 do for x=0,W-1 do
  local nx=(x-47.5)/39.5;local ny=(y-47.5)/41.5
  local r=math.sqrt(nx*nx+ny*ny)
  if r<=1 then
   local depth=(1-r)*40
   local angle=math.atan(ny,nx)
   local low=math.max(0,ny)
   local wall=3.2+.5*math.sin(3*angle+.7)+1.4*low*low
   local film=r>.76 and 45 or (r>.53 and 37 or 30)
   ims[1]:drawPixel(x,y,col(24,149,197,film))
   if depth<1 then
    ims[2]:drawPixel(x,y,col(12,76,119,215))
   elseif depth<wall-1.4 then
    ims[2]:drawPixel(x,y,col(17,124,174,205))
   elseif depth<wall then
    ims[2]:drawPixel(x,y,col(51,186,211,185))
   elseif depth<wall+1.2 then
    ims[2]:drawPixel(x,y,col(27,135,181,112))
   end
   local pool=4.8+3.6*low*low*low*low
   if ny>.42 and depth<pool and depth>1.1 then
    local edge=pool-depth
    if edge<1 then
     ims[3]:drawPixel(x,y,col(71,201,218,197))
    elseif edge<2.1 then
     ims[3]:drawPixel(x,y,col(28,157,191,212))
    else
     ims[3]:drawPixel(x,y,col(13,101,155,217))
    end
   end
  end
 end end
 local dy=1.3*math.sin(phase)
 local dz=1.15*math.sin(phase+1.2)
 -- Slanted water currents bind across the torso, merging into the walls.
 -- They remain translucent, unequal, and curved rather than geometric bars.
 local a={{12,36},{30,37+dy},{51,65+dy},{83,55}}
 local b={{15,63},{34,68-dz},{61,35-dz},{83,39}}
 curve(ims[4],a,5.6,col(15,96,150,132))
 curve(ims[4],{{12,35},{30,36+dy},{51,64+dy},{83,54}},3.8,col(47,169,204,119))
 curve(ims[4],b,4.9,col(15,105,158,126))
 curve(ims[4],{{15,62},{34,67-dz},{61,34-dz},{83,38}},3.1,col(53,180,210,117))
 -- Rear shoulder current only appears against the side, leaving face readable.
 curve(ims[4],{{62,16},{76,21+dz},{82,27},{83,35}},3.4,col(36,147,190,112))
 curve(ims[4],{{12,48},{18,38-dy},{19,28},{26,20}},2.5,col(35,156,192,111))
 -- Short, sharp reflected pressure points; no glistening droplets.
 local drift=.55*math.sin(phase)
 arc(ims[5],36.6,38.5,207+drift,221+drift,1.8,col(163,235,239,219))
 arc(ims[5],36.1,38,240+drift,252+drift,1.8,col(188,247,246,224))
 arc(ims[5],36.3,38.2,29-drift,38-drift,1.6,col(143,224,232,208))
 arc(ims[5],37.5,39.6,288,306,1.2,col(61,190,218,180))
 -- Surface streaks advect along the binding currents. Short runs fade at
 -- their ends so the loop doesn't snap when the leading run wraps.
 for k=0,2 do
  local t=(f/N+k/3)%1
  local lo=.06+t*.78
  local alpha=round(95+50*math.sin(math.pi*t))
  ribbonPart(ims[6],a,lo,math.min(.96,lo+.10),1.2,col(116,218,233,alpha))
  local u=(1-t)*.78+.06
  ribbonPart(ims[6],b,u,math.min(.96,u+.075),1.0,col(115,222,231,alpha-13))
 end
 -- Bottom currents run the other way, inside the heavy curved reservoir.
 for k=0,2 do
  local t=(f/N+k/3)%1
  local aa=132-t*84
  arc(ims[6],34.9,35.8,aa,aa+9,1.5,col(54,181,210,round(135+30*math.sin(math.pi*t))))
  arc(ims[3],36.2,37.2,aa-5,aa+2,1.3,col(8,77,131,189))
 end
 -- A compact side eddy suggests internal hydraulic pressure.
 curve(ims[6],{{18,65},{20,70+dy},{27,73},{30,71}},1.2,col(71,190,214,146))
 -- Enforce a fixed silhouette on every editable layer.
 for y=0,H-1 do for x=0,W-1 do
  if ((x-47.5)/39.5)^2+((y-47.5)/41.5)^2>1 then
   for i=1,6 do ims[i]:drawPixel(x,y,0) end
  end
 end end
 for i=1,6 do s:newCel(layers[i],f+1,ims[i],Point(0,0)) end
 local merged=Image(W,H,ColorMode.RGB)
 merged:drawSprite(s,f+1)
 atlas:drawImage(merged,Point(0,f*H))
 frames[f+1]=merged
end
local tag=s:newTag(1,N);tag.name="高压水牢_束缚回流";tag.aniDir=AniDir.FORWARD
s.data="水牢2D草稿v2；厚壁、交错束缚水流、下缘积水；96x96，16x100ms。待确认，未接入游戏。"
s:saveAs(root.."water_prison_bubble_v2.aseprite")
frames[1]:saveAs(root.."water_prison_bubble_v2.png")
atlas:saveAs(root.."water_prison_bubble_v2_strip.png")
local large=Image(frames[1]);large:resize(W*4,H*4);large:saveAs(root.."water_prison_bubble_v2_4x.png")
-- Dark / light backdrop preview, drawn and composited in Aseprite.
local preview=Sprite(224,112,ColorMode.RGB)
preview.layers[1].name="深浅底与灰色方块目标_仅展示"
local fx=preview:newLayer();fx.name="水牢泡泡预览"
local bg=Image(224,112,ColorMode.RGB)
bg:clear(col(40,49,62))
local function rect(im,x,y,w,h,c)
 for yy=y,y+h-1 do for xx=x,x+w-1 do im:drawPixel(xx,yy,c) end end
end
local function dummy(im,ox,oy)
 local outline=col(57,69,77);local dark=col(94,108,115);local mid=col(146,158,160);local light=col(183,193,189)
 rect(im,ox+39,oy+17,18,18,outline);rect(im,ox+40,oy+18,16,16,mid)
 rect(im,ox+41,oy+18,13,3,light);rect(im,ox+53,oy+21,3,12,dark)
 rect(im,ox+43,oy+26,2,2,outline);rect(im,ox+50,oy+26,2,2,outline)
 rect(im,ox+39,oy+35,18,24,outline);rect(im,ox+40,oy+35,16,23,dark)
 rect(im,ox+41,oy+36,13,3,mid);rect(im,ox+40,oy+40,3,17,mid)
 rect(im,ox+31,oy+36,7,24,outline);rect(im,ox+32,oy+37,5,21,mid)
 rect(im,ox+58,oy+36,7,24,outline);rect(im,ox+59,oy+37,5,21,dark)
 rect(im,ox+39,oy+59,8,21,outline);rect(im,ox+40,oy+59,6,19,mid)
 rect(im,ox+49,oy+59,8,21,outline);rect(im,ox+50,oy+59,6,19,dark)
 rect(im,ox+40,oy+77,6,2,light);rect(im,ox+50,oy+77,6,2,mid)
end
for side=0,1 do
 local ox=8+side*112
 rect(bg,ox-4,4,104,104,side==0 and col(22,36,51) or col(219,227,223))
 for y=90,106 do for x=ox-4,ox+99 do
  local checker=(math.floor((x-ox)/8)+math.floor((y-90)/8))%2
  bg:drawPixel(x,y,side==0 and (checker==0 and col(26,44,59) or col(30,48,63)) or (checker==0 and col(195,209,202) or col(205,217,211)))
 end end
 disc(bg,ox+47.5,101,27,3,side==0 and col(15,26,37) or col(161,181,176))
 dummy(bg,ox,8)
end
for f=1,N do
 if f>1 then preview:newEmptyFrame() end
 preview.frames[f].duration=.1
 preview:newCel(preview.layers[1],f,bg,Point(0,0))
 local overlay=Image(224,112,ColorMode.RGB)
 overlay:drawImage(frames[f],Point(8,8));overlay:drawImage(frames[f],Point(120,8))
 preview:newCel(fx,f,overlay,Point(0,0))
end
preview:saveAs(root.."water_prison_bubble_preview.aseprite")
local still=Image(preview);still:resize(896,448);still:saveAs(root.."water_prison_bubble_preview_4x.png")
preview:resize(896,448)
preview:saveCopyAs(root.."water_prison_bubble_preview_4x.gif")
print("Created 96x96, 16 frames, 100ms, 6 editable layers; preview 896x448")
