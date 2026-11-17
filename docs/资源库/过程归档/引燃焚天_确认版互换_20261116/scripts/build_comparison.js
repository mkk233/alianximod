(async function () {
  const root='D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116';
  if(!Project || Project.uuid!=='d8549d69-5160-499c-12b6-87d88f8bc273' || Outliner.elements.length) throw new Error('Wrong or non-empty project; no mutation');
  const filesystem=require('fs');
  for(const folder of ['textures','previews','blockbench','evidence']) filesystem.mkdirSync(root+'/'+folder,{recursive:true});
  filesystem.copyFileSync('D:/alianximod/docs/资源库/过程归档/引燃_全身燃烧重绘_v3_20261116/textures/aux_mannequin_palette.png',root+'/textures/aux_mannequin_palette.png');
  const protectedProjects=ModelProject.all.filter(project=>project!==Project).map(project=>({uuid:project.uuid,name:project.name,saved:project.saved,elements:project.elements.length,textures:project.textures.length}));
  const elements=[],textures=[];
  Undo.initEdit({elements,textures,outliner:true});
  const palette=new Texture({name:'AUX_palette',uv_width:8,uv_height:8}).fromPath(root+'/textures/aux_mannequin_palette.png').add(false);
  const kindle=new Texture({name:'APPROVED_kindle_v4_52_frames',uv_width:128,uv_height:7488,render_mode:'emissive',render_sides:'double'}).fromPath('D:/alianximod/docs/资源库/特效/火焰/引燃_承接焚天_v4/kindle_v4-strip.png').add(false);
  const fentian=new Texture({name:'APPROVED_fentian_v3_19_frames',uv_width:48,uv_height:1216,render_mode:'emissive',render_sides:'double'}).fromPath('D:/alianximod/docs/资源库/特效/火焰/焚天_全身烈焰_v3/fentian_v3-strip.png').add(false);
  textures.push(palette,kindle,fentian);
  function box(name,from,to,color,parent) {
    const element=new Cube({name,from,to,box_uv:false}).addTo(parent).init();
    for(const face of Object.values(element.faces)) face.extend({texture:palette.uuid,uv:[color,0,color+1,8]});
    elements.push(element);
    return element;
  }
  function target(name,offsetX,offsetZ) {
    const parent=new Group({name:'AUX_'+name+'_normal_target_1.80m',origin:[offsetX,0,offsetZ]}).init();
    function part(label,left,top,right,bottom,depth,color) {
      return box('AUX_'+name+'_'+label,[(left-24)*.45+offsetX,(64-bottom)*.45,offsetZ-depth/2],[(right-24)*.45+offsetX,(64-top)*.45,offsetZ+depth/2],color,parent);
    }
    part('head',18,0,30,14,6.3,3);
    part('torso',17,14,31,37,3.6,1);
    part('left_arm',11,14,17,38,3.6,1);
    part('right_arm',31,14,37,38,3.6,1);
    part('left_leg',17,37,24,64,3.6,0);
    part('right_leg',24,37,31,64,3.6,0);
    box('AUX_'+name+'_hair',[offsetX-2.7,27,offsetZ+3.16],[offsetX+2.7,28.8,offsetZ+3.18],4,parent);
    box('AUX_'+name+'_left_eye',[offsetX-1.35,25.2,offsetZ+3.18],[offsetX-.45,26.1,offsetZ+3.19],4,parent);
    box('AUX_'+name+'_right_eye',[offsetX+.9,25.2,offsetZ+3.18],[offsetX+1.8,26.1,offsetZ+3.19],4,parent);
    box('AUX_'+name+'_mouth',[offsetX-.9,23.85,offsetZ+3.18],[offsetX+.9,24.3,offsetZ+3.19],4,parent);
    box('AUX_'+name+'_left_boot',[offsetX-3.6,0,offsetZ-2],[offsetX,1.35,offsetZ+2],4,parent);
    box('AUX_'+name+'_right_boot',[offsetX,0,offsetZ-2],[offsetX+3.6,1.35,offsetZ+2],4,parent);
  }
  target('LEFT_KINDLE',-23,-12);
  target('RIGHT_FENTIAN',23,12);
  const kindleGroup=new Group({name:'APPROVED_LEFT_KINDLE_CAMERA_PLANE',origin:[-23,12.96,-12]}).init();
  const fentianGroup=new Group({name:'APPROVED_RIGHT_FENTIAN_CAMERA_PLANE',origin:[23,14.4,12]}).init();
  const kindlePlane=new Cube({name:'KINDLE_2.16x2.43m_center_0.81m_F04',from:[-40.28,-6.48,-12],to:[-5.72,32.4,-12],box_uv:false,shade:false}).addTo(kindleGroup).init();
  const fentianPlane=new Cube({name:'FENTIAN_1.35x1.80m_F11',from:[12.2,0,17.44],to:[33.8,28.8,17.44],box_uv:false,shade:false}).addTo(fentianGroup).init();
  for(const [side,face] of Object.entries(kindlePlane.faces)) face.extend({texture:side==='south'?kindle.uuid:null,uv:[0,432,128,576]});
  for(const [side,face] of Object.entries(fentianPlane.faces)) face.extend({texture:side==='south'?fentian.uuid:null,uv:[0,640,48,704]});
  elements.push(kindlePlane,fentianPlane);
  const ground=new Group({name:'AUX_ground_not_runtime',origin:[0,0,0]}).init();
  box('AUX_ground',[-48,-.35,-30],[48,0,30],6,ground);
  Project.credit='approved_for_recipient_integration | left fire_kindle receives old fentian v2 | right fire_fentian receives full-body v3 | 20261116-art-kindle-fentian-swap-04';
  Project.texture_width=128;Project.texture_height=7488;
  Undo.finishEdit('Create approved recipient comparison',{elements,textures,outliner:true});
  await new Promise(resolve=>setTimeout(resolve,500));
  Canvas.updateAll();
  unselectAllElements();
  Preview.selected.loadAnglePreset({position:[0,16,110],target:[0,16,0],projection:'orthographic',zoom:.35});
  Preview.selected.controls.update();Preview.selected.render();
  filesystem.writeFileSync(root+'/evidence/protected-projects-before.json',JSON.stringify({status:'approved_for_recipient_integration',projects:protectedProjects},null,2)+'\n');
  return {project:Project.uuid,elements:elements.length,textures:Texture.all.map(texture=>({name:texture.name,width:texture.width,height:texture.height,error:texture.error})),protected_projects:protectedProjects};
})()
