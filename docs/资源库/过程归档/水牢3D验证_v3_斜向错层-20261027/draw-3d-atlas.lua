local root="D:/alianximod/docs/资源库/过程归档/水牢3D验证_v3_斜向错层-20261027/"
app.fs.makeAllDirectories(root)
local W,H,N=256,128,16
local pc=app.pixelColor
local function col(r,g,b,a) return pc.rgba(r,g,b,a or 255) end
local function px(im,x,y,c) x=math.floor(x+.5);y=math.floor(y+.5);if y>=0 and y<H then im:drawPixel(x%W,y,c) end end
local function line(im,x0,y0,x1,y1,c,w)
 local n=math.max(math.abs(x1-x0),math.abs(y1-y0))*2
 for i=0,n do
  local t=n>0 and i/n or 0
  local xx=x0+(x1-x0)*t;local yy=y0+(y1-y0)*t
  for dy=-math.ceil(w),math.ceil(w) do for dx=-math.ceil(w),math.ceil(w) do
   if dx*dx+dy*dy<=w*w then px(im,xx+dx,yy+dy,c) end
  end end
 end
end
local s=Sprite(W,H,ColorMode.RGB)
local layers={s.layers[1],s:newLayer(),s:newLayer(),s:newLayer()}
for i,name in ipairs({"01 椭球水膜与厚底","02 稀疏水壁压力折光","03 束缚水流横向展开","04 水流滑动亮纹"}) do layers[i].name=name end
local atlas=Image(W,H*N,ColorMode.RGB)
for f=0,N-1 do
 if f>0 then s:newEmptyFrame() end
 s.frames[f+1].duration=.1
 local ims={Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB)}
 local p=f*math.pi*2/N
 for y=0,95 do for x=0,255 do
  local a=35
  local rr,gg,bb=24,149,197
  if y>70 then
   local edge=75+2*math.sin(x*2*math.pi/256*3)+math.sin(x*2*math.pi/256*7)
   if y>edge then a=154;rr=17;gg=111;bb=163 end
   if y>edge-2 and y<=edge then a=180;rr=53;gg=183;bb=209 end
   if y>88 then a=178;rr=13;gg=91;bb=148 end
  end
  ims[1]:drawPixel(x,y,col(rr,gg,bb,a))
 end end
 -- Long sparse light planes curve with the sphere. No painted circular rim.
 for k=0,3 do
  local sx=20+k*64
  for y=14,72 do
   local a=(y-14)/58
   local xx=sx+6*math.sin(a*math.pi*1.3+k*.6)
   for j=0,2 do px(ims[2],xx+j,y,col(20,123,177,65)) end
   if (y>=19 and y<29) or (y>=41 and y<49 and k%2==0) then
    px(ims[2],xx-1,y,col(85,204,226,110))
    if y%7<3 then px(ims[2],xx,y,col(144,230,237,142)) end
   end
  end
 end
 -- Curved bottom eddies move locally; the top/bottom poles stay quiet.
 for k=0,7 do
  local xx=k*32+f*2
  line(ims[2],xx,80+math.sin(k)*2,xx+8,78+math.sin(k)*2,col(50,178,208,152),.55)
 end
 -- Ribbon occupies y=96..127; width/taper come from the 3D mesh.
 for y=96,127 do for x=0,255 do
  local v=y-96
  local c=col(30,151,198,138)
  if v<3 then c=col(80,203,222,156)
  elseif v<8 then c=col(52,183,211,149)
  elseif v>25 then c=col(9,88,145,156)
  elseif v>20 then c=col(17,116,171,148) end
  ims[3]:drawPixel(x,y,c)
 end end
 for k=0,3 do
  local xx=(k*64+f*16)%256
  line(ims[4],xx,100+k%3,xx+8+k*3,99+k%3,col(123,226,236,182),.6)
  line(ims[4],xx+12+k*3,100+k%3,xx+17+k*3,102+k%3,col(76,206,228,154),.55)
  line(ims[4],xx+26,118,xx+34,117,col(39,166,207,156),.6)
 end
 for i=1,4 do s:newCel(layers[i],f+1,ims[i],Point(0,0)) end
 local im=Image(W,H,ColorMode.RGB);im:drawSprite(s,f+1)
 atlas:drawImage(im,Point(0,f*H))
 if f==0 then im:saveAs(root.."water_prison_3d_atlas_frame01.png") end
end
s:newTag(1,N).name="水牢水膜与束缚流_16帧"
s:saveAs(root.."water_prison_3d_atlas.aseprite")
atlas:saveAs(root.."water_prison_3d_atlas.png")
print("Aseprite: 256x128, 4 layers, 16x100ms; shell 256x96 + ribbon 256x32.")
