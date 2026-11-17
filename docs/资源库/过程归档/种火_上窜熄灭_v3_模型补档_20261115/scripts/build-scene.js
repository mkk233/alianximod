(async()=>{
const fs=require("fs");
const dir="D:/alianximod/docs/资源库/过程归档/种火_上窜熄灭_v3_模型补档_20261115";
if(ModelProject.all.length) throw new Error("Unexpected existing project; stop");
newProject(Formats.free);
Project.name="seed_sow_v3_preview";
Project.texture_width=64; Project.texture_height=64; Project.box_uv=false;
const elements=[], groups=[], textures=[];
Undo.initEdit({elements,groups,textures});
function group(name,parent,origin=[0,0,0]){const g=new Group({name,origin});g.addTo(parent||"root").init();groups.push(g);return g;}
function texture(name,url,w=64,h=64){const t=new Texture({name,render_mode:"default",render_sides:"double",uv_width:w,uv_height:h});t.fromDataURL(url).add(false);textures.push(t);return t;}
function solid(name,css){const c=document.createElement("canvas");c.width=c.height=16;const ctx=c.getContext("2d");ctx.fillStyle=css;ctx.fillRect(0,0,16,16);return texture(name,c.toDataURL(),16,16);}
function cube(name,from,to,tex,parent){const c=new Cube({name,from,to,box_uv:false,autouv:0,shade:false});for(const f of Object.values(c.faces)){f.texture=tex.uuid;f.uv=[0,0,tex.uv_width,tex.uv_height];}c.addTo(parent).init();elements.push(c);return c;}
function plate(name,from,to,tex,parent){const c=cube(name,from,to,tex,parent);["east","west","up","down"].forEach(k=>c.faces[k].texture=null);c.faces.north.uv=[tex.uv_width,0,0,tex.uv_height];return c;}
function label(name,text,cx,cy,cz,width,height,parent,color="#d9e6ef"){const canvas=document.createElement("canvas");canvas.width=1024;canvas.height=128;const ctx=canvas.getContext("2d");ctx.fillStyle="#19212b";ctx.fillRect(0,0,1024,128);ctx.font="500 44px Segoe UI";ctx.textAlign="center";ctx.textBaseline="middle";ctx.fillStyle=color;ctx.fillText(text,512,64,990);const t=texture("AUX_LABEL_"+name,canvas.toDataURL(),1024,128);return plate("AUX_LABEL_"+name,[cx-width/2,cy-height/2,cz],[cx+width/2,cy+height/2,cz+.01],t,parent);}
const mat={ground:solid("AUX_ground","#202a36"),grid:solid("AUX_grid","#364555"),body:solid("AUX_mannequin_translucent","rgba(120,148,170,0.20)"),edge:solid("AUX_mannequin_edges","#61798e"),cyan:solid("AUX_reference_measure","#44b7c2"),amber:solid("AUX_offset_measure","#e0b86e"),dark:solid("AUX_backdrop","#19212b")};
const frames=[];
for(let i=1;i<=16;i++){const file=dir+"/frames/frame_"+String(i).padStart(2,"0")+".png";frames.push(texture("v3_FRAME_"+String(i).padStart(2,"0")+"_50ms","data:image/png;base64,"+fs.readFileSync(file).toString("base64")));}
await Promise.all(textures.map(t=>new Promise(r=>{if(t.img.complete&&t.img.naturalWidth)r();else{t.img.addEventListener("load",r,{once:true});setTimeout(r,1000);}})));
function outline(name,a,b,tex,parent,w=.055){for(let axis=0;axis<3;axis++){const other=[0,1,2].filter(v=>v!==axis);for(let j=0;j<4;j++){const p=a.slice(),q=a.slice();p[axis]=a[axis];q[axis]=b[axis];other.forEach((k,i)=>{p[k]=q[k]=(j>>i&1)?b[k]:a[k];p[k]-=w/2;q[k]+=w/2;});cube(name+"_"+axis+"_"+j,p,q,tex,parent);}}}
function mannequin(x,parent){const g=group("AUX_MANNEQUIN_1.8_blocks",parent);const parts=[["head",[-3.6,21.6,-3.6],[3.6,28.8,3.6]],["body",[-3.6,10.8,-1.8],[3.6,21.6,1.8]],["arm_L",[-7.2,10.8,-1.8],[-3.6,21.6,1.8]],["arm_R",[3.6,10.8,-1.8],[7.2,21.6,1.8]],["leg_L",[-3.6,0,-1.8],[0,10.8,1.8]],["leg_R",[0,0,-1.8],[3.6,10.8,1.8]]];for(const [n,a,b] of parts){a[0]+=x;b[0]+=x;cube("AUX_"+n,a,b,mat.body,g);outline("AUX_edge_"+n,a,b,mat.edge,g);}return g;}
function floor(parent,width,depth){cube("AUX_ground_16u_grid",[-width/2,-.20,-depth/2],[width/2,-.10,depth/2],mat.ground,parent);for(let x=Math.ceil(-width/32)*16;x<=width/2;x+=16)cube("AUX_grid_X"+x,[x-.025,-.09,-depth/2],[x+.025,-.075,depth/2],mat.grid,parent);for(let z=Math.ceil(-depth/32)*16;z<=depth/2;z+=16)cube("AUX_grid_Z"+z,[-width/2,-.09,z-.025],[width/2,-.075,z+.025],mat.grid,parent);}
const main=group("01_REFERENCE_VIEW_1.75b_HISTORIC_SIZE");
floor(main,160,160);mannequin(0,main);
const bill=group("V3_CAMERA_PLANE_CENTER_0_14.4_0_OFFSET_1.28u",main,[0,14.4,0]);
const plane=plate("V3_CONFIRMED_FRAME_08_REF_28x28_THICK_0.12u",[-14,.4,1.22],[14,28.4,1.34],frames[7],bill);
outline("AUX_plane_boundary_28u",[-14,.4,1.22],[14,28.4,1.34],mat.cyan,bill,.045);
cube("AUX_body_center_marker",[-.18,14.22,-.18],[.18,14.58,.18],mat.amber,main);
cube("AUX_camera_offset_1.28u",[-.045,14.355,0],[.045,14.445,1.28],mat.amber,bill);
cube("AUX_height_28.8u",[-18.05,0,0],[-17.95,28.8,.1],mat.cyan,main);
for(const y of [0,28.8])cube("AUX_height_tick",[-19,y-.04,0],[-17,y+.04,.1],mat.cyan,main);
label("main_title","SEED SOW  /  v3 CONFIRMED ARCHIVE",0,38,-4,72,5,main);
label("main_dimensions","DUMMY 1.80b  |  REFERENCE FACE 1.75b",0,33,-4,65,4,main);
label("main_time","16 x 50ms  /  0.8s ONCE  /  FRAME 08",0,-4,10,60,4,main);
label("main_limit","Reference scale - runtime target span follows collision height",0,-8,10,76,3.2,main);
const phase=group("02_PHASE_COMPARISON_STATIC_SNAPSHOTS");
floor(phase,230,72);
const phases=[1,6,10,12,15,16];
const phaseNames=["IGNITE","RISE","BREAK","BURNOUT","EMBERS","CLEAR"];
for(let j=0;j<phases.length;j++){const x=(j-2.5)*35,idx=phases[j];const g=group("PHASE_"+String(idx).padStart(2,"0")+"_"+phaseNames[j],phase);mannequin(x,g);plate("V3_FRAME_"+idx,[x-14,.4,1.22],[x+14,28.4,1.34],frames[idx-1],g);label("phase_"+idx,"F"+String(idx).padStart(2,"0")+"  "+((idx-1)*50)+"ms  "+phaseNames[j],x,33,0,32,3.4,g);label("phase_span_"+idx,((idx-1)*50)+" - "+(idx*50)+" ms",x,-3,8,27,3,g);}
label("phase_title","CONFIRMED v3 / BOTTOM-TO-TOP BURNOUT / ONE PASS",0,41,-2,200,7,phase);
label("phase_note","Static frame samples: 1, 6, 10, 12, 15, 16  |  final frame fully transparent",0,-9,8,205,4.5,phase);
const side=group("03_SIDE_MEASUREMENT_LABELS",main);
label("side_note","FROZEN FRONT POSE / inspect offset + preview thickness",0,43,-4,95,4,side);
label("side_values","OFFSET 1.28u = 0.08b  |  FACE THICKNESS 0.12u (preview only)",0,-12,10,100,4,side);
side.visibility=false;
phase.visibility=false;
Undo.finishEdit("Archive confirmed v3 spatial reference without changing source");
Canvas.updateAll();
Project.saved=false;
window.seedSowRepair={dir,project:Project.uuid,main,phase,side,bill,plane,frames,groups,textures};
return {project:Project.name,uuid:Project.uuid,elements:elements.length,groups:groups.length,textures:Texture.all.length,frames:frames.map(t=>({name:t.name,width:t.width,height:t.height})),screenshotAPI:Screencam.screenshotPreview.toString().slice(0,1400),saveSignature:Codecs.project.write.toString()};
})()
