(async () => {
  const prior = globalThis.coolV4Review;
  if (!prior || Project.uuid !== prior.project) throw new Error('Expected active cooling v4 project');
  const fs = require('fs');
  const root = 'D:/alianximod/docs/资源库/过程归档/冷却_贴地冲击波_v4.1-20261112';
  const wave = prior.planes.find(p => p.name.includes('world_horizontal_expanding_wave'));
  if (!wave) throw new Error('Shockwave plane missing');
  const previousY = wave.from[1];
  Undo.initEdit({elements: [wave], textures: Object.values(prior.assets), outliner: true});
  wave.from[1] = 1.2;
  wave.to[1] = 1.2;
  wave.name = 'V4_1_04_ground_level_expanding_wave_y1_2';
  for (const [key, texture] of Object.entries(prior.assets)) {
    const slug = key === 'wave' ? 'shockwave' : key;
    texture.name = 'cool_v4_1_' + slug + '_strip.png';
    texture.fromPath(root + '/' + texture.name);
  }
  Undo.finishEdit('Lower cooling shockwave to 1.2 units above reference ground');
  await new Promise(resolve => setTimeout(resolve, 600));
  for (const texture of Object.values(prior.assets)) {
    if (texture.frameCount !== 16) throw new Error('Animated texture did not load');
  }
  Project.name = 'cool-v4.1-ground-level-shockwave-preview';
  const r = {...prior, root};
  globalThis.coolV41Review = r;
  fs.mkdirSync(root + '/blockbench_frames', {recursive: true});
  Canvas.updateAll();
  r.applyFrame(8);
  const modelPath = root + '/cool_v4_1_spatial.bbmodel';
  Project.save_path = modelPath;
  const raw = Codecs.project.compile();
  fs.writeFileSync(modelPath, typeof raw === 'string' ? raw : JSON.stringify(raw));
  Project.saved = true;
  const screenshot = path => new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('Viewport capture timeout')), 4000);
    r.preview.screenshot({crop: false}, data => {
      clearTimeout(timer);
      fs.writeFileSync(path, Buffer.from(data.split(',')[1], 'base64'));
      resolve(path);
    });
  });
  r.preview.camPers.position.set(54, 49, 98);
  r.preview.controls.target.set(0, 13, 0); r.preview.controls.update();
  for (let f = 0; f < 16; f++) {
    r.applyFrame(f);
    await screenshot(root + '/blockbench_frames/frame_' + String(f + 1).padStart(2, '0') + '.png');
  }
  r.applyFrame(11);
  await screenshot(root + '/cool_v4_1_spatial.png');
  r.preview.camPers.position.set(18, 92, 58);
  r.preview.controls.target.set(0, 9, 0); r.preview.controls.update();
  r.applyFrame(11);
  await screenshot(root + '/cool_v4_1_spatial_high_angle.png');
  r.preview.camPers.position.set(92, 30, 24);
  r.preview.controls.target.set(0, 14, 0); r.preview.controls.update();
  r.applyFrame(11);
  await screenshot(root + '/cool_v4_1_spatial_side.png');
  r.preview.camPers.position.set(54, 49, 98);
  r.preview.controls.target.set(0, 13, 0); r.preview.controls.update();
  r.applyFrame(11);
  const main = Preview.all.find(p => p.id === 'main');
  if (main) main.render();
  return {previousY, currentY: wave.from[1], referenceBlocks: wave.from[1] / 16,
    frameCount: 16, modelPath, screenshot: root + '/cool_v4_1_spatial.png',
    phaseOrder: 'descending steam first, ground-level shockwave from 450ms'};
})()
