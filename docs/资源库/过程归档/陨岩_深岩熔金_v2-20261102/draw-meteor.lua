local out='D:/alianximod/docs/资源库/过程归档/陨岩_深岩熔金_v2-20261102/'
local pc=app.pixelColor
local function C(hex) return pc.rgba(tonumber(hex:sub(1,2),16),tonumber(hex:sub(3,4),16),tonumber(hex:sub(5,6),16),255) end
local p={ink=C('191D22'),deep=C('24282C'),base=C('323538'),rock=C('3E4042'),plane=C('4A4A49'),edge=C('5B5853'),light=C('71695C'),warm=C('5E432D'),scorch=C('783E22'),ember=C('B65E21'),gold=C('EE9F32'),hot=C('FFD16B'),pin=C('FFE5A1')}
local function dot(im,x,y,c) if x>=0 and y>=0 and x<32 and y<32 then im:drawPixel(x,y,c) end end
local function polygon(im,pts,c)
 for y=0,31 do for x=0,31 do
  local inside=false;local j=#pts
  for i=1,#pts do local a,b=pts[i],pts[j]
   if ((a[2]>y+.5)~=(b[2]>y+.5)) and x+.5<(b[1]-a[1])*(y+.5-a[2])/(b[2]-a[2])+a[1] then inside=not inside end;j=i
  end
  if inside then dot(im,x,y,c) end
 end end
end
local function line(im,pts,c,width)
 for k=1,#pts-1 do
  local a,b=pts[k],pts[k+1];local n=math.max(math.abs(b[1]-a[1]),math.abs(b[2]-a[2]),1)
  for i=0,n do local x=math.floor(a[1]+(b[1]-a[1])*i/n+.5);local y=math.floor(a[2]+(b[2]-a[2])*i/n+.5)
   local w=width or 0
   for oy=-w,w do for ox=-w,w do if math.abs(ox)+math.abs(oy)<=w then dot(im,x+ox,y+oy,c) end end end
  end
 end
