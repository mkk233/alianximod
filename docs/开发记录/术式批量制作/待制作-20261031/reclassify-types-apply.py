# -*- coding: utf-8 -*-
"""Reclassify current status-sheet types. Do not touch the archive or other columns."""
import hashlib
import importlib.util
import json
import re
import sys
import zipfile
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree as ET

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
spec = importlib.util.spec_from_file_location("archive_write", HERE / "archive-write.py")
aw = importlib.util.module_from_spec(spec)
spec.loader.exec_module(aw)

EXPECTED_TYPES = ["剑术", "动作", "法术", "通用", "领域附魔", "附魔"]
PAGES = ("已完成", "待制作", "制作中", "待验收")
ROWS = [
    ("已完成", "sword_thrust", "剑术", "剑术", "正前方突刺，链上角色是攻击，保持剑术。"),
    ("已完成", "sword_horizontal_slash", "剑术", "剑术", "正前方扇形斩击，保持剑术。"),
    ("已完成", "sword_vertical_slash", "剑术", "剑术", "高倍率重击，保持剑术。"),
    ("已完成", "space_cut", "剑术", "剑术", "落点切割仍是剑术攻击，保持剑术。"),
    ("已完成", "tide_sever", "剑术", "剑术", "水属性薄切，链上角色是攻击，保持剑术。"),
    ("已完成", "stone_hammer", "剑术", "剑术", "土属性落点重击，保持剑术。"),
    ("已完成", "water_yielding_ribbon", "剑术", "剑术", "伤害来自中间那一刀，保持剑术。"),
    ("已完成", "phantom_sword", "剑术", "剑术", "复制主手剑飞出，保持剑术。"),
    ("已完成", "sword_come", "剑术", "剑术", "召回周围的剑；装配消耗 0 仍是剑术，不是动作。"),
    ("已完成", "sword_return_all", "剑术", "剑术", "收回已放出的剑再飞向目标，保持剑术。"),
    ("已完成", "water_blade", "剑术", "剑术", "必须手持剑的弧斩，保持剑术。"),
    ("已完成", "movement_dash", "动作", "动作", "向前高速移动，释放种类是动作，装配消耗 0，保持动作。"),
    ("已完成", "movement_leap", "动作", "动作", "向上弹射，装配消耗 0，保持动作。"),
    ("已完成", "magic_arcane_missile", "法术", "法术", "追踪弹幕，保持法术。"),
    ("已完成", "magic_create_water", "法术", "法术", "放置水源，保持法术。"),
    ("已完成", "test_sustained", "法术", "法术", "测试用持续弹，保持法术。"),
    ("已完成", "heat_beam", "法术", "法术", "持续火光束，保持法术。"),
    ("已完成", "parasite_seed", "法术", "法术", "种子陷阱攻击，保持法术。"),
    ("已完成", "metal_needle", "法术", "法术", "金属细针，保持法术。"),
    ("已完成", "fire_orb", "法术", "法术", "火球飞弹，保持法术。"),
    ("已完成", "fire_burst", "法术", "法术", "撞击或延迟后爆炸，保持法术。"),
    ("已完成", "water_weeping_arc", "法术", "法术", "水帘攻击，保持法术。"),
    ("已完成", "water_curtain", "法术", "法术", "身前水墙，附魔标记为否，保持法术。"),
    ("已完成", "water_mist", "法术", "法术", "自身雨雾领域，附魔标记为否；列表没有单独的领域，保持法术。"),
    ("已完成", "water_ring", "法术", "法术", "自身水环护体，附魔标记为否，保持法术。"),
    ("已完成", "water_deep_pressure", "法术", "法术", "自身重压领域，附魔标记为否，保持法术。"),
    ("已完成", "magic_ignite", "通用", "附魔", "附魔标记为是，效果键 fire，装配和魔力都是 2，命中后点燃。"),
    ("已完成", "universal_lock_target", "通用", "通用", "索敌规则，附魔标记为否，两书可用，保持通用。"),
    ("已完成", "rule_dual_release", "通用", "通用", "释放种类是规则，不是附魔，保持通用。"),
    ("已完成", "trajectory_drop", "通用", "通用", "只改下一段路径，保持通用。"),
    ("已完成", "rule_cast_anchor", "通用", "通用", "落点规则，保持通用。"),
    ("已完成", "rule_triple_cast", "通用", "通用", "复制后段攻击的规则，保持通用。"),
    ("已完成", "rule_gather_energy", "通用", "通用", "放大后段攻击能量；链上角色写攻击，但本体不单独打人，保持通用。"),
    ("已完成", "trajectory_3d_interleave", "通用", "通用", "只错开落点，保持通用。"),
    ("已完成", "vine_mark", "通用", "附魔", "附魔标记为是，效果键 wood，命中后缠住目标。"),
    ("已完成", "tide_mark", "通用", "附魔", "附魔标记为是，效果键 water，命中后附加伤害并加潮湿。"),
    ("已完成", "sinking_edge", "通用", "附魔", "附魔标记为是，效果键 earth，给攻击加重量、范围和击退。"),
    ("已完成", "keen_mark", "通用", "附魔", "附魔标记为是，效果键 metal，给攻击加金属伤害并缩短冷却。"),
    ("已完成", "trajectory_tracking", "通用", "通用", "只改飞行弹的追踪，保持通用。"),
    ("已完成", "trajectory_storm", "通用", "通用", "把后段可变弹道改成龙卷，保持通用。"),
    ("制作中", "fire_heat_up", "法术", "法术", "功能写明是独立火属性法术；链上角色的动作不是类型里的动作。"),
    ("制作中", "fire_burn", "法术", "法术", "功能写明是独立火属性法术，进入或升级炉心。"),
    ("制作中", "fire_cool", "法术", "法术", "功能写明是独立火属性法术，主动结束炉心。"),
    ("制作中", "fire_flame_trail", "通用", "附魔", "功能写明是火系附魔，附在移动载体上留火径，附魔标记为是。"),
    ("制作中", "fire_meteorite", "法术", "法术", "抛出炽热岩体的攻击，附魔标记为否，保持法术。"),
    ("制作中", "fire_shock_ring", "法术", "法术", "一次环形冲击攻击，保持法术。"),
    ("制作中", "rule_surface_anchor", "通用", "通用", "选择地表锚点的规则，附魔标记为否；列表没有规则类，与落点指定一样保持通用。"),
    ("制作中", "fire_upwell", "通用", "通用", "只把攻击改成上抛再落下，保持通用。"),
    ("制作中", "rule_stagger", "通用", "通用", "只错开已有副本的出手时间，保持通用。"),
    ("制作中", "rule_hit_continue", "通用", "通用", "绑定父攻击和后续分支的规则，保持通用。"),
    ("制作中", "fire_seed_sow", "通用", "附魔", "功能写明是命中后产火种的附魔，单独时交给下一次普通攻击。"),
    ("制作中", "fire_kindle", "通用", "附魔", "功能写明是火种持续伤害附魔。"),
    ("制作中", "fire_detonate", "通用", "附魔", "功能写明是消费剩余火种的结算附魔。"),
    ("制作中", "fire_nether_gloom", "领域附魔", "领域附魔", "附魔标记为是，费用按附魔，效果是领域内产种。列表有领域附魔、没有单独的领域。可独立释放只否定旧前置，不改成法术。"),
    ("制作中", "earth_break_ground", "通用", "附魔", "附魔标记为是，让适用攻击撞地时挖坑，自己不另造成生物伤害。"),
    ("制作中", "fire_spark", "法术", "法术", "独立火苗飞弹，附魔标记为否，保持法术。"),
    ("待验收", "fire_fentian", "法术", "法术", "给球内目标挂燃烧的攻击法术，附魔标记为否，保持法术。"),
    ("待验收", "fire_liaoli", "剑术", "剑术", "必须手持剑的持续刺击，附魔标记为否，保持剑术。"),
    ("待验收", "fire_scorch_field", "法术", "法术", "贴地火场本体，附魔标记为否，是载体不是领域附魔，保持法术。"),
    ("待验收", "fire_liaoyuan", "法术", "法术", "贴地火场攻击，附魔标记为否，保持法术。"),
]


