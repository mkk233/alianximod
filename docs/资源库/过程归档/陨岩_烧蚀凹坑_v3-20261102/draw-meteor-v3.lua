local out='D:/alianximod/docs/资源库/过程归档/陨岩_烧蚀凹坑_v3-20261102/'
local pc=app.pixelColor
local function C(h) return pc.rgba(tonumber(h:sub(1,2),16),tonumber(h:sub(3,4),16),tonumber(h:sub(5,6),16),255) end
local p={black=C('171719'),shadow=C('232123'),base=C('332D2B'),skin=C('403632'),face=C('4C4038'),ridge=C('5A4B3E'),ash=C('6E5B48'),edge=C('8A7053'),warm=C('55342B'),rust=C('743B28'),red=C('9E4729'),ember=C('C96632'),hot=C('E89A4E')}
local function dot(im,x,y,c) if x>=0 and x<32 and y>=0 and y<32 then im:drawPixel(x,y,c) end end
local function poly(im,pts,c)
 for y=0,31 do for x=0,31 do
  local inside=false;local j=#pts
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
local names={'黑褐烧蚀壳','熔壳流痕与剥落','大小蚀坑','凹坑内壁与断面','破口余热'}
local layers={s.layers[1]};for i=2,#names do layers[i]=s:newLayer() end
local im={};for i,n in ipairs(names) do layers[i].name=n;im[i]=Image(32,32,ColorMode.RGB) end
for y=0,31 do for x=0,31 do dot(im[1],x,y,p.base) end end
-- Broad fused folds, cut by pits rather than a branching vein network.
local folds={
 {p.skin,{{-2,-2},{12,-2},{10,2},{8,4},{5,6},{1,10},{-2,11}}},
 {p.face,{{5,-2},{13,-2},{11,1},{8,3},{6,6},{3,7},{5,3}}},
 {p.skin,{{17,-2},{23,-2},{20,4},{20,9},{17,13},{13,16},{10,15},{15,9},{16,3}}},
 {p.face,{{16,-2},{20,-2},{18,3},{18,7},{16,9},{15,6}}},
 {p.shadow,{{29,-2},{34,-2},{34,11},{30,14},{27,13},{26,9},{29,5}}},
 {p.face,{{30,10},{33,8},{34,17},{29,19},{24,17},{26,14}}},
 {p.skin,{{-2,17},{3,16},{7,18},{10,20},{14,21},{13,25},{9,28},{3,27},{-2,30}}},
 {p.face,{{-2,19},{4,18},{8,21},{11,22},{9,25},{7,24},{4,21},{0,22}}},
 {p.skin,{{14,20},{18,16},{23,16},{24,21},{19,26},{17,31},{11,34},{10,30},{14,25}}},
 {p.ridge,{{17,18},{20,17},{21,18},{20,21},{17,23},{15,25},{15,22}}},
 {p.face,{{25,28},{30,27},{34,25},{34,32},{22,34},{22,31}}},
 {p.shadow,{{-2,12},{4,10},{7,12},{6,15},{2,17},{-2,18}}}
}
for _,v in ipairs(folds) do poly(im[2],v[2],v[1]) end
line(im[2],{{10,0},{7,3},{5,4}},p.ridge)
line(im[2],{{18,1},{17,5}},p.ridge)
line(im[2],{{0,20},{3,19},{5,20}},p.ridge)
line(im[2],{{30,12},{28,14},{26,15}},p.ridge)
line(im[2],{{26,30},{29,29},{31,28}},p.ridge)
-- Asymmetric scooped pits. Dark upper wall and broken lower lip give inward depth.
local pits={
 {outer={{7,4},{12,3},{15,5},{15,8},{13,12},{8,14},{5,12},{4,8}},inner={{7,6},{11,5},{13,6},{13,9},{11,11},{7,12},{5,10}},floor={{7,11},{10,10},{13,8},{13,11},{10,13},{7,13}},lip={{6,12},{8,14},{11,13},{13,12},{15,8}}},
 {outer={{22,0},{27,1},{29,3},{28,7},{25,9},{21,7},{20,4}},inner={{23,2},{26,2},{27,4},{26,6},{23,7},{21,5}},floor={{22,6},{25,6},{27,4},{27,7},{24,8}},lip={{22,8},{25,9},{27,8},{29,5}}},
 {outer={{23,19},{28,19},{31,22},{30,26},{27,28},{22,27},{20,24},{21,21}},inner={{23,20},{27,20},{29,22},{28,25},{25,26},{22,25},{21,23}},floor={{22,24},{25,24},{28,22},{28,25},{25,27},{22,26}},lip={{22,27},{25,28},{28,27},{30,24}}},
 {outer={{4,24},{8,23},{11,26},{10,29},{7,31},{3,30},{2,27}},inner={{4,25},{7,25},{9,26},{8,29},{5,30},{3,28}},floor={{4,28},{7,28},{9,26},{9,29},{6,30}},lip={{4,30},{7,31},{10,29}}},
 {outer={{-2,11},{2,10},{4,12},{3,15},{0,17},{-2,15}},inner={{-2,12},{1,11},{2,13},{1,15},{-2,16}},floor={{0,15},{2,13},{3,14},{1,17},{-2,17}},lip={{0,17},{3,15}}}
}
for _,v in ipairs(pits) do
 poly(im[3],v.outer,p.shadow);poly(im[3],v.inner,p.black)
 poly(im[4],v.floor,p.skin);line(im[4],v.lip,p.ridge)
