(async function () {
  const root='D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116';
  if(!Project || Project.uuid!=='d8549d69-5160-499c-12b6-87d88f8bc273') throw new Error('Wrong project; no mutation');
  const filesystem=require('fs');
  const kindle=Cube.all.find(element=>element.name.startsWith('KINDLE_'));
  const fentian=Cube.all.find(element=>element.name.startsWith('FENTIAN_'));
  const kindleTexture=kindle.faces.south.getTexture();
  const fentianTexture=fentian.faces.south.getTexture();
  const captures=[];
  const states=[
    {name:'gui_front_approved_recipients',kindleFrame:4,fentianFrame:11,position:[0,16,110],target:[0,16,0],follow:true},
    {name:'gui_oblique_approved_recipients',kindleFrame:4,fentianFrame:6,position:[58,35,110],target:[0,15,0],follow:true},
    {name:'gui_frozen_side_distance',kindleFrame:4,fentianFrame:11,position:[115,28,22],target:[0,15,0],follow:false},
    {name:'gui_front_ending_stages',kindleFrame:46,fentianFrame:16,position:[0,16,110],target:[0,16,0],follow:true}
  ];
  for(const state of states) {
    Preview.selected.loadAnglePreset({position:state.position,target:state.target,projection:'orthographic',zoom:.35});
    Preview.selected.controls.update();
    const normal=new THREE.Vector3(0,0,1).applyQuaternion(Preview.selected.camera.quaternion);
    const distance=state.follow?Math.abs(normal.x)*4.8+Math.abs(normal.y)*14.4+Math.abs(normal.z)*4.8+.64:5.44;
    Undo.initEdit({elements:[kindle,fentian],outliner:true});
    const rotation=state.follow?new THREE.Euler().setFromQuaternion(Preview.selected.camera.quaternion,fentian.parent.mesh.rotation.order):new THREE.Euler(0,0,0);
    kindle.parent.rotation=[rotation.x,rotation.y,rotation.z].map(Math.radToDeg);
    fentian.parent.rotation=kindle.parent.rotation.slice();
    fentian.from[2]=12+distance;fentian.to[2]=12+distance;
    kindle.faces.south.uv=[0,(state.kindleFrame-1)*144,128,state.kindleFrame*144];
    fentian.faces.south.uv=[0,(state.fentianFrame-1)*64,48,state.fentianFrame*64];
    Undo.finishEdit('Approved comparison '+state.name,{elements:[kindle,fentian],outliner:true});
    Canvas.updateAll();
    kindleTexture.getMaterial().uniforms.SHADE.value=false;
    fentianTexture.getMaterial().uniforms.SHADE.value=false;
    unselectAllElements();
    const path=root+'/previews/'+state.name+'.png';
    await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:1100,height:700},data=>{filesystem.writeFileSync(path,Buffer.from(data.split(',')[1],'base64'));resolve()}));
    captures.push({...state,path,project:Project.uuid,status:'approved_for_recipient_integration',source:'connected native desktop main Preview.selected.screenshot immediate WebGL render',document_visibility:document.visibilityState,kindle_center_units:[-23,12.96,-12],kindle_normal_offset_units:0,fentian_target_center_units:[23,14.4,12],fentian_support_offset_units:distance,rotation_deg:fentian.parent.rotation.slice(),kindle_depth_test:kindleTexture.getMaterial().depthTest,fentian_depth_test:fentianTexture.getMaterial().depthTest});
  }
  Undo.initEdit({elements:[kindle,fentian],outliner:true});
  kindle.parent.rotation=[0,0,0];fentian.parent.rotation=[0,0,0];
  fentian.from[2]=17.44;fentian.to[2]=17.44;
  kindle.faces.south.uv=[0,432,128,576];fentian.faces.south.uv=[0,640,48,704];
  Undo.finishEdit('Save approved front comparison',{elements:[kindle,fentian],outliner:true});
  Canvas.updateAll();
  kindleTexture.getMaterial().uniforms.SHADE.value=false;
  fentianTexture.getMaterial().uniforms.SHADE.value=false;
  Preview.selected.loadAnglePreset({position:[0,16,110],target:[0,16,0],projection:'orthographic',zoom:.35});
  Preview.selected.controls.update();Preview.selected.render();
  const model=root+'/blockbench/kindle_fentian_approved_swap.bbmodel';
  Project.save_path=model;
  Codecs.project.write(Codecs.project.compile(),model);
  filesystem.writeFileSync(root+'/evidence/gui-captures.json',JSON.stringify({status:'approved_for_recipient_integration',blockbench:'5.2.2',plugin:'1.10.1',captures},null,2)+'\n');
  return {model,project:Project.uuid,saved:Project.saved,captures:captures.map(capture=>capture.path)};
})()
