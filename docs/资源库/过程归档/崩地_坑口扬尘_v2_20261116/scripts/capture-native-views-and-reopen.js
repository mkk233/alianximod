(function(){
const root='D:/alianximod/docs/资源库/过程归档/崩地_坑口扬尘_v2_20261116',fs=require('fs');
if(Project.uuid!=='0d65d0f5-9590-698b-4313-bc901925bd9c')throw Error('Wrong candidate');
const p=Preview.selected,cards=Group.all.filter(g=>g.name.endsWith('_CAMERA_PLANE'));
const evidence=[];
function view(name,pos,target,fov,t){
p.camera.position.fromArray(pos);p.controls.target.fromArray(target);p.camera.fov=fov;p.camera.updateProjectionMatrix();p.controls.update();
Timeline.setTime(t);Animator.preview();for(const g of cards)g.mesh.quaternion.copy(p.camera.quaternion);scene.updateMatrixWorld(true);
p.screenshot({crop:true,width:1100,height:900},data=>fs.writeFileSync(root+'/previews/'+name+'.png',Buffer.from(data.split(',')[1],'base64')));
evidence.push({name,time:t,camera:pos,target,fov,realViewport:p.id,cameraPlaneUpdated:true});
}
view('side-west-dust',[-182,83,0],[0,-8,0],38,0.65);
view('side-west-clear',[-182,83,0],[0,-8,0],38,1.4);
view('top-dust',[0.1,232,0.05],[0,-8,0],33,0.65);
view('top-clear',[0.1,232,0.05],[0,-8,0],33,1.4);
view('overall-final',[95,180,110],[0,-8,0],36,0.65);
fs.writeFileSync(root+'/evidence/native-capture-views.json',JSON.stringify(evidence,null,2));
const model=root+'/source/earth_break_ground_v2_preview.bbmodel',parsed=JSON.parse(fs.readFileSync(model,'utf8'));
const before=ModelProject.all.map(p=>({uuid:p.uuid,name:p.name,saved:p.saved}));
Codecs.project.load(parsed,{path:model});
const out={nativeLoad:'Codecs.project.load(JSON.parse(saved bbmodel), {path})',path:model,newProject:Project.uuid,counts:{elements:Outliner.elements.length,groups:Group.all.length,textures:Texture.all.length,animations:Animation.all.length},preservedAllPreviousProjects:before.every(p=>ModelProject.all.some(q=>p.uuid===q.uuid)),preservedYouming:ModelProject.all.some(p=>p.uuid==='bd0ab5c3-e2ff-a2d3-b619-3ba215a682d2'),previousProjects:before,afterProjectCount:ModelProject.all.length};
fs.writeFileSync(root+'/evidence/native-reopen.json',JSON.stringify(out,null,2));
return JSON.stringify(out);
})()
