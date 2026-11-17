local out = args.out
app.fs.makeAllDirectories(out)
local W,H = 96,112
local rgba = app.pixelColor.rgba
local floor, sin, pi = math.floor, math.sin, math.pi
local function round(n) return floor(n+0.5) end
local C = {
  shadow=rgba(119,48,29,190),
  red=rgba(190,67,27,230),
  orange=rgba(248,111,32,245),
  gold=rgba(255,173,64,255),
  core=rgba(255,218,132,255),
  ember=rgba(255,178,76,225),
}
local function pixel(img,x,y,c)
  x,y=round(x),round(y)
  if x>=0 and y>=0 and x<img.width and y<img.height then img:drawPixel(x,y,c) end
end
local function rect(img,x1,y1,x2,y2,c)
  for y=round(y1),round(y2) do
    for x=round(x1),round(x2) do pixel(img,x,y,c) end
  end
end
local function poly(img,pts,c)
  local ymin,ymax=H,-1
  for _,p in ipairs(pts) do ymin=math.min(ymin,p[2]);ymax=math.max(ymax,p[2]) end
  for y=math.max(0,floor(ymin)),math.min(img.height-1,math.ceil(ymax)) do
    local crosses={}
    for i=1,#pts do
      local a,b=pts[i],pts[i%#pts+1]
      if (a[2]<=y+0.5 and b[2]>y+0.5) or (b[2]<=y+0.5 and a[2]>y+0.5) then
        crosses[#crosses+1]=a[1]+(y+0.5-a[2])*(b[1]-a[1])/(b[2]-a[2])
      end
    end
    table.sort(crosses)
    for i=1,#crosses-1,2 do
      for x=math.ceil(crosses[i]),floor(crosses[i+1]) do pixel(img,x,y,c) end
    end
  end
end
local function tongue(img,cx,base,height,width,phase,seed,power)
  local h=math.max(2,round(height*power))
  if power<0.03 then return end
  local w=width
  local lean=round(1.3*sin(phase+seed))
  local flick=round(sin(phase*2+seed)*0.8)
  local pts={
    {cx-w,base},{cx+w,base},{cx+w+0.4,base-h*0.22},
    {cx+w+flick,base-h*0.43},{cx+0.9+lean,base-h*0.63},
    {cx+lean+0.8,base-h*0.86},{cx+lean,base-h},
    {cx+lean-0.9,base-h*0.73},{cx-0.8,base-h*0.59},
    {cx-w,base-h*0.45},{cx-w-0.8,base-h*0.19}
  }
  poly(img,pts,C.red)
  poly(img,{
    {cx-w+0.6,base},{cx+w-0.3,base},{cx+w-0.2,base-h*0.28},
    {cx+lean+0.1,base-h*0.81},{cx+lean-0.6,base-h*0.64},
    {cx-0.7,base-h*0.42}
  },C.orange)
  poly(img,{
    {cx-0.5,base},{cx+0.8,base},{cx+0.7,base-h*0.27},
    {cx+lean*0.4,base-h*0.60},{cx-0.6,base-h*0.33}
  },C.gold)
  if h>=10 then
    rect(img,cx,base-3,cx,base-1,C.core)
    pixel(img,cx+lean,base-h,C.orange)
  end
  if h>=22 then
    local side=seed%2<1 and -1 or 1
    poly(img,{
      {cx+side*w*0.6,base-h*0.29},
      {cx+side*(w+1),base-h*0.48},
      {cx+side*(w+1.5),base-h*0.69},
      {cx+side*(w-0.1),base-h*0.49},
      {cx,base-h*0.36}
    },C.orange)
  end
end
local names={
  '01 rear warmth',
  '02 short hand flames rear',
  '03 tall leg flames rear',
  '04 torso heat',
  '05 short hand flames front',
  '06 tall leg flames front',
  '07 rising embers',
}
local function drawFrame(f,power)
  local imgs={}
  for i=1,#names do imgs[i]=Image(W,H,ColorMode.RGB) end
  local phase=f/20*2*pi
  local hand=power
  local leg=power
  if power>0 then
    for _,x in ipairs({24,35,60,71}) do
      rect(imgs[1],x,64,x,68,rgba(183,67,30,round(130*power)))
    end
    rect(imgs[1],35,101,47,105,rgba(175,57,26,round(120*power)))
    rect(imgs[1],49,101,61,105,rgba(175,57,26,round(120*power)))
    tongue(imgs[2],23,69,15+2*sin(phase),1.7,phase,0.4,hand)
    tongue(imgs[2],35,68,12+2*sin(phase+1.3),1.2,phase,1.7,hand)
    tongue(imgs[2],60,68,12+2*sin(phase+2.1),1.2,phase,3.2,hand)
    tongue(imgs[2],72,69,15+2*sin(phase+3.1),1.7,phase,4.1,hand)
    tongue(imgs[3],34,104,28+3*sin(phase+0.4),2.0,phase,1.1,leg)
    tongue(imgs[3],46,104,25+3*sin(phase+2.4),1.4,phase,2.6,leg)
    tongue(imgs[3],49,104,25+3*sin(phase+4.1),1.4,phase,4.2,leg)
    tongue(imgs[3],61,104,28+3*sin(phase+3.0),2.0,phase,5.8,leg)
    tongue(imgs[4],37,67,8+2*sin(phase+1.1),1.1,phase,3.8,power)
    tongue(imgs[4],58,67,9+2*sin(phase+3.5),1.1,phase,2.2,power)
    tongue(imgs[4],41,57,7+sin(phase+0.6),0.9,phase,1.4,power)
    tongue(imgs[4],54,53,7+sin(phase+3.1),0.9,phase,4.5,power)
    tongue(imgs[5],26,68,11+2*sin(phase+0.8),1.15,phase,0.9,hand)
    tongue(imgs[5],69,68,11+2*sin(phase+3.4),1.15,phase,3.9,hand)
    tongue(imgs[6],38,104,23+3*sin(phase+1.8),1.35,phase,2.3,leg)
    tongue(imgs[6],57,104,23+3*sin(phase+4.3),1.35,phase,5.2,leg)
    rect(imgs[6],36,104,41,104,C.orange)
    rect(imgs[6],54,104,59,104,C.orange)
    for i=1,6 do
      local starts={{21,68,21},{75,68,21},{32,102,30},{64,102,30},{39,98,27},{56,98,27}}
      local s=starts[i]
      local travel=(f/20+(i*0.173))%1
      if travel>0.15 and travel<0.78 and power>0.65 then
        local x=s[1]+round(sin(phase+i)*1.2)
        local y=s[2]-floor(travel*s[3])
        local alpha=round(190*(1-travel))
        pixel(imgs[7],x,y,rgba(255,165,67,alpha))
        if i%2==0 then pixel(imgs[7],x,y-1,rgba(255,139,47,round(alpha*0.55))) end
      end
    end
  end
  return imgs
end
local function newSource(kind,count)
  local s=Sprite(W,H,ColorMode.RGB)
  local ls={}
  for i,name in ipairs(names) do
    local l=i==1 and s.layers[1] or s:newLayer()
    l.name=name;ls[i]=l
  end
  for f=0,count-1 do
    local frame=f==0 and s.frames[1] or s:newEmptyFrame()
    frame.duration=kind=='entry' and 0.06 or 0.08
    local power=kind=='entry' and ((f+1)/count)^0.8 or 1
    local imgs=drawFrame(f,power)
    for i,l in ipairs(ls) do
      local old=l:cel(frame)
      if old then old.image=imgs[i] else s:newCel(l,frame,imgs[i],Point(0,0)) end
    end
  end
  s:saveAs(out..'/heat_up_v3_1'..(kind=='entry' and '_entry' or '')..'.aseprite')
  local strip=Image(W,H*count,ColorMode.RGB)
  for f=1,count do
    local im=Image(W,H,ColorMode.RGB)
    im:drawSprite(s,f)
    strip:drawImage(im,Point(0,(f-1)*H))
  end
  strip:saveAs(out..'/heat_up_v3_1'..(kind=='entry' and '_entry' or '')..'_strip.png')
  return s
end
local loop=newSource('loop',20)
local entry=newSource('entry',8)
local mannequin=Image(W,H,ColorMode.RGB)
local skin=rgba(120,142,155,255)
local shade=rgba(51,68,82,255)
local body=rgba(68,87,103,255)
local lit=rgba(81,101,116,255)
rect(mannequin,36,8,59,31,skin)
rect(mannequin,36,8,39,31,rgba(101,121,136,255))
rect(mannequin,36,8,59,10,rgba(145,162,172,255))
rect(mannequin,41,19,43,21,rgba(35,51,63,255))
rect(mannequin,52,19,54,21,rgba(35,51,63,255))
rect(mannequin,36,32,59,67,body)
rect(mannequin,37,33,39,35,lit)
rect(mannequin,36,36,38,67,shade)
rect(mannequin,24,32,35,58,body)
rect(mannequin,60,32,71,58,body)
rect(mannequin,24,32,26,58,lit)
rect(mannequin,69,32,71,58,shade)
rect(mannequin,24,59,35,67,skin)
rect(mannequin,60,59,71,67,skin)
rect(mannequin,24,59,26,67,rgba(99,122,137,255))
rect(mannequin,69,59,71,67,rgba(99,122,137,255))
rect(mannequin,36,68,46,103,shade)
rect(mannequin,49,68,59,103,shade)
rect(mannequin,37,69,39,100,lit)
rect(mannequin,50,69,52,100,lit)
rect(mannequin,36,101,46,103,rgba(37,51,64,255))
rect(mannequin,49,101,59,103,rgba(37,51,64,255))
local glyphs={
 H={'101','101','111','101','101'}, E={'111','100','110','100','111'},
 A={'010','101','111','101','101'}, T={'111','010','010','010','010'},
 U={'101','101','101','101','111'}, P={'110','101','110','100','100'},
 V={'101','101','101','101','010'}, ['3']={'110','001','010','001','110'},
 ['1']={'010','110','010','010','111'}, ['.']={'000','000','000','000','010'}, ['/']={'001','001','010','100','100'}, [' ']={'000','000','000','000','000'}
}
local function text(img,str,x,y,color)
  for i=1,#str do
    local g=glyphs[str:sub(i,i)]
    if g then for gy=1,5 do for gx=1,3 do
      if g[gy]:sub(gx,gx)=='1' then pixel(img,x+(i-1)*4+gx-1,y+gy-1,color) end
    end end end
  end
end
local pw,ph=120,146
local preview=Sprite(pw,ph,ColorMode.RGB)
preview.layers[1].name='二维审稿用人偶与效果合成'
local seq={}
for f=1,8 do seq[#seq+1]={sprite=entry,index=f,duration=0.06} end
for cycle=1,2 do for f=1,20 do seq[#seq+1]={sprite=loop,index=f,duration=0.08} end end
for i,part in ipairs(seq) do
  local frame=i==1 and preview.frames[1] or preview:newEmptyFrame()
  frame.duration=part.duration
  local im=Image(pw,ph,ColorMode.RGB)
  rect(im,0,0,pw-1,ph-1,rgba(13,22,31,255))
  rect(im,4,18,pw-5,ph-5,rgba(24,36,49,255))
  text(im,'HEAT UP / V3.1',8,7,rgba(255,184,94,255))
  rect(im,23,132,96,132,rgba(46,62,75,255))
  local ox,oy=12,25
  for li=1,3 do im:drawImage(part.sprite.layers[li]:cel(part.index).image,Point(ox,oy)) end
  im:drawImage(mannequin,Point(ox,oy))
  for li=4,7 do im:drawImage(part.sprite.layers[li]:cel(part.index).image,Point(ox,oy)) end
  local cel=preview.layers[1]:cel(frame)
  if cel then cel.image=im else preview:newCel(preview.layers[1],frame,im,Point(0,0)) end
end
preview:saveAs(out..'/heat_up_v3_1_preview.aseprite')
app.sprite=preview
app.command.SpriteSize{ui=false,width=pw*4,height=ph*4,method='nearest'}
local still=Image(preview.width,preview.height,ColorMode.RGB)
still:drawSprite(preview,15)
still:saveAs(out..'/heat_up_v3_1_preview.png')
preview:saveAs(out..'/heat_up_v3_1_preview.gif')
return {loop=out..'/heat_up_v3_1.aseprite',entry=out..'/heat_up_v3_1_entry.aseprite',
 preview=out..'/heat_up_v3_1_preview.gif',still=out..'/heat_up_v3_1_preview.png',
 size={W,H},loopFrames=20,loopMs=80,entryFrames=8,entryMs=60,
 design='Hands base y68-69, tips y50-57. Legs base y104, tips y73-84. Mannequin hands y59-67, knees y86.'}
