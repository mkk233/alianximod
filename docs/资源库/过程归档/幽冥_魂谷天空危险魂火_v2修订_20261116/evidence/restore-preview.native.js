(()=>{
if(Project.name!=='youming-v2-soulvalley-danger-candidate')throw Error('Select the revision candidate, not a user project');
Animation.all[0].select();Timeline.pause();TextureAnimator.stop();Timeline.setTime(.7);Animator.preview();TextureAnimator.playAnimationFrame(.70000001);
for(const texture of Texture.all){const material=texture.getMaterial();if(material.uniforms?.SHADE)material.uniforms.SHADE.value=false;const light=material.uniforms?.LIGHTCOLOR?.value;if(light?.setRGB)light.setRGB(1,1,1);}
const preview=Preview.selected;preview.setProjectionMode(false);preview.camPers.fov=68;preview.camPers.updateProjectionMatrix();preview.camera.position.set(0,25.92,42);preview.controls.target.set(0,22,-50);preview.controls.update();Canvas.updateVisibility();
return {project:Project.uuid,viewport:preview.id,time:.7};
})()
