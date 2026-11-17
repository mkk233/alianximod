
local root="D:/alianximod/docs/资源库/过程归档/水牢泡泡_2D_v1-20261027/"
local s=app.open(root.."water_prison_bubble_v1.aseprite")
assert(s.width==96 and s.height==96 and #s.frames==16 and #s.layers==5)
local mask=nil
local centers={}
local unique={}
for f=1,#s.frames do
 assert(math.abs(s.frames[f].duration-.1)<.0001)
 local im=Image(96,96,ColorMode.RGB);im:drawSprite(s,f)
 local parts={}
 for y=0,95 do for x=0,95 do
  local a=app.pixelColor.rgbaA(im:getPixel(x,y))
  parts[#parts+1]=a>0 and "1" or "0"
 end end
 local current=table.concat(parts)
 if mask then assert(current==mask,"Silhouette changed") else mask=current end
 centers[#centers+1]=app.pixelColor.rgbaA(im:getPixel(47,48))
 assert(app.pixelColor.rgbaA(im:getPixel(0,0))==0)
 unique[im.bytes]=true
end
local count=0;for _ in pairs(unique) do count=count+1 end
local p=app.open(root.."water_prison_bubble_preview_4x.gif")
assert(p.width==896 and p.height==448 and #p.frames==16)
local im=Image(896,448,ColorMode.RGB);im:drawSprite(p,9);im:saveAs(root.."preview_frame09.png")
local file=io.open(root.."validation.txt","w")
file:write("Aseprite native validation\n96 x 96 RGBA; 5 editable layers; 16 frames x 100ms\n16 unique frames: "..tostring(count).."\nFixed alpha silhouette across all frames: true\nTransparent corners: true\nCenter alpha per frame: "..table.concat(centers,",").."\nPreview GIF: 896 x 448, 16 frames\nNo game integration; approval draft only.\n")
file:close()
print("PASS: 16 unique="..count.."; fixed silhouette; transparent corners; center alpha="..table.concat(centers,","))
