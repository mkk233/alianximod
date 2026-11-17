-- Aseprite MCP native Lua. Visual drafts only; no world-size or timing approval.
local out=app.params and app.params.out or nil
out='D:/alianximod/docs/资源库/过程归档/新十三式二维草稿-20261031/'
local names={'fire_spark','flame_trail','meteorite','shock_ring','surface_anchor','seed_sow','kindle','detonate','nether_gloom','ground_dust'}
local pc=app.pixelColor
local function col(r,g,b,a) return pc.rgba(r,g,b,a or 255) end
local red=col(211,40,23); local orange=col(255,111,24); local gold=col(255,202,51); local white=col(255,245,186)
local function dot(im,x,y,c) x=math.floor(x+.5); y=math.floor(y+.5); if x>=0 and y>=0 and x<im.width and y<im.height then im:drawPixel(x,y,c) end end
local function ellipse(im,cx,cy,rx,ry,c)
 for y=math.floor(cy-ry),math.ceil(cy+ry) do for x=math.floor(cx-rx),math.ceil(cx+rx) do
  if ((x-cx)/math.max(.1,rx))^2+((y-cy)/math.max(.1,ry))^2<=1 then dot(im,x,y,c) end
 end end
end
local function stroke(im,x0,y0,x1,y1,c,w)
 local n=math.max(1,math.ceil(math.max(math.abs(x1-x0),math.abs(y1-y0))))
 for i=0,n do local t=i/n; ellipse(im,x0+(x1-x0)*t,y0+(y1-y0)*t,w or .6,w or .6,c) end
end
local function ring(im,cx,cy,rx,ry,w,c)
 for y=math.floor(cy-ry-w),math.ceil(cy+ry+w) do for x=math.floor(cx-rx-w),math.ceil(cx+rx+w) do
  local d=math.sqrt(((x-cx)/math.max(1,rx))^2+((y-cy)/math.max(1,ry))^2)
  if math.abs(d-1)<w/math.max(1,rx) then dot(im,x,y,c) end
 end end
end
local function polygon(im,p,c)
 for y=0,63 do for x=0,63 do
  local hit=false; local j=#p
  for i=1,#p do local a=p[i]; local b=p[j]
   if ((a[2]>y)~=(b[2]>y)) and x<(b[1]-a[1])*(y-a[2])/(b[2]-a[2])+a[1] then hit=not hit end
   j=i
  end
  if hit then dot(im,x,y,c) end
 end end
end
local function flame(im,cx,base,h,w,phase,blue)
 for y=math.floor(base-h),base do
  local q=(base-y)/h; local sway=math.sin(q*7-phase)*q*w*.5
  local half=w*(1-q)^.72*(.8+.2*math.sin(q*12+phase))
  for x=math.floor(cx+sway-half),math.ceil(cx+sway+half) do
   local d=math.abs(x-cx-sway)/math.max(.1,half)
   local c=red
   if d<.75 then c=orange end
   if q<.66 and d<.48 then c=gold end
   if q<.35 and d<.25 then c=white end
   if blue then
    c=col(27,92,151)
    if d<.76 then c=col(20,167,209) end
    if q<.69 and d<.50 then c=col(92,229,232) end
    if q<.34 and d<.27 then c=col(218,255,239) end
   end
   dot(im,x,y,c)
  end
 end
end
local function sparks(im,cx,cy,t,n,blue)
 for i=1,n do
  local k=(i*.618+t*.55)%1; local a=i*2.399; local d=7+k*21
  local x=cx+math.cos(a)*d; local y=cy+math.sin(a)*d-k*8
  local c=blue and col(139,247,255,math.floor((1-k)*230)) or col(255,211,105,math.floor((1-k)*230))
  stroke(im,x,y,x-1,y+2,c,.55)
 end
