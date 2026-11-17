local root=args.root
local input=io.open(root..'/evidence/aseprite-production.json','r')
local report=json.decode(input:read('*a'));input:close()
local pixelColor=app.pixelColor
local result={attempt='art-coverage-02',entries={},alphaDifferences=0,silhouetteDifferences=0,sourceExportDifferences=0,currentInputDifferences=0}
for _,entry in ipairs(report.entries) do
 local original=Image{fromFile=root..'/references/'..entry.id..'_original.png'}
 local current=Image{fromFile='D:/alianximod/src/main/resources/assets/xueximod/textures/effect/'..entry.file}
 local blue=Image{fromFile=root..'/textures/'..entry.id..'_blue.png'}
 local document=app.open(root..'/source/'..entry.id..'_blue.aseprite')
 assert(document.width==entry.width and document.height==entry.frameHeight and #document.frames==entry.frames,entry.id)
 assert(blue.width==original.width and blue.height==original.height,entry.id)
 local alphaDiff,shapeDiff,exportDiff,inputDiff=0,0,0,0
 for frame=1,entry.frames do
  assert(math.abs(document.frames[frame].duration-entry.ms/1000)<.0001,entry.id..' duration')
  local visible=Image(entry.width,entry.frameHeight,ColorMode.RGB)
  visible:drawSprite(document,frame)
  for row=0,entry.frameHeight-1 do for column=0,entry.width-1 do
   local atlasRow=(frame-1)*entry.frameHeight+row
   local source=original:getPixel(column,atlasRow)
   local output=blue:getPixel(column,atlasRow)
   local alphaSource,alphaOutput=pixelColor.rgbaA(source),pixelColor.rgbaA(output)
   if alphaSource~=alphaOutput then alphaDiff=alphaDiff+1 end
   if (alphaSource>0)~=(alphaOutput>0) then shapeDiff=shapeDiff+1 end
   if source~=current:getPixel(column,atlasRow) then inputDiff=inputDiff+1 end
   if alphaOutput>0 and output~=visible:getPixel(column,row) then exportDiff=exportDiff+1 end
   if pixelColor.rgbaA(visible:getPixel(column,row))~=alphaOutput then exportDiff=exportDiff+1 end
  end end
 end
 document:close()
 assert(alphaDiff+shapeDiff+exportDiff+inputDiff==0,'Pixel verification failed: '..entry.id)
 result.entries[#result.entries+1]={id=entry.id,width=entry.width,frameHeight=entry.frameHeight,frames=entry.frames,alphaDifferences=alphaDiff,silhouetteDifferences=shapeDiff,sourceExportDifferences=exportDiff,currentInputDifferences=inputDiff}
end
local hot=Image{fromFile=root..'/references/preserve_sword_hot.png'}
local metal=Image{fromFile=root..'/references/preserve_sword_unlit.png'}
local protectedPixels,externalHalo=0,0
for row=0,19 do for column=0,63 do
 if pixelColor.rgbaA(hot:getPixel(column,row))>0 then
  if pixelColor.rgbaA(metal:getPixel(column,row))>0 then protectedPixels=protectedPixels+1 else externalHalo=externalHalo+1 end
 end
end end
result.swordHot={visibleProtectedPixels=protectedPixels,externalHaloPixels=externalHalo,action='preserve entire sword_hot; flame-only sheets are recolored separately'}
result.exclusions={}
for _,entry in ipairs(report.excluded) do
 local original=Image{fromFile='D:/alianximod/src/main/resources/assets/xueximod/textures/effect/'..entry.file}
 local reference=Image{fromFile=root..'/references/preserve_'..entry.id..'.png'}
 assert(original.width==reference.width and original.height==reference.height,entry.id)
 local differences=0
 for row=0,original.height-1 do for column=0,original.width-1 do
  if original:getPixel(column,row)~=reference:getPixel(column,row) then differences=differences+1 end
 end end
 assert(differences==0,entry.id..' exclusion changed')
 result.exclusions[#result.exclusions+1]={id=entry.id,pixelDifferences=differences}
end
local output=io.open(root..'/evidence/aseprite-verification.json','w')
output:write(json.encode(result));output:close()
return {verified=#result.entries,excludedVerified=#result.exclusions,alphaDifferences=0,silhouetteDifferences=0,sourceExportDifferences=0,currentInputDifferences=0,swordHot=result.swordHot}
