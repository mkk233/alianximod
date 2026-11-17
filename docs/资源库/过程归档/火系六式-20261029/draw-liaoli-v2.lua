-- Aseprite-made draft: red flame sheets and sharp gold rays inspired by the user's card reference.
local root='D:/alianximod/docs/资源库/过程归档/火系六式-20261029/'
local pc=app.pixelColor
local function col(r,g,b,a) return pc.rgba(r,g,b,a or 255) end
local dark=col(104,16,32,200);local red=col(195,27,31,240);local vermilion=col(245,56,24,250)
local gold=col(255,177,30);local yellow=col(255,226,73);local white=col(255,252,194)
local function pixel(im,x,y,c) x=math.floor(x);y=math.floor(y);if x>=0 and x<im.width and y>=0 and y<im.height then im:drawPixel(x,y,c) end end
local function rect(im,x,y,w,h,c) for yy=y,y+h-1 do for xx=x,x+w-1 do pixel(im,xx,yy,c) end end end
local function poly(im,pts,c)
 local minx,maxx,miny,maxy=1000,-1000,1000,-1000
 for _,p in ipairs(pts) do minx=math.min(minx,p[1]);maxx=math.max(maxx,p[1]);miny=math.min(miny,p[2]);maxy=math.max(maxy,p[2]) end
 for y=math.floor(miny),math.ceil(maxy) do for x=math.floor(minx),math.ceil(maxx) do
  local b=false;local j=#pts
  for i=1,#pts do local a,z=pts[i],pts[j];if(a[2]>y)~=(z[2]>y) and x<(z[1]-a[1])*(y-a[2])/(z[2]-a[2])+a[1] then b=not b end;j=i end
  if b then pixel(im,x,y,c) end
 end end
end
local function ray(im,cx,cy,angle,len,width,c,bend)
 local dx,dy=math.cos(angle),math.sin(angle);local nx,ny=-dy,dx
 poly(im,{{cx+nx*width,cy+ny*width},{cx+dx*len*.34+nx*(width*1.3+bend),cy+dy*len*.34+ny*(width*1.3+bend)},
 {cx+dx*len*.58+nx*(width*.26+bend),cy+dy*len*.58+ny*(width*.26+bend)},
 {cx+dx*len,cy+dy*len},{cx+dx*len*.7-nx*width*.23,cy+dy*len*.7-ny*width*.23},
 {cx+dx*len*.4-nx*width*.7,cy+dy*len*.4-ny*width*.7},{cx-nx*width,cy-ny*width}},c)
end
local function sword()
 local im=Image(128,96,ColorMode.RGB)
 poly(im,{{17,63},{99,38},{108,37},{102,43},{19,68}},col(117,130,147))
 poly(im,{{17,63},{99,38},{108,37},{21,65}},col(190,199,206))
 poly(im,{{14,58},{17,57},{24,71},{20,74}},col(99,104,116))
 poly(im,{{7,68},{16,65},{18,69},{9,73}},col(115,80,63))
 return im
end
local aura=Sprite(128,96,ColorMode.RGB);aura.layers[1].name='Split flame mantle'
for f=1,16 do
 if f>1 then aura:newEmptyFrame() end;aura.frames[f].duration=.05
 local im=Image(128,96,ColorMode.RGB);local p=(f-1)*math.pi*2/16
 for pass=1,3 do
  for j=0,4 do local x=34+j*15;local y=61-j*4.55;local wave=math.sin(p+j*1.2)
   for side=-1,1,2 do
    local angle=math.pi-.3+side*(.72+.22*math.sin(p+j*.8))
    local len=(17+(j*7)%17+wave*2)*(pass==1 and 1.18 or pass==2 and 1 or .82)
    local width=(pass==1 and 7 or pass==2 and 5 or 1.45)
    local shade=(pass==1 and dark or pass==2 and red or yellow)
    ray(im,x,y,angle,len,width,shade,side*(2+wave))
   end
  end
 end
 -- Long layered red sheets curl along the thrust axis. The narrow core stays connected to the sword.
 poly(im,{{21,64},{39,53},{58,53},{74,43},{90,43},{110,35},{99,46},{80,49},{65,58},{43,58},{28,68}},vermilion)
 poly(im,{{22,64},{45,57},{60,57},{77,47},{91,46},{108,38},{96,46},{81,49},{64,59},{43,60},{26,67}},gold)
 poly(im,{{23,64},{63,52},{106,38},{100,42},{64,55},{25,67}},yellow)
 poly(im,{{31,63},{72,50},{105,39},{100,42},{71,52},{31,65}},white)
 for j=0,7 do local t=((f+j*3)%16)/16;local x=30+j*10-7*t;local y=57-j*2.4-(j%2==0 and 1 or -1)*(10+9*t)
  if t<.8 then ray(im,x,y,-.3,3+j%3,.5,gold,0) end
 end
 aura:newCel(aura.layers[1],f,im)
