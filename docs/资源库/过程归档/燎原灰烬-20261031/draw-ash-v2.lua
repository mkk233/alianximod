
local dir="D:/alianximod/docs/资源库/过程归档/燎原灰烬-20261031/"
local rgba=app.pixelColor.rgba
local function rgb(r,g,b) return rgba(r,g,b,255) end
local function hash(x,y,s) return ((x*374761+y*668265+s*982451)%104729)%100 end
local base=Image(64,64,ColorMode.RGB)
local pal={rgb(35,35,34),rgb(42,42,40),rgb(48,48,45),rgb(55,55,51),rgb(64,63,58),rgb(73,72,66)}
for y=0,63 do for x=0,63 do
 local coarse=hash(math.floor(x/3),math.floor(y/3),6)
 local fine=hash(x,y,11)
 local k=2+math.floor(coarse/30)
 if fine<14 then k=k-1 elseif fine>90 then k=k+1 end
 base:drawPixel(x,y,pal[math.max(1,math.min(6,k))])
end end
local function pix(im,x,y,c) im:drawPixel(x%64,y%64,c) end
local function line(im,x0,y0,x1,y1,c)
 local n=math.max(math.abs(x1-x0),math.abs(y1-y0))
 for t=0,n do pix(im,math.floor(x0+(x1-x0)*t/math.max(1,n)+0.5),math.floor(y0+(y1-y0)*t/math.max(1,n)+0.5),c) end
end
local paths={
 {{-3,10},{2,12},{7,11},{10,14},{14,14},{17,18},{20,18}},
 {{8,41},{12,40},{14,36},{18,34},{21,35},{25,31}},
 {{29,5},{30,10},{34,13},{33,17},{36,20}},
 {{42,28},{46,26},{50,28},{53,32},{58,31},{64,34}},
 {{28,55},{32,51},{36,52},{39,48},{42,48}},
 {{54,51},{51,55},{52,59},{49,64}},
 {{3,57},{7,56},{10,59},{10,64}}
}
for _,p in ipairs(paths) do for i=1,#p-1 do
 local a,b=p[i],p[i+1]
 line(base,a[1],a[2]+1,b[1],b[2]+1,rgb(72,70,63))
 line(base,a[1],a[2],b[1],b[2],rgb(25,25,24))
end end
-- Short charcoal flakes and ash chips; all wrap over tile borders.
for i=1,70 do
 local x=(i*29+7)%64 local y=(i*43+math.floor(i/5)*7)%64
 local c=(i%3==0) and rgb(83,80,71) or rgb(31,32,31)
 pix(base,x,y,c) pix(base,x+1,y,c)
 if i%4==0 then pix(base,x+1,y+1,c) end
end
-- Keep original static ash base unchanged.
local spr=Sprite(64,64,ColorMode.RGB)
spr.layers[1].name="Continuous charcoal ash"
local layer=spr:newLayer() layer.name="Sparse staggered embers"
local sparks=Sprite(64,64,ColorMode.RGB)
sparks.layers[1].name="Embers only"
local combined={} local emberimgs={}
local points={{5,12,0},{17,35,9},{34,14,15},{51,29,4},{32,52,11},{52,57,19},{23,23,3},{44,7,14},{10,48,21},{60,43,7}}
for f=1,24 do
 if f>1 then spr:newEmptyFrame() sparks:newEmptyFrame() end
 spr.frames[f].duration=0.05 sparks.frames[f].duration=0.05
 spr:newCel(spr.layers[1],f,base,Point(0,0))
 local e=Image(64,64,ColorMode.RGB)
 for i,p in ipairs(points) do
  local t=(f-1+p[3])%24
  local heat=(1+math.cos(t*math.pi/12))/2
  if heat>0.15 then
   local r=math.floor(130+heat*125) local g=math.floor(48+heat*120)
   local fringe=rgb(math.floor(80+heat*60),math.floor(27+heat*24),22)
   pix(e,p[1]-1,p[2],fringe)
   pix(e,p[1],p[2]+1,fringe)
   pix(e,p[1],p[2],rgb(r,g,math.floor(28+heat*26)))
   if heat>0.35 then pix(e,p[1]+1,p[2],rgb(r,math.floor(g*0.80),30)) end
   if heat>0.72 and i%2==1 then pix(e,p[1],p[2]-1,rgb(255,184,65)) end
   if heat>0.94 and i%3==1 then pix(e,p[1]+1,p[2]-1,rgb(255,211,104)) end
  end
 end
 spr:newCel(layer,f,e,Point(0,0)) sparks:newCel(sparks.layers[1],f,e,Point(0,0))
 local c=Image(base) c:drawImage(e)
 combined[f]=c emberimgs[f]=e
end
spr:saveAs(dir.."liaoyuan-ground-ash-v2.aseprite")
sparks:saveAs(dir.."liaoyuan-ground-embers-v2.aseprite")
local strip=Image(64,1536,ColorMode.RGB) local estrip=Image(64,1536,ColorMode.RGB)
for f=1,24 do strip:drawImage(combined[f],Point(0,(f-1)*64)) estrip:drawImage(emberimgs[f],Point(0,(f-1)*64)) end
strip:saveAs(dir.."ground_ash_strip-v2.png") estrip:saveAs(dir.."ground_embers_strip-v2.png")
local single=Image(combined[1]) single:resize(512,512) single:saveAs(dir.."single-tile-preview-v2.png")
local preview=Sprite(512,512,ColorMode.RGB)
preview.layers[1].name="2D tiled material preview - NOT GAME SCREENSHOT"
for f=1,24 do
 if f>1 then preview:newEmptyFrame() end
 preview.frames[f].duration=0.05
 local im=Image(256,256,ColorMode.RGB)
 for ty=0,3 do for tx=0,3 do
  local frame=((f-1+tx*7+ty*11)%24)+1
  im:drawImage(combined[frame],Point(tx*64,ty*64))
 end end
 im:resize(512,512)
 preview:newCel(preview.layers[1],f,im,Point(0,0))
 if f==1 then im:saveAs(dir.."tiled-surface-preview-v2.png") end
end
preview:saveAs(dir.."tiled-surface-preview-v2.aseprite")
preview:saveAs(dir.."tiled-surface-preview-v2.gif")
return {ok=true,directory=dir,frames=24,frameMs=50,tile=64,strip="64x1536"}

