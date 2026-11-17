local out='D:/alianximod/docs/资源库/过程归档/陨岩_熔岩岩壳_v5-20261102/'
local src=app.open('D:/alianximod/docs/资源库/过程归档/陨岩_致密熔壳_v4-20261102/meteorite-solid-crust-v4.aseprite')
src.layers[4].isVisible=false
local crust=Image(32,32,ColorMode.RGB);crust:drawSprite(src,1)
local pc=app.pixelColor
local function C(h) return pc.rgba(tonumber(h:sub(1,2),16),tonumber(h:sub(3,4),16),tonumber(h:sub(5,6),16),255) end
local p={lava=C('DA4B1C'),red=C('AF3519'),orange=C('F47927'),bright=C('FFAB3D'),hot=C('FFD071'),warm=C('7E3221'),rim=C('54271F'),face=C('443332'),edge=C('75412B')}
local function dot(im,x,y,c) if x>=0 and x<32 and y>=0 and y<32 then im:drawPixel(x,y,c) end end
local function inside(x,y,pts)
 local v=false;local j=#pts
 for i=1,#pts do local a,b=pts[i],pts[j]
  if ((a[2]>y+.5)~=(b[2]>y+.5)) and x+.5<(b[1]-a[1])*(y+.5-a[2])/(b[2]-a[2])+a[1] then v=not v end;j=i
 end
 return v
end
local function poly(im,pts,c) for y=0,31 do for x=0,31 do if inside(x,y,pts) then dot(im,x,y,c) end end end end
local function line(im,pts,c)
 for k=1,#pts-1 do local a,b=pts[k],pts[k+1];local n=math.max(math.abs(b[1]-a[1]),math.abs(b[2]-a[2]),1)
  for i=0,n do dot(im,math.floor(a[1]+(b[1]-a[1])*i/n+.5),math.floor(a[2]+(b[2]-a[2])*i/n+.5),c) end
 end
end
local s=Sprite(32,32,ColorMode.RGB)
local names={'成片熔岩','橙黄高温流面','炽热岩壳边缘','平整暗色岩壳','岩面烧红反光'}
local layers={s.layers[1]};for i=2,#names do layers[i]=s:newLayer() end
local im={};for i,n in ipairs(names) do layers[i].name=n;im[i]=Image(32,32,ColorMode.RGB) end
for y=0,31 do for x=0,31 do dot(im[1],x,y,p.lava) end end
local flows={
 {p.orange,{{9,-2},{16,-2},{16,4},{18,7},{17,11},{13,12},{10,7}}},
 {p.orange,{{-2,7},{4,6},{8,8},{13,8},{15,12},{10,14},{4,12},{-2,14}}},
 {p.orange,{{15,9},{21,8},{27,9},{34,6},{34,11},{28,14},{23,14},{18,12}}},
 {p.orange,{{12,12},{17,11},{20,16},{18,22},{16,26},{11,25},{9,21},{13,18}}},
 {p.orange,{{-2,20},{5,19},{9,21},{13,22},{14,26},{7,27},{2,25},{-2,26}}},
 {p.orange,{{18,20},{23,20},{29,22},{34,19},{34,26},{28,28},{23,26},{17,27}}},
 {p.orange,{{16,25},{22,26},{22,34},{15,34},{13,29}}},
 {p.bright,{{11,-2},{14,-2},{13,3},{15,7},{14,10},{12,8},{11,5}}},
 {p.bright,{{-2,9},{3,8},{7,10},{11,10},{13,12},{9,12},{5,11},{1,11},{-2,12}}},
 {p.bright,{{17,10},{22,9},{27,11},{31,9},{34,9},{34,11},{28,13},{23,12},{19,12}}},
 {p.bright,{{14,12},{16,12},{17,16},{16,19},{14,21},{14,23},{11,22},{13,18},{14,16}}},
 {p.bright,{{-2,22},{4,21},{8,23},{12,23},{13,25},{8,25},{4,23},{0,24}}},
 {p.bright,{{18,23},{22,22},{28,24},{32,22},{34,22},{34,25},{29,26},{24,25},{20,25}}},
 {p.bright,{{16,26},{19,27},{20,31},{19,34},{17,34},{18,30}}}
}
for _,v in ipairs(flows) do poly(im[2],v[2],v[1]) end
for _,pts in ipairs({{{12,1},{12,3}},{{13,6},{14,7}},{{1,10},{3,10}},{{8,11},{9,11}},{{19,10},{21,10}},{{30,11},{31,10}},{{15,15},{15,17}},{{12,21},{12,22}},{{3,22},{5,23}},{{19,24},{21,24}},{{29,25},{31,24}},{{18,29},{18,31}}}) do line(im[2],pts,p.hot) end
-- Large flat crust islands. Molten material is exposed between them, without holes.
local plates={
 {{-3,-3},{11,-3},{12,1},{8,6},{5,9},{-3,8}},
 {{17,-3},{34,-3},{34,8},{28,8},{24,6},{20,7},{17,4}},
 {{11,9},{16,7},{20,9},{22,13},{19,17},{14,18},{10,15},{8,13}},
 {{-3,14},{2,12},{5,14},{7,19},{5,23},{-3,25}},
 {{27,12},{34,13},{34,26},{29,26},{26,23},{23,22},{23,18},{25,17}},
 {{11,22},{17,21},{20,24},{19,28},{22,34},{4,34},{5,30},{8,27},{8,24}},
 {{27,30},{34,29},{34,34},{25,34}}
}
local rockPixels=0
for _,pts in ipairs(plates) do
 local cx,cy=0,0;for _,v in ipairs(pts) do cx=cx+v[1];cy=cy+v[2] end;cx=cx/#pts;cy=cy/#pts
 for _,v in ipairs(pts) do v[1]=cx+(v[1]-cx)*1.12;v[2]=cy+(v[2]-cy)*1.12 end
 for y=0,31 do for x=0,31 do if inside(x,y,pts) then
  rockPixels=rockPixels+1;dot(im[3],x,y,p.warm)
  local near=not inside(x-1,y,pts) or not inside(x+1,y,pts) or not inside(x,y-1,pts) or not inside(x,y+1,pts)
  if not near then
   local v=crust:getPixel(x,y)
   local r=math.floor(pc.rgbaR(v)*.86+5);local g=math.floor(pc.rgbaG(v)*.77);local b=math.floor(pc.rgbaB(v)*.70)
   dot(im[4],x,y,pc.rgba(r,g,b,255))
  else
   if not inside(x,y+1,pts) or not inside(x+1,y,pts) then dot(im[3],x,y,p.rim) end
  end
 end end end
