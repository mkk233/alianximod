(()=>{
const t=window.flameTrailPreviewTask;if(Project.uuid!==t.PID)throw Error('Wrong project');
Undo.initEdit({textures:Texture.all,elements:Outliner.elements});
for(const tx of t.ages){tx.force_translucent=true;if(typeof tx.updateMaterial==='function')tx.updateMaterial();if(tx.material){tx.material.transparent=true;tx.material.depthWrite=false;tx.material.needsUpdate=true;}}
const tx=Texture.all.find(x=>x.name==='示意辅助_标注_总说明');
const cv=document.createElement('canvas');cv.width=1600;cv.height=200;const c=cv.getContext('2d');
c.fillStyle='#17212e';c.fillRect(0,0,1600,200);c.fillStyle='#f3eee1';c.font='600 48px "Microsoft YaHei"';c.fillText('留焰 v1  ·  同版空间补全草稿',30,59);
c.font='30px "Microsoft YaHei"';c.fillStyle='#bdcddd';c.fillText('16 模型单位 = 1 世界格    |    贴图面 8×8 = 0.5×0.5 格',30,105);
c.fillText('中心采样 ≤4 单位 = ≤0.25 格    |    每点40 tick寿命    |    青线 / 人偶 / 宿主是示意辅助',30,151);
tx.fromDataURL(cv.toDataURL());tx.uv_width=1600;tx.uv_height=200;
const plane=Outliner.elements.find(x=>x.name==='示意辅助_标注_总说明');plane.from[1]=85;plane.to[1]=125;plane.origin[1]=105;plane.faces.south.uv=[0,0,1600,200];
const ph=Texture.all.find(x=>x.name==='示意辅助_标注_当前阶段');
const cp=document.createElement('canvas');cp.width=1280;cp.height=128;const pc=cp.getContext('2d');pc.fillStyle='#17212e';pc.fillRect(0,0,1280,128);pc.fillStyle='#e9eef4';pc.font='48px "Microsoft YaHei"';pc.fillText('阶段见文件名：T=32 tick  |  旧点先熄灭',24,55);pc.font='30px "Microsoft YaHei"';pc.fillText('位移用32 tick演示；不是施法时长或已实现宿主的证明',24,105);ph.fromDataURL(cp.toDataURL());ph.uv_width=1280;ph.uv_height=128;t.phaseLabel.faces.south.uv=[0,0,1280,128];
t.phaseLabel.from[1]=-14;t.phaseLabel.to[1]=2;t.phaseLabel.origin[1]=-6;
t.updatePhaseLabel=function(tick){pc.fillStyle='#17212e';pc.fillRect(0,0,1280,128);pc.fillStyle='#e9eef4';pc.font='48px "Microsoft YaHei"';pc.fillText('示意阶段 T='+tick+' tick  |  旧点先熄灭',24,55);pc.font='30px "Microsoft YaHei"';pc.fillText('位移用32 tick演示；衰减为线性示意，非运行参数变更',24,105);ph.fromDataURL(cp.toDataURL());};
Undo.finishEdit('Make preview annotations legible and enable illustrative age transparency');
Canvas.scene.background=new THREE.Color('#101722');
return {fade:{force:t.ages[30].force_translucent,materialType:t.ages[30].material?.type,transparent:t.ages[30].material?.transparent},phase:t.setPhase(32,[170,135,340],[0,47,0]),sourceTextureUnchanged:Texture.all[0].name};
})()