end
-- Chipped outer lips and short ablation flutes, never connected to an ore vein.
for _,v in ipairs({{{7,4},{10,3},{12,3}},{{21,3},{22,1},{24,1}},{{22,20},{24,19},{27,19}},{{3,26},{5,24},{7,24}}}) do line(im[4],v,p.face) end
for _,v in ipairs({{{8,14},{10,14},{12,13}},{{26,9},{28,7}},{{26,28},{28,27}},{{5,31},{7,31}}}) do line(im[4],v,p.ash) end
for _,v in ipairs({{{13,0},{12,1}},{{3,2},{2,4},{1,5}},{{17,12},{16,14},{14,16}},{{21,11},{20,13},{18,15}},{{12,26},{11,29},{10,30}},{{29,16},{31,15}}}) do line(im[4],v,p.shadow) end
line(im[4],{{17,17},{19,16},{21,16}},p.ash)
line(im[4],{{18,18},{19,18}},p.edge)
poly(im[4],{{13,17},{16,17},{16,19},{14,20},{12,19}},p.shadow)
dot(im[4],14,17,p.black);dot(im[4],15,18,p.black);dot(im[4],14,20,p.face)
dot(im[4],30,2,p.black);dot(im[4],31,3,p.shadow);dot(im[4],30,4,p.face)
dot(im[4],1,30,p.shadow);dot(im[4],2,31,p.face)
-- Two exposed hot pockets; ember color stays inside the scooped breaks.
poly(im[5],{{23,23},{25,22},{27,23},{27,25},{24,26},{22,25}},p.warm)
poly(im[5],{{24,24},{26,23},{27,24},{26,26},{23,26}},p.rust)
poly(im[5],{{24,25},{25,24},{27,24},{26,25},{24,26}},p.red)
dot(im[5],25,25,p.ember);dot(im[5],26,24,p.ember);dot(im[5],26,25,p.hot)
line(im[5],{{7,11},{9,11},{10,10}},p.warm)
line(im[5],{{8,12},{10,11},{11,10}},p.rust)
dot(im[5],10,11,p.red);dot(im[5],9,12,p.ember)
for i=1,#layers do s:newCel(layers[i],1,im[i],Point(0,0)) end
s.frames[1].duration=.1;s.data='meteor-ablation-v3; 32x32 static opaque; pitted fused crust, ember pockets, no gold veins; pending user preview'
s:saveAs(out..'meteorite-ablation-v3.aseprite')
local flat=Image(32,32,ColorMode.RGB);flat:drawSprite(s,1);flat:saveAs(out..'meteorite-ablation-v3.png')
local hot=0;for y=0,31 do for x=0,31 do local v=flat:getPixel(x,y);assert(pc.rgbaA(v)==255);if pc.rgbaR(v)>140 and pc.rgbaR(v)>pc.rgbaB(v)*2 then hot=hot+1 end end end
app.activeSprite=s;app.command.SpriteSize{ui=false,width=512,height=512,method='nearest'}
local pr=Image(512,512,ColorMode.RGB);pr:drawSprite(s,1);pr:saveAs(out..'texture-preview.png')
local report={width=32,height=32,frames=1,layers=#layers,opaque=true,hotPixels=hot,version='meteor-ablation-v3'}
local f=io.open(out..'aseprite-report.json','w');f:write(json.encode(report));f:close()
return report