end
-- Warm edge reflections remain shallow; they never form circular crater rims.
local highlights={
 {{1,2},{5,2},{8,1}},{{1,5},{3,5},{5,4}},
 {{18,1},{22,2},{25,2}},{{25,4},{28,5},{30,4}},
 {{1,14},{4,13},{7,13}},{{4,17},{7,18},{9,16}},
 {{21,16},{23,15},{25,16}},{{28,18},{30,17},{31,17}},
 {{1,28},{4,27},{6,29}},{{9,29},{11,30},{12,31}},
 {{24,30},{27,30},{29,29}}
}
for _,pts in ipairs(highlights) do line(im[5],pts,p.face) end
line(im[5],{{2,7},{4,6},{6,5}},p.edge)
line(im[5],{{22,8},{24,7},{26,7}},p.edge)
line(im[5],{{3,20},{6,20},{8,19}},p.edge)
line(im[5],{{26,21},{28,22},{30,21}},p.edge)
line(im[5],{{10,26},{12,27},{13,28}},p.edge)
for y=0,31 do for x=0,31 do if pc.rgbaA(im[4]:getPixel(x,y))==0 then im[5]:drawPixel(x,y,0) end end end
for i=1,#layers do s:newCel(layers[i],1,im[i],Point(0,0)) end
s.frames[1].duration=.1
s.data='meteor-magma-crust-v5; 32x32 opaque static; broad orange-red magma between flat crust islands; no pits; pending user approval'
s:saveAs(out..'meteorite-magma-v5.aseprite')
local flat=Image(32,32,ColorMode.RGB);flat:drawSprite(s,1);flat:saveAs(out..'meteorite-magma-v5.png')
local bright=0;for y=0,31 do for x=0,31 do local v=flat:getPixel(x,y);assert(pc.rgbaA(v)==255);if pc.rgbaR(v)>170 and pc.rgbaR(v)>pc.rgbaB(v)*2 then bright=bright+1 end end end
app.activeSprite=s;app.command.SpriteSize{ui=false,width=512,height=512,method='nearest'}
local pr=Image(512,512,ColorMode.RGB);pr:drawSprite(s,1);pr:saveAs(out..'texture-preview.png')
local report={version='meteor-magma-crust-v5',width=32,height=32,frames=1,layers=#layers,opaque=true,hotPixels=bright,hotFraction=bright/1024,crustMaskPixels=rockPixels}
local f=io.open(out..'aseprite-report.json','w');f:write(json.encode(report));f:close()
return report