end
local sprite=Sprite(32,32,ColorMode.RGB)
local names={'岩壳底色','大块岩面','碎面与凹痕','炭化裂缝','熔金裂隙'}
local layers={sprite.layers[1]};for i=2,#names do layers[i]=sprite:newLayer() end
local imgs={};for i,n in ipairs(names) do layers[i].name=n;imgs[i]=Image(32,32,ColorMode.RGB) end
for y=0,31 do for x=0,31 do dot(imgs[1],x,y,p.base) end end
local rock=imgs[2]
local plates={
 {p.rock,{{-2,-2},{10,-2},{12,3},{9,7},{4,8},{-2,5}}},
 {p.plane,{{17,-2},{27,-2},{33,3},{29,8},{22,7},{17,10},{14,7},{15,3}}},
 {p.deep,{{10,-2},{18,-2},{15,3},{15,7},{12,10},{9,7},{12,3}}},
 {p.plane,{{-2,10},{4,9},{10,11},{10,16},{6,20},{1,19},{-2,16}}},
 {p.rock,{{15,11},{20,10},{24,12},{23,17},{19,20},{15,18},{13,15}}},
 {p.deep,{{26,9},{32,6},{34,18},{29,21},{24,17},{24,12}}},
 {p.rock,{{-2,23},{4,21},{10,23},{12,27},{9,33},{1,34},{-2,29}}},
 {p.plane,{{20,22},{25,20},{32,22},{33,27},{28,31},{24,33},{19,29}}},
 {p.deep,{{11,22},{16,19},{19,23},{18,28},{21,33},{12,34},{10,29}}},
 {p.rock,{{29,14},{34,12},{34,24},{30,23},{28,19}}}
}
for _,v in ipairs(plates) do polygon(rock,v[2],v[1]) end
local detail=imgs[3]
local lightEdges={{{1,1},{7,1},{9,3}},{{17,1},{24,1},{27,3}},{{1,11},{4,10},{8,12}},{{15,12},{18,11},{21,12}},{{2,24},{5,23},{8,24}},{{21,23},{24,22},{29,23}}}
for _,v in ipairs(lightEdges) do line(detail,v,p.edge) end
local darkEdges={{{0,6},{4,9},{8,8}},{{17,9},{21,8},{26,9}},{{2,19},{6,21},{10,17}},{{15,18},{18,21},{22,18}},{{1,29},{3,31},{7,31}},{{21,29},{24,31},{28,29}}}
for _,v in ipairs(darkEdges) do line(detail,v,p.deep) end
local facets={
 {p.rock,{{3,3},{7,3},{6,5},{3,5}}},{p.base,{{19,4},{23,3},{26,5},{23,6}}},
 {p.edge,{{4,12},{6,12},{7,13},{5,14}}},{p.base,{{2,14},{5,15},{3,17},{1,16}}},
 {p.base,{{17,14},{20,13},{21,15},{18,16}}},{p.edge,{{24,24},{27,24},{26,26},{24,26}}},
 {p.base,{{24,27},{28,26},{29,28},{26,29}}},{p.edge,{{4,25},{7,25},{6,27},{3,27}}}
}
for _,v in ipairs(facets) do polygon(detail,v[2],v[1]) end
for _,v in ipairs({{3,4},{21,4},{8,15},{17,15},{29,17},{5,28},{25,28},{12,2},{14,29},{2,22}}) do dot(detail,v[1],v[2],p.ink);dot(detail,v[1]+1,v[2],p.deep) end
local crack=imgs[4]
local main={{13,1},{12,4},{13,7},{11,10},{12,13},{14,15},{14,18},{17,20},{17,23},{19,26},{18,29},{20,31}}
local left={{12,13},{9,14},{7,17},{4,18},{1,18}}
local right={{14,18},{18,17},{20,15},{23,15},{25,12},{29,11}}
for _,path in ipairs({main,left,right}) do line(crack,path,p.deep,1);line(crack,path,p.ink,0) end
line(crack,{{13,6},{12,10},{13,13},{15,15}},p.warm,0)
line(crack,{{17,20},{18,23},{20,26}},p.warm,0)
line(crack,{{20,16},{23,16},{26,13}},p.scorch,0)
local molten=imgs[5]
local sections={
 {p.ember,{{12,4},{13,7},{11,10},{12,13},{14,15},{14,18}}},
 {p.ember,{{14,18},{18,17},{20,15},{23,15},{25,12}}},
 {p.scorch,{{9,14},{7,17},{4,18}}},
 {p.ember,{{17,21},{17,23},{19,26},{18,29}}},
 {p.gold,{{13,7},{11,10},{12,12}}},
 {p.gold,{{14,15},{14,17}}},
 {p.gold,{{18,17},{20,15},{22,15}}},
 {p.gold,{{17,23},{19,26}}},
 {p.hot,{{12,9},{11,10}}},
 {p.hot,{{19,16},{20,15}}},
 {p.hot,{{18,25},{19,26}}}
}
for _,v in ipairs(sections) do line(molten,v[2],v[1],0) end
dot(molten,14,16,p.hot);dot(molten,20,15,p.pin);dot(molten,19,26,p.pin)
for i=1,#layers do sprite:newCel(layers[i],1,imgs[i],Point(0,0)) end
sprite.frames[1].duration=.1
sprite.data='meteor-dark-gold-v2; 32x32 static opaque cube_all texture; native Aseprite layers; pending actual-preview approval'
sprite:saveAs(out..'meteorite-dark-gold.aseprite')
local flat=Image(32,32,ColorMode.RGB);flat:drawSprite(sprite,1);flat:saveAs(out..'meteorite-dark-gold.png')
local hotCount=0;for y=0,31 do for x=0,31 do local v=flat:getPixel(x,y);assert(pc.rgbaA(v)==255,'Opaque stone required');if pc.rgbaR(v)>150 and pc.rgbaR(v)>pc.rgbaB(v)*2 then hotCount=hotCount+1 end end end
app.activeSprite=sprite;app.command.SpriteSize{ui=false,width=512,height=512,method='nearest'}
local preview=Image(512,512,ColorMode.RGB);preview:drawSprite(sprite,1);preview:saveAs(out..'texture-preview-16x.png')
local report={version='meteor-dark-gold-v2',width=32,height=32,frames=1,opaque=true,brightHotPixels=hotCount,brightHotFraction=hotCount/1024,editableLayers=#layers}
local f=io.open(out..'aseprite-draw-report.json','w');f:write(json.encode(report));f:close()
return report
