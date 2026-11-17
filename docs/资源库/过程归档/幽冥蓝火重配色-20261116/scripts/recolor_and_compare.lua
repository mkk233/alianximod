local root = args.root
local base = 'D:/alianximod/src/main/resources/assets/xueximod/textures/effect/'
local pc = app.pixelColor
local rgba = pc.rgba
local function rgb(r,g,b,a) return rgba(r,g,b,a or 255) end
local function round(n) return math.floor(n+.5) end
local function luma(r,g,b) return .2126*r+.7152*g+.0722*b end
local stops = {
 {0,0,0,0}, {48,11,56,67},
 {luma(210,49,13),8,92,102}, {luma(245,84,13),7,149,162},
 {luma(255,139,20),14,196,204}, {luma(255,193,41),68,237,231},
 {luma(255,227,103),167,255,240}, {luma(255,248,183),226,255,244},
 {255,240,255,250}
}
local function mapped(c, mode, preserveCoal)
 local r,g,b,a=pc.rgbaR(c),pc.rgbaG(c),pc.rgbaB(c),pc.rgbaA(c)
 if a==0 then return c end
 if mode=='old' then return rgb(round(r*90/255),round(g*160/255),b,a) end
 local y=luma(r,g,b)
 if preserveCoal and (y<40 or (r-g<10 and r-b<20)) then return c end
 for j=2,#stops do
  if y<=stops[j][1] then
   local lo,hi=stops[j-1],stops[j];local t=(y-lo[1])/(hi[1]-lo[1])
   return rgb(round(lo[2]+t*(hi[2]-lo[2])),round(lo[3]+t*(hi[3]-lo[3])),round(lo[4]+t*(hi[4]-lo[4])),a)
  end
 end
 return rgb(240,255,250,a)
end
local function transform(im,mode,preserveCoal)
 local result=Image(im)
 for it in result:pixels() do it(mapped(it(),mode,preserveCoal)) end
 return result
