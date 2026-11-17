(async()=>{
const p=window.heatPreviewV31;
if(!p||Project.uuid!==p.project)throw new Error("Expected v3.1 project");
const fs=require("fs");
const v=Preview.all.find(v=>v.id==="mcp_offscreen_heat_v31_preview");
if(!v)throw new Error("Create the heat_v31_preview offscreen viewport first");
const folder=p.out+"/viewport_frames";
fs.mkdirSync(folder,{recursive:true});
TextureAnimator.stop();
v.camPers.fov=36;v.camPers.zoom=1;v.camPers.updateProjectionMatrix();
v.controls.autoRotate=false;
const oldBg=Canvas.scene.background;
Canvas.scene.background=new THREE.Color("#19252f");
const save=async file=>new Promise(resolve=>v.screenshot({crop:false},data=>{
  fs.writeFileSync(file,Buffer.from(data.split(",")[1],"base64"));resolve();
}));
try{
  for(const [name,position]of [["front",[39,26,66]],["rear",[-45,25,-63]]]){
    v.camera.position.set(...position);v.controls.target.set(0,15.5,0);v.controls.update();
    for(let frame=0;frame<20;frame++){
      p.setFrame("loop",frame);
      await save(folder+"/"+name+"_"+String(frame).padStart(2,"0")+".png");
    }
    p.setFrame("loop",6);
    await save(p.out+"/heat_up_v3_1_"+name+".png");
  }
  v.camera.position.set(39,26,66);v.controls.target.set(0,15.5,0);v.controls.update();
  for(let frame=0;frame<8;frame++){
    p.setFrame("entry",frame);
    await save(folder+"/entry_"+String(frame).padStart(2,"0")+".png");
  }
  p.setFrame("loop",6);
  fs.writeFileSync(p.out+"/heat_up_v3_1_spatial.bbmodel",Codecs.project.compile());
  Project.saved=true;
  return {frames:48,size:[v.width,v.height],folder,model:Project.save_path,
    capture:"Real Blockbench Preview.screenshot, same v3.1 source layers"};
}finally{Canvas.scene.background=oldBg;}
})()
