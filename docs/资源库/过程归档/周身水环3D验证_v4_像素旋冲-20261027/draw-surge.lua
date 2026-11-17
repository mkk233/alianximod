local root='D:/alianximod/docs/资源库/过程归档/周身水环3D验证_v4_像素旋冲-20261027/'
local pc=app.pixelColor.rgba
local C={pc(5,29,52,255),pc(8,66,111,255),pc(11,112,178,255),pc(27,174,209,255),pc(112,232,235,255),pc(233,255,251,255)}
local steps={0,1,4,12,24,40,53,61,65,68,74,86,101,115,123,127}
local strength={.6,.7,.88,1,1,1,.96,.92,.9,.84,.74,.64,.53,.46,.46,.53}
local function poly(im,p,c)
 for y=0,im.height-1 do for x=0,im.width-1 do
  local hit=false;local j=#p
  for i=1,#p do local a,b=p[i],p[j];if ((a[2]>y)~=(b[2]>y)) and x<(b[1]-a[1])*(y-a[2])/(b[2]-a[2])+a[1] then hit=not hit end;j=i end
  if hit then im:drawPixel(x,y,c) end
 end end
end
local function paint(im,p,c,f)
 local q={};local amp=strength[f+1]
 for _,v in ipairs(p) do table.insert(q,{math.floor(v[1]+.5),math.floor(29+(v[2]-29)*amp+.5)}) end
 poly(im,q,c)
end
local s=Sprite(96,32,ColorMode.RGB);local body=s.layers[1];body.name='深蓝轮廓与旋冲水体'
local shade=s:newLayer();shade.name='硬边蓝青色块'
local foam=s:newLayer();foam.name='冲刷亮刃与尖尾'
local bits=s:newLayer();bits.name='切向短碎水片'
local atlas=Image(96,512,ColorMode.RGB)
for f=0,15 do
 if f>0 then s:newEmptyFrame() end;s.frames[f+1].duration=.1
 local b=Image(96,32,ColorMode.RGB);local d=Image(96,32,ColorMode.RGB);local h=Image(96,32,ColorMode.RGB);local z=Image(96,32,ColorMode.RGB)
 paint(b,{{0,30},{12,27},{23,23},{35,18},{46,12},{56,8},{66,6},{76,8},{85,12},{92,17},{96,22},{89,22},{81,19},{73,17},{66,17},{60,21},{53,25},{42,28},{29,30},{14,31}},C[1],f)
 paint(b,{{4,29},{20,25},{37,18},{49,12},{59,9},{69,8},{78,11},{85,15},{92,20},{83,17},{75,15},{67,15},{61,18},{54,23},{43,26},{29,28}},C[3],f)
 paint(d,{{17,27},{32,23},{48,16},{60,12},{71,11},{79,13},{84,16},{74,14},{67,15},{59,20},{51,24},{40,27},{27,28}},C[2],f)
 paint(d,{{26,23},{41,16},{55,10},{66,8},{75,10},{83,14},{76,13},{68,12},{60,14},{48,19},{38,23}},C[4],f)
 paint(h,{{4,29},{19,25},{34,19},{48,13},{58,10},{65,9},{59,12},{48,16},{33,22},{19,27}},C[5],f)
 paint(h,{{63,8},{69,8},{77,11},{84,15},{91,19},{85,17},{77,14},{70,12},{64,12}},C[5],f)
 if f>=3 and f<=10 then
  paint(h,{{67,8},{72,9},{78,12},{83,15},{78,13},{71,11},{66,11}},C[6],f)
  paint(h,{{42,16},{50,12},{57,10},{52,13},{45,16}},C[6],f)
 end
 local x=(f*7)%58+13
 paint(h,{{x,26},{x+12,22},{x+8,25},{x+2,27}},C[4],f)
 if f>=3 and f<=12 then
  local q=f-3
  for j=0,2 do
   local xx=64+j*9+math.floor(q*.7);local yy=3+j*4-math.floor(q*.15)
   if xx<91 then poly(z,{{xx,yy+2},{xx+5,yy},{xx+7,yy},{xx+2,yy+3}},j==0 and C[5] or C[4]) end
  end
 end
 s:newCel(body,f+1,b,Point(0,0));s:newCel(shade,f+1,d,Point(0,0));s:newCel(foam,f+1,h,Point(0,0));s:newCel(bits,f+1,z,Point(0,0))
 b:drawImage(d,Point(0,0));b:drawImage(h,Point(0,0));b:drawImage(z,Point(0,0));atlas:drawImage(b,Point(0,f*32))
end
s:saveAs(root..'surge_arc.aseprite');atlas:saveAs(root..'surge_arc.png')
local ring=Sprite(128,32,ColorMode.RGB);local rb=ring.layers[1];rb.name='断续环流色块';local rh=ring:newLayer();rh.name='前重后尖的流光水纹';local ra=Image(128,512,ColorMode.RGB)
for f=0,15 do
 if f>0 then ring:newEmptyFrame() end;ring.frames[f+1].duration=.1
 local b=Image(128,32,ColorMode.RGB);local h=Image(128,32,ColorMode.RGB)
 for x=0,127 do
  local u=(x-steps[f+1])%128
  local a=0;local len=0;if u<83 then a=u;len=83 elseif u>=94 and u<125 then a=u-94;len=31 end
  if len>0 then
   local p=a/len;local width=math.floor(3+15*math.sin(p*math.pi*.85));local top=math.max(0,12-math.floor(width*.62));local bot=math.min(30,top+width)
   for y=top,bot do
    local c=C[3];if y==top or y==bot then c=C[1] elseif y>top+width*.65 then c=C[2] elseif y<top+width*.3 then c=C[4] end
    b:drawPixel(x,y,c)
    if y==top+1 and p>.25 then h:drawPixel(x,y,p>.66 and C[5] or C[4]) end
    if p>.65 and p<.85 and y==top+2 then h:drawPixel(x,y,C[6]) end
    if y==top+math.floor(width*.45) and p>.2 and p<.6 then h:drawPixel(x,y,C[4]) end
   end
  end
 end
 ring:newCel(rb,f+1,b,Point(0,0));ring:newCel(rh,f+1,h,Point(0,0));b:drawImage(h,Point(0,0));ra:drawImage(b,Point(0,f*32))
end
ring:saveAs(root..'surge_flow.aseprite');ra:saveAs(root..'surge_flow.png')
return {arc={width=96,height=32,frames=16,layers=4},flow={width=128,height=32,frames=16,layers=2}}

