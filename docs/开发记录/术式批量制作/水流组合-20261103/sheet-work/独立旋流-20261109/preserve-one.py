# -*- coding: utf-8 -*-
"""Keep native package parts while applying one authored row move."""
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).parent
AUTH = json.loads((ROOT / "authored-rows.json").read_text(encoding="utf-8"))
backup = ROOT / AUTH.get("backup", "original-backup.xlsx")
original = backup.read_bytes()
if hashlib.sha256(original).hexdigest().upper() != AUTH["sourceHash"]:
    raise SystemExit("Backup hash differs")
with zipfile.ZipFile(backup) as book:
    entries = book.infolist()
    data = {info.filename: book.read(info.filename) for info in entries}
ns = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
rels = ET.fromstring(data["xl/_rels/workbook.xml.rels"])
targets = {}
for node in rels:
    target = "xl/" + node.attrib["Target"].lstrip("/")
    targets[node.attrib["Id"]] = target.replace("xl/xl/", "xl/")
work = ET.fromstring(data["xl/workbook.xml"])
names = {}
for node in work.find("s:sheets", ns):
    names[node.attrib["name"]] = targets[node.attrib["{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"]]
src_name = AUTH.get("fromSheet", "待验收")
dst_name = AUTH.get("toSheet", "制作中")
src_part = names[src_name]
dst_part = names[dst_name]
src = data[src_part].decode("utf-8")
dst = data[dst_part].decode("utf-8")
sst = data["xl/sharedStrings.xml"].decode("utf-8")
items = ["".join(ET.fromstring(body).itertext()) for body in re.findall(r"<si\b[^>]*>[\s\S]*?</si>", sst)]
lookup = {value: index for index, value in enumerate(items)}
added = []

def intern(value):
    if value not in lookup:
        lookup[value] = len(items) + len(added)
        added.append(value)
    return lookup[value]

def rows(text):
    return {int(match.group(1)): match.group(0) for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', text)}

def shift_dimension(xml, before_max, after_max):
    if before_max == after_max:
        return xml
    old = f'ref="A1:EN{before_max}"'
    new = f'ref="A1:EN{after_max}"'
    if xml.count(old) != 1:
        raise SystemExit(f"Dimension {old} count {xml.count(old)}")
    return xml.replace(old, new, 1)

def shift_filter(xml, sheet, before_max, after_max):
    if before_max == after_max:
        return xml
    old = f"{sheet}!$A$2:$EN${before_max}"
    if old not in xml:
        return xml
    if xml.count(old) != 1:
        raise SystemExit(f"Filter {old} count {xml.count(old)}")
    return xml.replace(old, f"{sheet}!$A$2:$EN${after_max}", 1)

src_rows = rows(src)
dst_rows = rows(dst)
old = AUTH["fromRow"]
dest = AUTH["toRow"]
if old not in src_rows or dest in dst_rows:
    raise SystemExit(f"Row endpoints {old} {dest}")
for part, payload in data.items():
    if "comment" in part.lower() and part.endswith(".xml"):
        if re.search(r'<comment\b[^>]*ref="[A-Z]+' + str(old) + '"', payload.decode("utf-8")):
            raise SystemExit(f"A comment is anchored on row {old}")
row = src_rows[old]
match = re.search(r'<c r="B' + str(old) + r'"[^>]*t="s"[^>]*><v>(\d+)</v>', row)
if not match or items[int(match.group(1))] != AUTH["id"]:
    raise SystemExit("Source id mismatch")
moved = re.sub(r'r="([A-Z]+)' + str(old) + r'\b"', lambda found: 'r="' + found.group(1) + str(dest) + '"', row)
moved = re.sub(r'\br="' + str(old) + '"', 'r="' + str(dest) + '"', moved, count=1)
for column, value in (("N", AUTH["n"]), ("O", AUTH["o"]), ("DV", AUTH["dv"])):
    pattern = r'(<c r="' + column + str(dest) + r'"[^>]*t="s"[^>]*>)<v>\d+</v>'
    moved, count = re.subn(
        pattern,
        lambda found, index=intern(value): found.group(1) + "<v>" + str(index) + "</v>",
        moved,
        count=1,
    )
    if count != 1:
        raise SystemExit(f"{column} replacements {count}")
if src.count(row) != 1:
    raise SystemExit("Source row is not unique")
src = src.replace(row, "", 1)
dst = dst.replace("</sheetData>", moved + "</sheetData>", 1)
src_before = max(src_rows)
dst_before = max(dst_rows)
src_after = max(rows(src))
dst_after = max(rows(dst))
src = shift_dimension(src, src_before, src_after)
dst = shift_dimension(dst, dst_before, dst_after)
book = shift_filter(data["xl/workbook.xml"].decode("utf-8"), src_name, src_before, src_after)
book = shift_filter(book, dst_name, dst_before, dst_after)
book_changed = book.encode("utf-8") != data["xl/workbook.xml"]
if added:
    sst = sst.replace(
        "</sst>",
        "".join('<si><t xml:space="preserve">' + html.escape(value, quote=False) + "</t></si>" for value in added) + "</sst>",
        1,
    )
sst = re.sub(r'uniqueCount="\d+"', 'uniqueCount="' + str(len(items) + len(added)) + '"', sst, count=1)
patch = {
    src_part: src.encode("utf-8"),
    dst_part: dst.encode("utf-8"),
    "xl/sharedStrings.xml": sst.encode("utf-8"),
}
if book_changed:
    patch["xl/workbook.xml"] = book.encode("utf-8")
if any(rows(src).get(number) != src_rows[number] for number in src_rows if number != old):
    raise SystemExit("Another source row changed")
if any(rows(dst).get(number) != dst_rows[number] for number in dst_rows):
    raise SystemExit("Existing destination row changed")
candidate = ROOT / AUTH.get("candidate", "candidate.xlsx")
with zipfile.ZipFile(candidate, "w") as book_out:
    for entry in entries:
        book_out.writestr(entry, patch.get(entry.filename, data[entry.filename]))
with zipfile.ZipFile(candidate) as book_in:
    changed = [name for name in data if data[name] != book_in.read(name)]
    if set(changed) != set(patch):
        raise SystemExit(f"Unexpected parts {changed}")
artifact = ROOT / AUTH.get("artifact", "artifact-authored.xlsx")
with zipfile.ZipFile(artifact) as authored:
    missing = sorted(set(data) - set(authored.namelist()))
result = {
    "changedParts": changed,
    "unchangedParts": len(data) - len(changed),
    "newSharedStrings": len(added),
    "nativePartsMissingFromArtifactExport": missing,
    "from": f"{src_name}第{old}行",
    "to": f"{dst_name}第{dest}行",
}
(ROOT / AUTH.get("preservation", "native-preservation.json")).write_text(
    json.dumps(result, ensure_ascii=False, indent=2),
    encoding="utf-8",
)
print(json.dumps(result, ensure_ascii=False))
