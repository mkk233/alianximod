(async()=>{
if(Project.uuid!=='dfc5c52c-afcb-951b-9625-7c632d2e16d3')throw Error('Wrong active project');
const dest='D:/alianximod/docs/资源库/过程归档/幽冥_v2领域定稿预览_20261116';
const source=Texture.all.find(t=>t.name==='nether_gloom_v2_runtime.png'||t.name==='v2_runtime_unchanged');
if(!source||!source.img.complete)throw Error('Source texture not loaded');
const elements=[],groups=[],textures=[],animations=[];
Undo.initEdit({elements,groups,textures,animations,outliner:true});
const root=new Group({name:'DOMAIN_7x7x8_world_anchor',origin:[0,0,0]}).init();groups.push(root);
const aux=new Group({name:'AUX_ONLY_1block_tiles_1p8m_person',origin:[0,0,0]}).init();groups.push(aux);
const ext=new Group({name:'AUX_ONLY_7x7x8_bounds_hidden_in_beauty',origin:[0,0,0],visibility:false}).init();groups.push(ext);
const swatch=(name,color)=>{
 const c=document.createElement('canvas');c.width=c.height=16;const x=c.getContext('2d');x.fillStyle=color;x.fillRect(0,0,16,16);
 const t=new Texture({name,uv_width:16,uv_height:16}).fromDataURL(c.toDataURL()).add(false);textures.push(t);return t;
};
const tileA=swatch('AUX_tile_graphite','#48515a'),tileB=swatch('AUX_tile_slate','#525c65'),personTex=swatch('AUX_person_ochre','#af946b'),lineTex=swatch('AUX_bounds_bluegrey','#71828f'),markTex=swatch('AUX_anchor_ivory','#eee2bf');
const c=document.createElement('canvas');c.width=64;c.height=1024;const ctx=c.getContext('2d');ctx.imageSmoothingEnabled=false;ctx.drawImage(source.img,0,0);
const pixels=ctx.getImageData(0,0,64,1024);for(let p=3;p<pixels.data.length;p+=4)pixels.data[p]=Math.round(pixels.data[p]*210/255);ctx.putImageData(pixels,0,0);
Blockbench.writeFile(dest+'/textures/v2_preview_alpha210.png',{savetype:'image',content:c.toDataURL()});
const id='20261116-0000-4000-8000-000000000049',hex=id.replaceAll('-',''),M=(1n<<64n)-1n,A=BigInt('0x'+hex.slice(0,16)),B=BigInt('0x'+hex.slice(16));
const unit=(index,salt)=>{let x=A^(((B<<21n)|(B>>43n))&M)^((BigInt(index+1)*0x9E3779B97F4A7C15n)&M)^((BigInt(salt+1)*0xBF58476D1CE4E5B9n)&M);x^=x>>30n;x=(x*0xBF58476D1CE4E5B9n)&M;x^=x>>27n;x=(x*0x94D049BB133111EBn)&M;x^=x>>31n;return Number(x>>11n)/9007199254740992;};
const hash=(parseInt(hex.slice(0,8),16)^parseInt(hex.slice(8,16),16)^parseInt(hex.slice(16,24),16)^parseInt(hex.slice(24,32),16)),phase=((hash%16)+16)%16;
const phaseTextures=[];
for(let p=0;p<4;p++){
 const order=Array.from({length:16},(_,f)=>(f+phase+p*4)%16).join(' ');
 const t=new Texture({name:'v2_alpha210_phase_'+p*4,uv_width:64,uv_height:64,render_mode:'default',render_sides:'double',force_translucent:true,fps:10,frame_time:2,frame_order_type:'custom',frame_order:order,frame_interpolate:false}).fromDataURL(c.toDataURL()).add(false);
 textures.push(t);phaseTextures.push(t);
}
const box=(name,from,to,tex,parent,shade=true)=>{
const q=new Cube({name,from,to,autouv:0,box_uv:false,shade});
for(const f of Object.values(q.faces)){f.texture=tex.uuid;f.uv=[0,0,16,16];}q.addTo(parent).init();elements.push(q);return q;};
for(let x=-4;x<=4;x++)for(let z=-4;z<=4;z++)box('AUX_1block_tile_'+x+'_'+z,[x*16-7.9,-1.4,z*16-7.9],[x*16+7.9,0,z*16+7.9],(x+z)%2?tileA:tileB,aux);
const person=new Group({name:'AUX_person_1p8blocks',origin:[-28.8,0,17.6]}).addTo(aux).init();groups.push(person);
const px=-28.8,pz=17.6;
box('AUX_person_left_leg',[px-3.6,0,pz-2.1],[px-.2,10.8,pz+2.1],personTex,person);
box('AUX_person_right_leg',[px+.2,0,pz-2.1],[px+3.6,10.8,pz+2.1],personTex,person);
box('AUX_person_body',[px-4.2,10.8,pz-2.2],[px+4.2,21.6,pz+2.2],personTex,person);
box('AUX_person_head',[px-3.6,21.6,pz-3.6],[px+3.6,28.8,pz+3.6],personTex,person);
box('AUX_person_armL',[px-7.3,10.8,pz-2],[px-4.4,21.3,pz+2],personTex,person);
box('AUX_person_armR',[px+4.4,10.8,pz-2],[px+7.3,21.3,pz+2],personTex,person);
for(const x of [-56,56])for(const z of [-56,56])box('AUX_vertical_8blocks',[x-.10,0,z-.10],[x+.10,128,z+.10],lineTex,ext,false);
for(const y of [0.05,128]){for(const z of [-56,56])box('AUX_width7',[-56,y-.10,z-.10],[56,y+.10,z+.10],lineTex,ext,false);for(const x of [-56,56])box('AUX_depth7',[x-.10,y-.10,-56],[x+.10,y+.10,56],lineTex,ext,false);}
const anim=new Animation({name:'v2_runtime_pulse_and_4phase_19p2s_DEMO_NOT_DURATION',length:19.2,loop:'loop',snapping:20}).add(false);animations.push(anim);
const spacing=.9+.2*unit(0,7),cols=Math.round(7/spacing),rows=cols,step=7/cols,samples=[];
if(cols!==7)throw Error('Expected 49 representative clusters');
for(let column=0;column<cols;column++)for(let row=0;row<rows;row++){
 const i=row*cols+column,x=(-3.5+(column+.5)*step+(unit(i,1)-.5)*step*.72)*16,z=(-3.5+(row+.5)*step+(unit(i,2)-.5)*step*.72)*16,w=(.69+.37*unit(i,3))*16,h=(.50+.56*unit(i,4))*16,yaw=unit(i,5)*180;
 const g=new Group({name:'WISP_'+String(i).padStart(2,'0')+'_phase'+(i%4)*4,origin:[x,2.4,z],rotation:[0,yaw,0]}).addTo(root).init();groups.push(g);
 const q1=new Cube({name:'cross_XY_zero_thickness',from:[x-w/2,2.4,z],to:[x+w/2,2.4+h,z],origin:[x,2.4,z],autouv:0,box_uv:false,shade:false});
 const q2=new Cube({name:'cross_YZ_zero_thickness',from:[x,2.4,z-w/2],to:[x,2.4+h,z+w/2],origin:[x,2.4,z],autouv:0,box_uv:false,shade:false});
 for(const q of [q1,q2]){for(const f of Object.values(q.faces))f.texture=null;const face=q===q1?q.faces.north:q.faces.east;face.texture=phaseTextures[i%4].uuid;face.uv=[0,0,64,64];q.addTo(g).init();elements.push(q);}
 const ba=anim.getBoneAnimator(g);const angle=(i*2.4*180/Math.PI).toFixed(12);const pulse='(0.5+0.5*math.sin(query.anim_time*93.75+'+angle+'))';
 ba.addKeyframe({channel:'scale',time:0,interpolation:'linear',data_points:[{x:'0.8+0.2*'+pulse,y:'0.45+0.55*'+pulse,z:'0.8+0.2*'+pulse}]});
 samples.push({index:i,column,row,x:x/16,y:.15,z:z/16,width:w/16,height:h/16,yawDegrees:yaw,group:g.uuid,phase:i%4*4});
}
const focus=samples.find(s=>s.index===24);
const marker=new Group({name:'AUX_ONLY_single_anchor_measure_hidden',origin:[0,0,0],visibility:false}).init();groups.push(marker);
box('AUX_base_to_anchor_0p15',[focus.x*16-10,0,focus.z*16],[focus.x*16-9.75,2.4,focus.z*16+.25],markTex,marker,false);
box('AUX_anchor_level',[focus.x*16-11,2.32,focus.z*16],[focus.x*16+11,2.48,focus.z*16+.16],markTex,marker,false);
Undo.finishEdit('Build current gloom geometry from source parameters',{elements,groups,textures,animations,outliner:true});
window.gloomPreview={dest,project:Project.uuid,uuid:id,phase,spacing,cols,rows,samples,root:root.uuid,aux:aux.uuid,ext:ext.uuid,marker:marker.uuid,animation:anim.uuid,phaseTextures:phaseTextures.map(t=>t.uuid),focus:24};
Blockbench.writeFile(dest+'/evidence/scene-parameters.json',{content:JSON.stringify(window.gloomPreview,null,2)});
Canvas.updateAll();
anim.select();
return {project:Project.uuid,clusters:samples.length,cols,rows,spacing,domainPhase:phase,elements:elements.length,groups:groups.length,textureNames:phaseTextures.map(t=>t.name),animation:anim.uuid,focus};
})()
