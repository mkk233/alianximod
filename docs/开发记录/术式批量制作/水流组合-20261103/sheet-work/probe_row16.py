import re
import zipfile

path = r"D:\alianximod\docs\术式制作表.xlsx"
with zipfile.ZipFile(path) as book:
    xml = book.read("xl/worksheets/sheet3.xml").decode("utf-8")
match = re.search(r'<row\b[^>]*r="16"[^>]*>[\s\S]*?</row>', xml)
row = match.group(0)
print("LEN", len(row))
print(row[:700])
print("---A---")
print(re.search(r'<c r="A16".{0,80}', row).group(0) if re.search(r'<c r="A16"', row) else "NO A")
print("---B---")
print(re.search(r'<c r="B16".{0,80}', row).group(0) if re.search(r'<c r="B16"', row) else "NO B")