def load(path):
    payload = path.read_bytes()
    with zipfile.ZipFile(path) as archive:
        order = archive.infolist()
        parts = {info.filename: archive.read(info.filename) for info in order}
    shared = []
    if "xl/sharedStrings.xml" in parts:
        root = ET.fromstring(parts["xl/sharedStrings.xml"])
        for item in root:
            if aw.local(item.tag) == "si":
                texts = []
                for node in item.iter():
                    if aw.local(node.tag) == "t" and node.text:
                        texts.append(node.text)
                shared.append("".join(texts))
    names = aw.sheet_map(parts["xl/workbook.xml"], parts["xl/_rels/workbook.xml.rels"])
    return payload, order, parts, shared, names


def type_values(parts, names, shared):
    xml = parts[names["数据表"]].decode("utf-8")
    rows = {aw.row_number(row): row for row in aw.rows_of(xml)}
    values = []
    number = 4
    while number in rows:
        value = aw.read_at(rows[number], "Q", number, shared)
        if not value:
            return values, number
        values.append(value)
        number += 1
    return values, None


def page_rows(parts, names, shared):
    found = {}
    for title in PAGES:
        for code, number, row in aw.ids_of(parts[names[title]].decode("utf-8"), shared):
            found.setdefault(code, []).append((title, number, row))
    return found


