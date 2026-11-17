local dir="D:/alianximod/docs/资源库/过程归档/种火_播种_v2-20261111"
local src=Sprite{fromFile=dir.."/seed_sow_v2.aseprite"}
local scale=3
local function board(path,bg)
 local out=Sprite(64*scale*2+24,64*scale+28,ColorMode.RGB)
 out.layers[1].name="background"
 local framesNeeded=#src.frames
 while #out.frames<framesNeeded do out:newFrame() end
 for i,frame in ipairs(out.frames) do
  frame.duration=src.frames[i].duration
  local image=Image(out.width,out.height)
  image:clear(bg)
  local function label(x,y,text)
   -- no font dependency; panels are distinguished by background only
  end
  local rendered=Image(src.width,src.height)
  rendered:drawSprite(src,i)
  local big=Image(src.width*scale,src.height*scale)
  for it in rendered:pixels() do
   local c=Color(it())
   if c.alpha>0 then
    for dy=0,scale-1 do for dx=0,scale-1 do big:drawPixel(it.x*scale+dx,it.y*scale+dy,c) end end
   end
  end
  image:drawImage(big,Point(8,14))
  local light=Color{r=214,g=214,b=206,a=255}
  local second=Image(big.width,big.height); second:clear(light); second:drawImage(big)
  image:drawImage(second,Point(16+big.width,14))
  out:newCel(out.layers[1],frame,image,Point(0,0))
 end
 out:saveCopyAs(path)
 out:close()
end
board(dir.."/seed_sow_v2_preview.gif",Color{r=16,g=18,b=22,a=255})
print("preview")
src:close()
