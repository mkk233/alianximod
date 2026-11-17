-- Latest user reference: fire originates behind the caster, grows onto handles, gold blades and broad red plumes.
local root='D:/alianximod/docs/资源库/过程归档/火系六式-20261029/'
local pc=app.pixelColor
local function c(r,g,b,a)return pc.rgba(r,g,b,a or 255)end
local function px(im,x,y,color)x=math.floor(x);y=math.floor(y);if x>=0 and x<im.width and y>=0 and y<im.height then im:drawPixel(x,y,color)end end
local function rect(im,x,y,w,h,color)for yy=y,y+h-1 do for xx=x,x+w-1 do px(im,xx,yy,color)end end end
local function poly(im,p,color)
 local x0,x1,y0,y1=999,-999,999,-999
 for _,a in ipairs(p)do x0=math.min(x0,a[1]);x1=math.max(x1,a[1]);y0=math.min(y0,a[2]);y1=math.max(y1,a[2])end
 for y=math.floor(y0),math.ceil(y1)do for x=math.floor(x0),math.ceil(x1)do local inside=false;local j=#p
  for i=1,#p do local a,b=p[i],p[j];if(a[2]>y)~=(b[2]>y)and x<(b[1]-a[1])*(y-a[2])/(b[2]-a[2])+a[1]then inside=not inside end;j=i end
  if inside then px(im,x,y,color)end
 end end
