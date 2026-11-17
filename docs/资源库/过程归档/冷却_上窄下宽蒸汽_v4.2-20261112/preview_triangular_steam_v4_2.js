(async () => {
  const prior = globalThis.coolV41Review;
  if (!prior || Project.uuid !== prior.project) throw new Error('Expected cooling v4.1 project');
  const root = 'D:/alianximod/docs/资源库/过程归档/冷却_上窄下宽蒸汽_v4.2-20261112';
  const fs = require('fs');
  Undo.initEdit({textures: Object.values(prior.assets), elements: prior.planes});
  for (const [key, texture] of Object.entries(prior.assets)) {
    const slug = key === 'wave' ? 'shockwave' : key;
    texture.name = 'cool_v4_2_' + slug + '_strip.png';
    texture.fromPath(root + '/' + texture.name);
  }
  prior.planes.forEach(p => {p.name = p.name.replace(/^V4(?:_1)?_/, 'V4_2_');});
  Undo.finishEdit('Apply v4.2 narrow-top wide-base steam artwork');
  await new Promise(resolve => setTimeout(resolve, 600));
  if (Object.values(prior.assets).some(t => t.frameCount !== 16)) throw new Error('Texture frame count mismatch');
  const r = {...prior, root};
  globalThis.coolV42Review = r;
  Project.name = 'cool-v4.2-triangular-steam-ground-wave-preview';
  fs.mkdirSync(root + '/blockbench_frames', {recursive: true});
  Canvas.updateAll();
  r.applyFrame(9);
  Project.save_path = root + '/cool_v4_2_spatial.bbmodel';
  const raw = Codecs.project.compile();
  fs.writeFileSync(Project.save_path, typeof raw === 'string' ? raw : JSON.stringify(raw));
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
  for (const [name, position, target, frame] of [
    ['spatial', [54, 49, 98], [0, 13, 0], 9],
    ['spatial_high_angle', [18, 92, 58], [0, 9, 0], 10],
    ['spatial_side', [92, 30, 24], [0, 14, 0], 9],
    ['spatial_front', [0, 32, 112], [0, 16, 0], 9]
  ]) {
    r.preview.camPers.position.set(...position);
    r.preview.controls.target.set(...target); r.preview.controls.update();
    r.applyFrame(frame);
    await screenshot(root + '/cool_v4_2_' + name + '.png');
  }
  r.preview.camPers.position.set(54, 49, 98);
  r.preview.controls.target.set(0, 13, 0); r.preview.controls.update(); r.applyFrame(9);
  const main = Preview.all.find(p => p.id === 'main');
  if (main) main.render();
  const wave = r.planes.find(p => p.name.includes('ground_level'));
  return {frameCount: 16, waveHeight: wave ? wave.from[1] : null,
    model: Project.save_path, screenshot: root + '/cool_v4_2_spatial.png',
    silhouette: 'narrow top, progressively wider waist and broad ankle-level base'};
})()
