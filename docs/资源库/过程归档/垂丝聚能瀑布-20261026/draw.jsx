var pal=['143F7A','126AAF','199ED2','66D8EA','C6F8FA','F5FFFF'];
function mod(a,n){return (a%n+n)%n;}
function waterfall(f,w){
 var g=[],left=32-w/2;
 for(var y=0;y<128;y++){g[y]=[];
 for(var x=0;x<64;x++){
 var u=x-left,c=-1;
 var sway=Math.round(.7*Math.sin(2*Math.PI*(y-8*f)/32));
 if(u>=sway&&u<w+sway){
 var edge=Math.min(u-sway,w-1+sway-u);
 c=2;if(edge<1)c=0;else if(edge<3)c=1;
 var lane=mod(u,16),flow=mod(y-8*f+Math.floor(lane/4)*13,64);
 if(edge>=3){
 if(lane<3)c=1;
 if(lane>=5&&lane<=7)c=3;
 if(lane==6&&flow<36)c=4;
 if(lane==6&&flow>=26&&flow<34)c=5;
 if(lane>=11&&lane<=12&&flow>20&&flow<54)c=3;
 if(lane==11&&flow>43&&flow<51)c=4;
 }
 if(edge>=1&&edge<3&&mod(y-8*f,32)<20)c=3;
 if(edge>=1&&edge<2&&mod(y-8*f,32)>12&&mod(y-8*f,32)<18)c=4;
 }
 g[y][x]=c;
 }}return g;
}
function paint(d,g,f){
 var layer=d.artLayers.add();layer.name='frame_'+('0'+f).slice(-2)+'_100ms';
 for(var c=0;c<6;c++){
 var parts=[];
 for(var y=0;y<128;y++){for(var x=0;x<64;x++){
 if(g[y][x]!=c)continue;var e=x+1;while(e<64&&g[y][e]==c)e++;
 var coords=[[x,y+128*f],[e,y+128*f],[e,y+1+128*f],[x,y+1+128*f]],pts=[];
 for(var k=0;k<4;k++){var p=new PathPointInfo();p.kind=PointKind.CORNERPOINT;p.anchor=coords[k];p.leftDirection=coords[k];p.rightDirection=coords[k];pts.push(p);}
 var s=new SubPathInfo();s.closed=true;s.operation=ShapeOperation.SHAPEADD;s.entireSubPath=pts;parts.push(s);x=e-1;
 }}
 if(parts.length){var path=d.pathItems.add('瀑布像素',parts);path.makeSelection(0,false,SelectionType.REPLACE);var col=new SolidColor();col.rgb.hexValue=pal[c];d.selection.fill(col,ColorBlendMode.NORMAL,100,false);d.selection.deselect();path.remove();}
 }
}
