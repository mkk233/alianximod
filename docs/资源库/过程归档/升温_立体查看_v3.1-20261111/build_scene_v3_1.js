(async()=>{
const fs=require("fs");
const out="D:/alianximod/docs/资源库/过程归档/升温_立体查看_v3.1-20261111";
const previous={name:Project?.name,uuid:Project?.uuid};
newProject("free");
Project.name="heat-up-v3.1-spatial-preview";
Project.texture_width=96;Project.texture_height=112;
const elements=[],textures=[],cards=[],groups=[];
Undo.initEdit({elements,textures,outliner:true});
function group(name,origin,parent){
  const g=new Group({name,origin});if(parent)g.addTo(parent);g.init();groups.push(g);return g;
}
async function texture(name,url,uw,uh,animated=false){
  const t=new Texture({name,uv_width:uw,uv_height:uh});
  t.source=url;t.internal=true;t.add(false);
  await new Promise((resolve,reject)=>{
    const timer=setTimeout(()=>reject(new Error("Texture load timeout: "+name)),8000);
    t.load(()=>{clearTimeout(timer);resolve();});
  });
  if(animated){
    t.render_sides="double";t.frame_time=name.startsWith("entry")?1.2:1.6;
    t.frame_order_type="loop";t.updateMaterial();
    t.material.depthWrite=false;t.material.transparent=true;t.material.alphaTest=0.01;
  }
  textures.push(t);return t;
}
async function solid(name,color){
  const c=document.createElement("canvas");c.width=c.height=16;
  const ctx=c.getContext("2d");ctx.fillStyle=color;ctx.fillRect(0,0,16,16);
  return texture("辅助_"+name,c.toDataURL(),16,16);
}
const palette={};
for(const [name,color]of Object.entries({
  body:"#445767",skin:"#788e9b",pants:"#334452",dark:"#23333f",
  ground:"#293945",tile:"#31434e"
}))palette[name]=await solid(name,color);
const sheets={loop:{},entry:{}};
for(const phase of ["loop","entry"])for(const layer of [
  "warmth","hand_rear","leg_rear","torso","hand_front","leg_front","embers"
]){
  const name=phase+"_"+layer;
  const url="data:image/png;base64,"+fs.readFileSync(out+"/textures/"+name+".png").toString("base64");
  sheets[phase][layer]=await texture(name,url,96,112,true);
}
function cube(name,from,to,t,parent){
  const c=new Cube({name,from,to,autouv:0}).addTo(parent).init();
  for(const f of Object.values(c.faces)){f.texture=t.uuid;f.uv=[0,0,16,16];}
  elements.push(c);return c;
}
const root=group("示意辅助_玩家与v3.1分部位挂载",[0,0,0]);
const bones={
  body:group("躯干_短热纹",[0,24,0],root),
  head:group("头部_辅助",[0,24,0],root),
  right_arm:group("右臂_短手焰",[-5,22,0],root),
  left_arm:group("左臂_短手焰",[5,22,0],root),
  right_leg:group("右腿_长腿焰",[-2,12,0],root),
  left_leg:group("左腿_长腿焰",[2,12,0],root)
};
cube("辅助_头",[-4,24,-4],[4,32,4],palette.skin,bones.head);
cube("辅助_左眼",[-2.3,28,4.015],[-1.3,29,4.025],palette.dark,bones.head);
cube("辅助_右眼",[1.3,28,4.015],[2.3,29,4.025],palette.dark,bones.head);
cube("辅助_躯干",[-4,12,-2],[4,24,2],palette.body,bones.body);
cube("辅助_右袖",[-8,15,-2],[-4,24,2],palette.body,bones.right_arm);
cube("辅助_左袖",[4,15,-2],[8,24,2],palette.body,bones.left_arm);
cube("辅助_右手",[-8,12,-2],[-4,15,2],palette.skin,bones.right_arm);
cube("辅助_左手",[4,12,-2],[8,15,2],palette.skin,bones.left_arm);
cube("辅助_右腿",[-4,0,-2],[-0.333,12,2],palette.pants,bones.right_leg);
cube("辅助_左腿",[0.333,0,-2],[4,12,2],palette.pants,bones.left_leg);
const stage=group("示意辅助_地面格",[0,0,0]);
cube("辅助_地台",[-26,-1,-22],[26,-0.06,22],palette.ground,stage);
for(let x=-24;x<24;x+=8)for(let z=-20;z<20;z+=8){
  if(((x+24)/8+(z+20)/8)%2===0)cube("辅助_地面格",[x,-0.05,z],[x+8,-0.04,z+8],palette.tile,stage);
}
function card(name,rect,layer,bone,axis,depth,anchor){
  const [l,t,r,b]=rect;
  let from,to,face;
  if(axis==="z"){
    from=[(l-48)/3,(104-(b+1))/3,depth];
    to=[(r+1-48)/3,(104-t)/3,depth];
    face=depth>0?"south":"north";
  }else{
    from=[depth,(104-(b+1))/3,(l-anchor)/3];
    to=[depth,(104-t)/3,(r+1-anchor)/3];
    face=depth>0?"east":"west";
  }
  const c=new Cube({name,from,to,autouv:0,shade:false}).addTo(bones[bone]).init();
  for(const [side,f]of Object.entries(c.faces)){
    f.texture=side===face?sheets.loop[layer].uuid:null;
    f.uv=[l,t,r+1,b+1];
  }
  elements.push(c);cards.push({cube:c,name,layer,bone,axis,depth,rect,anchor,face});
  return c;
}
for(const a of [
  {bone:"right_arm",rect:[20,46,38,73],side:[20,46,29,73],outer:-8.16,anchor:24,embers:[18,40,30,75]},
  {bone:"left_arm",rect:[57,46,76,73],side:[66,46,75,73],outer:8.16,anchor:71,embers:[66,40,80,75]}
]){
  card(a.bone+"_后短焰",a.rect,"hand_rear",a.bone,"z",-2.18);
  card(a.bone+"_前掌焰",a.rect,"hand_front",a.bone,"z",2.18);
  card(a.bone+"_外侧短焰",a.side,"hand_rear",a.bone,"x",a.outer,a.anchor);
  card(a.bone+"_掌后暖边",a.rect,"warmth",a.bone,"z",-2.12);
  card(a.bone+"_前余烬",a.embers,"embers",a.bone,"z",2.42);
  card(a.bone+"_后余烬",a.embers,"embers",a.bone,"z",-2.42);
}
for(const a of [
  {bone:"right_leg",rect:[30,69,47,107],side:[30,69,38,107],outer:-4.16,anchor:34,embers:[29,68,44,108]},
  {bone:"left_leg",rect:[48,69,65,107],side:[57,69,65,107],outer:4.16,anchor:61,embers:[51,68,67,108]}
]){
  card(a.bone+"_后长焰",a.rect,"leg_rear",a.bone,"z",-2.18);
  card(a.bone+"_前长焰",a.rect,"leg_front",a.bone,"z",2.18);
  card(a.bone+"_外侧长焰",a.side,"leg_rear",a.bone,"x",a.outer,a.anchor);
  card(a.bone+"_足后暖边",a.rect,"warmth",a.bone,"z",-2.12);
  card(a.bone+"_前余烬",a.embers,"embers",a.bone,"z",2.42);
  card(a.bone+"_后余烬",a.embers,"embers",a.bone,"z",-2.42);
}
card("胸腹_少量热纹",[36,40,59,69],"torso","body","z",2.16);
card("背部_同源少量热纹",[36,40,59,69],"torso","body","z",-2.16);
card("右肋_腰侧热纹",[35,55,39,69],"torso","body","x",-4.10,37);
card("左肋_腰侧热纹",[56,55,60,69],"torso","body","x",4.10,58);
Canvas.updateAll();
Undo.finishEdit("升温v3.1：同源短手焰、长腿焰与少量胸腹热纹空间示意");
TextureAnimator.stop();
const p={out,root,bones,cards,sheets,project:Project.uuid,previous};
p.setFrame=function(phase,index){
  if(Project.uuid!==p.project)throw new Error("Heat preview project is not active");
  for(const c of cards){
    c.cube.faces[c.face].texture=sheets[phase][c.layer].uuid;
    c.cube.preview_controller.updateFaces(c.cube);
  }
  for(const t of Object.values(sheets[phase]))t.currentFrame=index;
  TextureAnimator.update(Object.values(sheets[phase]));
};
window.heatPreviewV31=p;
p.setFrame("loop",6);
Project.save_path=out+"/heat_up_v3_1_spatial.bbmodel";
fs.writeFileSync(Project.save_path,Codecs.project.compile());
Project.saved=true;
fs.writeFileSync(out+"/placement.json",JSON.stringify({
  version:"3.1",source:"../升温_躯干少量热纹_v3.1-20261111/heat_up_v3_1.aseprite",
  pixelToModel:1/3,textureOriginPixel:[48,104],frontDirection:"+Z",
  mannequinHeightModel:32,modelUnitToBlock:"not calibrated by a game test",
  loopFrameMs:80,entryFrameMs:60,
  rearTorso:"Back preview reuses the approved torso layer; no separately drawn back texture.",
  cards:cards.map(c=>({name:c.name,layer:c.layer,bone:c.bone,axis:c.axis,depth:c.depth,
    rect:c.rect,anchor:c.anchor,from:c.cube.from,to:c.cube.to,face:c.face}))
},null,2));
return {name:Project.name,project:Project.uuid,previous,cards:cards.length,
  elements:elements.length,textures:textures.length,
  loopFrames:sheets.loop.torso.frameCount,entryFrames:sheets.entry.torso.frameCount,
  model:Project.save_path};
})()
