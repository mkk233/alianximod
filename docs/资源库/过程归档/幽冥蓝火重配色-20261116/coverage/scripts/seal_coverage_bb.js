(async function () {
 const state=globalThis.netherCoverage;
 if(!state||Project.uuid!==state.project)throw new Error('Unexpected project; no save');
 const filesystem=require('fs'),crypto=require('crypto');
 const path=state.root+'/blockbench/nether_blue_coverage_02.bbmodel';
 const captureCode=filesystem.readFileSync(state.root+'/scripts/capture_coverage_bb.js','utf8');
 globalThis.netherCoverageCapture={family:'burn',frame:6,name:'bb_burn_loop_overall_F06'};
 await eval(captureCode);
 Project.save_path=path;
 Codecs.project.write(Codecs.project.compile(),path);
 const savedProject=Project.uuid;
 Codecs.project.load(JSON.parse(filesystem.readFileSync(path,'utf8')),{path});
 await new Promise(resolve=>setTimeout(resolve,600));
 if(Project.name!=='nether_blue_coverage_02')throw new Error('Unexpected reopened project');
 const reopenedProject=Project.uuid;
 const before=JSON.parse(filesystem.readFileSync(state.root+'/evidence/blockbench-before.json','utf8'));
 const protection=before.projects.map(baseline=>{
  const project=ModelProject.all.find(candidate=>candidate.uuid===baseline.uuid);
  if(!project)return {uuid:baseline.uuid,unchanged:false,reason:'missing'};
  const contentHash=crypto.createHash('sha256').update(JSON.stringify({
   elements:project.elements.map(element=>({uuid:element.uuid,name:element.name,from:element.from,to:element.to,origin:element.origin,rotation:element.rotation,visibility:element.visibility,vertices:element.vertices,faces:element.faces&&Object.fromEntries(Object.entries(element.faces).map(([side,face])=>[side,{texture:face.texture,uv:face.uv,vertices:face.vertices}]))})),
   textures:project.textures.map(texture=>({uuid:texture.uuid,name:texture.name,path:texture.path,source:texture.source}))
  })).digest('hex');
  return {uuid:project.uuid,name:project.name,saved:project.saved,contentHash,unchanged:contentHash===baseline.contentHash&&project.saved===baseline.saved&&project.name===baseline.name&&project.save_path===baseline.save_path};
 });
 const failures=Texture.all.filter(texture=>texture.error||!texture.img.complete).map(texture=>texture.name);
 const missingFaces=[];
 for(const element of Outliner.elements)for(const [side,face] of Object.entries(element.faces||{})){
  if(face.texture&&typeof face.texture==='string'&&!face.getTexture())missingFaces.push({element:element.name,side,texture:face.texture});
 }
 if(protection.some(project=>!project.unchanged)||failures.length||missingFaces.length)throw new Error('Reopen verification failed '+JSON.stringify({protection,failures,missingFaces}));
 Canvas.updateAll();unselectAllElements();updateSelection();
 for(const texture of Texture.all){const material=texture.getMaterial();if(material.uniforms?.SHADE)material.uniforms.SHADE.value=false;}
 Preview.selected.loadAnglePreset({position:[20,42,170],target:[0,16,0],projection:'orthographic',zoom:.32});
 Preview.selected.controls.update();Preview.selected.render();
 await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:1280,height:640},data=>{
  filesystem.writeFileSync(state.root+'/previews/bb_burn_loop_overall_F06.png',Buffer.from(data.split(',')[1],'base64'));resolve();
 }));
 const result={attempt:'art-coverage-02',application:'Blockbench Desktop 5.2.2 plugin 1.10.1',savedProject,reopenedProject,path,elements:Outliner.elements.length,meshes:Mesh.all.length,groups:Group.all.length,textures:Texture.all.length,textureErrors:failures,missingFaceTextures:missingFaces,protectedProjects:protection,allProtectedUnchanged:true,nativeReopen:true,guiWorkStopped:true};
 filesystem.writeFileSync(state.root+'/evidence/blockbench-reopen-verification.json',JSON.stringify(result,null,2));
 state.project=reopenedProject;
 state.captures=Array.from(new Map(state.captures.map(capture=>[capture.path,capture])).values());
 filesystem.writeFileSync(state.root+'/evidence/blockbench-build.json',JSON.stringify(state,null,2));
 filesystem.writeFileSync(state.root+'/evidence/blockbench-captures.json',JSON.stringify(state.captures,null,2));
 return {savedProject,reopenedProject,elements:result.elements,meshes:result.meshes,textures:result.textures,textureErrors:failures.length,missingFaces:missingFaces.length,protected:protection.length,allProtectedUnchanged:true,screenshots:state.captures.length,guiWorkStopped:true};
})()
