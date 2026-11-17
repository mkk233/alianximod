local root = "D:/alianximod/docs/资源库/过程归档/生存资源条_v3_低调势能候选_20261113"
local src_dir = root .. "/src"
local exp_dir = root .. "/export"
local prv_dir = root .. "/preview"
local v2_dir  = "D:/alianximod/docs/资源库/界面/生存资源条_细条_v2"

local FRAMES = 16
local FRAME_DURATION = 0.09
local ICON_W, ICON_H = 12, 12
local TRACK_W = 93
local MANA_TRACK_H = 6
local WATER_TRACK_H = 8
local FILL_W, FILL_H = 88, 4
local FILL_X_OFFSET = 3 -- tx=0 socket cradle (1px overlap under icon right rim), tx=1 collar ring, tx=2 inner border, tx=3..90 fill (88px)

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
  ["|"] = {"#", "#", "#", "#", "#"},
  ["("] = {".#", "#.", "#.", "#.", ".#"},
  [")"] = {"#.", ".#", ".#", ".#", "#."},
  ["<"] = {"..#", ".#.", "#..", ".#.", "..#"},
  [">"] = {"#..", ".#.", "..#", ".#.", "#.."},
  ["="] = {"...", "###", "...", "###", "..."},
  ["&"] = {".##.", "#.#.", ".##.", "#..#", ".##."},
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
-- PALETTES
-- ============================================================================

-- Scheme A: Starlight Indigo-Blue (Deep nocturnal indigo + sapphire-starlight)
local PAL_A = {
  border      = rgba(11, 14, 30, 255),
  collar_dark = rgba(20, 26, 54, 255),
  collar_mid  = rgba(36, 46, 88, 255),
  collar_lit  = rgba(54, 68, 124, 255),
  bed_1       = rgba(14, 18, 38, 255),
  bed_2       = rgba(19, 25, 50, 255),
  bed_3       = rgba(22, 30, 58, 255),
  bed_4       = rgba(28, 38, 72, 255),
  deep        = rgba(38, 60, 134, 255),
  body        = rgba(60, 100, 198, 255),
  bright      = rgba(94, 142, 234, 255),
  ridge       = rgba(138, 184, 250, 255),
  shimmer     = rgba(172, 210, 255, 255),
  crown       = rgba(206, 232, 255, 255),
  star_core   = rgba(235, 246, 255, 255),
  pure_star   = rgba(252, 254, 255, 255),
  low_deep    = rgba(28, 44, 98, 255),
  low_body    = rgba(44, 72, 148, 255),
  low_bright  = rgba(66, 102, 184, 255),
  low_ridge   = rgba(92, 132, 208, 255),
  full_deep   = rgba(46, 74, 156, 255),
  full_body   = rgba(74, 118, 218, 255),
  full_bright = rgba(114, 164, 246, 255),
  full_ridge  = rgba(168, 208, 255, 255),
  full_seal   = rgba(232, 245, 255, 255),
  ghost_1     = rgba(88, 118, 194, 255),
  ghost_2     = rgba(52, 72, 132, 255),
  text        = rgba(224, 236, 255, 255),
  text_shadow = rgba(14, 20, 44, 255),
}

-- Scheme B: Warm Champagne Platinum (Obsidian charcoal slot + translucent ivory-champagne)
local PAL_B = {
  border      = rgba(20, 18, 16, 255),
  collar_dark = rgba(34, 30, 26, 255),
  collar_mid  = rgba(56, 49, 42, 255),
  collar_lit  = rgba(80, 70, 58, 255),
  bed_1       = rgba(24, 21, 19, 255),
  bed_2       = rgba(30, 27, 24, 255),
  bed_3       = rgba(35, 31, 27, 255),
  bed_4       = rgba(44, 39, 33, 255),
  deep        = rgba(140, 108, 54, 255),
  body        = rgba(196, 162, 88, 255),
  bright      = rgba(228, 202, 136, 255),
  ridge       = rgba(244, 228, 182, 255),
  shimmer     = rgba(251, 242, 212, 255),
  crown       = rgba(253, 247, 226, 255),
  pearl_core  = rgba(255, 252, 240, 255),
  pure_pearl  = rgba(255, 254, 249, 255),
  low_deep    = rgba(96, 74, 38, 255),
  low_body    = rgba(138, 112, 60, 255),
  low_bright  = rgba(168, 142, 86, 255),
  low_ridge   = rgba(192, 168, 114, 255),
  full_deep   = rgba(158, 124, 64, 255),
  full_body   = rgba(212, 178, 102, 255),
  full_bright = rgba(238, 216, 156, 255),
  full_ridge  = rgba(250, 240, 204, 255),
  full_seal   = rgba(255, 253, 244, 255),
  ghost_1     = rgba(164, 146, 106, 255),
  ghost_2     = rgba(104, 92, 68, 255),
  text        = rgba(252, 245, 226, 255),
  text_shadow = rgba(32, 26, 18, 255),
}

-- Water Momentum: Deep Sea-Teal / Jade-Cyan (emerald-leaning teal, distinct from Indigo A!)
local PAL_W = {
  border       = rgba(8, 20, 23, 255),
  collar_dark  = rgba(16, 36, 42, 255),
  collar_mid   = rgba(28, 58, 66, 255),
  collar_lit   = rgba(44, 84, 94, 255),
  bed_1        = rgba(11, 26, 30, 255),
  bed_2        = rgba(15, 34, 40, 255),
  bed_3        = rgba(18, 41, 48, 255),
  bed_4        = rgba(23, 50, 58, 255),
  guide_line   = rgba(26, 56, 65, 255),
  clasp_unlit_housing = rgba(20, 40, 46, 255),
  clasp_unlit_20      = rgba(52, 84, 94, 255),
  clasp_unlit_50      = rgba(64, 100, 112, 255),
  clasp_lit_housing   = rgba(26, 66, 74, 255),
  clasp_lit_20        = rgba(146, 234, 236, 255),
  clasp_lit_50        = rgba(214, 252, 250, 255),
  -- Tier 1: Shallow Trickle (1-19)
  trickle_air    = rgba(12, 28, 33, 255),
  trickle_surf   = rgba(42, 128, 138, 255),
  trickle_mid    = rgba(28, 98, 108, 255),
  trickle_deep   = rgba(18, 70, 78, 255),
  trickle_ripple = rgba(56, 148, 158, 255),
  -- Tier 2: Steady Stream (20-49)
  stream_surf    = rgba(74, 180, 188, 255),
  stream_upper   = rgba(46, 148, 158, 255),
  stream_lower   = rgba(30, 114, 124, 255),
  stream_deep    = rgba(20, 82, 92, 255),
  stream_vein_hi = rgba(108, 208, 214, 255),
  stream_vein_md = rgba(64, 168, 178, 255),
  stream_lip     = rgba(138, 226, 230, 255),
  -- Tier 3: Rushing Torrent / Full Surge (50-100)
  torrent_trough = rgba(38, 126, 136, 255),
  torrent_surf   = rgba(88, 198, 204, 255),
  torrent_upper  = rgba(54, 164, 174, 255),
  torrent_lower  = rgba(34, 126, 138, 255),
  torrent_deep   = rgba(22, 90, 100, 255),
  surge_hi       = rgba(146, 234, 236, 255),
  foam_soft      = rgba(190, 246, 246, 255),
  foam_white     = rgba(234, 255, 254, 255),
  curl_pocket    = rgba(18, 74, 84, 255),
  -- Text
  text        = rgba(222, 248, 248, 255),
  text_shadow = rgba(8, 26, 30, 255),
  gain_text   = rgba(122, 224, 232, 255),
  spend_text  = rgba(242, 193, 78, 255),
}

-- ============================================================================
-- ICON GENERATORS (12x12, space-separated crisp pixel grids)
-- ============================================================================

