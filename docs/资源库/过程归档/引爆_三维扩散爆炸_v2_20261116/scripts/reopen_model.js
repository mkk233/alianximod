(()=>{
const state=window.detonate3d,fs=require('fs');
state.guard();
if(!Project.saved)throw new Error('Own model must be saved');
const path=state.root+'/blockbench/fire_detonate_v2_3d_candidate.bbmodel';
const original=Project.uuid;
Codecs.project.load(JSON.parse(fs.readFileSync(path,'utf8')),{path});
if(Project.uuid===original||Project.name!=='fire_detonate_v2_3d_candidate')throw new Error('Native reopen did not create expected document');
window.detonate3dReopened={original,uuid:Project.uuid,path,root:state.root};
return window.detonate3dReopened;
})()
