(() => {
 if(Project.uuid!=='a4aeb36c-dd37-cb64-4add-022dcda3aea3')throw Error('Unexpected project');
 const created=[];Undo.initEdit({elements:created,outliner:true});
 const T=n=>Texture.all.find(t=>t.name===n);
 const main=new Group({name:'meteorite_v2_material',origin:[8,8,8]}).init();
 const old=new Group({name:'示意辅助_旧材质对照',origin:[-18,8,8]}).init();
 const floor=new Group({name:'示意辅助_地面',origin:[0,0,0]}).init();
 const player=new Group({name:'示意辅助_两格高比例人偶',origin:[38,0,8]}).init();
 function box(name,from,to,tex,group,uv){
  const b=new Cube({name,from,to,origin:[0,0,0],box_uv:false});
  for(const f of Object.values(b.faces)){f.texture=tex.uuid;f.uv=[0,0,uv,uv];f.rotation=0;}
  b.addTo(group).init();created.push(b);return b;
 }
 box('meteorite_dark_gold_v2',[0,0,0],[16,16,16],T('meteorite_dark_gold_v2'),main,32);
 box('示意辅助_旧版陨岩',[-26,0,0],[-10,16,16],T('aux_old_meteorite_v1'),old,32);
 box('示意辅助_地面',[-50,-1,-32],[58,0,48],T('aux_meteor_floor'),floor,16);
 const P=(a)=>[a[0]+38,a[1],a[2]+8];
 for(const [name,a,b] of [
  ['头',[-4,24,-4],[4,32,4]],['躯干',[-4,12,-2],[4,24,2]],
  ['右臂',[-8,12,-2],[-4,24,2]],['左臂',[4,12,-2],[8,24,2]],
  ['右腿',[-4,0,-2],[0,12,2]],['左腿',[0,0,-2],[4,12,2]]])
  box('示意辅助_人偶_'+name,P(a),P(b),T('aux_meteor_player'),player,16);
 for(const c of created) if(c.name==='示意辅助_旧版陨岩'||c.name.startsWith('示意辅助_人偶_')) c.visibility=false;
 Canvas.updateAll();Undo.finishEdit('陨岩v2：1格立方体六面贴图与比例对照');
 return {project:Project.uuid,cubes:created.length,mainSize:[16,16,16],uv:[0,0,32,32],texture:T('meteorite_dark_gold_v2').uuid};
})()
