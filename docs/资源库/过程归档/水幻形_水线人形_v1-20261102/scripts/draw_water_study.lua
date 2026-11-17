local s=sprite
local base=s.layers[1]
base.name="01_water_contour"
local shine=s:newLayer(); shine.name="02_flowing_highlights"
local drops=s:newLayer(); drops.name="03_separated_droplets"
local ring=s:newLayer(); ring.name="04_ground_ripples"
local paths={
 {{42,12},{38,15},{36,23},{38,31},{43,35},{53,35},{58,31},{60,22},{57,14},{52,12},{42,12}},
 {{43,35},{43,39},{35,40},{29,44},{27,51},{25,60},{22,69},{22,77},{25,81},{29,80},{31,74},{32,65},{35,56}},
 {{53,35},{54,39},{61,40},{67,44},{69,52},{72,62},{74,71},{73,79},{69,82},{65,79},{64,72},{63,64},{60,56}},
 {{35,46},{37,57},{35,70},{38,77},{44,80},{53,79},{59,75},{58,65},{58,56},{60,45}},
 {{39,79},{35,88},{34,100},{31,110},{33,114},{42,114},{44,109},{43,99},{47,88},{48,83}},
 {{50,82},{53,91},{53,101},{52,110},{54,114},{63,114},{65,110},{61,100},{60,89},{56,80}},
 {{37,47},{43,49},{50,48},{56,44}},
 {{40,59},{44,64},{51,66},{57,63}},
 {{39,72},{46,75},{53,74},{58,70}},
 {{42,18},{41,24},{44,29}},
 {{55,17},{56,23},{54,28}}
}
local function rgba(r,g,b,a) return app.pixelColor.rgba(r,g,b,math.floor(math.max(0,math.min(255,a)))) end
local function dot(im,x,y,c,r)
 x=math.floor(x+0.5);y=math.floor(y+0.5);r=r or 0
 for yy=-r,r do for xx=-r,r do
  if xx*xx+yy*yy<=r*r and x+xx>=0 and x+xx<s.width and y+yy>=0 and y+yy<s.height then im:drawPixel(x+xx,y+yy,c) end
 end end
end
local function setcel(layer,frame,img)
 local cel=layer:cel(frame)
 if cel then cel.image=img else s:newCel(layer,frame,img,Point(0,0)) end
end
for frame=1,32 do
 local ims={Image(s.spec),Image(s.spec),Image(s.spec),Image(s.spec)}
 local phase=(frame-1)*math.pi*2/16
 local bursting=frame>=17
 local progress=bursting and math.min(1,(frame-17)/11) or 0
 local fade=frame>28 and math.max(0,(32-frame)/4) or 1
 for pid,path in ipairs(paths) do
  local sample=0
  for segment=1,#path-1 do
   local p0=path[math.max(1,segment-1)];local p1=path[segment]
   local p2=path[segment+1];local p3=path[math.min(#path,segment+2)]
   local distance=math.sqrt((p2[1]-p1[1])^2+(p2[2]-p1[2])^2)
   for step=0,math.max(2,math.floor(distance*2)) do
    local t=step/math.max(2,math.floor(distance*2))
    local function curve(axis)
     return 0.5*((2*p1[axis])+(-p0[axis]+p2[axis])*t+(2*p0[axis]-5*p1[axis]+4*p2[axis]-p3[axis])*t*t+(-p0[axis]+3*p1[axis]-3*p2[axis]+p3[axis])*t*t*t)
    end
    local x=curve(1);local y=curve(2)
    x=x+0.38*math.sin(y*0.21+phase+pid)
    if bursting then
     x=x+((x<48 and -1 or 1)*(3+pid%5)+math.sin(pid*3+math.floor(sample/7))*3)*progress
     y=y+(115-y)*progress*progress-3*math.sin(progress*math.pi)*(pid%3)
    end
    local visible=(not bursting or progress<0.17 or (math.floor(sample/6)+pid)%4~=0) and fade>0
    if visible then
     local opacity=fade*(1-0.5*progress)
     if pid>=7 then opacity=opacity*0.64 end
     dot(ims[1],x,y,rgba(9,103,158,185*opacity),1)
     dot(ims[1],x,y,rgba(33,182,217,235*opacity),0)
     if (math.floor(sample/2)-frame*2+pid*3)%15<5 or frame==17 then
      dot(ims[2],x,y,rgba(198,253,255,240*opacity),0)
     elseif (math.floor(sample/2)-frame*2+pid*3)%15<8 then
      dot(ims[2],x,y,rgba(103,230,244,220*opacity),0)
     end
    end
    sample=sample+1
   end
  end
 end
 local n=bursting and 36 or 7
 for index=1,n do
  local x,y,alpha
  if bursting then
   local seed=index*2.39996
   local sy=18+(index*19)%90
   local sx=36+(index*13)%26
   x=sx+math.sin(seed)*(5+index%9)*progress
   y=sy+(117-sy)*progress*progress-6*math.sin(progress*math.pi)*(index%3==0 and 1 or 0)
   alpha=fade*math.min(1,progress*4)*(1-progress*0.25)
  else
   local ticks=((frame+index*3)%16)/16
   local origins={{23,78},{70,80},{34,105},{61,102},{42,35},{59,61},{37,72}}
   x=origins[index][1]+math.sin(index)*ticks*2
   y=origins[index][2]+ticks*12
   alpha=(1-ticks)*0.65
  end
  dot(ims[3],x,y,rgba(20,139,192,210*alpha),1)
  dot(ims[3],x,y-1,rgba(130,240,250,245*alpha),0)
 end
 local radius=bursting and (10+progress*13) or (11+math.sin(phase)*0.5)
 local opacity=(bursting and 120 or 35)*fade
 for step=0,180 do
  local a=step*math.pi*2/180
  if (step+frame)%13>2 then dot(ims[4],48+math.cos(a)*radius,117+math.sin(a)*radius*0.17,rgba(65,191,219,opacity),0) end
 end
 setcel(base,frame,ims[1]);setcel(shine,frame,ims[2]);setcel(drops,frame,ims[3]);setcel(ring,frame,ims[4])
 s.frames[frame].duration=0.08
end
local idle=s:newTag(1,16);idle.name="idle_outline";idle.aniDir=AniDir.FORWARD
local burst=s:newTag(17,32);burst.name="cast_then_disperse";burst.aniDir=AniDir.FORWARD
s:saveAs(args.source)
local p=Sprite(96,128,ColorMode.RGB)
p.layers[1].name="actual_aseprite_preview_dark"
for frame=1,32 do
 if frame>1 then p:newEmptyFrame() end
 p.frames[frame].duration=0.08
 local bg=Image(p.spec);bg:clear(rgba(19,31,44,255))
 local ink=Image(s.spec);ink:drawSprite(s,frame)
 bg:drawImage(ink,Point(0,0))
 p:newCel(p.layers[1],frame,bg,Point(0,0))
end
p:saveAs(args.preview)
return {width=s.width,height=s.height,frames=#s.frames,layers=#s.layers,source=args.source,preview=args.preview}
