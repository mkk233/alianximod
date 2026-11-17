local root='D:/alianximod/docs/资源库/过程归档/周身水环3D验证_v8_细碎水膜毛边-20261027/'
local W,H,N=256,40,16
local pc=app.pixelColor
local C={pc.rgba(25,109,151,255),pc.rgba(40,151,185,255),pc.rgba(95,204,222,255),pc.rgba(162,231,237,255),pc.rgba(221,249,246,255),pc.rgba(246,254,250,255)}
local function pix(im,x,y,c)
 x=math.floor(x+.5)%W;y=math.floor(y+.5)
 if y>=0 and y<H then im:drawPixel(x,y,c) end
end
local function disc(im,cx,cy,rx,ry,c)
 for y=math.floor(cy-ry),math.ceil(cy+ry) do
  for x=math.floor(cx-rx),math.ceil(cx+rx) do
   if ((x-cx)/rx)^2+((y-cy)/ry)^2<=1.15 then pix(im,x,y,c) end
  end
 end
end
local function bez(p,t)
 local q=1-t
 return q*q*q*p[1][1]+3*q*q*t*p[2][1]+3*q*t*t*p[3][1]+t*t*t*p[4][1],q*q*q*p[1][2]+3*q*q*t*p[2][2]+3*q*t*t*p[3][2]+t*t*t*p[4][2]
end
local function strand(im,p,thick,c,highlight)
 for step=0,24 do
  local t=step/24;local x,y=bez(p,t);local r=thick*(1-.62*t)
  disc(im,x,y,math.max(.6,r),math.max(.6,r*.85),c)
  if highlight and step>5 and step<21 and step%2==0 then pix(highlight,x-.7,y,C[5]) end
 end
end
local function draw(name,seed,inner)
 local s=Sprite(W,H,ColorMode.RGB);local layers={s.layers[1],s:newLayer(),s:newLayer(),s:newLayer()}
 layers[1].name='连续薄水膜浪根';layers[2].name='不规则圆钝短舌与细叉';layers[3].name='水膜局部高光';layers[4].name='断离珠滴与细碎水团'
 local atlas=Image(W,H*N,ColorMode.RGB)
 local positions={1,5,12,21,25,29,39,53,58,64,71,75,84,94,99,104,118,126,130,137,143,151,160,164,170,179,189,194,201,212,219,224,231,242,247}
 local heights={4,3,7,4,9,4,3,5,3,5,4,10,3,5,3,6,4,5,9,4,3,5,3,6,4,10,3,5,3,5,8,4,5,3,6}
 for f=0,N-1 do
  if f>0 then s:newEmptyFrame() end;s.frames[f+1].duration=.1
  local im={Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB)}
  for x=0,W-1 do
   local edge=37+math.floor(.8*math.sin(x*.18)+.65*math.sin(x*.39+f*math.pi/8))
   for y=edge,H-1 do pix(im[1],x,y,y==H-1 and C[2] or C[3]) end
   if (x+seed)%29<11 then pix(im[3],x,edge+.1,C[4]) end
   if (x+seed)%53<5 then pix(im[3],x,edge+.1,C[5]) end
   if (x+seed)%47>=18 and (x+seed)%47<23 then pix(im[1],x,H-1,C[1]) end
  end
  for j,x0 in ipairs(positions) do
   local phase=(f+j*5+seed)%16;local t=phase/16;local pulse=(1-math.cos(2*math.pi*t))/2
   local h=heights[j]*(inner and .72 or 1)*(.65+.35*pulse)
   local x=(x0+(inner and 4 or 0))%W
   local drift=.7*math.sin((phase+2)*math.pi/8)
   local bend=1.5+(j%4)*.65+pulse
   local tipx=x+bend+drift
   local tipy=36-h
   local p={{x-1,38},{x+2,35},{x-1+drift,tipy+1},{tipx,tipy}}
   strand(im[2],p,1.05+(j%3)*.22,j%4==0 and C[2] or C[3],j%3~=1 and im[3] or nil)
   disc(im[2],tipx,tipy,.9+(j%3==0 and .4 or 0),.9,C[4])
   if j%3==0 then pix(im[3],tipx,tipy-.6,C[5]) end
   if j%5==0 then
    local p2={{x,36},{x+2,34},{x+5,35-h*.6},{x+5.5,34-h*.55}}
    strand(im[2],p2,.85,C[3],im[3])
   end
   if j%6==2 then
    local p3={{x+1,37},{x+4,33},{x+6,34},{x+5,35}}
    strand(im[2],p3,.7,C[4],nil)
   end
   if phase>=7 and phase<=13 and j%3~=0 then
    local age=(phase-7)/6
    local dx=tipx+1.3+age*4
    local dy=tipy-2-2.5*math.sin(age*math.pi)+age*1.5
    local radius=(j%4==0 and 1.5 or 1.0)*(1-.62*age)
    disc(im[4],dx,dy,math.max(.65,radius),math.max(.65,radius*(1.1-.2*age)),C[4])
    if age<.65 then pix(im[4],dx,dy-.6,C[5]) end
    if j%5==1 and age<.7 then pix(im[4],dx+3,dy+2,C[3]) end
   end
   if j%7==3 then
    local px=x+5+2*math.sin(phase*math.pi/8)
    local py=33+math.sin((phase+4)*math.pi/8)
    disc(im[2],px,py,1.4,.8,C[3]);pix(im[3],px,py-.3,C[5])
   end
  end
  local combined=Image(W,H,ColorMode.RGB)
  for k=1,4 do s:newCel(layers[k],f+1,im[k],Point(0,0));combined:drawImage(im[k],Point(0,0)) end
  atlas:drawImage(combined,Point(0,f*H))
 end
 s:saveAs(root..name..'.aseprite');atlas:saveAs(root..name..'.png')
 return {name=name,width=W,height=H,frames=#s.frames,layers=#s.layers}
end
return {draw('ragged_splash_outer',1,false),draw('ragged_splash_inner',9,true)}

