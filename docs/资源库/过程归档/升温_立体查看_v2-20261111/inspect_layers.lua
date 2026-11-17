local root="D:/alianximod/src/main/resources/assets/xueximod/textures/effect/furnace_v2/"
local out="D:/alianximod/docs/资源库/过程归档/升温_立体查看_v2-20261111/"
local names={"heat_rear","heat_rear_tongues","heat_front_tongues","heat_embers"}
local board=Image(96*4,112*3)
board:clear(Color{r=24,g=30,b=39,a=255})
for c,name in ipairs(names) do
  local strip=Image{fromFile=root..name..".png"}
  print(name..": "..strip.width.."x"..strip.height)
  for r,f in ipairs({1,7,14}) do
    local frame=Image(strip,Rectangle(0,(f-1)*112,96,112))
    board:drawImage(frame,Point((c-1)*96,(r-1)*112))
  end
end
board:resize(board.width*2,board.height*2)
board:saveAs(out.."runtime_layers.png")
