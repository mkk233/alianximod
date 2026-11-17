(async()=>{
const fs=require("fs");
const dir="D:/alianximod/docs/资源库/过程归档/种火_上窜熄灭_v3-20261111";
const cube=Outliner.elements.find(e=>e.name==="种火_镜头公告板");
const v=Preview.all.find(x=>x.id==="mcp_offscreen_seed_rise");
async function shot(y0,file,pos,size){const dx=pos[0],dz=pos[2];const len=Math.hypot(dx,dz)||1;const z=2.35;cube.from=[-12,y0,z];cube.to=[12,y0+28,z+0.12];cube.origin=[0,y0+14,z];cube.rotation=[0,Math.atan2(dx,dz)*180/Math.PI,0];cube.preview_controller.updateGeometry(cube);cube.preview_controller.updateTransform(cube);v.controls.target.set(0,16,0);v.camera.position.set(...pos);v.camera.aspect=size[0]/size[1];v.camera.updateProjectionMatrix();v.controls.update();const url=await new Promise(r=>v.screenshot({width:size[0],height:size[1],crop:false},r));fs.writeFileSync(dir+"/"+file,Buffer.from(url.split(",")[1],"base64"));}
await shot(-10,"bb_billboard_front.png",[0,17,62],[720,900]);
await shot(2,"bb_billboard_head.png",[0,17,62],[720,900]);
await shot(-10,"bb_billboard_side.png",[54,18,18],[960,720]);
await shot(-10,"bb_billboard_back.png",[-8,18,-62],[720,900]);
Codecs.project.write(dir+"/seed_sow_v3_preview.bbmodel");
return {billboard:true,gap:2.35};
})().catch(e=>({error:String(e.stack||e)}))
