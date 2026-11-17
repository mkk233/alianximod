-- Liaoyuan v2: low connected ground fire, spreading outward.
local root='D:/alianximod/docs/资源库/过程归档/火系六式-20261029/'
local pc=app.pixelColor
local function c(r,g,b,a)return pc.rgba(r,g,b,a or 255)end
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
 local sway=math.sin(phase)*w*.8;poly(im,{{x-w,y},{x-w,y-h*.25},{x-w*.55,y-h*.5},{x-w*.8,y-h*.7},{x,y-h*.55},{x+sway,y-h},{x+w*.5,y-h*.75},{x+w*.35,y-h*.48},{x+w,y-h*.6},{x+w*.8,y-h*.25},{x+w,y}},v)
end
local function saveSheet(s,name)s:saveAs(root..name..'.aseprite');local im=Image(s.width,s.height*#s.frames,ColorMode.RGB);for f=1,#s.frames do im:drawSprite(s,f,Point(0,(f-1)*s.height))end;im:saveAs(root..name..'-strip.png')end
local flame=Sprite(32,40,ColorMode.RGB);flame.layers[1].name='Low rolling ground flame'
local embers=Sprite(64,64,ColorMode.RGB);embers.layers[1].name='Transient ground embers'
for f=1,24 do if f>1 then flame:newEmptyFrame();embers:newEmptyFrame()end;flame.frames[f].duration=.05;embers.frames[f].duration=.05
 local phase=f*math.pi*2/24;local im=Image(32,40,ColorMode.RGB)
 tongue(im,10,37,22+math.sin(phase)*4,6,phase+1,c(203,38,16));tongue(im,22,37,29+math.cos(phase)*3,7,phase+2,c(203,38,16))
 tongue(im,11,37,20+math.sin(phase)*4,5,phase+1,c(250,83,14));tongue(im,22,37,26+math.cos(phase)*3,5,phase+2,c(250,83,14))
 tongue(im,13,37,19,4,phase,c(255,175,28));tongue(im,22,37,23,3,phase+2,c(255,202,49))
 tongue(im,16,37,15,3,phase,c(255,237,106));flame:newCel(flame.layers[1],f,im)
 local ground=Image(64,64,ColorMode.RGB)
 for y=0,63 do for x=0,63 do local n=math.sin(x*.35+math.sin(y*.21)*3)+math.sin(y*.38+math.cos(x*.2)*2)
  if n>.85 then px(ground,x,y,c(140,34,12,130))end
  if n>1.6 then px(ground,x,y,c(235,69,15,190))end
  if n>1.9 and (x+y+f)%9<4 then px(ground,x,y,c(255,176,30,220))end
 end end;embers:newCel(embers.layers[1],f,ground)
end
flame:newTag(1,24).name='ground_loop';embers:newTag(1,24).name='ember_loop';saveSheet(flame,'liaoyuan-ground-flame-v2');saveSheet(embers,'liaoyuan-ground-embers-v2')
local wave=Sprite(128,128,ColorMode.RGB);wave.layers[1].name='Outward spreading front'
for f=1,12 do if f>1 then wave:newEmptyFrame()end;wave.frames[f].duration=.05;local im=Image(128,128,ColorMode.RGB);local rad=5+f*4.25
 for y=3,124 do for x=3,124 do local dx,dy=x-63.5,y-63.5;local d=math.sqrt(dx*dx+dy*dy);local a=math.atan(dy,dx);local edge=rad+math.sin(a*15-f*.8)*1.4
  if d>edge-3 and d<edge+3 then px(im,x,y,c(222,47,11,230))end
  if d>edge-1 and d<edge+1.4 then px(im,x,y,c(255,192,28))end
 end end;wave:newCel(wave.layers[1],f,im)
end
wave:newTag(1,12).name='spread_once';saveSheet(wave,'liaoyuan-spread-wave-v2')
local preview=Sprite(256,192,ColorMode.RGB);preview.layers[1].name='Ground field context'
local locations={}
for row=-3,3 do for col=-5,5 do local x=col*15+(row%2)*5;local y=row*10
 if(x/86)^2+(y/36)^2<.88 and (col*7+row*11)%4~=0 then locations[#locations+1]={x=x,y=y,seed=col*13+row*7}end
end end
table.sort(locations,function(a,b)return a.y<b.y end)
for f=1,72 do if f>1 then preview:newEmptyFrame()end;preview.frames[f].duration=.05;local im=Image(256,192,ColorMode.RGB);im:clear(c(20,26,38))
 poly(im,{{17,118},{128,72},{240,118},{129,174}},c(45,51,57))
 for j=-4,4 do local y=123+j*9;for x=31,224 do local dx=(x-128)/100;local dy=(y-123)/43;if dx*dx+dy*dy<1 then if (x+2*y)%37==0 then px(im,x,y,c(77,81,80))end end end end
 local spread=math.min(1,f/12);local rx=96*spread;local ry=43*spread;local field=Image(256,192,ColorMode.RGB)
 for y=80,168 do for x=30,226 do local d=((x-128)/math.max(1,rx))^2+((y-124)/math.max(1,ry))^2
  if d<1 then local n=math.sin(x*.24+math.sin(y*.27)*3)+math.sin(y*.5+f*.15)
   px(field,x,y,c(151,37,12,140));if n>1 then px(field,x,y,c(224,61,11,190))end;if n>1.65 then px(field,x,y,c(255,145,18,220))end
   if d>.9 then px(field,x,y,c(255,158,20,240))end
  end
 end end;im:drawImage(field)
 for _,loc in ipairs(locations)do local dist=(loc.x/96)^2+(loc.y/43)^2
  if dist<spread^2 then local fire=Image(32,40,ColorMode.RGB);fire:drawSprite(flame,(f+loc.seed)%24+1);local scale=.43+(loc.y+30)/150;fire:resize(math.floor(32*scale),math.floor(40*scale))
   im:drawImage(fire,Point(math.floor(128+loc.x-fire.width/2),math.floor(124+loc.y-fire.height)))
  end
 end
 if f<=12 then for j=0,27 do local a=j*math.pi*2/28;local x=128+math.cos(a)*rx;local y=124+math.sin(a)*ry;local h=6+math.sin(f+j)*2;tongue(im,x,y,h,2.7,f+j,c(255,190,27))end end
 rect(im,123,84,10,10,c(170,169,157));rect(im,120,95,16,21,c(82,96,116));rect(im,115,96,5,20,c(59,71,89));rect(im,136,96,5,20,c(59,71,89));rect(im,120,116,6,14,c(59,71,89));rect(im,130,116,6,14,c(59,71,89))
 if f>12 then local front=Image(32,40,ColorMode.RGB);front:drawSprite(flame,(f-1)%24+1);front:resize(17,21);im:drawImage(front,Point(112,117));im:drawImage(front,Point(137,120))end
 preview:newCel(preview.layers[1],f,im)
end
preview:crop(0,64,256,128);preview:saveAs(root..'liaoyuan-ground-sea-preview-v2.aseprite');local big=Sprite(preview);big:resize(1024,512);big:saveCopyAs(root..'liaoyuan-ground-sea-preview-v2.gif');local still=Image(1024,512,ColorMode.RGB);still:drawSprite(big,28);still:saveAs(root..'liaoyuan-ground-sea-preview-v2.png')
return {preview=root..'liaoyuan-ground-sea-preview-v2.gif',approved=false}
