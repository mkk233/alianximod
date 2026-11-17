import re
import zipfile
import importlib.util

spec = importlib.util.spec_from_file_location(
    "register_claim",
    r"D:\alianximod\docs\开发记录\术式批量制作\水流组合-20261103\sheet-work\register_claim.py",
)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
with zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx") as book:
    xml = book.read("xl/worksheets/sheet1.xml").decode("utf-8")
    shared = mod.shared_items(book.read("xl/sharedStrings.xml").decode("utf-8"))
rows = mod.row_map(xml)
print("row2 len", len(rows[2]))
print("has C2", 'r="C2"' in rows[2])
print("value", mod.cell_value(rows[2], "C", 2, shared))
snippet = re.search(r'.{40}r="C2".{80}', rows[2])
print(snippet.group(0) if snippet else "no snippet")
