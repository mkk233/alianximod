(async function () {
 const root='D:/alianximod/docs/资源库/过程归档/幽冥蓝火重配色-20261116';
 const id='803970b1-7113-95b6-9e54-ee1415c7edff';
 if (!Project || Project.uuid!==id || Outliner.elements.length) throw new Error('Project changed or already populated; stop without mutation');
 const added=[],textures=[]; Undo.initEdit({elements:added,textures,outliner:true});
 const palette=new Texture({name:'AUX_reference_palette',uv_width:8,uv_height:8}).fromPath(root+'/textures/AUX_mannequin_palette.png').add(false);textures.push(palette);
 function texture(name,path,w,h){const t=new Texture({name,uv_width:w,uv_height:h,render_mode:'emissive',render_sides:'double'}).fromPath(root+'/'+path).add(false);textures.push(t);return t;}
 const sets={};
 for (const cfg of [{id:'kindle_v3_screenshot',w:48,h:64,n:19},{id:'fentian_conflagration',w:128,h:144,n:52},{id:'kindle_v1',w:64,h:64,n:16},{id:'fire_seed',w:32,h:32,n:8},{id:'liaoyuan_ground_flame',w:32,h:40,n:24}]) {
  sets[cfg.id]=['original','legacy','blue'].map((mode,index)=>texture(cfg.id+'_'+mode,(index===0?'references/'+cfg.id+'_original.png':'textures/'+cfg.id+'_'+(index===1?'legacy':'blue')+'.png'),cfg.w,cfg.h*cfg.n));
 }
 const ref=texture('REFERENCE_actual_nether_gloom_v2','references/nether_gloom_v2_runtime.png',64,1024);
 const vanilla=texture('REFERENCE_vanilla_soul_fire','references/soul_fire_0.png',16,512);
 function group(name,origin){return new Group({name,origin:origin||[0,0,0]}).init();}
 function cube(name,from,to,color,parent){const c=new Cube({name,from,to,box_uv:false}).addTo(parent).init();for(const face of Object.values(c.faces))face.extend({texture:palette.uuid,uv:[color,0,color+1,8]});added.push(c);return c;}
 function plane(name,x,y,z,w,h,t,frame,frameHeight,parent){const c=new Cube({name,from:[x-w/2,y,z],to:[x+w/2,y+h,z],shade:false,box_uv:false}).addTo(parent).init();for(const [side,face] of Object.entries(c.faces))face.extend({texture:side==='south'?t.uuid:null,uv:[0,(frame-1)*frameHeight,t.uv_width,frame*frameHeight]});added.push(c);return c;}
 const floor=group('AUX_ground_and_column_markers_not_runtime');
 cube('AUX_ground',[-74,-.5,-15],[74,0,17],6,floor);
 for(let x=-72;x<=72;x+=8)cube('AUX_grid_x_'+x,[x,0,-15],[x+.06,.02,17],0,floor);
 for(let z=-8;z<=16;z+=8)cube('AUX_grid_z_'+z,[-74,0,z],[74,.02,z+.06],0,floor);
 const positions=[-54,-18,18,54];
 for(let i=0;i<3;i++){
  const x=positions[i],g=group('AUX_entity_'+['ORIGINAL','OLD_RGB','NEW_SOUL'][i]+'_1p80m');
  cube('AUX_head_'+i,[x-2.7,22.5,-3.15],[x+2.7,28.8,3.15],3,g);
  cube('AUX_torso_'+i,[x-3.15,12.15,-1.8],[x+3.15,22.5,1.8],1,g);
  cube('AUX_left_arm_'+i,[x-5.85,11.7,-1.8],[x-3.15,22.5,1.8],1,g);
  cube('AUX_right_arm_'+i,[x+3.15,11.7,-1.8],[x+5.85,22.5,1.8],1,g);
  cube('AUX_left_leg_'+i,[x-3.15,0,-1.8],[x,12.15,1.8],0,g);
  cube('AUX_right_leg_'+i,[x,0,-1.8],[x+3.15,12.15,1.8],0,g);
  cube('AUX_hair_'+i,[x-2.7,27,3.16],[x+2.7,28.8,3.18],4,g);
  cube('AUX_eyeL_'+i,[x-1.35,25.2,3.18],[x-.45,26.1,3.19],4,g);
  cube('AUX_eyeR_'+i,[x+.9,25.2,3.18],[x+1.8,26.1,3.19],4,g);
  for(let n=0;n<=i;n++)cube('AUX_column_number_'+i+'_'+n,[x-2+i*-1+n*2,.03,9],[x-1+i*-1+n*2,.12,11],7,floor);
 }
 for(let i=0;i<3;i++){
  const x=positions[i];
  const v3=group('COMPARE_V3_'+i+'_焚天_全身烈焰_v3',[x,14.4,0]);
  plane('FIRE_V3_'+i,x,0,5.44,21.6,28.8,sets.kindle_v3_screenshot[i],6,64,v3);
  const kindleV4=group('COMPARE_KINDLEV4_'+i+'_引燃_承接焚天_v4',[x,14.4,0]);
  const kindlePlane=plane('FIRE_KINDLEV4_'+i,x,0,5.44,25.6,28.8,sets.fentian_conflagration[i],6,144,kindleV4);kindlePlane.visibility=false;
  const runtime=group('COMPARE_RUNTIME_'+i,[x,14.4,0]);
  const p=plane('FIRE_RUNTIME_'+i,x,0,5.44,28.8,28.8,sets.kindle_v1[i],6,64,runtime);p.visibility=false;
  const seed=group('COMPARE_SEED_'+i,[x,14.4,0]);
  for(let n=0;n<3;n++){const q=plane('FIRE_SEED_'+i+'_'+n,x+[-6,6,0][n],[18,18,29][n],5.44,6,6,sets.fire_seed[i],6,32,seed);q.visibility=false;}
  const attached=group('COMPARE_ATTACHED_'+i,[x,14.4,0]);
  const a=plane('FIRE_ATTACHED_'+i,x,0,5.44,14.4,18,sets.liaoyuan_ground_flame[i],6,40,attached);a.visibility=false;
 }
 const rg=group('REFERENCE_native_nether_and_MC_soul');
 plane('NETHER_NATIVE_v2_F06',54,0,5.44,28.8,28.8,ref,6,64,rg);
 plane('MC_SOUL_FIRE_0_reference_only',54,31,5.44,7,7,vanilla,1,16,rg);
 Project.credit='nether-blue-visual / art-v1-bb-finish-20261116 / candidate_pending_user_confirmation / no runtime integration / V3=焚天_全身烈焰_v3; KINDLEV4=引燃_承接焚天_v4';
 Undo.finishEdit('Create isolated soul palette comparison',{elements:added,textures,outliner:true});
 await new Promise(r=>setTimeout(r,400)); Canvas.updateAll(); unselectAllElements();
 for(const t of textures){const m=t.getMaterial();if(m.uniforms&&m.uniforms.SHADE)m.uniforms.SHADE.value=false;}
 Preview.selected.loadAnglePreset({position:[20,55,160],target:[0,16,0],projection:'orthographic',zoom:.21});
 Preview.selected.controls.update();Preview.selected.render();
 const path=root+'/blockbench/nether_blue_palette_art_v1.bbmodel';Project.save_path=path;Codecs.project.write(Codecs.project.compile(),path);
 return {project:Project.uuid,model:path,elements:added.length,textures:Texture.all.map(t=>({name:t.name,width:t.width,height:t.height,error:t.error,complete:t.img.complete})),otherProjects:ModelProject.all.filter(p=>p!==Project).map(p=>({uuid:p.uuid,name:p.name,saved:p.saved,elements:p.elements.length,textures:p.textures.length}))};
})()
