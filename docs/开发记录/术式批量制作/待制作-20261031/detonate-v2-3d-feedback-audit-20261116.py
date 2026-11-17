# -*- coding: utf-8 -*-
import hashlib
import html
import io
import re
import zipfile
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
paths = {
    "live": Path(r"D:\alianximod\docs\术式制作表.xlsx"),
    "backup": batch / "备份" / "detonate-v2-3d-feedback-术式制作表-20261116-155706-1.xlsx",
    "candidate": batch / "候选" / "detonate-v2-3d-feedback-术式制作表-20261116-155706-1.xlsx",
}
parts = {
    "done": "xl/worksheets/sheet1.xml",
    "todo": "xl/worksheets/sheet2.xml",
    "making": "xl/worksheets/sheet3.xml",
    "accept": "xl/worksheets/sheet4.xml",
}


def rows_of(sheet_xml):
    return re.findall(
        r"<(?:[\w]+:)?row\b[^>]*/>|<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>",
        sheet_xml,
    )


def row_number(row_xml):
    return int(re.search(r'\br="(\d+)"', row_xml).group(1))


def cell_xml(row_xml, column, number):
    matches = list(re.finditer(rf'<(?:[\w]+:)?c\b[^>]*\br="{column}{number}"', row_xml))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row_xml.find(">", start)
    if row_xml[opener_end - 1] == "/":
        end = opener_end + 1
    else:
        close = re.search(r"</(?:[\w]+:)?c>", row_xml[opener_end:])
        if not close:
            return None
        end = opener_end + close.end()
    return row_xml[start:end]


def cell_text(cell):
    if cell is None:
        return None
    inline = re.search(r"<(?:[\w]+:)?is\b[^>]*>([\s\S]*)</(?:[\w]+:)?is>", cell)
    if inline:
        nodes = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", inline.group(1))
        if len(nodes) != 1:
            return f"BAD_TEXT_NODES:{len(nodes)}"
        return html.unescape(nodes[0])
    value = re.search(r"<(?:[\w]+:)?v(?: [^>]*)?>([\s\S]*)</(?:[\w]+:)?v>", cell)
    if value:
        return html.unescape(value.group(1))
    return None


def ids_of(sheet_xml):
    found = []
    for row_xml in rows_of(sheet_xml):
        number = row_number(row_xml)
        if number < 3:
            continue
        code = cell_text(cell_xml(row_xml, "B", number))
        if code and code.strip() and not code.startswith("BAD_"):
            found.append((code.strip(), number))
    return found


report = []
for label, path in paths.items():
    data = path.read_bytes()
    report.append(f"{label} {len(data)} {hashlib.sha256(data).hexdigest().upper()}")
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        report.append(f"{label} zip {len(archive.namelist())} shared {'xl/sharedStrings.xml' in archive.namelist()}")
        all_codes = []
        for sheet_label, name in parts.items():
            xml = archive.read(name).decode("utf-8")
            found = ids_of(xml)
            all_codes.extend(code for code, _ in found)
            report.append(f"{label} {sheet_label} {len(found)}")
            if sheet_label == "making":
                report.append("MAKING " + ",".join(f"{number}:{code}" for code, number in found))
                target = [item for item in found if item[0] == "fire_detonate"]
                report.append(f"TARGET {target}")
                if target:
                    number = target[0][1]
                    row = next(row for row in rows_of(xml) if row_number(row) == number)
                    for column in ("N", "O", "EK", "DV"):
                        value = cell_text(cell_xml(row, column, number)) or ""
                        digest = hashlib.sha256(value.encode("utf-8")).hexdigest().upper()
                        report.append(f"{label} {column} {len(value)} {digest}")
                    if label == "candidate":
                        (batch / "detonate-v2-3d-feedback-dv-candidate.txt").write_text(cell_text(cell_xml(row, "DV", number)), encoding="utf-8", newline="\n")
        report.append(f"{label} total {len(all_codes)} unique {len(set(all_codes))}")

(batch / "detonate-v2-3d-feedback-audit.txt").write_text("\n".join(report) + "\n", encoding="utf-8", newline="\n")
