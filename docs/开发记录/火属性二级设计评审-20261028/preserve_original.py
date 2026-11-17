"""Keep original OOXML parts; merge only Artifact Tool-authored new cells.

Artifact Tool's full export normalizes unrelated styles and native metadata.
This preservation step is limited to the requested sheet's three new rows,
description widths, used dimension and filter extent. No workbook save API.
"""
from pathlib import Path
from copy import deepcopy
from zipfile import ZipFile
from lxml import etree as ET
import hashlib,json,re

root=Path(__file__).parent
spec=json.loads((root/'fire-techniques.json').read_text(encoding='utf-8'))
source=Path(spec['source'])
assert hashlib.sha256(source.read_bytes()).hexdigest().upper()==spec['baselineSha256'],'Source changed'
ns={'s':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
q=lambda local:'{'+ns['s']+'}'+local
with ZipFile(source) as src, ZipFile(root/'candidate-artifact.xlsx') as authored:
    target='xl/worksheets/sheet2.xml'
    original_xml=ET.fromstring(src.read(target))
    completed=ET.fromstring(src.read('xl/worksheets/sheet1.xml'))
    style_by_col={re.sub(r'\d','',c.get('r')):c.get('s') for c in completed.find('s:sheetData',ns).find("s:row[@r='3']",ns)}
    original_data=original_xml.find('s:sheetData',ns)
    assert all(int(r.get('r'))<=2 for r in original_data),'Pending sheet is not empty'
    # Locate the authored pending sheet through workbook relationships.
    abook=ET.fromstring(authored.read('xl/workbook.xml'))
    arels=ET.fromstring(authored.read('xl/_rels/workbook.xml.rels'))
    rid=next(s for s in abook.find('s:sheets',ns) if s.get('name')=='待制作').get('{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id')
    a_target=next(r for r in arels if r.get('Id')==rid).get('Target')
    a_target=a_target.lstrip('/') if a_target.startswith('/') else 'xl/'+a_target
    adata=ET.fromstring(authored.read(a_target)).find('s:sheetData',ns)
    strings=[]
    if 'xl/sharedStrings.xml' in authored.namelist():strings=list(ET.fromstring(authored.read('xl/sharedStrings.xml')))
    for entry in spec['rows']:
        r=deepcopy(adata.find(f"s:row[@r='{entry['row']}']",ns))
        assert r is not None
        r.attrib.clear();r.set('r',str(entry['row']));r.set('ht','225');r.set('customHeight','1')
        for c in r:
            col=re.sub(r'\d','',c.get('r'))
            if col in style_by_col and style_by_col[col] is not None:c.set('s',style_by_col[col])
            else:c.attrib.pop('s',None)
            if c.get('t')=='s':
                si=strings[int(c.find('s:v',ns).text)]
                for ch in list(c):c.remove(ch)
                c.set('t','inlineStr');inline=ET.SubElement(c,q('is'))
                for ch in si:inline.append(deepcopy(ch))
        original_data.append(r)
    original_xml.find('s:dimension',ns).set('ref','A1:DV5')
    original_xml.find('s:autoFilter',ns).set('ref','A2:DV5')
    def colnum(col):
        n=0
        for ch in col:n=n*26+ord(ch)-64
        return n
    for col,width in spec['newColumnWidths'].items():
        n=colnum(col)
        elem=next(c for c in original_xml.find('s:cols',ns) if int(c.get('min'))==n and int(c.get('max'))==n)
        elem.set('width',str(width));elem.set('customWidth','1')
    data=ET.tostring(original_xml,encoding='utf-8')
    output=root/'candidate-preserved.xlsx'
    with ZipFile(output,'w') as out:
        for part in src.infolist():out.writestr(part,data if part.filename==target else src.read(part.filename))
with ZipFile(source) as src, ZipFile(output) as dst:
    assert src.namelist()==dst.namelist()
    changed=[n for n in src.namelist() if src.read(n)!=dst.read(n)]
    assert changed==[target],changed
print(json.dumps({'candidate':str(output),'changed_parts':changed,'all_other_parts_identical':True},ensure_ascii=False))
