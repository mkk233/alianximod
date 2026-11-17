# Native package preservation after Artifact Tool authors the changed cells.
from pathlib import Path
import zipfile, json, re, html, hashlib
from xml.etree import ElementTree as E
D=Path(__file__).parent
auth=json.loads((D/'authored-rows.json').read_text(encoding='utf-8'))
original=(D/'original-backup.xlsx').read_bytes()
assert hashlib.sha256(original).hexdigest().upper()==auth['sourceHash']
with zipfile.ZipFile(D/'original-backup.xlsx') as z:
    entries=z.infolist()
    data={i.filename:z.read(i.filename) for i in entries}
ns={'s':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
rels=E.fromstring(data['xl/_rels/workbook.xml.rels'])
targets={n.attrib['Id']:'xl/'+n.attrib['Target'].lstrip('/') for n in rels}
targets={k:v.replace('xl/xl/','xl/') for k,v in targets.items()}
work=E.fromstring(data['xl/workbook.xml'])
names={n.attrib['name']:targets[n.attrib['{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id']] for n in work.find('s:sheets',ns)}
srcpart,dstpart=names['制作中'],names['待验收']
src,dst=data[srcpart].decode(),data[dstpart].decode()
sst=data['xl/sharedStrings.xml'].decode()
items=[''.join(E.fromstring(s).itertext()) for s in re.findall(r'<si\b[^>]*>[\s\S]*?</si>',sst)]
lookup={v:i for i,v in enumerate(items)}
new=[]
def intern(value):
    if value not in lookup:
        lookup[value]=len(items)+len(new)
        new.append(value)
    return lookup[value]
def rows(text):
    return {int(m.group(1)):m.group(0) for m in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>',text)}
srcrows=rows(src); dstrows=rows(dst)
assert max(srcrows)==20 and max(dstrows)==9
assert not any('<f' in srcrows[r['fromRow']] for r in auth['rows'])
# There are no moved native comments in this workbook; do not silently detach one.
for part,payload in data.items():
    if 'comments' in part.lower() and part.endswith('.xml'):
        text=payload.decode()
        if re.search(r'<comment\b[^>]*ref="[A-Z]+(?:18|19|20)"',text):
            raise RuntimeError('A native comment needs relocation before writing')
for r in auth['rows']:
    old, dest=r['fromRow'],r['toRow']
    row=srcrows[old]
    assert re.search(r'<c r="B'+str(old)+r'"[^>]*t="s"[^>]*><v>(\d+)</v>',row)
    idx=int(re.search(r'<c r="B'+str(old)+r'"[^>]*t="s"[^>]*><v>(\d+)</v>',row).group(1))
    assert items[idx]==r['id']
    moved=re.sub(r'r="([A-Z]+)'+str(old)+r'\b"',lambda m:'r="'+m[1]+str(dest)+'"',row)
    moved=re.sub(r'\br="'+str(old)+'"', 'r="'+str(dest)+'"',moved,count=1)
    for col,ix in [('N',13),('O',14),('DV',125)]:
        pat=r'(<c r="'+col+str(dest)+r'"[^>]*t="s"[^>]*>)<v>\d+</v>'
        moved,n=re.subn(pat,lambda m:m[1]+'<v>'+str(intern(r['after'][ix]))+'</v>',moved)
        assert n==1,(col,n)
    assert src.count(row)==1
    src=src.replace(row,'',1)
    dst=dst.replace('</sheetData>',moved+'</sheetData>',1)
src=src.replace('ref="A1:EN20"','ref="A1:EN17"').replace('ref="A2:EN20"','ref="A2:EN17"')
dst=dst.replace('ref="A1:EN9"','ref="A1:EN12"').replace('ref="A2:EN9"','ref="A2:EN12"')
book=data['xl/workbook.xml'].decode()
book=book.replace('制作中!$A$2:$EN$20','制作中!$A$2:$EN$17').replace('待验收!$A$2:$EN$9','待验收!$A$2:$EN$12')
sst=sst.replace('</sst>',''.join('<si><t xml:space="preserve">'+html.escape(v,quote=False)+'</t></si>' for v in new)+'</sst>')
sst=re.sub(r'uniqueCount="\d+"','uniqueCount="'+str(len(items)+len(new))+'"',sst,count=1)
patch={srcpart:src.encode(),dstpart:dst.encode(),'xl/workbook.xml':book.encode(),'xl/sharedStrings.xml':sst.encode()}
assert {k: v for k,v in rows(src).items()}=={k:v for k,v in srcrows.items() if k<18}
assert {k:v for k,v in rows(dst).items() if k<=9}==dstrows
# Preserve each original entry's native features and byte content outside the four scoped XML parts.
with zipfile.ZipFile(D/'candidate.xlsx','w') as z:
    for entry in entries: z.writestr(entry,patch.get(entry.filename,data[entry.filename]))
with zipfile.ZipFile(D/'candidate.xlsx') as z:
    changed=[n for n in data if data[n]!=z.read(n)]
    assert set(changed)==set(patch)
    for name in names.values(): E.fromstring(z.read(name))
# Export can rebuild native validation/style/annotation packages; archive transplant keeps those exact.
with zipfile.ZipFile(D/'artifact-authored.xlsx') as z:
    artifact_members=set(z.namelist())
    native_missing=sorted(set(data)-artifact_members)
result={'changedParts':changed,'unchangedParts':len(data)-len(changed),'newSharedStrings':len(new),'nativePartsMissingFromArtifactExport':native_missing,'otherRowsByteIdentical':True,'resourceDetailsAndArchiveByteIdentical':True}
(D/'native-preservation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False))

