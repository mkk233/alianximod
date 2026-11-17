local root="D:/alianximod/docs/资源库/过程归档/引爆_三维扩散爆炸_v2_20261116"
local palettes={
 orange={"FFFBE0","FFE8A0","FFBD55","E78537","EBE8DD","CCCAC2","A7A9A6","777E80"},
 soul={"E2FFF4","A7FFF0","44EDE7","0795A2","E1ECE9","C3CFCD","A1AEAD","748283"}
}
local radius={3,5,7.3,9.1,10.6,11.7,12.4,12.9,13.2,13.4,13.5,13.5,13.5,13.5,13.5,0}
local holes={0,0,0,0,0,1.8,4,6.4,8.8,10.7,12.4,14,15.5,17,18.8,0}
local function rgba(hex)
 return app.pixelColor.rgba(tonumber(hex:sub(1,2),16),tonumber(hex:sub(3,4),16),tonumber(hex:sub(5,6),16),255)
end
local report={}
for variant,palette in pairs(palettes) do
 local spr=Sprite(32,32,ColorMode.RGB)
 spr.layers[1].name="Cloud shell | opaque pixel silhouette"
 local shell=spr.layers[1]
 local light=spr:newLayer()
 light.name="Bright core | warm or soul rim"
 local strip=Image(32,512,ColorMode.RGB)
 local counts={}
 for frame=1,16 do
  if frame>1 then spr:newEmptyFrame() end
  spr.frames[frame].duration=.05
  local base=Image(32,32,ColorMode.RGB)
  local highlights=Image(32,32,ColorMode.RGB)
  local merged=Image(32,32,ColorMode.RGB)
  local count=0
  if frame<16 then
   for py=0,31 do for px=0,31 do
    local dx,dy=px-15.5,py-15.5
    local angle=math.atan(dy,dx)
    local uneven=1+.065*math.sin(angle*5+.7)+.04*math.cos(angle*3-1.2)
    local distance=math.sqrt(dx*dx+dy*dy)
    local outer=radius[frame]*uneven
    local hole=math.sqrt((dx+3.3)^2+(dy+3.6)^2)
    local kept=distance<=outer and (holes[frame]==0 or hole>holes[frame])
    if frame>=11 and ((px>=23 and py<14) or (px<8 and py>=21)) then kept=false end
    if frame>=13 and ((px>=17 and px<=19) or (py>=24 and py<=25)) then kept=false end
    if frame==15 and not (px>24 and py>=17 and py<=21) then kept=false end
    if kept then
     local tone
     if frame<=5 then
      local depth=distance/outer
      tone=depth<.65 and 1 or (depth<.87 and 2 or (dy>0 and 4 or 3))
     elseif frame<=8 then
      tone=(dx+dy<-3 and 5 or (dx+dy<10 and 6 or 7))
      if hole<holes[frame]+1.5 then tone=frame<=7 and 2 or 5 end
     else
      tone=(dx+dy<5 and 6 or (dx+dy<17 and 7 or 8))
      if hole<holes[frame]+1.1 then tone=5 end
     end
     local color=rgba(palette[tone])
     if tone<=4 then highlights:drawPixel(px,py,color) else base:drawPixel(px,py,color) end
     merged:drawPixel(px,py,color)
     count=count+1
    end
   end end
  end
  spr:newCel(shell,frame,base,Point(0,0))
  spr:newCel(light,frame,highlights,Point(0,0))
  merged:saveAs(root.."/frames/"..variant.."/puff_"..string.format("%02d",frame)..".png")
  strip:drawImage(merged,Point(0,(frame-1)*32))
  counts[#counts+1]=count
 end
 local tag=spr:newTag(1,16)
 tag.name="PUFF_ONCE_16x50ms"
 spr:saveAs(root.."/source/explosion_puff_"..variant..".aseprite")
 strip:saveAs(root.."/textures/explosion_puff_"..variant.."-strip.png")
 report[variant]={width=32,height=32,frames=16,duration_ms=50,alpha_counts=counts}
 spr:close()
end
local handle=io.open(root.."/evidence/aseprite-production.json","w")
handle:write(json.encode(report))
handle:close()
return report