-- Mana A Icon: Swirling Astral Core (旋聚灵核)
-- Sharp 4-pointed astral star core `✦` separated by deep indigo vortex moat from two clockwise-swirling spirit wings
local function make_mana_a_icon(mode)
  local img = new_image(ICON_W, ICON_H)
  local p
  if mode == "idle" then
    p = {
      o = PAL_A.border,
      d = rgba(15, 20, 44, 255),
      a = rgba(30, 44, 92, 255),
      s = rgba(46, 68, 132, 255),
      w = rgba(66, 96, 168, 255),
      m = rgba(34, 52, 110, 255),
      l = rgba(54, 84, 156, 255),
      W = rgba(72, 108, 186, 255),
      C = rgba(102, 140, 214, 255),
    }
  elseif mode == "full" then
    p = {
      o = PAL_A.border,
      d = rgba(20, 30, 70, 255),
      a = rgba(68, 112, 214, 255),
      s = rgba(124, 176, 252, 255),
      w = rgba(202, 230, 255, 255),
      m = rgba(76, 122, 226, 255),
      l = rgba(158, 202, 255, 255),
      W = rgba(218, 238, 255, 255),
      C = PAL_A.pure_star,
    }
  else
    p = {
      o = PAL_A.border,
      d = rgba(17, 25, 58, 255),
      a = rgba(56, 94, 192, 255),
      s = rgba(104, 154, 244, 255),
      w = rgba(172, 210, 255, 255),
      m = rgba(64, 106, 208, 255),
      l = rgba(134, 182, 252, 255),
      W = rgba(194, 224, 255, 255),
      C = PAL_A.star_core,
    }
  end

  stamp(img, 0, 0, {
    ". . . o o o o o o o . .",
    ". . o s s s s a w o o .",
    ". o s a d d d d d o o .",
    "o o a a d W W d d o o .",
    "o s a a d l l m d a o o",
    "o s d W l C C l W s o o",
    "o o s W l C C l W d s o",
    "o o a d m l l d a a s o",
    ". o o d d W W d a a o o",
    ". o o d d d d d a s o .",
    ". o o w a s s s s o . .",
    ". . o o o o o o o . . .",
  }, p)
  return img
end

-- Mana B Icon: Qi-Swirl Spirit Pearl (气旋灵珠)
-- Centered 4-6-6-6-6-4 round golden-champagne Spirit Pearl encircled across a 1px dark moat by
-- an upper-left crescent Qi wisp + lower-left to top-right rising ivory Qi silk vortex
local function make_mana_b_icon(mode)
  local img = new_image(ICON_W, ICON_H)
  local p
  if mode == "idle" then
    p = {
      o = PAL_B.border,
      d = rgba(24, 20, 17, 255),
      g = rgba(70, 54, 30, 255),
      m = rgba(96, 76, 44, 255),
      l = rgba(122, 98, 58, 255),
      c = rgba(144, 120, 76, 255),
      C = rgba(166, 144, 98, 255),
      a = rgba(88, 80, 62, 255),
      s = rgba(114, 106, 84, 255),
      w = rgba(138, 130, 106, 255),
    }
  elseif mode == "full" then
    p = {
      o = PAL_B.border,
      d = rgba(34, 28, 20, 255),
      g = rgba(162, 122, 56, 255),
      m = rgba(214, 172, 88, 255),
      l = rgba(242, 210, 134, 255),
      c = rgba(253, 238, 188, 255),
      C = PAL_B.pure_pearl,
      a = rgba(210, 198, 162, 255),
      s = rgba(244, 236, 210, 255),
      w = rgba(255, 253, 245, 255),
    }
  else
    p = {
      o = PAL_B.border,
      d = rgba(30, 25, 18, 255),
      g = rgba(146, 108, 48, 255),
      m = rgba(198, 156, 76, 255),
      l = rgba(232, 198, 118, 255),
      c = rgba(250, 232, 174, 255),
      C = PAL_B.pearl_core,
      a = rgba(192, 180, 144, 255),
      s = rgba(236, 226, 196, 255),
      w = rgba(253, 249, 236, 255),
    }
  end

  stamp(img, 0, 0, {
    ". . . o o o o o . . . .",
    ". . o s w w s a o . o o",
    ". o s d d d d d o o w o",
    "o s d d l c c m d d s o",
    "o a d l C C c l m d w o",
    "o o d c C C l l g d s o",
    ". o d m c l l m g d w o",
    ". o d m l m m g g d s o",
    ". o a d g g g g d s s o",
    ". o s d d d d d s w o .",
    ". . o s s w w s s o . .",
    ". . . o o o o o o . . .",
  }, p)
  return img
end

-- Water Momentum Icon: Right-Curling Wave Crest (向右翻卷浪头)
-- Grounded broad rolling ocean base + upward-right sweeping wave back + sharp right-lunging seafoam claw over open hollow
local function make_water_icon(mode)
  local img = new_image(ICON_W, ICON_H)
  local p
  if mode == "idle" then
    p = {
      o = PAL_W.border,
      d = rgba(14, 42, 48, 255),
      w = rgba(22, 68, 78, 255),
      l = rgba(34, 96, 106, 255),
      s = rgba(54, 126, 136, 255),
      F = rgba(102, 178, 184, 255),
    }
  elseif mode == "surge" then
    p = {
      o = PAL_W.border,
      d = rgba(18, 72, 82, 255),
      w = rgba(36, 136, 148, 255),
      l = rgba(70, 188, 198, 255),
      s = rgba(142, 234, 238, 255),
      F = rgba(246, 255, 255, 255),
    }
  else
    p = {
      o = PAL_W.border,
      d = rgba(16, 62, 72, 255),
      w = rgba(30, 118, 130, 255),
      l = rgba(58, 168, 178, 255),
      s = rgba(118, 216, 222, 255),
      F = rgba(228, 254, 254, 255),
    }
  end

  stamp(img, 0, 0, {
    ". . . . o o o o o o . .",
    ". . . o l s s F F F o o",
    ". . o l s w w l s F F o",
    ". o o l s w w d o o F o",
    ". o l w w d d o . . o o",
    "o o l w w d o . . . . .",
    "o l w w w d o o o . . .",
    "o l w w w w d d l l o o",
    "o l w w w w w d d d o o",
    "o o d d d d d d d o o .",
    ". o o o o o o o o o . .",
    ". . . . . . . . . . . .",
  }, p)
  return img
end

-- ============================================================================
-- TRACK GENERATORS (93x6 Mana, 93x8 Water with socket collar & embedded clasps)
-- ============================================================================

local function draw_bar_body(img, y_off, pal, with_guides)
  local beds = {pal.bed_1, pal.bed_2, pal.bed_3, pal.bed_4}
  for x = 0, TRACK_W - 2 do
    put_pixel(img, x, y_off + 0, pal.border)
    put_pixel(img, x, y_off + 5, pal.border)
  end
  -- Left interlocking socket cradle (tx=0 overlaps icon col 11; tx=1 metallic collar; tx=2 inner border)
  for r = 1, 4 do
    put_pixel(img, 0, y_off + r, pal.collar_dark)
    put_pixel(img, 1, y_off + r, pal.collar_mid)
    put_pixel(img, 2, y_off + r, pal.border)
  end
  put_pixel(img, 1, y_off + 1, pal.collar_lit)
  put_pixel(img, 1, y_off + 4, pal.collar_dark)

  -- Inner 88x4 recessed chamber (x = 3 .. 90)
  for r = 1, 4 do
    local bc = beds[r]
    for x = FILL_X_OFFSET, FILL_X_OFFSET + FILL_W - 1 do
      put_pixel(img, x, y_off + r, bc)
    end
  end

  if with_guides then
    local x20 = FILL_X_OFFSET + 18 - 1
    local x50 = FILL_X_OFFSET + 44 - 1
    for r = 1, 4 do
      put_pixel(img, x20, y_off + r, pal.guide_line)
      put_pixel(img, x50, y_off + r, pal.guide_line)
    end
  end

  -- Right bevel & chamfered cap (x = 91, 92)
  for r = 1, 4 do
    put_pixel(img, TRACK_W - 2, y_off + r, pal.collar_dark)
    put_pixel(img, TRACK_W - 1, y_off + r, pal.border)
  end
  put_pixel(img, TRACK_W - 2, y_off + 1, pal.collar_mid)
