(()=>{
 const root="D:/alianximod/docs/资源库/过程归档/水牢公告板验证_v4_等比缩放-20261027/";
 const fs=require('fs');const p=Preview.all.find(p=>p.id==='mcp_offscreen_water_prison_preview');const checks=[];
 TextureAnimator.stop();const prev=Canvas.scene.background;Canvas.scene.background=new THREE.Color('#172D38');p.controls.enableDamping=false;
 function camera(a,wide){const r=103;p.camPers.position.set(r*Math.sin(a),wide?29:35,r*Math.cos(a));p.controls.target.set(0,wide?11:19,0);p.camPers.fov=35;p.camPers.updateProjectionMatrix();p.controls.update();}
 function capture(path){let uri;Canvas.withoutGizmos(()=>{p.render();uri=p.canvas.toDataURL('image/png');});fs.writeFileSync(path,Buffer.from(uri.split(',')[1],'base64'));}
 for(const spec of [{name:'orbit',count:96,wide:false,orbit:true},{name:'fixed',count:32,wide:false,orbit:false},{name:'wide',count:32,wide:true,orbit:false}]){
  fs.mkdirSync(root+'frames-'+spec.name,{recursive:true});window.waterBillboardShow(spec.wide?1:0);
  for(let i=0;i<spec.count;i++){
   TextureAnimator.playAnimationFrame(i/20);camera(Math.PI/6+(spec.orbit?i/spec.count*Math.PI*2:0),spec.wide);capture(root+'frames-'+spec.name+'/frame_'+String(i).padStart(3,'0')+'.png');
   const m=Mesh.all.find(m=>m.name.startsWith('水牢2D_')&&m.name.includes(spec.wide?'宽矮':'人形'));const info=m.mesh.userData.billboardCheck;
   if(Math.abs(info.width-info.height)>1e-6)throw Error('Aspect ratio distorted');
   if(Math.abs(info.normalDot-1)>1e-6)throw Error('Billboard facing mismatch');
   if(info.planeViewDepth>=info.nearestTargetDepth)throw Error('Plane intersects target');
   checks.push({view:spec.name,frame:i,...info});
  }
 }
 window.waterBillboardShow(0);TextureAnimator.playAnimationFrame(0);
 for(const [name,angle]of [['front',0],['side',Math.PI/2],['back',Math.PI],['oblique',Math.PI/6]]){camera(angle,false);capture(root+'water_prison_billboard_'+name+'.png');}
 window.waterBillboardShow(1);camera(Math.PI/6,true);capture(root+'water_prison_billboard_wide.png');
 window.waterBillboardShow(0);camera(Math.PI/6,false);
 fs.writeFileSync(root+'render-validation.json',JSON.stringify({frames:checks.length,normalAlwaysFacesCamera:true,planeAlwaysInFrontOfTarget:true,checks},null,2));
 Canvas.scene.background=prev;TextureAnimator.start();
 return {savedFrames:checks.length,singleQuad:true,sourceTexture:'2D v3 unchanged',cameraFacingPass:true,depthPass:true};
})()