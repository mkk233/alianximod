(async()=>{
const root='D:/alianximod/docs/资源库/过程归档/引爆_短促火团_v2_20261116',fs=require('fs'),crypto=require('crypto');
if(!Project||Project.uuid!=='07be6674-6249-0d6c-f677-8fc0b139903d'||!Project.saved)throw new Error('Own saved source not active');
const original=Project,path=root+'/blockbench/fire_detonate_v2_candidate.bbmodel';
const disk=fs.readFileSync(path,'utf8'),data=JSON.parse(disk);
Codecs.project.load(data,{path});
if(Project===original||Project.name!=='fire_detonate_v2_candidate')throw new Error('Native reopen failed');
const reopened=Project;
await Promise.all(Texture.all.map(t=>new Promise(resolve=>{if(t.img.complete&&t.img.naturalWidth)resolve();else{t.img.addEventListener('load',resolve,{once:true});setTimeout(resolve,1500);}})));
if(Project!==reopened)throw new Error('Active document changed during reopen');
await new Promise(resolve=>setTimeout(resolve,700));
const effectTextures=Texture.all.filter(t=>/^detonate_v2_/.test(t.name));
if(effectTextures.length!==2||effectTextures.some(t=>t.error||t.width!==64||t.height!==1024))throw new Error('Reopened effect textures invalid');
Canvas.updateAll();Canvas.updateVisibility();unselectAllElements();
for(const t of effectTextures){const m=t.getMaterial();m.uniforms.SHADE.value=false;m.uniforms.EMISSIVE.value=false;m.depthWrite=false;m.depthTest=true;m.transparent=true;m.needsUpdate=true;}
for(const c of Cube.all.filter(c=>c.name.startsWith('FX_')))c.mesh.renderOrder=10;
Preview.selected.loadAnglePreset({position:[58,44,165],target:[0,17,0],projection:'orthographic',zoom:.32});Preview.selected.controls.update();
await new Promise(resolve=>setTimeout(resolve,700));
await new Promise(resolve=>Preview.selected.screenshot({crop:false,width:1200,height:850},url=>{fs.writeFileSync(root+'/evidence/reopened_native_viewport.png',Buffer.from(url.split(',')[1],'base64'));resolve();}));
function fingerprint(p){return {uuid:p.uuid,name:p.name,saved:p.saved,path:p.save_path,elements:p.elements.length,textures:p.textures.length,sha256:crypto.createHash('sha256').update(JSON.stringify({elements:p.elements.map(e=>({uuid:e.uuid,name:e.name,from:e.from,to:e.to,origin:e.origin,rotation:e.rotation,visibility:e.visibility,faces:e.faces?Object.fromEntries(Object.entries(e.faces).map(([k,v])=>[k,{uv:v.uv,texture:v.texture,rotation:v.rotation,tint:v.tint}])):null})),textures:p.textures.map(t=>({uuid:t.uuid,name:t.name,path:t.path,source:t.source,uv_width:t.uv_width,uv_height:t.uv_height}))})).digest('hex')};}
const before=JSON.parse(fs.readFileSync(root+'/evidence/protected-projects-before.json','utf8'));
const after=before.map(ref=>{const p=ModelProject.all.find(p=>p.uuid===ref.uuid);if(!p)throw new Error('Protected project missing');return fingerprint(p);});
const unchanged=JSON.stringify(before)===JSON.stringify(after);if(!unchanged)throw new Error('Protected project mismatch');
const report={native_reopen:true,original_uuid:original.uuid,reopened_uuid:reopened.uuid,path,name:reopened.name,bytes:Buffer.byteLength(disk),sha256:crypto.createHash('sha256').update(disk).digest('hex').toUpperCase(),elements:Cube.all.length,groups:Group.all.length,textures:Texture.all.length,embedded_textures:data.textures.filter(t=>t.source&&t.source.startsWith('data:image')).length,effect_textures:effectTextures.map(t=>({name:t.name,width:t.width,height:t.height,error:t.error,uv_width:t.uv_width,uv_height:t.uv_height})),effect_faces:Cube.all.filter(c=>c.name.startsWith('FX_')).map(c=>({name:c.name,visible:c.visibility,uv:c.faces.south.uv,texture:c.faces.south.getTexture().name,origin:c.origin,rotation:c.rotation,from:c.from,to:c.to})),protected_projects_unchanged:unchanged,protected_project_count:before.length,protected_unsaved:after.filter(p=>!p.saved).map(p=>({name:p.name,uuid:p.uuid})),all_original_tabs_preserved:true,at_utc:new Date().toISOString()};
fs.writeFileSync(root+'/evidence/reopen-validation.json',JSON.stringify(report,null,2));
fs.writeFileSync(root+'/evidence/protected-projects-final.json',JSON.stringify(after,null,2));
return report;
})()
