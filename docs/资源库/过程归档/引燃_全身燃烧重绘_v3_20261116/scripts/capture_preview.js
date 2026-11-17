(async function () {
  const root='D:/alianximod/docs/资源库/过程归档/引燃_全身燃烧重绘_v3_20261116';
  if(!Project || Project.uuid!=='c136d4c5-fe9b-4cf8-f91c-3df017448ec0') throw new Error('Wrong active project; no mutation');
  const filesystem=require('fs');
  const plane=Cube.all.find(element=>element.name.startsWith('KINDLE_plane'));
  const group=plane.parent;
  const texture=plane.faces.south.getTexture();
  const evidence=[];
  const states=[
    {name:'gui_oblique_sustain',frame:6,position:[45,26,65],target:[0,14.4,0],follow:true},
    {name:'gui_front_pulse',frame:11,position:[0,14.4,65],target:[0,14.4,0],follow:true},
    {name:'gui_side_distance_frozen_plane',frame:11,position:[70,24,16],target:[0,14.4,1.6],follow:false},
    {name:'gui_front_extinguish_F16',frame:16,position:[0,14.4,65],target:[0,14.4,0],follow:true},
    {name:'gui_front_off_F19',frame:19,position:[0,14.4,65],target:[0,14.4,0],follow:true}
  ];
  for(const state of states) {
    Preview.selected.loadAnglePreset({position:state.position,target:state.target,projection:'orthographic',zoom:.55});
    Preview.selected.controls.update();
    const normal=new THREE.Vector3(0,0,1).applyQuaternion(Preview.selected.camera.quaternion);
    const distance=state.follow ? Math.abs(normal.x)*4.8+Math.abs(normal.y)*14.4+Math.abs(normal.z)*4.8+.64 : 5.44;
    Undo.initEdit({elements:[plane],outliner:true});
    const rotation=state.follow ? new THREE.Euler().setFromQuaternion(Preview.selected.camera.quaternion,group.mesh.rotation.order) : new THREE.Euler(0,0,0);
    group.rotation=[rotation.x,rotation.y,rotation.z].map(Math.radToDeg);
    group.name='CANDIDATE_CAMERA_PLANE_F'+state.frame;
    plane.from[2]=distance;plane.to[2]=distance;
    plane.faces.south.uv=[0,(state.frame-1)*64,48,state.frame*64];
    plane.name='KINDLE_plane_1.35x1.80m_F'+state.frame;
    Undo.finishEdit('Candidate phase '+state.name,{elements:[plane],outliner:true});
    Canvas.updateAll();
    texture.getMaterial().uniforms.SHADE.value=false;
    unselectAllElements();
    const output=root+'/previews/'+state.name+'.png';
    await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:900,height:700},data=>{filesystem.writeFileSync(output,Buffer.from(data.split(',')[1],'base64'));resolve()}));
    evidence.push({...state,path:output,project:Project.uuid,source:'connected Blockbench Desktop main Preview.screenshot immediate WebGL render',document_visibility:document.visibilityState,plane_rotation_deg:group.rotation.slice(),plane_offset_units:distance,texture_path:texture.path,uv:plane.faces.south.uv.slice(),unlit_texture_preview:true,status:'candidate_pending_user_confirmation'});
  }
  Undo.initEdit({elements:[plane],outliner:true});
  group.rotation=[0,0,0];group.name='CANDIDATE_CAMERA_PLANE_F11';
  plane.from[2]=5.44;plane.to[2]=5.44;plane.faces.south.uv=[0,640,48,704];plane.name='KINDLE_plane_1.35x1.80m_F11';
  Undo.finishEdit('Save v3 default pulse pose',{elements:[plane],outliner:true});
  Canvas.updateAll();
  texture.getMaterial().uniforms.SHADE.value=false;
  Preview.selected.loadAnglePreset({position:[0,14.4,65],target:[0,14.4,0],projection:'orthographic',zoom:.55});
  Preview.selected.controls.update();Preview.selected.render();
  const model=root+'/blockbench/fire_kindle_v3_redraw_candidate_preview.bbmodel';
  Project.save_path=model;
  Codecs.project.write(Codecs.project.compile(),model);
  filesystem.writeFileSync(root+'/evidence/gui-captures.json',JSON.stringify({status:'candidate_pending_user_confirmation',blockbench:'5.2.2',plugin:'1.10.1',captures:evidence},null,2)+'\n');
  return {model,captures:evidence.map(capture=>capture.path),project:Project.uuid,saved:Project.saved};
})()
