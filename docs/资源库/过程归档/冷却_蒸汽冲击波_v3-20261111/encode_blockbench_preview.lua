local out=args.out
local s=Sprite(720,560,ColorMode.RGB)
s.layers[1].name="Actual Blockbench viewport captures"
for i=1,16 do
  local frame=i==1 and s.frames[1]or s:newEmptyFrame()
  frame.duration=0.075
  local im=Image{fromFile=out..string.format("/blockbench_frames/frame_%02d.png",i)}
  assert(im.width==720 and im.height==560,"Unexpected viewport size")
  local cel=s.layers[1]:cel(frame)
  if cel then cel.image=im else s:newCel(s.layers[1],frame,im,Point(0,0))end
end
s:saveAs(out.."/cool_v3_spatial_preview.aseprite")
for i,frame in ipairs(s.frames)do frame.duration=i==16 and 0.45 or(i%2==1 and 0.07 or 0.08)end
s:saveAs(out.."/cool_v3_spatial_preview.gif")
local board=Image(1080,560,ColorMode.RGB)
for k,fi in ipairs({2,4,6,8,10,13})do
  local im=Image(s.layers[1]:cel(fi).image)
  im:resize{width=360,height=280,method="nearest"}
  board:drawImage(im,Point(((k-1)%3)*360,math.floor((k-1)/3)*280))
end
board:saveAs(out.."/cool_v3_spatial_keyframes.png")
local flat=app.open(out.."/cool_v3_preview.aseprite")
for i,frame in ipairs(flat.frames)do frame.duration=i==16 and 0.45 or(i%2==1 and 0.07 or 0.08)end
app.sprite=flat
app.command.SpriteSize{ui=false,width=1008,height=464,method="nearest"}
flat:saveAs(out.."/cool_v3_preview.gif")
return{frames=16,sourceDurationMs=1200,gifLoopMs=1570,
 actualViewportFrames=out.."/blockbench_frames",gif=out.."/cool_v3_spatial_preview.gif"}
