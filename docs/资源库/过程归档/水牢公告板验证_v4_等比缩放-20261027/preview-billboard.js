(()=>{
 const texture=Texture.all.find(t=>t.name==='water_prison_bubble_v3_strip.png');
 texture.fps=10;const mat=texture.getOwnMaterial();mat.depthTest=true;mat.depthWrite=false;mat.transparent=true;mat.forceSinglePass=true;mat.uniforms.SHADE.value=false;
 const planes=Mesh.all.filter(m=>m.name.startsWith('水牢2D_'));
 window.waterBillboardChecks=[];
 for(const m of planes){
  const wide=m.name.includes('宽矮');const low=wide?[-10,2,-6]:[-7,4,-4];const high=wide?[10,20,6]:[7,34,4.1];
  const cornerVectors=[];for(const x of [low[0],high[0]])for(const y of [low[1],high[1]])for(const z of [low[2],high[2]])cornerVectors.push(new THREE.Vector3(x,y,z));
  m.mesh.frustumCulled=false;m.mesh.renderOrder=1;
  m.mesh.onBeforeRender=function(renderer,scene,camera){
   camera.updateMatrixWorld(true);let minX=Infinity,maxX=-Infinity,minY=Infinity,maxY=-Infinity,nearZ=-Infinity;
   for(const corner of cornerVectors){const v=corner.clone().applyMatrix4(camera.matrixWorldInverse);nearZ=Math.max(nearZ,v.z);const ndc=corner.clone().project(camera);minX=Math.min(minX,ndc.x);maxX=Math.max(maxX,ndc.x);minY=Math.min(minY,ndc.y);maxY=Math.max(maxY,ndc.y);}
   const d=Math.max(camera.near+.05,-nearZ-.08),projection=camera.projectionMatrix.elements;
   const sx=camera.isPerspectiveCamera?d/projection[0]:1/projection[0],sy=camera.isPerspectiveCamera?d/projection[5]:1/projection[5];
   const ndcX=(minX+maxX)/2,ndcY=(minY+maxY)/2;
   const local=new THREE.Vector3(ndcX*sx,ndcY*sy,-d);
   if(!camera.isPerspectiveCamera){local.x-=projection[12]/projection[0];local.y-=projection[13]/projection[5];}
   this.position.copy(local.applyMatrix4(camera.matrixWorld));
   this.quaternion.copy(camera.getWorldQuaternion(new THREE.Quaternion()));
   const bodyW=(maxX-minX)*sx,bodyH=(maxY-minY)*sy;
   const size=Math.hypot(bodyW*96/70,bodyH*96/74)*1.04;
   const width=size,height=size;
   this.scale.set(size/32,size/32,1);this.updateMatrixWorld(true);
   const n=new THREE.Vector3(0,0,1).applyQuaternion(this.quaternion),cn=new THREE.Vector3(0,0,1).applyQuaternion(camera.quaternion);
   this.userData.billboardCheck={camera:camera.uuid,normalDot:n.dot(cn),planeViewDepth:d,nearestTargetDepth:-nearZ,width,height};
  };
 }
 window.waterBillboardShow=function(which){for(const g of Group.all){const isWide=g.name.includes('宽矮'),visible=which===1?isWide:!isWide;g.mesh.visible=visible;g.children.forEach(e=>{if(e.mesh)e.mesh.visible=visible;});}};
 window.waterBillboardShow(0);Canvas.scene.background=new THREE.Color('#172D38');TextureAnimator.start();
 return {planes:planes.length,billboard:'per-render-camera quaternion',source:'unchanged 2D v3, 16 x 100ms',depthTest:true,depthWrite:false};
})()