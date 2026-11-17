(async()=>{
const fs=require("fs"),s=window.seedSowRepair;
if(Project.uuid!==s.project)throw new Error("Wrong active project");
const v=Preview.all.find(p=>p.id==="mcp_offscreen_seed_sow_archive"||p.id==="seed_sow_archive");
if(!v)throw new Error("Native viewport missing: "+Preview.all.map(p=>p.id));
function tree(g,b){g.visibility=b;for(const e of g.children){e.visibility=b;if(e instanceof Group)tree(e,b);}}
tree(s.main,true);tree(s.phase,false);tree(s.side,false);
const pos=[0,16,88],target=[0,16,0];
s.bill.rotation=[0,0,0];
for(const e of Outliner.elements){if(e.name.startsWith("AUX_LABEL_")){e.origin=e.from.map((x,i)=>(x+e.to[i])/2);e.rotation=[0,0,0];}}
Canvas.updateAll();
for(const p of [v,Preview.all.find(p=>p.id==="main")]){p.setProjectionMode(false);p.camPers.fov=35;p.camera.position.set(...pos);p.controls.target.set(...target);p.camPers.updateProjectionMatrix();p.controls.update();p.renderer.setClearColor(0x19212b,1);p.render();}
const url=await new Promise(r=>v.screenshot({crop:false},r));
fs.writeFileSync(s.dir+"/02-front-billboard.png",Buffer.from(url.split(",")[1],"base64"));
const normal=new THREE.Vector3(0,0,1).applyQuaternion(s.bill.mesh.quaternion).toArray();
return {file:s.dir+"/02-front-billboard.png",viewport:{id:v.id,width:v.width,height:v.height},planeNormal:normal,material:{transparent:s.frames[7].getMaterial().transparent,alphaTest:s.frames[7].getMaterial().alphaTest,side:s.frames[7].getMaterial().side},visibility:{main:s.main.visibility,phase:s.phase.visibility},bytes:fs.statSync(s.dir+"/02-front-billboard.png").size};
})()
