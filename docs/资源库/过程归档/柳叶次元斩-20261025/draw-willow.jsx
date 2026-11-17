var W=96,H=48;var out="D:/alianximod/docs/资源库/特效/次元斩/柳叶单斩_v1/";
var lengths=[10,25,46,64,74,78,76,76,74,68,58,46,34,24,12,0];
var widths=[0.5,0.65,0.9,2.8,4.8,5.7,5.4,4.1,2.8,1.8,1.1,0.7,0.5,0.45,0.4,0];
var pal=["131226","42316E","7560CD","B3A0F5","F3F1FF","728FE0"];
function drawFrames(doc,from,to){
for(var f=from;f<to;f++){var layer=doc.artLayers.add();layer.name="frame_"+("0"+f).slice(-2)+"_50ms";doc.activeLayer=layer;var grid=[];
for(var y=0;y<H;y++){grid[y]=[];for(var x=0;x<W;x++){var c=-1,L=lengths[f],t=(x+0.5-(48-L/2))/L;
if(f<15&&t>=0&&t<=1){var cy=24-0.14*(x+0.5-48)-1.5*Math.sin(Math.PI*t);
var half=widths[f]*Math.pow(Math.sin(Math.PI*t),0.85)*(0.84+0.32*t),dy=y+0.5-cy,dist=Math.abs(dy);
if(dist<=Math.max(0.48,half)){
if(f>=12){if(((x+f*2)%11)<7)c=f==12?3:(f==13?2:1);}
else if(f<3)c=dist<0.4?4:3;
else {var edge=half-dist;c=0;if(edge<1.1)c=dy<0?3:2;else if(edge<2)c=dy<0?2:1;
if(dy<0&&edge<0.65&&t>0.08&&t<0.94&&((x+3)%19)<13)c=4;
if(dy>0&&edge<0.8&&t>0.5)c=5;
if(half<1.0)c=3;
}}
}grid[y][x]=c;}}
for(var c=0;c<pal.length;c++){var parts=[];for(var y=0;y<H;y++){var x=0;while(x<W){if(grid[y][x]!=c){x++;continue;}var e=x+1;while(e<W&&grid[y][e]==c)e++;
var coords=[[x,y+H*f],[e,y+H*f],[e,y+1+H*f],[x,y+1+H*f]],pts=[];
for(var p=0;p<4;p++){var pt=new PathPointInfo();pt.kind=PointKind.CORNERPOINT;pt.anchor=coords[p];pt.leftDirection=coords[p];pt.rightDirection=coords[p];pts.push(pt);}
var sub=new SubPathInfo();sub.closed=true;sub.operation=ShapeOperation.SHAPEADD;sub.entireSubPath=pts;parts.push(sub);x=e;}}
if(parts.length){var path=doc.pathItems.add("willow-pixels",parts);path.makeSelection(0,false,SelectionType.REPLACE);var color=new SolidColor();color.rgb.hexValue=pal[c];doc.selection.fill(color,ColorBlendMode.NORMAL,100,false);doc.selection.deselect();path.remove();}
}}}
