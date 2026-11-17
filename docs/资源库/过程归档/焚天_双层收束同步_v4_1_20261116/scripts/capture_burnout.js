(async function () {
  const expected='7fa628fc-3a07-adca-a574-ce45af530859';
  function guard(){if(!Project || Project.uuid!==expected) throw new Error('Project takeover; stop');}
  guard();
  const root='D:/alianximod/docs/资源库/过程归档/焚天_双层收束同步_v4_1_20261116';
  const filesystem=require('fs');
  const timeline=JSON.parse(filesystem.readFileSync(root+'/timeline.json','utf8'));
  const background=Cube.all.find(cube=>cube.name==='BACKGROUND_52');
  const foreground=Cube.all.find(cube=>cube.name==='FOREGROUND_19');
  const texture=background.faces.south.getTexture();
  const foregroundTexture=foreground.faces.south.getTexture();
  Undo.initEdit({elements:[background,foreground],textures:[texture,foregroundTexture],outliner:true});
  texture.fromPath(root+'/textures/background_burnout_9tick_preview.png');
  texture.uv_width=128;texture.uv_height=1296;
  texture.render_mode='default';foregroundTexture.render_mode='default';
  Project.credit='candidate_pending_user_confirmation | authorized burnout repair | 20261116-art-fentian-burnout-sync-06 | preview tail only; runtime reuses original samples';
  Undo.finishEdit('Attach alpha-transition preview to own scene copy',{elements:[background,foreground],textures:[texture,foregroundTexture],outliner:true});
  await new Promise(resolve=>setTimeout(resolve,500));
  guard();
  const captures=[];
  function configure(tick) {
    guard();
    Undo.initEdit({elements:[background,foreground]});
    background.faces.south.uv=[0,tick*144,128,(tick+1)*144];
    const front=timeline.foreground_source_frame_zero_based[tick];
    foreground.faces.south.uv=[0,front*64,48,(front+1)*64];
    Undo.finishEdit('Burnout tick '+tick,{elements:[background,foreground]});
    Canvas.updateAll();
    for(const [index,plane] of [background,foreground].entries()){
      const material=plane.faces.south.getTexture().getMaterial();
      material.uniforms.SHADE.value=false;material.uniforms.EMISSIVE.value=false;material.depthWrite=false;plane.mesh.renderOrder=index+1;
    }
    unselectAllElements();
    Preview.selected.loadAnglePreset({position:[0,16,90],target:[0,16,0],projection:'orthographic',zoom:.55});
    Preview.selected.controls.update();
  }
  for(const tick of [0,3,6,8]){
    configure(tick);
    const path=root+'/previews/gui_burnout_t'+tick+'.png';
    guard();
    await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:900,height:760},data=>{guard();filesystem.writeFileSync(path,Buffer.from(data.split(',')[1],'base64'));resolve();}));
    guard();
    captures.push({tick,path,held_frame_one_based:40,sustain_alpha:timeline.held_sustain_alpha[tick],ember_alpha:timeline.ember_alpha[tick],ember_frame_zero_based:timeline.ember_source_frame_zero_based[tick],foreground_frame_zero_based:timeline.foreground_source_frame_zero_based[tick],project:Project.uuid,source:'native desktop main Preview.selected.screenshot immediate WebGL',document_visibility:document.visibilityState,depthTest:texture.getMaterial().depthTest,depthWrite:texture.getMaterial().depthWrite,SHADE:false,EMISSIVE:false,material_note:'BB emissive branch forces alpha1; disable it and disable shading for fullbright alpha-preserving preview'});
  }
  configure(3);guard();
  const path=root+'/blockbench/fentian_dual_layer_burnout_v4_1.bbmodel';
  Project.save_path=path;
  Codecs.project.write(Codecs.project.compile(),path);
  filesystem.writeFileSync(root+'/evidence/gui-captures.json',JSON.stringify({status:'candidate_pending_user_confirmation',repair_authorized:true,captures,geometry:'unmodified v4 source model; only frame UV and own background preview texture changed',saved_tick:3},null,2)+'\n');
  return {project:Project.uuid,saved:Project.saved,path,captures:captures.map(capture=>capture.path)};
})()
