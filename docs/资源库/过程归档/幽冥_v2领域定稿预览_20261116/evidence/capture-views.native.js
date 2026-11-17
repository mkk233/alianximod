(()=>{
const d=window.gloomPreview;if(Project.uuid!==d.project)throw Error('wrong project');
const a=Animation.all.find(a=>a.uuid===d.animation);a.select();Timeline.pause();TextureAnimator.stop();
const p=Preview.selected,base=Cube.all.map(q=>[q.uuid,q.visibility]),byId=new Map(Cube.all.map(q=>[q.uuid,q])),views=[];
const pose=t=>{Timeline.setTime(t);Animator.preview();TextureAnimator.playAnimationFrame(t+1e-8);for(const id of d.phaseTextures){const m=Texture.all.find(t=>t.uuid===id).getMaterial();m.uniforms.SHADE.value=false;const light=m.uniforms.LIGHTCOLOR.value;if(light.setRGB)light.setRGB(1,1,1);else light.set(1,1,1);}Canvas.updateVisibility();};
const camera=(pos,target,fov=42)=>{p.setProjectionMode(false);p.camPers.fov=fov;p.camPers.updateProjectionMatrix();p.camera.position.fromArray(pos);p.controls.target.fromArray(target);p.controls.update();};
const capture=(name,t)=>{pose(t);let wrote=false;Screencam.screenshotPreview(p,{crop:false},url=>{Blockbench.writeFile(d.dest+'/'+name,{savetype:'image',content:url});wrote=true;});if(!wrote)throw Error('Synchronous native screenshot not written');views.push({file:name,time:t,camera:p.camera.position.toArray(),target:p.controls.target.toArray(),fov:p.camPers.fov,viewport:p.id});};
Undo.initEdit({elements:Cube.all});
for(const [id,v] of base)byId.get(id).visibility=v;
camera([113,93,127],[0,8,0]);capture('01-domain-overview.png',.7);
camera([0,25.92,90],[0,12,0],62);capture('02-eye-height-1p62.png',.7);
const f=d.samples.find(s=>s.index===d.focus),fx=f.x*16,fz=f.z*16;
for(const q of Cube.all)q.visibility=q.parent?.uuid===f.group||/^AUX_1block_tile_-?[01]_-?[01]$/.test(q.name)||q.parent?.uuid===d.marker;
camera([fx+45,9,fz+21],[fx,8,fz],38);capture('03-side-anchor-plus0p15.png',.7);
for(const q of Cube.all)if(q.parent?.uuid===d.marker)q.visibility=false;
camera([fx+19,15,fz+41],[fx,8,fz],32);
[.1,.5,.9,1.3].forEach((t,i)=>capture('04-single-stage-'+(i+1)+'-frame'+String(i*4).padStart(2,'0')+'.png',t));
for(const [id,v] of base)byId.get(id).visibility=v;
Undo.finishEdit('Core view capture with original visibility restored');
camera([113,93,127],[0,8,0]);pose(.7);
Blockbench.writeFile(d.dest+'/evidence/view-cameras.json',{content:JSON.stringify({project:Project.uuid,views},null,2)});
d.viewCameras=views;
Codecs.project.write(Codecs.project.compile(),d.dest+'/youming-v2-domain-current-49.bbmodel');
return {views:views.map(v=>v.file),saved:Project.saved,project:Project.uuid,undoActive:!!Undo.current_save};
})()
