import re
import zipfile

XLSX = r"D:\alianximod\docs\术式制作表.xlsx"

def shared(xml):
    out = []
    for item in re.findall(r"<si\b[^>]*>(.*?)</si>", xml, re.S):
        texts = re.findall(r"<t[^>]*>(.*?)</t>", item, re.S)
        text = "".join(texts)
        text = text.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
        out.append(text)
    return out

with zipfile.ZipFile(XLSX) as zf:
    sst = shared(zf.read("xl/sharedStrings.xml").decode("utf-8"))
    arch = zf.read("xl/worksheets/sheet7.xml").decode("utf-8")
    row = re.search(r'<row\b[^>]*r="3"[^>]*>[\s\S]*?</row>', arch).group(0)
    for col in ["A", "B", "N", "O", "DV", "EP", "EQ", "ER", "ES", "ET", "EU", "EV", "EX"]:
        cell = re.search(rf'<c r="{col}3"([^>]*?)(?:/>|>(?:<v>(.*?)</v>)?</c>)', row)
        if not cell:
            print(col, "ABSENT")
            continue
        attrs, val = cell.group(1), cell.group(2)
        if not val:
            shown = ""
        elif 't="s"' in attrs:
            shown = sst[int(val)][:180].replace("\n", " / ")
        else:
            shown = val
        print(col, attrs.strip(), shown)
    # style of provenance cells
    print("--- styles ---")
    for col in ["EP", "EQ", "ER", "ES", "ET", "EU", "EV"]:
        cell = re.search(rf'<c r="{col}3"([^>]*?)(?:/>|>)', row)
        print(col, cell.group(0)[:80] if cell else "none")
