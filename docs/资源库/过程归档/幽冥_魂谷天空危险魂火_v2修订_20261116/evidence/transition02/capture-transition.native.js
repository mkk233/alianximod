(()=>{
const root='D:/alianximod/docs/资源库/过程归档/幽冥_魂谷天空危险魂火_v2修订_20261116';
if(Project.name!=='youming-v2-soulvalley-danger-candidate'||Cube.all.length!==273)throw Error('Open the unchanged rev01 candidate first');
const projectId=Project.uuid,preview=Preview.selected,originalSaved=Project.saved;
const originalProjects=ModelProject.all.map(project=>({uuid:project.uuid,name:project.name,saved:project.saved}));
const settings={
attemptId:'20261116-art-gloom-transition-02',
camera:{position:[0,25.92,42],target:[0,22,-50],fov:68,eyeBlocks:1.62},
durationSeconds:3.8,frameSeconds:.05,frameCount:76,
normalHold:[0,.4],enter:[.4,1],domainHold:[1,2.2],exit:[2.2,3],restoredHold:[3,3.8],
curve:'smoothstep(u)=u*u*(3-2*u)',
normalSky:[[0,'#75a6cf'],[.42,'#a8c5dc'],[.50,'#d0dbe1'],[.56,'#a9b8bf'],[1,'#6c7d88']],
normalSwatches:{AUX_distant_ground:'#65696a',AUX_far_fog_tinted_rock:'#7f8c91',AUX_middle_fog_tinted_rock:'#626c72',AUX_near_fog_tinted_rock:'#4e5559'},
meaning:'Fixed viewpoint environment tone mix only; no volume test, boundary crossing or expiry. Fire stays lit and animates continuously.',
candidateMaterial:'The existing rev01 sky and distant scene texture maps; not modified or replaced on disk.',
normalTone:'Neutral daytime demonstration reference, not captured Minecraft normal sky.'
};
const smoothstep=value=>{const clamped=Math.max(0,Math.min(1,value));return clamped*clamped*(3-2*clamped);};
const blendAt=time=>time<.4?0:time<1?smoothstep((time-.4)/.6):time<2.2?1:time<3?1-smoothstep((time-2.2)/.8):0;
const targets=Texture.all.filter(texture=>texture.name==='AUX_soulvalley_sky_gradient_preview_only'||Object.hasOwn(settings.normalSwatches,texture.name));
if(targets.length!==5)throw Error('Expected exactly five existing atmosphere proxy materials');
const temporary=[];
Animation.all[0].select();Timeline.pause();TextureAnimator.stop();
preview.setProjectionMode(false);preview.camPers.fov=settings.camera.fov;preview.camPers.updateProjectionMatrix();preview.camera.position.fromArray(settings.camera.position);preview.controls.target.fromArray(settings.camera.target);preview.controls.update();
Undo.initEdit({textures:targets});
try{
for(const texture of targets){
const material=texture.getMaterial(),originalFragment=material.fragmentShader,originalMap=material.uniforms.map.value;
const canvas=document.createElement('canvas');canvas.width=texture.img.naturalWidth;canvas.height=texture.img.naturalHeight;const context=canvas.getContext('2d');
if(texture.name==='AUX_soulvalley_sky_gradient_preview_only'){
const gradient=context.createLinearGradient(0,0,0,canvas.height);for(const [stop,color] of settings.normalSky)gradient.addColorStop(stop,color);context.fillStyle=gradient;
}else context.fillStyle=settings.normalSwatches[texture.name];
context.fillRect(0,0,canvas.width,canvas.height);
const normalMap=originalMap.clone();normalMap.image=canvas;normalMap.needsUpdate=true;
if(!originalFragment.includes('vec4 color=texture2D(map,vUv);'))throw Error('Unexpected native texture shader');
material.uniforms.transitionNormalMap={value:normalMap};material.uniforms.transitionBlend={value:0};
material.fragmentShader=originalFragment.replace('void main(void){','uniform sampler2D transitionNormalMap;uniform float transitionBlend;void main(void){').replace('vec4 color=texture2D(map,vUv);','vec4 color=mix(texture2D(transitionNormalMap,vUv),texture2D(map,vUv),transitionBlend);');
material.needsUpdate=true;temporary.push({texture: texture.uuid,name:texture.name,material,originalFragment,normalMap});
}
const frames=[];
for(let index=0;index<settings.frameCount;index++){
const time=index*settings.frameSeconds,weight=blendAt(time),animationTime=.7+time;
Timeline.setTime(animationTime);Animator.preview();TextureAnimator.playAnimationFrame(animationTime+1e-8);
for(const item of temporary)item.material.uniforms.transitionBlend.value=weight;
for(const texture of Texture.all){const material=texture.getMaterial();if(material.uniforms?.SHADE)material.uniforms.SHADE.value=false;const light=material.uniforms?.LIGHTCOLOR?.value;if(light?.setRGB)light.setRGB(1,1,1);}
Canvas.updateVisibility();
const file='frames/transition02/tone_'+String(index).padStart(3,'0')+'.png';
let written=false;
Screencam.screenshotPreview(preview,{crop:false,width:960,height:684},url=>{
Blockbench.writeFile(root+'/'+file,{savetype:'image',content:url});
const keyNames={0:'transition-normal.png',28:'transition-domain.png',68:'transition-restored.png'};
if(keyNames[index])Blockbench.writeFile(root+'/'+keyNames[index],{savetype:'image',content:url});
written=true;
});
if(!written)throw Error('Synchronous native viewport screenshot did not finish');
frames.push({file,time,animationTime,domainBlend:weight,viewport:preview.id});
}
Blockbench.writeFile(root+'/evidence/transition02/native-capture.json',{content:JSON.stringify({settings,project:projectId,viewport:preview.id,source:'Existing Blockbench GUI, native preview shader material blend before Screencam.screenshotPreview; no postprocess image filter',targetTextures:temporary.map(item=>({uuid:item.texture,name:item.name})),frames},null,2)});
}finally{
for(const item of temporary){item.material.fragmentShader=item.originalFragment;delete item.material.uniforms.transitionNormalMap;delete item.material.uniforms.transitionBlend;item.material.needsUpdate=true;item.normalMap.dispose();}
Timeline.setTime(.7);Animator.preview();TextureAnimator.playAnimationFrame(.70000001);Canvas.updateVisibility();
Undo.finishEdit('Temporary native preview atmosphere interpolation captured; original materials restored');
}
const diskModel=JSON.parse(require('fs').readFileSync(root+'/youming-v2-soulvalley-danger-candidate.bbmodel','utf8')),liveModel=JSON.parse(Codecs.project.compile());
const changedModelKeys=Array.from(new Set([...Object.keys(diskModel),...Object.keys(liveModel)])).filter(key=>JSON.stringify(diskModel[key])!==JSON.stringify(liveModel[key]));
if(originalSaved&&changedModelKeys.length===0)Project.saved=true;
const proof={project:Project.uuid,savedBefore:originalSaved,savedAfter:Project.saved,undoActive:!!Undo.current_save,temporaryShadersRestored:temporary.every(item=>item.material.fragmentShader===item.originalFragment&&!item.material.uniforms.transitionBlend),originalProjects:originalProjects.map(before=>({ ...before,stillPresent:ModelProject.all.some(project=>project.uuid===before.uuid),savedAfter:ModelProject.all.find(project=>project.uuid===before.uuid)?.saved})),modelWritten:false,sourceTexturesWritten:false};
Blockbench.writeFile(root+'/evidence/transition02/restoration.json',{content:JSON.stringify(proof,null,2)});
return {project:projectId,frames:settings.frameCount,seconds:settings.durationSeconds,entrySeconds:.6,exitSeconds:.8,temporaryShadersRestored:proof.temporaryShadersRestored,savedBefore:originalSaved,savedAfter:Project.saved,undoActive:proof.undoActive};
})()
