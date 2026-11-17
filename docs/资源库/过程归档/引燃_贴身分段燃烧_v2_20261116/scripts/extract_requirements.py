"""Read only N/O/DV (and B identifier); never write or recalculate the workbook."""
from pathlib import Path
from zipfile import ZipFile
import xml.etree.ElementTree as ET
import hashlib, json, datetime

root = Path(__file__).resolve().parents[1]
book = Path('D:/alianximod/docs/术式制作表.xlsx')
before = book.read_bytes()
ns = {'s': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
with ZipFile(book, 'r') as z:
    wb = ET.fromstring(z.read('xl/workbook.xml'))
    rel = ET.fromstring(z.read('xl/_rels/workbook.xml.rels'))
    rid = next(s for s in wb.find('s:sheets', ns) if s.attrib['name'] == '制作中').attrib['{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id']
    target = next(r.attrib['Target'] for r in rel if r.attrib['Id'] == rid)
    member = target.lstrip('/') if target.startswith('/') else 'xl/' + target
    strings = []
    if 'xl/sharedStrings.xml' in z.namelist():
        strings = [''.join(t.text or '' for t in si.findall('.//s:t', ns)) for si in ET.fromstring(z.read('xl/sharedStrings.xml'))]
    sheet = ET.fromstring(z.read(member))
    cells = {}
    for addr in ['B8', 'N8', 'O8', 'DV8']:
        c = sheet.find(f'.//s:c[@r="{addr}"]', ns)
        if c is None:
            value = None
        elif c.attrib.get('t') == 'inlineStr':
            value = ''.join(t.text or '' for t in c.findall('.//s:t', ns))
        else:
            value = c.findtext('s:v', default='', namespaces=ns)
            if c.attrib.get('t') == 's': value = strings[int(value)]
        cells[addr] = value
assert cells['B8'] == 'fire_kindle', cells['B8']
assert book.read_bytes() == before
snapshot = {
    'status': 'candidate_pending_user_confirmation',
    'batchId': '待制作-20261031', 'taskId': 'fire_kindle_v2_art', 'attemptId': '20261116-art-kindle-01',
    'source': str(book), 'size_bytes': len(before),
    'sha256': hashlib.sha256(before).hexdigest().upper(),
    'mtime_Asia_Shanghai': datetime.datetime.fromtimestamp(book.stat().st_mtime, datetime.timezone(datetime.timedelta(hours=8))).isoformat(),
    'extracted_at': datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat(),
    'sheet': '制作中', 'row': 8, 'cells_verbatim': cells,
    'method': 'Python standard library ZIP/XML; no workbook save, edit or recalculation',
}
out = root / 'requirements-snapshot.json'
out.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(snapshot, ensure_ascii=False, indent=2))
