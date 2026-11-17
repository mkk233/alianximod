local root=args.root
local base='D:/alianximod/src/main/resources/assets/xueximod/textures/effect/'
local entries={
 {'liaoli_v6/sword_unlit.png',20,1},
 {'liaoli_v6/sword_hot.png',20,1},
 {'liaoli_v6/rear_ring.png',96,6},
 {'liaoyuan_v3/ground_ash.png',16,1},
 {'liaoyuan_v3/ground_ash_b.png',16,1},
 {'liaoyuan_v3/ground_ash_c.png',16,1},
 {'liaoyuan_v3/ground_ash_d.png',16,1},
 {'liaoyuan_v3/ground_embers.png',16,6},
 {'surface_anchor_v2/surface_anchor.png',64,6},
 {'ground_dust_v1/ground_dust.png',64,6},
 {'furnace_v3/loop_torso.png',112,6},
 {'furnace_v3/loop_warmth.png',112,6},
 {'furnace_v3/loop_hand_front.png',112,6},
 {'burn_spatial_v1/loop/cape.png',112,6},
 {'burn_spatial_v1/loop/hand.png',112,6},
 {'burn_spatial_v1/loop/body_embers.png',112,6}
}
local rgba=app.pixelColor.rgba
local board=Image(768,384,ColorMode.RGB)
board:clear(rgba(30,37,46,255))
local digits={
 ['0']={'111','101','101','101','111'},['1']={'010','110','010','010','111'},
 ['2']={'110','001','010','100','111'},['3']={'110','001','010','001','110'},
 ['4']={'101','101','111','001','001'},['5']={'111','100','110','001','110'},
 ['6']={'011','100','110','101','010'},['7']={'111','001','010','010','010'},
 ['8']={'010','101','010','101','010'},['9']={'010','101','011','001','110'}
}
local records={}
for index,entry in ipairs(entries) do
 local atlas=Image{fromFile=base..entry[1]}
 local frame=Image(atlas.width,entry[2],ColorMode.RGB)
 frame:drawImage(atlas,Point(0,-(entry[3]-1)*entry[2]))
 local scale=math.min(112/frame.width,100/frame.height)
 frame:resize(math.floor(frame.width*scale),math.floor(frame.height*scale))
 local left=((index-1)%6)*128
 local top=math.floor((index-1)/6)*128
 board:drawImage(frame,Point(left+math.floor((128-frame.width)/2),top+20+math.floor((100-frame.height)/2)))
 for position=1,#tostring(index) do
  local glyph=digits[tostring(index):sub(position,position)]
  for row,line in ipairs(glyph) do for column=1,#line do
   if line:sub(column,column)=='1' then board:drawPixel(left+8+(position-1)*4+column,top+6+row,rgba(220,231,235,255)) end
  end end
 end
 local colors={}
 for pixel in atlas:pixels() do
  local value=pixel()
  if app.pixelColor.rgbaA(value)>0 then
   local key=string.format('#%02X%02X%02X',app.pixelColor.rgbaR(value),app.pixelColor.rgbaG(value),app.pixelColor.rgbaB(value))
   colors[key]=(colors[key] or 0)+1
  end
 end
 records[#records+1]={index=index,file=entry[1],width=atlas.width,height=atlas.height,frameHeight=entry[2],shownFrame=entry[3],colors=colors}
end
board:saveAs(root..'/references/source_material_inspection.png')
local output=io.open(root..'/evidence/source-material-inspection.json','w')
output:write(json.encode(records))
output:close()
return {inspected=#records,preview=root..'/references/source_material_inspection.png'}
