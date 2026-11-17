(function(viewName){
const root='D:/alianximod/docs/资源库/过程归档/崩地_飞扬土尘_v2r2_20261116';
if(Project.uuid!=='937be075-0517-53f5-6aa9-c7f18ce35ab8')throw Error('Wrong target project');
const fs=require('fs'),preview=Preview.selected,animation=Animation.all[0];
const cameras={overall:{position:[145,109,165],target:[0,-14,0],fov:42},side:{position:[0,24,225],target:[0,-10,0],fov:38}};
const camera=cameras[viewName];
if(!camera)throw Error('Unknown view');
preview.camera.position.fromArray(camera.position);preview.controls.target.fromArray(camera.target);
preview.camera.fov=camera.fov;preview.camera.updateProjectionMatrix();preview.controls.update();
const cards=Group.all.filter(group=>group.name.endsWith('_CAMERA_PLANE'));
const euler=new THREE.Euler().setFromQuaternion(preview.camera.quaternion,'ZYX');
if(viewName==='overall'){
Undo.initEdit({outliner:true});
for(const group of cards)group.rotation=[euler.x*180/Math.PI,euler.y*180/Math.PI,euler.z*180/Math.PI];
Canvas.updateAll();Undo.finishEdit('Set saved candidate camera-facing reference orientation');
}
animation.select();
function pose(time){
Timeline.setTime(time);Animator.preview();
for(const group of cards)group.mesh.quaternion.copy(preview.camera.quaternion);
scene.updateMatrixWorld(true);
}
function shot(path){
let written=false;
preview.screenshot({crop:false,width:1280,height:668},data=>{fs.writeFileSync(root+'/previews/'+path,Buffer.from(data.split(',')[1],'base64'));written=true;});
if(!written)throw Error('Native screenshot callback was not synchronous');
}
const samples=[];
for(let index=0;index<46;index++){
const time=index*.05;pose(time);
shot(viewName+'-frames/'+viewName+'_'+String(index).padStart(3,'0')+'.png');
samples.push({index,time:+time.toFixed(3),visibleDustFrames:Group.all.filter(group=>/_FRAME_/.test(group.name)&&group.mesh.scale.x>.5).length,visibleGrains:Group.all.filter(group=>/_ALPHA_/.test(group.name)&&group.mesh.scale.x>.5).length});
}
for(const [name,time] of [['before',.2],['launch',.5],['apex',.85],['fall',1.2],['thin-tail',1.5],['clear',1.85]]){pose(time);shot(viewName+'-'+name+'.png');}
const trajectory=[];
for(const time of [.4,.55,.70,.85,1.0,1.15,1.3,1.45,1.6,1.8]){
pose(time);
trajectory.push({time,grains:Group.all.filter(group=>group.name.endsWith('_BALLISTIC_visual_only')).map(group=>({name:group.name,positionModel:group.mesh.getWorldPosition(new THREE.Vector3()).toArray()})),dustRoots:Group.all.filter(group=>group.name.endsWith('_WORLD_motion')).map(group=>({name:group.name,positionModel:group.mesh.getWorldPosition(new THREE.Vector3()).toArray()}))});
}
pose(.8);
if(viewName==='overall')Codecs.project.write(Codecs.project.compile({compressed:false}),root+'/source/earth_ground_airborne_v2r2_preview.bbmodel');
const evidence={method:'Actual desktop Blockbench active Preview.screenshot; native Timeline.setTime / Animator.preview; no offline rendering, no compositing; fixed viewport crop:false',project:Project.uuid,view:preview.id,viewName,camera,nativeViewport:[preview.width,preview.height],outputSize:[1280,668],samples,trajectory,textureErrors:Texture.all.map(texture=>({name:texture.name,error:texture.error})),saved:Project.saved};
fs.writeFileSync(root+'/evidence/native-capture-'+viewName+'.json',JSON.stringify(evidence,null,2));
return JSON.stringify({viewName,frames:samples.length,visibleDust:samples.map(sample=>sample.visibleDustFrames),visibleGrains:samples.map(sample=>sample.visibleGrains),saved:Project.saved});
})
