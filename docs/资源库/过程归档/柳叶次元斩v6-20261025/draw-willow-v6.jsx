var W=96,H=48,N=16,out="D:/alianximod/docs/资源库/特效/次元斩/柳叶单斩_v6/";
var pal=["131226","42316E","7560CD","B3A0F5","F3F1FF","728FE0"];
function drawFrames(doc,from,to){
for(var f=from;f<to;f++){var layer=doc.artLayers.add();layer.name="frame_"+("0"+f).slice(-2)+"_50ms";doc.activeLayer=layer;
var grid=[],q=Math.max(0,Math.min(1,(f-4)/8)),remaining=1-q*q,L=f<4?[18,42,66,78][f]:Math.max(1,78*remaining);
var width=f<4?[0.5,1.2,3.5,5.7][f]:5.7*remaining;var tilt=0.14,curve=1.5*remaining;
for(var y=0;y<H;y++){grid[y]=[];for(var x=0;x<W;x++){var c=-1,t=(x+0.5-(48-L/2))/L;
if(f==12){if(x==48&&y==24)c=4;}
else if(f<12&&t>=0&&t<=1){var cy=24.5-tilt*(x+0.5-48)-curve*Math.sin(Math.PI*t);var half=width*Math.pow(Math.sin(Math.PI*t),0.85)*(0.84+0.32*t),dy=y+0.5-cy,dist=Math.abs(dy);
if(dist<=Math.max(0.5,half)){if(f<2)c=4;else{var edge=half-dist;c=0;if(edge<1.1)c=dy<0?3:2;else if(edge<2)c=dy<0?2:1;
if(dy<0&&edge<0.7&&t>0.04&&t<0.96)c=4;if(dy>0&&edge<0.8&&t>0.5)c=5;if(half<1)c=3;}}
}grid[y][x]=c;}}

function put(x,y,c){x=Math.round(x);y=Math.round(y);if(x>=0&&x<W&&y>=0&&y<H)grid[y][x]=c;}
function stroke(ax,ay,bx,by,c){var steps=Math.max(Math.abs(bx-ax),Math.abs(by-ay));for(var z=0;z<=steps;z++){var t=steps?z/steps:0;put(ax+(bx-ax)*t,ay+(by-ay)*t,c);}}
function tri(a,b,c){var loX=Math.floor(Math.min(a[0],b[0],c[0])),hiX=Math.ceil(Math.max(a[0],b[0],c[0])),loY=Math.floor(Math.min(a[1],b[1],c[1])),hiY=Math.ceil(Math.max(a[1],b[1],c[1]));for(var yy=loY;yy<=hiY;yy++)for(var xx=loX;xx<=hiX;xx++){var e1=(xx-a[0])*(b[1]-a[1])-(yy-a[1])*(b[0]-a[0]),e2=(xx-b[0])*(c[1]-b[1])-(yy-b[1])*(c[0]-b[0]),e3=(xx-c[0])*(a[1]-c[1])-(yy-c[1])*(a[0]-c[0]);if((e1>=0&&e2>=0&&e3>=0)||(e1<=0&&e2<=0&&e3<=0))put(xx,yy,0);}stroke(a[0],a[1],b[0],b[1],f<7?4:3);stroke(b[0],b[1],c[0],c[1],2);}
if(f>=2&&f<=11){
var seeds=[[26,18,-1,11,5],[44,15,-1,14,6],[67,16,-1,10,4],[36,30,1,13,5],[63,28,1,15,6]];
var grow=f<4?(f-1)/2:Math.max(0.1,1-Math.pow((f-4)/8,2)),drift=Math.min(4,(f-2)*0.7);
for(var si=0;si<seeds.length;si++){var ss=seeds[si],cx=48+(ss[0]-48)*grow,cy=24+(ss[1]-24)*grow+ss[2]*drift*grow;
tri([cx-ss[3]*grow/2,cy],[cx+ss[3]*grow/2,cy-1.5*grow],[cx+(si%2?2:-3)*grow,cy+ss[2]*ss[4]*grow]);}
if(f<=6){var bright=f<=4?4:3;stroke(24,24,20,17,bright);stroke(20,17,15,15,2);stroke(59,23,66,31,bright);stroke(66,31,74,33,2);stroke(73,18,78,11,3);}
}
if(f>=12&&f<=14){var size=[5,3,1][f-12];stroke(47-size,24,47,23,3);stroke(49,25,49+size,24,f==12?4:2);}

for(var c=0;c<pal.length;c++){var parts=[];for(var y=0;y<H;y++){var x=0;while(x<W){if(grid[y][x]!=c){x++;continue;}var e=x+1;while(e<W&&grid[y][e]==c)e++;var coords=[[x,y+H*f],[e,y+H*f],[e,y+1+H*f],[x,y+1+H*f]],pts=[];
for(var p=0;p<4;p++){var pt=new PathPointInfo();pt.kind=PointKind.CORNERPOINT;pt.anchor=coords[p];pt.leftDirection=coords[p];pt.rightDirection=coords[p];pts.push(pt);}var sub=new SubPathInfo();sub.closed=true;sub.operation=ShapeOperation.SHAPEADD;sub.entireSubPath=pts;parts.push(sub);x=e;}}
if(parts.length){var path=doc.pathItems.add("willow-v2-pixels",parts);path.makeSelection(0,false,SelectionType.REPLACE);var color=new SolidColor();color.rgb.hexValue=pal[c];doc.selection.fill(color,ColorBlendMode.NORMAL,100,false);doc.selection.deselect();path.remove();}}
}}






