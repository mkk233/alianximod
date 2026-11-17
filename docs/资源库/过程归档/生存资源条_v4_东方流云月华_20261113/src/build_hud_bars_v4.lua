local root    = "D:/alianximod/docs/资源库/过程归档/生存资源条_v4_东方流云月华_20261113"
local src_dir = root .. "/src"
local exp_dir = root .. "/export"
local prv_dir = root .. "/preview"
local v2_dir  = "D:/alianximod/docs/资源库/界面/生存资源条_细条_v2"

local FRAMES = 16
local FRAME_DURATION = 0.09

local ICON_W, ICON_H = 18, 18
local TRACK_W, TRACK_H = 102, 9
local FILL_W, FILL_H = 90, 5
local FILL_X_OFFSET = 6 -- tx=0..5 left Chinese fretwork clasp (6px), tx=6..95 inner chamber (90px), tx=96..101 right fretwork end-cap (6px)
local FILL_Y_OFFSET = 2 -- ty=0 proud corner/clasp, ty=1 top rail, ty=2..6 inner chamber (5px), ty=7 bottom rail, ty=8 proud corner/clasp
local ICON_OVERLAP  = 3 -- track starts at icon_x + 15 so tx=0..2 tucks smoothly behind icon right tail (ix=15..17)

local function rgba(r, g, b, a)
  return app.pixelColor.rgba(r, g, b, a or 255)
end

local function r_of(c) return app.pixelColor.rgbaR(c) end
local function g_of(c) return app.pixelColor.rgbaG(c) end
local function b_of(c) return app.pixelColor.rgbaB(c) end
local function a_of(c) return app.pixelColor.rgbaA(c) end

local function lerp_color(c1, c2, t)
  if t <= 0 then return c1 end
  if t >= 1 then return c2 end
  local r = math.floor(r_of(c1) + (r_of(c2) - r_of(c1)) * t + 0.5)
  local g = math.floor(g_of(c1) + (g_of(c2) - g_of(c1)) * t + 0.5)
  local b = math.floor(b_of(c1) + (b_of(c2) - b_of(c1)) * t + 0.5)
  local a = math.floor(a_of(c1) + (a_of(c2) - a_of(c1)) * t + 0.5)
  return rgba(r, g, b, a)
end

local function new_image(w, h, bg)
  local img = Image(w, h, ColorMode.RGB)
  local clear = bg or rgba(0, 0, 0, 0)
  for y = 0, h - 1 do
    for x = 0, w - 1 do
      img:drawPixel(x, y, clear)
    end
  end
  return img
end

local function put_pixel(img, x, y, c)
  if x < 0 or y < 0 or x >= img.width or y >= img.height then return end
  local sa = a_of(c)
  if sa <= 0 then return end
  if sa >= 255 then
    img:drawPixel(x, y, c)
    return
  end
  local dc = img:getPixel(x, y)
  local da = a_of(dc)
  if da <= 0 then
    img:drawPixel(x, y, c)
    return
  end
  local t = sa / 255.0
  local r = math.floor(r_of(dc) * (1 - t) + r_of(c) * t + 0.5)
  local g = math.floor(g_of(dc) * (1 - t) + g_of(c) * t + 0.5)
  local b = math.floor(b_of(dc) * (1 - t) + b_of(c) * t + 0.5)
  local out_a = math.min(255, math.floor(sa + da * (1 - t) + 0.5))
  img:drawPixel(x, y, rgba(r, g, b, out_a))
end

local function fill_rect(img, x, y, w, h, c)
  for yy = y, y + h - 1 do
    for xx = x, x + w - 1 do
      put_pixel(img, xx, yy, c)
    end
  end
end

local function stroke_rect(img, x, y, w, h, c)
  for xx = x, x + w - 1 do
    put_pixel(img, xx, y, c)
    put_pixel(img, xx, y + h - 1, c)
  end
  for yy = y, y + h - 1 do
    put_pixel(img, x, yy, c)
    put_pixel(img, x + w - 1, yy, c)
  end
end

local function blit(dst, src, dx, dy, sw, sh, sx, sy)
  sx = sx or 0
  sy = sy or 0
  sw = sw or (src.width - sx)
  sh = sh or (src.height - sy)
  for y = 0, sh - 1 do
    for x = 0, sw - 1 do
      if sx + x < src.width and sy + y < src.height then
        local c = src:getPixel(sx + x, sy + y)
        if a_of(c) > 0 then
          put_pixel(dst, dx + x, dy + y, c)
        end
      end
    end
  end
end

local function scale_nn(src, factor)
  local dst = new_image(src.width * factor, src.height * factor)
  for y = 0, src.height - 1 do
    for x = 0, src.width - 1 do
      local c = src:getPixel(x, y)
      if a_of(c) > 0 then
        for dy = 0, factor - 1 do
          for dx = 0, factor - 1 do
            dst:drawPixel(x * factor + dx, y * factor + dy, c)
          end
        end
      end
    end
  end
  return dst
end

