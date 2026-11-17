(async function () {
 const state=globalThis.netherCoverage;
 const options=globalThis.netherCoverageCapture;
 if(!state||!options||!Project||Project.uuid!==state.project)throw new Error('Project changed; stop shared GUI work');
 const filesystem=require('fs');
 const family=options.family;
 const stage=options.stage||'loop';
 const frame=options.frame||6;
 const byId=new Map(Outliner.elements.map(element=>[element.uuid,element]));
 const groups=new Map(Group.all.map(group=>[group.uuid,group]));
 function getTexture(key,path,width,height){
  let selected=Texture.all.find(texture=>texture.uuid===state.textures[key]||texture.name===key);
  if(!selected)selected=new Texture({name:key,uv_width:width,uv_height:height,render_mode:'emissive',render_sides:'double'}).fromPath(path).add(false);
  state.textures[key]=selected.uuid;
  return selected;
 }
 function asset(id,mode,width,height){
  const path=mode==='original'?state.root+'/references/'+id+'_original.png':state.root+'/textures/'+id+'_'+mode+'.png';
  return getTexture(id+'_'+mode,path,width,height);
 }
 function shift(node,offset){
  if(node.origin)node.origin=node.origin.map((value,axis)=>value+offset[axis]);
  if(node instanceof Cube){
   node.from=node.from.map((value,axis)=>value+offset[axis]);
   node.to=node.to.map((value,axis)=>value+offset[axis]);
  }
  if(node instanceof Group)for(const child of node.children)shift(child,offset);
 }
 Undo.initEdit({elements:Outliner.elements,outliner:true,textures:[]});
 for(const element of Outliner.elements)element.visibility=false;
 for(const station of state.stations){
  const visible=station.family===family&&(!options.blueOnly||station.mode==='blue');
  groups.get(station.wrapper).visibility=visible;
  for(const uuid of station.members){
   const element=byId.get(uuid);
   if(element)element.visibility=visible&&station.originalVisibility[uuid]!==false;
  }
  if(station.family==='liaoli'){
   const diving=family==='liaoli'&&stage==='thrust';
   const pose=groups.get(station.bodyPose);
   if(pose)pose.rotation[0]=diving?-90:0;
   if(diving!==!!station.poseApplied){
    const backFan=groups.get(station.backFan);
    if(backFan)shift(backFan,[0,diving?-12:12,0]);
    station.poseApplied=diving;
   }
  }
 }
 for(const record of state.faces.filter(record=>record.family===family)){
  const element=byId.get(record.element);
  const face=element.faces[record.side];
  let selected;
  let height=record.frameHeight;
  let selectedFrame=frame;
  if(family==='heat'||family==='burn'){
   const phase=stage==='entry'?'entry':'loop';
   const count=phase==='entry'?8:20;
   selectedFrame=((frame-1)%count)+1;
   selected=asset((family==='heat'?'furnace':'burn')+'_'+phase+'_'+record.key,record.mode,96,112*count);
  }else if(family==='liaoli'){
   if(record.key==='rear_ring'){
    selected=asset('liaoli_rear_ring',record.mode,96,2304);
   }else{
    const suffix=stage==='ignite'?'back_ignite':stage==='thrust'?'thrust_backflow':'back_flow';
    height=stage==='thrust'?64:48;
    const count=stage==='ignite'?12:24;
    selectedFrame=((frame-1)%count)+1;
    selected=asset('liaoli_'+suffix,record.mode,128,height*count);
   }
  }else if(record.key==='ground_embers'){
   selected=asset('liaoyuan_v3_ground_embers',record.mode,16,384);
  }else{
   selected=face.getTexture();
  }
  face.texture=selected.uuid;
  if(Array.isArray(record.baseUV)){
   face.uv=record.baseUV.map((value,index)=>index%2===1?value/record.frameHeight*height+(selectedFrame-1)*height:value);
  }else{
   face.uv=Object.fromEntries(Object.entries(record.baseUV).map(([vertex,uv])=>[vertex,[uv[0],uv[1]/record.frameHeight*height+(selectedFrame-1)*height]]));
  }
 }
 Undo.finishEdit('Coverage '+family+' '+stage+' F'+frame,{elements:Outliner.elements,outliner:true,textures:Texture.all});
 await new Promise(resolve=>setTimeout(resolve,300));
 Canvas.updateAll();unselectAllElements();updateSelection();
 for(const texture of Texture.all){
  const material=texture.getMaterial();
  if(material.uniforms?.SHADE)material.uniforms.SHADE.value=false;
 }
 const camera=options.camera||{position:[20,42,170],target:[0,16,0],projection:'orthographic',zoom:.32};
 Preview.selected.loadAnglePreset(camera);
 Preview.selected.controls.update();Preview.selected.render();
 const screenshot=state.root+'/previews/'+options.name+'.png';
 await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:1280,height:640},data=>{
  filesystem.writeFileSync(screenshot,Buffer.from(data.split(',')[1],'base64'));resolve();
 }));
 state.captures.push({family,stage,frame,blueOnly:!!options.blueOnly,camera,path:screenshot,source:'real Blockbench main Preview.screenshot, no postprocessing'});
 filesystem.writeFileSync(state.root+'/evidence/blockbench-captures.json',JSON.stringify(state.captures,null,2));
 filesystem.writeFileSync(state.root+'/evidence/blockbench-build.json',JSON.stringify(state,null,2));
 return {family,stage,frame,screenshot,visibleElements:Outliner.elements.filter(element=>element.visibility).length,textureErrors:Texture.all.filter(texture=>texture.error||!texture.img.complete).map(texture=>texture.name)};
})()
