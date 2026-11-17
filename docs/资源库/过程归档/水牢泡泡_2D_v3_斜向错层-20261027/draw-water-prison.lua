
local root=app.params["out"] or "D:/alianximod/docs/资源库/过程归档/水牢泡泡_2D_v3_斜向错层-20261027/"
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
local names={"01 透明封闭水体","02 厚重高压水壁","03 下缘积水与回流","04 斜向错层束缚水流","05 水壁受压亮边","06 流动短纹"}
for i,l in ipairs(layers) do l.name=names[i] end
local atlas=Image(W,H*N,ColorMode.RGB)
local frames={}

-- Unequal, tapering currents with short rounded transitions.
local function sampleCurrent(nodes,phase,amp)
 local out={}
 for i=1,#nodes-1 do
  local p0=nodes[math.max(1,i-1)]
  local p1=nodes[i];local p2=nodes[i+1]
  local p3=nodes[math.min(#nodes,i+2)]
  for j=0,24 do
   local t=j/24;local t2=t*t;local t3=t2*t
   local u=((i-1)+t)/(#nodes-1)
   local function cr(a,b,c,d)
    return .5*((2*b)+(-a+c)*t+(2*a-5*b+4*c-d)*t2+(-a+3*b-3*c+d)*t3)
   end
   local xx=cr(p0[1],p1[1],p2[1],p3[1])
   local yy=cr(p0[2],p1[2],p2[2],p3[2])
   yy=yy+amp*math.sin(phase+u*3.2)*math.sin(math.pi*u)
   local width=p1[3]+(p2[3]-p1[3])*(t*t*(3-2*t))
   out[#out+1]={xx,yy,width,u}
  end
 end
 return out
end
local function paintCurrent(im,path,shade,core)
 for _,p in ipairs(path) do disc(im,p[1],p[2]+.3,p[3]*.5,p[3]*.5,shade) end
 for _,p in ipairs(path) do disc(im,p[1],p[2]-.35,p[3]*.34,p[3]*.34,core) end
end
local function movingAccent(im,path,start,span,c,width)
 local pts={}
 for _,p in ipairs(path) do
  if p[4]>=start and p[4]<=start+span then
   pts[#pts+1]={p[1],p[2]-.7}
  end
 end
 if #pts>1 then stroke(im,pts,width,c) end
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

 local dy=.7*math.sin(phase)
 -- Main stream sweeps from a broad lower-left anchor to a narrow upper-right
 -- tail. Short streams turn back before the centre: no central X junction.
 local a=sampleCurrent({{13,62,2},{29,60,6},{48,52,5},{69,43,3},{83,41,.8}},phase,.65)
 local b=sampleCurrent({{12,36,2.5},{18,37,4},{25,39,5},{32,46,3},{39,49,.7}},phase+1.4,.55)
 local c=sampleCurrent({{34,74,.7},{48,69,3},{61,66,4},{72,59,.7}},phase+2.5,.5)
 paintCurrent(ims[4],c,col(17,104,159,102),col(41,164,202,89))
 paintCurrent(ims[4],b,col(13,103,153,125),col(43,177,208,111))
 paintCurrent(ims[4],a,col(12,95,150,151),col(44,177,211,137))
 -- Uneven secondary water tails preserve liquid character at the sharp ends.
 curve(ims[4],{{70,44},{75,45},{78,43},{81,43}},1.0,col(52,176,209,101))
 curve(ims[4],{{32,46},{34,50},{36,51},{38,51}},1.0,col(40,160,202,80))
 -- Side return is kept against the enclosing wall to retain containment.
 curve(ims[4],{{77,28},{81,32},{83,35},{83,40}},1.8,col(32,151,192,99))
 -- Short, sharp reflected pressure points; no glistening droplets.
 local drift=.55*math.sin(phase)
 arc(ims[5],36.6,38.5,207+drift,221+drift,1.8,col(163,235,239,219))
 arc(ims[5],36.1,38,240+drift,252+drift,1.8,col(188,247,246,224))
 arc(ims[5],36.3,38.2,29-drift,38-drift,1.6,col(143,224,232,208))
 arc(ims[5],37.5,39.6,288,306,1.2,col(61,190,218,180))

 -- Only short highlights travel along each stream; the composition remains fixed.
 for k=0,1 do
  local t=(f/N+k*.5)%1
  local lo=.04+t*.81
  local visibility=math.sin(math.pi*t)
  movingAccent(ims[6],a,lo,.105,col(126,226,235,round(45+113*visibility)),1.2)
 end
 local tb=(f/N+.3)%1
 movingAccent(ims[6],b,.08+tb*.71,.13,col(107,213,229,round(30+99*math.sin(math.pi*tb))),1.1)
 local tc=(1-f/N+.1)%1
 movingAccent(ims[6],c,.09+tc*.69,.12,col(83,204,224,round(25+90*math.sin(math.pi*tc))),1.0)
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
local tag=s:newTag(1,N);tag.name="高压水牢_斜向错层回流";tag.aniDir=AniDir.FORWARD
s.data="水牢2D草稿v3；厚壁、斜向错层束缚水流、下缘积水；96x96，16x100ms。待确认，未接入游戏。"
s:saveAs(root.."water_prison_bubble_v3.aseprite")
frames[1]:saveAs(root.."water_prison_bubble_v3.png")
atlas:saveAs(root.."water_prison_bubble_v3_strip.png")
local large=Image(frames[1]);large:resize(W*4,H*4);large:saveAs(root.."water_prison_bubble_v3_4x.png")
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
