window.captureGloomSway=mode=>{
const data=window.gloomSway;
if(Project.uuid!==data.project)throw Error('Wrong project');
const preview=Preview.selected,animation=Animation.all[0];
animation.select();Timeline.pause();TextureAnimator.stop();
const wisps=Group.all.filter(group=>group.name.startsWith('WISP_'));
const proxies=Cube.all.filter(cube=>cube.parent?.name.startsWith('AUX_SKY_')||cube.parent?.name.startsWith('AUX_DISTANT_SCENERY_'));
const visibility=Cube.all.map(cube=>({cube,visible:cube.visibility}));
const camera=(position,target,fov)=>{preview.setProjectionMode(false);preview.camPers.fov=fov;preview.camPers.updateProjectionMatrix();preview.camera.position.fromArray(position);preview.controls.target.fromArray(target);preview.controls.update();};
const pose=time=>{
Timeline.setTime(time);Animator.preview();TextureAnimator.playAnimationFrame(time+0.00000001);
for(const texture of Texture.all){
const material=texture.getMaterial();
if(material.uniforms?.SHADE)material.uniforms.SHADE.value=false;
if(material.uniforms?.LIGHTCOLOR?.value?.setRGB)material.uniforms.LIGHTCOLOR.value.setRGB(1,1,1);
}
Canvas.updateVisibility();
};
const frames=[];
const capture=(file,time,width=960,height=684)=>{
pose(time);
let written=false;
Screencam.screenshotPreview(preview,{crop:false,width,height},url=>{Blockbench.writeFile(data.dest+'/'+file,{savetype:'image',content:url});written=true;});
if(!written)throw Error('Native synchronous screenshot did not finish');
const scales=wisps.map(group=>({name:group.name,scale:group.mesh.scale.toArray(),position:group.mesh.position.toArray(),rotation:group.mesh.rotation.toArray().slice(0,3)}));
frames.push({file,time,textureFrames:data.phaseTextures.map(uuid=>Texture.all.find(texture=>texture.uuid===uuid).currentFrame),transforms:scales});
};
Undo.initEdit({elements:Cube.all});
try{
if(mode==='inside'){
camera([0,25.92,42],[0,22,-50],68);
capture('01-inside-sway.png',0.7,1280,912);
for(let index=0;index<64;index++)capture('frames/inside/frame_'+String(index).padStart(3,'0')+'.png',index*0.05);
}
if(mode==='close'){
const focus=wisps.find(group=>group.name.startsWith('WISP_24_'));
for(const cube of Cube.all){
if(cube.parent?.name.startsWith('WISP_')&&cube.parent!==focus)cube.visibility=false;
if(cube.parent?.name.startsWith('EMBER_'))cube.visibility=false;
}
const position=focus.origin;
camera([position[0]+29,position[1]+11,position[2]+13],[position[0],position[1]+7,position[2]],37);
capture('02-close-fixed-root.png',0.7,1280,912);
for(let index=0;index<64;index++)capture('frames/close/frame_'+String(index).padStart(3,'0')+'.png',index*0.05);
}
if(mode==='side'){
for(const cube of proxies)cube.visibility=false;
camera([110,31,25],[-8,14,0],42);
capture('03-side-person-height.png',0.7,1280,912);
}
}finally{
for(const entry of visibility)entry.cube.visibility=entry.visible;
Undo.finishEdit('Native '+mode+' viewport capture; restore all original visibility');
camera([0,25.92,42],[0,22,-50],68);pose(0.7);
}
const proof={nativeSource:'Screencam.screenshotPreview / existing GUI main viewport / synchronous',project:Project.uuid,mode,frames,frameDurationMs:50,duration:mode==='side'?0:3.2,allWispScalesIdentity:frames.every(frame=>frame.transforms.every(transform=>transform.scale.every(value=>Math.abs(value-1)<1e-8))),cameraFixed:true,previewOnly:true};
Blockbench.writeFile(data.dest+'/evidence/capture-'+mode+'.json',{content:JSON.stringify(proof,null,2)});
return {mode,project:Project.uuid,frames:frames.length,allWispScalesIdentity:proof.allWispScalesIdentity,undoActive:!!Undo.current_save};
};
