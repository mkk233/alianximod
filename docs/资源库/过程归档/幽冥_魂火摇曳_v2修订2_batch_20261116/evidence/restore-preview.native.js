(()=>{
const destination='D:/alianximod/docs/资源库/过程归档/幽冥_魂火摇曳_v2修订2_batch_20261116';
if(Project.name!=='youming-v2-fixed-root-sway-candidate')throw Error('Open the sway04 candidate first; do not affect other projects');
window.gloomSway=JSON.parse(require('fs').readFileSync(destination+'/evidence/native-scene.json','utf8'));
window.gloomSway.project=Project.uuid;
Animation.all[0].select();Timeline.pause();TextureAnimator.stop();
Timeline.setTime(0.7);Animator.preview();TextureAnimator.playAnimationFrame(0.70000001);
for(const texture of Texture.all){
const material=texture.getMaterial();
if(material.uniforms?.SHADE)material.uniforms.SHADE.value=false;
if(material.uniforms?.LIGHTCOLOR?.value?.setRGB)material.uniforms.LIGHTCOLOR.value.setRGB(1,1,1);
}
const preview=Preview.selected;
preview.setProjectionMode(false);preview.camPers.fov=68;preview.camPers.updateProjectionMatrix();
preview.camera.position.set(0,25.92,42);preview.controls.target.set(0,22,-50);preview.controls.update();
Canvas.updateVisibility();
eval(require('fs').readFileSync(destination+'/evidence/capture-sway.native.js','utf8'));
return {project:Project.uuid,viewport:preview.id,restoreOnly:true,captureFunction:'captureGloomSway(inside|close|side)'};
})()
