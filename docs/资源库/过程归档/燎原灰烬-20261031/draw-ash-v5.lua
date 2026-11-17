
local dir="D:/alianximod/docs/资源库/过程归档/燎原灰烬-20261031/"
local function rgb(r,g,b) return app.pixelColor.rgba(r,g,b,255) end
local palette={
 rgb(42,43,42),rgb(49,50,48),rgb(56,57,55),rgb(63,64,61),
 rgb(70,71,68),rgb(78,79,75),rgb(61,58,52),rgb(87,87,80)
}
local bag={1,2,2,3,3,3,4,4,4,5,5,6,7,8}
local suffixes={"v5","v5b","v5c","v5d"}
local comps={}
local points={{3,4,0},{12,10,8},{6,13,16}}
local embers={}
for f=1,24 do
 local im=Image(16,16,ColorMode.RGB)
 for i,p in ipairs(points) do
  local t=(f-1+p[3])%24
  local h=(1+math.cos(t*math.pi/12))/2
  if h>0.18 then
   im:drawPixel(p[1],p[2],rgb(math.floor(132+123*h),math.floor(49+120*h),math.floor(24+27*h)))
   if h>0.80 and i~=2 then
    im:drawPixel(p[1]+1,p[2],rgb(145,53,25))
   end
  end
 end
 embers[f]=im
end
local esp=Sprite(16,16,ColorMode.RGB) esp.layers[1].name="Native pixel sparse orange embers"
local es=Image(16,384,ColorMode.RGB)
for f=1,24 do
 if f>1 then esp:newEmptyFrame() end
 esp.frames[f].duration=0.05 esp:newCel(esp.layers[1],f,embers[f])
 es:drawImage(embers[f],Point(0,(f-1)*16))
end
esp:saveAs(dir.."liaoyuan-ground-embers-v5.aseprite")
es:saveAs(dir.."ground_embers_strip-v5.png")
for k,suffix in ipairs(suffixes) do
 math.randomseed(5200+k*173)
 local base=Image(16,16,ColorMode.RGB)
 for y=0,15 do for x=0,15 do
  local idx=bag[math.random(#bag)]
  -- Every native pixel is discrete: no gradients, fields, or large polygons.
  base:drawPixel(x,y,palette[idx])
 end end
 base:saveAs(dir.."ground_ash_base-"..suffix..".png")
 local sp=Sprite(16,16,ColorMode.RGB)
 sp.layers[1].name="Discrete native 16px ash grain"
 local el=sp:newLayer() el.name="Sparse orange embers"
 local strip=Image(16,384,ColorMode.RGB)
 comps[k]={}
 for f=1,24 do
  if f>1 then sp:newEmptyFrame() end
  sp.frames[f].duration=0.05
  sp:newCel(sp.layers[1],f,base)
  sp:newCel(el,f,embers[f])
  local c=Image(base) c:drawImage(embers[f])
  comps[k][f]=c strip:drawImage(c,Point(0,(f-1)*16))
 end
 sp:saveAs(dir.."liaoyuan-ground-ash-"..suffix..".aseprite")
 strip:saveAs(dir.."ground_ash_strip-"..suffix..".png")
 local single=Image(comps[k][1]) single:resize(512,512)
 single:saveAs(dir.."single-tile-preview-"..suffix..".png")
end
local layout={{1,3,2,4},{4,2,1,3},{2,1,4,2},{3,4,3,1}}
local preview=Sprite(512,512,ColorMode.RGB)
preview.layers[1].name="2D 4x4 ash preview - NOT GAME SCREENSHOT"
for f=1,24 do
 if f>1 then preview:newEmptyFrame() end
 preview.frames[f].duration=0.05
 local im=Image(64,64,ColorMode.RGB)
 for y=0,3 do for x=0,3 do
  local phase=(f-1+x*7+y*11)%24+1
  im:drawImage(comps[layout[y+1][x+1]][phase],Point(x*16,y*16))
 end end
 im:resize(512,512)
 preview:newCel(preview.layers[1],f,im)
 if f==1 then im:saveAs(dir.."mixed-surface-preview-v5.png") end
end
preview:saveAs(dir.."mixed-surface-preview-v5.aseprite")
preview:saveAs(dir.."mixed-surface-preview-v5.gif")
return {ok=true,base="16x16",strip="16x384",frames=24,frameMs=50,variants=4}

