local dir="D:/alianximod/docs/资源库/过程归档/种火_上窜熄灭_v3-20261111"
local src=app.open("D:/alianximod/docs/资源库/过程归档/种火_播种_v2-20261111/seed_sow_v2.aseprite")
local sp=Sprite(64,64,ColorMode.RGB)
sp.layers[1].name="flame_rise_and_bottom_up_burnout"
for i=2,16 do sp:newEmptyFrame() end
local sheets=Image(64,1024)
local poses={}
local rise={6,4,2,0,-2,-4,-6,-8,-8,-10,-10,-12,-12,-14,-14,-14}
local sourceFrames={1,2,3,4,5,6,7,8,7,6,8,7,6,6,6,6}
for f=1,16 do
  local im=Image(64,64)
  local original=Image(64,64)
  original:drawSprite(src,sourceFrames[f])
  -- Preserve palette and flame shape. Move upward without scaling.
  -- A broken edge travels from the base toward the tips.
  local cutoff=64
  if f>=8 then cutoff=54-(f-8)*5 end
  for y=0,63 do for x=0,63 do
    local c=original:getPixel(x,y)
    local jag=({0,2,-2,4,0,-2,2})[math.floor(x/4)%7+1]
    local edge=cutoff+jag
    local hole=((math.floor(x/2)*7+math.floor(y/2)*3+f)%13)<3
    if f<16 and app.pixelColor.rgbaA(c)>0 and y<edge and
       not(f>=9 and y>edge-6 and hole) then
      local yy=y+rise[f]
      if yy>=0 and yy<64 then im:drawPixel(x,yy,c) end
    end
  end end
  sp.frames[f].duration=0.05
  sp:newCel(sp.layers[1],f,im,Point(0,0))
  poses[f]=im
  sheets:drawImage(im,Point(0,(f-1)*64))
  im:saveAs(dir..string.format("/frame_%02d.png",f))
end
sp:saveAs(dir.."/seed_sow_v3.aseprite")
sheets:saveAs(dir.."/seed_sow_v3-strip.png")
local board=Sprite(536,280,ColorMode.RGB)
board.layers[1].name="dark_light_preview"
for i=2,16 do board:newEmptyFrame() end
local contact=Image(536,280*4)
for f=1,16 do
  local im=Image(536,280)
  im:clear(Color{r=18,g=22,b=28,a=255})
  local dark=Image(256,256); dark:clear(Color{r=24,g=31,b=39,a=255})
  local light=Image(256,256); light:clear(Color{r=200,g=207,b=202,a=255})
  local enlarged=Image(poses[f]); enlarged:resize(256,256)
  dark:drawImage(enlarged); light:drawImage(enlarged)
  im:drawImage(dark,Point(8,12)); im:drawImage(light,Point(272,12))
  board.frames[f].duration=f==16 and 0.5 or 0.05
  board:newCel(board.layers[1],f,im)
  if f==6 then im:saveAs(dir.."/preview_peak.png") end
  if f==4 or f==8 or f==12 or f==15 then
    local row=({[4]=0,[8]=1,[12]=2,[15]=3})[f]
    contact:drawImage(im,Point(0,row*280))
  end
end
contact:saveAs(dir.."/phases.png")
board:saveAs(dir.."/preview.aseprite")
board:saveCopyAs(dir.."/preview.gif")
board:close(); sp:close(); src:close()
print("v3: 16 frames, 50ms, rise + bottom-up erosion, no scale")
