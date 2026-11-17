(async()=>{
const dest='D:/alianximod/docs/资源库/过程归档/幽冥_魂谷天空危险魂火_v2修订_20261116';
if(Project.uuid!=='de80cb3f-d9bf-e9be-d20d-95c24ad180c9')throw Error('Wrong project');
const fs=require('fs'),preview=Preview.selected;
const animation=Animation.all[0];
animation.select();Timeline.pause();TextureAnimator.stop();
const oldTextures=Texture.all.filter(texture=>texture.name.startsWith('v2_alpha210_phase_'));
const white=texture=>{const material=texture.getMaterial();material.uniforms.SHADE.value=false;const light=material.uniforms.LIGHTCOLOR.value;if(light.setRGB)light.setRGB(1,1,1);else light.set(1,1,1);};
const camera=(position,target,fov=68)=>{preview.setProjectionMode(false);preview.camPers.fov=fov;preview.camPers.updateProjectionMatrix();preview.camera.position.fromArray(position);preview.controls.target.fromArray(target);preview.controls.update();};
const capture=file=>{let written=false;Screencam.screenshotPreview(preview,{crop:false,width:1280,height:912},url=>{Blockbench.writeFile(dest+'/'+file,{savetype:'image',content:url});written=true;});if(!written)throw Error('Native synchronous screenshot failed');};
camera([0,25.92,42],[0,22,-50]);
Timeline.setTime(.7);Animator.preview();TextureAnimator.playAnimationFrame(.70000001);oldTextures.forEach(white);Canvas.updateVisibility();
capture('compare-old-same-camera.png');
const elements=[],groups=[],textures=[];
Undo.initEdit({elements:Cube.all,textures:Texture.all,animations:Animation.all,groups:Group.all,outliner:true});
const source=new Texture({name:'v2_soulvalley_Aseprite_palette_unchanged_mask',uv_width:64,uv_height:64}).fromPath(dest+'/textures/youming_v2_soulvalley_strip.png').add(false);
textures.push(source);
await new Promise((resolve,reject)=>{if(source.img.complete&&source.img.naturalWidth)resolve();else{source.img.addEventListener('load',resolve,{once:true});source.img.addEventListener('error',reject,{once:true});}});
const materialCanvas=document.createElement('canvas');materialCanvas.width=64;materialCanvas.height=1024;
const materialContext=materialCanvas.getContext('2d');materialContext.drawImage(source.img,0,0);
const pixels=materialContext.getImageData(0,0,64,1024);for(let offset=3;offset<pixels.data.length;offset+=4)pixels.data[offset]=Math.round(pixels.data[offset]*210/255);materialContext.putImageData(pixels,0,0);
Blockbench.writeFile(dest+'/textures/preview_alpha210.png',{savetype:'image',content:materialCanvas.toDataURL()});
for(const texture of oldTextures){texture.name=texture.name.replace('v2_alpha210','v2_soulvalley_alpha210');texture.fromDataURL(materialCanvas.toDataURL());}
const swatch=(name,color)=>{const canvas=document.createElement('canvas');canvas.width=canvas.height=16;const context=canvas.getContext('2d');context.fillStyle=color;context.fillRect(0,0,16,16);const texture=new Texture({name,uv_width:16,uv_height:16,render_sides:'double'}).fromDataURL(canvas.toDataURL()).add(false);textures.push(texture);return texture;};
const groundA=swatch('AUX_ground_coal_teal','#182c33'),groundB=swatch('AUX_ground_dry_teal','#21353c');
for(const cube of Cube.all.filter(cube=>cube.name.startsWith('AUX_1block_tile_'))){const parts=cube.name.split('_'),sum=Number(parts.at(-1))+Number(parts.at(-2));for(const face of Object.values(cube.faces))face.texture=(sum%2?groundA:groundB).uuid;}
const sky=new Group({name:'AUX_SKY_PREVIEW_ONLY_not_domain_boundary',origin:[0,0,0]}).init();groups.push(sky);
const scenery=new Group({name:'AUX_DISTANT_SCENERY_PREVIEW_ONLY_not_runtime',origin:[0,0,0]}).init();groups.push(scenery);
const skyCanvas=document.createElement('canvas');skyCanvas.width=16;skyCanvas.height=256;const skyContext=skyCanvas.getContext('2d');
const gradient=skyContext.createLinearGradient(0,0,0,256);gradient.addColorStop(0,'#081923');gradient.addColorStop(.42,'#102c3b');gradient.addColorStop(.50,'#416e77');gradient.addColorStop(.56,'#294a55');gradient.addColorStop(1,'#101f29');skyContext.fillStyle=gradient;skyContext.fillRect(0,0,16,256);
const skyTexture=new Texture({name:'AUX_soulvalley_sky_gradient_preview_only',uv_width:16,uv_height:256,render_sides:'double'}).fromDataURL(skyCanvas.toDataURL()).add(false);textures.push(skyTexture);
const box=(name,from,to,texture,parent)=>{const cube=new Cube({name,from,to,autouv:0,box_uv:false,shade:false});for(const face of Object.values(cube.faces)){face.texture=texture.uuid;face.uv=[0,0,16,16];}cube.addTo(parent).init();elements.push(cube);return cube;};
const skyCube=box('AUX_sky_enclosure_256blocks_NOT_world_effect',[-2048,-2048,-2048],[2048,2048,2048],skyTexture,sky);
for(const direction of ['north','south','east','west'])skyCube.faces[direction].uv=[0,0,16,256];
skyCube.faces.up.uv=[0,0,16,1];skyCube.faces.down.uv=[0,255,16,256];
const farGround=swatch('AUX_distant_ground','#233a43');
box('AUX_horizon_ground_proxy',[-1800,-2.1,-1800],[1800,-1.6,1800],farGround,scenery);
const ridgeTextures=[swatch('AUX_far_fog_tinted_rock','#31525e'),swatch('AUX_middle_fog_tinted_rock','#243e49'),swatch('AUX_near_fog_tinted_rock','#1b303b')];
for(let index=0;index<32;index++){
const angle=index/32*Math.PI*2,radius=380+(index%3)*100,width=40+(index*17%55),height=40+(index*37%95),centerX=Math.sin(angle)*radius,centerZ=Math.cos(angle)*radius;
box('AUX_distant_silhouette_'+index,[centerX-width/2,-1,centerZ-width/2],[centerX+width/2,height,centerZ+width/2],ridgeTextures[index%3],scenery);
box('AUX_distant_silhouette_step_'+index,[centerX-width*.3,height,centerZ-width*.3],[centerX+width*.28,height+18+(index%4)*11,centerZ+width*.28],ridgeTextures[index%3],scenery);
}
const samples=JSON.parse(fs.readFileSync('D:/alianximod/docs/资源库/过程归档/幽冥_v2领域定稿预览_20261116/evidence/scene-parameters.json','utf8')).samples;
for(const sample of samples){
const group=Group.all.find(group=>group.uuid===sample.group),widthScale=.94,heightScale=1.16;
for(const cube of group.children){for(const axis of [0,2]){cube.from[axis]=group.origin[axis]+(cube.from[axis]-group.origin[axis])*widthScale;cube.to[axis]=group.origin[axis]+(cube.to[axis]-group.origin[axis])*widthScale;}cube.to[1]=group.origin[1]+(cube.to[1]-group.origin[1])*heightScale;}
const animator=animation.getBoneAnimator(group),point=animator.scale[0].data_points[0],phase=(sample.index*2.4*180/Math.PI).toFixed(10);
const pulse='(0.5+0.5*math.sin(query.anim_time*93.75+'+phase+'))';
point.x='0.82+0.18*'+pulse;point.z=point.x;point.y='0.52+0.48*'+pulse;
}
const emberRoot=new Group({name:'VISUAL_CANDIDATE_8_sparse_embers_no_gameplay',origin:[0,0,0]}).init();groups.push(emberRoot);
const emberTexture=swatch('VISUAL_ember_core_derived_existing_v2_palette','#ccfaf5'),emberGroups=[];
for(let index=0;index<8;index++){
const sample=samples[(index*6+2)%49],position=[sample.x*16,2.4,sample.z*16],group=new Group({name:'EMBER_'+index+'_visual_only',origin:position}).addTo(emberRoot).init();groups.push(group);emberGroups.push(group.uuid);
const size=.45+(index%3)*.16;
box('EMBER_pixel_'+index,[position[0]-size/2,position[1],position[2]-size/2],[position[0]+size/2,position[1]+size*1.5,position[2]+size/2],emberTexture,group);
const animator=animation.getBoneAnimator(group),phase=index*.19,life='math.mod(query.anim_time+'+phase+',1.6)/1.6',growth='math.sin(('+life+')*180)';
animator.addKeyframe({channel:'position',time:0,interpolation:'linear',data_points:[{x:'.65*math.sin(query.anim_time*112.5+'+index*41+')',y:'9+15*('+life+')',z:'.5*math.cos(query.anim_time*112.5+'+index*41+')'}]});
animator.addKeyframe({channel:'scale',time:0,interpolation:'linear',data_points:[{x:growth,y:growth,z:growth}]});
}
animation.name='CANDIDATE_low_ripping_flames_sparse_embers_DEMO';
Undo.finishEdit('Soul-valley atmosphere and dangerous low v2 flame candidate',{elements:Cube.all,textures:Texture.all,groups:Group.all,animations:Animation.all,outliner:true});
Canvas.updateAll();
window.gloomRevision={dest,project:Project.uuid,animation:animation.uuid,phaseTextures:oldTextures.map(texture=>texture.uuid),sky:sky.uuid,scenery:scenery.uuid,emberRoot:emberRoot.uuid,emberGroups,samples,comparisonCamera:{position:[0,25.92,42],target:[0,22,-50],fov:68},originalProjects:window.gloomRevisionBefore};
Blockbench.writeFile(dest+'/evidence/native-scene.json',{content:JSON.stringify(window.gloomRevision,null,2)});
return {project:Project.uuid,clusters:samples.length,emberCount:8,skyProxy:'native model far enclosure + distant scenery, not domain walls',textures:Texture.all.length,cubes:Cube.all.length};
})()