def apply():
    payload, order, parts, shared, names = load(LIVE)
    baseline = hashlib.sha256(payload).hexdigest().upper()
    recorded = json.loads((HERE / "reclassify-types-20261101-read.json").read_text(encoding="utf-8"))
    if baseline != recorded["sha256"]:
        aw.fail("读取后源表变了")
    values, next_row = type_values(parts, names, shared)
    if values != EXPECTED_TYPES or next_row != 10:
        aw.fail("类型列表与读取不一致: " + ",".join(values))
    located = page_rows(parts, names, shared)
    if len(ROWS) != 60 or len({item[1] for item in ROWS}) != 60:
        aw.fail("分类清单不是 60 个唯一编号")
    by_sheet = {title: [] for title in PAGES}
    for title, code, old, new, _basis in ROWS:
        places = located.get(code, [])
        if len(places) != 1 or places[0][0] != title:
            aw.fail("编号位置不对: " + code)
        _title, number, row = places[0]
        current = aw.read_at(row, "C", number, shared)
        if current != old:
            aw.fail(code + " 当前类型是 " + str(current))
        if old != new:
            by_sheet[title].append((number, row, code, old, new))
    changed = {}
    for title, edits in by_sheet.items():
        if not edits:
            continue
        xml = parts[names[title]].decode("utf-8")
        rows = aw.rows_of(xml)
        edit_at = {number: (row, code, old, new) for number, row, code, old, new in edits}
        new_rows = []
        for row in rows:
            number = aw.row_number(row)
            if number not in edit_at:
                new_rows.append(row)
                continue
            original, code, old, new = edit_at[number]
            if row != original:
                aw.fail("行内容对不上: " + code)
            sample = aw.cell_span(row, "C", number)
            style = aw.style_of(sample[0]) if sample else None
            updated, action = aw.force_text(row, "C", number, new, {old}, shared, style)
            if action != "forced" or aw.read_at(updated, "C", number, shared) != new:
                aw.fail("类型没写上: " + code)
            if aw.read_at(updated, "B", number, shared) != code:
                aw.fail("编号被改动: " + code)
            if aw.read_at(updated, "N", number, shared) != aw.read_at(original, "N", number, shared):
                aw.fail("功能描述被改动: " + code)
            new_rows.append(updated)
        changed[names[title]] = aw.replace_data(xml, new_rows).encode("utf-8")
    workbook = parts["xl/workbook.xml"].decode("utf-8")
    old_range = "数据表!$Q$4:$Q$10"
    new_range = "数据表!$Q$4:$Q$9"
    matches = list(re.finditer(re.escape(old_range) + r"(?!\d)", workbook))
    if len(matches) != 1:
        aw.fail("类型命名范围出现 %s 次" % len(matches))
    match = matches[0]
    workbook = workbook[:match.start()] + new_range + workbook[match.end():]
    changed["xl/workbook.xml"] = workbook.encode("utf-8")
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = HERE / "备份" / f"术式制作表-reclassify-types-{stamp}-1.xlsx"
    output = HERE / "reclassify-types-20261101-candidate-preserved.xlsx"
    backup.parent.mkdir(exist_ok=True)
    backup.write_bytes(payload)
    with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
        for info in order:
            dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
    with zipfile.ZipFile(output) as dst:
        different = [name for name in dst.namelist() if dst.read(name) != parts[name]]
        if set(different) != set(changed):
            aw.fail("意外改动: " + ",".join(different))
        for title in ("待制作", "待验收", "数据表", "待制作原始数据", "资源状态明细"):
            if dst.read(names[title]) != parts[names[title]]:
                aw.fail(title + " 被改动")
        new_parts = dict(parts)
        new_parts.update(changed)
        after_values, after_next = type_values(new_parts, names, shared)
        if after_values != EXPECTED_TYPES or after_next != 10:
            aw.fail("类型选项被改动")
        after = page_rows(new_parts, names, shared)
        invalid = []
        for title, code, old, new, _basis in ROWS:
            places = after[code]
            if len(places) != 1 or places[0][0] != title:
                aw.fail("写后位置变了: " + code)
            _title, number, row = places[0]
            current = aw.read_at(row, "C", number, shared)
            if current != new:
                aw.fail(code + " 写后类型是 " + str(current))
            if current not in EXPECTED_TYPES:
                invalid.append(code)
        if invalid:
            aw.fail("非法类型: " + ",".join(invalid))
        counts = {}
        for title in PAGES:
            counts[title] = len(aw.ids_of(new_parts[names[title]].decode("utf-8"), shared))
        if counts != {"已完成": 40, "待制作": 0, "制作中": 16, "待验收": 4}:
            aw.fail("行数变了")
    summary = {
        "baselineSha256": baseline,
        "candidateSha256": hashlib.sha256(output.read_bytes()).hexdigest().upper(),
        "backup": str(backup),
        "changedCount": sum(1 for _title, _code, old, new, _basis in ROWS if old != new),
        "keptCount": sum(1 for _title, _code, old, new, _basis in ROWS if old == new),
        "oldRange": old_range,
        "newRange": new_range,
        "optionsUntouched": True,
        "submitted": False,
    }
    (HERE / "reclassify-types-20261101-build.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(summary, ensure_ascii=False))


def report():
    submit = json.loads((HERE / "reclassify-types-20261101-submit.json").read_text(encoding="utf-8"))
    payload, _order, parts, shared, names = load(LIVE)
    live = hashlib.sha256(payload).hexdigest().upper()
    values, _next_row = type_values(parts, names, shared)
    defined = re.search(r'<definedName name="术式类型">([^<]*)</definedName>', parts["xl/workbook.xml"].decode("utf-8"))
    located = page_rows(parts, names, shared)
    rows = []
    invalid = []
    changed = 0
    for title, code, old, new, basis in ROWS:
        places = located.get(code, [])
        current = None
        number = None
        if len(places) == 1:
            number = places[0][1]
            current = aw.read_at(places[0][2], "C", number, shared)
        if current not in EXPECTED_TYPES:
            invalid.append(code)
        applied = bool(submit.get("submitted")) and live == submit.get("sha256") and current == new
        if applied and old != new:
            changed += 1
        rows.append({
            "sheet": title,
            "row": number,
            "id": code,
            "old": old,
            "new": new,
            "current": current,
            "changed": applied and old != new,
            "basis": basis,
        })
    result = {
        "submitted": bool(submit.get("submitted")) and live == submit.get("sha256"),
        "submitError": submit.get("error"),
        "candidate": str(HERE / "reclassify-types-20261101-candidate-preserved.xlsx"),
        "enum": values,
        "definedName": defined.group(1) if defined else None,
        "checked": len(rows),
        "changed": changed,
        "kept": sum(1 for item in rows if item["current"] == item["new"] and item["old"] == item["new"]),
        "intended": sum(1 for item in rows if item["old"] != item["new"]),
        "unresolved": 0,
        "ambiguities": [],
        "invalidRemaining": invalid,
        "counts": {title: len(aw.ids_of(parts[names[title]].decode("utf-8"), shared)) for title in PAGES},
        "rows": rows,
        "baselineSha256": "F098F1C154DEA04185CAD4F60695054A54F27B54E1D6DFAFADD6449E29FAF9E4",
        "liveSha256": live,
        "bytes": len(payload),
        "mtime": datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
        "reopenMatches": bool(submit.get("sha256")) and live == submit.get("sha256") and all(item["current"] == item["new"] for item in rows),
        "archiveUntouched": True,
        "codeConflict": "TechniqueType 仍只有 SWORD、MOVEMENT、MAGIC、UNIVERSAL。表内附魔和领域附魔没有代码列。本次没改 Java。",
        "method": "ooxml-part-copy",
        "exportRebuilt": False,
    }
    (HERE / "reclassify-types-20261101-report.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps({key: result[key] for key in result if key != "rows"}, ensure_ascii=False))


if __name__ == "__main__":
    mode = sys.argv[1]
    if mode == "apply":
        apply()
    elif mode == "report":
        report()
    else:
        aw.fail("未知模式")
