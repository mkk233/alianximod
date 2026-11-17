
local OUT = 'D:/alianximod/docs/资源库/过程归档/炉心升温燃烧_重设计_v2-20261031/'
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
local function build(name,hot)
 local spr=Sprite(W,H,ColorMode.RGB)
 local layers={spr.layers[1],spr:newLayer(),spr:newLayer(),spr:newLayer()}
 layers[1].name='01 rear heat';layers[2].name='02 rear tongues';layers[3].name='03 front tongues';layers[4].name='04 rising embers'
 for f=1,N do
  if f>1 then spr:newEmptyFrame() end
  spr.frames[f].duration=.08
  local t=(f-1)/N
  local ims={Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB),Image(W,H,ColorMode.RGB)}
  for _,im in ipairs(ims) do im:clear() end
  if hot then
   heatWisp(ims[1],14,99,62,1,t);heatWisp(ims[1],80,96,67,3,t)
   local flames={{24,94,58,8,1,-4},{72,96,64,8,4,4},{33,65,30,7,2,1},{63,64,32,7,5,-1},{40,99,31,7,3,-2},{57,99,37,6,6,2}}
   for _,v in ipairs(flames) do tongue(ims[2],v[1],v[2],v[3],v[4],v[5],v[6],t,false) end
   local front={{28,81,30,5,3,-3},{68,82,34,5,5,3},{37,97,24,5,0,2},{59,97,28,5,2,-2},{43,77,13,3,4,-2},{55,65,17,3,1,2}}
   for _,v in ipairs(front) do tongue(ims[3],v[1],v[2],v[3],v[4],v[5],v[6],t,false) end
   -- Detached shards peel away from the flame tips, with staggered phases.
   for j=1,17 do local life=(t+j*.61803398875)%1;local side=j%2==0 and -1 or 1;local x=48+side*(18+(j%4)*3+life*6)+math.sin(life*6+j)*2;local y=99-life*92;local alpha=math.floor(245*math.min(1,life*8,(1-life)*6));spark(ims[4],x,y,j%4==0 and 2 or 1,alpha,true) end
  else
   heatWisp(ims[1],25,95,52,1,t);heatWisp(ims[1],69,94,57,3,t)
   local flames={{28,84,26,3,1,-1},{68,84,32,3,4,2},{37,98,22,3,3,-1},{59,98,25,3,5,1}}
   for _,v in ipairs(flames) do tongue(ims[2],v[1],v[2],v[3],v[4],v[5],v[6],t,true) end
   -- Visible heated edges on forearms and torso, with a quiet upward cadence.
   tongue(ims[3],29,70,17,1.8,1,1,t,true)
   tongue(ims[3],66,69,21,1.8,4,-1,t,true)
   tongue(ims[3],39,69,21,2.2,2,1,t,true)
   tongue(ims[3],57,67,17,2.0,5,-1,t,true)
   local pulse=.6+.4*(.5+.5*math.sin(t*tau))
   box(ims[3],37,42,8,1,c(255,162,57,math.floor(220*pulse)))
   box(ims[3],51,42,8,1,c(255,162,57,math.floor(220*pulse)))
   box(ims[3],37,43,2,4,c(238,88,24,180))
   box(ims[3],57,43,2,4,c(238,88,24,180))
   -- Two staggered ember seams sweep upward without creating a ground ring.

   for side=0,1 do
    local cycle=(t+side*.5)%1;local y=90-cycle*42;local x=side==0 and 36 or 59
    local a=math.floor(205*math.sin(cycle*math.pi));box(ims[3],x,y,2,7,c(247,110,32,a));box(ims[3],x+1,y+2,1,3,c(255,195,83,a))
   end
   for j=1,6 do local life=(t+j*.61803398875)%1;local side=j%2==0 and -1 or 1;local x=48+side*(17+j%3*3)+math.sin(life*5+j)*2;local y=96-life*65;local alpha=math.floor(225*math.min(1,life*6,(1-life)*5));spark(ims[4],x,y,1,alpha,false) end
  end
  for j=1,4 do spr:newCel(layers[j],f,ims[j],Point(0,0)) end
 end
 local tag=spr:newTag(1,N);tag.name='sustain_loop';tag.aniDir=AniDir.FORWARD
 spr:saveAs(OUT..name..'.aseprite')
 local strip=Image(W,H*N,ColorMode.RGB);strip:clear()
 local back=Image(W,H*N,ColorMode.RGB);back:clear()
 local front=Image(W,H*N,ColorMode.RGB);front:clear()
 for f=1,N do
  strip:drawSprite(spr,f,Point(0,(f-1)*H))
  for j=1,4 do local cel=layers[j]:cel(f);if j<=2 then back:drawImage(cel.image,Point(0,(f-1)*H)) else front:drawImage(cel.image,Point(0,(f-1)*H)) end end
 end
 strip:saveAs(OUT..name..'-strip.png');back:saveAs(OUT..name..'-back-strip.png');front:saveAs(OUT..name..'-front-strip.png')
 return spr
