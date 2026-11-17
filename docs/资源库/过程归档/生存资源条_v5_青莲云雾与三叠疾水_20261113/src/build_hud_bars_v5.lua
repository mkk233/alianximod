-- ============================================================================
-- XueXi Mod HUD Resource Bars v5 (2x High-Density Pixel Craftsmanship)
--   1. Mana Icon (64x56, 0.5x HUD = 32x28): Ref2 D Blooming Ice-Azure Lotus
--      + Warm Ivory-Gold / Lavender-Pink Core + Dark Mirror Ring & Dewdrops
--      + 16-Frame Outward-Drifting Cloud Mist ("莲花朝外散出云雾")
--   2. Water Icon (48x40, 0.5x HUD = 24x20): Ref3 #8 Three S-Stream Blades
--      + Blazing White Katana Bellies + Soft Deep-Sea Cushion (ZERO black line)
--   3. Double-Stepped Chinese Fretwork Track (204x20, inner chamber 180x10;
--      0.5x HUD = 102x10, inner 90x5) preserving v4.1 acclaimed fill dynamics
-- ============================================================================

local root    = "D:/alianximod/docs/资源库/过程归档/生存资源条_v5_青莲云雾与三叠疾水_20261113"
local ref_dir = root .. "/ref"
local src_dir = root .. "/src"
local exp_dir = root .. "/export"
local prv_dir = root .. "/preview"

local FRAMES = 16
local FRAME_DURATION = 0.09

local MANA_ICON_W, MANA_ICON_H   = 64, 56
local WATER_ICON_W, WATER_ICON_H = 48, 40
local TRACK_W, TRACK_H           = 204, 20
local FILL_W, FILL_H             = 180, 10
local FILL_X_OFFSET              = 18
local FILL_Y_OFFSET              = 5

local MANA_TRACK_DX  = 52
local MANA_TRACK_DY  = 14
local WATER_ICON_DX  = 6
local WATER_TRACK_DX = 52
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

