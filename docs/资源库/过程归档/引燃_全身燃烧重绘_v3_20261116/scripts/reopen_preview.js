(async function () {
  const root='D:/alianximod/docs/资源库/过程归档/引燃_全身燃烧重绘_v3_20261116';
  if(!Project || Project.uuid!=='c136d4c5-fe9b-4cf8-f91c-3df017448ec0' || !Project.saved) throw new Error('Unexpected or unsaved candidate; no reopen');
  const filesystem=require('fs');
  const original=Project;
  const path=root+'/blockbench/fire_kindle_v3_redraw_candidate_preview.bbmodel';
  const bytes=filesystem.readFileSync(path);
  Codecs.project.load(JSON.parse(bytes.toString('utf8')),{path});
  await new Promise(resolve=>setTimeout(resolve,400));
  if(Project===original || !Project.name.startsWith('fire_kindle_v3')) throw new Error('Reopen did not create expected candidate');
  Canvas.updateAll();
  const plane=Cube.all.find(element=>element.name.startsWith('KINDLE_plane'));
  plane.faces.south.getTexture().getMaterial().uniforms.SHADE.value=false;
  Preview.selected.loadAnglePreset({position:[0,14.4,65],target:[0,14.4,0],projection:'orthographic',zoom:.55});
  Preview.selected.controls.update();
  unselectAllElements();
  const image=root+'/previews/gui_reopened_front.png';
  await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:900,height:700},data=>{filesystem.writeFileSync(image,Buffer.from(data.split(',')[1],'base64'));resolve()}));
  const report={
    status:'candidate_pending_user_confirmation',
    native_codec:'Codecs.project.load',
    original_project:original.uuid,
    reopened_project:Project.uuid,
    model:path,
    model_sha256:require('crypto').createHash('sha256').update(bytes).digest('hex').toUpperCase(),
    elements:Outliner.elements.length,
    textures:Texture.all.map(texture=>({name:texture.name,width:texture.width,height:texture.height,loaded:texture.img.complete,error:texture.error})),
    screenshot:image,
    other_projects:ModelProject.all.filter(project=>project!==Project && project!==original).map(project=>({uuid:project.uuid,name:project.name,saved:project.saved,elements:project.elements.length,textures:project.textures.length})),
    original_candidate_retained:true,
    user_projects_not_closed:true
  };
  filesystem.writeFileSync(root+'/evidence/reopen.json',JSON.stringify(report,null,2)+'\n');
  return report;
})()
