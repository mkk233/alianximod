local root = "D:/alianximod/docs/资源库/过程归档/生存资源条-20261110"

local function rgba(r, g, b, a)
  return app.pixelColor.rgba(r, g, b, a or 255)
end

local function newImage(w, h)
  local img = Image(w, h, ColorMode.RGB)
  local clear = rgba(0, 0, 0, 0)
  for y = 0, h - 1 do
    for x = 0, w - 1 do
      img:drawPixel(x, y, clear)
    end
  end
  return img
end

local function alpha(c)
  return app.pixelColor.rgbaA(c)
end

local function blit(dst, src, dx, dy, sw, sh)
  sw = sw or src.width
  sh = sh or src.height
  for y = 0, sh - 1 do
    for x = 0, sw - 1 do
      local c = src:getPixel(x, y)
      if alpha(c) > 0 then
        dst:drawPixel(dx + x, dy + y, c)
      end
    end
  end
end

local function stamp(img, ox, oy, rows, colors)
  for y, row in ipairs(rows) do
    for x = 1, #row do
      local color = colors[row:sub(x, x)]
      if color then
        img:drawPixel(ox + x - 1, oy + y - 1, color)
      end
    end
  end
end

local function groove(img, x, y, w, h, border, shadow, lit, bed)
  for yy = 0, h - 1 do
    for xx = 0, w - 1 do
      local c = bed
      if xx == 0 or yy == 0 or xx == w - 1 or yy == h - 1 then
        c = border
      elseif yy == 1 or xx == 1 then
        c = shadow
      elseif yy == h - 2 or xx == w - 2 then
        c = lit
      end
      img:drawPixel(x + xx, y + yy, c)
    end
  end
end

local function liquid(w, h, phase, deep, mid, hi, core)
  local img = newImage(w, h)
  for y = 0, h - 1 do
    for x = 0, w - 1 do
      local c = mid
      if y == 0 then
        c = hi
      elseif y >= h - 2 then
        c = deep
      end
      local n = (x + phase) % 8
      if y == 0 and (n == 1 or n == 2) then
        c = core
      elseif y == 1 and n == 5 then
        c = hi
      elseif y == 2 and n == 3 then
        c = deep
      end
      img:drawPixel(x, y, c)
    end
  end
  return img
end

local GLYPH = {
  ["0"] = {"###", "#.#", "#.#", "#.#", "###"},
  ["1"] = {".#.", "##.", ".#.", ".#.", "###"},
  ["2"] = {"###", "..#", "###", "#..", "###"},
  ["3"] = {"###", "..#", "###", "..#", "###"},
  ["4"] = {"#.#", "#.#", "###", "..#", "..#"},
  ["5"] = {"###", "#..", "###", "..#", "###"},
  ["6"] = {"###", "#..", "###", "#.#", "###"},
  ["7"] = {"###", "..#", ".#.", ".#.", ".#."},
  ["8"] = {"###", "#.#", "###", "#.#", "###"},
  ["9"] = {"###", "#.#", "###", "..#", "###"},
  ["/"] = {"..#", ".#.", ".#.", ".#.", "#.."},
}

local function textWidth(text)
  return #text * 4 - 1
end

local function drawText(img, x, y, text, color, shadow)
  local cx = x
  for i = 1, #text do
    local glyph = GLYPH[text:sub(i, i)]
    if glyph then
      for gy, row in ipairs(glyph) do
        for gx = 1, #row do
          if row:sub(gx, gx) == "#" then
            img:drawPixel(cx + gx, y + gy, shadow)
            img:drawPixel(cx + gx - 1, y + gy - 1, color)
          end
        end
      end
    end
    cx = cx + 4
  end
end

local OUT = rgba(0, 0, 0, 255)
local MANA = {
  outline = rgba(74, 48, 18, 255),
  mid = rgba(214, 158, 52, 255),
  light = rgba(255, 214, 120, 255),
  core = rgba(255, 246, 214, 255),
  border = rgba(28, 22, 14, 255),
  shadow = rgba(58, 40, 18, 255),
  lit = rgba(120, 86, 40, 255),
  bed = rgba(32, 24, 14, 255),
  deep = rgba(120, 78, 22, 255),
  fill = rgba(214, 158, 52, 255),
  hi = rgba(255, 214, 120, 255),
}
local WATER = {
  outline = rgba(8, 32, 48, 255),
  deep = rgba(14, 86, 128, 255),
  mid = rgba(42, 168, 206, 255),
  light = rgba(150, 226, 246, 255),
  core = rgba(236, 250, 255, 255),
  border = rgba(8, 18, 26, 255),
  shadow = rgba(10, 42, 58, 255),
  lit = rgba(48, 110, 136, 255),
  bed = rgba(8, 22, 30, 255),
}

