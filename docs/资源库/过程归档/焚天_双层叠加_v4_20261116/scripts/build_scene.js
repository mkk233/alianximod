(async function () {
  const expected='349751a0-a77d-026b-4ead-bc3a553fd738';
  if(!Project || Project.uuid!==expected || Outliner.elements.length) throw new Error('Wrong or nonempty project; stop');
  const root='D:/alianximod/docs/资源库/过程归档/焚天_双层叠加_v4_20261116';
  const archive='D:/alianximod/docs/资源库/特效/火焰/焚天_双层叠加_v4';
  const filesystem=require('fs');
  for(const folder of ['textures','previews','blockbench','evidence']) filesystem.mkdirSync(root+'/'+folder,{recursive:true});
  filesystem.copyFileSync('D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/textures/aux_mannequin_palette.png',root+'/textures/aux_mannequin_palette.png');
  const protectedProjects=ModelProject.all.filter(project=>project!==Project).map(project=>({uuid:project.uuid,name:project.name,saved:project.saved,elements:project.elements.length,textures:project.textures.length}));
  const elements=[],textures=[];
  Undo.initEdit({elements,textures,outliner:true});
  const palette=new Texture({name:'aux_mannequin_palette.png',uv_width:8,uv_height:8}).fromPath(root+'/textures/aux_mannequin_palette.png').add(false);
  const background=new Texture({name:'background_fentian_v2-strip.png',uv_width:128,uv_height:7488,render_mode:'emissive',render_sides:'double'}).fromPath(archive+'/background_fentian_v2-strip.png').add(false);
  const foreground=new Texture({name:'foreground_fentian_v3-strip.png',uv_width:48,uv_height:1216,render_mode:'emissive',render_sides:'double'}).fromPath(archive+'/foreground_fentian_v3-strip.png').add(false);
  textures.push(palette,background,foreground);
  const target=new Group({name:'AUX_single_target_H1.8_bbox0.6',origin:[0,0,0]}).init();
  function box(name,from,to,color) {
    if(Project.uuid!==expected) throw new Error('Project takeover; stop');
    const cube=new Cube({name:'AUX_'+name,from,to,box_uv:false}).addTo(target).init();
    for(const face of Object.values(cube.faces)) face.extend({texture:palette.uuid,uv:[color,0,color+1,8]});
    elements.push(cube);
    return cube;
  }
  box('head',[-3.6,21.6,-3.6],[3.6,28.8,3.6],3);
  box('torso',[-2.4,10.8,-2.4],[2.4,21.6,2.4],1);
  box('left_arm',[-4.8,10.8,-2.4],[-2.4,21.6,2.4],1);
  box('right_arm',[2.4,10.8,-2.4],[4.8,21.6,2.4],1);
  box('left_leg',[-2.4,0,-2.4],[-.24,10.8,2.4],0);
  box('right_leg',[.24,0,-2.4],[2.4,10.8,2.4],0);
  box('left_eye',[-2.4,24.96,3.61],[-1.44,25.92,3.62],4);
  box('right_eye',[1.44,24.96,3.61],[2.4,25.92,3.62],4);
  box('ground',[-22,-.35,-12],[22,0,12],6);
  const backGroup=new Group({name:'BACKGROUND_CAMERA_PLANE',origin:[0,12.96,0]}).init();
  const frontGroup=new Group({name:'FOREGROUND_CAMERA_PLANE',origin:[0,14.4,0]}).init();
  const backPlane=new Cube({name:'BACKGROUND_52',from:[-17.28,-6.48,0],to:[17.28,32.4,0],shade:false,box_uv:false}).addTo(backGroup).init();
  const frontPlane=new Cube({name:'FOREGROUND_19',from:[-10.8,0,5.44],to:[10.8,28.8,5.44],shade:false,box_uv:false}).addTo(frontGroup).init();
  for(const [side,face] of Object.entries(backPlane.faces)) face.extend({texture:side==='south'?background.uuid:null,uv:[0,432,128,576]});
  for(const [side,face] of Object.entries(frontPlane.faces)) face.extend({texture:side==='south'?foreground.uuid:null,uv:[0,640,48,704]});
  elements.push(backPlane,frontPlane);
  Project.credit='approved_for_recipient_integration | 20261116-art-fentian-dual-layer-05 | reused original pixels';
  Project.texture_width=128;Project.texture_height=7488;
  Undo.finishEdit('Create authorized same-target dual layer',{elements,textures,outliner:true});
  await new Promise(resolve=>setTimeout(resolve,500));
  if(Project.uuid!==expected) throw new Error('Project takeover; stop');
  Canvas.updateAll();unselectAllElements();
  filesystem.writeFileSync(root+'/evidence/protected-projects-before.json',JSON.stringify({projects:protectedProjects},null,2)+'\n');
  return {uuid:Project.uuid,elements:Outliner.elements.length,groups:Group.all.length,textures:Texture.all.map(texture=>({name:texture.name,loaded:texture.img.complete,error:texture.error})),protected_count:protectedProjects.length};
})()
