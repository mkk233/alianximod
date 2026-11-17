(async () => {
  const root = 'D:/alianximod/docs/资源库/过程归档/冷却_下压蒸汽冲击波_v4-20261112';
  if (Project.name !== 'cool-v4-downward-vapour-shockwave-preview') throw new Error('Unexpected active project');
  const fs = require('fs');
  const p = Preview.all.find(p => p.id.includes('cool_v4_review'));
  if (!p) throw new Error('Review viewport missing');
  Undo.initEdit({elements: [], textures: [], outliner: true});
  const helpers = new Group({name: 'REFERENCE_ONLY_player_and_ground', origin: [0, 0, 0]}).init();
  const vfx = new Group({name: 'V4_ASEPRITE_TEXTURES_16frames_75ms', origin: [0, 0, 0]}).init();
  const solid = (name, hex) => {
    const c = document.createElement('canvas'); c.width = 16; c.height = 16;
    const ctx = c.getContext('2d'); ctx.fillStyle = hex; ctx.fillRect(0, 0, 16, 16);
    return new Texture({name: 'REFERENCE_' + name, render_mode: 'default'}).fromDataURL(c.toDataURL()).add(false);
  };
  const tex = {
    head: solid('head', '#849ba4'), body: solid('shirt', '#425d70'),
    arm: solid('arms', '#516c7b'), legs: solid('trousers', '#34495e'),
    ground: solid('ground', '#24343e'), grid: solid('one_block_grid', '#3e525b'),
    eye: solid('eyes', '#253844')
  };
  const box = (name, from, to, texture, parent = helpers) => {
    const cube = new Cube({name, from, to, box_uv: false, autouv: 0}).addTo(parent).init();
    Object.values(cube.faces).forEach(face => {face.texture = texture.uuid; face.uv = [0, 0, 16, 16];});
    return cube;
  };
  box('REFERENCE_ground', [-48, -1, -48], [48, -0.12, 48], tex.ground);
  for (let d = -48; d <= 48; d += 16) {
    box('REFERENCE_grid_x_' + d, [-48, -0.11, d - 0.035], [48, -0.08, d + 0.035], tex.grid);
    box('REFERENCE_grid_z_' + d, [d - 0.035, -0.11, -48], [d + 0.035, -0.08, 48], tex.grid);
  }
  box('REFERENCE_head', [-4, 24, -4], [4, 32, 4], tex.head);
  box('REFERENCE_torso', [-4, 12, -2], [4, 24, 2], tex.body);
  box('REFERENCE_left_arm', [-8, 12, -2], [-4, 24, 2], tex.arm);
  box('REFERENCE_right_arm', [4, 12, -2], [8, 24, 2], tex.arm);
  box('REFERENCE_left_hand', [-8, 12, -2.02], [-4, 15, 2.02], tex.head);
  box('REFERENCE_right_hand', [4, 12, -2.02], [8, 15, 2.02], tex.head);
  box('REFERENCE_left_leg', [-4, 0, -2], [-0.4, 12, 2], tex.legs);
  box('REFERENCE_right_leg', [0.4, 0, -2], [4, 12, 2], tex.legs);
  box('REFERENCE_eye_L', [-2.7, 27.8, 4.01], [-1.4, 28.8, 4.04], tex.eye);
  box('REFERENCE_eye_R', [1.4, 27.8, 4.01], [2.7, 28.8, 4.04], tex.eye);
  const assets = {};
  for (const name of ['rear', 'front', 'fragments']) {
    assets[name] = new Texture({
      name: 'cool_v4_' + name + '_strip.png', uv_width: 64, uv_height: 64,
      frame_time: 1.5, frame_interpolate: false, render_mode: 'emissive', render_sides: 'double', force_translucent: true
    }).fromPath(root + '/cool_v4_' + name + '_strip.png').add(false);
  }
  assets.wave = new Texture({name: 'cool_v4_shockwave_strip.png', uv_width: 96, uv_height: 96, frame_time: 1.5, frame_interpolate: false, render_mode: 'emissive', render_sides: 'double', force_translucent: true}).fromPath(root + '/cool_v4_shockwave_strip.png').add(false);
  assets.wave.render_sides = 'double';
  const planes = [];
  const plane = (name, from, to, texture, face, uv) => {
    const q = new Cube({name, from, to, box_uv: false, autouv: 0, shade: false}).addTo(vfx).init();
    Object.values(q.faces).forEach(f => { f.texture = null; });
    q.faces[face].texture = texture.uuid; q.faces[face].uv = uv;
    planes.push(q); return q;
  };
  const s = 2 / 3;
  plane('V4_01_rear_steam', [-32*s, -8*s, -2.5], [32*s, 56*s, -2.5], assets.rear, 'south', [0, 0, 64, 64]);
  plane('V4_02_front_steam', [-32*s, -8*s, 2.7], [32*s, 56*s, 2.7], assets.front, 'south', [0, 0, 64, 64]);
  plane('V4_03_disappearing_wisps', [-32*s, -8*s, 2.9], [32*s, 56*s, 2.9], assets.fragments, 'south', [0, 0, 64, 64]);
  plane('V4_04_world_horizontal_expanding_wave', [-32, 10, -32], [32, 10, 32], assets.wave, 'up', [0, 0, 96, 96]);
  Undo.finishEdit('Build cooling v4 with Aseprite vapour and expanding pressure wave');
  await new Promise(resolve => setTimeout(resolve, 600));
  const applyFrame = f => {
    TextureAnimator.stop();
    Object.values(assets).forEach(t => {t.currentFrame = f;});
    planes.forEach(q => Canvas.updateUV(q));
    p.render();
  };
  globalThis.coolV4Review = {root, project: Project.uuid, assets, planes, preview: p, applyFrame};
  p.setProjectionMode(false);
  p.camPers.position.set(54, 49, 98); p.camPers.fov = 38; p.camPers.updateProjectionMatrix();
  p.controls.target.set(0, 13, 0); p.controls.update();
  p.renderer.setClearColor('#17252e', 1);
  Canvas.updateAll();
  applyFrame(8);
  const modelPath = root + '/cool_v4_spatial.bbmodel';
  const raw = Codecs.project.compile();
  fs.writeFileSync(modelPath, typeof raw === 'string' ? raw : JSON.stringify(raw));
  Project.save_path = modelPath; Project.saved = true;
  const frameDir = root + '/blockbench_frames';
  fs.mkdirSync(frameDir, {recursive: true});
  const screenshot = () => new Promise(resolve => p.screenshot({crop: false}, resolve));
  const shot = await screenshot();
  fs.writeFileSync(root + '/cool_v4_spatial.png', Buffer.from(shot.split(',')[1], 'base64'));
  return {
    project: Project.name, modelPath, screenshot: root + '/cool_v4_spatial.png',
    frames: Object.fromEntries(Object.entries(assets).map(([k, t]) => [k, t.frameCount])),
    elements: Outliner.elements.length, textureDimensions: Object.fromEntries(Object.entries(assets).map(([k,t]) => [k,[t.width,t.height,t.uv_width,t.uv_height]]))
  };
})()

