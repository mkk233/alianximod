(async function () {
  const root = 'D:/alianximod/docs/资源库/过程归档/引燃_全身燃烧重绘_v3_20261116';
  if (!Project || Project.uuid !== 'c136d4c5-fe9b-4cf8-f91c-3df017448ec0' || Outliner.elements.length) throw new Error('Wrong project or non-empty candidate; no mutation');
  const added = [], textures = [];
  Undo.initEdit({elements: added, textures, outliner: true});
  const palette = new Texture({name: 'AUX_reference_palette', uv_width: 8, uv_height: 8}).fromPath(root + '/textures/aux_mannequin_palette.png').add(false);
  const fire = new Texture({name: 'CANDIDATE_kindle_v3_19_frames', uv_width: 48, uv_height: 1216, render_mode: 'emissive', render_sides: 'double'}).fromPath(root + '/textures/kindle_v3_strip.png').add(false);
  textures.push(palette, fire);
  const target = new Group({name: 'AUX_normal_target_1.80m_not_runtime', origin: [0,0,0]}).init();
  const effect = new Group({name: 'CANDIDATE_CAMERA_PLANE_F11', origin: [0,14.4,0]}).init();
  const ground = new Group({name: 'AUX_ground_not_runtime', origin: [0,0,0]}).init();
  function cube(name, from, to, color, parent) {
    const element = new Cube({name, from, to, box_uv: false}).addTo(parent).init();
    for (const face of Object.values(element.faces)) face.extend({texture: palette.uuid, uv: [color,0,color+1,8]});
    added.push(element);
    return element;
  }
  function body(name, left, top, right, bottom, depth, color) {
    return cube(name, [(left-24)*.45,(64-bottom)*.45,-depth/2], [(right-24)*.45,(64-top)*.45,depth/2], color, target);
  }
  body('AUX_head',18,0,30,14,6.3,3);
  body('AUX_torso',17,14,31,37,3.6,1);
  body('AUX_left_arm',11,14,17,38,3.6,1);
  body('AUX_right_arm',31,14,37,38,3.6,1);
  body('AUX_left_leg',17,37,24,64,3.6,0);
  body('AUX_right_leg',24,37,31,64,3.6,0);
  cube('AUX_hair',[-2.7,27,3.16],[2.7,28.8,3.18],4,target);
  cube('AUX_left_eye',[-1.35,25.2,3.18],[-.45,26.1,3.19],4,target);
  cube('AUX_right_eye',[.9,25.2,3.18],[1.8,26.1,3.19],4,target);
  cube('AUX_mouth',[-.9,23.85,3.18],[.9,24.3,3.19],4,target);
  cube('AUX_left_boot',[-3.6,0,-2],[0,1.35,2],4,target);
  cube('AUX_right_boot',[0,0,-2],[3.6,1.35,2],4,target);
  const plane = new Cube({name: 'KINDLE_plane_1.35x1.80m_F11', from: [-10.8,0,5.44], to: [10.8,28.8,5.44], box_uv: false, shade: false}).addTo(effect).init();
  for (const [side, face] of Object.entries(plane.faces)) face.extend({texture: side === 'south' ? fire.uuid : null, uv: [0,640,48,704]});
  added.push(plane);
  cube('AUX_ground',[-16,-.35,-12],[16,0,12],6,ground);
  for (let coordinate=-16; coordinate<=16; coordinate+=8) cube('AUX_grid_x_'+coordinate,[coordinate,-.01,-12],[coordinate+.06,.01,12],0,ground);
  for (let coordinate=-8; coordinate<=8; coordinate+=8) cube('AUX_grid_z_'+coordinate,[-16,-.01,coordinate],[16,.01,coordinate+.06],0,ground);
  Project.texture_width=48;
  Project.texture_height=1216;
  Project.credit='candidate_pending_user_confirmation | fire_kindle_v2_art | 20261116-art-kindle-revision-03 | artist 01a1e08a-817e-7b60-b465-7a8baaca1f4b';
  Undo.finishEdit('Build isolated Kindle v3 preview',{elements: added,textures,outliner:true});
  await new Promise(resolve=>setTimeout(resolve,400));
  Canvas.updateAll();
  unselectAllElements();
  Preview.selected.loadAnglePreset({position:[0,14.4,65],target:[0,14.4,0],projection:'orthographic',zoom:.55});
  Preview.selected.controls.update();
  Preview.selected.render();
  return {project:Project.uuid,elements:added.length,textures:Texture.all.map(texture=>({name:texture.name,width:texture.width,height:texture.height,error:texture.error})),other_projects:ModelProject.all.filter(project=>project!==Project).map(project=>({uuid:project.uuid,name:project.name,saved:project.saved,elements:project.elements.length,textures:project.textures.length}))};
})()