local function stamp(img, ox, oy, rows, palette)
  for y, raw_row in ipairs(rows) do
    local row = raw_row:gsub(" ", "")
    if #row ~= ICON_W and ox == 0 and oy == 0 and img.width == ICON_W then
      error(string.format("Stamp row %d has length %d (expected %d): %s", y, #row, ICON_W, raw_row))
    end
    for x = 1, #row do
      local ch = row:sub(x, x)
      local c = palette[ch]
      if c then
        put_pixel(img, ox + x - 1, oy + y - 1, c)
      end
    end
  end
end

-- Crisp 3x5 / 4x5 pixel font for preview annotations & HUD numbers
local FONT = {
  ["0"] = {"###", "#.#", "#.#", "#.#", "###"},
  ["1"] = {".#.", "##.", ".#.", ".#.", "###"},
  ["2"] = {"###", "..#", "###", "#..", "###"},
  ["3"] = {"###", "..#", ".##", "..#", "###"},
  ["4"] = {"#.#", "#.#", "###", "..#", "..#"},
  ["5"] = {"###", "#..", "###", "..#", "###"},
  ["6"] = {"###", "#..", "###", "#.#", "###"},
  ["7"] = {"###", "..#", ".#.", ".#.", ".#."},
  ["8"] = {"###", "#.#", "###", "#.#", "###"},
  ["9"] = {"###", "#.#", "###", "..#", "###"},
  ["/"] = {"..#", ".#.", ".#.", ".#.", "#.."},
  ["+"] = {"...", ".#.", "###", ".#.", "..."},
  ["-"] = {"...", "...", "###", "...", "..."},
  [":"] = {".", "#", ".", "#", "."},
  ["."] = {".", ".", ".", ".", "#"},
  [","] = {".", ".", ".", "#", "#"},
  ["|"] = {"#", "#", "#", "#", "#"},
  ["("] = {".#", "#.", "#.", "#.", ".#"},
  [")"] = {"#.", ".#", ".#", ".#", "#."},
  ["<"] = {"..#", ".#.", "#..", ".#.", "..#"},
  [">"] = {"#..", ".#.", "..#", ".#.", "#.."},
  ["="] = {"...", "###", "...", "###", "..."},
  ["&"] = {".##.", "#.#.", ".##.", "#..#", ".##."},
  ["#"] = {".#.#.", "#####", ".#.#.", "#####", ".#.#."},
  [" "] = {"..", "..", "..", "..", ".."},
  ["A"] = {".#.", "#.#", "###", "#.#", "#.#"},
  ["B"] = {"##.", "#.#", "##.", "#.#", "##."},
  ["C"] = {".##", "#..", "#..", "#..", ".##"},
  ["D"] = {"##.", "#.#", "#.#", "#.#", "##."},
  ["E"] = {"###", "#..", "##.", "#..", "###"},
  ["F"] = {"###", "#..", "##.", "#..", "#.."},
  ["G"] = {".##", "#..", "#.#", "#.#", ".##"},
  ["H"] = {"#.#", "#.#", "###", "#.#", "#.#"},
  ["I"] = {"###", ".#.", ".#.", ".#.", "###"},
  ["J"] = {"..#", "..#", "..#", "#.#", ".#."},
  ["K"] = {"#.#", "#.#", "##.", "#.#", "#.#"},
  ["L"] = {"#..", "#..", "#..", "#..", "###"},
  ["M"] = {"#...#", "##.##", "#.#.#", "#...#", "#...#"},
  ["N"] = {"#..#", "##.#", "#.##", "#..#", "#..#"},
  ["O"] = {"###", "#.#", "#.#", "#.#", "###"},
  ["P"] = {"##.", "#.#", "##.", "#..", "#.."},
  ["Q"] = {"###", "#.#", "#.#", "###", "..#"},
  ["R"] = {"##.", "#.#", "##.", "#.#", "#.#"},
  ["S"] = {".##", "#..", ".#.", "..#", "##."},
  ["T"] = {"###", ".#.", ".#.", ".#.", ".#."},
  ["U"] = {"#.#", "#.#", "#.#", "#.#", "###"},
  ["V"] = {"#.#", "#.#", "#.#", ".#.", ".#."},
  ["W"] = {"#...#", "#...#", "#.#.#", "##.##", "#...#"},
  ["X"] = {"#.#", "#.#", ".#.", "#.#", "#.#"},
  ["Y"] = {"#.#", "#.#", ".#.", ".#.", ".#."},
  ["Z"] = {"###", "..#", ".#.", "#..", "###"},
  ["x"] = {"...", "#.#", ".#.", "#.#", "..."},
  ["v"] = {"...", "#.#", "#.#", ".#.", "..."},
  ["s"] = {"...", ".##", ".#.", "##.", "..."},
  ["m"] = {".....", "##.##", "#.#.#", "#...#", "....."},
}

local function draw_text(img, x, y, str, color, shadow)
  local cx = x
  for i = 1, #str do
    local ch = str:sub(i, i)
    local g = FONT[ch] or FONT[ch:upper()]
    if g then
      local gw = #g[1]
      if shadow then
        for gy = 1, #g do
          local row = g[gy]
          for gx = 1, gw do
            if row:sub(gx, gx) == "#" then
              put_pixel(img, cx + gx, y + gy, shadow)
            end
          end
        end
      end
      for gy = 1, #g do
        local row = g[gy]
        for gx = 1, gw do
          if row:sub(gx, gx) == "#" then
            put_pixel(img, cx + gx - 1, y + gy - 1, color)
          end
        end
      end
      cx = cx + gw + 1
    else
      cx = cx + 3
    end
  end
  return cx
end

-- ============================================================================
-- PALETTES: V4 EASTERN MOONLIGHT CLOUD (MANA) & SINUOUS STREAM (WATER)
-- ============================================================================

local PAL_FRAME = {
  ink_outer  = rgba(18, 23, 33, 255),  -- #121721 deep matte ink-indigo contour
  slate_dark = rgba(33, 42, 58, 255),  -- #212A3A matte dark slate bevel
  stone_rail = rgba(49, 62, 84, 255),  -- #313E54 cool stone-grey rail spine
  fret_lit   = rgba(70, 86, 114, 255), -- #465672 fretwork corner scroll
  fret_hi    = rgba(90, 108, 138, 255),-- #5A6C8A subtle top jade-stone corner glint
}

local PAL_MANA = {
  -- Cavity
  cav_shadow = rgba(16, 22, 42, 255),
  cav_bed_1  = rgba(21, 29, 54, 255),
  cav_bed_2  = rgba(24, 34, 62, 255),
  cav_mark   = rgba(31, 43, 76, 255),
  -- Moonlight Star-Blue & Flowing Cloud Veins (Hue ~220-226 deg, brimming condensed volume, distinct from cyan-teal water)
  deep       = rgba(54, 86, 162, 255),  -- #3656A2 rich moonlight sapphire bed (zero black holes!)
  mid_dark   = rgba(74, 114, 198, 255), -- #4A72C6 condensed periwinkle mana body
  body       = rgba(102, 148, 228, 255),-- #6694E4 translucent moonlight azure
  silk       = rgba(144, 186, 248, 255),-- #90BAF8 sweeping silver-blue cloud silk
  cloud_lit  = rgba(188, 220, 255, 255),-- #BCDCFF luminous long cloud ribbon
  volute     = rgba(228, 243, 255, 255),-- #E4F3FF grand cloud billow crest
  crown      = rgba(247, 252, 255, 255),-- #F7FCFF gathering cloud crown
  -- Low state (<= 22%)
  low_deep   = rgba(32, 50, 96, 255),
  low_mid    = rgba(46, 72, 132, 255),
  low_body   = rgba(64, 98, 168, 255),
  low_silk   = rgba(94, 134, 202, 255),
  low_lit    = rgba(126, 166, 226, 255),
  -- Full state (100% brimming condensed moonlight qi)
  full_deep  = rgba(72, 110, 192, 255),
  full_mid   = rgba(96, 140, 222, 255),
  full_body  = rgba(128, 174, 244, 255),
  full_silk  = rgba(172, 210, 255, 255),
  full_lit   = rgba(214, 236, 255, 255),
  full_crest = rgba(246, 252, 255, 255),
  -- Cast spend afterglow
  ghost_hi   = rgba(146, 182, 238, 255),
  ghost_mid  = rgba(92, 124, 186, 255),
  ghost_low  = rgba(52, 72, 124, 255),
  -- Text
  text        = rgba(226, 238, 255, 255),
  text_shadow = rgba(14, 20, 40, 255),
}

local PAL_WATER = {
  -- Cavity
  cav_shadow = rgba(7, 24, 32, 255),
  cav_bed_1  = rgba(10, 32, 42, 255),
  cav_bed_2  = rgba(13, 38, 49, 255),
  cav_notch  = rgba(22, 54, 68, 255),
  -- Fretwork Threshold Cloud-Clasps (20 & 50)
  clasp_ink        = rgba(14, 21, 28, 255),
  clasp_unlit_edge = rgba(34, 49, 62, 255),
  clasp_unlit_core = rgba(48, 72, 88, 255),
  clasp_lit_edge   = rgba(44, 88, 104, 255),
  clasp_lit_20     = rgba(118, 236, 242, 255),
  clasp_lit_50     = rgba(196, 255, 255, 255),
  -- Tier 1: Trickle (1..19, calm shallow 2-3px bottom thread)
  t1_deep    = rgba(14, 66, 82, 255),
  t1_body    = rgba(22, 98, 118, 255),
  t1_crest   = rgba(38, 134, 156, 255),
  t1_glint   = rgba(66, 168, 188, 255),
  -- Tier 2: Stream (20..49, full 5px smooth S-ribbons)
  t2_deep    = rgba(15, 76, 94, 255),
  t2_mid     = rgba(24, 118, 138, 255),
  t2_body    = rgba(36, 158, 180, 255),
  t2_vein    = rgba(74, 206, 222, 255),
  t2_blade   = rgba(112, 224, 236, 255),
  t2_tip     = rgba(176, 246, 252, 255),
  -- Tier 3: Torrent (50..100, 2x rapid sinuous blades & foam)
  t3_deep    = rgba(16, 86, 106, 255),
  t3_mid     = rgba(28, 134, 156, 255),
  t3_body    = rgba(46, 178, 198, 255),
  t3_surge   = rgba(94, 226, 238, 255),
  t3_blade   = rgba(168, 248, 254, 255),
  t3_foam    = rgba(232, 255, 255, 255),
  -- Text
  text        = rgba(210, 248, 252, 255),
  gain_text   = rgba(124, 242, 248, 255),
  text_shadow = rgba(8, 24, 30, 255),
}

-- ============================================================================
-- 18x18 FREEFORM CUTOUT ICONS (ZERO SQUARE BOX!)
-- ============================================================================

-- 1. MANA ICON (18x18): Ref3 #6 Crescent Moon + Ref2 D Azure Lotus Core + Ref2 B Cloud Tail
local function assert_sealed_contour(img, label, contour_alphas)
  local dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}
  for y = 0, img.height - 1 do
    for x = 0, img.width - 1 do
      local c = img:getPixel(x, y)
      if a_of(c) > 0 and not contour_alphas[c] then
        for _, d in ipairs(dirs) do
          local nx, ny = x + d[1], y + d[2]
          if nx >= 0 and ny >= 0 and nx < img.width and ny < img.height then
            if a_of(img:getPixel(nx, ny)) == 0 then
              print(string.format("SEAL_WARN: %s pixel (%d,%d) touches transparent (%d,%d)", label, x, y, nx, ny))
            end
          end
        end
      end
    end
  end
end

local MANA_ICON_ROWS = {
  ". . . . . . o o o o o o . . k . . .",
  ". . . . o o z w s s m b o k r k . .",
  ". . . o z z w s b n o o o r z r k .",
  ". . o z z w s b n o . o o k r k . .",
  ". o z z w s b n o . o l c o k . . .",
  ". o z w s b n o . o c x l o . . . .",
  "o z w s b n o . o c p x p c o . . .",
  "o z w s b n o o l c x x l c o o . .",
  "o z w s b n o o p l x x l g q g o .",
  "o w w s b n o . o p c l g q q q g o",
  ". o w s b n o . . o p g q q g q g o",
  ". o w s m b n o . . o o g q g o o .",
  ". . o w s m b n o o . . o o o o o .",
  ". . . o w s m m b n o o o s w b o .",
  ". . . . o w s s m m b b m s w o . .",
  ". . . . . o o s s s m m s b o . . .",
  ". . . . . . . o o b m s b o . . . .",
  ". . . . . . . . . o o o o . . . . .",
}

local MANA_ICON_FULL_ROWS = {
  ". . . . . . o o o o o o . . k . . .",
  ". . . . o o z z w s m b o k r k . .",
  ". . . o z z z w s b o o o r z r k .",
  ". . o z z w s b n o . o o k r k . .",
  ". o z z w s b n o o o x l o k . . .",
  ". o z w s b n o o c l x l c o . . .",
  "o z w s b n o o l p x x p l o . . .",
  "o z w s b n o o l x x x x l c o . .",
  "o z w s b n o o c l x x l g q g o .",
  "o w w s b n o . o p l l g q q q g o",
  ". o w s b n o . . o g q q q q q g o",
  ". o w s m b n o . . o o g q q o o .",
  ". . o w s m b n o o . . o o o o o .",
  ". . . o w s m m b n o o o z w b o .",
  ". . . . o w s s m m b b m s z o . .",
  ". . . . . o o s s s m m s b o . . .",
  ". . . . . . . o o b m s b o . . . .",
  ". . . . . . . . . o o o o . . . . .",
}

local function make_mana_icon(mode)
  local img = new_image(ICON_W, ICON_H)
  local pal
  local rows = (mode == "full") and MANA_ICON_FULL_ROWS or MANA_ICON_ROWS
  if mode == "idle" then
    pal = {
      o = rgba(18, 24, 42, 255),
      n = rgba(24, 34, 58, 255),
      b = rgba(40, 56, 94, 255),
      m = rgba(56, 78, 124, 255),
      s = rgba(76, 102, 152, 255),
      w = rgba(102, 122, 162, 255),
      z = rgba(124, 142, 178, 255),
      p = rgba(32, 52, 92, 255),
      c = rgba(46, 74, 126, 255),
      l = rgba(68, 102, 158, 255),
      x = rgba(98, 134, 188, 255),
      g = rgba(48, 70, 114, 255),
      q = rgba(68, 96, 146, 255),
      k = rgba(20, 28, 50, 180),
      r = rgba(76, 102, 150, 255),
    }
  elseif mode == "full" then
    pal = {
      o = rgba(22, 31, 56, 255),
      n = rgba(32, 48, 86, 255),
      b = rgba(68, 100, 164, 255),
      m = rgba(122, 162, 224, 255),
      s = rgba(190, 218, 252, 255),
      w = rgba(248, 238, 218, 255),
      z = rgba(255, 252, 240, 255),
      p = rgba(68, 122, 208, 255),
      c = rgba(126, 192, 255, 255),
      l = rgba(208, 240, 255, 255),
      x = rgba(255, 255, 255, 255),
      g = rgba(146, 192, 250, 255),
      q = rgba(234, 246, 255, 255),
      k = rgba(24, 36, 66, 190),
      r = rgba(210, 234, 255, 255),
    }
  else -- normal
    pal = {
      o = rgba(22, 31, 56, 255),
      n = rgba(30, 44, 79, 255),
      b = rgba(59, 89, 148, 255),
      m = rgba(110, 149, 214, 255),
      s = rgba(175, 203, 245, 255),
      w = rgba(242, 230, 206, 255),
      z = rgba(255, 248, 232, 255),
      p = rgba(53, 104, 186, 255),
      c = rgba(107, 178, 248, 255),
      l = rgba(184, 230, 255, 255),
      x = rgba(255, 255, 255, 255),
      g = rgba(125, 168, 232, 255),
      q = rgba(216, 236, 255, 255),
      k = rgba(24, 36, 66, 185),
      r = rgba(176, 210, 252, 255),
    }
  end
  stamp(img, 0, 0, rows, pal)
  assert_sealed_contour(img, "mana_icon_" .. mode, {[pal.o] = true, [pal.k] = true})
  return img
end

-- 2. WATER MAIN ICON (18x18): Ref3 #11 S-Curve Torrent + Ref3 #8 Three Upturned Blades
local WATER_MAIN_ROWS = {
  ". . . . . . . . . . h . . . . . . .",
  ". . . o o o o o . h w h . . . . . .",
  ". . o d m c l c o . h . . o o . . .",
  ". o d c l w w l c o o o o l w o . .",
  ". o o o o o o c l w w l c l o . . .",
  ". . . . . . . o o o o o o o . . . .",
  ". . o o o o o . . . . . . . . o o .",
  ". o d m c l w o o o . . . . o l w o",
  "o d c l w w w l c o o o o o c w w o",
  "o d m c l w w w w l l c c l w c o .",
  ". o o o o o o o c c l w w l c o . .",
  ". . . . . . . . o o o o o o o . . .",
  ". . . . o o o o . . . o o . . . . .",
  ". . . o d m c l o o o l w o . . . .",
  ". . o d c c l w w l l w o . . . . .",
  ". . . o o o o o o o o o . . . . . .",
  ". . . . h l h . . . . . . . . . . .",
  ". . . . . h . . . . . . . . . . . .",
}

-- 3. WATER ALT-8 ICON (18x18): Pure Ref3 #8 Three Staggered S-Stream Blades
local WATER_ALT8_ROWS = {
  ". . . . . . . . . . . . . . o o . .",
  ". . . o o o o o . . . . . o l w o .",
  ". . o d m c l w l o o o o c w l o .",
  ". o d c l l c c l w w l l w c o . .",
  ". . o o o o o o c l l c c o o . . .",
  ". . . . . . . . . o o o o . . . . .",
  ". . o o o o o . . . . . . . . o o .",
  ". o d m c l w l o o . . . . o l w o",
  "o d c l w l c c l w l o o c l w w o",
  ". o o o o o m c l w w l c l w c o .",
  ". . . . . . o o o c l l c c o o . .",
  ". . . . . . . . . o o o o o . . . .",
  ". . o o o o o . . . . . . . o o . .",
  ". o d m c l w l o o . . . o l w o .",
  "o d c l c c l w w l c c l w c o . .",
  ". o o o o o o c l l c c o o o . . .",
  ". . o c o . . . o o o o . . . . . .",
  ". . . o . . . . . . . . . . . . . .",
}

local function water_icon_palette(mode)
  if mode == "idle" then
    return {
      o = rgba(14, 38, 50, 240),
      h = rgba(14, 38, 50, 150),
      n = rgba(14, 38, 48, 255),
      d = rgba(18, 56, 70, 255),
      m = rgba(26, 78, 96, 255),
      c = rgba(38, 104, 124, 255),
      l = rgba(56, 132, 152, 255),
      w = rgba(84, 162, 180, 255),
    }
  elseif mode == "surge" then
    return {
      o = rgba(15, 44, 58, 242),
      h = rgba(15, 44, 58, 165),
      n = rgba(16, 56, 72, 255),
      d = rgba(22, 102, 124, 255),
      m = rgba(38, 158, 182, 255),
      c = rgba(82, 216, 232, 255),
      l = rgba(164, 248, 255, 255),
      w = rgba(244, 255, 255, 255),
    }
  else -- normal
    return {
      o = rgba(15, 44, 58, 242),
      h = rgba(15, 44, 58, 160),
      n = rgba(16, 52, 68, 255),
      d = rgba(20, 90, 112, 255),
      m = rgba(30, 138, 164, 255),
      c = rgba(62, 194, 214, 255),
      l = rgba(128, 236, 246, 255),
      w = rgba(224, 254, 255, 255),
    }
  end
end

local WATER_ALT11_ROWS = {
  ". . . . . . o o o o o o o o . . . .",
  ". . . . . o n d m c c l w l o . . .",
  ". . . . . . o o o o o m c w o . . .",
  ". . . . . . . . o o d c l c o . . .",
  ". . . . . o o o d m c l c o . . . .",
  ". . . o o l w c m c o o o . . . . .",
  ". . o w w l c m o o . . . . . . . .",
  ". . o w l c m c o o . . . . h . . .",
  ". . . o l w c m c l o o . h w h . .",
  ". . . . o o d m c l w l o o h . . .",
  ". . . . . . o o d m c w w l o o . .",
  ". . . h . . . . o d m c w l w o . .",
  ". . h l h . . o o d m c l w w o . .",
  ". . . h . o o d m c l w w l c o . .",
  ". . o o o d m c c l w w l c m o . .",
  ". o d d m c c l w w w l c m o . . .",
  ". o o o o o o o o o o o o o . . . .",
  ". . . . . . . . . . . . . . . . . .",
}

local function make_water_icon(mode, variant)
  local img = new_image(ICON_W, ICON_H)
  local rows = (variant == "alt11") and WATER_ALT11_ROWS or WATER_MAIN_ROWS
  local pal = water_icon_palette(mode)
  stamp(img, 0, 0, rows, pal)
  assert_sealed_contour(img, "water_icon_" .. (variant or "main") .. "_" .. mode, {[pal.o] = true, [pal.h] = true})
  return img
end

-- ============================================================================
-- CHINESE FRETWORK (MEANDER) TRACKS (102x9, 7px body + 9px proud fretwork caps)
-- ============================================================================

local LEFT_FRET_CAP = {
  ". . o o o o",
  ". . o h l o",
  ". o o o l l",
  "o l s o o d",
  "o h l s s d",
  "o d s o o d",
  ". o o o s s",
  ". . o s d o",
  ". . o o o o",
}

local RIGHT_FRET_CAP = {
  "o o o o . .",
  "o h l o . .",
  "l l o o o .",
  "d o o h l o",
  "d s s l h o",
  "d o o s d o",
  "s s o o o .",
  "o s d o . .",
  "o o o o . .",
}

local function draw_fretwork_track_base(img, is_water)
  local fret_pal = {
    o = PAL_FRAME.ink_outer,
    d = PAL_FRAME.slate_dark,
    s = PAL_FRAME.stone_rail,
    l = PAL_FRAME.fret_lit,
    h = PAL_FRAME.fret_hi,
  }
  stamp(img, 0, 0, LEFT_FRET_CAP, fret_pal)
  stamp(img, TRACK_W - 6, 0, RIGHT_FRET_CAP, fret_pal)

  local cav_sh  = is_water and PAL_WATER.cav_shadow or PAL_MANA.cav_shadow
  local cav_b1  = is_water and PAL_WATER.cav_bed_1  or PAL_MANA.cav_bed_1
  local cav_b2  = is_water and PAL_WATER.cav_bed_2  or PAL_MANA.cav_bed_2
  local cav_mk  = is_water and PAL_WATER.cav_notch  or PAL_MANA.cav_mark

  for tx = FILL_X_OFFSET, FILL_X_OFFSET + FILL_W - 1 do
    -- Continuous unbroken matte slate-stone rails (like Ref2 B & D), subtly darker right against the fretwork shoulders
    local dist_end = math.min(tx - FILL_X_OFFSET, (FILL_X_OFFSET + FILL_W - 1) - tx)
    local top_c = (dist_end == 0) and PAL_FRAME.ink_outer
               or ((dist_end <= 3) and PAL_FRAME.slate_dark or PAL_FRAME.stone_rail)
    local bot_c = (dist_end == 0) and PAL_FRAME.ink_outer or PAL_FRAME.slate_dark
    img:drawPixel(tx, 1, top_c)
    img:drawPixel(tx, 7, bot_c)

    -- 5px inner recessed cavity (ty=2..6)
    img:drawPixel(tx, 2, cav_sh)
    img:drawPixel(tx, 3, cav_b1)
    img:drawPixel(tx, 4, ((tx % 15 == 7) and cav_mk or cav_b1))
    img:drawPixel(tx, 5, cav_b2)
    img:drawPixel(tx, 6, cav_sh)
  end
  -- Subtle dark shadow bevel at extreme left/right chamber walls
  for ty = 2, 6 do
    img:drawPixel(FILL_X_OFFSET, ty, cav_sh)
    img:drawPixel(FILL_X_OFFSET + FILL_W - 1, ty, cav_sh)
  end
end

local function make_mana_track()
  local img = new_image(TRACK_W, TRACK_H)
  draw_fretwork_track_base(img, false)
  return img
end

-- Water 20 (tx=23) & 50 (tx=50) Chinese Cloud-Clasps
-- clasp_state: 0 (<20), 1 (20..49), 2 (>=50)
local function make_water_track(clasp_state)
  local img = new_image(TRACK_W, TRACK_H)
  draw_fretwork_track_base(img, true)

  local lit20 = (clasp_state or 0) >= 1
  local lit50 = (clasp_state or 0) >= 2
  local x20 = FILL_X_OFFSET + 18 - 1 -- tx = 23 (18px = 20% of 90px)
  local x50 = FILL_X_OFFSET + 45 - 1 -- tx = 50 (45px = 50% of 90px)

  -- 20/100 Top Cloud-Stud (ty=0..1, plus subtle chamber notch at ty=2)
  local e20 = lit20 and PAL_WATER.clasp_lit_edge or PAL_WATER.clasp_unlit_edge
  local c20 = lit20 and PAL_WATER.clasp_lit_20   or PAL_WATER.clasp_unlit_core
  local s20 = lit20 and lerp_color(PAL_WATER.clasp_lit_edge, PAL_WATER.clasp_lit_20, 0.55) or PAL_WATER.clasp_unlit_edge
  for dx = -2, 2 do
    put_pixel(img, x20 + dx, 0, PAL_WATER.clasp_ink)
  end
  if lit20 then put_pixel(img, x20, 0, PAL_WATER.clasp_lit_edge) end
  put_pixel(img, x20 - 2, 1, PAL_WATER.clasp_ink)
  put_pixel(img, x20 - 1, 1, s20)
  put_pixel(img, x20,     1, c20)
  put_pixel(img, x20 + 1, 1, s20)
  put_pixel(img, x20 + 2, 1, PAL_WATER.clasp_ink)
  put_pixel(img, x20,     2, PAL_WATER.cav_notch)

  -- 50/100 Symmetric Double-Biting Main Fretwork Clasp (ty=0..1 and ty=7..8)
  local e50 = lit50 and PAL_WATER.clasp_lit_edge or PAL_WATER.clasp_unlit_edge
  local c50 = lit50 and PAL_WATER.clasp_lit_50   or PAL_WATER.clasp_unlit_core
  local s50 = lit50 and PAL_WATER.clasp_lit_20   or PAL_WATER.clasp_unlit_edge
  for dx = -2, 2 do
    put_pixel(img, x50 + dx, 0, PAL_WATER.clasp_ink)
    put_pixel(img, x50 + dx, 8, PAL_WATER.clasp_ink)
  end
  put_pixel(img, x50, 0, lit50 and e50 or PAL_WATER.clasp_ink)
  put_pixel(img, x50, 8, lit50 and e50 or PAL_WATER.clasp_ink)

  put_pixel(img, x50 - 2, 1, PAL_WATER.clasp_ink)
  put_pixel(img, x50 - 1, 1, s50)
  put_pixel(img, x50,     1, c50)
  put_pixel(img, x50 + 1, 1, s50)
  put_pixel(img, x50 + 2, 1, PAL_WATER.clasp_ink)

  put_pixel(img, x50 - 2, 7, PAL_WATER.clasp_ink)
  put_pixel(img, x50 - 1, 7, s50)
  put_pixel(img, x50,     7, c50)
  put_pixel(img, x50 + 1, 7, s50)
  put_pixel(img, x50 + 2, 7, PAL_WATER.clasp_ink)

  put_pixel(img, x50, 2, PAL_WATER.cav_notch)
  put_pixel(img, x50, 6, PAL_WATER.cav_notch)
  return img
end

-- ============================================================================
-- 90x5 ANIMATED FILLS (16 SEAMLESS FRAMES @ 90ms)
-- ============================================================================

local function calc_pixels(val, max_val)
  if val <= 0 or max_val <= 0 then return 0 end
  if val >= max_val then return FILL_W end
  return math.max(1, math.min(FILL_W, math.floor(val / max_val * FILL_W + 0.5)))
end

-- 16x5 seamless Oriental Cloud Volute tile (Ref2 B rolling cloud + Ref2 D moonlight silk)
-- Tokens: d=deep, m=mid_dark, b=body, s=silk, l=cloud_lit, W=volute
local CLOUD_TILE_16x5 = {
  "b b s s l W W l s b b m b b s b",
  "s l l s l W b b W l s b s l l s",
  "b s b m b s l W l s b m m b s b",
  "m b s l l s b m m b s l l s b m",
  "d d m b b m d d d d m b b m d d",
}

local function parse_tile_16x5(rows)
  local grid = {}
  for y = 1, 5 do
    grid[y] = {}
    local clean = rows[y]:gsub(" ", "")
    for x = 1, 16 do
      grid[y][x] = clean:sub(x, x)
    end
  end
  return grid
end

local MANA_CLOUD_GRID = parse_tile_16x5(CLOUD_TILE_16x5)

local function make_mana_fill_frame(state, frame_idx)
  local img = new_image(FILL_W, FILL_H)
  local f0 = frame_idx - 1
  local phase = (state == "low") and 0.0 or (2.0 * math.pi * (f0 / FRAMES))

  local c_deep, c_mid, c_body, c_silk, c_lit, c_crest
  if state == "low" then
    c_deep  = PAL_MANA.low_deep
    c_mid   = PAL_MANA.low_mid
    c_body  = PAL_MANA.low_body
    c_silk  = PAL_MANA.low_silk
    c_lit   = PAL_MANA.low_lit
    c_crest = PAL_MANA.low_lit
  elseif state == "full" then
    local breath = 0.5 + 0.5 * math.sin(phase)
    c_deep  = PAL_MANA.full_deep
    c_mid   = PAL_MANA.full_mid
    c_body  = PAL_MANA.full_body
    c_silk  = PAL_MANA.full_silk
    c_lit   = lerp_color(PAL_MANA.full_lit, PAL_MANA.full_crest, 0.25 * breath)
    c_crest = lerp_color(PAL_MANA.full_lit, PAL_MANA.full_crest, 0.70 + 0.30 * breath)
  else -- normal
    c_deep  = PAL_MANA.deep
    c_mid   = PAL_MANA.mid_dark
    c_body  = PAL_MANA.body
    c_silk  = PAL_MANA.silk
    c_lit   = PAL_MANA.cloud_lit
    c_crest = PAL_MANA.volute
  end

  for x = 0, FILL_W - 1 do
    local x_norm = x / (FILL_W - 1)
    local swell = (state == "full") and (0.13 * x_norm) or (0.18 * x_norm)

    local u1 = 2.0 * math.pi * ((x + 5.0) / 38.0) - phase
    local skew1 = math.sin(u1 + 0.52 * math.sin(u1))
    local y_cloud = 1.85 - 1.35 * skew1
    local billow_head = math.max(0.0, math.sin(u1 - 0.45))

    local u2 = 2.0 * math.pi * ((x + 23.0) / 53.0) - phase
    local skew2 = math.sin(u2 - 0.36 * math.sin(u2))
    local y_silk = 2.65 + 1.15 * skew2

    for y = 0, FILL_H - 1 do
      local base_v
      if y == 0 then base_v = 0.38
      elseif y == 1 then base_v = 0.48
      elseif y == 2 then base_v = 0.45
      elseif y == 3 then base_v = 0.36
      else base_v = 0.24 end

      local d1 = math.abs(y - y_cloud)
      local w1 = 0.88 + 0.48 * (billow_head ^ 2)
      local r1 = math.max(0.0, 1.0 - (d1 / w1) ^ 1.65) * (0.48 + 0.20 * billow_head)

      -- Subtle Ref2 B azure cloud-curl shadow tuck directly beneath billowing cloud crests
      local under_d = y - (y_cloud + 1.25)
      local curl_tuck = 0.0
      if under_d > -0.55 and under_d < 0.75 and billow_head > 0.15 then
        curl_tuck = 0.20 * billow_head * (1.0 - math.abs(under_d) / 0.75)
      end

      local d2 = math.abs(y - y_silk)
      local r2 = math.max(0.0, 1.0 - (d2 / 0.85) ^ 1.8) * 0.36

      local v = base_v + swell + math.max(r1, r2 * 0.92) + 0.14 * math.min(r1, r2) - curl_tuck

      local c
      if v >= 0.96 then c = c_crest
      elseif v >= 0.80 then c = c_lit
      elseif v >= 0.63 then c = c_silk
      elseif v >= 0.45 then c = c_body
      elseif v >= 0.30 then c = c_mid
      else c = c_deep end

      if state == "full" and y == 1 then
        local glint_pos = (x - math.floor(f0 * 90 / 16 + 0.5)) % 45
        if glint_pos == 14 and v >= 0.72 then
          c = PAL_MANA.crown
        end
      end
      if x == 0 then
        c = lerp_color(c, c_deep, 0.25)
      end
      img:drawPixel(x, y, c)
    end
  end
  return img
end

-- Water 16x5 seamless stream tiles:
-- Tier 2 (20..49): Ref3 #8 smooth overlapping S-ribbons (1x speed)
local WATER_T2_TILE = {
  "d d m b v v L L v b m d d d d d",
  "m b v L L v b m m v L v b m d m",
  "v v b m m d m b v v b L L v b v",
  "b m d d d m b v L v m m b v b m",
  "d d d d m m b m m d d d d m m d",
}

-- Tier 3 (50..100): Ref3 #11+#8 rapid sinuous blades & foam threads (2x speed)
local WATER_T3_TILE = {
  "m m b s B F F B s b m d d m b m",
  "s B B F B s m m b s B F B s b s",
  "B s m d m b s s B F B s m d m B",
  "m d d m b s B F F B s m d d m s",
  "d d m m b m s b b m d d d m m d",
}

local WATER_T2_GRID = parse_tile_16x5(WATER_T2_TILE)
local WATER_T3_GRID = parse_tile_16x5(WATER_T3_TILE)

local function make_water_fill_frame(tier, frame_idx)
  local img = new_image(FILL_W, FILL_H)
  local f0 = frame_idx - 1
  local phase1 = 2.0 * math.pi * (f0 / FRAMES)
  local phase2 = 4.0 * math.pi * (f0 / FRAMES)

  if tier == 1 then
    -- 1..19 Trickle: occupies only bottom 2-3px (fy=2..4), top 2px stay dark chamber air
    for x = 0, FILL_W - 1 do
      local s = math.sin(2.0 * math.pi * (x / 32.0) - phase1)
      img:drawPixel(x, 0, PAL_WATER.cav_shadow)
      img:drawPixel(x, 1, PAL_WATER.cav_bed_1)
      if s > 0.15 then
        img:drawPixel(x, 2, (s > 0.72) and PAL_WATER.t1_glint or PAL_WATER.t1_crest)
      else
        img:drawPixel(x, 2, PAL_WATER.cav_bed_1)
      end
      img:drawPixel(x, 3, (s > -0.10) and PAL_WATER.t1_crest or PAL_WATER.t1_body)
      img:drawPixel(x, 4, PAL_WATER.t1_deep)
    end
  elseif tier == 2 then
    -- 20..49 Stream: full 5px height, 1x speed smooth Ref3 #8 S-ribbons
    for x = 0, FILL_W - 1 do
      local x_norm = x / (FILL_W - 1)
      local u1 = 2.0 * math.pi * (x / 36.0) - phase1
      local y_r1 = 1.35 - 0.82 * math.sin(u1 + 0.35 * math.sin(u1))
      local u2 = 2.0 * math.pi * ((x + 19.0) / 48.0) - phase1
      local y_r2 = 3.05 + 0.78 * math.sin(u2 - 0.30 * math.sin(u2))

      for y = 0, FILL_H - 1 do
        local base = (y == 4) and 0.16 or ((y == 0) and 0.30 or 0.38)
        local s1 = math.max(0.0, 1.0 - (math.abs(y - y_r1) / 0.90) ^ 1.8) * 0.50
        local s2 = math.max(0.0, 1.0 - (math.abs(y - y_r2) / 0.85) ^ 1.8) * 0.42
        local v = base + 0.12 * x_norm + math.max(s1, s2)

        local c
        if v >= 0.84 then c = PAL_WATER.t2_blade
        elseif v >= 0.68 then c = PAL_WATER.t2_vein
        elseif v >= 0.48 then c = PAL_WATER.t2_body
        elseif v >= 0.28 then c = PAL_WATER.t2_mid
        else c = PAL_WATER.t2_deep end

        if x == 0 then c = lerp_color(c, PAL_WATER.t2_deep, 0.30) end
        img:drawPixel(x, y, c)
      end
    end
  else
    -- 50..100 Torrent: full 5px height, 2x speed (2 cycles per 16f) sinuous blades & seafoam
    for x = 0, FILL_W - 1 do
      local x_norm = x / (FILL_W - 1)
      local u1 = 2.0 * math.pi * (x / 34.0) - phase2
      local wave1 = math.sin(u1 + 0.42 * math.sin(u1))
      local y_b1 = 1.30 - 0.85 * wave1

      local u2 = 2.0 * math.pi * ((x + 17.0) / 46.0) - phase2
      local wave2 = math.sin(u2 - 0.38 * math.sin(u2))
      local y_b2 = 2.95 + 0.85 * wave2

      for y = 0, FILL_H - 1 do
        local base = (y == 4) and 0.20 or ((y == 0) and 0.34 or 0.42)
        local s1 = math.max(0.0, 1.0 - (math.abs(y - y_b1) / 0.92) ^ 1.7) * 0.56
        local s2 = math.max(0.0, 1.0 - (math.abs(y - y_b2) / 0.88) ^ 1.7) * 0.48
        local v = base + 0.16 * x_norm + math.max(s1, s2) + 0.10 * math.min(s1, s2)

        local c
        if v >= 0.95 and x >= 18 then c = PAL_WATER.t3_foam
        elseif v >= 0.82 then c = PAL_WATER.t3_blade
        elseif v >= 0.65 then c = PAL_WATER.t3_surge
        elseif v >= 0.45 then c = PAL_WATER.t3_body
        elseif v >= 0.26 then c = PAL_WATER.t3_mid
        else c = PAL_WATER.t3_deep end

        if x == 0 then c = lerp_color(c, PAL_WATER.t3_deep, 0.25) end
        img:drawPixel(x, y, c)
      end
    end
  end
  return img
end

-- ============================================================================
-- COMPOSITE BAR RENDERERS (WITH CLOUD CROWN & WATER STREAM SPEARHEADS)
-- ============================================================================

local function render_mana_bar(dst, x, y, val, max_val, frame_idx, afterglow_val)
  local px = calc_pixels(val, max_val)
  local is_full = (px >= FILL_W and val >= max_val)
  local is_low  = (px > 0 and (val / math.max(1, max_val)) <= 0.22)
  local icon_mode = (px == 0) and "idle" or (is_full and "full" or "normal")
  local fill_mode = is_full and "full" or (is_low and "low" or "normal")

  local track_x = x + ICON_W - ICON_OVERLAP -- x + 15
  local track_y = y + 4
  local fill_x  = track_x + FILL_X_OFFSET   -- x + 21
  local fill_y  = track_y + FILL_Y_OFFSET   -- y + 6

  blit(dst, make_mana_track(), track_x, track_y)
  blit(dst, make_mana_icon(icon_mode), x, y)

  -- Optional dispersing moon-mist afterglow on cast spend (e.g. 48<-68)
  if afterglow_val and afterglow_val > val then
    local g_px = calc_pixels(afterglow_val, max_val)
    for gx = px, g_px - 1 do
      local t = (gx - px) / math.max(1, g_px - px)
      local c_top = lerp_color(PAL_MANA.ghost_hi, PAL_MANA.cav_bed_1, t * 0.72)
      local c_mid = lerp_color(PAL_MANA.ghost_mid, PAL_MANA.cav_bed_1, t * 0.65)
      local c_bot = lerp_color(PAL_MANA.ghost_low, PAL_MANA.cav_bed_2, t * 0.75)
      put_pixel(dst, fill_x + gx, fill_y + 0, c_bot)
      put_pixel(dst, fill_x + gx, fill_y + 1, c_top)
      put_pixel(dst, fill_x + gx, fill_y + 2, c_mid)
      put_pixel(dst, fill_x + gx, fill_y + 3, c_bot)
    end
  end

  if px <= 0 then return end

  local fill = make_mana_fill_frame(fill_mode, frame_idx)
  blit(dst, fill, fill_x, fill_y, px, FILL_H)

  -- Right liquid meniscus: Plump Gathering Cloud Crown (Ref2 B & D rounded bright cloud head!)
  if is_full then
    for dy = 0, FILL_H - 1 do
      local edge_c = (dy >= 1 and dy <= 3) and PAL_MANA.crown or PAL_MANA.full_lit
      put_pixel(dst, fill_x + FILL_W - 1, fill_y + dy, edge_c)
      if dy >= 1 and dy <= 3 then
        put_pixel(dst, fill_x + FILL_W - 2, fill_y + dy, PAL_MANA.full_crest)
      end
    end
  elseif is_low then
    put_pixel(dst, fill_x + px - 1, fill_y + 0, PAL_MANA.cav_shadow)
    put_pixel(dst, fill_x + px - 1, fill_y + 1, PAL_MANA.low_lit)
    put_pixel(dst, fill_x + px - 1, fill_y + 2, PAL_MANA.low_silk)
    put_pixel(dst, fill_x + px - 1, fill_y + 3, PAL_MANA.low_body)
    put_pixel(dst, fill_x + px - 1, fill_y + 4, PAL_MANA.cav_shadow)
  else
    -- Rounded cloud-head meniscus crown
    put_pixel(dst, fill_x + px - 1, fill_y + 0, PAL_MANA.cav_shadow)
    put_pixel(dst, fill_x + px - 1, fill_y + 1, PAL_MANA.volute)
    put_pixel(dst, fill_x + px - 1, fill_y + 2, PAL_MANA.crown)
    put_pixel(dst, fill_x + px - 1, fill_y + 3, PAL_MANA.cloud_lit)
    put_pixel(dst, fill_x + px - 1, fill_y + 4, PAL_MANA.cav_bed_2)
    if px >= 3 then
      put_pixel(dst, fill_x + px - 2, fill_y + 0, PAL_MANA.cloud_lit)
      put_pixel(dst, fill_x + px - 2, fill_y + 1, PAL_MANA.crown)
      put_pixel(dst, fill_x + px - 2, fill_y + 2, PAL_MANA.volute)
      put_pixel(dst, fill_x + px - 2, fill_y + 3, PAL_MANA.cloud_lit)
      put_pixel(dst, fill_x + px - 2, fill_y + 4, PAL_MANA.body)
      put_pixel(dst, fill_x + px - 3, fill_y + 1, PAL_MANA.volute)
      put_pixel(dst, fill_x + px - 3, fill_y + 2, PAL_MANA.volute)
    end
  end
end

local function render_water_bar(dst, x, y, val, frame_idx, icon_variant)
  local px = calc_pixels(val, 100)
  local clasp_state = (val >= 50) and 2 or ((val >= 20) and 1 or 0)
  local icon_mode = (px == 0) and "idle" or ((val >= 50) and "surge" or "normal")

  local track_x = x + ICON_W - ICON_OVERLAP
  local track_y = y + 4
  local fill_x  = track_x + FILL_X_OFFSET
  local fill_y  = track_y + FILL_Y_OFFSET

  blit(dst, make_water_track(clasp_state), track_x, track_y)
  blit(dst, make_water_icon(icon_mode, icon_variant), x, y)

  if px <= 0 then return end

  local tier = (val >= 50) and 3 or ((val >= 20) and 2 or 1)
  local fill = make_water_fill_frame(tier, frame_idx)
  blit(dst, fill, fill_x, fill_y, px, FILL_H)

  local f0 = frame_idx - 1
  if tier == 1 then
    -- Low rounded trickle tongue (2px tall at bottom)
    put_pixel(dst, fill_x + px - 1, fill_y + 0, PAL_WATER.cav_shadow)
    put_pixel(dst, fill_x + px - 1, fill_y + 1, PAL_WATER.cav_bed_1)
    put_pixel(dst, fill_x + px - 1, fill_y + 2, PAL_WATER.cav_bed_1)
    put_pixel(dst, fill_x + px - 1, fill_y + 3, PAL_WATER.t1_crest)
    put_pixel(dst, fill_x + px - 1, fill_y + 4, PAL_WATER.t1_body)
    if px >= 2 then
      put_pixel(dst, fill_x + px - 2, fill_y + 2, PAL_WATER.t1_body)
      put_pixel(dst, fill_x + px - 2, fill_y + 3, PAL_WATER.t1_glint)
    end
  elseif tier == 2 then
    -- Streamlined Ref3 #8 upturned single blade tip
    local bob = ((f0 % 8) < 4) and 0 or 1
    put_pixel(dst, fill_x + px - 1, fill_y + 0, (bob == 0) and PAL_WATER.t2_tip or PAL_WATER.cav_shadow)
    put_pixel(dst, fill_x + px - 1, fill_y + 1, PAL_WATER.t2_tip)
    put_pixel(dst, fill_x + px - 1, fill_y + 2, PAL_WATER.t2_blade)
    put_pixel(dst, fill_x + px - 1, fill_y + 3, PAL_WATER.cav_bed_1)
    put_pixel(dst, fill_x + px - 1, fill_y + 4, PAL_WATER.cav_shadow)
    if px >= 3 then
      put_pixel(dst, fill_x + px - 2, fill_y + 1, PAL_WATER.t2_blade)
      put_pixel(dst, fill_x + px - 2, fill_y + 2, PAL_WATER.t2_tip)
      put_pixel(dst, fill_x + px - 2, fill_y + 3, PAL_WATER.t2_vein)
      put_pixel(dst, fill_x + px - 2, fill_y + 4, PAL_WATER.cav_bed_2)
      put_pixel(dst, fill_x + px - 3, fill_y + 2, PAL_WATER.t2_blade)
    end
  else
    -- Tier 3 (>=50): Surging stacked water-blade spearhead (or full-seal surge at 100)
    if px >= FILL_W then
      local pulse = ((f0 * 2) % 8 < 4)
      for dy = 0, FILL_H - 1 do
        local c_end = (dy <= 2) and PAL_WATER.t3_foam or (pulse and PAL_WATER.t3_blade or PAL_WATER.t3_surge)
        put_pixel(dst, fill_x + FILL_W - 1, fill_y + dy, c_end)
      end
      put_pixel(dst, fill_x + FILL_W - 2, fill_y + 0, PAL_WATER.t3_foam)
      put_pixel(dst, fill_x + FILL_W - 2, fill_y + 1, pulse and PAL_WATER.t3_foam or PAL_WATER.t3_blade)
      put_pixel(dst, fill_x + FILL_W - 2, fill_y + 2, PAL_WATER.t3_blade)
    else
      local p4 = f0 % 4
      -- Sharp staggered double-blade front inside 5px height
      put_pixel(dst, fill_x + px - 1, fill_y + 0, (p4 <= 1) and PAL_WATER.t3_foam or PAL_WATER.cav_shadow)
      put_pixel(dst, fill_x + px - 1, fill_y + 1, PAL_WATER.t3_foam)
      put_pixel(dst, fill_x + px - 1, fill_y + 2, PAL_WATER.cav_bed_1)
      put_pixel(dst, fill_x + px - 1, fill_y + 3, (p4 >= 2) and PAL_WATER.t3_foam or PAL_WATER.t3_blade)
      put_pixel(dst, fill_x + px - 1, fill_y + 4, PAL_WATER.cav_shadow)
      if px >= 4 then
        put_pixel(dst, fill_x + px - 2, fill_y + 0, PAL_WATER.t3_blade)
        put_pixel(dst, fill_x + px - 2, fill_y + 1, PAL_WATER.t3_foam)
        put_pixel(dst, fill_x + px - 2, fill_y + 2, PAL_WATER.t3_surge)
        put_pixel(dst, fill_x + px - 2, fill_y + 3, PAL_WATER.t3_foam)
        put_pixel(dst, fill_x + px - 2, fill_y + 4, PAL_WATER.t3_mid)
        put_pixel(dst, fill_x + px - 3, fill_y + 1, PAL_WATER.t3_blade)
        put_pixel(dst, fill_x + px - 3, fill_y + 2, PAL_WATER.t3_foam)
        put_pixel(dst, fill_x + px - 3, fill_y + 3, PAL_WATER.t3_surge)
      end
    end
  end
end

-- ============================================================================
-- EXPORT 1x NATIVE COMPONENTS & MASTER EDITABLE .ASEPRITE
-- ============================================================================

local function make_strip_16(gen_fn)
  local strip = new_image(FILL_W, FILL_H * FRAMES)
  for f = 1, FRAMES do
    blit(strip, gen_fn(f), 0, (f - 1) * FILL_H)
  end
  return strip
end

local mana_icon_idle = make_mana_icon("idle")
local mana_icon_norm = make_mana_icon("normal")
local mana_icon_full = make_mana_icon("full")
local mana_track     = make_mana_track()
local mana_fill_low  = make_mana_fill_frame("low", 1)
local mana_fill_norm_strip = make_strip_16(function(f) return make_mana_fill_frame("normal", f) end)
local mana_fill_full_strip = make_strip_16(function(f) return make_mana_fill_frame("full", f) end)

local water_icon_idle       = make_water_icon("idle", "main")
local water_icon_norm       = make_water_icon("normal", "main")
local water_icon_surge      = make_water_icon("surge", "main")
local water_icon_alt11_idle  = make_water_icon("idle", "alt11")
local water_icon_alt11_norm  = make_water_icon("normal", "alt11")
local water_icon_alt11_surge = make_water_icon("surge", "alt11")

local water_track_0  = make_water_track(0)
local water_track_20 = make_water_track(1)
local water_track_50 = make_water_track(2)
local water_track_states = new_image(TRACK_W, TRACK_H * 3)
blit(water_track_states, water_track_0,  0, 0)
blit(water_track_states, water_track_20, 0, TRACK_H)
blit(water_track_states, water_track_50, 0, TRACK_H * 2)

local water_fill_t1_strip = make_strip_16(function(f) return make_water_fill_frame(1, f) end)
local water_fill_t2_strip = make_strip_16(function(f) return make_water_fill_frame(2, f) end)
local water_fill_t3_strip = make_strip_16(function(f) return make_water_fill_frame(3, f) end)

mana_icon_idle:saveAs(exp_dir .. "/mana_icon_idle.png")
mana_icon_norm:saveAs(exp_dir .. "/mana_icon.png")
mana_icon_full:saveAs(exp_dir .. "/mana_icon_full.png")
mana_track:saveAs(exp_dir .. "/mana_track.png")
mana_fill_low:saveAs(exp_dir .. "/mana_fill_low.png")
mana_fill_norm_strip:saveAs(exp_dir .. "/mana_fill_strip.png")
mana_fill_full_strip:saveAs(exp_dir .. "/mana_fill_full_strip.png")

os.remove(exp_dir .. "/water_icon_alt8.png")
os.remove(exp_dir .. "/water_icon_alt8_idle.png")
os.remove(exp_dir .. "/water_icon_alt8_surge.png")

water_icon_idle:saveAs(exp_dir .. "/water_icon_idle.png")
water_icon_norm:saveAs(exp_dir .. "/water_icon.png")
water_icon_surge:saveAs(exp_dir .. "/water_icon_surge.png")
water_icon_alt11_idle:saveAs(exp_dir .. "/water_icon_alt11_idle.png")
water_icon_alt11_norm:saveAs(exp_dir .. "/water_icon_alt11.png")
water_icon_alt11_surge:saveAs(exp_dir .. "/water_icon_alt11_surge.png")

water_track_0:saveAs(exp_dir .. "/water_track.png")
water_track_states:saveAs(exp_dir .. "/water_track_states.png")
water_fill_t1_strip:saveAs(exp_dir .. "/water_fill_tier1_trickle_strip.png")
water_fill_t2_strip:saveAs(exp_dir .. "/water_fill_tier2_stream_strip.png")
water_fill_t3_strip:saveAs(exp_dir .. "/water_fill_tier3_torrent_strip.png")

-- Master multi-layer 16-frame editable .aseprite file for all v4 components
do
  local mw, mh = 286, 118
  local master = Sprite(mw, mh, ColorMode.RGB)
  master.frames[1].duration = FRAME_DURATION
  for f = 2, FRAMES do
    local nf = master:newEmptyFrame()
    nf.duration = FRAME_DURATION
  end

  local l_bg       = master.layers[1]; l_bg.name = "ref_dark_backing"
  local l_m_static = master:newLayer(); l_m_static.name = "mana_icons_and_fret_track"
  local l_m_fill   = master:newLayer(); l_m_fill.name   = "mana_cloud_fills_16f"
  local l_w_static = master:newLayer(); l_w_static.name = "water_icons_and_fret_tracks"
  local l_w_fill   = master:newLayer(); l_w_fill.name   = "water_stream_tiers_16f"

  local bg_img = new_image(mw, mh, rgba(16, 18, 24, 255))
  local m_static = new_image(mw, mh)
  blit(m_static, mana_icon_idle, 6, 6)
  blit(m_static, mana_icon_norm, 28, 6)
  blit(m_static, mana_icon_full, 50, 6)
  blit(m_static, mana_track, 74, 10)
  blit(m_static, mana_track, 180, 10)

  local w_static = new_image(mw, mh)
  -- Main 8+11 icons
  blit(w_static, water_icon_idle,  6, 32)
  blit(w_static, water_icon_norm,  28, 32)
  blit(w_static, water_icon_surge, 50, 32)
  -- Alt pure #8 icons
  blit(w_static, water_icon_alt11_idle,  6, 56)
  blit(w_static, water_icon_alt11_norm,  28, 56)
  blit(w_static, water_icon_alt11_surge, 50, 56)
  -- 3 water track clasp states
  blit(w_static, water_track_0,  74, 34)
  blit(w_static, water_track_20, 74, 52)
  blit(w_static, water_track_50, 74, 70)
  -- Extra preview row for low mana static
  blit(m_static, mana_track, 74, 92)

  for f = 1, FRAMES do
    master:newCel(l_bg, f, bg_img, Point(0, 0))
    master:newCel(l_m_static, f, m_static, Point(0, 0))
    master:newCel(l_w_static, f, w_static, Point(0, 0))

    local mf = new_image(mw, mh)
    blit(mf, make_mana_fill_frame("normal", f), 80, 12)
    blit(mf, make_mana_fill_frame("full", f),   186, 12)
    blit(mf, mana_fill_low,                     80, 94)
    master:newCel(l_m_fill, f, mf, Point(0, 0))

    local wf = new_image(mw, mh)
    blit(wf, make_water_fill_frame(1, f), 80, 36)
    blit(wf, make_water_fill_frame(2, f), 80, 54)
    blit(wf, make_water_fill_frame(3, f), 80, 72)
    master:newCel(l_w_fill, f, wf, Point(0, 0))
  end

  master:saveAs(src_dir .. "/hud_bars_v4_components.aseprite")

  local overview = new_image(mw, mh, rgba(16, 18, 24, 255))
  blit(overview, m_static, 0, 0)
  blit(overview, make_mana_fill_frame("normal", 1), 80, 12)
  blit(overview, make_mana_fill_frame("full", 1),   186, 12)
  blit(overview, mana_fill_low,                     80, 94)
  blit(overview, w_static, 0, 0)
  blit(overview, make_water_fill_frame(1, 1), 80, 36)
  blit(overview, make_water_fill_frame(2, 1), 80, 54)
  blit(overview, make_water_fill_frame(3, 1), 80, 72)
  overview:saveAs(exp_dir .. "/components_overview_sheet.png")
  master:close()
end

-- ============================================================================
-- SIMULATED HUD VIEWPORT BACKGROUNDS (DAYTIME SKY+TERRAIN & DARK CAVE)
-- ============================================================================

local function paint_daytime_viewport(img, ox, oy, w, h)
  for y = 0, h - 1 do
    for x = 0, w - 1 do
      local c
      if y < math.floor(h * 0.56) then
        local t = y / math.max(1, math.floor(h * 0.56))
        c = lerp_color(rgba(116, 152, 198, 255), rgba(154, 184, 216, 255), t)
        if y >= 12 and y <= 23 and ((x + 16) % 68 < 30) then
          c = lerp_color(c, rgba(216, 228, 240, 255), (y <= 16) and 0.55 or 0.35)
        end
      elseif y < math.floor(h * 0.68) then
        c = ((x * 7 + 13) % 19 < 10) and rgba(82, 108, 78, 255) or rgba(74, 98, 72, 255)
      else
        local hash = (x * 17 + y * 31) % 13
        if y < math.floor(h * 0.74) then
          c = (hash < 4) and rgba(92, 126, 74, 255) or rgba(82, 114, 66, 255)
        else
          c = (hash < 5) and rgba(72, 64, 54, 255) or rgba(64, 56, 48, 255)
        end
      end
      put_pixel(img, ox + x, oy + y, c)
    end
  end
end

local function paint_cave_viewport(img, ox, oy, w, h)
  for y = 0, h - 1 do
    for x = 0, w - 1 do
      local bx = math.floor(x / 8)
      local by = math.floor(y / 8)
      local hash = (x * 23 + y * 41 + bx * 7 + by * 13) % 17
      local t = y / math.max(1, h)
      local base = lerp_color(rgba(22, 24, 30, 255), rgba(14, 16, 21, 255), t)
      if hash == 0 or hash == 1 then
        base = lerp_color(base, rgba(34, 37, 45, 255), 0.7)
      elseif hash == 5 then
        base = lerp_color(base, rgba(10, 11, 15, 255), 0.7)
      end
      put_pixel(img, ox + x, oy + y, base)
    end
  end
end

local function draw_vanilla_hud_ref(img, ox, oy, w, h)
  local bar_w = 162
  local left = ox + math.floor((w - bar_w) / 2)
  local bot_y = oy + h - 22
  for i = 0, 8 do
    local sx = left + i * 18
    fill_rect(img, sx, bot_y, 18, 18, rgba(22, 22, 24, 210))
    stroke_rect(img, sx, bot_y, 18, 18, (i == 0) and rgba(210, 214, 220, 255) or rgba(78, 80, 86, 255))
  end
  local xp_y = bot_y - 7
  fill_rect(img, left, xp_y, bar_w, 5, rgba(16, 22, 14, 230))
  stroke_rect(img, left, xp_y, bar_w, 5, rgba(10, 12, 10, 255))
  fill_rect(img, left + 1, xp_y + 1, 96, 3, rgba(96, 182, 68, 255))
  fill_rect(img, left + 1, xp_y + 1, 96, 1, rgba(154, 226, 112, 255))
  local status_y = xp_y - 9
  for i = 0, 9 do
    stamp(img, left + i * 8, status_y, {
      ". # . # .",
      "# # # # #",
      "# # # # #",
      ". # # # .",
      ". . # . .",
    }, {["#"] = (i < 8) and rgba(188, 48, 48, 255) or rgba(52, 28, 28, 255)})
    stamp(img, left + bar_w - 5 - i * 8, status_y, {
      ". . # # .",
      ". # # # .",
      "# # # . .",
      "# # . . .",
      "# . . . .",
    }, {["#"] = (i < 9) and rgba(164, 108, 56, 255) or rgba(52, 38, 26, 255)})
  end
end

local function render_hud_cluster(dst, ox, oy, mana_val, mana_max, water_val, frame_idx, water_variant, show_change)
  -- Mana icon at (6, 3), track at (21, 7), fill at (27, 9), text at (127, 9)
  render_mana_bar(dst, ox + 6, oy + 3, mana_val, mana_max, frame_idx, nil)
  draw_text(dst, ox + 127, oy + 9, string.format("%d/%d", mana_val, mana_max), PAL_MANA.text, PAL_MANA.text_shadow)

  -- Water icon at (6, 22), track at (21, 26), fill at (27, 28), text at (127, 28)
  render_water_bar(dst, ox + 6, oy + 22, water_val, frame_idx, water_variant or "main")
  draw_text(dst, ox + 127, oy + 28, string.format("%d/100", water_val), PAL_WATER.text, PAL_WATER.text_shadow)

  if show_change then
    draw_text(dst, ox + 27, oy + 38, "+12", PAL_WATER.gain_text, PAL_WATER.text_shadow)
  end
end

-- ============================================================================
-- PREVIEW 1: icons_closeup_8x.png
-- ============================================================================
do
  local v2_mana  = Image{fromFile = v2_dir .. "/mana_icon.png"}
  local v2_water = Image{fromFile = v2_dir .. "/water_icon.png"}

  -- Build directly at final crisp pixel scale (board 1x = 248x148 -> 8x for icons, 2x/4x for headers)
  local cw, ch = 1760, 1240
  local canvas = new_image(cw, ch, rgba(14, 16, 22, 255))

  -- Header bar
  fill_rect(canvas, 0, 0, cw, 56, rgba(22, 26, 36, 255))
  stroke_rect(canvas, 0, 0, cw, 56, rgba(44, 54, 74, 255))
  local h_txt = new_image(430, 8)
  draw_text(h_txt, 0, 1, "V4.1 ICON REDESIGN (18x18 CUTOUT SILHOUETTE @ 8x) VS OLD V2 (9x9)", rgba(228, 236, 250, 255), rgba(8, 10, 16, 255))
  blit(canvas, scale_nn(h_txt, 4), 28, 16)

  local cards = {
    {x = 24,   w = 340, title = "OLD V2 (9x9)",          sub = "BOXY DIAMOND & STATIC DROP", border = rgba(68, 74, 88, 255)},
    {x = 384,  w = 436, title = "MANA: MOON + LOTUS",    sub = "REF 6 CRESCENT + D CORE + B CLOUD", border = rgba(72, 102, 166, 255)},
    {x = 840,  w = 436, title = "WATER MAIN: REF #8",    sub = "SPINDLE 3-STREAM WILLOW BLADES", border = rgba(46, 128, 148, 255)},
    {x = 1296, w = 440, title = "WATER ALT: REF #11",    sub = "PERSPECTIVE S-RIVER TORRENT", border = rgba(46, 128, 148, 255)},
  }

  for _, card in ipairs(cards) do
    fill_rect(canvas, card.x, 72, card.w, 824, rgba(20, 23, 31, 255))
    stroke_rect(canvas, card.x, 72, card.w, 824, card.border)
    local t1 = new_image(140, 7)
    draw_text(t1, 0, 1, card.title, rgba(232, 240, 255, 255), rgba(8, 10, 16, 255))
    blit(canvas, scale_nn(t1, 3), card.x + 16, 86)
    local t2 = new_image(180, 7)
    draw_text(t2, 0, 1, card.sub, rgba(158, 174, 202, 255), nil)
    blit(canvas, scale_nn(t2, 2), card.x + 16, 114)
  end

  local function draw_icon_showcase_row(card_x, row_y, label_str, icon_img)
    local lbl = new_image(80, 7)
    draw_text(lbl, 0, 1, label_str, rgba(196, 210, 234, 255), rgba(8, 10, 16, 255))
    blit(canvas, scale_nn(lbl, 2), card_x + 18, row_y + 2)

    -- Dark cave swatch (168x168) + Daytime sky swatch (168x168)
    local sw_w, sw_h = 188, 162
    local sw_dark_x = card_x + 20
    local sw_sky_x  = card_x + 224
    local sw_y      = row_y + 22
    fill_rect(canvas, sw_dark_x, sw_y, sw_w, sw_h, rgba(15, 18, 25, 255))
    stroke_rect(canvas, sw_dark_x, sw_y, sw_w, sw_h, rgba(42, 50, 66, 255))

    fill_rect(canvas, sw_sky_x, sw_y, sw_w, sw_h, rgba(132, 166, 204, 255))
    fill_rect(canvas, sw_sky_x, sw_y + 92, sw_w, sw_h - 92, rgba(158, 188, 218, 255))
    stroke_rect(canvas, sw_sky_x, sw_y, sw_w, sw_h, rgba(64, 84, 108, 255))

    local up8 = scale_nn(icon_img, 8)
    local pad_x = math.floor((sw_w - up8.width) / 2)
    local pad_y = math.floor((sw_h - up8.height) / 2)
    blit(canvas, up8, sw_dark_x + pad_x, sw_y + pad_y)
    blit(canvas, up8, sw_sky_x + pad_x,  sw_y + pad_y)

    -- 1x & 2x corner badges
    local bar_y = sw_y + sw_h + 6
    local bar_h = 46
    fill_rect(canvas, sw_dark_x, bar_y, sw_w, bar_h, rgba(14, 17, 24, 255))
    stroke_rect(canvas, sw_dark_x, bar_y, sw_w, bar_h, rgba(36, 44, 58, 255))
    fill_rect(canvas, sw_sky_x, bar_y, sw_w, bar_h, rgba(138, 172, 208, 255))
    stroke_rect(canvas, sw_sky_x, bar_y, sw_w, bar_h, rgba(64, 84, 108, 255))

    local tag1 = new_image(16, 7); draw_text(tag1, 0, 1, "1x", rgba(168, 184, 210, 255), rgba(8, 10, 16, 255))
    local tag2 = new_image(16, 7); draw_text(tag2, 0, 1, "2x", rgba(168, 184, 210, 255), rgba(8, 10, 16, 255))
    local tag1_sky = new_image(16, 7); draw_text(tag1_sky, 0, 1, "1x", rgba(242, 248, 255, 255), rgba(20, 30, 48, 255))
    local tag2_sky = new_image(16, 7); draw_text(tag2_sky, 0, 1, "2x", rgba(242, 248, 255, 255), rgba(20, 30, 48, 255))
    local up2 = scale_nn(icon_img, 2)

    blit(canvas, scale_nn(tag1, 2), sw_dark_x + 12, bar_y + 16)
    blit(canvas, icon_img,          sw_dark_x + 44, bar_y + math.floor((bar_h - icon_img.height) / 2))
    blit(canvas, scale_nn(tag2, 2), sw_dark_x + 88, bar_y + 16)
    blit(canvas, up2,               sw_dark_x + 122, bar_y + math.floor((bar_h - up2.height) / 2))

    blit(canvas, scale_nn(tag1_sky, 2), sw_sky_x + 12, bar_y + 16)
    blit(canvas, icon_img,              sw_sky_x + 44, bar_y + math.floor((bar_h - icon_img.height) / 2))
    blit(canvas, scale_nn(tag2_sky, 2), sw_sky_x + 88, bar_y + 16)
    blit(canvas, up2,                   sw_sky_x + 122, bar_y + math.floor((bar_h - up2.height) / 2))
  end

  -- Old v2 card
  do
    local cx = cards[1].x
    local lbl1 = new_image(90, 7); draw_text(lbl1, 0, 1, "V2 MANA (9x9 @ 8x)", rgba(210, 198, 164, 255), nil)
    blit(canvas, scale_nn(lbl1, 2), cx + 20, 154)
    fill_rect(canvas, cx + 20, 180, 140, 140, rgba(15, 18, 25, 255))
    stroke_rect(canvas, cx + 20, 180, 140, 140, rgba(50, 56, 70, 255))
    fill_rect(canvas, cx + 176, 180, 140, 140, rgba(134, 168, 206, 255))
    stroke_rect(canvas, cx + 176, 180, 140, 140, rgba(64, 84, 108, 255))
    local m8 = scale_nn(v2_mana, 8)
    blit(canvas, m8, cx + 20 + 34, 180 + 34)
    blit(canvas, m8, cx + 176 + 34, 180 + 34)
    fill_rect(canvas, cx + 20, 326, 140, 34, rgba(14, 17, 24, 255))
    stroke_rect(canvas, cx + 20, 326, 140, 34, rgba(40, 46, 58, 255))
    blit(canvas, v2_mana, cx + 48, 338)
    blit(canvas, scale_nn(v2_mana, 2), cx + 92, 334)
    fill_rect(canvas, cx + 176, 326, 140, 34, rgba(138, 172, 208, 255))
    stroke_rect(canvas, cx + 176, 326, 140, 34, rgba(64, 84, 108, 255))
    blit(canvas, v2_mana, cx + 204, 338)
    blit(canvas, scale_nn(v2_mana, 2), cx + 248, 334)

    local lbl2 = new_image(95, 7); draw_text(lbl2, 0, 1, "V2 WATER (9x9 @ 8x)", rgba(164, 208, 220, 255), nil)
    blit(canvas, scale_nn(lbl2, 2), cx + 20, 396)
    fill_rect(canvas, cx + 20, 422, 140, 140, rgba(15, 18, 25, 255))
    stroke_rect(canvas, cx + 20, 422, 140, 140, rgba(50, 56, 70, 255))
    fill_rect(canvas, cx + 176, 422, 140, 140, rgba(134, 168, 206, 255))
    stroke_rect(canvas, cx + 176, 422, 140, 140, rgba(64, 84, 108, 255))
    local w8 = scale_nn(v2_water, 8)
    blit(canvas, w8, cx + 20 + 34, 422 + 34)
    blit(canvas, w8, cx + 176 + 34, 422 + 34)
    fill_rect(canvas, cx + 20, 568, 140, 34, rgba(14, 17, 24, 255))
    stroke_rect(canvas, cx + 20, 568, 140, 34, rgba(40, 46, 58, 255))
    blit(canvas, v2_water, cx + 48, 580)
    blit(canvas, scale_nn(v2_water, 2), cx + 92, 576)
    fill_rect(canvas, cx + 176, 568, 140, 34, rgba(138, 172, 208, 255))
    stroke_rect(canvas, cx + 176, 568, 140, 34, rgba(64, 84, 108, 255))
    blit(canvas, v2_water, cx + 204, 580)
    blit(canvas, scale_nn(v2_water, 2), cx + 248, 576)
  end

  -- Mana 3 states
  draw_icon_showcase_row(cards[2].x, 140, "NORMAL (1..79)", mana_icon_norm)
  draw_icon_showcase_row(cards[2].x, 388, "FULL (80/80)",   mana_icon_full)
  draw_icon_showcase_row(cards[2].x, 636, "EMPTY (0/80)",   mana_icon_idle)

  -- Water Main (8+11) 3 states
  draw_icon_showcase_row(cards[3].x, 140, "NORMAL (1..49)",  water_icon_norm)
  draw_icon_showcase_row(cards[3].x, 388, "SURGE (>=50)",    water_icon_surge)
  draw_icon_showcase_row(cards[3].x, 636, "IDLE (0/100)",    water_icon_idle)

  -- Water Alt-8 3 states
  draw_icon_showcase_row(cards[4].x, 140, "NORMAL (1..49)",  water_icon_alt11_norm)
  draw_icon_showcase_row(cards[4].x, 388, "SURGE (>=50)",    water_icon_alt11_surge)
  draw_icon_showcase_row(cards[4].x, 636, "IDLE (0/100)",    water_icon_alt11_idle)

  -- Bottom strip: 4x, 2x, 1x connected icon + Chinese fretwork mouth join check!
  fill_rect(canvas, 24, 912, cw - 48, 304, rgba(19, 22, 30, 255))
  stroke_rect(canvas, 24, 912, cw - 48, 304, rgba(52, 64, 86, 255))
  local j_hdr = new_image(420, 8)
  draw_text(j_hdr, 0, 1, "ICON + CHINESE FRETWORK MOUTH SEAMLESS JOIN CHECK (3x / 2x / 1x)", rgba(224, 234, 250, 255), rgba(8, 10, 16, 255))
  blit(canvas, scale_nn(j_hdr, 3), 44, 928)

  local join_1x = new_image(412, 58, rgba(16, 19, 26, 255))
  paint_daytime_viewport(join_1x, 208, 2, 202, 54)
  -- Left (dark): Mana, Water Main, Water Alt8
  render_mana_bar(join_1x, 6, 2, 56, 80, 1, nil)
  render_water_bar(join_1x, 6, 20, 68, 1, "main")
  render_water_bar(join_1x, 6, 38, 68, 1, "alt11")
  -- Right (day sky): Mana, Water Main, Water Alt8
  render_mana_bar(join_1x, 214, 2, 56, 80, 1, nil)
  render_water_bar(join_1x, 214, 20, 68, 1, "main")
  render_water_bar(join_1x, 214, 38, 68, 1, "alt11")

  blit(canvas, scale_nn(join_1x, 3), 44, 968)
  -- True 2x & 1x right beside it
  blit(canvas, scale_nn(join_1x, 1), 1310, 976)
  local l1x = new_image(60, 7); draw_text(l1x, 0, 1, "TRUE 1x", rgba(200, 214, 236, 255), nil)
  blit(canvas, scale_nn(l1x, 2), 1310, 956)

  local crop2x = new_image(132, 58)
  blit(crop2x, join_1x, 0, 0, 132, 58, 0, 0)
  blit(canvas, scale_nn(crop2x, 2), 1310, 1068)
  local l2x = new_image(90, 7); draw_text(l2x, 0, 1, "GUI SCALE 2x", rgba(200, 214, 236, 255), nil)
  blit(canvas, scale_nn(l2x, 2), 1310, 1048)

  local spr_ic = Sprite(join_1x.width, join_1x.height, ColorMode.RGB)
  spr_ic.layers[1].name = "icons_join_1x"
  spr_ic.cels[1].image = join_1x
  spr_ic:saveAs(src_dir .. "/icons_closeup.aseprite")
  spr_ic:close()

  canvas:saveAs(prv_dir .. "/icons_closeup_8x.png")
end

-- ============================================================================
-- PREVIEW 2: hud_context_day_night_4x.png
-- ============================================================================
do
  local vp_w, vp_h = 244, 122
  local ctx_w = vp_w * 2 + 18
  local ctx_h = 18 + vp_h * 2 + 18
  local ctx = new_image(ctx_w, ctx_h, rgba(14, 16, 22, 255))

  fill_rect(ctx, 0, 0, ctx_w, 14, rgba(22, 26, 35, 255))
  draw_text(ctx, 6, 5, "V4.1 EASTERN MOON-CLOUD MANA & STREAM WATER HUD: MAIN (#8) VS ALT (#11)", rgba(226, 234, 248, 255), rgba(8, 10, 16, 255))

  local x_left  = 6
  local x_right = vp_w + 12
  local y_day   = 18
  local y_cave  = 18 + vp_h + 10

  -- Top row: Daytime sky + terrain (Left = Main 8+11 at 54/80 & 35/100; Right = Alt #8 at 80/80 & 68/100)
  paint_daytime_viewport(ctx, x_left, y_day, vp_w, vp_h)
  draw_vanilla_hud_ref(ctx, x_left, y_day, vp_w, vp_h)
  render_hud_cluster(ctx, x_left, y_day, 54, 80, 35, 1, "main", true)
  stroke_rect(ctx, x_left, y_day, vp_w, vp_h, rgba(52, 70, 92, 255))

  paint_daytime_viewport(ctx, x_right, y_day, vp_w, vp_h)
  draw_vanilla_hud_ref(ctx, x_right, y_day, vp_w, vp_h)
  render_hud_cluster(ctx, x_right, y_day, 80, 80, 68, 1, "alt11", false)
  stroke_rect(ctx, x_right, y_day, vp_w, vp_h, rgba(52, 70, 92, 255))

  -- Bottom row: Dark Cave (Left = Main 8+11 at 80/80 & 68/100; Right = Main 8+11 Low/Trickle vs Alt #8)
  paint_cave_viewport(ctx, x_left, y_cave, vp_w, vp_h)
  draw_vanilla_hud_ref(ctx, x_left, y_cave, vp_w, vp_h)
  render_hud_cluster(ctx, x_left, y_cave, 80, 80, 68, 5, "main", true)
  stroke_rect(ctx, x_left, y_cave, vp_w, vp_h, rgba(46, 56, 74, 255))

  paint_cave_viewport(ctx, x_right, y_cave, vp_w, vp_h)
  draw_vanilla_hud_ref(ctx, x_right, y_cave, vp_w, vp_h)
  render_hud_cluster(ctx, x_right, y_cave, 54, 80, 35, 5, "alt11", true)
  stroke_rect(ctx, x_right, y_cave, vp_w, vp_h, rgba(46, 56, 74, 255))

  local c_day_m  = new_image(168, 48); blit(c_day_m,  ctx, 0, 0, 168, 48, x_left + 1,  y_day + 1)
  local c_cave_m = new_image(168, 48); blit(c_cave_m, ctx, 0, 0, 168, 48, x_left + 1,  y_cave + 1)
  local c_day_a  = new_image(168, 48); blit(c_day_a,  ctx, 0, 0, 168, 48, x_right + 1, y_day + 1)
  local c_cave_a = new_image(168, 48); blit(c_cave_a, ctx, 0, 0, 168, 48, x_right + 1, y_cave + 1)

  local function draw_vp_tag(vx, vy, txt)
    fill_rect(ctx, vx + vp_w - 82, vy + 4, 78, 9, rgba(12, 16, 24, 185))
    draw_text(ctx, vx + vp_w - 79, vy + 6, txt, rgba(226, 236, 252, 255), rgba(6, 8, 14, 255))
  end
  draw_vp_tag(x_left,  y_day,  "DAY | MAIN #8")
  draw_vp_tag(x_right, y_day,  "DAY | ALT #11")
  draw_vp_tag(x_left,  y_cave, "CAVE | MAIN #8")
  draw_vp_tag(x_right, y_cave, "CAVE | ALT #11")

  local spr_ctx = Sprite(ctx.width, ctx.height, ColorMode.RGB)
  spr_ctx.layers[1].name = "hud_context_1x"
  spr_ctx.cels[1].image = ctx
  spr_ctx:saveAs(src_dir .. "/hud_context_day_night.aseprite")
  spr_ctx:close()

  local main_4x = scale_nn(ctx, 4)
  local inset_h = 156
  local final_img = new_image(main_4x.width, main_4x.height + inset_h, rgba(13, 15, 20, 255))
  blit(final_img, main_4x, 0, 0)

  local iy = main_4x.height + 6
  fill_rect(final_img, 20, iy, final_img.width - 40, inset_h - 16, rgba(19, 22, 30, 255))
  stroke_rect(final_img, 20, iy, final_img.width - 40, inset_h - 16, rgba(48, 58, 76, 255))
  local hdr = new_image(420, 8)
  draw_text(hdr, 0, 1, "TRUE 1x (RAW PIXEL) & 2x (GUI SCALE 2) UNFILTERED PROPORTION INSETS:", rgba(214, 226, 244, 255), rgba(8, 10, 16, 255))
  blit(final_img, scale_nn(hdr, 2), 36, iy + 10)

  stroke_rect(c_day_m,  0, 0, 168, 48, rgba(68, 86, 110, 255))
  stroke_rect(c_cave_m, 0, 0, 168, 48, rgba(68, 86, 110, 255))
  stroke_rect(c_day_a,  0, 0, 168, 48, rgba(56, 94, 110, 255))
  stroke_rect(c_cave_a, 0, 0, 168, 48, rgba(56, 94, 110, 255))

  blit(final_img, c_day_m,  36,  iy + 58)
  blit(final_img, c_cave_m, 216, iy + 58)
  blit(final_img, scale_nn(c_cave_m, 2), 398, iy + 34)

  local r_base = math.floor(final_img.width / 2) + 24
  blit(final_img, c_day_a,  r_base,       iy + 58)
  blit(final_img, c_cave_a, r_base + 180, iy + 58)
  blit(final_img, scale_nn(c_day_a, 2),  r_base + 362, iy + 34)

  final_img:saveAs(prv_dir .. "/hud_context_day_night_4x.png")
end

-- ============================================================================
-- PREVIEW 3: states_progression_4x.png
-- ============================================================================
do
  local w, h = 448, 258
  local st = new_image(w, h, rgba(14, 16, 22, 255))

  fill_rect(st, 0, 0, w, 14, rgba(22, 26, 36, 255))
  draw_text(st, 8, 5, "V4.1 STATES PROGRESSION: BRIMMING MOON-CLOUD MANA & STREAMLINE WATER", rgba(226, 236, 250, 255), rgba(8, 10, 16, 255))

  -- Top panel: Mana 5 states on Cave Dark (left) & Daytime Sky (right)
  fill_rect(st, 6, 18, 436, 114, rgba(18, 21, 30, 255))
  stroke_rect(st, 6, 18, 436, 114, rgba(50, 66, 106, 255))
  draw_text(st, 12, 23, "MANA: MOONLIGHT FLOWING CLOUD (CAVE DARK VS DAY SKY)", PAL_MANA.cloud_lit, rgba(8, 12, 24, 255))
  paint_daytime_viewport(st, 226, 31, 210, 96)
  stroke_rect(st, 226, 31, 210, 96, rgba(58, 80, 104, 255))

  local mana_rows = {
    {val = 0,  ghost = nil, label = "0/80 EMPTY"},
    {val = 14, ghost = nil, label = "14/80 LOW"},
    {val = 48, ghost = nil, label = "48/80 HALF"},
    {val = 48, ghost = 68,  label = "48<-68 CAST"},
    {val = 80, ghost = nil, label = "80/80 FULL"},
  }
  for idx, r in ipairs(mana_rows) do
    local ry = 32 + (idx - 1) * 19
    render_mana_bar(st, 12, ry, r.val, 80, 1, r.ghost)
    draw_text(st, 134, ry + 7, r.label, PAL_MANA.text, PAL_MANA.text_shadow)

    render_mana_bar(st, 232, ry, r.val, 80, 1, r.ghost)
    draw_text(st, 354, ry + 7, r.label, PAL_MANA.text, PAL_MANA.text_shadow)
  end

  -- Bottom panel: Water 5 states on Cave Dark (left, Main 8+11) & Daytime Sky (right, Main 8+11)
  fill_rect(st, 6, 137, 436, 115, rgba(16, 21, 26, 255))
  stroke_rect(st, 6, 137, 436, 115, rgba(40, 82, 96, 255))
  draw_text(st, 12, 142, "WATER MOMENTUM: 4-TIER FLOW & 20/50 CLOUD-CLASPS (CAVE VS DAY)", PAL_WATER.t2_blade, rgba(6, 14, 18, 255))
  paint_daytime_viewport(st, 226, 150, 210, 97)
  stroke_rect(st, 226, 150, 210, 97, rgba(58, 80, 104, 255))

  local water_rows = {
    {val = 0,   label = "0 IDLE"},
    {val = 12,  label = "12 TRICKLE"},
    {val = 35,  label = "35 STREAM(>20)"},
    {val = 68,  label = "68 TORRENT(>50)"},
    {val = 100, label = "100 FULL SURGE"},
  }
  for idx, r in ipairs(water_rows) do
    local ry = 151 + (idx - 1) * 19
    render_water_bar(st, 12, ry, r.val, 1, "main")
    draw_text(st, 134, ry + 7, r.label, PAL_WATER.text, PAL_WATER.text_shadow)

    render_water_bar(st, 232, ry, r.val, 1, "main")
    draw_text(st, 354, ry + 7, r.label, PAL_WATER.text, PAL_WATER.text_shadow)
  end

  local spr_st = Sprite(st.width, st.height, ColorMode.RGB)
  spr_st.layers[1].name = "states_progression_1x"
  spr_st.cels[1].image = st
  spr_st:saveAs(src_dir .. "/states_progression.aseprite")
  spr_st:close()

  scale_nn(st, 4):saveAs(prv_dir .. "/states_progression_4x.png")
end

-- ============================================================================
-- PREVIEW 4: lowkey_flow_preview_4x.gif (16 SEAMLESS FRAMES @ 90ms)
-- ============================================================================
do
  local gw, gh = 408, 120
  local anim_spr = Sprite(gw, gh, ColorMode.RGB)
  anim_spr.layers[1].name = "lowkey_flow_16f"
  anim_spr.frames[1].duration = FRAME_DURATION
  for f = 2, FRAMES do
    local nf = anim_spr:newEmptyFrame()
    nf.duration = FRAME_DURATION
  end

  local anim_4x = Sprite(gw * 4, gh * 4, ColorMode.RGB)
  anim_4x.layers[1].name = "lowkey_flow_4x"
  anim_4x.frames[1].duration = FRAME_DURATION
  for f = 2, FRAMES do
    local nf = anim_4x:newEmptyFrame()
    nf.duration = FRAME_DURATION
  end

  for f = 1, FRAMES do
    local frame_img = new_image(gw, gh, rgba(15, 17, 23, 255))
    fill_rect(frame_img, 0, 0, gw, 14, rgba(22, 26, 36, 255))
    draw_text(frame_img, 6, 5, "V4.1 16F SEAMLESS LOOP (90ms/F): LONG MOON-CLOUD & WATER 4-TIER FLOW", rgba(224, 234, 250, 255), rgba(8, 10, 16, 255))

    -- Left column: Mana Half/Full + Alt-8 Water comparison
    fill_rect(frame_img, 5, 18, 194, 97, rgba(19, 22, 31, 255))
    stroke_rect(frame_img, 5, 18, 194, 97, rgba(48, 62, 96, 255))
    draw_text(frame_img, 10, 22, "MANA LONG CLOUD & ALT #11 WATER", PAL_MANA.cloud_lit, nil)

    render_mana_bar(frame_img, 10, 31, 48, 80, f, nil)
    draw_text(frame_img, 132, 38, "48/80 HALF", PAL_MANA.text, PAL_MANA.text_shadow)

    render_mana_bar(frame_img, 10, 51, 80, 80, f, nil)
    draw_text(frame_img, 132, 58, "80/80 FULL", PAL_MANA.text, PAL_MANA.text_shadow)

    render_water_bar(frame_img, 10, 72, 35, f, "alt11")
    draw_text(frame_img, 132, 79, "ALT11 35", PAL_WATER.text, PAL_WATER.text_shadow)

    render_water_bar(frame_img, 10, 92, 68, f, "alt11")
    draw_text(frame_img, 132, 99, "ALT11 68", PAL_WATER.text, PAL_WATER.text_shadow)

    -- Right column: Main 8+11 Water 4 flow tiers (12 / 35 / 68 / 100)
    fill_rect(frame_img, 205, 18, 198, 97, rgba(16, 21, 26, 255))
    stroke_rect(frame_img, 205, 18, 198, 97, rgba(40, 82, 96, 255))
    draw_text(frame_img, 210, 22, "WATER MAIN #8 FLOW MAGNITUDE", PAL_WATER.t2_blade, nil)

    render_water_bar(frame_img, 210, 31, 12, f, "main")
    draw_text(frame_img, 332, 38, "12 SLOW", PAL_WATER.text, PAL_WATER.text_shadow)

    render_water_bar(frame_img, 210, 51, 35, f, "main")
    draw_text(frame_img, 332, 58, "35 MID", PAL_WATER.text, PAL_WATER.text_shadow)

    render_water_bar(frame_img, 210, 72, 68, f, "main")
    draw_text(frame_img, 332, 79, "68 2xFAST", PAL_WATER.text, PAL_WATER.text_shadow)

    render_water_bar(frame_img, 210, 92, 100, f, "main")
    draw_text(frame_img, 332, 99, "100 MAX", PAL_WATER.text, PAL_WATER.text_shadow)

    anim_spr:newCel(anim_spr.layers[1], f, frame_img, Point(0, 0))
    anim_4x:newCel(anim_4x.layers[1], f, scale_nn(frame_img, 4), Point(0, 0))
  end

  anim_spr:saveAs(src_dir .. "/lowkey_flow_preview.aseprite")
  anim_4x:saveAs(prv_dir .. "/lowkey_flow_preview_4x.gif")
  anim_spr:close()
  anim_4x:close()
end

print("Round 3 HUD bars v4.1 polish generated successfully.")
