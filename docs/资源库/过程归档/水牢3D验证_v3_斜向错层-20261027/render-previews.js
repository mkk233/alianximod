(async()=>{
 const root="D:/alianximod/docs/资源库/过程归档/水牢3D验证_v3_斜向错层-20261027/";
 const fs=require('fs');const p=Preview.all.find(p=>p.id==='mcp_offscreen_water_prison_preview');
 TextureAnimator.stop();
 const oldBackground=Canvas.scene.background;
 Canvas.scene.background=new THREE.Color('#172D38');
 p.controls.enableDamping=false;
 const png=path=>{let data;Canvas.withoutGizmos(()=>{p.render();data=p.canvas.toDataURL('image/png');});fs.writeFileSync(path,Buffer.from(data.split(',')[1],'base64'));};
 function camera(az,wide){const r=103;p.camPers.position.set(r*Math.sin(az),wide?29:35,r*Math.cos(az));p.controls.target.set(0,wide?14:20,0);p.camPers.fov=35;p.camPers.updateProjectionMatrix();p.controls.update();}
 for(const spec of [{name:'fixed',count:32,wide:false,orbit:false},{name:'orbit',count:96,wide:false,orbit:true},{name:'wide',count:32,wide:true,orbit:false}]){
  fs.mkdirSync(root+'frames-'+spec.name,{recursive:true});
  window.waterPrisonShow(spec.wide?1:0);
  for(let i=0;i<spec.count;i++){
   const time=i/20;
   TextureAnimator.playAnimationFrame(time);
   camera(Math.PI/6+(spec.orbit?i/spec.count*Math.PI*2:0),spec.wide);
   png(root+'frames-'+spec.name+'/frame_'+String(i).padStart(3,'0')+'.png');
   
  }
 }
 window.waterPrisonShow(0);TextureAnimator.playAnimationFrame(0);
 camera(0,false);png(root+'water_prison_3d_front.png');
 camera(Math.PI/2,false);png(root+'water_prison_3d_side.png');
 camera(Math.PI,false);png(root+'water_prison_3d_back.png');
 camera(Math.PI/6,false);png(root+'water_prison_3d_oblique.png');
 window.waterPrisonShow(1);camera(Math.PI/6,true);png(root+'water_prison_3d_wide.png');
 window.waterPrisonShow(0);camera(Math.PI/6,false);
 Canvas.scene.background=oldBackground;
 return {savedFrames:160,size:[p.canvas.width,p.canvas.height],fixed:1.6,orbit:4.8,wide:1.6};
})()