end
local entries={
 {id='fire_seed',file='fire_seed/fire_seed_flame.png',w=32,h=32,n=8,ms=100},
 {id='kindle_v1',file='kindle_v1/kindle.png',w=64,h=64,n=16,ms=80},
 {id='fire_spark_v3',file='fire_spark_v3/fire_spark.png',w=64,h=64,n=16,ms=80},
 {id='flame_trail_v3',file='flame_trail_v3/flame_trail.png',w=64,h=64,n=16,ms=80},
 {id='shock_ring_v1',file='shock_ring_v1/shock_ring.png',w=64,h=64,n=16,ms=40,times={40,40,30,40,40,40,30,40,40,40,30,40,40,40,30,40}},
 {id='seed_sow_v3',file='seed_sow_v3/seed_sow.png',w=64,h=64,n=16,ms=50},
 {id='detonate_v1',file='detonate_v1/detonate.png',w=64,h=64,n=16,ms=80},
 {id='liaoyuan_ground_flame',file='liaoyuan_v2/ground_flame.png',w=32,h=40,n=24,ms=50},
 {id='liaoyuan_ground_embers',file='liaoyuan_v2/ground_embers.png',w=64,h=64,n=24,ms=50,coal=true},
 {id='liaoyuan_spread_wave',file='liaoyuan_v2/spread_wave.png',w=128,h=128,n=12,ms=50},
 {id='fentian_conflagration',file='fentian_v2/conflagration.png',w=128,h=144,n=52,ms=50}
}
local result={status='candidate_pending_user_confirmation',entries={},checks={}}
local art={}
local function create(entry,input)
 assert(input.width==entry.w and input.height==entry.h*entry.n,'Unexpected atlas geometry '..entry.id)
 local blue=transform(input,'blue',entry.coal)
 local old=transform(input,'old')
 local aDiff,maskDiff=0,0
 for y=0,input.height-1 do for x=0,input.width-1 do
  local a=pc.rgbaA(input:getPixel(x,y));local b=pc.rgbaA(blue:getPixel(x,y))
  if a~=b then aDiff=aDiff+1 end
  if (a>0)~=(b>0) then maskDiff=maskDiff+1 end
 end end
 assert(aDiff==0 and maskDiff==0,'Alpha changed')
 input:saveAs(root..'/references/'..entry.id..'_original.png')
 blue:saveAs(root..'/textures/'..entry.id..'_blue.png')
 old:saveAs(root..'/textures/'..entry.id..'_legacy.png')
 local s=Sprite(entry.w,entry.h,ColorMode.RGB)
 local original=s.layers[1];original.name='REFERENCE_original_RGB';original.isVisible=false
 local legacy=s:newLayer();legacy.name='REFERENCE_legacy_multiply_90_160_255';legacy.isVisible=false
 local candidate=s:newLayer();candidate.name='CANDIDATE_soul_palette';candidate.isVisible=true
 local frames={original={},legacy={},blue={}}
 for f=1,entry.n do
  if f>1 then s:newEmptyFrame(f) end;s.frames[f].duration=(entry.times and entry.times[f] or entry.ms)/1000
  for index,pair in ipairs({{input,original,'original'},{old,legacy,'legacy'},{blue,candidate,'blue'}}) do
   local im=Image(entry.w,entry.h,ColorMode.RGB);im:drawImage(pair[1],Point(0,-(f-1)*entry.h))
   s:newCel(pair[2],f,im,Point(0,0));frames[pair[3]][f]=im
  end
 end
 if entry.id=='fentian_conflagration' then
  for _,t in ipairs({{'gather',1,3},{'ten_pulses',4,40},{'embers',41,52}}) do local tag=s:newTag(t[2],t[3]);tag.name=t[1] end
 elseif entry.id=='kindle_v3_screenshot' then
  for _,t in ipairs({{'sustain_loop',1,10},{'segment_pulse',11,13},{'extinguish_once',14,19}}) do local tag=s:newTag(t[2],t[3]);tag.name=t[1] end
 else local tag=s:newTag(1,entry.n);tag.name='source_frame_order' end
 s.data='nether-blue-visual;art-v1-20261116;candidate only;timing follows source-art preview, runtime sampler unchanged'
 s:saveAs(root..'/source/'..entry.id..'_blue.aseprite')
 frames.blue[1]:saveAs(root..'/textures/'..entry.id..'_blue_F01.png')
 result.entries[#result.entries+1]={id=entry.id,input=entry.file,w=entry.w,h=entry.h,frames=entry.n,previewMs=entry.ms,frameMs=entry.times,alphaDifferences=aDiff,maskDifferences=maskDiff,coalPreserved=entry.coal or false}
 art[entry.id]={sprite=s,frames=frames,entry=entry}
 return art[entry.id]
end
for _,entry in ipairs(entries) do create(entry,Image{fromFile=base..entry.file}) end
local source=app.open('D:/alianximod/docs/资源库/过程归档/引燃_全身燃烧重绘_v3_20261116/source/kindle_v3.aseprite')
local strip=Image(48,64*#source.frames,ColorMode.RGB)
for f=1,#source.frames do local im=Image(48,64,ColorMode.RGB);im:drawSprite(source,f);strip:drawImage(im,Point(0,(f-1)*64)) end
create({id='kindle_v3_screenshot',file=source.filename,w=48,h=64,n=19,ms=50},strip)
local ref=Image{fromFile=base..'nether_gloom_v2/nether_gloom.png'}
ref:saveAs(root..'/references/nether_gloom_v2_runtime.png')
local refFrames={}
for f=1,16 do local im=Image(64,64,ColorMode.RGB);im:drawImage(ref,Point(0,-(f-1)*64));refFrames[f]=im end
local font={
 A={'010','101','111','101','101'},B={'110','101','110','101','110'},C={'011','100','100','100','011'},D={'110','101','101','101','110'},
 E={'111','100','110','100','111'},F={'111','100','110','100','100'},G={'011','100','101','101','011'},H={'101','101','111','101','101'},
 I={'111','010','010','010','111'},J={'001','001','001','101','010'},K={'101','101','110','101','101'},L={'100','100','100','100','111'},
 M={'10001','11011','10101','10001','10001'},N={'1001','1101','1011','1001','1001'},O={'010','101','101','101','010'},P={'110','101','110','100','100'},
 Q={'010','101','101','111','011'},R={'110','101','110','101','101'},S={'011','100','010','001','110'},T={'111','010','010','010','010'},
 U={'101','101','101','101','111'},V={'101','101','101','101','010'},W={'10001','10001','10101','11011','10001'},X={'101','101','010','101','101'},
 Y={'101','101','010','010','010'},Z={'111','001','010','100','111'},['0']={'111','101','101','101','111'},['1']={'010','110','010','010','111'},
 ['2']={'110','001','010','100','111'},['3']={'110','001','010','001','110'},['4']={'101','101','111','001','001'},['5']={'111','100','110','001','110'},
 ['6']={'011','100','110','101','010'},['7']={'111','001','010','010','010'},['8']={'010','101','010','101','010'},['9']={'010','101','011','001','110'}
}
local function rect(im,x,y,w,h,c) for yy=y,y+h-1 do for xx=x,x+w-1 do if xx>=0 and yy>=0 and xx<im.width and yy<im.height then im:drawPixel(xx,yy,c) end end end end
local function text(im,str,x,y,c)
 for j=1,#str do local glyph=font[str:sub(j,j)];if glyph then for yy,row in ipairs(glyph) do for xx=1,#row do if row:sub(xx,xx)=='1' then im:drawPixel(x+xx-1,y+yy-1,c) end end end;x=x+#glyph[1]+1 else x=x+3 end end
end
local function mannequin(im,x,y)
 local skin=rgb(184,149,117);local shirt=rgb(53,113,119);local pants=rgb(65,72,103);local dark=rgb(35,38,42)
 rect(im,x+18,y,12,14,skin);rect(im,x+18,y,12,4,rgb(65,47,37));rect(im,x+21,y+6,2,2,dark);rect(im,x+26,y+6,2,2,dark)
 rect(im,x+17,y+14,14,23,shirt);rect(im,x+11,y+14,6,24,shirt);rect(im,x+31,y+14,6,24,shirt)
 rect(im,x+17,y+37,7,27,pants);rect(im,x+24,y+37,7,27,pants)
 rect(im,x+16,y+61,8,3,dark);rect(im,x+24,y+61,8,3,dark)
end
local bg=rgb(31,38,47);local label=rgb(186,208,213)
local function comparison(id,sequence,ms,withTarget)
 local a=art[id];local s=Sprite(352,112,ColorMode.RGB);s.layers[1].name='AUX_comparison_same_source_frame'
 local first
 for i,f in ipairs(sequence) do
  if i>1 then s:newEmptyFrame(i) end;s.frames[i].duration=ms/1000
  local im=Image(352,112,ColorMode.RGB);im:clear(bg)
  for c,title in ipairs({'ORIGINAL','OLD RGB','NEW SOUL','NETHER REF'}) do
   local x=(c-1)*88;text(im,title,x+8,6,label);rect(im,x+6,94,76,1,rgb(61,77,86))
   local flame
   if c<4 then flame=a.frames[({'original','legacy','blue'})[c]][f] else flame=refFrames[(math.floor((i-1)*ms/80)%16)+1] end
   if withTarget and c<4 then mannequin(im,x+20,26) end
   local fx=x+round((88-flame.width)/2);local fy=90-flame.height
   im:drawImage(flame,Point(fx,fy))
   rect(im,x+16,101,56,2,({rgb(255,193,41),rgb(90,160,255),rgb(14,196,204),rgb(68,237,231)})[c])
  end
  s:newCel(s.layers[1],i,im,Point(0,0));if i==6 or (not first) then first=Image(im) end
 end
 s:saveAs(root..'/source/review_'..id..'.aseprite')
 first:saveAs(root..'/previews/'..id..'_comparison_native.png')
 first:resize(1056,336);first:saveAs(root..'/previews/'..id..'_comparison.png')
 s:saveAs(root..'/previews/'..id..'_comparison_native.gif')
 app.sprite=s;app.command.SpriteSize{ui=false,width=1056,height=336,method='nearest'}
 s:saveAs(root..'/previews/'..id..'_comparison.gif')
 return s
end
local function range(n) local t={};for i=1,n do t[#t+1]=i end;return t end
comparison('fire_seed',range(8),100,false)
comparison('kindle_v1',range(16),80,true)
local sequence={};for cycle=1,3 do for tick=0,9 do sequence[#sequence+1]=(cycle>1 and tick<3) and tick+11 or tick+1 end end
for f=14,19 do sequence[#sequence+1]=f end;for n=1,10 do sequence[#sequence+1]=19 end
comparison('kindle_v3_screenshot',sequence,50,true)
local board=Image(352,244,ColorMode.RGB);board:clear(bg)
text(board,'EXACT NETHER V2 PALETTE',12,10,label)
for i,c in ipairs({rgb(11,56,67),rgb(8,92,102),rgb(7,149,162),rgb(14,196,204),rgb(68,237,231),rgb(167,255,240),rgb(226,255,244)}) do rect(board,12+(i-1)*46,24,42,14,c) end
text(board,'NETHER V2',12,50,label);board:drawImage(refFrames[6],Point(16,64))
text(board,'MC SOUL 0',104,50,label);text(board,'MC SOUL 1',202,50,label);text(board,'PARTICLE',292,50,label)
for j,file in ipairs({'soul_fire_0.png','soul_fire_1.png','soul_fire_flame.png'}) do
 local original=Image{fromFile=root..'/references/'..file};local side=j==3 and 8 or 16
 local frame=Image(side,side,ColorMode.RGB);frame:drawImage(original);frame:resize(side*4,side*4);board:drawImage(frame,Point(({104,202,300})[j],64))
end
text(board,'BRIGHTNESS ORDER KEPT',12,149,label);text(board,'ALPHA AND SHAPE UNCHANGED',12,161,label)
text(board,'KINDLE SIX COLOR MAP',12,181,label)
local sourceColors={rgb(210,49,13),rgb(245,84,13),rgb(255,139,20),rgb(255,193,41),rgb(255,227,103),rgb(255,248,183)}
for i,c in ipairs(sourceColors) do rect(board,12+(i-1)*54,196,48,14,c);rect(board,12+(i-1)*54,218,48,14,mapped(c,'blue')) end
board:resize(1056,732);board:saveAs(root..'/previews/palette_and_native_references.png')
local lut=Image(256,1,ColorMode.RGB)
for x=0,255 do lut:drawPixel(x,0,mapped(rgb(x,x,x),'blue')) end
lut:saveAs(root..'/textures/soul_luma_lookup_256.png')
local f=io.open(root..'/evidence/aseprite-generation.json','w');f:write(json.encode(result));f:close()
app.sprite=art.kindle_v3_screenshot.sprite
return result
