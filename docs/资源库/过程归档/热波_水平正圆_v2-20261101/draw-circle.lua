local out='D:/alianximod/docs/资源库/过程归档/热波_水平正圆_v2-20261101/'
local pc=app.pixelColor
local function col(r,g,b,a) return pc.rgba(r,g,b,a or 255) end
local function dot(im,x,y,c)
  x=math.floor(x+.5);y=math.floor(y+.5)
  if x>=0 and y>=0 and x<64 and y<64 then im:drawPixel(x,y,c) end
end
local function circle(im,cx,cy,r,w,c)
  for y=0,63 do for x=0,63 do
    local d=math.sqrt((x-cx)^2+(y-cy)^2)
    if math.abs(d-r)<w then im:drawPixel(x,y,c) end
  end end
end
local function stroke(im,x0,y0,x1,y1,c)
  local steps=math.max(1,math.ceil(math.max(math.abs(x1-x0),math.abs(y1-y0))))
  for j=0,steps do local u=j/steps;dot(im,x0+(x1-x0)*u,y0+(y1-y0)*u,c) end
end
local s=Sprite(64,64,ColorMode.RGB)
s.layers[1].name='glow';local layers={s.layers[1],s:newLayer(),s:newLayer()}
layers[2].name='body';layers[3].name='sparks'
local sheet=Image(64,1024,ColorMode.RGB)
local checks={}
for f=1,16 do
  if f>1 then s:newEmptyFrame(f) end
  s.frames[f].duration=((f-1)%4==2) and .03 or .04
  local t=(f-1)/16;local r=5+24*t;local a=math.floor(255*(1-t)^.6)
  local glow=Image(64,64,ColorMode.RGB);local body=Image(64,64,ColorMode.RGB);local sparks=Image(64,64,ColorMode.RGB)
  circle(glow,31.5,31.5,r,4,col(255,82,17,math.floor(a*.25)))
  circle(body,31.5,31.5,r,1.8,col(255,112,28,a))
  circle(body,31.5,31.5,r-1,.8,col(255,226,113,a))
  for j=1,12 do
    local an=j*math.pi/6+.1
    stroke(sparks,31.5+math.cos(an)*r,31.5+math.sin(an)*r,
      31.5+math.cos(an)*(r+2),31.5+math.sin(an)*(r+2),col(255,214,95,a))
  end
  local imgs={glow,body,sparks}
  for k=1,3 do s:newCel(layers[k],f,imgs[k],Point(0,0)) end
  for k=1,2 do
    local minx,miny,maxx,maxy=64,64,-1,-1
    for y=0,63 do for x=0,63 do
      local px=imgs[k]:getPixel(x,y)
      assert(px==imgs[k]:getPixel(y,x),'x/z symmetry mismatch')
      assert(px==imgs[k]:getPixel(63-x,y),'center symmetry mismatch')
      if pc.rgbaA(px)>0 then minx=math.min(minx,x);miny=math.min(miny,y);maxx=math.max(maxx,x);maxy=math.max(maxy,y) end
    end end
    assert(maxx-minx==maxy-miny,'unequal diameter')
  end
  assert(pc.rgbaA(body:getPixel(31,31))==0,'center must remain empty')
  sheet:drawSprite(s,f,Point(0,(f-1)*64))
  checks[#checks+1]={frame=f,radius=r,alpha=a,duration=s.frames[f].duration}
end
s.data='heat-circle-v2; horizontal world circle; center=(31.5,31.5); symmetric XZ; 16frames total0.6s; visual only, not repeated damage; pending user approval'
s:saveAs(out..'shock_ring_circle.aseprite')
sheet:saveAs(out..'shock_ring_circle-strip.png')
local still=Image(64,64,ColorMode.RGB);still:drawSprite(s,10);still:saveAs(out..'shock_ring_circle-frame10.png')
local preview=Sprite(64,64,ColorMode.RGB);preview.layers[1].name='actual-circle-preview'
for f=1,16 do
 if f>1 then preview:newEmptyFrame(f) end
 preview.frames[f].duration=s.frames[f].duration
 local im=Image(64,64,ColorMode.RGB);im:drawSprite(s,f);preview:newCel(preview.layers[1],f,im,Point(0,0))
end
preview:newEmptyFrame(17);preview.frames[17].duration=.4
app.command.SpriteSize{ui=false,width=512,height=512,method='nearest'}
preview:saveAs(out..'circle-2d-preview.gif')
preview:close()
print(json.encode{source=out..'shock_ring_circle.aseprite',frames=16,width=64,height=64,totalDuration=.6,symmetry='body and glow: exact x/y swap and centered mirror, all16 frames',checks=checks})
