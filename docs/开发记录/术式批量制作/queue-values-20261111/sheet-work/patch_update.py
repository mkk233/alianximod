# -*- coding: utf-8 -*-
from pathlib import Path

path = Path(__file__).with_name("update_values.py")
text = path.read_text(encoding="utf-8")
start = text.find("    trajectory = (")
end = text.find("    lookup_new = lookup.replace", start)
if start < 0 or end < 0:
    raise SystemExit("trajectory block missing")
replacement = '''    trajectory = (
        '<row r="11" ht="31.2" customHeight="1" s="19">'
        + inline("A", 11, 29, "平行")
        + inline("B", 11, 29, "PARALLEL")
        + inline("C", 11, 29, "沿施放朝向的水平方向飞行，保留原生成位置")
        + "</row>"
    )
    role = (
        '<row r="17" ht="46.8" customHeight="1" s="19">'
        + inline("E", 17, 29, "平行改写")
        + inline("F", 17, 29, "TRAJECTORY_PARALLEL")
        + inline("G", 17, 29, "把后续允许改写的术式改成水平飞行；不改生成位置，不与垂落、喷涌、龙卷叠成多套运动")
        + "</row>"
    )
'''
text = text[:start] + replacement + text[end:]
needle = '    workbook = data["xl/workbook.xml"].decode("utf-8")\n'
insert = needle + '    styles = ensure_wrap_style(data["xl/styles.xml"].decode("utf-8"))\n'
if needle not in text:
    raise SystemExit("workbook decode missing")
text = text.replace(needle, insert, 1)
needle = '        "xl/workbook.xml": workbook_new.encode("utf-8"),\n    }\n'
insert = '        "xl/workbook.xml": workbook_new.encode("utf-8"),\n        "xl/styles.xml": styles.encode("utf-8"),\n    }\n'
if needle not in text:
    raise SystemExit("patch close missing")
text = text.replace(needle, insert, 1)
helper = '''
def ensure_wrap_style(styles):
    if styles.count("<xf ") != 32:
        raise SystemExit("Style count changed")
    marker = '</cellXfs>'
    added = (
        '<xf numFmtId="0" fontId="3" fillId="10" borderId="0" applyAlignment="1" '
        'pivotButton="0" quotePrefix="0" xfId="0"><alignment horizontal="center" '
        'vertical="center" wrapText="1"/></xf></cellXfs>'
    )
    styles = styles.replace(marker, added, 1)
    styles = re.sub(
        r'(<cellXfs[^>]*count=")(\d+)(")',
        lambda match: match.group(1) + str(int(match.group(2)) + 1) + match.group(3),
        styles,
        count=1,
    )
    if styles.count("<xf ") != 33:
        raise SystemExit("Wrapped style was not appended")
    return styles


'''
marker = "\ndef main():\n"
if marker not in text:
    raise SystemExit("main missing")
text = text.replace(marker, "\n" + helper + "def main():\n", 1)
path.write_text(text, encoding="utf-8", newline="\n")
print("patched")
