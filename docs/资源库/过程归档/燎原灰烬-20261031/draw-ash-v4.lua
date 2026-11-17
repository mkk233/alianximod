
local dir="D:/alianximod/docs/资源库/过程归档/燎原灰烬-20261031/"
local rgba=app.pixelColor.rgba
local function rgb(r,g,b) return rgba(r,g,b,255) end

math.randomseed(314159)
local base=Image(64,64,ColorMode.RGB)
local function pix(im,x,y,c) im:drawPixel(x%64,y%64,c) end
local cols={
 {38,39,39},{46,47,46},{54,56,56},{63,64,61},
 {72,73,70},{83,81,73},{93,95,94},{105,103,93},
 {62,58,51},{47,44,39},{76,80,83},{88,85,76}
}
local field={}
for y=0,63 do field[y]={} for x=0,63 do field[y][x]={54,55,53} end end
-- Irregular overlapping dust drifts; wrapped distance has no tile-edge seam.
for i=1,65 do
 local cx=math.random(0,63) local cy=math.random(0,63)
 local rx=math.random(3,11) local ry=math.random(2,8)
 local c=cols[math.random(#cols)]
 for y=0,63 do for x=0,63 do
  local dx=math.abs(x-cx) dx=math.min(dx,64-dx)
  local dy=math.abs(y-cy) dy=math.min(dy,64-dy)
  local d=(dx/rx)^2+(dy/ry)^2
  if d<1 then
   local w=(1-d)*0.72
   local v=field[y][x]
   for k=1,3 do v[k]=v[k]*(1-w)+c[k]*w end
  end
 end end
end
for y=0,63 do for x=0,63 do
 local v=field[y][x] local n=math.random(-7,7)
 pix(base,x,y,rgb(math.floor(v[1]+n),math.floor(v[2]+n),math.floor(v[3]+n)))
end end
-- Uneven ash flakes, powders, and charcoal chips with organic chipped silhouettes.
local shapes={
 {{0,0},{1,0},{0,1}},
 {{0,0},{1,0},{2,0},{1,1},{2,1}},
 {{0,0},{1,0},{-1,1},{0,1},{1,1},{0,2}},
 {{0,0},{0,1}},
 {{0,0}},
 {{0,0},{1,0},{2,1},{1,1},{2,2},{3,2}}
}
for i=1,390 do
 local x=math.random(0,63) local y=math.random(0,63)
 local c=cols[math.random(#cols)]
 local sh=shapes[math.random(#shapes)]
 local rot=math.random(0,3)
 for _,pt in ipairs(sh) do
  local dx,dy=pt[1],pt[2]
  for j=1,rot do dx,dy=-dy,dx end
  local shade=math.random(-3,3)
  pix(base,x+dx,y+dy,rgb(c[1]+shade,c[2]+shade,c[3]+shade))
 end
end
-- Sparse pale powder specks soften the dark char without drawing long lines.
for i=1,190 do
 local x=math.random(0,63) local y=math.random(0,63)
 local c=cols[math.random(4,12)]
 pix(base,x,y,rgb(c[1],c[2],c[3]))
end
base:saveAs(dir.."ground_ash_base-v4.png")
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
spr:saveAs(dir.."liaoyuan-ground-ash-v4.aseprite")
sparks:saveAs(dir.."liaoyuan-ground-embers-v4.aseprite")
local strip=Image(64,1536,ColorMode.RGB) local estrip=Image(64,1536,ColorMode.RGB)
for f=1,24 do strip:drawImage(combined[f],Point(0,(f-1)*64)) estrip:drawImage(emberimgs[f],Point(0,(f-1)*64)) end
strip:saveAs(dir.."ground_ash_strip-v4.png") estrip:saveAs(dir.."ground_embers_strip-v4.png")
local single=Image(combined[1]) single:resize(512,512) single:saveAs(dir.."single-tile-preview-v4.png")
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
 if f==1 then im:saveAs(dir.."tiled-surface-preview-v4.png") end
end
preview:saveAs(dir.."tiled-surface-preview-v4.aseprite")
preview:saveAs(dir.."tiled-surface-preview-v4.gif")
return {ok=true,directory=dir,frames=24,frameMs=50,tile=64,strip="64x1536"}

