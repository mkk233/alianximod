local spr = sprite
local rgba = app.pixelColor.rgba
local red=rgba(216,52,20,255)
local orange=rgba(246,94,18,255)
local amber=rgba(255,149,24,255)
local gold=rgba(255,202,50,255)
local yellow=rgba(255,232,109,255)
local cream=rgba(255,246,197,255)
local function polygon(img, pts, col)
  for y=0,31 do
    for x=0,31 do
      local inside=false
      local j=#pts
      for i=1,#pts do
        local xi,yi=pts[i][1],pts[i][2]
        local xj,yj=pts[j][1],pts[j][2]
        if ((yi>y+0.5) ~= (yj>y+0.5)) and (x+0.5 < (xj-xi)*(y+0.5-yi)/(yj-yi)+xi) then inside=not inside end
        j=i
      end
      if inside then img:drawPixel(x,y,col) end
    end
  end
end
app.transaction("Paint fire seed flame loop",function()
 local core=spr:newLayer()
 core.name="Golden core"
 local spark=spr:newLayer()
 spark.name="Rising ember"
 for f=1,8 do
  local phase=(f-1)*math.pi/4
  local sway=math.floor(2*math.sin(phase)+0.5)
  local lift=math.floor(math.sin(phase+0.8)+0.5)
  local img=Image(32,32,ColorMode.RGB)
  polygon(img,{{16+sway,3+lift},{15+sway,8},{19+sway,12},{20,16},{23-sway,12},{25,17},{26,22},{24,26},{21,29},{12,30},{8,28},{5,24},{6,19},{9+sway,15},{8,21},{12,17},{13+sway,11},{14+sway,7}},red)
  polygon(img,{{16+sway,6+lift},{16+sway,10},{19,14},{20,19},{23,15},{24,21},{22,26},{19,28},{12,28},{8,25},{7,21},{10,23},{14,17},{14+sway,12}},orange)
  polygon(img,{{15+sway,10+lift},{17+sway,14},{17,19},{21,18},{22,22},{20,26},{17,28},{11,27},{9,24},{12,22},{14,18}},amber)
  spr:newCel(spr.layers[1],f,img,Point(0,0))
  local ci=Image(32,32,ColorMode.RGB)
  polygon(ci,{{16+sway,15+lift},{17,20},{20,21},{19,25},{17,27},{13,27},{11,25},{13,22},{14,19}},gold)
  polygon(ci,{{15,19+lift},{16,22},{18,23},{17,26},{14,27},{12,25},{14,23}},yellow)
  polygon(ci,{{15,22},{16,24},{16,26},{14,26},{14,24}},cream)
  spr:newCel(core,f,ci,Point(0,0))
  local si=Image(32,32,ColorMode.RGB)
  if f<=5 then
    local sy=7-f
    local sx=23+math.floor(math.sin(phase))
    si:drawPixel(sx,sy,gold)
    if f<4 then si:drawPixel(sx,sy+1,amber) end
  elseif f>=7 then
    si:drawPixel(8,9-(f-7)*2,amber)
  end
  spr:newCel(spark,f,si,Point(0,0))
  spr.frames[f].duration=0.1
 end
 local tag=spr:newTag(1,8)
 tag.name="Ember loop"
end)
spr:saveAs(args.path)
return {path=spr.filename,width=spr.width,height=spr.height,frames=#spr.frames,layers=#spr.layers}
