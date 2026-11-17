local out='D:/alianximod/docs/资源库/过程归档/拔刀斩_v1-20261102/'
local s=Sprite(64,64,ColorMode.RGB)
local pc=app.pixelColor
local function C(h) return pc.rgba(tonumber(h:sub(1,2),16),tonumber(h:sub(3,4),16),tonumber(h:sub(5,6),16),255) end
local base=Image(64,64,ColorMode.RGB);local detail=Image(64,64,ColorMode.RGB)
local colors={'25354A','CD9977','CD9977','25252D','30465E','242C3B','1B202B','C7B79C','A6D6E3','4B7288','C8A45E','202C3B','5A2732','171C29','EAF9FF','424E5B'}
local function rect(im,x,y,w,h,col) for py=y,y+h-1 do for px=x,x+w-1 do im:drawPixel(px,py,C(col)) end end end
for i,col in ipairs(colors) do local k=i-1;rect(base,(k%4)*16,math.floor(k/4)*16,16,16,col) end
-- Dark indigo uniform, clean pixels so the motion stays readable.
rect(detail,0,0,3,16,'172738');rect(detail,13,0,3,16,'1B2D40')
for y=0,10 do rect(detail,math.floor(y/2)+3,y,2,1,'B6B6A5');rect(detail,10-math.floor(y/2),y,2,1,'82949C') end
rect(detail,1,12,14,3,'A99474');rect(detail,6,12,3,3,'645849')
rect(detail,16,0,2,16,'B98065');rect(detail,29,1,2,14,'E1B190')
-- Face tile: hair, eyebrows and eyes face north in the model.
rect(detail,32,0,16,4,'25252D');rect(detail,32,4,2,6,'25252D');rect(detail,46,4,2,6,'25252D')
rect(detail,35,7,3,1,'594039');rect(detail,42,7,3,1,'594039')
rect(detail,35,8,3,2,'F4E4CB');rect(detail,42,8,3,2,'F4E4CB');rect(detail,37,8,1,2,'293C47');rect(detail,42,8,1,2,'293C47')
rect(detail,39,10,2,2,'AE755F');rect(detail,38,13,4,1,'A36B59')
rect(detail,50,1,3,12,'343640');rect(detail,56,2,2,13,'30313A');rect(detail,61,0,2,10,'1B2029')
rect(detail,0,16,2,16,'1C2D40');rect(detail,13,16,3,16,'233A51');rect(detail,2,29,11,2,'B7AB91')
rect(detail,17,16,3,16,'1A2332');rect(detail,26,17,2,14,'303C4D');rect(detail,33,16,3,16,'29303C')
rect(detail,48,17,16,2,'E0CFB0');rect(detail,48,28,16,2,'9C8C71')
-- Steel blade: broad bright cutting edge and a cool spine.
rect(detail,0,32,4,16,'F1FFFF');rect(detail,4,32,3,16,'D5F1F6');rect(detail,12,32,4,16,'628BA1')
rect(detail,16,32,3,16,'7298AB');rect(detail,28,32,4,16,'365165')
rect(detail,32,32,16,2,'E6C77E');rect(detail,32,45,16,3,'8C693B')
for y=0,15 do local x=y%6;rect(detail,49+x,y+32,2,1,'D7CBB5');rect(detail,58-x,y+32,2,1,'B1A891') end
-- Dark burgundy scabbard with a bright lip.
rect(detail,0,48,16,2,'BD9855');rect(detail,0,50,3,14,'773747');rect(detail,12,50,4,14,'351F2A');rect(detail,2,61,10,2,'B49253')
rect(detail,16,48,2,16,'0E131E');rect(detail,31,48,1,16,'2A3542')
rect(detail,32,48,16,3,'FFFFFF');rect(detail,32,61,16,3,'82CCE6')
s.layers[1].name='材质底色';s:newCel(s.layers[1],1,base,Point(0,0));local layer=s:newLayer();layer.name='像素细节';s:newCel(layer,1,detail,Point(0,0))
s:saveAs(out..'iaido-atlas.aseprite');local flat=Image(64,64,ColorMode.RGB);flat:drawSprite(s,1);flat:saveAs(out..'iaido-atlas.png')
app.activeSprite=s;app.command.SpriteSize{ui=false,width=512,height=512,method='nearest'};local preview=Image(512,512,ColorMode.RGB);preview:drawSprite(s,1);preview:saveAs(out..'atlas-preview.png')
return {width=64,height=64,frames=1,layers=2,tiles=16}
