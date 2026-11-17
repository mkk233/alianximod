(async()=>{
const s=window.seedSowRepair,fs=require("fs");
if(Project.uuid!==s.project)throw new Error("Wrong project");
const changed=Outliner.elements.filter(e=>e.name==="AUX_head"||e.name==="AUX_body"||e.name==="AUX_arm_L"||e.name==="AUX_arm_R"||e.name==="AUX_leg_L"||e.name==="AUX_leg_R");
Undo.initEdit({elements:changed});
for(const e of changed)for(const f of Object.values(e.faces))f.texture=null;
Undo.finishEdit("Make auxiliary mannequin open wire reference to avoid implying runtime depth");
function hideLabels(){for(const e of Outliner.elements){if(/AUX_LABEL_(main_time|main_limit|phase_span_|phase_note)/.test(e.name))e.visibility=false;}}
hideLabels();Canvas.updateAll();
const v=Preview.all.find(p=>p.id==="mcp_offscreen_seed_sow_archive");
v.render();Preview.all.find(p=>p.id==="main").render();
const url=await new Promise(r=>v.screenshot({crop:false},r));
fs.writeFileSync(s.dir+"/02-front-billboard.png",Buffer.from(url.split(",")[1],"base64"));
return {helperFillFacesRemoved:changed.length,sourceTexturesChanged:false,file:s.dir+"/02-front-billboard.png",plane:s.plane.faces.south.uv};
})()
