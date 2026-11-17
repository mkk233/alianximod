local root=args.root
local base='D:/alianximod/src/main/resources/assets/xueximod/textures/effect/'
local pixelColor=app.pixelColor
local rgba=pixelColor.rgba
local function round(value) return math.floor(value+.5) end
local function luma(red,green,blue) return .2126*red+.7152*green+.0722*blue end
local stops={
 {0,0,0,0},{48,11,56,67},
 {luma(210,49,13),8,92,102},{luma(245,84,13),7,149,162},
 {luma(255,139,20),14,196,204},{luma(255,193,41),68,237,231},
 {luma(255,227,103),167,255,240},{luma(255,248,183),226,255,244},{255,240,255,250}
}
local function mapped(value,mode,preserveNeutral)
 local red,green,blue,alpha=pixelColor.rgbaR(value),pixelColor.rgbaG(value),pixelColor.rgbaB(value),pixelColor.rgbaA(value)
 if alpha==0 then return value end
 if mode=='legacy' then return rgba(round(red*90/255),round(green*160/255),blue,alpha) end
 local brightness=luma(red,green,blue)
 if preserveNeutral and (brightness<40 or (red-green<10 and red-blue<20)) then return value end
 for index=2,#stops do
  if brightness<=stops[index][1] then
   local lower,upper=stops[index-1],stops[index]
   local factor=(brightness-lower[1])/(upper[1]-lower[1])
   return rgba(round(lower[2]+factor*(upper[2]-lower[2])),round(lower[3]+factor*(upper[3]-lower[3])),round(lower[4]+factor*(upper[4]-lower[4])),alpha)
  end
 end
 return rgba(240,255,250,alpha)
end
local function transform(image,mode,preserveNeutral)
 local output=Image(image)
 for pixel in output:pixels() do pixel(mapped(pixel(),mode,preserveNeutral)) end
 return output
