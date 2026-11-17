local source, input = ...
local output = input.output
local projectileSource = app.open(input.projectile)
assert(source.width == 64 and source.height == 64 and #source.frames == 16)
assert(projectileSource.width == 64 and projectileSource.height == 64)
local pixel = app.pixelColor
local width, height = 384, 112
local centerY, groundY = 43, 76
local launchTick, lastEmissionTick, totalTicks = 8, 56, 104
local startX, speed, sampleStep, life = 32, 8, 4, 40
local stamps, projectileFrames, agedStamps = {}, {}, {}
local function renderFrame(spriteValue, index)
  local image = Image(spriteValue.width, spriteValue.height, ColorMode.RGB)
  image:drawSprite(spriteValue, index, Point(0, 0))
  return image
end
local function nearest(image, scale)
  local result = Image(image.width * scale, image.height * scale, ColorMode.RGB)
  for vertical = 0, result.height - 1 do
    for horizontal = 0, result.width - 1 do
      result:putPixel(horizontal, vertical, image:getPixel(math.floor(horizontal / scale), math.floor(vertical / scale)))
    end
  end
  return result
end
local function setCel(spriteValue, layer, frameIndex, image)
  local cel = layer:cel(frameIndex)
  if cel then
    cel.image = image
  else
    spriteValue:newCel(layer, frameIndex, image, Point(0, 0))
  end
end
local function saveImage(image, filename)
  local document = Sprite(image.width, image.height, ColorMode.RGB)
  document.cels[1].image = image
  document:saveAs(filename)
  document:close()
end
for frameIndex = 1, 16 do
  local transformed = Image(32, 32, ColorMode.RGB)
  local phase = (frameIndex - 1) * math.pi / 8
  for vertical = 0, 31 do
    for horizontal = 0, 31 do
      local envelope = math.sin(math.pi * (horizontal + 1) / 33) ^ 0.62
      local center = 16 + 0.9 * math.sin(horizontal * 0.29 + phase)
      local upper = envelope * (6.0 + 1.7 * math.sin(horizontal * 0.65 - phase) + 1.1 * math.cos(horizontal * 1.25 + phase))
      local lower = envelope * (4.5 + 0.8 * math.sin(horizontal * 0.44 + phase))
      local distance = vertical < center and (center - vertical) / math.max(0.1, upper) or (vertical - center) / math.max(0.1, lower)
      if distance < 1 then
        local color
        if distance < 0.18 then color = pixel.rgba(255, 225, 103, 250)
        elseif distance < 0.38 then color = pixel.rgba(255, 171, 37, 245)
        elseif distance < 0.67 then color = pixel.rgba(255, 100, 13, 235)
        elseif distance < 0.86 then color = pixel.rgba(226, 56, 12, 200)
        else color = pixel.rgba(143, 39, 24, 130) end
        transformed:putPixel(horizontal, vertical, color)
      end
    end
  end
  stamps[frameIndex] = transformed
  local projectileRaw = renderFrame(projectileSource, frameIndex)
  local projectile = Image(32, 32, ColorMode.RGB)
  for vertical = 0, 31 do
    for horizontal = 0, 31 do
      projectile:putPixel(horizontal, vertical, projectileRaw:getPixel(horizontal * 2, vertical * 2))
    end
  end
  projectileFrames[frameIndex] = projectile
end
for halfAge = 0, 79 do
  local age = halfAge / 2
  local frameIndex = math.min(16, math.floor(age * 16 / life) + 1)
  local faded = Image(32, 32, ColorMode.RGB)
  local remaining = 1 - age / life
  local opacity = 0.44 * remaining ^ 1.6
  local verticalScale = 0.35 + 0.65 * remaining
  for vertical = 0, 31 do
    for horizontal = 0, 31 do
      local sampleY = math.floor((vertical - 16) / verticalScale + 16)
      if sampleY >= 0 and sampleY < 32 then
        local color = stamps[frameIndex]:getPixel(horizontal, sampleY)
        local grain = (horizontal * 19 + vertical * 31 + frameIndex * 13) % 29
        local erosion = math.max(0, age - 22) / 18
        if grain / 29 >= erosion * 0.7 then
          faded:putPixel(horizontal, vertical, pixel.rgba(pixel.rgbaR(color), math.floor(pixel.rgbaG(color) * (0.6 + 0.4 * remaining)), math.floor(pixel.rgbaB(color) * (0.5 + 0.5 * remaining)), math.floor(pixel.rgbaA(color) * opacity + 0.5)))
        end
      end
    end
  end
  agedStamps[halfAge] = faded
  saveImage(faded, output .. string.format("/trail-age-%02d.png", halfAge))
end
local textureStrip = Image(32, 512, ColorMode.RGB)
local projectileStrip = Image(32, 512, ColorMode.RGB)
local stampDocument = Sprite(32, 32, ColorMode.RGB)
stampDocument.layers[1].name = "v2新候选_水平火舌_16帧"
for frameIndex = 1, 16 do
  textureStrip:drawImage(stamps[frameIndex], Point(0, (frameIndex - 1) * 32))
  projectileStrip:drawImage(projectileFrames[frameIndex], Point(0, (frameIndex - 1) * 32))
  saveImage(projectileFrames[frameIndex], output .. string.format("/projectile-frame-%02d.png", frameIndex))
  if frameIndex > 1 then stampDocument:newFrame() end
  setCel(stampDocument, stampDocument.layers[1], frameIndex, stamps[frameIndex])
  stampDocument.frames[frameIndex].duration = 0.08
end
stampDocument:saveAs(output .. "/trail-stamp-v2.aseprite")
saveImage(textureStrip, output .. "/trail-stamp-v2-strip.png")
saveImage(projectileStrip, output .. "/projectile-v3-nn-strip.png")
local background = Image(width, height, ColorMode.RGB)
for vertical = 0, height - 1 do
  for horizontal = 0, width - 1 do
    local color
    if vertical < groundY then
      local band = math.floor(vertical / 16)
      color = pixel.rgba(18 + band, 27 + band, 38 + band, 255)
    elseif vertical == groundY then
      color = pixel.rgba(63, 77, 87, 255)
    else
      local tile = math.floor(horizontal / 64)
      local shade = tile % 2
      color = pixel.rgba(31 + shade * 2, 43 + shade * 2, 52 + shade * 2, 255)
      if horizontal % 64 == 0 or vertical == 94 then color = pixel.rgba(23, 33, 43, 255) end
      if vertical == 78 then color = pixel.rgba(45, 59, 69, 255) end
    end
    background:putPixel(horizontal, vertical, color)
  end
end
local scene = Sprite(width, height, ColorMode.RGB)
scene.layers[1].name = "示意辅助_固定地面背景_64px每格"
local backgroundLayer = scene.layers[1]
local trailLayer = scene:newLayer()
trailLayer.name = "留焰_固定路径采样_40tick衰减"
local projectileLayer = scene:newLayer()
projectileLayer.name = "火苗v3_移动子弹"
local samples = {}
local intervals = {3, 5, 4, 3, 4, 5}
local offset, index = 0, 0
while offset <= (lastEmissionTick - launchTick) * speed do
  samples[#samples + 1] = {x = startX + offset, birth = launchTick + offset / speed}
  offset = offset + intervals[index % #intervals + 1]
  index = index + 1
end
for tick = 0, totalTicks - 1 do
  local frameIndex = tick + 1
  if frameIndex > 1 then scene:newFrame() end
  setCel(scene, backgroundLayer, frameIndex, background)
  local trail = Image(width, height, ColorMode.RGB)
  for _, sample in ipairs(samples) do
    local age = tick - sample.birth
    if age >= 0 and age < life then
      trail:drawImage(agedStamps[math.floor(age * 2)], Point(sample.x - 16, centerY - 16))
    end
  end
  setCel(scene, trailLayer, frameIndex, trail)
  local projectile = Image(width, height, ColorMode.RGB)
  if tick <= lastEmissionTick then
    local currentX = startX + math.max(0, tick - launchTick) * speed
    local image = projectileFrames[(tick % 16) + 1]
    if tick < launchTick then
      local extent = math.max(2, math.floor(32 * (tick + 1) / launchTick))
      local charge = Image(extent, extent, ColorMode.RGB)
      for vertical = 0, extent - 1 do
        for horizontal = 0, extent - 1 do
          charge:putPixel(horizontal, vertical, image:getPixel(math.min(31, math.floor(horizontal * 32 / extent)), math.min(31, math.floor(vertical * 32 / extent))))
        end
      end
      projectile:drawImage(charge, Point(currentX - math.floor(extent / 2), centerY - math.floor(extent / 2)))
    else
      projectile:drawImage(image, Point(currentX - 16, centerY - 16))
    end
  end
  setCel(scene, projectileLayer, frameIndex, projectile)
  scene.frames[frameIndex].duration = 0.05
end
scene:saveAs(output .. "/flame-trail-v2-composite.aseprite")
for _, tick in ipairs({4, 20, 40, 52, 68, 84, 96}) do
  local frame = renderFrame(scene, tick + 1)
  saveImage(nearest(frame, 3), output .. string.format("/composite-t%02d.png", tick))
end
return {version = app.version, backend = "batch", ui = app.isUIAvailable, width = width, height = height, frames = totalTicks, frameMs = 50, samples = #samples, sampleStepPixels = intervals, pixelsPerBlock = 64, footprintPixels = 32, lifeTicks = life, launchTick = launchTick, lastEmissionTick = lastEmissionTick, speedPixelsPerTick = speed, sourceUnmodified = true, trailTexture = "new v2 horizontal flame candidate", projectileTexture = "existing fire_spark_v3 nearest 2:1", scene = output .. "/flame-trail-v2-composite.aseprite"}
