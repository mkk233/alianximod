var srcFile=new File('D:/alianximod/docs/资源库/过程归档/水流长带-20261024/pixel-frames.json');srcFile.open('r');var data=eval('('+srcFile.read()+')');srcFile.close();
var pal=data.palette;
function part(kind,f){
 var h=12,g=[];if(kind=='upper')h=22;else if(kind=='middle')h=16;
 for(var y=0;y<h;y++){g[y]=[];for(var x=0;x<128;x++){
 var c=1;
 if(kind=='upper'){c=y<20?data.frames[f][y][x]:1;}
 else if(kind=='lower'){c=y<2?1:data.frames[f][y+20][x];}
 else {
 var q=((x-2*f)%32+32)%32;
 if(y>=3&&y<=5&&q>=3&&q<21)c=(y==3&&q<15)?3:2;
 var q2=((x-2*f+13)%64+64)%64;
 if(y>=10&&y<=11&&q2<27)c=2;
 }
 g[y][x]=c;
 }}return g;
}
function paint(doc,g,oy,name){
 var layer=doc.artLayers.add();layer.name=name;
 for(var c=0;c<6;c++){
 var parts=[];
 for(var y=0;y<g.length;y++){for(var x=0;x<128;x++){
 if(g[y][x]!=c)continue;var end=x+1;while(end<128&&g[y][end]==c)end++;
 var coords=[[x,y+oy],[end,y+oy],[end,y+1+oy],[x,y+1+oy]],pts=[];
 for(var k=0;k<4;k++){var p=new PathPointInfo();p.kind=PointKind.CORNERPOINT;p.anchor=coords[k];p.leftDirection=coords[k];p.rightDirection=coords[k];pts.push(p);}
 var s=new SubPathInfo();s.closed=true;s.operation=ShapeOperation.SHAPEADD;s.entireSubPath=pts;parts.push(s);x=end-1;
 }}
 if(parts.length){var path=doc.pathItems.add('水带像素',parts);path.makeSelection(0,false,SelectionType.REPLACE);var col=new SolidColor();col.rgb.hexValue=pal[c];doc.selection.fill(col,ColorBlendMode.NORMAL,100,false);doc.selection.deselect();path.remove();}
 }
}
