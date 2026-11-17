import re
import zipfile

XLSX = r"D:\alianximod\docs\术式制作表.xlsx"

def shared(zf):
    xml = zf.read("xl/sharedStrings.xml").decode("utf-8")
    items = re.findall(r"<si>(.*?)</si>", xml)
    out = []
    for item in items:
        texts = re.findall(r"<t[^>]*>(.*?)</t>", item)
        out.append("".join(texts))
    return out

with zipfile.ZipFile(XLSX) as zf:
    sst = shared(zf)
    print("SST", len(sst))
    for i, name in enumerate(["xl/comments1.xml", "xl/comments2.xml", "xl/comments3.xml", "xl/comments4.xml"], 1):
        xml = zf.read(name).decode("utf-8")
        refs = re.findall(r'\b(?:ref|r)="([A-Z]+\d+)"', xml)
        print(name, "anchors", refs)
    arch = zf.read("xl/worksheets/sheet7.xml").decode("utf-8")
    m = re.search(r'<row\b[^>]*r="10"[^>]*>[\s\S]*?</row>', arch)
    row = m.group(0)
    print("ARCH10_OPEN", row[:180])
    cells = re.findall(r'<c r="([A-Z]+)10"([^>]*)>(?:<v>([^<]*)</v>)?', row)
    print("ARCH10_CELLS", len(cells))
    for col, attrs, val in cells[:12]:
        shown = sst[int(val)] if 't="s"' in attrs and val else val
        print(" ", col, shown)
    dim = re.search(r"<dimension[^/]*/>", arch).group(0)
    print("ARCH_DIM", dim)
    # last archive row B
    m23 = re.search(r'<row\b[^>]*r="23"[^>]*>[\s\S]*?</row>', arch)
    b = re.search(r'<c r="B23"([^>]*)><v>([^<]*)</v>', m23.group(0))
    attrs, val = b.group(1), b.group(2)
    print("ARCH23_B", sst[int(val)] if 't="s"' in attrs else val)
    mid = zf.read("xl/worksheets/sheet3.xml").decode("utf-8")
    m16 = re.search(r'<row\b[^>]*r="16"[^>]*>', mid).group(0)
    print("MID16", m16)
    dv = re.search(r'<c r="DV16"([^>]*)><v>([^<]*)</v>', mid)
    print("DV16_ATTR", dv.group(1), "LEN", len(sst[int(dv.group(2))]))
    # header row2 labels for selected cols
    sheet = zf.read("xl/worksheets/sheet1.xml").decode("utf-8")
    h = re.search(r'<row\b[^>]*r="2"[^>]*>[\s\S]*?</row>', sheet).group(0)
    def col_index(col):
        n = 0
        for ch in col:
            n = n * 26 + ord(ch) - 64
        return n
    headers = {}
    for col, attrs, val in re.findall(r'<c r="([A-Z]+)2"([^>]*)><v>([^<]*)</v></c>', h):
        headers[col] = sst[int(val)] if 't="s"' in attrs else val
    want = ["A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","BT","BU","BV","BW","CR","CS","CT","CU","CV","CW","CX","CY","CZ","DA","DB","DM","DV","EN"]
    for col in want:
        print(col, headers.get(col))
    # confirm deep maintenance cells
    row26 = re.search(r'<row\b[^>]*r="26"[^>]*>[\s\S]*?</row>', sheet).group(0)
    for col in ["BU","BV","CV","CW","N","O","DV","CU","CX","BW"]:
        cell = re.search(rf'<c r="{col}26"([^>]*)>(?:<v>([^<]*)</v>)?', row26)
        if not cell:
            print("DEEP", col, "ABSENT")
            continue
        attrs, val = cell.group(1), cell.group(2)
        shown = sst[int(val)] if val and 't="s"' in attrs else val
        print("DEEP", col, shown)
    # comment text mention row 26?
    c1 = zf.read("xl/comments1.xml").decode("utf-8")
    print("COMMENT1_ROW26", "26" in c1)
    print("COMMENT_SHEETS", [n for n in zf.namelist() if "comment" in n or "vml" in n])
