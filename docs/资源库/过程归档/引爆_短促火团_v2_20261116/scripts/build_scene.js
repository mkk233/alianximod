(async()=>{
const root='D:/alianximod/docs/资源库/过程归档/引爆_短促火团_v2_20261116';
const fs=require('fs'),crypto=require('crypto');
const expected='07be6674-6249-0d6c-f677-8fc0b139903d';
function guard(){if(!Project||Project.uuid!==expected)throw new Error('Active document changed; stop');}
guard();if(Outliner.elements.length)throw new Error('New project not empty; stop');
const protectedProjects=ModelProject.all.filter(p=>p!==Project);
function fingerprint(p){return {uuid:p.uuid,name:p.name,saved:p.saved,path:p.save_path,elements:p.elements.length,textures:p.textures.length,sha256:crypto.createHash('sha256').update(JSON.stringify({elements:p.elements.map(e=>({uuid:e.uuid,name:e.name,from:e.from,to:e.to,origin:e.origin,rotation:e.rotation,visibility:e.visibility,faces:e.faces?Object.fromEntries(Object.entries(e.faces).map(([k,v])=>[k,{uv:v.uv,texture:v.texture,rotation:v.rotation,tint:v.tint}])):null})),textures:p.textures.map(t=>({uuid:t.uuid,name:t.name,path:t.path,source:t.source,uv_width:t.uv_width,uv_height:t.uv_height}))})).digest('hex')};}
const before=protectedProjects.map(fingerprint);
fs.writeFileSync(root+'/evidence/protected-projects-before.json',JSON.stringify(before,null,2));
const elements=[],groups=[],textures=[];
Undo.initEdit({elements,groups,textures,outliner:true});
function group(name,parent,origin=[0,0,0]){const g=new Group({name,origin}).addTo(parent||'root').init();groups.push(g);return g;}
function tex(name,path,w,h){const t=new Texture({name,uv_width:w,uv_height:h,render_mode:'default',render_sides:'double'}).fromPath(path).add(false);textures.push(t);return t;}
function color(name,c){const cv=document.createElement('canvas');cv.width=cv.height=16;const ct=cv.getContext('2d');ct.fillStyle=c;ct.fillRect(0,0,16,16);const t=new Texture({name:'AUX_'+name,uv_width:16,uv_height:16}).fromDataURL(cv.toDataURL()).add(false);textures.push(t);return t;}
const mat={ground:color('ground','#252f39'),grid:color('block_grid','#435362'),head:color('head','#95a6aa'),shirt:color('shirt','#596e7a'),limbs:color('arms','#728b95'),legs:color('legs','#3b515e'),eyes:color('eyes','#293640'),hit:color('hit_point','#cce8ed'),bias:color('bias','#e8bd70')};
const flame={orange:tex('detonate_v2_orange_16x50ms',root+'/textures/detonate_v2_orange-strip.png',64,1024),soul:tex('detonate_v2_soul_16x50ms',root+'/textures/detonate_v2_soul-strip.png',64,1024)};
function cube(name,a,b,t,parent){guard();const c=new Cube({name,from:a,to:b,box_uv:false,autouv:0}).addTo(parent).init();for(const f of Object.values(c.faces))f.extend({texture:t.uuid,uv:[0,0,t.uv_width,t.uv_height]});elements.push(c);return c;}
function plate(name,a,b,t,parent,frame=1){const c=cube(name,a,b,t,parent);for(const [n,f]of Object.entries(c.faces)){f.texture=(n==='south'||n==='north')?t.uuid:null;f.uv=n==='north'?[64,(frame-1)*64,0,frame*64]:[0,(frame-1)*64,64,frame*64];}c.shade=false;return c;}
function label(name,text,x,y,z,width,parent){const cv=document.createElement('canvas');cv.width=1024;cv.height=128;const ct=cv.getContext('2d');ct.fillStyle='#202a34';ct.fillRect(0,0,1024,128);ct.font='500 46px Segoe UI';ct.textAlign='center';ct.textBaseline='middle';ct.fillStyle='#e6edf1';ct.fillText(text,512,64,992);const t=new Texture({name:'AUX_label_'+name,uv_width:1024,uv_height:128,render_sides:'double'}).fromDataURL(cv.toDataURL()).add(false);textures.push(t);const c=cube('AUX_label_'+name,[x-width/2,y-1.8,z],[x+width/2,y+1.8,z],t,parent);for(const[n,f]of Object.entries(c.faces)){f.texture=n==='south'?t.uuid:null;f.uv=[0,0,1024,128];}return c;}
function floor(parent,width){cube('AUX_ground_not_damage_area',[-width/2,-.4,-20],[width/2,-.2,24],mat.ground,parent);for(let x=-Math.floor(width/32)*16;x<width/2;x+=16)cube('AUX_1block_grid_x'+x,[x-.035,-.19,-20],[x+.035,-.17,24],mat.grid,parent);for(let z=-16;z<=24;z+=16)cube('AUX_1block_grid_z'+z,[-width/2,-.19,z-.035],[width/2,-.17,z+.035],mat.grid,parent);}
function dummy(x,parent){const g=group('AUX_MC_target_height_28.8u_1.8b',parent);const parts=[['head',[-3.6,21.6,-3.6],[3.6,28.8,3.6],mat.head],['body',[-3.6,10.8,-1.8],[3.6,21.6,1.8],mat.shirt],['armL',[-7.2,10.8,-1.8],[-3.6,21.6,1.8],mat.limbs],['armR',[3.6,10.8,-1.8],[7.2,21.6,1.8],mat.limbs],['legL',[-3.6,0,-1.8],[-.18,10.8,1.8],mat.legs],['legR',[.18,0,-1.8],[3.6,10.8,1.8],mat.legs],['eyeL',[-2.4,25,3.61],[-1.35,26,3.62],mat.eyes],['eyeR',[1.35,25,3.61],[2.4,26,3.62],mat.eyes]];for(const[n,a,b,t]of parts){a[0]+=x;b[0]+=x;cube('AUX_'+n,a,b,t,g);}return g;}
const effects=[];
function target(name,x,spent,span,variant,parent){const g=group(name,parent);dummy(x,g);let plane=null,bill=null;if(spent>0){bill=group('FX_CAMERA_PLANE_'+variant+'_spent'+spent,g,[x,16,3.08]);plane=plate('FX_'+variant+'_S'+spent+'_side'+span+'b',[x-span*8,16-span*8,3.08],[x+span*8,16+span*8,3.08],flame[variant],bill,5);effects.push({plane,bill,x,spent,span,variant,parent:g,anchor:[x,16,1.8]});}return {group:g,plane,bill};}
const main=group('01_MAIN_TWO_VARIANTS_SPENT50');floor(main,90);
const a=target('ORANGE_SPENT50',-20,50,1.2,'orange',main),b=target('SOUL_SPENT50',20,50,1.2,'soul',main);
label('orange','ORANGE  /  SPENT 50',-20,34,0,35,main);label('soul','SOUL  /  SPENT 50',20,34,0,35,main);
label('title','DETONATE v2  /  0.8s ONCE  /  CANDIDATE',0,42,-1,86,main);
label('units','Target 1.80b  |  face 1.20b  |  ground grid 1b',0,-5,11,83,main);
const comparison=group('02_ACTUAL_SPENT_STRENGTH_CANDIDATE');floor(comparison,162);
const spent=[0,10,50,100],span=[0,.8,1.2,1.6],xs=[-57,-19,19,57];
for(let i=0;i<4;i++){target('COMPARE_S'+spent[i],xs[i],spent[i],span[i],'orange',comparison);label('compare_'+spent[i],spent[i]===0?'SPENT 0 / NO EXTRA FX':'SPENT '+spent[i]+' / '+span[i].toFixed(2)+'b',xs[i],34,0,37,comparison);}
label('comparison','ACTUAL SPENT  /  SIZES ARE CANDIDATE VISUAL PARAMETERS',0,43,-1,150,comparison);
label('compare_units','0.8s once  /  16 frames  /  scale only, damage unchanged',0,-6,12,142,comparison);
comparison.visibility=false;
const depth=group('03_AUX_FROZEN_DEPTH_MEASUREMENT',main);
cube('AUX_hit_surface_point',[-20.18,15.82,1.62],[-19.82,16.18,1.98],mat.hit,depth);
cube('AUX_camera_bias_1.28u_0.08b',[-20.05,15.95,1.8],[-19.95,16.05,3.08],mat.bias,depth);
label('depth','FROZEN FRONT PLANE  /  0.08b BIAS  /  ZERO THICKNESS',-20,39,-2,78,depth);
depth.visibility=false;
Project.texture_width=64;Project.texture_height=1024;Project.credit='candidate_pending_user_confirmation | fire_detonate_v2_art | 20261116-art-detonate-03 | auxiliary targets/ground/labels are preview only';
Undo.finishEdit('Create isolated detonate v2 candidate spatial preview',{elements,groups,textures,outliner:true});
await Promise.all(textures.map(t=>new Promise(r=>{if(t.img.complete&&t.img.naturalWidth)r();else{t.img.addEventListener('load',r,{once:true});setTimeout(r,1200);}})));
guard();Canvas.updateAll();unselectAllElements();
for(const t of Object.values(flame)){const m=t.getMaterial();if(m.uniforms.SHADE)m.uniforms.SHADE.value=false;if(m.uniforms.EMISSIVE)m.uniforms.EMISSIVE.value=false;m.depthWrite=false;}
const pv=Preview.selected;pv.loadAnglePreset({position:[65,48,160],target:[0,17,0],projection:'orthographic',zoom:.72});pv.controls.update();
await new Promise(resolve=>pv.screenshot({crop:false,width:1200,height:850},data=>{fs.writeFileSync(root+'/previews/gui_01_burst_threequarter.png',Buffer.from(data.split(',')[1],'base64'));resolve();}));
const after=protectedProjects.map(fingerprint);if(JSON.stringify(before)!==JSON.stringify(after))throw new Error('Protected project fingerprint changed');
fs.writeFileSync(root+'/evidence/protected-projects-after-build.json',JSON.stringify(after,null,2));
window.detonateArt={root,expected,effects,main,comparison,depth,orange:a,soul:b,flame,protected:before};
Project.save_path=root+'/blockbench/fire_detonate_v2_candidate.bbmodel';Codecs.project.write(Codecs.project.compile({editor_state:true}),Project.save_path);
return {project:Project.uuid,saved:Project.saved,path:Project.save_path,elements:elements.length,textures:Texture.all.map(t=>({name:t.name,width:t.width,height:t.height,error:t.error})),protectedProjectsUnchanged:true,preview:root+'/previews/gui_01_burst_threequarter.png'};
})()
