-- XueXi Mod Imprint Desk UI v1 - Multi-layer Aseprite Pixel Art Generator
-- Generates editable .aseprite sources (src/) and native 1x PNG exports (export/)
-- Compatible with Aseprite 1.3.17 (API v39)

local BASE = "D:/alianximod/docs/资源库/过程归档/刻印台UI_v1_黑曜星坛与隐藏背包_20261113"
local SRC_DIR = BASE .. "/src"
local EXP_DIR = BASE .. "/export"

local function rgba(r, g, b, a)
  return app.pixelColor.rgba(
    math.max(0, math.min(255, math.floor(r + 0.5))),
    math.max(0, math.min(255, math.floor(g + 0.5))),
    math.max(0, math.min(255, math.floor(b + 0.5))),
    math.max(0, math.min(255, math.floor((a or 255) + 0.5)))
  )
end

local function hex(h, a)
  h = h:gsub("#", "")
  local r = tonumber(h:sub(1, 2), 16)
  local g = tonumber(h:sub(3, 4), 16)
  local b = tonumber(h:sub(5, 6), 16)
  local alpha = a or ((#h >= 8) and tonumber(h:sub(7, 8), 16) or 255)
  return rgba(r, g, b, alpha)
end

local function hexRgb(h)
  h = h:gsub("#", "")
  return tonumber(h:sub(1, 2), 16), tonumber(h:sub(3, 4), 16), tonumber(h:sub(5, 6), 16)
end

local function mixHex(h1, h2, t, a)
  local r1, g1, b1 = hexRgb(h1)
  local r2, g2, b2 = hexRgb(h2)
  return rgba(r1 + (r2 - r1) * t, g1 + (g2 - g1) * t, b1 + (b2 - b1) * t, a or 255)
end

local function put(img, x, y, col)
  if x >= 0 and y >= 0 and x < img.width and y < img.height then
    local sa = app.pixelColor.rgbaA(col)
    if sa <= 0 then return end
    if sa >= 255 then
      img:drawPixel(x, y, col)
      return
    end
    local dst = img:getPixel(x, y)
    local da = app.pixelColor.rgbaA(dst)
    if da == 0 then
      img:drawPixel(x, y, col)
      return
    end
    local sr, sg, sb = app.pixelColor.rgbaR(col), app.pixelColor.rgbaG(col), app.pixelColor.rgbaB(col)
    local dr, dg, db = app.pixelColor.rgbaR(dst), app.pixelColor.rgbaG(dst), app.pixelColor.rgbaB(dst)
    local af = sa / 255.0
    local df = (da / 255.0) * (1.0 - af)
    local outA = af + df
    if outA > 0 then
      local outR = (sr * af + dr * df) / outA
      local outG = (sg * af + dg * df) / outA
      local outB = (sb * af + db * df) / outA
      img:drawPixel(x, y, rgba(outR, outG, outB, outA * 255.0))
    end
  end
end

local function fillRect(img, x, y, w, h, col)
  for py = y, y + h - 1 do
    for px = x, x + w - 1 do
      put(img, px, py, col)
    end
  end
end

local function strokeRect(img, x, y, w, h, col)
  for px = x, x + w - 1 do
    put(img, px, y, col)
    put(img, px, y + h - 1, col)
  end
  for py = y, y + h - 1 do
    put(img, x, py, col)
    put(img, x + w - 1, py, col)
  end
end

local function drawLine(img, x0, y0, x1, y1, col)
  local dx = math.abs(x1 - x0)
  local sx = x0 < x1 and 1 or -1
  local dy = -math.abs(y1 - y0)
  local sy = y0 < y1 and 1 or -1
  local err = dx + dy
  while true do
    put(img, x0, y0, col)
    if x0 == x1 and y0 == y1 then break end
    local e2 = 2 * err
    if e2 >= dy then err = err + dy; x0 = x0 + sx end
    if e2 <= dx then err = err + dx; y0 = y0 + sy end
  end
end

local function drawRadialGlow(img, cx, cy, rx, ry, hexCol, maxAlpha)
  local r, g, b = hexRgb(hexCol)
  local x0 = math.floor(cx - rx)
  local x1 = math.ceil(cx + rx)
  local y0 = math.floor(cy - ry)
  local y1 = math.ceil(cy + ry)
  for py = y0, y1 do
    for px = x0, x1 do
      local nx = (px - cx) / rx
      local ny = (py - cy) / ry
      local d2 = nx * nx + ny * ny
      if d2 < 1.0 then
        local falloff = (1.0 - math.sqrt(d2))
        falloff = falloff * falloff
        local a = maxAlpha * falloff
        if a >= 2 then
          put(img, px, py, rgba(r, g, b, a))
        end
      end
    end
  end
end

local function brickNoise(x, y)
  local n = (x * 73856093 + y * 19349663) % 97
  return n / 97.0
end

local function drawIsoObsidianBox(img, cx, topY, hw, h, opts)
  opts = opts or {}
  local hh = math.max(2, math.floor(hw / 2))
  local cOut    = hex(opts.out or "#09080F")
  local cTop    = opts.top or "#534C73"
  local cTopHi  = opts.topHi or "#7B72A6"
  local cTopRim = opts.topRim or "#9E94CC"
  local cLeft   = opts.left or "#342E4D"
  local cLeftHi = opts.leftHi or "#4B436E"
  local cRight  = opts.right or "#1A1629"
  local cRightHi= opts.rightHi or "#28223D"
  local cMortar = hex(opts.mortar or "#0E0C17")
  local cPurpleTrim = opts.purpleTrim

  for dx = -hw, hw - 1 do
    local adx = dx < 0 and (-dx - 1) or dx
    local wallTop = topY + hh + math.floor((hw - 1 - adx) / 2)
    local wallBot = wallTop + h - 1
    local isLeft = (dx < 0)

    for py = wallTop, wallBot do
      local relY = py - wallTop
      local course = math.floor(relY / 4)
      local inCourse = relY % 4
      local stagger = (course % 2) * 4
      local isMortarH = (inCourse == 0 and relY > 0)
      local isMortarV = (((adx + stagger) % 7) == 0) and (relY > 1) and (py < wallBot - 1)

      local col
      if dx == -hw or dx == hw - 1 or py == wallBot then
        col = cOut
      elseif dx == -1 then
        col = isMortarH and hex("#1F1B30") or hex(cLeftHi)
      elseif dx == 0 then
        col = hex("#100E1A")
      elseif isMortarH or isMortarV then
        col = cMortar
      else
        local n = brickNoise(cx + dx, py)
        if isLeft then
          if inCourse == 1 then
            col = mixHex(cLeft, cLeftHi, 0.55)
          else
            col = mixHex(cLeft, "#1E1A2E", n * 0.35)
          end
        else
          if inCourse == 1 then
            col = mixHex(cRight, cRightHi, 0.45)
          else
            col = mixHex(cRight, "#110F1C", n * 0.35)
          end
        end
      end
      put(img, cx + dx, py, col)
    end
  end

  for dy = 0, 2 * hh - 1 do
    local distFromMid = math.abs(dy - hh + 0.5)
    local span = math.max(1, math.floor((hh - distFromMid + 0.5) * 2))
    for dx = -span, span - 1 do
      local px = cx + dx
      local py = topY + dy
      local onOuter = (dx == -span or dx == span - 1 or dy == 0)
      local onInnerBevel = (dx == -span + 1 or dx == span - 2 or dy == 1)
      local onFrontLip = (dy >= hh and (dx <= -span + 2 or dx >= span - 3))
      local col
      if onOuter then
        col = cOut
      elseif cPurpleTrim and onFrontLip then
        col = hex(cPurpleTrim)
      elseif onInnerBevel and dy <= hh then
        col = hex(cTopRim)
      elseif onFrontLip then
        col = hex(cTopHi)
      else
        local slab = ((dx + dy * 2) % 8 == 0) or ((dx - dy * 2) % 9 == 0)
        if slab then
          col = hex("#2A2540")
        else
          local n = brickNoise(px, py)
          col = mixHex(cTop, cTopHi, n * 0.35)
        end
      end
      put(img, px, py, col)
    end
  end

  if opts.cornice then
    local chw = hw + 2
    local chh = math.max(2, math.floor(chw / 2))
    local cTopY = topY - 2
    for dx = -chw, chw - 1 do
      local adx = dx < 0 and (-dx - 1) or dx
      local wTop = cTopY + chh + math.floor((chw - 1 - adx) / 2)
      for py = wTop, wTop + 3 do
        local col
        if dx == -chw or dx == chw - 1 or py == wTop + 3 then
          col = cOut
        elseif dx < 0 then
          col = (py == wTop) and hex("#9188BA") or hex("#5E5680")
        else
          col = (py == wTop) and hex("#5A527A") or hex("#322C47")
        end
        put(img, cx + dx, py, col)
      end
    end
    for dy = 0, 2 * chh - 1 do
      local distFromMid = math.abs(dy - chh + 0.5)
      local span = math.max(1, math.floor((chh - distFromMid + 0.5) * 2))
      for dx = -span, span - 1 do
        local px = cx + dx
        local py = cTopY + dy
        local onOuter = (dx == -span or dx == span - 1 or dy == 0)
        local onInner = (dx == -span + 1 or dx == span - 2 or dy == 1)
        local onFront = (dy >= chh and (dx <= -span + 2 or dx >= span - 3))
        if onOuter then
          put(img, px, py, cOut)
        elseif cPurpleTrim and onFront then
          put(img, px, py, hex(cPurpleTrim))
        elseif onInner and dy <= chh then
          put(img, px, py, hex("#B0A6DB"))
        else
          local slab = ((dx + dy * 2) % 7 == 0)
          put(img, px, py, slab and hex("#423B61") or mixHex("#5D5582", "#8278B0", brickNoise(px, py) * 0.45))
        end
      end
    end
  end
end

local function drawCornerMerlons(img, cx, topY, hw)
  local hh = math.max(2, math.floor(hw / 2))
  local pts = {
    {cx, topY - 3, 3, 4},
    {cx - hw + 1, topY + hh - 3, 3, 5},
    {cx + hw - 1, topY + hh - 3, 3, 5},
    {cx, topY + 2 * hh - 3, 3, 4},
  }
  for _, m in ipairs(pts) do
    drawIsoObsidianBox(img, m[1], m[2], m[3], m[4], {
      top = "#5E5682", topHi = "#8A80B5", left = "#3F3959", right = "#242036"
    })
  end
end

local function drawDarkSlot18(img, sx, sy, accentHex)
  fillRect(img, sx, sy, 18, 18, hex("#161324"))
  for i = 0, 17 do
    put(img, sx + i, sy, hex("#07060C"))
    put(img, sx, sy + i, hex("#07060C"))
  end
  for i = 1, 17 do
    put(img, sx + i, sy + 17, hex("#4E466E"))
    put(img, sx + 17, sy + i, hex("#4E466E"))
  end
  put(img, sx + 17, sy, hex("#262138"))
  put(img, sx, sy + 17, hex("#262138"))
  for i = 1, 16 do
    put(img, sx + i, sy + 1, hex("#0D0B14"))
    put(img, sx + 1, sy + i, hex("#0D0B14"))
  end
  for i = 2, 15 do
    put(img, sx + i, sy + 16, hex("#27223B"))
    put(img, sx + 16, sy + i, hex("#27223B"))
  end
  if accentHex then
    local ac = hex(accentHex, 145)
    put(img, sx + 2, sy + 2, ac)
    put(img, sx + 15, sy + 2, ac)
    put(img, sx + 2, sy + 15, ac)
    put(img, sx + 15, sy + 15, ac)
  end
end

local function drawTurretCrystal(img, cx, cy, pal, boost)
  boost = boost or 0.0
  drawRadialGlow(img, cx, cy, 13, 12, pal.glow, 70 + boost * 50)
  local rows = {
    {dy = -8, l = 0, r = 0},
    {dy = -7, l = -1, r = 1},
    {dy = -6, l = -2, r = 2},
    {dy = -5, l = -3, r = 3},
    {dy = -4, l = -4, r = 4},
    {dy = -3, l = -5, r = 5},
    {dy = -2, l = -5, r = 5},
    {dy = -1, l = -5, r = 5},
    {dy =  0, l = -5, r = 5},
    {dy =  1, l = -4, r = 4},
    {dy =  2, l = -4, r = 4},
    {dy =  3, l = -3, r = 3},
    {dy =  4, l = -2, r = 2},
    {dy =  5, l = -1, r = 1},
  }
  for _, row in ipairs(rows) do
    local py = cy + row.dy
    for dx = row.l, row.r do
      local px = cx + dx
      local col
      local onCrownEdge = (math.abs(dx) + math.abs(row.dy + 4) == 3 and row.dy <= -1)
      if dx == row.l or dx == row.r or row.dy == -8 or row.dy == 5 then
        col = hex(pal.out)
      elseif onCrownEdge or (dx == -1 and row.dy >= -2 and row.dy <= 3) then
        col = mixHex(pal.bright, "#FFFFFF", 0.72 + boost * 0.25)
      elseif row.dy <= -3 and math.abs(dx) <= 2 then
        col = mixHex(pal.bright, "#FFFFFF", 0.50 + boost * 0.3)
      elseif dx < -1 then
        col = mixHex(pal.mid, pal.bright, 0.68 + boost * 0.22)
      elseif dx <= 1 then
        col = mixHex(pal.mid, pal.bright, 0.38 + boost * 0.25)
      else
        col = mixHex(pal.shadow, pal.mid, 0.35 + boost * 0.25)
      end
      put(img, px, py, col)
    end
  end
  put(img, cx - 2, cy - 4, hex("#FFFFFF"))
  put(img, cx - 1, cy - 5, hex("#FFFFFF"))
  put(img, cx, cy - 6, hex("#FFFFFF"))
end

local function drawMiniLantern(img, cx, cy, boost)
  boost = boost or 0.0
  drawRadialGlow(img, cx, cy, 9, 8, "#C26BFF", 65 + boost * 45)
  for dy = -4, 3 do
    local span = 4 - math.floor(math.abs(dy + 0.5) * 0.65)
    for dx = -span, span - 1 do
      local col
      if dx == -span or dx == span - 1 or dy == -4 or dy == 3 then
        col = hex("#2D1259")
      elseif dy <= -1 then
        col = mixHex("#F5DCFF", "#FFFFFF", 0.6 + boost * 0.35)
      elseif dx < 0 then
        col = mixHex("#DC8FFF", "#FFFFFF", 0.45 + boost * 0.35)
      else
        col = mixHex("#9638F0", "#D480FF", 0.4 + boost * 0.35)
      end
      put(img, cx + dx, cy + dy, col)
    end
  end
end

local function buildAltarHub()
  local W, H = 184, 192
  local CX = 92
  local spr = Sprite(W, H, ColorMode.RGB)
  spr.filename = SRC_DIR .. "/imprint_altar_hub_v1.aseprite"

  local lGlowBg   = spr.layers[1]; lGlowBg.name = "01_ambient_backglow"
  local lBastion  = spr:newLayer(); lBastion.name = "02_obsidian_base_bastion"
  local lBanners  = spr:newLayer(); lBanners.name = "03_banners_and_prow_sigil"
  local lTowers   = spr:newLayer(); lTowers.name  = "04_elemental_towers"
  local lApex     = spr:newLayer(); lApex.name    = "05_apex_amethyst_octahedron"
  local lConduits = spr:newLayer(); lConduits.name= "06_rune_veins_and_conduits"
  local lTablet   = spr:newLayer(); lTablet.name  = "07_embedded_operating_tablet"
  local lAnim     = spr:newLayer(); lAnim.name    = "08_breathing_glow_anim"

  spr.frames[1].duration = 0.12
  for f = 2, 8 do
    local fr = spr:newEmptyFrame(f)
    fr.duration = 0.12
  end

  local function makeStaticLayer(layer, drawFn)
    local img = Image(W, H, ColorMode.RGB)
    drawFn(img)
    spr:newCel(layer, 1, img, Point(0, 0))
    for f = 2, 8 do
      spr:newCel(layer, f, img, Point(0, 0))
    end
  end

  makeStaticLayer(lGlowBg, function(img)
    drawRadialGlow(img, CX, 24, 40, 28, "#9D3DFF", 80)
    drawRadialGlow(img, CX, 96, 86, 78, "#4A1985", 46)
    drawRadialGlow(img, CX, 156, 54, 32, "#7A28DB", 56)
  end)

  makeStaticLayer(lBastion, function(img)
    drawIsoObsidianBox(img, CX - 58, 46, 10, 26, {cornice = true})
    drawIsoObsidianBox(img, CX + 58, 46, 10, 26, {cornice = true})
    drawIsoObsidianBox(img, CX, 25, 24, 34, {cornice = true, purpleTrim = "#8B3DFF"})
    drawIsoObsidianBox(img, CX - 22, 33, 18, 30, {cornice = true, purpleTrim = "#7C3AED"})
    drawIsoObsidianBox(img, CX + 22, 33, 18, 30, {cornice = true, purpleTrim = "#7C3AED"})
    drawIsoObsidianBox(img, CX - 38, 52, 22, 58, {cornice = true})
    drawIsoObsidianBox(img, CX + 38, 52, 22, 58, {cornice = true})
    drawIsoObsidianBox(img, CX, 46, 54, 78, {})

    drawIsoObsidianBox(img, CX, 114, 48, 34, {top = "#26203B", left = "#1E1930", right = "#131020"})
    drawIsoObsidianBox(img, CX - 18, 123, 12, 16, {cornice = true})
    drawIsoObsidianBox(img, CX + 18, 123, 12, 16, {cornice = true})

    drawIsoObsidianBox(img, CX - 56, 134, 12, 24, {cornice = true})
    drawIsoObsidianBox(img, CX + 56, 134, 12, 24, {cornice = true})
    drawIsoObsidianBox(img, CX - 43, 137, 13, 28, {cornice = true, purpleTrim = "#8B3DFF"})
    drawIsoObsidianBox(img, CX + 43, 137, 13, 28, {cornice = true, purpleTrim = "#8B3DFF"})
    drawIsoObsidianBox(img, CX - 24, 136, 15, 36, {cornice = true, purpleTrim = "#9E4DFF"})
    drawIsoObsidianBox(img, CX + 24, 136, 15, 36, {cornice = true, purpleTrim = "#9E4DFF"})
    drawIsoObsidianBox(img, CX, 143, 16, 36, {
      cornice = true, top = "#4A426B", topHi = "#7367A3", left = "#312B4A", right = "#1D182E", purpleTrim = "#B866FF"
    })
    drawIsoObsidianBox(img, CX - 24, 168, 8, 8, {cornice = true})
    drawIsoObsidianBox(img, CX + 24, 168, 8, 8, {cornice = true})
    drawIsoObsidianBox(img, CX, 176, 8, 8, {cornice = true, purpleTrim = "#9E4DFF"})

    drawIsoObsidianBox(img, CX - 15, 25, 5, 6, {})
    drawIsoObsidianBox(img, CX + 15, 25, 5, 6, {})
    drawIsoObsidianBox(img, CX - 31, 34, 5, 6, {})
    drawIsoObsidianBox(img, CX + 31, 34, 5, 6, {})
  end)

  makeStaticLayer(lBanners, function(img)
    local function drawBanner(bx, topY, isLeft)
      for dx = -1, 9 do
        local drop = isLeft and math.floor((dx + 1) / 3) or math.floor((9 - dx) / 3)
        put(img, bx + dx, topY + drop, hex("#8A7D9E"))
        put(img, bx + dx, topY + drop + 1, hex("#161224"))
      end
      for dx = 0, 8 do
        local slope = isLeft and math.floor(dx / 2) or math.floor((8 - dx) / 2)
        local notch = math.max(0, 3 - math.abs(dx - 4))
        local y0 = topY + 2 + slope
        local y1 = y0 + 28 - notch
        for py = y0, y1 do
          local col
          if dx == 0 or dx == 8 or py == y1 then
            col = hex("#180A2E")
          elseif dx == 1 or dx == 7 then
            col = hex("#6C2BD9")
          else
            col = ((py + dx) % 5 == 0) and hex("#3A166E") or hex("#2B0F54")
          end
          put(img, bx + dx, py, col)
        end
      end
      local gyBase = topY + 8
      local glyphs = {
        { {0,-1},{-1,0},{0,0},{1,0},{0,1},{-1,2},{1,2} },
        { {-1,-1},{1,-1},{0,0},{-1,1},{0,1},{1,1},{0,2} },
        { {0,-1},{-1,0},{1,0},{0,1},{0,2} }
      }
      for gi, g in ipairs(glyphs) do
        local gcx = bx + 4
        local gcy = gyBase + (gi - 1) * 7
        for _, pt in ipairs(g) do
          put(img, gcx + pt[1], gcy + pt[2], (pt[1] == 0 and pt[2] == 0) and hex("#F7DCFF") or hex("#C975FF"))
        end
      end
    end

    drawBanner(CX - 29, 145, true)
    drawBanner(CX + 21, 145, false)

    drawRadialGlow(img, CX, 164, 11, 11, "#B85CFF", 85)
    for py = 156, 172 do
      put(img, CX - 1, py, hex("#F5D6FF"))
      put(img, CX,     py, hex("#BD66FF"))
    end
    drawLine(img, CX - 5, 160, CX - 1, 157, hex("#EAB8FF"))
    drawLine(img, CX,     157, CX + 4, 160, hex("#C975FF"))
    drawLine(img, CX - 5, 160, CX - 1, 163, hex("#F5D6FF"))
    drawLine(img, CX,     163, CX + 4, 160, hex("#D485FF"))
    drawLine(img, CX - 7, 167, CX - 1, 171, hex("#F2C4FF"))
    drawLine(img, CX,     171, CX + 6, 167, hex("#C975FF"))
    drawLine(img, CX - 7, 164, CX - 4, 166, hex("#E5A8FF"))
    drawLine(img, CX + 6, 164, CX + 3, 166, hex("#BD66FF"))
    put(img, CX - 7, 164, hex("#FFFFFF")); put(img, CX + 6, 164, hex("#F5D6FF"))
    put(img, CX - 1, 164, hex("#FFFFFF")); put(img, CX - 1, 172, hex("#FFFFFF"))
  end)

  makeStaticLayer(lTowers, function(img)
    drawIsoObsidianBox(img, CX - 42, 36, 12, 24, {cornice = true, purpleTrim = "#4EE08E"})
    drawCornerMerlons(img, CX - 42, 34, 13)
    drawTurretCrystal(img, CX - 42, 28, {
      out = "#07381C", shadow = "#0E6B34", mid = "#18AD55", bright = "#3AF282", glow = "#3AF282"
    }, 0)

    drawIsoObsidianBox(img, CX + 42, 36, 12, 24, {cornice = true, purpleTrim = "#FF9838"})
    drawCornerMerlons(img, CX + 42, 34, 13)
    drawTurretCrystal(img, CX + 42, 28, {
      out = "#4A1204", shadow = "#8F290A", mid = "#D94E14", bright = "#FFA834", glow = "#FF8C24"
    }, 0)

    drawIsoObsidianBox(img, CX - 64, 96, 16, 38, {cornice = true, purpleTrim = "#A84DFF"})
    drawIsoObsidianBox(img, CX - 64, 74, 12, 22, {cornice = true, purpleTrim = "#C270FF"})
    drawCornerMerlons(img, CX - 64, 72, 13)
    drawTurretCrystal(img, CX - 64, 65, {
      out = "#2A0854", shadow = "#5E1FA8", mid = "#9D3DFF", bright = "#E294FF", glow = "#B85CFF"
    }, 0)
    fillRect(img, CX - 66, 108, 4, 21, hex("#120A24"))
    for py = 110, 126 do put(img, CX - 64, py, hex("#C86BFF")) end
    put(img, CX - 66, 113, hex("#F2C4FF")); put(img, CX - 62, 113, hex("#F2C4FF"))
    put(img, CX - 65, 114, hex("#E094FF")); put(img, CX - 63, 114, hex("#E094FF"))
    put(img, CX - 66, 120, hex("#E094FF")); put(img, CX - 62, 120, hex("#E094FF"))
    put(img, CX - 64, 115, hex("#FFFFFF"))

    drawIsoObsidianBox(img, CX + 64, 96, 16, 38, {cornice = true, purpleTrim = "#66CCFF"})
    drawIsoObsidianBox(img, CX + 64, 74, 12, 22, {cornice = true, purpleTrim = "#4EE2FF"})
    drawCornerMerlons(img, CX + 64, 72, 13)
    drawTurretCrystal(img, CX + 64, 65, {
      out = "#072854", shadow = "#13529E", mid = "#1B78E0", bright = "#4EE2FF", glow = "#38C8FF"
    }, 0)
    fillRect(img, CX + 62, 108, 4, 21, hex("#120A24"))
    for py = 110, 126 do put(img, CX + 64, py, hex("#C86BFF")) end
    put(img, CX + 62, 113, hex("#F2C4FF")); put(img, CX + 66, 113, hex("#F2C4FF"))
    put(img, CX + 63, 114, hex("#E094FF")); put(img, CX + 65, 114, hex("#E094FF"))
    put(img, CX + 62, 120, hex("#E094FF")); put(img, CX + 66, 120, hex("#E094FF"))
    put(img, CX + 64, 115, hex("#FFFFFF"))

    drawMiniLantern(img, CX - 43, 134, 0)
    drawMiniLantern(img, CX + 43, 134, 0)
    drawMiniLantern(img, CX - 24, 34, 0)
    drawMiniLantern(img, CX + 24, 34, 0)
  end)

  local function renderApexOctahedron(img, boost)
    boost = boost or 0.0
    local acx, acy = CX, 23
    drawRadialGlow(img, acx, acy, 26, 23, "#AD4BFF", 92 + boost * 55)
    for dy = -21, 29 do
      local dist = math.abs(dy)
      if dist >= 14 then
        local a = math.max(0, 220 - (dist - 14) * 17) + boost * 35
        put(img, acx - 1, acy + dy, hex("#F2C4FF", math.min(255, a)))
        put(img, acx,     acy + dy, hex("#B85CFF", math.min(255, a * 0.85)))
      end
    end

    for dy = -15, 14 do
      local norm = (dy < 0) and (-dy - 0.5) / 14.5 or (dy + 0.5) / 14.5
      local hw = math.max(1, math.floor((1.0 - norm) * 12.5 + 0.5))
      local innerHw = math.max(0, math.floor((1.0 - norm) * 5.8 + 0.5))
      for dx = -hw, hw - 1 do
        local px = acx + dx
        local py = acy + dy
        local isBorder = (dx == -hw or dx == hw - 1 or dy == -15 or dy == 14)
        local isOuterWire = (dx == -hw + 1 or dx == hw - 2)
        local onInnerRidge = (dx == -innerHw or dx == innerHw - 1)
        local onEquator = (dy == -1 or (dy == 0 and math.abs(dx) <= hw - 2))
        local col
        if isBorder then
          col = hex("#451085")
        elseif isOuterWire then
          col = mixHex("#EBB8FF", "#FFFFFF", (dy <= 0) and 0.72 or 0.42)
        elseif onEquator or onInnerRidge or dx == -1 then
          col = mixHex("#F7DCFF", "#FFFFFF", 0.85 + boost * 0.15)
        elseif math.abs(dx) < innerHw and dy < 0 then
          col = mixHex("#EBB8FF", "#FFFFFF", 0.48 + boost * 0.28)
        elseif math.abs(dx) < innerHw and dy >= 0 then
          col = mixHex("#B554FF", "#F0C2FF", 0.40 + boost * 0.28)
        elseif dy < 0 and dx < 0 then
          col = mixHex("#D882FF", "#F7DCFF", 0.40 + boost * 0.28)
        elseif dy < 0 and dx > 0 then
          col = mixHex("#9D3DFF", "#DE94FF", 0.35 + boost * 0.28)
        elseif dy >= 0 and dx < 0 then
          col = mixHex("#8A2BE2", "#CC75FF", 0.32 + boost * 0.28)
        else
          col = mixHex("#5818A1", "#9638F0", 0.30 + boost * 0.28)
        end
        put(img, px, py, col)
      end
    end

    local sparks = { {CX - 18, 15}, {CX + 17, 17}, {CX - 15, 34}, {CX + 15, 33} }
    for _, sp in ipairs(sparks) do
      local sa = math.min(255, 160 + boost * 90)
      put(img, sp[1], sp[2], hex("#FFFFFF", sa))
      put(img, sp[1] - 1, sp[2], hex("#E5A8FF", sa * 0.65))
      put(img, sp[1] + 1, sp[2], hex("#E5A8FF", sa * 0.65))
      put(img, sp[1], sp[2] - 1, hex("#E5A8FF", sa * 0.65))
      put(img, sp[1], sp[2] + 1, hex("#E5A8FF", sa * 0.65))
    end
  end

  makeStaticLayer(lApex, function(img)
    renderApexOctahedron(img, 0)
  end)

  makeStaticLayer(lConduits, function(img)
    drawLine(img, CX - 34, 43, CX - 16, 52, hex("#FFE8A3"))
    drawLine(img, CX - 34, 44, CX - 16, 53, hex("#C06BFF"))
    drawLine(img, CX + 33, 43, CX + 15, 52, hex("#FFE8A3"))
    drawLine(img, CX + 33, 44, CX + 15, 53, hex("#C06BFF"))

    drawRadialGlow(img, 41, 82, 10, 7, "#B85CFF", 95)
    drawRadialGlow(img, 143, 82, 10, 7, "#B85CFF", 95)
    for px = 35, 46 do
      put(img, px, 80, hex("#7E2EE0", 200))
      put(img, px, 81, hex("#EBB8FF"))
      put(img, px, 82, hex("#FFFFFF"))
      put(img, px, 83, hex("#C86BFF"))
      put(img, px, 84, hex("#691EB8", 200))
    end
    for px = 138, 149 do
      put(img, px, 80, hex("#7E2EE0", 200))
      put(img, px, 81, hex("#EBB8FF"))
      put(img, px, 82, hex("#FFFFFF"))
      put(img, px, 83, hex("#C86BFF"))
      put(img, px, 84, hex("#691EB8", 200))
    end

    drawRadialGlow(img, CX, 134, 9, 12, "#A84DFF", 90)
    for py = 124, 143 do
      put(img, CX - 2, py, hex("#7E2EE0", 190))
      put(img, CX - 1, py, hex("#FFFFFF"))
      put(img, CX,     py, hex("#E29EFF"))
      put(img, CX + 1, py, hex("#7E2EE0", 190))
    end
  end)

  makeStaticLayer(lTablet, function(img)
    local tx, ty, tw, th = 46, 56, 92, 68
    fillRect(img, tx - 2, ty, tw + 4, th + 4, hex("#07050D", 205))

    fillRect(img, 64, 53, 56, 4, hex("#625A82"))
    strokeRect(img, 64, 53, 56, 4, hex("#0B0912"))
    for px = 66, 117 do put(img, px, 54, hex("#9A91C2")) end
    fillRect(img, 78, 51, 28, 5, hex("#706794"))
    strokeRect(img, 78, 51, 28, 5, hex("#0B0912"))
    for px = 80, 103 do put(img, px, 52, hex("#B2A8DB")) end

    fillRect(img, tx + 1, ty, tw - 2, th, hex("#635B82"))
    fillRect(img, tx, ty + 1, tw, th - 2, hex("#635B82"))
    strokeRect(img, tx + 1, ty, tw - 2, th, hex("#0B0912"))
    strokeRect(img, tx, ty + 1, tw, th - 2, hex("#0B0912"))

    for px = tx + 2, tx + tw - 3 do
      put(img, px, ty + 1, hex("#A89FD4"))
      put(img, px, ty + 2, hex("#7E75A3"))
      put(img, px, ty + th - 2, hex("#231E34"))
      put(img, px, ty + th - 3, hex("#3B3454"))
    end
    for py = ty + 2, ty + th - 3 do
      put(img, tx + 1, py, hex("#9289BA"))
      put(img, tx + 2, py, hex("#6E6691"))
      put(img, tx + tw - 2, py, hex("#262138"))
      put(img, tx + tw - 3, py, hex("#423A5E"))
    end

    fillRect(img, tx + 4, ty + 3, tw - 8, th - 7, hex("#4D4669"))
    strokeRect(img, tx + 4, ty + 3, tw - 8, th - 7, hex("#231E36"))

    for py = ty + 4, ty + th - 5 do
      for px = tx + 5, tx + tw - 6 do
        local n = brickNoise(px, py)
        if n > 0.82 then
          put(img, px, py, hex("#5B537A"))
        elseif n < 0.14 then
          put(img, px, py, hex("#3E3857"))
        end
      end
    end

    fillRect(img, 53, 59, 78, 12, hex("#29243D"))
    for px = 53, 130 do
      put(img, px, 59, hex("#14111F"))
      put(img, px, 70, hex("#756C99"))
    end
    fillRect(img, 90, 53, 4, 4, hex("#B85CFF"))
    put(img, 91, 54, hex("#FFFFFF")); put(img, 92, 54, hex("#F5D6FF"))
    put(img, 91, 55, hex("#EABFFF")); put(img, 92, 55, hex("#C86BFF"))

    for py = 74, 90, 3 do
      put(img, 48, py, hex("#E5A8FF"))
      put(img, 135, py, hex("#E5A8FF"))
    end

    strokeRect(img, 61, 71, 22, 22, hex("#2A243D"))
    strokeRect(img, 62, 72, 20, 20, hex("#8279AD"))
    strokeRect(img, 101, 71, 22, 22, hex("#2A243D"))
    strokeRect(img, 102, 72, 20, 20, hex("#8279AD"))
    drawDarkSlot18(img, 63, 73, "#8C42E8")
    drawDarkSlot18(img, 103, 73, "#8C42E8")

    for px = 56, 127 do
      put(img, px, 93, hex("#241F36"))
      put(img, px, 94, hex("#7A719E"))
    end

    strokeRect(img, 61, 95, 62, 22, hex("#120F1C"))
    fillRect(img, 62, 96, 60, 20, hex("#1B162B"))
    fillRect(img, 55, 102, 5, 8, hex("#756C99"))
    strokeRect(img, 55, 102, 5, 8, hex("#14111F"))
    put(img, 57, 105, hex("#E5A8FF")); put(img, 57, 106, hex("#FFFFFF"))

    fillRect(img, 124, 102, 5, 8, hex("#756C99"))
    strokeRect(img, 124, 102, 5, 8, hex("#14111F"))
    put(img, 126, 105, hex("#E5A8FF")); put(img, 126, 106, hex("#FFFFFF"))

    local corners = { {46, 56}, {134, 56}, {46, 120}, {134, 120} }
    for _, c in ipairs(corners) do
      fillRect(img, c[1], c[2], 4, 4, hex("#877EB5"))
      strokeRect(img, c[1], c[2], 4, 4, hex("#0E0C17"))
      put(img, c[1] + 1, c[2] + 1, hex("#CFC7F2"))
      put(img, c[1] + 2, c[2] + 2, hex("#4E476B"))
    end
  end)

  for f = 1, 8 do
    local phase = (f - 1) / 8.0 * math.pi * 2.0
    local breath = (1.0 - math.cos(phase)) * 0.5
    local img = Image(W, H, ColorMode.RGB)
    if breath > 0.02 then
      drawRadialGlow(img, CX, 23, 27, 23, "#DF8FFF", math.floor(breath * 72))
      drawRadialGlow(img, CX - 42, 28, 13, 12, "#47FF93", math.floor(breath * 52))
      drawRadialGlow(img, CX + 42, 28, 13, 12, "#FFB84D", math.floor(breath * 52))
      drawRadialGlow(img, CX - 64, 65, 14, 13, "#D982FF", math.floor(breath * 56))
      drawRadialGlow(img, CX + 64, 65, 14, 13, "#66EAFF", math.floor(breath * 56))
      drawRadialGlow(img, CX - 43, 134, 10, 9, "#E5A8FF", math.floor(breath * 52))
      drawRadialGlow(img, CX + 43, 134, 10, 9, "#E5A8FF", math.floor(breath * 52))
      drawRadialGlow(img, CX, 164, 11, 11, "#D480FF", math.floor(breath * 54))
      drawRadialGlow(img, 41, 82, 9, 6, "#F0C2FF", math.floor(breath * 55))
      drawRadialGlow(img, 143, 82, 9, 6, "#F0C2FF", math.floor(breath * 55))
      drawRadialGlow(img, CX, 134, 8, 10, "#E5A8FF", math.floor(breath * 48))

      local ca = math.floor(breath * 150)
      put(img, CX - 1, 19, hex("#FFFFFF", ca))
      put(img, CX,     19, hex("#FFFFFF", ca))
      put(img, CX - 64, 115, hex("#FFFFFF", ca))
      put(img, CX + 64, 115, hex("#FFFFFF", ca))
    end
    spr:newCel(lAnim, f, img, Point(0, 0))
  end

  spr:saveAs(spr.filename)

  local flat1 = Image(W, H, ColorMode.RGB)
  flat1:drawSprite(spr, 1, Point(0, 0))
  flat1:saveAs(EXP_DIR .. "/altar_hub.png")

  local strip = Image(W, H * 8, ColorMode.RGB)
  for f = 1, 8 do
    strip:drawSprite(spr, f, Point(0, (f - 1) * H))
  end
  strip:saveAs(EXP_DIR .. "/altar_hub_glow_strip.png")

  spr:close()
end

local function buildStudySlips()
  local spr = Sprite(16, 16, ColorMode.RGB)
  spr.filename = SRC_DIR .. "/imprint_study_slips_v1.aseprite"
  local lBase = spr.layers[1]; lBase.name = "01_bamboo_or_tome_body"
  local lCord = spr:newLayer(); lCord.name = "02_binding_and_trim"
  local lRune = spr:newLayer(); lRune.name = "03_ink_or_glow_runes"

  spr:newEmptyFrame(2)
  spr:newEmptyFrame(3)

  do
    local b = Image(16, 16, ColorMode.RGB)
    local c = Image(16, 16, ColorMode.RGB)
    local r = Image(16, 16, ColorMode.RGB)
    for y = 1, 14 do
      for x = 1, 14 do
        local s = (x - 7.5) - (y - 7.5)
        local t = (x - 7.5) + (y - 7.5)
        if math.abs(s) <= 8.5 and math.abs(t) <= 4.5 then
          local isOut = (math.abs(s) >= 7.5 or math.abs(t) >= 3.5)
          if isOut then
            put(b, x, y, hex("#3D2812"))
          elseif s >= 5.5 then
            put(b, x, y, (math.abs(t) <= 1.2) and hex("#5C3D1C") or hex("#F0D79E"))
          else
            local slat = math.floor(s + 9) % 3
            if t < -1.2 then
              put(b, x, y, (slat == 0) and hex("#B58945") or hex("#ECD298"))
            elseif t <= 1.2 then
              put(b, x, y, (slat == 0) and hex("#996F33") or hex("#D6B272"))
            else
              put(b, x, y, hex("#875E29"))
            end
          end
        end
      end
    end
    local cords = { {6,6},{7,7},{8,8},{9,9}, {5,7},{6,8},{7,9},{8,10} }
    for _, pt in ipairs(cords) do
      put(c, pt[1], pt[2], (pt[1] + pt[2] <= 14) and hex("#6CF2CA") or hex("#228A6E"))
    end
    put(c, 7, 7, hex("#B8FFEA")); put(c, 9, 10, hex("#3CB896")); put(c, 10, 11, hex("#228A6E"))
    put(r, 9, 5, hex("#422A14")); put(r, 10, 6, hex("#422A14")); put(r, 5, 9, hex("#422A14"))
    spr:newCel(lBase, 1, b, Point(0, 0))
    spr:newCel(lCord, 1, c, Point(0, 0))
    spr:newCel(lRune, 1, r, Point(0, 0))
  end

  do
    local b = Image(16, 16, ColorMode.RGB)
    local c = Image(16, 16, ColorMode.RGB)
    local r = Image(16, 16, ColorMode.RGB)
    drawRadialGlow(b, 8, 8, 7.8, 7.8, "#A84DFF", 75)
    for y = 1, 14 do
      for x = 1, 14 do
        local s = (x - 7.5) - (y - 7.5)
        local t = (x - 7.5) + (y - 7.5)
        if math.abs(s) <= 9.0 and math.abs(t) <= 4.5 then
          local isOut = (math.abs(s) >= 8.0 or math.abs(t) >= 3.5)
          if isOut then
            put(b, x, y, hex("#2D183E"))
          elseif s >= 6.0 then
            put(b, x, y, (math.abs(t) <= 1.2) and hex("#4A1E75") or hex("#FFE8A6"))
          elseif s <= -6.0 then
            put(b, x, y, (t < 0) and hex("#64F0C8") or hex("#238F72"))
          else
            local slat = math.floor(s + 9) % 3
            if t < -1.2 then
              put(b, x, y, (slat == 0) and hex("#BA8C42") or hex("#F5DC9C"))
            elseif t <= 1.2 then
              put(b, x, y, (slat == 0) and hex("#91672C") or hex("#D9B068"))
            else
              put(b, x, y, hex("#6E4B1F"))
            end
          end
        end
      end
    end
    local cords = { {6,6},{7,7},{8,8},{9,9}, {5,7},{6,8},{7,9},{8,10} }
    for _, pt in ipairs(cords) do
      put(c, pt[1], pt[2], ((pt[1] + pt[2]) % 2 == 0) and hex("#FFE070") or hex("#B85CFF"))
    end
    put(c, 7, 7, hex("#FFFFFF")); put(c, 7, 8, hex("#E5A8FF")); put(c, 9, 10, hex("#FFD24C")); put(c, 10, 11, hex("#B85CFF"))
    put(r, 9, 5, hex("#C86BFF")); put(r, 10, 6, hex("#E5A8FF")); put(r, 5, 9, hex("#C86BFF"))
    put(r, 12, 3, hex("#FFFFFF")); put(r, 11, 3, hex("#E5A8FF")); put(r, 12, 4, hex("#E5A8FF"))
    spr:newCel(lBase, 2, b, Point(0, 0))
    spr:newCel(lCord, 2, c, Point(0, 0))
    spr:newCel(lRune, 2, r, Point(0, 0))
  end

  do
    local b = Image(16, 16, ColorMode.RGB)
    local c = Image(16, 16, ColorMode.RGB)
    local r = Image(16, 16, ColorMode.RGB)
    drawRadialGlow(b, 8, 8, 8, 8, "#C86BFF", 85)
    fillRect(b, 3, 2, 10, 12, hex("#36195E"))
    strokeRect(b, 2, 1, 12, 14, hex("#160829"))
    fillRect(b, 4, 12, 8, 2, hex("#F2DFB6"))
    fillRect(b, 12, 3, 1, 9, hex("#DEC38E"))
    fillRect(c, 3, 2, 2, 11, hex("#D99B2B"))
    put(c, 3, 3, hex("#FFF099")); put(c, 3, 7, hex("#FFF099")); put(c, 3, 11, hex("#FFF099"))
    fillRect(c, 10, 2, 3, 2, hex("#FFD866"))
    fillRect(c, 10, 10, 3, 2, hex("#D99B2B"))
    fillRect(r, 7, 5, 3, 4, hex("#B85CFF"))
    put(r, 8, 4, hex("#F5D6FF")); put(r, 8, 9, hex("#9435EB"))
    put(r, 6, 6, hex("#E5A8FF")); put(r, 10, 6, hex("#9435EB"))
    put(r, 8, 6, hex("#FFFFFF")); put(r, 8, 7, hex("#F5D0FF"))
    put(r, 13, 2, hex("#FFFFFF")); put(r, 12, 2, hex("#FFE899")); put(r, 13, 3, hex("#E5A8FF"))
    spr:newCel(lBase, 3, b, Point(0, 0))
    spr:newCel(lCord, 3, c, Point(0, 0))
    spr:newCel(lRune, 3, r, Point(0, 0))
  end

  spr:saveAs(spr.filename)
  local names = { "slip_mortal_icon.png", "slip_mystic_icon.png", "slip_transcendent_icon.png" }
  for f = 1, 3 do
    local out = Image(16, 16, ColorMode.RGB)
    out:drawSprite(spr, f, Point(0, 0))
    out:saveAs(EXP_DIR .. "/" .. names[f])
  end
  spr:close()
end

local function buildAltarWidgets()
  local spr = Sprite(64, 32, ColorMode.RGB)
  spr.filename = SRC_DIR .. "/imprint_altar_widgets_v1.aseprite"
  local lArrows = spr.layers[1]; lArrows.name = "01_mode_arrows"
  local lPlus   = spr:newLayer(); lPlus.name  = "02_mode_plus"
  local lBadges = spr:newLayer(); lBadges.name= "03_status_badges"
  local lGhost  = spr:newLayer(); lGhost.name = "04_slot_ghost_overlay"

  local imgA = Image(64, 32, ColorMode.RGB)
  local function drawRightArrow(ox, oy, cOut, cFill, cHi, glowHex)
    if glowHex then drawRadialGlow(imgA, ox + 8, oy + 6, 8, 6, glowHex, 85) end
    fillRect(imgA, ox + 1, oy + 4, 8, 4, hex(cOut))
    for d = 0, 5 do
      fillRect(imgA, ox + 8 + d, oy + 1 + d, 2, 10 - 2 * d, hex(cOut))
    end
    fillRect(imgA, ox + 2, oy + 5, 7, 2, hex(cFill))
    for d = 0, 3 do
      fillRect(imgA, ox + 9 + d, oy + 2 + d, 1, 8 - 2 * d, hex(cFill))
    end
    for px = ox + 2, ox + 9 do put(imgA, px, oy + 5, hex(cHi)) end
    put(imgA, ox + 9, oy + 3, hex(cHi)); put(imgA, ox + 10, oy + 4, hex(cHi)); put(imgA, ox + 11, oy + 5, hex("#FFFFFF"))
  end
  drawRightArrow(0,  0, "#120F1C", "#5C5578", "#8C84AD", nil)
  drawRightArrow(16, 0, "#2A0E52", "#B85CFF", "#FFE8A8", "#B85CFF")
  spr:newCel(lArrows, 1, imgA, Point(0, 0))

  local imgP = Image(64, 32, ColorMode.RGB)
  local function drawPlus(ox, oy, cOut, cFill, cHi, glowHex)
    if glowHex then drawRadialGlow(imgP, ox + 7, oy + 7, 7, 7, glowHex, 75) end
    fillRect(imgP, ox + 5, oy + 1, 4, 12, hex(cOut))
    fillRect(imgP, ox + 1, oy + 5, 12, 4, hex(cOut))
    fillRect(imgP, ox + 6, oy + 2, 2, 10, hex(cFill))
    fillRect(imgP, ox + 2, oy + 6, 10, 2, hex(cFill))
    put(imgP, ox + 6, oy + 2, hex(cHi)); put(imgP, ox + 2, oy + 6, hex(cHi))
    put(imgP, ox + 6, oy + 6, hex("#FFFFFF")); put(imgP, ox + 7, oy + 6, hex(cHi))
  end
  drawPlus(32, 0, "#120F1C", "#5E577A", "#9189B3", nil)
  drawPlus(48, 0, "#240E47", "#B85CFF", "#F5D6FF", "#A84DFF")
  spr:newCel(lPlus, 1, imgP, Point(0, 0))

  local imgB = Image(64, 32, ColorMode.RGB)
  local chk = { {1,4},{2,5},{3,4},{4,3},{5,2},{5,1} }
  for _, p in ipairs(chk) do
    for dy = -1, 1 do
      for dx = -1, 1 do
        put(imgB, p[1] + dx, 16 + p[2] + dy, hex("#072E14"))
      end
    end
  end
  for _, p in ipairs(chk) do
    put(imgB, p[1], 16 + p[2], (p[2] <= 2) and hex("#80FF9E") or hex("#22E05E"))
  end
  put(imgB, 2, 16 + 4, hex("#52FF7E"))

  fillRect(imgB, 8 + 2, 16 + 0, 3, 7, hex("#381205"))
  fillRect(imgB, 8 + 3, 16 + 1, 1, 3, hex("#FFD159"))
  put(imgB, 8 + 3, 16 + 1, hex("#FFFFFF"))
  put(imgB, 8 + 3, 16 + 5, hex("#FF8838"))

  for i = 1, 5 do
    for dy = -1, 1 do
      for dx = -1, 1 do
        put(imgB, 16 + i + dx, 16 + i + dy, hex("#36060E"))
        put(imgB, 16 + (6 - i) + dx, 16 + i + dy, hex("#36060E"))
      end
    end
  end
  for i = 1, 5 do
    put(imgB, 16 + i, 16 + i, hex("#FF5967"))
    put(imgB, 16 + (6 - i), 16 + i, hex("#FF5967"))
  end
  put(imgB, 16 + 3, 16 + 3, hex("#FFD1D6"))
  spr:newCel(lBadges, 1, imgB, Point(0, 0))

  local imgG = Image(64, 32, ColorMode.RGB)
  strokeRect(imgG, 32, 16, 16, 16, hex("#A84DFF", 120))
  strokeRect(imgG, 33, 17, 14, 14, hex("#E5A8FF", 55))
  spr:newCel(lGhost, 1, imgG, Point(0, 0))

  spr:saveAs(spr.filename)
  local out = Image(64, 32, ColorMode.RGB)
  out:drawSprite(spr, 1, Point(0, 0))
  out:saveAs(EXP_DIR .. "/altar_widgets.png")
  spr:close()
end

local function buildButtons()
  do
    local spr = Sprite(60, 60, ColorMode.RGB)
    spr.filename = SRC_DIR .. "/imprint_buttons_v1.aseprite"
    local lFrame = spr.layers[1]; lFrame.name = "01_obsidian_iron_frame"
    local lFace  = spr:newLayer(); lFace.name  = "02_amethyst_face"
    local lRunes = spr:newLayer(); lRunes.name = "03_side_vertical_runes"

    local imgFr = Image(60, 60, ColorMode.RGB)
    local imgFa = Image(60, 60, ColorMode.RGB)
    local imgRu = Image(60, 60, ColorMode.RGB)

    local states = {
      { oy = 0,  bevelHi = "#8279A6", bevelMid = "#544D70", faceTop = "#312054", faceBot = "#1E1336", rim = "#7C3AED", runeHi = "#F2C4FF", runeMid = "#B85CFF", glow = 55 },
      { oy = 20, bevelHi = "#B5A8E0", bevelMid = "#73669E", faceTop = "#4B2E80", faceBot = "#2D1A52", rim = "#C975FF", runeHi = "#FFFFFF", runeMid = "#E5A8FF", glow = 115 },
      { oy = 40, bevelHi = "#4A4759", bevelMid = "#343240", faceTop = "#24222E", faceBot = "#1A1922", rim = "#2E2C3B", runeHi = "#57536B", runeMid = "#3E3B4D", glow = 0 },
    }

    for _, st in ipairs(states) do
      local oy = st.oy
      fillRect(imgFr, 0, oy, 60, 20, hex(st.bevelMid))
      strokeRect(imgFr, 0, oy, 60, 20, hex("#0A0812"))
      for px = 2, 57 do
        put(imgFr, px, oy + 1, hex(st.bevelHi))
        put(imgFr, px, oy + 18, hex("#191626"))
      end
      for py = oy + 2, oy + 17 do
        put(imgFr, 1, py, hex(st.bevelHi))
        put(imgFr, 58, py, hex("#191626"))
      end
      local rc = { {1, oy + 1}, {57, oy + 1}, {1, oy + 17}, {57, oy + 17} }
      for _, c in ipairs(rc) do
        fillRect(imgFr, c[1], c[2], 2, 2, hex(st.bevelHi))
      end

      for py = 0, 13 do
        local t = py / 13.0
        local rowCol = mixHex(st.faceTop, st.faceBot, t)
        for px = 3, 56 do
          put(imgFa, px, oy + 3 + py, rowCol)
        end
      end
      strokeRect(imgFa, 3, oy + 3, 54, 14, hex(st.rim))
      strokeRect(imgFa, 4, oy + 3, 7, 14, hex(st.bevelMid))
      strokeRect(imgFa, 49, oy + 3, 7, 14, hex(st.bevelMid))
      if st.glow > 0 then
        drawRadialGlow(imgFa, 30, oy + 10, 22, 8, "#A84DFF", st.glow)
      end

      local runeRows = {
        {1,0,1},
        {0,1,0},
        {1,1,1},
        {0,0,0},
        {1,1,0},
        {0,1,1},
        {1,0,1},
        {0,0,0},
        {0,1,0},
        {1,0,1},
      }
      for ri, pat in ipairs(runeRows) do
        local ry = oy + 4 + ri
        for ci = 1, 3 do
          if pat[ci] == 1 then
            local col = (ci == 2) and hex(st.runeHi) or hex(st.runeMid)
            put(imgRu, 5 + ci, ry, col)
            put(imgRu, 49 + ci, ry, col)
          end
        end
      end
    end

    spr:newCel(lFrame, 1, imgFr, Point(0, 0))
    spr:newCel(lFace,  1, imgFa, Point(0, 0))
    spr:newCel(lRunes, 1, imgRu, Point(0, 0))
    spr:saveAs(spr.filename)

    local out = Image(60, 60, ColorMode.RGB)
    out:drawSprite(spr, 1, Point(0, 0))
    out:saveAs(EXP_DIR .. "/confirm_button_states.png")
    spr:close()
  end

  do
    local spr = Sprite(52, 54, ColorMode.RGB)
    spr.filename = SRC_DIR .. "/imprint_corner_buttons_v1.aseprite"
    local lBase = spr.layers[1]; lBase.name = "01_stone_button_body"
    local lGlow = spr:newLayer(); lGlow.name = "02_rune_and_active_glow"

    local imgB = Image(52, 54, ColorMode.RGB)
    local imgG = Image(52, 54, ColorMode.RGB)

    fillRect(imgB, 0, 0, 52, 18, hex("#3D3754"))
    strokeRect(imgB, 0, 0, 52, 18, hex("#0B0912"))
    fillRect(imgB, 2, 2, 48, 14, hex("#1E1930"))
    strokeRect(imgB, 2, 2, 48, 14, hex("#583899"))
    for px = 2, 49 do put(imgB, px, 1, hex("#685F8C")) end
    for ry = 5, 12, 2 do
      put(imgG, 5, ry, hex("#9D54FF")); put(imgG, 46, ry, hex("#9D54FF"))
    end

    fillRect(imgB, 0, 18, 52, 18, hex("#594E7D"))
    strokeRect(imgB, 0, 18, 52, 18, hex("#0E0A1A"))
    fillRect(imgB, 2, 20, 48, 14, hex("#2E224A"))
    strokeRect(imgB, 2, 20, 48, 14, hex("#A855F7"))
    for px = 2, 49 do put(imgB, px, 19, hex("#9B8CC4")) end
    drawRadialGlow(imgG, 26, 27, 20, 7, "#A84DFF", 55)
    for ry = 23, 30, 2 do
      put(imgG, 5, ry, hex("#E5A8FF")); put(imgG, 46, ry, hex("#E5A8FF"))
    end

    fillRect(imgB, 0, 36, 52, 18, hex("#6B2BD9"))
    strokeRect(imgB, 0, 36, 52, 18, hex("#2A0B52"))
    fillRect(imgB, 2, 38, 48, 14, hex("#2B1652"))
    strokeRect(imgG, 1, 37, 50, 16, hex("#C86BFF"))
    strokeRect(imgG, 2, 38, 48, 14, hex("#F7D9FF"))
    drawRadialGlow(imgG, 26, 45, 24, 8, "#B85CFF", 90)
    put(imgG, 2, 38, hex("#FFFFFF")); put(imgG, 49, 38, hex("#FFFFFF"))
    put(imgG, 2, 51, hex("#FFFFFF")); put(imgG, 49, 51, hex("#FFFFFF"))

    spr:newCel(lBase, 1, imgB, Point(0, 0))
    spr:newCel(lGlow, 1, imgG, Point(0, 0))
    spr:saveAs(spr.filename)

    local out = Image(52, 54, ColorMode.RGB)
    out:drawSprite(spr, 1, Point(0, 0))
    out:saveAs(EXP_DIR .. "/corner_button_states.png")
    spr:close()
  end
end

local function buildInventoryDrawer()
  local W, H = 178, 104
  local spr = Sprite(W, H, ColorMode.RGB)
  spr.filename = SRC_DIR .. "/imprint_widgets_and_drawer_v1.aseprite"

  local lAura   = spr.layers[1]; lAura.name   = "01_shadow_and_aura"
  local lFrame  = spr:newLayer(); lFrame.name  = "02_obsidian_carved_frame"
  local lCrest  = spr:newLayer(); lCrest.name  = "03_top_amethyst_crest"
  local lMain   = spr:newLayer(); lMain.name   = "04_main_slots_3x9"
  local lBeam   = spr:newLayer(); lBeam.name   = "05_divider_beam"
  local lHotbar = spr:newLayer(); lHotbar.name = "06_hotbar_slots_1x9"

  local imgAu = Image(W, H, ColorMode.RGB)
  fillRect(imgAu, 1, 5, W - 2, H - 6, hex("#07050D", 180))
  drawRadialGlow(imgAu, 89, 7, 20, 9, "#A84DFF", 75)
  spr:newCel(lAura, 1, imgAu, Point(0, 0))

  local imgFr = Image(W, H, ColorMode.RGB)
  fillRect(imgFr, 2, 5, W - 4, 93, hex("#241F36"))
  strokeRect(imgFr, 2, 5, W - 4, 93, hex("#0A0812"))
  for px = 4, W - 5 do
    put(imgFr, px, 6, hex("#5E5680"))
    put(imgFr, px, 7, hex("#413B59"))
    put(imgFr, px, 96, hex("#161322"))
    put(imgFr, px, 95, hex("#322C47"))
  end
  for py = 7, 95 do
    put(imgFr, 3, py, hex("#524B6E"))
    put(imgFr, 4, py, hex("#39334F"))
    put(imgFr, W - 4, py, hex("#171424"))
    put(imgFr, W - 5, py, hex("#2C2740"))
  end
  strokeRect(imgFr, 7, 13, 164, 78, hex("#0D0B16"))

  local cBoxes = { {0, 2}, {W - 12, 2}, {0, 89}, {W - 12, 89} }
  for _, cb in ipairs(cBoxes) do
    fillRect(imgFr, cb[1], cb[2], 12, 12, hex("#4E466E"))
    strokeRect(imgFr, cb[1], cb[2], 12, 12, hex("#0A0812"))
    for i = 1, 10 do
      put(imgFr, cb[1] + i, cb[2] + 1, hex("#877DB3"))
      put(imgFr, cb[1] + i, cb[2] + 10, hex("#231F33"))
    end
    fillRect(imgFr, cb[1] + 3, cb[2] + 3, 6, 6, hex("#2A1B47"))
    put(imgFr, cb[1] + 5, cb[2] + 4, hex("#E5A8FF")); put(imgFr, cb[1] + 6, cb[2] + 4, hex("#E5A8FF"))
    put(imgFr, cb[1] + 4, cb[2] + 5, hex("#C86BFF")); put(imgFr, cb[1] + 7, cb[2] + 5, hex("#C86BFF"))
    put(imgFr, cb[1] + 5, cb[2] + 5, hex("#FFFFFF")); put(imgFr, cb[1] + 6, cb[2] + 5, hex("#F7DCFF"))
    put(imgFr, cb[1] + 5, cb[2] + 6, hex("#F7DCFF")); put(imgFr, cb[1] + 6, cb[2] + 6, hex("#C86BFF"))
    put(imgFr, cb[1] + 4, cb[2] + 6, hex("#9D44FF")); put(imgFr, cb[1] + 7, cb[2] + 6, hex("#9D44FF"))
    put(imgFr, cb[1] + 5, cb[2] + 7, hex("#B85CFF")); put(imgFr, cb[1] + 6, cb[2] + 7, hex("#B85CFF"))
  end

  fillRect(imgFr, 74, 94, 30, 7, hex("#352E4D"))
  strokeRect(imgFr, 74, 94, 30, 7, hex("#0A0812"))
  for px = 76, 101 do put(imgFr, px, 95, hex("#594F7D")) end
  fillRect(imgFr, 85, 96, 8, 3, hex("#8F38E8"))
  put(imgFr, 88, 97, hex("#F5D6FF")); put(imgFr, 89, 97, hex("#FFFFFF"))
  spr:newCel(lFrame, 1, imgFr, Point(0, 0))

  local imgCr = Image(W, H, ColorMode.RGB)
  fillRect(imgCr, 56, 3, 66, 6, hex("#3D3657"))
  strokeRect(imgCr, 56, 3, 66, 4, hex("#0A0812"))
  for px = 57, 120 do put(imgCr, px, 4, hex("#685F8C")) end
  for dy = -6, 6 do
    local hw = 9 - math.floor(math.abs(dy) * 0.8)
    for dx = -hw, hw - 1 do
      local px = 89 + dx
      local py = 6 + dy
      if dx == -hw or dx == hw - 1 or dy == -6 or dy == 6 then
        put(imgCr, px, py, hex("#0B0912"))
      elseif dx == -hw + 1 or dy == -5 then
        put(imgCr, px, py, hex("#7E74A8"))
      else
        put(imgCr, px, py, hex("#2A2242"))
      end
    end
  end
  for dy = -3, 3 do
    local hw = 4 - math.abs(dy)
    for dx = -hw, hw do
      put(imgCr, 89 + dx, 6 + dy, (math.abs(dx) + math.abs(dy) <= 1) and hex("#FFFFFF") or hex("#C266FF"))
    end
  end
  spr:newCel(lCrest, 1, imgCr, Point(0, 0))

  local imgMa = Image(W, H, ColorMode.RGB)
  for row = 0, 2 do
    for col = 0, 8 do
      drawDarkSlot18(imgMa, 8 + col * 18, 14 + row * 18, nil)
    end
  end
  spr:newCel(lMain, 1, imgMa, Point(0, 0))

  local imgBe = Image(W, H, ColorMode.RGB)
  for px = 8, 169 do
    put(imgBe, px, 68, hex("#0D0B16"))
    put(imgBe, px, 69, hex("#4B436B"))
    put(imgBe, px, 70, hex("#2B2640"))
    put(imgBe, px, 71, hex("#0D0B16"))
  end
  for _, rx in ipairs({ 44, 89, 134 }) do
    put(imgBe, rx - 1, 69, hex("#A84DFF"))
    put(imgBe, rx,     69, hex("#F2C4FF"))
    put(imgBe, rx + 1, 69, hex("#A84DFF"))
  end
  spr:newCel(lBeam, 1, imgBe, Point(0, 0))

  local imgHo = Image(W, H, ColorMode.RGB)
  for col = 0, 8 do
    drawDarkSlot18(imgHo, 8 + col * 18, 72, "#6D28D9")
  end
  spr:newCel(lHotbar, 1, imgHo, Point(0, 0))

  spr:saveAs(spr.filename)
  local out = Image(W, H, ColorMode.RGB)
  out:drawSprite(spr, 1, Point(0, 0))
  out:saveAs(EXP_DIR .. "/inventory_drawer.png")
  spr:close()
end

local function buildTreeNodes()
  local W, H = 192, 56
  local spr = Sprite(W, H, ColorMode.RGB)
  spr.filename = SRC_DIR .. "/imprint_tree_nodes_v1.aseprite"

  local lLip    = spr.layers[1]; lLip.name    = "01_tile_3d_bottom_lip"
  local lBody   = spr:newLayer(); lBody.name   = "02_tile_elemental_body"
  local lBevel  = spr:newLayer(); lBevel.name  = "03_tile_chiseled_bevel"
  local lFrames = spr:newLayer(); lFrames.name = "04_state_frames_26x26"
  local lBadges = spr:newLayer(); lBadges.name = "05_corner_badges_and_pips"

  local imgL = Image(W, H, ColorMode.RGB)
  local imgB = Image(W, H, ColorMode.RGB)
  local imgV = Image(W, H, ColorMode.RGB)
  local imgF = Image(W, H, ColorMode.RGB)
  local imgD = Image(W, H, ColorMode.RGB)

  local tiles = {
    { out="#10240C", lip="#234F1B", deep="#387A2B", main="#4E9E3E", hi="#84DB67", spec="#BAFF9E" },
    { out="#2E0F03", lip="#6E2808", deep="#AB4910", main="#D96B1B", hi="#FFA845", spec="#FFE09E" },
    { out="#071930", lip="#123966", deep="#1D5B9E", main="#2878C8", hi="#66C2FF", spec="#BFF0FF" },
    { out="#1F1105", lip="#47280F", deep="#6E411B", main="#8C5828", hi="#C48B52", spec="#E8BE8E" },
    { out="#291C06", lip="#614412", deep="#996F22", main="#C89B3C", hi="#FFE07D", spec="#FFF7CC" },
    { out="#150E24", lip="#32234A", deep="#4E3B70", main="#68528C", hi="#A48AD4", spec="#DECFFF" },
    { out="#241606", lip="#523411", deep="#805622", main="#A67838", hi="#E6B667", spec="#FFF0B8", rule=true },
    { out="#0B0C12", lip="#161721", deep="#20212E", main="#2A2B36", hi="#4B4D61", spec="#6E718A", locked=true },
  }

  for idx, t in ipairs(tiles) do
    local ox = (idx - 1) * 24
    local oy = 0
    for y = 0, 21 do
      for x = 0, 21 do
        local cornerCut = ((x == 0 or x == 21) and (y == 0 or y == 21))
        if not cornerCut then
          local isOuter = (x == 0 or x == 21 or y == 0 or y == 21 or
            ((x == 1 or x == 20) and (y == 1 or y == 20)))
          if isOuter then
            put(imgL, ox + x, oy + y, hex(t.out))
          elseif y >= 19 then
            put(imgL, ox + x, oy + y, hex(t.lip))
          elseif y == 1 or (x == 1 and y <= 17) then
            put(imgV, ox + x, oy + y, (x <= 4 and y <= 4) and hex(t.spec) or hex(t.hi))
          elseif x == 20 or y == 18 then
            put(imgV, ox + x, oy + y, hex(t.deep))
          else
            local inIconRim = (x == 2 or x == 19 or y == 2 or y == 17)
            if inIconRim then
              put(imgB, ox + x, oy + y, mixHex(t.main, t.hi, (y == 2 or x == 2) and 0.35 or 0.0))
            else
              local n = brickNoise(ox + x, oy + y)
              local c = (n > 0.78) and mixHex(t.main, t.hi, 0.22)
                     or (n < 0.20) and mixHex(t.main, t.deep, 0.35)
                     or hex(t.main)
              put(imgB, ox + x, oy + y, c)
            end
          end
        end
      end
    end
    if t.rule then
      put(imgV, ox + 3, oy + 3, hex(t.spec))
      put(imgV, ox + 18, oy + 3, hex(t.spec))
      put(imgV, ox + 3, oy + 16, hex(t.hi))
      put(imgV, ox + 18, oy + 16, hex(t.hi))
    end
  end

  local function drawFrame26(ox, oy, cOuter, cMain, cInner, cClasps, glowHex)
    if glowHex then
      for y = 0, 25 do
        for x = 0, 25 do
          if x <= 2 or x >= 23 or y <= 2 or y >= 23 then
            put(imgF, ox + x, oy + y, hex(glowHex, 95))
          end
        end
      end
    end
    for i = 2, 23 do
      put(imgF, ox + i, oy + 0, hex(cOuter)); put(imgF, ox + i, oy + 25, hex(cOuter))
      put(imgF, ox + 0, oy + i, hex(cOuter)); put(imgF, ox + 25, oy + i, hex(cOuter))
      put(imgF, ox + i, oy + 1, hex(cMain));  put(imgF, ox + i, oy + 24, hex(cMain))
      put(imgF, ox + 1, oy + i, hex(cMain));  put(imgF, ox + 24, oy + i, hex(cMain))
    end
    for i = 3, 22 do
      put(imgF, ox + i, oy + 2, hex(cInner)); put(imgF, ox + i, oy + 23, hex(cInner))
      put(imgF, ox + 2, oy + i, hex(cInner)); put(imgF, ox + 23, oy + i, hex(cInner))
    end
    local cs = { {1,1}, {21,1}, {1,21}, {21,21} }
    for _, c in ipairs(cs) do
      fillRect(imgF, ox + c[1], oy + c[2], 4, 4, hex(cMain))
      put(imgF, ox + c[1] + 1, oy + c[2] + 1, hex(cClasps))
      put(imgF, ox + c[1] + 2, oy + c[2] + 2, hex(cClasps))
    end
  end

  drawFrame26(0, 26, "#8F5A0A", "#FFD24C", "#FFF6CC", "#FFFFFF", "#FFB82E")
  drawFrame26(28, 26, "#5B189E", "#BD5CFF", "#F5D4FF", "#70FFF5", "#B85CFF")
  put(imgF, 28 + 12, 26 + 1, hex("#70FFF5")); put(imgF, 28 + 13, 26 + 1, hex("#FFFFFF"))
  put(imgF, 28 + 12, 26 + 24, hex("#70FFF5")); put(imgF, 28 + 13, 26 + 24, hex("#FFFFFF"))
  put(imgF, 28 + 1, 26 + 12, hex("#70FFF5")); put(imgF, 28 + 24, 26 + 12, hex("#70FFF5"))
  drawFrame26(56, 26, "#4A5878", "#DCE8FF", "#FFFFFF", "#FFFFFF", nil)

  do
    local bx, by = 88, 26
    for y = 0, 9 do
      for x = 0, 9 do
        local s = (x - 4.5) - (y - 4.5)
        local t = (x - 4.5) + (y - 4.5)
        if math.abs(s) <= 5.5 and math.abs(t) <= 3.2 then
          if math.abs(s) >= 4.5 or math.abs(t) >= 2.3 then
            put(imgD, bx + x, by + y, hex("#361F0A"))
          elseif s >= 3.0 then
            put(imgD, bx + x, by + y, hex("#FFF4BA"))
          else
            put(imgD, bx + x, by + y, (t < 0) and hex("#F2CC78") or hex("#C7913A"))
          end
        end
      end
    end
    put(imgD, bx + 4, by + 4, hex("#B85CFF"))
    put(imgD, bx + 5, by + 5, hex("#70FFF5"))
    put(imgD, bx + 4, by + 5, hex("#FFFFFF"))
  end

  do
    local lx, ly = 102, 26
    fillRect(imgD, lx + 1, ly + 0, 6, 4, hex("#0D0E14"))
    put(imgD, lx + 2, ly + 1, hex("#B8BED4")); put(imgD, lx + 3, ly + 0, hex("#DCE2F5"))
    put(imgD, lx + 4, ly + 0, hex("#B8BED4")); put(imgD, lx + 5, ly + 1, hex("#8C92A8"))
    put(imgD, lx + 2, ly + 2, hex("#9AA0B8")); put(imgD, lx + 5, ly + 2, hex("#757B91"))
    fillRect(imgD, lx + 3, ly + 1, 2, 2, rgba(0, 0, 0, 0))
    fillRect(imgD, lx + 0, ly + 3, 8, 6, hex("#0D0E14"))
    fillRect(imgD, lx + 1, ly + 4, 6, 4, hex("#5A5F75"))
    for px = lx + 1, lx + 6 do put(imgD, px, ly + 4, hex("#8E95B0")) end
    put(imgD, lx + 3, ly + 5, hex("#161721")); put(imgD, lx + 4, ly + 5, hex("#161721"))
    put(imgD, lx + 3, ly + 6, hex("#161721"))
  end

  do
    local px, py = 114, 26
    fillRect(imgD, px + 1, py + 0, 6, 8, hex("#10121C"))
    fillRect(imgD, px + 0, py + 1, 8, 6, hex("#10121C"))
    fillRect(imgD, px + 1, py + 1, 6, 5, hex("#4E5670"))
    for x = 1, 6 do put(imgD, px + x, py + 1, hex("#8591B5")) end
    fillRect(imgD, px + 2, py + 2, 4, 3, hex("#363D52"))
  end

  spr:newCel(lLip,    1, imgL, Point(0, 0))
  spr:newCel(lBody,   1, imgB, Point(0, 0))
  spr:newCel(lBevel,  1, imgV, Point(0, 0))
  spr:newCel(lFrames, 1, imgF, Point(0, 0))
  spr:newCel(lBadges, 1, imgD, Point(0, 0))

  spr:saveAs(spr.filename)
  local out = Image(W, H, ColorMode.RGB)
  out:drawSprite(spr, 1, Point(0, 0))
  out:saveAs(EXP_DIR .. "/tree_nodes_sheet.png")
  spr:close()
end

local function buildScreenAndTooltip9Slice()
  local spr = Sprite(48, 24, ColorMode.RGB)
  spr.filename = SRC_DIR .. "/imprint_screen_border_v1.aseprite"
  local lBorder  = spr.layers[1]; lBorder.name  = "01_screen_steel_border_9slice"
  local lTooltip = spr:newLayer(); lTooltip.name = "02_arcane_tooltip_9slice"

  local imgS = Image(48, 24, ColorMode.RGB)
  strokeRect(imgS, 0, 0, 24, 24, hex("#0A0C12"))
  strokeRect(imgS, 1, 1, 22, 22, hex("#A6B0C4"))
  strokeRect(imgS, 2, 2, 20, 20, hex("#687185"))
  strokeRect(imgS, 3, 3, 18, 18, hex("#1D212E"))
  local sc = { {1,1}, {17,1}, {1,17}, {17,17} }
  for _, c in ipairs(sc) do
    fillRect(imgS, c[1], c[2], 6, 6, hex("#7D879C"))
    strokeRect(imgS, c[1], c[2], 6, 6, hex("#232836"))
    for x = c[1] + 1, c[1] + 4 do put(imgS, x, c[2] + 1, hex("#BAC4D9")) end
    put(imgS, c[1] + 2, c[2] + 2, hex("#252A38"))
    put(imgS, c[1] + 3, c[2] + 3, hex("#4E566B"))
  end
  fillRect(imgS, 7, 4, 10, 16, rgba(0, 0, 0, 0))
  fillRect(imgS, 4, 7, 16, 10, rgba(0, 0, 0, 0))

  local imgT = Image(48, 24, ColorMode.RGB)
  local ox = 24
  fillRect(imgT, ox + 1, 1, 22, 22, hex("#100D1C", 236))
  for i = 2, 21 do
    put(imgT, ox + i, 0, hex("#6B2BD9", 210)); put(imgT, ox + i, 23, hex("#6B2BD9", 210))
    put(imgT, ox + 0, i, hex("#6B2BD9", 210)); put(imgT, ox + 23, i, hex("#6B2BD9", 210))
    put(imgT, ox + i, 1, hex("#B85CFF"));      put(imgT, ox + i, 22, hex("#9D44FF"))
    put(imgT, ox + 1, i, hex("#B85CFF"));      put(imgT, ox + 22, i, hex("#9D44FF"))
  end
  strokeRect(imgT, ox + 2, 2, 20, 20, hex("#E5A8FF", 120))
  put(imgT, ox + 2, 2, hex("#FFFFFF")); put(imgT, ox + 21, 2, hex("#FFFFFF"))
  put(imgT, ox + 2, 21, hex("#E5A8FF")); put(imgT, ox + 21, 21, hex("#E5A8FF"))

  spr:newCel(lBorder,  1, imgS, Point(0, 0))
  spr:newCel(lTooltip, 1, imgT, Point(0, 0))
  spr:saveAs(spr.filename)

  local outS = Image(24, 24, ColorMode.RGB)
  outS:drawImage(imgS, Point(0, 0))
  outS:saveAs(EXP_DIR .. "/screen_border_9slice.png")

  local outT = Image(24, 24, ColorMode.RGB)
  outT:drawImage(imgT, Point(-24, 0))
  outT:saveAs(EXP_DIR .. "/tooltip_frame_9slice.png")

  spr:close()
end

buildAltarHub()
buildStudySlips()
buildAltarWidgets()
buildButtons()
buildInventoryDrawer()
buildTreeNodes()
buildScreenAndTooltip9Slice()

print("ALL_ASEPRITE_ASSETS_GENERATED_OK")
