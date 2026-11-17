local out='D:/alianximod/docs/资源库/特效/水流/周身水环_Aseprite_v2/'
local source=Image{fromFile='D:/alianximod/docs/资源库/特效/水流/周身水环_v1/water_ring.png'}
local pc=app.pixelColor
local colors={pc.rgba(20,63,122,255),pc.rgba(18,106,175,255),pc.rgba(25,158,210,255),pc.rgba(102,216,234,255),pc.rgba(198,248,250,255),pc.rgba(245,255,255,255)}
local ring=Sprite(128,64,ColorMode.RGB)
ring:deleteLayer(ring.layers[1])
local layers={}
for _,name in ipairs({'后环水体','后环白沫','前环水体','前环白沫'}) do local l=ring:newLayer();l.name=name;layers[#layers+1]=l end
local body=Sprite(128,96,ColorMode.RGB)
body:deleteLayer(body.layers[1])
local back=body:newLayer();back.name='后环'
local person=body:newLayer();person.name='角色占位_仅预览'
local front=body:newLayer();front.name='前环'
local function rect(im,x,y,w,h,c) for yy=y,y+h-1 do for xx=x,x+w-1 do im:drawPixel(xx,yy,c) end end end
local dummy=Image(128,96,ColorMode.RGB)
rect(dummy,56,8,16,16,pc.rgba(140,161,169,255))
rect(dummy,53,26,22,30,pc.rgba(91,117,128,255))
rect(dummy,45,28,7,25,pc.rgba(117,141,151,255));rect(dummy,76,28,7,25,pc.rgba(117,141,151,255))
rect(dummy,54,56,9,29,pc.rgba(67,91,103,255));rect(dummy,65,56,9,29,pc.rgba(67,91,103,255))
rect(dummy,58,11,4,3,pc.rgba(175,191,196,255))
for f=0,15 do
 if f>0 then ring:newEmptyFrame(f+1);body:newEmptyFrame(f+1) end
 ring.frames[f+1].duration=.1;body.frames[f+1].duration=.1
 local ims={Image(128,64),Image(128,64),Image(128,64),Image(128,64)}
 for y=0,63 do for x=0,127 do
  local c=source:getPixel(x,y+f*64)
  if pc.rgbaA(c)>0 then
   local li=1;if y>=30 then li=3 end
   if pc.rgbaR(c)>=190 then li=li+1 end
   ims[li]:drawPixel(x,y,c)
  end
 end end
 -- Add a few water beads moving with the ring, on separate foam cels.
 for j=0,2 do
  local a=2*math.pi*(f/48+j/3)
  local x=math.floor(64+54*math.cos(a)+.5)
  local y=math.floor(30+21*math.sin(a)+.5)
  local li=2;if y>=30 then li=4 end
  ims[li]:drawPixel(x,y,colors[5])
  if math.sin(a)>.2 then ims[li]:drawPixel(x+1,y,colors[4]) end
 end
 local composite=Image(128,64)
 for i=1,4 do ring:newCel(layers[i],f+1,ims[i],Point(0,0));composite:drawImage(ims[i]) end
 composite:saveAs(out..string.format('单帧/ring/frame_%02d.png',f))
 local bi=Image(128,96);bi:drawImage(ims[1],Point(0,24));bi:drawImage(ims[2],Point(0,24))
 local fi=Image(128,96);fi:drawImage(ims[3],Point(0,24));fi:drawImage(ims[4],Point(0,24))
 body:newCel(back,f+1,bi,Point(0,0));body:newCel(person,f+1,dummy,Point(0,0));body:newCel(front,f+1,fi,Point(0,0))
 local preview=Image(128,96);preview:drawSprite(body,f+1);preview:saveAs(out..string.format('单帧/body/frame_%02d.png',f))
end
ring:newTag(1,16).name='水环循环_100ms';body:newTag(1,16).name='周身环绕演示'
ring:saveAs(out..'water_ring.aseprite');body:saveAs(out..'body_preview.aseprite')
local band=Sprite(128,16,ColorMode.RGB);band.layers[1].name='水体';local foam=band:newLayer();foam.name='流动白沫'
for f=0,15 do
 if f>0 then band:newEmptyFrame(f+1) end
 band.frames[f+1].duration=.1
 local water=Image(128,16);local bubbles=Image(128,16)
 for y=2,13 do for x=0,127 do
  local z=(y-2)/12;local t=2*math.pi*x/128;local p=2*math.pi*f/16;local c=3
  if y>=12 then c=1 elseif y>=9 then c=2 elseif y<=3 then c=4 end
  local q=math.sin(4*t-p+z*3)
  if q>.15 and y>=5 and y<=7 then c=4 end
  if q>.62 and y==6 then c=5 end
  if math.sin(3*t-p+z*5)>.74 and y<=4 then c=5;if y==3 then c=6 end end
  if c>=5 then bubbles:drawPixel(x,y,colors[c]) else water:drawPixel(x,y,colors[c]) end
 end end
 band:newCel(band.layers[1],f+1,water,Point(0,0));band:newCel(foam,f+1,bubbles,Point(0,0))
 local im=Image(128,16);im:drawSprite(band,f+1);im:saveAs(out..string.format('单帧/band/frame_%02d.png',f))
end
band:newTag(1,16).name='展开水纹循环';band:saveAs(out..'water_band.aseprite')
return {ok=true,frames=16,ringLayers=#ring.layers,bodyLayers=#body.layers,bandLayers=#band.layers}
