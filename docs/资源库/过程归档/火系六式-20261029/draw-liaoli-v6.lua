-- Draft spatial study: three expanding rings travel behind the player; sword handles lean rearward.
local root='D:/alianximod/docs/资源库/过程归档/火系六式-20261029/'
local pc=app.pixelColor
local function c(r,g,b,a)return pc.rgba(r,g,b,a or 255)end
local function px(im,x,y,v)x=math.floor(x);y=math.floor(y);if x>=0 and x<im.width and y>=0 and y<im.height then im:drawPixel(x,y,v)end end
local function rect(im,x,y,w,h,v)for yy=y,y+h-1 do for xx=x,x+w-1 do px(im,xx,yy,v)end end end
local function stamp(im,tex,cx,cy,a,sx,sy,flip)
 local ca,sa=math.cos(a),math.sin(a);local x0,x1,y0,y1=999,-999,999,-999
 for _,u in ipairs({0,tex.width*sx})do for _,v in ipairs({-tex.height*sy/2,tex.height*sy/2})do local x=cx+ca*u-sa*v;local y=cy+sa*u+ca*v;x0=math.min(x0,x);x1=math.max(x1,x);y0=math.min(y0,y);y1=math.max(y1,y)end end
 for y=math.floor(y0),math.ceil(y1)do for x=math.floor(x0),math.ceil(x1)do local dx,dy=x-cx,y-cy;local tx=math.floor((dx*ca+dy*sa)/sx);local ty=math.floor((-dx*sa+dy*ca)/sy+tex.height/2)
 if tx>=0 and ty>=0 and tx<tex.width and ty<tex.height then if flip then ty=tex.height-1-ty end;local p=tex:getPixel(tx,ty);if pc.rgbaA(p)>0 then px(im,x,y,p)end end end end