end
local catalog={}
local furnaceKeys={'warmth','hand_rear','leg_rear','torso','hand_front','leg_front','embers'}
local burnKeys={'cape','rear','shoulder_right','shoulder_left','abdomen_right','abdomen_left','hip_left','shin_right','shin_left','hand','hand_embers_right','hand_embers_left','body_embers'}
for _,phase in ipairs({'entry','loop'}) do
 for _,key in ipairs(furnaceKeys) do
  catalog[#catalog+1]={id='furnace_'..phase..'_'..key,file='furnace_v3/'..phase..'_'..key..'.png',width=96,frameHeight=112,frames=phase=='entry' and 8 or 20,ms=phase=='entry' and 60 or 80,phase=phase,family='furnace_v3'}
 end
 for _,key in ipairs(burnKeys) do
  catalog[#catalog+1]={id='burn_'..phase..'_'..key,file='burn_spatial_v1/'..phase..'/'..key..'.png',width=96,frameHeight=112,frames=phase=='entry' and 8 or 20,ms=phase=='entry' and 60 or 80,phase=phase,family='burn_spatial_v1'}
 end
end
for _,entry in ipairs({
 {id='liaoli_back_ignite',file='liaoli_v6/back_ignite.png',width=128,frameHeight=48,frames=12,ms=50},
 {id='liaoli_back_flow',file='liaoli_v6/back_flow.png',width=128,frameHeight=48,frames=24,ms=50},
 {id='liaoli_thrust_backflow',file='liaoli_v6/thrust_backflow.png',width=128,frameHeight=64,frames=24,ms=50},
 {id='liaoli_rear_ring',file='liaoli_v6/rear_ring.png',width=96,frameHeight=96,frames=24,ms=50},
 {id='liaoyuan_v3_ground_embers',file='liaoyuan_v3/ground_embers.png',width=16,frameHeight=16,frames=24,ms=50,preserveNeutral=true}
}) do catalog[#catalog+1]=entry end
local art={}
local report={attempt='art-coverage-02',candidate=true,entries={},excluded={},reusedNative={}}
local function frameImage(atlas,height,index)
 local output=Image(atlas.width,height,ColorMode.RGB)
 output:drawImage(atlas,Point(0,-(index-1)*height))
 return output
end
for _,entry in ipairs(catalog) do
 local original=Image{fromFile=base..entry.file}
 assert(original.width==entry.width and original.height==entry.frameHeight*entry.frames,'Layout mismatch: '..entry.id)
 local blue=transform(original,'blue',entry.preserveNeutral)
 local legacy=transform(original,'legacy')
 local alphaDiff,maskDiff,changed,preserved=0,0,0,0
 for row=0,original.height-1 do for column=0,original.width-1 do
  local source=original:getPixel(column,row)
  local target=blue:getPixel(column,row)
  local sourceAlpha,targetAlpha=pixelColor.rgbaA(source),pixelColor.rgbaA(target)
  if sourceAlpha~=targetAlpha then alphaDiff=alphaDiff+1 end
  if (sourceAlpha>0)~=(targetAlpha>0) then maskDiff=maskDiff+1 end
  if sourceAlpha>0 then
   if source~=target then changed=changed+1 else preserved=preserved+1 end
  end
 end end
 assert(alphaDiff==0 and maskDiff==0,entry.id)
 original:saveAs(root..'/references/'..entry.id..'_original.png')
 blue:saveAs(root..'/textures/'..entry.id..'_blue.png')
 legacy:saveAs(root..'/textures/'..entry.id..'_legacy.png')
 local document=Sprite(entry.width,entry.frameHeight,ColorMode.RGB)
 local originalLayer=document.layers[1]
 originalLayer.name='REFERENCE_original';originalLayer.isVisible=false
 local legacyLayer=document:newLayer()
 legacyLayer.name='COMPARATOR_multiply_90_160_255';legacyLayer.isVisible=false
 local blueLayer=document:newLayer()
 blueLayer.name='CANDIDATE_same_soul_palette';blueLayer.isVisible=true
 for frame=1,entry.frames do
  if frame>1 then document:newEmptyFrame(frame) end
  document.frames[frame].duration=entry.ms/1000
  document:newCel(originalLayer,frame,frameImage(original,entry.frameHeight,frame),Point(0,0))
  document:newCel(legacyLayer,frame,frameImage(legacy,entry.frameHeight,frame),Point(0,0))
  document:newCel(blueLayer,frame,frameImage(blue,entry.frameHeight,frame),Point(0,0))
 end
 local tag=document:newTag(1,entry.frames)
 tag.name=entry.phase=='entry' and 'entry_once' or 'source_frame_order'
 document.data='nether-blue-visual / art-coverage-02 / candidate only / original dimensions UV and alpha retained'
 document:saveAs(root..'/source/'..entry.id..'_blue.aseprite')
 document:close()
 entry.alphaDifferences=alphaDiff;entry.silhouetteDifferences=maskDiff
 entry.changedVisiblePixels=changed;entry.preservedVisiblePixels=preserved
 report.entries[#report.entries+1]=entry
 art[entry.id]={original=original,legacy=legacy,blue=blue,entry=entry}
end
for _,entry in ipairs({
 {id='sword_unlit',file='liaoli_v6/sword_unlit.png',height=20,reason='physical sword and hilt; preserve every pixel'},
 {id='sword_hot',file='liaoli_v6/sword_hot.png',height=20,reason='all 338 visible pixels overlap unlit physical sword; no separate external halo; preserve every pixel'},
 {id='ground_ash',file='liaoyuan_v3/ground_ash.png',height=16,reason='neutral ash; remove old runtime tint rather than recolor'},
 {id='ground_ash_b',file='liaoyuan_v3/ground_ash_b.png',height=16,reason='neutral ash; preserve'},
 {id='ground_ash_c',file='liaoyuan_v3/ground_ash_c.png',height=16,reason='neutral ash; preserve'},
 {id='ground_ash_d',file='liaoyuan_v3/ground_ash_d.png',height=16,reason='neutral ash; preserve'},
 {id='surface_anchor',file='surface_anchor_v2/surface_anchor.png',height=64,reason='golden triangular anchor marker, not fire; preserve'},
 {id='ground_dust',file='ground_dust_v1/ground_dust.png',height=64,reason='brown soil/dust, not flame; preserve'}
}) do
 local original=Image{fromFile=base..entry.file}
 original:saveAs(root..'/references/preserve_'..entry.id..'.png')
 if entry.id=='ground_ash' then transform(original,'legacy'):saveAs(root..'/references/preserve_ground_ash_legacy_comparator.png') end
 entry.width=original.width;entry.atlasHeight=original.height
 report.excluded[#report.excluded+1]=entry
 art[entry.id]={original=original,legacy=transform(original,'legacy'),blue=original,entry={frameHeight=entry.height,frames=original.height/entry.height}}
end
for _,entry in ipairs({
 {id='native_particle',original='particle_flame.png',blue='particle_soul_fire_flame.png',height=8,resource='minecraft:particle/soul_fire_flame'},
 {id='native_fire_0',original='block_fire_0.png',blue='block_soul_fire_0.png',height=16,resource='minecraft:block/soul_fire_0'},
 {id='native_fire_1',original='block_fire_1.png',blue='block_soul_fire_1.png',height=16,resource='minecraft:block/soul_fire_1'}
}) do
 local original=Image{fromFile=root..'/references/'..entry.original}
 local blue=Image{fromFile=root..'/references/'..entry.blue}
 local legacy=transform(original,'legacy')
 legacy:saveAs(root..'/references/'..entry.id..'_legacy.png')
 art[entry.id]={original=original,legacy=legacy,blue=blue,entry={frameHeight=entry.height,frames=original.height/entry.height}}
 entry.width=original.width;entry.atlasHeight=original.height
 entry.note='Reuse native soul art, NOT same-alpha recolor; original soul mcmeta is preserved'
 report.reusedNative[#report.reusedNative+1]=entry
end
local font={
 A={'010','101','111','101','101'},B={'110','101','110','101','110'},C={'011','100','100','100','011'},
 D={'110','101','101','101','110'},E={'111','100','110','100','111'},F={'111','100','110','100','100'},
 G={'011','100','101','101','011'},H={'101','101','111','101','101'},I={'111','010','010','010','111'},
 J={'001','001','001','101','010'},K={'101','101','110','101','101'},L={'100','100','100','100','111'},
 M={'10001','11011','10101','10001','10001'},N={'1001','1101','1011','1001','1001'},O={'010','101','101','101','010'},
 P={'110','101','110','100','100'},Q={'010','101','101','111','011'},R={'110','101','110','101','101'},
 S={'011','100','010','001','110'},T={'111','010','010','010','010'},U={'101','101','101','101','111'},
 V={'101','101','101','101','010'},W={'10001','10001','10101','11011','10001'},X={'101','101','010','101','101'},
 Y={'101','101','010','010','010'},Z={'111','001','010','100','111'},['0']={'111','101','101','101','111'},
 ['1']={'010','110','010','010','111'},['2']={'110','001','010','100','111'},['3']={'110','001','010','001','110'},
 ['4']={'101','101','111','001','001'},['5']={'111','100','110','001','110'},['6']={'011','100','110','101','010'},
 ['7']={'111','001','010','010','010'},['8']={'010','101','010','101','010'},['9']={'010','101','011','001','110'}
}
local background=rgba(30,37,46,255)
local label=rgba(204,220,226,255)
local function text(image,stringValue,left,top)
 for index=1,#stringValue do
  local glyph=font[stringValue:sub(index,index)]
  if glyph then
   for row,line in ipairs(glyph) do for column=1,#line do
    if line:sub(column,column)=='1' then image:drawPixel(left+column-1,top+row-1,label) end
   end end
   left=left+#glyph[1]+1
  else left=left+3 end
 end
end
local function rectangle(image,left,top,width,height,color)
 for row=top,top+height-1 do for column=left,left+width-1 do
  if column>=0 and row>=0 and column<image.width and row<image.height then image:drawPixel(column,row,color) end
 end end
end
local function mannequin(image,left,top)
 local shade=rgba(100,108,118,255)
 rectangle(image,left+36,top+8,24,24,shade)
 rectangle(image,left+36,top+32,24,36,shade)
 rectangle(image,left+24,top+32,12,36,shade)
 rectangle(image,left+60,top+32,12,36,shade)
 rectangle(image,left+36,top+68,12,36,shade)
 rectangle(image,left+49,top+68,11,36,shade)
end
local function assetFrame(id,mode,frame)
 local entry=art[id]
 return frameImage(entry[mode],entry.entry.frameHeight,((frame-1)%entry.entry.frames)+1)
end
local function composite(family,phase,mode,frame)
 local output=Image(96,112,ColorMode.RGB)
 local keys=family=='furnace' and furnaceKeys or burnKeys
 for _,key in ipairs(keys) do output:drawImage(assetFrame(family..'_'..phase..'_'..key,mode,frame)) end
 return output
end
local function panel(family,phase,frame)
 local output=Image(480,136,ColorMode.RGB);output:clear(background)
 for column,mode in ipairs({'original','legacy','blue'}) do
  local left=(column-1)*160
  text(output,({'ORIGINAL','OLD RGB COMPARATOR','NEW SOUL'})[column],left+8,6)
  mannequin(output,left+32,20)
  output:drawImage(composite(family,phase,mode,frame),Point(left+32,20))
 end
 return output
end
for _,family in ipairs({'furnace','burn'}) do
 for _,phase in ipairs({'entry','loop'}) do
  local frames=phase=='entry' and 8 or 20
  local document=Sprite(480,136,ColorMode.RGB)
  document.layers[1].name='AUX_three_column_comparison'
  for frame=1,frames do
   if frame>1 then document:newEmptyFrame(frame) end
   document.frames[frame].duration=phase=='entry' and .06 or .08
   document:newCel(document.layers[1],frame,panel(family,phase,frame))
  end
  document:saveAs(root..'/source/review_'..family..'_'..phase..'.aseprite')
  app.sprite=document
  app.command.SpriteSize{ui=false,width=960,height=272,method='nearest'}
  document:saveAs(root..'/previews/'..family..'_'..phase..'_comparison.gif')
  document:close()
 end
end
local overview=Image(768,800,ColorMode.RGB);overview:clear(background)
for column,title in ipairs({'ORIGINAL','OLD RGB COMPARATOR','NEW SOUL OR PRESERVE'}) do text(overview,title,(column-1)*256+16,8) end
local function place(id,mode,frame,left,top,scale)
 local image=assetFrame(id,mode,frame)
 if scale and scale~=1 then image:resize(image.width*scale,image.height*scale) end
 overview:drawImage(image,Point(left,top))
end
for column,mode in ipairs({'original','legacy','blue'}) do
 local left=(column-1)*256
 text(overview,'FURNACE 14 SHEETS',left+12,27)
 mannequin(overview,left+80,40)
 overview:drawImage(composite('furnace','loop',mode,6),Point(left+80,40))
 text(overview,'BURN MOUNT 26 SHEETS',left+12,158)
 mannequin(overview,left+80,171)
 overview:drawImage(composite('burn','loop',mode,6),Point(left+80,171))
 text(overview,'LIAOLI FIRE ONLY',left+12,290)
 place('liaoli_back_flow',mode,6,left+10,307,1)
 place('sword_hot',mode=='legacy' and 'original' or mode,1,left+42,335,1)
 place('liaoli_rear_ring',mode,6,left+150,304,1)
 place('liaoli_thrust_backflow',mode,6,left+14,360,1)
 text(overview,'ASH PRESERVED EMBERS BLUE',left+12,434)
 place('ground_ash',mode,1,left+22,450,4)
 place('liaoyuan_v3_ground_embers',mode,6,left+22,450,4)
 place('surface_anchor',mode=='legacy' and 'original' or mode,6,left+110,452,1)
 place('ground_dust',mode=='legacy' and 'original' or mode,6,left+180,450,1)
 text(overview,'NATIVE SOUL REUSE',left+12,532)
 place('native_fire_0',mode,6,left+18,551,5)
 place('native_fire_1',mode,6,left+106,551,5)
 place('native_particle',mode,1,left+202,575,5)
 text(overview,'METAL STAYS SAME',left+12,650)
 place('sword_unlit','original',1,left+20,670,3)
 place('sword_hot','original',1,left+20,724,3)
end
overview:saveAs(root..'/previews/coverage_overview_native.png')
overview:resize(1152,1200)
overview:saveAs(root..'/previews/coverage_overview.png')
local document=Sprite(480,136,ColorMode.RGB)
document.layers[1].name='AUX_liaoli_fire_comparison'
for frame=1,24 do
 if frame>1 then document:newEmptyFrame(frame) end
 document.frames[frame].duration=.05
 local image=Image(480,136,ColorMode.RGB);image:clear(background)
 for column,mode in ipairs({'original','legacy','blue'}) do
  local left=(column-1)*160
  text(image,({'ORIGINAL','OLD RGB COMPARATOR','NEW SOUL'})[column],left+5,5)
  image:drawImage(assetFrame('liaoli_back_flow',mode,frame),Point(left+16,20))
  image:drawImage(assetFrame('sword_hot','original',1),Point(left+48,40))
  image:drawImage(assetFrame('liaoli_thrust_backflow',mode,frame),Point(left+16,68))
 end
 document:newCel(document.layers[1],frame,image)
end
document:saveAs(root..'/source/review_liaoli.aseprite')
app.sprite=document
app.command.SpriteSize{ui=false,width=960,height=272,method='nearest'}
document:saveAs(root..'/previews/liaoli_fire_comparison.gif')
document:close()
local output=io.open(root..'/evidence/aseprite-production.json','w')
output:write(json.encode(report));output:close()
return {recolored=#report.entries,preserved=#report.excluded,nativeReuse=#report.reusedNative,alphaDifferences=0,overview=root..'/previews/coverage_overview.png'}
