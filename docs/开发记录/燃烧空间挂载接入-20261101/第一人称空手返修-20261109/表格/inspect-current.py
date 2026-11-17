# -*- coding: utf-8 -*-
"""Read-only location of fire_burn. Does not save the workbook."""
import html
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"


def local(tag):
    return tag.rsplit("}", 1)[-1]


def shared_list(xml):
    root = ET.fromstring(xml)
    found = []
    for item in root:
        if local(item.tag) != "si":
            continue
        texts = []
        for node in item.iter():
            if local(node.tag) == "t" and node.text:
                texts.append(html.unescape(node.text))
            elif local(node.tag) == "t" and node.tail:
                pass
        found.append("".join(texts))
    return found


def rows_of(xml):
    return re.findall(r"<row\b[^>]*>[\s\S]*?</row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_span(row, column, number):
    matches = list(re.finditer(rf'<c r="{column}{number}"', row))
    if len(matches) != 1:
        return None
    start = matches[0].start()
    opener_end = row.find(">", start)
    if row[opener_end - 1] == "/":
        return row[start : opener_end + 1]
    close = re.search(r"</c>", row[opener_end:])
    if not close:
        return None
    return row[start : opener_end + close.end()]


def cell_text(cell, shared):
    if cell is None:
        return None
    if 't="inlineStr"' in cell or "<is>" in cell:
        texts = re.findall(r"<t[^>]*>([\s\S]*?)</t>", cell)
        return html.unescape("".join(texts))
    value = re.search(r"<v>([\s\S]*?)</v>", cell)
    if not value:
        return None
    raw = html.unescape(value.group(1))
    if 't="s"' in cell and raw.isdigit():
        return shared[int(raw)]
    return raw


def read_at(row, column, number, shared):
    return cell_text(cell_span(row, column, number), shared)


def main():
    data = LIVE.read_bytes()
    book = zipfile.ZipFile(LIVE)
    names = book.namelist()
    rels = ET.fromstring(book.read("xl/_rels/workbook.xml.rels"))
    targets = {}
    for node in rels:
        target = "xl/" + node.attrib["Target"].lstrip("/")
        targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
    work = ET.fromstring(book.read("xl/workbook.xml"))
    sheets = []
    for node in work.find("s:sheets", NS):
        sheets.append({
            "name": node.attrib["name"],
            "part": targets[node.attrib[REL]],
        })
    shared = shared_list(book.read("xl/sharedStrings.xml"))
    workbook_xml = book.read("xl/workbook.xml").decode("utf-8")
    defined = re.findall(r"<definedName\b[^>]*>[\s\S]*?</definedName>", workbook_xml)
    comment_parts = [name for name in names if "comment" in name.lower()]
    status = ["已完成", "待制作", "制作中", "待验收"]
    counts = {}
    ids = {name: [] for name in status}
    hits = []
    headers = {}
    filters = {}
    dimensions = {}
    for sheet in sheets:
        xml = book.read(sheet["part"]).decode("utf-8")
        filters[sheet["name"]] = re.findall(r"<autoFilter\b[^>]*ref=\"([^\"]+)\"", xml)
        dimensions[sheet["name"]] = re.findall(r"<dimension\b[^>]*ref=\"([^\"]+)\"", xml)
        rows = rows_of(xml)
        if sheet["name"] in status or sheet["name"] in ("资源状态明细", "待制作原始数据", "数据表"):
            header_row = next((row for row in rows if row_number(row) == 2), None)
            if header_row:
                labels = {}
                for match in re.finditer(r'<c r="([A-Z]+)2"', header_row):
                    text = read_at(header_row, match.group(1), 2, shared)
                    if text in ("编号", "名称", "功能描述", "特效", "备注", "术式编号"):
                        labels[text] = match.group(1)
                headers[sheet["name"]] = labels
        id_column = "B"
        if sheet["name"] == "资源状态明细":
            id_column = headers.get(sheet["name"], {}).get("术式编号", "A")
        if sheet["name"] in status:
            values = []
            for row in rows:
                number = row_number(row)
                if number < 3:
                    continue
                value = read_at(row, "B", number, shared)
                if value and str(value).strip():
                    values.append({"row": number, "id": str(value).strip(), "raw": value})
            ids[sheet["name"]] = values
            counts[sheet["name"]] = len(values)
        for row in rows:
            number = row_number(row)
            if number < 3 and sheet["name"] in status:
                continue
            for column in ("A", "B", "C"):
                value = read_at(row, column, number, shared)
                if value and "fire_burn" == str(value).strip():
                    item = {
                        "sheet": sheet["name"],
                        "part": sheet["part"],
                        "row": number,
                        "column": column,
                        "A": read_at(row, "A", number, shared),
                        "B": read_at(row, "B", number, shared),
                        "N": read_at(row, "N", number, shared),
                        "O": read_at(row, "O", number, shared),
                        "DV": read_at(row, "DV", number, shared),
                        "cellCount": len(re.findall(r"<c\b", row)),
                    }
                    hits.append(item)
    flat = []
    for name in status:
        for item in ids[name]:
            flat.append((item["id"], name, item["row"]))
    dupes = {}
    for key, name, row in flat:
        dupes.setdefault(key, []).append({"sheet": name, "row": row})
    repeated = {key: places for key, places in dupes.items() if len(places) > 1}
    comment_hits = []
    for part in comment_parts:
        text = book.read(part).decode("utf-8", errors="replace")
        if "fire_burn" in text or re.search(r'ref="[A-Z]+7"', text):
            comment_hits.append({"part": part, "length": len(text)})
    out = {
        "bytes": len(data),
        "sheets": sheets,
        "counts": counts,
        "total": sum(counts.values()),
        "unique": len(dupes),
        "repeated": repeated,
        "headers": headers,
        "filters": {key: value for key, value in filters.items() if value},
        "dimensions": dimensions,
        "hits": hits,
        "definedNames": defined,
        "commentParts": comment_parts,
        "commentHits": comment_hits,
        "makingIds": [item["id"] for item in ids["制作中"]],
        "pendingIds": [item["id"] for item in ids["待验收"]],
        "queueIds": [item["id"] for item in ids["待制作"]],
    }
    Path(__file__).with_name("inspect-current.json").write_text(
        json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(json.dumps({
        "counts": counts,
        "total": out["total"],
        "unique": out["unique"],
        "repeatedCount": len(repeated),
        "hits": [{k: item[k] for k in ("sheet", "row", "column", "A", "B")} for item in hits],
        "headers": headers,
        "filters": out["filters"],
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