local function manaIcon()
  local img = newImage(9, 9)
  stamp(img, 0, 0, {
    "....#....",
    "...#L#...",
    "..#LmL#..",
    ".#Lm#mL#.",
    "#Lm#C#mL#",
    ".#Lm#mL#.",
    "..#LmL#..",
    "...#L#...",
    "....#....",
  }, {
    ["#"] = MANA.outline,
    L = MANA.light,
    m = MANA.mid,
    C = MANA.core,
  })
  return img
end

local function waterIcon()
  local img = newImage(9, 9)
  stamp(img, 0, 0, {
    "....#....",
    "...#L#...",
    "..#LCL#..",
    ".#LCWCL#.",
    ".#LWWWL#.",
    ".#LDDDL#.",
    "..#LLL#..",
    "...###...",
    ".........",
  }, {
    ["#"] = WATER.outline,
    L = WATER.light,
    C = WATER.core,
    W = WATER.mid,
    D = WATER.deep,
  })
  return img
end

local TRACK_W, TRACK_H, FILL_W, FILL_H = 91, 9, 87, 5

local function manaTrack()
  local img = newImage(TRACK_W, TRACK_H)
  groove(img, 0, 0, TRACK_W, TRACK_H, MANA.border, MANA.shadow, MANA.lit, MANA.bed)
  return img
end

local function waterTrack()
  local img = newImage(TRACK_W, 11)
  groove(img, 0, 2, TRACK_W, TRACK_H, WATER.border, WATER.shadow, WATER.lit, WATER.bed)
  local function tickAt(ratio)
    local pixels = math.max(1, math.floor(ratio * FILL_W + 0.5))
    return 2 + pixels - 1
  end
  local tick20 = tickAt(0.20)
  local tick50 = tickAt(0.50)
  img:drawPixel(tick20, 1, rgba(232, 206, 120, 255))
  img:drawPixel(tick20, 2, rgba(255, 236, 180, 255))
  img:drawPixel(tick50, 0, rgba(255, 244, 210, 255))
  img:drawPixel(tick50, 1, rgba(255, 244, 210, 255))
  img:drawPixel(tick50, 2, rgba(255, 250, 230, 255))
  return img
end

local manaFill = liquid(FILL_W, FILL_H, 0, MANA.deep, MANA.fill, MANA.hi, MANA.core)
local waterFill = liquid(FILL_W, FILL_H, 0, WATER.deep, WATER.mid, WATER.light, WATER.core)
local manaMark = manaIcon()
local waterMark = waterIcon()

local function paintMeter(dst, x, y, icon, track, fill, ratio, label, textColor, textShadow, fillY)
  local iconY = y + math.floor((track.height - icon.height) / 2)
  blit(dst, icon, x, iconY)
  local tx = x + icon.width + 3
  blit(dst, track, tx, y)
  local pixels = 0
  if ratio >= 1 then
    pixels = FILL_W
  elseif ratio > 0 then
    pixels = math.max(1, math.floor(ratio * FILL_W + 0.5))
  end
  if pixels > 0 then
    blit(dst, fill, tx + 2, y + fillY, pixels, FILL_H)
  end
  local textX = tx + TRACK_W + 4
  local textY = y + math.floor((track.height - 6) / 2)
  drawText(dst, textX, textY, label, textColor, textShadow)
end

local function saveFlat(img, asePath, pngPath, layerName)
  local spr = Sprite(img.width, img.height, ColorMode.RGB)
  spr.layers[1].name = layerName
  local cel = spr.cels[1]
  cel.image = img
  spr:saveAs(asePath)
  img:saveAs(pngPath)
  spr:close()
end

local sheet = newImage(208, 28)
blit(sheet, manaMark, 0, 0)
blit(sheet, waterMark, 0, 12)
blit(sheet, manaTrack(), 16, 0)
blit(sheet, waterTrack(), 16, 12)
blit(sheet, manaFill, 120, 1)
blit(sheet, waterFill, 120, 16)

local src = root .. "/src"
local exportDir = root .. "/export"
local preview = root .. "/preview"

saveFlat(sheet, src .. "/resource_bars_v1.aseprite", exportDir .. "/resource_bars_sheet.png", "sheet")
manaMark:saveAs(exportDir .. "/mana_icon.png")
waterMark:saveAs(exportDir .. "/water_icon.png")
manaTrack():saveAs(exportDir .. "/mana_track.png")
waterTrack():saveAs(exportDir .. "/water_track.png")
manaFill:saveAs(exportDir .. "/mana_fill.png")
waterFill:saveAs(exportDir .. "/water_fill.png")

