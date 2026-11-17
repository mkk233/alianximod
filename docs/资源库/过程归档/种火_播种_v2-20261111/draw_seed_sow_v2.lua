local dir="D:/alianximod/docs/资源库/过程归档/种火_播种_v2-20261111"
local W,H,N=64,64,16
local cols={
 k=Color{r=92,g=32,b=12,a=255}, d=Color{r=168,g=48,b=12,a=255},
 r=Color{r=226,g=74,b=16,a=255}, o=Color{r=252,g=128,b=26,a=255},
 y=Color{r=255,g=198,b=52,a=255}, w=Color{r=255,g=246,b=184,a=255},
}
local clear=Color{r=0,g=0,b=0,a=0}
local function put(im,x,y,name)
 if x<0 or y<0 or x>=32 or y>=32 then return end
 local c=cols[name]; local X,Y=x*2,y*2
 im:drawPixel(X,Y,c); im:drawPixel(X+1,Y,c); im:drawPixel(X,Y+1,c); im:drawPixel(X+1,Y+1,c)
end
local function fill(im,x0,x1,y,name)
 for x=math.min(x0,x1),math.max(x0,x1) do put(im,x,y,name) end
end
local function band(im,cx,top,bottom,half,hot)
 for y=top,bottom do
  local t=(y-top)/math.max(1,bottom-top)
  local width=math.max(0,math.floor(half*(0.35+t*1.15)+0.5))
  local sway=math.floor(math.sin((y+cx)*0.9)* (t<0.5 and 1 or 0))
  local x=cx+sway
  local name=t>0.82 and "k" or (t>0.58 and "d" or (t>0.34 and "r" or (t>0.16 and "o" or "y")))
  fill(im,x-width,x+width,y,name)
  if hot and t<0.42 then fill(im,x,x,y,"w") end
  if t<0.25 then put(im,x,y,"w") end
 end
end
local function ground(im,y)
 fill(im,12,18,y,"k"); fill(im,13,17,y-1,"d"); put(im,14,y-1,"r"); put(im,16,y-1,"r")
end
local function spark(im,x,y,name) put(im,x,y,name); if name=="w" then put(im,x,y+1,"y") end end
local function drawFrame(i,s)
 local im=Image(W,H); im:clear(clear)
 ground(im,26)
 band(im,16,s[1],s[2],s[3],s[4])
 local side=math.max(2,s[3]-1)
 band(im,12,s[1]+4,25,math.max(1,side-1),false)
 band(im,20,s[1]+3,25,math.max(1,side-1),false)
 local spread=math.max(0,6-(math.abs(i-6)))
 spark(im,9-spread,18-math.floor(spread/2), i%2==0 and "w" or "y")
 spark(im,23+spread,19-math.floor(spread/2),"o")
 if i>3 and i<12 then spark(im,16,s[1]-2,"w"); spark(im,7,22,"y"); spark(im,25,21,"o") end
 return im
end
local function bounds(im)
 local x0,y0,x1,y1=im.width,im.height,-1,-1
 for it in im:pixels() do
  local c=Color(it())
  if c.alpha>0 then
   if it.x<x0 then x0=it.x end
   if it.y<y0 then y0=it.y end
   if it.x>x1 then x1=it.x end
   if it.y>y1 then y1=it.y end
  end
 end
 return x0,y0,x1,y1
end
local function scaleTo(src,scale,ax,ay)
 local dst=Image(W,H); dst:clear(clear)
 if scale<=0.02 then return dst end
 local x0,y0,x1,y1=bounds(src)
 local dx0=math.floor(ax+(x0-ax)*scale)-1
 local dy0=math.floor(ay+(y0-ay)*scale)-1
 local dx1=math.ceil(ax+(x1+1-ax)*scale)+1
 local dy1=math.ceil(ay+(y1+1-ay)*scale)+1
 for y=dy0,dy1 do
  for x=dx0,dx1 do
   local sx=math.floor(ax+(x+0.5-ax)/scale)
   local sy=math.floor(ay+(y+0.5-ay)/scale)
   if sx>=0 and sy>=0 and sx<W and sy<H then
    local c=Color(src:getPixel(sx,sy))
    if c.alpha>0 and x>=0 and y>=0 and x<W and y<H then dst:drawPixel(x,y,c) end
   end
  end
 end
 return dst
