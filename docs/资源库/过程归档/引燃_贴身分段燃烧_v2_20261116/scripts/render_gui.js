(async function () {
  const root='D:/alianximod/docs/资源库/过程归档/引燃_贴身分段燃烧_v2_20261116';
  if(!Project || !Project.name.startsWith('fire_kindle_v2')) throw new Error('Wrong active project');
  const fs=require('fs');
  const plane=Cube.all.find(c=>c.name.startsWith('KINDLE_plane'));
  const fx=plane.parent;
  const texture=plane.faces.south.getTexture();
  const evidence=[];
  const states=[
    {name:'gui_oblique_sustain',frame:6,position:[45,26,65],target:[0,14.4,0],follow:true},
    {name:'gui_front_pulse',frame:11,position:[0,14.4,65],target:[0,14.4,0],follow:true},
    {name:'gui_side_distance_frozen_plane',frame:11,position:[70,24,16],target:[0,14.4,1.6],follow:false},
    {name:'gui_front_extinguish_F16',frame:16,position:[0,14.4,65],target:[0,14.4,0],follow:true},
    {name:'gui_front_off_F19',frame:19,position:[0,14.4,65],target:[0,14.4,0],follow:true}
  ];
  for(const s of states) {
    Preview.selected.loadAnglePreset({position:s.position,target:s.target,projection:'orthographic',zoom:.55});
    Preview.selected.controls.update();
    const normal=new THREE.Vector3(0,0,1).applyQuaternion(Preview.selected.camera.quaternion);
    const distance=s.follow ? Math.abs(normal.x)*4.8+Math.abs(normal.y)*14.4+Math.abs(normal.z)*4.8+.64 : 5.44;
    Undo.initEdit({elements:[plane],outliner:true});
    const rot=s.follow?new THREE.Euler().setFromQuaternion(Preview.selected.camera.quaternion,fx.mesh.rotation.order):new THREE.Euler(0,0,0);
    fx.rotation=[rot.x,rot.y,rot.z].map(Math.radToDeg);
    fx.name='CANDIDATE_CAMERA_PLANE_F'+s.frame;
    plane.from[2]=distance;plane.to[2]=distance;
    plane.faces.south.uv=[0,(s.frame-1)*64,48,s.frame*64];
    plane.name='KINDLE_plane_1.35x1.80m_F'+s.frame;
    Undo.finishEdit('Set candidate display '+s.name,{elements:[plane],outliner:true});
    Canvas.updateAll();
    texture.getMaterial().uniforms.SHADE.value=false;
    unselectAllElements();
    const output=root+'/previews/'+s.name+'.png';
    await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:900,height:700},data=>{fs.writeFileSync(output,Buffer.from(data.split(',')[1],'base64'));resolve()}));
    evidence.push({...s,path:output,project:Project.uuid,viewport:'main',source:'connected Blockbench Desktop Preview.screenshot, native WebGL render on demand',document_visibility:document.visibilityState,plane_rotation_deg:fx.rotation.slice(),plane_offset_units:distance,texture_path:texture.path,uv:plane.faces.south.uv.slice(),unlit_texture_preview:true});
  }
  Undo.initEdit({elements:[plane],outliner:true});
  fx.rotation=[0,0,0];fx.name='CANDIDATE_CAMERA_PLANE_F11_pulse';
  plane.from[2]=5.44;plane.to[2]=5.44;plane.faces.south.uv=[0,640,48,704];plane.name='KINDLE_plane_1.35x1.80m_F11';
  Undo.finishEdit('Save candidate default front pulse',{elements:[plane],outliner:true});
  Canvas.updateAll();texture.getMaterial().uniforms.SHADE.value=false;
  Preview.selected.loadAnglePreset({position:[0,14.4,65],target:[0,14.4,0],projection:'orthographic',zoom:.55});
  Preview.selected.controls.update();Preview.selected.render();
  fs.writeFileSync(root+'/evidence/gui-captures.json',JSON.stringify({status:'candidate_pending_user_confirmation',blockbench:'5.2.2',plugin:'1.10.1',captures:evidence},null,2)+'\n');
  const model=root+'/blockbench/fire_kindle_v2_candidate_preview.bbmodel';
  Project.save_path=model;
  Codecs.project.write(Codecs.project.compile(),model);
  return {model,captures:evidence.map(e=>e.path),project:Project.uuid,saved:Project.saved};
})()
