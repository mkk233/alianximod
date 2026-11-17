(async function () {
 const root='D:/alianximod/docs/资源库/过程归档/幽冥蓝火重配色-20261116';
 if(!Project||Project.uuid!=='803970b1-7113-95b6-9e54-ee1415c7edff')throw new Error('External active project; stop');
 const filesystem=require('fs'),evidence=[];
 const own=Cube.all.filter(c=>c.name.startsWith('FIRE_'));
 Undo.initEdit({elements:Cube.all,outliner:true});
 const states=[
  {name:'bb_v3_overall_F06',mode:'V3',frame:6,position:[24,48,170],target:[0,15,0],zoom:.21,follow:true},
  {name:'bb_v3_front_F11',mode:'V3',frame:11,position:[0,16,180],target:[0,16,0],zoom:.21,follow:true},
  {name:'bb_v3_side_frozen_plane',mode:'V3',frame:6,position:[100,28,50],target:[18,14.4,1],zoom:.54,follow:false,focusNew:true},
  {name:'bb_kindle_v4_overall_F06',mode:'KINDLEV4',frame:6,position:[24,48,170],target:[0,15,0],zoom:.21,follow:true},
  {name:'bb_kindle_v4_side_frozen_plane',mode:'KINDLEV4',frame:6,position:[100,28,50],target:[18,14.4,1],zoom:.54,follow:false,focusNew:true},
  {name:'bb_kindle_v4_gather_F02',mode:'KINDLEV4',frame:2,position:[0,16,180],target:[0,16,0],zoom:.21,follow:true},
  {name:'bb_kindle_v4_embers_F46',mode:'KINDLEV4',frame:46,position:[0,16,180],target:[0,16,0],zoom:.21,follow:true},
  {name:'bb_runtime_kindle_overall_F06',mode:'RUNTIME',frame:6,position:[18,42,170],target:[0,15,0],zoom:.21,follow:true},
  {name:'bb_fire_seed_attached_overall_F06',mode:'SEED',frame:6,position:[18,42,170],target:[0,17,0],zoom:.21,follow:true},
  {name:'bb_actual_target_flame_overall_F06',mode:'ATTACHED',frame:6,position:[18,42,170],target:[0,15,0],zoom:.21,follow:true},
  {name:'bb_v3_extinguish_F16',mode:'V3',frame:16,position:[0,16,180],target:[0,16,0],zoom:.21,follow:true},
  {name:'bb_v3_off_F19',mode:'V3',frame:19,position:[0,16,180],target:[0,16,0],zoom:.21,follow:true}
 ];
 for(const state of states){
  if(!Project||Project.uuid!=='803970b1-7113-95b6-9e54-ee1415c7edff')throw new Error('External project switch during capture; stop');
  Preview.selected.loadAnglePreset({position:state.position,target:state.target,projection:'orthographic',zoom:state.zoom});Preview.selected.controls.update();
  const q=Preview.selected.camera.quaternion,normal=new THREE.Vector3(0,0,1).applyQuaternion(q);
  const distance=state.follow?Math.abs(normal.x)*4.8+Math.abs(normal.y)*14.4+Math.abs(normal.z)*4.8+.64:5.44;
  for(const c of own){
   const mode=c.name.split('_')[1];c.visibility=mode===state.mode;
   if(state.focusNew&&!c.name.startsWith('FIRE_'+state.mode+'_2'))c.visibility=false;
   if(!c.visibility)continue;
   const frameHeight=mode==='SEED'?32:(mode==='ATTACHED'?40:(mode==='KINDLEV4'?144:64)),t=c.faces.south.getTexture();
   c.faces.south.uv=[0,(state.frame-1)*frameHeight,t.uv_width,state.frame*frameHeight];
   if(mode!=='SEED'){
    const rot=state.follow?new THREE.Euler().setFromQuaternion(q,c.parent.mesh.rotation.order):new THREE.Euler(0,0,0);
    c.parent.rotation=[rot.x,rot.y,rot.z].map(Math.radToDeg);c.from[2]=distance;c.to[2]=distance;
   }
  }
  for(const g of Group.all.filter(g=>g.name.startsWith('AUX_entity_'))){
   for(const c of g.children)c.visibility=!state.focusNew||g.name.includes('NEW_SOUL');
  }
  for(const c of Cube.all.filter(c=>c.name.startsWith('NETHER_NATIVE')||c.name.startsWith('MC_SOUL')))c.visibility=!state.focusNew;
  Canvas.updateAll();unselectAllElements();
  for(const t of Texture.all){const m=t.getMaterial();if(m.uniforms&&m.uniforms.SHADE)m.uniforms.SHADE.value=false;}
  const path=root+'/previews/'+state.name+'.png';
  await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:1280,height:640},data=>{filesystem.writeFileSync(path,Buffer.from(data.split(',')[1],'base64'));resolve();}));
  evidence.push({...state,path,project:Project.uuid,source:'real connected Blockbench Desktop main Preview.screenshot',visibility:document.visibilityState,planeOffset:distance});
 }
 for(const c of own){const mode=c.name.split('_')[1];c.visibility=mode==='V3';if(c.visibility){c.parent.rotation=[0,0,0];c.from[2]=5.44;c.to[2]=5.44;c.faces.south.uv=[0,320,c.faces.south.getTexture().uv_width,384];}}
 for(const g of Group.all.filter(g=>g.name.startsWith('AUX_entity_')))for(const c of g.children)c.visibility=true;
 for(const c of Cube.all.filter(c=>c.name.startsWith('NETHER_NATIVE')||c.name.startsWith('MC_SOUL')))c.visibility=true;
 Canvas.updateAll();unselectAllElements();Preview.selected.loadAnglePreset({position:[0,16,180],target:[0,16,0],projection:'orthographic',zoom:.21});Preview.selected.controls.update();Preview.selected.render();
 Undo.finishEdit('Capture same-frame palette comparison and key phases',{elements:Cube.all,outliner:true});
 const path=root+'/blockbench/nether_blue_palette_art_v1.bbmodel';Project.save_path=path;Codecs.project.write(Codecs.project.compile(),path);
 filesystem.writeFileSync(root+'/evidence/blockbench-captures.json',JSON.stringify({status:'candidate_pending_user_confirmation',attempt:'art-v1-bb-finish-20261116',project:Project.uuid,captures:evidence},null,2));
 return {model:path,project:Project.uuid,captures:evidence.map(e=>e.path),saved:Project.saved};
})()
