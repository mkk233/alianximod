# Read-only structure probe. Does not modify the workbook.
import zipfile
import re
from xml.etree import ElementTree as ET

XLSX = r"D:\alianximod\docs\术式制作表.xlsx"
NS = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}

with zipfile.ZipFile(XLSX) as zf:
    names = zf.namelist()
    print("PARTS", len(names))
    for name in names:
        if "comment" in name.lower() or "vml" in name.lower() or name.endswith(".xml"):
            if name.startswith("xl/"):
                info = zf.getinfo(name)
                print(f"{name} {info.file_size}")
    sheet = zf.read("xl/worksheets/sheet1.xml").decode("utf-8")
    print("SHEET1_HEAD", sheet[:500].replace("\n", " "))
    rows = re.findall(r"<row\b[^>]*r=\"26\"[^>]*>", sheet)
    print("ROW26_OPEN", rows[:1])
    m = re.search(r"<row\b[^>]*r=\"26\"[^>]*>[\s\S]*?</row>", sheet)
    row = m.group(0)
    print("ROW26_LEN", len(row))
    print("ROW26_HAS_26_VALUE", "26" in row)
    cells = re.findall(r"<c r=\"([A-Z]+)26\"([^>]*)>(?:<v>([^<]*)</v>)?", row)
    print("CELL_COUNT", len(cells))
    print("SAMPLE", cells[:8])
    sst = zf.read("xl/sharedStrings.xml").decode("utf-8")
    print("SST_HEAD", sst[:300].replace("\n", " "))
    arch = zf.read("xl/worksheets/sheet7.xml").decode("utf-8")
    print("ARCH_HEAD", arch[:400].replace("\n", " "))
    opens = re.findall(r"<row\b[^>]*r=\"(\d+)\"", arch)
    print("ARCH_ROWS", opens)
    mid = zf.read("xl/worksheets/sheet3.xml").decode("utf-8")
    print("MID_DIM", re.search(r"<dimension[^/]*/>", mid).group(0))
    print("MID_FILTER", re.search(r"<autoFilter[^/]*/>", mid).group(0) if "<autoFilter" in mid else None)
    print("MID_ROWS", re.findall(r"<row\b[^>]*r=\"(\d+)\"", mid))
    todo = zf.read("xl/worksheets/sheet2.xml").decode("utf-8")
    print("TODO_DIM", re.search(r"<dimension[^/]*/>", todo).group(0))
    print("TODO_ROWS", re.findall(r"<row\b[^>]*r=\"(\d+)\"", todo))
    print("TODO_VAL", re.findall(r"<dataValidation\b[^>]*sqref=\"([^\"]+)\"", todo)[:5])
    print("MID_VAL", re.findall(r'sqref="([^"]+)"', mid))
    wb = zf.read("xl/workbook.xml").decode("utf-8")
    for line in wb.split("definedName"):
        if "FilterDatabase" in line or "制作中" in line:
            print("DN", line[:240].replace("\n", " "))
