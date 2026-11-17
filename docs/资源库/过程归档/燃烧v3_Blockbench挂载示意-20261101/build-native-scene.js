(() => {
 const created=[]; Undo.initEdit({elements:created,outliner:true});
 const G=n=>Group.all.find(g=>g.name===n), T=n=>Texture.all.find(t=>t.name===n);
 const handR=new Group({name:'fx_right_fist',origin:[-6,13.2,0]}).addTo(G('right_arm')).init();
 const handL=new Group({name:'fx_left_fist',origin:[6,13.2,0]}).addTo(G('left_arm')).init();
 function grid(name,texture,us,vs,map,group) {
  const m=new Mesh({name,vertices:{},faces:{},origin:[0,0,0]});
  for(let j=0;j<vs.length;j++) for(let i=0;i<us.length;i++) m.vertices['v'+j+'_'+i]=map(us[i],vs[j]);
  for(let j=0;j<vs.length-1;j++) for(let i=0;i<us.length-1;i++){
   const a='v'+j+'_'+i,b='v'+(j+1)+'_'+i,c='v'+(j+1)+'_'+(i+1),d='v'+j+'_'+(i+1);
   m.faces['f'+j+'_'+i]=new MeshFace(m,{vertices:[a,b,c,d],texture:texture.uuid,uv:{[a]:[us[i],vs[j]],[b]:[us[i],vs[j+1]],[c]:[us[i+1],vs[j+1]],[d]:[us[i+1],vs[j]]}});
  }
  m.addTo(G(group)||group).init();created.push(m);return m;
 }
 const bounds={"abdomen_left":[47,51,54,67],"abdomen_right":[38,50,45,69],"cape":[24,39,78,104],"embers":[21,25,78,99],"hand_left":[56,45,75,73],"hand_right":[22,45,41,73],"hip_left":[55,62,60,77],"rear_left":[59,40,66,58],"rear_right":[32,44,39,59],"shin_left":[51,78,58,95],"shin_right":[36,77,43,95],"shoulder_left":[55,42,60,52],"shoulder_right":[36,43,43,55]};
 const seq=(a,b,n)=>Array.from({length:n+1},(_,i)=>a+(b-a)*i/n);
 grid('fx_cape_curved',T('burn_v3_cape'),[22,32,40,48,56,66,80],[38,48,64,80,96,106],(u,v)=>[(u-48)*.36,24-(v-39)*.36,-2.5-Math.max(0,v-39)/67*4.2-.30*Math.sin((u-24)/54*Math.PI)],'cape_anchor');
 for(const p of ['shoulder_right','shoulder_left','abdomen_right','abdomen_left','hip_left','shin_right','shin_left']){
  const b=bounds[p],cx=(b[0]+b[2])/2,root=p==='hip_left'||p==='shin_left'?'left_leg':p==='shin_right'?'right_leg':'body';
  grid('fx_'+p,T('burn_part_'+p),seq(b[0]-1,b[2]+1,3),seq(b[1]-1,b[3]+1,3),(u,v)=>[(u-48)*.4,24-(v-40)*.4,2.20+.38*Math.sin((v-b[1])/(b[3]-b[1])*Math.PI)+.10*Math.cos((u-cx)*.6)],root);
 }
 for(const [p,bx,xshift,group] of [['hand_right',31,.8,'fx_right_fist'],['hand_left',65,-.8,'fx_left_fist']]){
  const b=bounds[p];
  for(const side of [1,-1]){
   grid('fx_'+p+(side>0?'_front_shell':'_back_shell'),T('burn_v3_hand-fire'),seq(b[0]-1,b[2]+1,6),seq(b[1]-1,b[3]+1,5),(u,v)=>{
    const tilt=Math.sin(Math.max(0,Math.min(1,(73-v)/29))*Math.PI);
    const curvature=Math.cos((u-bx)/12*Math.PI/2);
    return [(u-48)*.4+xshift,24-(v-40)*.4,side*(2.22+.75*curvature+1.05*tilt)];
   },group);
  }
 }
 for(const p of ['rear_right','rear_left']){
  const b=bounds[p];
  grid('fx_'+p,T('burn_v3_rear-fire'),seq(b[0]-1,b[2]+1,3),seq(b[1]-1,b[3]+1,3),(u,v)=>[(u-48)*.4,24-(v-40)*.4,-2.3-.4*Math.sin((v-b[1])/(b[3]-b[1])*Math.PI)],'body');
 }
 grid('fx_body_embers_front',T('burn_v3_embers'),[19,48,80],[24,62,100],(u,v)=>[(u-48)*.4,24-(v-40)*.4,3.4],'body');
 Texture.all.filter(t=>t.name.startsWith('burn_')).forEach(t=>{t.fps=12.5;t.currentFrame=5;t.frame_interpolate=false;t.render_sides='double';t.force_translucent=true;t.updateMaterial();});
 Canvas.updateAll();Undo.finishEdit('燃烧原稿的完整火簇与弯曲披风空间示意');
 return {meshCount:created.length,groups:Group.all.length,textureCount:Texture.all.length};
})()