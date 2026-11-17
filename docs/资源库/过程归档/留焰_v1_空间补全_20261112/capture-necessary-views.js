(()=>{
const t=window.flameTrailPreviewTask;if(Project.uuid!==t.PID)throw Error('Wrong project');
const views=[
['viewport-side-t32.png',[280,85,105],[38,30,0]],
['viewport-back-t32.png',[-160,120,-335],[0,40,0]],
['viewport-scale-close-t32.png',[-118,33,75],[-118,15,-5]]
];
const captures=[];
for(const [name,cam,target] of views){t.setPhase(32,cam,target);for(const tx of t.ages)tx.material.uniforms.SHADE.value=false;captures.push(t.capture(name));}
t.setPhase(32,[170,135,340],[0,47,0]);for(const tx of t.ages)tx.material.uniforms.SHADE.value=false;
const saved=t.saveModel('flame-trail-v1-spatial-draft.bbmodel');
const logPath=t.DIR+'/capture-log.json';const log=JSON.parse(t.fs.readFileSync(logPath,'utf8'));log.captures.push(...captures.map(capture=>({capture})));t.fs.writeFileSync(logPath,JSON.stringify(log,null,2));
return {captures,saved,projects:ModelProject.all.map(p=>({uuid:p.uuid,name:p.name,saved:p.saved})),writesSettled:true};
})()
