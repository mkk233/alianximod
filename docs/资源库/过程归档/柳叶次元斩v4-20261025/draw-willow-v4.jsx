var W=96,H=48,N=26,out="D:/alianximod/docs/资源库/特效/次元斩/柳叶单斩_v4/";
var pal=["131226","42316E","7560CD","B3A0F5","F3F1FF","728FE0"];
function drawFrames(doc,from,to){
for(var f=from;f<to;f++){var layer=doc.artLayers.add();layer.name="frame_"+("0"+f).slice(-2)+"_50ms";doc.activeLayer=layer;
var grid=[],q=Math.max(0,Math.min(1,(f-9)/11)),remaining=1-q,L=f<4?[18,42,66,78][f]:78;
var width=f<4?[0.5,1.2,3.5,5.7][f]:5.7*remaining;var tilt=0.14,curve=1.5*remaining;
for(var y=0;y<H;y++){grid[y]=[];for(var x=0;x<W;x++){var c=-1,t=(x+0.5-(48-L/2))/L;
if(f>=20&&f<25){var span=[78,54,30,12,1][f-20],left=Math.floor(48-span/2);if(y==Math.round(24-0.14*(x+0.5-48))&&x>=left&&x<left+span)c=[4,4,3,3,4][f-20];}
else if(f<20&&t>=0&&t<=1){var cy=24.5-tilt*(x+0.5-48)-curve*Math.sin(Math.PI*t);var half=width*Math.pow(Math.sin(Math.PI*t),0.85)*(0.84+0.32*t),dy=y+0.5-cy,dist=Math.abs(dy);
if(dist<=Math.max(0.5,half)){if(f<2)c=4;else{var edge=half-dist;c=0;if(edge<1.1)c=dy<0?3:2;else if(edge<2)c=dy<0?2:1;
if(dy<0&&edge<0.7&&t>0.04&&t<0.96)c=4;if(dy>0&&edge<0.8&&t>0.5)c=5;if(half<1)c=3;}}
}grid[y][x]=c;}}
for(var c=0;c<pal.length;c++){var parts=[];for(var y=0;y<H;y++){var x=0;while(x<W){if(grid[y][x]!=c){x++;continue;}var e=x+1;while(e<W&&grid[y][e]==c)e++;var coords=[[x,y+H*f],[e,y+H*f],[e,y+1+H*f],[x,y+1+H*f]],pts=[];
for(var p=0;p<4;p++){var pt=new PathPointInfo();pt.kind=PointKind.CORNERPOINT;pt.anchor=coords[p];pt.leftDirection=coords[p];pt.rightDirection=coords[p];pts.push(pt);}var sub=new SubPathInfo();sub.closed=true;sub.operation=ShapeOperation.SHAPEADD;sub.entireSubPath=pts;parts.push(sub);x=e;}}
if(parts.length){var path=doc.pathItems.add("willow-v2-pixels",parts);path.makeSelection(0,false,SelectionType.REPLACE);var color=new SolidColor();color.rgb.hexValue=pal[c];doc.selection.fill(color,ColorBlendMode.NORMAL,100,false);doc.selection.deselect();path.remove();}}
}}




