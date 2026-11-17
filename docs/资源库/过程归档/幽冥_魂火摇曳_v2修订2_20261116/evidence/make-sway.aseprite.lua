local destination = 'D:/alianximod/docs/资源库/过程归档/幽冥_魂火摇曳_v2修订2_20261116'
local sourcePath = 'D:/alianximod/docs/资源库/过程归档/幽冥_魂谷天空危险魂火_v2修订_20261116/youming_v2_soulvalley_palette.aseprite'
local sourceSprite = app.open(sourcePath)
local sourceFrames = {}
for frameIndex, sourceFrame in ipairs(sourceSprite.frames) do
  local flattened = Image(64, 64, ColorMode.RGB)
  flattened:drawSprite(sourceSprite, sourceFrame, Point(0, 0))
  sourceFrames[frameIndex] = flattened
end
local result = Sprite(64, 64, ColorMode.RGB)
local rootLayer = result.layers[1]
rootLayer.name = 'ROOT_FIXED_rows44to63'
local tongueLayers = {}
for tongueIndex = 1, 4 do
  local layer = result:newLayer()
  layer.name = 'TONGUE_' .. tongueIndex .. '_local_sway_and_fragment'
  tongueLayers[tongueIndex] = layer
end
local offsets = {0, 5, 9, 13}
local phases = {0.2, 2.1, 4.4, 5.5}
local rootImage = Image(64, 64, ColorMode.RGB)
for pixelY = 44, 63 do
  for pixelX = 0, 63 do
    rootImage:drawPixel(pixelX, pixelY, sourceFrames[1]:getPixel(pixelX, pixelY))
  end
end
app.transaction(function()
  for frameIndex = 1, 16 do
    if frameIndex > 1 then result:newFrame() end
    result.frames[frameIndex].duration = 0.1
    local existingRoot = rootLayer:cel(frameIndex)
    if existingRoot then result:deleteCel(existingRoot) end
    result:newCel(rootLayer, frameIndex, rootImage, Point(0, 0))
    local angle = (frameIndex - 1) * math.pi * 2 / 16
    for tongueIndex = 1, 4 do
      local layer = tongueLayers[tongueIndex]
      local existingCel = layer:cel(frameIndex)
      if existingCel then result:deleteCel(existingCel) end
      local image = Image(64, 64, ColorMode.RGB)
      local localFrame = sourceFrames[((frameIndex - 1 + offsets[tongueIndex]) % 16) + 1]
      local leftPixel = (tongueIndex - 1) * 16
      for pixelY = 0, 43 do
        local heightWeight = math.max(0, (44 - pixelY) / 44)
        local sway = (3 * math.sin(angle + phases[tongueIndex]) + math.sin(2 * angle + phases[tongueIndex] - pixelY * 0.08)) * heightWeight ^ 1.1
        local shift = math.floor(sway / 2 + 0.5) * 2
        for pixelX = leftPixel, leftPixel + 15 do
          local value = localFrame:getPixel(pixelX, pixelY)
          local targetX = pixelX + shift
          if app.pixelColor.rgbaA(value) > 0 and targetX >= 6 and targetX <= 59 then
            image:drawPixel(targetX, pixelY, value)
          end
        end
      end
      result:newCel(layer, frameIndex, image, Point(0, 0))
    end
  end
end)
result:saveAs(destination .. '/youming_v2_sway_fixed_root.aseprite')
local sheet = Image(64, 1024, ColorMode.RGB)
for frameIndex, frame in ipairs(result.frames) do
  sheet:drawSprite(result, frame, Point(0, (frameIndex - 1) * 64))
end
sheet:saveAs(destination .. '/textures/youming_v2_sway_strip.png')
result:saveCopyAs(destination .. '/sway-2d-native.gif')
local proof = io.open(destination .. '/evidence/aseprite-native-save.txt', 'w')
proof:write('Aseprite=' .. tostring(app.version) .. ' ui=' .. tostring(app.isUIAvailable) .. '\n')
proof:write('source=' .. sourcePath .. '\noutput=' .. result.filename .. '\n')
proof:write('64x64, 16 frames, 100ms; five editable layers. Fixed root rows44..63 copied exactly from prior frame0.\n')
proof:write('Four local tongue phase offsets 0/5/9/13 frames; nearest 2px horizontal offsets, max4px, zero root displacement; x clipped to existing visible envelope6..59.\n')
proof:write('Existing v2 palette and local flame fragments reused. No whole-cluster resize or rigid pivot.\n')
proof:close()
return {file=result.filename, frames=#result.frames, layers=#result.layers, ui=app.isUIAvailable}
