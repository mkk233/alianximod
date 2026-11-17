import fs from 'node:fs';
const proc='D:/alianximod/docs/资源库/过程归档/卡通水流-20261024/';
const out='D:/alianximod/docs/资源库/特效/水流/卡通水弹_v1/';
const TAU=Math.PI*2, N=16, W=32;
const palette=['125B91','168BC5','2CBEDF','79E4ED','F0FFF9'];
function frame(f){
 const p=TAU*(f%N)/N,g=Array.from({length:32},()=>Array(32).fill(-1));
 const cx=22.2,cy=16,rx=7.3+.25*Math.sin(p),ry=7.1+.45*Math.cos(p);
 const mask=Array.from({length:32},()=>Array(32).fill(false));
 for(let y=0;y<32;y++)for(let x=0;x<32;x++){
  const X=x+.5,Y=y+.5;
  const head=((X-cx)/rx)**2+((Y-cy)/ry)**2<1;
  const wave=Math.sin(X*.32-p), tailY=17.3+1.35*wave;
  const tail=X>=5.5+.8*Math.cos(p)&&X<23&&Math.abs(Y-tailY)<.55+(X-5.5)*.23;
  const upper=X>7+.5*Math.sin(p)&&X<21&&Math.abs(Y-(11.3+1.25*Math.sin(X*.27-p)))<.6+(X-7)*.065;
  const lower=X>10+.6*Math.cos(p)&&X<24&&Math.abs(Y-(21.5+1.0*Math.sin(X*.31-p+1)))<.55+(X-10)*.07;
  if(!(head||tail||upper||lower))continue;
  mask[y][x]=true;let c=1;
  if(Y<16+.8*Math.sin(X*.3-p))c=2;
  if(Y>20+.7*Math.sin(X*.35-p))c=0;
  if(X>26.5&&Y>16)c=0;
  if(tail&&Y<tailY-0.4)c=2;
  g[y][x]=c;
 }
 function disc(cx,cy,r,c,clip=true){for(let y=Math.max(0,Math.floor(cy-r));y<Math.min(32,Math.ceil(cy+r));y++)for(let x=Math.max(0,Math.floor(cx-r));x<Math.min(32,Math.ceil(cx+r));x++){if((x+.5-cx)**2+(y+.5-cy)**2<=r*r&&(!clip||mask[y][x]))g[y][x]=c;}}
 function stroke(points,r,c,clip=true){for(let k=1;k<points.length;k++){const a=points[k-1],b=points[k],steps=Math.ceil(Math.hypot(b[0]-a[0],b[1]-a[1])*4);for(let s=0;s<=steps;s++){let q=s/(steps||1);disc(a[0]+q*(b[0]-a[0]),a[1]+q*(b[1]-a[1]),r,c,clip);}}}
 // Leading crescent: the lower segment rolls inward; the top and nose stay readable.
 const arc=[];for(let i=0;i<=32;i++){const a=(-155+i*(212+8*Math.sin(p))/32)*Math.PI/180;arc.push([cx+(rx-1.1)*Math.cos(a),cy+(ry-1.1)*Math.sin(a)]);}
 stroke(arc,1.65,2);stroke(arc,1.05,3);
 stroke(arc.slice(7,24),.55,4);
 // Inner fold changes curvature, carrying two short highlights forward.
 const fold=[];for(let i=0;i<=28;i++){const a=(-130+i*235/28+10*Math.sin(p))*Math.PI/180;fold.push([22.2+(3.8+.25*Math.cos(p))*Math.cos(a),16.1+(3.0+.3*Math.sin(p))*Math.sin(a)]);}
 stroke(fold,1.1,1);stroke(fold.slice(2,23),.65,2);stroke(fold.slice(4,12),.55,3);
 for(let j=0;j<2;j++){
  const phase=((f/N+j*.5)%1),x0=8+phase*13;
  const points=[];for(let k=0;k<=9;k++){const x=x0+k*.32,y=17.3+1.35*Math.sin(x*.32-p)-1.1;points.push([x,y]);}
  stroke(points,.55,phase>.12&&phase<.85?3:2);
 }
 // Thin secondary stream; no uniform dark border.
 const top=[];for(let x=8;x<19;x+=.5)top.push([x,11.3+1.25*Math.sin(x*.27-p)]);
 stroke(top,.5,3);
 // Two droplets shed from the rear; zero-size phases make the wrap invisible.
 for(let j=0;j<2;j++){
  const q=(f/N+j*.5)%1;
  const radius=1.2*Math.sin(Math.PI*q);
  const dx=8.4-5.4*q,dy=14.6+(j?7.2:-1.2)+1.3*Math.sin(q*Math.PI);
  disc(dx,dy,radius,2,false);
  if(radius>.85)disc(dx-.2,dy-.3,.5,3,false);
 }
 return g;
}
const frames=Array.from({length:N},(_,i)=>frame(i));
if(JSON.stringify(frame(0))!==JSON.stringify(frame(16)))throw Error('loop seam');
fs.writeFileSync(proc+'pixel-frames.json',JSON.stringify({width:32,height:32,frames,palette}));
fs.writeFileSync(out+'water_bolt.png.mcmeta',JSON.stringify({animation:{frametime:2,interpolate:false,width:32,height:32,frames:Array.from({length:16},(_,i)=>i)}},null,2)+'\n');
function build(start,end,create){
 const init=create?`var doc=app.documents.add(32,512,72,'卡通水弹_16帧',NewDocumentMode.RGB,DocumentFill.TRANSPARENT);var blank=doc.activeLayer;`:`var doc=app.activeDocument;`;
 return `var old=app.preferences.rulerUnits;app.preferences.rulerUnits=Units.PIXELS;try{${init}
var frames=${JSON.stringify(frames)},pal=${JSON.stringify(palette)};
for(var f=${start};f<${end};f++){
 var layer=doc.artLayers.add();layer.name='frame_'+('0'+f).slice(-2)+'_100ms';doc.activeLayer=layer;
 for(var c=0;c<pal.length;c++){
  var parts=[];
  for(var y=0;y<32;y++){var x=0;while(x<32){if(frames[f][y][x]!=c){x++;continue;}var e=x+1;while(e<32&&frames[f][y][e]==c)e++;
   var coords=[[x,y+32*f],[e,y+32*f],[e,y+1+32*f],[x,y+1+32*f]],pts=[];
   for(var p=0;p<4;p++){var pt=new PathPointInfo();pt.kind=PointKind.CORNERPOINT;pt.anchor=coords[p];pt.leftDirection=coords[p];pt.rightDirection=coords[p];pts.push(pt);}
   var sub=new SubPathInfo();sub.closed=true;sub.operation=ShapeOperation.SHAPEADD;sub.entireSubPath=pts;parts.push(sub);x=e;
  }}
  if(parts.length){var path=doc.pathItems.add('pixel-mask',parts);path.makeSelection(0,false,SelectionType.REPLACE);var color=new SolidColor();color.rgb.hexValue=pal[c];doc.selection.fill(color,ColorBlendMode.NORMAL,100,false);doc.selection.deselect();path.remove();}
 }
}
${create?'blank.remove();':''}
doc.saveAs(new File('${out}water_bolt.psd'),new PhotoshopSaveOptions(),false,Extension.LOWERCASE);
doc.saveAs(new File('${out}water_bolt.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);
var sample=doc.duplicate('sample',true);sample.crop([UnitValue(0,'px'),UnitValue(0,'px'),UnitValue(32,'px'),UnitValue(32,'px')]);sample.resizeImage(UnitValue(320,'px'),UnitValue(320,'px'),72,ResampleMethod.NEARESTNEIGHBOR);sample.saveAs(new File('${out}water_bolt_keyframe.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);sample.close(SaveOptions.DONOTSAVECHANGES);app.activeDocument=doc;
return {ok:true,document_id:doc.id,drawn:${end}};
}finally{app.preferences.rulerUnits=old;}`;
}
for(const [name,start,end,create] of [['key',0,1,true],['rest',1,16,false]]){const code=build(start,end,create);fs.writeFileSync(proc+'draw-'+name+'.jsx',code);fs.writeFileSync(proc+'request-'+name+'.json',JSON.stringify({name:'photoshop_execute_script',arguments:{code,timeout_ms:180000}}));}
console.log('Prepared 16 looped frames, 32x32, 5 colors.');
