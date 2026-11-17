local root = 'D:/alianximod/docs/资源库/过程归档/引燃_贴身分段燃烧_v2_20261116'
for _,dir in ipairs({'source','textures','previews','blockbench','evidence'}) do app.fs.makeAllDirectories(root..'/'..dir) end
local pc = app.pixelColor
local function rgb(r,g,b) return pc.rgba(r,g,b,255) end
local pal = {rgb(158,36,23),rgb(221,58,19),rgb(255,100,22),rgb(255,163,40),rgb(255,218,130),rgb(255,244,226),rgb(255,255,255)}
local function px(im,x,y,c) if x>=0 and x<im.width and y>=0 and y<im.height then im:drawPixel(math.floor(x),math.floor(y),c) end end
local function rect(im,x,y,w,h,c) for yy=y,y+h-1 do for xx=x,x+w-1 do px(im,xx,yy,c) end end end
local function tongue(im,cx,base,w,h,phase,seed,pulse)
  for j=0,h-1 do
    local t=j/h
    local sway=math.floor((math.sin(t*6.4+phase*.62+seed)*1.6+math.sin(t*12+seed)*.7)*t)
    local center=cx+sway
    local width=math.max(1,math.floor(w*(1-t)^.68))
    for dx=-width,width do
      local edge=math.abs(dx)/math.max(1,width)
      local idx=2
      if edge<.78 then idx=3 end
      if edge<.5 and t<.77 then idx=4 end
      if edge<.25 and t<.55 then idx=5 end
      if pulse>0 and edge<.63 and t<.72 then idx=math.min(7,idx+pulse) end
      if j<2 and edge>.8 then idx=1 end
      px(im,center+dx,base-j,pal[idx])
    end
  end
