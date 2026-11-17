local out='D:/alianximod/docs/资源库/过程归档/拔刀斩_v2-20261102/'
local reports={}
for _,view in ipairs({'front','side'}) do
 local dir=out..'frames-'..view..'/'
 local first=Image{fromFile=dir..'frame-000.png'}
 local s=Sprite(first.width,first.height,ColorMode.RGB)
 s.layers[1].name='真实Blockbench视口'
 s:newCel(s.layers[1],1,first,Point(0,0))
 s.frames[1].duration=.20
 for i=1,80 do
  local frame=s:newEmptyFrame()
  local im=Image{fromFile=dir..string.format('frame-%03d.png',i)}
  s:newCel(s.layers[1],frame,im,Point(0,0))
  frame.duration=i==80 and .60 or .02
 end
 assert(#s.frames==81,'Expected 81 native capture frames')
 s.data='Native Blockbench viewport capture; single 1.6s animation, 50fps capture, 0.2s initial and 0.6s final preview holds; presentation loop only'
 s:saveAs(out..'iaido-'..view..'-preview.aseprite')
 s:saveAs(out..'iaido-'..view..'-preview.gif')
 reports[#reports+1]={view=view,frames=#s.frames,width=s.width,height=s.height,previewDuration=2.38,animationDuration=1.6}
 s:close()
end
local dir=out..'frames-front/'
local first=Image{fromFile=dir..'frame-016.png'}
local slow=Sprite(first.width,first.height,ColorMode.RGB)
slow.layers[1].name='真实Blockbench视口_四分之一速度'
slow:newCel(slow.layers[1],1,first,Point(0,0))
slow.frames[1].duration=.20
for i=17,34 do
 local frame=slow:newEmptyFrame()
 slow:newCel(slow.layers[1],frame,Image{fromFile=dir..string.format('frame-%03d.png',i)},Point(0,0))
 frame.duration=i==34 and .4 or .08
end
slow:saveAs(out..'iaido-transition-slow.gif')
slow:close()
return reports