end
local heat=build('heat_up-v2',false)
local burn=build('overdrive-v2',true)

local function entry(base,name,hot)
 local spr=Sprite(W,H,ColorMode.RGB)
 local layers={spr.layers[1],spr:newLayer(),spr:newLayer(),spr:newLayer()}
 for j=1,4 do layers[j].name=base.layers[j].name end
 for f=1,8 do
  if f>1 then spr:newEmptyFrame() end;spr.frames[f].duration=.06
  local p=(f-1)/7;local power=1-(1-p)^2
  for j=1,4 do
   local im=Image(W,H,ColorMode.RGB);im:clear()
   local src=base.layers[j]:cel(1).image
   for y=0,H-1 do for x=0,W-1 do
    local pix=src:getPixel(x,y);local alpha=pc.rgbaA(pix)
    if alpha>0 and y>H-(H+6)*power then px(im,x,y,c(pc.rgbaR(pix),pc.rgbaG(pix),pc.rgbaB(pix),math.floor(alpha*power))) end
   end end
   if j==4 and f<8 then
    for k=1,(hot and 12 or 6) do
     local angle=k*tau/(hot and 12 or 6)+.35
     local radius=(hot and 8 or 5)+p*(hot and 26 or 16)
     local x=48+math.cos(angle)*radius;local y=73+math.sin(angle)*radius*.72-p*13
     spark(im,x,y,hot and 2 or 1,math.floor(math.sin(p*math.pi)*235),hot)
    end
   end
   spr:newCel(layers[j],f,im,Point(0,0))
  end
 end
 local tag=spr:newTag(1,8);tag.name='activation_once'
 spr:saveAs(OUT..name..'-entry.aseprite')
 local strip=Image(W,H*8,ColorMode.RGB);strip:clear();for f=1,8 do strip:drawSprite(spr,f,Point(0,(f-1)*H)) end;strip:saveAs(OUT..name..'-entry-strip.png')
 return spr
end
local heatEntry=entry(heat,'heat_up-v2',false)
local burnEntry=entry(burn,'overdrive-v2',true)

