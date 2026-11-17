(()=>{
const state=window.detonate3dReopened,fs=require('fs'),crypto=require('crypto');
if(!state||Project.uuid!==state.uuid)throw new Error('Wrong reopened document');
const puff=Texture.all.filter(texture=>texture.name.startsWith('explosion_puff_'));
if(puff.length!==2||puff.some(texture=>texture.error||texture.width!==32||texture.height!==512))throw new Error('Texture reopen mismatch');
Canvas.updateAll();Canvas.updateVisibility();unselectAllElements();
for(const texture of puff){const material=texture.getMaterial();material.uniforms.SHADE.value=false;material.uniforms.EMISSIVE.value=false;material.transparent=true;material.depthWrite=false;material.depthTest=true;material.needsUpdate=true;}
const preview=Preview.selected;
preview.loadAnglePreset({position:[62,46,106],target:[0,17,0],projection:'orthographic',zoom:.34});preview.controls.update();
const direction=new THREE.Vector3(0,0,-1).applyQuaternion(preview.camera.quaternion);
Cube.all.filter(cube=>cube.name.startsWith('FX_PUFF_')&&cube.visibility).map(cube=>({cube,depth:new THREE.Vector3(...cube.origin).sub(preview.camera.position).dot(direction)})).sort((left,right)=>right.depth-left.depth).forEach((item,index)=>item.cube.mesh.renderOrder=20+index);
preview.screenshot({crop:false,width:1200,height:920},url=>fs.writeFileSync(state.root+'/evidence/reopened_native_viewport.png',Buffer.from(url.split(',')[1],'base64')));
const before=window.detonate3dProtected;
const after=before.map(item=>{const project=ModelProject.all.find(project=>project.uuid===item.uuid);if(!project)throw new Error('Protected tab missing');return window.detonate3dFingerprint(project);});
if(JSON.stringify(before)!==JSON.stringify(after))throw new Error('Protected tab changed');
fs.writeFileSync(state.root+'/evidence/protected-after.json',JSON.stringify(after,null,2));
const report={native_gui_reopen:true,source_uuid:state.original,reopened_uuid:Project.uuid,path:state.path,sha256:crypto.createHash('sha256').update(fs.readFileSync(state.path)).digest('hex'),cubes:Cube.all.length,groups:Group.all.length,textures:Texture.all.length,puffs:puff.map(texture=>({name:texture.name,width:texture.width,height:texture.height,error:texture.error})),protected_count:before.length,protected_unchanged:true,preserved_unsaved:after.filter(item=>!item.saved).map(item=>({uuid:item.uuid,name:item.name})),material_override:'native preview only: unshaded, non-emissive alpha, depth test true, depth write false, sorted back-to-front',at:new Date().toISOString()};
fs.writeFileSync(state.root+'/evidence/reopen-validation.json',JSON.stringify(report,null,2));
return report;
})()
