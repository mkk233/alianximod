# -*- coding: utf-8 -*-
import hashlib, html, json, re, zipfile
from datetime import datetime
from pathlib import Path
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
data = live.read_bytes()
st = live.stat()
z = zipfile.ZipFile(live)
parts = {
    "已完成": "xl/worksheets/sheet1.xml",
    "待制作": "xl/worksheets/sheet2.xml",
    "制作中": "xl/worksheets/sheet3.xml",
    "待验收": "xl/worksheets/sheet4.xml",
    "数据表": "xl/worksheets/sheet5.xml",
    "资源状态明细": "xl/worksheets/sheet6.xml",
    "待制作原始数据": "xl/worksheets/sheet7.xml",
}

def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)

def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))

def cell_span(row, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c r="{column}{number}"', row))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row.find(">", start)
    if row[opener_end-1] == "/":
        return row[start:opener_end+1]
    close = re.search(r"</(?:[\w]+:)?c>", row[opener_end:])
    return row[start:opener_end+close.end()]

def cell_text(cell):
    if not cell:
        return None
    inline = re.search(r"<(?:[\w]+:)?is\b[^>]*>([\s\S]*)</(?:[\w]+:)?is>", cell)
    if inline:
        return html.unescape("".join(re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", inline.group(1))))
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    return html.unescape(value.group(1)) if value else ""

def ids(xml):
    found=[]
    for row in rows_of(xml):
        n=row_number(row)
        cell=cell_span(row,"B",n)
        code=cell_text(cell)
        if n>=3 and code and code.strip():
            found.append({"row":n,"id":code.strip(),"name":cell_text(cell_span(row,"A",n))})
    return found

out={
 "sha256": hashlib.sha256(data).hexdigest().upper(),
 "mtime": datetime.fromtimestamp(st.st_mtime).isoformat(timespec="milliseconds"),
 "bytes": len(data),
 "sheets": {},
}
for title, name in parts.items():
    xml=z.read(name).decode("utf-8")
    dim=re.search(r'<dimension[^>]*ref="([^"]+)"', xml)
    vals=re.findall(r'<dataValidation\b[^>]*sqref="([^"]+)"', xml)
    comments=len(re.findall(r'<comment\b', z.read(name.replace("worksheets/sheet","comments/comment")).decode("utf-8"))) if name.startswith("xl/worksheets/sheet") and int(name[-5])<=4 else None
    out["sheets"][title]={
        "dimension": dim.group(1) if dim else None,
        "rowCount": len(rows_of(xml)),
        "rowNumbers": [row_number(r) for r in rows_of(xml)],
        "ids": ids(xml),
        "validationCount": len(vals),
        "validations": vals,
        "hasId": "fire_liaoyuan" in xml,
    }
# headers and target from 待验收 and 制作中
for title in ("待验收","制作中"):
    xml=z.read(parts[title]).decode("utf-8")
    rows=rows_of(xml)
    header=next(r for r in rows if row_number(r)==1)
    headers={}
    for col in ["A","B","C","D","E","F","N","O","DV","AS","BV","BW","EB","EC","EG","EH"]:
        cell=cell_span(header,col,1)
        headers[col]={"text": cell_text(cell), "xmlHead": (cell or "")[:180]}
    out["sheets"][title]["headers"]=headers

acc=z.read(parts["待验收"]).decode("utf-8")
target=None
for row in rows_of(acc):
    n=row_number(row)
    if cell_text(cell_span(row,"B",n))=="fire_liaoyuan":
        target={"row":n,"cells":{}}
        for col in ["A","B","C","D","E","F","N","O","DV","AS","BV","BW","EB","EC","EG","EH"]:
            cell=cell_span(row,col,n)
            target["cells"][col]={"text": cell_text(cell), "len": None if cell_text(cell) is None else len(cell_text(cell)), "xmlHead": (cell or "")[:240]}
        target["hasFormula"]= "<f" in row or "<f " in row
        target["rowBytes"]=len(row.encode("utf-8"))
        break
out["target"]=target
# comments referencing row
for i in range(1,5):
    cxml=z.read(f"xl/comments/comment{i}.xml").decode("utf-8")
    refs=re.findall(r'ref="([A-Z]+\d+)"', cxml)
    out[f"comment{i}Refs"]=refs
Path(r"D:\alianximod\docs\开发记录\燎原本体伤害-20261112\inspect-before.json").write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
print("rows", {k:(len(v["ids"]), v["ids"][:2], v["dimension"], v["hasId"]) for k,v in out["sheets"].items()})
print("target row", None if not target else target["row"], "E", None if not target else target["cells"]["E"])
print("N head", None if not target else target["cells"]["N"]["text"][:80])
print("header E", out["sheets"]["待验收"]["headers"]["E"])
