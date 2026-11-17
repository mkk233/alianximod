"""Offline audit of the actual compiled layout; no game launch, no production resource writes."""
from pathlib import Path
import re, json, subprocess, zipfile, hashlib, sys
ROOT = Path('D:/alianximod')
OUT = Path(__file__).resolve().parent
SRC = ROOT/'src/main/java/com/bslx/XueXiMod'

def catalog():
    text = (SRC/'shu/ModTechniques.java').read_text('utf-8-sig')
    text = text.replace('WaterPhantomDefinition.TECHNIQUE_ID', '"water_phantom"')
    definitions = {}
    roles = {}
    release = (SRC/'shu/technique/release/ReleaseDefinitions.java').read_text('utf-8-sig')
    for match in re.finditer(r'(?:define|cohortSword)\("([^"]+)"[^;]*?ChainRole\.(\w+)', release):
        roles[match[1]] = match[2]
    roles.update(rule_surface_anchor='SEEK_SURFACE',rule_stagger='RULE_STAGGER',rule_hit_continue='RULE_HIT_CONTINUE',fire_upwell='TRAJECTORY_UPWELL',rule_cooperative='RULE_COOPERATIVE')
    for m in re.finditer(r'public static final Technique (\w+) = new Technique\((.*?)\n    \);', text, re.S):
        strings = re.findall(r'"([^"\n]*)"',m[2]); attr = re.search(r'ShuAttribute\.(\w+)',m[2]); typ = re.search(r'TechniqueType\.(\w+)',m[2])
        definitions[m[1]]=dict(id=strings[0],name=strings[1],attribute=attr[1] if attr else 'NONE',type=typ[1])
    items=[definitions[k] for k in re.findall(r'TechniqueManager.register\((\w+)\)',text)]
    water=(SRC/'shu/WaterTechniques.java').read_text('utf-8-sig')
    for id,name in re.findall(r'(?:new Technique|barrier|domain|guard)\("(water_\w+)",\s*"([^"]+)"',water):
        items.append(dict(id=id,name=name,attribute='WATER',type='SWORD' if id=='water_blade' else 'MAGIC'))
    for id,name,typ in [('clone_body','普通分身','MAGIC'),('sword_oblique_slash','斩击','SWORD'),('movement_spin','回旋','MOVEMENT'),('rule_cooperative','协同','UNIVERSAL')]:
        items.append(dict(id=id,name=name,attribute='NONE',type=typ))
    graph=(SRC/'player/LearningGraph.java').read_text('utf-8-sig')
    parents={m[1]:[re.findall(r'"([^"]+)"',a) for a in re.findall(r'any\(([^)]*)\)',m[2])] for m in re.finditer(r'node\("([^"]+)",\s*Stage\.\w+(.*?)\);',graph,re.S)}
    for item in items:
        item['parents']=parents.get(item['id'],[])
        item['role']=roles.get(item['id'],'ATTACK')
        item['energy']=2 if item['id']=='rule_gather_energy' else 1
    assert len(items)==85 and len({i['id'] for i in items})==85
    assert not {p for i in items for group in i['parents'] for p in group}-{i['id'] for i in items}
    (OUT/'gpt-layout-input-20261114.json').write_text(json.dumps(items,ensure_ascii=False,indent=2),encoding='utf-8')
    return items

def run():
    catalog()
    for stale in (OUT/'com/bslx/XueXiMod/gui').glob('ImprintTreeGeometry*.class'):
        assert stale.resolve().is_relative_to(OUT)
        stale.unlink()
    cp=(ROOT/'build/tide-classpath.txt').read_text('utf-8-sig').strip()
    cp=str(OUT)+';'+cp
    args=['javac','-encoding','UTF-8','-cp',cp,'-d',str(OUT),'-proc:none',str(OUT/'GptLayoutAudit.java')]
    subprocess.run(args,check=True,cwd=OUT)
    result = subprocess.run(['java','-Xmx1G','-Dfile.encoding=UTF-8','-cp',cp,'com.bslx.XueXiMod.gui.GptLayoutAudit',str(OUT)],cwd=OUT)
    if result.returncode: raise SystemExit(result.returncode)

if __name__=='__main__': run()
