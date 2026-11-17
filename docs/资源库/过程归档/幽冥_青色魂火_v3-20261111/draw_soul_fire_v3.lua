-- 幽冥 · 青色魂火 v3：16x16 MC 原版贴图密度，按 4 倍块放大成 64x64 帧，16 帧无缝循环。
-- 经 Aseprite MCP run_lua 执行；不读取任何外部图片。
local out = args.out
local L, S, N = 16, 4, 16
local rgb = app.pixelColor.rgba
local P = {
  rgb(4, 52, 62, 255),    -- 1 深石油青描边
  rgb(0, 120, 132, 255),  -- 2 暗青
  rgb(0, 160, 170, 255),  -- 3 青
  rgb(28, 212, 220, 255), -- 4 亮青
  rgb(112, 246, 240, 255),-- 5 浅青
  rgb(206, 255, 249, 255),-- 6 焰芯
  rgb(242, 255, 253, 255) -- 7 火星最亮
}

local function hash(ix, iy)
  local n = (ix * 374761393 + iy * 668265263 + 1013904223) & 0x7fffffff
  n = ((n ~ (n >> 13)) * 1274126177) & 0x7fffffff
  n = n ~ (n >> 16)
  return (n % 1000) / 999
end

-- 竖直方向以 16 格为周期的平滑噪声，帧号整数平移即无缝上流
local function vnoise(x, y)
  local gx, gy = x / 4, y / 4
  local x0, y0 = math.floor(gx), math.floor(gy)
  local fx, fy = gx - x0, gy - y0
  fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy)
  local function g(a, b) return hash(a, b % 4) end
  local a, b = g(x0, y0), g(x0 + 1, y0)
  local c, d = g(x0, y0 + 1), g(x0 + 1, y0 + 1)
  return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy
end

-- cx, 高, 半宽, 相位, 高度频率, 摆幅
local tongues = {
  {1.6, 5.0, 1.5, 0.6, 2, 0.5},
  {4.4, 9.5, 2.3, 2.1, 1, 0.9},
  {8.0, 13.0, 3.0, 0.0, 1, 1.1},
  {11.7, 10.0, 2.4, 4.0, 1, 0.9},
  {14.5, 5.5, 1.5, 3.2, 2, 0.5},
}

local function heatAt(x, y, f)
  local th = 2 * math.pi * (f - 1) / N
  local hb = (L - 1 - y) + 0.5
  local px = x + 0.5
  local shape = 0
  for _, v in ipairs(tongues) do
    local cx, h, w, ph, k, sway = v[1], v[2], v[3], v[4], v[5], v[6]
    local ht = h + 1.5 * math.sin(k * th + ph)
    if hb < ht then
      local r = hb / ht
      local lean = sway * math.sin(th + ph + 1.0) * r
      local tw = w * (1 - r) ^ 0.8 + 0.4
      local val = 1 - math.abs(px - (cx + lean)) / tw
      if val > shape then shape = val end
    end
  end
  local base = math.max(0, 1 - hb / 3) * 0.20
  local body = shape + base
  if body <= 0 then return 0 end
  local n = vnoise(x, y + (f - 1))
  return body + (n - 0.5) * 0.55 * math.min(1, body * 2.5)
end

