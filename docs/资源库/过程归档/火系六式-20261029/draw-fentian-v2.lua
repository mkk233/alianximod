-- Fentian v2 draft: overwhelming target-centred conflagration; each four-tick pulse flares visibly.
local root='D:/alianximod/docs/资源库/过程归档/火系六式-20261029/'
local pc=app.pixelColor
local function c(r,g,b,a)return pc.rgba(r,g,b,a or 255)end
local red=c(188,25,24);local orange=c(248,61,12);local amber=c(255,133,13);local gold=c(255,219,41);local white=c(255,251,182)
local function px(im,x,y,v)x=math.floor(x);y=math.floor(y);if x>=0 and x<im.width and y>=0 and y<im.height then im:drawPixel(x,y,v)end end
local function rect(im,x,y,w,h,v)for yy=y,y+h-1 do for xx=x,x+w-1 do px(im,xx,yy,v)end end end
local function poly(im,p,v)
 local x0,x1,y0,y1=999,-999,999,-999;for _,a in ipairs(p)do x0=math.min(x0,a[1]);x1=math.max(x1,a[1]);y0=math.min(y0,a[2]);y1=math.max(y1,a[2])end
 for y=math.floor(y0),math.ceil(y1)do for x=math.floor(x0),math.ceil(x1)do local inside=false;local j=#p
  for i=1,#p do local a,b=p[i],p[j];if(a[2]>y)~=(b[2]>y)and x<(b[1]-a[1])*(y-a[2])/(b[2]-a[2])+a[1]then inside=not inside end;j=i end
  if inside then px(im,x,y,v)end
 end end
end
local function tongue(im,x,y,h,w,phase,v)
 local sway=math.sin(phase)*w
 poly(im,{{x-w,y},{x-w*1.1,y-h*.25},{x-w*.65,y-h*.48},{x-w*.9,y-h*.73},{x-w*.15,y-h*.60},{x+sway,y-h},{x+w*.52,y-h*.72},{x+w*.3,y-h*.51},{x+w*.95,y-h*.65},{x+w*.73,y-h*.28},{x+w,y}},v)
end
local function ellipse(im,cx,cy,rx,ry,width,v)
 for y=math.floor(cy-ry-3),math.ceil(cy+ry+3)do for x=math.floor(cx-rx-3),math.ceil(cx+rx+3)do local d=math.sqrt(((x-cx)/rx)^2+((y-cy)/ry)^2);if math.abs(d-1)<width then px(im,x,y,v)end end end