end
local function frameImages(id,f)
 local body=Image(64,64,ColorMode.RGB); local glow=Image(64,64,ColorMode.RGB); local part=Image(64,64,ColorMode.RGB)
 local t=(f-1)/16; local p=t*math.pi*2
 if id==1 then
  ellipse(glow,43,33,12,9,col(255,102,14,60))
  for x=7,48 do
   local u=(x-7)/41; local cy=33+math.sin(u*8-p)*3*(1-u); local w=6.5*u^.8
   for y=math.floor(cy-w),math.ceil(cy+w) do
    local d=math.abs(y-cy)/math.max(.1,w); local c=red
    if d<.8 then c=orange end; if d<.46 and u>.48 then c=gold end; if d<.2 and u>.73 then c=white end
    dot(body,x,y,c)
   end
  end
  flame(body,42,35,18,5,p,false); sparks(part,35,31,t,5,false)
 elseif id==2 then
  for x=5,58 do
   local u=(x-5)/53; local y=44-18*u+6*math.sin(u*6)
   ellipse(glow,x,y,3,5,col(240,64,15,math.floor(15+45*u)))
   stroke(body,x,y,x,y-2-math.max(0,math.sin(u*25-p))*9,col(255,93,22,math.floor(50+205*u)),1.2)
   dot(body,x,y,col(255,210,67,math.floor(45+205*u)))
  end
  sparks(part,30,35,t,5,false)
 elseif id==3 then
  local top={{32,11},{53,22},{32,33},{11,22}}; local left={{11,22},{32,33},{32,56},{11,45}}; local right={{32,33},{53,22},{53,45},{32,56}}
  polygon(body,top,col(93,78,70)); polygon(body,left,col(56,43,40)); polygon(body,right,col(73,52,42))
  for y=12,54 do for x=12,52 do
   if pc.rgbaA(body:getPixel(x,y))>0 and (x*17+y*29)%13<3 then
    local v=18+((x*7+y*11)%17); local old=body:getPixel(x,y)
    dot(body,x,y,col(math.max(0,pc.rgbaR(old)-v),math.max(0,pc.rgbaG(old)-v),math.max(0,pc.rgbaB(old)-v)))
   end
  end end
  local paths={{{23,17},{29,22},{26,29},{32,33},{29,41},{33,48},{32,55}},{{29,22},{39,21},{43,27}},{{18,27},{24,31},{22,39},{29,41}},{{37,31},{40,37},{37,43},{45,48}},{{40,37},{49,34},{51,30}}}
  for _,pts in ipairs(paths) do for j=1,#pts-1 do local a=pts[j];local b=pts[j+1]
   stroke(glow,a[1],a[2],b[1],b[2],col(255,100,11,90),2.1);stroke(body,a[1],a[2],b[1],b[2],orange,1)
   if (j+f)%5~=0 then stroke(body,a[1],a[2],b[1],b[2],gold,.5) end
  end end
  sparks(part,32,30,t,6,false)
 elseif id==4 then
  local r=5+25*t; local a=math.floor(255*(1-t)^.6)
  ring(glow,32,36,r,r*.52,4,col(255,82,17,math.floor(a*.25)))
  ring(body,32,36,r,r*.52,1.8,col(255,112,28,a))
  ring(body,32,36,r-1,(r-1)*.52,.8,col(255,226,113,a))
  for j=1,12 do local an=j*math.pi/6+.1; local x=32+math.cos(an)*r;local y=36+math.sin(an)*r*.52
   stroke(part,x,y,x+math.cos(an)*2,y+math.sin(an)*2,col(255,214,95,a),.5)
  end
 elseif id==5 then
  local c=col(220,232,239); local a=math.floor(150+65*math.sin(p))
  ring(glow,32,39,19,9,3,col(94,199,219,35)); ring(body,32,39,18,8,.8,col(140,220,234,a))
  local pts={{18,39},{46,39},{32,32},{32,46}}
  for i,v in ipairs(pts) do if i<3 then stroke(body,v[1]-3,v[2],v[1]+3,v[2],c,.6) else stroke(body,v[1],v[2]-2,v[1],v[2]+2,c,.6) end end
  stroke(body,32,20,32,29,col(255,226,149),.7);stroke(body,29,26,32,30,col(255,226,149),.7);stroke(body,35,26,32,30,col(255,226,149),.7)
  dot(part,32,39,white)
 elseif id==6 then
  local d=3+18*t; local a=math.floor(255*(1-t)^.9)
  ellipse(glow,32,32,8*(1-t)+2,8*(1-t)+2,col(255,153,28,math.floor(110*(1-t))))
  for j=1,9 do local an=j*2.399; local x=32+math.cos(an)*d;local y=32+math.sin(an)*d
   stroke(part,x,y,x+math.cos(an)*3*(1-t),y+math.sin(an)*3*(1-t),col(255,208,72,a),.6)
  end
  ellipse(body,32,32,math.max(.5,4*(1-t)),math.max(.5,4*(1-t)),col(255,248,197,a))
 elseif id==7 then
  ellipse(glow,32,44,15,11,col(243,84,14,55))
  flame(body,22,48,20+3*math.sin(p),5,p+1,false); flame(body,41,48,22+3*math.sin(p+1),5,p+2,false)
  flame(body,32,51,34+2*math.sin(p+2),8,p,false); sparks(part,32,31,t,8,false)
 elseif id==8 then
  local u=math.min(1,t*2); local r=3+20*u; local a=math.floor(255*math.max(0,1-t)^.65)
  ellipse(glow,32,33,r+5,r+3,col(255,84,7,math.floor(a*.28)))
  for j=0,8 do local an=j*math.pi*2/9; local rr=r*(.65+.12*math.sin(j*3+p))
   ellipse(body,32+math.cos(an)*r*.58,33+math.sin(an)*r*.48,rr*.62,rr*.65,col(223,49,18,a))
  end
  ellipse(body,32,34,r*.8,r*.7,col(255,117,21,a));ellipse(body,32,35,r*.48,r*.44,col(255,214,76,a))
  if t<.5 then ellipse(body,32,34,r*.25,r*.24,col(255,248,189,a)) end
  sparks(part,32,33,t,11,false)
 elseif id==9 then
  ellipse(glow,32,44,22,10,col(16,164,218,38))
  flame(body,17,47,15+2*math.sin(p),4,p+1,true); flame(body,46,46,19+2*math.sin(p+2),5,p+3,true)
  flame(body,31,51,32+3*math.sin(p+1),7,p,true);sparks(part,32,30,t,8,true)
 elseif id==10 then
  local a=math.floor(220*(1-t)); local r=5+22*t
  for j=1,10 do local an=j*2.399;local x=32+math.cos(an)*r;local y=43+math.sin(an)*r*.4-10*math.sin(t*math.pi)
   ellipse(glow,x,y,3+4*t,2+3*t,col(119,87,64,math.floor(a*.28)))
   local c=(j%2==0) and col(139,104,74,a) or col(92,72,58,a)
   ellipse(body,x,y,1+(j%3)*.5,1+(j%2)*.5,c)
   dot(part,x-1,y-1,col(184,148,104,a))
  end
 end
 return {glow,body,part}
