(()=>{
const projectId='8a58e270-0fce-3a38-f8c5-528d850c4a39';
if(Project.uuid!==projectId||Outliner.elements.length)throw Error('Unexpected project or existing content');
const directory='D:/alianximod/docs/资源库/过程归档/留焰_v2_子弹尾焰整体预览_20261112';
const fileSystem=require('fs');
const elements=[],textures=[];
Undo.initEdit({elements,textures,outliner:true});
const root=new Group({name:'留焰v2_子弹和固定路径尾焰_待本次视觉确认'}).init();
const trailGroup=new Group({name:'留焰_97个固定采样点_每点40tick'}).addTo(root).init();
const projectileGroup=new Group({name:'火苗v3_移动子弹'}).addTo(root).init();
const helperGroup=new Group({name:'示意辅助_地面与一格刻度'}).addTo(root).init();
function loadTexture(name,file){
  const texture=new Texture({name,uv_width:32,uv_height:32,render_mode:'default',force_translucent:true,render_sides:'double',keep_size:true}).fromPath(directory+'/'+file).add(false);
  texture.uv_width=32;texture.uv_height=32;textures.push(texture);return texture;
}
function solidTexture(name,color){
  const canvas=document.createElement('canvas');canvas.width=4;canvas.height=4;
  const context=canvas.getContext('2d');context.fillStyle=color;context.fillRect(0,0,4,4);
  const texture=new Texture({name,uv_width:4,uv_height:4,render_mode:'default'}).fromDataURL(canvas.toDataURL()).add(false);
  textures.push(texture);return texture;
}
const agedTextures=Array.from({length:80},(_,index)=>loadTexture('v2尾焰_age'+(index/2)+'tick','trail-age-'+String(index).padStart(2,'0')+'.png'));
const projectileTextures=Array.from({length:16},(_,index)=>loadTexture('火苗v3_F'+(index+1),'projectile-frame-'+String(index+1).padStart(2,'0')+'.png'));
const floorTexture=solidTexture('示意辅助_石板地面','#263540');
const gridTexture=solidTexture('示意辅助_一格刻度','#1d2933');
function solid(name,from,to,texture){
  const faces={};for(const face of ['north','south','east','west','up','down'])faces[face]={texture:texture.uuid,uv:[0,0,4,4]};
  const cube=new Cube({name,from,to,faces,box_uv:false,shade:false}).addTo(helperGroup).init();elements.push(cube);return cube;
}
function card(name,position,size,texture,parent){
  const faces={};for(const face of ['north','south','east','west','up','down'])faces[face]={texture:null};
  faces.south={texture:texture.uuid,uv:[0,0,32,32]};
  const cube=new Cube({name,from:[position[0]-size/2,position[1]-size/2,position[2]],to:[position[0]+size/2,position[1]+size/2,position[2]],origin:position.slice(),faces,box_uv:false,shade:false}).addTo(parent).init();
  elements.push(cube);return cube;
}
solid('示意辅助_固定地面',[-12,-1,-16],[112,0,18],floorTexture);
for(let position=-8;position<=104;position+=16)solid('示意辅助_一世界格刻度_'+position,[position-.07,.01,-16],[position+.07,.04,18],gridTexture);
for(const depth of [-8,8])solid('示意辅助_地面横缝_'+depth,[-12,.01,depth-.07],[112,.04,depth+.07],gridTexture);
const intervals=[3,5,4,3,4,5],samples=[];
let offset=0,index=0;
while(offset<=384){
  const position=[offset/4,8.25,0],birth=8+offset/8;
  const cube=card('留焰_采样_'+index,position,8,agedTextures[0],trailGroup);
  samples.push({index,offsetPixels:offset,position,birth,cube});
  offset+=intervals[index%intervals.length];index++;
}
const projectile=card('火苗v3_子弹',[0,8.25,0],8,projectileTextures[0],projectileGroup);
const task={projectId,directory,fileSystem,samples,agedTextures,projectileTextures,projectile,elements,textures,tick:40,captures:[]};
window.flameTrailCompositeV2=task;
task.setPhase=function(tick,cameraPosition,target){
  if(Project.uuid!==projectId)throw Error('Wrong active project');
  const view=Preview.selected;if(!view.canvas.isConnected)throw Error('Expected connected main viewport');
  Undo.initEdit({elements:samples.map(sample=>sample.cube).concat(projectile)});
  view.setNormalCamera();view.camera.fov=35;view.camera.updateProjectionMatrix();
  view.camera.position.set(...cameraPosition);view.controls.target.set(...target);view.controls.update();
  const euler=new THREE.Euler().setFromQuaternion(view.camera.quaternion,'ZYX');
  const rotation=[euler.x,euler.y,euler.z].map(value=>value*180/Math.PI);
  let active=0;
  for(const sample of samples){
    const age=tick-sample.birth;
    sample.cube.visibility=age>=0&&age<40;
    sample.cube.rotation=rotation.slice();
    sample.cube.faces.south.texture=agedTextures[Math.max(0,Math.min(79,Math.floor(age*2)))].uuid;
    sample.cube.name='留焰_点'+sample.index+'_出生'+sample.birth+'_年龄'+age.toFixed(3);
    if(sample.cube.visibility)active++;
  }
  const center=[Math.max(0,tick-8)*2,8.25,0];
  const size=tick<8?Math.max(2,Math.floor(32*(tick+1)/8))/4:8;
  projectile.from=[center[0]-size/2,center[1]-size/2,0];
  projectile.to=[center[0]+size/2,center[1]+size/2,0];
  projectile.origin=center;projectile.rotation=rotation.slice();projectile.visibility=tick<=56;
  projectile.faces.south.texture=projectileTextures[tick%16].uuid;
  Canvas.scene.background=new THREE.Color('#141e29');
  Canvas.updateAll();
  for(const texture of [...agedTextures,...projectileTextures]){
    const material=texture.material;
    material.transparent=true;material.depthWrite=false;material.uniforms.SHADE.value=false;material.needsUpdate=true;
  }
  for(const sample of samples)sample.cube.mesh.renderOrder=sample.index;
  projectile.mesh.renderOrder=1000;
  view.render();task.tick=tick;task.active=active;
  Undo.finishEdit('Show composite phase tick '+tick);
  return {tick,active,projectileVisible:projectile.visibility,projectilePosition:center,camera:view.camera.position.toArray(),target:view.controls.target.toArray()};
};
task.capture=function(filename){
  if(Project.uuid!==projectId)throw Error('Wrong active project');
  if(!/^viewport-[a-z0-9-]+\.png$/.test(filename))throw Error('Unexpected viewport filename');
  const view=Preview.selected;if(!view.canvas.isConnected)throw Error('Expected connected main viewport');
  view.render();
  const path=directory+'/'+filename;
  fileSystem.writeFileSync(path,Buffer.from(view.canvas.toDataURL('image/png').split(',')[1],'base64'));
  const capture={path,tick:task.tick,activeSamples:task.active,viewId:view.id,connected:true,width:view.canvas.width,height:view.canvas.height,camera:view.camera.position.toArray(),target:view.controls.target.toArray()};
  task.captures.push(capture);return capture;
};
task.save=function(){
  if(Project.uuid!==projectId)throw Error('Wrong active project');
  const path=directory+'/flame-trail-v2-projectile-composite.bbmodel';
  const compiled=Codecs.project.compile({compressed:false});
  fileSystem.writeFileSync(path,typeof compiled==='string'?compiled:JSON.stringify(compiled,null,2),'utf8');
  Project.save_path=path;Project.saved=true;return {path,bytes:fileSystem.statSync(path).size};
};
task.writeLog=function(){
  fileSystem.writeFileSync(directory+'/blockbench-capture-log.json',JSON.stringify({projectId,version:Blockbench.version,viewport:'connected main; explicit render() then canvas.toDataURL; no offscreen or compositing',visibilityState:document.visibilityState,captures:task.captures},null,2),'utf8');
};
fileSystem.writeFileSync(directory+'/blockbench-scene-parameters.json',JSON.stringify({attemptId:'20261112-projectile-composite-3d-resume',projectId,pixelsPerBlock:64,modelUnitsPerBlock:16,planePixels:32,planeModelUnits:8,planeWorldBlocks:.5,sampleStepsPixels:intervals,maxSampleStepWorldBlocks:5/64,sampleLifeTicks:40,ageTextureQuantizationTicks:.5,animationFrames:104,frameMilliseconds:50,launchTick:8,lastEmissionTick:56,projectileSpeedPixelsPerTick:8,projectileSpeedWorldBlocksPerTick:.125,previewOnly:true,newTrailTexture:'trail-stamp-v2-strip.png',projectileSource:'fire_spark_v3',opacity:'0.44*(1-age/40)^1.6',inkVerticalScale:'0.35+0.65*(1-age/40)',samples:samples.map(sample=>({index:sample.index,birthTick:sample.birth,positionModelUnits:sample.position,offsetPixels:sample.offsetPixels}))},null,2),'utf8');
Undo.finishEdit('Create same-version projectile and stationary flame trail composite');
unselectAllElements();
return {projectId,elements:elements.length,textures:textures.length,samples:samples.length,textureLoading:true,existingProjects:ModelProject.all.map(project=>({uuid:project.uuid,name:project.name,saved:project.saved}))};
})()
