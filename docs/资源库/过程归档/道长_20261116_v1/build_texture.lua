local dir=args.dir
local sprite=Sprite(128,64,ColorMode.RGB)
sprite.filename=dir.."/daozhang_v1.aseprite"
sprite.layers[1].name="01_material_base"
local base=Image(128,64,ColorMode.RGB)
local detail=Image(128,64,ColorMode.RGB)
local face=Image(128,64,ColorMode.RGB)
local accent=Image(128,64,ColorMode.RGB)
local function col(h) return app.pixelColor.rgba(tonumber(h:sub(1,2),16),tonumber(h:sub(3,4),16),tonumber(h:sub(5,6),16),255) end
local palettes={
skin={"c8ac8d","dfc6a4","ae9174","b99c7e"},
robe={"71878a","82999a","586e74","667e83"},
ivory={"d4cdb2","eee5ca","aaa58f","bdb9a2"},
shoe={"35484a","526668","26383c","405559"},
hair={"e0dfd0","f6f1de","acaeaa","c5c8bd"},
jade={"6f958a","a4bca2","476c65","5e8377"}}
local function rect(im,x,y,w,h,c)
 for yy=y,y+h-1 do for xx=x,x+w-1 do im:drawPixel(xx,yy,col(c)) end end
end
local function dot(im,x,y,c) im:drawPixel(x,y,col(c)) end
local function net(p)
 local u,v,w,h,d=p.u,p.v,p.w,p.h,p.d
 return {
  {name="up",x=u+d,y=v,w=w,h=d},
  {name="down",x=u+d+w,y=v,w=w,h=d},
  {name="east",x=u,y=v+d,w=d,h=h},
  {name="north",x=u+d,y=v+d,w=w,h=h},
  {name="west",x=u+d+w,y=v+d,w=d,h=h},
  {name="south",x=u+2*d+w,y=v+d,w=w,h=h}
 }
end
for _,p in ipairs(args.patches) do
 local pal=palettes[p.material]
 for _,f in ipairs(net(p)) do
  local c=pal[1]
  if f.name=="up" then c=pal[2] elseif f.name=="down" then c=pal[3] elseif f.name=="east" or f.name=="west" then c=pal[4] end
  rect(base,f.x,f.y,f.w,f.h,c)
  if p.material=="robe" then
   if f.h>3 then
    for y=2,f.h-2 do
     if f.w>2 then dot(detail,f.x+1,f.y+y,pal[2]) end
     if f.w>4 then dot(detail,f.x+f.w-2,f.y+y,pal[3]) end
    end
   end
   if p.name=="skirt" then
    rect(accent,f.x,f.y+f.h-2,f.w,1,"a4b3ad")
    rect(accent,f.x,f.y+f.h-1,f.w,1,"465f66")
   elseif p.name=="sleeve" and f.name~="up" and f.name~="down" then
    rect(accent,f.x,f.y+f.h-2,f.w,1,"d4cdb2")
    rect(accent,f.x,f.y+f.h-1,f.w,1,"526b71")
   end
  elseif p.material=="hair" then
   for x=0,f.w-1 do
    if x%3==1 then
     for y=1,f.h-1 do dot(detail,f.x+x,f.y+y,(y%4==0) and pal[4] or pal[2]) end
    end
   end
  elseif p.material=="ivory" then
   if f.w>1 and f.h>3 then rect(detail,f.x,f.y+1,1,f.h-2,pal[3]) end
  elseif p.material=="shoe" then
   if f.h>1 then rect(detail,f.x,f.y+f.h-1,f.w,1,pal[3]) end
  end
 end
 if p.name=="head" then
  local x,y=p.u+p.d,p.v+p.d
  rect(face,x,y,7,1,"e0dfd0")
  dot(face,x,y+1,"c5c8bd") dot(face,x+6,y+1,"c5c8bd")
  rect(face,x+1,y+1,5,1,"dfc6a4")
  dot(face,x+1,y+3,"34403d") dot(face,x+5,y+3,"34403d")
  dot(face,x+1,y+4,"ae9174") dot(face,x+5,y+4,"ae9174")
  dot(face,x+2,y+3,"dfc6a4") dot(face,x+4,y+3,"dfc6a4")
  rect(face,x+2,y+5,3,1,"9d8069")
  rect(face,x+1,y+6,5,1,"e0dfd0")
 elseif p.name=="torso" then
  local x,y=p.u+p.d,p.v+p.d
  for r=0,5 do
   local a=math.min(3,math.floor(r/2))
   dot(accent,x+a,y+r,"d4cdb2")
   dot(accent,x+6-a,y+r,"eee5ca")
  end
  rect(accent,x+3,y+6,1,5,"60797e")
 elseif p.name=="bun" or p.name=="bun_base" then
  local x,y=p.u+p.d,p.v+p.d
  rect(accent,x,y+p.h-1,p.w,1,"6f958a")
 elseif p.name=="sash" then
  local x,y=p.u+p.d,p.v+p.d
  rect(accent,x,y+p.h-2,p.w,1,"6f958a")
 elseif p.name=="clasp" then
  local x,y=p.u+p.d,p.v+p.d
  dot(accent,x,y,"b6c7ae")
  dot(accent,x+1,y+1,"476c65")
 end
end
rect(base,96,44,32,20,"849093")
app.transaction("Draw Daozhang v1 original pixel atlas",function()
 sprite.cels[1].image=base
 for _,item in ipairs({{"02_pixel_folds_and_hair",detail},{"03_face",face},{"04_collar_cuffs_jade",accent}}) do
  local l=sprite:newLayer() l.name=item[1] sprite:newCel(l,1,item[2],Point(0,0))
 end
end)
sprite:saveAs(dir.."/daozhang_v1.aseprite")
local flat=Image(sprite.spec)
flat:drawSprite(sprite,1)
flat:saveAs(dir.."/daozhang_v1.png")
local big=Image(1024,512,ColorMode.RGB)
for yy=0,511 do for xx=0,1023 do
 local p=flat:getPixel(math.floor(xx/8),math.floor(yy/8))
 if app.pixelColor.rgbaA(p)==0 then
 local g=(math.floor(xx/32)+math.floor(yy/32))%2==0 and 52 or 60
 p=app.pixelColor.rgba(g,g+5,g+6,255)
 end
 big:drawPixel(xx,yy,p)
end end
big:saveAs(dir.."/preview_texture_8x.png")
app.activeSprite=sprite
app.refresh()
return {file=sprite.filename,width=sprite.width,height=sprite.height,layers=#sprite.layers,frames=#sprite.frames,atlas=dir.."/daozhang_v1.png",preview=dir.."/preview_texture_8x.png"}

