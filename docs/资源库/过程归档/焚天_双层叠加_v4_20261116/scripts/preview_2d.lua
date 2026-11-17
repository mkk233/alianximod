local root = "D:/alianximod/docs/资源库/过程归档/焚天_双层叠加_v4_20261116"
local archive = "D:/alianximod/docs/资源库/特效/火焰/焚天_双层叠加_v4"
app.fs.makeAllDirectories(root .. "/previews")
local background = app.open(archive .. "/background_fentian_v2.aseprite")
local foreground = app.open(archive .. "/foreground_fentian_v3.aseprite")
local result = Sprite(144, 162, ColorMode.RGB)
result.layers[1].name = "approved_background_then_AUX_target_then_foreground"
local reference = Image(144, 162, ColorMode.RGB)
local function rectangle(left, top, right, bottom, color)
  for vertical = top, bottom - 1 do
    for horizontal = left, right - 1 do
      reference:drawPixel(horizontal, vertical, color)
    end
  end
end
rectangle(57, 15, 87, 45, Color(167, 180, 184))
rectangle(62, 45, 82, 90, Color(60, 101, 113))
rectangle(52, 45, 62, 90, Color(60, 101, 113))
rectangle(82, 45, 92, 90, Color(60, 101, 113))
rectangle(62, 90, 71, 135, Color(49, 61, 75))
rectangle(73, 90, 82, 135, Color(49, 61, 75))
rectangle(62, 27, 66, 31, Color(30, 37, 46))
rectangle(78, 27, 82, 31, Color(30, 37, 46))
local function frameImage(source, number, width, height)
  local image = Image(source.width, source.height, ColorMode.RGB)
  image:drawSprite(source, number)
  image:resize(width, height)
  return image
end
for index = 0, 51 do
  if index > 0 then result:newEmptyFrame() end
  result.frames[index + 1].duration = 0.05
  local image = Image(144, 162, ColorMode.RGB)
  local backdrop = frameImage(background, index + 1, 144, 162)
  image:drawImage(backdrop, Point(0, 0))
  image:drawImage(reference, Point(0, 0))
  local frontIndex
  if index < 40 then
    local phase = index % 4
    frontIndex = phase < 3 and (10 + phase) or (index % 10)
  elseif index < 49 then
    frontIndex = 10 + index - 40
  end
  if frontIndex then image:drawImage(frameImage(foreground, frontIndex + 1, 90, 120), Point(27, 15)) end
  result:newCel(result.layers[1], index + 1, image)
end
result:saveAs(root .. "/previews/dual_layer_2d_preview.aseprite")
result.cels[4].image:saveAs(root .. "/previews/dual_layer_2d_native.png")
app.sprite = result
app.command.SpriteSize { ui=false, width=576, height=648, method="nearest" }
result.cels[4].image:saveAs(root .. "/previews/dual_layer_2d_nearest.png")
result.cels[46].image:saveAs(root .. "/previews/dual_layer_2d_ending.png")
result:saveCopyAs(root .. "/previews/dual_layer_demo.gif")
return {source_background_frames=#background.frames, source_foreground_frames=#foreground.frames, preview_frames=#result.frames, frame_ms=50, native_size={144,162}, enlarged_size={576,648}, demo_only=true, tail="background12ticks; foreground3pulse+6end=9ticks", output=root .. "/previews"}
