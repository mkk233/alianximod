-- User reference: swords spread around the character, fire flows along each blade direction.
local root='D:/alianximod/docs/资源库/过程归档/火系六式-20261029/'
local pc=app.pixelColor
local function c(r,g,b,a) return pc.rgba(r,g,b,a or 255) end
local function px(im,x,y,color) x=math.floor(x);y=math.floor(y);if x>=0 and x<im.width and y>=0 and y<im.height then im:drawPixel(x,y,color) end end
local function rect(im,x,y,w,h,color) for yy=y,y+h-1 do for xx=x,x+w-1 do px(im,xx,yy,color) end end end
local function poly(im,p,color)
 local x0,x1,y0,y1=999,-999,999,-999
 for _,a in ipairs(p) do x0=math.min(x0,a[1]);x1=math.max(x1,a[1]);y0=math.min(y0,a[2]);y1=math.max(y1,a[2]) end
 for y=math.floor(y0),math.ceil(y1) do for x=math.floor(x0),math.ceil(x1) do
  local inside=false;local j=#p
  for i=1,#p do local a,b=p[i],p[j];if(a[2]>y)~=(b[2]>y) and x<(b[1]-a[1])*(y-a[2])/(b[2]-a[2])+a[1] then inside=not inside end;j=i end
  if inside then px(im,x,y,color) end
 end end
end
local sword=Sprite(64,20,ColorMode.RGB);sword.layers[1].name='Sword motif'
local blade=Image(64,20,ColorMode.RGB)
poly(blade,{{3,8},{14,7},{16,5},{23,5},{24,8},{57,8},{63,10},{57,13},{24,13},{22,16},{17,16},{14,13},{3,12}},c(40,42,48))
poly(blade,{{23,8},{53,7},{63,10},{53,12},{23,13}},c(75,78,84))
poly(blade,{{24,8},{53,7},{63,10},{53,9},{24,10}},c(138,142,144))
poly(blade,{{13,7},{18,5},{23,5},{24,8},{20,9},{18,13},{14,13}},c(193,48,57))
poly(blade,{{15,8},{20,6},{22,7},{19,9},{17,12},{15,12}},c(247,88,63))
poly(blade,{{27,9},{38,9},{43,10},{39,11},{28,11}},c(242,192,80))
poly(blade,{{47,9},{53,8},{56,9},{51,10}},c(255,234,135))
rect(blade,3,9,11,2,c(119,92,76));sword:newCel(sword.layers[1],1,blade);sword:saveAs(root..'liaoli-orbit-sword-v3.aseprite');blade:saveAs(root..'liaoli-orbit-sword-v3.png')
local stream=Sprite(96,32,ColorMode.RGB);stream.layers[1].name='Directional fire stream'
for f=1,24 do
 if f>1 then stream:newEmptyFrame() end;stream.frames[f].duration=.05
 local im=Image(96,32,ColorMode.RGB);local phase=(f-1)/24*math.pi*2
 -- Long motion-aligned tongues, with travelling breaks and off-axis red sheets.
 for x=0,94 do
  local t=x/95;local fade=1-t
  local mid=16+math.sin(x*.075-phase*2)*(.7+2.3*t)
  local width=(2+math.sin(x*.14-phase*2)*.65)*(1-t*.68)
  local outer=width+2+math.sin(x*.1-phase*2)*1.2
  for y=math.floor(mid-outer),math.ceil(mid+outer) do
   local d=math.abs(y-mid)
   if d<outer and t<.82+.12*math.sin(phase+x*.08) then px(im,x,y,c(222,42,23,math.floor(200*(1-t*.45)))) end
   if d<width+1 and t<.9 then px(im,x,y,c(255,106,22,math.floor(250*(1-t*.45)))) end
   if d<width and t<.82 and ((x-f*4)%28<22 or x<16) then px(im,x,y,c(255,204,48,math.floor(255*(1-t*.35)))) end
   if d<.6 and t<.52 and ((x-f*4)%24<14 or x<12) then px(im,x,y,c(255,249,182)) end
  end
 end
 for side=-1,1,2 do
  for j=0,2 do local lead=14+j*22+((f*3+j*5)%18);local yy=16+side*(3+j%2*2)
   poly(im,{{lead-15,yy},{lead-8,yy+side*2},{lead+3,yy+side*5},{lead+13,yy+side*6},{lead+4,yy+side*2},{lead-2,yy}},c(233,53,25,210))
   poly(im,{{lead-8,yy+side},{lead+2,yy+side*3},{lead+10,yy+side*4},{lead+3,yy+side*2}},c(255,159,34,230))
  end
 end
 for j=0,4 do local x=(f*4+j*19)%92;local y=16+(j%2==0 and 1 or -1)*(8+j%4);rect(im,x,y,2+j%2,1,c(255,190,55,210)) end
 stream:newCel(stream.layers[1],f,im)
