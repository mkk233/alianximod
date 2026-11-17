-- 垂丝示意图：弹道只移动瀑布上沿，帘身每拍从上沿垂直泻下。
-- 水色取自垂丝瀑布单帧：#143f7a #126aaf #199ed2 #66d8ea #c6f8fa #f5ffff

local W, H = 576, 336
local GROUND = 228
local HALF = 13

local function C(r, g, b, a)
  return Color { r = r, g = g, b = b, a = a or 255 }
end

local EDGE = C(0x14, 0x3f, 0x7a)
local DEEP = C(0x12, 0x6a, 0xaf)
local BODY = C(0x19, 0x9e, 0xd2)
local LITE = C(0x66, 0xd8, 0xea)
local CORE = C(0xc6, 0xf8, 0xfa)
local FOAM = C(0xf5, 0xff, 0xff)
local BG = C(7, 16, 28)
local BAR = C(12, 32, 48)
local GROUND_COL = C(18, 40, 58)
local GROUND_TOP = C(42, 78, 104)
local LINE = C(32, 64, 92)
local GHOST = C(92, 64, 58)
local DOMAIN = C(72, 186, 156)
local DOMAIN_FILL = C(18, 70, 62, 90)
local BAD = C(176, 72, 72)
local SPARK = C(255, 214, 130)
local SWEEP = C(20, 72, 112)

local function put(img, x, y, col)
  local px = math.floor(x + 0.5)
  local py = math.floor(y + 0.5)
  if px < 0 or py < 0 or px >= img.width or py >= img.height then return end
  img:drawPixel(px, py, col)
end

local function fillRect(img, x, y, w, h, col)
  local gc = img.context
  gc.antialias = false
  gc.color = col
  gc:fillRect(Rectangle(x, y, w, h))
end

local function hash(x, y)
  local n = (math.floor(x) * 374761393 + math.floor(y) * 668265263) % 100000
  if n < 0 then n = -n end
  return n % 7
end

local function lerp(a, b, t)
  return a + (b - a) * t
end

local function clamp01(v)
  if v < 0 then return 0 end
  if v > 1 then return 1 end
  return v
end

local function blit(img, x, y, rows, map, scale)
  scale = scale or 1
  for ry, row in ipairs(rows) do
    for rx = 1, #row do
      local col = map[row:sub(rx, rx)]
      if col then
        for dy = 0, scale - 1 do
          for dx = 0, scale - 1 do
            put(img, x + (rx - 1) * scale + dx, y + (ry - 1) * scale + dy, col)
          end
        end
      end
    end
  end
end

local function drawFeet(img, feetX, feetY, rows, map, scale)
  scale = scale or 2
  local h = #rows * scale
  local w = #rows[1] * scale
  blit(img, feetX - math.floor(w / 2), feetY - h + 1, rows, map, scale)
end

local CASTER = {
  "...##...",
  "..#oo#..",
  "..#oo#..",
  ".##oo###",
  ".#oooo#.",
  "..#oo#..",
  "..#..#..",
  "..#..#..",
  "..#..#..",
}
local TARGET = {
  "..####..",
  ".##ee##.",
  ".######.",
  "..####..",
  "..#..#..",
  "..#..#..",
  "..#..#..",
  "..#..#..",
}
local function casterMap()
  return { ["#"] = C(36, 28, 24), o = C(232, 214, 186) }
end
local function targetMap(body)
  return { ["#"] = body, e = C(255, 236, 214) }
end

local function dashRect(img, x, y, w, h, col)
  local function edge(x0, y0, x1, y1)
    local dx, dy = x1 - x0, y1 - y0
    local len = math.sqrt(dx * dx + dy * dy)
    local steps = math.floor(len)
    for s = 0, steps do
      if (s % 5) < 3 then
        put(img, x0 + dx * (s / math.max(len, 1)), y0 + dy * (s / math.max(len, 1)), col)
      end
    end
  end
  edge(x, y, x + w, y)
  edge(x + w, y, x + w, y + h)
  edge(x, y + h, x + w, y + h)
  edge(x, y, x, y + h)
end

local function crossMark(img, x, y, col)
  for i = -2, 2 do
    put(img, x + i, y + i, col)
    put(img, x + i, y - i, col)
  end
end

local function plusMark(img, x, y, col)
  for i = -2, 2 do
    put(img, x + i, y, col)
    put(img, x, y + i, col)
  end
end

