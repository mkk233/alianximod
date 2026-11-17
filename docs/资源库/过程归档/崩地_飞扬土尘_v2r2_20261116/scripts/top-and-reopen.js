(function(){
const root='D:/alianximod/docs/资源库/过程归档/崩地_飞扬土尘_v2r2_20261116';
if(Project.uuid!=='937be075-0517-53f5-6aa9-c7f18ce35ab8')throw Error('Wrong project');
const fs=require('fs'),preview=Preview.selected;
preview.camera.position.set(0,235,.1);preview.controls.target.set(0,0,0);preview.camera.fov=40;preview.camera.updateProjectionMatrix();preview.controls.update();
for(const [name,time] of [['top-before',.2],['top-apex',.85],['top-clear',1.85]]){
Timeline.setTime(time);Animator.preview();
for(const group of Group.all.filter(group=>group.name.endsWith('_CAMERA_PLANE')))group.mesh.quaternion.copy(preview.camera.quaternion);
scene.updateMatrixWorld(true);
preview.screenshot({crop:false,width:1280,height:668},data=>fs.writeFileSync(root+'/previews/'+name+'.png',Buffer.from(data.split(',')[1],'base64')));
}
const path=root+'/source/earth_ground_airborne_v2r2_preview.bbmodel';
const previous=Project.uuid;
const existing=ModelProject.all.map(project=>({name:project.name,uuid:project.uuid,saved:project.saved}));
Codecs.project.load(JSON.parse(fs.readFileSync(path,'utf8')),{path});
const evidence={method:'Native Codecs.project.write then native Codecs.project.load of the saved file; existing tabs retained',previous,reopened:Project.uuid,name:Project.name,path,elements:Outliner.elements.length,groups:Group.all.length,textures:Texture.all.length,animations:Animation.all.length,previousProjects:existing};
fs.writeFileSync(root+'/evidence/native-reopen.json',JSON.stringify(evidence,null,2));
return JSON.stringify(evidence);
})()