end
local effect=Sprite(128,144,ColorMode.RGB);effect.layers[1].name='Conflagration and four-tick pulses'
for f=1,52 do if f>1 then effect:newEmptyFrame()end;effect.frames[f].duration=.05
 local im=Image(128,144,ColorMode.RGB);local phase=f*.65
 if f<4 then
  local r=29-f*5;ellipse(im,64,111,r,r*.28,.16,red);ellipse(im,64,111,r*.8,r*.2,.12,gold)
  for j=0,7 do local a=j*math.pi/4;local dist=21-f*4;rect(im,math.floor(64+math.cos(a)*dist),math.floor(92+math.sin(a)*dist),2,3,amber)end
 elseif f<=40 then
  local beat=(f-4)%4;local pulse=({1,.88,.71,.76})[beat+1];local h=90+18*pulse
  ellipse(im,64,118,39+beat*5,8+beat*2,.13,red);ellipse(im,64,118,34+beat*5,7+beat*2,.08,gold)
  tongue(im,39,117,h*.7,12,phase+2,red);tongue(im,88,118,h*.79,14,phase+4,red)
  tongue(im,64,123,h,25,phase*.5,red)
  tongue(im,42,117,h*.65,10,phase+2,orange);tongue(im,85,117,h*.75,11,phase+4,orange)
  tongue(im,64,121,h*.93,22,phase*.5+.18,orange)
  tongue(im,62,120,h*.87,17,phase*.5+.25,amber)
  tongue(im,64,121,h*.78,13,phase*.5+.3,gold)
  tongue(im,64,120,h*.64,7+pulse*3,phase*.5,white)
  -- Broad peripheral flame sheets and rising incandescent fragments, with negative gaps.
  for side=-1,1,2 do
   local x=64+side*23
   poly(im,{{x,111},{x+side*9,98},{x+side*15,79},{x+side*13,64},{x+side*24,38},{x+side*14,88},{x+side*18,100},{x+side*6,119}},orange)
   poly(im,{{x,111},{x+side*8,96},{x+side*12,77},{x+side*18,53},{x+side*10,94},{x+side*4,113}},gold)
  end
  for j=1,22 do local age=((f*3+j*7)%35)/35;local x=64+math.sin(j*2.4+age*.6)*(23+j%22);local y=115-age*(74+j%23)
   if age<.9 then rect(im,math.floor(x),math.floor(y),j%4==0 and 2 or 1,j%3==0 and 3 or 1,j%3==0 and white or gold)end
  end
  if beat==0 then
   poly(im,{{62,123},{61,71},{64,34},{67,71},{66,123}},c(255,255,225))
   for j=0,7 do local a=j*math.pi/4;local len=j%2==0 and 36 or 22;poly(im,{{62,92},{64+math.cos(a)*len,92+math.sin(a)*len},{66,94}},gold)end
  end
 else
  local t=(f-40)/12
  for j=0,29 do local a=j*2.4;local dist=18+31*t;local x=64+math.cos(a)*dist;local y=86+math.sin(a)*dist*.65-23*t
   if (j+f)%4~=0 then rect(im,math.floor(x),math.floor(y),1+(j%4==0 and 1 or 0),1+(j%3==0 and 1 or 0),j%3==0 and c(235,90,23,math.floor(255*(1-t)))or c(99,48,36,math.floor(230*(1-t))))end
  end
 end
 effect:newCel(effect.layers[1],f,im)
end
effect:newTag(1,3).name='gather';effect:newTag(4,40).name='ten_pulses';effect:newTag(41,52).name='embers'
effect:saveAs(root..'fentian-conflagration-v2.aseprite')
local strip=Image(128,144*52,ColorMode.RGB);for f=1,52 do strip:drawSprite(effect,f,Point(0,(f-1)*144))end;strip:saveAs(root..'fentian-conflagration-v2-strip.png')
local preview=Sprite(160,160,ColorMode.RGB);preview.layers[1].name='Target context preview'
for f=1,64 do if f>1 then preview:newEmptyFrame()end;preview.frames[f].duration=.05;local im=Image(160,160,ColorMode.RGB);im:clear(c(20,26,38))
 rect(im,73,80,14,14,c(143,151,163));rect(im,69,95,22,23,c(81,93,110));rect(im,63,96,6,22,c(58,68,85));rect(im,91,96,6,22,c(58,68,85));rect(im,69,118,9,13,c(58,68,85));rect(im,83,118,9,13,c(58,68,85))
 if f<=52 then local fx=Image(128,144,ColorMode.RGB);fx:drawSprite(effect,f);im:drawImage(fx,Point(16,8))end
 rect(im,28,149,104,1,c(56,66,82));if f<=40 then rect(im,28,149,math.floor(104*f/40),1,gold)end
 preview:newCel(preview.layers[1],f,im)
end
preview:saveAs(root..'fentian-conflagration-preview-v2.aseprite');local big=Sprite(preview);big:resize(640,640);big:saveCopyAs(root..'fentian-conflagration-preview-v2.gif');local still=Image(640,640,ColorMode.RGB);still:drawSprite(big,20);still:saveAs(root..'fentian-conflagration-preview-v2.png')
return {preview=root..'fentian-conflagration-preview-v2.gif',pulseTicks={4,8,12,16,20,24,28,32,36,40},approved=false}
