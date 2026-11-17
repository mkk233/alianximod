(async()=>{
const fs=require("fs"),s=window.seedSowRepair;
if(Project.uuid!==s.project)throw new Error("Wrong active project");
const v=Preview.all.find(p=>p.id==="mcp_offscreen_seed_sow_archive"),gui=Preview.all.find(p=>p.id==="main");
const evidence=[];
function tree(g,b){g.visibility=b;for(const e of g.children){e.visibility=b;if(e instanceof Group)tree(e,b);}}
function hideLowLabels(){for(const e of Outliner.elements)if(/AUX_LABEL_(main_time|main_limit|phase_span_|phase_note)/.test(e.name))e.visibility=false;}
function pose(pos,target,follow,phases=false,side=false){tree(s.main,!phases);tree(s.phase,phases);tree(s.side,side);hideLowLabels();const n=new THREE.Vector3(...pos).sub(new THREE.Vector3(0,14.4,0)).normalize();const rot=[-Math.asin(n.y)*180/Math.PI,Math.atan2(n.x,n.z)*180/Math.PI,0];s.bill.rotation=follow?rot:[0,0,0];for(const e of Outliner.elements){if(e.name.startsWith("AUX_LABEL_")){e.origin=e.from.map((a,i)=>(a+e.to[i])/2);e.rotation=phases?[0,0,0]:rot;if(side&&(e.name==="AUX_LABEL_main_title"||e.name==="AUX_LABEL_main_dimensions"))e.visibility=false;}}
if(side){for(const [name,y,w] of [["side_note",38,72],["side_values",33,76]]){const e=Outliner.elements.find(q=>q.name==="AUX_LABEL_"+name);const h=e.to[1]-e.from[1];e.from=[-w/2,y-h/2,0];e.to=[w/2,y+h/2,.01];e.origin=[0,y,.005];e.rotation=rot;}}
Canvas.updateAll();
for(const p of [v,gui]){p.setProjectionMode(false);p.camera.position.set(...pos);p.controls.target.set(...target);p.controls.update();p.renderer.setClearColor(0x19212b,1);}
return {rot,n:n.toArray()};}
async function shot(file,pos,target,fov,follow,phases=false,side=false,w=1600,h=1100){v.resize(w,h);const p=pose(pos,target,follow,phases,side);for(const q of [v,gui]){q.camPers.fov=fov;q.camPers.updateProjectionMatrix();q.render();}await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));const url=await new Promise(r=>v.screenshot({crop:false},r));fs.writeFileSync(s.dir+"/"+file,Buffer.from(url.split(",")[1],"base64"));const actual=new THREE.Vector3(0,0,1).applyQuaternion(s.bill.mesh.quaternion);evidence.push({file,viewport:v.id,width:v.width,height:v.height,camera:pos,target,fov,followCamera:follow,pitchYawDegrees:follow?p.rot:[0,0,0],normal:actual.toArray(),normalCameraDot:actual.dot(new THREE.Vector3(...p.n)),phaseFrames:phases?[1,6,10,12,15,16]:[8],bytes:fs.statSync(s.dir+"/"+file).size});}
await shot("01-hero-3quarter.png",[53,37,73],[0,16,0],36,true);
await shot("02-front-billboard.png",[0,14.4,88],[0,16,0],35,true);
await shot("03-side-offset-thickness.png",[78,19,20],[0,16,0],38,false,false,true);
await shot("04-closeup-burnout-phases.png",[0,19,240],[0,17,0],17.5,false,true,false,2048,760);
v.resize(1600,1100);pose([53,37,73],[0,16,0],true);for(const q of [v,gui]){q.camPers.fov=36;q.camPers.updateProjectionMatrix();q.render();}
const model=s.dir+"/seed_sow_v3_preview.bbmodel";
if(fs.existsSync(model))throw new Error("Model file already exists; check author before overwrite");
Project.save_path=model;
const content=Codecs.project.compile({editor_state:true});
Codecs.project.write(content,model);
await new Promise(r=>setTimeout(r,300));
const parsed=JSON.parse(fs.readFileSync(model,"utf8"));
const save={path:model,bytes:fs.statSync(model).size,sha256:require("crypto").createHash("sha256").update(fs.readFileSync(model)).digest("hex").toUpperCase(),elementCount:parsed.elements.length,textureCount:parsed.textures.length,embeddedTextureCount:parsed.textures.filter(t=>t.source&&t.source.startsWith("data:image")).length,meta:parsed.meta,projectSaved:Project.saved};
fs.writeFileSync(s.dir+"/viewport-capture-evidence.json",JSON.stringify({attemptId:"20261115-art-seed-sow-05",capturedUtc:new Date().toISOString(),engine:"real Blockbench Desktop 5.2.2 / native GUI Preview WebGL renderer",noExternalImageComposition:true,shots:evidence,save},null,2),"utf8");
return {shots:evidence,save};
})()
