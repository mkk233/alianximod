(async function () {
  const root='D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116';
  if(!Project || Project.uuid!=='d8549d69-5160-499c-12b6-87d88f8bc273' || !Project.saved) throw new Error('Unexpected or unsaved project');
  const filesystem=require('fs');
  const original=Project;
  const path=root+'/blockbench/kindle_fentian_approved_swap.bbmodel';
  const bytes=filesystem.readFileSync(path);
  Codecs.project.load(JSON.parse(bytes.toString('utf8')),{path});
  await new Promise(resolve=>setTimeout(resolve,500));
  if(Project===original || Project.name!=='kindle_fentian_approved_swap') throw new Error('Unexpected reopen');
  Canvas.updateAll();
  for(const element of Cube.all.filter(element=>element.name.startsWith('KINDLE_') || element.name.startsWith('FENTIAN_'))) element.faces.south.getTexture().getMaterial().uniforms.SHADE.value=false;
  unselectAllElements();
  Preview.selected.loadAnglePreset({position:[0,16,110],target:[0,16,0],projection:'orthographic',zoom:.35});
  Preview.selected.controls.update();
  const screenshot=root+'/previews/gui_reopened_front.png';
  await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:1100,height:700},data=>{filesystem.writeFileSync(screenshot,Buffer.from(data.split(',')[1],'base64'));resolve()}));
  const report={
    status:'approved_for_recipient_integration',
    native_reopen:'Codecs.project.load',
    original_project:original.uuid,
    reopened_project:Project.uuid,
    model:path,
    model_sha256:require('crypto').createHash('sha256').update(bytes).digest('hex').toUpperCase(),
    elements:Outliner.elements.length,
    groups:Group.all.length,
    textures:Texture.all.map(texture=>({name:texture.name,width:texture.width,height:texture.height,loaded:texture.img.complete,error:texture.error})),
    screenshot,
    protected_projects_after:ModelProject.all.filter(project=>project!==Project && project!==original).map(project=>({uuid:project.uuid,name:project.name,saved:project.saved,elements:project.elements.length,textures:project.textures.length})),
    documents_closed:0
  };
  filesystem.writeFileSync(root+'/evidence/reopen.json',JSON.stringify(report,null,2)+'\n');
  return report;
})()
