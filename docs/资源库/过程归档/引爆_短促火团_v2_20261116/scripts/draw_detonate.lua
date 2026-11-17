local root="D:/alianximod/docs/资源库/过程归档/引爆_短促火团_v2_20261116"
local orange={"D2310D","F5540D","FF8B14","FFC129","FFE367","FFF8B7"}
local soul={"085C66","0795A2","0EC4CC","44EDE7","A7FFF0","E2FFF4"}
local palettes={orange=orange,soul=soul}
local poses={}
local function col(hex)
 return app.pixelColor.rgba(tonumber(hex:sub(1,2),16),tonumber(hex:sub(3,4),16),tonumber(hex:sub(5,6),16),255)
end
local function canvas() local t={} for y=0,63 do t[y]={} end return t end
local function poly(im,pts,v)
 for y=0,63 do for x=0,63 do
  local inside=false local j=#pts
  for i=1,#pts do local a,b=pts[i],pts[j]
   if ((a[2]>y+.5)~=(b[2]>y+.5)) and x+.5<(b[1]-a[1])*(y+.5-a[2])/(b[2]-a[2])+a[1] then inside=not inside end
   j=i
  end
  if inside then im[y][x]=v end
 end end
end
local shapes={
 {{-.9,-.45},{-.5,-.8},{-.1,-.8},{.02,-1.2},{.50,-.83},{.50,-.6},{.90,-.3},{.90,.25},{.55,.7},{.1,.87},{-.45,.7},{-.85,.35}},
 {{-1,-.2},{-.64,-.5},{-.62,-.9},{-.1,-1},{.25,-.75},{.75,-.65},{.92,-.15},{.65,.35},{.72,.58},{.15,.86},{-.45,.72},{-.72,.28}},
 {{-.85,-.5},{-.25,-.85},{.1,-.75},{.48,-1.05},{.7,-.55},{.92,-.08},{.77,.4},{.25,.72},{-.1,.9},{-.65,.65},{-.92,.15}}
}
local function lobe(im,x,y,rx,ry,v,shape)
 local p={} for _,a in ipairs(shapes[shape or 1]) do
  p[#p+1]={math.floor(x+a[1]*rx+.5),math.floor(y+a[2]*ry+.5)}
 end poly(im,p,v)
end
local lobes={{-13,-1,9,9,1},{-9,-12,9,8,2},{4,-15,8,9,1},{15,-7,9,8,3},{15,7,8,9,2},{3,13,10,8,3},{-10,10,9,8,1}}
local expansion={.22,.42,.66,.86,1,1.03,1.04}
for f=1,16 do
 local mass,heart,ember=canvas(),canvas(),canvas()
 if f<=7 then
  local s=expansion[f]
  for _,b in ipairs(lobes) do lobe(mass,32+b[1]*s,34+b[2]*s,b[3]*s,b[4]*s,1,b[5]) end
  lobe(mass,32,34,16*s,15*s,1,2)
  for i,b in ipairs(lobes) do lobe(mass,32+b[1]*s,33+b[2]*s,(b[3]-1.4)*s,(b[4]-1.5)*s,2,b[5]) end
  lobe(mass,32,34,15*s,14*s,3,2)
  for i,b in ipairs(lobes) do
   if i%2==1 then lobe(heart,32+(b[1]-.5)*s,33+(b[2]-.5)*s,(b[3]-3.4)*s,(b[4]-3.5)*s,3,b[5]) end
  end
  lobe(heart,31,33,11.2*s,11.4*s,4,1)
  lobe(heart,29,31,7.8*s,8.5*s,5,2)
  if f<=5 then lobe(heart,29,31,4.8*s,5.8*s,6,3) else lobe(heart,28,29,3.6*s,4.4*s,5,3) end
  if f>=6 then
   poly(heart,{{33,36},{36,34},{39,34},{39,38},{36,41},{32,41}},3)
   poly(heart,{{24,25},{27,25},{27,29},{25,30}},4)
  end
 elseif f<=12 then
  local t=f-7
  local shrink=({.89,.73,.57,.40,.25})[t]
  local spread=({1.04,1.10,1.16,1.23,1.29})[t]
  for i,b in ipairs(lobes) do
   local alive=(t==1) or (t==2 and i~=5 and i~=7) or (t==3 and (i==1 or i==2 or i==4 or i==6)) or (t==4 and (i==2 or i==4 or i==6)) or (t==5 and (i==2 or i==4))
   if alive then
   local x=32+b[1]*spread+(i%2==0 and 1 or -1)*t*.2
   local y=34+b[2]*spread-t*.40
   local r=shrink*(i==2 and .91 or 1)
   lobe(mass,x,y,b[3]*r,b[4]*r,1,b[5])
   lobe(mass,x-.3,y-.5,(b[3]-1.3)*r,(b[4]-1.7)*r,2,b[5])
   if t<=3 then lobe(heart,x-1,y-1.2,(b[3]-3.2)*r,(b[4]-3.2)*r,3,b[5]) end
   if t<=2 and i%3~=0 then lobe(heart,x-1,y-1.5,(b[3]-5)*r,(b[4]-4.8)*r,4,b[5]) end
   end
  end
  if f==8 then lobe(heart,29,31,6,7,4,2);lobe(heart,28,29,3,4,5,1)
  elseif f==9 then lobe(heart,29,30,3,4,3,2) end
 elseif f<=15 then
  local flecks={{21,15},{53,20}}
  for i,p in ipairs(flecks) do
   if (f<=14) or (f==15 and i==1) then
    local r=f==13 and 1.8 or (f==14 and 1.3 or .8)
    lobe(ember,p[1]+(i-2)*.3*(f-13),p[2]-(f-13),r,r*1.2,f==13 and 2 or 1,(i%3)+1)
   end
  end
 end
 poses[f]={mass,heart,ember}
end
local layerNames={"01_thick_fire_lobes","02_bright_broken_heart","03_final_embers"}
local images={}
for _,variant in ipairs({"orange","soul"}) do
 local sp=Sprite(64,64,ColorMode.RGB)
 local layers={sp.layers[1],sp:newLayer(),sp:newLayer()}
 for i,l in ipairs(layers) do l.name=layerNames[i] end
 for f=2,16 do sp:newEmptyFrame() end
 local sheet=Image(64,1024);images[variant]={}
 for f=1,16 do
  local merged=Image(64,64)
  for li=1,3 do
   local im=Image(64,64)
   for y=0,63 do for x=0,63 do local v=poses[f][li][y][x];if v then im:drawPixel(x,y,col(palettes[variant][v])) end end end
   sp:newCel(layers[li],f,im,Point(0,0));merged:drawImage(im)
  end
  sp.frames[f].duration=.05
  images[variant][f]=merged
  sheet:drawImage(merged,Point(0,(f-1)*64))
  merged:saveAs(root.."/frames/"..variant..string.format("/frame_%02d.png",f))
 end
 local tag=sp:newTag(1,16);tag.name="detonate_once_800ms";tag.aniDir=AniDir.FORWARD
 sp:saveAs(root.."/source/detonate_v2_"..variant..".aseprite")
 sheet:saveAs(root.."/textures/detonate_v2_"..variant.."-strip.png")
 sp:saveCopyAs(root.."/previews/raw_"..variant.."_once.gif")
 sp:close()
end
local function rect(im,x,y,w,h,c)
 local co=col(c) for yy=y,y+h-1 do for xx=x,x+w-1 do if xx>=0 and yy>=0 and xx<im.width and yy<im.height then im:drawPixel(xx,yy,co) end end end
end
local board=Sprite(864,336,ColorMode.RGB)
board.layers[1].name="AUX_target_ground_strength_comparison"
for f=2,28 do board:newEmptyFrame() end
local scales={.8,1.2,1.6}
local frameImage={}
for f=1,28 do
 local im=Image(864,336);im:clear(Color{r=24,g=30,b=39,a=255})
 for row,variant in ipairs({"orange","soul"}) do for j,span in ipairs(scales) do
  local cx=(j-1)*288+144;local base=(row-1)*168+145
  rect(im,(j-1)*288+5,(row-1)*168+8,278,150,"202A36")
  rect(im,(j-1)*288+5,base,278,4,"56606A")
  for x=(j-1)*288+16,j*288-1,48 do rect(im,x,base+4,1,9,"364452") end
  rect(im,cx-11,base-130,22,32,"8D9AA0")
  rect(im,cx-13,base-98,26,49,"536D79")
  rect(im,cx-24,base-98,11,49,"78919B");rect(im,cx+13,base-98,11,49,"78919B")
  rect(im,cx-12,base-49,11,49,"3A505F");rect(im,cx+1,base-49,11,49,"3A505F")
  rect(im,cx-7,base-118,3,3,"28323B");rect(im,cx+4,base-118,3,3,"28323B")
  if f<=16 then
   local face=Image(images[variant][f]);local px=math.floor(span*72+.5);face:resize(px,px)
   im:drawImage(face,Point(cx-math.floor(px/2),base-72-math.floor(px/2)))
  end
 end end
 board.frames[f].duration=.05;board:newCel(board.layers[1],f,im)
 if f==5 then im:saveAs(root.."/previews/strength_compare_unlabelled.png") end
end
board:saveAs(root.."/source/target_comparison_preview.aseprite")
board:saveCopyAs(root.."/previews/target_comparison_2d.gif")
board:close()
local contact=Image(1024,512);contact:clear(Color{r=24,g=30,b=39,a=255})
for row,variant in ipairs({"orange","soul"}) do for j,f in ipairs({1,3,5,7,9,11,13,16}) do
 local a=Image(images[variant][f]);a:resize(128,128);contact:drawImage(a,Point((j-1)*128,(row-1)*256+40))
end end
contact:saveAs(root.."/previews/keyframes_unlabelled.png")
return {status="candidate_pending_user_confirmation",frames=16,durationMs=800,size={64,64},palette=palettes,previewPauseMs=600,output=root}
