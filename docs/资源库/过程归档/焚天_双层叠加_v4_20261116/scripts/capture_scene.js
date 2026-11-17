(async function () {
  const expected='349751a0-a77d-026b-4ead-bc3a553fd738';
  function guard(){if(!Project || Project.uuid!==expected) throw new Error('Active project changed; stop');}
  guard();
  const root='D:/alianximod/docs/资源库/过程归档/焚天_双层叠加_v4_20261116';
  const filesystem=require('fs');
  const background=Cube.all.find(cube=>cube.name==='BACKGROUND_52');
  const foreground=Cube.all.find(cube=>cube.name==='FOREGROUND_19');
  const captures=[];
  const states=[
    {name:'gui_front',backgroundFrame:5,foregroundFrame:11,position:[0,16,90],target:[0,16,0],follow:true,phase:'sustain, real segment pulse sample'},
    {name:'gui_oblique',backgroundFrame:8,foregroundFrame:8,position:[42,30,86],target:[0,15,0],follow:true,phase:'sustain between segment pulses'},
    {name:'gui_frozen_side_distance',backgroundFrame:5,foregroundFrame:11,position:[84,26,18],target:[0,15,0],follow:false,phase:'front planes frozen for spacing only'},
    {name:'gui_ending',backgroundFrame:46,foregroundFrame:16,position:[0,16,90],target:[0,16,0],follow:true,phase:'natural end tick offset 5: background45; foreground15'}
  ];
  function configure(state){
    guard();
    Preview.selected.loadAnglePreset({position:state.position,target:state.target,projection:'orthographic',zoom:.55});
    Preview.selected.controls.update();
    const normal=new THREE.Vector3(0,0,1).applyQuaternion(Preview.selected.camera.quaternion);
    const distance=state.follow?Math.abs(normal.x)*4.8+Math.abs(normal.y)*14.4+Math.abs(normal.z)*4.8+.64:5.44;
    Undo.initEdit({elements:[background,foreground],outliner:true});
    const rotation=state.follow?new THREE.Euler().setFromQuaternion(Preview.selected.camera.quaternion,foreground.parent.mesh.rotation.order):new THREE.Euler(0,0,0);
    background.parent.rotation=[rotation.x,rotation.y,rotation.z].map(Math.radToDeg);
    foreground.parent.rotation=background.parent.rotation.slice();
    foreground.from[2]=distance;foreground.to[2]=distance;
    background.faces.south.uv=[0,(state.backgroundFrame-1)*144,128,state.backgroundFrame*144];
    foreground.faces.south.uv=[0,(state.foregroundFrame-1)*64,48,state.foregroundFrame*64];
    Undo.finishEdit('Dual layer '+state.name,{elements:[background,foreground],outliner:true});
    Canvas.updateAll();
    for(const [index,plane] of [background,foreground].entries()){
      const material=plane.faces.south.getTexture().getMaterial();
      material.uniforms.SHADE.value=false;
      material.depthWrite=false;
      plane.mesh.renderOrder=index+1;
    }
    unselectAllElements();
    return distance;
  }
  for(const state of states){
    const distance=configure(state);
    const path=root+'/previews/'+state.name+'.png';
    guard();
    await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:900,height:760},data=>{guard();filesystem.writeFileSync(path,Buffer.from(data.split(',')[1],'base64'));resolve();}));
    guard();
    captures.push({...state,path,project:Project.uuid,foreground_offset_units:distance,rotation_deg:foreground.parent.rotation.slice(),document_visibility:document.visibilityState,source:'native desktop main Preview.selected.screenshot immediate WebGL',depthTest:background.faces.south.getTexture().getMaterial().depthTest,depthWrite:false,drawOrder:['background','foreground']});
  }
  configure(states[0]);guard();
  const model=root+'/blockbench/fentian_dual_layer_v4.bbmodel';
  Project.save_path=model;
  Codecs.project.write(Codecs.project.compile(),model);
  filesystem.writeFileSync(root+'/evidence/gui-captures.json',JSON.stringify({status:'approved_for_recipient_integration',blockbench:'5.2.2',plugin:'1.10.1',captures,preview_only_material_settings:'per-plane fullbright, depthWrite=false, depthTest retained, renderOrder background then foreground; not new runtime parameters'},null,2)+'\n');
  return {model,saved:Project.saved,uuid:Project.uuid,captures:captures.map(capture=>capture.path)};
})()