end

local function make_mana_track(pal)
  local img = new_image(TRACK_W, MANA_TRACK_H)
  draw_bar_body(img, 0, pal, false)
  return img
end

-- clasp_state: 0 (<20), 1 (20..49), 2 (>=50)
local function draw_water_clasps(img, clasp_state)
  local lit20 = clasp_state >= 1
  local lit50 = clasp_state >= 2
  local x20 = FILL_X_OFFSET + 18 - 1 -- 20
  local x50 = FILL_X_OFFSET + 44 - 1 -- 46

  local h20 = lit20 and PAL_W.clasp_lit_housing or PAL_W.clasp_unlit_housing
  local t20 = lit20 and PAL_W.clasp_lit_20 or PAL_W.clasp_unlit_20
  for dx = -1, 1 do
    put_pixel(img, x20 + dx, 0, PAL_W.border)
  end
  put_pixel(img, x20 - 1, 1, h20)
  put_pixel(img, x20,     1, t20)
  put_pixel(img, x20 + 1, 1, h20)

  local h50 = lit50 and PAL_W.clasp_lit_housing or PAL_W.clasp_unlit_housing
  local t50 = lit50 and PAL_W.clasp_lit_50 or PAL_W.clasp_unlit_50
  for dx = -1, 1 do
    put_pixel(img, x50 + dx, 0, PAL_W.border)
    put_pixel(img, x50 + dx, 7, PAL_W.border)
  end
  put_pixel(img, x50 - 1, 1, h50)
  put_pixel(img, x50,     1, t50)
  put_pixel(img, x50 + 1, 1, h50)
  put_pixel(img, x50 - 1, 6, h50)
  put_pixel(img, x50,     6, t50)
  put_pixel(img, x50 + 1, 6, h50)
end

local function make_water_track(clasp_state)
  local img = new_image(TRACK_W, WATER_TRACK_H)
  draw_bar_body(img, 1, PAL_W, true)
  draw_water_clasps(img, clasp_state or 0)
  return img
end

-- ============================================================================
-- FILL & ANIMATION GENERATORS (88x4 per frame, 16 seamless frames)
-- ============================================================================

local function calc_pixels(val, max_val)
  if val <= 0 or max_val <= 0 then return 0 end
  if val >= max_val then return FILL_W end
  return math.max(1, math.min(FILL_W, math.floor(val / max_val * FILL_W + 0.5)))
end

local function make_mana_fill_frame(pal, state, frame_idx)
  local img = new_image(FILL_W, FILL_H)
  local c0, c1, c2, c3
  if state == "low" then
    c0, c1, c2, c3 = pal.low_ridge, pal.low_bright, pal.low_body, pal.low_deep
  elseif state == "full" then
    c0, c1, c2, c3 = pal.full_ridge, pal.full_bright, pal.full_body, pal.full_deep
  else
    c0, c1, c2, c3 = pal.ridge, pal.bright, pal.body, pal.deep
  end

  local shift = frame_idx - 1
  for x = 0, FILL_W - 1 do
    local phase = (x - shift) % 16
    local r0, r1, r2, r3 = c0, c1, c2, c3
    if state == "normal" then
      if phase >= 2 and phase <= 5 then
        r0 = pal.shimmer
      elseif phase == 6 or phase == 1 then
        r0 = lerp_color(c0, pal.shimmer, 0.5)
      end
      if phase >= 4 and phase <= 7 then
        r1 = lerp_color(c1, c0, 0.35)
      end
    elseif state == "full" then
      local breath = 0.5 + 0.5 * math.sin((frame_idx - 1) / FRAMES * 2 * math.pi)
      if phase >= 1 and phase <= 7 then
        r0 = lerp_color(c0, pal.full_seal, 0.55 + 0.25 * breath)
      else
        r0 = lerp_color(c0, pal.full_seal, 0.28 + 0.15 * breath)
      end
      r1 = lerp_color(c1, pal.full_ridge, 0.38 + 0.14 * breath)
      if phase >= 3 and phase <= 9 then
        r1 = lerp_color(r1, pal.full_seal, 0.25)
      end
    end
    if x == 0 then
      r0 = lerp_color(r0, r2, 0.35)
      r1 = lerp_color(r1, r3, 0.25)
    end
    img:drawPixel(x, 0, r0)
    img:drawPixel(x, 1, r1)
    img:drawPixel(x, 2, r2)
    img:drawPixel(x, 3, r3)
  end
  return img
end

local function render_mana_bar(dst, x, y, scheme, val, max_val, frame_idx, afterglow_from)
  local pal = (scheme == "A") and PAL_A or PAL_B
  local make_icon = (scheme == "A") and make_mana_a_icon or make_mana_b_icon
  local px = calc_pixels(val, max_val)
  local icon_mode = (px == 0) and "idle" or ((px >= FILL_W) and "full" or "normal")
  local fill_state = (px >= FILL_W) and "full" or (((val / max_val) <= 0.22) and "low" or "normal")

  local track_x = x + ICON_W - 1
  local fill_x = track_x + FILL_X_OFFSET
  local track = make_mana_track(pal)
  local icon = make_icon(icon_mode)
  blit(dst, track, track_x, y + 3)
  blit(dst, icon, x, y)

  if afterglow_from and afterglow_from > val then
    local ghost_px = calc_pixels(afterglow_from, max_val)
    for gx = px, ghost_px - 1 do
      local t = (gx - px) / math.max(1, (ghost_px - px))
      local gc_top = lerp_color(pal.ghost_1, pal.bed_2, t * 0.65)
      local gc_bot = lerp_color(pal.ghost_2, pal.bed_3, t * 0.65)
      put_pixel(dst, fill_x + gx, y + 4, gc_top)
      put_pixel(dst, fill_x + gx, y + 5, gc_top)
      put_pixel(dst, fill_x + gx, y + 6, gc_bot)
      put_pixel(dst, fill_x + gx, y + 7, lerp_color(pal.ghost_2, pal.bed_4, 0.5))
    end
    if ghost_px > px then
      put_pixel(dst, fill_x + ghost_px - 1, y + 4, pal.ghost_1)
      put_pixel(dst, fill_x + ghost_px - 1, y + 5, pal.ghost_1)
    end
  end

  if px > 0 then
    local fill = make_mana_fill_frame(pal, fill_state, frame_idx)
    local fx = fill_x
    local fy = y + 4
    blit(dst, fill, fx, fy, px, FILL_H)

    local core_c = (scheme == "A") and PAL_A.star_core or PAL_B.pearl_core
    if px >= FILL_W then
      put_pixel(dst, fx + FILL_W - 2, fy + 0, pal.full_seal)
      put_pixel(dst, fx + FILL_W - 2, fy + 1, lerp_color(pal.full_bright, pal.full_seal, 0.55))
      put_pixel(dst, fx + FILL_W - 1, fy + 0, pal.full_seal)
      put_pixel(dst, fx + FILL_W - 1, fy + 1, core_c)
      put_pixel(dst, fx + FILL_W - 1, fy + 2, pal.full_bright)
      put_pixel(dst, fx + FILL_W - 1, fy + 3, pal.full_body)
    elseif fill_state == "low" then
      put_pixel(dst, fx + px - 1, fy + 1, pal.low_ridge)
      put_pixel(dst, fx + px - 1, fy + 2, pal.low_bright)
      put_pixel(dst, fx + px - 1, fy + 0, lerp_color(pal.low_bright, pal.bed_1, 0.35))
    else
      if px >= 2 then
        put_pixel(dst, fx + px - 2, fy + 0, pal.crown)
        put_pixel(dst, fx + px - 2, fy + 1, pal.ridge)
      end
      put_pixel(dst, fx + px - 1, fy + 0, pal.ridge)
      put_pixel(dst, fx + px - 1, fy + 1, core_c)
      put_pixel(dst, fx + px - 1, fy + 2, pal.bright)
      put_pixel(dst, fx + px - 1, fy + 3, lerp_color(pal.body, pal.deep, 0.4))
    end
  end
