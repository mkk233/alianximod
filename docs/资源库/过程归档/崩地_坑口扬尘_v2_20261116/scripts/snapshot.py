from pathlib import Path
from zipfile import ZipFile
from xml.etree import ElementTree as ET
import json, hashlib, datetime

ROOT = Path(__file__).resolve().parents[1]
BOOK = Path('D:/alianximod/docs/术式制作表.xlsx')
NS={'s':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
data=BOOK.read_bytes()
with ZipFile(BOOK) as z:
    wb=ET.fromstring(z.read('xl/workbook.xml'))
    rels={x.attrib['Id']:x.attrib['Target'] for x in ET.fromstring(z.read('xl/_rels/workbook.xml.rels'))}
    shared=[]
    if 'xl/sharedStrings.xml' in z.namelist():
        shared=[''.join(x.itertext()) for x in ET.fromstring(z.read('xl/sharedStrings.xml'))]
    sheet=next(s for s in wb.find('s:sheets',NS) if s.attrib['name']=='制作中')
    target=rels[sheet.attrib['{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id']]
    target=target.lstrip('/') if target.startswith('/') else 'xl/'+target
    xml=ET.fromstring(z.read(target))
    def val(c):
        if c.attrib.get('t')=='inlineStr': return ''.join(c.find('s:is',NS).itertext())
        v=c.find('s:v',NS)
        if v is None: return None
        return shared[int(v.text)] if c.attrib.get('t')=='s' else v.text
    rows=[]
    for row in xml.findall('.//s:sheetData/s:row',NS):
        d={c.attrib['r']:val(c) for c in row}
        if 'earth_break_ground' in d.values() or '崩地' in d.values(): rows.append((row.attrib['r'],d))
    assert len(rows)==1,rows
    rn,cells=rows[0]
    snap={'batchId':'待制作-20261031','taskId':'earth_break_ground_v2_art_preview','attemptId':'20261116-art-ground-v2-03','source':str(BOOK),'sheet':'制作中','row':int(rn),'sha256':hashlib.sha256(data).hexdigest().upper(),'bytes':len(data),'mtime':datetime.datetime.fromtimestamp(BOOK.stat().st_mtime,datetime.timezone(datetime.timedelta(hours=8))).isoformat(),'captured_at':datetime.datetime.now().astimezone().isoformat(),'cells':{col:cells.get(col+rn) for col in ['B','N','O','DV']},'all_populated_row_cells':cells,'workbook_modified':False}
    assert snap['sha256']=='87F22C7DCE22FD0AB83F219C1D18938AF381D2286C106A62CEBCA1A6A56E9C75',snap['sha256']
    (ROOT/'evidence').mkdir(exist_ok=True)
    (ROOT/'evidence'/'requirements-N-O-DV-original.json').write_text(json.dumps(snap,ensure_ascii=False,indent=2),encoding='utf-8')
    text='崩地候选 v2 — 制作中原文快照\n\n读取源：'+str(BOOK)+'\nSHA256：'+snap['sha256']+'\n行：'+rn+'\n\n'
    for col in ['B','N','O','DV']:
        text+=col+' 原文\n'+str(snap['cells'][col])+'\n\n'
    (ROOT/'evidence'/'requirements-N-O-DV-original.txt').write_text(text,encoding='utf-8')
    offsets=[{'east':x,'down':d,'south':z} for x in range(-2,3) for z in range(-2,3) if x*x+z*z<=4 for d in range(3) if 2*d<=4-x*x-z*z]
    assert len(offsets)==23
    (ROOT/'evidence'/'crater-offsets-23.json').write_text(json.dumps({'formula':'r^2<=4; d=0..2; 2d<=4-r^2; cap32','count':23,'offsets':offsets},indent=2),encoding='utf-8')
    print(json.dumps({'row':rn,'hash':snap['sha256'],'lengths':{k:len(str(v)) for k,v in snap['cells'].items()},'DV':snap['cells']['DV']},ensure_ascii=False))
assert hashlib.sha256(BOOK.read_bytes()).digest()==hashlib.sha256(data).digest()
