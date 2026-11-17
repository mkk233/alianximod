
local root=app.params["out"] or "D:/alianximod/docs/资源库/过程归档/水牢泡泡_2D_v1-20261027/"
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
local layers={s.layers[1],s:newLayer(),s:newLayer(),s:newLayer(),s:newLayer()}
local names={"01 淡青透明水膜","02 闭合薄壳与蓝青短暗边","03 缓缓流动的折射水纹","04 不对称白青高光","05 贴膜小水珠"}
for i,l in ipairs(layers) do l.name=names[i] end
local atlas=Image(W,H*N,ColorMode.RGB)
local frames={}
for f=0,N-1 do
 if f>0 then s:newEmptyFrame() end
 s.frames[f+1].duration=.1
 local phase=f*2*math.pi/N
 local ims={}
 for i=1,5 do ims[i]=Image(W,H,ColorMode.RGB) end
 -- Fixed silhouette, discrete tone bands. No soft blur / antialiasing.
 for y=0,H-1 do for x=0,W-1 do
  local nx=(x-47.5)/39.5;local ny=(y-47.5)/41.5
  local r=math.sqrt(nx*nx+ny*ny)
  if r<=1 then
   local depth=(1-r)*40
   local angle=math.atan(ny,nx)
   local a=18
   if r>.78 then a=27 elseif r>.55 then a=22 end
   ims[1]:drawPixel(x,y,col(63,182,217,a))
   local band=1.7+.35*math.sin(angle*3+.7)
   if depth<.8 then
    local bright=ny<-.3 or (nx<-.7 and ny<.2)
    ims[2]:drawPixel(x,y,bright and col(86,198,220,160) or col(36,137,181,136))
   elseif depth<band then
    ims[2]:drawPixel(x,y,col(100,220,233,112))
   elseif depth<band+1.4 and (nx<-.3 or ny>.4) then
    ims[2]:drawPixel(x,y,col(53,167,207,64))
   end
  end
 end end
 -- Broken highlights anchored to the shell; only a tiny surface slide.
 local drift=1.1*math.sin(phase)
 local glow=round(190+18*math.cos(phase))
 arc(ims[4],35.7,37.4,196+drift,220+drift,2.6,col(192,247,250,glow))
 arc(ims[4],35.7,37.4,229+drift,249+drift,2.3,col(222,255,253,glow+25))
 arc(ims[4],34.8,36.5,234+drift,247+drift,1.1,col(246,255,252,234))
 arc(ims[4],36.7,38.5,29-drift,48-drift,1.9,col(185,245,246,184))
 arc(ims[4],36.3,38.2,58-drift,66-drift,1.2,col(226,255,252,200))
 arc(ims[4],38,40,286,301,1.1,col(142,234,244,117))
 arc(ims[2],38.1,40.1,113,130,1.3,col(23,105,155,144))
 arc(ims[2],38.1,40.1,5,18,1.0,col(31,116,166,121))
 -- Asymmetric membrane ribbons, not full concentric rings.
 local dy=1.4*math.sin(phase)
 curve(ims[3],{{52,18+dy},{69,16+dy},{80,29},{76,39+dy}},1.6,col(122,225,239,round(72+15*math.cos(phase))))
 curve(ims[3],{{56,21+dy},{68,21},{74,28+dy},{75,34+dy}},1.1,col(217,253,251,57))
 curve(ims[3],{{22,58-dy},{23,75-dy},{39,81},{53,74-dy}},1.5,col(109,220,236,round(66-12*math.cos(phase))))
 curve(ims[3],{{26,63-dy},{30,73},{42,76},{46,74}},1.0,col(216,250,249,60))
 curve(ims[3],{{36,39},{44,34+dy},{50,35+dy},{58,39}},1.0,col(141,231,240,round(32+10*math.sin(phase))))
 -- A few angular water facets near the shell.
 stroke(ims[3],{{18,47+dy},{17,51+dy},{19,55+dy}},1.4,col(111,223,235,82))
 stroke(ims[3],{{64,78-dy},{69,75-dy},{71,72-dy}},1.2,col(105,213,234,92))
 -- Adhering droplets move locally; no orbit, no detached splash.
 for _,p in ipairs({{71,62,1.5,1},{28,72,1.25,2.2},{69,29,1.05,4}}) do
  local xx=p[1]+.7*math.sin(phase+p[4])
  local yy=p[2]+1.3*math.sin(phase+p[4]+.4)
  disc(ims[5],xx,yy,p[3]+.6,p[3]+.75,col(40,147,190,106))
  disc(ims[5],xx,yy,p[3],p[3]+.15,col(133,230,240,111))
  px(ims[5],xx-.5,yy-.7,col(230,255,252,172))
 end
 for i=1,5 do s:newCel(layers[i],f+1,ims[i],Point(0,0)) end
 local merged=Image(W,H,ColorMode.RGB)
 merged:drawSprite(s,f+1)
 atlas:drawImage(merged,Point(0,f*H))
 frames[f+1]=merged
end
local tag=s:newTag(1,N);tag.name="水膜流动_循环";tag.aniDir=AniDir.FORWARD
s.data="2D水牢泡泡草稿；固定闭合轮廓；中心半透明；只作美术预览，未接入游戏。"
s:saveAs(root.."water_prison_bubble_v1.aseprite")
frames[1]:saveAs(root.."water_prison_bubble_v1.png")
atlas:saveAs(root.."water_prison_bubble_v1_strip.png")
local large=Image(frames[1]);large:resize(W*4,H*4);large:saveAs(root.."water_prison_bubble_v1_4x.png")
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
print("Created 96x96, 16 frames, 100ms, 5 editable layers; preview 896x448")
