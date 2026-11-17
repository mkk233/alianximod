var pal=['143F7A','126AAF','199ED2','66D8EA','C6F8FA','F5FFFF'];
var TAU=Math.PI*2;
function grid(w,h){var a=[];for(var y=0;y<h;y++){a[y]=[];for(var x=0;x<w;x++)a[y][x]=-1;}return a;}
function ring(f){
 var g=grid(128,64),p=TAU*f/16;
 for(var y=0;y<64;y++){for(var x=0;x<128;x++){
 var dx=(x+.5-64)/50,dy=(y+.5-30)/18,r=Math.sqrt(dx*dx+dy*dy),t=Math.atan2(dy,dx);
 var front=Math.max(0,Math.sin(t)),thick=.11+.065*front;
 var ripple=.014*Math.sin(5*t-p)+.009*Math.sin(9*t-2*p);
 var d=r-1-ripple,c=-1;
 if(d>=-thick&&d<=thick){
 var z=(d+thick)/(2*thick);c=2;
 if(z>.79)c=0;else if(z>.57)c=1;else if(z<.2)c=3;
 var flow=Math.sin(4*t-p+z*3);
 if(flow>.15&&z>.23&&z<.43)c=3;
 if(flow>.62&&z>.25&&z<.37)c=4;
 if(Math.sin(3*t-p+z*5)>.74&&z<.22){c=4;if(z>.05&&z<.14)c=5;}
 }
 if(c>=0)g[y][x]=c;
 }}
 // Small moving foam hooks follow the tangent of the ring, not a rotating bitmap.
 for(var j=0;j<3;j++){
 var a=p/3+j*TAU/3,extent=.28+.08*Math.sin(a*2);
 for(var k=0;k<28;k++){
 var q=k/27,angle=a-extent*q,rr=1-.07*Math.sin(q*Math.PI);
 var xx=Math.round(64+50*rr*Math.cos(angle)),yy=Math.round(30+18*rr*Math.sin(angle));
 if(xx>=0&&xx<128&&yy>=0&&yy<64){g[yy][xx]=4;if(q<.5&&yy+1<64)g[yy+1][xx]=5;}
 }
 }return g;
}
function band(f){var g=grid(128,16);for(var y=2;y<14;y++){for(var x=0;x<128;x++){
 var z=(y-2)/12,p=TAU*f/16,t=TAU*x/128,c=2;
 if(y>=12)c=0;else if(y>=9)c=1;else if(y<=3)c=3;
 var q=Math.sin(4*t-p+z*3);
 if(q>.15&&y>=5&&y<=7)c=3;
 if(q>.62&&y==6)c=4;
 if(Math.sin(3*t-p+z*5)>.74&&y<=4){c=4;if(y==3)c=5;}
 g[y][x]=c;
 }}return g;}
function paint(d,g,oy,name,section,foam){var l=d.artLayers.add();l.name=name;
 for(var c=0;c<6;c++){
 if(foam&&c<4)continue;if(!foam&&c>=4)continue;
 var parts=[];
 for(var y=0;y<g.length;y++){if(section=='back'&&y>=30)continue;if(section=='front'&&y<30)continue;
 for(var x=0;x<g[y].length;x++){if(g[y][x]!=c)continue;var e=x+1;while(e<g[y].length&&g[y][e]==c)e++;
 var a=[[x,y+oy],[e,y+oy],[e,y+1+oy],[x,y+1+oy]],pts=[];
 for(var k=0;k<4;k++){var p=new PathPointInfo();p.kind=PointKind.CORNERPOINT;p.anchor=a[k];p.leftDirection=a[k];p.rightDirection=a[k];pts.push(p);}
 var s=new SubPathInfo();s.closed=true;s.operation=ShapeOperation.SHAPEADD;s.entireSubPath=pts;parts.push(s);x=e-1;
 }}
 if(parts.length){var path=d.pathItems.add('水环像素',parts);path.makeSelection(0,false,SelectionType.REPLACE);var col=new SolidColor();col.rgb.hexValue=pal[c];d.selection.fill(col,ColorBlendMode.NORMAL,100,false);d.selection.deselect();path.remove();}
 }}

