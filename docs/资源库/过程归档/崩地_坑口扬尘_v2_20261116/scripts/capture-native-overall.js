(function(){
const root='D:/alianximod/docs/资源库/过程归档/崩地_坑口扬尘_v2_20261116';
if(Project.uuid!=='0d65d0f5-9590-698b-4313-bc901925bd9c')throw Error('Wrong candidate project');
const fs=require('fs'),p=Preview.selected,anim=Animation.all[0];
const cards=Group.all.filter(g=>g.name.endsWith('_CAMERA_PLANE'));
const e=new THREE.Euler().setFromQuaternion(p.camera.quaternion,'ZYX');
for(const g of cards)g.rotation=[e.x*180/Math.PI,e.y*180/Math.PI,e.z*180/Math.PI];
Canvas.updateAll();anim.select();
function pose(t){Timeline.setTime(t);Animator.preview();for(const g of cards)g.mesh.quaternion.copy(p.camera.quaternion);scene.updateMatrixWorld(true);}
function shot(file){let written=false;p.screenshot({crop:true,width:1100,height:900},data=>{fs.writeFileSync(root+'/previews/'+file,Buffer.from(data.split(',')[1],'base64'));written=true;});if(!written)throw Error('Screenshot callback not synchronous');}
pose(0.6);
const model=root+'/source/earth_break_ground_v2_preview.bbmodel';
Codecs.project.write(Codecs.project.compile({compressed:false}),model);
let samples=[];
for(let i=0;i<40;i++){const t=i/20;pose(t);shot('3d-frames/overall_'+String(i).padStart(3,'0')+'.png');samples.push({index:i,time:t,project:Project.uuid,view:p.id,visibleCards:Group.all.filter(g=>/_frame_\d\d$/.test(g.name)&&g.mesh.scale.x>0.5).length});}
for(const pair of [['overall-before.png',0.2],['overall-dust.png',0.65],['overall-clear.png',1.4]]){pose(pair[1]);shot(pair[0]);}
const motions=Group.all.filter(g=>g.name.endsWith('_WORLD_motion'));
pose(0.4);const starts=motions.map(g=>({name:g.name,world:g.mesh.getWorldPosition(new THREE.Vector3()).toArray()}));
pose(1.2);const ends=motions.map(g=>({name:g.name,world:g.mesh.getWorldPosition(new THREE.Vector3()).toArray()}));
pose(0.65);
const evidence={method:'Real Blockbench 5.2.2 active desktop Preview.screenshot -> PNG; Timeline.setTime + Animator.preview; no offline renderer',project:Project.uuid,modelPath:model,saved:Project.saved,view:p.id,camera:{position:p.camera.position.toArray(),target:p.controls.target.toArray(),quaternion:p.camera.quaternion.toArray()},nativeViewport:[p.width,p.height],pngSize:[1100,900],samples,emitterWorldStart:starts,emitterWorldEnd:ends,textureErrors:Texture.all.map(t=>({name:t.name,error:t.error}))};
fs.writeFileSync(root+'/evidence/native-capture-overall.json',JSON.stringify(evidence,null,2));
return JSON.stringify({model,saved:Project.saved,frames:samples.length,visibleCounts:samples.map(s=>s.visibleCards),start:starts[0],end:ends[0],view:p.id});
})()