local LOTUS_CX = 31
local function mir_x(x) return 62 - x end

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

  local l_ring   = new_image(MANA_ICON_W, MANA_ICON_H)
  local l_rear   = new_image(MANA_ICON_W, MANA_ICON_H)
  local l_petals = new_image(MANA_ICON_W, MANA_ICON_H)
  local l_front  = new_image(MANA_ICON_W, MANA_ICON_H)

  local r_cx, r_cy = 31.0, 23.5
  local r_in, r_out = 17.5, 19.7
  for y = 3, 44 do
    for x = 10, 52 do
      local dx = x - r_cx
      local dy = y - r_cy
      local d = math.sqrt(dx * dx + dy * dy)
      if d <= r_in then
        local ny = dy / r_in
        local swirl = math.sin(dx * 0.34 + dy * 0.25) * math.cos(dx * 0.18 - dy * 0.32)
        local base = lerp_color(rgba(13, 30, 50, 255), rgba(9, 19, 34, 255), clamp((ny + 1.0) * 0.5, 0, 1))
        if not is_empty and d > 11.5 and ny < 0.15 and swirl > 0.15 then
          base = lerp_color(base, rgba(22, 62, 96, 255), (swirl - 0.15) * (is_full and 0.72 or 0.52))
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
    put_sym(l_ring, 31, 1, b_mid)
    put_sym(l_ring, 30, 2, b_mid); put_sym(l_ring, 31, 2, b_ice)
    put_sym(l_ring, 29, 3, b_dp);  put_sym(l_ring, 30, 3, b_ice); put_sym(l_ring, 31, 3, b_hi)
    put_sym(l_ring, 30, 4, b_dp);  put_sym(l_ring, 31, 4, b_ice)
    put_sym(l_ring, 31, 5, b_mid)
  end

  do
    local gold_stem = is_empty and rgba(96, 92, 88, 255) or rgba(255, 232, 182, 255)
    local d_hi  = is_empty and rgba(118, 150, 184, 255) or rgba(255, 255, 255, 255)
    local d_ice = is_empty and rgba(82, 116, 156, 255)  or rgba(168, 244, 255, 255)
    local d_cyn = is_empty and rgba(56, 86, 126, 255)   or rgba(82, 206, 250, 255)
    local d_blu = is_empty and rgba(38, 60, 96, 255)    or rgba(42, 124, 210, 255)
    local d_dp  = is_empty and rgba(26, 42, 70, 255)    or rgba(28, 86, 166, 255)

    put_sym(l_ring, 31, 42, gold_stem)
    put_sym(l_ring, 31, 43, d_ice)
    put_sym(l_ring, 30, 44, d_cyn); put_sym(l_ring, 31, 44, d_hi)
    put_sym(l_ring, 30, 45, d_blu); put_sym(l_ring, 31, 45, d_cyn)
    put_sym(l_ring, 31, 46, gold_stem)
    put_sym(l_ring, 31, 47, d_ice)
    put_sym(l_ring, 30, 48, d_cyn); put_sym(l_ring, 31, 48, d_hi)
    put_sym(l_ring, 29, 49, d_blu); put_sym(l_ring, 30, 49, d_ice); put_sym(l_ring, 31, 49, d_hi)
    put_sym(l_ring, 29, 50, d_dp);  put_sym(l_ring, 30, 50, d_cyn); put_sym(l_ring, 31, 50, d_ice)
    put_sym(l_ring, 30, 51, d_blu); put_sym(l_ring, 31, 51, d_cyn)
    put_sym(l_ring, 31, 52, d_cyn)
    put_sym(l_ring, 31, 53, d_blu)
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

  local back_tall_spans = {
    {15, 15}, {15, 16}, {14, 18}, {14, 19}, {14, 21},
    {14, 22}, {14, 23}, {14, 24}, {15, 24}, {15, 25},
    {15, 25}, {16, 25}, {16, 25}, {17, 25}, {18, 25},
  }
  draw_paired_petal(l_petals, 10, back_tall_spans,
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
  draw_center_petal(l_petals, 5, center_crown_hw,
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
    {5,  6},  {6,  9},  {7,  13}, {7,  16}, {8,  19},
    {9,  21}, {9,  22}, {10, 23}, {11, 23}, {12, 23},
    {13, 23}, {14, 23}, {16, 23}, {18, 23},
  }
  draw_paired_petal(l_petals, 17, wing_spans,
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

  local inner_cup_spans = {
    {20, 20}, {20, 22}, {19, 23}, {19, 25}, {19, 26},
    {19, 27}, {20, 27}, {20, 28}, {21, 28}, {21, 28},
    {22, 28}, {23, 28}, {24, 28}, {25, 28},
  }
  draw_paired_petal(l_petals, 15, inner_cup_spans,
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
  draw_center_petal(l_petals, 12, inner_center_hw,
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
    local core_r = (is_full and 6.4 or 5.5) + 0.55 * breath
    for y = 16, 29 do
      for x = 24, 38 do
        local cur = l_petals:getPixel(x, y)
        if a_of(cur) > 0 then
          local is_bright_rim = (r_of(cur) >= 244 and g_of(cur) >= 249 and b_of(cur) >= 245)
          local dx = (x - 31.0) / 1.00
          local dy = (y - 23.6) / 1.22
          local d = math.sqrt(dx * dx + dy * dy)
          if d < core_r then
            local k = (1.0 - d / core_r) ^ 1.65
            if is_bright_rim and d > 2.2 then
              k = k * 0.25
            end
            local target = (d < 2.3) and rgba(255, 255, 255, 255)
                        or ((y < 21) and rgba(238, 204, 255, 255) or rgba(255, 236, 184, 255))
            l_petals:drawPixel(x, y, lerp_color(cur, target, k * (is_full and 0.80 or 0.68)))
          end
        end
      end
    end
  end

  local lower_guard_spans = {
    {11, 16}, {11, 19}, {12, 22}, {13, 24},
    {14, 25}, {16, 26}, {18, 26}, {21, 25},
  }
  draw_paired_petal(l_petals, 29, lower_guard_spans,
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
    {14, 15}, {14, 18}, {15, 20}, {15, 23}, {16, 25},
    {17, 27}, {18, 29}, {19, 30}, {20, 31}, {22, 31},
    {24, 31}, {26, 31}, {29, 31},
  }
  draw_paired_petal(l_petals, 23, front_cup_spans,
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
    local y = 34 + i - 1
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
      put_sym(l_petals, LOTUS_CX - dx, y, c)
    end
  end

  if not is_empty then
    local m_white  = rgba(248, 254, 255, 255)
    local m_bright = is_full and rgba(214, 248, 255, 255) or rgba(192, 242, 255, 255)
    local m_cyan   = is_full and rgba(124, 218, 255, 255) or rgba(102, 202, 248, 255)
    local m_azure  = rgba(56, 138, 222, 255)
    local m_shadow = rgba(20, 58, 122, 195)

    -- 1. Sleek 1-2px elliptical cloud loops faithfully matching Ref2 D's airy C-arcs
    for ang_step = 0, 719 do
      local a = (ang_step / 720.0) * 2.0 * math.pi
      local ca, sa = math.cos(a), math.sin(a)
      local px = 31.0 + 28.2 * ca
      local py = 31.8 + 4.2 * sa
      local adx = math.abs(px - 31.0)
      local out_dist = math.abs(ca)
      local flow_coord = (sa < 0) and (out_dist * 0.8) or (1.6 - out_dist * 0.8)
      local wave = 0.5 + 0.5 * math.sin(2.0 * math.pi * (flow_coord * 1.25 - phase))

      if sa < -0.04 then
        if adx >= 17.5 then
          local ix = math.floor(px + 0.5)
          local iy = math.floor(py + 0.5)
          local c_top = lerp_color(m_cyan, m_bright, wave * 0.85)
          put_pixel(l_rear, ix, iy,     with_alpha(c_top, 225))
          if adx >= 23.0 then
            put_pixel(l_rear, ix, iy + 1, with_alpha(m_azure, 165))
          end
        end
      else
        -- Open central gap around downward calyx (adx >= 7.2) with razor-sharp inward needle taper
        if adx >= 7.2 then
          local taper_in = smoothstep(7.2, 14.0, adx)
          local ix = math.floor(px + 0.5)
          local iy = math.floor(py + 0.5)
          local crest_c = lerp_color(m_bright, m_white, wave * (is_full and 0.96 or 0.82))
          local body_c  = lerp_color(m_azure, m_cyan, 0.45 + 0.55 * wave)
          local alpha_m = math.floor((is_full and 252 or 240) * taper_in)
          put_pixel(l_front, ix, iy, with_alpha(crest_c, alpha_m))
          if adx >= 11.0 then
            put_pixel(l_front, ix, iy + 1, with_alpha(body_c, math.floor(alpha_m * 0.82)))
          end
          if adx >= 23.5 then
            put_pixel(l_front, ix, iy - 1, with_alpha(m_white, math.floor(alpha_m * 0.68)))
          end
        end
      end
    end

    -- Delicate inner secondary curl inside the outer cloud loop (as seen in Ref2 D)
    for dx = 18, 22 do
      local wy = (dx >= 20) and 30 or 31
      put_sym(l_rear, LOTUS_CX - dx, wy, with_alpha(m_cyan, 185))
    end

    -- Lower calyx cloud-tail tendrils tucked gracefully under the mirror ring (y = 37..40)
    for dx = 7, 19 do
      local u = (dx - 7) / 12.0
      local wy = (dx <= 11) and 37 or ((dx <= 15) and 39 or 40)
      local pulse = 0.5 + 0.5 * math.sin(2.0 * math.pi * (dx / 12.0 - phase))
      local al = math.floor(210 * math.sin(u * math.pi) * (is_full and 1.08 or 0.94))
      local c_hi = lerp_color(m_cyan, m_bright, pulse)
      put_sym(l_front, LOTUS_CX - dx, wy,     with_alpha(c_hi, clamp(al, 0, 255)))
      put_sym(l_front, LOTUS_CX - dx, wy + 1, with_alpha(m_shadow, clamp(math.floor(al * 0.55), 0, 255)))
    end

    -- 2. True 16-Frame Outward-Drifting Slender Cloud-Mist Wisps (1-2px razor-tapered wisps!)
    local function draw_outward_wisp(target_layer, y_base, d_start, d_end, len_px, cyc_t, strength)
      local env = math.sin(math.pi * cyc_t)
      if env <= 0.04 then return end
      local center_d = lerp(d_start, d_end, cyc_t)
      local half_l = len_px * (0.72 + 0.28 * env) * 0.5
      local d0 = math.floor(center_d - half_l + 0.5)
      local d1 = math.floor(center_d + half_l + 0.5)
      for d = d0, d1 do
        local u = (d - d0) / math.max(1, d1 - d0)
        -- Brighter outward leading head (u ~ 0.65) with graceful trailing tail
        local prof = math.sin(math.pi * (u ^ 0.85))
        local wave_y = (u > 0.72) and -1 or 0
        local a_main = clamp(math.floor(248 * env * prof * strength), 0, 252)
        if a_main > 16 then
          local c_top = lerp_color(m_cyan, m_white, clamp(0.38 + 0.62 * prof * env, 0, 1))
          local lx = LOTUS_CX - d
          local rx = mir_x(lx)
          put_pixel(target_layer, lx, y_base + wave_y, with_alpha(c_top, a_main))
          put_pixel(target_layer, rx, y_base + wave_y, with_alpha(c_top, a_main))
          if prof > 0.46 then
            local a_sub = math.floor(a_main * 0.62)
            put_pixel(target_layer, lx, y_base + wave_y + 1, with_alpha(m_azure, a_sub))
            put_pixel(target_layer, rx, y_base + wave_y + 1, with_alpha(m_azure, a_sub))
          end
          if rx >= 61 and d >= 30 then
            put_pixel(target_layer, 63, y_base + wave_y, with_alpha(c_top, math.floor(a_main * 0.75)))
          end
        end
      end
    end

    local str_mult = is_full and 1.12 or 0.96
    -- Primary outward cloud-mist streams gliding from lotus base past outer cloud ring (d = 15 -> 31)
    draw_outward_wisp(l_front, 34, 15.0, 31.0, 11.0, (phase + 0.00) % 1.0, 1.00 * str_mult)
    draw_outward_wisp(l_front, 35, 15.0, 31.0, 10.5, (phase + 0.50) % 1.0, 0.94 * str_mult)
    -- Upper-side ethereal mist wisps drifting outward beneath wide wing tips (y = 24..25)
    draw_outward_wisp(l_rear,  24, 17.5, 30.5, 9.0,  (phase + 0.25) % 1.0, 0.85 * str_mult)
    draw_outward_wisp(l_rear,  25, 17.5, 30.5, 9.0,  (phase + 0.75) % 1.0, 0.80 * str_mult)
    -- Lower calyx mist threads drifting outward (y = 38..39)
    draw_outward_wisp(l_front, 38, 10.0, 26.5, 9.5,  (phase + 0.18) % 1.0, 0.88 * str_mult)
    draw_outward_wisp(l_front, 39, 10.0, 26.5, 9.0,  (phase + 0.68) % 1.0, 0.82 * str_mult)
  end

  -- Cushion wraps ONLY solid structural elements (ring, beads, dewdrops, wing tips), NEVER wrapping dark bubbles above airy mist wisps!
  local comp_mask = new_image(MANA_ICON_W, MANA_ICON_H)
  blit(comp_mask, l_ring,   0, 0)
  blit(comp_mask, l_petals, 0, 0)
  local cushion = build_soft_cushion(comp_mask, 8, 20, 44, 112, 50, 0, function(x, y)
    local dx, dy = x - r_cx, y - r_cy
    return (dx * dx + dy * dy) <= (r_out * r_out)
  end)
  local l_base = new_image(MANA_ICON_W, MANA_ICON_H)
  blit(l_base, cushion, 0, 0)
  blit(l_base, l_ring,  0, 0)

  local merged = new_image(MANA_ICON_W, MANA_ICON_H)
  blit(merged, l_base,   0, 0)
  blit(merged, l_rear,   0, 0)
  blit(merged, l_petals, 0, 0)
  blit(merged, l_front,  0, 0)

  return {
    base   = l_base,
    rear   = l_rear,
    petals = l_petals,
    front  = l_front,
    merged = merged,
  }
end

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

local function make_water_icon_layers(mode, frame_idx)
  local f0 = (frame_idx or 1) - 1
  local is_idle  = (mode == "idle")
  local is_surge = (mode == "surge")
  local speed    = is_idle and 0 or (is_surge and 2 or 1)
  local phase    = (f0 * speed / FRAMES) % 1.0
  local wave_ang = 2.0 * math.pi * phase

  local l_streams = new_image(WATER_ICON_W, WATER_ICON_H)
  local l_glints  = new_image(WATER_ICON_W, WATER_ICON_H)

  local c_navy   = is_idle and rgba(10, 34, 68, 255)  or rgba(9, 54, 128, 255)
  local c_cobalt = is_idle and rgba(14, 52, 98, 255)  or rgba(11, 78, 172, 255)
  local c_azure  = is_idle and rgba(20, 78, 132, 255) or (is_surge and rgba(22, 148, 238, 255) or rgba(18, 132, 224, 255))
  local c_cyan   = is_idle and rgba(32, 108, 162, 255) or (is_surge and rgba(38, 222, 255, 255) or rgba(26, 208, 255, 255))
  local c_ice    = is_idle and rgba(52, 136, 188, 255) or rgba(148, 246, 255, 255)
  local c_white  = is_idle and rgba(78, 158, 204, 255) or rgba(255, 255, 255, 255)

  for x = 2, 45 do
    local span = STREAM1_COLS[x]
    if span then
      local yt, yb = span[1], span[2]
      local xn = (x - 2) / 43.0
      local pulse = is_idle and 0.0 or (0.5 + 0.5 * math.sin(2.0 * math.pi * (xn * 1.35) - wave_ang))
      for y = yt, yb do
        local yn = (yb > yt) and ((y - yt) / (yb - yt)) or 0.5
        local flow_t = clamp(xn * 1.65 + (1.0 - yn) * 0.18 - 0.04, 0.0, 1.0)
        local c
        if flow_t < 0.42 then
          c = lerp_color(c_navy, c_azure, smoothstep(0.0, 0.42, flow_t))
        else
          c = lerp_color(c_azure, c_cyan, smoothstep(0.42, 0.92, flow_t))
        end
        -- Crisp Ref3 #8 layered inner cobalt shadow stroke along left arch
        if x >= 6 and x <= 18 and y == yt + 1 then
          c = lerp_color(c_cobalt, c, smoothstep(11.0, 19.0, x))
        elseif x >= 9 and x <= 18 and y == yt then
          c = lerp_color(c, c_cyan, 0.52)
        end
        put_pixel(l_streams, x, y, c)

        if not is_idle and x >= 26 then
          local belly_start = is_surge and 24 or 27
          if x >= belly_start then
            local bx = (x - belly_start) / (45 - belly_start)
            local dist_bot = yb - y
            local w_strength = smoothstep(0.0, 0.35, bx) * (0.82 + 0.18 * pulse)
            if dist_bot == 0 and x <= 44 then
              put_pixel(l_glints, x, y, with_alpha(c_white, math.floor(255 * w_strength)))
            elseif dist_bot == 1 and x >= 29 and x <= 43 then
              put_pixel(l_glints, x, y, with_alpha(is_surge and c_white or c_ice, math.floor(235 * w_strength)))
            elseif x >= 44 then
              put_pixel(l_glints, x, y, c_ice)
            end
          end
        end

        if not is_idle and xn >= 0.30 and xn <= 0.75 and math.abs(yn - 0.55) <= 0.28 and pulse > 0.72 then
          local ga = math.floor((pulse - 0.72) / 0.28 * (is_surge and 210 or 145))
          put_pixel(l_glints, x, y, with_alpha(c_ice, ga))
        end
      end
    end
  end

  for x = 2, 44 do
    local span = STREAM2_COLS[x]
    if span then
      local yt, yb = span[1], span[2]
      local xn = (x - 2) / 42.0
      local pulse = is_idle and 0.0 or (0.5 + 0.5 * math.sin(2.0 * math.pi * (xn * 1.35 - 0.18) - wave_ang))
      for y = yt, yb do
        local yn = (yb > yt) and ((y - yt) / (yb - yt)) or 0.5
        local flow_t = clamp(xn * 1.60 + (1.0 - yn) * 0.15 - 0.04, 0.0, 1.0)
        local c
        if flow_t < 0.44 then
          c = lerp_color(c_cobalt, c_azure, smoothstep(0.0, 0.44, flow_t))
        else
          c = lerp_color(c_azure, c_cyan, smoothstep(0.44, 0.90, flow_t))
        end
        -- Ref3 #8 upper cyan arch crest + dark navy fold groove on left fork
        if x >= 10 and x <= 18 and y <= yt + 1 then
          c = lerp_color(c, c_cyan, 0.78)
        elseif x >= 9 and x <= 17 and y == yt + 2 then
          c = lerp_color(c_navy, c, smoothstep(13.0, 18.0, x))
        end
        put_pixel(l_streams, x, y, c)

        if not is_idle and x >= 19 and x <= 43 then
          local spine_y = (x <= 33)
            and lerp(18.2, 24.6, (x - 19) / 14.0)
            or  lerp(24.6, 21.3, (x - 33) / 10.0)
          local dy_s = math.abs(y - spine_y)
          local boost = (is_surge and 0.22 or 0.0) + 0.16 * pulse
          if dy_s <= (0.68 + boost) then
            put_pixel(l_glints, x, y, c_white)
          elseif dy_s <= (1.35 + boost) then
            put_pixel(l_glints, x, y, with_alpha(c_ice, is_surge and 235 or 190))
          end
        end
      end
    end
  end

  for x = 4, 36 do
    local span = STREAM3_COLS[x]
    if span then
      local yt, yb = span[1], span[2]
      local xn = (x - 4) / 32.0
      local pulse = is_idle and 0.0 or (0.5 + 0.5 * math.sin(2.0 * math.pi * (xn * 1.30 - 0.35) - wave_ang))
      for y = yt, yb do
        local flow_t = clamp(xn * 1.75, 0.0, 1.0)
        local c = lerp_color(c_cobalt, c_cyan, smoothstep(0.0, 0.72, flow_t))
        if x >= 11 and x <= 19 and y <= yt + 1 then
          c = is_idle and c_cyan or lerp_color(c, c_ice, 0.48 * (1.0 - math.abs(x - 15) / 5.0))
        elseif x >= 10 and x <= 18 and y == yt + 2 then
          c = lerp_color(c_azure, c, smoothstep(14.0, 19.0, x))
        end
        put_pixel(l_streams, x, y, c)

        if not is_idle then
          if x >= 20 and x <= 26 and y <= yt + 1 then
            local g_str = (x >= 21 and x <= 24 and y == yt) and 1.0 or 0.65
            put_pixel(l_glints, x, y, with_alpha((g_str > 0.8 or is_surge) and c_white or c_ice, math.floor((225 + 30 * pulse) * g_str)))
          elseif x >= 27 and x <= 35 and y == yt then
            local ga = is_surge and 230 or math.floor(150 + 90 * pulse)
            put_pixel(l_glints, x, y, with_alpha(c_ice, ga))
          end
        end
      end
    end
  end

  if not is_idle then
    local drift1 = (phase >= 0.35 and phase < 0.85) and 1 or 0
    local drift2 = (((phase + 0.5) % 1.0) >= 0.35 and ((phase + 0.5) % 1.0) < 0.85) and 1 or 0
    local sp_hi  = is_surge and c_white or c_ice
    local sp_cyn = c_cyan
    local sp_az  = c_azure

    put_pixel(l_glints, 23 + drift1, 4, sp_az)
    put_pixel(l_glints, 24 + drift1, 4, sp_cyn)
    put_pixel(l_glints, 25 + drift1, 4, sp_hi)

    put_pixel(l_glints, 28 + drift2, 5, sp_cyn)
    put_pixel(l_glints, 29 + drift2, 5, sp_cyn)
    put_pixel(l_glints, 29 + drift2, 6, sp_hi)
    put_pixel(l_glints, 30 + drift2, 6, sp_hi)
    put_pixel(l_glints, 31 + drift2, 6, sp_cyn)
    put_pixel(l_glints, 30 + drift2, 7, sp_cyn)
    put_pixel(l_glints, 31 + drift2, 7, sp_hi)
    put_pixel(l_glints, 32 + drift2, 7, sp_cyn)
    put_pixel(l_glints, 32 + drift2, 8, sp_az)

    put_pixel(l_glints, 35 + drift1, 10, sp_cyn)
    put_pixel(l_glints, 36 + drift1, 10, sp_hi)
    put_pixel(l_glints, 35 + drift1, 11, sp_az)

    put_pixel(l_glints, 7 + drift2, 14, sp_cyn)
    put_pixel(l_glints, 8 + drift2, 14, sp_hi)
    put_pixel(l_glints, 6 + drift2, 15, sp_cyn)
    put_pixel(l_glints, 7 + drift2, 15, sp_az)

    put_pixel(l_glints, 7, 26, sp_cyn)

    put_pixel(l_glints, 11 + drift1, 34, sp_cyn)
    put_pixel(l_glints, 12 + drift1, 34, sp_hi)
    put_pixel(l_glints, 10 + drift1, 35, sp_cyn)
    put_pixel(l_glints, 11 + drift1, 35, sp_az)
    put_pixel(l_glints, 14 + drift2, 34, sp_cyn)
  end

  local union_mask = new_image(WATER_ICON_W, WATER_ICON_H)
  blit(union_mask, l_streams, 0, 0)
  blit(union_mask, l_glints,  0, 0)
  local l_cushion = build_soft_cushion(union_mask, 7, 24, 50, 128, 56, 0, nil)

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

local CACHE_WATER_ICON_LAYERS = { idle = {}, stream = {}, surge = {} }
for f = 1, FRAMES do
  CACHE_WATER_ICON_LAYERS.idle[f]   = (f == 1) and make_water_icon_layers("idle", 1) or CACHE_WATER_ICON_LAYERS.idle[1]
  CACHE_WATER_ICON_LAYERS.stream[f] = make_water_icon_layers("stream", f)
  CACHE_WATER_ICON_LAYERS.surge[f]  = make_water_icon_layers("surge", f)
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
  local icon_mode = (px == 0) and "idle" or ((val >= 50) and "surge" or "stream")

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

  local icon_layers = CACHE_WATER_ICON_LAYERS[icon_mode][frame_idx]
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
local water_icon_stream_1 = CACHE_WATER_ICON_LAYERS.stream[1].merged
local water_icon_surge_1  = CACHE_WATER_ICON_LAYERS.surge[1].merged
local water_icon_stream_strip = make_vertical_strip(WATER_ICON_W, WATER_ICON_H, function(f) return CACHE_WATER_ICON_LAYERS.stream[f].merged end)
local water_icon_surge_strip  = make_vertical_strip(WATER_ICON_W, WATER_ICON_H, function(f) return CACHE_WATER_ICON_LAYERS.surge[f].merged end)

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

-- Save clean transparent-alpha exports
mana_icon_empty_1:saveAs(exp_dir .. "/mana_icon_empty.png")
mana_icon_norm_1:saveAs(exp_dir .. "/mana_icon.png")
mana_icon_strip:saveAs(exp_dir .. "/mana_icon_strip.png")
mana_icon_full_1:saveAs(exp_dir .. "/mana_icon_full.png")
mana_icon_full_strip:saveAs(exp_dir .. "/mana_icon_full_strip.png")

water_icon_idle_1:saveAs(exp_dir .. "/water_icon_idle.png")
water_icon_stream_1:saveAs(exp_dir .. "/water_icon.png")
water_icon_stream_strip:saveAs(exp_dir .. "/water_icon_stream_strip.png")
water_icon_surge_1:saveAs(exp_dir .. "/water_icon_surge.png")
water_icon_surge_strip:saveAs(exp_dir .. "/water_icon_surge_strip.png")

mana_track_img:saveAs(exp_dir .. "/mana_track.png")
mana_fill_low:saveAs(exp_dir .. "/mana_fill_low.png")
mana_fill_strip:saveAs(exp_dir .. "/mana_fill_strip.png")
mana_fill_full_strip:saveAs(exp_dir .. "/mana_fill_full_strip.png")

water_track_0:saveAs(exp_dir .. "/water_track.png")
water_track_states:saveAs(exp_dir .. "/water_track_states.png")
water_fill_t1_strip:saveAs(exp_dir .. "/water_fill_tier1_trickle_strip.png")
water_fill_t2_strip:saveAs(exp_dir .. "/water_fill_tier2_stream_strip.png")
water_fill_t3_strip:saveAs(exp_dir .. "/water_fill_tier3_torrent_strip.png")

-- Save dedicated multi-layer 16-frame editable .aseprite for Mana Lotus Icon (transparent canvas!)
do
  local spr = Sprite(MANA_ICON_W, MANA_ICON_H, ColorMode.RGB)
  spr.frames[1].duration = FRAME_DURATION
  for f = 2, FRAMES do
    local nf = spr:newEmptyFrame()
    nf.duration = FRAME_DURATION
  end
  local l1 = spr.layers[1]; l1.name = "mirror_ring_and_pendants"
  local l2 = spr:newLayer(); l2.name = "rear_mist_loops"
  local l3 = spr:newLayer(); l3.name = "azure_lotus_petals_and_core"
  local l4 = spr:newLayer(); l4.name = "front_outward_cloud_mist"
  for f = 1, FRAMES do
    local ly = CACHE_MANA_ICON_LAYERS.normal[f]
    spr:newCel(l1, f, ly.base,   Point(0, 0))
    spr:newCel(l2, f, ly.rear,   Point(0, 0))
    spr:newCel(l3, f, ly.petals, Point(0, 0))
    spr:newCel(l4, f, ly.front,  Point(0, 0))
  end
  spr:saveAs(src_dir .. "/mana_lotus_icon_v5.aseprite")
  spr:close()
end

-- Save dedicated multi-layer 16-frame editable .aseprite for Water #8 Icon (transparent canvas!)
do
  local spr = Sprite(WATER_ICON_W, WATER_ICON_H, ColorMode.RGB)
  spr.frames[1].duration = FRAME_DURATION
  for f = 2, FRAMES do
    local nf = spr:newEmptyFrame()
    nf.duration = FRAME_DURATION
  end
  local l1 = spr.layers[1]; l1.name = "soft_deep_sea_cushion"
  local l2 = spr:newLayer(); l2.name = "three_s_stream_bodies"
  local l3 = spr:newLayer(); l3.name = "white_katana_bellies_and_spray"
  for f = 1, FRAMES do
    local ly = CACHE_WATER_ICON_LAYERS.stream[f]
    spr:newCel(l1, f, ly.cushion, Point(0, 0))
    spr:newCel(l2, f, ly.streams, Point(0, 0))
    spr:newCel(l3, f, ly.glints,  Point(0, 0))
  end
  spr:saveAs(src_dir .. "/water_stream8_icon_v5.aseprite")
  spr:close()
end

-- Master multi-layer 16-frame editable .aseprite + components_overview_sheet.png
do
  local mw, mh = 560, 260
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
  draw_label_scaled(bg_img, 12, 8, 2, "V5 NATIVE 2x COMPONENTS (64x56 LOTUS | 48x40 WATER #8 | 204x20 TRACK)", rgba(226, 236, 252, 255), rgba(6, 8, 14, 255))

  local trk_img = new_image(mw, mh)
  -- Mana tracks
  blit(trk_img, mana_track_img, 240, 32)
  blit(trk_img, mana_track_img, 240, 62)
  blit(trk_img, mana_track_img, 240, 92)
  -- Water tracks (0, >=20, >=50)
  blit(trk_img, water_track_0,  240, 142)
  blit(trk_img, water_track_20, 240, 172)
  blit(trk_img, water_track_50, 240, 202)

  for f = 1, FRAMES do
    master:newCel(l_bg,     f, bg_img,  Point(0, 0))
    master:newCel(l_tracks, f, trk_img, Point(0, 0))

    local ic_img = new_image(mw, mh)
    blit(ic_img, mana_icon_empty_1,                           12,  32)
    blit(ic_img, CACHE_MANA_ICON_LAYERS.normal[f].merged,     84,  32)
    blit(ic_img, CACHE_MANA_ICON_LAYERS.full[f].merged,       156, 32)

    blit(ic_img, water_icon_idle_1,                           18,  148)
    blit(ic_img, CACHE_WATER_ICON_LAYERS.stream[f].merged,    90,  148)
    blit(ic_img, CACHE_WATER_ICON_LAYERS.surge[f].merged,     162, 148)
    master:newCel(l_icons, f, ic_img, Point(0, 0))

    local fl_img = new_image(mw, mh)
    blit(fl_img, mana_fill_low,               240 + FILL_X_OFFSET, 32 + FILL_Y_OFFSET)
    blit(fl_img, CACHE_MANA_FILL.normal[f],   240 + FILL_X_OFFSET, 62 + FILL_Y_OFFSET)
    blit(fl_img, CACHE_MANA_FILL.full[f],     240 + FILL_X_OFFSET, 92 + FILL_Y_OFFSET)

    blit(fl_img, CACHE_WATER_FILL[1][f], 240 + FILL_X_OFFSET, 142 + FILL_Y_OFFSET)
    blit(fl_img, CACHE_WATER_FILL[2][f], 240 + FILL_X_OFFSET, 172 + FILL_Y_OFFSET)
    blit(fl_img, CACHE_WATER_FILL[3][f], 240 + FILL_X_OFFSET, 202 + FILL_Y_OFFSET)
    master:newCel(l_fills, f, fl_img, Point(0, 0))
  end

  master:saveAs(src_dir .. "/hud_bars_v5_components.aseprite")

  local ov = new_image(mw, mh)
  blit(ov, bg_img,  0, 0)
  blit(ov, trk_img, 0, 0)
  blit(ov, master.layers[3]:cel(1).image, 0, 0)
  blit(ov, master.layers[4]:cel(1).image, 0, 0)
  draw_text(ov, 18,  94, "EMPTY",  rgba(164, 182, 210, 255), nil)
  draw_text(ov, 90,  94, "MIST16", rgba(196, 224, 255, 255), nil)
  draw_text(ov, 166, 94, "FULL16", rgba(255, 244, 206, 255), nil)
  draw_text(ov, 24,  196, "IDLE",   rgba(164, 182, 210, 255), nil)
  draw_text(ov, 92,  196, "STRM16", rgba(164, 242, 255, 255), nil)
  draw_text(ov, 164, 196, "SURG16", rgba(232, 255, 255, 255), nil)
  draw_text(ov, 452, 39,  "MANA LOW",   rgba(164, 186, 222, 255), nil)
  draw_text(ov, 452, 69,  "MANA CLOUD", rgba(204, 228, 255, 255), nil)
  draw_text(ov, 452, 99,  "MANA FULL",  rgba(246, 250, 255, 255), nil)
  draw_text(ov, 452, 149, "WATER T1",   rgba(134, 206, 220, 255), nil)
  draw_text(ov, 452, 179, "WATER T2",   rgba(164, 238, 248, 255), nil)
  draw_text(ov, 452, 209, "WATER T3",   rgba(226, 255, 255, 255), nil)
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
  render_mana_bar_2x(dst, ox + 10, oy + 4, mana_val, mana_max, frame_idx, nil)
  draw_label_scaled(dst, ox + 274, oy + 22, 2, string.format("%d/%d", mana_val, mana_max), PAL_MANA.text, PAL_MANA.text_shadow)

  render_water_bar_2x(dst, ox + 10, oy + 62, water_val, frame_idx)
  draw_label_scaled(dst, ox + 274, oy + 76, 2, string.format("%d/100", water_val), PAL_WATER.text, PAL_WATER.text_shadow)

  if show_change then
    draw_label_scaled(dst, ox + 80, oy + 98, 2, "+12", PAL_WATER.gain_text, PAL_WATER.text_shadow)
  end
end

-- ============================================================================
-- PREVIEW 1: ref_vs_v5_gate.png (1:1 STRICT REFERENCE VS V5 SIDE-BY-SIDE GATE)
-- ============================================================================
do
  local ref_lotus = Image{fromFile = ref_dir .. "/crop_ref_D_lotus.png"}
  local ref_water = Image{fromFile = ref_dir .. "/crop_ref_8_water.png"}

  local cw, ch = 1660, 1060
  local gate = new_image(cw, ch, rgba(13, 16, 22, 255))

  fill_rect(gate, 0, 0, cw, 54, rgba(20, 25, 36, 255))
  stroke_rect(gate, 0, 0, cw, 54, rgba(46, 58, 82, 255))
  draw_label_scaled(gate, 24, 16, 3, "V5 STRICT 1:1 VISUAL GATE: ORIGINAL REF CROPS VS V5 PIXEL ART (DARK + DAY SKY + HUD SCALE)", rgba(232, 240, 255, 255), rgba(8, 10, 16, 255))

  -- Column layout:
  -- Col 1 (x=24,  w=516): ORIGINAL REFERENCE CROPS
  -- Col 2 (x=560, w=536): V5 ON MATCHED DARK BACKING (5x / 6x + CONNECTED BRIDGE)
  -- Col 3 (x=1116,w=520): V5 ON DAYTIME SKY BACKING + GUI SCALE 2 & 3 HUD CHECK
  local cols = {
    {x = 24,   w = 516, title = "1. ORIGINAL REFERENCE CROP",        border = rgba(86, 102, 134, 255)},
    {x = 560,  w = 536, title = "2. V5 PIXEL ART (DARK BACKING)",    border = rgba(76, 122, 198, 255)},
    {x = 1116, w = 520, title = "3. V5 DAYTIME SKY + HUD 0.5x SCALE", border = rgba(58, 148, 176, 255)},
  }

  -- Top Row (y = 70, h = 468): MANA — REF2 D LOTUS + OUTWARD CLOUD MIST + MEANDER BRIDGE
  -- Bot Row (y = 556, h = 480): WATER — REF3 #8 THREE S-STREAM BLADES
  for _, col in ipairs(cols) do
    fill_rect(gate, col.x, 70, col.w, 468, rgba(18, 22, 31, 255))
    stroke_rect(gate, col.x, 70, col.w, 468, col.border)
    draw_label_scaled(gate, col.x + 16, 84, 2, col.title, rgba(226, 236, 252, 255), rgba(8, 10, 16, 255))

    fill_rect(gate, col.x, 556, col.w, 480, rgba(18, 22, 31, 255))
    stroke_rect(gate, col.x, 556, col.w, 480, col.border)
    draw_label_scaled(gate, col.x + 16, 570, 2, col.title, rgba(226, 236, 252, 255), rgba(8, 10, 16, 255))
  end

  -- Row 1 Col 1: Original crop_ref_D_lotus.png (426x257) placed 1:1 on its dark slate card
  fill_rect(gate, 44, 112, 476, 404, rgba(23, 27, 36, 255))
  stroke_rect(gate, 44, 112, 476, 404, rgba(46, 56, 74, 255))
  blit(gate, ref_lotus, 44 + math.floor((476 - ref_lotus.width) / 2), 136)
  draw_label_scaled(gate, 60, 484, 2, "REF2 D: BLOOMING LOTUS + CLOUD MIST + FRET BRIDGE", rgba(176, 196, 226, 255), nil)

  -- Row 1 Col 2: V5 Lotus + Connected Fretwork Track at 4x (matches ref_lotus pixel grid scale ~4.25x!) + 5x solo Lotus
  fill_rect(gate, 580, 112, 496, 404, rgba(23, 27, 36, 255))
  stroke_rect(gate, 580, 112, 496, 404, rgba(46, 56, 74, 255))
  do
    local connected_crop = new_image(112, 56)
    render_mana_bar_2x(connected_crop, 4, 0, 64, 80, 3, nil)
    local up4 = scale_nn(connected_crop, 4) -- 448x224, exact 1:1 visual scale with crop_ref_D_lotus.png!
    blit(gate, up4, 580 + math.floor((496 - up4.width) / 2), 148)

    -- Also show Normal vs Full vs Empty at 2x at bottom of Col 2
    blit(gate, scale_nn(mana_icon_empty_1, 2), 604, 392)
    blit(gate, scale_nn(CACHE_MANA_ICON_LAYERS.normal[3].merged, 2), 762, 392)
    blit(gate, scale_nn(CACHE_MANA_ICON_LAYERS.full[3].merged, 2),   920, 392)
    draw_label_scaled(gate, 628, 376, 2, "EMPTY",  rgba(154, 172, 200, 255), nil)
    draw_label_scaled(gate, 782, 376, 2, "NORMAL", rgba(196, 224, 255, 255), nil)
    draw_label_scaled(gate, 948, 376, 2, "FULL",   rgba(255, 244, 204, 255), nil)
  end

  -- Row 1 Col 3: V5 Lotus + Connected Bridge on Bright Daytime Sky + GUI Scale 2 (1x native) & GUI Scale 3 (1.5x)
  paint_daytime_sky_swatch(gate, 1136, 112, 480, 272)
  stroke_rect(gate, 1136, 112, 480, 272, rgba(64, 88, 116, 255))
  do
    local connected_sky = new_image(112, 56)
    render_mana_bar_2x(connected_sky, 4, 0, 64, 80, 3, nil)
    blit(gate, scale_nn(connected_sky, 4), 1136 + math.floor((480 - 448) / 2), 138)

    -- True HUD Scale strip below
    fill_rect(gate, 1136, 396, 234, 120, rgba(20, 24, 34, 255))
    stroke_rect(gate, 1136, 396, 234, 120, rgba(48, 60, 82, 255))
    draw_label_scaled(gate, 1148, 406, 2, "GUI SCALE 2 (0.5x=1:1)", rgba(196, 214, 240, 255), nil)
    local mini_bar = new_image(260, 56)
    render_mana_bar_2x(mini_bar, 0, 0, 58, 80, 3, nil)
    blit(gate, mini_bar, 1144, 434, 218, 56, 0, 0)

    paint_daytime_sky_swatch(gate, 1382, 396, 234, 120)
    stroke_rect(gate, 1382, 396, 234, 120, rgba(64, 88, 116, 255))
    draw_label_scaled(gate, 1394, 406, 2, "DAY SKY 1:1 NATIVE", rgba(244, 250, 255, 255), rgba(16, 26, 44, 255))
    blit(gate, mini_bar, 1390, 434, 218, 56, 0, 0)
  end

  -- Row 2 Col 1: Original crop_ref_8_water.png (302x373) placed 1:1
  fill_rect(gate, 44, 598, 476, 418, rgba(11, 14, 25, 255))
  stroke_rect(gate, 44, 598, 476, 418, rgba(46, 56, 74, 255))
  blit(gate, ref_water, 44 + math.floor((476 - ref_water.width) / 2), 614)
  draw_label_scaled(gate, 72, 992, 2, "REF3 #8: THREE S-STREAM KATANA WATER BLADES", rgba(164, 214, 236, 255), nil)

  -- Row 2 Col 2: V5 Water #8 Icon at 6x (288x240, exact 1:1 visual scale with crop_ref_8_water.png!) + Idle/Stream/Surge @ 2x
  fill_rect(gate, 580, 598, 496, 418, rgba(11, 14, 25, 255))
  stroke_rect(gate, 580, 598, 496, 418, rgba(46, 56, 74, 255))
  do
    local w_up6 = scale_nn(water_icon_stream_1, 6) -- 288x240, exact match to ref_water's 288px inner box!
    local bx = 580 + math.floor((496 - w_up6.width) / 2)
    local by = 618
    stroke_rect(gate, bx - 6, by - 6, w_up6.width + 12, w_up6.height + 12, rgba(72, 74, 112, 255))
    blit(gate, w_up6, bx, by)

    blit(gate, scale_nn(water_icon_idle_1,   2), 624, 908)
    blit(gate, scale_nn(water_icon_stream_1, 2), 780, 908)
    blit(gate, scale_nn(water_icon_surge_1,  2), 936, 908)
    draw_label_scaled(gate, 642, 886, 2, "IDLE (0)",    rgba(148, 172, 200, 255), nil)
    draw_label_scaled(gate, 780, 886, 2, "STREAM 1x",   rgba(164, 238, 255, 255), nil)
    draw_label_scaled(gate, 940, 886, 2, "SURGE 2x",    rgba(232, 255, 255, 255), nil)
  end

  -- Row 2 Col 3: V5 Water #8 on Bright Daytime Sky (6x) + HUD 0.5x Scale Check (Soft Deep-Sea Cushion, ZERO black line!)
  paint_daytime_sky_swatch(gate, 1136, 598, 480, 282)
  stroke_rect(gate, 1136, 598, 480, 282, rgba(64, 88, 116, 255))
  do
    local w_up6 = scale_nn(water_icon_stream_1, 6)
    blit(gate, w_up6, 1136 + math.floor((480 - w_up6.width) / 2), 618)

    fill_rect(gate, 1136, 892, 234, 124, rgba(16, 20, 30, 255))
    stroke_rect(gate, 1136, 892, 234, 124, rgba(48, 60, 82, 255))
    draw_label_scaled(gate, 1148, 902, 2, "GUI SCALE 2 (0.5x=1:1)", rgba(196, 214, 240, 255), nil)
    local mini_wbar = new_image(260, 44)
    render_water_bar_2x(mini_wbar, 0, 0, 68, 1)
    blit(gate, mini_wbar, 1144, 938, 218, 44, 0, 0)

    paint_daytime_sky_swatch(gate, 1382, 892, 234, 124)
    stroke_rect(gate, 1382, 892, 234, 124, rgba(64, 88, 116, 255))
    draw_label_scaled(gate, 1394, 902, 2, "DAY SKY 1:1 NATIVE", rgba(244, 250, 255, 255), rgba(16, 26, 44, 255))
    blit(gate, mini_wbar, 1390, 938, 218, 44, 0, 0)
  end

  gate:saveAs(prv_dir .. "/ref_vs_v5_gate.png")
end

-- ============================================================================
-- PREVIEW 2: hud_context_day_night_4x.png
-- ============================================================================
do
  -- Each simulated HUD viewport at 2x native resolution is 504x248 (clean room for HUD text + corner tag)
  local vp_w, vp_h = 504, 248
  local top_w = vp_w * 2 + 36
  local top_h = 42 + vp_h * 2 + 32
  local ctx_2x = new_image(top_w, top_h, rgba(14, 17, 24, 255))

  fill_rect(ctx_2x, 0, 0, top_w, 32, rgba(22, 27, 38, 255))
  draw_label_scaled(ctx_2x, 14, 9, 2, "V5 HUD CONTEXT SIMULATION: LOTUS OUTWARD MIST (MANA) + REF #8 THREE S-STREAMS (WATER)", rgba(228, 238, 252, 255), rgba(8, 10, 16, 255))

  local x_left  = 12
  local x_right = vp_w + 24
  local y_day   = 42
  local y_cave  = 42 + vp_h + 16

  -- Daytime Sky + Grass: Left (Normal Stream 54/80 & 35/100) | Right (Full Lotus & Torrent Surge 80/80 & 72/100)
  paint_daytime_viewport(ctx_2x, x_left, y_day, vp_w, vp_h)
  draw_vanilla_hud_ref_2x(ctx_2x, x_left, y_day, vp_w, vp_h)
  render_hud_cluster_2x(ctx_2x, x_left, y_day, 54, 80, 35, 3, true)
  stroke_rect(ctx_2x, x_left, y_day, vp_w, vp_h, rgba(58, 78, 104, 255))

  paint_daytime_viewport(ctx_2x, x_right, y_day, vp_w, vp_h)
  draw_vanilla_hud_ref_2x(ctx_2x, x_right, y_day, vp_w, vp_h)
  render_hud_cluster_2x(ctx_2x, x_right, y_day, 80, 80, 72, 3, false)
  stroke_rect(ctx_2x, x_right, y_day, vp_w, vp_h, rgba(58, 78, 104, 255))

  -- Dark Cave: Left (Full Lotus & Surge 80/80 & 68/100) | Right (Low Mana & Trickle Water 14/80 & 12/100)
  paint_cave_viewport(ctx_2x, x_left, y_cave, vp_w, vp_h)
  draw_vanilla_hud_ref_2x(ctx_2x, x_left, y_cave, vp_w, vp_h)
  render_hud_cluster_2x(ctx_2x, x_left, y_cave, 80, 80, 68, 7, true)
  stroke_rect(ctx_2x, x_left, y_cave, vp_w, vp_h, rgba(48, 58, 78, 255))

  paint_cave_viewport(ctx_2x, x_right, y_cave, vp_w, vp_h)
  draw_vanilla_hud_ref_2x(ctx_2x, x_right, y_cave, vp_w, vp_h)
  render_hud_cluster_2x(ctx_2x, x_right, y_cave, 14, 80, 12, 7, false)
  stroke_rect(ctx_2x, x_right, y_cave, vp_w, vp_h, rgba(48, 58, 78, 255))

  -- Capture clean HUD crops BEFORE stamping viewport corner labels
  local crop_day  = new_image(354, 120); blit(crop_day,  ctx_2x, 0, 0, 354, 120, x_left + 2, y_day + 2)
  local crop_cave = new_image(354, 120); blit(crop_cave, ctx_2x, 0, 0, 354, 120, x_left + 2, y_cave + 2)

  -- Place viewport badge at top-right with zero overlap with HUD numbers (x <= 346 vs badge x = 352)!
  local function tag_vp(vx, vy, label)
    fill_rect(ctx_2x, vx + vp_w - 144, vy + 8, 136, 20, rgba(12, 16, 24, 195))
    draw_label_scaled(ctx_2x, vx + vp_w - 138, vy + 11, 2, label, rgba(226, 236, 252, 255), rgba(6, 8, 14, 255))
  end
  tag_vp(x_left,  y_day,  "DAY | STREAM")
  tag_vp(x_right, y_day,  "DAY | SURGE")
  tag_vp(x_left,  y_cave, "CAVE | SURGE")
  tag_vp(x_right, y_cave, "CAVE | LOW")

  local spr_ctx = Sprite(ctx_2x.width, ctx_2x.height, ColorMode.RGB)
  spr_ctx.layers[1].name = "hud_context_2x_native"
  spr_ctx.cels[1].image = ctx_2x
  spr_ctx:saveAs(src_dir .. "/hud_context_day_night.aseprite")
  spr_ctx:close()

  -- Upscale 2x -> 4x logical GUI scale, plus bottom strip showing REAL GUI Scale 2 (0.5x = 1:1 native) & GUI Scale 3 (0.5x = 1.5x native)
  local main_4x = scale_nn(ctx_2x, 2) -- 2024x1124
  local out_h = main_4x.height + 256
  local out = new_image(main_4x.width, out_h, rgba(12, 15, 21, 255))
  blit(out, main_4x, 0, 0)

  local by = main_4x.height + 8
  fill_rect(out, 24, by, main_4x.width - 48, 232, rgba(18, 22, 31, 255))
  stroke_rect(out, 24, by, main_4x.width - 48, 232, rgba(48, 60, 82, 255))
  draw_label_scaled(out, 44, by + 12, 3, "REAL IN-GAME 0.5x POSE-SCALED RENDER INSETS: GUI SCALE 2 (1:1 NATIVE PIXELS) & GUI SCALE 3 (1.5x)", rgba(224, 236, 252, 255), rgba(8, 10, 16, 255))

  -- GUI Scale 2 (2 screen px per logical GUI px * 0.5F pose scale = exact 1.0x native pixel!)
  draw_label_scaled(out, 48,  by + 48, 2, "GUI SCALE 2 DAY (1:1 NATIVE):",  rgba(186, 210, 240, 255), nil)
  blit(out, crop_day, 48, by + 72)
  stroke_rect(out, 48, by + 72, crop_day.width, crop_day.height, rgba(64, 84, 110, 255))

  draw_label_scaled(out, 432, by + 48, 2, "GUI SCALE 2 CAVE (1:1 NATIVE):", rgba(186, 210, 240, 255), nil)
  blit(out, crop_cave, 432, by + 72)
  stroke_rect(out, 432, by + 72, crop_cave.width, crop_cave.height, rgba(52, 64, 86, 255))

  -- GUI Scale 3 (3 screen px per logical GUI px * 0.5F pose scale = 1.5x nearest-neighbor)
  local day_gs3  = scale_nn_ratio(crop_day,  3, 2) -- 528x171
  local cave_gs3 = scale_nn_ratio(crop_cave, 3, 2)
  draw_label_scaled(out, 836,  by + 38, 2, "GUI SCALE 3 DAY (3x*0.5F = 1.5x):",  rgba(196, 224, 252, 255), nil)
  blit(out, day_gs3, 836, by + 56)
  stroke_rect(out, 836, by + 56, day_gs3.width, day_gs3.height, rgba(64, 84, 110, 255))

  draw_label_scaled(out, 1404, by + 38, 2, "GUI SCALE 3 CAVE (3x*0.5F = 1.5x):", rgba(196, 224, 252, 255), nil)
  blit(out, cave_gs3, 1404, by + 56)
  stroke_rect(out, 1404, by + 56, cave_gs3.width, cave_gs3.height, rgba(52, 64, 86, 255))

  out:saveAs(prv_dir .. "/hud_context_day_night_4x.png")
end

-- ============================================================================
-- PREVIEW 3: states_progression_4x.png (5 MANA STATES + 5 WATER STATES, DARK & DAY)
-- ============================================================================
do
  -- Build at 2x native resolution (872x612), then scale_nn(2) -> 1744x1224 (4x HUD logical scale, zero vertical touching!)
  local bw, bh = 872, 612
  local prog = new_image(bw, bh, rgba(14, 17, 24, 255))

  fill_rect(prog, 0, 0, bw, 28, rgba(22, 27, 38, 255))
  draw_label_scaled(prog, 14, 8, 2, "V5 STATES PROGRESSION (4x LOGICAL HUD SCALE): DARK CAVE (LEFT) VS DAYTIME SKY (RIGHT)", rgba(232, 240, 255, 255), rgba(8, 10, 16, 255))

  fill_rect(prog, 12, 36, 418, 304, rgba(18, 21, 29, 255))
  stroke_rect(prog, 12, 36, 418, 304, rgba(52, 74, 118, 255))
  paint_daytime_sky_swatch(prog, 442, 36, 418, 304)
  stroke_rect(prog, 442, 36, 418, 304, rgba(68, 96, 128, 255))

  fill_rect(prog, 12, 352, 418, 248, rgba(16, 21, 28, 255))
  stroke_rect(prog, 12, 352, 418, 248, rgba(44, 104, 124, 255))
  paint_daytime_sky_swatch(prog, 442, 352, 418, 248)
  stroke_rect(prog, 442, 352, 418, 248, rgba(68, 96, 128, 255))

  local mana_states = {
    {val = 0,  max = 80, ghost = nil, label = "0/80 EMPTY"},
    {val = 14, max = 80, ghost = nil, label = "14/80 LOW"},
    {val = 46, max = 80, ghost = nil, label = "46/80 CLOUD"},
    {val = 48, max = 80, ghost = 68,  label = "48<-68 CAST"},
    {val = 80, max = 80, ghost = nil, label = "80/80 FULL"},
  }

  for i, st in ipairs(mana_states) do
    local ry = 42 + (i - 1) * 59
    render_mana_bar_2x(prog, 20,  ry, st.val, st.max, 3, st.ghost)
    draw_label_scaled(prog, 286, ry + 18, 2, st.label, PAL_MANA.text, PAL_MANA.text_shadow)

    render_mana_bar_2x(prog, 450, ry, st.val, st.max, 3, st.ghost)
    draw_label_scaled(prog, 716, ry + 18, 2, st.label, rgba(246, 250, 255, 255), rgba(16, 24, 42, 255))
  end

  local water_states = {
    {val = 0,   label = "0/100 IDLE"},
    {val = 12,  label = "12/100 T1"},
    {val = 35,  label = "35/100 T2"},
    {val = 68,  label = "68/100 T3"},
    {val = 100, label = "100/100 MAX"},
  }

  for i, st in ipairs(water_states) do
    local ry = 360 + (i - 1) * 47
    render_water_bar_2x(prog, 20,  ry, st.val, 3)
    draw_label_scaled(prog, 286, ry + 14, 2, st.label, PAL_WATER.text, PAL_WATER.text_shadow)

    render_water_bar_2x(prog, 450, ry, st.val, 3)
    draw_label_scaled(prog, 716, ry + 14, 2, st.label, rgba(242, 252, 255, 255), rgba(12, 26, 36, 255))
  end

  local spr_p = Sprite(prog.width, prog.height, ColorMode.RGB)
  spr_p.layers[1].name = "states_progression_2x"
  spr_p.cels[1].image = prog
  spr_p:saveAs(src_dir .. "/states_progression.aseprite")
  spr_p:close()

  scale_nn(prog, 2):saveAs(prv_dir .. "/states_progression_4x.png")
end

-- ============================================================================
-- PREVIEW 4: lowkey_flow_preview_4x.gif (16 FRAMES @ 90ms SEAMLESS LOOP)
-- ============================================================================
do
  -- Build at 2x native resolution (476x336) -> scale_nn(2) -> 952x672 crisp 4x HUD logical GIF!
  local gw, gh = 476, 336
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
    draw_label_scaled(frm, 12, 7, 2, string.format("V5 16F LOOP (90MS): LOTUS MIST + 4-TIER WATER  F%02d/16", f), rgba(228, 238, 252, 255), rgba(8, 10, 16, 255))

    -- Mana panel (Normal mist + cloud fill | Full lotus + full cloud fill)
    fill_rect(frm, 10, 32, gw - 20, 124, rgba(18, 22, 31, 255))
    stroke_rect(frm, 10, 32, gw - 20, 124, rgba(56, 80, 128, 255))
    render_mana_bar_2x(frm, 18, 34, 56, 80, f, nil)
    draw_label_scaled(frm, 286, 52, 2, "56/80 OUTWARD MIST", PAL_MANA.text, PAL_MANA.text_shadow)

    render_mana_bar_2x(frm, 18, 95, 80, 80, f, nil)
    draw_label_scaled(frm, 286, 113, 2, "80/80 FULL LOTUS", rgba(255, 244, 206, 255), PAL_MANA.text_shadow)

    -- Water panel (Tier 1 Trickle | Tier 2 Stream 1x | Tier 3 Torrent 2x)
    fill_rect(frm, 10, 164, gw - 20, 162, rgba(16, 21, 29, 255))
    stroke_rect(frm, 10, 164, gw - 20, 162, rgba(44, 108, 128, 255))

    render_water_bar_2x(frm, 18, 170, 12, f)
    draw_label_scaled(frm, 286, 184, 2, "12/100 T1 SLOW", rgba(148, 214, 226, 255), PAL_WATER.text_shadow)

    render_water_bar_2x(frm, 18, 220, 35, f)
    draw_label_scaled(frm, 286, 234, 2, "35/100 T2 STREAM 1x", PAL_WATER.text, PAL_WATER.text_shadow)

    render_water_bar_2x(frm, 18, 270, 76, f)
    draw_label_scaled(frm, 286, 284, 2, "76/100 T3 SURGE 2x", rgba(232, 255, 255, 255), PAL_WATER.text_shadow)

    spr_gif_2x:newCel(l_gif_2x, f, frm, Point(0, 0))
    spr_gif_4x:newCel(l_gif_4x, f, scale_nn(frm, 2), Point(0, 0))
  end

  spr_gif_2x:saveAs(src_dir .. "/lowkey_flow_preview.aseprite")
  spr_gif_4x:saveAs(prv_dir .. "/lowkey_flow_preview_4x.gif")
  spr_gif_2x:close()
  spr_gif_4x:close()
end

print("V5_BUILD_SUCCESS")
