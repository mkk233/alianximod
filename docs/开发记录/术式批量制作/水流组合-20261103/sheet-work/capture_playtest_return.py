# -*- coding: utf-8 -*-
"""Save the three-item playtest snapshot before any workbook candidate."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

ROOT = Path(r"D:\alianximod")
LIVE = ROOT / "docs" / "术式制作表.xlsx"
WORK = ROOT / "docs" / "开发记录" / "术式批量制作" / "水流组合-20261103" / "sheet-work"
PRIOR = WORK / "待验收转换前完整需求快照.json"
DEEP = WORK / "运行故障-20261104待验收快照.json"
OUT = WORK / "实测三项-20261104返修快照.json"
BASELINE = "5CD41F680C9E0E98EF8A3BB191B6F2E29BE85F7D9FFE49882BF1224591F83305"
ORDER = ["water_vortex", "water_bubble_column", "water_deep_pressure"]
SHORT_N = {
    "water_vortex": "干地水平牵引。入卷收取水势；与上升气泡和真实水组成龙卷后才按湿度扣返",
    "water_bubble_column": "真实水里的上升水柱。底部与旋流重合时挂上去，父旋流关闭后不再独立续挂",
    "water_deep_pressure": "低矮的重压领域。范围内暂时积起不流动的水，里面的人变慢、变潮湿，氧气消耗加快",
}
USER_QUOTE = "\n".join([
    "水流漩涡的特效完全和你给的预览不一样，现在只有几条直线出现在这里",
    "其二",
    "深海重压生成的水域没有破坏掉地面上的草，应该只保留完成方块自动破坏草，蕨，火把等同类型方块",
    "其三聚怪效果错误的对施法者生效了",
    "都修复一下",
])
APPEND = "\n".join([
    "2026-11-04 用户实测原话：「" + USER_QUOTE + "」",
    "本次据此从待验收回制作中。旋流运行外观与已确认 v3.1 一致：干地水平弯曲内旋，组合为连续漏斗螺旋加水下汇入。深海水域冲掉草、蕨、火把等水可冲毁附着物，保留地形、建筑、楼梯、半砖、容器和自然水。聚怪排除施法者本人及载着施法者的骑乘组，组合气泡提升同样不重新带入本人。其他玩家和友军规则不变。上一轮区块加载无重入修复保留。不回退为待确认，也不从原始档案恢复。",
    "设计 2/2、实现 3/3 不重置，本次无正式复审。",
    "制作中 | 水流组合-20261103 | docs/开发记录/术式批量制作/水流组合-20261103/记录.md",
])


def fail(message):
    raise SystemExit(message)


def col_letter(number):
    letters = ""
    while number:
        number, remainder = divmod(number - 1, 26)
        letters = chr(65 + remainder) + letters
    return letters


def shared_items(xml):
    items = []
    for body in re.findall(r"<si\b[^>]*>(.*?)</si>", xml, re.S):
        parts = re.findall(r"<t\b[^>]*>(.*?)</t>", body, re.S)
        items.append(html.unescape("".join(parts)))
    return items


def row_map(xml):
    found = {}
    for match in re.finditer(r'<row\b[^>]*r="(\d+)"[^>]*>[\s\S]*?</row>', xml):
        found[int(match.group(1))] = match.group(0)
    return found


def cell_value(row, column, number, shared):
    match = re.search(rf'<c r="{column}{number}\b"[^>]*(?:/>|>[\s\S]*?</c>)', row)
    if not match:
        return None
    cell = match.group(0)
    if cell.endswith("/>"):
        return ""
    if 't="inlineStr"' in cell or "<is>" in cell:
        return html.unescape("".join(re.findall(r"<t\b[^>]*>(.*?)</t>", cell, re.S)))
    raw = re.search(r"<v>(.*?)</v>", cell, re.S)
    if not raw:
        return ""
    if 't="s"' in cell:
        return shared[int(raw.group(1))]
    return raw.group(1)


def sheet_names(workbook_xml, rels_xml):
    rels = {}
    for match in re.finditer(r'Id="([^"]+)"[^>]*Target="([^"]+)"', rels_xml):
        rels[match.group(1)] = match.group(2)
    names = {}
    for match in re.finditer(r'<sheet[^>]*name="([^"]+)"[^>]*r:id="([^"]+)"', workbook_xml):
        target = rels[match.group(2)]
        if not target.startswith("xl/"):
            target = "xl/" + target.lstrip("/")
        names[match.group(1)] = target.replace("\\", "/")
    return names


def replace_once(text, old, new, label):
    if text.count(old) != 1:
        fail(f"{label} count {text.count(old)}")
    return text.replace(old, new, 1)


def main():
    payload = LIVE.read_bytes()
    source_hash = hashlib.sha256(payload).hexdigest().upper()
    if source_hash != BASELINE:
        fail(f"Source hash is {source_hash}")
    prior = json.loads(PRIOR.read_text(encoding="utf-8"))
    deep = json.loads(DEEP.read_text(encoding="utf-8"))
    vortex_n = replace_once(
        prior["rows"]["water_vortex"]["before"]["N"],
        "主人、其他玩家、盟友和敌人都受运动影响；这和深海只对敌人生效的减速、潮湿、窒息不是同一套关系。",
        "2026-11-04 用户实测修改覆盖此前「主人也受运动影响」：聚怪排除施法者本人，以及载着施法者的骑乘组；本人不被吸引、环绕或入卷，组合气泡提升也不把本人重新带入。其他玩家、盟友和敌人仍受运动影响。这和深海只对敌人生效的减速、潮湿、窒息不是同一套关系。",
        "vortex owner",
    )
    vortex_o = replace_once(
        prior["rows"]["water_vortex"]["before"]["O"],
        "当前v3实际Blockbench草稿待用户确认。",
        "2026-11-03 已采用 v3.1。运行外观与该确认版一致：干地是水平面内弯曲的内旋水带，不是贴地放射直线；组成水龙卷后是连续漏斗螺旋，并有水下汇入。历史草稿句「当前v3实际Blockbench草稿待用户确认」不再表示当前状态。",
        "vortex visual",
    )
    bubble_n = prior["rows"]["water_bubble_column"]["before"]["N"] + (
        "\n\n2026-11-04 用户实测修改：上升排除施法者本人，以及载着施法者的骑乘组。"
        "挂到旋流上之后的提升同样不把本人重新带入。其他玩家和友军的规则不变。"
    )
    bubble_o = replace_once(
        prior["rows"]["water_bubble_column"]["before"]["O"],
        "当前组合v3待确认。",
        "2026-11-03 已采用 v3.1，本次美术没有改版。历史草稿句「当前组合v3待确认」不再表示当前状态。",
        "bubble visual",
    )
    deep_n = replace_once(
        deep["originalNODV"]["N"],
        "不覆盖实心方块、已有的天然水、其他流体或方块实体，不主动替换植物。",
        "不覆盖挡移动的地形和建筑、楼梯、半砖、容器和方块实体，也不覆盖已有的天然水和其他流体。2026-11-04 用户实测修改覆盖此前「只填空气、不主动替换植物」：草、蕨、火把等水可冲毁且不挡移动、没有方块实体的附着物要先掉落一次再放上临时水。不能把非完整立方体一概删除。",
        "deep plants",
    ) + "\n\n上一轮区块加载无重入修复保留：ChunkEvent.Load 只读传入的 ChunkAccess，改世界仍延后到 tell / TickTask，不在回调里同步调用 Level.getBlockState、getChunk 或 setBlock。"
    deep_o = deep["originalNODV"]["O"]
    restored = {
        "water_vortex": {"N": vortex_n, "O": vortex_o},
        "water_bubble_column": {"N": bubble_n, "O": bubble_o},
        "water_deep_pressure": {"N": deep_n, "O": deep_o},
    }
    for code, item in restored.items():
        if item["O"].rstrip().endswith("待确认。"):
            fail(f"{code} O still ends unconfirmed")
        if "不主动替换植物。" in item["N"] or "主人、其他玩家、盟友和敌人都受运动影响" in item["N"]:
            fail(f"{code} kept a superseded rule")
    with zipfile.ZipFile(LIVE) as book:
        stored = {name: book.read(name) for name in book.namelist()}
    names = sheet_names(
        stored["xl/workbook.xml"].decode("utf-8"),
        stored["xl/_rels/workbook.xml.rels"].decode("utf-8"),
    )
    shared = shared_items(stored["xl/sharedStrings.xml"].decode("utf-8"))
    located = {code: [] for code in ORDER}
    for title in ("已完成", "待制作", "制作中", "待验收"):
        for number, row in row_map(stored[names[title]].decode("utf-8")).items():
            if number < 3:
                continue
            code = cell_value(row, "B", number, shared)
            if code in located:
                located[code].append((title, number))
    expected = {
        "water_vortex": [("待验收", 10)],
        "water_bubble_column": [("待验收", 11)],
        "water_deep_pressure": [("待验收", 12)],
    }
    if located != expected:
        fail(f"Located {located}")
    accept_xml = stored[names["待验收"]].decode("utf-8")
    accept_rows = row_map(accept_xml)
    headers = accept_rows[2]
    rows = {}
    for code, places in expected.items():
        number = places[0][1]
        row = accept_rows[number]
        cells = {}
        for index in range(1, 145):
            column = col_letter(index)
            value = cell_value(row, column, number, shared)
            if value not in (None, ""):
                cells[column] = {"header": cell_value(headers, column, 2, shared), "value": value}
        if cells["N"]["value"] != SHORT_N[code]:
            fail(f"{code} live N changed")
        current = {column: item["value"] for column, item in cells.items()}
        rows[code] = {
            "sheet": "待验收",
            "row": number,
            "cells": cells,
            "originalNODV": {"N": current["N"], "O": current["O"], "DV": current["DV"]},
            "restore": {
                "N": restored[code]["N"],
                "O": restored[code]["O"],
                "DV": current["DV"] + "\n" + APPEND,
            },
        }
    detail_rows = []
    detail_xml = stored[names["资源状态明细"]].decode("utf-8")
    for number, row in row_map(detail_xml).items():
        code = cell_value(row, "A", number, shared)
        if code not in ORDER:
            continue
        detail_cells = {}
        for index in range(1, 40):
            column = col_letter(index)
            value = cell_value(row, column, number, shared)
            if value not in (None, ""):
                detail_cells[column] = value
        detail_rows.append({"row": number, "id": code, "cells": detail_cells})
    if [(item["row"], item["id"], item["cells"].get("B")) for item in detail_rows] != [(4, "water_deep_pressure", "2"), (5, "water_deep_pressure", "3")]:
        fail(f"Details {detail_rows}")
    document = {
        "writtenBeforeWorkbookEdit": True,
        "capturedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "sourceHash": source_hash,
        "sourceMtime": datetime.fromtimestamp(LIVE.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
        "sourceSize": len(payload),
        "approvedVersion": "旋流和气泡柱的完整需求来自待验收转换前完整需求快照的 before。深海完整需求优先来自运行故障-20261104待验收快照的 originalNODV。v3.1 采用保留。不用待制作原始档案覆盖。",
        "userModification": USER_QUOTE,
        "explicitChanges": [
            "旋流运行外观与已确认 v3.1 一致：干地水平弯曲内旋，组合连续漏斗螺旋加水下汇入。",
            "深海水域冲掉草、蕨、火把等水可冲毁附着物，保留地形、建筑、楼梯、半砖、容器和自然水。",
            "聚怪排除施法者本人及载着施法者的骑乘组，组合气泡提升同样不重新带入本人。其他玩家和友军规则不变。",
            "上一轮区块加载无重入修复保留。",
        ],
        "rows": rows,
        "relatedDetails": {"policy": "资源状态明细不复制、不改。", "rows": detail_rows},
    }
    OUT.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
    if hashlib.sha256(LIVE.read_bytes()).hexdigest().upper() != source_hash:
        fail("Workbook changed while writing snapshot")
    print("SNAPSHOT", OUT)


if __name__ == "__main__":
    main()
