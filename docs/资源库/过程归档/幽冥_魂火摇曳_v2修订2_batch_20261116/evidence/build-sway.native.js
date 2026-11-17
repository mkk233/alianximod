(async()=>{
const data=window.gloomSway;
if(Project.uuid!==data.project)throw Error('Wrong project');
const fs=require('fs'),animation=Animation.all[0];
animation.select();Timeline.pause();TextureAnimator.stop();
Undo.initEdit({textures:Texture.all,animations:Animation.all});
const inputUrl='data:image/png;base64,'+fs.readFileSync(data.dest+'/textures/youming_v2_sway_strip.png').toString('base64');
const image=new Image();
await new Promise((resolve,reject)=>{image.onload=resolve;image.onerror=reject;image.src=inputUrl;});
const materialCanvas=document.createElement('canvas');materialCanvas.width=64;materialCanvas.height=1024;
const context=materialCanvas.getContext('2d');context.drawImage(image,0,0);
const pixels=context.getImageData(0,0,64,1024);
for(let offset=3;offset<pixels.data.length;offset+=4)pixels.data[offset]=Math.round(pixels.data[offset]*210/255);
context.putImageData(pixels,0,0);
const materialUrl=materialCanvas.toDataURL();
Blockbench.writeFile(data.dest+'/textures/preview_alpha210.png',{savetype:'image',content:materialUrl});
const phases=Texture.all.filter(texture=>texture.name.startsWith('v2_soulvalley_alpha210_phase_'));
for(const texture of phases){texture.name=texture.name.replace('v2_soulvalley_alpha210','v2_sway_fixedroot_alpha210');texture.fromDataURL(materialUrl);}
for(const texture of Texture.all.filter(texture=>texture.name==='youming_v2_soulvalley_strip.png'||texture.name==='nether_gloom_v2_runtime.png')){
texture.name='SOURCE_sway04_fixed_root_64x1024';texture.fromDataURL(inputUrl);
}
const removed=[];
for(const group of Group.all.filter(group=>group.name.startsWith('WISP_'))){
const animator=animation.getBoneAnimator(group);
for(const keyframe of [...animator.scale]){removed.push({group:group.uuid,channel:keyframe.channel,time:keyframe.time});keyframe.remove();}
}
animation.name='CANDIDATE_fixed_root_local_tongue_sway_100ms_NO_cluster_scale';
animation.length=3.2;
Undo.finishEdit('Cancel 49 whole-cluster scale curves; use Aseprite fixed-root local tongue frames');
Canvas.updateAll();Timeline.setTime(0);Animator.preview();TextureAnimator.playAnimationFrame(0.00000001);
data.phaseTextures=phases.map(texture=>texture.uuid);
data.animation=animation.uuid;
data.samples=JSON.parse(fs.readFileSync('D:/alianximod/docs/资源库/过程归档/幽冥_魂谷天空危险魂火_v2修订_20261116/evidence/native-scene.json','utf8')).samples;
data.removed=removed;
data.originalGeometry=Cube.all.filter(cube=>cube.parent?.name.startsWith('WISP_')).map(cube=>({uuid:cube.uuid,from:[...cube.from],to:[...cube.to],rotation:[...cube.rotation]}));
Blockbench.writeFile(data.dest+'/evidence/native-scene.json',{content:JSON.stringify(data,null,2)});
Codecs.project.write(Codecs.project.compile(),data.dest+'/youming-v2-fixed-root-sway-candidate.bbmodel');
return {project:Project.uuid,saved:Project.saved,removedScaleKeys:removed.length,clusters:data.samples.length,cubes:Cube.all.length,textures:Texture.all.length,undoActive:!!Undo.current_save};
})()
