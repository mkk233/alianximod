(async()=>{
 const elements=[],textures=[],groups=[];
 const water=Texture.all.find(t=>t.name==='water_prison_3d_atlas.png');
 Undo.initEdit({elements,textures:[water],outliner:true});
 Project.texture_width=256;Project.texture_height=128;
 water.fps=10;water.frame_time=2;water.render_sides='double';water.force_translucent=true;water.updateMaterial();
 function solid(name,color){const c=document.createElement('canvas');c.width=c.height=16;const ctx=c.getContext('2d');ctx.fillStyle=color;ctx.fillRect(0,0,16,16);const t=new Texture({name,uv_width:16,uv_height:16}).fromDataURL(c.toDataURL()).add(false);textures.push(t);return t;}
 const grey=solid('占位_浅灰','#A9B7B7'),mid=solid('占位_灰蓝','#70858D'),dark=solid('占位_深灰','#334752'),light=solid('占位_亮灰','#CBD5CD');
 function group(name){const g=new Group({name}).init();groups.push(g);return g;}
 function cube(name,from,to,t,g){const c=new Cube({name,from,to,autouv:0}).addTo(g).init();for(const f of Object.values(c.faces)){f.texture=t.uuid;f.uv=[0,0,16,16];}elements.push(c);return c;}
 function dummy(g,wide){if(!wide){
 cube('头',[-4,26,-4],[4,34,4],grey,g);cube('躯干',[-4,14,-2],[4,26,2],mid,g);
 cube('左臂',[-7,14,-2],[-4.5,25,2],grey,g);cube('右臂',[4.5,14,-2],[7,25,2],mid,g);
 cube('左腿',[-3.8,4,-2],[-.5,14,2],grey,g);cube('右腿',[.5,4,-2],[3.8,14,2],mid,g);
 cube('左眼',[-2.4,29.6,4.01],[-1.5,30.5,4.1],dark,g);cube('右眼',[1.5,29.6,4.01],[2.4,30.5,4.1],dark,g);
 }else{
 cube('宽矮身体',[-10,6,-6],[10,14,6],mid,g);cube('宽矮头',[-5,13,-4],[5,20,4],grey,g);
 for(const x of [-7,4])for(const z of [-5,3])cube('宽矮腿',[x,2,z],[x+3,6,z+2],grey,g);
 cube('宽矮左眼',[-2.8,16.4,4.01],[-1.6,17.5,4.1],dark,g);cube('宽矮右眼',[1.6,16.4,4.01],[2.8,17.5,4.1],dark,g);
 }}
 const root=group('01_人形目标_常规比例');dummy(root,false);
 const wideGroup=group('02_宽矮目标_三轴适配');dummy(wideGroup,true);
 function createWater(g,radii,cy){
  const mesh=new Mesh({name:'水牢_闭合椭球与三道错层水流',vertices:{}}).addTo(g).init();elements.push(mesh);
  const rx=radii[0],ry=radii[1],rz=radii[2];
  const shellKeys=[],ribbonKeys=[];
  const v=point=>mesh.addVertices(point)[0];
  function pt(theta,lon,s=1){return [rx*Math.sin(theta)*Math.sin(lon)*s,cy+ry*Math.cos(theta)*s,rz*Math.sin(theta)*Math.cos(lon)*s];}
  function face(keys,uvs,part){const a=mesh.vertices[keys[0]],b=mesh.vertices[keys[1]],c=mesh.vertices[keys[2]];const ab=b.map((x,i)=>x-a[i]),ac=c.map((x,i)=>x-a[i]);const n=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]];const cen=keys.map(k=>mesh.vertices[k]).reduce((sum,p)=>sum.map((x,i)=>x+p[i]),[0,0,0]).map(x=>x/keys.length);const dot=n[0]*cen[0]/rx**2+n[1]*(cen[1]-cy)/ry**2+n[2]*cen[2]/rz**2;if(dot<0){keys.reverse();uvs.reverse();}const uv={};keys.forEach((k,i)=>uv[k]=uvs[i]);const fk=mesh.addFaces(new MeshFace(mesh,{vertices:keys,uv,texture:water.uuid}))[0];(part==='shell'?shellKeys:ribbonKeys).push(fk);}
  const rings=[];const seg=24,lat=12;
  const top=v([0,cy+ry,0]),bottom=v([0,cy-ry,0]);
  for(let j=1;j<lat;j++){const ring=[];for(let i=0;i<seg;i++)ring.push(v(pt(Math.PI*j/lat,2*Math.PI*i/seg)));rings.push(ring);}
  for(let i=0;i<seg;i++){const ni=(i+1)%seg,u0=i*256/seg,u1=(i+1)*256/seg;
   face([top,rings[0][i],rings[0][ni]],[[(u0+u1)/2,0],[u0,8],[u1,8]],'shell');
   for(let j=0;j<rings.length-1;j++)face([rings[j][i],rings[j+1][i],rings[j+1][ni],rings[j][ni]],[[u0,(j+1)*8],[u0,(j+2)*8],[u1,(j+2)*8],[u1,(j+1)*8]],'shell');
   face([rings.at(-1)[i],bottom,rings.at(-1)[ni]],[[u0,88],[(u0+u1)/2,96],[u1,88]],'shell');
  }
  const rad=Math.PI/180;
  function pixelNode(x,y,w){const nx=(x-47.5)/39.5,ny=(47.5-y)/41.5;const nz=Math.sqrt(Math.max(.001,1-nx*nx-ny*ny));return [Math.atan2(nx,nz),Math.asin(ny),w];}
  const a=[[ -127*rad,-.31,.12],[-103*rad,-.35,1],pixelNode(13,62,2),pixelNode(29,60,6),pixelNode(48,52,5),pixelNode(69,43,3),pixelNode(83,41,1),[103*rad,.15,.8],[128*rad,.19,.08]];
  const b=[[-103*rad,.27,.12],pixelNode(12,36,2.5),pixelNode(18,37,4),pixelNode(25,39,5),pixelNode(32,46,3),pixelNode(39,49,.08)];
  const c=[pixelNode(34,74,.1),pixelNode(48,69,3),pixelNode(61,66,4),pixelNode(72,59,.1)];
  function ribbon(nodes){
   const points=[];
   for(let k=0;k<nodes.length-1;k++)for(let q=0;q<8;q++){
    const t=q/8,t2=t*t,t3=t2*t,p0=nodes[Math.max(0,k-1)],p1=nodes[k],p2=nodes[k+1],p3=nodes[Math.min(nodes.length-1,k+2)];
    function cr(n){return .5*(2*p1[n]+(-p0[n]+p2[n])*t+(2*p0[n]-5*p1[n]+4*p2[n]-p3[n])*t2+(-p0[n]+3*p1[n]-3*p2[n]+p3[n])*t3);}
    points.push([cr(0),cr(1),Math.max(.08,p1[2]+(p2[2]-p1[2])*(t*t*(3-2*t)))]);
   }points.push(nodes.at(-1));const pairs=[];
   for(let i=0;i<points.length;i++){
    const p=points[i],prev=points[Math.max(0,i-1)],next=points[Math.min(points.length-1,i+1)];
    const du=(next[0]-prev[0])*Math.cos(p[1]),dv=next[1]-prev[1],len=Math.hypot(du,dv)||1;
    const half=p[2]/41.5*.5;const dlon=-dv/len*half/Math.max(.15,Math.cos(p[1])),dlat=du/len*half;
    pairs.push([v(pt(Math.PI/2-(p[1]+dlat),p[0]+dlon,1.012)),v(pt(Math.PI/2-(p[1]-dlat),p[0]-dlon,1.012))]);
   }
   for(let i=0;i<pairs.length-1;i++)face([pairs[i][0],pairs[i][1],pairs[i+1][1],pairs[i+1][0]],[[256*i/(pairs.length-1),96],[256*i/(pairs.length-1),128],[256*(i+1)/(pairs.length-1),128],[256*(i+1)/(pairs.length-1),96]],'ribbon');
  }
  ribbon(a);ribbon(b);ribbon(c);
  mesh.prisonShellFaceKeys=shellKeys;mesh.prisonRibbonFaceKeys=ribbonKeys;
  return mesh;
 }
 const mainWater=createWater(root,[13.5,20,12.5],20);
 const wideWater=createWater(wideGroup,[20,14,16],14);
 wideGroup.visibility=false;
 Canvas.updateAll();
 for(const m of [mainWater,wideWater]){m.mesh.renderOrder=1;}
 Undo.finishEdit('水牢v3：闭合椭球与贴壳斜向束缚水流');
 TextureAnimator.stop();TextureAnimator.playAnimationFrame(0);
 const mat=water.getOwnMaterial();mat.depthWrite=false;mat.depthTest=true;mat.transparent=true;
 if(mat.uniforms.SHADE)mat.uniforms.SHADE.value=false;
 Project.prisonValidation={main:root.uuid,wide:wideGroup.uuid,mainWater:mainWater.uuid,wideWater:wideWater.uuid};
 return {groups:groups.map(g=>({name:g.name,uuid:g.uuid})),waterMeshes:[mainWater,wideWater].map(m=>({uuid:m.uuid,vertices:Object.keys(m.vertices).length,faces:Object.keys(m.faces).length,shell:m.prisonShellFaceKeys.length,ribbons:m.prisonRibbonFaceKeys.length,geometry:{indexed:!!m.mesh.geometry.index,vertices:m.mesh.geometry.attributes.position.count,groups:m.mesh.geometry.groups}})),texture:{frameCount:water.frameCount,fps:water.fps,displayHeight:water.display_height,uv:[water.getUVWidth(),water.getUVHeight()]}}
})()