local field = {}
for f = 1, N do
  local g = {}
  for y = 0, L - 1 do
    g[y] = {}
    for x = 0, L - 1 do
      local h = heatAt(x, y, f)
      local c = 0
      if h >= 0.18 then
        if h < 0.30 then c = 2 elseif h < 0.46 then c = 3 elseif h < 0.62 then c = 4
        elseif h < 0.88 then c = 5 else c = 6 end
      end
      if y == L - 1 and x >= 1 and x <= L - 2 and c < 2 then c = 2 end
      g[y][x] = c
    end
  end
  -- 去掉孤立像素，避免噪点
  for y = 0, L - 2 do
    for x = 0, L - 1 do
      if g[y][x] > 0 then
        local nb = 0
        for _, d in ipairs({{1,0},{-1,0},{0,1},{0,-1}}) do
          local yy, xx = y + d[2], x + d[1]
          if yy >= 0 and yy < L and xx >= 0 and xx < L and g[yy][xx] > 0 then nb = nb + 1 end
        end
        if nb == 0 then g[y][x] = -1 end
      end
    end
  end
  -- 外缘压暗：接触透明的像素最多为暗青，顶端接触透明为深描边
  local e = {}
  for y = 0, L - 1 do
    e[y] = {}
    for x = 0, L - 1 do
      local c = g[y][x]
      if c > 0 then
        local up = (y == 0) or g[y - 1][x] <= 0
        local side = (x == 0) or (x == L - 1) or g[y][x - 1] <= 0 or g[y][x + 1] <= 0
        -- 顶缘一格压暗，侧缘最多亮到青色，内部保留亮焰芯
        if up then c = (c >= 3) and 2 or 1 end
        if side and c > 3 then c = 3 end
      end
      e[y][x] = c
    end
  end
  field[f] = e
end

local s = Sprite(L * S, L * S, ColorMode.RGB)
local layers = {s.layers[1], s:newLayer(), s:newLayer(), s:newLayer()}
layers[1].name = "01_deep_teal_edge"
layers[2].name = "02_cyan_body"
layers[3].name = "03_pale_soul_core"
layers[4].name = "04_soul_sparks"

local function block(im, x, y, c)
  for yy = y * S, y * S + S - 1 do
    for xx = x * S, x * S + S - 1 do im:drawPixel(xx, yy, c) end
  end
end

-- 火星：从火舌顶部脱离，逐帧上升并变暗；每个周期 16 帧
local sparks = {{2, 3, 0.0}, {7, 9, 1.7}, {11, 13, 3.1}, {5, 6, 4.4}, {13, 1, 2.2}}

for f = 1, N do
  if f > 1 then s:newEmptyFrame(f) end
  s.frames[f].duration = 0.08
  local im = {}
  for k = 1, 4 do im[k] = Image(L * S, L * S, ColorMode.RGB) end
  local e = field[f]
  for y = 0, L - 1 do
    for x = 0, L - 1 do
      local c = e[y][x]
      if c > 0 then
        local layer = (c <= 2) and 1 or ((c <= 4) and 2 or 3)
        block(im[layer], x, y, P[c])
      end
    end
  end
  for _, sp in ipairs(sparks) do
    local age = (f - 1 + sp[2]) % N
    if age < 10 then
      local sy = L - 1 - math.floor(8 + age * 0.75)
      local sx = sp[1] + math.floor(math.sin(age * 0.7 + sp[3]) + 0.5)
      if sy >= 0 and sx >= 0 and sx < L and e[sy][sx] <= 0 then
        local col = (age < 3) and P[7] or ((age < 6) and P[5] or P[4])
        block(im[4], sx, sy, col)
      end
    end
  end
  for k = 1, 4 do s:newCel(layers[k], f, im[k], Point(0, 0)) end
end

local tag = s:newTag(1, N)
tag.name = "soul_flame_loop_1280ms"
s.data = "youming-cyan-soul-v3; 64x64; 16 frames; 80ms; 16px MC texel grid at 4x; Aseprite MCP; draft only"
s:saveAs(out .. "/youming_cyan_soul_v3.aseprite")
local sheet = Image(L * S, L * S * N, ColorMode.RGB)
for f = 1, N do sheet:drawSprite(s, f, Point(0, (f - 1) * L * S)) end
sheet:saveAs(out .. "/youming_cyan_soul_v3_strip.png")
local first = Image(L * S, L * S, ColorMode.RGB)
first:drawSprite(s, 1)
first:saveAs(out .. "/youming_cyan_soul_v3_frame01.png")
return {file = s.filename, width = s.width, height = s.height, frames = #s.frames, layers = #s.layers}