-- 一整面瀑布：上沿是亮口，水从口上垂直往下流。
local function curtain(img, lipX, lipY, fall, frame)
  local top = math.floor(lipY + 0.5)
  local bottom = math.floor(math.min(lipY + fall, GROUND - 2) + 0.5)
  if bottom - top < 3 then return end
  local left = math.floor(lipX - HALF + 0.5)
  local right = math.floor(lipX + HALF + 0.5)
  for x = left, right do
    local dx = x - lipX
    local edge = math.abs(dx) >= HALF - 1
    for y = top, bottom do
      local down = math.floor(y + frame * 4) % 9
      local ragged = y > bottom - 4 and hash(x, y + frame) == 0
      if not ragged then
        local col
        if edge then
          col = (hash(x, y) == 0) and DEEP or EDGE
        elseif down == 0 then
          col = FOAM
        elseif down <= 2 then
          col = CORE
        elseif down == 4 or down == 5 then
          col = LITE
        else
          col = (hash(x, 0) == 1) and DEEP or BODY
        end
        put(img, x, y, col)
      end
    end
  end
  for x = left - 1, right + 1 do
    put(img, x, top, FOAM)
    put(img, x, top + 1, CORE)
  end
  if bottom >= GROUND - 4 then
    for x = left, right, 2 do
      put(img, x, bottom, FOAM)
      put(img, x - 1, bottom + 1, LITE)
    end
  end
end

-- 上沿从上一拍挪到这一拍时，两面帘之间的体积。用疏点表示，不画成第二道实帘。
local function sweep(img, x0, y0, x1, y1, fall)
  local left = math.floor(math.min(x0, x1) - HALF)
  local right = math.floor(math.max(x0, x1) + HALF)
  local top = math.floor(math.min(y0, y1))
  local bottom = math.floor(math.min(GROUND - 2, math.max(y0, y1) + fall))
  for x = left, right do
    for y = top, bottom do
      if ((x + y) % 3) == 0 then put(img, x, y, SWEEP) end
    end
  end
end

local function fallOf(frame, lipY)
  local grown = 22 + 120 * clamp01(frame / 9)
  local room = GROUND - 2 - lipY
  if grown > room then return room end
  return grown
end

-- 锁定后上沿只沿释放方向伸出，停住后再略沉。不再转向。
local function lip1(frame)
  local reach = clamp01(frame / 7)
  local x = 58 + 74 * reach
  local y = 86
  if frame > 8 then y = 86 + (frame - 8) * 2.4 end
  return x, y, fallOf(frame, y)
end

local function target1(frame)
  local feetX, feetY = 132, GROUND - 1
  if frame >= 9 then
    local t = clamp01((frame - 9) / 4)
    feetX = lerp(132, 168, t)
    feetY = lerp(GROUND - 1, 150, t)
  end
  return feetX, feetY
end

-- 追踪只搬上沿。帘始终从当前上沿垂下。
local function target2(frame)
  local t = clamp01((frame - 1) / 15)
  return lerp(300, 352, t), lerp(GROUND - 1, 168, t)
end

local function lip2(frame)
  local y = 74
  local early = math.min(frame, 11)
  local txEarly = target2(early)
  local x = lerp(236, txEarly, clamp01((early - 1) / 10))
  if frame >= 12 then
    local leaped = x + 26
    local txNow = target2(frame)
    local settle = clamp01((frame - 12) / 4)
    x = lerp(leaped, txNow, settle)
  end
  return x, y, fallOf(frame, y)
end

local function target3(frame)
  local t = clamp01((frame - 1) / 15)
  return lerp(532, 546, t), lerp(188, 160, t)
end

-- 生在域内一点，追踪时上沿离开该点，但不升到被划掉的高处。
local function lip3(frame)
  local tx = target3(frame)
  local chase = clamp01((frame - 1) / 13)
  local x = lerp(436, tx - 16, chase)
  local y = lerp(132, 108, chase)
  return x, y, fallOf(frame, y)
end

local outDir = args.outDir
local spr = Sprite(W, H, ColorMode.RGB)
app.activeSprite = spr
local report = {}

