local root='D:/alianximod/docs/资源库/过程归档/周身水环3D验证_v5_大浪溅射-20261027/'
local pc=app.pixelColor.rgba
local C={pc(10,35,73,255),pc(17,69,115,255),pc(27,116,161,255),pc(88,186,208,255),pc(184,224,224,255),pc(242,249,237,255)}
local amp={.22,.35,.52,.72,.90,1,1.04,1.06,1.02,.96,.85,.69,.52,.35,.23,.2}
local spread={.64,.7,.79,.87,.94,1,1.04,1.07,1.08,1.08,1.05,1,.9,.8,.7,.65}
local function poly(im,p,c)
 local minx,maxx,miny,maxy=im.width-1,0,im.height-1,0
 for _,v in ipairs(p) do minx=math.min(minx,math.floor(v[1]));maxx=math.max(maxx,math.ceil(v[1]));miny=math.min(miny,math.floor(v[2]));maxy=math.max(maxy,math.ceil(v[2])) end
 for y=math.max(0,miny),math.min(im.height-1,maxy) do for x=math.max(0,minx),math.min(im.width-1,maxx) do
  local hit=false;local j=#p
  for i=1,#p do local a,b=p[i],p[j];if ((a[2]>y)~=(b[2]>y)) and x<(b[1]-a[1])*(y-a[2])/(b[2]-a[2])+a[1] then hit=not hit end;j=i end
  if hit then im:drawPixel(x,y,c) end
 end end
end
local function paint(im,p,c,f)
 local q={}
 for _,v in ipairs(p) do
  local influence=math.max(0,(60-v[2])/54)
  local dx=(f>=7 and f<=11) and (f-7)*.5*influence or 0
  table.insert(q,{math.floor(45+(v[1]-45)*spread[f+1]+dx+.5),math.floor(61+(v[2]-61)*amp[f+1]+.5)})
 end
 poly(im,q,c)
