
local dir="D:/alianximod/docs/资源库/过程归档/燎原灰烬-20261031/"
local strips={}
for _,s in ipairs({"","b","c","d"}) do strips[#strips+1]=Image{fromFile=dir.."ground_ash_strip-v4"..s..".png"} end
local layout={{1,3,2,4},{4,2,1,3},{2,1,4,2},{3,4,3,1}}
local spr=Sprite(512,512,ColorMode.RGB)
spr.layers[1].name="2D four-variant ash preview - NOT GAME SCREENSHOT"
for f=1,24 do
 if f>1 then spr:newEmptyFrame() end
 spr.frames[f].duration=0.05
 local im=Image(256,256,ColorMode.RGB)
 for y=0,3 do for x=0,3 do
  local k=layout[y+1][x+1]
  local phase=(f-1+x*7+y*11)%24
  local tile=Image(strips[k],Rectangle(0,phase*64,64,64))
  im:drawImage(tile,Point(x*64,y*64))
 end end
 im:resize(512,512)
 spr:newCel(spr.layers[1],f,im,Point(0,0))
 if f==1 then im:saveAs(dir.."mixed-surface-preview-v4.png") end
end
spr:saveAs(dir.."mixed-surface-preview-v4.aseprite")
spr:saveAs(dir.."mixed-surface-preview-v4.gif")
return {ok=true}

