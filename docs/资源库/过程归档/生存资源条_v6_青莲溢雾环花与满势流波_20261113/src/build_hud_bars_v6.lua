-- ============================================================================
-- XueXi Mod HUD Resource Bars v6 (2x High-Density Pixel Craftsmanship)
--   1. Mana Icon (68x58, 0.5x HUD = 34x29): Ref2 D Blooming Ice-Azure Lotus
--      + Inner-Cup Overflowing Spirit Mist ("从花里溢出云雾")
--      + Multi-Tier Cloud-Mist Embracing Blossom ("多层云雾环绕花朵", ZERO dark halo)
--   2. Water Icon (48x40, 0.5x HUD = 24x20): Ref3 #8 Three S-Stream Blades
--      + STATIC when < 100/100; VISIBLY FLOWING (traveling S-waves + whitewater
--      crests + downstream spray) when FULL (100/100) + Clean Sapphire Cushion
--   3. Double-Stepped Chinese Fretwork Track (204x20, inner chamber 180x10;
--      0.5x HUD = 102x10, inner 90x5) preserving v4.1 acclaimed fill dynamics
-- ============================================================================

local root    = "D:/alianximod/docs/资源库/过程归档/生存资源条_v6_青莲溢雾环花与满势流波_20261113"
local ref_dir = root .. "/ref"
local src_dir = root .. "/src"
local exp_dir = root .. "/export"
local prv_dir = root .. "/preview"

local FRAMES = 16
local FRAME_DURATION = 0.09

local MANA_ICON_W, MANA_ICON_H   = 68, 58
local WATER_ICON_W, WATER_ICON_H = 48, 40
local TRACK_W, TRACK_H           = 204, 20
local FILL_W, FILL_H             = 180, 10
local FILL_X_OFFSET              = 18
local FILL_Y_OFFSET              = 5

local MANA_TRACK_DX  = 54
local MANA_TRACK_DY  = 15
local WATER_ICON_DX  = 8
local WATER_TRACK_DX = 54
local WATER_TRACK_DY = 10

local function rgba(r, g, b, a)
  return app.pixelColor.rgba(
    math.max(0, math.min(255, math.floor(r + 0.5))),
    math.max(0, math.min(255, math.floor(g + 0.5))),
    math.max(0, math.min(255, math.floor(b + 0.5))),
    math.max(0, math.min(255, math.floor((a or 255) + 0.5)))
  )
end

local function r_of(c) return app.pixelColor.rgbaR(c) end
local function g_of(c) return app.pixelColor.rgbaG(c) end
local function b_of(c) return app.pixelColor.rgbaB(c) end
local function a_of(c) return app.pixelColor.rgbaA(c) end

local function clamp(v, lo, hi)
  if v < lo then return lo end
  if v > hi then return hi end
  return v
end

local function smoothstep(e0, e1, x)
  local t = clamp((x - e0) / math.max(1e-6, e1 - e0), 0.0, 1.0)
  return t * t * (3.0 - 2.0 * t)
end

local function lerp(a, b, t)
  return a + (b - a) * t
end

local function lerp_color(c1, c2, t)
  if t <= 0 then return c1 end
  if t >= 1 then return c2 end
  return rgba(
    lerp(r_of(c1), r_of(c2), t),
    lerp(g_of(c1), g_of(c2), t),
    lerp(b_of(c1), b_of(c2), t),
    lerp(a_of(c1), a_of(c2), t)
  )
end

local function with_alpha(c, a)
  return rgba(r_of(c), g_of(c), b_of(c), a)
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
  local sa = a_of(c) / 255.0
  if sa <= 0.0 then return end
  if sa >= 0.999 then
    img:drawPixel(x, y, c)
    return
  end
  local dc = img:getPixel(x, y)
  local da = a_of(dc) / 255.0
  if da <= 0.0 then
    img:drawPixel(x, y, c)
    return
  end
  local out_a = sa + da * (1.0 - sa)
  local r = (r_of(c) * sa + r_of(dc) * da * (1.0 - sa)) / out_a
  local g = (g_of(c) * sa + g_of(dc) * da * (1.0 - sa)) / out_a
  local b = (b_of(c) * sa + b_of(dc) * da * (1.0 - sa)) / out_a
  img:drawPixel(x, y, rgba(r, g, b, out_a * 255.0))
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
      if sx + x >= 0 and sy + y >= 0 and sx + x < src.width and sy + y < src.height then
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

local function scale_nn_ratio(src, num, den)
  local dw = math.max(1, math.floor(src.width * num / den + 0.5))
  local dh = math.max(1, math.floor(src.height * num / den + 0.5))
  local dst = new_image(dw, dh)
  for y = 0, dh - 1 do
    local sy = math.min(src.height - 1, math.floor((y + 0.5) * den / num))
    for x = 0, dw - 1 do
      local sx = math.min(src.width - 1, math.floor((x + 0.5) * den / num))
      dst:drawPixel(x, y, src:getPixel(sx, sy))
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

local function build_soft_cushion(mask_img, r_col, g_col, b_col, a1, a2, a3, skip_fn)
  local w, h = mask_img.width, mask_img.height
  local out = new_image(w, h)
  for y = 0, h - 1 do
    for x = 0, w - 1 do
      if a_of(mask_img:getPixel(x, y)) < 200 and (not skip_fn or not skip_fn(x, y)) then
        local min_d2 = 999.0
        for dy = -3, 3 do
          local ny = y + dy
          if ny >= 0 and ny < h then
            for dx = -3, 3 do
              local nx = x + dx
              if nx >= 0 and nx < w then
                local ma = a_of(mask_img:getPixel(nx, ny))
                if ma >= 90 then
                  local d2 = dx * dx + dy * dy
                  if d2 < min_d2 then min_d2 = d2 end
                end
              end
            end
          end
        end
        if min_d2 <= 6.25 then
          local d = math.sqrt(min_d2)
          local alpha = 0
          if d <= 1.05 then alpha = a1
          elseif d <= 1.65 then alpha = a2
          elseif d <= 2.50 then alpha = a3 end
          if alpha > 0 then
            put_pixel(out, x, y, rgba(r_col, g_col, b_col, alpha))
          end
        end
      end
    end
  end
  return out
end

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
  ["%"] = {"#.#", "..#", ".#.", "#..", "#.#"},
  ["&"] = {".#.", "#.#", ".#.", "#.#", ".##"},
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

local function draw_label_scaled(dst, x, y, scale, str, color, shadow)
  local tw = #str * 6 + 8
  local tmp = new_image(tw, 7)
  draw_text(tmp, 1, 1, str, color, shadow)
  blit(dst, scale_nn(tmp, scale), x, y)
end

local PAL_FRAME = {
  ink_outer   = rgba(17, 22, 32, 255),
  groove_dark = rgba(23, 30, 43, 255),
  slate_dark  = rgba(44, 55, 75, 255),
  stone_mid   = rgba(66, 80, 106, 255),
  stone_lit   = rgba(92, 110, 140, 255),
  fret_hi     = rgba(118, 139, 172, 255),
  fret_peak   = rgba(146, 168, 200, 255),
}

local PAL_MANA = {
  cav_shadow = rgba(14, 20, 38, 255),
  cav_bed_1  = rgba(20, 28, 52, 255),
  cav_bed_2  = rgba(24, 34, 62, 255),
  cav_mark   = rgba(31, 43, 76, 255),
  deep       = rgba(54, 86, 162, 255),
  mid_dark   = rgba(74, 114, 198, 255),
  body       = rgba(102, 148, 228, 255),
  silk       = rgba(144, 186, 248, 255),
  cloud_lit  = rgba(188, 220, 255, 255),
  volute     = rgba(228, 243, 255, 255),
  crown      = rgba(247, 252, 255, 255),
  low_deep   = rgba(32, 50, 96, 255),
  low_mid    = rgba(46, 72, 132, 255),
  low_body   = rgba(64, 98, 168, 255),
  low_silk   = rgba(94, 134, 202, 255),
  low_lit    = rgba(126, 166, 226, 255),
  full_deep  = rgba(72, 110, 192, 255),
  full_mid   = rgba(96, 140, 222, 255),
  full_body  = rgba(128, 174, 244, 255),
  full_silk  = rgba(172, 210, 255, 255),
  full_lit   = rgba(214, 236, 255, 255),
  full_crest = rgba(246, 252, 255, 255),
  ghost_hi   = rgba(146, 182, 238, 255),
  ghost_mid  = rgba(92, 124, 186, 255),
  ghost_low  = rgba(52, 72, 124, 255),
  text        = rgba(226, 238, 255, 255),
  text_shadow = rgba(14, 20, 40, 255),
}

local PAL_WATER = {
  cav_shadow = rgba(7, 24, 32, 255),
  cav_bed_1  = rgba(10, 32, 42, 255),
  cav_bed_2  = rgba(13, 38, 49, 255),
  cav_notch  = rgba(22, 54, 68, 255),
  clasp_ink        = rgba(14, 21, 28, 255),
  clasp_unlit_edge = rgba(34, 49, 62, 255),
  clasp_unlit_core = rgba(48, 72, 88, 255),
  clasp_lit_edge   = rgba(44, 92, 112, 255),
  clasp_lit_20     = rgba(110, 236, 246, 255),
  clasp_lit_50     = rgba(206, 255, 255, 255),
  t1_deep    = rgba(14, 66, 82, 255),
  t1_body    = rgba(22, 98, 118, 255),
  t1_crest   = rgba(38, 134, 156, 255),
  t1_glint   = rgba(66, 168, 188, 255),
  t2_deep    = rgba(15, 76, 94, 255),
  t2_mid     = rgba(24, 118, 138, 255),
  t2_body    = rgba(36, 158, 180, 255),
  t2_vein    = rgba(74, 206, 222, 255),
  t2_blade   = rgba(112, 224, 236, 255),
  t2_tip     = rgba(176, 246, 252, 255),
  t3_deep    = rgba(16, 86, 106, 255),
  t3_mid     = rgba(28, 134, 156, 255),
  t3_body    = rgba(46, 178, 198, 255),
  t3_surge   = rgba(94, 226, 238, 255),
  t3_blade   = rgba(168, 248, 254, 255),
  t3_foam    = rgba(232, 255, 255, 255),
  text        = rgba(210, 248, 252, 255),
  gain_text   = rgba(124, 242, 248, 255),
  text_shadow = rgba(8, 24, 30, 255),
}

local LOTUS_CX = 33
local function mir_x(x) return 66 - x end

local function put_sym(img, x, y, c)
  put_pixel(img, x, y, c)
  local mx = mir_x(x)
  if mx ~= x then
    put_pixel(img, mx, y, c)
  end
end

local function draw_center_petal(img, y_top, half_widths, fill_fn, rim_fn)
  local n = #half_widths
  local prev_hw = -1
  for i = 1, n do
    local y = y_top + i - 1
    local hw = half_widths[i]
    for dx = 0, hw do
      local is_rim = (i == 1)
                  or (dx == hw)
                  or (prev_hw >= 0 and dx > prev_hw)
      local c
      if is_rim then
        c = rim_fn(i - 1, n - 1, dx, hw)
      else
        c = fill_fn(i - 1, n - 1, dx, hw)
      end
      if c then put_sym(img, LOTUS_CX - dx, y, c) end
    end
    prev_hw = hw
  end
end

local function draw_paired_petal(img, y_top, spans, fill_fn, rim_fn)
  local n = #spans
  for i = 1, n do
    local y = y_top + i - 1
    local xl, xr = spans[i][1], spans[i][2]
    local p_xl = spans[i - 1] and spans[i - 1][1] or nil
    local p_xr = spans[i - 1] and spans[i - 1][2] or nil
    local n_xl = spans[i + 1] and spans[i + 1][1] or nil
    local n_xr = spans[i + 1] and spans[i + 1][2] or nil
    local w = math.max(1, xr - xl)
    for x = xl, xr do
      local top_expose = (not p_xl) or (x < p_xl) or (x > p_xr)
      local bot_expose = (not n_xl) or (x < n_xl) or (x > n_xr)
      local is_outer_rim = (x == xl) or top_expose
      local is_lower_rim = bot_expose and (i >= math.floor(n * 0.45))
      local is_inner_rim = (x == xr)
      local ty = (i - 1) / math.max(1, n - 1)
      local tx = (x - xl) / w
      local c
      if is_outer_rim or is_inner_rim or is_lower_rim then
        c = rim_fn(ty, tx, is_outer_rim or top_expose, is_inner_rim, is_lower_rim)
      else
        c = fill_fn(ty, tx, x, y)
      end
      if c then put_sym(img, x, y, c) end
    end
  end
end

