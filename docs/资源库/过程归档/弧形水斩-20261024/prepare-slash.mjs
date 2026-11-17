import fs from 'node:fs';
const proc='D:/alianximod/docs/资源库/过程归档/弧形水斩-20261024/';
const out='D:/alianximod/docs/资源库/特效/水流/弧形水斩_v1/';
const W=64,H=64,N=16,PI=Math.PI;
const palette=['164A83','167AB9','21B6DC','78E2ED','D5FCFC','F7FFFF'];
const ends=[-199,-187,-171,-142,-96,-43,4,20,32,42,50,56,61,64,66,66];
const starts=[-213,-213,-213,-213,-213,-213,-213,-190,-155,-120,-83,-50,-15,22,48,66];
const strength=[.22,.32,.48,.65,.86,1,1,.96,.85,.72,.59,.46,.32,.20,.10,0];
function draw(f){
 const g=Array.from({length:H},()=>Array(W).fill(-1));if(f===15)return g;
 const start=starts[f],end=ends[f],amp=strength[f],growth=.9+Math.min(f,6)*.016+Math.max(0,f-6)*.007;
 const cx=32,cy=34,rx=25.5,ry=24;
 const rad=a=>a*PI/180;
 function put(x,y,c,clip=false){if(x>=0&&x<W&&y>=0&&y<H&&(!clip||g[y][x]>=0))g[y][x]=c;}
 function disc(xc,yc,r,c,clip=false){for(let y=Math.floor(yc-r);y<=Math.ceil(yc+r);y++)for(let x=Math.floor(xc-r);x<=Math.ceil(xc+r);x++)if((x+.5-xc)**2+(y+.5-yc)**2<r*r)put(x,y,c,clip);}
 function line(pts,r,c,clip=false){for(let k=1;k<pts.length;k++){const a=pts[k-1],b=pts[k],n=Math.max(1,Math.ceil(Math.hypot(b[0]-a[0],b[1]-a[1])*3));for(let j=0;j<=n;j++){const t=j/n;disc(a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t,r,c,clip);}}}
 function poly(pts,c){let minx=Math.floor(Math.min(...pts.map(a=>a[0]))),maxx=Math.ceil(Math.max(...pts.map(a=>a[0]))),miny=Math.floor(Math.min(...pts.map(a=>a[1]))),maxy=Math.ceil(Math.max(...pts.map(a=>a[1])));for(let y=miny;y<=maxy;y++)for(let x=minx;x<=maxx;x++){let inside=false;for(let i=0,j=pts.length-1;i<pts.length;j=i++){const a=pts[i],b=pts[j],X=x+.5,Y=y+.5;if((a[1]>Y)!=(b[1]>Y)&&X<(b[0]-a[0])*(Y-a[1])/(b[1]-a[1])+a[0])inside=!inside;}if(inside)put(x,y,c);}}
 function polar(a,offset=0){let t=rad(a),r=growth+(.75*Math.sin(2*t+.7)+offset)/25;return[cx+rx*r*Math.cos(t),cy+ry*r*Math.sin(t)];}
 // Sweeping crescent: expanding front, narrowing split tail, deliberately uneven thickness.
 for(let y=0;y<H;y++)for(let x=0;x<W;x++){
  const dx=(x+.5-cx)/rx,dy=(y+.5-cy)/ry;
  let a=Math.atan2(dy,dx)*180/PI;if(a>95)a-=360;
  if(a<start||a>end)continue;
  const u=(a-start)/(end-start),theta=rad(a),rho=Math.sqrt(dx*dx+dy*dy);
  const outer=growth+.75*Math.sin(2*theta+.7)/25;
  let thick=(.65+9.6*Math.pow(Math.sin(u*PI/2),.85))*amp;
  if(u>.86)thick*=.3+.7*(1-u)/.14;
  thick*=.88+.12*Math.sin(theta*2-f*.45);
  const depth=(outer-rho)*25;
  if(depth<0||depth>thick)continue;
  const d=depth/Math.max(thick,.01);
  if(f>=7&&u<.65&&d>.28&&Math.sin(theta*8+f*.75)>.38)continue;
  if(f>=10&&Math.sin(theta*11-f*.43)>.5-(f-10)*.10)continue;
  let c=2;
  if(d>.64&&Math.sin(theta*5-f*.12)>-.05)c=1;
  if(d>.85&&Math.sin(theta*3+.4)>.1)c=0;
  if(d<.24)c=3;
  if(d<.12&&u>.14&&Math.sin(theta*7-f*.35)>-.45)c=5;
  if(d>=.27&&d<.42&&Math.sin(theta*9-f*.9)>.05)c=4;
  if(f>11&&c<2)c=2;
  g[y][x]=c;
 }
 // A broken parallel water thread near the tail, never a second complete ring.
 if(f>=3&&f<=10){
  const pts=[];for(let a=start+8;a<Math.min(start+48,end-15);a+=2)pts.push(polar(a,-2.4-1.0*Math.sin(rad(a*4)+f)));
  if(pts.length>1)line(pts,.55+amp*.3,3);
 }
 // Rolling front aligned to the tangent: a narrow hooked splash, no round projectile head.
 if(f>=3&&f<=10){
  const a=rad(end-7),origin=polar(end-7,-1.1),tangent=[-Math.sin(a),Math.cos(a)],normal=[Math.cos(a),Math.sin(a)];
  const scale=amp*.95;
  const local=(u,v)=>[origin[0]+scale*(u*tangent[0]+v*normal[0]),origin[1]+scale*(u*tangent[1]+v*normal[1])];
  const shape=[[-8,-4.5],[-3,-4.2],[1,-2],[4,1],[3.4,3.5],[1,4.7],[-1.5,3],[-2,1],[-.4,.2],[.3,2],[1.6,1.6],[1,.1],[-1.6,-1.3],[-5,-1.8]].map(([u,v])=>local(u,v));
  poly(shape,2);
  const lip=[[-7,-3.5],[-3,-2.6],[.5,-1],[2.7,1],[2.1,3],[.2,3.3],[-.8,1.7]].map(([u,v])=>local(u,v));
  line(lip,1.15*scale+.25,4,true);line(lip.slice(1,6),.55*scale+.25,5,true);
 }
 // Two embedded open hooks break the smooth inner rim without becoming circular whirlpools.
 if(f>=4&&f<=10){
  for(const [part,sz] of [[.55,.72],[.78,.65]]){
   const angle=start+(end-start)*part,a=rad(angle),origin=polar(angle,-2.4);
   const tangent=[-Math.sin(a),Math.cos(a)],normal=[Math.cos(a),Math.sin(a)],scale=sz*amp;
   const local=(u,v)=>[origin[0]+scale*(u*tangent[0]+v*normal[0]),origin[1]+scale*(u*tangent[1]+v*normal[1])];
   const hook=[[-4,-1],[-2,1],[.5,1.4],[2.4,.2],[1.4,-1.6],[-.5,-1.1]].map(([u,v])=>local(u,v));
   line(hook,.9*amp+.2,3,true);line(hook.slice(1,5),.5*amp+.2,5,true);
  }
 }
 // Tangential droplets emerge at different moments and shrink to zero.
 const drops=[{on:6,end:15,o:[54,36],v:[.28,1.8],r:1.7},{on:7,end:15,o:[49,20],v:[1.05,.75],r:1.5},{on:9,end:15,o:[33,10],v:[1.7,-.12],r:1.4},{on:10,end:15,o:[14,19],v:[.9,-.7],r:1.1}];
 for(const d of drops){if(f<d.on||f>=d.end)continue;const age=f-d.on,q=age/(d.end-d.on),r=d.r*Math.sin(PI*(.2+.8*q));const x=d.o[0]+d.v[0]*age,y=d.o[1]+d.v[1]*age;disc(x,y,r,3);if(r>1.1)disc(x-.2,y-.4,.6,5);}
 return g;
}
const frames=Array.from({length:N},(_,i)=>draw(i));
fs.writeFileSync(proc+'pixel-frames.json',JSON.stringify({width:W,height:H,palette,frames}));
fs.writeFileSync(out+'water_arc_slash.png.mcmeta',JSON.stringify({animation:{width:64,height:64,frametime:1,interpolate:false,frames:Array.from({length:16},(_,i)=>i)}},null,2)+'\n');
function script(ids,create){return `var old=app.preferences.rulerUnits;app.preferences.rulerUnits=Units.PIXELS;try{
${create?"var doc=app.documents.add(64,1024,72,'弧形水斩_单次挥剑16帧',NewDocumentMode.RGB,DocumentFill.TRANSPARENT);var blank=doc.activeLayer;":"var doc=app.activeDocument;"}
var frames=${JSON.stringify(frames)},pal=${JSON.stringify(palette)},ids=${JSON.stringify(ids)};
for(var z=0;z<ids.length;z++){var f=ids[z],layer=doc.artLayers.add();layer.name='frame_'+('0'+f).slice(-2)+'_50ms';doc.activeLayer=layer;
for(var c=0;c<pal.length;c++){var parts=[];for(var y=0;y<64;y++){var x=0;while(x<64){if(frames[f][y][x]!=c){x++;continue;}var e=x+1;while(e<64&&frames[f][y][e]==c)e++;
var coords=[[x,y+64*f],[e,y+64*f],[e,y+1+64*f],[x,y+1+64*f]],pts=[];for(var p=0;p<4;p++){var pt=new PathPointInfo();pt.kind=PointKind.CORNERPOINT;pt.anchor=coords[p];pt.leftDirection=coords[p];pt.rightDirection=coords[p];pts.push(pt);}var sub=new SubPathInfo();sub.closed=true;sub.operation=ShapeOperation.SHAPEADD;sub.entireSubPath=pts;parts.push(sub);x=e;
}}if(parts.length){var path=doc.pathItems.add('pixel-mask',parts);path.makeSelection(0,false,SelectionType.REPLACE);var color=new SolidColor();color.rgb.hexValue=pal[c];doc.selection.fill(color,ColorBlendMode.NORMAL,100,false);doc.selection.deselect();path.remove();}}
}
${create?'blank.remove();':''}
doc.saveAs(new File('${out}water_arc_slash.psd'),new PhotoshopSaveOptions(),false,Extension.LOWERCASE);doc.saveAs(new File('${out}water_arc_slash.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);
var key=doc.duplicate('斩击关键帧',true);key.crop([UnitValue(0,'px'),UnitValue(384,'px'),UnitValue(64,'px'),UnitValue(448,'px')]);key.saveAs(new File('${out}water_arc_slash_keyframe.png'),new PNGSaveOptions(),true,Extension.LOWERCASE);key.close(SaveOptions.DONOTSAVECHANGES);app.activeDocument=doc;
return {ok:true,document_id:doc.id,drawn:ids.length};}finally{app.preferences.rulerUnits=old;}`;}
for(const[name,ids,create]of[['key',[6],true],['rest',Array.from({length:16},(_,i)=>i).filter(i=>i!==6),false]]){const code=script(ids,create);fs.writeFileSync(proc+'draw-'+name+'.jsx',code);fs.writeFileSync(proc+'request-'+name+'.json',JSON.stringify({name:'photoshop_execute_script',arguments:{code,timeout_ms:180000}}));}
console.log('Prepared one-shot water arc: 16 frames, 64x64, 50ms each');