end

local function make_water_fill_frame(tier, frame_idx)
  local img = new_image(FILL_W, FILL_H)
  local f0 = frame_idx - 1

  if tier == 1 then
    for x = 0, FILL_W - 1 do
      local phase = (x - f0) % 16
      local r1 = PAL_W.trickle_surf
      local r2 = PAL_W.trickle_mid
      local r3 = PAL_W.trickle_deep
      if phase >= 3 and phase <= 6 then
        r1 = PAL_W.trickle_ripple
      elseif phase == 11 or phase == 12 then
        r2 = lerp_color(PAL_W.trickle_mid, PAL_W.trickle_deep, 0.45)
      end
      img:drawPixel(x, 0, PAL_W.trickle_air)
      img:drawPixel(x, 1, r1)
      img:drawPixel(x, 2, r2)
      img:drawPixel(x, 3, r3)
    end
  elseif tier == 2 then
    for x = 0, FILL_W - 1 do
      local rows = {PAL_W.stream_surf, PAL_W.stream_upper, PAL_W.stream_lower, PAL_W.stream_deep}
      for y = 0, 3 do
        local p16 = (x + y * 2 - f0) % 16
        local c = rows[y + 1]
        if p16 >= 0 and p16 <= 2 then
          if y == 0 then c = PAL_W.stream_vein_hi
          elseif y == 1 then c = PAL_W.stream_vein_md
          elseif y == 2 and p16 == 1 then c = PAL_W.stream_upper end
        elseif p16 == 9 or p16 == 10 then
          if y == 0 then c = lerp_color(PAL_W.stream_surf, PAL_W.stream_vein_hi, 0.65)
          elseif y == 1 and p16 == 9 then c = PAL_W.stream_vein_md end
        elseif (p16 == 5 or p16 == 6) and y >= 2 then
          c = lerp_color(c, PAL_W.stream_deep, 0.38)
        end
        img:drawPixel(x, y, c)
      end
    end
  else
    -- Tier 3 (50..100): 2x speed (2px/frame -> 2 seamless cycles per 16 frames!), organic non-striped surge
    local shift2x = f0 * 2
    for x = 0, FILL_W - 1 do
      local rows = {PAL_W.torrent_surf, PAL_W.torrent_upper, PAL_W.torrent_lower, PAL_W.torrent_deep}
      for y = 0, 3 do
        local p16 = (x + y * 2 - shift2x) % 16
        local surf16 = (x - shift2x) % 16
        local c = rows[y + 1]

        if y == 0 then
          if surf16 >= 0 and surf16 <= 2 then
            c = PAL_W.foam_soft
          elseif surf16 == 3 then
            c = PAL_W.surge_hi
          elseif surf16 == 6 or surf16 == 7 then
            c = PAL_W.torrent_trough
          elseif surf16 == 10 or surf16 == 11 then
            c = PAL_W.surge_hi
          end
        elseif y == 1 then
          if p16 >= 0 and p16 <= 2 then
            c = (p16 == 1) and PAL_W.foam_soft or PAL_W.surge_hi
          elseif p16 == 9 or p16 == 10 then
            c = PAL_W.surge_hi
          elseif p16 == 5 or p16 == 6 then
            c = PAL_W.torrent_lower
          end
        elseif y == 2 then
          if p16 == 0 or p16 == 1 or p16 == 9 then
            c = PAL_W.torrent_surf
          elseif p16 >= 4 and p16 <= 6 then
            c = PAL_W.curl_pocket
          end
        else
          if p16 == 0 or p16 == 1 then
            c = PAL_W.torrent_lower
          end
        end
        img:drawPixel(x, y, c)
      end
    end
  end
  return img
end

local function render_water_bar(dst, x, y, val, frame_idx)
  local px = calc_pixels(val, 100)
  local clasp_state = (val >= 50) and 2 or ((val >= 20) and 1 or 0)
  local icon_mode = (px == 0) and "idle" or ((val >= 50) and "surge" or "normal")

  local track_x = x + ICON_W - 1
  local fill_x = track_x + FILL_X_OFFSET
  local track = make_water_track(clasp_state)
  local icon = make_water_icon(icon_mode)
  blit(dst, track, track_x, y + 2)
  blit(dst, icon, x, y)

  if px <= 0 then return end

  local tier = (val >= 50) and 3 or ((val >= 20) and 2 or 1)
  local fill = make_water_fill_frame(tier, frame_idx)
  local fx = fill_x
  local fy = y + 4
  blit(dst, fill, fx, fy, px, FILL_H)

  local f0 = frame_idx - 1
  if tier == 1 then
    put_pixel(dst, fx + px - 1, fy + 0, PAL_W.trickle_air)
    put_pixel(dst, fx + px - 1, fy + 1, PAL_W.trickle_air)
    put_pixel(dst, fx + px - 1, fy + 2, PAL_W.trickle_surf)
    put_pixel(dst, fx + px - 1, fy + 3, PAL_W.trickle_mid)
    if px >= 2 then
      put_pixel(dst, fx + px - 2, fy + 0, PAL_W.trickle_air)
      put_pixel(dst, fx + px - 2, fy + 1, PAL_W.trickle_ripple)
      put_pixel(dst, fx + px - 2, fy + 2, PAL_W.trickle_surf)
    end
  elseif tier == 2 then
    local bob = ((f0 % 8) < 4) and 0 or 1
    if px >= 3 then
      put_pixel(dst, fx + px - 3, fy + 0, PAL_W.stream_vein_hi)
      put_pixel(dst, fx + px - 2, fy + 0, PAL_W.stream_lip)
      put_pixel(dst, fx + px - 2, fy + 1, PAL_W.stream_vein_hi)
      put_pixel(dst, fx + px - 2, fy + 2, PAL_W.stream_deep)
    end
    put_pixel(dst, fx + px - 1, fy + 0, (bob == 0) and PAL_W.stream_lip or PAL_W.bed_1)
    put_pixel(dst, fx + px - 1, fy + 1, PAL_W.stream_lip)
    put_pixel(dst, fx + px - 1, fy + 2, PAL_W.stream_upper)
    put_pixel(dst, fx + px - 1, fy + 3, PAL_W.bed_4)
  else
    local shift2x = f0 * 2
    local foam_start = math.max(2, px - 22)
    for gx = foam_start, px - 3 do
      local fp = (gx - shift2x) % 16
      if fp == 0 or fp == 1 then
        put_pixel(dst, fx + gx, fy + 0, PAL_W.foam_white)
      elseif fp == 9 and gx >= px - 14 then
        put_pixel(dst, fx + gx, fy + 0, PAL_W.foam_soft)
      elseif fp == 5 and gx >= px - 12 then
        put_pixel(dst, fx + gx, fy + 1, PAL_W.foam_white)
      end
    end
    if px >= FILL_W then
      local pulse = ((f0 * 2) % 8 < 4)
      put_pixel(dst, fx + FILL_W - 3, fy + 0, PAL_W.foam_soft)
      put_pixel(dst, fx + FILL_W - 2, fy + 0, PAL_W.foam_white)
      put_pixel(dst, fx + FILL_W - 2, fy + 1, pulse and PAL_W.foam_white or PAL_W.foam_soft)
      put_pixel(dst, fx + FILL_W - 2, fy + 2, PAL_W.surge_hi)
      put_pixel(dst, fx + FILL_W - 1, fy + 0, PAL_W.foam_white)
      put_pixel(dst, fx + FILL_W - 1, fy + 1, PAL_W.foam_white)
      put_pixel(dst, fx + FILL_W - 1, fy + 2, pulse and PAL_W.foam_soft or PAL_W.surge_hi)
      put_pixel(dst, fx + FILL_W - 1, fy + 3, PAL_W.torrent_upper)
    else
      local phase4 = f0 % 4
      if px >= 4 then
        put_pixel(dst, fx + px - 4, fy + 0, PAL_W.surge_hi)
        put_pixel(dst, fx + px - 3, fy + 0, PAL_W.foam_soft)
        put_pixel(dst, fx + px - 3, fy + 1, PAL_W.surge_hi)
        put_pixel(dst, fx + px - 2, fy + 0, PAL_W.foam_white)
        put_pixel(dst, fx + px - 2, fy + 1, PAL_W.foam_soft)
        put_pixel(dst, fx + px - 2, fy + 2, PAL_W.curl_pocket)
        put_pixel(dst, fx + px - 2, fy + 3, PAL_W.torrent_lower)
      end
      put_pixel(dst, fx + px - 1, fy + 0, (phase4 ~= 3) and PAL_W.foam_white or PAL_W.foam_soft)
      put_pixel(dst, fx + px - 1, fy + 1, (phase4 <= 1) and PAL_W.foam_white or PAL_W.foam_soft)
      put_pixel(dst, fx + px - 1, fy + 2, (phase4 == 1) and PAL_W.surge_hi or PAL_W.bed_3)
      put_pixel(dst, fx + px - 1, fy + 3, PAL_W.bed_4)
    end
  end
