
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

local s=app.open(OUT..'burn-v3-spatial-parts.aseprite')
local src=app.open('D:/alianximod/docs/资源库/特效/火焰/燃烧_双手附焰与火焰披风_v3/overdrive-v3.aseprite')
for _,l in ipairs(s.layers) do if l.name=='embers' then l.isVisible=false end end
local names={'hand_embers_right','hand_embers_left','body_embers'};local ls={};local sheets={}
for i,n in ipairs(names) do ls[i]=s:newLayer();ls[i].name=n;sheets[i]=Image(W,H*N,ColorMode.RGB);sheets[i]:clear() end
local bad=0
for f=1,N do local t=(f-1)/N;local ims={};for i=1,3 do ims[i]=Image(W,H,ColorMode.RGB);ims[i]:clear() end
 for side=0,1 do local bx=side==0 and 31 or 65;local life=(t+side*.53)%1;tongue(ims[side+1],bx+math.sin(life*6+side)*4,54-life*20,5,1.5,side,1,t,false) end
 for j=1,15 do local life=(t+j*.61803398875)%1;local side=j%2==0 and -1 or 1;local x=48+side*(16+j%4*3)+math.sin(life*6+j)*4;local y=98-life*72;local a=math.floor(235*math.min(1,life*8,(1-life)*5));spark(ims[3],x,y,j%5==0 and 2 or 1,a,true) end
 local orig=Image(W,H,ColorMode.RGB);orig:clear();local cel=src.layers[5]:cel(f);orig:drawImage(cel.image,cel.position)
 for y=0,H-1 do for x=0,W-1 do local got=false;local pxv=0;for j=3,1,-1 do local col=ims[j]:getPixel(x,y);if pc.rgbaA(col)>0 then if got then ims[j]:drawPixel(x,y,0) else got=true;pxv=col end end end;if pxv~=orig:getPixel(x,y) and (pc.rgbaA(pxv)>0 or pc.rgbaA(orig:getPixel(x,y))>0) then bad=bad+1 end end end
 for i=1,3 do s:newCel(ls[i],f,ims[i],Point(0,0));sheets[i]:drawImage(ims[i],Point(0,(f-1)*H)) end
end
s:saveAs(OUT..'burn-v3-spatial-parts.aseprite');for i,n in ipairs(names) do sheets[i]:saveAs(OUT..'burn-v3-'..n..'-strip.png') end
return {emberPixelMismatches=bad,activeLayers=15}
