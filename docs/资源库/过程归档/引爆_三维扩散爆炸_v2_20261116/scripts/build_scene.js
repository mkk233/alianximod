(async()=>{
const root='D:/alianximod/docs/资源库/过程归档/引爆_三维扩散爆炸_v2_20261116';
const fs=require('fs'),expected='5f702182-73a6-cd40-964d-2ad6a7c2a361';
if(Project.uuid!==expected||Outliner.elements.length)throw new Error('Own empty project required');
const elements=[],groups=[],textures=[],effects=[];
Undo.initEdit({elements,groups,textures,outliner:true});
function group(name,parent){const item=new Group({name,origin:[0,0,0]}).addTo(parent||'root').init();groups.push(item);return item;}
function solid(name,hex){const canvas=document.createElement('canvas');canvas.width=canvas.height=16;const ctx=canvas.getContext('2d');ctx.fillStyle=hex;ctx.fillRect(0,0,16,16);const item=new Texture({name:'AUX_'+name,uv_width:16,uv_height:16}).fromDataURL(canvas.toDataURL()).add(false);textures.push(item);return item;}
function cube(name,from,to,texture,parent){const item=new Cube({name,from,to,box_uv:false,autouv:0}).addTo(parent).init();for(const face of Object.values(item.faces))face.extend({texture:texture.uuid,uv:[0,0,texture.uv_width,texture.uv_height]});elements.push(item);return item;}
const palette={floor:solid('ground','#3c4648'),grid:solid('one_block_grid','#63706d'),head:solid('head','#adbbb8'),body:solid('shirt','#657f85'),arm:solid('arm','#93a6a4'),leg:solid('leg','#4d5e69'),eye:solid('eye','#24373c')};
const puff={};
for(const variant of ['orange','soul']){puff[variant]=new Texture({name:'EXPLOSION_PUFF_'+variant+'_16x50ms',uv_width:32,uv_height:512,render_sides:'double'}).fromPath(root+'/textures/explosion_puff_'+variant+'-strip.png').add(false);textures.push(puff[variant]);}
function label(name,text,x,y,z,width,parent){const canvas=document.createElement('canvas');canvas.width=1536;canvas.height=128;const ctx=canvas.getContext('2d');ctx.fillStyle='#26313a';ctx.fillRect(0,0,1536,128);ctx.font='500 48px Segoe UI';ctx.textAlign='center';ctx.textBaseline='middle';ctx.fillStyle='#e2e9e6';ctx.fillText(text,768,64,1500);const texture=new Texture({name:'AUX_label_'+name,uv_width:1536,uv_height:128,render_sides:'double'}).fromDataURL(canvas.toDataURL()).add(false);textures.push(texture);const item=cube('AUX_LABEL_'+name,[x-width/2,y-1.4,z],[x+width/2,y+1.4,z],texture,parent);for(const[faceName,face]of Object.entries(item.faces))face.texture=faceName==='south'?texture.uuid:null;item.shade=false;return item;}
function floor(parent,width){cube('AUX_GROUND_NOT_DAMAGE_RADIUS',[-width/2,-.45,-40],[width/2,-.2,40],palette.floor,parent);for(let coord=-Math.floor(width/32)*16;coord<=width/2;coord+=16)cube('AUX_GRID_X_'+coord,[coord-.04,-.18,-40],[coord+.04,-.16,40],palette.grid,parent);for(let coord=-32;coord<=32;coord+=16)cube('AUX_GRID_Z_'+coord,[-width/2,-.18,coord-.04],[width/2,-.16,coord+.04],palette.grid,parent);}
function dummy(x,parent){const item=group('AUX_MC_TARGET_1.80_BLOCKS',parent);const parts=[['head',[-3.6,21.6,-3.6],[3.6,28.8,3.6],'head'],['torso',[-3.6,10.8,-1.8],[3.6,21.6,1.8],'body'],['armL',[-7.2,10.8,-1.8],[-3.6,21.6,1.8],'arm'],['armR',[3.6,10.8,-1.8],[7.2,21.6,1.8],'arm'],['legL',[-3.6,0,-1.8],[-.18,10.8,1.8],'leg'],['legR',[.18,0,-1.8],[3.6,10.8,1.8],'leg'],['eyeL',[-2.4,25,3.62],[-1.35,26,3.64],'eye'],['eyeR',[1.35,25,3.62],[2.4,26,3.64],'eye']];for(const[name,from,to,color]of parts){from[0]+=x;to[0]+=x;cube('AUX_'+name,from,to,palette[color],item);}return item;}
const seeds=[];
for(let index=0;index<24;index++){
 const vertical=1-2*(index+.5)/24;
 const angle=index*2.3999632297+.23*Math.sin(index*1.7);
 const radial=Math.sqrt(1-vertical*vertical);
 const direction=[Math.cos(angle)*radial,vertical,Math.sin(angle)*radial];
 seeds.push({index,direction,onset:index%4,lifetime:13+index%4,travel:10.6+((index*7)%9)*.49,span:16.5+((index*5)%7)*1.0,roll:(index*137.5)%360,center:false});
}
seeds.push({index:24,direction:[0,.13,0],onset:0,lifetime:9,travel:1,span:27,roll:8,center:true});
seeds.push({index:25,direction:[.2,-.2,.4],onset:1,lifetime:10,travel:2,span:23,roll:147,center:true});
function effect(name,x,scale,parent){
 const target=group(name,parent);dummy(x,target);
 const particles=group('FX_26_WORLD_POSITIONED_CAMERA_FACING_PUFFS',target);
 if(scale>0)for(const seed of seeds){const item=cube('FX_PUFF_'+String(seed.index).padStart(2,'0'),[-1,-1,0],[1,1,0],puff.orange,particles);for(const[faceName,face]of Object.entries(item.faces)){face.texture=faceName==='south'?puff.orange.uuid:null;face.uv=[0,0,32,32];}item.shade=false;effects.push({cube:item,seed,anchor:[x,16,1.8],scale,parent:particles});}
 return target;
}
const main=group('01_MAIN_SPENT50_FULL_VOLUME');floor(main,80);effect('MAIN_ORANGE_OR_SOUL_S50',0,1,main);
label('main_title','DETONATE v2-3D | RADIAL PUFF CLOUD | CANDIDATE',0,44,-4,76,main);
label('main_units','SPENT 50 / TARGET 1.80b / GRID 1b / NOT A DAMAGE RADIUS',0,2.6,41,74,main);
const comparison=group('02_ACTUAL_SPENT_STRENGTHS');
floor(comparison,240);
const values=[0,10,50,100],scales=[0,.65,1,1.35],locations=[-90,-30,30,90];
for(let index=0;index<4;index++){effect('SPENT_'+values[index],locations[index],scales[index],comparison);label('strength_'+values[index],values[index]===0?'S 0 / NO EXTRA FX':'S '+values[index]+' / SCALE '+scales[index].toFixed(2),locations[index],45,0,56,comparison);}
label('strength_title','ACTUAL CONSUMPTION / VISUAL SCALE ONLY / SAME DAMAGE RULE',0,55,0,228,comparison);
Project.texture_width=32;Project.texture_height=512;
Project.credit='candidate_pending_user_confirmation | fire_detonate_v2_art | 20261116-art-detonate-3d-01 | 26 independent world-positioned puff billboards, not one whole CAMERA_PLANE | AUX excluded from runtime';
Undo.finishEdit('Build isolated 3D radial detonation candidate',{elements,groups,textures,outliner:true});
await Promise.all(textures.map(texture=>new Promise(resolve=>{if(texture.img.complete&&texture.img.naturalWidth)resolve();else{texture.img.addEventListener('load',resolve,{once:true});setTimeout(resolve,1200);}})));
function visible(node,on){node.visibility=on;if(node.children)node.children.forEach(child=>visible(child,on));}
function guard(){if(Project.uuid!==expected)throw new Error('Active project changed');}
function mode(kind){visible(main,kind!=='strength');visible(comparison,kind==='strength');}
function camera(kind){
 const angle=kind==='strength'?{position:[12,60,280],target:[0,20,0],projection:'orthographic',zoom:.135}:kind==='side'?{position:[98,81,-23],target:[0,15,0],projection:'orthographic',zoom:.34}:{position:[62,46,106],target:[0,17,0],projection:'orthographic',zoom:.34};
 Preview.selected.loadAnglePreset(angle);Preview.selected.controls.update();
}
function pose(frame,variant='orange'){
 guard();const camera=Preview.selected.camera;
 const samples=[];
 for(const effect of effects){
  const {seed,cube:plane,scale,anchor}=effect;
  const age=frame-1-seed.onset,alive=age>=0&&age<seed.lifetime;
  plane.visibility=effect.parent.visibility&&alive;
  const progress=Math.max(0,Math.min(1,age/(seed.lifetime-1)));
  const textureFrame=Math.min(15,Math.floor(progress*15));
  const travel=seed.travel*(1-Math.exp(-Math.max(0,age)*.32));
  const center=anchor.map((value,axis)=>value+seed.direction[axis]*travel*scale+(axis===1?Math.max(0,age)*.055*scale:0));
  const span=seed.span*scale;
  const roll=new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(0,0,1),seed.roll*Math.PI/180);
  const rotation=new THREE.Euler().setFromQuaternion(camera.quaternion.clone().multiply(roll),'ZYX');
  plane.origin=center.slice();plane.rotation=[rotation.x,rotation.y,rotation.z].map(value=>value*180/Math.PI);
  plane.from=[center[0]-span/2,center[1]-span/2,center[2]];plane.to=[center[0]+span/2,center[1]+span/2,center[2]];
  plane.faces.south.texture=puff[variant].uuid;plane.faces.south.uv=[0,textureFrame*32,32,(textureFrame+1)*32];
  if(effect.parent.visibility)samples.push({index:seed.index,alive,age,textureFrame,center,span});
 }
 Canvas.updateAll();Canvas.updateVisibility();unselectAllElements();
 for(const texture of Object.values(puff)){const material=texture.getMaterial();material.uniforms.SHADE.value=false;material.uniforms.EMISSIVE.value=false;material.transparent=true;material.depthWrite=false;material.depthTest=true;material.needsUpdate=true;}
 const facing= new THREE.Vector3(0,0,-1).applyQuaternion(camera.quaternion);
 const sorted=effects.filter(effect=>effect.cube.visibility).map(effect=>({effect,depth:new THREE.Vector3(...effect.cube.origin).sub(camera.position).dot(facing)})).sort((left,right)=>right.depth-left.depth);
 sorted.forEach((entry,index)=>entry.effect.cube.mesh.renderOrder=20+index);
 return samples;
}
window.detonate3d={root,expected,effects,seeds,puff,main,comparison,mode,camera,pose,visible,guard};
mode('main');camera('main');pose(7);
await new Promise(resolve=>setTimeout(resolve,150));
await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:1200,height:920},data=>{fs.writeFileSync(root+'/previews/gui_peak_threequarter.png',Buffer.from(data.split(',')[1],'base64'));resolve();}));
fs.writeFileSync(root+'/evidence/particle-seeds.json',JSON.stringify(seeds,null,2));
Project.save_path=root+'/blockbench/fire_detonate_v2_3d_candidate.bbmodel';Codecs.project.write(Codecs.project.compile({editor_state:true}),Project.save_path);
return {uuid:Project.uuid,elements:elements.length,groups:groups.length,textures:textures.length,particles_per_effect:26,path:Project.save_path};
})()
