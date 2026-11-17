
local s=sprite
local b=Sprite(224,100,ColorMode.RGB)
local bg=b.layers[1];bg.name="Preview_background_only"
local art=b:newLayer();art.name="Exact_v3_frames"
local rgb=app.pixelColor.rgba
local font={
D={"1110","1001","1001","1001","1001","1001","1110"},
A={"0110","1001","1001","1111","1001","1001","1001"},
R={"1110","1001","1001","1110","1010","1001","1001"},
K={"1001","1010","1100","1000","1100","1010","1001"},
L={"1000","1000","1000","1000","1000","1000","1111"},
I={"111","010","010","010","010","010","111"},
G={"0111","1000","1000","1011","1001","1001","0111"},
H={"1001","1001","1001","1111","1001","1001","1001"},
T={"11111","00100","00100","00100","00100","00100","00100"}
}
local function rect(im,x,y,w,h,c)
 for yy=y,y+h-1 do for xx=x,x+w-1 do im:drawPixel(xx,yy,c) end end
end
local function label(im,text,x,y,c)
 for j=1,#text do local ch=text:sub(j,j);local g=font[ch];if g then for yy,row in ipairs(g) do for xx=1,#row do if row:sub(xx,xx)=="1" then im:drawPixel(x+xx-1,y+yy-1,c) end end end;x=x+#g[1]+2 else x=x+3 end end
end
local bi=Image(224,100,ColorMode.RGB);bi:clear(rgb(9,15,23,255))
rect(bi,4,4,105,92,rgb(20,32,41,255));rect(bi,115,4,105,92,rgb(185,196,190,255))
label(bi,"DARK",12,10,rgb(145,180,185,255));label(bi,"LIGHT",123,10,rgb(56,76,78,255))
rect(bi,14,82,84,1,rgb(44,65,70,255));rect(bi,125,82,84,1,rgb(141,158,151,255))
for f=1,16 do
 if f>1 then b:newEmptyFrame(f) end;b.frames[f].duration=.08
 b:newCel(bg,f,bi,Point(0,0))
 local ai=Image(224,100,ColorMode.RGB)
 ai:drawSprite(s,f,Point(24,23));ai:drawSprite(s,f,Point(135,23))
 b:newCel(art,f,ai,Point(0,0))
end
b:saveAs(args.out.."/youming_cyan_soul_v3_preview.aseprite")
local first=Image(224,100,ColorMode.RGB);first:drawSprite(b,5);first:resize(672,300);first:saveAs(args.out.."/youming_cyan_soul_v3_preview.png")
return {file=b.filename,frames=#b.frames}

