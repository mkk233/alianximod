
local OUT = 'D:/alianximod/docs/资源库/过程归档/燃烧v3_Blockbench挂载示意-20261101/'
local W,H,N = 96,112,20
local pc=app.pixelColor
local tau=math.pi*2
local function c(r,g,b,a) return pc.rgba(r,g,b,a or 255) end
local function px(im,x,y,col) x=math.floor(x);y=math.floor(y);if x>=0 and x<im.width and y>=0 and y<im.height then im:drawPixel(x,y,col) end end
local function box(im,x,y,w,h,col) for yy=math.floor(y),math.floor(y+h-1) do for xx=math.floor(x),math.floor(x+w-1) do px(im,xx,yy,col) end end end
local function poly(im,pts,col)
 local lo,hi=10000,-10000;for _,p in ipairs(pts) do lo=math.min(lo,p[2]);hi=math.max(hi,p[2]) end
 for y=math.floor(lo),math.ceil(hi) do local hits={};local scan=y+.5
  for j=1,#pts do local a,b=pts[j],pts[j%#pts+1];if (a[2]<=scan and b[2]>scan) or (b[2]<=scan and a[2]>scan) then hits[#hits+1]=a[1]+(scan-a[2])*(b[1]-a[1])/(b[2]-a[2]) end end
  table.sort(hits);for j=1,#hits-1,2 do for x=math.ceil(hits[j]),math.floor(hits[j+1]) do px(im,x,y,col) end end
 end
end
local function tongue(im,bx,by,ht,wd,phase,lean,t,heat)
 local height=ht*(1+.12*math.sin(t*tau+phase))
 for y=math.floor(by-height),by do
  local u=(by-y)/height
  local bend=lean*u+math.sin(u*6.5-t*tau+phase)*wd*.50*u
  local center=bx+bend
  local half=wd*(1-u)^.83*(.9+.18*math.sin(u*10-t*tau+phase))
  for x=math.floor(center-half),math.ceil(center+half) do
   local r=math.abs(x-center)/(half+.01)
   if r<=1 then
    local col
    if heat then
     if r>.73 then col=c(153,45,20,145) elseif r>.35 then col=c(244,94,27,205) else col=c(255,177,64,235) end
     if r<.16 and u<.35 then col=c(255,220,126,245) end
    else
     if r>.77 then col=c(170,36,26,235) elseif r>.47 then col=c(241,67,24,250) elseif r>.20 then col=c(255,139,32,255) else col=c(255,219,100,255) end
     if r<.12 and u<.27 then col=c(255,245,190,255) end
    end
    px(im,x,y,col)
   end
  end
 end
end
local function heatWisp(im,bx,by,ht,phase,t)
 for y=by-ht,by do local u=(by-y)/ht;local x=bx+3*math.sin(u*5-t*tau+phase); local width=1+math.floor(2*(1-u)); local alpha=math.floor(65*(1-u));box(im,x,y,width,1,c(255,117,38,alpha)) end
end
local function spark(im,x,y,size,alpha,hot)
 local base=hot and c(255,159,43,alpha) or c(243,119,36,alpha)
 box(im,x,y,size,size+1,base)
 px(im,x,y,hot and c(255,240,160,alpha) or c(255,204,106,alpha))
 if size>1 then px(im,x+1,y-1,c(255,205,88,alpha));px(im,x-1,y+size,c(233,85,27,math.floor(alpha*.5))) end
end

local src=app.open('D:/alianximod/docs/资源库/特效/火焰/燃烧_双手附焰与火焰披风_v3/overdrive-v3.aseprite')
local names={'cape','rear_right','rear_left','shoulder_right','shoulder_left','abdomen_right','abdomen_left','hip_left','shin_right','shin_left','hand_right','hand_left','embers'}
local s=Sprite(W,H,ColorMode.RGB);local ls={s.layers[1]}; for i=2,#names do ls[i]=s:newLayer() end
for i,n in ipairs(names) do ls[i].name=n end
local sheets={};for i=1,#names do sheets[i]=Image(W,H*N,ColorMode.RGB);sheets[i]:clear() end
local patches={{39,54,10,2.8,1,1},{57,51,9,2.5,4,-1},{41,68,17,3.2,3,2},{50,66,15,3.3,0,2},{57,76,13,2.5,5,-1},{39,94,16,3.1,2,1},{54,94,15,3,6,-1}}
local mismatches=0
for f=1,N do
 if f>1 then s:newEmptyFrame() end;s.frames[f].duration=.08
 local originals={}; for i=1,5 do originals[i]=Image(W,H,ColorMode.RGB);originals[i]:clear();local cel=src.layers[i]:cel(f);if cel then originals[i]:drawImage(cel.image,cel.position) end end
 local imgs={};for i=1,#names do imgs[i]=Image(W,H,ColorMode.RGB);imgs[i]:clear() end
 imgs[1]:drawImage(originals[1]); imgs[13]:drawImage(originals[5])
 for y=0,H-1 do for x=0,W-1 do
  local ri=x<48 and 2 or 3;imgs[ri]:drawPixel(x,y,originals[2]:getPixel(x,y))
  local hi=x<48 and 11 or 12;imgs[hi]:drawPixel(x,y,originals[4]:getPixel(x,y))
 end end
 local t=(f-1)/N
 for i,v in ipairs(patches) do tongue(imgs[i+3],v[1],v[2],v[3],v[4],v[5],v[6],t,false) end
 for y=0,H-1 do for x=0,W-1 do
  local occupied=false;local finalPixel=0
  for i=10,4,-1 do local pxv=imgs[i]:getPixel(x,y);if pc.rgbaA(pxv)>0 then if occupied then imgs[i]:drawPixel(x,y,0) else occupied=true;finalPixel=pxv end end end
  if finalPixel~=originals[3]:getPixel(x,y) then mismatches=mismatches+1 end
 end end
 for i=1,#names do s:newCel(ls[i],f,imgs[i],Point(0,0));sheets[i]:drawImage(imgs[i],Point(0,(f-1)*H)) end
end
s:saveAs(OUT..'burn-v3-spatial-parts.aseprite')
for i,n in ipairs(names) do sheets[i]:saveAs(OUT..'burn-v3-'..n..'-strip.png') end
return {frames=N,layers=#names,bodyPixelMismatches=mismatches,source=OUT..'burn-v3-spatial-parts.aseprite'}
