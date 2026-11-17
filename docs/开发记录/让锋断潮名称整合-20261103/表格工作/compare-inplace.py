# -*- coding: utf-8 -*-
import hashlib
import html
import json
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

WORK = Path(r"D:\alianximod\docs\开发记录\让锋断潮名称整合-20261103\表格工作")
PREP = json.loads((WORK / "prepare-inplace.json").read_text(encoding="utf-8"))
LIVE = Path(PREP["backup"])
CANDIDATE = Path(PREP["candidate"])


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
        found.append("".join(texts))
    return found


def cell_map(xml, shared):
    root = ET.fromstring(xml)
    found = {}
    for row in root.iter():
        if local(row.tag) != "row":
            continue
        number = row.attrib.get("r")
        for cell in list(row):
            if local(cell.tag) != "c":
                continue
            ref = cell.attrib.get("r")
            kind = cell.attrib.get("t")
            style = cell.attrib.get("s")
            if kind == "inlineStr":
                text = "".join(html.unescape(node.text or "") for node in cell.iter() if local(node.tag) == "t")
            else:
                value = next((child.text for child in list(cell) if local(child.tag) == "v"), None)
                text = None if value is None else html.unescape(value)
                if kind == "s" and text and text.isdigit():
                    text = shared[int(text)]
            found[ref] = {"text": text, "style": style, "kind": kind}
    return found


def features(xml):
    return {
        "dimension": re.findall(r'<dimension ref="([^"]+)"', xml),
        "autoFilter": re.findall(r'<autoFilter ref="([^"]+)"', xml),
        "sqref": sorted(set(re.findall(r'sqref="([^"]+)"', xml))),
        "merges": sorted(re.findall(r'<mergeCell ref="([^"]+)"', xml)),
        "validations": len(re.findall(r"<dataValidation\b", xml)),
    }


def main():
    with zipfile.ZipFile(LIVE) as left, zipfile.ZipFile(CANDIDATE) as right:
        left_names = left.namelist()
        right_names = right.namelist()
        only_left = sorted(set(left_names) - set(right_names))
        only_right = sorted(set(right_names) - set(left_names))
        changed = []
        for name in sorted(set(left_names) & set(right_names)):
            if left.read(name) != right.read(name):
                changed.append(name)
        print(json.dumps({
            "onlyLeft": only_left,
            "onlyRight": only_right,
            "changedCount": len(changed),
            "changed": changed,
        }, ensure_ascii=False, indent=2))
        left_shared = shared_list(left.read("xl/sharedStrings.xml")) if "xl/sharedStrings.xml" in left_names else []
        right_shared = shared_list(right.read("xl/sharedStrings.xml")) if "xl/sharedStrings.xml" in right_names else []
        sheets = [name for name in left_names if name.startswith("xl/worksheets/sheet")]
        value_changes = []
        style_changes = []
        feature_changes = {}
        for name in sheets:
            if name not in right_names:
                continue
            left_xml = left.read(name)
            right_xml = right.read(name)
            left_cells = cell_map(left_xml, left_shared)
            right_cells = cell_map(right_xml, right_shared)
            refs = set(left_cells) | set(right_cells)
            for ref in sorted(refs):
                old = left_cells.get(ref)
                new = right_cells.get(ref)
                if (old or {}).get("text") != (new or {}).get("text"):
                    value_changes.append({"sheet": name, "ref": ref, "old": None if not old else old["text"], "new": None if not new else new["text"]})
                elif old and new and old.get("style") != new.get("style"):
                    style_changes.append({"sheet": name, "ref": ref, "old": old.get("style"), "new": new.get("style")})
            left_feat = features(left_xml.decode("utf-8"))
            right_feat = features(right_xml.decode("utf-8"))
            if left_feat != right_feat:
                feature_changes[name] = {"old": left_feat, "new": right_feat}
        print("VALUE", len(value_changes))
        for item in value_changes:
            old = item["old"]
            new = item["new"]
            item["old"] = old if old is None or len(str(old)) < 80 else str(old)[:80] + "..."
            item["new"] = new if new is None or len(str(new)) < 80 else str(new)[:80] + "..."
            print(json.dumps(item, ensure_ascii=False))
        print("STYLE", len(style_changes))
        print(json.dumps(style_changes[:30], ensure_ascii=False))
        print("FEATURES", json.dumps(feature_changes, ensure_ascii=False)[:2000])


if __name__ == "__main__":
    main()
