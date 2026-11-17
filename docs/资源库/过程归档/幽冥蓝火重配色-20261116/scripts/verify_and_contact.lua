local root=args.root
local pc=app.pixelColor
local rgba=pc.rgba
local function read(path) local f=io.open(path,'r');local s=f:read('*a');f:close();return s end
local metadata=json.decode(read(root..'/evidence/aseprite-generation.json'))
local shock=app.open(root..'/source/shock_ring_v1_blue.aseprite')
local times={40,40,30,40,40,40,30,40,40,40,30,40,40,40,30,40}
for f=1,16 do shock.frames[f].duration=times[f]/1000 end
shock:saveAs(root..'/source/shock_ring_v1_blue.aseprite')
for _,entry in ipairs(metadata.entries) do if entry.id=='shock_ring_v1' then entry.previewMs=40;entry.frameMs=times end end
local out=io.open(root..'/evidence/aseprite-generation.json','w');out:write(json.encode(metadata));out:close()
local verification={backend='Aseprite official MCP batch',entries={}}
local frameChoices={4,6,8,6,8,8,8,6,6,6,20,6}
local sheet=Image(768,336,ColorMode.RGB);sheet:clear(rgba(25,34,42,255))
local digits={['0']={'111','101','101','101','111'},['1']={'010','110','010','010','111'},['2']={'110','001','010','100','111'},['3']={'110','001','010','001','110'},['4']={'101','101','111','001','001'},['5']={'111','100','110','001','110'},['6']={'011','100','110','101','010'},['7']={'111','001','010','010','010'},['8']={'010','101','010','101','010'},['9']={'010','101','011','001','110'}}
local function rect(im,x,y,w,h,c) for yy=y,y+h-1 do for xx=x,x+w-1 do im:drawPixel(xx,yy,c) end end end
local function number(im,n,x,y) local t=string.format('%02d',n);for i=1,2 do for yy,row in ipairs(digits[t:sub(i,i)]) do for xx=1,3 do if row:sub(xx,xx)=='1' then rect(im,x+(i-1)*5+xx-1,y+yy-1,1,1,rgba(207,226,231,255)) end end end end end
for i,e in ipairs(metadata.entries) do
 local src=Image{fromFile=root..'/references/'..e.id..'_original.png'}
 local dst=Image{fromFile=root..'/textures/'..e.id..'_blue.png'}
 local legacy=Image{fromFile=root..'/textures/'..e.id..'_legacy.png'}
 local s=app.open(root..'/source/'..e.id..'_blue.aseprite')
 local alpha,shape,rgbaMismatch=0,0,0
 local frames={}
 for f=1,#s.frames do
  local im=Image(e.w,e.h,ColorMode.RGB);im:drawSprite(s,f);frames[#frames+1]=s.frames[f].duration*1000
  for y=0,e.h-1 do for x=0,e.w-1 do
   local a=src:getPixel(x,(f-1)*e.h+y);local b=dst:getPixel(x,(f-1)*e.h+y)
   if pc.rgbaA(a)~=pc.rgbaA(b) then alpha=alpha+1 end
   if (pc.rgbaA(a)>0)~=(pc.rgbaA(b)>0) then shape=shape+1 end
   if im:getPixel(x,y)~=b and pc.rgbaA(b)>0 then rgbaMismatch=rgbaMismatch+1 end
  end end
 end
 assert(alpha==0 and shape==0 and rgbaMismatch==0,'Verification failed '..e.id)
 verification.entries[#verification.entries+1]={id=e.id,w=src.width,atlasHeight=src.height,frameHeight=e.h,frames=#s.frames,alphaDiff=alpha,shapeDiff=shape,sourceVisiblePixelDiff=rgbaMismatch,frameMs=frames}
 local x=((i-1)%4)*192;local y=math.floor((i-1)/4)*112
 rect(sheet,x+2,y+2,188,108,rgba(34,44,53,255));number(sheet,i,x+8,y+8)
 for c,img in ipairs({src,legacy,dst}) do
  local frame=Image(e.w,e.h,ColorMode.RGB);frame:drawImage(img,Point(0,-(frameChoices[i]-1)*e.h))
  local scale=math.min(56/e.w,72/e.h);frame:resize(math.max(1,math.floor(e.w*scale)),math.max(1,math.floor(e.h*scale)))
  sheet:drawImage(frame,Point(x+(c-1)*62+4+math.floor((60-frame.width)/2),y+96-frame.height))
  rect(sheet,x+(c-1)*62+14,y+101,38,2,({rgba(255,193,41,255),rgba(90,160,255,255),rgba(14,196,204,255)})[c])
 end
end
sheet:resize(1536,672);sheet:saveAs(root..'/previews/all_source_shapes_same_frame.png')
local f=io.open(root..'/evidence/aseprite-reopen-verification.json','w');f:write(json.encode(verification));f:close()
return {verified=#verification.entries,alphaDifferences=0,shapeDifferences=0,visibleSourceDifferences=0,contact=root..'/previews/all_source_shapes_same_frame.png',shockTiming=times}
