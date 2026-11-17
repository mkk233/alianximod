(function(){
const root='D:/alianximod/docs/资源库/过程归档/崩地_飞扬土尘_v2r2_20261116';
if(Project.uuid!=='937be075-0517-53f5-6aa9-c7f18ce35ab8'||Outliner.elements.length)throw Error('Isolated target not empty or not current');
const fs=require('fs');
const recipe=JSON.parse(fs.readFileSync(root+'/evidence/motion-recipe.json','utf8'));
const before=ModelProject.all.filter(project=>project.uuid!==Project.uuid).map(project=>({name:project.name,uuid:project.uuid,saved:project.saved}));
Undo.initEdit({elements:[],outliner:true,textures:[],animations:[]});
Project.texture_width=144;Project.texture_height=1152;
function texture(name,file,width,height){
return new Texture({name,render_mode:'default',render_sides:'double',uv_width:width,uv_height:height}).fromDataURL('data:image/png;base64,'+fs.readFileSync(root+'/exports/'+file).toString('base64')).add(false);
}
const dustTexture=texture('CANDIDATE_airborne_3x24_frames','ground_airborne_v2r2-atlas.png',144,1152);
const gritTexture=texture('CANDIDATE_visual_grit_alpha','visual_grit_v2r2-atlas.png',24,32);
const auxTexture=texture('AUX_ONLY_ground_and_human','preview_auxiliary_palette.png',64,16);
const main=new Group({name:'ANCHOR_C_atCenterOf_impact_16units_per_block',origin:[0,0,0]}).init();
function group(name,parent,origin){return new Group({name,origin:origin||[0,0,0]}).addTo(parent).init();}
const terrain=group('AUX_flat_ground_7x7',main);
const removed=group('AUX_REMOVE_exact_23_cells_preview_simulation',main);
function cube(name,from,to,parent,topBank,sideBank){
const element=new Cube({name,from,to,autouv:0,box_uv:false}).addTo(parent).init();
for(const faceName of Object.keys(element.faces)){
const bank=faceName==='up'?topBank:sideBank;
element.faces[faceName].texture=auxTexture.uuid;
element.faces[faceName].uv=[bank*16,0,bank*16+16,16];
}
return element;
}
const cells=[];
for(let east=-3;east<=3;east++)for(let south=-3;south<=3;south++)for(let depth=0;depth<=3;depth++){
const hit=east*east+south*south<=4&&depth<=2&&2*depth<=4-east*east-south*south;
if(hit)cells.push([east,-depth,south]);
cube((hit?'REMOVE_':'GROUND_')+east+'_'+depth+'_'+south,[16*east-8,-8-16*depth,16*south-8],[16*east+8,8-16*depth,16*south+8],hit?removed:terrain,depth===0?0:(depth<2?1:2),depth<2?1:2);
}
if(cells.length!==23)throw Error('Crater mismatch');
const person=group('AUX_PERSON_1p8_blocks_not_runtime',main,[44,8,31]);
const px=44,pz=31,py=8;
cube('AUX_leg_L',[px-4.5,py,pz-2.2],[px-0.5,py+12,pz+2.2],person,3,3);
cube('AUX_leg_R',[px+0.5,py,pz-2.2],[px+4.5,py+12,pz+2.2],person,3,3);
cube('AUX_torso',[px-4.8,py+12,pz-2.5],[px+4.8,py+22.4,pz+2.5],person,3,3);
cube('AUX_arm_L',[px-8,py+12,pz-2],[px-5,py+22,pz+2],person,3,3);
cube('AUX_arm_R',[px+5,py+12,pz-2],[px+8,py+22,pz+2],person,3,3);
cube('AUX_head',[px-3.2,py+22.4,pz-3.2],[px+3.2,py+28.8,pz+3.2],person,3,3);
const dustRoot=group('CANDIDATE_9_asymmetric_lifted_dust_streams',main);
const gritRoot=group('CANDIDATE_12_visual_grains_NO_blocks_drops_damage',main);
const animation=new Animation({name:'animation.earth_airborne_v2r2_preview_only',length:recipe.previewSeconds,loop:'loop',snapping:20}).add(false);
function key(target,channel,time,values,interpolation){
return animation.getBoneAnimator(target).addKeyframe({channel,time:+time.toFixed(5),interpolation:interpolation||'linear',data_points:[{x:values[0],y:values[1],z:values[2]}]});
}
function visible(target,start,end){
key(target,'scale',0,[0,0,0],'step');key(target,'scale',start,[1,1,1],'step');key(target,'scale',end,[0,0,0],'step');key(target,'scale',recipe.previewSeconds,[0,0,0],'step');
}
key(removed,'scale',0,[1,1,1],'step');key(removed,'scale',recipe.trigger,[0,0,0],'step');key(removed,'scale',recipe.previewSeconds,[0,0,0],'step');
recipe.dust.forEach((spec,index)=>{
const [east,south,size,driftX,driftZ,rise,delay,bank]=spec;
const origin=[east*16,recipe.rootY*16,south*16];
const label=String(index+1).padStart(2,'0');
const motion=group('dust_'+label+'_WORLD_motion',dustRoot,origin);
const card=group('dust_'+label+'_CAMERA_PLANE',motion,origin);
const birth=recipe.trigger+delay;
key(motion,'position',0,[0,0,0]);
const poses=[[0,0,0],[0.10,0.15,0.46],[0.25,0.38,0.88],[0.40,0.62,1],[0.65,0.82,0.94],[0.90,0.96,0.84],[1.20,1.08,0.77]];
for(const [age,spread,height] of poses)key(motion,'position',birth+age,[driftX*16*spread,rise*16*height,driftZ*16*spread]);
for(let frame=0;frame<23;frame++){
const phase=group('dust_'+label+'_FRAME_'+String(frame+1).padStart(2,'0'),card,origin);
const element=new Cube({name:'pixel_cluster_'+label+'_'+frame,from:[origin[0]-size*8,origin[1]-size*16/12,origin[2]],to:[origin[0]+size*8,origin[1]+size*16*11/12,origin[2]],autouv:0,box_uv:false}).addTo(phase).init();
for(const face of Object.values(element.faces))face.texture=null;
element.faces.south.texture=dustTexture.uuid;
element.faces.south.uv=[bank*48,frame*48,bank*48+48,frame*48+48];
visible(phase,birth+frame*.05,birth+(frame+1)*.05);
}
});
recipe.grit.forEach((spec,index)=>{
const [east,south,driftX,driftZ,apex,flight,size,delay]=spec;
const origin=[east*16,recipe.rootY*16,south*16];
const label=String(index+1).padStart(2,'0');
const motion=group('grit_'+label+'_BALLISTIC_visual_only',gritRoot,origin);
const birth=recipe.trigger+delay;
key(motion,'position',0,[0,0,0]);
for(let sample=0;sample<=22;sample++){
const phase=sample/22;
key(motion,'position',birth+phase*flight,[driftX*phase*16,4*apex*phase*(1-phase)*16,driftZ*phase*16]);
key(motion,'rotation',birth+phase*flight,[phase*(90+index*13),phase*(145-index*5),phase*(35+index*11)]);
}
for(let fade=0;fade<3;fade++){
const visual=group('grit_'+label+'_ALPHA_'+[255,160,64][fade],motion,origin);
const half=size*8;
const element=new Cube({name:'visual_soil_grain_'+label+'_'+fade,from:[origin[0]-half,origin[1]-half*.72,origin[2]-half*.85],to:[origin[0]+half,origin[1]+half*.72,origin[2]+half*.85],autouv:0,box_uv:false}).addTo(visual).init();
for(const face of Object.values(element.faces)){face.texture=gritTexture.uuid;face.uv=[(index%3)*8,fade*8,(index%3)*8+8,fade*8+8];}
const starts=[birth,birth+flight-.15,birth+flight-.075],ends=[birth+flight-.15,birth+flight-.075,birth+flight];
visible(visual,starts[fade],ends[fade]);
}
});
Canvas.updateAll();
Undo.finishEdit('Create isolated airborne soil revision with ballistic grit');
Modes.options.animate.select();animation.select();Timeline.setTime(.8);Animator.preview();
fs.writeFileSync(root+'/evidence/native-build.json',JSON.stringify({project:Project.uuid,name:Project.name,previousProjects:before,removedCells:cells,elements:Outliner.elements.length,groups:Group.all.length,textures:Texture.all.length,animations:Animation.all.length,recipe},null,2));
return JSON.stringify({project:Project.uuid,elements:Outliner.elements.length,groups:Group.all.length,textures:Texture.all.length,animations:Animation.all.length,removedCells:cells.length,dustStreams:9,ballisticGrains:12});
})()