end

-- ============================================================================
-- EXPORT 1x CANDIDATE COMPONENTS & MASTER EDITABLE .ASEPRITE
-- ============================================================================

local function make_strip_16(gen_fn)
  local strip = new_image(FILL_W, FILL_H * FRAMES)
  for f = 1, FRAMES do
    local frame_img = gen_fn(f)
    blit(strip, frame_img, 0, (f - 1) * FILL_H)
  end
  return strip
end

local mana_a_icon_idle = make_mana_a_icon("idle")
local mana_a_icon_norm = make_mana_a_icon("normal")
local mana_a_icon_full = make_mana_a_icon("full")
local mana_a_track     = make_mana_track(PAL_A)
local mana_a_fill_low  = make_mana_fill_frame(PAL_A, "low", 1)
local mana_a_fill_norm_strip = make_strip_16(function(f) return make_mana_fill_frame(PAL_A, "normal", f) end)
local mana_a_fill_full_strip = make_strip_16(function(f) return make_mana_fill_frame(PAL_A, "full", f) end)

local mana_b_icon_idle = make_mana_b_icon("idle")
local mana_b_icon_norm = make_mana_b_icon("normal")
local mana_b_icon_full = make_mana_b_icon("full")
local mana_b_track     = make_mana_track(PAL_B)
local mana_b_fill_low  = make_mana_fill_frame(PAL_B, "low", 1)
local mana_b_fill_norm_strip = make_strip_16(function(f) return make_mana_fill_frame(PAL_B, "normal", f) end)
local mana_b_fill_full_strip = make_strip_16(function(f) return make_mana_fill_frame(PAL_B, "full", f) end)

local water_icon_idle  = make_water_icon("idle")
local water_icon_norm  = make_water_icon("normal")
local water_icon_surge = make_water_icon("surge")
local water_track_0    = make_water_track(0)
local water_track_20   = make_water_track(1)
local water_track_50   = make_water_track(2)
local water_track_states = new_image(TRACK_W, WATER_TRACK_H * 3)
blit(water_track_states, water_track_0,  0, 0)
blit(water_track_states, water_track_20, 0, WATER_TRACK_H)
blit(water_track_states, water_track_50, 0, WATER_TRACK_H * 2)

local water_fill_t1_strip = make_strip_16(function(f) return make_water_fill_frame(1, f) end)
local water_fill_t2_strip = make_strip_16(function(f) return make_water_fill_frame(2, f) end)
local water_fill_t3_strip = make_strip_16(function(f) return make_water_fill_frame(3, f) end)

mana_a_icon_idle:saveAs(exp_dir .. "/mana_a_icon_idle.png")
mana_a_icon_norm:saveAs(exp_dir .. "/mana_a_icon.png")
mana_a_icon_full:saveAs(exp_dir .. "/mana_a_icon_full.png")
mana_a_track:saveAs(exp_dir .. "/mana_a_track.png")
mana_a_fill_low:saveAs(exp_dir .. "/mana_a_fill_low.png")
mana_a_fill_norm_strip:saveAs(exp_dir .. "/mana_a_fill_strip.png")
mana_a_fill_full_strip:saveAs(exp_dir .. "/mana_a_fill_full_strip.png")

mana_b_icon_idle:saveAs(exp_dir .. "/mana_b_icon_idle.png")
mana_b_icon_norm:saveAs(exp_dir .. "/mana_b_icon.png")
mana_b_icon_full:saveAs(exp_dir .. "/mana_b_icon_full.png")
mana_b_track:saveAs(exp_dir .. "/mana_b_track.png")
mana_b_fill_low:saveAs(exp_dir .. "/mana_b_fill_low.png")
mana_b_fill_norm_strip:saveAs(exp_dir .. "/mana_b_fill_strip.png")
mana_b_fill_full_strip:saveAs(exp_dir .. "/mana_b_fill_full_strip.png")

water_icon_idle:saveAs(exp_dir .. "/water_icon_idle.png")
water_icon_norm:saveAs(exp_dir .. "/water_icon.png")
water_icon_surge:saveAs(exp_dir .. "/water_icon_surge.png")
water_track_0:saveAs(exp_dir .. "/water_track.png")
water_track_states:saveAs(exp_dir .. "/water_track_states.png")
water_fill_t1_strip:saveAs(exp_dir .. "/water_fill_tier1_trickle_strip.png")
water_fill_t2_strip:saveAs(exp_dir .. "/water_fill_tier2_stream_strip.png")
water_fill_t3_strip:saveAs(exp_dir .. "/water_fill_tier3_torrent_strip.png")

