-- Aseprite MCP draft authoring. Six effects; no game integration or approved assets.
local root='D:/alianximod/docs/资源库/过程归档/火系六式-20261029/'
local pc=app.pixelColor
local pi=math.pi
local function rgba(r,g,b,a) return pc.rgba(r,g,b,a or 255) end
local colors={rgba(133,33,35,210),rgba(211,51,30,240),rgba(248,91,28),rgba(255,163,47),rgba(255,218,101),rgba(255,247,196)}
local function px(im,x,y,c) x=math.floor(x);y=math.floor(y);if x>=0 and x<im.width and y>=0 and y<im.height then im:drawPixel(x,y,c) end end
local function rect(im,x,y,w,h,c) for yy=y,y+h-1 do for xx=x,x+w-1 do px(im,xx,yy,c) end end end
local function poly(im,pts,c)
 local xmin,xmax,ymin,ymax=999,-999,999,-999
 for _,v in ipairs(pts) do xmin=math.min(xmin,v[1]);xmax=math.max(xmax,v[1]);ymin=math.min(ymin,v[2]);ymax=math.max(ymax,v[2]) end
 for y=math.floor(ymin),math.ceil(ymax) do for x=math.floor(xmin),math.ceil(xmax) do
  local inside=false;local j=#pts
  for i=1,#pts do local a,b=pts[i],pts[j];if (a[2]>y)~=(b[2]>y) and x<(b[1]-a[1])*(y-a[2])/(b[2]-a[2])+a[1] then inside=not inside end;j=i end
  if inside then px(im,x,y,c) end
 end end
end
local function flame(im,x,y,h,w,p,intensity)
 local sway=math.sin(p)*w*.7
 local pts={{x-w,y},{x-w*.9,y-h*.27},{x-w*1.1,y-h*.48},{x-w*.25,y-h*.40},{x-w*.45,y-h*.77},{x+sway,y-h},{x+w*.55,y-h*.72},{x+w*.5,y-h*.52},{x+w*.95,y-h*.67},{x+w,y-h*.27},{x+w*.6,y}}
 poly(im,pts,colors[2]);
 poly(im,{{x-w*.66,y},{x-w*.55,y-h*.3},{x-w*.35,y-h*.54},{x+sway*.7,y-h*.83},{x+w*.26,y-h*.57},{x+w*.66,y-h*.33},{x+w*.48,y}},colors[3]);
 poly(im,{{x-w*.36,y},{x-w*.28,y-h*.27},{x+sway*.23,y-h*.56},{x+w*.35,y-h*.26},{x+w*.24,y}},colors[4]);
 if intensity then poly(im,{{x-1,y},{x-1,y-h*.18},{x+sway*.1,y-h*.35},{x+1,y-h*.18},{x+1,y}},colors[5]) end
end
local function ellipse(im,cx,cy,rx,ry,thickness,c,phase,broken)
 for y=math.floor(cy-ry-2),math.ceil(cy+ry+2) do for x=math.floor(cx-rx-2),math.ceil(cx+rx+2) do
  local dx=(x-cx)/rx;local dy=(y-cy)/ry;local d=math.sqrt(dx*dx+dy*dy)
  local angle=math.atan(dy,dx)
  if math.abs(d-1)<thickness and (not broken or math.sin(angle*7+phase)>.1) then px(im,x,y,c) end
 end end
end
local function sparks(im,count,frame,scale,bright)
 for j=1,count do
  local age=((frame*2+j*7)%24)/24
  local x=32+math.sin(j*3.88+age*.7)*(10+j%9)*scale
  local y=49-age*(22+j%17)*scale
  if age>.1 and age<.88 then rect(im,math.floor(x),math.floor(y),1,(j%3==0 and 2 or 1),colors[bright and 5 or 4]) end
 end
end
local function reference(kind)
 local im=Image(64,64,ColorMode.RGB)
 local dim=rgba(79,88,99);local light=rgba(112,124,138);local dark=rgba(50,59,72)
 if kind=='liaoyuan' then
  poly(im,{{3,43},{29,30},{61,44},{34,61}},dark)
  for j=0,4 do local x=9+j*8;rect(im,x,44+j%2*5,6,1,dim) end
 elseif kind=='liaoli' then
  poly(im,{{13,38},{48,25},{54,25},{51,30},{17,42}},light)
  poly(im,{{13,38},{48,25},{51,26},{16,40}},rgba(178,185,185))
  poly(im,{{10,35},{13,34},{20,44},{17,46}},dim)
  poly(im,{{8,41},{13,39},{15,43},{10,47}},rgba(106,76,57))
 else
  rect(im,28,18,8,8,light);rect(im,25,27,14,17,dim)
  rect(im,26,44,5,10,dark);rect(im,34,44,5,10,dark)
  rect(im,21,28,4,15,dark);rect(im,39,28,4,15,dark)
 end
 return im
