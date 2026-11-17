-- 火苗 v2：16x16 MC 像素密度，4 倍放大到 64x64，16 帧无缝循环。
-- 横向飞行火舌，尾在左、头在右；只由 Aseprite MCP 绘制。
local out = args.out
local L, S, N = 16, 4, 16
local rgb = app.pixelColor.rgba
local P = {
  rgb(92, 18, 8, 255),     -- 1 暗红外缘
  rgb(164, 36, 8, 255),    -- 2 红
  rgb(224, 78, 8, 255),    -- 3 橙红
  rgb(255, 142, 18, 255),  -- 4 橙
  rgb(255, 208, 42, 255),  -- 5 金
  rgb(255, 246, 168, 255), -- 6 浅黄焰芯
}

local function hash(ix, iy)
  local n = (ix * 374761393 + iy * 668265263 + 1013904223) & 0x7fffffff
  n = ((n ~ (n >> 13)) * 1274126177) & 0x7fffffff
  return (n ~ (n >> 16)) % 1000 / 999
end

local function noise(x, y)
  local gx, gy = x / 4, y / 4
  local x0, y0 = math.floor(gx), math.floor(gy)
  local fx, fy = gx - x0, gy - y0
  fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy)
  local function g(a, b) return hash(a, b % 4) end
  return (g(x0, y0) * (1 - fx) + g(x0 + 1, y0) * fx) * (1 - fy)
       + (g(x0, y0 + 1) * (1 - fx) + g(x0 + 1, y0 + 1) * fx) * fy
end

local tongues = {
  {7.4, 14.2, 3.4, 0.0, 1.1},
  {4.7, 10.5, 2.0, 2.2, 0.8},
  {10.1, 9.2, 1.8, 4.4, 0.8},
}

local function heatAt(x, y, frame)
  local phase = (frame - 1) * math.pi * 2 / N
  local along = x + 0.5
  local center = 8.0
  local best = 0
  for _, tongue in ipairs(tongues) do
    local baseY, reach, width, offset, sway = tongue[1], tongue[2], tongue[3], tongue[4], tongue[5]
    local flow = L - along
    local reachNow = reach + 1.2 * math.sin(phase + offset)
    if flow <= reachNow then
      local ratio = flow / reachNow
      local yNow = baseY + sway * math.sin(phase * 2 + offset + ratio * 3)
      local half = width * (0.35 + 0.65 * (1 - ratio) ^ 0.65) + 0.35
      local value = 1 - math.abs((y + 0.5) - yNow) / half
      if value > best then best = value end
    end
  end
  if best <= 0 then return 0 end
  return best + (noise(x + frame - 1, y) - 0.5) * 0.42 * math.min(1, best * 2)
end

local field = {}
for frame = 1, N do
  local raw, shaped = {}, {}
  for y = 0, L - 1 do
    raw[y], shaped[y] = {}, {}
    for x = 0, L - 1 do
      local heat = heatAt(x, y, frame)
      local color = 0
      if heat >= 0.18 then
        if heat < 0.34 then color = 2 elseif heat < 0.52 then color = 3
        elseif heat < 0.72 then color = 4 elseif heat < 0.92 then color = 5 else color = 6 end
      end
      raw[y][x] = color
    end
  end
  for y = 0, L - 1 do
    for x = 0, L - 1 do
      local color = raw[y][x]
      if color > 0 then
        local function empty(xx, yy)
          return xx < 0 or xx >= L or yy < 0 or yy >= L or raw[yy][xx] <= 0
        end
        local front = empty(x + 1, y)
        local side = empty(x, y - 1) or empty(x, y + 1) or empty(x - 1, y)
        if front and color > 1 then color = (color >= 4) and 2 or 1 end
        if side and color > 2 then color = 2 end
      end
      shaped[y][x] = color
    end
  end
  field[frame] = shaped
end

local sprite = Sprite(L * S, L * S, ColorMode.RGB)
local layers = {sprite.layers[1], sprite:newLayer(), sprite:newLayer(), sprite:newLayer()}
layers[1].name = "01_dark_edge"
layers[2].name = "02_orange_body"
layers[3].name = "03_hot_core"
layers[4].name = "04_sparks"

local function block(image, x, y, color)
  for yy = y * S, y * S + S - 1 do
    for xx = x * S, x * S + S - 1 do image:drawPixel(xx, yy, color) end
  end
end

local sparks = {{2, 0}, {5, 6}, {8, 11}, {3, 3}}
for frame = 1, N do
  if frame > 1 then sprite:newEmptyFrame(frame) end
  sprite.frames[frame].duration = 0.08
  local images = {}
  for index = 1, 4 do images[index] = Image(L * S, L * S, ColorMode.RGB) end
  local shaped = field[frame]
  for y = 0, L - 1 do
    for x = 0, L - 1 do
      local color = shaped[y][x]
      if color > 0 then
        local layer = color <= 2 and 1 or (color <= 4 and 2 or 3)
        block(images[layer], x, y, P[color])
      end
    end
  end
  for _, spark in ipairs(sparks) do
    local age = (frame - 1 + spark[2]) % N
    if age < 8 then
      local sx = 14 - age
      local sy = spark[1] + math.floor(math.sin(age + spark[2]) + 0.5)
      if sx >= 0 and sy >= 0 and sy < L and shaped[sy][sx] <= 0 then
        block(images[4], sx, sy, age < 3 and P[6] or (age < 6 and P[5] or P[4]))
      end
    end
  end
  for index = 1, 4 do sprite:newCel(layers[index], frame, images[index], Point(0, 0)) end
end

local tag = sprite:newTag(1, N)
tag.name = "spark_loop_1280ms"
sprite.data = "fire-spark-v2; 64x64; 16 frames; 80ms; 16px MC grid at 4x; Aseprite MCP; draft only"
sprite:saveAs(out .. "/fire_spark_v2.aseprite")
local sheet = Image(L * S, L * S * N, ColorMode.RGB)
for frame = 1, N do sheet:drawSprite(sprite, frame, Point(0, (frame - 1) * L * S)) end
sheet:saveAs(out .. "/fire_spark_v2_strip.png")
return {file = sprite.filename, frames = #sprite.frames, layers = #sprite.layers}