end
local function export(s,name)
 s:saveAs(root..name..'.aseprite');local im=Image(s.width,s.height*#s.frames,ColorMode.RGB)
 for f=1,#s.frames do im:drawSprite(s,f,Point(0,(f-1)*s.height))end;im:saveAs(root..name..'-strip.png')
end
local blade=Image{fromFile=root..'liaoli-orbit-sword-v3.png'}
local lit=Image(blade)
for it in lit:pixels()do local p=it();if pc.rgbaA(p)>0 then local v=pc.rgbaR(p)+pc.rgbaG(p)+pc.rgbaB(p);it(v>360 and c(255,249,147)or v>170 and c(255,224,32)or c(255,154,18))end end
lit:saveAs(root..'liaoli-golden-sword-v5.png');local litSource=Sprite(64,20,ColorMode.RGB);litSource:newCel(litSource.layers[1],1,lit);litSource:saveAs(root..'liaoli-golden-sword-v5.aseprite')
local plume=Sprite(128,48,ColorMode.RGB);plume.layers[1].name='Broad red plume and gold streams'
for f=1,24 do
 if f>1 then plume:newEmptyFrame()end;plume.frames[f].duration=.05
 local im=Image(128,48,ColorMode.RGB);local phase=(f-1)/24*math.pi*2
 for side=-1,1,2 do
  local wob=math.sin(phase)*2
  poly(im,{{4,24},{20,24+side*4},{40,24+side*7},{62,24+side*12},{78,24+side*(19+wob)},{94,24+side*15},{126,24+side*22},{113,24+side*12},{98,24+side*8},{110,24+side*4},{88,24+side*5},{78,24+side*2},{51,24+side*4},{27,24+side*2}},c(207,32,13,225))
  poly(im,{{14,24},{39,24+side*5},{59,24+side*9},{78,24+side*(16+wob)},{87,24+side*12},{117,24+side*17},{103,24+side*10},{80,24+side*7},{63,24+side*3},{38,24+side*2}},c(248,61,10,245))
  poly(im,{{3,24},{24,24+side*3},{49,24+side*4},{64,24+side*9},{75,24+side*12},{81,24+side*9},{105,24+side*13},{81,24+side*7},{74,24+side*9},{63,24+side*6},{47,24+side*2},{21,24+side}},c(255,161,12,250))
  -- moving slender golden filaments cross the red sheet with a small travelling wave.
  for x=2,122 do local t=x/124;local y=24+side*(1+11*t+math.sin(x*.1-phase*2)*3*t)
   if (x-f*4)%48<36 then px(im,x,y,c(255,227,34));if x<62 then px(im,x,y+side,c(255,246,107))end end
  end
 end
 poly(im,{{1,23},{20,21},{38,22},{58,21},{78,23},{100,24},{78,26},{57,27},{36,26},{18,27},{1,25}},c(255,190,18))
 poly(im,{{2,24},{22,22},{45,24},{65,22},{86,24},{61,25},{40,25},{20,26}},c(255,235,38))
 for j=0,7 do local x=(f*4+j*16)%123;local y=24+(j%2==0 and 1 or -1)*(5+j%4*4);rect(im,x,y,2,1,c(255,249,174))end
 plume:newCel(plume.layers[1],f,im)
end
plume:newTag(1,24).name='flow_from_back_loop';export(plume,'liaoli-back-plume-v5')
local intro=Sprite(128,48,ColorMode.RGB);intro.layers[1].name='Back to handle ignition'
for f=1,12 do if f>1 then intro:newEmptyFrame()end;intro.frames[f].duration=.05
 local im=Image(128,48,ColorMode.RGB);im:drawSprite(plume,f);local front=math.floor(127*(f/12)^1.2)
 for it in im:pixels()do if it.x>front then it(0)end end
 intro:newCel(intro.layers[1],f,im)
end
intro:newTag(1,12).name='ignite_once';export(intro,'liaoli-back-ignite-v5')
local split=Sprite(128,64,ColorMode.RGB);split.layers[1].name='Two side streams on thrust sword'
for f=1,24 do if f>1 then split:newEmptyFrame()end;split.frames[f].duration=.05
 local im=Image(128,64,ColorMode.RGB);local phase=(f-1)/24*math.pi*2
 for side=-1,1,2 do
  for x=5,122 do local t=(x-5)/117;local gap=2+15*math.sin(t*math.pi*.65);local mid=32+side*(gap+math.sin(t*10-phase*2)*2*t)
   local width=(2+2.8*math.sin(t*math.pi))*(1-t*.55)
   for y=math.floor(mid-width-2),math.ceil(mid+width+2)do local d=math.abs(y-mid)
    if d<width+2 then px(im,x,y,c(226,43,15,235))end
    if d<width then px(im,x,y,c(255,124,10))end
    if d<width*.56 then px(im,x,y,c(255,221,31))end
    if d<.75 and (x-f*4)%32<21 then px(im,x,y,c(255,251,176))end
   end
   if x%18==f%18 then poly(im,{{x,mid},{x+5,mid+side*5},{x+13,mid+side*8},{x+7,mid+side*2}},c(250,79,11))end
  end
 end
 split:newCel(split.layers[1],f,im)
end
split:newTag(1,24).name='split_left_right_loop';export(split,'liaoli-thrust-split-v5')
local function stamp(im,texture,cx,cy,a,sx,sy)
 local ca,sa=math.cos(a),math.sin(a)
 local x0,x1,y0,y1=999,-999,999,-999
 for _,u in ipairs({0,texture.width*sx})do for _,v in ipairs({-texture.height*sy/2,texture.height*sy/2})do local x=cx+ca*u-sa*v;local y=cy+sa*u+ca*v;x0=math.min(x0,x);x1=math.max(x1,x);y0=math.min(y0,y);y1=math.max(y1,y)end end
 for y=math.floor(y0),math.ceil(y1)do for x=math.floor(x0),math.ceil(x1)do local dx,dy=x-cx,y-cy
  local tx=math.floor((dx*ca+dy*sa)/sx);local ty=math.floor((-dx*sa+dy*ca)/sy+texture.height/2)
  if tx>=0 and ty>=0 and tx<texture.width and ty<texture.height then local p=texture:getPixel(tx,ty);if pc.rgbaA(p)>0 then px(im,x,y,p)end end
 end end
end
local function body(im)
 rect(im,153,104,14,14,c(179,170,153));rect(im,151,102,18,6,c(40,46,59));rect(im,148,119,24,31,c(66,76,92));rect(im,151,120,3,26,c(97,111,130))
 rect(im,142,120,6,27,c(40,46,59));rect(im,172,120,6,27,c(40,46,59));rect(im,148,150,10,24,c(40,46,59));rect(im,163,150,10,24,c(40,46,59));rect(im,149,146,23,3,c(151,67,55))
end
local orbit=Sprite(320,224,ColorMode.RGB);orbit.layers[1].name='Back ignition and sword fan'
local direct=Sprite(224,128,ColorMode.RGB);direct.layers[1].name='Direct thrust with twin streams'
local angles={-180,-154,-128,-102,-78,-52,-26,0}
for f=1,52 do
 if f>1 then orbit:newEmptyFrame();direct:newEmptyFrame()end;orbit.frames[f].duration=.05;direct.frames[f].duration=.05
 local im=Image(320,224,ColorMode.RGB);im:clear(c(20,26,38));local active=f<=40
 if active then local flames=Image(320,224,ColorMode.RGB)
  for j,degrees in ipairs(angles)do local a=degrees*math.pi/180;local fx=Image(128,48,ColorMode.RGB)
   local pair=math.min(j,9-j)
   fx:drawSprite(f<=12 and intro or plume,f<=12 and f or (f+pair*3-1)%24+1)
   stamp(flames,fx,160,126,a,.98,.75)
  end
  im:drawImage(flames,Point(0,0),f>36 and math.floor((41-f)/5*255)or 255)
  local blades=Image(320,224,ColorMode.RGB)
  for _,degrees in ipairs(angles)do local a=degrees*math.pi/180;local src=Image(blade);local front=127*(math.min(12,f)/12)^1.2
   for it in src:pixels()do local p=it();if pc.rgbaA(p)>0 and 23+it.x*.62<front then it(lit:getPixel(it.x,it.y))end end
   stamp(blades,src,160+math.cos(a)*23,126+math.sin(a)*23,a,.62,.7)
  end;im:drawImage(blades)
 end;body(im);rect(im,88,202,144,1,c(52,62,78));if active then rect(im,88,202,math.floor(144*f/40),1,c(240,171,49))end;orbit:newCel(orbit.layers[1],f,im)
 local directIm=Image(224,128,ColorMode.RGB);directIm:clear(c(20,26,38));local dl=Image(224,128,ColorMode.RGB)
 rect(directIm,62,40,14,14,c(179,170,153));rect(directIm,60,38,18,6,c(40,46,59))
 rect(directIm,58,55,22,31,c(66,76,92));rect(directIm,59,86,8,22,c(40,46,59));rect(directIm,72,86,8,22,c(40,46,59))
 rect(directIm,80,62,7,17,c(66,76,92));rect(directIm,87,73,18,6,c(179,170,153))
 stamp(dl,active and lit or blade,103,75,-.22,1.6,1.2)
 if active then local fx=Image(128,64,ColorMode.RGB);fx:drawSprite(split,(f-1)%24+1)
  if f<9 then local front=128*f/9;for it in fx:pixels()do if it.x>front then it(0)end end end
  stamp(dl,fx,188,56,math.pi-.22,1.25,1.1)
 end
 directIm:drawImage(dl,Point(0,0),f>36 and f<=40 and math.floor((41-f)/5*255)or 255)
 rect(directIm,30,113,164,1,c(52,62,78));if active then rect(directIm,30,113,math.floor(164*f/40),1,c(240,171,49))end;direct:newCel(direct.layers[1],f,directIm)
end
orbit:saveAs(root..'liaoli-back-origin-preview-v5.aseprite');direct:saveAs(root..'liaoli-thrust-split-preview-v5.aseprite')
local big=Sprite(orbit);big:resize(960,672);big:saveCopyAs(root..'liaoli-back-origin-preview-v5.gif');local still=Image(960,672,ColorMode.RGB);still:drawSprite(big,22);still:saveAs(root..'liaoli-back-origin-preview-v5.png')
local dbig=Sprite(direct);dbig:resize(896,512);dbig:saveCopyAs(root..'liaoli-thrust-split-preview-v5.gif');local ds=Image(896,512,ColorMode.RGB);ds:drawSprite(dbig,22);ds:saveAs(root..'liaoli-thrust-split-preview-v5.png')
return {orbit=root..'liaoli-back-origin-preview-v5.gif',direct=root..'liaoli-thrust-split-preview-v5.gif',approved=false}
