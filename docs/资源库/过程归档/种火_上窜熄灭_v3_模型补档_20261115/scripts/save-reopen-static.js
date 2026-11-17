(()=>{
const fs=require("fs"),s=window.seedSowRepair;
const dir="D:/alianximod/docs/资源库/过程归档/种火_上窜熄灭_v3_模型补档_20261115",path=dir+"/seed_sow_v3_preview.bbmodel";
if(!Project||Project.name!=="seed_sow_v3_preview")throw new Error("Wrong project");
const before={projectUuid:Project.uuid,path:Project.save_path,hash:require("crypto").createHash("sha256").update(fs.readFileSync(path)).digest("hex").toUpperCase(),reopenExists:fs.existsSync(dir+"/reopen-validation.json")};
let invalidated=false;
if(s&&s.plane){s.plane=null;invalidated=true;}
if(Undo.current_save&&typeof Undo.cancelEdit==="function")Undo.cancelEdit();
const plane=Outliner.elements.find(e=>e.name==="V3_CONFIRMED_FRAME_08_REF_28x28_THICK_0.12u"),tex=Texture.all.find(t=>t.name==="v3_FRAME_08_50ms");
if(!plane||!tex)throw new Error("Expected native plane/texture missing");
Undo.initEdit({elements:[plane]});plane.faces.south.texture=tex.uuid;plane.faces.north.texture=tex.uuid;Undo.finishEdit("Restore static confirmed frame 08; no continuing playback");
Canvas.updateAll();
Project.save_path=path;
const compiled=Codecs.project.compile({editor_state:true});
Codecs.project.write(compiled,path);
const disk=fs.readFileSync(path,"utf8"),data=JSON.parse(disk),original=Project;
Codecs.project.load(data,{path});
const reopen={atUtc:new Date().toISOString(),path:Project.save_path,name:Project.name,originalUuid:original.uuid,reopenedUuid:Project.uuid,differentNativeProjectObject:Project!==original,elements:Outliner.elements.length,groups:Group.all.length,textures:Texture.all.length,embeddedTextures:data.textures.filter(t=>t.source&&t.source.startsWith("data:image")).length,diskBytes:Buffer.byteLength(disk),diskSha256:require("crypto").createHash("sha256").update(disk).digest("hex").toUpperCase(),pendingDemoInvalidated:invalidated,timingDemoNotDelivered:true};
if(Project===original||reopen.elements!==634||reopen.textures!==43)throw new Error("Native reopen failed");
if(original.saved&&typeof original.close==="function")original.close();
fs.writeFileSync(dir+"/reopen-validation.json",JSON.stringify({before,reopen},null,2),"utf8");
return {before,reopen,remainingProjects:ModelProject.all.map(p=>({name:p.name,path:p.save_path,saved:p.saved})),frame8Texture:tex.uuid};
})()
