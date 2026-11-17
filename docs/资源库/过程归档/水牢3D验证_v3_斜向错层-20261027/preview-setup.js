(()=>{
 const meshes=Mesh.all.filter(m=>m.name.startsWith('水牢_'));
 const tex=Texture.all.find(t=>t.name==='water_prison_3d_atlas.png');
 const mat=tex.getOwnMaterial();
 mat.depthWrite=false;mat.depthTest=true;mat.transparent=true;mat.forceSinglePass=true;
 mat.uniforms.SHADE.value=false;
 for(const m of meshes){
  const geo=m.mesh.geometry;
  const original=Array.from(geo.index.array);
  const triangles=[];for(let i=0;i<original.length;i+=3)triangles.push(original.slice(i,i+3));
  m.mesh.onBeforeRender=function(renderer,scene,camera,geometry){
   this.updateMatrixWorld(true);camera.updateMatrixWorld(true);
   const mm=new THREE.Matrix4().multiplyMatrices(camera.matrixWorldInverse,this.matrixWorld);
   const pos=geometry.attributes.position.array;
   const sorted=triangles.map(ids=>{let z=0;for(const id of ids){const v=new THREE.Vector3(pos[id*3],pos[id*3+1],pos[id*3+2]).applyMatrix4(mm);z+=v.z;}return {ids,z:z/3};}).sort((a,b)=>a.z-b.z);
   let j=0;for(const tri of sorted)for(const id of tri.ids)geometry.index.array[j++]=id;
   geometry.index.needsUpdate=true;
  };
 }
 const names=['01_人形目标_常规比例','02_宽矮目标_三轴适配'];
 window.waterPrisonShow=function(which){for(let i=0;i<names.length;i++){const g=Group.all.find(g=>g.name===names[i]);const visible=which===i||which===2;g.mesh.visible=visible;g.children.forEach(e=>{if(e.mesh)e.mesh.visible=visible;});}};
 window.waterPrisonShow(0);
 return {waterMeshes:meshes.length,material:{depthWrite:mat.depthWrite,depthTest:mat.depthTest,transparent:mat.transparent},sorting:'native preview triangles back to front per camera'};
})();
(()=>{
 const water=Texture.all.find(t=>t.name==='water_prison_3d_atlas.png');
 const mat=water.getOwnMaterial();
 if(!mat.userData.wpBaseVertex){mat.userData.wpBaseVertex=mat.vertexShader;mat.userData.wpBaseFragment=mat.fragmentShader;}
 mat.vertexShader=mat.userData.wpBaseVertex.replace('attribute float highlight;','attribute float highlight; attribute float wpShell; varying float wpShellMask; varying vec3 wpNormal; varying vec3 wpView;').replace('vec4 mvPosition=modelViewMatrix*vec4(position,1.0);','vec4 mvPosition=modelViewMatrix*vec4(position,1.0);wpShellMask=wpShell;wpNormal=normalize(normalMatrix*normal);wpView=-mvPosition.xyz;');
 const pressure='vec4 color=texture2D(map,vUv);if(wpShellMask>0.5){float q=abs(dot(normalize(wpNormal),normalize(wpView)));if(q<=0.25){color.rgb=vec3(0.055,0.39,0.58);color.a=max(color.a,0.69);}else if(q<=0.55){color.rgb=vec3(0.16,0.65,0.77);color.a=max(color.a,0.43);}}';
 mat.fragmentShader=mat.userData.wpBaseFragment.replace('uniform sampler2D map;','varying float wpShellMask; varying vec3 wpNormal; varying vec3 wpView; uniform sampler2D map;').replace('vec4 color=texture2D(map,vUv);',pressure);
 mat.depthWrite=false;mat.depthTest=true;mat.transparent=true;mat.forceSinglePass=true;mat.uniforms.SHADE.value=false;mat.needsUpdate=true;
 for(const m of Mesh.all.filter(m=>m.name.startsWith('水牢_'))){
  const geo=m.mesh.geometry;const wide=m.parent.name.includes('宽矮');const r=wide?[20,14,16]:[13.5,20,12.5];const cy=wide?14:20;
  const mask=[];let k=0;
  for(const face of Object.values(m.faces)){
   const av=face.vertices.reduce((sum,key)=>sum+face.uv[key][1],0)/face.vertices.length;
   for(const key of face.vertices){const p=m.vertices[key];mask.push(av<70?1:0);if(av<96){const n=new THREE.Vector3(p[0]/(r[0]*r[0]),(p[1]-cy)/(r[1]*r[1]),p[2]/(r[2]*r[2])).normalize();geo.attributes.normal.setXYZ(k,n.x,n.y,n.z);}k++;}
  }
  geo.setAttribute('wpShell',new THREE.Float32BufferAttribute(mask,1));geo.attributes.normal.needsUpdate=true;
 }
 return {singleShell:true,edgeAlpha:[35,110,176],nativePreviewShader:true};
})();
(()=>{const p=Preview.selected;p.setNormalCamera();p.camPers.position.set(51.5,35,89.2);p.controls.target.set(0,20,0);p.camPers.fov=35;p.camPers.updateProjectionMatrix();p.controls.update();Canvas.scene.background=new THREE.Color('#172D38');TextureAnimator.start();return {playing:true};})()