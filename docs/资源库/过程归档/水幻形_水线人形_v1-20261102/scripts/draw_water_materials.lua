local function rgba(r,g,b,a) return app.pixelColor.rgba(r,g,b,a) end
local flow=Sprite(16,16,ColorMode.RGB)
flow.layers[1].name="flowing_water_ribbon"
for frame=1,16 do
 if frame>1 then flow:newEmptyFrame() end
 flow.frames[frame].duration=0.08
 local img=Image(flow.spec)
 for y=0,15 do for x=0,15 do
  local edge=math.min(x,15-x)
  local band=(y+frame-1+math.floor(x/4))%16
  local color=rgba(20,158+math.min(edge,5)*7,200+math.min(edge,5)*5,205+math.min(edge,5)*7)
  if edge<2 then color=rgba(10,100+edge*25,152+edge*15,175+edge*20) end
  if band<3 and x>3 and x<12 then color=rgba(176,247,251,246) end
  if band==3 and x>4 and x<13 then color=rgba(103,224,239,240) end
  img:drawPixel(x,y,color)
 end end
 flow:newCel(flow.layers[1],frame,img,Point(0,0))
end
local tag=flow:newTag(1,16);tag.name="flow";tag.aniDir=AniDir.FORWARD
flow:saveAs(args.flow)
local drop=Sprite(16,16,ColorMode.RGB);drop.layers[1].name="water_droplet"
local img=Image(drop.spec)
for y=2,13 do
 local width=(y<7) and (y-2)/2 or math.sqrt(math.max(0,16-(y-9)^2))
 for x=2,13 do
  local dx=x-7.5
  if math.abs(dx)<=width then
   local edge=width-math.abs(dx)
   local c=rgba(23,167,213,235)
   if edge<1 then c=rgba(10,103,161,220) end
   if x>=6 and x<=7 and y>=5 and y<=9 then c=rgba(208,255,255,250) end
   img:drawPixel(x,y,c)
  end
 end
end
drop:newCel(drop.layers[1],1,img,Point(0,0))
drop:saveAs(args.drop)
return {flow=args.flow,flowFrames=16,drop=args.drop,dimensions=16}
