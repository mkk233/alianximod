local root='D:/alianximod/docs/资源库/过程归档/周身水环3D验证_v7_双缘锯齿旋流-20261027/'
local W,H,N=256,40,16
local pc=app.pixelColor
local C={pc.rgba(14,48,85,255),pc.rgba(18,91,137,255),pc.rgba(32,145,183,255),pc.rgba(91,201,218,255),pc.rgba(181,236,235,255),pc.rgba(243,252,241,255)}
local function put(im,x,y,c)
 x=math.floor(x)%W;y=math.floor(y)
 if y>=0 and y<im.height then im:drawPixel(x,y,c) end
end
local function poly(im,p,c)
 local lo,hi=im.height-1,0
 for _,v in ipairs(p) do lo=math.min(lo,v[2]);hi=math.max(hi,v[2]) end
 for y=math.max(0,math.floor(lo)),math.min(im.height-1,math.ceil(hi)) do
  local xs={}
  for i=1,#p do local a,b=p[i],p[i%#p+1]
   if (a[2]<=y+.5 and b[2]>y+.5) or (b[2]<=y+.5 and a[2]>y+.5) then xs[#xs+1]=a[1]+(y+.5-a[2])*(b[1]-a[1])/(b[2]-a[2]) end
  end
  table.sort(xs)
  for i=1,#xs-1,2 do for x=math.ceil(xs[i]),math.floor(xs[i+1]) do put(im,x,y,c) end end
 end
end
local function line(im,x1,y1,x2,y2,c,width)
 local n=math.max(math.abs(x2-x1),math.abs(y2-y1),1)
 for t=0,n do local u=t/n
  for dy=0,(width or 1)-1 do put(im,x1+(x2-x1)*u,y1+(y2-y1)*u+dy,c) end
 end
end
local function splash(file,seed,count,inner)
 local s=Sprite(W,H,ColorMode.RGB)
 local layers={s.layers[1],s:newLayer(),s:newLayer(),s:newLayer()}
 layers[1].name='连续青蓝浪根';layers[2].name='逆时针弯折多锯齿';layers[3].name='分叉尖端与局部白沫';layers[4].name='顺流脱落水滴'
 local atlas=Image(W,H*N,ColorMode.RGB)
 for f=0,N-1 do
  if f>0 then s:newEmptyFrame() end;s.frames[f+1].duration=.1
  local im={Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB)}
  for x=0,W-1 do
   local top=34+math.floor(1.5*math.sin(x*.16+f*math.pi/8))
   for y=top,H-1 do put(im[1],x,y,y>37 and C[2] or C[3]) end
   if ((x+seed)%37)<18 then put(im[1],x,top+1,C[4]) end
  end
  for j=0,count-1 do
   local phase=(f+j*5+seed)%16
   local a=phase*math.pi/8
   local pulse=.5+.5*math.sin(a)
   local x=j*W/count+2*math.sin(j*2.39+seed)
   local span=9+(j*7+seed)%8
   local height=12+(j*11+seed)%11+3*pulse
   local lean=6+(j*3+seed)%7+2*pulse
   local base=35
   local tipx=x+lean
   local tipy=base-height
   poly(im[2],{{x-5,base+2},{x-3,base-3},{x+1,base-8},{tipx-5,tipy+4},{tipx,tipy},{tipx-2,tipy+6},{tipx-3,tipy+10},{x+5,base-4},{x+span-2,base+2}},C[2])
   poly(im[2],{{x-3,base},{x,base-5},{tipx-4,tipy+6},{tipx,tipy},{tipx-2,tipy+7},{x+4,base-5},{x+span-3,base}},C[3])
   poly(im[3],{{x-1,base-5},{tipx-4,tipy+5},{tipx,tipy},{tipx-2,tipy+4},{tipx-4,tipy+8},{x+2,base-6}},C[4])
   if j%3~=1 then
    line(im[3],tipx-3,tipy+3,tipx,tipy,C[5],1)
    if j%3==0 then put(im[3],tipx,tipy,C[6]) end
   end
   if j%4~=2 then
    local by=base-6-height*.25
    poly(im[2],{{x+4,base-2},{x+5,by+3},{x+span+3,by-3},{x+span,by+3},{x+9,base-1}},C[3])
    line(im[3],x+8,by+1,x+span+3,by-3,C[4],1)
   end
   if phase>=4 and phase<=12 and j%3~=1 then
    local t=(phase-4)/8
    local dx=tipx+2+8*t
    local dy=tipy-2-5*math.sin(t*math.pi)+5*t
    local size=2+(j%2)
    if phase>=11 then size=1 end
    poly(im[4],{{dx,dy+size},{dx+size+2,dy},{dx+size+1,dy+size},{dx-1,dy+size+1}},j%2==0 and C[5] or C[4])
    if phase<10 and j%4==0 then put(im[4],dx+4,dy-2,C[5]) end
   end
  end
  local combined=Image(W,H,ColorMode.RGB)
  for i=1,4 do s:newCel(layers[i],f+1,im[i],Point(0,0));combined:drawImage(im[i],Point(0,0)) end
  atlas:drawImage(combined,Point(0,f*H))
 end
 s:saveAs(root..file..'.aseprite');atlas:saveAs(root..file..'.png')
 return {file=file,frames=#s.frames,layers=#s.layers,clusters=count}
end
local reports={splash('saw_splash_outer',0,20,false),splash('saw_splash_inner',7,16,true)}
local s=Sprite(W,24,ColorMode.RGB);local ls={s.layers[1],s:newLayer(),s:newLayer()}
ls[1].name='断续青蓝主弧';ls[2].name='分离追赶水块';ls[3].name='错相短流线'
local atlas=Image(W,24*N,ColorMode.RGB)
local segments={{2,72},{102,88},{207,24}}
for f=0,N-1 do
 if f>0 then s:newEmptyFrame() end;s.frames[f+1].duration=.1
 local im={Image(W,24,ColorMode.RGB),Image(W,24,ColorMode.RGB),Image(W,24,ColorMode.RGB)}
 for k,p in ipairs(segments) do
  local x=p[1];local l=p[2];local shift=math.floor(2*math.sin((f+k*3)*math.pi/8))
  poly(im[1],{{x,15},{x+7,9},{x+24,6},{x+l-12+shift,3},{x+l,7},{x+l-3,15},{x+l-15,19},{x+8,21}},C[2])
  poly(im[1],{{x+5,12},{x+24,7},{x+l-12+shift,3},{x+l,7},{x+l-5,11},{x+24,15}},C[3])
  poly(im[1],{{x+9,11},{x+30,7},{x+l-14+shift,4},{x+l-5,6},{x+l-16,8},{x+35,10}},C[4])
  for n=0,2 do
   local xx=x+14+n*17+3*math.sin((f+n*4)*math.pi/8)
   line(im[3],xx,13+n*2,xx+9+n*2,12+n*2,n%2==0 and C[4] or C[3],1)
  end
 end
 for k=0,2 do
  local phase=(f+k*5)%16;local t=phase/16
  local starts={74,190,234};local x=starts[k+1]+t*22
  local len=8-3*math.sin(t*math.pi)
  poly(im[2],{{x,14},{x+len-3,9},{x+len+3,9},{x+len,14},{x+1,16}},C[3])
  line(im[3],x+len-2,10,x+len+2,9,C[4],1)
 end
 local combined=Image(W,24,ColorMode.RGB)
 for i=1,3 do s:newCel(ls[i],f+1,im[i],Point(0,0));combined:drawImage(im[i],Point(0,0)) end
 atlas:drawImage(combined,Point(0,f*24))
end
s:saveAs(root..'broken_saw_flow.aseprite');atlas:saveAs(root..'broken_saw_flow.png')
return {splash=reports,body={frames=#s.frames,layers=#s.layers}}

