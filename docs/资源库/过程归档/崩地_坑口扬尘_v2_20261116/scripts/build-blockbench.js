(function(){
const root='D:/alianximod/docs/资源库/过程归档/崩地_坑口扬尘_v2_20261116';
if(Project.uuid!=='0d65d0f5-9590-698b-4313-bc901925bd9c'||Outliner.elements.length) throw Error('Target project not empty or not current; preserve existing projects');
const fs=require('fs');
Undo.initEdit({elements:[],outliner:true,textures:[],animations:[]});
Project.texture_width=96;Project.texture_height=512;
const tex=new Texture({name:'ground_dust_v2-atlas',render_mode:'default',render_sides:'double',uv_width:96,uv_height:512}).fromDataURL('data:image/png;base64,'+fs.readFileSync(root+'/exports/ground_dust_v2-atlas.png').toString('base64')).add(false);
const aux=new Texture({name:'AUX_ONLY_ground_scale_palette',render_mode:'default',render_sides:'front',uv_width:64,uv_height:16}).fromDataURL('data:image/png;base64,'+fs.readFileSync(root+'/exports/preview_auxiliary_palette.png').toString('base64')).add(false);
const main=new Group({name:'ANCHOR_C_Vec3_atCenterOf_impact',origin:[0,0,0]}).init();
function grp(name,parent,origin){return new Group({name,origin:origin||[0,0,0]}).addTo(parent).init();}
const terrain=grp('AUX_flat_ground_7x7_16u_per_block',main);
const removed=grp('AUX_REMOVE_exact_23_cells_on_EXECUTED_preview_only',main);
function cube(name,from,to,parent,topBank,sideBank){
const c=new Cube({name,from,to,autouv:0,box_uv:false}).addTo(parent).init();
for(const f of Object.keys(c.faces)){const bank=f==='up'?topBank:sideBank;c.faces[f].texture=aux.uuid;c.faces[f].uv=[bank*16,0,bank*16+16,16];}
return c;
}
let count=0;
for(let x=-3;x<=3;x++)for(let z=-3;z<=3;z++)for(let d=0;d<=3;d++){
const hit=x*x+z*z<=4&&d<=2&&2*d<=4-x*x-z*z;
if(hit)count++;
cube((hit?'REMOVE_':'GROUND_')+x+'_'+d+'_'+z,[16*x-8,-8-16*d,16*z-8],[16*x+8,8-16*d,16*z+8],hit?removed:terrain,d===0?0:(d<2?1:2),d<2?1:2);
}
if(count!==23)throw Error('Crater cell count '+count);
const person=grp('AUX_PERSON_1p8_blocks_not_runtime',main,[45.6,8,29.6]);
const px=45.6,pz=29.6,py=8;
cube('AUX_leg_L',[px-4.5,py,pz-2.2],[px-0.5,py+12,pz+2.2],person,3,3);
cube('AUX_leg_R',[px+0.5,py,pz-2.2],[px+4.5,py+12,pz+2.2],person,3,3);
cube('AUX_torso',[px-4.8,py+12,pz-2.5],[px+4.8,py+22.4,pz+2.5],person,3,3);
cube('AUX_arm_L',[px-8,py+12,pz-2],[px-5,py+22,pz+2],person,3,3);
cube('AUX_arm_R',[px+5,py+12,pz-2],[px+8,py+22,pz+2],person,3,3);
cube('AUX_head',[px-3.2,py+22.4,pz-3.2],[px+3.2,py+28.8,pz+3.2],person,3,3);
const dustRoot=grp('CANDIDATE_DUST_only_11_separate_emitters',main);
const anim=new Animation({name:'animation.earth_ground_v2_preview_only',length:2,loop:'loop',snapping:20}).add(false);
function key(g,ch,t,v,interp){const a=anim.getBoneAnimator(g);return a.addKeyframe({channel:ch,time:t,interpolation:interp||'linear',data_points:[{x:v[0],y:v[1],z:v[2]}]});}
key(removed,'scale',0,[1,1,1],'step');key(removed,'scale',0.4,[0,0,0],'step');key(removed,'scale',2,[0,0,0],'step');
const specs=[[-1.52,-0.22,1.15,-0.58,-0.10,0],[-1.05,1.05,1.22,-0.40,0.43,1],[0.15,1.62,1.12,0.04,0.60,2],[1.20,0.92,1.30,0.44,0.36,0],[1.62,-0.30,1.17,0.62,-0.15,2],[0.88,-1.27,1.28,0.36,-0.50,1],[-0.36,-1.62,1.16,-0.13,-0.64,0],[-1.28,-1.12,1.08,-0.47,-0.43,2],[-0.42,0.20,1.25,-0.30,0.14,1],[0.48,0.28,1.19,0.35,0.22,0],[0.10,-0.52,1.12,0.06,-0.40,2]];
const recipe=[];
specs.forEach((s,i)=>{
const [x,z,size,dx,dz,bank]=s, origin=[x*16,8.48,z*16],n=String(i+1).padStart(2,'0');
const motion=grp('dust_'+n+'_WORLD_motion',dustRoot,origin);
const billboard=grp('dust_'+n+'_CAMERA_PLANE',motion,origin);
billboard.rotation=[-34,42,0];
key(motion,'position',0,[0,0,0]);key(motion,'position',0.4,[0,0,0]);
key(motion,'position',0.8,[dx*9.6,1.8,dz*9.6]);key(motion,'position',1.2,[dx*16,3.8,dz*16]);key(motion,'position',2,[dx*16,3.8,dz*16]);
for(let f=0;f<15;f++){
const fg=grp('dust_'+n+'_frame_'+String(f+1).padStart(2,'0'),billboard,origin);
const c=new Cube({name:'pixel_card_'+n+'_'+f,from:[origin[0]-size*8,origin[1]-size*2,origin[2]],to:[origin[0]+size*8,origin[1]+size*14,origin[2]],autouv:0,box_uv:false}).addTo(fg).init();
for(const face of Object.values(c.faces))face.texture=null;
c.faces.south.texture=tex.uuid;c.faces.south.uv=[bank*32,f*32,bank*32+32,f*32+32];
key(fg,'scale',0,[0,0,0],'step');key(fg,'scale',+(0.4+f*0.05).toFixed(4),[1,1,1],'step');key(fg,'scale',+(0.45+f*0.05).toFixed(4),[0,0,0],'step');key(fg,'scale',2,[0,0,0],'step');
}
recipe.push({id:n,variant:'abc'[bank],rootOffsetFromImpactCenter:[x,0.53,z],textureAnchorPx:[16,28],cardSizeBlocks:[size,size],driftBlocks:[dx,0.2375,dz],durationMs:800,animationFrameMs:50});
});
Canvas.updateAll();
Undo.finishEdit('Create isolated earth ground v2 dust candidate');
Modes.options.animate.select();anim.select();Timeline.setTime(0.6);Animator.preview();
fs.writeFileSync(root+'/evidence/emitter-layout-candidate.json',JSON.stringify({project:Project.uuid,unitsPerBlock:16,impactCenterModel:[0,0,0],groundTopY:8,rootHeightAboveGroundBlocks:0.03,previewTriggerTime:0.4,dustDuration:0.8,clearTail:[1.2,2],removedCells:count,emitters:recipe},null,2));
return JSON.stringify({project:Project.uuid,elements:Outliner.elements.length,groups:Group.all.length,textures:Texture.all.length,animations:Animation.all.length,removed:count,emitters:recipe.length,needs:'native save and real viewport inspection'});
})()
