from pathlib import Path
from PIL import Image,ImageSequence
import json,hashlib,struct,datetime

root=Path(__file__).resolve().parents[1]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest().upper()
guards={
 'D:/alianximod/docs/术式制作表.xlsx':'87F22C7DCE22FD0AB83F219C1D18938AF381D2286C106A62CEBCA1A6A56E9C75',
 'D:/alianximod/docs/资源库/特效/大地/崩地_v1/ground_dust.aseprite':'3B518E0FD4D3272D05C4A377987D018EA7F9E37AB603ED798AFDFE5B507F0C07',
 'D:/alianximod/docs/资源库/特效/大地/崩地_v1/ground_dust-strip.png':'0C7479F9F3DC02E3216D7EBF6A195C98730CF9841A57693C210658821F0A476A',
 'D:/alianximod/src/main/resources/assets/xueximod/textures/effect/ground_dust_v1/ground_dust.png':'0C7479F9F3DC02E3216D7EBF6A195C98730CF9841A57693C210658821F0A476A'
}
guard_results=[{'path':p,'expected_sha256':expected,'sha256':sha(Path(p)),'unchanged':sha(Path(p))==expected} for p,expected in guards.items()]
assert all(x['unchanged'] for x in guard_results),guard_results
(root/'evidence'/'protected-input-hashes-final.json').write_text(json.dumps(guard_results,ensure_ascii=False,indent=2),encoding='utf-8')

native=json.loads((root/'source'/'earth_break_ground_v2_preview.bbmodel').read_text(encoding='utf-8'))
assert native['meta']['model_format']=='free'
assert len(native['elements'])==367
assert len(native['textures'])==2
assert len(native['animations'])==1
gif=json.loads((root/'evidence'/'gif-verification.json').read_text(encoding='utf-8'))
rec=json.loads((root/'evidence'/'reopened-render.json').read_text(encoding='utf-8'))
assert rec['renderedAfterNativeReopen'] and rec['saved']
assert all(t['error']==0 for t in rec['textureErrors'])
snapshot=json.loads((root/'evidence'/'requirements-N-O-DV-original.json').read_text(encoding='utf-8'))
assert snapshot['cells']['B']=='earth_break_ground' and len(snapshot['cells']['DV'])==1116

def info(p):
    out={'path':p.relative_to(root).as_posix(),'sha256':sha(p),'bytes':p.stat().st_size}
    if p.suffix.lower() in ['.png','.gif']:
        im=Image.open(p);out.update(width=im.width,height=im.height,mode=im.mode)
        if p.suffix.lower()=='.gif':
            durations=[f.info.get('duration',0) for f in ImageSequence.Iterator(im)]
            out.update(frames=len(durations),duration_ms=sum(durations),frame_durations_ms=durations)
    if p.suffix.lower()=='.aseprite':
        b=p.read_bytes();magic,n,w,h,depth=struct.unpack_from('<5H',b,4)
        assert magic==0xa5e0
        durations=[];offset=128
        for _ in range(n):
            framebytes,frame_magic,chunks,duration=struct.unpack_from('<IHHH',b,offset)
            assert frame_magic==0xf1fa
            durations.append(duration);offset+=framebytes
        out.update(width=w,height=h,color_depth=depth,frames=n,duration_ms=sum(durations),frame_durations_ms=durations)
    if p.suffix.lower()=='.bbmodel': out.update(format=native['meta']['model_format'],elements=len(native['elements']),textures=len(native['textures']),animations=len(native['animations']),native_saved_and_reopened=True)
    return out

receipt=Path('D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/ground-v2-art-preview-20261116.md')
manifest={
 'version':'earth-ground-mouth-dust-v2-20261116','batchId':'待制作-20261031','taskId':'earth_break_ground_v2_art_preview','attemptId':'20261116-art-ground-v2-03',
 'generated_at':datetime.datetime.now().astimezone().isoformat(),'parent':'01a1ce06-b681-7c90-b11d-260d3c169911',
 'status':{'art':'candidate complete','user_same_version_confirmation':False,'formal_adoption':False,'runtime_integrated':False,'compiled':False,'game_tested':False,'writes_converged':True},
 'review_budget':{'design':'2/2','implementation':'3/3','statement':'本阶段额度已用完，返修结果未追加复审'},
 'model_request':{'requested':'gpt-6-astra','reasoning_effort':'max','reason':'parent-reported actual Carbon/Argon art task spawn Unknown model; no agent_id/artifacts','backend_identity_independently_checked':False},
 'input_workbook':{'sha256':snapshot['sha256'],'bytes':snapshot['bytes'],'sheet':'制作中','row':11,'N_chars':len(snapshot['cells']['N']),'O_chars':len(snapshot['cells']['O']),'DV_chars':len(snapshot['cells']['DV'])},
 'units':{'model_units_per_block':16,'impact_center':[0,0,0],'ground_top_model_y':8,'dust_root_offset_from_C_y_blocks':0.53,'dust_root_above_top_blocks':0.03,'texture_anchor_px':[16,28]},
 'visual':{'emitters':11,'texture_variants':3,'dust_frames':16,'frame_ms':50,'dust_duration_ms':800,'preview_duration_ms':2000,'preview_source_frames':40,'removed_cells_preview_only':23},
 'gif_verification':gif,'native_reopen':rec,
 'receipt':{'path':str(receipt),'sha256':sha(receipt),'bytes':receipt.stat().st_size},
 'protected_inputs':guard_results,
 'artifacts':[info(p) for p in sorted(root.rglob('*')) if p.is_file() and p.name!='manifest.json'],
 'hash_note':'SHA256 covers all listed delivered files. manifest.json is excluded to avoid recursive self-hashing; receipt references this manifest by path only.'
}
(root/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'artifacts':len(manifest['artifacts']),'aseprite':next(a for a in manifest['artifacts'] if a['path']=='source/ground_dust_v2.aseprite'),'bbmodel':next(a for a in manifest['artifacts'] if a['path'].endswith('.bbmodel')),'gif':gif['checks']['earth_break_ground_v2-real-blockbench.gif'],'all_protected_inputs_unchanged':True,'manifest_sha256':sha(root/'manifest.json')},ensure_ascii=False))
