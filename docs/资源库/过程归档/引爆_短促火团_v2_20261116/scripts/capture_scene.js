(async()=>{
const s=window.detonateArt,fs=require('fs');
if(!s||!Project||Project.uuid!==s.expected)throw new Error('Wrong active project');
const pv=Preview.selected,root=s.root,records=[];
fs.mkdirSync(root+'/previews/gui_frames',{recursive:true});
fs.mkdirSync(root+'/previews/strength_frames',{recursive:true});
function guard(){if(Project.uuid!==s.expected)throw new Error('Project takeover; stop');}
function visible(node,on){node.visibility=on;if(node.children)for(const child of node.children)visible(child,on);}
function mode(kind){visible(s.main,kind!=='strength');visible(s.comparison,kind==='strength');visible(s.depth,false);}
function material(){for(const t of Object.values(s.flame)){const m=t.getMaterial();m.uniforms.SHADE.value=false;m.uniforms.EMISSIVE.value=false;m.depthWrite=false;m.depthTest=true;m.transparent=true;m.needsUpdate=true;}for(const e of s.effects)e.plane.mesh.renderOrder=10;}
function frame(f){for(const e of s.effects){e.plane.faces.south.uv=[0,(f-1)*64,64,f*64];e.plane.faces.north.texture=null;}}
function faceCamera(frozen=false){
 const normal=new THREE.Vector3(0,0,1).applyQuaternion(pv.camera.quaternion);
 const rot=new THREE.Euler().setFromQuaternion(pv.camera.quaternion,'ZYX');
 for(const e of s.effects){const n=frozen?new THREE.Vector3(0,0,1):normal;const c=e.anchor.map((v,i)=>v+[n.x,n.y,n.z][i]*3.2);e.bill.origin=c;e.bill.rotation=[0,0,0];e.plane.origin=c.slice();e.plane.rotation=frozen?[0,0,0]:[rot.x,rot.y,rot.z].map(v=>v*180/Math.PI);e.plane.from=[c[0]-e.span*8,c[1]-e.span*8,c[2]];e.plane.to=[c[0]+e.span*8,c[1]+e.span*8,c[2]];}
}
function camera(kind){
 if(kind==='strength')pv.loadAnglePreset({position:[0,40,200],target:[0,17,0],projection:'orthographic',zoom:.20});
 else if(kind==='depth')pv.loadAnglePreset({position:[68,39,45],target:[-20,16,1.8],projection:'orthographic',zoom:.40});
 else pv.loadAnglePreset({position:[58,44,165],target:[0,17,0],projection:'orthographic',zoom:.32});
 pv.controls.update();faceCamera(kind==='depth');
}
function refresh(){Canvas.updateAll();Canvas.updateVisibility();unselectAllElements();material();}
async function shot(file,f,kind,w=1200,h=850){guard();refresh();await new Promise(resolve=>pv.screenshot({crop:false,width:w,height:h},data=>{guard();fs.writeFileSync(root+'/previews/'+file,Buffer.from(data.split(',')[1],'base64'));resolve();}));records.push({file,frame_one_based:f,start_ms:(f-1)*50,mode:kind,project_uuid:Project.uuid,source:'native desktop Preview.selected.screenshot WebGL',camera_position:pv.camera.position.toArray(),camera_target:pv.controls.target.toArray(),zoom:pv.camera.zoom,document_visibility:document.visibilityState,plane_camera_facing:kind!=='depth',camera_bias_blocks:.20});}
Undo.initEdit({elements:Outliner.elements,groups:Group.all,outliner:true});
const guide=Outliner.elements.find(e=>e.name==='AUX_camera_bias_1.28u_0.08b');if(guide){guide.name='AUX_camera_bias_3.2u_0.20b';guide.to[2]=5;}
const depthLabel=Texture.all.find(t=>t.name==='AUX_label_depth');if(depthLabel){const cv=document.createElement('canvas');cv.width=1024;cv.height=128;const ct=cv.getContext('2d');ct.fillStyle='#202a34';ct.fillRect(0,0,1024,128);ct.font='500 46px Segoe UI';ct.textAlign='center';ct.textBaseline='middle';ct.fillStyle='#e6edf1';ct.fillText('FROZEN FRONT / 0.20b BIAS / ZERO THICKNESS',512,64,992);depthLabel.fromDataURL(cv.toDataURL());}
mode('main');camera('main');frame(5);refresh();
await shot('gui_01_burst_threequarter.png',5,'main');
for(let f=1;f<=16;f++){frame(f);await shot('gui_frames/frame_'+String(f).padStart(2,'0')+'.png',f,'main');}
frame(11);await shot('gui_03_dissipation_threequarter.png',11,'main');
mode('strength');camera('strength');frame(5);await shot('gui_04_strength_comparison.png',5,'strength',1400,1000);
for(let f=1;f<=16;f++){frame(f);await shot('strength_frames/frame_'+String(f).padStart(2,'0')+'.png',f,'strength',1400,1000);}
mode('main');visible(s.soul.group,false);for(const n of s.main.children)if(n.name&&n.name.startsWith('AUX_label_'))visible(n,false);visible(s.depth,true);camera('depth');frame(5);await shot('gui_02_frozen_side_depth.png',5,'depth',1000,850);
mode('main');camera('main');frame(5);refresh();
Undo.finishEdit('Capture native v2 temporal and spatial previews; save burst pose',{elements:Outliner.elements,groups:Group.all,outliner:true});
Project.save_path=root+'/blockbench/fire_detonate_v2_candidate.bbmodel';Codecs.project.write(Codecs.project.compile({editor_state:true}),Project.save_path);
fs.writeFileSync(root+'/evidence/native-captures.json',JSON.stringify({captures:records,frame_duration_ms:50,frame_count:16,effect_duration_ms:800,preview_pause_ms:600,material:{SHADE:false,EMISSIVE:false,depthWrite:false,depthTest:true,transparent:true,renderOrder:10},saved_frame_one_based:5,candidate_only:true},null,2));
return {captured:records.length,saved:Project.saved,path:Project.save_path,main:root+'/previews/gui_01_burst_threequarter.png'};
})()
