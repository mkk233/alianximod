(() => {
 if(Project.uuid!=='9ef18951-91d4-889d-1f8e-c5f52e08573e')throw Error('Unexpected active project');
 const created=[];const heatTexture=Texture.all.find(t=>t.name==='heat_circle_v2');Undo.initEdit({elements:created,outliner:true,textures:[heatTexture]});
 const tex=n=>Texture.all.find(t=>t.name===n);
 const aux=new Group({name:'示意辅助_地面与玩家',origin:[0,0,0]}).init();
 const fx=new Group({name:'fx_heat_circle',origin:[0,0,0]}).init();
 function box(name,from,to,mat){
  const c=new Cube({name,from,to,origin:[0,0,0],box_uv:false});
  for(const f of Object.values(c.faces)){f.texture=mat.uuid;f.uv=[0,0,16,16];}
  c.addTo(aux).init();created.push(c);return c;
 }
 box('示意辅助_地面',[-96,-1,-96],[96,0,96],tex('aux_floor'));
 for(let q=-96;q<=96;q+=16){
  box('示意辅助_XZ格线X_'+q,[q-.08,.01,-96],[q+.08,.03,96],tex('aux_grid'));
  box('示意辅助_XZ格线Z_'+q,[-96,.01,q-.08],[96,.03,q+.08],tex('aux_grid'));
 }
 box('示意辅助_头',[-4,24,-4],[4,32,4],tex('aux_player'));
 box('示意辅助_躯干',[-4,12,-2],[4,24,2],tex('aux_player'));
 box('示意辅助_右臂',[-8,12,-2],[-4,24,2],tex('aux_player'));
 box('示意辅助_左臂',[4,12,-2],[8,24,2],tex('aux_player'));
 box('示意辅助_右腿',[-4,0,-2],[0,12,2],tex('aux_player'));
 box('示意辅助_左腿',[0,0,-2],[4,12,2],tex('aux_player'));
 const ring=new Mesh({name:'fx_heat_world_circle',origin:[0,0,0],vertices:{a:[-80,.8,80],b:[80,.8,80],c:[80,.8,-80],d:[-80,.8,-80]},faces:{}});
 ring.faces.surface=new MeshFace(ring,{vertices:['a','b','c','d'],texture:tex('heat_circle_v2').uuid,uv:{a:[0,0],b:[64,0],c:[64,64],d:[0,64]}});
 ring.addTo(fx).init();created.push(ring);
 const t=tex('heat_circle_v2');t.render_mode='default';t.fps=16/.6;t.currentFrame=8;
 Canvas.updateAll();TextureAnimator.update([t]);t.getOwnMaterial().uniforms.SHADE.value=false;t.getOwnMaterial().uniforms.EMISSIVE.value=false;
 Undo.finishEdit('热波水平正圆：实际XZ等比平面及辅助参照');
 return {project:Project.uuid,effectMeshes:1,planeModelUnits:[160,160],planeBlocks:[10,10],yOffsetBlocks:.05,unitScale:16,textureFrames:t.frameCount,center:[0,0,0],auxiliaryCount:created.length-1};
})()
