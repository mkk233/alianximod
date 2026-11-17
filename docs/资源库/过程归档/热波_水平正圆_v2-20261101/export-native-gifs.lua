local out='D:/alianximod/docs/资源库/过程归档/热波_水平正圆_v2-20261101/'
for _,view in ipairs({'top','oblique'}) do
 local s=Sprite(900,900,ColorMode.RGB);s.layers[1].name='Actual Blockbench viewport'
 for i=0,16 do
  if i>0 then s:newEmptyFrame(i+1) end
  s.frames[i+1].duration=i==16 and .4 or ((i%4==2) and .03 or .04)
  local im=Image{fromFile=out..'bb-frames-'..view..'/frame-'..string.format('%03d',i)..'.png'}
  s:newCel(s.layers[1],i+1,im,Point(0,0))
 end
 s:saveAs(out..'bb-'..view..'-animation.gif');s:close()
end
return {views=2,framesPerPreview=17,effectDuration=.6,previewPause=.4}
