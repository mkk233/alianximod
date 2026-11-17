import json
from pathlib import Path
from collections import Counter
import openpyxl
from openpyxl.utils import get_column_letter

root = Path(__file__).parent
book = Path('D:/alianximod/docs/术式制作表.xlsx')
wb = openpyxl.load_workbook(book, data_only=False)
info = {'sheets': [], 'headers': [], 'validations': [], 'enums': [], 'examples': []}
ids = []
for ws in wb:
    rows = []
    if ws.title != '数据表':
        for row in ws.iter_rows(min_row=3):
            values = {cell.column_letter: cell.value for cell in row if cell.value is not None}
            if values:
                rows.append({'row': row[0].row, 'values': values})
                if values.get('B'):
                    ids.append(str(values['B']).strip())
                if any(word in str(values.get('A', '')) for word in ['火', '爆', '领域', '刺', '牢', '水环']):
                    info['examples'].append({'sheet': ws.title, 'row': row[0].row, 'values': values})
        info['sheets'].append({'name': ws.title, 'max_row': ws.max_row, 'max_column': ws.max_column, 'rows': rows, 'freeze': ws.freeze_panes, 'filter': ws.auto_filter.ref})
    else:
        info['enums'] = [[cell.value for cell in row] for row in ws.iter_rows()]
target = wb['待制作']
for col in range(1, target.max_column + 1):
    a, b = target.cell(1, col), target.cell(2, col)
    info['headers'].append({'col': get_column_letter(col), 'group': a.value, 'title': b.value, 'comment': b.comment.text if b.comment else None, 'style3': target.cell(3,col).style_id, 'width': target.column_dimensions[get_column_letter(col)].width})
for dv in target.data_validations.dataValidation:
    info['validations'].append({'range': str(dv.sqref), 'type': dv.type, 'formula1': dv.formula1})
info['duplicate_ids'] = [v for v,n in Counter(ids).items() if n > 1]
info['defined_names'] = {key: val.attr_text for key,val in wb.defined_names.items()}
(root/'workbook-inspection.json').write_text(json.dumps(info, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({'sheets': [{**s, 'rows': [{'row':r['row'], 'name':r['values'].get('A'), 'id':r['values'].get('B')} for r in s['rows']]} for s in info['sheets']], 'headers':info['headers'], 'enums':info['enums'], 'validations':info['validations'], 'duplicate_ids':info['duplicate_ids'], 'defined_names':info['defined_names'], 'examples':info['examples']}, ensure_ascii=False))