local function make_mana_icon_layers(mode, frame_idx)
  local f0 = (frame_idx or 1) - 1
  local phase = f0 / FRAMES
  local theta = 2.0 * math.pi * phase
  local breath = 0.5 + 0.5 * math.sin(theta)
  local is_empty = (mode == "empty")
  local is_full  = (mode == "full")

  local l_ring           = new_image(MANA_ICON_W, MANA_ICON_H)
  local l_rear_embrace   = new_image(MANA_ICON_W, MANA_ICON_H)
  local l_petals_back    = new_image(MANA_ICON_W, MANA_ICON_H)
  local l_inner_overflow = new_image(MANA_ICON_W, MANA_ICON_H)
  local l_petals_front   = new_image(MANA_ICON_W, MANA_ICON_H)
  local l_front_embrace  = new_image(MANA_ICON_W, MANA_ICON_H)

  local r_cx, r_cy = 33.0, 24.5
  local r_in, r_out = 17.5, 19.7
  for y = 4, 45 do
    for x = 12, 54 do
      local dx = x - r_cx
      local dy = y - r_cy
      local d = math.sqrt(dx * dx + dy * dy)
      if d <= r_in then
        local ny = dy / r_in
        local swirl = math.sin(dx * 0.34 + dy * 0.25 - theta * 0.5) * math.cos(dx * 0.18 - dy * 0.32 + theta * 0.5)
        local base = lerp_color(rgba(13, 30, 50, 255), rgba(9, 19, 34, 255), clamp((ny + 1.0) * 0.5, 0, 1))
        if not is_empty and ny < 0.22 then
          local cup_aura = math.max(0.0, 1.0 - math.sqrt((dx / 12.5) ^ 2 + ((dy + 5.5) / 8.5) ^ 2))
          if cup_aura > 0 then
            base = lerp_color(base, rgba(44, 108, 168, 255), cup_aura * (is_full and 0.56 or 0.40) * (0.82 + 0.18 * breath))
          end
          if d > 10.5 and swirl > 0.10 then
            base = lerp_color(base, rgba(26, 74, 118, 255), (swirl - 0.10) * (is_full and 0.75 or 0.55))
          end
        end
        if d > r_in - 1.0 then
          base = lerp_color(base, rgba(8, 13, 22, 255), 0.75)
        end
        put_pixel(l_ring, x, y, base)
      elseif d <= r_out then
        local ny = dy / r_out
        local edge_dist = math.min(d - r_in, r_out - d)
        local metal
        if ny < -0.52 then
          local top_t = clamp((-ny - 0.52) / 0.48, 0, 1)
          metal = lerp_color(rgba(92, 116, 142, 255), rgba(162, 206, 222, 255), top_t)
        elseif ny < 0.20 then
          metal = lerp_color(rgba(88, 108, 134, 255), rgba(56, 70, 92, 255), (ny + 0.52) / 0.72)
        else
          local bot_glow = math.max(0.0, 1.0 - math.abs(dx) / 8.0) * 0.35
          metal = lerp_color(rgba(48, 60, 82, 255), rgba(84, 112, 142, 255), bot_glow)
        end
        if is_empty then
          metal = lerp_color(metal, rgba(36, 44, 60, 255), 0.45)
        end
        if edge_dist < 0.52 then
          metal = lerp_color(rgba(12, 18, 28, 255), metal, edge_dist / 0.52 * 0.55)
        end
        put_pixel(l_ring, x, y, metal)
      elseif d <= r_out + 0.75 then
        local a = clamp((r_out + 0.75 - d) / 0.75, 0, 1) * 185
        put_pixel(l_ring, x, y, rgba(10, 16, 26, a))
      end
    end
  end

  do
    local b_hi  = is_empty and rgba(112, 146, 182, 255) or rgba(255, 255, 255, 255)
    local b_ice = is_empty and rgba(78, 112, 152, 255)  or (is_full and rgba(186, 250, 255, 255) or rgba(152, 242, 255, 255))
    local b_mid = is_empty and rgba(52, 78, 118, 255)   or rgba(74, 194, 244, 255)
    local b_dp  = is_empty and rgba(34, 52, 84, 255)    or rgba(40, 118, 198, 255)
    put_sym(l_ring, 33, 2, b_mid)
    put_sym(l_ring, 32, 3, b_mid); put_sym(l_ring, 33, 3, b_ice)
    put_sym(l_ring, 31, 4, b_dp);  put_sym(l_ring, 32, 4, b_ice); put_sym(l_ring, 33, 4, b_hi)
    put_sym(l_ring, 32, 5, b_dp);  put_sym(l_ring, 33, 5, b_ice)
    put_sym(l_ring, 33, 6, b_mid)
  end

  do
    local gold_stem = is_empty and rgba(96, 92, 88, 255) or rgba(255, 232, 182, 255)
    local d_hi  = is_empty and rgba(118, 150, 184, 255) or rgba(255, 255, 255, 255)
    local d_ice = is_empty and rgba(82, 116, 156, 255)  or rgba(168, 244, 255, 255)
    local d_cyn = is_empty and rgba(56, 86, 126, 255)   or rgba(82, 206, 250, 255)
    local d_blu = is_empty and rgba(38, 60, 96, 255)    or rgba(42, 124, 210, 255)
    local d_dp  = is_empty and rgba(26, 42, 70, 255)    or rgba(28, 86, 166, 255)

    put_sym(l_ring, 33, 43, gold_stem)
    put_sym(l_ring, 33, 44, d_ice)
    put_sym(l_ring, 32, 45, d_cyn); put_sym(l_ring, 33, 45, d_hi)
    put_sym(l_ring, 32, 46, d_blu); put_sym(l_ring, 33, 46, d_cyn)
    put_sym(l_ring, 33, 47, gold_stem)
    put_sym(l_ring, 33, 48, d_ice)
    put_sym(l_ring, 32, 49, d_cyn); put_sym(l_ring, 33, 49, d_hi)
    put_sym(l_ring, 31, 50, d_blu); put_sym(l_ring, 32, 50, d_ice); put_sym(l_ring, 33, 50, d_hi)
    put_sym(l_ring, 31, 51, d_dp);  put_sym(l_ring, 32, 51, d_cyn); put_sym(l_ring, 33, 51, d_ice)
    put_sym(l_ring, 32, 52, d_blu); put_sym(l_ring, 33, 52, d_cyn)
    put_sym(l_ring, 33, 53, d_cyn)
    put_sym(l_ring, 33, 54, d_blu)
  end

  local function petal_palette(ty, core_prox)
    if is_empty then
      local c_tip = rgba(34, 56, 102, 255)
      local c_mid = rgba(52, 84, 138, 255)
      local c_low = rgba(68, 106, 162, 255)
      if ty < 0.42 then return lerp_color(c_tip, c_mid, ty / 0.42) end
      return lerp_color(c_mid, c_low, (ty - 0.42) / 0.58)
    end
    local tip_sapphire = is_full and rgba(54, 98, 224, 255)  or rgba(47, 90, 214, 255)
    local mid_azure    = is_full and rgba(92, 174, 250, 255) or rgba(78, 162, 246, 255)
    local belly_cyan   = is_full and rgba(138, 224, 255, 255) or rgba(114, 210, 252, 255)
    local base_c
    if ty < 0.36 then
      base_c = lerp_color(tip_sapphire, mid_azure, ty / 0.36)
    else
      base_c = lerp_color(mid_azure, belly_cyan, (ty - 0.36) / 0.64)
    end
    if core_prox and core_prox > 0 then
      local lav = rgba(224, 196, 255, 255)
      base_c = lerp_color(base_c, lav, clamp(core_prox, 0, 1))
    end
    return base_c
  end

  local rim_snow = is_empty and rgba(132, 164, 204, 255) or rgba(242, 250, 255, 255)
  local rim_soft = is_empty and rgba(96, 130, 174, 255)  or rgba(196, 236, 255, 255)

  -- ==========================================================================
  -- BACK PETALS LAYER (l_petals_back): Tall Flanking + Central Crown + Wide Wings
  -- ==========================================================================
  local back_tall_spans = {
    {17, 17}, {17, 18}, {16, 20}, {16, 21}, {16, 23},
    {16, 24}, {16, 25}, {16, 26}, {17, 26}, {17, 27},
    {17, 27}, {18, 27}, {18, 27}, {19, 27}, {20, 27},
  }
  draw_paired_petal(l_petals_back, 11, back_tall_spans,
    function(ty, tx)
      local shade = petal_palette(ty, (ty > 0.55 and tx > 0.55) and ((ty - 0.55) * 0.85) or 0)
      if tx > 0.72 and ty < 0.70 and not is_empty then
        shade = lerp_color(shade, rgba(46, 86, 206, 255), 0.35)
      end
      return shade
    end,
    function(ty, _, is_outer)
      return (is_outer or ty < 0.55) and rim_snow or rim_soft
    end
  )

  local center_crown_hw = {
    0, 1, 2, 3, 4, 5, 5, 6, 6, 7, 7, 8, 8, 8, 8, 8, 7, 7, 7, 6
  }
  draw_center_petal(l_petals_back, 6, center_crown_hw,
    function(i, n, dx, hw)
      local ty = i / math.max(1, n)
      local ridge = (hw > 0) and (1.0 - dx / hw) or 0
      local c = petal_palette(ty * 0.92, (ty > 0.52) and ((ty - 0.52) * 0.95 * ridge) or 0)
      if ty < 0.42 and dx <= 1 and not is_empty then
        c = lerp_color(c, rgba(44, 82, 208, 255), 0.25)
      end
      return c
    end,
    function(i, n)
      return (i / math.max(1, n) < 0.82) and rim_snow or rim_soft
    end
  )

  local wing_spans = {
    {7,  8},  {8,  11}, {9,  15}, {9,  18}, {10, 21},
    {11, 23}, {11, 24}, {12, 25}, {13, 25}, {14, 25},
    {15, 25}, {16, 25}, {18, 25}, {20, 25},
  }
  draw_paired_petal(l_petals_back, 18, wing_spans,
    function(ty, tx)
      local tip_t = clamp(tx * 0.72 + ty * 0.42, 0, 1)
      local c = petal_palette(tip_t, (tx > 0.68 and ty > 0.25) and ((tx - 0.68) * 1.3) or 0)
      if ty > 0.78 and not is_empty then
        c = lerp_color(c, rgba(45, 96, 208, 255), (ty - 0.78) * 1.6)
      end
      return c
    end,
    function(ty, tx, is_outer, _, is_lower)
      if is_lower and tx > 0.45 then return rim_soft end
      return (is_outer or ty < 0.75) and rim_snow or rim_soft
    end
  )

  -- ==========================================================================
  -- FRONT CUPPING PETALS & CORE LAYER (l_petals_front)
  -- ==========================================================================
  local inner_cup_spans = {
    {22, 22}, {22, 24}, {21, 25}, {21, 27}, {21, 28},
    {21, 29}, {22, 29}, {22, 30}, {23, 30}, {23, 30},
    {24, 30}, {25, 30}, {26, 30}, {27, 30},
  }
  draw_paired_petal(l_petals_front, 16, inner_cup_spans,
    function(ty, tx)
      if is_empty then return petal_palette(ty * 0.8 + 0.15, 0) end
      local c_tip  = rgba(114, 162, 248, 255)
      local c_lav  = rgba(218, 192, 255, 255)
      local c_warm = rgba(255, 242, 208, 255)
      local c = lerp_color(c_tip, c_lav, smoothstep(0.05, 0.58, ty))
      local warm_w = smoothstep(0.32, 0.90, ty) * smoothstep(0.15, 0.95, tx)
      return lerp_color(c, c_warm, warm_w * (is_full and 0.92 or 0.80))
    end,
    function(ty)
      if is_empty then return rim_snow end
      return (ty > 0.45) and rgba(255, 253, 248, 255) or rgba(246, 251, 255, 255)
    end
  )

  local inner_center_hw = {
    0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 5, 5, 5, 4, 4, 3, 2, 1
  }
  draw_center_petal(l_petals_front, 13, inner_center_hw,
    function(i, n, dx, hw)
      local ty = i / math.max(1, n)
      if is_empty then
        return lerp_color(rgba(44, 68, 118, 255), rgba(74, 108, 162, 255), ty)
      end
      local r_norm = (hw > 0) and (dx / hw) or 0
      local apex_lav = lerp_color(rgba(152, 142, 248, 255), rgba(236, 198, 255, 255), clamp(ty / 0.38, 0, 1))
      local glow_boost = (is_full and 0.14 or 0.0) + 0.08 * breath
      local core_t = smoothstep(0.24 - glow_boost, 0.68, ty) * (1.0 - 0.38 * r_norm)
      local warm_gold = lerp_color(rgba(255, 224, 162, 255), rgba(255, 255, 255, 255), smoothstep(0.42, 0.80, core_t))
      local c = lerp_color(apex_lav, warm_gold, smoothstep(0.18, 0.54, ty))
      if ty > 0.80 then
        c = lerp_color(c, rgba(132, 212, 255, 255), (ty - 0.80) / 0.20 * 0.65)
      end
      return c
    end,
    function(i, n)
      if is_empty then return rim_snow end
      local ty = i / math.max(1, n)
      return (ty > 0.38) and rgba(255, 254, 246, 255) or rgba(248, 250, 255, 255)
    end
  )

  if not is_empty then
    local core_r = (is_full and 6.6 or 5.6) + 0.55 * breath
    for y = 17, 30 do
      for x = 26, 40 do
        local cur = l_petals_front:getPixel(x, y)
        if a_of(cur) > 0 then
          local is_bright_rim = (r_of(cur) >= 244 and g_of(cur) >= 249 and b_of(cur) >= 245)
          local dx = (x - 33.0) / 1.00
          local dy = (y - 24.6) / 1.22
          local d = math.sqrt(dx * dx + dy * dy)
          if d < core_r then
            local k = (1.0 - d / core_r) ^ 1.65
            if is_bright_rim and d > 2.2 then
              k = k * 0.25
            end
            local target = (d < 2.3) and rgba(255, 255, 255, 255)
                        or ((y < 22) and rgba(238, 204, 255, 255) or rgba(255, 236, 184, 255))
            l_petals_front:drawPixel(x, y, lerp_color(cur, target, k * (is_full and 0.82 or 0.70)))
          end
        end
      end
    end
  end

  local lower_guard_spans = {
    {13, 18}, {13, 21}, {14, 24}, {15, 26},
    {16, 27}, {18, 28}, {20, 28}, {23, 27},
  }
  draw_paired_petal(l_petals_front, 30, lower_guard_spans,
    function(ty, tx)
      if is_empty then return petal_palette(0.35 + ty * 0.4, 0) end
      local c1 = rgba(46, 98, 212, 255)
      local c2 = rgba(108, 206, 252, 255)
      return lerp_color(c1, c2, clamp(ty * 0.75 + (1.0 - tx) * 0.35, 0, 1))
    end,
    function(_, _, is_outer, _, is_lower)
      return (is_lower or is_outer) and rim_snow or rim_soft
    end
  )

  local front_cup_spans = {
    {16, 17}, {16, 20}, {17, 22}, {17, 25}, {18, 27},
    {19, 29}, {20, 31}, {21, 32}, {22, 33}, {24, 33},
    {26, 33}, {28, 33}, {31, 33},
  }
  draw_paired_petal(l_petals_front, 24, front_cup_spans,
    function(ty, tx)
      if is_empty then return petal_palette(0.25 + ty * 0.55, 0) end
      local hi = rgba(164, 236, 255, 255)
      local mid = rgba(88, 186, 248, 255)
      local shd = rgba(48, 106, 218, 255)
      local v = clamp((1.0 - ty) * 0.55 + tx * 0.45, 0, 1)
      if v > 0.58 then
        return lerp_color(mid, hi, (v - 0.58) / 0.42)
      else
        return lerp_color(shd, mid, v / 0.58)
      end
    end,
    function(ty, tx, is_outer, is_inner, is_lower)
      if is_inner and ty >= 0.62 then
        return is_empty and rgba(38, 62, 108, 255) or rgba(56, 122, 226, 255)
      end
      if not is_outer and not is_lower and tx > 0.35 then
        return is_empty and rim_snow or rgba(250, 254, 255, 255)
      end
      return rim_snow
    end
  )

  local down_calyx_hw = {5, 6, 6, 5, 4, 3, 2, 0}
  for i, hw in ipairs(down_calyx_hw) do
    local y = 35 + i - 1
    local next_hw = down_calyx_hw[i + 1]
    local ty = (i - 1) / (#down_calyx_hw - 1)
    for dx = 0, hw do
      local is_bot_rim = (not next_hw) or (dx > next_hw)
      local c
      if is_bot_rim then
        c = rim_snow
      elseif i == 1 then
        c = is_empty and rgba(30, 50, 92, 255) or rgba(42, 86, 202, 255)
      else
        if is_empty then
          c = lerp_color(rgba(38, 64, 114, 255), rgba(74, 114, 168, 255), ty)
        else
          c = lerp_color(rgba(48, 98, 214, 255), rgba(116, 220, 255, 255), ty)
        end
      end
      put_sym(l_petals_front, LOTUS_CX - dx, y, c)
    end
  end

  -- ==========================================================================
  -- V6 INNER-CUP MIST OVERFLOW + MULTI-TIER CLOUD EMBRACE (PURE VAPOR, NO DARK SHADOW!)
  -- ==========================================================================
  if not is_empty then
    local m_snow   = rgba(252, 255, 255, 255)
    local m_pearl  = is_full and rgba(232, 252, 255, 255) or rgba(218, 248, 255, 255)
    local m_cyan   = is_full and rgba(124, 224, 255, 255) or rgba(106, 214, 255, 255)
    local m_sky    = rgba(78, 192, 252, 255)
    local m_lav    = rgba(236, 206, 255, 255)
    local m_ivory  = rgba(255, 248, 222, 255)
    local str_mult = is_full and 1.18 or 1.00

    -- Helper: stamp a single-side soft vapor puff with crisp silk center & vibrant cyan/lavender feathering
    -- Strictly bounded inside x = 2..65 so zero horizontal edge clipping ever occurs!
    local function stamp_vapor_puff_1side(layer, side_sign, dx_center, y_center, rx, ry, col_core, col_edge, peak_alpha, protect_tips)
      if peak_alpha <= 4 then return end
      local cx = (side_sign < 0) and (LOTUS_CX - dx_center) or (mir_x(LOTUS_CX) + dx_center)
      local x0 = math.max(1, math.floor(cx - rx - 0.5))
      local x1 = math.min(MANA_ICON_W - 2, math.ceil(cx + rx + 0.5))
      local y0 = math.floor(y_center - ry - 0.5)
      local y1 = math.ceil(y_center + ry + 0.5)
      for iy = y0, y1 do
        if iy >= 1 and iy < MANA_ICON_H - 1 then
          for ix = x0, x1 do
            local adx = math.abs(ix - r_cx)
            -- Soft boundary fade near extreme left/right canvas margins (adx > 28.5 -> 0 by adx = 31.5)
            local edge_guard = 1.0 - smoothstep(28.2, 31.4, adx)
            -- Keep sharp petal tips unobstructed when `protect_tips` is enabled
            if protect_tips then
              if iy <= 9 and adx <= 2.2 then edge_guard = edge_guard * 0.15 end
              if iy >= 10 and iy <= 13 and adx >= 15.2 and adx <= 17.8 then edge_guard = edge_guard * 0.25 end
              if iy >= 17 and iy <= 20 and adx >= 23.5 and adx <= 26.5 then edge_guard = edge_guard * 0.25 end
            end
            if edge_guard > 0.04 then
              local nx = (ix - cx) / math.max(0.45, rx)
              local ny = (iy - y_center) / math.max(0.45, ry)
              local d2 = nx * nx + ny * ny
              if d2 < 1.0 then
                local k = (1.0 - d2) ^ 1.35
                local a = clamp(math.floor(peak_alpha * k * edge_guard + 0.5), 0, 252)
                if a >= 8 then
                  local c = lerp_color(col_edge, col_core, k ^ 0.65)
                  put_pixel(layer, ix, iy, with_alpha(c, a))
                end
              end
            end
          end
        end
      end
    end

    local function stamp_sym_vapor_puff(layer, dx_center, y_center, rx, ry, col_core, col_edge, peak_alpha, protect_tips)
      stamp_vapor_puff_1side(layer, -1, dx_center, y_center, rx, ry, col_core, col_edge, peak_alpha, protect_tips)
      stamp_vapor_puff_1side(layer,  1, dx_center, y_center, rx, ry, col_core, col_edge, peak_alpha, protect_tips)
    end

    local function draw_1side_mist_ribbon(layer, side_sign, steps, pos_fn, radius_fn, alpha_fn, col_fn, protect_tips)
      for s = 0, steps do
        local u = s / steps
        local dx_c, y_c = pos_fn(u)
        local rx, ry = radius_fn(u)
        local al = alpha_fn(u)
        if al > 5 then
          local c_core, c_edge = col_fn(u)
          stamp_vapor_puff_1side(layer, side_sign, dx_c, y_c, rx, ry, c_core, c_edge, al, protect_tips)
        end
      end
    end

    -- ------------------------------------------------------------------------
    -- PART A: INNER-CUP MIST OVERFLOW ("从花里溢出云雾")
    --   1. Deep inner-cup glowing vapor bed in `l_inner_overflow` (between back & front petals)
    --   2. Crisp-cored rising plumes welling directly out of the golden-lavender heart,
    --      arching UP through the deep-sapphire petal valleys AND spilling OVER the front
    --      cupping petal lips with organic left/right breathing phase offset!
    -- ------------------------------------------------------------------------
    stamp_sym_vapor_puff(l_inner_overflow, 3.5, 18.8 - 0.4 * breath, 5.2, 4.2, m_ivory, m_lav,  (is_full and 215 or 182) + 20 * breath, true)
    stamp_sym_vapor_puff(l_inner_overflow, 8.8, 17.2 - 0.5 * breath, 5.4, 3.6, m_snow,  m_cyan, (is_full and 205 or 172) + 20 * breath, true)
    -- Subtle warm-snow vapor pool glowing right inside the flower throat (on front layer so you see the mist source!)
    stamp_sym_vapor_puff(l_front_embrace,  2.2, 22.8 - 0.3 * breath, 3.6, 2.2, m_snow,  m_ivory,(is_full and 168 or 138) + 24 * breath, true)

    local function emit_overflow_plume_1side(target_layer, side_sign, t_cyc, dx0, y0, dx_mid, y_mid, dx1, y1, rad_scale, alpha_peak)
      local env = math.sin(math.pi * t_cyc)
      if env <= 0.03 then return end
      local function bezier(u)
        local om = 1.0 - u
        return om * om * dx0 + 2.0 * om * u * dx_mid + u * u * dx1,
               om * om * y0  + 2.0 * om * u * y_mid  + u * u * y1
      end
      local u_head = 0.08 + 0.88 * t_cyc
      local seg_half = 0.30
      draw_1side_mist_ribbon(target_layer, side_sign, 26,
        function(s)
          local u = clamp(u_head - seg_half + 2.0 * seg_half * s, 0.0, 1.0)
          return bezier(u)
        end,
        function(s)
          local head_swell = math.sin(math.pi * (s ^ 0.78))
          return (0.95 + 1.15 * head_swell) * rad_scale, (0.78 + 0.82 * head_swell) * rad_scale
        end,
        function(s)
          local prof = math.sin(math.pi * (s ^ 0.80))
          return alpha_peak * env * prof * str_mult
        end,
        function(s)
          local u = clamp(u_head - seg_half + 2.0 * seg_half * s, 0.0, 1.0)
          if u < 0.28 then
            return m_ivory, m_lav
          elseif u < 0.62 then
            return m_snow, m_pearl
          else
            return m_snow, m_cyan
          end
        end,
        true
      )
    end

    -- Organic left/right phase offset (0.28 cycle) so mist billows naturally from inside the flower!
    for _, side in ipairs({{s = -1, off = 0.00}, {s = 1, off = 0.28}}) do
      local p = (phase + side.off) % 1.0
      -- 1. Inner-valley background upwelling (between back & front petals)
      emit_overflow_plume_1side(l_inner_overflow, side.s, (p + 0.00) % 1.0, 3.0, 21.5,  8.5, 13.0, 19.5,  8.8, 1.02, 228)
      emit_overflow_plume_1side(l_inner_overflow, side.s, (p + 0.50) % 1.0, 5.2, 23.2, 13.2, 17.2, 25.8, 14.2, 1.00, 220)

      -- 2. Foreground VISIBLE cup-overflow plumes rising directly out of the golden flower heart
      --    across the deep-blue upper sapphire petal valleys (`y = 22 -> 13 -> 8`)!
      emit_overflow_plume_1side(l_front_embrace,  side.s, (p + 0.16) % 1.0, 2.4, 22.0,  7.4, 14.2, 16.8,  9.4, 0.86, 214)
      emit_overflow_plume_1side(l_front_embrace,  side.s, (p + 0.66) % 1.0, 3.4, 22.6,  9.6, 15.8, 20.5, 11.8, 0.82, 198)

      -- 3. Foreground Over-the-Lip Spill cascading over front cupping petals into mid-waist embrace!
      emit_overflow_plume_1side(l_front_embrace,  side.s, (p + 0.38) % 1.0, 2.8, 23.2, 11.2, 21.2, 23.8, 24.4, 0.90, 196)
      emit_overflow_plume_1side(l_front_embrace,  side.s, (p + 0.88) % 1.0, 4.0, 24.2, 13.0, 23.4, 25.5, 26.2, 0.85, 182)

      if is_full then
        emit_overflow_plume_1side(l_front_embrace, side.s, (p + 0.42) % 1.0, 1.8, 20.2, 5.4, 11.6, 13.2, 7.2, 0.82, 204)
      end
    end

    -- ------------------------------------------------------------------------
    -- PART B: MULTI-TIER CLOUD-MIST EMBRACING THE BLOSSOM ("多层云雾环绕花朵")
    --   Vibrant ice-cyan feathering + high-alpha snow-white silk core -> glowing
    --   celestial clouds on dark cave AND pure crisp snow-clouds on daytime sky!
    -- ------------------------------------------------------------------------

    -- Tier 1: Upper-Shoulder C-Scroll Embracing Cloud Ribbons (`l_rear_embrace`, y = 9..19)
    for _, side in ipairs({{s = -1, off = 0.0}, {s = 1, off = 0.22}}) do
      local p_side = (phase + side.off) % 1.0
      local sway = math.sin(2.0 * math.pi * p_side)
      draw_1side_mist_ribbon(l_rear_embrace, side.s, 30,
        function(u)
          local dx_c = 13.5 + 12.6 * math.sin(u * math.pi * 0.80) + 0.85 * sway * u
          local y_c  = 17.2 - 7.4 * (u ^ 0.90) + 0.55 * math.cos(2.0 * math.pi * p_side + u * 3.2)
          return dx_c, y_c
        end,
        function(u)
          local curl_head = (u > 0.66) and (0.50 * math.sin((u - 0.66) / 0.34 * math.pi)) or 0.0
          return 1.15 + 0.55 * math.sin(u * math.pi) + curl_head, 0.85 + 0.38 * math.sin(u * math.pi) + curl_head * 0.7
        end,
        function(u)
          local travel = 0.72 + 0.28 * math.sin(2.0 * math.pi * (u * 1.15 - p_side))
          return (is_full and 236 or 212) * (math.sin(u * math.pi) ^ 0.58) * travel
        end,
        function(u)
          return m_snow, (u > 0.5) and m_cyan or m_sky
        end,
        false
      )
    end

    -- Tier 2: Mid-Waist 3D Embracing Cloud Sash (Strictly inside x = 3..64, wrapping around wing tips & across waist!)
    do
      for ang_step = 0, 359 do
        local a = (ang_step / 360.0) * math.pi
        local ca, sa = math.cos(a), math.sin(a)
        local adx = 28.4 * math.abs(ca)
        if adx >= 18.8 then
          local px = r_cx + 28.4 * ca
          local py = 24.8 - 3.2 * sa
          local wave = 0.65 + 0.35 * math.sin(2.0 * math.pi * ((adx - 18.8) / 12.0 - phase))
          local taper = smoothstep(18.8, 22.8, adx)
          local al = clamp(math.floor((is_full and 232 or 206) * taper * wave), 0, 248)
          local ix, iy = math.floor(px + 0.5), math.floor(py + 0.5)
          put_pixel(l_rear_embrace, ix, iy, with_alpha(lerp_color(m_cyan, m_snow, wave), al))
          put_pixel(l_rear_embrace, ix, iy + 1, with_alpha(m_sky, math.floor(al * 0.68)))
        end
      end

      for _, side in ipairs({{s = -1, off = 0.0}, {s = 1, off = 0.18}}) do
        local p_side = (phase + side.off) % 1.0
        draw_1side_mist_ribbon(l_front_embrace, side.s, 34,
          function(u)
            local dx_c = 4.8 + 24.4 * u
            local y_c  = 28.8 - 3.6 * (u ^ 1.30) + 0.60 * math.sin(2.0 * math.pi * (u * 1.25 - p_side))
            return dx_c, y_c
          end,
          function(u)
            local outer = smoothstep(0.38, 0.82, u)
            return 1.05 + 0.65 * outer, 0.72 + 0.44 * outer
          end,
          function(u)
            local wave = 0.68 + 0.32 * math.sin(2.0 * math.pi * (u * 1.40 - p_side))
            local env_in  = smoothstep(0.0, 0.16, u)
            local env_out = 1.0 - 0.78 * smoothstep(0.82, 1.0, u)
            local base_a = lerp(is_full and 142 or 118, is_full and 236 or 212, smoothstep(0.38, 0.74, u))
            return base_a * env_in * env_out * wave
          end,
          function(u)
            return m_snow, (u < 0.32) and m_pearl or m_cyan
          end,
          true
        )
      end
    end

    -- Tier 3: Enriched Ref2 D Lower Cradle Elliptical Cloud Ring + Pure-Vapor Outward Wisps (dx <= 29.0, zero clipping!)
    for ang_step = 0, 719 do
      local a = (ang_step / 720.0) * 2.0 * math.pi
      local ca, sa = math.cos(a), math.sin(a)
      local px = r_cx + 28.2 * ca
      local py = 32.8 + 4.3 * sa
      local adx = math.abs(px - r_cx)
      local out_dist = math.abs(ca)
      local flow_coord = (sa < 0) and (out_dist * 0.8) or (1.6 - out_dist * 0.8)
      local wave = 0.5 + 0.5 * math.sin(2.0 * math.pi * (flow_coord * 1.25 - phase))

      if sa < -0.04 then
        if adx >= 17.5 then
          local ix = math.floor(px + 0.5)
          local iy = math.floor(py + 0.5)
          local c_top = lerp_color(m_cyan, m_pearl, wave * 0.88)
          put_pixel(l_rear_embrace, ix, iy, with_alpha(c_top, 220))
          if adx >= 23.0 then
            put_pixel(l_rear_embrace, ix, iy + 1, with_alpha(m_sky, 145))
          end
        end
      else
        if adx >= 7.2 then
          local taper_in = smoothstep(7.2, 14.0, adx)
          local ix = math.floor(px + 0.5)
          local iy = math.floor(py + 0.5)
          local crest_c = lerp_color(m_pearl, m_snow, wave * (is_full and 0.96 or 0.84))
          local body_c  = lerp_color(m_sky, m_cyan, 0.48 + 0.52 * wave)
          local alpha_m = math.floor((is_full and 248 or 234) * taper_in)
          put_pixel(l_front_embrace, ix, iy, with_alpha(crest_c, alpha_m))
          if adx >= 11.0 then
            put_pixel(l_front_embrace, ix, iy + 1, with_alpha(body_c, math.floor(alpha_m * 0.72)))
          end
          if adx >= 23.5 then
            put_pixel(l_front_embrace, ix, iy - 1, with_alpha(m_snow, math.floor(alpha_m * 0.66)))
          end
        end
      end
    end

    -- Lower calyx pure-vapor tendrils (NO dark shadow pixels!)
    for dx = 7, 19 do
      local u = (dx - 7) / 12.0
      local wy = (dx <= 11) and 38 or ((dx <= 15) and 40 or 41)
      local pulse = 0.5 + 0.5 * math.sin(2.0 * math.pi * (dx / 12.0 - phase))
      local al = math.floor(204 * math.sin(u * math.pi) * (is_full and 1.10 or 0.95))
      local c_hi = lerp_color(m_cyan, m_snow, pulse)
      put_sym(l_front_embrace, LOTUS_CX - dx, wy,     with_alpha(c_hi, clamp(al, 0, 250)))
      put_sym(l_front_embrace, LOTUS_CX - dx, wy + 1, with_alpha(m_cyan, clamp(math.floor(al * 0.45), 0, 200)))
    end

    -- Outward-Drifting Slender Cloud-Mist Wisps across Lower, Mid, and Upper Flanks (PURE LUMINOUS VAPOR!)
    local function draw_outward_wisp(target_layer, y_base, d_start, d_end, len_px, cyc_t, strength)
      local env = math.sin(math.pi * cyc_t)
      if env <= 0.04 then return end
      local center_d = lerp(d_start, d_end, cyc_t)
      local half_l = len_px * (0.72 + 0.28 * env) * 0.5
      local d0 = math.floor(center_d - half_l + 0.5)
      local d1 = math.min(31, math.floor(center_d + half_l + 0.5))
      for d = d0, d1 do
        local u = (d - d0) / math.max(1, d1 - d0)
        local prof = math.sin(math.pi * (u ^ 0.85))
        local edge_fade = 1.0 - smoothstep(28.5, 31.5, d)
        local wave_y = (u > 0.72) and -1 or 0
        local a_main = clamp(math.floor(244 * env * prof * strength * edge_fade), 0, 248)
        if a_main > 12 then
          local c_top = lerp_color(m_pearl, m_snow, clamp(0.42 + 0.58 * prof * env, 0, 1))
          local lx = LOTUS_CX - d
          local rx = mir_x(lx)
          put_pixel(target_layer, lx, y_base + wave_y, with_alpha(c_top, a_main))
          put_pixel(target_layer, rx, y_base + wave_y, with_alpha(c_top, a_main))
          if prof > 0.48 then
            local a_sub = math.floor(a_main * 0.62)
            put_pixel(target_layer, lx, y_base + wave_y + 1, with_alpha(m_cyan, a_sub))
            put_pixel(target_layer, rx, y_base + wave_y + 1, with_alpha(m_cyan, a_sub))
          end
        end
      end
    end

    draw_outward_wisp(l_front_embrace, 35, 14.5, 30.2, 10.5, (phase + 0.00) % 1.0, 0.98 * str_mult)
    draw_outward_wisp(l_front_embrace, 36, 14.5, 30.2, 10.0, (phase + 0.50) % 1.0, 0.92 * str_mult)
    draw_outward_wisp(l_rear_embrace,  24, 17.5, 29.8, 9.0,  (phase + 0.25) % 1.0, 0.90 * str_mult)
    draw_outward_wisp(l_rear_embrace,  15, 16.0, 28.2, 8.5,  (phase + 0.75) % 1.0, 0.86 * str_mult)
    draw_outward_wisp(l_front_embrace, 39, 10.5, 26.5, 9.0,  (phase + 0.18) % 1.0, 0.86 * str_mult)
    draw_outward_wisp(l_front_embrace, 40, 10.5, 26.5, 8.5,  (phase + 0.68) % 1.0, 0.80 * str_mult)
  end

  -- Cushion wraps STRICTLY solid structural elements (mirror ring, beads, solid petals) — ZERO dark cushion on ANY mist!
  local comp_mask = new_image(MANA_ICON_W, MANA_ICON_H)
  blit(comp_mask, l_ring,        0, 0)
  blit(comp_mask, l_petals_back, 0, 0)
  blit(comp_mask, l_petals_front,0, 0)
  local cushion = build_soft_cushion(comp_mask, 10, 32, 74, 96, 38, 0, function(x, y)
    local dx, dy = x - r_cx, y - r_cy
    return (dx * dx + dy * dy) <= (r_out * r_out)
  end)
  local l_base = new_image(MANA_ICON_W, MANA_ICON_H)
  blit(l_base, cushion, 0, 0)
  blit(l_base, l_ring,  0, 0)

  local merged = new_image(MANA_ICON_W, MANA_ICON_H)
  blit(merged, l_base,           0, 0)
  blit(merged, l_rear_embrace,   0, 0)
  blit(merged, l_petals_back,    0, 0)
  blit(merged, l_inner_overflow, 0, 0)
  blit(merged, l_petals_front,   0, 0)
  blit(merged, l_front_embrace,  0, 0)

  return {
    base           = l_base,
    rear_embrace   = l_rear_embrace,
    petals_back    = l_petals_back,
    inner_overflow = l_inner_overflow,
    petals_front   = l_petals_front,
    front_embrace  = l_front_embrace,
    merged         = merged,
  }
end

-- Base S-stream geometry (Stream 1 without static barb at x=38..40 so Full mode can animate a traveling wave-crest barb!)
local STREAM1_BASE_COLS = {
  [2]={11,11}, [3]={10,11}, [4]={9,11}, [5]={8,10}, [6]={7,10}, [7]={7,9}, [8]={6,9},
  [9]={6,9}, [10]={5,9}, [11]={5,9}, [12]={5,9}, [13]={5,9}, [14]={5,10}, [15]={6,10},
  [16]={6,11}, [17]={6,11}, [18]={7,12}, [19]={7,12}, [20]={8,13}, [21]={8,13},
  [22]={9,14}, [23]={10,14}, [24]={10,15}, [25]={11,15}, [26]={11,16}, [27]={12,16},
  [28]={12,17}, [29]={13,17}, [30]={13,17}, [31]={13,17}, [32]={14,17}, [33]={14,17},
  [34]={14,17}, [35]={14,17}, [36]={14,17}, [37]={14,17},
  [38]={13,17}, [39]={13,17}, [40]={13,16}, [41]={13,16}, [42]={14,16}, [43]={14,15},
  [44]={14,15}, [45]={14,14},
}

local STREAM1_COLS = {
  [2]={11,11}, [3]={10,11}, [4]={9,11}, [5]={8,10}, [6]={7,10}, [7]={7,9}, [8]={6,9},
  [9]={6,9}, [10]={5,9}, [11]={5,9}, [12]={5,9}, [13]={5,9}, [14]={5,10}, [15]={6,10},
  [16]={6,11}, [17]={6,11}, [18]={7,12}, [19]={7,12}, [20]={8,13}, [21]={8,13},
  [22]={9,14}, [23]={10,14}, [24]={10,15}, [25]={11,15}, [26]={11,16}, [27]={12,16},
  [28]={12,17}, [29]={13,17}, [30]={13,17}, [31]={13,17}, [32]={14,17}, [33]={14,17},
  [34]={14,17}, [35]={14,17}, [36]={14,17}, [37]={13,17},
  [38]={11,17}, [39]={12,17}, [40]={13,16}, [41]={13,16}, [42]={14,16}, [43]={14,15},
  [44]={14,15}, [45]={14,14},
}

local STREAM2_COLS = {
  [2]={22,24}, [3]={21,23}, [4]={20,22}, [5]={19,22}, [6]={18,21}, [7]={18,21},
  [8]={17,21}, [9]={17,21}, [10]={15,21}, [11]={14,21}, [12]={14,21}, [13]={14,21},
  [14]={14,21}, [15]={14,21}, [16]={15,21}, [17]={15,22}, [18]={16,22}, [19]={17,22},
  [20]={17,23}, [21]={18,23}, [22]={18,24}, [23]={19,24}, [24]={19,25}, [25]={20,25},
  [26]={20,25}, [27]={21,26}, [28]={21,26}, [29]={22,26}, [30]={22,26}, [31]={23,26},
  [32]={23,26}, [33]={23,26}, [34]={23,26}, [35]={23,26}, [36]={23,26}, [37]={23,25},
  [38]={23,25}, [39]={23,25}, [40]={22,24}, [41]={22,24}, [42]={21,23}, [43]={21,22},
  [44]={20,20},
}

local STREAM3_COLS = {
  [4]={32,32}, [5]={31,32}, [6]={30,31}, [7]={29,31}, [8]={28,30}, [9]={28,30},
  [10]={27,30}, [11]={27,30}, [12]={26,30}, [13]={26,30}, [14]={26,30}, [15]={26,30},
  [16]={26,30}, [17]={26,30}, [18]={26,30}, [19]={27,30}, [20]={27,31}, [21]={27,31},
  [22]={27,31}, [23]={28,31}, [24]={28,31}, [25]={28,32}, [26]={29,32}, [27]={29,32},
  [28]={29,32}, [29]={29,32}, [30]={30,32}, [31]={30,32}, [32]={30,31}, [33]={30,31},
  [34]={29,31}, [35]={29,30}, [36]={29,29},
}

-- ============================================================================
-- V6 WATER #8 ICON:
--   mode == "idle"   : 0/100 STATIC deep-sea dark blue
--   mode == "static" : 1..99/100 PURE STATIC crisp Ref3 #8 S-streams & spray
--   mode == "full"   : 100/100 VISIBLY FLOWING 16-frame hydrodynamic S-waves,
--                      traveling dorsal wave-crest barbs, racing whitewater
--                      spines, and 8-11px downstream gliding spray droplets!
-- ============================================================================
local function make_water_icon_layers(mode, frame_idx)
  local f0 = (frame_idx or 1) - 1
  local is_idle = (mode == "idle")
  local is_full = (mode == "full")
  local phase   = is_full and (f0 / FRAMES) or 0.0
  local theta   = 2.0 * math.pi * phase

  local l_streams = new_image(WATER_ICON_W, WATER_ICON_H)
  local l_glints  = new_image(WATER_ICON_W, WATER_ICON_H)

  local c_navy   = is_idle and rgba(10, 34, 68, 255)  or rgba(9, 54, 128, 255)
  local c_cobalt = is_idle and rgba(14, 52, 98, 255)  or rgba(11, 78, 172, 255)
  local c_azure  = is_idle and rgba(20, 78, 132, 255) or (is_full and rgba(24, 152, 242, 255) or rgba(18, 132, 224, 255))
  local c_cyan   = is_idle and rgba(32, 108, 162, 255) or (is_full and rgba(42, 226, 255, 255) or rgba(26, 208, 255, 255))
  local c_ice    = is_idle and rgba(52, 136, 188, 255) or rgba(148, 246, 255, 255)
  local c_white  = is_idle and rgba(78, 158, 204, 255) or rgba(255, 255, 255, 255)

  -- Sub-pixel traveling wave displacement helper for `full` (100/100) mode
  local function stream_wave_dy(xn, phi_off, amp_max)
    if not is_full then return 0.0 end
    local env = 0.42 + (amp_max - 0.42) * smoothstep(0.04, 0.82, xn)
    return env * math.sin(2.0 * math.pi * (xn * 1.28 - phase) - phi_off)
  end

  -- Crisp pixel-art selective AA coverage: solid 1.0 inside core, clean 1-step rim AA, zero blurry fringe!
  local function pixel_cov(y, yt_f, yb_f)
    local lo = math.max(y - 0.5, yt_f - 0.5)
    local hi = math.min(y + 0.5, yb_f + 0.5)
    local raw = clamp(hi - lo, 0.0, 1.0)
    if not is_full then return (raw >= 0.5) and 1.0 or 0.0 end
    if raw >= 0.46 then return 1.0 end
    if raw >= 0.22 then return 0.68 end
    return 0.0
  end

  -- --------------------------------------------------------------------------
  -- STREAM 1 (Upper S-Blade: x = 2..45, razor-sharp katana tip + traveling S-wave!)
  -- --------------------------------------------------------------------------
  local s1_table = is_full and STREAM1_BASE_COLS or STREAM1_COLS
  -- Crisp traveling dorsal wave-crest barb when FULL (glides x = 22 -> 40 across 16 frames!)
  local barb1_cx  = math.floor(22.0 + 18.0 * ((phase + 0.15) % 1.0) + 0.5)
  local barb1_env = math.sin(math.pi * ((phase + 0.15) % 1.0))

  for x = 2, 45 do
    local span = s1_table[x]
    if span then
      local xn = (x - 2) / 43.0
      local dy = stream_wave_dy(xn, 0.0, 1.38)
      local barb_lift = 0.0
      if is_full and barb1_env > 0.25 then
        if x == barb1_cx then
          barb_lift = 2.0 * barb1_env
        elseif x == barb1_cx + 1 then
          barb_lift = 1.0 * barb1_env
        end
      end
      local yt_f = span[1] + dy - barb_lift
      local yb_f = span[2] + dy
      local surge_pkt = is_full and (0.5 + 0.5 * math.sin(2.0 * math.pi * (xn * 1.45 - phase * 2.0))) or 0.0

      local y_min = math.max(0, math.floor(yt_f - 1.0))
      local y_max = math.min(WATER_ICON_H - 1, math.ceil(yb_f + 1.0))
      for y = y_min, y_max do
        local cov = pixel_cov(y, yt_f, yb_f)
        if cov > 0.04 then
          local span_h = math.max(1.0, yb_f - yt_f)
          local yn = clamp((y - yt_f) / span_h, 0.0, 1.0)
          local flow_t = clamp(xn * 1.65 + (1.0 - yn) * 0.18 - 0.04 + (is_full and 0.12 * surge_pkt or 0.0), 0.0, 1.0)
          local c
          if flow_t < 0.42 then
            c = lerp_color(c_navy, c_azure, smoothstep(0.0, 0.42, flow_t))
          else
            c = lerp_color(c_azure, c_cyan, smoothstep(0.42, 0.92, flow_t))
          end
          if x >= 6 and x <= 18 and math.abs(y - (yt_f + 1.0)) < 0.65 then
            c = lerp_color(c_cobalt, c, smoothstep(11.0, 19.0, x))
          elseif x >= 9 and x <= 18 and math.abs(y - yt_f) < 0.65 then
            c = lerp_color(c, c_cyan, 0.52)
          end
          put_pixel(l_streams, x, y, with_alpha(c, math.floor(255 * cov + 0.5)))

          if not is_idle then
            -- Blazing white katana belly on right half + racing spine crest in FULL mode!
            local belly_start = is_full and 22 or 27
            if x >= belly_start then
              local bx = (x - belly_start) / (45.0 - belly_start)
              local dist_bot = yb_f - y
              local w_str = smoothstep(0.0, 0.32, bx) * (is_full and (0.78 + 0.22 * surge_pkt) or 0.90)
              if dist_bot <= 0.85 and x <= 45 then
                put_pixel(l_glints, x, y, with_alpha(c_white, math.floor(255 * w_str * cov)))
              elseif dist_bot <= 1.85 and x >= (is_full and 26 or 29) and x <= 44 then
                put_pixel(l_glints, x, y, with_alpha(is_full and c_white or c_ice, math.floor(235 * w_str * cov)))
              elseif x >= 44 then
                put_pixel(l_glints, x, y, with_alpha(c_ice, math.floor(240 * cov)))
              end
            end
            -- Racing upstream -> downstream whitewater surge crest in FULL mode
            if is_full and xn >= 0.14 and xn <= 0.82 and surge_pkt > 0.58 then
              local crest_k = ((surge_pkt - 0.58) / 0.42) * (1.0 - math.abs(yn - 0.48) * 1.45)
              if crest_k > 0.05 then
                local gc = (crest_k > 0.55) and c_white or c_ice
                put_pixel(l_glints, x, y, with_alpha(gc, math.floor(245 * clamp(crest_k, 0, 1) * cov)))
              end
            end
            -- Brighten traveling dorsal wave-crest barb tip
            if is_full and barb_lift > 0.65 and (y - yt_f) <= 1.1 then
              put_pixel(l_glints, x, y, with_alpha(c_ice, math.floor(220 * cov)))
            end
          end
        end
      end
    end
  end

  -- --------------------------------------------------------------------------
  -- STREAM 2 (Middle S-Blade: x = 2..45 in full, 2..44 in static/idle)
  -- --------------------------------------------------------------------------
  for x = 2, 44 do
    local span = STREAM2_COLS[x]
    if span then
      local xn = (x - 2) / 42.0
      local dy = stream_wave_dy(xn, 2.05, 1.38)
      local yt_f = span[1] + dy
      local yb_f = span[2] + dy
      local surge_pkt = is_full and (0.5 + 0.5 * math.sin(2.0 * math.pi * (xn * 1.40 - phase * 2.0 - 0.28))) or 0.0

      local y_min = math.max(0, math.floor(yt_f - 1.0))
      local y_max = math.min(WATER_ICON_H - 1, math.ceil(yb_f + 1.0))
      for y = y_min, y_max do
        local cov = pixel_cov(y, yt_f, yb_f)
        if cov > 0.04 then
          local span_h = math.max(1.0, yb_f - yt_f)
          local yn = clamp((y - yt_f) / span_h, 0.0, 1.0)
          local flow_t = clamp(xn * 1.60 + (1.0 - yn) * 0.15 - 0.04 + (is_full and 0.12 * surge_pkt or 0.0), 0.0, 1.0)
          local c
          if flow_t < 0.44 then
            c = lerp_color(c_cobalt, c_azure, smoothstep(0.0, 0.44, flow_t))
          else
            c = lerp_color(c_azure, c_cyan, smoothstep(0.44, 0.90, flow_t))
          end
          if x >= 10 and x <= 18 and (y - yt_f) <= 1.2 then
            c = lerp_color(c, c_cyan, 0.78)
          elseif x >= 9 and x <= 17 and math.abs(y - (yt_f + 2.0)) < 0.65 then
            c = lerp_color(c_navy, c, smoothstep(13.0, 18.0, x))
          end
          put_pixel(l_streams, x, y, with_alpha(c, math.floor(255 * cov + 0.5)))

          if not is_idle then
            local spine_start = is_full and 13 or 19
            if x >= spine_start and x <= 43 then
              local base_spine_y = (x < 19)
                and lerp(16.2, 18.2, (x - 13) / 6.0)
                or ((x <= 33) and lerp(18.2, 24.6, (x - 19) / 14.0) or lerp(24.6, 21.3, (x - 33) / 10.0))
              local spine_y = base_spine_y + dy
              local dy_s = math.abs(y - spine_y)
              local boost = is_full and (0.28 * surge_pkt) or 0.0
              local upstream_fade = (x < 19) and (smoothstep(13.0, 19.0, x) * surge_pkt) or 1.0
              if upstream_fade > 0.08 then
                if dy_s <= (0.68 + boost) then
                  put_pixel(l_glints, x, y, with_alpha(c_white, math.floor(255 * upstream_fade * cov)))
                elseif dy_s <= (1.38 + boost) then
                  put_pixel(l_glints, x, y, with_alpha(c_ice, math.floor((is_full and 230 or 190) * upstream_fade * cov)))
                end
              end
            end
          end
        end
      end
    end
  end

  -- --------------------------------------------------------------------------
  -- STREAM 3 (Lower Slender S-Blade: x = 4..36)
  -- --------------------------------------------------------------------------
  for x = 4, 36 do
    local span = STREAM3_COLS[x]
    if span then
      local xn = (x - 4) / 32.0
      local dy = stream_wave_dy(xn, 4.10, 1.25)
      local yt_f = span[1] + dy
      local yb_f = span[2] + dy
      local surge_pkt = is_full and (0.5 + 0.5 * math.sin(2.0 * math.pi * (xn * 1.35 - phase * 2.0 - 0.55))) or 0.0

      local y_min = math.max(0, math.floor(yt_f - 1.0))
      local y_max = math.min(WATER_ICON_H - 1, math.ceil(yb_f + 1.0))
      for y = y_min, y_max do
        local cov = pixel_cov(y, yt_f, yb_f)
        if cov > 0.04 then
          local flow_t = clamp(xn * 1.75 + (is_full and 0.14 * surge_pkt or 0.0), 0.0, 1.0)
          local c = lerp_color(c_cobalt, c_cyan, smoothstep(0.0, 0.72, flow_t))
          if x >= 11 and x <= 19 and (y - yt_f) <= 1.2 then
            c = is_idle and c_cyan or lerp_color(c, c_ice, 0.48 * (1.0 - math.abs(x - 15) / 5.0))
          elseif x >= 10 and x <= 18 and math.abs(y - (yt_f + 2.0)) < 0.65 then
            c = lerp_color(c_azure, c, smoothstep(14.0, 19.0, x))
          end
          put_pixel(l_streams, x, y, with_alpha(c, math.floor(255 * cov + 0.5)))

          if not is_idle then
            if is_full then
              -- Racing whitewater highlight sweeping left->right along Stream 3 dorsal ridge
              if (y - yt_f) <= 1.35 and xn >= 0.18 then
                local g_str = clamp(0.45 + 0.55 * surge_pkt, 0, 1) * ((y - yt_f <= 0.65) and 1.0 or 0.62)
                put_pixel(l_glints, x, y, with_alpha((surge_pkt > 0.52) and c_white or c_ice, math.floor(245 * g_str * cov)))
              end
            else
              -- Canonical Ref3 #8 static glint placement
              if x >= 20 and x <= 26 and y <= span[1] + 1 then
                local g_str = (x >= 21 and x <= 24 and y == span[1]) and 1.0 or 0.65
                put_pixel(l_glints, x, y, with_alpha((g_str > 0.8) and c_white or c_ice, math.floor(235 * g_str)))
              elseif x >= 27 and x <= 35 and y == span[1] then
                put_pixel(l_glints, x, y, with_alpha(c_ice, 185))
              end
            end
          end
        end
      end
    end
  end

  -- --------------------------------------------------------------------------
  -- SPRAY STREAKS & DROPLETS (Static canonical in `static`; 8-10px gliding in `full`!)
  -- --------------------------------------------------------------------------
  if not is_idle then
    if not is_full then
      -- 1..99/100: Pure frozen canonical Ref3 #8 spray streaks & droplets
      put_pixel(l_glints, 23, 4, c_azure)
      put_pixel(l_glints, 24, 4, c_cyan)
      put_pixel(l_glints, 25, 4, c_ice)

      put_pixel(l_glints, 28, 5, c_cyan)
      put_pixel(l_glints, 29, 5, c_cyan)
      put_pixel(l_glints, 29, 6, c_ice)
      put_pixel(l_glints, 30, 6, c_ice)
      put_pixel(l_glints, 31, 6, c_cyan)
      put_pixel(l_glints, 30, 7, c_cyan)
      put_pixel(l_glints, 31, 7, c_ice)
      put_pixel(l_glints, 32, 7, c_cyan)
      put_pixel(l_glints, 32, 8, c_azure)

      put_pixel(l_glints, 35, 10, c_cyan)
      put_pixel(l_glints, 36, 10, c_ice)
      put_pixel(l_glints, 35, 11, c_azure)

      put_pixel(l_glints, 7, 14, c_cyan)
      put_pixel(l_glints, 8, 14, c_ice)
      put_pixel(l_glints, 6, 15, c_cyan)
      put_pixel(l_glints, 7, 15, c_azure)

      put_pixel(l_glints, 7, 26, c_cyan)

      put_pixel(l_glints, 11, 34, c_cyan)
      put_pixel(l_glints, 12, 34, c_ice)
      put_pixel(l_glints, 10, 35, c_cyan)
      put_pixel(l_glints, 11, 35, c_azure)
      put_pixel(l_glints, 14, 34, c_cyan)
    else
      -- 100/100 FULL: Crisp integer-snapped pixel-art spray streaks & droplets gliding 9px downstream!
      local function glide_spray(t_off, x0, y0, x1, y1, tail_dx, tail_dy)
        local t = (phase + t_off) % 1.0
        local env = math.sin(math.pi * t)
        if env <= 0.08 then return end
        local ix = math.floor(lerp(x0, x1, t) + 0.5)
        local iy = math.floor(lerp(y0, y1, t) + 0.5)
        local a_hi = clamp(math.floor(255 * (env ^ 0.72)), 0, 255)
        put_pixel(l_glints, ix, iy, with_alpha(c_white, a_hi))
        put_pixel(l_glints, ix - tail_dx, iy - tail_dy, with_alpha(c_cyan, math.floor(a_hi * 0.92)))
        if env > 0.35 then
          put_pixel(l_glints, ix - tail_dx * 2, iy - tail_dy, with_alpha(c_azure, math.floor(a_hi * 0.75)))
        end
      end

      glide_spray(0.00, 20.0, 4.0, 29.0,  5.0, 1, 0)
      glide_spray(0.50, 26.0, 5.0, 36.0,  9.0, 1, 0)
      glide_spray(0.25, 31.0, 7.0, 41.0, 11.0, 1, 0)
      glide_spray(0.75, 23.0, 4.0, 33.0,  7.0, 1, 0)

      glide_spray(0.15,  5.0, 15.0, 12.0, 13.0, 1, 0)
      glide_spray(0.65,  6.0, 27.0, 12.0, 25.0, 1, 0)
      glide_spray(0.10,  9.0, 35.0, 18.0, 34.0, 1, 0)
      glide_spray(0.60, 12.0, 35.0, 21.0, 33.0, 1, 0)
    end
  end

  -- Clean saturated sapphire-cobalt translucent water aura (ZERO grey/black daytime halo!)
  local union_mask = new_image(WATER_ICON_W, WATER_ICON_H)
  blit(union_mask, l_streams, 0, 0)
  blit(union_mask, l_glints,  0, 0)
  local l_cushion = build_soft_cushion(union_mask, 10, 62, 148, 86, 32, 0, nil)

  local merged = new_image(WATER_ICON_W, WATER_ICON_H)
  blit(merged, l_cushion, 0, 0)
  blit(merged, l_streams, 0, 0)
  blit(merged, l_glints,  0, 0)

  return {
    cushion = l_cushion,
    streams = l_streams,
    glints  = l_glints,
    merged  = merged,
  }
end

-- ============================================================================
-- DOUBLE-STEPPED CHINESE FRETWORK (MEANDER) TRACKS (204x20, INNER 180x10)
--   tx = 0..17  : Authentic Ref2 D 6px exposed bridge tongue + double-stepped '回' meander
--   tx = 18..197: 180x10 inner recessed chamber (ty = 5..14)
--   tx = 198..203: Symmetric Chinese meander end-cap
-- ============================================================================

local LEFT_BRIDGE_20 = {
  ". . . . . . . . . . o o o o o o o o",
  ". . . . . . . . . . o p h h o h p o",
  ". . . . . . . . . . o h g g o l m o",
  ". . . . . . o o o o o h g h h l g g",
  ". . . . . . o p h h l l g o o o o d",
  ". . . . . . o h g g g g g o s s s s",
  ". . . . . . o m g h h l o o c c c c",
  ". . . . . . o d g o o o o s b b b b",
  "o o o o o o o d g o l h m s b b b b",
  "o h h l l m m m g l h m d s b b b b",
  "o d d m m d d d g m d d d s b b b b",
  "o o o o o o o d g o d d d s b b b b",
  ". . . . . . o d g o o o o s b b b b",
  ". . . . . . o m g m l m o o c c c c",
  ". . . . . . o d g g g g g o s s s s",
  ". . . . . . o d m m m d g o o o o d",
  ". . . . . . o o o o o d g m m d g g",
  ". . . . . . . . . . o d g g o m d o",
  ". . . . . . . . . . o d d m o d d o",
  ". . . . . . . . . . o o o o o o o o",
}

local RIGHT_CAP_20 = {
  "o o o o o o",
  "o l h h p o",
  "g m g g h o",
  "g l h g h o",
  "d o o g l o",
  "s s o g h o",
  "c c o m m o",
  "b b o g d o",
  "b b s h l o",
  "b b s m h o",
  "b b s d m o",
  "b b s d d o",
  "b b o g d o",
  "c c o m d o",
  "s s o g d o",
  "d o o g d o",
  "g d m g d o",
  "g d g g d o",
  "o d d m d o",
  "o o o o o o",
}

local function draw_fretwork_track_base(img, is_water)
  local cav_sh = is_water and PAL_WATER.cav_shadow or PAL_MANA.cav_shadow
  local cav_b1 = is_water and PAL_WATER.cav_bed_1  or PAL_MANA.cav_bed_1
  local cav_b2 = is_water and PAL_WATER.cav_bed_2  or PAL_MANA.cav_bed_2
  local cav_mk = is_water and PAL_WATER.cav_notch  or PAL_MANA.cav_mark

  local fret_pal = {
    o = PAL_FRAME.ink_outer,
    g = PAL_FRAME.groove_dark,
    d = PAL_FRAME.slate_dark,
    m = PAL_FRAME.stone_mid,
    l = PAL_FRAME.stone_lit,
    h = PAL_FRAME.fret_hi,
    p = PAL_FRAME.fret_peak,
    s = cav_sh,
    c = cav_b1,
    b = cav_b2,
  }

  stamp(img, 0, 0, LEFT_BRIDGE_20, fret_pal)
  stamp(img, TRACK_W - 6, 0, RIGHT_CAP_20, fret_pal)

  for tx = FILL_X_OFFSET, FILL_X_OFFSET + FILL_W - 1 do
    local dist_end = math.min(tx - FILL_X_OFFSET, (FILL_X_OFFSET + FILL_W - 1) - tx)
    local top_hi = (dist_end <= 2) and PAL_FRAME.stone_lit or PAL_FRAME.fret_hi
    local top_md = (dist_end <= 1) and PAL_FRAME.slate_dark or PAL_FRAME.stone_mid

    -- Top double-molded stone rail (ty = 0..4)
    img:drawPixel(tx, 0, PAL_FRAME.ink_outer)
    img:drawPixel(tx, 1, top_hi)
    img:drawPixel(tx, 2, top_md)
    img:drawPixel(tx, 3, PAL_FRAME.groove_dark)
    img:drawPixel(tx, 4, PAL_FRAME.ink_outer)

    -- 10px recessed inner chamber (ty = 5..14)
    for fy = 0, FILL_H - 1 do
      local ty = FILL_Y_OFFSET + fy
      local c
      if fy == 0 or fy == FILL_H - 1 then
        c = cav_sh
      elseif fy <= 2 then
        c = cav_b1
      elseif fy == 4 or fy == 5 then
        c = ((tx - FILL_X_OFFSET) % 30 == 14) and cav_mk or cav_b1
      else
        c = cav_b2
      end
      img:drawPixel(tx, ty, c)
    end

    -- Bottom double-molded stone rail (ty = 15..19)
    img:drawPixel(tx, 15, PAL_FRAME.ink_outer)
    img:drawPixel(tx, 16, PAL_FRAME.groove_dark)
    img:drawPixel(tx, 17, PAL_FRAME.stone_mid)
    img:drawPixel(tx, 18, PAL_FRAME.slate_dark)
    img:drawPixel(tx, 19, PAL_FRAME.ink_outer)
  end

  -- Subtle vertical shadow bevels on extreme left/right chamber edges
  for fy = 0, FILL_H - 1 do
    img:drawPixel(FILL_X_OFFSET,              FILL_Y_OFFSET + fy, cav_sh)
    img:drawPixel(FILL_X_OFFSET + FILL_W - 1, FILL_Y_OFFSET + fy, cav_sh)
  end
end

local function make_mana_track()
  local img = new_image(TRACK_W, TRACK_H)
  draw_fretwork_track_base(img, false)
  return img
end

-- Water 20 (tx = 16 + 36 - 1 = 51) & 50 (tx = 16 + 90 - 1 = 105) Embedded Fretwork Clasps
local function make_water_track(clasp_state)
  local img = new_image(TRACK_W, TRACK_H)
  draw_fretwork_track_base(img, true)

  local lit20 = (clasp_state or 0) >= 1
  local lit50 = (clasp_state or 0) >= 2
  local x20 = FILL_X_OFFSET + 36 - 1 -- tx = 51 (20% of 180px)
  local x50 = FILL_X_OFFSET + 90 - 1 -- tx = 105 (50% of 180px)

  -- 20/100 Top Embedded Fretwork Clasp (ty = 0..4)
  do
    local e_col = lit20 and PAL_WATER.clasp_lit_edge or PAL_WATER.clasp_unlit_edge
    local c_col = lit20 and PAL_WATER.clasp_lit_20   or PAL_WATER.clasp_unlit_core
    local m_col = lit20 and lerp_color(e_col, c_col, 0.55) or PAL_WATER.clasp_unlit_edge
    for dx = -3, 3 do
      put_pixel(img, x20 + dx, 0, PAL_WATER.clasp_ink)
    end
    if lit20 then
      put_pixel(img, x20 - 1, 0, e_col)
      put_pixel(img, x20,     0, m_col)
      put_pixel(img, x20 + 1, 0, e_col)
    end
    put_pixel(img, x20 - 3, 1, PAL_WATER.clasp_ink)
    put_pixel(img, x20 - 2, 1, e_col)
    put_pixel(img, x20 - 1, 1, m_col)
    put_pixel(img, x20,     1, c_col)
    put_pixel(img, x20 + 1, 1, m_col)
    put_pixel(img, x20 + 2, 1, e_col)
    put_pixel(img, x20 + 3, 1, PAL_WATER.clasp_ink)

    put_pixel(img, x20 - 2, 2, PAL_WATER.clasp_ink)
    put_pixel(img, x20 - 1, 2, e_col)
    put_pixel(img, x20,     2, c_col)
    put_pixel(img, x20 + 1, 2, e_col)
    put_pixel(img, x20 + 2, 2, PAL_WATER.clasp_ink)

    put_pixel(img, x20 - 1, 3, PAL_WATER.clasp_ink)
    put_pixel(img, x20,     3, m_col)
    put_pixel(img, x20 + 1, 3, PAL_WATER.clasp_ink)
    put_pixel(img, x20,     4, lit20 and e_col or PAL_WATER.clasp_ink)
    put_pixel(img, x20,     5, PAL_WATER.cav_notch)
  end

  -- 50/100 Symmetric Double-Biting Fretwork Clasp (ty = 0..4 and ty = 15..19)
  do
    local e_col = lit50 and PAL_WATER.clasp_lit_edge or PAL_WATER.clasp_unlit_edge
    local c_col = lit50 and PAL_WATER.clasp_lit_50   or PAL_WATER.clasp_unlit_core
    local m_col = lit50 and PAL_WATER.clasp_lit_20   or PAL_WATER.clasp_unlit_edge
    for dx = -4, 4 do
      put_pixel(img, x50 + dx, 0,  PAL_WATER.clasp_ink)
      put_pixel(img, x50 + dx, 19, PAL_WATER.clasp_ink)
    end
    if lit50 then
      for dx = -1, 1 do
        put_pixel(img, x50 + dx, 0,  (dx == 0) and m_col or e_col)
        put_pixel(img, x50 + dx, 19, (dx == 0) and m_col or e_col)
      end
    end
    local function stamp_jaw(y1, y2, y3, y4, yn)
      put_pixel(img, x50 - 4, y1, PAL_WATER.clasp_ink)
      put_pixel(img, x50 - 3, y1, e_col)
      put_pixel(img, x50 - 2, y1, m_col)
      put_pixel(img, x50 - 1, y1, c_col)
      put_pixel(img, x50,     y1, c_col)
      put_pixel(img, x50 + 1, y1, c_col)
      put_pixel(img, x50 + 2, y1, m_col)
      put_pixel(img, x50 + 3, y1, e_col)
      put_pixel(img, x50 + 4, y1, PAL_WATER.clasp_ink)

      put_pixel(img, x50 - 3, y2, PAL_WATER.clasp_ink)
      put_pixel(img, x50 - 2, y2, e_col)
      put_pixel(img, x50 - 1, y2, m_col)
      put_pixel(img, x50,     y2, c_col)
      put_pixel(img, x50 + 1, y2, m_col)
      put_pixel(img, x50 + 2, y2, e_col)
      put_pixel(img, x50 + 3, y2, PAL_WATER.clasp_ink)

      put_pixel(img, x50 - 2, y3, PAL_WATER.clasp_ink)
      put_pixel(img, x50 - 1, y3, e_col)
      put_pixel(img, x50,     y3, m_col)
      put_pixel(img, x50 + 1, y3, e_col)
      put_pixel(img, x50 + 2, y3, PAL_WATER.clasp_ink)

      put_pixel(img, x50 - 1, y4, PAL_WATER.clasp_ink)
      put_pixel(img, x50,     y4, lit50 and m_col or e_col)
      put_pixel(img, x50 + 1, y4, PAL_WATER.clasp_ink)
      put_pixel(img, x50,     yn, PAL_WATER.cav_notch)
    end
    stamp_jaw(1, 2, 3, 4, 5)
    stamp_jaw(18, 17, 16, 15, 14)
  end

  return img
end

-- ============================================================================
-- 180x10 ANIMATED FILLS (PRESERVING V4.1 ACCLAIMED DYNAMICS AT 2x PRECISION)
-- ============================================================================

local function calc_pixels(val, max_val)
  if val <= 0 or max_val <= 0 then return 0 end
  if val >= max_val then return FILL_W end
  return math.max(1, math.min(FILL_W, math.floor(val / max_val * FILL_W + 0.5)))
end

local function sample_ramp(stops, v)
  local cv = clamp(v, stops[1][1], stops[#stops][1])
  for i = 1, #stops - 1 do
    local v0, c0 = stops[i][1], stops[i][2]
    local v1, c1 = stops[i + 1][1], stops[i + 1][2]
    if cv <= v1 then
      local t = smoothstep(v0, v1, cv)
      return lerp_color(c0, c1, t)
    end
  end
  return stops[#stops][2]
end

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
  else
    c_deep  = PAL_MANA.deep
    c_mid   = PAL_MANA.mid_dark
    c_body  = PAL_MANA.body
    c_silk  = PAL_MANA.silk
    c_lit   = PAL_MANA.cloud_lit
    c_crest = PAL_MANA.volute
  end

  local ramp = {
    {0.20, c_deep},
    {0.34, c_mid},
    {0.49, c_body},
    {0.66, c_silk},
    {0.83, c_lit},
    {0.98, c_crest},
  }

  local base_profile = {0.34, 0.42, 0.48, 0.49, 0.46, 0.43, 0.39, 0.34, 0.28, 0.22}

  for x = 0, FILL_W - 1 do
    local x_norm = x / (FILL_W - 1)
    local swell = (state == "full") and (0.13 * x_norm) or (0.18 * x_norm)

    -- Upsampled 2x from v4.1's 38px & 53px wavelengths -> 76px & 106px
    local u1 = 2.0 * math.pi * ((x + 10.0) / 76.0) - phase
    local skew1 = math.sin(u1 + 0.52 * math.sin(u1))
    local y_cloud = 3.90 - 2.75 * skew1
    local billow_head = math.max(0.0, math.sin(u1 - 0.45))

    local u2 = 2.0 * math.pi * ((x + 46.0) / 106.0) - phase
    local skew2 = math.sin(u2 - 0.36 * math.sin(u2))
    local y_silk = 5.65 + 2.35 * skew2

    -- Fine Ref2 D horizontal gossamer cloud stratum near lower half
    local u3 = 2.0 * math.pi * ((x + 22.0) / 60.0) - phase
    local y_mist = 6.80 + 1.15 * math.sin(u3)

    for y = 0, FILL_H - 1 do
      local base_v = base_profile[y + 1]

      local d1 = math.abs(y - y_cloud)
      local w1 = 1.85 + 0.96 * (billow_head ^ 2)
      local r1 = math.max(0.0, 1.0 - (d1 / w1) ^ 1.70) * (0.48 + 0.20 * billow_head)

      local under_d = y - (y_cloud + 2.50)
      local curl_tuck = 0.0
      if under_d > -1.10 and under_d < 1.50 and billow_head > 0.15 then
        curl_tuck = 0.20 * billow_head * (1.0 - math.abs(under_d) / 1.50)
      end

      local d2 = math.abs(y - y_silk)
      local r2 = math.max(0.0, 1.0 - (d2 / 1.75) ^ 1.80) * 0.38

      local d3 = math.abs(y - y_mist)
      local r3 = math.max(0.0, 1.0 - (d3 / 1.30) ^ 1.90) * 0.22

      local v = base_v + swell + math.max(r1, math.max(r2 * 0.92, r3)) + 0.14 * math.min(r1, r2) - curl_tuck
      local c = sample_ramp(ramp, v)

      if state == "full" and (y == 2 or y == 3) then
        local glint_pos = (x - math.floor(f0 * FILL_W / FRAMES + 0.5)) % 90
        if (glint_pos == 28 or glint_pos == 29) and v >= 0.72 then
          c = PAL_MANA.crown
        end
      end
      if x <= 1 then
        c = lerp_color(c, c_deep, (2 - x) * 0.16)
      end
      if y == 0 or y == FILL_H - 1 then
        c = lerp_color(c, c_deep, 0.22)
      end
      img:drawPixel(x, y, c)
    end
  end
  return img
end

local function make_water_fill_frame(tier, frame_idx)
  local img = new_image(FILL_W, FILL_H)
  local f0 = frame_idx - 1
  local phase1 = 2.0 * math.pi * (f0 / FRAMES)
  local phase2 = 4.0 * math.pi * (f0 / FRAMES)

  if tier == 1 then
    -- 1..19 Trickle: calm shallow bottom water thread (y = 5..9), top half stays dark chamber air
    for x = 0, FILL_W - 1 do
      local s = math.sin(2.0 * math.pi * (x / 64.0) - phase1)
      local s2 = math.sin(2.0 * math.pi * ((x + 19.0) / 44.0) - phase1)
      local surf_y = 5.45 - 0.85 * s - 0.35 * s2
      for y = 0, FILL_H - 1 do
        local c
        if y == 0 or y == FILL_H - 1 then
          c = (y == 0) and PAL_WATER.cav_shadow or PAL_WATER.t1_deep
        elseif y < surf_y - 0.45 then
          c = (y <= 2) and PAL_WATER.cav_bed_1 or PAL_WATER.cav_bed_2
        elseif y < surf_y + 0.55 then
          c = (s > 0.58) and PAL_WATER.t1_glint or PAL_WATER.t1_crest
        elseif y <= 7 then
          c = lerp_color(PAL_WATER.t1_crest, PAL_WATER.t1_body, (y - surf_y) / 3.2)
        else
          c = PAL_WATER.t1_deep
        end
        img:drawPixel(x, y, c)
      end
    end
  elseif tier == 2 then
    -- 20..49 Stream: full 10px height, 1x speed smooth Ref3 #8 S-ribbons
    local ramp2 = {
      {0.18, PAL_WATER.t2_deep},
      {0.30, PAL_WATER.t2_mid},
      {0.48, PAL_WATER.t2_body},
      {0.68, PAL_WATER.t2_vein},
      {0.84, PAL_WATER.t2_blade},
      {0.96, PAL_WATER.t2_tip},
    }
    for x = 0, FILL_W - 1 do
      local x_norm = x / (FILL_W - 1)
      local u1 = 2.0 * math.pi * (x / 72.0) - phase1
      local y_r1 = 2.85 - 1.65 * math.sin(u1 + 0.35 * math.sin(u1))
      local u2 = 2.0 * math.pi * ((x + 38.0) / 96.0) - phase1
      local y_r2 = 6.35 + 1.55 * math.sin(u2 - 0.30 * math.sin(u2))

      for y = 0, FILL_H - 1 do
        local base = (y == 9) and 0.16 or ((y == 0) and 0.28 or 0.38)
        local s1 = math.max(0.0, 1.0 - (math.abs(y - y_r1) / 1.80) ^ 1.80) * 0.52
        local s2 = math.max(0.0, 1.0 - (math.abs(y - y_r2) / 1.70) ^ 1.80) * 0.44
        local v = base + 0.12 * x_norm + math.max(s1, s2)
        local c = sample_ramp(ramp2, v)
        if x <= 1 then c = lerp_color(c, PAL_WATER.t2_deep, (2 - x) * 0.18) end
        img:drawPixel(x, y, c)
      end
    end
  else
    -- 50..100 Torrent: full 10px height, 2x rapid speed (2 cycles / 16 frames) sinuous blades & seafoam
    local ramp3 = {
      {0.18, PAL_WATER.t3_deep},
      {0.28, PAL_WATER.t3_mid},
      {0.46, PAL_WATER.t3_body},
      {0.65, PAL_WATER.t3_surge},
      {0.82, PAL_WATER.t3_blade},
      {0.96, PAL_WATER.t3_foam},
    }
    for x = 0, FILL_W - 1 do
      local x_norm = x / (FILL_W - 1)
      local u1 = 2.0 * math.pi * (x / 68.0) - phase2
      local wave1 = math.sin(u1 + 0.42 * math.sin(u1))
      local y_b1 = 2.75 - 1.75 * wave1

      local u2 = 2.0 * math.pi * ((x + 34.0) / 92.0) - phase2
      local wave2 = math.sin(u2 - 0.38 * math.sin(u2))
      local y_b2 = 6.15 + 1.75 * wave2

      for y = 0, FILL_H - 1 do
        local base = (y == 9) and 0.20 or ((y == 0) and 0.32 or 0.42)
        local s1 = math.max(0.0, 1.0 - (math.abs(y - y_b1) / 1.85) ^ 1.70) * 0.56
        local s2 = math.max(0.0, 1.0 - (math.abs(y - y_b2) / 1.75) ^ 1.70) * 0.48
        local v = base + 0.16 * x_norm + math.max(s1, s2) + 0.10 * math.min(s1, s2)
        if x < 36 and v > 0.90 then v = 0.90 end
        local c = sample_ramp(ramp3, v)
        if x <= 1 then c = lerp_color(c, PAL_WATER.t3_deep, (2 - x) * 0.16) end
        img:drawPixel(x, y, c)
      end
    end
  end
  return img
end

-- ============================================================================
-- PRE-COMPUTED 16-FRAME CACHES + COMPOSITE BAR RENDERERS
-- ============================================================================

local CACHE_MANA_TRACK = make_mana_track()
local CACHE_WATER_TRACK = {
  [0] = make_water_track(0),
  [1] = make_water_track(1),
  [2] = make_water_track(2),
}

local CACHE_MANA_ICON_LAYERS = { empty = {}, normal = {}, full = {} }
for f = 1, FRAMES do
  CACHE_MANA_ICON_LAYERS.empty[f]  = (f == 1) and make_mana_icon_layers("empty", 1) or CACHE_MANA_ICON_LAYERS.empty[1]
  CACHE_MANA_ICON_LAYERS.normal[f] = make_mana_icon_layers("normal", f)
  CACHE_MANA_ICON_LAYERS.full[f]   = make_mana_icon_layers("full", f)
end

local CACHE_WATER_ICON_LAYERS = { idle = {}, static = {}, full = {} }
CACHE_WATER_ICON_LAYERS.idle[1]   = make_water_icon_layers("idle", 1)
CACHE_WATER_ICON_LAYERS.static[1] = make_water_icon_layers("static", 1)
for f = 1, FRAMES do
  CACHE_WATER_ICON_LAYERS.idle[f]   = CACHE_WATER_ICON_LAYERS.idle[1]
  CACHE_WATER_ICON_LAYERS.static[f] = CACHE_WATER_ICON_LAYERS.static[1]
  CACHE_WATER_ICON_LAYERS.full[f]   = make_water_icon_layers("full", f)
end

local CACHE_MANA_FILL = { low = {}, normal = {}, full = {} }
for f = 1, FRAMES do
  CACHE_MANA_FILL.low[f]    = (f == 1) and make_mana_fill_frame("low", 1) or CACHE_MANA_FILL.low[1]
  CACHE_MANA_FILL.normal[f] = make_mana_fill_frame("normal", f)
  CACHE_MANA_FILL.full[f]   = make_mana_fill_frame("full", f)
end

local CACHE_WATER_FILL = { [1] = {}, [2] = {}, [3] = {} }
for f = 1, FRAMES do
  CACHE_WATER_FILL[1][f] = make_water_fill_frame(1, f)
  CACHE_WATER_FILL[2][f] = make_water_fill_frame(2, f)
  CACHE_WATER_FILL[3][f] = make_water_fill_frame(3, f)
end

local function render_mana_bar_2x(dst, x, y, val, max_val, frame_idx, afterglow_val)
  local px = calc_pixels(val, max_val)
  local is_full = (px >= FILL_W and val >= max_val)
  local is_low  = (px > 0 and (val / math.max(1, max_val)) <= 0.22)
  local icon_mode = (px == 0) and "empty" or (is_full and "full" or "normal")
  local fill_mode = is_full and "full" or (is_low and "low" or "normal")

  local track_x = x + MANA_TRACK_DX
  local track_y = y + MANA_TRACK_DY
  local fill_x  = track_x + FILL_X_OFFSET
  local fill_y  = track_y + FILL_Y_OFFSET

  blit(dst, CACHE_MANA_TRACK, track_x, track_y)

  -- Cast-spend dispersing cloud afterglow
  if afterglow_val and afterglow_val > val then
    local g_px = calc_pixels(afterglow_val, max_val)
    for gx = px, g_px - 1 do
      local t = (gx - px) / math.max(1, g_px - px)
      for fy = 1, FILL_H - 2 do
        local prof = math.sin(math.pi * (fy / (FILL_H - 1)))
        local c_src = lerp_color(PAL_MANA.ghost_low, PAL_MANA.ghost_hi, prof)
        local c_bg  = (fy <= 4) and PAL_MANA.cav_bed_1 or PAL_MANA.cav_bed_2
        put_pixel(dst, fill_x + gx, fill_y + fy, lerp_color(c_src, c_bg, clamp(t * 0.80 + (1.0 - prof) * 0.25, 0, 1)))
      end
    end
  end

  if px > 0 then
    local fill = CACHE_MANA_FILL[fill_mode][frame_idx]
    blit(dst, fill, fill_x, fill_y, px, FILL_H)

    -- Plump gathering cloud-crown meniscus at liquid front
    if is_full then
      for dy = 0, FILL_H - 1 do
        local mid_d = math.abs(dy - 4.5)
        local edge_c = (mid_d <= 2.6) and PAL_MANA.crown or PAL_MANA.full_lit
        put_pixel(dst, fill_x + FILL_W - 1, fill_y + dy, edge_c)
        if mid_d <= 3.0 then
          put_pixel(dst, fill_x + FILL_W - 2, fill_y + dy, PAL_MANA.full_crest)
        end
      end
    elseif is_low then
      for dy = 0, FILL_H - 1 do
        local c = (dy == 0 or dy == 9) and PAL_MANA.cav_shadow
               or ((dy >= 2 and dy <= 5) and PAL_MANA.low_lit or PAL_MANA.low_body)
        put_pixel(dst, fill_x + px - 1, fill_y + dy, c)
      end
    elseif px >= 5 then
      for dx = 0, 4 do
        local rx = fill_x + px - 1 - dx
        for dy = 0, FILL_H - 1 do
          local ny = (dy - 4.5) / 4.5
          local cap_r = math.sqrt((dx / 4.2) ^ 2 + ny ^ 2)
          if dx <= 1 and math.abs(ny) > 0.78 then
            put_pixel(dst, rx, fill_y + dy, (dy == 0 or dy == 9) and PAL_MANA.cav_shadow or PAL_MANA.cav_bed_2)
          elseif cap_r <= 0.95 then
            local glow = 1.0 - cap_r
            local mc = (glow > 0.55) and PAL_MANA.crown or ((glow > 0.28) and PAL_MANA.volute or PAL_MANA.cloud_lit)
            put_pixel(dst, rx, fill_y + dy, mc)
          end
        end
      end
    end
  end

  -- Lotus icon rendered OVER track's left tongue so petals & outward mist overlap naturally!
  local icon_layers = CACHE_MANA_ICON_LAYERS[icon_mode][frame_idx]
  blit(dst, icon_layers.merged, x, y)
end

local function render_water_bar_2x(dst, x, y, val, frame_idx)
  local px = calc_pixels(val, 100)
  local clasp_state = (val >= 50) and 2 or ((val >= 20) and 1 or 0)
  -- V6 STRICT RULE: <=0 is static "idle", 1..99 is PURE STATIC "static", >=100 is animated "full"!
  local icon_mode = (val <= 0) and "idle" or ((val >= 100) and "full" or "static")
  local icon_frame = (val >= 100) and frame_idx or 1

  local track_x = x + WATER_TRACK_DX
  local track_y = y + WATER_TRACK_DY
  local fill_x  = track_x + FILL_X_OFFSET
  local fill_y  = track_y + FILL_Y_OFFSET

  blit(dst, CACHE_WATER_TRACK[clasp_state], track_x, track_y)

  if px > 0 then
    local tier = (val >= 50) and 3 or ((val >= 20) and 2 or 1)
    local fill = CACHE_WATER_FILL[tier][frame_idx]
    blit(dst, fill, fill_x, fill_y, px, FILL_H)

    local f0 = frame_idx - 1
    if tier == 1 then
      -- Shallow rounded trickle front at bottom
      for dx = 0, math.min(2, px - 1) do
        local rx = fill_x + px - 1 - dx
        for dy = 0, 5 - dx do
          put_pixel(dst, rx, fill_y + dy, (dy == 0) and PAL_WATER.cav_shadow or PAL_WATER.cav_bed_1)
        end
        put_pixel(dst, rx, fill_y + 6 - math.min(1, dx), PAL_WATER.t1_glint)
        put_pixel(dst, rx, fill_y + 7, PAL_WATER.t1_crest)
        put_pixel(dst, rx, fill_y + 8, PAL_WATER.t1_body)
      end
    elseif tier == 2 and px >= 5 then
      -- Upturned Ref3 #8 katana water-tip spearhead
      local bob = ((f0 % 8) < 4) and 0 or 1
      for dy = 0, FILL_H - 1 do
        put_pixel(dst, fill_x + px - 1, fill_y + dy, (dy >= 5) and PAL_WATER.cav_bed_2 or PAL_WATER.cav_bed_1)
        put_pixel(dst, fill_x + px - 2, fill_y + dy, (dy >= 6) and PAL_WATER.cav_bed_2 or PAL_WATER.cav_bed_1)
      end
      put_pixel(dst, fill_x + px - 1, fill_y + 1 + bob, PAL_WATER.t2_tip)
      put_pixel(dst, fill_x + px - 1, fill_y + 2 + bob, PAL_WATER.t2_tip)
      put_pixel(dst, fill_x + px - 2, fill_y + 2 + bob, PAL_WATER.t2_tip)
      put_pixel(dst, fill_x + px - 2, fill_y + 3 + bob, PAL_WATER.t2_blade)
      put_pixel(dst, fill_x + px - 2, fill_y + 4 + bob, PAL_WATER.t2_vein)
      put_pixel(dst, fill_x + px - 3, fill_y + 3 + bob, PAL_WATER.t2_tip)
      put_pixel(dst, fill_x + px - 3, fill_y + 4 + bob, PAL_WATER.t2_blade)
      put_pixel(dst, fill_x + px - 4, fill_y + 4 + bob, PAL_WATER.t2_tip)
    elseif tier == 3 then
      if px >= FILL_W then
        local pulse = ((f0 * 2) % 8 < 4)
        for dy = 0, FILL_H - 1 do
          local c_end = (dy >= 1 and dy <= 6) and PAL_WATER.t3_foam or (pulse and PAL_WATER.t3_blade or PAL_WATER.t3_surge)
          put_pixel(dst, fill_x + FILL_W - 1, fill_y + dy, c_end)
          if dy >= 2 and dy <= 6 then
            put_pixel(dst, fill_x + FILL_W - 2, fill_y + dy, PAL_WATER.t3_foam)
          end
        end
      elseif px >= 6 then
        -- Surging double-blade rapid spearhead
        local p4 = f0 % 4
        local shift = (p4 <= 1) and 0 or 1
        for dy = 0, FILL_H - 1 do
          put_pixel(dst, fill_x + px - 1, fill_y + dy, PAL_WATER.cav_bed_1)
          if dy == 4 or dy == 5 or dy >= 8 then
            put_pixel(dst, fill_x + px - 2, fill_y + dy, PAL_WATER.cav_bed_2)
          end
        end
        -- Upper blade tip
        put_pixel(dst, fill_x + px - 1, fill_y + 1 + shift, PAL_WATER.t3_foam)
        put_pixel(dst, fill_x + px - 2, fill_y + 1 + shift, PAL_WATER.t3_foam)
        put_pixel(dst, fill_x + px - 2, fill_y + 2 + shift, PAL_WATER.t3_blade)
        put_pixel(dst, fill_x + px - 3, fill_y + 2 + shift, PAL_WATER.t3_foam)
        -- Lower blade tip
        put_pixel(dst, fill_x + px - 1, fill_y + 6 - shift, PAL_WATER.t3_foam)
        put_pixel(dst, fill_x + px - 2, fill_y + 6 - shift, PAL_WATER.t3_foam)
        put_pixel(dst, fill_x + px - 2, fill_y + 7 - shift, PAL_WATER.t3_blade)
        put_pixel(dst, fill_x + px - 3, fill_y + 6 - shift, PAL_WATER.t3_foam)
      end
    end
  end

  local icon_layers = CACHE_WATER_ICON_LAYERS[icon_mode][icon_frame]
  blit(dst, icon_layers.merged, x + WATER_ICON_DX, y)
end

-- ============================================================================
-- EXPORT ALL NATIVE 2x TRANSPARENT-ALPHA PNGs & MULTI-LAYER .ASEPRITE SOURCES
-- ============================================================================

local function make_vertical_strip(w, h, gen_fn)
  local strip = new_image(w, h * FRAMES)
  for f = 1, FRAMES do
    blit(strip, gen_fn(f), 0, (f - 1) * h)
  end
  return strip
end

local mana_icon_empty_1 = CACHE_MANA_ICON_LAYERS.empty[1].merged
local mana_icon_norm_1  = CACHE_MANA_ICON_LAYERS.normal[1].merged
local mana_icon_full_1  = CACHE_MANA_ICON_LAYERS.full[1].merged
local mana_icon_strip      = make_vertical_strip(MANA_ICON_W, MANA_ICON_H, function(f) return CACHE_MANA_ICON_LAYERS.normal[f].merged end)
local mana_icon_full_strip = make_vertical_strip(MANA_ICON_W, MANA_ICON_H, function(f) return CACHE_MANA_ICON_LAYERS.full[f].merged end)

local water_icon_idle_1   = CACHE_WATER_ICON_LAYERS.idle[1].merged
local water_icon_static_1 = CACHE_WATER_ICON_LAYERS.static[1].merged
local water_icon_full_1   = CACHE_WATER_ICON_LAYERS.full[1].merged
local water_icon_full_strip = make_vertical_strip(WATER_ICON_W, WATER_ICON_H, function(f) return CACHE_WATER_ICON_LAYERS.full[f].merged end)

local mana_track_img  = CACHE_MANA_TRACK
local water_track_0   = CACHE_WATER_TRACK[0]
local water_track_20  = CACHE_WATER_TRACK[1]
local water_track_50  = CACHE_WATER_TRACK[2]
local water_track_states = new_image(TRACK_W, TRACK_H * 3)
blit(water_track_states, water_track_0,  0, 0)
blit(water_track_states, water_track_20, 0, TRACK_H)
blit(water_track_states, water_track_50, 0, TRACK_H * 2)

local mana_fill_low        = CACHE_MANA_FILL.low[1]
local mana_fill_strip      = make_vertical_strip(FILL_W, FILL_H, function(f) return CACHE_MANA_FILL.normal[f] end)
local mana_fill_full_strip = make_vertical_strip(FILL_W, FILL_H, function(f) return CACHE_MANA_FILL.full[f] end)

local water_fill_t1_strip  = make_vertical_strip(FILL_W, FILL_H, function(f) return CACHE_WATER_FILL[1][f] end)
local water_fill_t2_strip  = make_vertical_strip(FILL_W, FILL_H, function(f) return CACHE_WATER_FILL[2][f] end)
local water_fill_t3_strip  = make_vertical_strip(FILL_W, FILL_H, function(f) return CACHE_WATER_FILL[3][f] end)

-- Save clean transparent-alpha exports (NO water_icon_stream_strip or water_icon_surge_strip in v6!)
mana_icon_empty_1:saveAs(exp_dir .. "/mana_icon_empty.png")
mana_icon_norm_1:saveAs(exp_dir .. "/mana_icon.png")
mana_icon_strip:saveAs(exp_dir .. "/mana_icon_strip.png")
mana_icon_full_1:saveAs(exp_dir .. "/mana_icon_full.png")
mana_icon_full_strip:saveAs(exp_dir .. "/mana_icon_full_strip.png")

water_icon_idle_1:saveAs(exp_dir .. "/water_icon_idle.png")
water_icon_static_1:saveAs(exp_dir .. "/water_icon.png")
water_icon_full_1:saveAs(exp_dir .. "/water_icon_full.png")
water_icon_full_strip:saveAs(exp_dir .. "/water_icon_full_strip.png")

mana_track_img:saveAs(exp_dir .. "/mana_track.png")
mana_fill_low:saveAs(exp_dir .. "/mana_fill_low.png")
mana_fill_strip:saveAs(exp_dir .. "/mana_fill_strip.png")
mana_fill_full_strip:saveAs(exp_dir .. "/mana_fill_full_strip.png")

water_track_0:saveAs(exp_dir .. "/water_track.png")
water_track_states:saveAs(exp_dir .. "/water_track_states.png")
water_fill_t1_strip:saveAs(exp_dir .. "/water_fill_tier1_trickle_strip.png")
water_fill_t2_strip:saveAs(exp_dir .. "/water_fill_tier2_stream_strip.png")
water_fill_t3_strip:saveAs(exp_dir .. "/water_fill_tier3_torrent_strip.png")

-- Save dedicated 6-layer 16-frame editable .aseprite for Mana Lotus Icon (transparent canvas!)
do
  local spr = Sprite(MANA_ICON_W, MANA_ICON_H, ColorMode.RGB)
  spr.frames[1].duration = FRAME_DURATION
  for f = 2, FRAMES do
    local nf = spr:newEmptyFrame()
    nf.duration = FRAME_DURATION
  end
  local l1 = spr.layers[1]; l1.name = "mirror_ring_and_pendants"
  local l2 = spr:newLayer(); l2.name = "rear_embracing_clouds"
  local l3 = spr:newLayer(); l3.name = "back_crown_and_wing_petals"
  local l4 = spr:newLayer(); l4.name = "inner_cup_overflow_mist"
  local l5 = spr:newLayer(); l5.name = "front_cupping_petals_and_core"
  local l6 = spr:newLayer(); l6.name = "front_overflow_and_embracing_clouds"
  for f = 1, FRAMES do
    local ly = CACHE_MANA_ICON_LAYERS.normal[f]
    spr:newCel(l1, f, ly.base,           Point(0, 0))
    spr:newCel(l2, f, ly.rear_embrace,   Point(0, 0))
    spr:newCel(l3, f, ly.petals_back,    Point(0, 0))
    spr:newCel(l4, f, ly.inner_overflow, Point(0, 0))
    spr:newCel(l5, f, ly.petals_front,   Point(0, 0))
    spr:newCel(l6, f, ly.front_embrace,  Point(0, 0))
  end
  spr:saveAs(src_dir .. "/mana_lotus_icon_v6.aseprite")
  spr:close()
end

-- Save dedicated multi-layer 16-frame editable .aseprite for Water #8 Full-Flow Icon (transparent canvas!)
do
  local spr = Sprite(WATER_ICON_W, WATER_ICON_H, ColorMode.RGB)
  spr.frames[1].duration = FRAME_DURATION
  for f = 2, FRAMES do
    local nf = spr:newEmptyFrame()
    nf.duration = FRAME_DURATION
  end
  local l1 = spr.layers[1]; l1.name = "clean_sapphire_aura_cushion"
  local l2 = spr:newLayer(); l2.name = "three_undulating_s_streams"
  local l3 = spr:newLayer(); l3.name = "whitewater_crests_and_spray"
  for f = 1, FRAMES do
    local ly = CACHE_WATER_ICON_LAYERS.full[f]
    spr:newCel(l1, f, ly.cushion, Point(0, 0))
    spr:newCel(l2, f, ly.streams, Point(0, 0))
    spr:newCel(l3, f, ly.glints,  Point(0, 0))
  end
  spr:saveAs(src_dir .. "/water_stream8_icon_v6.aseprite")
  spr:close()
end

-- Master multi-layer 16-frame editable .aseprite + components_overview_sheet.png
do
  local mw, mh = 576, 268
  local master = Sprite(mw, mh, ColorMode.RGB)
  master.frames[1].duration = FRAME_DURATION
  for f = 2, FRAMES do
    local nf = master:newEmptyFrame()
    nf.duration = FRAME_DURATION
  end

  local l_bg       = master.layers[1]; l_bg.name = "preview_dark_backing"
  local l_tracks   = master:newLayer(); l_tracks.name = "fretwork_tracks_and_clasps"
  local l_fills    = master:newLayer(); l_fills.name  = "cloud_and_water_fills_16f"
  local l_icons    = master:newLayer(); l_icons.name  = "lotus_and_stream8_icons_16f"

  local bg_img = new_image(mw, mh, rgba(15, 18, 25, 255))
  draw_label_scaled(bg_img, 12, 8, 2, "V6 NATIVE 2x COMPONENTS (68x58 LOTUS | 48x40 WATER #8 | 204x20 TRACK)", rgba(226, 236, 252, 255), rgba(6, 8, 14, 255))

  local trk_img = new_image(mw, mh)
  blit(trk_img, mana_track_img, 252, 34)
  blit(trk_img, mana_track_img, 252, 64)
  blit(trk_img, mana_track_img, 252, 94)
  blit(trk_img, water_track_0,  252, 146)
  blit(trk_img, water_track_20, 252, 176)
  blit(trk_img, water_track_50, 252, 206)

  for f = 1, FRAMES do
    master:newCel(l_bg,     f, bg_img,  Point(0, 0))
    master:newCel(l_tracks, f, trk_img, Point(0, 0))

    local ic_img = new_image(mw, mh)
    blit(ic_img, mana_icon_empty_1,                       10,  32)
    blit(ic_img, CACHE_MANA_ICON_LAYERS.normal[f].merged, 88,  32)
    blit(ic_img, CACHE_MANA_ICON_LAYERS.full[f].merged,   166, 32)

    blit(ic_img, water_icon_idle_1,                       18,  152)
    blit(ic_img, water_icon_static_1,                     96,  152)
    blit(ic_img, CACHE_WATER_ICON_LAYERS.full[f].merged,  174, 152)
    master:newCel(l_icons, f, ic_img, Point(0, 0))

    local fl_img = new_image(mw, mh)
    blit(fl_img, mana_fill_low,             252 + FILL_X_OFFSET, 34 + FILL_Y_OFFSET)
    blit(fl_img, CACHE_MANA_FILL.normal[f], 252 + FILL_X_OFFSET, 64 + FILL_Y_OFFSET)
    blit(fl_img, CACHE_MANA_FILL.full[f],   252 + FILL_X_OFFSET, 94 + FILL_Y_OFFSET)

    blit(fl_img, CACHE_WATER_FILL[1][f],    252 + FILL_X_OFFSET, 146 + FILL_Y_OFFSET)
    blit(fl_img, CACHE_WATER_FILL[2][f],    252 + FILL_X_OFFSET, 176 + FILL_Y_OFFSET)
    blit(fl_img, CACHE_WATER_FILL[3][f],    252 + FILL_X_OFFSET, 206 + FILL_Y_OFFSET)
    master:newCel(l_fills, f, fl_img, Point(0, 0))
  end

  master:saveAs(src_dir .. "/hud_bars_v6_components.aseprite")

  local ov = new_image(mw, mh)
  blit(ov, bg_img,  0, 0)
  blit(ov, trk_img, 0, 0)
  blit(ov, master.layers[3]:cel(5).image, 0, 0)
  blit(ov, master.layers[4]:cel(5).image, 0, 0)
  draw_text(ov, 20,  96, "EMPTY",  rgba(164, 182, 210, 255), nil)
  draw_text(ov, 98,  96, "MIST16", rgba(196, 224, 255, 255), nil)
  draw_text(ov, 176, 96, "FULL16", rgba(255, 244, 206, 255), nil)
  draw_text(ov, 24,  200, "IDLE",   rgba(164, 182, 210, 255), nil)
  draw_text(ov, 98,  200, "STATIC", rgba(164, 242, 255, 255), nil)
  draw_text(ov, 174, 200, "FLOW16", rgba(232, 255, 255, 255), nil)
  draw_text(ov, 464, 41,  "MANA LOW",   rgba(164, 186, 222, 255), nil)
  draw_text(ov, 464, 71,  "MANA CLOUD", rgba(204, 228, 255, 255), nil)
  draw_text(ov, 464, 101, "MANA FULL",  rgba(246, 250, 255, 255), nil)
  draw_text(ov, 464, 153, "WATER T1",   rgba(134, 206, 220, 255), nil)
  draw_text(ov, 464, 183, "WATER T2",   rgba(164, 238, 248, 255), nil)
  draw_text(ov, 464, 213, "WATER T3",   rgba(226, 255, 255, 255), nil)
  ov:saveAs(exp_dir .. "/components_overview_sheet.png")
  master:close()
end

-- ============================================================================
-- VIEWPORT BACKGROUNDS & HUD CLUSTER HELPERS
-- ============================================================================

local function paint_daytime_sky_swatch(img, ox, oy, w, h)
  for y = 0, h - 1 do
    local t = y / math.max(1, h - 1)
    local row_c = lerp_color(rgba(122, 160, 206, 255), rgba(168, 198, 228, 255), t)
    for x = 0, w - 1 do
      local c = row_c
      if y >= math.floor(h * 0.22) and y <= math.floor(h * 0.44) and ((x + 24) % 96 < 44) then
        c = lerp_color(c, rgba(224, 236, 248, 255), 0.42)
      end
      put_pixel(img, ox + x, oy + y, c)
    end
  end
end

local function paint_daytime_viewport(img, ox, oy, w, h)
  for y = 0, h - 1 do
    for x = 0, w - 1 do
      local c
      if y < math.floor(h * 0.58) then
        local t = y / math.max(1, math.floor(h * 0.58))
        c = lerp_color(rgba(118, 156, 202, 255), rgba(162, 192, 224, 255), t)
        if y >= math.floor(h * 0.14) and y <= math.floor(h * 0.26) and ((x + 32) % 136 < 60) then
          c = lerp_color(c, rgba(222, 234, 246, 255), (y <= math.floor(h * 0.19)) and 0.52 or 0.32)
        end
      elseif y < math.floor(h * 0.70) then
        local bx = math.floor(x / 2)
        c = ((bx * 7 + 13) % 19 < 10) and rgba(82, 108, 78, 255) or rgba(74, 98, 72, 255)
      else
        local bx, by = math.floor(x / 2), math.floor(y / 2)
        local hash = (bx * 17 + by * 31) % 13
        if y < math.floor(h * 0.76) then
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
      local bx = math.floor(x / 16)
      local by = math.floor(y / 16)
      local px = math.floor(x / 2)
      local py = math.floor(y / 2)
      local hash = (px * 23 + py * 41 + bx * 7 + by * 13) % 17
      local t = y / math.max(1, h)
      local base = lerp_color(rgba(22, 25, 32, 255), rgba(14, 16, 22, 255), t)
      if hash == 0 or hash == 1 then
        base = lerp_color(base, rgba(34, 38, 47, 255), 0.7)
      elseif hash == 5 then
        base = lerp_color(base, rgba(10, 12, 16, 255), 0.7)
      end
      put_pixel(img, ox + x, oy + y, base)
    end
  end
end

-- Draws vanilla hotbar/hearts/xp at 2x scale (matching 0.5x HUD bars rendered at 1:1 native)
local function draw_vanilla_hud_ref_2x(img, ox, oy, w, h)
  local v1_w, v1_h = math.floor(w / 2), math.floor(h / 2)
  local tmp = new_image(v1_w, v1_h)
  local bar_w = 162
  local left = math.floor((v1_w - bar_w) / 2)
  local bot_y = v1_h - 22
  for i = 0, 8 do
    local sx = left + i * 18
    fill_rect(tmp, sx, bot_y, 18, 18, rgba(22, 22, 24, 210))
    stroke_rect(tmp, sx, bot_y, 18, 18, (i == 0) and rgba(210, 214, 220, 255) or rgba(78, 80, 86, 255))
  end
  local xp_y = bot_y - 7
  fill_rect(tmp, left, xp_y, bar_w, 5, rgba(16, 22, 14, 230))
  stroke_rect(tmp, left, xp_y, bar_w, 5, rgba(10, 12, 10, 255))
  fill_rect(tmp, left + 1, xp_y + 1, 96, 3, rgba(96, 182, 68, 255))
  fill_rect(tmp, left + 1, xp_y + 1, 96, 1, rgba(154, 226, 112, 255))
  local status_y = xp_y - 9
  for i = 0, 9 do
    stamp(tmp, left + i * 8, status_y, {
      ". # . # .",
      "# # # # #",
      "# # # # #",
      ". # # # .",
      ". . # . .",
    }, {["#"] = (i < 8) and rgba(188, 48, 48, 255) or rgba(52, 28, 28, 255)})
    stamp(tmp, left + bar_w - 5 - i * 8, status_y, {
      ". . # # .",
      ". # # # .",
      "# # # . .",
      "# # . . .",
      "# . . . .",
    }, {["#"] = (i < 9) and rgba(164, 108, 56, 255) or rgba(52, 38, 26, 255)})
  end
  blit(img, scale_nn(tmp, 2), ox, oy)
end

local function render_hud_cluster_2x(dst, ox, oy, mana_val, mana_max, water_val, frame_idx, show_change)
  render_mana_bar_2x(dst, ox + 8, oy + 3, mana_val, mana_max, frame_idx, nil)
  draw_label_scaled(dst, ox + 274, oy + 22, 2, string.format("%d/%d", mana_val, mana_max), PAL_MANA.text, PAL_MANA.text_shadow)

  render_water_bar_2x(dst, ox + 8, oy + 62, water_val, frame_idx)
  draw_label_scaled(dst, ox + 274, oy + 76, 2, string.format("%d/100", water_val), PAL_WATER.text, PAL_WATER.text_shadow)

  if show_change then
    draw_label_scaled(dst, ox + 80, oy + 98, 2, "+12", PAL_WATER.gain_text, PAL_WATER.text_shadow)
  end
end

-- ============================================================================
-- PREVIEW 1: ref_vs_v6_gate.png (1:1 STRICT REFERENCE VS V6 SIDE-BY-SIDE GATE)
-- ============================================================================
do
  local ref_lotus = Image{fromFile = ref_dir .. "/crop_ref_D_lotus.png"}
  local ref_water = Image{fromFile = ref_dir .. "/crop_ref_8_water.png"}

  local cw, ch = 1680, 1080
  local gate = new_image(cw, ch, rgba(13, 16, 22, 255))

  fill_rect(gate, 0, 0, cw, 54, rgba(20, 25, 36, 255))
  stroke_rect(gate, 0, 0, cw, 54, rgba(46, 58, 82, 255))
  draw_label_scaled(gate, 24, 16, 3, "V6 STRICT 1:1 GATE: LOTUS CUP-OVERFLOW & CLOUD EMBRACE + WATER #8 STATIC/100-FLOW", rgba(232, 240, 255, 255), rgba(8, 10, 16, 255))

  local cols = {
    {x = 24,   w = 516, title = "1. ORIGINAL REFERENCE CROP",         border = rgba(86, 102, 134, 255)},
    {x = 560,  w = 548, title = "2. V6 PIXEL ART (DARK BACKING)",     border = rgba(76, 122, 198, 255)},
    {x = 1128, w = 528, title = "3. V6 DAYTIME SKY + HUD 0.5x SCALE", border = rgba(58, 148, 176, 255)},
  }

  for _, col in ipairs(cols) do
    fill_rect(gate, col.x, 70, col.w, 478, rgba(18, 22, 31, 255))
    stroke_rect(gate, col.x, 70, col.w, 478, col.border)
    draw_label_scaled(gate, col.x + 16, 84, 2, col.title, rgba(226, 236, 252, 255), rgba(8, 10, 16, 255))

    fill_rect(gate, col.x, 566, col.w, 490, rgba(18, 22, 31, 255))
    stroke_rect(gate, col.x, 566, col.w, 490, col.border)
    draw_label_scaled(gate, col.x + 16, 580, 2, col.title, rgba(226, 236, 252, 255), rgba(8, 10, 16, 255))
  end

  -- Row 1 Col 1: Original crop_ref_D_lotus.png
  fill_rect(gate, 44, 112, 476, 416, rgba(23, 27, 36, 255))
  stroke_rect(gate, 44, 112, 476, 416, rgba(46, 56, 74, 255))
  blit(gate, ref_lotus, 44 + math.floor((476 - ref_lotus.width) / 2), 140)
  draw_label_scaled(gate, 56, 496, 2, "REF2 D: BLOOMING LOTUS + CLOUD MIST + FRET BRIDGE", rgba(176, 196, 226, 255), nil)

  -- Row 1 Col 2: V6 Lotus + Connected Fretwork Track at 4x (F05 peak cup-overflow + embracing clouds)
  fill_rect(gate, 580, 112, 508, 416, rgba(23, 27, 36, 255))
  stroke_rect(gate, 580, 112, 508, 416, rgba(46, 56, 74, 255))
  do
    local connected_crop = new_image(116, 58)
    render_mana_bar_2x(connected_crop, 2, 0, 80, 80, 5, nil)
    local up4 = scale_nn(connected_crop, 4) -- 464x232
    blit(gate, up4, 580 + math.floor((508 - up4.width) / 2), 132)

    blit(gate, scale_nn(mana_icon_empty_1, 2),                       596, 398)
    blit(gate, scale_nn(CACHE_MANA_ICON_LAYERS.normal[5].merged, 2), 766, 398)
    blit(gate, scale_nn(CACHE_MANA_ICON_LAYERS.full[5].merged, 2),   936, 398)
    draw_label_scaled(gate, 626, 380, 2, "EMPTY",   rgba(154, 172, 200, 255), nil)
    draw_label_scaled(gate, 786, 380, 2, "NORMAL",  rgba(196, 224, 255, 255), nil)
    draw_label_scaled(gate, 964, 380, 2, "FULL",    rgba(255, 244, 204, 255), nil)
  end

  -- Row 1 Col 3: V6 Lotus on Bright Daytime Sky (ZERO grey mist halo!) + HUD Scale Check
  paint_daytime_sky_swatch(gate, 1148, 112, 488, 276)
  stroke_rect(gate, 1148, 112, 488, 276, rgba(64, 88, 116, 255))
  do
    local connected_sky = new_image(116, 58)
    render_mana_bar_2x(connected_sky, 2, 0, 80, 80, 5, nil)
    blit(gate, scale_nn(connected_sky, 4), 1148 + math.floor((488 - 464) / 2), 134)

    fill_rect(gate, 1148, 400, 238, 128, rgba(20, 24, 34, 255))
    stroke_rect(gate, 1148, 400, 238, 128, rgba(48, 60, 82, 255))
    draw_label_scaled(gate, 1158, 410, 2, "GUI SCALE 2 (0.5x=1:1)", rgba(196, 214, 240, 255), nil)
    local mini_bar = new_image(262, 58)
    render_mana_bar_2x(mini_bar, 0, 0, 58, 80, 5, nil)
    blit(gate, mini_bar, 1156, 442, 222, 58, 0, 0)

    paint_daytime_sky_swatch(gate, 1398, 400, 238, 128)
    stroke_rect(gate, 1398, 400, 238, 128, rgba(64, 88, 116, 255))
    draw_label_scaled(gate, 1408, 410, 2, "DAY SKY 1:1 NATIVE", rgba(244, 250, 255, 255), rgba(16, 26, 44, 255))
    blit(gate, mini_bar, 1406, 442, 222, 58, 0, 0)
  end

  -- Row 2 Col 1: Original crop_ref_8_water.png
  fill_rect(gate, 44, 608, 476, 428, rgba(11, 14, 25, 255))
  stroke_rect(gate, 44, 608, 476, 428, rgba(46, 56, 74, 255))
  blit(gate, ref_water, 44 + math.floor((476 - ref_water.width) / 2), 624)
  draw_label_scaled(gate, 72, 1006, 2, "REF3 #8: THREE S-STREAM KATANA WATER BLADES", rgba(164, 214, 236, 255), nil)

  -- Row 2 Col 2: V6 Water #8 Icon at 6x + IDLE (0) / STATIC (<100) / FLOWING (100) @ 2x
  fill_rect(gate, 580, 608, 508, 428, rgba(11, 14, 25, 255))
  stroke_rect(gate, 580, 608, 508, 428, rgba(46, 56, 74, 255))
  do
    local w_up6 = scale_nn(water_icon_static_1, 6) -- 288x240
    local bx = 580 + math.floor((508 - w_up6.width) / 2)
    local by = 628
    stroke_rect(gate, bx - 6, by - 6, w_up6.width + 12, w_up6.height + 12, rgba(72, 74, 112, 255))
    blit(gate, w_up6, bx, by)

    blit(gate, scale_nn(water_icon_idle_1,   2),                       614, 924)
    blit(gate, scale_nn(water_icon_static_1, 2),                       786, 924)
    blit(gate, scale_nn(CACHE_WATER_ICON_LAYERS.full[5].merged, 2),    958, 924)
    draw_label_scaled(gate, 622, 900, 2, "0 IDLE",      rgba(148, 172, 200, 255), nil)
    draw_label_scaled(gate, 770, 900, 2, "1..99 STATIC",rgba(164, 238, 255, 255), nil)
    draw_label_scaled(gate, 948, 900, 2, "100 FLOW16",  rgba(232, 255, 255, 255), nil)
  end

  -- Row 2 Col 3: V6 Water #8 on Bright Daytime Sky (Clean Sapphire Aura, ZERO grey halo!) + HUD Scale Check
  paint_daytime_sky_swatch(gate, 1148, 608, 488, 286)
  stroke_rect(gate, 1148, 608, 488, 286, rgba(64, 88, 116, 255))
  do
    local w_up6 = scale_nn(CACHE_WATER_ICON_LAYERS.full[5].merged, 6)
    blit(gate, w_up6, 1148 + math.floor((488 - w_up6.width) / 2), 628)

    fill_rect(gate, 1148, 906, 238, 130, rgba(16, 20, 30, 255))
    stroke_rect(gate, 1148, 906, 238, 130, rgba(48, 60, 82, 255))
    draw_label_scaled(gate, 1158, 916, 2, "GUI SCALE 2 (100 FLOW)", rgba(196, 214, 240, 255), nil)
    local mini_wbar = new_image(262, 44)
    render_water_bar_2x(mini_wbar, 0, 0, 100, 5)
    blit(gate, mini_wbar, 1156, 954, 222, 44, 0, 0)

    paint_daytime_sky_swatch(gate, 1398, 906, 238, 130)
    stroke_rect(gate, 1398, 906, 238, 130, rgba(64, 88, 116, 255))
    draw_label_scaled(gate, 1408, 916, 2, "DAY SKY 1:1 NATIVE", rgba(244, 250, 255, 255), rgba(16, 26, 44, 255))
    blit(gate, mini_wbar, 1406, 954, 222, 44, 0, 0)
  end

  gate:saveAs(prv_dir .. "/ref_vs_v6_gate.png")
end

-- ============================================================================
-- PREVIEW 2: anim_frames_audit.png (F01 / F05 / F09 / F13 STATIC MOTION PROOF)
-- ============================================================================
do
  local aw, ah = 1680, 1160
  local audit = new_image(aw, ah, rgba(13, 16, 22, 255))
  fill_rect(audit, 0, 0, aw, 50, rgba(20, 25, 36, 255))
  stroke_rect(audit, 0, 0, aw, 50, rgba(46, 58, 82, 255))
  draw_label_scaled(audit, 24, 14, 3, "V6 FRAME DISPLACEMENT AUDIT (F01 / F05 / F09 / F13): LOTUS OVERFLOW + WATER 100 WAVE VS 68 STATIC", rgba(232, 240, 255, 255), rgba(8, 10, 16, 255))

  local sample_frames = {1, 5, 9, 13}
  local col_w = 396
  local rows_meta = {
    {y = 64,  h = 286, title = "ROW 1: MANA 54/80 NORMAL (DARK) - CUP OVERFLOW PLUMES + EMBRACING CLOUDS (4x)", sky = false},
    {y = 362, h = 286, title = "ROW 2: MANA 80/80 FULL (DAY SKY) - PURE TRANSLUCENT VAPOR, ZERO GREY HALO (4x)", sky = true},
    {y = 660, h = 294, title = "ROW 3: WATER 100/100 FULL (ICON FLOWING) - S-WAVE UNDULATION + GLIDING SPRAY (5x)", sky = false},
    {y = 966, h = 178, title = "ROW 4: WATER 68/100 UNFILLED CONTROL - ICON 100% STATIC WHILE BAR FILL FLOWS (2x)", sky = false},
  }

  for _, rm in ipairs(rows_meta) do
    fill_rect(audit, 20, rm.y, aw - 40, rm.h, rgba(18, 22, 31, 255))
    stroke_rect(audit, 20, rm.y, aw - 40, rm.h, rgba(56, 78, 112, 255))
    draw_label_scaled(audit, 34, rm.y + 10, 2, rm.title, rgba(224, 236, 252, 255), rgba(8, 10, 16, 255))
  end

  for ci, f in ipairs(sample_frames) do
    local cx = 36 + (ci - 1) * (col_w + 12)
    local ftag = string.format("F%02d/16", f)

    -- Row 1: Mana Normal 4x on dark + reference guide ticks
    fill_rect(audit, cx, 94, col_w - 8, 244, rgba(15, 19, 28, 255))
    stroke_rect(audit, cx, 94, col_w - 8, 244, rgba(44, 58, 84, 255))
    for _, gy in ipairs({142, 174, 206, 242}) do
      fill_rect(audit, cx + 8, gy, col_w - 24, 1, rgba(42, 62, 92, 130))
    end
    local m_norm_4x = scale_nn(CACHE_MANA_ICON_LAYERS.normal[f].merged, 4) -- 272x232
    blit(audit, m_norm_4x, cx + math.floor((col_w - 8 - m_norm_4x.width) / 2), 100)
    draw_label_scaled(audit, cx + 12, 102, 2, ftag, rgba(196, 226, 255, 255), rgba(8, 10, 16, 255))

    -- Row 2: Mana Full 4x on daytime sky swatch
    paint_daytime_sky_swatch(audit, cx, 392, col_w - 8, 244)
    stroke_rect(audit, cx, 392, col_w - 8, 244, rgba(68, 96, 128, 255))
    local m_full_4x = scale_nn(CACHE_MANA_ICON_LAYERS.full[f].merged, 4)
    blit(audit, m_full_4x, cx + math.floor((col_w - 8 - m_full_4x.width) / 2), 398)
    draw_label_scaled(audit, cx + 12, 400, 2, ftag, rgba(248, 252, 255, 255), rgba(16, 26, 44, 255))

    -- Row 3: Water 100/100 FULL 5x (240x200) with horizontal wave-height reference lines!
    fill_rect(audit, cx, 690, col_w - 8, 252, rgba(11, 15, 25, 255))
    stroke_rect(audit, cx, 690, col_w - 8, 252, rgba(44, 96, 124, 255))
    for _, gy in ipairs({745, 795, 850}) do
      fill_rect(audit, cx + 12, gy, col_w - 32, 1, rgba(46, 98, 128, 150))
    end
    local w_full_5x = scale_nn(CACHE_WATER_ICON_LAYERS.full[f].merged, 5) -- 240x200
    blit(audit, w_full_5x, cx + math.floor((col_w - 8 - w_full_5x.width) / 2), 716)
    draw_label_scaled(audit, cx + 12, 698, 2, ftag .. " FLOW", rgba(168, 248, 255, 255), rgba(8, 10, 16, 255))

    -- Row 4: Water 68/100 Unfilled Control (bar at 2x cropped: icon static + torrent fill moving!)
    fill_rect(audit, cx, 996, col_w - 8, 136, rgba(14, 19, 27, 255))
    stroke_rect(audit, cx, 996, col_w - 8, 136, rgba(48, 72, 96, 255))
    local w_ctrl = new_image(182, 44)
    render_water_bar_2x(w_ctrl, 0, 2, 68, f)
    blit(audit, scale_nn(w_ctrl, 2), cx + 12, 1030)
    draw_label_scaled(audit, cx + 12, 1006, 2, ftag .. " ICON STATIC", rgba(164, 214, 236, 255), rgba(8, 10, 16, 255))
  end

  audit:saveAs(prv_dir .. "/anim_frames_audit.png")
end

-- ============================================================================
-- PREVIEW 3: hud_context_day_night_4x.png
-- ============================================================================
do
  local vp_w, vp_h = 528, 248
  local top_w = vp_w * 2 + 36
  local top_h = 42 + vp_h * 2 + 32
  local ctx_2x = new_image(top_w, top_h, rgba(14, 17, 24, 255))

  fill_rect(ctx_2x, 0, 0, top_w, 32, rgba(22, 27, 38, 255))
  draw_label_scaled(ctx_2x, 14, 9, 2, "V6 HUD CONTEXT: UNFILLED WATER (<100 ICON STATIC) VS 100/100 FULL (ICON FLOWING)", rgba(228, 238, 252, 255), rgba(8, 10, 16, 255))

  local x_left  = 12
  local x_right = vp_w + 24
  local y_day   = 42
  local y_cave  = 42 + vp_h + 16

  -- Daytime Sky + Grass: Left (54/80 + 68/100 Static Icon) | Right (80/80 Full + 100/100 Flowing Icon)
  paint_daytime_viewport(ctx_2x, x_left, y_day, vp_w, vp_h)
  draw_vanilla_hud_ref_2x(ctx_2x, x_left, y_day, vp_w, vp_h)
  render_hud_cluster_2x(ctx_2x, x_left, y_day, 54, 80, 68, 5, true)
  stroke_rect(ctx_2x, x_left, y_day, vp_w, vp_h, rgba(58, 78, 104, 255))

  paint_daytime_viewport(ctx_2x, x_right, y_day, vp_w, vp_h)
  draw_vanilla_hud_ref_2x(ctx_2x, x_right, y_day, vp_w, vp_h)
  render_hud_cluster_2x(ctx_2x, x_right, y_day, 80, 80, 100, 5, false)
  stroke_rect(ctx_2x, x_right, y_day, vp_w, vp_h, rgba(58, 78, 104, 255))

  -- Dark Cave: Left (54/80 + 68/100 Static Icon) | Right (80/80 Full + 100/100 Flowing Icon)
  paint_cave_viewport(ctx_2x, x_left, y_cave, vp_w, vp_h)
  draw_vanilla_hud_ref_2x(ctx_2x, x_left, y_cave, vp_w, vp_h)
  render_hud_cluster_2x(ctx_2x, x_left, y_cave, 54, 80, 68, 5, true)
  stroke_rect(ctx_2x, x_left, y_cave, vp_w, vp_h, rgba(48, 58, 78, 255))

  paint_cave_viewport(ctx_2x, x_right, y_cave, vp_w, vp_h)
  draw_vanilla_hud_ref_2x(ctx_2x, x_right, y_cave, vp_w, vp_h)
  render_hud_cluster_2x(ctx_2x, x_right, y_cave, 80, 80, 100, 5, false)
  stroke_rect(ctx_2x, x_right, y_cave, vp_w, vp_h, rgba(48, 58, 78, 255))

  -- Capture clean HUD crops (full 80/80 + 100/100 day & cave) BEFORE stamping viewport corner labels
  local crop_day  = new_image(366, 120); blit(crop_day,  ctx_2x, 0, 0, 366, 120, x_right + 2, y_day + 2)
  local crop_cave = new_image(366, 120); blit(crop_cave, ctx_2x, 0, 0, 366, 120, x_right + 2, y_cave + 2)

  local function tag_vp(vx, vy, label)
    fill_rect(ctx_2x, vx + vp_w - 168, vy + 8, 160, 20, rgba(12, 16, 24, 195))
    draw_label_scaled(ctx_2x, vx + vp_w - 162, vy + 11, 2, label, rgba(226, 236, 252, 255), rgba(6, 8, 14, 255))
  end
  tag_vp(x_left,  y_day,  "DAY | 68 STATIC")
  tag_vp(x_right, y_day,  "DAY | 100 FLOW")
  tag_vp(x_left,  y_cave, "CAVE | 68 STATIC")
  tag_vp(x_right, y_cave, "CAVE | 100 FLOW")

  local spr_ctx = Sprite(ctx_2x.width, ctx_2x.height, ColorMode.RGB)
  spr_ctx.layers[1].name = "hud_context_2x_native"
  spr_ctx.cels[1].image = ctx_2x
  spr_ctx:saveAs(src_dir .. "/hud_context_day_night.aseprite")
  spr_ctx:close()

  local main_4x = scale_nn(ctx_2x, 2)
  local out_h = main_4x.height + 256
  local out = new_image(main_4x.width, out_h, rgba(12, 15, 21, 255))
  blit(out, main_4x, 0, 0)

  local by = main_4x.height + 8
  fill_rect(out, 24, by, main_4x.width - 48, 232, rgba(18, 22, 31, 255))
  stroke_rect(out, 24, by, main_4x.width - 48, 232, rgba(48, 60, 82, 255))
  draw_label_scaled(out, 44, by + 12, 3, "REAL IN-GAME 0.5x POSE-SCALED RENDER INSETS: GUI SCALE 2 (1:1 NATIVE PIXELS) & GUI SCALE 3 (1.5x)", rgba(224, 236, 252, 255), rgba(8, 10, 16, 255))

  draw_label_scaled(out, 48,  by + 48, 2, "GUI SCALE 2 DAY (1:1 NATIVE):",  rgba(186, 210, 240, 255), nil)
  blit(out, crop_day, 48, by + 72)
  stroke_rect(out, 48, by + 72, crop_day.width, crop_day.height, rgba(64, 84, 110, 255))

  draw_label_scaled(out, 432, by + 48, 2, "GUI SCALE 2 CAVE (1:1 NATIVE):", rgba(186, 210, 240, 255), nil)
  blit(out, crop_cave, 432, by + 72)
  stroke_rect(out, 432, by + 72, crop_cave.width, crop_cave.height, rgba(52, 64, 86, 255))

  local day_gs3  = scale_nn_ratio(crop_day,  3, 2)
  local cave_gs3 = scale_nn_ratio(crop_cave, 3, 2)
  draw_label_scaled(out, 840,  by + 38, 2, "GUI SCALE 3 DAY (3x*0.5F = 1.5x):",  rgba(196, 224, 252, 255), nil)
  blit(out, day_gs3, 840, by + 56)
  stroke_rect(out, 840, by + 56, day_gs3.width, day_gs3.height, rgba(64, 84, 110, 255))

  draw_label_scaled(out, 1440, by + 38, 2, "GUI SCALE 3 CAVE (3x*0.5F = 1.5x):", rgba(196, 224, 252, 255), nil)
  blit(out, cave_gs3, 1440, by + 56)
  stroke_rect(out, 1440, by + 56, cave_gs3.width, cave_gs3.height, rgba(52, 64, 86, 255))

  out:saveAs(prv_dir .. "/hud_context_day_night_4x.png")
end

-- ============================================================================
-- PREVIEW 4: states_progression_4x.png (5 MANA STATES + 5 WATER STATES, DARK & DAY)
-- ============================================================================
do
  local bw, bh = 980, 628
  local prog = new_image(bw, bh, rgba(14, 17, 24, 255))

  fill_rect(prog, 0, 0, bw, 28, rgba(22, 27, 38, 255))
  draw_label_scaled(prog, 14, 8, 2, "V6 STATES PROGRESSION (4x LOGICAL HUD): DARK CAVE (LEFT) VS DAYTIME SKY (RIGHT)", rgba(232, 240, 255, 255), rgba(8, 10, 16, 255))

  fill_rect(prog, 12, 36, 472, 314, rgba(18, 21, 29, 255))
  stroke_rect(prog, 12, 36, 472, 314, rgba(52, 74, 118, 255))
  paint_daytime_sky_swatch(prog, 496, 36, 472, 314)
  stroke_rect(prog, 496, 36, 472, 314, rgba(68, 96, 128, 255))

  fill_rect(prog, 12, 362, 472, 254, rgba(16, 21, 28, 255))
  stroke_rect(prog, 12, 362, 472, 254, rgba(44, 104, 124, 255))
  paint_daytime_sky_swatch(prog, 496, 362, 472, 254)
  stroke_rect(prog, 496, 362, 472, 254, rgba(68, 96, 128, 255))

  local mana_states = {
    {val = 0,  max = 80, ghost = nil, label = "0/80 EMPTY (NO MIST)"},
    {val = 14, max = 80, ghost = nil, label = "14/80 LOW + OVERFLOW"},
    {val = 54, max = 80, ghost = nil, label = "54/80 OVERFLOW+EMBRACE"},
    {val = 48, max = 80, ghost = 68,  label = "48<-68 CAST AFTERGLOW"},
    {val = 80, max = 80, ghost = nil, label = "80/80 FULL OVERFLOW"},
  }

  for i, st in ipairs(mana_states) do
    local ry = 42 + (i - 1) * 61
    render_mana_bar_2x(prog, 18,  ry, st.val, st.max, 5, st.ghost)
    draw_label_scaled(prog, 284, ry + 19, 2, st.label, PAL_MANA.text, PAL_MANA.text_shadow)

    render_mana_bar_2x(prog, 502, ry, st.val, st.max, 5, st.ghost)
    fill_rect(prog, 764, ry + 15, 196, 18, rgba(16, 28, 48, 125))
    draw_label_scaled(prog, 768, ry + 19, 2, st.label, rgba(246, 250, 255, 255), rgba(16, 24, 42, 255))
  end

  local water_states = {
    {val = 0,   label = "0/100 IDLE"},
    {val = 12,  label = "12/100 ICON STATIC"},
    {val = 35,  label = "35/100 ICON STATIC"},
    {val = 68,  label = "68/100 ICON STATIC"},
    {val = 100, label = "100/100 ICON FLOWING"},
  }

  for i, st in ipairs(water_states) do
    local ry = 370 + (i - 1) * 48
    render_water_bar_2x(prog, 18,  ry, st.val, 5)
    draw_label_scaled(prog, 284, ry + 14, 2, st.label, (st.val >= 100) and rgba(232, 255, 255, 255) or PAL_WATER.text, PAL_WATER.text_shadow)

    render_water_bar_2x(prog, 502, ry, st.val, 5)
    fill_rect(prog, 764, ry + 10, 196, 18, rgba(14, 30, 44, 125))
    draw_label_scaled(prog, 768, ry + 14, 2, st.label, rgba(242, 252, 255, 255), rgba(12, 26, 36, 255))
  end

  local spr_p = Sprite(prog.width, prog.height, ColorMode.RGB)
  spr_p.layers[1].name = "states_progression_2x"
  spr_p.cels[1].image = prog
  spr_p:saveAs(src_dir .. "/states_progression.aseprite")
  spr_p:close()

  scale_nn(prog, 2):saveAs(prv_dir .. "/states_progression_4x.png")
end

-- ============================================================================
-- PREVIEW 5: lowkey_flow_preview_4x.gif (16 FRAMES @ 90ms SEAMLESS LOOP)
-- ============================================================================
do
  -- Build at 2x native resolution (716x360) -> scale_nn(2) -> 1432x720 crisp 4x HUD logical GIF + live icon zoom pods!
  local gw, gh = 716, 360
  local spr_gif_2x = Sprite(gw, gh, ColorMode.RGB)
  local spr_gif_4x = Sprite(gw * 2, gh * 2, ColorMode.RGB)
  spr_gif_2x.frames[1].duration = FRAME_DURATION
  spr_gif_4x.frames[1].duration = FRAME_DURATION
  for f = 2, FRAMES do
    local nf2 = spr_gif_2x:newEmptyFrame(); nf2.duration = FRAME_DURATION
    local nf4 = spr_gif_4x:newEmptyFrame(); nf4.duration = FRAME_DURATION
  end
  local l_gif_2x = spr_gif_2x.layers[1]; l_gif_2x.name = "flow_preview_2x"
  local l_gif_4x = spr_gif_4x.layers[1]; l_gif_4x.name = "flow_preview_4x"

  for f = 1, FRAMES do
    local frm = new_image(gw, gh, rgba(14, 17, 24, 255))
    fill_rect(frm, 0, 0, gw, 26, rgba(22, 27, 38, 255))
    draw_label_scaled(frm, 12, 7, 2, string.format("V6 16F LOOP (90MS): LOTUS CUP-OVERFLOW & EMBRACE + WATER 100 FLOW  F%02d/16", f), rgba(228, 238, 252, 255), rgba(8, 10, 16, 255))

    -- Left Mana panel (54/80 Normal Overflow + 80/80 Full Overflow)
    fill_rect(frm, 10, 32, 540, 132, rgba(18, 22, 31, 255))
    stroke_rect(frm, 10, 32, 540, 132, rgba(56, 80, 128, 255))
    render_mana_bar_2x(frm, 16, 36, 54, 80, f, nil)
    draw_label_scaled(frm, 284, 54, 2, "MANA 54/80 (CUP OVERFLOW)", PAL_MANA.text, PAL_MANA.text_shadow)

    render_mana_bar_2x(frm, 16, 100, 80, 80, f, nil)
    draw_label_scaled(frm, 284, 118, 2, "MANA 80/80 FULL (EMBRACE)", rgba(255, 244, 206, 255), PAL_MANA.text_shadow)

    -- Left Water panel (35/100 Static | 68/100 Static | 100/100 FULL Flowing!)
    fill_rect(frm, 10, 172, 540, 178, rgba(16, 21, 29, 255))
    stroke_rect(frm, 10, 172, 540, 178, rgba(44, 108, 128, 255))

    render_water_bar_2x(frm, 16, 178, 35, f)
    draw_label_scaled(frm, 284, 192, 2, "WATER 35/100 (ICON STATIC)", PAL_WATER.text, PAL_WATER.text_shadow)

    render_water_bar_2x(frm, 16, 234, 68, f)
    draw_label_scaled(frm, 284, 248, 2, "WATER 68/100 (ICON STATIC)", rgba(164, 232, 246, 255), PAL_WATER.text_shadow)

    render_water_bar_2x(frm, 16, 292, 100, f)
    draw_label_scaled(frm, 284, 306, 2, "WATER 100/100 FULL (ICON FLOWING)", rgba(232, 255, 255, 255), PAL_WATER.text_shadow)

    -- Right Live Magnified Icon Pods (2x here -> 4x native / 8x logical in GIF!)
    fill_rect(frm, 558, 32, 148, 152, rgba(18, 22, 31, 255))
    stroke_rect(frm, 558, 32, 148, 152, rgba(76, 116, 188, 255))
    draw_text(frm, 568, 40, "LOTUS 80/80 ZOOM", rgba(214, 234, 255, 255), nil)
    blit(frm, scale_nn(CACHE_MANA_ICON_LAYERS.full[f].merged, 2), 564, 56)

    fill_rect(frm, 558, 192, 148, 158, rgba(14, 19, 28, 255))
    stroke_rect(frm, 558, 192, 148, 158, rgba(48, 134, 162, 255))
    draw_text(frm, 566, 200, "68 STATIC VS 100 FLOW", rgba(176, 244, 255, 255), nil)
    blit(frm, water_icon_static_1, 568, 218)
    draw_text(frm, 622, 234, "68:STATIC", rgba(156, 206, 228, 255), nil)
    blit(frm, scale_nn(CACHE_WATER_ICON_LAYERS.full[f].merged, 2), 584, 264)

    spr_gif_2x:newCel(l_gif_2x, f, frm, Point(0, 0))
    spr_gif_4x:newCel(l_gif_4x, f, scale_nn(frm, 2), Point(0, 0))
  end

  spr_gif_2x:saveAs(src_dir .. "/lowkey_flow_preview.aseprite")
  spr_gif_4x:saveAs(prv_dir .. "/lowkey_flow_preview_4x.gif")
  spr_gif_2x:close()
  spr_gif_4x:close()
end

print("V6_BUILD_SUCCESS")