end
local names={'liaoyuan','liaoli','fentian','heat_up','overdrive','cool'}
local sprites={};local exports={}
for _,name in ipairs(names) do
 local s=Sprite(64,64,ColorMode.RGB);s.layers[1].name='Effect';local layer=s.layers[1]
 for f=1,16 do
  if f>1 then s:newEmptyFrame() end;s.frames[f].duration=.075
  local im=Image(64,64,ColorMode.RGB);local p=2*pi*(f-1)/16
  if name=='liaoyuan' then
   ellipse(im,32,46,27,13,.065,colors[2],p,false)
   ellipse(im,32,46,25,11,.05,colors[4],p,true)
   for row=0,2 do for j=0,4 do
    local x=12+j*9+row%2*3;local y=39+row*7+(j%2)
    local dist=((x-32)/27)^2+((y-46)/13)^2
    if dist<.85 then local h=7+3*math.sin(p+j*1.7+row);flame(im,x,y,h,2.8,p+j+row,true) end
   end end
   for j=1,10 do px(im,11+j*4,46+math.sin(j*2.4)*7,colors[3]) end
  elseif name=='liaoli' then
   for j=0,5 do local x=18+j*6;local y=38-j*2.25;flame(im,x,y,8+3*math.sin(p+j),2.4,p+j,true) end
   poly(im,{{15,38},{48,25},{51,26},{18,40}},colors[5])
   if f>=9 and f<=12 then
    local r=(f-8)*2
    ellipse(im,51,26,r,r*.8,.18,colors[4],p,true)
    for j=0,5 do local a=j*pi/3+p*.1;rect(im,math.floor(51+math.cos(a)*(r+2)),math.floor(26+math.sin(a)*(r+2)),2,1,colors[5]) end
   end
  elseif name=='fentian' then
   local pulse=.65+.35*math.sin((f-1)%4/4*pi)
   ellipse(im,32,52,23,7,.11,colors[3],p,true)
   for j=0,6 do local a=j*2*pi/7;local x=32+math.cos(a)*11;local y=49+math.sin(a)*5
    flame(im,x,y,(16+j%3*4)*pulse,3.8,p+j,true)
   end
   sparks(im,16,f,.95,true)
  elseif name=='heat_up' then
   ellipse(im,32,51,17,5,.075,rgba(255,150,54,110),p,true)
   for j=0,3 do local a=j*pi/2+p*.15;local x=32+math.cos(a)*15;local y=47+math.sin(a)*5
    flame(im,x,y,4+math.sin(p+j)*1.5,1.6,p+j,false)
   end
   sparks(im,8,f,.85,false)
  elseif name=='overdrive' then
   ellipse(im,32,51,22,7,.11,colors[2],p,false)
   ellipse(im,32,51,19,6,.08,colors[4],p,true)
   for j=0,5 do local a=j*pi/3;local x=32+math.cos(a)*17;local y=48+math.sin(a)*5
    flame(im,x,y,8+math.sin(p+j)*3,2.4,p+j,true)
   end
   sparks(im,14,f,1.05,true)
  elseif name=='cool' then
   local t=(f-1)/15;local a=math.floor(230*(1-t)^.55);local r=7+23*t
   ellipse(im,32,41,r,r*.44,.11,rgba(191,213,226,a),p,false)
   ellipse(im,32,40,r*.91,r*.4,.06,rgba(241,248,248,a),p,true)
   for j=0,7 do local ang=j*pi/4;local cx=32+math.cos(ang)*r;local cy=40+math.sin(ang)*r*.4-t*5
    local rad=2+t*3
    for yy=math.floor(cy-rad),math.ceil(cy+rad) do for xx=math.floor(cx-rad),math.ceil(cx+rad) do
     if ((xx-cx)/rad)^2+((yy-cy)/(rad*.7))^2<1 and (xx+yy+j)%4~=0 then px(im,xx,yy,rgba(219,234,239,math.floor(a*.6))) end
    end end
   end
  end
  s:newCel(layer,f,im,Point(0,0))
 end
 local tag=s:newTag(1,16);tag.name=(name=='cool' and 'one_shot' or 'loop_preview')
 s.data='DRAFT v1. User visual approval pending. 64x64, 16 frames, 75ms/frame. No gameplay validation.'
 s:saveAs(root..name..'-v1.aseprite')
 local sheet=Image(64,64*16,ColorMode.RGB)
 for f=1,16 do sheet:drawSprite(s,f,Point(0,(f-1)*64)) end
 sheet:saveAs(root..name..'-v1-strip.png')
 sprites[name]=s;exports[#exports+1]=name
end
local board=Sprite(224,152,ColorMode.RGB);board.layers[1].name='Review board';local boardLayer=board.layers[1]
for f=1,16 do
 if f>1 then board:newEmptyFrame() end;board.frames[f].duration=.075
 local im=Image(224,152,ColorMode.RGB);im:clear(rgba(18,23,32))
 for i,name in ipairs(names) do local x=8+(i-1)%3*72;local y=8+math.floor((i-1)/3)*72
  rect(im,x-1,y-1,66,66,rgba(43,51,65));rect(im,x,y,64,64,rgba(25,32,44))
  im:drawImage(reference(name),Point(x,y))
  local effect=Image(64,64,ColorMode.RGB);effect:drawSprite(sprites[name],f)
  im:drawImage(effect,Point(x,y),255,BlendMode.NORMAL)
 end
 board:newCel(boardLayer,f,im,Point(0,0))
end
board:saveAs(root..'six-effects-review-v1.aseprite')
local enlarged=Sprite(board);enlarged:resize(896,608);enlarged:saveCopyAs(root..'six-effects-review-v1.gif')
local still=Image(896,608,ColorMode.RGB);still:drawSprite(enlarged,10);still:saveAs(root..'six-effects-review-v1.png')
return {drafts=exports,preview=root..'six-effects-review-v1.gif',still=root..'six-effects-review-v1.png',frames=16,approved=false}