end
local source=Sprite(48,64,ColorMode.RGB)
source.layers[1].name='01_persistent_foot_flames'
local layers={source.layers[1]}
for _,name in ipairs({'02_mid_body_tongues','03_high_flank_tips','04_rising_sparks','05_segment_hot_cores'}) do local l=source:newLayer(); l.name=name; layers[#layers+1]=l end
source.data='fire_kindle v2; candidate_pending_user_confirmation; 48x64; 50ms/frame; sustain 1-10, pulse 11-13, extinguish 14-19. Target-attached CAMERA_PLANE. Source and previews only, not runtime integration.'
local flats={}
local footprints={}
for f=1,19 do
  if f>1 then source:newEmptyFrame(f) end
  source.frames[f].duration=.05
  local phase=(f-1)%10
  local pulse=f>=11 and f<=13 and ({2,2,1})[f-10] or 0
  local fade=f>=14 and ({.78,.56,.36,.20,.09,0})[f-13] or 1
  local ims={}; for i=1,5 do ims[i]=Image(48,64,ColorMode.RGB) end
  if fade>0 then
    local bases={{15,61,4,14,1},{22,61,3,10,3},{29,61,4,18,6},{35,60,3,12,5}}
    for _,v in ipairs(bases) do
      local h=math.max(1,math.floor((v[4]+math.sin(phase*.628+v[5])*2)*fade))
      tongue(ims[1],v[1],v[2],math.max(1,math.floor(v[3]*math.sqrt(fade))),h,phase,v[5],pulse)
    end
    if fade>.2 then
      for _,v in ipairs({{12,50,4,18,2},{36,48,4,21,7},{17,43,3,14,5},{32,37,3,15,3}}) do
        local h=math.floor((v[4]+math.sin(phase*.628+v[5])*3)*fade)
        tongue(ims[2],v[1],v[2],math.max(1,math.floor(v[3]*fade)),h,phase,v[5],pulse)
      end
    end
    if fade>.5 then
      tongue(ims[3],10,35,3,math.floor((16+math.sin(phase*.628)*3)*fade),phase,4,pulse)
      tongue(ims[3],38,32,3,math.floor((20+math.sin(phase*.628+2)*3)*fade),phase,8,pulse)
    end
    for n=1,4 do
      local sy=math.floor(43-((phase*2+n*11)%34))
      local sx=({9,39,13,35})[n]+math.floor(math.sin(phase*.5+n))
      if f<14 or n<=2 then
        rect(ims[4],sx,sy,1,2,pal[pulse>0 and 7 or 5])
        if (phase+n)%3==0 then px(ims[4],sx,sy+2,pal[3]) end
      end
    end
    if pulse>0 then
      for _,v in ipairs({{15,58,1,6,1},{29,57,1,8,4},{12,47,1,8,2},{36,44,1,8,5}}) do tongue(ims[5],v[1],v[2],v[3],v[4],phase,v[5],2) end
    end
  end
  local flat=Image(48,64,ColorMode.RGB)
  for i=1,5 do source:newCel(layers[i],f,ims[i],Point(0,0)); flat:drawImage(ims[i]) end
  flats[f]=flat
  local count=0; for p in flat:pixels() do if pc.rgbaA(p())>0 then count=count+1 end end
  footprints[f]=count
end
for _,v in ipairs({{'sustain_loop',1,10},{'segment_pulse',11,13},{'extinguish_once',14,19}}) do local t=source:newTag(v[2],v[3]);t.name=v[1] end
source:saveAs(root..'/source/kindle_v2.aseprite')
local strip=Image(48,64*19,ColorMode.RGB)
local neutral=Image(48,64*19,ColorMode.RGB)
for f,im in ipairs(flats) do
  strip:drawImage(im,Point(0,(f-1)*64))
  local grey=Image(im)
  for p in grey:pixels() do local c=p(); if pc.rgbaA(c)>0 then local lum=math.floor(.2126*pc.rgbaR(c)+.7152*pc.rgbaG(c)+.0722*pc.rgbaB(c)+.5);p(rgb(lum,lum,lum)) end end
  neutral:drawImage(grey,Point(0,(f-1)*64))
end
strip:saveAs(root..'/textures/kindle_v2_strip.png')
neutral:saveAs(root..'/textures/kindle_v2_luminance_strip.png')
for _,f in ipairs({1,6,11,13,14,16,18,19}) do flats[f]:saveAs(root..'/textures/kindle_v2_frame_'..string.format('%02d',f)..'.png') end
local function mannequin(im,ox,oy)
  local outline=rgb(29,40,54);local shade=rgb(60,76,91);local body=rgb(96,119,134);local light=rgb(137,157,165);local face=rgb(182,167,143)
  rect(im,ox+18,oy,12,14,outline);rect(im,ox+19,oy+1,10,12,face);rect(im,ox+19,oy+1,10,3,shade)
  px(im,ox+21,oy+6,outline);px(im,ox+26,oy+6,outline);rect(im,ox+22,oy+10,4,1,shade)
  rect(im,ox+17,oy+14,14,23,outline);rect(im,ox+18,oy+15,12,20,body);rect(im,ox+18,oy+15,2,18,light)
  rect(im,ox+11,oy+14,6,24,outline);rect(im,ox+12,oy+15,4,19,body);rect(im,ox+12,oy+34,4,3,face)
  rect(im,ox+31,oy+14,6,24,outline);rect(im,ox+32,oy+15,4,19,shade);rect(im,ox+32,oy+34,4,3,face)
  rect(im,ox+17,oy+37,7,24,outline);rect(im,ox+18,oy+38,5,21,shade);rect(im,ox+24,oy+37,7,24,outline);rect(im,ox+25,oy+38,5,21,body)
  rect(im,ox+16,oy+60,8,3,outline);rect(im,ox+24,oy+60,8,3,outline)
end
local function base(w,h)
  local im=Image(w,h,ColorMode.RGB);im:clear(rgb(21,27,38));return im
end
local function composite(f,gloom)
  local im=base(80,86)
  for x=0,79,8 do rect(im,x,77,1,9,rgb(34,43,55)) end
  rect(im,0,77,80,1,rgb(61,69,81))
  rect(im,26,77,28,2,rgb(12,17,25))
  mannequin(im,16,14)
  local fx=flats[f]
  if gloom then
    fx=Image(fx)
    for p in fx:pixels() do local c=p();if pc.rgbaA(c)>0 then p(rgb(math.floor(pc.rgbaR(c)*90/255+.5),math.floor(pc.rgbaG(c)*160/255+.5),pc.rgbaB(c))) end end
  end
  im:drawImage(fx,Point(16,14))
  return im
end
local sequence={}; for cycle=1,3 do for tick=0,9 do sequence[#sequence+1]=(cycle>1 and tick<3) and 11+tick or 1+tick end end
for f=14,19 do sequence[#sequence+1]=f end
for i=1,10 do sequence[#sequence+1]=19 end
local font={A='010101111101101',B='110101110101110',C='011100100100011',D='110101101101110',E='111100110100111',F='111100110100100',G='011100101101011',H='101101111101101',I='111010010010111',K='101101110101101',L='100100100100111',M='101111111101101',N='101111111111101',O='010101101101010',P='110101110100100',R='110101110101101',S='011100010001110',T='111010010010010',U='101101101101111',V='101101101101010',W='101101111111101',X='101101010101101',Y='101101010010010',['0']='111101101101111',['1']='010110010010111',['2']='110001010100111',['3']='110001010001110',['4']='101101111001001',['5']='111100110001110',['6']='011100111101111',['7']='111001010010010',['8']='111101111101111',['9']='111101111001110',['.']='000000000000010',['-']='000000111000000'}
local function text(im,label,x,y,c)
  for i=1,#label do local ch=label:sub(i,i);local bits=font[ch];if bits then for k=1,15 do if bits:sub(k,k)=='1' then px(im,x+(i-1)*4+(k-1)%3,y+math.floor((k-1)/3),c) end end end end
end
local review=Sprite(160,100,ColorMode.RGB);review.layers[1].name='Normal and existing 90-160-255 tint on target'
for i,f in ipairs(sequence) do
  if i>1 then review:newEmptyFrame(i) end;review.frames[i].duration=.05
  local im=base(160,100);im:drawImage(composite(f),Point(0,0));im:drawImage(composite(f,true),Point(80,0))
  text(im,'NORMAL',28,89,rgb(255,218,130));text(im,'GLOOM',110,89,rgb(90,160,255))
  review:newCel(review.layers[1],i,im,Point(0,0))
end
review:saveAs(root..'/source/kindle_v2_target_review.aseprite')
local keys={1,6,11,13,14,16,18,19}
local board=base(80*4,108*2)
for i,f in ipairs(keys) do
  local x=((i-1)%4)*80;local y=math.floor((i-1)/4)*108
  board:drawImage(composite(f),Point(x,y))
  local accent=f<=10 and rgb(238,142,50) or f<=13 and rgb(255,236,156) or rgb(130,155,175)
  local labels={'BASE F01','BASE F06','PULSE F11','PULSE F13','END F14','END F16','END F18','OFF F19'}
  text(board,labels[i],x+20,y+91,accent)
  text(board,'50MS PER FRAME',x+12,y+101,rgb(105,123,141))
end
board:resize(960,648);board:saveAs(root..'/previews/kindle_v2_keyframes.png')
local hero=composite(11);hero:resize(480,516);hero:saveAs(root..'/previews/kindle_v2_target_pulse.png')
local baseHero=composite(6);baseHero:resize(480,516);baseHero:saveAs(root..'/previews/kindle_v2_target_sustain.png')
local compare=base(160,100);compare:drawImage(composite(11),Point(0,0));compare:drawImage(composite(11,true),Point(80,0));text(compare,'NORMAL',28,89,rgb(255,218,130));text(compare,'GLOOM',110,89,rgb(90,160,255));compare:resize(640,400);compare:saveAs(root..'/previews/kindle_v2_tint_comparison.png')
local aux=Image(8,8,ColorMode.RGB)
local cols={rgb(77,96,113),rgb(108,132,149),rgb(149,171,179),rgb(184,169,145),rgb(42,54,69),rgb(255,207,93),rgb(50,63,78),rgb(220,230,229)}
for y=0,7 do for x=0,7 do aux:drawPixel(x,y,cols[x+1]) end end
aux:saveAs(root..'/textures/aux_mannequin_palette.png')
return {source=root..'/source/kindle_v2.aseprite',frames=19,frame_ms=50,pixel_counts=footprints,status='candidate_pending_user_confirmation'}
