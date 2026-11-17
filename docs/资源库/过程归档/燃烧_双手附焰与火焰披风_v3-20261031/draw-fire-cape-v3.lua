
local OUT = 'D:/alianximod/docs/资源库/过程归档/燃烧_双手附焰与火焰披风_v3-20261031/'
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
local mannequin=Image(W,H,ColorMode.RGB);mannequin:clear()
local edge,shadow,base,light=c(36,48,63),c(61,77,94),c(83,102,120),c(113,132,147)
box(mannequin,39,23,18,17,edge);box(mannequin,40,24,16,15,base);box(mannequin,40,24,16,3,light);box(mannequin,53,27,3,12,shadow)
box(mannequin,36,40,24,31,edge);box(mannequin,37,40,22,29,base);box(mannequin,37,40,22,3,light);box(mannequin,55,43,4,26,shadow)
box(mannequin,26,40,10,31,edge);box(mannequin,27,41,8,28,base);box(mannequin,27,41,2,27,light)
box(mannequin,60,40,10,31,edge);box(mannequin,61,41,8,28,shadow);box(mannequin,61,41,2,28,base)
box(mannequin,36,70,12,28,edge);box(mannequin,37,71,10,25,shadow);box(mannequin,37,71,2,25,base)
box(mannequin,48,70,12,28,edge);box(mannequin,49,71,10,25,shadow);box(mannequin,49,71,2,25,base)
local glyph={K={'10001','10010','10100','11000','10100','10010','10001'},['3']={'11110','00001','00001','01110','00001','00001','11110'},C={'01111','10000','10000','10000','10000','10000','01111'},G={'01111','10000','10000','10111','10001','10001','01111'},A={'01110','10001','10001','11111','10001','10001','10001'},B={'11110','10001','10001','11110','10001','10001','11110'},D={'11110','10001','10001','10001','10001','10001','11110'},E={'11111','10000','10000','11110','10000','10000','11111'},F={'11111','10000','10000','11110','10000','10000','10000'},H={'10001','10001','10001','11111','10001','10001','10001'},I={'111','010','010','010','010','010','111'},L={'10000','10000','10000','10000','10000','10000','11111'},M={'10001','11011','10101','10101','10001','10001','10001'},N={'10001','11001','11001','10101','10011','10011','10001'},O={'01110','10001','10001','10001','10001','10001','01110'},P={'11110','10001','10001','11110','10000','10000','10000'},R={'11110','10001','10001','11110','10100','10010','10001'},S={'01111','10000','10000','01110','00001','00001','11110'},T={'11111','00100','00100','00100','00100','00100','00100'},U={'10001','10001','10001','10001','10001','10001','01110'},V={'10001','10001','10001','10001','10001','01010','00100'},W={'10001','10001','10001','10101','10101','11011','10001'},Y={'10001','10001','01010','00100','00100','00100','00100'},['2']={'01110','10001','00001','00110','01000','10000','11111'},['/']={'00001','00010','00010','00100','01000','01000','10000'},['-']={'00000','00000','00000','11111','00000','00000','00000'}}
local function text(im,str,x,y,col)
 for j=1,#str do local ch=str:sub(j,j);local pat=glyph[ch];if pat then for yy,row in ipairs(pat) do for xx=1,#row do if row:sub(xx,xx)=='1' then px(im,x+xx-1,y+yy-1,col) end end end end;x=x+(pat and #pat[1]+1 or 4) end
end

local names={'01 fire cape','02 rear body fire','03 attached body fire','04 attached hand fire','05 embers'}
local function cape(im,t)
 for y=39,104 do
  local u=(y-39)/65
  local sway=4*math.sin(t*tau-u*4)*u
  local center=48+sway+3*u
  local width=8+18*u
  local low=math.floor(center-width);local high=math.floor(center+width)
  for x=low,high do
   local edge=math.min(x-low,high-x)
   local hem=96+5*math.sin(x*.35-t*tau)+3*math.sin(x*.7+t*tau)
   local ridge=math.sin((x-center)*.36+u*5-t*tau*2)+.4*math.sin(x*.7-u*8+t*tau)
   local hole=u>.64 and ridge>1.18 and edge>4
   if y<hem and not hole then
    local color
    if edge<2 or y>hem-2 then color=c(164,36,27,230)
    elseif edge<4 then color=c(226,64,27,242)
    elseif ridge>.78 then color=c(255,214,96,238)
    elseif ridge>.08 then color=c(255,140,36,232)
    elseif ridge>-.68 then color=c(237,81,24,232)
    else color=c(182,43,25,220) end
    px(im,x,y,color)
   end
  end
 end
 -- Shoulder fastening and glowing flow folds, the cape hangs from the upper back.
 poly(im,{{40,39},{47,42},{55,39},{58,45},{47,47},{37,45}},c(255,166,54,245))
 for j=1,4 do
  local bx=21+j*11+3*math.sin(t*tau+j)
  tongue(im,bx,96+(j%2)*3,14+j%3*4,2.8,j,2,t,false)
 end