local function heart(dst, x, y)
  stamp(dst, x, y, {
    "#.#.#",
    "#####",
    "#####",
    ".###.",
    "..#..",
  }, { ["#"] = rgba(176, 42, 42, 255) })
end

local function armor(dst, x, y)
  stamp(dst, x, y, {
    ".###.",
    "#...#",
    "#.#.#",
    "#...#",
    ".###.",
  }, { ["#"] = rgba(168, 168, 176, 255) })
end

local function food(dst, x, y)
  stamp(dst, x, y, {
    "..##.",
    ".###.",
    "###..",
    "##...",
    "#....",
  }, { ["#"] = rgba(150, 96, 48, 255) })
end

local function slot(dst, x, y)
  for yy = 0, 15 do
    for xx = 0, 15 do
      local c = rgba(24, 24, 24, 255)
      if xx == 0 or yy == 0 or xx == 15 or yy == 15 then
        c = rgba(58, 58, 58, 255)
      elseif yy == 1 or xx == 1 then
        c = rgba(14, 14, 14, 255)
      end
      dst:drawPixel(x + xx, y + yy, c)
    end
  end
end

local context = newImage(240, 196)
for y = 0, 195 do
  for x = 0, 239 do
    context:drawPixel(x, y, rgba(12, 12, 12, 255))
  end
end

local clusterX = 8
paintMeter(context, clusterX, 8, manaMark, manaTrack(), manaFill, 62 / 80, "62/80",
  rgba(255, 236, 196, 255), rgba(42, 24, 8, 255), 2)
paintMeter(context, clusterX, 24, waterMark, waterTrack(), waterFill, 0.55, "55",
  rgba(232, 248, 255, 255), rgba(6, 24, 36, 255), 4)

local left = 120 - 91
local right = 120 + 91
local function bracketPixel(x, y)
  if x >= 0 and y >= 0 and x < 240 and y < 196 then
    context:drawPixel(x, y, rgba(128, 54, 54, 255))
  end
end
for x = left, right do
  bracketPixel(x, 62)
  bracketPixel(x, 184)
end
for y = 62, 184 do
  bracketPixel(left, y)
  bracketPixel(right, y)
end

for x = 120 - 32, 120 + 31 do
  for y = 70, 76 do
    local c = rgba(150, 150, 150, 255)
    if y == 70 or y == 76 or x == 120 - 32 or x == 120 + 31 then
      c = rgba(90, 90, 90, 255)
    end
    context:drawPixel(x, y, c)
  end
end

for i = 0, 9 do
  armor(context, left + 6 + i * 8, 84)
  heart(context, left + 6 + i * 8, 98)
  food(context, right - 10 - i * 8, 98)
end

for x = left + 4, right - 4 do
  for y = 112, 115 do
    local c = y == 100 and rgba(110, 196, 86, 255) or rgba(48, 110, 40, 255)
    context:drawPixel(x, y, c)
  end
end

for i = 0, 8 do
  slot(context, left + 6 + i * 18, 126)
end

context:saveAs(preview .. "/context_v1.png")
saveFlat(context, src .. "/context_v1.aseprite", preview .. "/context_v1.png", "context")

local states = newImage(240, 176)
for y = 0, 175 do
  for x = 0, 239 do
    states:drawPixel(x, y, rgba(12, 12, 12, 255))
  end
end
local samples = {
  {0, "0/80", 10},
  {40 / 80, "40/80", 28},
  {1, "80/80", 46},
}
for _, sample in ipairs(samples) do
  paintMeter(states, 8, sample[3], manaMark, manaTrack(), manaFill, sample[1], sample[2],
    rgba(255, 236, 196, 255), rgba(42, 24, 8, 255), 2)
end
local waterSamples = {
  {0, "0", 74},
  {0.20, "20", 96},
  {0.50, "50", 118},
  {1, "100", 140},
}
for _, sample in ipairs(waterSamples) do
  paintMeter(states, 8, sample[3], waterMark, waterTrack(), waterFill, sample[1], sample[2],
    rgba(232, 248, 255, 255), rgba(6, 24, 36, 255), 4)
end
states:saveAs(preview .. "/states_v1.png")
saveFlat(states, src .. "/states_v1.aseprite", preview .. "/states_v1.png", "states")

print("resource bars drawn")
