(()=>{
const task=window.flameTrailCompositeV2;
if(!task||Project.uuid!=='8a58e270-0fce-3a38-f8c5-528d850c4a39')throw Error('Expected the existing v2 project');
if(task.textures.some(texture=>!texture.img.complete||texture.img.naturalWidth===0))throw Error('Textures not ready');
task.captures=[];
task.setPhase(40,[60,27,80],[38,8.25,0]);
task.capture('viewport-close-perspective-t40.png');
task.setPhase(40,[38,8.25,90],[38,8.25,0]);
task.capture('viewport-side-t40.png');
for(const tick of [4,20,40,52,68,84,96]){
  task.setPhase(tick,[40,34,140],[40,8.25,0]);
  task.capture('viewport-phase-t'+String(tick).padStart(2,'0')+'.png');
}
task.setPhase(40,[60,27,80],[38,8.25,0]);
const saved=task.save();
task.writeLog();
return {saved,captures:task.captures,finalProject:{uuid:Project.uuid,elements:Outliner.elements.length,textures:Texture.all.length,saved:Project.saved},otherProjects:ModelProject.all.filter(project=>project.uuid!==task.projectId).map(project=>({uuid:project.uuid,name:project.name,saved:project.saved}))};
})()