end

local sprites={}
for id,name in ipairs(names) do
 local s=Sprite(64,64,ColorMode.RGB); s.layers[1].name='glow'; local layers={s.layers[1],s:newLayer(),s:newLayer()};layers[2].name='body';layers[3].name='sparks'
 for f=1,16 do if f>1 then s:newEmptyFrame(f) end;s.frames[f].duration=.08
  local ims=frameImages(id,f)
  for k=1,3 do s:newCel(layers[k],f,ims[k],Point(0,0)) end
 end
 s.data='Draft v1, unapproved. Preview animation speed is illustrative.'
 s:saveAs(out..name..'.aseprite')
 local sheet=Image(64,64*16,ColorMode.RGB)
 for f=1,16 do sheet:drawSprite(s,f,Point(0,(f-1)*64)) end
 sheet:saveAs(out..name..'-strip.png')
 sprites[id]=s
end

local glyph={['0']={'111','101','101','101','111'},['1']={'010','110','010','010','111'},['2']={'111','001','111','100','111'},['3']={'111','001','111','001','111'},['4']={'101','101','111','001','001'},['5']={'111','100','111','001','111'},['6']={'111','100','111','101','111'},['7']={'111','001','010','010','010'},['8']={'111','101','111','101','111'},['9']={'111','101','111','001','111'}}
local function label(im,x,y,n)
 local txt=string.format('%02d',n)
 for j=1,#txt do for row,line in ipairs(glyph[txt:sub(j,j)]) do for q=1,3 do if line:sub(q,q)=='1' then dot(im,x+(j-1)*4+q,y+row,col(162,175,188)) end end end end
end
local board=Sprite(400,160,ColorMode.RGB);board.layers[1].name='draft review';
for f=1,16 do
 if f>1 then board:newEmptyFrame(f) end;board.frames[f].duration=.08
 local im=Image(400,160,ColorMode.RGB);im:clear(col(16,21,30))
 for id,s in ipairs(sprites) do
  local x=((id-1)%5)*80;local y=math.floor((id-1)/5)*80
  for yy=y+2,y+77 do for xx=x+2,x+77 do dot(im,xx,yy,col(24,30,41)) end end
  im:drawSprite(s,f,Point(x+8,y+8));label(im,x+4,y+3,id)
 end
 board:newCel(board.layers[1],f,im,Point(0,0))
end
board:saveAs(out..'review-board.aseprite')
local still=Image(400,160,ColorMode.RGB);still:drawSprite(board,6);still:resize(1200,480);still:saveAs(out..'review-board.png')
local large=Sprite(board);large:resize(1200,480);large:saveAs(out..'review-board.gif');large:close()
board:close();for _,s in ipairs(sprites) do s:close() end
return {count=#names,frames=16,source=out..'review-board.aseprite',preview=out..'review-board.png',animation=out..'review-board.gif'}
