local base='D:/alianximod/docs/资源库/过程归档/周身水环3D验证_v8_细碎水膜毛边-20261027/'
local out='D:/alianximod/docs/资源库/过程归档/水环接入-20261027/'
local atlas=Image(768,640,ColorMode.RGB)
local files={'broken_saw_flow.png','ragged_splash_outer.png','ragged_splash_inner.png'}
local alpha={}
for i,name in ipairs(files) do
 local im=Image{fromFile=base..name}
 atlas:drawImage(im,Point((i-1)*256,0))
 for it in im:pixels() do local a=app.pixelColor.rgbaA(it());alpha[tostring(a)]=true end
end
atlas:saveAs(out..'water_ring_v8_atlas.png')
return {width=atlas.width,height=atlas.height,alphaValues=alpha}

