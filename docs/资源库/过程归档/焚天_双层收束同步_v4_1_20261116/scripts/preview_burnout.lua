local root = "D:/alianximod/docs/资源库/过程归档/焚天_双层收束同步_v4_1_20261116"
local archive = "D:/alianximod/docs/资源库/特效/火焰/焚天_双层叠加_v4"
for _, folder in ipairs({"previews", "textures", "source", "evidence", "blockbench"}) do app.fs.makeAllDirectories(root .. "/" .. folder) end
local background = app.open(archive .. "/background_fentian_v2.aseprite")
local foreground = app.open(archive .. "/foreground_fentian_v3.aseprite")
local sustainAlpha = {1,.94,.82,.66,.48,.30,.15,.05,0}
local emberAlpha = {0,.18,.36,.55,.72,.82,.65,.35,0}
local emberFrames = {41,42,43,45,46,47,49,51,52}
local function frameImage(source, frame)
  local image = Image(source.width, source.height, ColorMode.RGB)
  image:drawSprite(source, frame)
  return image
end
local function fade(source, coefficient)
  local image = Image(source)
  for pixel in image:pixels() do
    local rgba = pixel()
    pixel(app.pixelColor.rgba(app.pixelColor.rgbaR(rgba), app.pixelColor.rgbaG(rgba), app.pixelColor.rgbaB(rgba), math.floor(app.pixelColor.rgbaA(rgba) * coefficient + .5)))
  end
  return image
end
local held = frameImage(background, 40)
local tail = Sprite(128,144,ColorMode.RGB)
tail.layers[1].name = "01_held_F40_alpha_only_demo"
local emberLayer = tail:newLayer()
emberLayer.name = "02_original_embers_alpha_only_demo"
local strip = Image(128,1296,ColorMode.RGB)
local preview = Sprite(144,162,ColorMode.RGB)
preview.layers[1].name = "AUX_target_and_approved_samples_burnout_demo"
local reference = Image(144,162,ColorMode.RGB)
local function rectangle(left,top,right,bottom,color)
  for vertical=top,bottom-1 do
    for horizontal=left,right-1 do reference:drawPixel(horizontal,vertical,color) end
  end
end
rectangle(57,15,87,45,Color(167,180,184))
rectangle(62,45,82,90,Color(60,101,113))
rectangle(52,45,62,90,Color(60,101,113))
rectangle(82,45,92,90,Color(60,101,113))
rectangle(62,90,71,135,Color(49,61,75))
rectangle(73,90,82,135,Color(49,61,75))
rectangle(62,27,66,31,Color(30,37,46))
rectangle(78,27,82,31,Color(30,37,46))
local observations={}
for index=1,9 do
  if index>1 then tail:newEmptyFrame();preview:newEmptyFrame() end
  tail.frames[index].duration=.05;preview.frames[index].duration=.05
  local sustain=fade(held,sustainAlpha[index])
  local embers=fade(frameImage(background,emberFrames[index]),emberAlpha[index])
  tail:newCel(tail.layers[1],index,sustain)
  tail:newCel(emberLayer,index,embers)
  local merged=Image(128,144,ColorMode.RGB)
  merged:drawImage(sustain,Point(0,0))
  merged:drawImage(embers,Point(0,0))
  strip:drawImage(merged,Point(0,(index-1)*144))
  local alphaSum=0
  for pixel in merged:pixels() do alphaSum=alphaSum+app.pixelColor.rgbaA(pixel()) end
  local front=frameImage(foreground,index+10)
  local frontAlpha=0
  for pixel in front:pixels() do frontAlpha=frontAlpha+app.pixelColor.rgbaA(pixel()) end
  table.insert(observations,{tick=index-1,backgroundAlphaSum=alphaSum,foregroundAlphaSum=frontAlpha})
  local composite=Image(144,162,ColorMode.RGB)
  composite:clear(Color(32,39,45,255))
  merged:resize(144,162)
  composite:drawImage(merged,Point(0,0))
  composite:drawImage(reference,Point(0,0))
  front:resize(90,120)
  composite:drawImage(front,Point(27,15))
  preview:newCel(preview.layers[1],index,composite)
end
tail:saveAs(root .. "/source/background_burnout_alpha_preview.aseprite")
strip:saveAs(root .. "/textures/background_burnout_9tick_preview.png")
preview:saveAs(root .. "/source/dual_layer_burnout_preview.aseprite")
preview.cels[1].image:saveAs(root .. "/previews/burnout_native_t0.png")
app.sprite=preview
app.command.SpriteSize{ui=false,width=576,height=648,method="nearest"}
for _,index in ipairs({1,4,7,9}) do preview.cels[index].image:saveAs(root .. "/previews/burnout_t" .. (index-1) .. ".png") end
preview:saveCopyAs(root .. "/previews/burnout_9tick.gif")
return {status="candidate_pending_user_confirmation",backgroundFrames=#background.frames,foregroundFrames=#foreground.frames,observations=observations,previewOnly=true,frames=9,durationMs=450,originalsNotSaved=true}
