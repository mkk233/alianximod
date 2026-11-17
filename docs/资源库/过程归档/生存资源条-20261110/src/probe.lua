local f = assert(io.open("D:/alianximod/docs/资源库/过程归档/生存资源条-20261110/probe-out.txt", "w"))
f:write("version=" .. tostring(app.version) .. "\n")
local function dump(name, value, depth)
  depth = depth or 0
  if depth > 2 then
    f:write(string.rep(" ", depth) .. name .. "=<deep>\n")
    return
  end
  local kind = type(value)
  f:write(string.rep(" ", depth) .. name .. "=" .. kind)
  if kind ~= "table" then
    f:write(":" .. tostring(value) .. "\n")
    return
  end
  f:write("\n")
  for key, child in pairs(value) do
    dump(tostring(key), child, depth + 1)
  end
end
dump("params", app.params)
f:close()
