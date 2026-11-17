local out='D:/alianximod/docs/资源库/过程归档/陨岩_致密熔壳_v4-20261102/'
local pc=app.pixelColor
local function C(h) return pc.rgba(tonumber(h:sub(1,2),16),tonumber(h:sub(3,4),16),tonumber(h:sub(5,6),16),255) end
local p={deep=C('1D2027'),shadow=C('252831'),dark=C('2D3038'),base=C('35373D'),rock=C('3E3F43'),face=C('49484A'),rim=C('565254'),ash=C('635C59'),scorch=C('3D2A2B'),warm=C('57322B'),red=C('783D2C'),ember=C('AA5732'),heat=C('D08143')}
local function dot(im,x,y,c) if x>=0 and x<32 and y>=0 and y<32 then im:drawPixel(x,y,c) end end
local function poly(im,pts,c)
 for y=0,31 do for x=0,31 do local inside=false;local j=#pts
  for i=1,#pts do local a,b=pts[i],pts[j]
   if ((a[2]>y+.5)~=(b[2]>y+.5)) and x+.5<(b[1]-a[1])*(y+.5-a[2])/(b[2]-a[2])+a[1] then inside=not inside end;j=i
  end
  if inside then dot(im,x,y,c) end
 end end
end
local function line(im,pts,c)
 for k=1,#pts-1 do local a,b=pts[k],pts[k+1];local n=math.max(math.abs(b[1]-a[1]),math.abs(b[2]-a[2]),1)
  for i=0,n do dot(im,math.floor(a[1]+(b[1]-a[1])*i/n+.5),math.floor(a[2]+(b[2]-a[2])*i/n+.5),c) end
 end
end
local s=Sprite(32,32,ColorMode.RGB)
local names={'致密黑色熔壳','大块斜切岩面','烧蚀擦痕','边缘破损余热'}
local layers={s.layers[1]};for i=2,#names do layers[i]=s:newLayer() end
local im={};for i,n in ipairs(names) do layers[i].name=n;im[i]=Image(32,32,ColorMode.RGB) end
for y=0,31 do for x=0,31 do dot(im[1],x,y,p.base) end end
local planes={
 {p.rock,{{-2,-2},{15,-2},{13,5},{5,11},{-2,8}}},
 {p.dark,{{15,-2},{34,-2},{34,10},{24,12},{13,5}}},
 {p.face,{{-2,9},{7,8},{13,6},{25,13},{19,19},{7,22},{-2,19}}},
 {p.dark,{{-2,20},{7,22},{14,29},{12,34},{-2,34}}},
 {p.base,{{25,13},{34,11},{34,28},{26,31},{19,26},{19,19}}},
 {p.rock,{{7,22},{19,19},{19,26},{26,31},{23,34},{12,34},{14,29}}},
 {p.shadow,{{1,-2},{5,-2},{3,3},{0,5},{-2,5}}},
 {p.shadow,{{21,1},{24,-2},{34,-2},{34,3},{28,5},{24,4}}},
 {p.base,{{0,13},{5,12},{10,13},{14,16},{8,19},{1,17}}},
 {p.rock,{{18,5},{22,6},{26,8},{24,10},{21,9},{19,7}}},
 {p.shadow,{{24,13},{27,11},{34,9},{34,12},{28,14},{24,16}}},
 {p.dark,{{20,21},{23,18},{29,17},{28,20},{24,22},{22,24}}},
 {p.base,{{12,23},{16,21},{18,21},{18,26},{21,29},{19,30},{16,27}}},
 {p.shadow,{{-2,28},{3,26},{7,27},{10,30},{8,32},{2,30},{-2,32}}}
}
for _,v in ipairs(planes) do poly(im[2],v[2],v[1]) end
-- Broken low-contrast rims describe solid facets, not cavities or mineral veins.
poly(im[3],{{0,9},{6,8},{12,6},{15,7},{12,8},{8,10},{3,11},{0,11}},p.rim)
poly(im[3],{{16,9},{20,11},{23,13},{20,14},{19,12},{16,11}},p.rim)
poly(im[3],{{9,22},{13,21},{17,20},{19,20},{17,22},{13,23},{11,24}},p.face)
poly(im[3],{{6,0},{10,-1},{13,0},{12,2},{8,4},{5,5},{4,4}},p.face)
poly(im[3],{{28,26},{32,24},{34,25},{34,28},{30,29},{27,30}},p.rock)
-- Shallow directional scuffs stay within the plane shading.
local marks={
 {p.base,{{8,2},{11,1}}},{p.rim,{{5,5},{8,4}}},
 {p.base,{{3,6},{5,6}}},{p.dark,{{12,3},{11,5}}},
 {p.shadow,{{17,1},{19,2},{21,4}}},{p.base,{{25,5},{28,6},{30,6}}},
 {p.rock,{{28,7},{31,7}}},{p.ash,{{8,9},{11,8}}},
 {p.rim,{{1,12},{3,12},{5,11}}},{p.rock,{{8,11},{11,10},{13,11}}},
 {p.base,{{14,12},{16,13},{17,14}}},{p.rock,{{3,16},{6,17},{8,17}}},
 {p.dark,{{2,18},{5,19},{7,20}}},{p.rock,{{12,17},{15,16}}},
 {p.base,{{1,23},{4,24},{7,25}}},{p.rock,{{3,22},{5,23}}},
 {p.face,{{11,25},{12,26},{13,27}}},{p.rim,{{14,22},{16,21}}},
 {p.dark,{{18,28},{21,30}}},{p.face,{{19,31},{21,32}}},
 {p.dark,{{26,25},{25,27}}},{p.face,{{29,28},{31,27}}},
 {p.dark,{{30,30},{32,29}}},{p.rock,{{20,17},{22,16}}}
}
for _,v in ipairs(marks) do line(im[3],v[2],v[1]) end
-- One open, cooled fracture at the edge. No pits, rings, isolated ore flecks or branching gold.
poly(im[4],{{34,15},{34,24},{30,24},{27,25},{25,24},{27,22},{29,20},{30,17}},p.scorch)
poly(im[4],{{34,17},{34,23},{30,23},{28,24},{27,23},{30,20},{31,18}},p.warm)
poly(im[4],{{34,18},{34,22},{31,22},{29,23},{29,22},{31,20},{32,18}},p.red)
line(im[4],{{31,20},{30,22},{29,23}},p.ember)
dot(im[4],31,21,p.heat)
for i=1,#layers do s:newCel(layers[i],1,im[i],Point(0,0)) end
s.frames[1].duration=.1
s.data='meteor-solid-crust-v4; 32x32 opaque static; solid angular crust, no pits, no branching ore veins; pending visual confirmation'
s:saveAs(out..'meteorite-solid-crust-v4.aseprite')
local flat=Image(32,32,ColorMode.RGB);flat:drawSprite(s,1);flat:saveAs(out..'meteorite-solid-crust-v4.png')
local bright=0;for y=0,31 do for x=0,31 do local v=flat:getPixel(x,y);assert(pc.rgbaA(v)==255);if pc.rgbaR(v)>140 and pc.rgbaR(v)>pc.rgbaB(v)*2 then bright=bright+1 end end end
app.activeSprite=s;app.command.SpriteSize{ui=false,width=512,height=512,method='nearest'}
local pr=Image(512,512,ColorMode.RGB);pr:drawSprite(s,1);pr:saveAs(out..'texture-preview.png')
local report={version='meteor-solid-crust-v4',width=32,height=32,frames=1,layers=#layers,opaque=true,hotPixels=bright}
local f=io.open(out..'aseprite-report.json','w');f:write(json.encode(report));f:close()
return report
