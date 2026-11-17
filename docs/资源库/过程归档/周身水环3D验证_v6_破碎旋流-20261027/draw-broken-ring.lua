local root='D:/alianximod/docs/资源库/过程归档/周身水环3D验证_v6_破碎旋流-20261027/'
local pc=app.pixelColor.rgba
local C={pc(10,35,73,255),pc(17,69,115,255),pc(27,116,161,255),pc(88,186,208,255),pc(184,224,224,255),pc(242,249,237,255)}
local s=Sprite(128,32,ColorMode.RGB)
local core=s.layers[1];core.name='两段主弧_固定浪座'
local chunks=s:newLayer();chunks.name='断裂水块_沿环追赶汇合'
local foam=s:newLayer();foam.name='局部流动白沫'
local atlas=Image(128,512,ColorMode.RGB)
local lengths={6,6,5,4,3,3,3,3,3,3,3,4,4,5,6,6}
local counts={}
for f=0,15 do
 if f>0 then s:newEmptyFrame() end;s.frames[f+1].duration=.1
 local b=Image(128,32,ColorMode.RGB);local q=Image(128,32,ColorMode.RGB);local h=Image(128,32,ColorMode.RGB)
 local phase=f*math.pi/8
 for x=0,127 do
  local main=(x<=42) or (x>=63 and x<=105)
  if main then
   local edge=(x<=42) and math.min(x+4,43-x) or math.min(x-62,106-x)
   local thin=math.max(0,4-edge)
   local top=3+math.floor(1.5*math.sin(x*.2)+math.sin(phase+x*.07))+thin
   local bot=25+math.floor(1.4*math.sin(x*.15+.5))-thin*2
   for y=top,bot do
    local c=C[3];if y>=16 then c=C[2] end;if y==top or y==bot then c=C[1] end;if y<9 and y>top then c=C[4] end
    b:drawPixel(x,y,c)
    local u=(x-math.floor(2*math.sin(phase)))%33
    if u<19 and y>=top+1 and y<=top+3 then h:drawPixel(x,y,C[5]) end
    if u<9 and y==top+1 then h:drawPixel(x,y,C[6]) end
    if u>=13 and u<26 and y==12 then h:drawPixel(x,y,C[4]) end
   end
  end
 end
 for gap=0,1 do
  for part=0,1 do
   local p=(f+gap*8+part*8)%16
   local t=p/16
   local center=(gap==0 and 42 or 105)+t*(gap==0 and 22 or 24)
   local len=lengths[p+1]*(part==1 and .65 or 1)
   local yy=10+math.floor(2*math.sin(t*math.pi*2+part))
   local hh=part==0 and 9 or 5
   for dx=-math.ceil(len/2),math.ceil(len/2) do
    if math.abs(dx)<=len/2 then
     local x=(math.floor(center+dx+.5))%128
     local taper=math.floor(math.abs(dx)/math.max(1,len/2)*2)
     for y=yy+taper,yy+hh-taper do
      local c=C[3]
      if y==yy+taper then c=C[5] elseif y>=yy+hh-1 then c=C[2] elseif dx>=len/2-1 then c=C[4] end
      q:drawPixel(x,y,c)
     end
    end
   end
   if part==0 and p>=4 and p<=11 then
    local x=(math.floor(center)-4)%128
    for k=0,2 do q:drawPixel((x+k)%128,21,C[4]);q:drawPixel((x+k)%128,22,C[2]) end
   end
  end
 end
 s:newCel(core,f+1,b,Point(0,0));s:newCel(chunks,f+1,q,Point(0,0));s:newCel(foam,f+1,h,Point(0,0))
 b:drawImage(q,Point(0,0));b:drawImage(h,Point(0,0))
 local empty=0
 for x=0,127 do local used=false;for y=0,31 do if pc and app.pixelColor.rgbaA(b:getPixel(x,y))>0 then used=true;break end end;if not used then empty=empty+1 end end
 table.insert(counts,empty)
 atlas:drawImage(b,Point(0,f*32))
end
s:saveAs(root..'broken_ring_flow.aseprite');atlas:saveAs(root..'broken_ring_flow.png')
return {frames=#s.frames,layers=#s.layers,emptyColumnsPerFrame=counts}

