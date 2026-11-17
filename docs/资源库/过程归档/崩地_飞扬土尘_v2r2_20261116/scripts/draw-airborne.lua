local root=args.root
local spr=Sprite(144,48,ColorMode.RGB)
spr.filename=root..'/source/ground_airborne_v2r2.aseprite'
local layers={spr.layers[1],spr:newLayer(),spr:newLayer()}
layers[1].name='01_thin_detached_dust'
layers[2].name='02_irregular_soil_clusters'
layers[3].name='03_flying_pixel_grit'
for frame=2,24 do spr:newEmptyFrame() end
local palette={{91,65,43},{126,91,56},{155,114,70},{181,140,91},{204,167,117},{168,139,104}}
local function hash(first,second,third) return ((first*7387+second*3253+third*977+127)%997)/997 end
local function pixel(image,x,y,tone,alpha,bank)
 if x>=bank*48+2 and x<bank*48+46 and y>=2 and y<46 and alpha>0 then
  local color=palette[tone]
  image:drawPixel(x,y,app.pixelColor.rgba(color[1],color[2],color[3],math.floor(alpha)))
 end
end
local shapes={
 {{2,3},{1,5},{0,7},{0,8},{1,7},{2,5},{3,3}},
 {{3,2},{1,5},{0,6},{1,7},{0,7},{2,5},{2,4},{3,2}},
 {{1,3},{0,5},{0,6},{1,6},{1,5},{2,3}}
}
for frame=1,24 do
 spr.frames[frame].duration=0.05
 local age=(frame-1)*0.05
 local images={Image(144,48,ColorMode.RGB),Image(144,48,ColorMode.RGB),Image(144,48,ColorMode.RGB)}
 if frame<24 then
  for bank=0,2 do
   for cluster=0,9 do
    local seed=cluster+bank*23
    local delay=(cluster%3)*0.025
    local localAge=age-delay
    if localAge>=0 then
     local side=(hash(seed,2,9)-0.5)*2
     local centerX=24+side*(3+localAge*12)+(bank-1)*localAge*2
     local rise=(10+hash(seed,4,5)*12)*(1-math.exp(-localAge*6))
     local centerY=41-rise+localAge*localAge*3-(cluster%4)*1.1
     local fade=math.max(0,1-math.max(0,localAge-0.22)/0.88)
     local alpha=math.min(1,localAge*16+0.20)*fade*(205+hash(seed,4,7)*35)
     local breakup=math.max(0,(localAge-0.28)/0.88)
     local shape=shapes[1+(seed%3)]
     for row,span in ipairs(shape) do
      for column=span[1],span[1]+span[2]-1 do
       local noise=hash(math.floor(column/2),math.floor(row/2),seed)
       if noise>breakup*0.78 then
        local scatterX=localAge>0.5 and math.floor(side*(localAge-0.5)*7) or 0
        local scatterY=localAge>0.5 and math.floor((noise-0.4)*(localAge-0.5)*8) or 0
        local px=bank*48+math.floor(centerX+column-3)+scatterX
        local py=math.floor(centerY+row-4)+scatterY
        local tone=row<3 and 5 or (row>5 and 1 or (noise<0.45 and 2 or 3))
        if column<2 and row>3 then tone=2 end
        pixel(images[2],px,py,tone,alpha,bank)
        if cluster%2==0 then pixel(images[1],px+1,py-2,6,alpha*0.24,bank) end
       end
      end
     end
    end
   end
   for grain=0,15 do
    local seed=grain+bank*19
    local side=(hash(seed,5,2)-0.5)*2
    local travel=age/1.15
    local px=bank*48+math.floor(24+side*(4+travel*18))
    local py=math.floor(42-(28+hash(seed,3,8)*12)*travel+21*travel*travel)
    local fade=math.max(0,1-math.max(0,age-0.25)/0.85)
    local alpha=fade*(130+hash(seed,3,2)*90)
    pixel(images[3],px,py,grain%3==0 and 2 or 4,alpha,bank)
    if grain%5==0 and age<0.45 then pixel(images[3],px+1,py,3,alpha,bank) end
   end
  end
 end
 for index=1,3 do spr:newCel(layers[index],frame,images[index],Point(0,0)) end
end
local tag=spr:newTag(1,24)
tag.name='single_airborne_burst_1200ms'
spr:saveAs(spr.filename)
local atlas=Image(144,1152,ColorMode.RGB)
for frame=1,24 do
 local image=Image(144,48,ColorMode.RGB)
 image:drawSprite(spr,frame,Point(0,0))
 image:saveAs(root..'/exports/frames/dust_'..string.format('%02d',frame)..'.png')
 atlas:drawImage(image,Point(0,(frame-1)*48))
end
atlas:saveAs(root..'/exports/ground_airborne_v2r2-atlas.png')
for bank=0,2 do
 local strip=Image(48,1152,ColorMode.RGB)
 strip:drawImage(atlas,Point(-bank*48,0))
 strip:saveAs(root..'/exports/ground_airborne_v2r2-'..string.char(97+bank)..'-strip.png')
end
spr:saveCopyAs(root..'/previews/ground_airborne_v2r2-original.gif')
local grain=Sprite(24,8,ColorMode.RGB)
grain.layers[1].name='visual_grit_three_tones_four_alpha_phases'
for frame=2,4 do grain:newEmptyFrame() end
local grainAtlas=Image(24,32,ColorMode.RGB)
local opacities={255,160,64,0}
for frame=1,4 do
 grain.frames[frame].duration=0.05
 local image=Image(24,8,ColorMode.RGB)
 for bank=0,2 do
  for yy=0,7 do for xx=0,7 do
   local color=palette[2+bank+(hash(math.floor(xx/2),math.floor(yy/2),bank)>0.65 and 1 or 0)]
   image:drawPixel(bank*8+xx,yy,app.pixelColor.rgba(color[1],color[2],color[3],opacities[frame]))
  end end
 end
 grain:newCel(grain.layers[1],frame,image,Point(0,0))
 grainAtlas:drawImage(image,Point(0,(frame-1)*8))
end
grain:saveAs(root..'/source/visual_grit_v2r2.aseprite')
grainAtlas:saveAs(root..'/exports/visual_grit_v2r2-atlas.png')
return {source=spr.filename,width=144,height=48,frames=24,layers=3,frameMs=50,lifetimeMs=1200,finalFrame='fully transparent',style='hand-defined stepped pixel clusters, no smooth filtering'}