-- Master multi-layer 16-frame editable .aseprite file for components
do
  local master = Sprite(244, 96, ColorMode.RGB)
  master.frames[1].duration = FRAME_DURATION
  for f = 2, FRAMES do
    local nf = master:newEmptyFrame()
    nf.duration = FRAME_DURATION
  end

  local l_bg = master.layers[1]
  l_bg.name = "ref_dark_backing"
  local l_ma_track = master:newLayer(); l_ma_track.name = "mana_a_track_and_icons"
  local l_ma_fill  = master:newLayer(); l_ma_fill.name  = "mana_a_fill_16f"
  local l_mb_track = master:newLayer(); l_mb_track.name = "mana_b_track_and_icons"
  local l_mb_fill  = master:newLayer(); l_mb_fill.name  = "mana_b_fill_16f"
  local l_w_track  = master:newLayer(); l_w_track.name  = "water_tracks_and_icons"
  local l_w_fill   = master:newLayer(); l_w_fill.name   = "water_fills_tiers_16f"

  local bg_img = new_image(244, 96, rgba(16, 18, 22, 255))
  local ma_static = new_image(244, 96)
  blit(ma_static, mana_a_icon_idle, 6, 6)
  blit(ma_static, mana_a_icon_norm, 22, 6)
  blit(ma_static, mana_a_icon_full, 38, 6)
  blit(ma_static, mana_a_track, 56, 9)

  local mb_static = new_image(244, 96)
  blit(mb_static, mana_b_icon_idle, 6, 24)
  blit(mb_static, mana_b_icon_norm, 22, 24)
  blit(mb_static, mana_b_icon_full, 38, 24)
  blit(mb_static, mana_b_track, 56, 27)

  local w_static = new_image(244, 96)
  blit(w_static, water_icon_idle,  6, 44)
  blit(w_static, water_icon_norm,  22, 44)
  blit(w_static, water_icon_surge, 38, 44)
  blit(w_static, water_track_0,  56, 46)
  blit(w_static, water_track_20, 56, 60)
  blit(w_static, water_track_50, 56, 74)

  for f = 1, FRAMES do
    master:newCel(l_bg, f, bg_img, Point(0, 0))
    master:newCel(l_ma_track, f, ma_static, Point(0, 0))
    master:newCel(l_mb_track, f, mb_static, Point(0, 0))
    master:newCel(l_w_track,  f, w_static,  Point(0, 0))

    local ma_f = new_image(244, 96)
    blit(ma_f, make_mana_fill_frame(PAL_A, "normal", f), 59, 10)
    blit(ma_f, make_mana_fill_frame(PAL_A, "full", f),   152, 10)
    master:newCel(l_ma_fill, f, ma_f, Point(0, 0))

    local mb_f = new_image(244, 96)
    blit(mb_f, make_mana_fill_frame(PAL_B, "normal", f), 59, 28)
    blit(mb_f, make_mana_fill_frame(PAL_B, "full", f),   152, 28)
    master:newCel(l_mb_fill, f, mb_f, Point(0, 0))

    local w_f = new_image(244, 96)
    blit(w_f, make_water_fill_frame(1, f), 59, 48)
    blit(w_f, make_water_fill_frame(2, f), 59, 62)
    blit(w_f, make_water_fill_frame(3, f), 59, 76)
    master:newCel(l_w_fill, f, w_f, Point(0, 0))
  end

  master:saveAs(src_dir .. "/hud_bars_v3_components.aseprite")
  local flat_overview = new_image(244, 96, rgba(16, 18, 22, 255))
  blit(flat_overview, ma_static, 0, 0)
  blit(flat_overview, make_mana_fill_frame(PAL_A, "normal", 1), 59, 10)
  blit(flat_overview, make_mana_fill_frame(PAL_A, "full", 1),   152, 10)
  blit(flat_overview, mb_static, 0, 0)
  blit(flat_overview, make_mana_fill_frame(PAL_B, "normal", 1), 59, 28)
  blit(flat_overview, make_mana_fill_frame(PAL_B, "full", 1),   152, 28)
  blit(flat_overview, w_static, 0, 0)
  blit(flat_overview, make_water_fill_frame(1, 1), 59, 48)
  blit(flat_overview, make_water_fill_frame(2, 1), 59, 62)
  blit(flat_overview, make_water_fill_frame(3, 1), 59, 76)
  flat_overview:saveAs(exp_dir .. "/components_overview_sheet.png")
  master:close()
end

-- ============================================================================
-- SIMULATED BACKGROUNDS & VANILLA HUD HELPERS FOR PREVIEWS
-- ============================================================================

local function paint_daytime_viewport(img, ox, oy, w, h)
  for y = 0, h - 1 do
    for x = 0, w - 1 do
      local c
      if y < math.floor(h * 0.56) then
        local t = y / math.max(1, math.floor(h * 0.56))
        c = lerp_color(rgba(116, 152, 198, 255), rgba(154, 184, 216, 255), t)
        if y >= 14 and y <= 23 and ((x + 18) % 68 < 28) then
          c = lerp_color(c, rgba(214, 226, 238, 255), (y <= 17) and 0.55 or 0.35)
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
      local base = lerp_color(rgba(22, 24, 29, 255), rgba(15, 16, 20, 255), t)
      if hash == 0 or hash == 1 then
        base = lerp_color(base, rgba(34, 37, 44, 255), 0.7)
      elseif hash == 5 then
        base = lerp_color(base, rgba(11, 12, 15, 255), 0.7)
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

local function render_hud_cluster(dst, ox, oy, scheme, mana_val, mana_max, water_val, frame_idx, show_change)
  local pal_m = (scheme == "A") and PAL_A or PAL_B
  render_mana_bar(dst, ox + 8, oy + 5, scheme, mana_val, mana_max, frame_idx, nil)
  draw_text(dst, ox + 116, oy + 9, string.format("%d/%d", mana_val, mana_max), pal_m.text, pal_m.text_shadow)

  render_water_bar(dst, ox + 8, oy + 20, water_val, frame_idx)
  draw_text(dst, ox + 116, oy + 24, string.format("%d/100", water_val), PAL_W.text, PAL_W.text_shadow)

  if show_change then
    draw_text(dst, ox + 22, oy + 32, "+12", PAL_W.gain_text, PAL_W.text_shadow)
  end
end

-- ============================================================================
-- PREVIEW 1: icons_closeup_8x.png
-- ============================================================================
do
  local v2_mana = Image{fromFile = v2_dir .. "/mana_icon.png"}
  local v2_water = Image{fromFile = v2_dir .. "/water_icon.png"}

  local src_board = new_image(228, 124, rgba(16, 18, 23, 255))
  fill_rect(src_board, 0, 0, 228, 13, rgba(24, 27, 35, 255))
  draw_text(src_board, 6, 4, "V3 ICON REDESIGN: V2 OLD VS A / B / WAVE", rgba(226, 232, 244, 255), rgba(8, 10, 14, 255))

  local cols = {
    {x = 6,   w = 44, title = "V2 OLD"},
    {x = 54,  w = 54, title = "A:CORE"},
    {x = 112, w = 54, title = "B:PEARL"},
    {x = 170, w = 52, title = "WAVE"},
  }
  for _, col in ipairs(cols) do
    fill_rect(src_board, col.x, 16, col.w, 102, rgba(22, 25, 32, 255))
    stroke_rect(src_board, col.x, 16, col.w, 102, rgba(40, 46, 58, 255))
    draw_text(src_board, col.x + 4, 20, col.title, rgba(208, 218, 236, 255), rgba(8, 10, 14, 255))
  end

  local function place_icon_pair(icon, x, y, label)
    draw_text(src_board, x, y - 7, label, rgba(154, 166, 186, 255), nil)
    fill_rect(src_board, x, y, 18, 18, rgba(17, 19, 24, 255))
    stroke_rect(src_board, x, y, 18, 18, rgba(46, 52, 64, 255))
    local pad = math.floor((18 - icon.width) / 2)
    blit(src_board, icon, x + pad, y + pad)

    fill_rect(src_board, x + 22, y, 18, 18, rgba(132, 166, 204, 255))
    stroke_rect(src_board, x + 22, y, 18, 18, rgba(64, 84, 108, 255))
    blit(src_board, icon, x + 22 + pad, y + pad)
  end

  place_icon_pair(v2_mana,  10, 38, "MANA 9x9")
  place_icon_pair(v2_water, 10, 68, "WATER 9x9")

  place_icon_pair(mana_a_icon_idle, 61, 36, "EMPTY")
  place_icon_pair(mana_a_icon_norm, 61, 63, "NORMAL")
  place_icon_pair(mana_a_icon_full, 61, 90, "FULL")

  place_icon_pair(mana_b_icon_idle, 119, 36, "EMPTY")
  place_icon_pair(mana_b_icon_norm, 119, 63, "NORMAL")
  place_icon_pair(mana_b_icon_full, 119, 90, "FULL")

  place_icon_pair(water_icon_idle,  176, 36, "0 IDLE")
  place_icon_pair(water_icon_norm,  176, 63, "NORMAL")
  place_icon_pair(water_icon_surge, 176, 90, "SURGE")

  local spr = Sprite(src_board.width, src_board.height, ColorMode.RGB)
  spr.layers[1].name = "icons_closeup_1x"
  spr.cels[1].image = src_board
  spr:saveAs(src_dir .. "/icons_closeup.aseprite")
  spr:close()

  local top_8x = scale_nn(src_board, 8)
  local footer_h = 132
  local out = new_image(top_8x.width, top_8x.height + footer_h, rgba(14, 16, 20, 255))
  blit(out, top_8x, 0, 0)

  local fy = top_8x.height + 8
  fill_rect(out, 16, fy, out.width - 32, footer_h - 16, rgba(22, 25, 32, 255))
  stroke_rect(out, 16, fy, out.width - 32, footer_h - 16, rgba(48, 56, 70, 255))

  local label_strip = new_image(440, 10)
  draw_text(label_strip, 2, 2, "TRUE 1x & 2x NATIVE SIZE + SOCKET COLLAR CHECK:", rgba(210, 220, 238, 255), rgba(8, 10, 14, 255))
  blit(out, scale_nn(label_strip, 2), 32, fy + 10)

  local real_1x = new_image(380, 32, rgba(18, 20, 26, 255))
  stroke_rect(real_1x, 0, 0, 380, 32, rgba(58, 66, 82, 255))
  blit(real_1x, v2_mana, 8, 4)
  blit(real_1x, v2_water, 8, 18)
  render_mana_bar(real_1x, 36, 3, "A", 48, 80, 1, nil)
  render_water_bar(real_1x, 36, 17, 68, 1)
  paint_daytime_viewport(real_1x, 194, 2, 180, 28)
  render_mana_bar(real_1x, 200, 3, "B", 62, 80, 1, nil)
  render_water_bar(real_1x, 200, 17, 68, 1)

  local tag_1x = scale_nn((function() local t = new_image(24, 7); draw_text(t, 0, 1, "1x:", rgba(180, 200, 230, 255)); return t end)(), 2)
  local tag_2x = scale_nn((function() local t = new_image(24, 7); draw_text(t, 0, 1, "2x:", rgba(180, 200, 230, 255)); return t end)(), 2)
  blit(out, tag_1x, 36, fy + 60)
  blit(out, real_1x, 86, fy + 52)
  blit(out, tag_2x, 510, fy + 60)
  blit(out, scale_nn(real_1x, 2), 560, fy + 40)

  out:saveAs(prv_dir .. "/icons_closeup_8x.png")