app.transaction(function()
  for _ = 1, 15 do spr:newFrame() end
  for _, fr in ipairs(spr.frames) do fr.duration = 0.12 end
  local tag = spr:newTag(1, #spr.frames)
  tag.name = "schematic"

  local bg = spr.layers[1]
  bg.name = "bg"
  local guides = spr:newLayer()
  guides.name = "guides"
  local actors = spr:newLayer()
  actors.name = "actors"
  local silk = spr:newLayer()
  silk.name = "silk"
  local captions = spr:newLayer()
  captions.name = "captions"

  local bgImg = Image(spr.spec)
  bgImg:clear()
  fillRect(bgImg, 0, 0, W, H, BG)
  fillRect(bgImg, 0, 0, W, 28, BAR)
  fillRect(bgImg, 0, 250, W, H - 250, BAR)
  for _, x in ipairs({ 192, 384 }) do fillRect(bgImg, x, 0, 1, H, LINE) end
  fillRect(bgImg, 0, GROUND, W, 18, GROUND_COL)
  for x = 0, W - 1 do
    put(bgImg, x, GROUND, GROUND_TOP)
  end

  local capImg = Image(spr.spec)
  capImg:clear()
  local labels = {
    { args.title1, 6, 3 }, { args.title2, 198, 3 }, { args.title3, 390, 3 },
    { args.cap1, 6, 254 }, { args.cap2, 198, 254 }, { args.cap3, 390, 254 },
    { args.note, 8, 304 },
  }
  for _, item in ipairs(labels) do
    local file = item[1]
    if file and file ~= "" then
      local ok, label = pcall(function() return Image { fromFile = file } end)
      if ok and label then capImg:drawImage(label, Point(item[2], item[3])) end
    end
  end
  for i = 1, #spr.frames do
    spr:newCel(bg, spr.frames[i], bgImg:clone(), Point(0, 0))
    spr:newCel(captions, spr.frames[i], capImg:clone(), Point(0, 0))
  end

  local tips = {}
  for frame = 1, 16 do
    local guide = Image(spr.spec)
    guide:clear()
    local actor = Image(spr.spec)
    actor:clear()
    local water = Image(spr.spec)
    water:clear()

    local x1, y1, f1 = lip1(frame)
    local px1, py1 = lip1(math.max(1, frame - 1))
    if frame >= 9 then
      drawFeet(guide, 132, GROUND - 1, TARGET, { ["#"] = GHOST, e = GHOST }, 2)
    end
    drawFeet(actor, 46, 118, CASTER, casterMap(), 2)
    local t1x, t1y = target1(frame)
    drawFeet(actor, t1x, t1y, TARGET, targetMap(C(196, 92, 72)), 2)
    if frame > 8 and math.abs(x1 - px1) + math.abs(y1 - py1) > 6 then
      sweep(water, px1, py1, x1, y1, f1)
    end
    curtain(water, x1, y1, f1, frame)
    plusMark(guide, 58, 86, CORE)

    local x2, y2, f2 = lip2(frame)
    local px2, py2, pf2 = lip2(math.max(1, frame - 1))
    drawFeet(actor, 214, 112, CASTER, casterMap(), 2)
    local t2x, t2y = target2(frame)
    drawFeet(actor, t2x, t2y, TARGET, targetMap(C(196, 92, 72)), 2)
    local leap = math.abs(x2 - px2) > 16
    if leap then
      local mx = (px2 + x2) / 2
      drawFeet(actor, mx, y2 + f2 * 0.62, TARGET, targetMap(C(186, 214, 96)), 2)
      sweep(water, px2, py2, x2, y2, math.max(f2, pf2))
    end
    curtain(water, x2, y2, f2, frame)
    plusMark(guide, 236, 74, CORE)
    if leap then
      drawFeet(water, (px2 + x2) / 2, y2 + f2 * 0.62, {
        ".#.",
        "###",
        ".#.",
      }, { ["#"] = SPARK }, 2)
    end

    fillRect(guide, 404, 78, 108, 140, DOMAIN_FILL)
    dashRect(guide, 404, 78, 108, 140, DOMAIN)
    crossMark(guide, 448, 52, BAD)
    local x3, y3, f3 = lip3(frame)
    local px3, py3 = lip3(math.max(1, frame - 1))
    local t3x, t3y = target3(frame)
    drawFeet(actor, t3x, t3y, TARGET, targetMap(C(196, 92, 72)), 2)
    if frame > 2 and math.abs(x3 - px3) > 8 then
      sweep(water, px3, py3, x3, y3, f3)
    end
    curtain(water, x3, y3, f3, frame)
    plusMark(guide, 436, 132, CORE)

    spr:newCel(guides, spr.frames[frame], guide, Point(0, 0))
    spr:newCel(actors, spr.frames[frame], actor, Point(0, 0))
    spr:newCel(silk, spr.frames[frame], water, Point(0, 0))
    tips[frame] = { x1, y1, f1, x2, y2, f2, x3, y3, f3 }
  end

  local sheet = Image(W * 2, H * 2, ColorMode.RGB)
  sheet:clear()
  local keys = { 4, 8, 12, 16 }
  for i, fn in ipairs(keys) do
    local col = (i - 1) % 2
    local row = math.floor((i - 1) / 2)
    sheet:drawSprite(spr, fn, Point(col * W, row * H))
  end
  sheet:saveAs(outDir .. "/weeping-trajectory-keys.png")
  local function flat(entry)
    return {
      x1 = math.floor(entry[1]), y1 = math.floor(entry[2]), f1 = math.floor(entry[3]),
      x2 = math.floor(entry[4]), y2 = math.floor(entry[5]), f2 = math.floor(entry[6]),
      x3 = math.floor(entry[7]), y3 = math.floor(entry[8]), f3 = math.floor(entry[9]),
    }
  end
  report.frameTips = { f4 = flat(tips[4]), f8 = flat(tips[8]), f12 = flat(tips[12]), f16 = flat(tips[16]) }
end)

local asePath = outDir .. "/weeping-trajectory.aseprite"
spr:saveAs(asePath)
report.file = asePath
report.frames = #spr.frames
report.size = { W, H }
report.saved = true
return report