end
local function makeLoop()
 local spr=Sprite(W,H,ColorMode.RGB);local layers={spr.layers[1]};for j=2,5 do layers[j]=spr:newLayer() end
 for j=1,5 do layers[j].name=names[j] end
 for f=1,N do
  if f>1 then spr:newEmptyFrame() end;spr.frames[f].duration=.08
  local t=(f-1)/N;local ims={};for j=1,5 do ims[j]=Image(W,H,ColorMode.RGB);ims[j]:clear() end
  cape(ims[1],t)
  -- Small burning patches are attached to the body instead of forming an outer aura.
  tongue(ims[2],35,58,13,3,2,-2,t,false)
  tongue(ims[2],62,57,16,3,5,2,t,false)
  local patches={{39,54,10,2.8,1,1},{57,51,9,2.5,4,-1},{41,68,17,3.2,3,2},{50,66,15,3.3,0,2},{57,76,13,2.5,5,-1},{39,94,16,3.1,2,1},{54,94,15,3,6,-1}}
  for _,v in ipairs(patches) do tongue(ims[3],v[1],v[2],v[3],v[4],v[5],v[6],t,false) end
  -- Both fists are the brightest anchors. Three tongues join one burning hand.
  for side=0,1 do
   local bx=side==0 and 31 or 65;local ph=side*2.4
   tongue(ims[4],bx,72,25,6,ph,side==0 and -2 or 2,t,false)
   tongue(ims[4],bx-5,70,17,3.2,ph+1,-3,t,false)
   tongue(ims[4],bx+5,71,20,3.2,ph+3,3,t,false)
   box(ims[4],bx-3,65,7,5,c(255,215,98,255))
   box(ims[4],bx-2,66,5,3,c(255,247,190,255))
   local life=(t+side*.53)%1
   tongue(ims[5],bx+math.sin(life*6+side)*4,54-life*20,5,1.5,side,1,t,false)
  end
  for j=1,15 do
   local life=(t+j*.61803398875)%1;local side=j%2==0 and -1 or 1
   local x=48+side*(16+j%4*3)+math.sin(life*6+j)*4
   local y=98-life*72;local a=math.floor(235*math.min(1,life*8,(1-life)*5))
   spark(ims[5],x,y,j%5==0 and 2 or 1,a,true)
  end
  for j=1,5 do spr:newCel(layers[j],f,ims[j],Point(0,0)) end
 end
 local tag=spr:newTag(1,N);tag.name='sustain_loop';tag.aniDir=AniDir.FORWARD
 spr:saveAs(OUT..'overdrive-v3.aseprite')
 return spr