end
stream:newTag(1,24).name='outward_flow_loop';stream:saveAs(root..'liaoli-orbit-stream-v3.aseprite')
local strip=Image(96,32*24,ColorMode.RGB);for f=1,24 do strip:drawSprite(stream,f,Point(0,(f-1)*32)) end;strip:saveAs(root..'liaoli-orbit-stream-v3-strip.png')
local function stamp(im,texture,cx,cy,angle,sx,sy)
 local ca,sa=math.cos(angle),math.sin(angle)
 for yy=0,texture.height-1 do for xx=0,texture.width-1 do local p=texture:getPixel(xx,yy)
  if pc.rgbaA(p)>0 then local u=xx*sx;local v=(yy-texture.height/2)*sy
   local x=cx+ca*u-sa*v;local y=cy+sa*u+ca*v
   px(im,x,y,p);if sx>=.8 then px(im,x+1,y,p) end
  end
 end end
end
local function body(im)
 local skin=c(179,170,153);local coat=c(66,76,92);local edge=c(97,111,130);local shade=c(40,46,59)
 rect(im,153,104,14,14,skin);rect(im,151,102,18,6,shade)
 rect(im,148,119,24,31,coat);rect(im,151,120,3,26,edge)
 rect(im,142,120,6,27,shade);rect(im,172,120,6,27,shade)
 rect(im,148,150,10,24,shade);rect(im,163,150,10,24,shade)
 rect(im,148,172,11,3,edge);rect(im,163,172,11,3,edge)
 rect(im,149,146,23,3,c(151,67,55))
end
local preview=Sprite(320,224,ColorMode.RGB);preview.layers[1].name='Character and sword fan preview'
local angles={-174,-148,-122,-96,-70,-44,-18,10}
for f=1,48 do
 if f>1 then preview:newEmptyFrame() end;preview.frames[f].duration=.05
 local im=Image(320,224,ColorMode.RGB);im:clear(c(20,26,38))
 local active=f<=40;local expand=f<5 and f/5 or 1
 for j,degrees in ipairs(angles) do
  local a=degrees*math.pi/180;local ca,sa=math.cos(a),math.sin(a);local radius=23*expand
  local x,y=160+ca*radius,126+sa*radius
  if active then
   -- eight independent swords fan out around the shoulders and sides; no rotation of the whole formation.
   stamp(im,blade,x,y,a,.62,.7)
   if f>=4 then local flame=Image(96,32,ColorMode.RGB);flame:drawSprite(stream,(f+j*3-1)%24+1)
    stamp(im,flame,x+ca*39,y+sa*39,a,.66,.6)
   end
  end
 end
 body(im)
 rect(im,88,202,144,1,c(52,62,78));if f<=40 then rect(im,88,202,math.floor(144*f/40),1,c(240,171,49)) end
 preview:newCel(preview.layers[1],f,im)
end
preview:saveAs(root..'liaoli-orbit-preview-v3.aseprite')
local enlarged=Sprite(preview);enlarged:resize(960,672);enlarged:saveCopyAs(root..'liaoli-orbit-preview-v3.gif')
local still=Image(960,672,ColorMode.RGB);still:drawSprite(enlarged,20);still:saveAs(root..'liaoli-orbit-preview-v3.png')
return {preview=root..'liaoli-orbit-preview-v3.gif',still=root..'liaoli-orbit-preview-v3.png',swords=8,approved=false}