end
local function point(im,ax,ay)
 local x,y=math.floor(ax),math.floor(ay)
 im:drawPixel(x,y,cols.w); im:drawPixel(x-1,y,cols.y)
 im:drawPixel(x,y-1,cols.y); im:drawPixel(x-1,y-1,cols.o)
end

local burn={
 {22,24,1,true},{19,24,2,true},{16,24,2,true},{13,24,3,true},
 {10,24,4,true},{9,24,4,true},{11,24,4,true},{10,24,3,true},
}
local hero=drawFrame(6,burn[6])
local x0,y0,x1,y1=bounds(hero)
local ax=(x0+x1)/2
local ay=y1
local shrink={0.84,0.68,0.52,0.38,0.26,0.15,0.07}

local sp=Sprite(W,H,ColorMode.RGB)
sp.filename=dir.."/seed_sow_v2.aseprite"
while #sp.frames<N do sp:newFrame() end
for _,f in ipairs(sp.frames) do f.duration=0.08 end
local layer=sp.layers[1]; layer.name="seed"
for i,s in ipairs(burn) do
 sp:newCel(layer,sp.frames[i],drawFrame(i,s),Point(0,0))
end
for n,scale in ipairs(shrink) do
 local im=scaleTo(hero,scale,ax,ay)
 if scale<=0.15 then point(im,ax,ay) end
 sp:newCel(layer,sp.frames[8+n],im,Point(0,0))
end
local blank=Image(W,H); blank:clear(clear)
sp:newCel(layer,sp.frames[16],blank,Point(0,0))
sp:saveAs(sp.filename)
app.command.ExportSpriteSheet{ui=false,type="vertical",textureFilename=dir.."/seed_sow_v2-strip.png",dataFilename=dir.."/seed_sow_v2.json",dataFormat="json-array",splitLayers=false,trim=false,extrude=false,ignoreEmpty=false}
local function enlarge(srcSprite,factor,path)
 local pv=Sprite(srcSprite)
 pv:resize(W*factor,H*factor)
 for _,cel in ipairs(pv.cels) do
  local sc=Image(cel.image.width*factor,cel.image.height*factor)
  for it in cel.image:pixels() do
   local c=Color(it())
   if c.alpha>0 then
    for dy=0,factor-1 do for dx=0,factor-1 do sc:drawPixel(it.x*factor+dx,it.y*factor+dy,c) end end
   end
  end
  cel.image:resize(sc.width,sc.height); cel.image:clear(); cel.image:drawImage(sc)
  cel.position=Point(cel.position.x*factor,cel.position.y*factor)
 end
 pv:saveCopyAs(path); pv:close()
end
enlarge(sp,4,dir.."/seed_sow_v2.gif")
local board=Sprite(64*3*2+24,64*3+28,ColorMode.RGB)
while #board.frames<N do board:newFrame() end
for i,frame in ipairs(board.frames) do
 frame.duration=0.08
 local image=Image(board.width,board.height)
 image:clear(Color{r=16,g=18,b=22,a=255})
 local rendered=Image(W,H); rendered:drawSprite(sp,i)
 local big=Image(W*3,H*3)
 for it in rendered:pixels() do
  local c=Color(it())
  if c.alpha>0 then for dy=0,2 do for dx=0,2 do big:drawPixel(it.x*3+dx,it.y*3+dy,c) end end end
 end
 image:drawImage(big,Point(8,14))
 local second=Image(big.width,big.height)
 second:clear(Color{r=214,g=214,b=206,a=255}); second:drawImage(big)
 image:drawImage(second,Point(16+big.width,14))
 board:newCel(board.layers[1],frame,image,Point(0,0))
end
board:saveCopyAs(dir.."/seed_sow_v2_preview.gif")
board:close(); sp:close(); print("shrink")
