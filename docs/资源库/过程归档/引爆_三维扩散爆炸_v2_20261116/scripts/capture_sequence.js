(async()=>{
const state=window.detonate3d,fs=require('fs');
state.guard();
const mode=window.detonate3dCaptureMode||'main';
const variant=mode==='soul'?'soul':'orange';
const preview=Preview.selected;
const settings=mode==='strength'?{position:[12,60,280],target:[0,20,0],projection:'orthographic',zoom:.135}:mode==='side'?{position:[98,81,-23],target:[0,15,0],projection:'orthographic',zoom:.34}:{position:[62,46,106],target:[0,17,0],projection:'orthographic',zoom:.34};
const width=mode==='strength'?1600:1200,height=mode==='strength'?780:920;
state.mode(mode);
if(mode==='side')for(const item of state.main.children)if(item.name.startsWith('AUX_LABEL_'))state.visible(item,false);
preview.loadAnglePreset(settings);preview.controls.update();
fs.mkdirSync(state.root+'/previews/'+mode,{recursive:true});
const samples=[];
Undo.initEdit({elements:Outliner.elements,groups:Group.all,outliner:true});
for(let frame=1;frame<=20;frame++){
 state.guard();const poses=state.pose(frame,variant);
 const file=state.root+'/previews/'+mode+'/frame_'+String(frame).padStart(2,'0')+'.png';
 await new Promise(resolve=>preview.screenshot({crop:false,width,height},url=>{fs.writeFileSync(file,Buffer.from(url.split(',')[1],'base64'));resolve();}));
 samples.push({file,frame,at_ms:(frame-1)*50,camera_position:preview.camera.position.toArray(),camera_target:preview.controls.target.toArray(),camera_zoom:preview.camera.zoom,source:'actual Desktop Blockbench Preview.selected.screenshot',project_uuid:Project.uuid,variant,particles:poses});
 if(frame===7&&mode==='main')fs.copyFileSync(file,state.root+'/previews/gui_peak_threequarter.png');
 if(frame===10&&mode==='main')fs.copyFileSync(file,state.root+'/previews/gui_breakup_threequarter.png');
 if(frame===7&&mode==='side')fs.copyFileSync(file,state.root+'/previews/gui_peak_side_top.png');
 if(frame===7&&mode==='strength')fs.copyFileSync(file,state.root+'/previews/gui_strength_comparison.png');
 if(frame===7&&mode==='soul')fs.copyFileSync(file,state.root+'/previews/gui_peak_soul.png');
}
state.mode('main');preview.loadAnglePreset({position:[62,46,106],target:[0,17,0],projection:'orthographic',zoom:.34});preview.controls.update();state.pose(7);
Undo.finishEdit('Capture '+mode+' complete native 3D explosion sequence',{elements:Outliner.elements,groups:Group.all,outliner:true});
Project.save_path=state.root+'/blockbench/fire_detonate_v2_3d_candidate.bbmodel';Codecs.project.write(Codecs.project.compile({editor_state:true}),Project.save_path);
fs.writeFileSync(state.root+'/evidence/captures_'+mode+'.json',JSON.stringify({duration_ms:1000,frame_ms:50,preview_hold_ms:650,mode,samples},null,2));
return {mode,captured:samples.length,saved:Project.saved,first:samples[0].file,last:samples[19].file};
})()
