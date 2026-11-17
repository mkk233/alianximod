(()=>{
const data=window.gloomRevision;
if(Project.uuid!==data.project)throw Error('Wrong project');
const preview=Preview.selected,animation=Animation.all.find(animation=>animation.uuid===data.animation);
animation.select();Timeline.pause();TextureAnimator.stop();
const skyGroup=Group.all.find(group=>group.uuid===data.sky),sceneryGroup=Group.all.find(group=>group.uuid===data.scenery);
const nativePose=time=>{Timeline.setTime(time);Animator.preview();TextureAnimator.playAnimationFrame(time+1e-8);for(const texture of Texture.all){const material=texture.getMaterial();if(material.uniforms?.SHADE)material.uniforms.SHADE.value=false;const light=material.uniforms?.LIGHTCOLOR?.value;if(light?.setRGB)light.setRGB(1,1,1);}Canvas.updateVisibility();};
const camera=(position,target,fov)=>{preview.setProjectionMode(false);preview.camPers.fov=fov;preview.camPers.updateProjectionMatrix();preview.camera.position.fromArray(position);preview.controls.target.fromArray(target);preview.controls.update();};
const views=[];
const capture=(file,time,width=1280,height=912)=>{nativePose(time);let written=false;Screencam.screenshotPreview(preview,{crop:false,width,height},url=>{Blockbench.writeFile(data.dest+'/'+file,{savetype:'image',content:url});written=true;});if(!written)throw Error('Native synchronous capture failed');views.push({file,time,position:preview.camera.position.toArray(),target:preview.controls.target.toArray(),fov:preview.camPers.fov,viewport:preview.id,skyProxyVisible:skyGroup.children[0].visibility});};
const proxyCubes=Cube.all.filter(cube=>cube.parent===skyGroup||cube.parent===sceneryGroup);
Undo.initEdit({elements:proxyCubes});
for(const cube of proxyCubes)cube.visibility=false;
camera([113,93,127],[0,8,0],42);capture('02-overall-three-quarter.png',.7);
camera([110,31,25],[-8,14,0],42);capture('03-side-person-height.png',.7);
for(const cube of proxyCubes)cube.visibility=true;
Undo.finishEdit('Capture exterior inspection views and restore interior proxy visibility');
camera(data.comparisonCamera.position,data.comparisonCamera.target,data.comparisonCamera.fov);
capture('01-inside-eye-sky-and-fire.png',.7);
const frames=[];
for(let index=0;index<80;index++){
const time=index*.05,file='frames/inside_'+String(index).padStart(3,'0')+'.png';
capture(file,time,960,684);
frames.push({file,time,textureFrames:data.phaseTextures.map(uuid=>Texture.all.find(texture=>texture.uuid===uuid).currentFrame)});
}
nativePose(.7);
Blockbench.writeFile(data.dest+'/evidence/capture.json',{content:JSON.stringify({nativeSource:'Screencam.screenshotPreview, existing live main viewport, synchronous',project:Project.uuid,views:views.slice(0,3),comparison:{old:'compare-old-same-camera.png',new:'01-inside-eye-sky-and-fire.png',...data.comparisonCamera,time:.7},frames,frameDurationMs:50,duration:4,transitionDemonstrated:false,note:'Steady inside-domain atmosphere; no synthetic interpolation; loop resets excerpt, not spell lifetime.'},null,2)});
Codecs.project.write(Codecs.project.compile(),data.dest+'/youming-v2-soulvalley-danger-candidate.bbmodel');
return {project:Project.uuid,saved:Project.saved,views:views.slice(0,3),frames:frames.length,undoActive:!!Undo.current_save};
})()
