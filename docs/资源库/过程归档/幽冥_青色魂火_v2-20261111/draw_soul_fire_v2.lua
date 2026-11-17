
local out = args.out
local s = Sprite(64,64,ColorMode.RGB)
local layers={s.layers[1],s:newLayer(),s:newLayer(),s:newLayer()}
layers[1].name="01_deep_teal_mantle"
layers[2].name="02_cyan_flame"
layers[3].name="03_pale_soul_core"
layers[4].name="04_rising_embers"
local rgb=app.pixelColor.rgba
local C={rgb(11,56,67,255),rgb(8,92,102,255),rgb(7,149,162,255),rgb(14,196,204,255),rgb(68,237,231,255),rgb(167,255,240,255),rgb(226,255,244,255)}
local function pix(im,x,y,c)
 x=math.floor(x)*2;y=math.floor(y)*2
 for yy=y,y+1 do for xx=x,x+1 do if xx>=0 and xx<64 and yy>=0 and yy<64 then im:drawPixel(xx,yy,c) end end end
end
local function poly(im,p,c)
 local minx,maxx,miny,maxy=32,0,32,0
 for _,v in ipairs(p) do minx=math.min(minx,v[1]);maxx=math.max(maxx,v[1]);miny=math.min(miny,v[2]);maxy=math.max(maxy,v[2]) end
 for y=math.floor(miny),math.ceil(maxy) do for x=math.floor(minx),math.ceil(maxx) do
 local inside=false;local j=#p
 for i=1,#p do local a,b=p[i],p[j]
 if ((a[2]>y+.5)~=(b[2]>y+.5)) and (x+.5<(b[1]-a[1])*(y+.5-a[2])/(b[2]-a[2])+a[1]) then inside=not inside end
 j=i end
 if inside then pix(im,x,y,c) end
 end end
end
local function shifted(p,x,y)
 local q={};for _,v in ipairs(p) do q[#q+1]={v[1]+x,v[2]+y} end;return q
end
local tongues={{7,28,6,14,.4},{12,28,7,19,2.2},{18,28,8,23,.8},{24,28,6,17,3.8},{27,28,4,10,1.7}}
for f=1,16 do
 if f>1 then s:newEmptyFrame(f) end
 s.frames[f].duration=.08
 local im={Image(64,64,ColorMode.RGB),Image(64,64,ColorMode.RGB),Image(64,64,ColorMode.RGB),Image(64,64,ColorMode.RGB)}
 local t=(f-1)*math.pi*2/16
 for n,v in ipairs(tongues) do
 local x,y,w,h,ph=v[1],v[2],v[3],v[4],v[5]
 h=h+math.floor(1.7*math.sin(t+ph)+.5)
 local bend=math.floor(2.3*math.sin(t+ph+.7)+.5)
 local p={{-w*.50,0},{-w*.62,-3},{-w*.39,-h*.33},{-w*.48,-h*.49},{-w*.15,-h*.69},{bend-1,-h},{bend+1,-h+1},{bend+1,-h*.70},{w*.24,-h*.54},{w*.15,-h*.39},{w*.55,-h*.20},{w*.53,-1}}
 poly(im[1],shifted(p,x,y),C[2])
 -- broad broken turquoise tongue, leaving one-pixel petrol edges
 local q={{-w*.25,-1},{-w*.42,-4},{-w*.14,-h*.36},{-w*.25,-h*.55},{bend,-h+2},{bend,-h*.64},{w*.12,-h*.42},{w*.34,-h*.22},{w*.32,-1}}
 poly(im[2],shifted(q,x,y),C[4])
 -- offset light blade, with a broken rising band
 local sway=math.floor(math.sin(t+ph))
 local r={{-1+sway,-1},{-2+sway,-5},{sway,-h*.44},{bend,-h*.64},{1+bend,-h*.52},{1+sway,-h*.29},{2+sway,-3},{1+sway,-1}}
 poly(im[3],shifted(r,x,y),C[5])
 local pulse=(f*2+n*5)%12
 local hy=y-4-pulse
 if hy>y-h+5 then
 pix(im[3],x+sway,hy,C[6]);pix(im[3],x+sway,hy+1,C[6])
 if n%2==0 then pix(im[3],x+sway+1,hy+1,C[7]) end
 end
 if n==2 or n==3 then
 poly(im[3],shifted({{-1,-1},{-2,-3},{-1,-6},{1,-9},{1,-5},{2,-3},{1,-1}},x+sway,y),C[6])
 end
 -- small side tongues fork away from the main column
 local side=(n%2==0) and 1 or -1
 poly(im[2],shifted({{side*2,-5},{side*4,-8},{side*4,-11},{side*3,-9},{side,-6}},x,y),C[3])
 end
 -- uneven luminous coal-free base, with little transparent gaps
 for x=6,27 do
 if (x+f)%9~=0 and (x+2*f)%13~=0 then pix(im[2],x,28,C[3]) end
 if x%4~=0 then pix(im[3],x,27,C[4]) end
 end
 -- square sparks detach and rise; no screen-space blur
 for n=1,7 do
 local age=((f-1)+n*3)%16
 local ex=5+(n*7)%24+math.floor(math.sin(t+n)*1.0)
 local ey=24-age-(n%3)*4
 if age<12 and ey>1 then
 local col=age<5 and C[6] or (age<9 and C[5] or C[3])
 pix(im[4],ex,ey,col)
 if n%3==0 and age<7 then pix(im[4],ex,ey+1,C[4]) end
 end
 end
 for k=1,4 do s:newCel(layers[k],f,im[k],Point(0,0)) end
end
local tag=s:newTag(1,16);tag.name="soul_flame_loop_1280ms"
s.data="youming-cyan-soul-v2; 64x64;16frames;80ms; 32px logical blocks at 2x; Aseprite MCP; draft only"
s:saveAs(out.."/youming_cyan_soul_v2.aseprite")
local sheet=Image(64,1024,ColorMode.RGB)
for f=1,16 do sheet:drawSprite(s,f,Point(0,(f-1)*64)) end
sheet:saveAs(out.."/youming_cyan_soul_v2_strip.png")
local first=Image(64,64,ColorMode.RGB);first:drawSprite(s,1);first:saveAs(out.."/youming_cyan_soul_v2_frame01.png")
return {file=s.filename,width=s.width,height=s.height,frames=#s.frames,layers=#s.layers,duration_ms=80}