end
aura:newTag(1,16).name='sustain_loop';aura:saveAs(root..'liaoli-afterburners-aura-v2.aseprite')
local auraStrip=Image(128,96*16,ColorMode.RGB);for f=1,16 do auraStrip:drawSprite(aura,f,Point(0,(f-1)*96)) end;auraStrip:saveAs(root..'liaoli-afterburners-aura-v2-strip.png')
local hit=Sprite(96,96,ColorMode.RGB);hit.layers[1].name='Contact burst'
for f=1,10 do
 if f>1 then hit:newEmptyFrame() end;hit.frames[f].duration=.05
 local im=Image(96,96,ColorMode.RGB);local t=(f-1)/9;local expansion=math.sin(math.min(1,t*2)*math.pi/2)
 local fade=1-t*.72
 for pass=1,3 do for j=0,10 do
  local a=j*2*math.pi/11+.13*math.sin(j*3.1)
  local len=(21+(j*7)%19)*(.35+.65*expansion)
  local width=(pass==1 and 7 or pass==2 and 3.8 or 1.2)*fade
  local c=(pass==1 and col(202,26,33,math.floor(235*fade)) or pass==2 and col(255,171,20,math.floor(255*fade)) or col(255,237,112,math.floor(255*fade)))
  ray(im,48,48,a,len*(pass==3 and .85 or 1),width,c,math.sin(j)*2)
 end end
 if f<5 then for j=0,7 do ray(im,48,48,j*math.pi/4,12*(1-t),3,white,0) end end
 hit:newCel(hit.layers[1],f,im)
end
hit:newTag(1,10).name='hit_once';hit:saveAs(root..'liaoli-afterburners-hit-v2.aseprite')
local hitStrip=Image(96,960,ColorMode.RGB);for f=1,10 do hitStrip:drawSprite(hit,f,Point(0,(f-1)*96)) end;hitStrip:saveAs(root..'liaoli-afterburners-hit-v2-strip.png')
local preview=Sprite(192,112,ColorMode.RGB);preview.layers[1].name='Sword context preview'
for f=1,48 do
 if f>1 then preview:newEmptyFrame() end;preview.frames[f].duration=.05
 local im=Image(192,112,ColorMode.RGB);im:clear(col(22,27,39))
 local alpha= f<=3 and math.floor(f/3*255) or f>37 and math.max(0,math.floor((41-f)/4*255)) or 255
 local offset=f<=3 and (3-f)*2 or 0
 im:drawImage(sword(),Point(18-offset,10))
 if f<=40 then local fx=Image(128,96,ColorMode.RGB);fx:drawSprite(aura,(f-1)%16+1);im:drawImage(fx,Point(18-offset,10),alpha) end
 if f>=21 and f<=30 then local burst=Image(96,96,ColorMode.RGB);burst:drawSprite(hit,f-20);im:drawImage(burst,Point(76,2)) end
 -- timeline baseline: 2s hold + a short visual rest; damage burst shown once for demonstration.
 rect(im,18,104,156,1,col(49,59,77));if f<=40 then rect(im,18,104,math.floor(156*f/40),1,col(227,148,45)) end
 preview:newCel(preview.layers[1],f,im)
end
preview:saveAs(root..'liaoli-afterburners-preview-v2.aseprite')
local large=Sprite(preview);large:resize(960,560);large:saveCopyAs(root..'liaoli-afterburners-preview-v2.gif')
local board=Image(768,224,ColorMode.RGB);board:clear(col(22,27,39))
for i,f in ipairs({10,23}) do local shot=Image(192,112,ColorMode.RGB);shot:drawSprite(preview,f);shot:resize(384,224);board:drawImage(shot,Point((i-1)*384,0)) end
board:saveAs(root..'liaoli-afterburners-preview-v2.png')
return {preview=root..'liaoli-afterburners-preview-v2.gif',still=root..'liaoli-afterburners-preview-v2.png',auraFrames=16,hitFrames=10,approved=false}