end
local s=Sprite(96,64,ColorMode.RGB)
local layers={s.layers[1],s:newLayer(),s:newLayer(),s:newLayer(),s:newLayer()}
local names={'深靛厚浪底','青蓝翻卷水面','大面积白色浪冠','粗短爪状分叉','外抛大水块'}
for i,l in ipairs(layers) do l.name=names[i] end
local atlas=Image(96,1024,ColorMode.RGB)
for f=0,15 do
 if f>0 then s:newEmptyFrame() end;s.frames[f+1].duration=.1
 local b=Image(96,64,ColorMode.RGB);local d=Image(96,64,ColorMode.RGB);local h=Image(96,64,ColorMode.RGB);local p=Image(96,64,ColorMode.RGB);local drops=Image(96,64,ColorMode.RGB)
 paint(b,{{5,62},{12,56},{20,48},{24,40},{28,30},{33,23},{41,17},{50,13},{59,14},{68,17},{76,23},{84,32},{86,40},{80,46},{73,42},{68,39},{64,43},{64,49},{70,54},{81,58},{91,62},{76,63},{63,61},{49,63},{33,62},{17,63}},C[1],f)
 paint(b,{{10,60},{22,47},{29,33},{37,23},{47,18},{57,17},{67,20},{76,27},{80,35},{77,42},{69,37},{63,39},{59,46},{63,53},{75,59},{57,60},{44,62},{29,60}},C[2],f)
 paint(d,{{14,59},{25,44},{31,31},{39,23},{46,20},{42,27},{37,36},{33,46},{30,54},{24,59}},C[3],f)
 paint(d,{{30,60},{36,46},{41,32},{48,23},{54,20},{56,24},{50,31},{46,40},{45,50},{48,59}},C[3],f)
 paint(d,{{49,60},{48,49},{51,38},{57,28},{64,24},{69,26},{63,31},{58,40},{56,49},{60,57},{70,60}},C[3],f)
 paint(d,{{23,51},{29,35},{38,25},{42,24},{36,31},{31,41},{29,48}},C[4],f)
 paint(d,{{36,56},{40,39},{47,29},{52,26},{46,36},{44,47},{42,56}},C[4],f)
 paint(d,{{55,54},{53,44},{58,33},{64,30},{62,35},{58,44},{60,52}},C[4],f)
 paint(h,{{26,31},{27,23},{34,16},{43,12},{52,11},{61,13},{70,18},{79,25},{85,33},{87,40},{82,47},{76,43},{74,37},{69,35},{65,31},{61,29},{56,30},{52,25},{47,25},{44,29},{40,28},{37,32},{32,30}},C[5],f)
 paint(h,{{27,28},{30,21},{37,16},{45,13},{54,13},{63,16},{71,21},{77,27},{82,33},{82,38},{77,37},{73,32},{68,30},{63,26},{57,27},{53,23},{48,22},{44,26},{40,25},{36,28},{32,27}},C[6],f)
 local claws={
 {{27,27},{24,22},{24,17},{28,13},{33,13},{38,17},{39,22},{35,24},{34,19},{31,17},{28,19},{30,24}},
 {{38,22},{35,16},{37,11},{42,8},{47,9},{51,13},{50,17},{46,18},{46,13},{42,12},{40,16},{43,21}},
 {{51,23},{49,16},{51,11},{56,8},{61,10},{65,14},{64,19},{60,21},{60,15},{57,13},{54,16},{56,22}},
 {{63,28},{64,22},{66,17},{72,16},{77,19},{80,24},{78,29},{74,30},{74,24},{71,22},{69,25},{68,30}},
 {{73,36},{75,30},{80,26},{85,28},{89,33},{88,38},{84,40},{83,36},{84,33},{81,31},{79,35},{78,40}},
 {{79,44},{83,39},{88,38},{92,42},{93,47},{90,51},{86,50},{87,46},{87,43},{84,43},{83,48}}
 }
 for _,q in ipairs(claws) do paint(p,q,C[6],f) end
 local gaps={{{29,21},{31,19},{34,21},{33,25},{31,25}},{{41,16},{43,14},{45,16},{44,21},{42,20}},{{56,18},{58,16},{60,18},{59,23},{57,23}},{{69,26},{71,24},{73,27},{72,31},{70,30}},{{79,35},{81,33},{83,36},{81,41},{79,39}}}
 for _,q in ipairs(gaps) do paint(p,q,C[3],f) end
 local cuts={{{34,7},{33,13},{36,18},{38,19},{39,17},{37,13},{39,8}},{{48,4},{47,10},{49,16},{51,18},{53,16},{51,12},{53,7}},{{63,7},{62,13},{65,19},{67,21},{69,19},{67,15},{68,11}},{{78,16},{77,22},{79,28},{82,30},{83,27},{81,23},{82,19}}}
 local mask=Image(96,64,ColorMode.RGB)
 for _,q in ipairs(cuts) do paint(mask,q,C[6],f) end
 for yy=0,63 do for xx=0,95 do if mask:getPixel(xx,yy)~=0 then b:drawPixel(xx,yy,0);d:drawPixel(xx,yy,0);h:drawPixel(xx,yy,0);p:drawPixel(xx,yy,0) end end end
 if f>=5 and f<=14 then
  local t=(f-5)/9
  local seeds={{19,16,-11,-13,8},{34,5,-6,-9,9},{62,6,12,-12,10},{79,20,12,-9,9},{85,35,8,-4,8}}
  for j,seed in ipairs(seeds) do
   local x=seed[1]+seed[3]*t
   local y=seed[2]+seed[4]*t+58*t*t
   local size=math.max(.6,seed[5]*(1-.88*t))
   poly(drops,{{x-size*.6,y},{x,y-size*.5},{x+size*.6,y},{x+size*.4,y+size},{x-size*.2,y+size*1.2}},C[5])
   poly(drops,{{x-size*.4,y},{x,y-size*.4},{x+size*.35,y},{x+size*.1,y+size*.7}},C[6])
  end
 end
 local ims={b,d,h,p,drops}
 for i,l in ipairs(layers) do s:newCel(l,f+1,ims[i],Point(0,0)) end
 local flat=Image(96,64,ColorMode.RGB);for _,im in ipairs(ims) do flat:drawImage(im,Point(0,0)) end
 atlas:drawImage(flat,Point(0,f*64))
end
s:saveAs(root..'great_splash.aseprite');atlas:saveAs(root..'great_splash.png')
local ring=Sprite(128,32,ColorMode.RGB);local rb=ring.layers[1];rb.name='持续环绕水体';local rf=ring:newLayer();rf.name='大块断续白沫'
local ra=Image(128,512,ColorMode.RGB)
for f=0,15 do
 if f>0 then ring:newEmptyFrame() end;ring.frames[f+1].duration=.1
 local b=Image(128,32,ColorMode.RGB);local h=Image(128,32,ColorMode.RGB)
 for x=0,127 do
  local u=(x-f*8)%128;local top=3+math.floor(2*math.sin(u*.22));local bot=24+math.floor(2*math.sin(u*.18+1))
  for y=top,bot do
   local c=C[3];if y>=16 then c=C[2] end;if y==top or y==bot then c=C[1] end;if y<9 then c=C[4] end
   b:drawPixel(x,y,c)
   if u%43<28 and y>=top+1 and y<=top+3 then h:drawPixel(x,y,C[5]) end
   if u%43<15 and y==top+1 then h:drawPixel(x,y,C[6]) end
   if u%51>17 and u%51<43 and y==12 then h:drawPixel(x,y,C[4]) end
  end
 end
 ring:newCel(rb,f+1,b,Point(0,0));ring:newCel(rf,f+1,h,Point(0,0));b:drawImage(h,Point(0,0));ra:drawImage(b,Point(0,f*32))
end
ring:saveAs(root..'splash_ring_flow.aseprite');ra:saveAs(root..'splash_ring_flow.png')
return {splash={frames=16,width=96,height=64,layers=5},flow={frames=16,width=128,height=32,layers=2}}

