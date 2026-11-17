local src=sprite
assert(src and src.width==64 and src.height==64 and #src.frames==16,"Unexpected v1 source")
local out=args.output
local pc=app.pixelColor
local bg1=pc.rgba(32,43,58,255)
local bg2=pc.rgba(42,55,72,255)
local border=pc.rgba(23,33,46,255)
local function checker(w,h,tile)
  local im=Image(w,h,ColorMode.RGB)
  for y=0,h-1 do
    for x=0,w-1 do
      im:putPixel(x,y,((math.floor(x/tile)+math.floor(y/tile))%2==0) and bg1 or bg2)
    end
  end
  return im
end
local function nearest(im,scale)
  local result=Image(im.width*scale,im.height*scale,ColorMode.RGB)
  for y=0,result.height-1 do
    for x=0,result.width-1 do
      result:putPixel(x,y,im:getPixel(math.floor(x/scale),math.floor(y/scale)))
    end
  end
  return result
end
local contact=Image(848,848,ColorMode.RGB)
contact:clear(border)
local preview=Sprite(288,288,ColorMode.RGB)
preview.layers[1].name="v1 NN4 preview on checker - Aseprite native batch"
local durations={}
for i=1,16 do
  local frame=Image(64,64,ColorMode.RGB)
  frame:drawSprite(src,i,Point(0,0))
  local tile=checker(192,192,12)
  tile:drawImage(nearest(frame,3),Point(0,0))
  contact:drawImage(tile,Point(16+((i-1)%4)*208,16+math.floor((i-1)/4)*208))
  local gifFrame=Image(288,288,ColorMode.RGB)
  gifFrame:clear(border)
  gifFrame:drawImage(checker(256,256,16),Point(16,16))
  gifFrame:drawImage(nearest(frame,4),Point(16,16))
  if i>1 then preview:newFrame() end
  preview.layers[1]:cel(i).image=gifFrame
  preview.frames[i].duration=src.frames[i].duration
  durations[i]=math.floor(src.frames[i].duration*1000+0.5)
end
local contactSprite=Sprite(848,848,ColorMode.RGB)
contactSprite.layers[1].name="v1 original frames 01-16 - NN3 - Aseprite native batch"
contactSprite.cels[1].image=contact
contactSprite:saveAs(out.."/2d-v1-contact-aseprite-batch.png")
preview:saveAs(out.."/2d-v1-80ms-aseprite-batch.gif")
return {
 backend="Aseprite native batch",ui=app.isUIAvailable,version=app.version,
 source=src.filename,sourceWidth=src.width,sourceHeight=src.height,frames=#src.frames,
 frameDurationsMs=durations,previewLoopMs=1280,
 contact={path=out.."/2d-v1-contact-aseprite-batch.png",width=848,height=848,scale=3,columns=4,rows=4,frameOrder="row-major 01-16"},
 gif={path=out.."/2d-v1-80ms-aseprite-batch.gif",width=288,height=288,scale=4,frames=16,durationMs=80,loop="forward repeat"},
 notes="Preview only: 80ms per source frame; world lifetime remains 40 ticks with 16-frame mapping. Original source not saved."
}
