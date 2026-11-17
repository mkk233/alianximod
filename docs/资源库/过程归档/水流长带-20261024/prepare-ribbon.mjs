import fs from 'node:fs';
const proc='D:/alianximod/docs/资源库/过程归档/水流长带-20261024/';
const out='D:/alianximod/docs/资源库/特效/水流/水流长带_v1/';
const W=128,H=32,N=16,TAU=Math.PI*2;
const palette=['143F7A','126AAF','199ED2','66D8EA','C6F8FA','F5FFFF'];
function draw(f){ f=f%16;
 const p=TAU*(f%16)/16,g=Array.from({length:H},()=>Array(W).fill(-1));
 const top=x=>13+1.6*Math.sin(TAU*x/W)+.45*Math.sin(p+TAU*x/W);
 const bottom=x=>25+1.9*Math.sin(TAU*x/W+.6)+.45*Math.sin(p+TAU*x/W);
 const put=(x,y,c,clip=false)=>{x=((x%W)+W)%W;if(y>=0&&y<H&&(!clip||g[y][x]>=0))g[y][x]=c;};
 function disc(cx,cy,r,c,clip=false){for(let y=Math.floor(cy-r);y<=Math.ceil(cy+r);y++)for(let x=Math.floor(cx-r);x<=Math.ceil(cx+r);x++)if((x+.5-cx)**2+(y+.5-cy)**2<r*r)put(x,y,c,clip);}
 function line(pts,r,c,clip=false){for(let k=1;k<pts.length;k++){const a=pts[k-1],b=pts[k],n=Math.max(1,Math.ceil(Math.hypot(b[0]-a[0],b[1]-a[1])*3));for(let j=0;j<=n;j++){const t=j/n;disc(a[0]+t*(b[0]-a[0]),a[1]+t*(b[1]-a[1]),r,c,clip);}}}
 function poly(pts,c){let minX=Math.floor(Math.min(...pts.map(a=>a[0]))),maxX=Math.ceil(Math.max(...pts.map(a=>a[0])));let minY=Math.floor(Math.min(...pts.map(a=>a[1]))),maxY=Math.ceil(Math.max(...pts.map(a=>a[1])));for(let y=minY;y<=maxY;y++)for(let x=minX;x<=maxX;x++){let inside=false;for(let i=0,j=pts.length-1;i<pts.length;j=i++){let a=pts[i],b=pts[j],xx=x+.5,yy=y+.5;if((a[1]>yy)!=(b[1]>yy)&&xx<(b[0]-a[0])*(yy-a[1])/(b[1]-a[1])+a[0])inside=!inside;}if(inside)put(x,y,c);}}
 // A connected broad stream, with a stable silhouette and a darker lower face.
 for(let y=0;y<H;y++)for(let x=0;x<W;x++){
  const yy=y+.5,t=top(x+.5),b=bottom(x+.5);
  if(yy<t||yy>b)continue;
  const depth=(yy-t)/(b-t);let c=depth>.80?0:depth>.46?1:2;
  if(depth<.17)c=3;g[y][x]=c;
 }
 // Long staggered streams advect at 32 pixels per cycle, below the stationary larger waves.
 for(let lane=0;lane<3;lane++){
  let segment=[];
  for(let x=-4;x<=W+4;x+=.35){
   const travel=TAU*(x-2*f)/32+lane*1.9;
   const keep=Math.sin(travel)>(lane===0?-.6:-.15);
   const y=top(x)+2.8+lane*3.2+1.1*Math.sin(travel+lane);
   if(keep)segment.push([x,y]);else if(segment.length){line(segment,lane===0?.8:.65,lane===0?4:3,true);segment=[];}
  }
  if(segment.length)line(segment,.65,3,true);
 }
 // Three irregular curling crests. Their size and fold phase differ; no detached circular head.
 const crests=[[19,8.2,1.06,0],[62,5.7,.90,2.1],[103,7.0,1,4.2]];
 for(const [base,height,scale,phase] of crests){
  const t=p+phase,cx=base+.6*Math.sin(t),cy=top(base)+.9,h=height+.55*Math.sin(t);
  const xy=(x,y)=>[cx+x*scale,cy+y];
  const shape=[xy(-12,3),xy(-9,-1),xy(-6,-h+3),xy(-2,-h),xy(2,-h+.2),xy(5,-h+2),xy(7,-h+4.7),xy(6,-h+6.5),xy(3,-h+6.5),xy(1.8,-h+5),xy(3,-h+3.8),xy(.5,-h+3),xy(-1.8,-h+5),xy(-2,1),xy(4,4),xy(10,3)];
  poly(shape,1);
  poly([xy(-11,2),xy(-7,-2),xy(-3,-h+1),xy(1,-h+1),xy(4,-h+3),xy(4,-h+4),xy(1,-h+3),xy(-2,-h+5),xy(-2,1),xy(4,3),xy(-5,4)],2);
  const lip=[xy(-11,1.4),xy(-8,-1.5),xy(-5,-h+2),xy(-2,-h+1),xy(1.5,-h+1),xy(4,-h+2.5),xy(5.1,-h+4.5),xy(3.8,-h+5.4),xy(2.3,-h+4.6)];
  line(lip,1.8,4,true);line(lip.slice(0,8),1.0,5,true);
  // Inner blue fold and its narrow secondary bright stream.
  line([xy(-4,-h+4),xy(-4,-1),xy(-1,2),xy(5,3.4),xy(10,2.6)],.65,3,true);
  // A compact foam fleck grows and falls back into each crest, out of phase.
  const q=(f/16+phase/TAU)%1,r=.95*Math.sin(Math.PI*q);
  disc(cx+1+5*q,cy-h-1.2-1.1*Math.sin(Math.PI*q),r,4);
 }
 // Interrupted connecting white foam, leaving the blue body clearly visible.
 for(let x=0;x<W;x++){
  const near=crests.some(([cx])=>Math.abs(x-cx)<12);
  if(!near&&Math.sin(TAU*(x-2*f)/32)>.2){const y=top(x)+.55;disc(x+.5,y,.85,4,true);}
 }
 return g;
}
const frames=Array.from({length:N},(_,f)=>draw(f));
if(JSON.stringify(draw(0))!==JSON.stringify(draw(16)))throw Error('non-looping geometry');
fs.writeFileSync(proc+'pixel-frames.json',JSON.stringify({width:W,height:H,palette,frames}));
fs.writeFileSync(out+'water_ribbon.png.mcmeta',JSON.stringify({animation:{width:W,height:H,frametime:2,interpolate:false,frames:Array.from({length:N},(_,i)=>i)}},null,2)+'\n');
function build(start,end,create){
return `var old=app.preferences.rulerUnits;app.preferences.rulerUnits=Units.PIXELS;try{
${create?"var doc=app.documents.add(128,512,72,'水流长带_16帧',NewDocumentMode.RGB,DocumentFill.TRANSPARENT);var blank=doc.activeLayer;":"var doc=app.activeDocument;"}
var frames=${JSON.stringify(frames)},pal=${JSON.stringify(palette)};
for(var f=${start};f<${end};f++){
 var layer=doc.artLayers.add();layer.name='frame_'+('0'+f).slice(-2)+'_100ms';doc.activeLayer=layer;
 for(var c=0;c<pal.length;c++){
 var parts=[];for(var y=0;y<32;y++){var x=0;while(x<128){if(frames[f][y][x]!=c){x++;continue;}var e=x+1;while(e<128&&frames[f][y][e]==c)e++;
 var coords=[[x,y+32*f],[e,y+32*f],[e,y+1+32*f],[x,y+1+32*f]],pts=[];
 for(var p=0;p<4;p++){var pt=new PathPointInfo();pt.kind=PointKind.CORNERPOINT;pt.anchor=coords[p];pt.leftDirection=coords[p];pt.rightDirection=coords[p];pts.push(pt);}
 var sub=new SubPathInfo();sub.closed=true;sub.operation=ShapeOperation.SHAPEADD;sub.entireSubPath=pts;parts.push(sub);x=e;
 }}if(parts.length){var path=doc.pathItems.add('pixel-mask',parts);path.makeSelection(0,false,SelectionType.REPLACE);var col=new SolidColor();col.rgb.hexValue=pal[c];doc.selection.fill(col,ColorBlendMode.NORMAL,100,false);doc.selection.deselect();path.remove();}
 }
}
${create?'blank.remove();':''}
doc.saveAs(new File('${out}water_ribbon.psd'),new PhotoshopSaveOptions(),false,Extension.LOWERCASE);doc.saveAs(new File('${out}water_ribbon.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);
var sample=doc.duplicate('keyframe',true);sample.crop([UnitValue(0,'px'),UnitValue(0,'px'),UnitValue(128,'px'),UnitValue(32,'px')]);sample.saveAs(new File('${out}water_ribbon_keyframe.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);sample.close(SaveOptions.DONOTSAVECHANGES);app.activeDocument=doc;
return {ok:true,document_id:doc.id,drawn:${end}};}finally{app.preferences.rulerUnits=old;}`;
}
for(const [name,start,end,create] of [['key',0,1,true],['rest',1,16,false]]){const code=build(start,end,create);fs.writeFileSync(proc+'draw-'+name+'.jsx',code);fs.writeFileSync(proc+'request-'+name+'.json',JSON.stringify({name:'photoshop_execute_script',arguments:{code,timeout_ms:180000}}));}
console.log('Prepared ribbon, 128x32, 16 frames');

