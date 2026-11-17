local root=args.root
local rgba=app.pixelColor.rgba
local w,h,n=32,40,24
local original=Image{fromFile=root..'/references/liaoyuan_ground_flame_original.png'}
local old=Image{fromFile=root..'/textures/liaoyuan_ground_flame_legacy.png'}
local blue=Image{fromFile=root..'/textures/liaoyuan_ground_flame_blue.png'}
local ref=Image{fromFile=root..'/references/nether_gloom_v2_runtime.png'}
local function rect(im,x,y,w,h,c) for yy=y,y+h-1 do for xx=x,x+w-1 do im:drawPixel(xx,yy,c) end end end
local s=Sprite(352,112,ColorMode.RGB);s.layers[1].name='AUX_actual_ground_flame_target_color_comparison'
local titles=Image{fromFile=root..'/previews/kindle_v1_comparison_native.png'}
for f=1,n do
 if f>1 then s:newEmptyFrame(f) end;s.frames[f].duration=.05
 local im=Image(352,112,ColorMode.RGB);im:clear(rgba(31,38,47,255))
 local header=Image(352,16,ColorMode.RGB);header:drawImage(titles);im:drawImage(header)
 for c=1,4 do
  local x=(c-1)*88
  if c<4 then
   rect(im,x+38,26,12,14,rgba(184,149,117,255));rect(im,x+38,26,12,4,rgba(65,47,37,255))
   rect(im,x+37,40,14,23,rgba(53,113,119,255));rect(im,x+31,40,6,24,rgba(53,113,119,255));rect(im,x+51,40,6,24,rgba(53,113,119,255))
   rect(im,x+37,63,7,27,rgba(65,72,103,255));rect(im,x+44,63,7,27,rgba(65,72,103,255))
   local frame=Image(w,h,ColorMode.RGB);frame:drawImage(({original,old,blue})[c],Point(0,-(f-1)*h));im:drawImage(frame,Point(x+28,50))
  else
   local frame=Image(64,64,ColorMode.RGB);frame:drawImage(ref,Point(0,-(math.floor((f-1)*50/80)%16)*64));im:drawImage(frame,Point(x+12,26))
  end
  rect(im,x+6,94,76,1,rgba(61,77,86,255));rect(im,x+16,101,56,2,({rgba(255,193,41,255),rgba(90,160,255,255),rgba(14,196,204,255),rgba(68,237,231,255)})[c])
 end
 s:newCel(s.layers[1],f,im,Point(0,0))
 if f==6 then im:saveAs(root..'/previews/attached_flame_comparison_native.png');im:resize(1056,336);im:saveAs(root..'/previews/attached_flame_comparison.png') end
end
s:saveAs(root..'/source/review_attached_flame.aseprite');s:saveAs(root..'/previews/attached_flame_comparison_native.gif')
app.sprite=s;app.command.SpriteSize{ui=false,width=1056,height=336,method='nearest'};s:saveAs(root..'/previews/attached_flame_comparison.gif')
return {source='review_attached_flame.aseprite',frames=24,frameMs=50,status='candidate only; entity placement is comparison layout'}
