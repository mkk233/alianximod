(async function () {
  if(!Project || Project.uuid!=='fe507dca-652f-f434-b477-f92726a559a7') throw new Error('Unexpected active project; stop without mutation');
  const root='D:/alianximod/docs/资源库/过程归档/焚天_双层收束同步_v4_1_20261116';
  const filesystem=require('fs');
  const protectedProjects=ModelProject.all.map(project=>({uuid:project.uuid,name:project.name,saved:project.saved,elements:project.elements.length,textures:project.textures.length}));
  const source='D:/alianximod/docs/资源库/过程归档/焚天_双层叠加_v4_20261116/blockbench/fentian_dual_layer_v4.bbmodel';
  const original=Project;
  Codecs.project.load(JSON.parse(filesystem.readFileSync(source,'utf8')),{path:source});
  if(Project===original || Project.name!=='fentian_dual_layer_v4') throw new Error('Unexpected native copy load; stop');
  const copy=Project.uuid;
  await new Promise(resolve=>setTimeout(resolve,500));
  if(Project.uuid!==copy) throw new Error('Active project changed; stop');
  Project.name='fentian_dual_layer_burnout_v4_1';
  Project.save_path=root+'/blockbench/fentian_dual_layer_burnout_v4_1.bbmodel';
  filesystem.writeFileSync(root+'/evidence/protected-projects-before.json',JSON.stringify({projects:protectedProjects,source_model:source,copied_project:copy},null,2)+'\n');
  return {copied_project:copy,original_project:original.uuid,elements:Outliner.elements.length,groups:Group.all.length,textures:Texture.all.length,protected:protectedProjects.length};
})()
