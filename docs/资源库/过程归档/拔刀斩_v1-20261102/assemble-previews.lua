local out='D:/alianximod/docs/资源库/过程归档/拔刀斩_v1-20261102/'
local reports={}
for _,view in ipairs({'front','side'}) do
 local dir=out..'frames-'..view..'/'
 local first=Image{fromFile=dir..'frame-000.png'}
 local s=Sprite(first.width,first.height,ColorMode.RGB)
 s.layers[1].name='真实Blockbench视口'
 s:newCel(s.layers[1],1,first,Point(0,0))
 s.frames[1].duration=.20
 for i=1,40 do
  local frame=s:newEmptyFrame()
  local im=Image{fromFile=dir..string.format('frame-%03d.png',i)}
  s:newCel(s.layers[1],frame,im,Point(0,0))
  frame.duration=i==40 and .60 or .04
 end
 assert(#s.frames==41,'Expected 41 native capture frames')
 s.data='Native Blockbench viewport capture; single 1.6s animation, 25fps capture, 0.2s initial and 0.6s final preview holds; presentation loop only'
 s:saveAs(out..'iaido-'..view..'-preview.aseprite')
 s:saveAs(out..'iaido-'..view..'-preview.gif')
 reports[#reports+1]={view=view,frames=#s.frames,width=s.width,height=s.height,previewDuration=2.36,animationDuration=1.6}
 s:close()
end
return reports
