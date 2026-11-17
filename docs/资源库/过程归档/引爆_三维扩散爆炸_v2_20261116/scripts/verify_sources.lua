local root="D:/alianximod/docs/资源库/过程归档/引爆_三维扩散爆炸_v2_20261116"
local records={}
for _,variant in ipairs({"orange","soul"}) do
 local spr=app.open(root.."/source/explosion_puff_"..variant..".aseprite")
 local strip=Image{fromFile=root.."/textures/explosion_puff_"..variant.."-strip.png"}
 local frames={}
 for frame=1,#spr.frames do
  local flattened=Image(32,32,ColorMode.RGB)
  flattened:drawSprite(spr,frame)
  local mismatches,count=0,0
  for py=0,31 do for px=0,31 do
   local actual=flattened:getPixel(px,py)
   if actual~=strip:getPixel(px,py+(frame-1)*32) then mismatches=mismatches+1 end
   if app.pixelColor.rgbaA(actual)>0 then count=count+1 end
  end end
  if mismatches>0 then error("Source and strip do not match") end
  frames[#frames+1]={frame=frame,duration_ms=spr.frames[frame].duration*1000,alpha_pixels=count,mismatches=mismatches}
 end
 records[variant]={width=spr.width,height=spr.height,layers=#spr.layers,frames=frames}
 spr:close()
end
local file=io.open(root.."/evidence/aseprite-reopen-verification.json","w")
file:write(json.encode(records));file:close()
return records