end
local burn=makeLoop()
local function exportLayers(spr,prefix)
 for j=0,5 do
  local strip=Image(W,H*#spr.frames,ColorMode.RGB);strip:clear()
  local part=j==0 and nil or Sprite(W,H,ColorMode.RGB)
  for f=1,#spr.frames do
   if j==0 then strip:drawSprite(spr,f,Point(0,(f-1)*H))
   else
    local im=spr.layers[j]:cel(f).image;strip:drawImage(im,Point(0,(f-1)*H))
    if f>1 then part:newEmptyFrame() end;part.frames[f].duration=spr.frames[f].duration;part:newCel(part.layers[1],f,im,Point(0,0))
   end
  end
  local suffix=({'cape','rear-fire','body-fire','hand-fire','embers'})[j] or 'combined'
  strip:saveAs(OUT..prefix..'-'..suffix..'-strip.png')
  if part then part.layers[1].name=names[j];part:saveAs(OUT..prefix..'-'..suffix..'.aseprite') end
 end
end
exportLayers(burn,'overdrive-v3')
local entry=Sprite(W,H,ColorMode.RGB);local entryLayers={entry.layers[1]};for j=2,5 do entryLayers[j]=entry:newLayer() end
for j=1,5 do entryLayers[j].name=names[j] end
for f=1,8 do
 if f>1 then entry:newEmptyFrame() end;entry.frames[f].duration=.06
 local p=(f-1)/7;local power=1-(1-p)^2
 for j=1,5 do
  local im=Image(W,H,ColorMode.RGB);im:clear();local src=burn.layers[j]:cel(1).image
  for y=0,H-1 do for x=0,W-1 do
   local value=src:getPixel(x,y);local a=pc.rgbaA(value)
   local revealed=j~=1 or y<=39+power*68
   if a>0 and revealed then px(im,x,y,c(pc.rgbaR(value),pc.rgbaG(value),pc.rgbaB(value),math.floor(a*power))) end
  end end
  if j==5 and f<8 then for side=0,1 do for k=1,5 do local angle=k*tau/5;local bx=side==0 and 31 or 65;spark(im,bx+math.cos(angle)*(4+p*12),67+math.sin(angle)*(4+p*12)-p*8,1,math.floor(math.sin(p*math.pi)*230),true) end end end
  entry:newCel(entryLayers[j],f,im,Point(0,0))
 end
end
local tag=entry:newTag(1,8);tag.name='activation_once';entry:saveAs(OUT..'overdrive-v3-entry.aseprite')
local entryStrip=Image(W,H*8,ColorMode.RGB);entryStrip:clear();for f=1,8 do entryStrip:drawSprite(entry,f,Point(0,(f-1)*H)) end;entryStrip:saveAs(OUT..'overdrive-v3-entry-strip.png')
local preview=Sprite(248,154,ColorMode.RGB);preview.layers[1].name='preview only - front and rear'
for f=1,N*2+8 do
 if f>1 then preview:newEmptyFrame() end;preview.frames[f].duration=f<=8 and .06 or .08
 local source=f<=8 and entry or burn;local sf=f<=8 and f or (f-9)%N+1
 local im=Image(248,154,ColorMode.RGB);im:clear(c(16,23,33))
 box(im,8,26,112,120,c(23,33,46));box(im,128,26,112,120,c(23,33,46))
 text(im,'OVERDRIVE / FIRE CAPE',10,8,c(185,197,208))
 text(im,f<=8 and 'START' or 'LOOP',203,8,c(232,151,71))
 text(im,'FRONT / HANDS',16,31,c(255,192,100));text(im,'BACK / CAPE',138,31,c(255,164,84))
 for side=0,1 do
  local panel=Image(W,H,ColorMode.RGB);panel:clear()
  if side==0 then panel:drawImage(source.layers[1]:cel(sf).image) end
  panel:drawImage(source.layers[2]:cel(sf).image)
  panel:drawImage(mannequin)
  if side==1 then panel:drawImage(source.layers[1]:cel(sf).image) else panel:drawImage(source.layers[3]:cel(sf).image) end
  panel:drawImage(source.layers[4]:cel(sf).image);panel:drawImage(source.layers[5]:cel(sf).image)
  im:drawImage(panel,Point(16+side*120,39))
 end
 preview:newCel(preview.layers[1],f,im,Point(0,0))
end
preview:saveAs(OUT..'overdrive-v3-front-back-preview.aseprite')
local still=Image(248,154,ColorMode.RGB);still:drawSprite(preview,14);still:resize(992,616);still:saveAs(OUT..'overdrive-v3-front-back-preview.png')
-- Approved heat source is copied, not redrawn. This produces only its separate preview.
local HD='D:/alianximod/docs/资源库/特效/火焰/升温_贴身炽热_v2/'
local heat=app.open(HD..'heat_up-v2.aseprite');local heatEntry=app.open(HD..'heat_up-v2-entry.aseprite')
local heatPreview=Sprite(120,154,ColorMode.RGB);heatPreview.layers[1].name='preview only'
for f=1,N*2+8 do
 if f>1 then heatPreview:newEmptyFrame() end;heatPreview.frames[f].duration=f<=8 and .06 or .08
 local source=f<=8 and heatEntry or heat;local sf=f<=8 and f or (f-9)%N+1
 local im=Image(120,154,ColorMode.RGB);im:clear(c(16,23,33));box(im,4,26,112,120,c(23,33,46));text(im,'HEAT UP / V2',10,8,c(255,195,110));text(im,f<=8 and 'START' or 'LOOP',12,31,c(186,194,203))
 local panel=Image(W,H,ColorMode.RGB);panel:clear();for j=1,2 do panel:drawImage(source.layers[j]:cel(sf).image) end;panel:drawImage(mannequin);for j=3,4 do panel:drawImage(source.layers[j]:cel(sf).image) end;im:drawImage(panel,Point(12,39))
 heatPreview:newCel(heatPreview.layers[1],f,im,Point(0,0))
end
heatPreview:saveAs(HD..'heat_up-v2-preview.aseprite')
local stillHeat=Image(120,154,ColorMode.RGB);stillHeat:drawSprite(heatPreview,14);stillHeat:resize(480,616);stillHeat:saveAs(HD..'heat_up-v2-preview.png')
return {draft=OUT,approvedHeat=HD,loopFrames=N,entryFrames=8}