end

-- ============================================================================
-- PREVIEW 2: compare_ab_hud_context_4x.png
-- ============================================================================
do
  local vp_w, vp_h = 216, 126
  local canvas_w = vp_w * 2 + 18
  local canvas_h = vp_h * 2 + 34
  local ctx = new_image(canvas_w, canvas_h, rgba(13, 15, 19, 255))

  fill_rect(ctx, 0, 0, canvas_w, 14, rgba(22, 25, 32, 255))
  draw_text(ctx, 8, 5, "SCHEME A: INDIGO + ASTRAL CORE + WAVE", PAL_A.ridge, rgba(8, 10, 18, 255))
  draw_text(ctx, vp_w + 14, 5, "SCHEME B: WARM PLATINUM + PEARL + WAVE", PAL_B.ridge, rgba(16, 12, 8, 255))

  local y_day = 17
  paint_daytime_viewport(ctx, 4, y_day, vp_w, vp_h)
  stroke_rect(ctx, 4, y_day, vp_w, vp_h, rgba(46, 54, 68, 255))
  draw_vanilla_hud_ref(ctx, 4, y_day, vp_w, vp_h)
  render_hud_cluster(ctx, 4, y_day, "A", 62, 80, 35, 1, true)
  draw_text(ctx, 4 + vp_w - 44, y_day + 5, "DAY SKY", rgba(240, 246, 255, 255), rgba(24, 34, 48, 255))

  paint_daytime_viewport(ctx, vp_w + 14, y_day, vp_w, vp_h)
  stroke_rect(ctx, vp_w + 14, y_day, vp_w, vp_h, rgba(46, 54, 68, 255))
  draw_vanilla_hud_ref(ctx, vp_w + 14, y_day, vp_w, vp_h)
  render_hud_cluster(ctx, vp_w + 14, y_day, "B", 62, 80, 35, 1, true)
  draw_text(ctx, vp_w + 14 + vp_w - 44, y_day + 5, "DAY SKY", rgba(240, 246, 255, 255), rgba(24, 34, 48, 255))

  local y_cave = y_day + vp_h + 10
  paint_cave_viewport(ctx, 4, y_cave, vp_w, vp_h)
  stroke_rect(ctx, 4, y_cave, vp_w, vp_h, rgba(42, 46, 56, 255))
  draw_vanilla_hud_ref(ctx, 4, y_cave, vp_w, vp_h)
  render_hud_cluster(ctx, 4, y_cave, "A", 62, 80, 68, 1, true)
  draw_text(ctx, 4 + vp_w - 52, y_cave + 5, "DARK CAVE", rgba(186, 196, 214, 255), rgba(8, 10, 14, 255))

  paint_cave_viewport(ctx, vp_w + 14, y_cave, vp_w, vp_h)
  stroke_rect(ctx, vp_w + 14, y_cave, vp_w, vp_h, rgba(42, 46, 56, 255))
  draw_vanilla_hud_ref(ctx, vp_w + 14, y_cave, vp_w, vp_h)
  render_hud_cluster(ctx, vp_w + 14, y_cave, "B", 62, 80, 68, 1, true)
  draw_text(ctx, vp_w + 14 + vp_w - 52, y_cave + 5, "DARK CAVE", rgba(186, 196, 214, 255), rgba(8, 10, 14, 255))

  local spr = Sprite(ctx.width, ctx.height, ColorMode.RGB)
  spr.layers[1].name = "compare_ab_hud_context_1x"
  spr.cels[1].image = ctx
  spr:saveAs(src_dir .. "/compare_ab_hud_context.aseprite")
  spr:close()

  local main_4x = scale_nn(ctx, 4)
  local inset_bar_h = 144
  local final_img = new_image(main_4x.width, main_4x.height + inset_bar_h, rgba(13, 15, 19, 255))
  blit(final_img, main_4x, 0, 0)

  local iy = main_4x.height + 6
  fill_rect(final_img, 16, iy, final_img.width - 32, inset_bar_h - 14, rgba(20, 23, 30, 255))
  stroke_rect(final_img, 16, iy, final_img.width - 32, inset_bar_h - 14, rgba(48, 56, 70, 255))

  local hdr = new_image(400, 8)
  draw_text(hdr, 0, 1, "TRUE 1x (RAW PIXEL) & 2x (GUI SCALE 2) INSETS:", rgba(210, 220, 238, 255), rgba(8, 10, 14, 255))
  blit(final_img, scale_nn(hdr, 2), 32, iy + 10)

  local crop_a_day  = new_image(160, 42); blit(crop_a_day,  ctx, 0, 0, 160, 42, 4, y_day)
  local crop_a_cave = new_image(160, 42); blit(crop_a_cave, ctx, 0, 0, 160, 42, 4, y_cave)
  local crop_b_day  = new_image(160, 42); blit(crop_b_day,  ctx, 0, 0, 160, 42, vp_w + 14, y_day)
  local crop_b_cave = new_image(160, 42); blit(crop_b_cave, ctx, 0, 0, 160, 42, vp_w + 14, y_cave)

  stroke_rect(crop_a_day,  0, 0, 160, 42, rgba(70, 84, 104, 255))
  stroke_rect(crop_a_cave, 0, 0, 160, 42, rgba(70, 84, 104, 255))
  stroke_rect(crop_b_day,  0, 0, 160, 42, rgba(96, 86, 68, 255))
  stroke_rect(crop_b_cave, 0, 0, 160, 42, rgba(96, 86, 68, 255))

  blit(final_img, crop_a_day,  32,  iy + 54)
  blit(final_img, crop_a_cave, 200, iy + 54)
  blit(final_img, scale_nn(crop_a_cave, 2), 372, iy + 34)

  local right_base = math.floor(final_img.width / 2) + 16
  blit(final_img, crop_b_day,  right_base,       iy + 54)
  blit(final_img, crop_b_cave, right_base + 168, iy + 54)
  blit(final_img, scale_nn(crop_b_cave, 2), right_base + 340, iy + 34)

  final_img:saveAs(prv_dir .. "/compare_ab_hud_context_4x.png")
