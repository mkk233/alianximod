(()=>{
const d=window.gloomPreview;if(Project.uuid!==d.project)throw Error('wrong project');
Animation.all.find(a=>a.uuid===d.animation).select();Timeline.pause();TextureAnimator.stop();
const p=Preview.selected;p.setProjectionMode(false);p.camPers.fov=42;p.camPers.updateProjectionMatrix();p.camera.position.set(113,93,127);p.controls.target.set(0,8,0);p.controls.update();
const pose=t=>{Timeline.setTime(t);Animator.preview();TextureAnimator.playAnimationFrame(t+1e-8);for(const id of d.phaseTextures){const m=Texture.all.find(t=>t.uuid===id).getMaterial();m.uniforms.SHADE.value=false;const c=m.uniforms.LIGHTCOLOR.value;if(c.setRGB)c.setRGB(1,1,1);else c.set(1,1,1);}Canvas.updateVisibility();};
const frames=[];
for(let n=0;n<96;n++){
 const t=n*.05;pose(t);const file='frames/domain_'+String(n).padStart(3,'0')+'.png';
 let wrote=false;Screencam.screenshotPreview(p,{crop:false,width:960,height:684},url=>{Blockbench.writeFile(d.dest+'/'+file,{savetype:'image',content:url});wrote=true;});
 if(!wrote)throw Error('native viewport frame not written');
 frames.push({file,time:t,textureFrames:d.phaseTextures.map(id=>Texture.all.find(t=>t.uuid===id).currentFrame)});
}
pose(.7);
Blockbench.writeFile(d.dest+'/evidence/animation-capture.json',{content:JSON.stringify({project:Project.uuid,viewport:p.id,source:'Screencam.screenshotPreview live main viewport',frames,frameDurationMs:50,durationSeconds:4.8,note:'Demonstration segment restarts at GIF loop; not domain lifetime. No synthetic frames.'},null,2)});
return {count:frames.length,durationSeconds:4.8,first:frames[0],last:frames[95],project:Project.uuid};
})()
