local s=Sprite(64,32,ColorMode.RGB)
local body=s.layers[1];body.name='浪体轮廓'
local shade=s:newLayer();shade.name='卷浪暗部与流线'
local foam=s:newLayer();foam.name='流动白沫'
local spray=s:newLayer();spray.name='浪尖飞沫'
local pc=app.pixelColor.rgba
local blue=pc(20,157,205,255)
local dark=pc(12,80,145,255)
local mid=pc(15,117,184,255)
local cyan=pc(66,211,229,255)
local pale=pc(146,240,245,255)
local white=pc(231,255,255,255)
local function poly(im,pts,c)
 for y=0,31 do for x=0,63 do
  local inside=false;local j=#pts
  for i=1,#pts do local a,b=pts[i],pts[j]
   if ((a[2]>y)~=(b[2]>y)) and x<(b[1]-a[1])*(y-a[2])/(b[2]-a[2])+a[1] then inside=not inside end
   j=i
  end
  if inside then im:drawPixel(x,y,c) end
 end end
end
local function line(im,x0,y0,x1,y1,c,width)
 x0=math.floor(x0+.5);y0=math.floor(y0+.5);x1=math.floor(x1+.5);y1=math.floor(y1+.5)
 local dx=math.abs(x1-x0);local dy=-math.abs(y1-y0);local sx=x0<x1 and 1 or -1;local sy=y0<y1 and 1 or -1;local err=dx+dy
 while true do
  for yy=0,(width or 1)-1 do if x0>=0 and x0<64 and y0+yy>=0 and y0+yy<32 then im:drawPixel(x0,y0+yy,c) end end
  if x0==x1 and y0==y1 then break end
  local e=2*err;if e>=dy then err=err+dy;x0=x0+sx end;if e<=dx then err=err+dx;y0=y0+sy end
 end
end
local contour={{0,29},{8,27},{16,24},{23,20},{28,14},{32,9},{38,5},{45,3},{51,4},{57,7},{60,11},{60,15},{57,18},{53,18},{50,16},{50,13},{53,12},{55,13},{55,10},{51,8},{47,9},{43,13},{41,18},{44,22},{51,25},{59,27},{64,29},{60,31},{49,30},{37,31},{25,29},{15,31},{5,30}}
local atlas=Image(64,512,ColorMode.RGB)
local function warp(pts,f)
 local out={};local phase=f*math.pi/8
 for _,p in ipairs(pts) do local k=math.max(0,math.min(1,(p[1]-18)/35));local high=math.max(0,(27-p[2])/26)
  table.insert(out,{math.floor(p[1]+1.5*math.sin(phase)*k+.5),math.floor(p[2]+1.7*math.cos(phase)*k*high+.5)})
 end
 return out
end
for f=0,15 do
 if f>0 then s:newEmptyFrame() end;s.frames[f+1].duration=.1
 local b=Image(64,32,ColorMode.RGB);local d=Image(64,32,ColorMode.RGB);local h=Image(64,32,ColorMode.RGB);local q=Image(64,32,ColorMode.RGB)
 local pts=warp(contour,f);poly(b,pts,blue)
 poly(d,warp({{5,29},{18,25},{26,21},{33,14},{40,10},{44,10},{40,15},{36,23},{42,28},{55,30},{36,30},{26,28},{15,30}},f),mid)
 poly(d,warp({{19,27},{29,22},{36,16},{39,14},{35,23},{40,27},{48,29},{35,29},{27,27}},f),dark)
 local edge=warp({{0,29},{8,27},{16,24},{23,20},{28,14},{32,9},{38,5},{45,3},{51,4},{57,7},{60,11},{60,15},{57,18},{53,18},{50,16},{50,13},{53,12},{55,13}},f)
 for i=1,#edge-1 do local a,bp=edge[i],edge[i+1]
  line(h,a[1],a[2],bp[1],bp[2],cyan,3)
  if (i+math.floor(f/2))%5~=0 then line(h,a[1],a[2],bp[1],bp[2],pale,2) end
  if i>3 and (i+f)%4<2 then line(h,a[1],a[2],bp[1],bp[2],white,1) end
 end
 for j=0,2 do
  local x=(f*3+j*21)%56;local y=27-math.floor(7*math.sin((x/56)*math.pi))
  line(h,x,y,x+6,y-2,cyan,1)
 end
 for y=0,31 do for x=0,63 do if app.pixelColor.rgbaA(b:getPixel(x,y))==0 then d:drawPixel(x,y,0) end end end
 for j=0,2 do
  local t=((f+j*5)%16)/16
  if t<.8 then
   local x=math.floor(48+j*3+9*t);local y=math.floor(3+j*2-5*math.sin(math.pi*t))
   if x>=0 and x<63 and y>=0 and y<31 then
    q:drawPixel(x,y,pale);q:drawPixel(x+1,y,white)
    if t<.45 then q:drawPixel(x,y+1,cyan) end
   end
  end
 end
 s:newCel(body,f+1,b,Point(0,0));s:newCel(shade,f+1,d,Point(0,0));s:newCel(foam,f+1,h,Point(0,0));s:newCel(spray,f+1,q,Point(0,0))
 b:drawImage(d,Point(0,0));b:drawImage(h,Point(0,0));b:drawImage(q,Point(0,0));atlas:drawImage(b,Point(0,f*32))
end
s:saveAs('D:/alianximod/docs/资源库/过程归档/周身水环3D验证_v3_Aseprite浪-20261027/curl_wave.aseprite')
atlas:saveAs('D:/alianximod/docs/资源库/过程归档/周身水环3D验证_v3_Aseprite浪-20261027/curl_wave.png')
return {frames=#s.frames,layers=#s.layers,width=s.width,height=s.height}

