(async()=>{
 const elements=[],textures=[],groups=[];
 const water=Texture.all.find(t=>t.name==='water_prison_bubble_v3_strip.png');
 Undo.initEdit({elements,textures:[water],outliner:true});
 Project.texture_width=96;Project.texture_height=96;
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
function plane(g,wide){const m=new Mesh({name:wide?'水牢2D_宽矮_单平面':'水牢2D_人形_单平面',origin:[0,wide?11:19,5],vertices:{}}).addTo(g).init();elements.push(m);const vs=m.addVertices([-16,-16,0],[16,-16,0],[16,16,0],[-16,16,0]);const uv={};uv[vs[0]]=[0,96];uv[vs[1]]=[96,96];uv[vs[2]]=[96,0];uv[vs[3]]=[0,0];m.addFaces(new MeshFace(m,{vertices:vs,uv,texture:water.uuid}));return m;}
const p=plane(root,false),w=plane(wideGroup,true);wideGroup.visibility=false;wideGroup.children.forEach(e=>{e.visibility=false;});Canvas.updateAll();wideGroup.mesh.visible=false;wideGroup.children.forEach(e=>e.mesh.visible=false);Undo.finishEdit('水牢：原二维动画与当前镜头公告板平面');TextureAnimator.stop();return {textureFrames:water.frameCount,fps:water.fps,planes:[p,w].map(m=>({uuid:m.uuid,vertices:Object.keys(m.vertices).length,faces:Object.keys(m.faces).length}))};})()