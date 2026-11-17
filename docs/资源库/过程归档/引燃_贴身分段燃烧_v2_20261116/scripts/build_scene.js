(async function () {
  const root = 'D:/alianximod/docs/资源库/过程归档/引燃_贴身分段燃烧_v2_20261116';
  if (!Project || Project.uuid !== '41e5bed5-092c-5649-685e-72cee9618239') throw new Error('Unexpected active project; no mutation');
  if (Outliner.elements.length) throw new Error('Candidate project is not empty; no overwrite');
  const added = [], texs = [];
  Undo.initEdit({elements:added,textures:texs,outliner:true});
  const palette = new Texture({name:'AUX_reference_palette',uv_width:8,uv_height:8}).fromPath(root+'/textures/aux_mannequin_palette.png').add(false);
  const fire = new Texture({name:'CANDIDATE_kindle_v2_19_frames',uv_width:48,uv_height:1216,render_mode:'emissive',render_sides:'double'}).fromPath(root+'/textures/kindle_v2_strip.png').add(false);
  texs.push(palette,fire);
  const aux = new Group({name:'AUX_normal_target_1.80m_not_runtime',origin:[0,0,0]}).init();
  const fx = new Group({name:'CANDIDATE_CAMERA_PLANE_F11_pulse',origin:[0,14.4,0]}).init();
  const floor = new Group({name:'AUX_ground_and_depth_reference_not_runtime',origin:[0,0,0]}).init();
  function cube(name,from,to,color,parent) {
    const c=new Cube({name,from,to,box_uv:false}).addTo(parent).init();
    for(const face of Object.values(c.faces)) face.extend({texture:palette.uuid,uv:[color,0,color+1,8]});
    added.push(c);return c;
  }
  function body(name,x1,y1,x2,y2,depth,color) {
    return cube(name,[(x1-24)*.45,(64-y2)*.45,-depth/2],[(x2-24)*.45,(64-y1)*.45,depth/2],color,aux);
  }
  body('AUX_head',18,0,30,14,6.3,3);
  body('AUX_torso',17,14,31,37,3.6,1);
  body('AUX_left_arm',11,14,17,38,3.6,1);
  body('AUX_right_arm',31,14,37,38,3.6,0);
  body('AUX_left_leg',17,37,24,64,3.6,0);
  body('AUX_right_leg',24,37,31,64,3.6,1);
  cube('AUX_hair',[-2.7,27.45,3.16],[2.7,28.8,3.18],4,aux);
  cube('AUX_left_eye',[-1.35,25.2,3.18],[-.9,25.65,3.19],4,aux);
  cube('AUX_right_eye',[.9,25.2,3.18],[1.35,25.65,3.19],4,aux);
  cube('AUX_mouth',[-.9,23.85,3.18],[.9,24.3,3.19],0,aux);
  cube('AUX_left_boot',[-3.3,0,-2.0],[0,1.1,2.0],4,aux);
  cube('AUX_right_boot',[0,0,-2.0],[3.3,1.1,2.0],4,aux);
  const plane=new Cube({name:'KINDLE_plane_1.35x1.80m_F11',from:[-10.8,0,5.44],to:[10.8,28.8,5.44],box_uv:false,shade:false}).addTo(fx).init();
  for(const [side,face] of Object.entries(plane.faces)) face.extend({texture:side==='south'?fire.uuid:null,uv:[0,640,48,704]});
  added.push(plane);
  cube('AUX_ground',[-16,-.35,-12],[16,0,12],6,floor);
  for(let i=-16;i<=16;i+=8) cube('AUX_ground_grid_x_'+i,[i,-.01,-12],[i+.06,.01,12],0,floor);
  for(let i=-8;i<=8;i+=8) cube('AUX_ground_grid_z_'+i,[-16,-.01,i],[16,.01,i+.06],0,floor);
  Project.name='fire_kindle_v2_candidate_preview';
  Project.texture_width=48;Project.texture_height=1216;
  Project.credit='candidate_pending_user_confirmation | fire_kindle_v2_art | 20261116-art-kindle-01 | artist 01a1e08a-817e-7b60-b465-7a8baaca1f4b';
  Undo.finishEdit('Build isolated Kindle v2 candidate preview',{elements:added,textures:texs,outliner:true});
  await new Promise(resolve=>setTimeout(resolve,400));
  Canvas.updateAll();
  unselectAllElements();
  Preview.selected.loadAnglePreset({position:[0,14.4,65],target:[0,14.4,0],projection:'orthographic',zoom:.55});
  Preview.selected.controls.update();Preview.selected.render();
  return {project:Project.uuid,elements:added.length,textures:Texture.all.map(t=>({name:t.name,width:t.width,height:t.height,error:t.error})),units_per_block:16,target_height_units:28.8,plane_width:21.6,plane_height:28.8,plane_front_z:5.44};
})()
