local root='D:/alianximod/docs/资源库/过程归档/引燃_全身燃烧重绘_v3_20261116'
for _,folder in ipairs({'source','textures','previews','blockbench','evidence'}) do app.fs.makeAllDirectories(root..'/'..folder) end
local pixel=app.pixelColor
local function rgba(red,green,blue) return pixel.rgba(red,green,blue,255) end
local palette={rgba(139,35,17),rgba(210,49,13),rgba(245,84,13),rgba(255,139,20),rgba(255,193,41),rgba(255,227,103),rgba(255,248,183)}
local function point(image,xx,yy,color)
  xx=math.floor(xx);yy=math.floor(yy)
  if xx>=0 and yy>=0 and xx<image.width and yy<image.height then image:drawPixel(xx,yy,color) end
end
local function rectangle(image,left,top,width,height,color)
  for yy=top,top+height-1 do for xx=left,left+width-1 do point(image,xx,yy,color) end end
end
local function polygon(image,vertices,color)
  for yy=0,image.height-1 do
    local crosses={}
    local prior=vertices[#vertices]
    for _,vertex in ipairs(vertices) do
      if (vertex[2]<=yy+.5 and prior[2]>yy+.5) or (prior[2]<=yy+.5 and vertex[2]>yy+.5) then
        crosses[#crosses+1]=vertex[1]+(yy+.5-vertex[2])*(prior[1]-vertex[1])/(prior[2]-vertex[2])
      end
      prior=vertex
    end
    table.sort(crosses)
    for index=1,#crosses-1,2 do for xx=math.ceil(crosses[index]-.5),math.floor(crosses[index+1]-.5) do point(image,xx,yy,color) end end
  end
end
local shapes={
  {
    {{1,18},{0,13},{2,13},{2,8},{5,8},{5,3},{3,3},{3,0},{7,0},{7,5},{10,5},{10,9},{13,9},{13,5},{12,5},{12,2},{15,2},{15,7},{17,7},{17,14},{15,14},{15,18},{11,18},{11,20},{4,20},{4,18}},
    {{3,17},{3,12},{5,12},{5,9},{7,9},{7,7},{9,7},{9,11},{13,11},{13,9},{15,9},{15,15},{12,15},{12,18},{6,18},{6,17}},
    {{5,16},{5,13},{8,13},{8,11},{10,11},{10,14},{13,14},{13,16},{10,16},{10,18},{7,18},{7,16}},
    {{7,15},{9,15},{9,17},{7,17}},
    {{10,5},{12,5},{12,8},{10,8}}
  },
  {
    {{0,17},{1,12},{3,12},{3,8},{1,8},{1,4},{4,4},{4,6},{7,6},{7,2},{6,2},{6,0},{10,0},{10,4},{13,4},{13,7},{16,7},{16,11},{18,11},{18,15},{15,15},{15,19},{10,19},{10,20},{4,20},{4,18}},
    {{3,16},{3,13},{5,13},{5,10},{8,10},{8,6},{10,6},{10,9},{13,9},{13,12},{16,12},{16,14},{12,14},{12,17},{9,17},{9,19},{5,19},{5,16}},
    {{6,16},{6,12},{9,12},{9,10},{11,10},{11,14},{13,14},{13,16},{10,16},{10,18},{7,18},{7,16}},
    {{8,14},{10,14},{10,17},{8,17}},
    {{3,7},{5,7},{5,10},{3,10}}
  },
  {
    {{1,18},{1,14},{0,14},{0,10},{3,10},{3,5},{5,5},{5,1},{9,1},{9,3},{7,3},{7,7},{11,7},{11,4},{14,4},{14,0},{16,0},{16,5},{18,5},{18,10},{16,10},{16,16},{13,16},{13,19},{9,19},{9,20},{4,20},{4,18}},
    {{3,17},{3,12},{5,12},{5,9},{8,9},{8,11},{11,11},{11,8},{14,8},{14,6},{16,6},{16,8},{14,8},{14,14},{11,14},{11,17},{8,17},{8,19},{5,19},{5,17}},
    {{5,16},{5,13},{8,13},{8,14},{10,14},{10,12},{12,12},{12,15},{10,15},{10,18},{7,18},{7,16}},
    {{7,15},{9,15},{9,18},{7,18}},
    {{6,5},{8,5},{8,8},{6,8}}
  }
}
local function puff(image,left,top,phase,scale,mirror,pulse)
  local shape=shapes[phase%3+1]
  local colors={3,5,6,7,0}
  for layer,vertices in ipairs(shape) do
    local transformed={}
    for _,vertex in ipairs(vertices) do
      local horizontal=mirror and 18-vertex[1] or vertex[1]
      transformed[#transformed+1]={left+math.floor(horizontal*scale),top+math.floor(vertex[2]*scale)}
    end
    local index=colors[layer]
    if index>0 then index=math.min(7,index+pulse);polygon(image,transformed,palette[index])
    else polygon(image,transformed,0) end
  end
  if phase%3==0 then rectangle(image,left+math.floor(3*scale),top+math.floor(16*scale),math.max(1,math.floor(3*scale)),math.max(1,math.floor(2*scale)),palette[2]) end
end
local art=Sprite(48,64,ColorMode.RGB)
art.layers[1].name='01_rolling_lower_mantle'
local layers={art.layers[1]}
for _,name in ipairs({'02_body_fire_volume','03_shoulder_breakup','04_detached_embers'}) do local layer=art:newLayer();layer.name=name;layers[#layers+1]=layer end
art.data='candidate_pending_user_confirmation | fire_kindle_v2_art | 20261116-art-kindle-revision-03 | new v3 pixels, not v2 thickening | 19 frames, 50ms | CAMERA_PLANE'
local frames={}
local counts={}
local sizes={.68,.82,1,1.05,1,.92,.8,.65,.48,.32}
local rises={0,0,1,2,4,6,8,11,14,17}
local seeds={
  {1,10,49,0,1.05,false},{1,21,48,4,1.03,true},{1,28,47,7,.86,false},
  {2,6,32,2,1.1,false},{2,27,29,6,1.08,true},{2,16,35,8,.92,false},
  {2,11,19,7,.92,true},{2,25,18,1,.91,false},
  {3,9,10,4,.72,false},{3,28,8,8,.78,true},{3,18,13,1,.6,false}
}
for frame=1,19 do
  if frame>1 then art:newEmptyFrame(frame) end
  art.frames[frame].duration=.05
  local phase=(frame-1)%10
  local pulse=frame>=11 and frame<=13 and ({1,1,0})[frame-10] or 0
  local ending=frame>=14
  local fade=ending and ({.88,.72,.52,.32,.16,0})[frame-13] or 1
  local images={}
  for layer=1,4 do images[layer]=Image(48,64,ColorMode.RGB) end
  if fade>0 then
    if not ending or frame<16 then
      local lift=phase%3
      local mantle={
        {{13,45},{13,36},{15,36},{15,27},{18,27},{18,20},{21,20},{21,25},{24,25},{24,31},{27,31},{27,23},{30,23},{30,29},{33,29},{33,37},{35,37},{35,46},{31,46},{31,50},{18,50},{18,47}},
        {{15,43},{15,36},{18,36},{18,31},{21,31},{21,28},{24,28},{24,35},{29,35},{29,30},{31,30},{31,40},{29,40},{29,47},{22,47},{22,44}},
        {{18,43},{18,38},{21,38},{21,34},{24,34},{24,39},{27,39},{27,44},{24,44},{24,47},{21,47},{21,43}}
      }
      for band,vertices in ipairs(mantle) do
        local moved={}
        for _,vertex in ipairs(vertices) do moved[#moved+1]={vertex[1],vertex[2]-lift+(ending and 6 or 0)} end
        polygon(images[2],moved,palette[math.min(7,({3,5,6})[band]+pulse)])
      end
      local cleft=phase%5
      polygon(images[2],{{22+cleft,28-lift},{25+cleft,28-lift},{25+cleft,32-lift},{23+cleft,32-lift},{23+cleft,36-lift},{21+cleft,36-lift},{21+cleft,32-lift},{22+cleft,32-lift}},0)
      rectangle(images[2],17+phase%3,42-lift,3,4,0)
      rectangle(images[2],29-phase%3,38-lift,2,3,0)
      rectangle(images[2],18,47-lift,5,2,palette[2])
    end
    for index,seed in ipairs(seeds) do
      local age=(phase+seed[4])%10
      local growth=sizes[age+1]*seed[5]
      local left=seed[2]+math.floor((18-18*growth)/2)
      local top=seed[3]-rises[age+1]
      if ending then
        growth=growth*fade
        left=seed[2]+math.floor((18-18*growth)/2)
        top=seed[3]+math.floor((1-fade)*14)-rises[age+1]
      end
      if not ending or seed[1]==1 or frame<16 or (index%3==0 and frame<18) then
        puff(images[seed[1]],left,top,phase+index,growth,seed[6],pulse)
      end
    end
    if not ending then
      puff(images[1],14,48,phase,.75,false,pulse)
      puff(images[1],25,47,phase+1,.74,true,pulse)
    end
    for index=1,5 do
      local age=(phase+index*3)%10
      local horizontal=({8,38,15,33,23})[index]
      local vertical=({22,29,13,17,8})[index]-age
      if age<7 and (not ending or frame<16) then
        rectangle(images[4],horizontal,vertical,age<4 and 2 or 1,age<3 and 3 or 2,palette[age<4 and 5 or 3])
      end
    end
  end
  local flat=Image(48,64,ColorMode.RGB)
  for layer=1,4 do art:newCel(layers[layer],frame,images[layer],Point(0,0));flat:drawImage(images[layer]) end
  frames[frame]=flat
  local count=0;for iterator in flat:pixels() do if pixel.rgbaA(iterator())>0 then count=count+1 end end
  counts[frame]=count
end
for _,tag in ipairs({{'sustain_loop',1,10},{'segment_pulse',11,13},{'extinguish_once',14,19}}) do local created=art:newTag(tag[2],tag[3]);created.name=tag[1] end
art:saveAs(root..'/source/kindle_v3.aseprite')
local strip=Image(48,1216,ColorMode.RGB)
for frame,image in ipairs(frames) do strip:drawImage(image,Point(0,(frame-1)*64)) end
strip:saveAs(root..'/textures/kindle_v3_strip.png')
for _,frame in ipairs({1,6,11,16,19}) do frames[frame]:saveAs(root..'/textures/kindle_v3_F'..string.format('%02d',frame)..'.png') end
local function mannequin(image,left,top)
  local outline=rgba(27,35,43);local shirt=rgba(53,113,119);local shirtLight=rgba(73,143,143);local skin=rgba(184,149,117);local pants=rgba(65,72,103)
  rectangle(image,left+18,top,12,14,outline);rectangle(image,left+19,top+1,10,12,skin)
  rectangle(image,left+18,top,12,4,rgba(65,47,37));rectangle(image,left+21,top+6,2,2,outline);rectangle(image,left+26,top+6,2,2,outline)
  rectangle(image,left+22,top+10,4,1,rgba(104,72,55))
  rectangle(image,left+17,top+14,14,23,outline);rectangle(image,left+18,top+15,12,21,shirt);rectangle(image,left+18,top+15,3,20,shirtLight)
  rectangle(image,left+11,top+14,6,24,outline);rectangle(image,left+12,top+15,4,19,shirt);rectangle(image,left+12,top+34,4,3,skin)
  rectangle(image,left+31,top+14,6,24,outline);rectangle(image,left+32,top+15,4,19,shirt);rectangle(image,left+32,top+34,4,3,skin)
  rectangle(image,left+17,top+37,7,26,outline);rectangle(image,left+18,top+38,5,23,pants)
  rectangle(image,left+24,top+37,7,26,outline);rectangle(image,left+25,top+38,5,23,pants)
  rectangle(image,left+16,top+61,8,3,outline);rectangle(image,left+24,top+61,8,3,outline)
end
local function background(width,height)
  local image=Image(width,height,ColorMode.RGB);image:clear(rgba(31,38,47));return image
end
local function composite(frame,gloom)
  local image=background(80,86)
  rectangle(image,0,78,80,8,rgba(38,47,55));rectangle(image,24,78,32,2,rgba(18,23,29))
  mannequin(image,16,14)
  local fire=frames[frame]
  if gloom then
    fire=Image(fire)
    for iterator in fire:pixels() do local color=iterator();if pixel.rgbaA(color)>0 then iterator(rgba(math.floor(pixel.rgbaR(color)*90/255+.5),math.floor(pixel.rgbaG(color)*160/255+.5),pixel.rgbaB(color))) end end
  end
  image:drawImage(fire,Point(16,14))
  return image
end
local sequence={}
for cycle=1,3 do for tick=0,9 do sequence[#sequence+1]=(cycle>1 and tick<3) and tick+11 or tick+1 end end
for frame=14,19 do sequence[#sequence+1]=frame end
for count=1,10 do sequence[#sequence+1]=19 end
local review=Sprite(160,96,ColorMode.RGB);review.layers[1].name='AUX_target_normal_left_existing_gloom_right'
for index,frame in ipairs(sequence) do
  if index>1 then review:newEmptyFrame(index) end
  review.frames[index].duration=.05
  local image=background(160,96)
  image:drawImage(composite(frame,false),Point(0,0));image:drawImage(composite(frame,true),Point(80,0))
  rectangle(image,26,89,28,2,palette[5]);rectangle(image,106,89,28,2,rgba(90,160,255))
  review:newCel(review.layers[1],index,image,Point(0,0))
end
review:saveAs(root..'/source/kindle_v3_target_review.aseprite')
review:saveAs(root..'/previews/kindle_v3_target_native.gif')
local native=background(160,96)
native:drawImage(composite(6),Point(0,0));native:drawImage(composite(6,true),Point(80,0))
native:saveAs(root..'/previews/kindle_v3_reading_scale.png')
app.command.SpriteSize{ui=false,width=640,height=384,method='nearest'}
review:saveAs(root..'/previews/kindle_v3_target_demo.gif')
local board=background(320,96)
for index,frame in ipairs({1,6,11,16}) do
  board:drawImage(composite(frame),Point((index-1)*80,0))
  rectangle(board,(index-1)*80+24,90,index*8,2,index==4 and rgba(115,134,148) or palette[5])
end
board:saveAs(root..'/previews/kindle_v3_keyframes_native.png')
board:resize(960,288);board:saveAs(root..'/previews/kindle_v3_keyframes.png')
local hero=composite(11);hero:resize(480,516);hero:saveAs(root..'/previews/kindle_v3_target_pulse.png')
local auxiliary=Image(8,8,ColorMode.RGB)
local colors={rgba(65,72,103),rgba(53,113,119),rgba(73,143,143),rgba(184,149,117),rgba(42,35,32),rgba(255,193,41),rgba(50,60,67),rgba(220,230,229)}
for yy=0,7 do for xx=0,7 do auxiliary:drawPixel(xx,yy,colors[xx+1]) end end
auxiliary:saveAs(root..'/textures/aux_mannequin_palette.png')
app.sprite=art
return {status='candidate_pending_user_confirmation',source=art.filename,frames=19,frame_ms=50,pixel_counts=counts,review_frames=#sequence}
