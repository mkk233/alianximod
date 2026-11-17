local root="D:/alianximod/docs/资源库/过程归档/引爆_短促火团_v2_20261116"
local result={}
for _,variant in ipairs({"orange","soul"}) do
 local s=app.open(root.."/source/detonate_v2_"..variant..".aseprite")
 local strip=Image{fromFile=root.."/textures/detonate_v2_"..variant.."-strip.png"}
 local rows={}
 assert(s.width==64 and s.height==64 and #s.frames==16 and #s.layers==3)
 assert(strip.width==64 and strip.height==1024)
 for f=1,16 do
  local im=Image(64,64);im:drawSprite(s,f)
  local count,mismatch=0,0
  for y=0,63 do for x=0,63 do
   local c=im:getPixel(x,y)
   if app.pixelColor.rgbaA(c)>0 then count=count+1 end
   if c~=strip:getPixel(x,y+(f-1)*64) then mismatch=mismatch+1 end
  end end
  assert(mismatch==0);assert(math.abs(s.frames[f].duration-.05)<.000001)
  if f==16 then assert(count==0) end
  rows[#rows+1]={frame=f,durationMs=s.frames[f].duration*1000,opaquePixels=count,sourceStripMismatches=mismatch}
 end
 result[variant]={width=s.width,height=s.height,frames=#s.frames,layers=#s.layers,sequence=rows,finalTransparent=true}
 s:close()
end
local file=io.open(root.."/evidence/aseprite-source-verification.json","w");file:write(json.encode(result));file:close()
return result
