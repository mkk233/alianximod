(()=>{
const PID='e380502f-05e4-5cab-c7a2-4ff5f7e3c7c5';
if(Project.uuid!==PID)throw Error('Wrong project');
const DIR='D:/alianximod/docs/资源库/过程归档/留焰_v1_空间补全_20261112';
const fs=require('fs');
const strip=Texture.all.find(t=>t.uuid==='2ca01f05-4bd0-6144-cbbc-ab14cf348686');
if(!strip||!strip.img.complete||strip.img.naturalWidth!==64)throw Error('v1 strip not ready');
const editElements=[],editTextures=[];
Undo.initEdit({elements:editElements,textures:editTextures,outliner:true});
function texture(name,w,h,paint,file){
 const cv=document.createElement('canvas');cv.width=w;cv.height=h;const cx=cv.getContext('2d');cx.imageSmoothingEnabled=false;paint(cx);
 const data=cv.toDataURL('image/png');const t=new Texture({name,uv_width:w,uv_height:h,render_mode:'emissive',render_sides:'double',keep_size:true}).fromDataURL(data).add(false);
 t.uv_width=w;t.uv_height=h;editTextures.push(t);
 if(file)fs.writeFileSync(DIR+'/'+file,Buffer.from(data.split(',')[1],'base64'));
 return t;
}
function solid(name,color){return texture(name,4,4,c=>{c.fillStyle=color;c.fillRect(0,0,4,4)})}
const palette={
floor:solid('示意辅助_地面_非运行','#202a38'),
grid:solid('示意辅助_一格网线','#34485c'),
path:solid('示意辅助_实际经过路径中心','#6ddbd5'),
host:solid('示意辅助_移动宿主_非正式造型','#eee9c9'),
player:solid('示意辅助_1.8格人偶','#526c84'),
ruler:solid('示意辅助_尺寸标尺','#e4edf2'),
ghost:solid('示意辅助_轨迹节点','#426f79')};
function group(name,parent){const g=new Group({name}).init();if(parent)g.addTo(parent);return g}
const root=group('留焰_v1_空间补全_草稿_待用户确认3D');
const helpers=group('示意辅助_地面_玩家_宿主_标尺_说明',root);
function cube(name,from,to,tex,parent,extra={}){
 const faces={};for(const face of ['north','south','east','west','up','down'])faces[face]={texture:tex.uuid,uv:[0,0,tex.uv_width,tex.uv_height]};
 const c=new Cube({name,from,to,faces,box_uv:false,...extra}).addTo(parent||root).init();editElements.push(c);return c;
}
function plane(name,center,w,h,tex,parent,rotation=[0,0,0]){
 const faces={};for(const face of ['north','south','east','west','up','down'])faces[face]={texture:null};
 faces.south={texture:tex.uuid,uv:[0,0,tex.uv_width,tex.uv_height]};
 const c=new Cube({name,from:[center[0]-w/2,center[1]-h/2,center[2]],to:[center[0]+w/2,center[1]+h/2,center[2]],origin:center.slice(),rotation,faces,box_uv:false}).addTo(parent||root).init();editElements.push(c);return c;
}
function label(name,lines,center,w,h,font=38){
 const t=texture('示意辅助_标注_'+name,1024,Math.round(1024*h/w),cx=>{
 cx.fillStyle='#17212e';cx.fillRect(0,0,cx.canvas.width,cx.canvas.height);
 cx.fillStyle='#e9eef4';cx.font='600 '+font+'px "Microsoft YaHei",sans-serif';cx.textBaseline='top';
 lines.forEach((s,i)=>cx.fillText(s,28,18+i*(font+12)));
 });
 return plane('示意辅助_标注_'+name,center,w,h,t,helpers);
}
for(const [n,x] of [['A',-108],['B',0],['C',108]]){
 cube('示意辅助_地面_'+n,[x-50,-1,-38],[x+50,0,38],palette.floor,helpers);
 for(let v=-32;v<=32;v+=16)cube('示意辅助_地面一格网线_'+n,[x-48,.02,v-.10],[x+48,.06,v+.10],palette.grid,helpers);
 for(let v=-48;v<=48;v+=16)cube('示意辅助_地面一格网线_'+n,[x+v-.10,.02,-32],[x+v+.10,.06,32],palette.grid,helpers);
}
function player(x,z,name){
 const g=group('示意辅助_玩家1.8格_'+name,helpers);
 cube('示意辅助_双腿左',[x-3.2,0,z-1.6],[x-.3,11.2,z+1.6],palette.player,g);
 cube('示意辅助_双腿右',[x+.3,0,z-1.6],[x+3.2,11.2,z+1.6],palette.player,g);
 cube('示意辅助_躯干',[x-3.2,11.2,z-1.6],[x+3.2,22.4,z+1.6],palette.player,g);
 cube('示意辅助_头部',[x-3.2,22.4,z-3.2],[x+3.2,28.8,z+3.2],palette.player,g);
 cube('示意辅助_手臂左',[x-6.4,11.2,z-1.6],[x-3.2,22.4,z+1.6],palette.player,g);
 cube('示意辅助_手臂右',[x+3.2,11.2,z-1.6],[x+6.4,22.4,z+1.6],palette.player,g);
}
player(-146,26,'直线');player(-40,26,'上升下降');player(74,26,'环绕');
label('A',['A  普通载体 · 直线','沿真实路径留焰  /  不改变弹道'],[-108,71,-28],100,16,42);
label('B',['B  喷涌组合 · 上升—下降','仅说明组合关系'],[0,71,-28],100,16,42);
label('C',['C  风暴轨迹 · 旋转/环绕','路径中心留空  /  非整片伤害'],[108,71,-28],100,16,42);
label('总说明',['留焰 v1  ·  同版空间补全草稿','16 模型单位 = 1 世界格   |   贴图面 8×8 = 0.5×0.5 格','中心采样间距 ≤4 单位 = ≤0.25 格   |   每点40 tick寿命','青线/人偶/白色宿主/标尺均为示意辅助；此工程不接入游戏'],[0,100,-28],310,25,37);
const frames=[],ages=[];
for(let f=0;f<16;f++)frames.push(texture('v1原像素_F'+String(f+1).padStart(2,'0'),64,64,c=>c.drawImage(strip.img,0,f*64,64,64,0,0,64,64),'v1-frame-'+String(f+1).padStart(2,'0')+'.png'));
for(let a=0;a<40;a++){const f=Math.min(15,Math.floor(a*16/40));ages.push(texture('示意衰减_age'+a+'_F'+(f+1),64,64,c=>{c.globalAlpha=1-a/40;c.drawImage(strip.img,0,f*64,64,64,0,0,64,64)}));}
const board=texture('示意辅助_二维同版接触表',1152,864,c=>{
 c.fillStyle='#17212e';c.fillRect(0,0,1152,864);
 c.fillStyle='#f3eee1';c.font='600 28px "Microsoft YaHei"';c.fillText('留焰 v1 / 原像素 16 帧 / 最近邻 3× / 未重画',32,40);
 c.fillStyle='#8ea3b8';c.font='18px "Microsoft YaHei"';c.fillText('原 80ms/帧仅供展示（1.28s一轮）；运行单点寿命40 tick，独立映射16帧。',32,70);
 for(let i=0;i<16;i++){const x=32+(i%4)*280,y=98+Math.floor(i/4)*188;
 for(let yy=0;yy<152;yy+=16)for(let xx=0;xx<256;xx+=16){c.fillStyle=((xx+yy)/16)%2?'#232f3f':'#293748';c.fillRect(x+xx,y+yy,16,16);}
 c.drawImage(strip.img,0,i*64,64,64,x+40,y-20,192,192);
 c.fillStyle='#c1cddb';c.font='18px monospace';c.fillText('F'+String(i+1).padStart(2,'0'),x,y+176);
 }
},'2d-v1-contact-native-blockbench.png');
const paths=[
{id:'straight',title:'直线',fun:u=>[-148+80*u,12,-8]},
{id:'rise-fall',title:'上升下降',fun:u=>[-40+80*u,12+40*Math.sin(Math.PI*u),-8+16*u]},
{id:'orbit',title:'旋转环绕',fun:u=>[108+20*Math.cos(-Math.PI/2+3*Math.PI*u),10+44*u,20*Math.sin(-Math.PI/2+3*Math.PI*u)]}
];
const samples=[];
for(const p of paths){
 const dense=[];let length=0;let prev=p.fun(0);dense.push({u:0,pos:prev,s:0});
 for(let i=1;i<=1000;i++){let q=p.fun(i/1000);length+=Math.hypot(...q.map((v,j)=>v-prev[j]));dense.push({u:i/1000,pos:q,s:length});prev=q;}
 const n=Math.ceil(length/4);p.count=n+1;p.length=length;p.maxSpacing=0;p.group=group('留焰_'+p.title+'_采样点',root);p.points=[];
 for(let i=0;i<=n;i++){
 const s=i*length/n;let k=dense.findIndex(q=>q.s>=s);if(k<0)k=dense.length-1;
 let u=dense[k].u;if(k>0){const q=dense[k],r=dense[k-1];u=r.u+(q.u-r.u)*(s-r.s)/(q.s-r.s);}
 const pos=p.fun(u);const birth=u*32;const c=plane('留焰_'+p.id+'_点'+i,pos,8,8,ages[0],p.group);
 const rec={cube:c,pos,birth,index:i,path:p.id};p.points.push(rec);samples.push(rec);
 if(i)p.maxSpacing=Math.max(p.maxSpacing,Math.hypot(...pos.map((v,j)=>v-p.points[i-1].pos[j])));
 const r=.18;cube('示意辅助_路径采样中心_'+p.id+'_'+i,pos.map(v=>v-r),pos.map(v=>v+r),palette.path,helpers);
 }
 p.host=cube('示意辅助_移动宿主_'+p.id,[-1,-1,-1],[1,1,1],palette.host,helpers);
 for(let i=0;i<80;i++){
 const a=p.fun(i/80),b=p.fun((i+1)/80),mid=a.map((v,j)=>(v+b[j])/2),len=Math.hypot(...b.map((v,j)=>v-a[j]));
 const direction=new THREE.Vector3(...b.map((v,j)=>v-a[j])).normalize();
 const quat=new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(0,1,0),direction);
 const er=new THREE.Euler().setFromQuaternion(quat,'ZYX');
 cube('示意辅助_实际路径中心线_'+p.id+'_'+i,[mid[0]-.055,mid[1]-len/2,mid[2]-.055],[mid[0]+.055,mid[1]+len/2,mid[2]+.055],palette.ghost,helpers,{origin:mid,rotation:[er.x,er.y,er.z].map(v=>v*180/Math.PI)});
 }
}
cube('示意辅助_半格8单位标尺',[-123,23,7],[-115,23.2,7.2],palette.ruler,helpers);
for(const x of [-123,-119,-115])cube('示意辅助_四分之一格4单位标记',[x-.1,21,7],[x+.1,25,7.2],palette.ruler,helpers);
label('采样细节',['0.5格 = 8单位','中线刻度0.25格'],[-118,32,7],36,8,42);
const phaseLabel=label('当前阶段',['示意阶段 T=32 tick','先生成的点先熄灭；宿主运动仅演示'],[0,-10,30],160,12,48);
const ctx={PID,DIR,fs,paths,samples,ages,frames,helpers,phaseLabel,root,editElements,editTextures,tick:32};
window.flameTrailPreviewTask=ctx;
ctx.setPhase=function(tick,cam,target){
 if(Project.uuid!==PID)throw Error('Wrong project');
 const view=Preview.selected;if(!view.canvas.isConnected)throw Error('Not a connected viewport');
 view.setNormalCamera();view.camera.position.set(...cam);view.controls.target.set(...target);view.controls.update();
 const er=new THREE.Euler().setFromQuaternion(view.camera.quaternion,'ZYX');
 const rot=[er.x,er.y,er.z].map(v=>v*180/Math.PI);
 const counts={};
 for(const s of samples){
 const age=tick-s.birth;const visible=age>=0&&age<40;s.cube.visibility=visible;
 s.cube.rotation=rot.slice();s.cube.faces.south.texture=ages[Math.max(0,Math.min(39,Math.floor(age)))].uuid;
 s.cube.name='留焰_'+s.path+'_点'+s.index+'_age'+age.toFixed(1)+'_F'+(Math.min(15,Math.floor(Math.max(0,age)*16/40))+1);
 if(visible)counts[s.path]=(counts[s.path]||0)+1;
 }
 for(const p of paths){const pos=p.fun(Math.min(1,Math.max(0,tick/32)));p.host.from=pos.map(v=>v-1.8);p.host.to=pos.map(v=>v+1.8);}
 ctx.tick=tick;ctx.counts=counts;Canvas.updateAll();view.render();
 return {tick,counts,camera:view.camera.position.toArray(),target:view.controls.target.toArray(),rot};
};
ctx.saveModel=function(name){const data=Codecs.project.compile({compressed:false});const text=typeof data==='string'?data:JSON.stringify(data,null,2);const file=DIR+'/'+name;fs.writeFileSync(file,text,'utf8');Project.save_path=file;Project.saved=true;return {path:file,bytes:fs.statSync(file).size};};
ctx.capture=function(name){const v=Preview.selected;if(!v.canvas.isConnected)throw Error('Viewport disconnected');v.render();const data=v.canvas.toDataURL('image/png');const file=DIR+'/'+name;fs.writeFileSync(file,Buffer.from(data.split(',')[1],'base64'));return {path:file,width:v.canvas.width,height:v.canvas.height,camera:v.camera.position.toArray(),target:v.controls.target.toArray(),tick:ctx.tick,counts:ctx.counts};};
ctx.setPhase(32,[205,165,355],[0,37,0]);
Undo.finishEdit('Build flame trail v1 spatial draft with unchanged original pixels');
unselectAllElements();
Canvas.updateAll();
fs.writeFileSync(DIR+'/path-sampling.json',JSON.stringify({unitsPerBlock:16,planeUnits:[8,8],maxStepUnits:4,sampleLifeTicks:40,demonstrationTravelTicks:32,fade:'illustrative linear alpha = 1-age/40; not a verified runtime fade law',paths:paths.map(p=>({id:p.id,count:p.count,lengthUnits:p.length,maxSpacingUnits:p.maxSpacing,points:p.points.map(s=>({index:s.index,position:s.pos,birthTick:s.birth}))}))},null,2),'utf8');
return {project:Project.uuid,groups:Group.all.length,elements:Outliner.elements.length,textures:Texture.all.length,paths:paths.map(p=>({id:p.id,count:p.count,maxSpacing:p.maxSpacing})),saved:ctx.saveModel('flame-trail-v1-spatial-draft.bbmodel')};
})()
