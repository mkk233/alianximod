"""Assemble existing Aseprite pixels/native screenshots; no drawing of effect art."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json, hashlib, sys, base64, io

ROOT = Path(__file__).resolve().parents[1]
REPO = Path('D:/alianximod')
RECEIPT = REPO / 'docs/开发记录/术式批量制作/待制作-20261031/detonate-v2-art-preview-20261116.md'
SNAPSHOT = REPO / 'docs/开发记录/术式批量制作/待制作-20261031/detonate-v2-input-snapshot-20261116.json'
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def write_json(path, obj): path.write_text(json.dumps(obj, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
def font(n): return ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', n)
def panel_text(draw, xy, text, size=20, color='#dce7ed'): draw.text(xy,text,font=font(size),fill=color)
def flatten(im):
    bg=Image.new('RGBA',im.size,'#171e27');bg.alpha_composite(im.convert('RGBA'));return bg.convert('RGB')

if '--manifest-only' not in sys.argv:
    for folder, name in [('gui_frames','detonate_v2_native_complete.gif'),('strength_frames','detonate_v2_native_strengths.gif')]:
        frames=[flatten(Image.open(ROOT/'previews'/folder/f'frame_{f:02}.png')) for f in range(1,17)]
        # One global palette avoids palette flicker. Original screenshot PNGs remain untouched.
        sample=Image.new('RGB',(frames[0].width*4,frames[0].height*4))
        for i,im in enumerate(frames):sample.paste(im,((i%4)*im.width,(i//4)*im.height))
        palette=sample.quantize(colors=256,method=Image.Quantize.MEDIANCUT)
        indexed=[im.quantize(palette=palette,dither=Image.Dither.NONE) for im in frames]
        indexed[0].save(ROOT/'previews'/name,save_all=True,append_images=indexed[1:],duration=[50]*15+[650],loop=0,disposal=2,optimize=False)

    keys=[1,3,5,7,9,11,13,16]
    board=Image.new('RGB',(1280,580),'#171e27');d=ImageDraw.Draw(board)
    panel_text(d,(24,16),'引爆 v2 · 短促火团 / 同形橙火与幽冥蓝火',26)
    panel_text(d,(24,54),'64×64 · 16帧×50ms · 单次0.8秒 · 候选待确认',17,'#97aebd')
    for row,variant in enumerate(['orange','soul']):
        y=114+row*220
        panel_text(d,(24,y-20),'普通橙火' if row==0 else '幽冥蓝火',17,'#ffb950' if row==0 else '#6ce6e7')
        for j,f in enumerate(keys):
            image=Image.open(ROOT/'frames'/variant/f'frame_{f:02}.png').convert('RGBA').resize((128,128),Image.Resampling.NEAREST)
            board.paste(image,(16+j*158,y+8),image)
            panel_text(d,(24+j*158,y+145),f'F{f:02} · {(f-1)*50}ms',16)
    panel_text(d,(24,548),'末帧全透明；消散期火屑依次减少。尺寸、挂点与强度映射须同版确认。',17,'#97aebd')
    board.save(ROOT/'previews/keyframes.png')

    strength=Image.new('RGB',(864,424),'#171e27');d=ImageDraw.Draw(strength)
    panel_text(d,(18,12),'同源强度对照 · 示例实际消耗 / 候选面片边长',21)
    for j,(spent,side) in enumerate([(10,.8),(50,1.2),(100,1.6)]):
        panel_text(d,(40+j*288,49),f'S={spent}  /  {side:.2f}格',19)
    strength.paste(Image.open(ROOT/'previews/strength_compare_unlabelled.png'),(0,78))
    strength.save(ROOT/'previews/strength_comparison.png')

    proof=Image.new('RGB',(1200,920),'#171e27');d=ImageDraw.Draw(proof)
    panel_text(d,(20,15),'真实 Blockbench 视口逐帧样本 · 两种颜色 / 实际消耗50',24)
    for i,f in enumerate(keys):
        im=flatten(Image.open(ROOT/'previews/gui_frames'/f'frame_{f:02}.png')).resize((300,213),Image.Resampling.NEAREST)
        x=(i%4)*300;y=70+(i//4)*400
        proof.paste(im,(x,y));panel_text(d,(x+12,y+225),f'F{f:02} · {(f-1)*50}ms',18)
    panel_text(d,(20,874),'PNG 来自桌面主视口；GIF 的0.6秒空白停顿仅便于重复观看。',18,'#97aebd')
    proof.save(ROOT/'previews/native_animation_keyframes.png')

    ordinary=Image.open(ROOT/'textures/detonate_v2_orange-strip.png').convert('RGBA')
    blue=Image.open(ROOT/'textures/detonate_v2_soul-strip.png').convert('RGBA')
    orange=['D2310D','F5540D','FF8B14','FFC129','FFE367','FFF8B7']
    soul=['085C66','0795A2','0EC4CC','44EDE7','A7FFF0','E2FFF4']
    rgb=lambda s:tuple(bytes.fromhex(s))
    lut={rgb(a):rgb(b) for a,b in zip(orange,soul)}
    assert ordinary.size==blue.size==(64,1024)
    assert ordinary.getchannel('A').tobytes()==blue.getchannel('A').tobytes()
    assert all(a[3]==0 or (lut[a[:3]]==b[:3] and a[3]==b[3]) for a,b in zip(ordinary.getdata(),blue.getdata()))
    bounds=[]
    for f in range(1,17):
        a=ordinary.crop((0,(f-1)*64,64,f*64))
        b=blue.crop((0,(f-1)*64,64,f*64))
        assert a.tobytes()==Image.open(ROOT/f'frames/orange/frame_{f:02}.png').convert('RGBA').tobytes()
        assert b.tobytes()==Image.open(ROOT/f'frames/soul/frame_{f:02}.png').convert('RGBA').tobytes()
        bounds.append({'frame':f,'alpha_bbox':a.getchannel('A').getbbox()})
    assert bounds[-1]['alpha_bbox'] is None
    bb=json.loads((ROOT/'blockbench/fire_detonate_v2_candidate.bbmodel').read_text(encoding='utf-8'))
    embeds=[]
    for t in bb['textures']:
        if t['name'] in ['detonate_v2_orange-strip.png','detonate_v2_soul-strip.png']:
            decoded=Image.open(io.BytesIO(base64.b64decode(t['source'].split(',',1)[1]))).convert('RGBA')
            source=Image.open(ROOT/'textures'/t['name']).convert('RGBA')
            assert decoded.tobytes()==source.tobytes()
            embeds.append({'name':t['name'],'embedded_decoded_pixels_equal_source':True})
    gifs=[]
    for name in ['detonate_v2_native_complete.gif','detonate_v2_native_strengths.gif','target_comparison_2d.gif']:
        im=Image.open(ROOT/'previews'/name);timings=[]
        for i in range(im.n_frames): im.seek(i);timings.append(im.info.get('duration'))
        assert sum(timings)==1400
        gifs.append({'name':name,'frames':im.n_frames,'durations_ms':timings,'total_ms':sum(timings)})
    (ROOT/'evidence/input-snapshot-used.json').write_bytes(SNAPSHOT.read_bytes())
    audit={'source_pixels_verified_by':'Aseprite MCP verify_sources.lua','two_variant_alpha_equal':True,'soul_palette_exact':True,'frame_exports_equal_strips':True,'alpha_bounds':bounds,'bbmodel_embedded_textures':embeds,'gif_timings':gifs,'effects_not_exported_from_headless_renderer':True,'native_viewport_pngs_unmodified':True,'gif_processing':'native screenshot sequence composited over solid dark preview background; global 256-color GIF palette, no dithering; no shape/pixel repaint', 'reopen_first_immediate_capture':'blank because native project load UI had not settled; one later native capture after 700ms succeeded; no asset changes','formal_review_added':False}
    write_json(ROOT/'evidence/asset-verification.json',audit)

artifacts=[]
for p in sorted(ROOT.rglob('*')):
    if not p.is_file() or p.name=='manifest.json':continue
    item={'path':p.relative_to(ROOT).as_posix(),'bytes':p.stat().st_size,'sha256':sha(p)}
    if p.suffix.lower() in ['.png','.gif']:
        im=Image.open(p);item.update(width=im.width,height=im.height,frames=getattr(im,'n_frames',1))
    artifacts.append(item)
manifest={
 'version':'fire_detonate_v2_candidate_20261116',
 'status':'candidate_pending_user_confirmation',
 'batchId':'待制作-20261031','taskId':'fire_detonate_v2_art','attemptId':'20261116-art-detonate-03',
 'parentAgentId':'01a1ce06-b681-7c90-b11d-260d3c169911','parentIdentitySource':str(SNAPSHOT),
 'authorAgentId':None,'authorBackendModel':'not independently exposed; requested art fallback gpt-6-astra max',
 'fallbacksReportedByParent':[{'model':'antigravity/gemini-4-carbon','error':'Unknown model','agent_id':None,'writes':0},{'model':'antigravity/gemini-4-argon','error':'Unknown model','agent_id':None,'writes':0}],
 'reviewBudget':{'design':'2/2','implementation':'3/3','additional_review':False,'note':'本阶段额度已用完，返修结果未追加复审'},
 'input':{'snapshot_path':str(SNAPSHOT),'snapshot_sha256':sha(SNAPSHOT),'workbook_sha256':'1473D5EF076D4B8B19A59CBCC92DF3A394CDD888FB03E4B4025AC250D1F264B1','sheet':'制作中','row':9,'technique':'fire_detonate'},
 'animation':{'width':64,'height':64,'frame_count':16,'duration_per_frame_ms':50,'total_ms':800,'playback':'once per qualifying target hit','strip_layout':'vertical, frame1 at top','strip_width':64,'strip_height':1024,'uv':'u=0..1; v=(f-1)/16..f/16; f=1..16','last_frame_transparent':True,'preview_only_pause_ms':600,'preview_gif_loop':True},
 'spatial':{'blockbench_app':'Desktop 5.2.2','plugin':'1.10.1','model_units_per_block':16,'target_height_units':28.8,'target_height_blocks':1.8,'axes':'+X target right, +Y up, +Z reference front/camera','preview_hit_point_relative_target_feet':[0,16,1.8],'runtime_proposed_anchor':'real carrier target hit world position, no added fixed Y1.0','camera_bias_blocks':0.20,'camera_bias_units':3.2,'origin':'center of fire billboard after camera bias','thickness':0,'rotation':'CAMERA_PLANE follows current viewer; side-depth shot deliberately freezes plane for measurement','lifetime_binding':'fixed world hit point for 16 ticks; not attached to target motion','front_camera_position':[58,44,165],'front_camera_target':[0,17,0],'depth_camera_position':[68,39,45],'depth_camera_target':[-20,16,1.8]},
 'intensity_candidate':{'basis':'actual remaining S consumed by this target on this real release, not prepaid or duplicated','sample_spent':[0,10,50,100],'sample_quad_side_blocks':[0,0.8,1.2,1.6],'zero_behavior':'no extra detonate spawn','positive_mapping_candidate':'0<S<=10: L=0.60+0.02*S; 10<S<=50: L=0.80+0.01*(S-10); S>50: L=1.20+0.008*(S-50). Values above 100 are an unpreviewed extrapolation pending confirmation; no resource or damage cap is implied.','does_not_change_damage':True,'duration_and_opacity_constant':True},
 'material':{'runtime_proposed':'entityTranslucent + FULL_BRIGHT; depth-tested; white vertex RGB','alpha':'hard opaque palette pixels with transparent outside; no soft glow','filter':'nearest neighbor, blur=false, clamp=true','bb_preview':{'SHADE':False,'EMISSIVE':False,'transparent':True,'depthTest':True,'depthWrite':False,'renderOrder':10},'blue_texture_no_double_tint':True},
 'variants':{'orange':['#D2310D','#F5540D','#FF8B14','#FFC129','#FFE367','#FFF8B7'],'soul':['#085C66','#0795A2','#0EC4CC','#44EDE7','#A7FFF0','#E2FFF4'],'geometry_and_timing':'identical','soul_reference':'docs/资源库/过程归档/幽冥蓝火重配色-20261116/visual-handoff.md'},
 'auxiliary_objects_not_runtime':['AUX_*','MC mannequins','ground grid','labels','comparison groups','measurement marker/rod','whole .bbmodel scene'],
 'state':{'art_candidate_produced':True,'user_confirmed':False,'integrated':False,'compiled':False,'Forge_verified':False,'game_started':False,'spreadsheet_written':False,'index_written':False,'src_written':False,'all_writes_confined':True,'subagents_spawned':0},
 'runtime_gap':'Current supplied BatchTriggerVisuals uses load.detonate, fixed halfSpan .6, hit+Y1, host=-1; does not read spent. Future Grok integration must use actual spent, suppress zero, and bind confirmed geometry/blue mode. Not implemented by this art task.',
 'receipt':{'path':str(RECEIPT),'sha256':sha(RECEIPT) if RECEIPT.exists() else None},
 'artifacts':artifacts,
 'manifest_self_hash':'excluded to avoid recursive hash; all other delivered files listed'
}
write_json(ROOT/'manifest.json',manifest)
print(json.dumps({'files':len(artifacts),'root':str(ROOT),'key_hashes':{p:sha(ROOT/p) for p in ['source/detonate_v2_orange.aseprite','source/detonate_v2_soul.aseprite','textures/detonate_v2_orange-strip.png','textures/detonate_v2_soul-strip.png','blockbench/fire_detonate_v2_candidate.bbmodel','previews/detonate_v2_native_complete.gif']},'receipt_written':RECEIPT.exists()},ensure_ascii=False))