end

-- ============================================================================
-- PREVIEW 3: states_progression_4x.png
-- ============================================================================
do
  local w, h = 420, 238
  local st = new_image(w, h, rgba(14, 16, 21, 255))

  fill_rect(st, 0, 0, w, 14, rgba(22, 25, 33, 255))
  draw_text(st, 8, 5, "STATES PROGRESSION: MANA FULLNESS (A/B) & WATER 4-TIER FLOW", rgba(226, 234, 248, 255), rgba(8, 10, 14, 255))

  fill_rect(st, 6, 18, 201, 102, rgba(19, 22, 30, 255))
  stroke_rect(st, 6, 18, 201, 102, rgba(42, 52, 82, 255))
  draw_text(st, 12, 23, "MANA A: STARLIGHT INDIGO", PAL_A.ridge, rgba(8, 10, 18, 255))

  fill_rect(st, 213, 18, 201, 102, rgba(21, 19, 18, 255))
  stroke_rect(st, 213, 18, 201, 102, rgba(72, 62, 48, 255))
  draw_text(st, 219, 23, "MANA B: WARM PLATINUM", PAL_B.ridge, rgba(16, 12, 8, 255))

  local mana_rows = {
    {val = 0,  ghost = nil, label = "0/80 EMPTY"},
    {val = 14, ghost = nil, label = "14/80 LOW"},
    {val = 48, ghost = nil, label = "48/80 HALF"},
    {val = 48, ghost = 68,  label = "48<-68 CAST"},
    {val = 80, ghost = nil, label = "80/80 FULL"},
  }
  for idx, r in ipairs(mana_rows) do
    local ry = 33 + (idx - 1) * 17
    render_mana_bar(st, 12, ry, "A", r.val, 80, 1, r.ghost)
    draw_text(st, 122, ry + 4, r.label, PAL_A.text, PAL_A.text_shadow)

    render_mana_bar(st, 219, ry, "B", r.val, 80, 1, r.ghost)
    draw_text(st, 329, ry + 4, r.label, PAL_B.text, PAL_B.text_shadow)
  end

  fill_rect(st, 6, 125, 408, 107, rgba(17, 22, 26, 255))
  stroke_rect(st, 6, 125, 408, 107, rgba(38, 66, 74, 255))
  draw_text(st, 12, 130, "WATER MOMENTUM TIERS (CAVE DARK VS DAY SKY): 20 & 50 CLASPS", PAL_W.stream_lip, rgba(8, 14, 18, 255))

  paint_daytime_viewport(st, 215, 139, 193, 88)
  stroke_rect(st, 215, 139, 193, 88, rgba(52, 76, 94, 255))

  local water_rows = {
    {val = 0,   label = "0 IDLE"},
    {val = 12,  label = "12 TRICKLE"},
    {val = 35,  label = "35 STREAM(>20)"},
    {val = 68,  label = "68 TORRENT(>50)"},
    {val = 100, label = "100 FULL SURGE"},
  }
  for idx, r in ipairs(water_rows) do
    local ry = 141 + (idx - 1) * 17
    render_water_bar(st, 12, ry, r.val, 1)
    draw_text(st, 122, ry + 4, r.label, PAL_W.text, PAL_W.text_shadow)

    render_water_bar(st, 221, ry, r.val, 1)
    draw_text(st, 331, ry + 4, r.label, PAL_W.text, PAL_W.text_shadow)
  end

  local spr = Sprite(st.width, st.height, ColorMode.RGB)
  spr.layers[1].name = "states_progression_1x"
  spr.cels[1].image = st
  spr:saveAs(src_dir .. "/states_progression.aseprite")
  spr:close()

  scale_nn(st, 4):saveAs(prv_dir .. "/states_progression_4x.png")
end

-- ============================================================================
-- PREVIEW 4: lowkey_flow_preview_4x.gif (16 frames, 90ms/frame = 1.44s loop)
-- ============================================================================
do
  local gw, gh = 368, 108
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

  local kf_sheet = new_image(182 * 2 + 12, 92 * 2 + 24, rgba(14, 16, 20, 255))
  draw_text(kf_sheet, 6, 4, "ANIMATION QA CHECK: FRAMES 1 / 5 / 9 / 13", rgba(220, 230, 246, 255), nil)
  local kf_idx = 0

  for f = 1, FRAMES do
    local frame_img = new_image(gw, gh, rgba(15, 17, 22, 255))
    fill_rect(frame_img, 0, 0, gw, 14, rgba(22, 25, 33, 255))
    draw_text(frame_img, 6, 5, "16F SEAMLESS LOOP (90ms/F): MANA A/B & WATER 4 TIERS", rgba(220, 230, 246, 255), rgba(8, 10, 14, 255))

    fill_rect(frame_img, 5, 18, 174, 85, rgba(19, 21, 27, 255))
    stroke_rect(frame_img, 5, 18, 174, 85, rgba(44, 50, 66, 255))
    draw_text(frame_img, 10, 22, "MANA SUBTLE LUSTER", rgba(176, 190, 214, 255), nil)

    render_mana_bar(frame_img, 10, 31, "A", 48, 80, f, nil)
    draw_text(frame_img, 118, 35, "A 48/80", PAL_A.text, PAL_A.text_shadow)

    render_mana_bar(frame_img, 10, 48, "A", 80, 80, f, nil)
    draw_text(frame_img, 118, 52, "A 80/80", PAL_A.text, PAL_A.text_shadow)

    render_mana_bar(frame_img, 10, 66, "B", 48, 80, f, nil)
    draw_text(frame_img, 118, 70, "B 48/80", PAL_B.text, PAL_B.text_shadow)

    render_mana_bar(frame_img, 10, 83, "B", 80, 80, f, nil)
    draw_text(frame_img, 118, 87, "B 80/80", PAL_B.text, PAL_B.text_shadow)

    fill_rect(frame_img, 184, 18, 179, 85, rgba(16, 21, 25, 255))
    stroke_rect(frame_img, 184, 18, 179, 85, rgba(38, 66, 74, 255))
    draw_text(frame_img, 190, 22, "WATER FLOW MAGNITUDE", PAL_W.stream_lip, nil)

    render_water_bar(frame_img, 190, 31, 12, f)
    draw_text(frame_img, 298, 35, "12 SLOW", PAL_W.text, PAL_W.text_shadow)

    render_water_bar(frame_img, 190, 48, 35, f)
    draw_text(frame_img, 298, 52, "35 MID", PAL_W.text, PAL_W.text_shadow)

    render_water_bar(frame_img, 190, 66, 68, f)
    draw_text(frame_img, 298, 70, "68 FAST", PAL_W.text, PAL_W.text_shadow)

    render_water_bar(frame_img, 190, 83, 100, f)
    draw_text(frame_img, 298, 87, "100 MAX", PAL_W.text, PAL_W.text_shadow)

    anim_spr:newCel(anim_spr.layers[1], f, frame_img, Point(0, 0))
    anim_4x:newCel(anim_4x.layers[1], f, scale_nn(frame_img, 4), Point(0, 0))

    if f == 1 or f == 5 or f == 9 or f == 13 then
      local kx = 4 + (kf_idx % 2) * 184
      local ky = 14 + math.floor(kf_idx / 2) * 92
      blit(kf_sheet, frame_img, kx, ky, 179, 85, 184, 18)
      draw_text(kf_sheet, kx + 142, ky + 4, string.format("F%d", f), rgba(255, 236, 170, 255), rgba(0, 0, 0, 255))
      kf_idx = kf_idx + 1
    end
  end

  anim_spr:saveAs(src_dir .. "/lowkey_flow_preview.aseprite")
  anim_4x:saveAs(prv_dir .. "/lowkey_flow_preview_4x.gif")
  anim_spr:close()
  anim_4x:close()
end

print("Round 5 HUD bars v3 generated successfully.")
