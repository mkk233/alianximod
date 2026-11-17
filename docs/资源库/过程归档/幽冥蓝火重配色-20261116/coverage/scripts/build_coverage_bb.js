(async function () {
 const root='D:/alianximod/docs/资源库/过程归档/幽冥蓝火重配色-20261116/coverage';
 const expected='97d2ac09-d5a8-c46f-1f79-8a8b4e5a7840';
 if(!Project||Project.uuid!==expected||Outliner.elements.length)throw new Error('Unexpected or occupied project; no mutation');
 const filesystem=require('fs');
 const sources={
  heat:'D:/alianximod/docs/资源库/特效/火焰/升温_贴身炽热_v3.1/spatial/heat_up_v3_1_spatial.bbmodel',
  burn:'D:/alianximod/docs/资源库/特效/火焰/燃烧_空间挂载_v1/燃烧v3_空间挂载示意_v1.bbmodel',
  liaoli:'D:/alianximod/docs/资源库/特效/火焰/燎里_剑阵姿态_v7/liaoli-dive-leg-array-forward.bbmodel'
 };
 const state={project:expected,root,sources,stations:[],faces:[],textures:{},captures:[],notes:[]};
 const cache=new Map();
 function texture(key,path,width,height){
  if(cache.has(key))return cache.get(key);
  const result=new Texture({name:key,uv_width:width,uv_height:height,render_mode:'emissive',render_sides:'double'}).fromPath(path).add(false);
  cache.set(key,result);
  state.textures[key]=result.uuid;
  return result;
 }
 function candidate(id,mode,width,height){
  const path=mode==='original'?root+'/references/'+id+'_original.png':root+'/textures/'+id+'_'+mode+'.png';
  return texture(id+'_'+mode,path,width,height);
 }
 function walk(node,visitor){
  visitor(node);
  if(node instanceof Group)for(const child of node.children)walk(child,visitor);
 }
 function shift(node,offset){
  walk(node,child=>{
   if(child.origin)child.origin=child.origin.map((value,axis)=>value+offset[axis]);
   if(child instanceof Cube){
    child.from=child.from.map((value,axis)=>value+offset[axis]);
    child.to=child.to.map((value,axis)=>value+offset[axis]);
   }
  });
 }
 function textureKey(family,name){
  if(family==='heat'&&name.startsWith('loop_'))return name.slice(5);
  if(family==='burn'){
   if(name==='burn_v3_cape')return 'cape';
   if(name==='burn_v3_rear-fire')return 'rear';
   if(name==='burn_v3_hand-fire')return 'hand';
   if(name.startsWith('burn_part_'))return name.slice(10);
  }
  return null;
 }
 for(const [family,source] of Object.entries(sources)){
  const previousRoots=new Set(Outliner.root);
  const data=JSON.parse(filesystem.readFileSync(source,'utf8'));
  delete data.animations;
  Codecs.project.merge(data,source);
  const imported=Outliner.root.filter(node=>!previousRoots.has(node));
  Undo.initEdit({elements:[],outliner:true,textures:[]});
  const master=new Group({name:'STATION_'+family+'_original',origin:[0,0,0]}).init();
  for(const node of imported)node.addTo(master);
  const wrappers=[master,master.duplicate(),master.duplicate()];
  for(const [column,wrapper] of wrappers.entries()){
   const mode=['original','legacy','blue'][column];
   const offset=[(column-1)*46,0,0];
   wrapper.name='STATION_'+family+'_'+mode;
   const members=[];
   walk(wrapper,node=>members.push(node));
   const station={family,mode,wrapper:wrapper.uuid,column,offset,members:members.map(node=>node.uuid),originalVisibility:{}};
   for(const node of members){
    station.originalVisibility[node.uuid]=node.visibility!==false;
    if(!(node instanceof Cube||node instanceof Mesh))continue;
    for(const [side,face] of Object.entries(node.faces)){
     const oldTexture=face.getTexture();
     const key=oldTexture&&textureKey(family,oldTexture.name);
     if(!key)continue;
     const assetPrefix=family==='heat'?'furnace':'burn';
     const record={family,mode,key,element:node.uuid,side,baseUV:JSON.parse(JSON.stringify(face.uv)),frameHeight:112};
     state.faces.push(record);
     const selected=candidate(assetPrefix+'_loop_'+key,mode,96,2240);
     face.texture=selected.uuid;
     if(node instanceof Cube)face.uv=[face.uv[0],face.uv[1]+560,face.uv[2],face.uv[3]+560];
     else for(const coordinates of Object.values(face.uv))coordinates[1]+=560;
     node.shade=false;
    }
   }
   if(family==='liaoli'){
    const sourceElements=members.filter(node=>node instanceof Mesh);
    for(const hilt of sourceElements.filter(node=>node.name.endsWith('_hilt'))){
     const stem=hilt.name.slice(0,-5);
     const tip=sourceElements.find(node=>node.name===stem+'_tip');
     if(!tip)throw new Error('Missing sword tip '+stem);
     const direction=new THREE.Vector3(...tip.origin).sub(new THREE.Vector3(...hilt.origin));
     const length=direction.length()+4;
     direction.normalize();
     const start=new THREE.Vector3(...hilt.origin).addScaledVector(direction,-2);
     const end=start.clone().addScaledVector(direction,length);
     const normal=new THREE.Vector3(0,1,0).multiplyScalar(length*.1875);
     const vertices={rootLow:start.clone().sub(normal).toArray(),rootHigh:start.clone().add(normal).toArray(),tipHigh:end.clone().add(normal).toArray(),tipLow:end.clone().sub(normal).toArray()};
     const flame=new Mesh({name:'FIRE_'+stem,origin:[0,0,0],vertices}).addTo(hilt.parent).init();
     const selected=candidate('liaoli_back_flow',mode,128,1152);
     const face=new MeshFace(flame,{vertices:['rootLow','rootHigh','tipHigh','tipLow'],uv:{rootLow:[0,288],rootHigh:[0,240],tipHigh:[128,240],tipLow:[128,288]},texture:selected.uuid});
     const faceId=flame.addFaces(face)[0];
     station.members.push(flame.uuid);station.originalVisibility[flame.uuid]=true;
     state.faces.push({family,mode,key:'sword_fire',element:flame.uuid,side:faceId,baseUV:{rootLow:[0,48],rootHigh:[0,0],tipHigh:[128,0],tipLow:[128,48]},frameHeight:48});
    }
    const ringTexture=candidate('liaoli_rear_ring',mode,96,2304);
    const ring=new Cube({name:'FIRE_rear_ring_color_reference',from:[-15,3,24],to:[15,33,24],origin:[0,18,24],box_uv:false,shade:false}).addTo(wrapper).init();
    for(const [side,face] of Object.entries(ring.faces))face.extend({texture:side==='south'?ringTexture.uuid:null,uv:[0,480,96,576]});
    station.members.push(ring.uuid);station.originalVisibility[ring.uuid]=true;
    state.faces.push({family,mode,key:'rear_ring',element:ring.uuid,side:'south',baseUV:[0,0,96,96],frameHeight:96});
    station.bodyPose=members.find(node=>node instanceof Group&&node.name==='BodyPose')?.uuid;
    station.backFan=members.find(node=>node instanceof Group&&node.name==='BackFan')?.uuid;
   }
   shift(wrapper,offset);
   wrapper.visibility=family==='burn';
   state.stations.push(station);
  }
  Undo.finishEdit('Import approved '+family+' geometry and attach three color comparisons',{elements:Outliner.elements,outliner:true,textures:Texture.all});
 }
 Undo.initEdit({elements:[],outliner:true,textures:[]});
 const auxiliary=Texture.all.find(selected=>selected.name==='示意辅助_灰色玩家');
 function cube(name,from,to,parent){
  const result=new Cube({name,from,to,box_uv:false}).addTo(parent).init();
  for(const face of Object.values(result.faces))face.extend({texture:auxiliary.uuid,uv:[0,0,16,16]});
  return result;
 }
 function plane(name,from,to,parent,selected,uv,side='south'){
  const result=new Cube({name,from,to,box_uv:false,shade:false}).addTo(parent).init();
  for(const [faceName,face] of Object.entries(result.faces))face.extend({texture:faceName===side?selected.uuid:null,uv});
  return result;
 }
 for(const [column,mode] of ['original','legacy','blue'].entries()){
  const center=(column-1)*46;
  const wrapper=new Group({name:'STATION_native_'+mode}).init();
  cube('AUX_entity_head',[center-4,24,-4],[center+4,32,4],wrapper);
  cube('AUX_entity_body',[center-4,12,-2],[center+4,24,2],wrapper);
  cube('AUX_entity_right_arm',[center-8,12,-2],[center-4,24,2],wrapper);
  cube('AUX_entity_left_arm',[center+4,12,-2],[center+8,24,2],wrapper);
  cube('AUX_entity_right_leg',[center-4,0,-2],[center-.1,12,2],wrapper);
  cube('AUX_entity_left_leg',[center+.1,0,-2],[center+4,12,2],wrapper);
  for(let layer=0;layer<3;layer++){
   const kind=layer%2;
   const filename=mode==='blue'?'block_soul_fire_'+kind+'.png':mode==='legacy'?'native_fire_'+kind+'_legacy.png':'block_fire_'+kind+'.png';
   const selected=texture('NATIVE_entity_'+kind+'_'+mode,root+'/references/'+filename,16,512);
   const width=20-layer*2;
   const created=plane('NATIVE_entity_layer_'+layer,[center-width/2,layer*8,4.8+layer*.2],[center+width/2,layer*8+20,4.8+layer*.2],wrapper,selected,[0,80,16,96]);
   state.faces.push({family:'native',mode,key:'entity_'+kind,element:created.uuid,side:'south',baseUV:[0,0,16,16],frameHeight:16});
  }
  const particleFile=mode==='blue'?'particle_soul_fire_flame.png':mode==='legacy'?'native_particle_legacy.png':'particle_flame.png';
  const particle=texture('NATIVE_particle_'+mode,root+'/references/'+particleFile,8,8);
  for(let index=0;index<3;index++)plane('NATIVE_particle_'+index,[center+12,index*9+7,3],[center+17,index*9+12,3],wrapper,particle,[0,0,8,8]);
  const ashFile=mode==='legacy'?root+'/references/preserve_ground_ash_legacy_comparator.png':root+'/references/preserve_ground_ash.png';
  const ash=texture('PRESERVE_ash_'+mode,ashFile,16,16);
  plane('PRESERVE_ground_ash',[center-10,-.1,-6],[center+10,-.1,14],wrapper,ash,[0,0,16,16],'up');
  const embers=candidate('liaoyuan_v3_ground_embers',mode,16,384);
  const emberPlane=plane('FIRE_ground_embers',[center-10,.05,-6],[center+10,.05,14],wrapper,embers,[0,80,16,96],'up');
  state.faces.push({family:'native',mode,key:'ground_embers',element:emberPlane.uuid,side:'up',baseUV:[0,0,16,16],frameHeight:16});
  wrapper.visibility=false;
  const members=[];walk(wrapper,node=>members.push(node));
  state.stations.push({family:'native',mode,wrapper:wrapper.uuid,column,offset:[center,0,0],members:members.map(node=>node.uuid),originalVisibility:Object.fromEntries(members.map(node=>[node.uuid,node.visibility!==false]))});
 }
 Undo.finishEdit('Add native soul entity and particle reference',{elements:Outliner.elements,outliner:true,textures:Texture.all});
 await new Promise(resolve=>setTimeout(resolve,600));
 for(const station of state.stations){
  for(const element of Outliner.elements.filter(element=>station.members.includes(element.uuid)))element.visibility=station.family==='burn'&&station.originalVisibility[element.uuid]!==false;
 }
 Canvas.updateAll();unselectAllElements();updateSelection();
 for(const selected of Texture.all){
  const material=selected.getMaterial();
  if(material.uniforms?.SHADE)material.uniforms.SHADE.value=false;
 }
 Preview.selected.loadAnglePreset({position:[20,42,170],target:[0,16,0],projection:'orthographic',zoom:.26});
 Preview.selected.controls.update();Preview.selected.render();
 Project.credit='nether-blue-visual / art-coverage-02 / candidate only / approved geometry copied through native merge / no runtime integration';
 state.notes.push('Three comparison columns original, hypothetical legacy RGB, new palette. Native blue column reuses original MC soul textures.');
 state.notes.push('Liaoli sword metal and auxiliary mannequins unchanged. Added flame cards show color placement, not exact Forge geometry or billboard simulation.');
 globalThis.netherCoverage=state;
 filesystem.writeFileSync(root+'/evidence/blockbench-build.json',JSON.stringify(state,null,2));
 Project.save_path=root+'/blockbench/nether_blue_coverage_02.bbmodel';
 Codecs.project.write(Codecs.project.compile(),Project.save_path);
 return {project:Project.uuid,model:Project.save_path,elements:Outliner.elements.length,meshes:Mesh.all.length,textures:Texture.all.length,failedTextures:Texture.all.filter(selected=>selected.error||!selected.img.complete).map(selected=>selected.name),stations:state.stations.map(station=>({family:station.family,mode:station.mode,elements:station.members.length}))};
})()
