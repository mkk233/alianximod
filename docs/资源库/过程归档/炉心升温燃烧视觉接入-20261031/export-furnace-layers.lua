-- Read approved furnace sprites and write per-layer vertical strips.
-- Does not save or modify the source files. No preview, no combined sheet.
local OUT = "D:/alianximod/src/main/resources/assets/xueximod/textures/effect/furnace_v2/"
local LOG = "D:/alianximod/docs/资源库/过程归档/炉心升温燃烧视觉接入-20261031/export-log.txt"
local jobs = {
  {
    source = "D:/alianximod/docs/资源库/特效/火焰/升温_贴身炽热_v2/heat_up-v2.aseprite",
    names = { "heat_rear.png", "heat_rear_tongues.png", "heat_front_tongues.png", "heat_embers.png" },
  },
  {
    source = "D:/alianximod/docs/资源库/特效/火焰/升温_贴身炽热_v2/heat_up-v2-entry.aseprite",
    names = { "heat_entry_rear.png", "heat_entry_rear_tongues.png", "heat_entry_front_tongues.png", "heat_entry_embers.png" },
  },
  {
    source = "D:/alianximod/docs/资源库/特效/火焰/燃烧_双手附焰与火焰披风_v3/overdrive-v3-entry.aseprite",
    names = {
      "burn_entry_cape.png",
      "burn_entry_rear_fire.png",
      "burn_entry_body_fire.png",
      "burn_entry_hand_fire.png",
      "burn_entry_embers.png",
    },
  },
}

local lines = {}
local function note(text)
  lines[#lines + 1] = text
  print(text)
end

for _, job in ipairs(jobs) do
  local sprite = app.open(job.source)
  if not sprite then
    error("cannot open " .. job.source)
  end
  local layerCount = #sprite.layers
  if layerCount ~= #job.names then
    error(job.source .. " layer count " .. layerCount .. " != " .. #job.names)
  end
  local frameCount = #sprite.frames
  local width = sprite.width
  local height = sprite.height
  local durations = {}
  for frameIndex = 1, frameCount do
    durations[#durations + 1] = string.format("%.5f", sprite.frames[frameIndex].duration)
  end
  note(job.source)
  note("size " .. width .. "x" .. height .. " frames " .. frameCount .. " durations " .. table.concat(durations, ","))
  for layerIndex, layer in ipairs(sprite.layers) do
    note("layer " .. layerIndex .. " " .. layer.name .. " -> " .. job.names[layerIndex])
    local strip = Image(width, height * frameCount, ColorMode.RGB)
    strip:clear()
    for frameIndex = 1, frameCount do
      local cel = layer:cel(frameIndex)
      if cel and cel.image then
        strip:drawImage(cel.image, Point(cel.position.x, (frameIndex - 1) * height + cel.position.y))
      end
    end
    strip:saveAs(OUT .. job.names[layerIndex])
  end
  sprite:close()
end

local logFile = io.open(LOG, "w")
logFile:write(table.concat(lines, "\n"))
logFile:write("\n")
logFile:close()
