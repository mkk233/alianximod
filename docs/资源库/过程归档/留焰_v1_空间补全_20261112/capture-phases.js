(async()=>{
const t=window.flameTrailPreviewTask;if(Project.uuid!==t.PID)throw Error('Wrong project');
Undo.initEdit({textures:t.ages,elements:[t.phaseLabel]});
for(const tx of t.ages){tx.render_mode='default';tx.force_translucent=true;tx.updateMaterial();tx.material.transparent=true;tx.material.depthWrite=false;tx.material.uniforms.SHADE.value=false;tx.material.needsUpdate=true;}
t.phaseLabel.from[2]=60;t.phaseLabel.to[2]=60;t.phaseLabel.origin[2]=60;
Undo.finishEdit('Preserve actual alpha fade and move stage caption in front of ground');
const cv=document.createElement('canvas');cv.width=1152;cv.height=904;const c=cv.getContext('2d');c.imageSmoothingEnabled=false;c.fillStyle='#17212e';c.fillRect(0,0,1152,904);
const strip=Texture.all.find(tx=>tx.uuid==='2ca01f05-4bd0-6144-cbbc-ab14cf348686');
c.fillStyle='#f3eee1';c.font='600 28px "Microsoft YaHei"';c.fillText('留焰 v1 / 原像素 16 帧 / 最近邻 3× / 未重画',32,40);
c.fillStyle='#8ea3b8';c.font='18px "Microsoft YaHei"';c.fillText('原 80ms/帧仅供展示（1.28s一轮）；运行单点40 tick，独立映射16帧。',32,70);
for(let i=0;i<16;i++){const x=32+i%4*280,y=98+Math.floor(i/4)*188;for(let yy=0;yy<152;yy+=16)for(let xx=0;xx<256;xx+=16){c.fillStyle=((xx+yy)/16)%2?'#232f3f':'#293748';c.fillRect(x+xx,y+yy,16,16);}c.drawImage(strip.img,0,i*64,64,64,x+40,y-20,192,192);c.fillStyle='#c1cddb';c.font='18px monospace';c.fillText('F'+String(i+1).padStart(2,'0'),x,y+176);}
c.fillStyle='#dfc18b';c.font='18px "Microsoft YaHei"';c.fillText('Blockbench 原生素材接触表；Aseprite 打开/图像导入超时，尚未完成其 GUI 预览。',32,884);
t.fs.writeFileSync(t.DIR+'/2d-v1-contact-native-blockbench.png',Buffer.from(cv.toDataURL().split(',')[1],'base64'));
const log=[];
for(const tick of [0,8,20,32,48,64,72]){
 t.updatePhaseLabel(tick);
 t.setPhase(tick,[170,135,340],[0,47,0]);
 await new Promise(r=>setTimeout(r,100));
 for(const tx of t.ages)tx.material.uniforms.SHADE.value=false;
 const capture=t.capture('viewport-phase-t'+String(tick).padStart(2,'0')+'.png');
 const model=t.saveModel('phase-t'+String(tick).padStart(2,'0')+'.bbmodel');
 log.push({capture,model});
}
t.updatePhaseLabel(32);t.setPhase(32,[170,135,340],[0,47,0]);await new Promise(r=>setTimeout(r,100));log.push({capture:t.capture('viewport-overview-t32.png'),model:t.saveModel('flame-trail-v1-spatial-draft.bbmodel')});
t.fs.writeFileSync(t.DIR+'/capture-log.json',JSON.stringify({source:'Blockbench desktop connected main viewport; render() + canvas.toDataURL; no offscreen/headless/compositing',viewportId:Preview.selected.id,visibilityState:document.visibilityState,captures:log},null,2));
return {completed:true,captures:log.length,activePhase:t.tick,visible:t.counts};
})()
