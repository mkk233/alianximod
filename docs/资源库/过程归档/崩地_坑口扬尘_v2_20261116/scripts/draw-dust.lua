local root = args.root
local s = Sprite(96,32,ColorMode.RGB)
s.filename = root .. '/source/ground_dust_v2.aseprite'
local names={'01_under_dust','02_broken_earth_clusters','03_separate_grit'}
local layers={s.layers[1],s:newLayer(),s:newLayer()}
for i,l in ipairs(layers) do l.name=names[i] end
for i=2,16 do s:newEmptyFrame() end
local palettes={{108,81,58},{147,112,77},{174,137,94},{194,159,114},{123,99,76},{165,142,112}}
local function hash(a,b,c) return ((a*7387+b*3253+c*977+127)%997)/997 end
local rows={{2,4},{1,6},{0,7},{0,8},{1,7},{1,6},{3,3}}
local function dot(img,x,y,c,a)
 if x>=0 and x<96 and y>=0 and y<32 and a>0 then img:drawPixel(x,y,app.pixelColor.rgba(c[1],c[2],c[3],math.floor(a))) end
end
for f=1,16 do
 s.frames[f].duration=0.05
 local ims={Image(96,32,ColorMode.RGB),Image(96,32,ColorMode.RGB),Image(96,32,ColorMode.RGB)}
 local t=(f-1)/15
 if f<16 then
  for v=0,2 do
   for j=0,8 do
    local age=t-(j%3)*0.028
    if age>=0 then
     local seed=j+v*17
     local spread=(j-4)*1.15 + (hash(seed,1,7)-0.5)*2
     local cx=16+spread+(j-4)*age*1.35+(v-1)*age*2
     local cy=26-(2+j%4)*age*3.3-6*age+7*age*age
     local fade=math.max(0,1-math.max(0,age-0.26)/0.71)
     local alpha=math.min(1,age*13+0.22)*fade*(j%2==0 and 210 or 165)
     local erode=math.max(0,(age-0.40)/0.55)
     for yy,row in ipairs(rows) do
      for xx=row[1],row[1]+row[2]-1 do
       local h=hash(xx,yy,seed)
       if h>erode*0.90 and not (age<0.09 and (xx<2 or xx>5 or yy<2 or yy>5)) then
        local px=math.floor(cx+xx-4)+v*32
        local py=math.floor(cy+yy-4)
        if px>=v*32+1 and px<v*32+31 then
         local tone=yy<3 and 4 or (yy>5 and 1 or (h<0.4 and 2 or 3))
         dot(ims[2],px,py,palettes[tone],alpha)
         if j%2==0 then dot(ims[1],px-1,py-1,palettes[6],alpha*0.33) end
        end
       end
      end
     end
    end
   end
   for j=0,17 do
    local born=(j%4)*0.025
    local age=t-born
    if age>=0 then
     local side=(j%2==0 and -1 or 1)
     local px=math.floor(16+side*(2+hash(j,v,4)*4+age*(5+hash(j,v,8)*6)))
     local py=math.floor(25-age*(12+hash(j,v,9)*12)+age*age*8)
     local fade=math.max(0,1-math.max(0,age-0.18)/0.77)
     local alpha=fade*(125+hash(j,v,10)*105)
     if px>=1 and px<31 then
      local tone=(j%3==0 and 1 or 5)
      dot(ims[3],v*32+px,py,palettes[tone],alpha)
      if j%4==0 and age<0.55 then dot(ims[3],v*32+px+1,py,palettes[3],alpha) end
     end
    end
   end
  end
 end
 for i=1,3 do s:newCel(layers[i],f,ims[i],Point(0,0)) end
end
local tag=s:newTag(1,16); tag.name='single_burst_800ms'; tag.aniDir=AniDir.FORWARD
s:saveAs(s.filename)
local atlas=Image(96,512,ColorMode.RGB)
for f=1,16 do
 local frame=Image(96,32,ColorMode.RGB); frame:drawSprite(s,f,Point(0,0))
 frame:saveAs(root..'/exports/frames/dust_'..string.format('%02d',f)..'.png')
 atlas:drawImage(frame,Point(0,(f-1)*32))
end
atlas:saveAs(root..'/exports/ground_dust_v2-atlas.png')
for v=0,2 do
 local bank=Image(32,512,ColorMode.RGB)
 bank:drawImage(atlas,Point(-v*32,0))
 bank:saveAs(root..'/exports/ground_dust_v2-'..string.char(97+v)..'-strip.png')
end
s:saveCopyAs(root..'/previews/ground_dust_v2-original.gif')
local aux=Sprite(64,16,ColorMode.RGB); aux.layers[1].name='preview_only_flat_ground_and_scale_reference'
local im=Image(64,16,ColorMode.RGB)
for y=0,15 do for x=0,63 do
 local bank=math.floor(x/16); local lx=x%16; local h=hash(math.floor(lx/2),math.floor(y/2),bank+19)
 local col
 if bank==0 then col=({{99,115,69},{111,126,76},{89,103,64},{120,131,83}})[1+math.floor(h*4)]
 elseif bank==1 then col=({{106,82,59},{118,91,66},{127,101,74},{95,77,59}})[1+math.floor(h*4)]
 elseif bank==2 then col=({{83,78,68},{96,87,73},{103,93,78},{79,73,64}})[1+math.floor(h*4)]
 else col=({{100,121,139},{110,133,152},{91,113,132},{120,141,156}})[1+math.floor(h*4)] end
 if lx==0 or y==15 then col={math.floor(col[1]*0.88),math.floor(col[2]*0.88),math.floor(col[3]*0.88)} end
 im:drawPixel(x,y,app.pixelColor.rgba(col[1],col[2],col[3],255))
end end
aux:newCel(aux.layers[1],1,im,Point(0,0)); aux:saveAs(root..'/source/preview_auxiliary_palette.aseprite')
im:saveAs(root..'/exports/preview_auxiliary_palette.png')
return {source=s.filename,width=96,height=32,frames=16,layers=3,durationMs=800,layout='3 banks 32x32; vertical time',lastFrame='transparent',auxiliary='preview only'}
