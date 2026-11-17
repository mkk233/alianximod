(async () => {
  const prior = globalThis.coolV42Review;
  if (!prior || Project.uuid !== prior.project) throw new Error('Expected cooling v4.2 project');
  const root = 'D:/alianximod/docs/资源库/过程归档/冷却_等宽蒸汽_v4.3-20261112';
  const fs = require('fs');
  Undo.initEdit({textures: Object.values(prior.assets), elements: prior.planes});
  for (const [key, texture] of Object.entries(prior.assets)) {
    const slug = key === 'wave' ? 'shockwave' : key;
    texture.name = 'cool_v4_3_' + slug + '_strip.png';
    texture.fromPath(root + '/' + texture.name);
  }
  prior.planes.forEach(p => {p.name = p.name.replace(/^V4(?:_[123])?_/, 'V4_3_');});
  Undo.finishEdit('Apply v4.3 equal-width steam artwork');
  await new Promise(resolve => setTimeout(resolve, 600));
  if (Object.values(prior.assets).some(t => t.frameCount !== 16)) throw new Error('Texture frame count mismatch');
  const r = {...prior, root};
  const freshView = Preview.all.find(p => p.id.includes('cool_v43_review'));
  if (!freshView || freshView.renderer.getContext().isContextLost()) {
    throw new Error('Create a fresh cool_v43_review MCP viewport before capture');
  }
  r.preview = freshView;
  r.preview.setProjectionMode(false);
  r.preview.camPers.fov = 38;
  r.preview.camPers.updateProjectionMatrix();
  r.preview.renderer.setClearColor('#17252e', 1);
  r.applyFrame = f => {
    TextureAnimator.stop();
    Object.values(r.assets).forEach(t => {t.currentFrame = f;});
    r.planes.forEach(p => Canvas.updateUV(p));
    r.preview.render();
  };
  globalThis.coolV43Review = r;
  Project.name = 'cool-v4.3-equal-width-steam-ground-wave-preview';
  fs.mkdirSync(root + '/blockbench_frames', {recursive: true});
  Canvas.updateAll();
  r.applyFrame(9);
  Project.save_path = root + '/cool_v4_3_spatial.bbmodel';
  const raw = Codecs.project.compile();
  fs.writeFileSync(Project.save_path, typeof raw === 'string' ? raw : JSON.stringify(raw));
  Project.saved = true;
  const screenshot = path => new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('Viewport capture timeout')), 4000);
    r.preview.screenshot({crop: false}, data => {
      clearTimeout(timer);
      const bytes = Buffer.from(data.split(',')[1], 'base64');
      if (bytes.length < 5000) { reject(new Error('Unexpected empty viewport image')); return; }
      fs.writeFileSync(path, bytes);
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
    await screenshot(root + '/cool_v4_3_' + name + '.png');
  }
  r.preview.camPers.position.set(54, 49, 98);
  r.preview.controls.target.set(0, 13, 0); r.preview.controls.update(); r.applyFrame(9);
  const main = Preview.all.find(p => p.id === 'main');
  if (main) main.render();
  const wave = r.planes.find(p => p.name.includes('ground_level'));
  return {frameCount: 16, waveHeight: wave ? wave.from[1] : null,
    model: Project.save_path, screenshot: root + '/cool_v4_3_spatial.png',
    silhouette: 'equal-width upper-body, waist and ankle steam envelope'};
})()

