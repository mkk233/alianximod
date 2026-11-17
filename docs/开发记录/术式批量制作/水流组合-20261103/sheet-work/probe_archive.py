import re
import zipfile

XLSX = r"D:\alianximod\docs\术式制作表.xlsx"

def col_index(col):
    n = 0
    for ch in col:
        n = n * 26 + ord(ch) - 64
    return n

def col_letter(n):
    s = ""
    while n:
        n, rem = divmod(n - 1, 26)
        s = chr(65 + rem) + s
    return s

def shared(xml):
    out = []
    for item in re.findall(r"<si\b[^>]*>(.*?)</si>", xml, re.S):
        texts = re.findall(r"<t[^>]*>(.*?)</t>", item, re.S)
        out.append(re.sub(r"\s+", " ", "".join(texts))[:80])
    return out

with zipfile.ZipFile(XLSX) as zf:
    sst_xml = zf.read("xl/sharedStrings.xml").decode("utf-8")
    sst = shared(sst_xml)
    print("parsed", len(sst), "unique attr", re.search(r'uniqueCount="(\d+)"', sst_xml).group(1))
    arch = zf.read("xl/worksheets/sheet7.xml").decode("utf-8")
    print("ARCH_DIM", re.search(r"<dimension[^/]*/>", arch).group(0))
    h1 = re.search(r'<row\b[^>]*r="1"[^>]*>[\s\S]*?</row>', arch).group(0)
    h2 = re.search(r'<row\b[^>]*r="2"[^>]*>[\s\S]*?</row>', arch).group(0)
    def cells(row, number):
        found = []
        for col, attrs, val in re.findall(rf'<c r="([A-Z]+){number}"([^>]*?)(?:/>|>(?:<v>(.*?)</v>)?</c>)', row):
            if not val:
                shown = ""
            elif 't="s"' in attrs:
                shown = sst[int(val)]
            else:
                shown = val
            found.append((col_index(col), col, shown, attrs))
        return found
    print("--- H1 nonempty ---")
    for idx, col, shown, attrs in cells(h1, 1):
        if shown:
            print(col, shown)
    print("--- H2 nonempty past column 140 ---")
    for idx, col, shown, attrs in cells(h2, 2):
        if idx >= 140 or shown:
            if idx >= 140 or col in ("A", "B", "DV"):
                print(col, idx, shown)
    row10 = re.search(r'<row\b[^>]*r="10"[^>]*>[\s\S]*?</row>', arch).group(0)
    print("--- ROW10 nonempty ---")
    for idx, col, shown, attrs in cells(row10, 10):
        if shown:
            print(col, shown)
    row3 = re.search(r'<row\b[^>]*r="3"[^>]*>[\s\S]*?</row>', arch).group(0)
    print("ROW3_OPEN", row3[:220])
    print("ROW3_CELLCOUNT", len(re.findall(r"<c ", row3)))
    # max col in archive row 3
    cols = [col_index(c) for c in re.findall(r'<c r="([A-Z]+)3"', row3)]
    print("ROW3_MAXCOL", max(cols), col_letter(max(cols)))