end
local function export(s,name)s:saveAs(root..name..'.aseprite');local im=Image(s.width,s.height*#s.frames,ColorMode.RGB);for f=1,#s.frames do im:drawSprite(s,f,Point(0,(f-1)*s.height))end;im:saveAs(root..name..'-strip.png')end
local ring=Sprite(96,96,ColorMode.RGB);ring.layers[1].name='Flame ring loop'
for f=1,24 do if f>1 then ring:newEmptyFrame()end;ring.frames[f].duration=.05;local im=Image(96,96,ColorMode.RGB);local phase=(f-1)*math.pi*2/24
 for y=3,92 do for x=3,92 do local dx,dy=x-47.5,y-47.5;local r=math.sqrt(dx*dx+dy*dy);local a=math.atan(dy,dx);local base=32+math.sin(a*9-phase)*1.3;local tongues=3+math.max(0,math.sin(a*13-phase*2))*6
  if r>base-2 and r<base+tongues then px(im,x,y,c(216,42,13,230))end
  if r>base-1 and r<base+tongues*.6 then px(im,x,y,c(255,103,13))end
  if r>base and r<base+1.7 then px(im,x,y,c(255,225,35))end
  if r>base+.3 and r<base+.85 and math.sin(a*7+phase)>.1 then px(im,x,y,c(255,250,153))end
 end end
 ring:newCel(ring.layers[1],f,im)
end
ring:newTag(1,24).name='fire_ring_loop';export(ring,'liaoli-rear-ring-v6')
local plume=app.open(root..'liaoli-back-plume-v5.aseprite');local ignition=app.open(root..'liaoli-back-ignite-v5.aseprite');local split=app.open(root..'liaoli-thrust-split-v5.aseprite')
local blade=Image{fromFile=root..'liaoli-orbit-sword-v3.png'};local gold=Image{fromFile=root..'liaoli-golden-sword-v5.png'}
local angles={-180,-154,-128,-102,-78,-52,-26,0}
local function makeView(name,side)
 local s=Sprite(400,280,ColorMode.RGB);s.layers[1].name=side and 'Oblique spatial preview' or 'Front symmetry preview'
 local function project(x,y,z)
  if side then return 224+x*.78+z*.65,207-y*.92+z*.21 end
  return 200+x,190-y end
 local function lineTexture(im,texture,start,finish,thickness,flip)
  local x0,y0=project(start[1],start[2],start[3]);local x1,y1=project(finish[1],finish[2],finish[3]);local dx,dy=x1-x0,y1-y0
  stamp(im,texture,x0,y0,math.atan(dy,dx),math.sqrt(dx*dx+dy*dy)/texture.width,thickness,flip)
 end
 for f=1,60 do if f>1 then s:newEmptyFrame()end;s.frames[f].duration=.05;local im=Image(400,280,ColorMode.RGB);im:clear(c(20,26,38))
  local effects=Image(400,280,ColorMode.RGB)
  -- Three phase-separated rings coexist. Their centres move along -Z (behind the player), radius grows.
  for j=2,0,-1 do local t=((f-1)/24+j/3)%1;local rad=14+43*t;local z=-17-65*t;local cy=25
   local alpha=math.floor(250*(1-.35*t));local phase=(f-1)*math.pi*2/24
   for k=0,359 do local a=k*math.pi/180;local wob=1+math.sin(a*11-phase)*.018
    for thick=-1,4 do local rr=rad*wob+thick*(.5+t*.5);local x,y=project(math.cos(a)*rr,cy+math.sin(a)*rr,z)
     local shade=thick>=3 and c(218,43,13,alpha)or thick>=1 and c(255,125,15,alpha)or c(255,233,55,alpha)
     px(effects,x,y,shade);px(effects,x+1,y,shade)
    end
    if k%7==0 then local rr=rad+4+2*math.sin(phase+k);local x,y=project(math.cos(a)*rr,cy+math.sin(a)*rr,z);px(effects,x,y,c(255,175,26,alpha))end
   end
  end
  im:drawImage(effects)
  local rays=Image(400,280,ColorMode.RGB);local swords=Image(400,280,ColorMode.RGB)
  for j,degrees in ipairs(angles)do local a=degrees*math.pi/180;local dx,dy=math.cos(a),-math.sin(a);local pair=math.min(j,9-j)
   local tex=Image(128,48,ColorMode.RGB);tex:drawSprite(f<=12 and ignition or plume,f<=12 and f or (f+pair*3-1)%24+1)
   lineTexture(rays,tex,{0,25,-18},{dx*118,25+dy*118,14},.75,j>4)
   -- Handles are 18 units behind the player, tips 6 units behind: an explicit rearward handle tilt.
   local hilt={dx*23,25+dy*23,-18};local tip={dx*62,25+dy*62,-6}
   local src=Image(blade);local front=127*(math.min(f,12)/12)^1.2
   for it in src:pixels()do if pc.rgbaA(it())>0 and 23+it.x*.62<front then it(gold:getPixel(it.x,it.y))end end
   lineTexture(swords,src,hilt,tip,.85,j>4)
  end
  im:drawImage(rays);im:drawImage(swords)
  local bx,by=project(0,0,0)
  rect(im,math.floor(bx-8),math.floor(by-62),16,16,c(179,170,153));rect(im,math.floor(bx-10),math.floor(by-64),20,7,c(40,46,59))
  rect(im,math.floor(bx-13),math.floor(by-45),26,31,c(66,76,92));rect(im,math.floor(bx-12),math.floor(by-14),10,25,c(40,46,59));rect(im,math.floor(bx+3),math.floor(by-14),10,25,c(40,46,59))
  rect(im,math.floor(bx-19),math.floor(by-43),6,28,c(40,46,59));rect(im,math.floor(bx+13),math.floor(by-43),6,25,c(97,111,130))
  if side then
   local held=Image(400,280,ColorMode.RGB);lineTexture(held,gold,{12,20,10},{12,20,73},1)
   local two=Image(128,64,ColorMode.RGB);two:drawSprite(split,(f-1)%24+1);lineTexture(held,two,{12,20,63},{12,20,-60},.7)
   im:drawImage(held)
   -- unobtrusive ground direction: left/back, right/forward; the flame rings move to the left/back.
   local x0,y0=project(0,-17,-55);local x1,y1=project(0,-17,75)
   for k=0,80 do local t=k/80;px(im,x0+(x1-x0)*t,y0+(y1-y0)*t,c(65,76,94))end
   for k=0,5 do px(im,x1-k,y1-k*.2,c(134,145,158));px(im,x1-k,y1-k,c(134,145,158))end
  end
  s:newCel(s.layers[1],f,im)
 end
 s:saveAs(root..name..'.aseprite');local large=Sprite(s);large:resize(1000,700);large:saveCopyAs(root..name..'.gif');local still=Image(1000,700,ColorMode.RGB);still:drawSprite(large,18);still:saveAs(root..name..'.png')
 return root..name..'.gif'
end
local front=makeView('liaoli-rings-front-preview-v6',false);local oblique=makeView('liaoli-rings-oblique-preview-v6',true)
return {front=front,oblique=oblique,ringCount=3,approved=false}
