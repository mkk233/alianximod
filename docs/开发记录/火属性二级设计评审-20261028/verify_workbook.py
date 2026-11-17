import json
import sys
from pathlib import Path
from collections import Counter
import openpyxl

root=Path(__file__).parent
source=Path('D:/alianximod/docs/术式制作表.xlsx')
candidate=Path(sys.argv[1]) if len(sys.argv)>1 else root/'candidate-artifact.xlsx'
original=openpyxl.load_workbook(source,data_only=False)
new=openpyxl.load_workbook(candidate,data_only=False)
spec=json.loads((root/'fire-techniques.json').read_text(encoding='utf-8'))
issues=[]
def report(kind,loc,a,b):
    if a!=b: issues.append({'kind':kind,'location':loc,'before':str(a)[:350],'after':str(b)[:350]})
def style(c):
    return tuple(str(v) for v in [c.font,c.fill,c.border,c.alignment,c.number_format,c.protection])
def note(c):
    return (c.comment.text,c.comment.author) if c.comment else None
report('sheets','workbook',original.sheetnames,new.sheetnames)
report('names','workbook',{k:v.attr_text for k,v in original.defined_names.items()},{k:v.attr_text for k,v in new.defined_names.items()})
for oldws in original:
    ws=new[oldws.title]
    for row in oldws:
        for cell in row:
            c=ws[cell.coordinate]
            report('value',f'{ws.title}!{cell.coordinate}',cell.value,c.value)
            report('style',f'{ws.title}!{cell.coordinate}',style(cell),style(c))
            report('comment',f'{ws.title}!{cell.coordinate}',note(cell),note(c))
    report('merges',ws.title,str(oldws.merged_cells),str(ws.merged_cells))
    report('freeze',ws.title,oldws.freeze_panes,ws.freeze_panes)
    report('validation',ws.title,[str(v) for v in oldws.data_validations.dataValidation],[str(v) for v in ws.data_validations.dataValidation])
    report('tables',ws.title,dict(oldws.tables),dict(ws.tables))
    report('conditional-formatting',ws.title,str(oldws.conditional_formatting._cf_rules),str(ws.conditional_formatting._cf_rules))
    for k,dim in oldws.column_dimensions.items():
        if ws.title=='待制作' and k in spec['newColumnWidths']: continue
        report('column-dimension',f'{ws.title}:{k}',str(dim),str(ws.column_dimensions[k]))
    for k,dim in oldws.row_dimensions.items():
        report('row-dimension',f'{ws.title}:{k}',str(dim),str(ws.row_dimensions[k]))
for entry in spec['rows']:
    for col,value in entry['values'].items():
        report('new-value',f'待制作!{col}{entry["row"]}',value,new['待制作'][f'{col}{entry["row"]}'].value)
    for validation in new['待制作'].data_validations.dataValidation:
        formula=validation.formula1 or ''
        if validation.type!='list' or not formula.startswith('='):continue
        name=original.defined_names[formula[1:]]
        allowed=[]
        for title,cellrange in name.destinations:
            allowed += [cell.value for row in original[title][cellrange] for cell in row]
        for col,value in entry['values'].items():
            addr=f'{col}{entry["row"]}'
            if addr in validation.sqref and value not in allowed:issues.append({'kind':'invalid-enum','location':addr,'value':value})
counts={}
ids=[]
for title in ['已完成','待制作','制作中','待验收']:
    ws=new[title]
    occupied=[r for r in range(3,ws.max_row+1) if any(ws.cell(r,c).value is not None for c in range(1,127))]
    counts[title]=len(occupied)
    for r in occupied:
        value=ws.cell(r,2).value
        if not value:issues.append({'kind':'missing-id','location':f'{title}!B{r}'})
        else:ids.append(value)
duplicates=[v for v,n in Counter(ids).items() if n>1]
if duplicates:issues.append({'kind':'duplicate-ids','values':duplicates})
result={'file':str(candidate),'counts':counts,'issue_count':len(issues),'issues':issues}
(root/'verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({**result,'issues':issues[:12]},ensure_ascii=False))
sys.exit(1 if issues else 0)
