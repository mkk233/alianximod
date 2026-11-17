import importlib.util
import zipfile

spec = importlib.util.spec_from_file_location(
    "move",
    r"D:\alianximod\docs\开发记录\术式批量制作\水流组合-20261103\sheet-work\move_to_acceptance.py",
)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
with zipfile.ZipFile(r"D:\alianximod\docs\术式制作表.xlsx") as book:
    xml = book.read("xl/worksheets/sheet3.xml").decode("utf-8")
rows = mod.row_map(xml)
row = rows[18]
start = row.find('r="N18"')
print(row[start - 20:start + 80])
moved = mod.retarget(row, 18, 10)
start = moved.find("N10")
print("MOVED", moved[start - 30:start + 90] if start >= 0 else "NO N10")
print("ROWTAG", moved[:80])