local mannequin=Image(W,H,ColorMode.RGB);mannequin:clear()
local edge,shadow,base,light=c(36,48,63),c(61,77,94),c(83,102,120),c(113,132,147)
box(mannequin,39,23,18,17,edge);box(mannequin,40,24,16,15,base);box(mannequin,40,24,16,3,light);box(mannequin,53,27,3,12,shadow)
box(mannequin,36,40,24,31,edge);box(mannequin,37,40,22,29,base);box(mannequin,37,40,22,3,light);box(mannequin,55,43,4,26,shadow)
box(mannequin,26,40,10,31,edge);box(mannequin,27,41,8,28,base);box(mannequin,27,41,2,27,light)
box(mannequin,60,40,10,31,edge);box(mannequin,61,41,8,28,shadow);box(mannequin,61,41,2,28,base)
box(mannequin,36,70,12,28,edge);box(mannequin,37,71,10,25,shadow);box(mannequin,37,71,2,25,base)
box(mannequin,48,70,12,28,edge);box(mannequin,49,71,10,25,shadow);box(mannequin,49,71,2,25,base)
local glyph={C={'01111','10000','10000','10000','10000','10000','01111'},G={'01111','10000','10000','10111','10001','10001','01111'},A={'01110','10001','10001','11111','10001','10001','10001'},B={'11110','10001','10001','11110','10001','10001','11110'},D={'11110','10001','10001','10001','10001','10001','11110'},E={'11111','10000','10000','11110','10000','10000','11111'},F={'11111','10000','10000','11110','10000','10000','10000'},H={'10001','10001','10001','11111','10001','10001','10001'},I={'111','010','010','010','010','010','111'},L={'10000','10000','10000','10000','10000','10000','11111'},M={'10001','11011','10101','10101','10001','10001','10001'},N={'10001','11001','11001','10101','10011','10011','10001'},O={'01110','10001','10001','10001','10001','10001','01110'},P={'11110','10001','10001','11110','10000','10000','10000'},R={'11110','10001','10001','11110','10100','10010','10001'},S={'01111','10000','10000','01110','00001','00001','11110'},T={'11111','00100','00100','00100','00100','00100','00100'},U={'10001','10001','10001','10001','10001','10001','01110'},V={'10001','10001','10001','10001','10001','01010','00100'},W={'10001','10001','10001','10101','10101','11011','10001'},Y={'10001','10001','01010','00100','00100','00100','00100'},['2']={'01110','10001','00001','00110','01000','10000','11111'},['/']={'00001','00010','00010','00100','01000','01000','10000'},['-']={'00000','00000','00000','11111','00000','00000','00000'}}
local function text(im,str,x,y,col)
 for j=1,#str do local ch=str:sub(j,j);local pat=glyph[ch];if pat then for yy,row in ipairs(pat) do for xx=1,#row do if row:sub(xx,xx)=='1' then px(im,x+xx-1,y+yy-1,col) end end end end;x=x+(pat and #pat[1]+1 or 4) end
end
local preview=Sprite(248,154,ColorMode.RGB);preview.layers[1].name='preview only - mannequin and labels'
for f=1,N*2+8 do
 if f>1 then preview:newEmptyFrame() end;preview.frames[f].duration=f<=8 and .06 or .08
 local sourceFrame=f<=8 and f or ((f-9)%N+1)
 local im=Image(248,154,ColorMode.RGB);im:clear(c(16,23,33))
 box(im,8,26,112,120,c(23,33,46));box(im,128,26,112,120,c(23,33,46))
 text(im,'FURNACE / REDESIGN 2',10,8,c(165,184,198))
 text(im,f<=8 and 'START' or 'LOOP',203,8,c(205,145,77))
 text(im,'HEAT UP',18,31,c(255,196,112));text(im,'OVERDRIVE',138,31,c(255,150,88))
 for index,spr in ipairs(f<=8 and {heatEntry,burnEntry} or {heat,burn}) do
  local baseX=index==1 and 16 or 136;local baseY=39
  local tmp=Image(W,H,ColorMode.RGB);tmp:clear()
  for j=1,2 do tmp:drawImage(spr.layers[j]:cel(sourceFrame).image) end
  tmp:drawImage(mannequin)
  for j=3,4 do tmp:drawImage(spr.layers[j]:cel(sourceFrame).image) end
  im:drawImage(tmp,Point(baseX,baseY))
 end
 preview:newCel(preview.layers[1],f,im,Point(0,0))
end
preview:saveAs(OUT..'comparison-v2.aseprite')
local still=Image(248,154,ColorMode.RGB);still:drawSprite(preview,14);still:resize(992,616);still:saveAs(OUT..'comparison-v2.png')
print('Created 2 layered effect sprites + comparison, 20 frames x 80ms, no runtime edits.')
return {folder=OUT,frames=N,width=W,height=H}
