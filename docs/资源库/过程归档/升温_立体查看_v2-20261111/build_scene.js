(async()=>{
const fs=require("fs");
const out="D:/alianximod/docs/资源库/过程归档/升温_立体查看_v2-20261111";
const source="D:/alianximod/src/main/resources/assets/xueximod/textures/effect/furnace_v2";
const previous={name:Project.name,uuid:Project.uuid};
if(Project.name==="heat-up-v2-spatial-preview"&&Outliner.elements.length===0){
  if(Undo.current_save)Undo.finishEdit("升温预览空场景初始化");
}else newProject("free");
Project.name="heat-up-v2-spatial-preview";
Project.texture_width=96; Project.texture_height=112;
const elements=[],textures=[],groups=[],cards=[];
Undo.initEdit({elements,textures,outliner:true});
function group(name,origin,parent){
  const g=new Group({name,origin});
  if(parent)g.addTo(parent);
  g.init();groups.push(g);return g;
}
async function texture(name,url,uw,uh){
  const t=new Texture({name,uv_width:uw,uv_height:uh});
  t.source=url;t.internal=true;t.add(false);
  await new Promise((resolve,reject)=>{
    const timeout=setTimeout(()=>reject(new Error("Texture load timeout: "+name)),8000);
    t.load(()=>{clearTimeout(timeout);resolve();});
  });
  t.render_sides="double";t.updateMaterial();textures.push(t);
  return t;
}
async function solid(name,color){
  const c=document.createElement("canvas");c.width=c.height=16;
  const ctx=c.getContext("2d");ctx.fillStyle=color;ctx.fillRect(0,0,16,16);
  return texture(name,c.toDataURL(),16,16);
}
const palette={};
for(const [name,color] of Object.entries({body:"#617786",skin:"#a0b0b6",pants:"#344651",dark:"#202c35",ground:"#303e49",tile:"#374752"})){
  palette[name]=await solid("辅助_"+name,color);
}
const sheets={loop:{},entry:{}};
for(const phase of ["loop","entry"]){
  for(const layer of ["rear","rear_tongues","front_tongues","embers"]){
    const stem="heat_"+(phase==="entry"?"entry_":"")+layer;
    const url="data:image/png;base64,"+fs.readFileSync(source+"/"+stem+".png").toString("base64");
    const t=await texture(stem,url,96,112);
    t.frame_time=1.6;t.frame_order_type="loop";
    t.material.depthWrite=false;
    t.material.transparent=true;
    t.material.alphaTest=0.01;
    sheets[phase][layer]=t;
  }
}
function cube(name,from,to,t,parent){
  const c=new Cube({name,from,to,autouv:0}).addTo(parent).init();
  for(const f of Object.values(c.faces)){f.texture=t.uuid;f.uv=[0,0,16,16];}
  elements.push(c);return c;
}
const root=group("示意辅助_玩家与升温挂载",[0,0,0]);
const bones={
 body:group("躯干",[0,24,0],root),
 head:group("头部",[0,24,0],root),
 right_arm:group("右臂",[-5,22,0],root),
 left_arm:group("左臂",[5,22,0],root),
 right_leg:group("右腿",[-1.9,12,0],root),
 left_leg:group("左腿",[1.9,12,0],root)
};
cube("辅助_头",[-4,24,-4],[4,32,4],palette.skin,bones.head);
cube("辅助_左眼",[-2.5,28,4.01],[-1,29.3,4.03],palette.dark,bones.head);
cube("辅助_右眼",[1,28,4.01],[2.5,29.3,4.03],palette.dark,bones.head);
cube("辅助_躯干",[-4,12,-2],[4,24,2],palette.body,bones.body);
cube("辅助_右臂",[-8,15,-2],[-4,24,2],palette.body,bones.right_arm);
cube("辅助_左臂",[4,15,-2],[8,24,2],palette.body,bones.left_arm);
cube("辅助_右手",[-8,12,-2],[-4,15,2],palette.skin,bones.right_arm);
cube("辅助_左手",[4,12,-2],[8,15,2],palette.skin,bones.left_arm);
cube("辅助_右腿",[-3.9,0,-2],[0.1,12,2],palette.pants,bones.right_leg);
cube("辅助_左腿",[-0.1,0,-2],[3.9,12,2],palette.pants,bones.left_leg);
const stage=group("示意辅助_地面",[0,0,0]);
cube("辅助_地台",[-26,-1,-22],[26,-0.05,22],palette.ground,stage);
for(let x=-24;x<24;x+=8)for(let z=-20;z<20;z+=8){
  if(((x+24)/8+(z+20)/8)%2===0)cube("辅助_地面格",[x,-0.04,z],[x+8,0,z+8],palette.tile,stage);
}
function plane(name,window,layer,bone,axis,depth,centerX=0){
  const [l,t,r,b,ax,ay]=window;
  const pivot=bones[bone].origin;
  const left=centerX+(l-ax)/3,right=centerX+(r+1-ax)/3;
  const top=pivot[1]-(t-ay)/3,bottom=pivot[1]-(b+1-ay)/3;
  let from,to,faces;
  if(axis==="z"){
    const zz=-depth;
    from=[pivot[0]+left,bottom,zz-0.004];to=[pivot[0]+right,top,zz+0.004];
    faces=["north","south"];
  }else{
    const dh=Math.min((r+1-l)/6,3.2);
    from=[pivot[0]+depth-0.004,bottom,-dh];to=[pivot[0]+depth+0.004,top,dh];
    faces=["east","west"];
  }
  const c=new Cube({name,from,to,autouv:0,shade:false}).addTo(bones[bone]).init();
  for(const [side,f] of Object.entries(c.faces)){
    f.texture=faces.includes(side)?sheets.loop[layer].uuid:null;
    f.uv=[l,t,r+1,b+1];
  }
  elements.push(c);cards.push({cube:c,layer,faces,window});
}
plane("背后_热边",[22,38,73,95,48,40],"rear","body","z",2.55);
plane("躯干_前侧热流",[36,42,60,93,48,40],"front_tongues","body","z",-2.7);
plane("余烬_前",[23,31,72,96,48,40],"embers","body","z",-2.9);
plane("余烬_后",[23,31,72,96,48,40],"embers","body","z",2.75);
for(const [bone,center,outer,rear,front] of [
 ["right_arm",-1,-3.55,[25,49,34,71,31,40],[28,46,35,70,31,40]],
 ["left_arm",1,3.55,[62,49,71,71,65,40],[61,46,67,70,65,40]]
]){
 for(const [layer,w] of [["rear_tongues",rear],["front_tongues",front]]){
  plane(bone+"_"+layer+"_前",w,layer,bone,"z",-2.6,center);
  plane(bone+"_"+layer+"_后",w,layer,bone,"z",2.6,center);
  plane(bone+"_"+layer+"_外",w,layer,bone,"x",outer);
 }
}
for(const [bone,outer,w] of [
 ["right_leg",-2.7,[25,72,47,98,42,70]],
 ["left_leg",2.7,[48,72,71,98,54,70]]
]){
 plane(bone+"_后热流",w,"rear_tongues",bone,"z",2.6);
 plane(bone+"_外热流",w,"rear_tongues",bone,"x",outer);
}
Canvas.updateAll();
Undo.finishEdit("升温v2：按现行分层素材与挂点建立空间示意");
TextureAnimator.stop();
window.heatPreview={out,root,bones,cards,sheets,project:Project.uuid,previous};
window.heatPreview.setFrame=function(phase,index){
  for(const c of cards){
    const tex=sheets[phase][c.layer];
    for(const f of c.faces)c.cube.faces[f].texture=tex.uuid;
    c.cube.preview_controller.updateFaces(c.cube);
  }
  for(const t of Object.values(sheets[phase]))t.currentFrame=index;
  TextureAnimator.update(Object.values(sheets[phase]));
};
window.heatPreview.setFrame("loop",6);
Project.save_path=out+"/heat_up_v2_spatial.bbmodel";
fs.writeFileSync(Project.save_path,Codecs.project.compile());
Project.saved=true;
return {name:Project.name,previous,elements:elements.length,cards:cards.length,textures:textures.map(t=>({name:t.name,w:t.width,h:t.height,frames:t.frameCount})),saved:Project.save_path};
})()
