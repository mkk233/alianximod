(async function () {
 const root='D:/alianximod/docs/资源库/过程归档/幽冥蓝火重配色-20261116';
 const reopenedId='7fd9fc60-dd61-6b41-eafd-daefc3fb48ea';
 if(!Project||Project.uuid!==reopenedId)throw new Error('Unexpected project after native reopen');
 const filesystem=require('fs');
 const crypto=require('crypto');
 const before=JSON.parse(filesystem.readFileSync(root+'/evidence/blockbench-before.json','utf8'));
 const protectedProjects=before.projects.filter(project=>project.uuid!=='803970b1-7113-95b6-9e54-ee1415c7edff');
 const protection=protectedProjects.map(baseline=>{
  const project=ModelProject.all.find(candidate=>candidate.uuid===baseline.uuid);
  if(!project)return {uuid:baseline.uuid,unchanged:false,reason:'missing project'};
  const contentHash=crypto.createHash('sha256').update(JSON.stringify({
   elements:project.elements.map(element=>({
    uuid:element.uuid,name:element.name,from:element.from,to:element.to,origin:element.origin,
    rotation:element.rotation,visibility:element.visibility,
    faces:element.faces&&Object.fromEntries(Object.entries(element.faces).map(([side,face])=>[side,{texture:face.texture,uv:face.uv}]))
   })),
   textures:project.textures.map(texture=>({uuid:texture.uuid,name:texture.name,path:texture.path,source:texture.source}))
  })).digest('hex');
  return {uuid:project.uuid,name:project.name,saved:project.saved,elements:project.elements.length,textures:project.textures.length,contentHash,unchanged:contentHash===baseline.contentHash&&project.saved===baseline.saved&&project.name===baseline.name&&project.save_path===baseline.save_path};
 });
 if(protection.some(project=>!project.unchanged))throw new Error('Protected project changed; stop GUI work');
 Canvas.updateAll();unselectAllElements();
 for(const texture of Texture.all){
  const material=texture.getMaterial();
  if(material.uniforms&&material.uniforms.SHADE)material.uniforms.SHADE.value=false;
 }
 Preview.selected.loadAnglePreset({position:[0,16,180],target:[0,16,0],projection:'orthographic',zoom:.21});
 Preview.selected.controls.update();
 Preview.selected.render();
 const screenshot=root+'/previews/bb_reopened_model_front_F06.png';
 await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:1280,height:640},data=>{
  filesystem.writeFileSync(screenshot,Buffer.from(data.split(',')[1],'base64'));
  resolve();
 }));
 const result={
  attempt:'art-v1-bb-finish-20261116',
  source:'native Codecs.project.load of saved bbmodel in connected Blockbench Desktop 5.2.2',
  reopenedProject:Project.uuid,
  saved:Project.saved,
  model:Project.save_path,
  elements:Cube.all.length,
  groups:Group.all.length,
  textures:Texture.all.map(texture=>({name:texture.name,width:texture.width,height:texture.height,error:texture.error,complete:texture.img.complete})),
  visibleFire:Cube.all.filter(cube=>cube.name.startsWith('FIRE_')&&cube.visibility).map(cube=>({name:cube.name,uv:cube.faces.south.uv,texture:cube.faces.south.getTexture().name})),
  screenshot,
  comparisonLighting:'Preview material SHADE=false for all comparison textures; not a Forge lighting claim',
  protection,
  allProtectedProjectsUnchanged:protection.every(project=>project.unchanged)
 };
 filesystem.writeFileSync(root+'/evidence/blockbench-reopen-verification.json',JSON.stringify(result,null,2));
 return result;
})()
