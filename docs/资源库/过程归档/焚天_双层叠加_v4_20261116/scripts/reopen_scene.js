(async function () {
  const expected='349751a0-a77d-026b-4ead-bc3a553fd738';
  if(!Project || Project.uuid!==expected || !Project.saved) throw new Error('Wrong or unsaved project; stop');
  const root='D:/alianximod/docs/资源库/过程归档/焚天_双层叠加_v4_20261116';
  const filesystem=require('fs');
  const original=Project;
  const path=root+'/blockbench/fentian_dual_layer_v4.bbmodel';
  const bytes=filesystem.readFileSync(path);
  Codecs.project.load(JSON.parse(bytes.toString('utf8')),{path});
  if(Project===original || Project.name!=='fentian_dual_layer_v4') throw new Error('Unexpected reopen; stop');
  const reopened=Project.uuid;
  await new Promise(resolve=>setTimeout(resolve,500));
  if(Project.uuid!==reopened) throw new Error('Project takeover after reopen; stop');
  Canvas.updateAll();
  for(const [index,name] of ['BACKGROUND_52','FOREGROUND_19'].entries()){
    const plane=Cube.all.find(cube=>cube.name===name);
    const material=plane.faces.south.getTexture().getMaterial();
    material.uniforms.SHADE.value=false;material.depthWrite=false;plane.mesh.renderOrder=index+1;
  }
  unselectAllElements();
  Preview.selected.loadAnglePreset({position:[0,16,90],target:[0,16,0],projection:'orthographic',zoom:.55});
  Preview.selected.controls.update();Preview.selected.render();
  const before=JSON.parse(filesystem.readFileSync(root+'/evidence/protected-projects-before.json','utf8')).projects;
  const after=ModelProject.all.filter(project=>project!==Project && project!==original).map(project=>({uuid:project.uuid,name:project.name,saved:project.saved,elements:project.elements.length,textures:project.textures.length}));
  const report={status:'approved_for_recipient_integration',native_reopen:'Codecs.project.load',original_project:original.uuid,reopened_project:reopened,model:path,model_sha256:require('crypto').createHash('sha256').update(bytes).digest('hex').toUpperCase(),elements:Outliner.elements.length,groups:Group.all.length,textures:Texture.all.map(texture=>({name:texture.name,width:texture.width,height:texture.height,loaded:texture.img.complete,error:texture.error})),protected_projects_after:after,protected_observable_summaries_equal:JSON.stringify(before)===JSON.stringify(after),documents_closed:0,additional_screenshots:0};
  filesystem.writeFileSync(root+'/evidence/reopen.json',JSON.stringify(report,null,2)+'\n');
  return report;
})()
