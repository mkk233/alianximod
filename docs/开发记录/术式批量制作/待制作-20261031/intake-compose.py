# -*- coding: utf-8 -*-
import json
from pathlib import Path

here = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
cells = json.loads((here / "intake-cells.json").read_text(encoding="utf-8"))
lists = json.loads((here / "intake-lists.json").read_text(encoding="utf-8"))
options = {key: value[1:] for key, value in lists.items()}
common = [
    "2026-10-29 入表报告写明当时全部在待制作、尚未领取、没有写入待制作原始数据。",
    "现行待制作原始数据仍只有燎原、燎里、焚天、升温、燃烧、冷却、灼地，不含这 13 个编号。",
    "本编号没有资源状态明细行。待制作行没有公式。",
    "src 下现行编号以及改名前的 fire_surface_anchor、fire_stagger、fire_hit_continue、fire_break_ground 字符串检索为 0 个文件。这只是路径检索，不是源码审查。",
    "资源库 README 没有这些编号。没有把火种常驻火苗当成其中任何一项的确认素材。",
    "没有这 13 项的设计 n/2 或实现 n/3 正式复审记录。火系六式旧批额度不套用，也不因本目录日期清零。",
]
history = {
    "fire_flame_trail": common + ["2026-10-29 留焰覆盖-结果：待制作第 3 行整行替换为留焰，旧编号 fire_flame_flow 不在现行状态页。"],
    "fire_meteorite": common + ["2026-10-30 批注-结果：基础冷却改为 40tick。编号未改。"],
    "fire_shock_ring": common + ["2026-10-30 热波-结果：显示名由震环改为热波，编号仍是 fire_shock_ring，基础伤害 4、冷却 16tick。"],
    "rule_surface_anchor": common + ["2026-10-30 编号-结果：由 fire_surface_anchor 改为 rule_surface_anchor，属性为无。"],
    "fire_upwell": common + ["入表后没有看到后续编号或冷却批注改过本行。"],
    "rule_stagger": common + ["2026-10-30 编号-结果：由 fire_stagger 改为 rule_stagger，属性为无。"],
    "rule_hit_continue": common + ["2026-10-30 编号-结果：由 fire_hit_continue 改为 rule_hit_continue，属性为无。"],
    "fire_seed_sow": common + ["2026-10-30 批注-结果：基础冷却改为 5tick。资源写在主行，不是明细页。"],
    "fire_kindle": common + ["主行 DV 仍写单段 2×(1+F) 是录入草案，待定稿。资源写在主行。"],
    "fire_detonate": common + ["主行 DV 仍写 0.2×S×(1+F) 是引爆草稿。资源写在主行。"],
    "fire_nether_gloom": common + ["主行 DV 写产种量和周期未写入数值格。"],
    "earth_break_ground": common + ["2026-10-30 编号-结果：由 fire_break_ground 改为 earth_break_ground，属性为土。"],
    "fire_spark": common + ["2026-10-30 火苗-结果：追加为待制作行，冷却 5tick，基础伤害 1，主行产种 2。"],
}
intake = {
    "stage": "信息采集",
    "countsAsReview": False,
    "sheetWritten": False,
    "workbook": {
        "path": "docs/术式制作表.xlsx",
        "sha256": cells["sha256"],
        "bytes": cells["bytes"],
        "mtime": "2026-10-31 01:55:51.895",
    },
    "statusCounts": cells["statusCounts"],
    "uniqueStatusIds": sum(cells["statusCounts"].values()),
    "duplicateStatusIds": cells["duplicateStatusIds"],
    "emptyIdAnomalies": cells["emptyIdAnomalies"],
    "statusIds": cells["statusIds"],
    "archive": {
        "count": cells["archiveCount"],
        "duplicateIds": cells["archiveDuplicateIds"],
        "ids": cells["archiveIds"],
        "pendingMissingFromArchive": [row["id"] for row in cells["pendingRows"]],
    },
    "headerComments": cells["headerComments"],
    "validationCounts": {name: len(rules) for name, rules in cells["validations"].items()},
    "dropdowns": {
        "弹道": options["A"],
        "链上角色": options["E"],
        "释放种类": options["I"],
        "释放标签": options["M"],
        "术式类型": options["Q"],
        "属性": options["U"],
        "外形": options["Y"],
        "是否": options["AC"],
        "领域边界": options["AG"],
        "领域外观": options["AK"],
        "曲线函数": options["AO"],
        "附魔效果键": options["AS"],
        "操作列表": options["BJ"],
        "作用对象列表": options["BL"],
        "触发时机列表": options["BN"],
        "寿命策略列表": options["BP"],
        "计量单位列表": options["BR"],
        "资源列表": options["BT"],
        "状态列表": options["BV"],
    },
    "pending": [
        {
            "row": row["row"],
            "id": row["id"],
            "cells": row["cells"],
            "formulaCount": sum(1 for cell in row["cells"].values() if cell.get("f")),
            "resourceDetailRows": [],
            "history": history[row["id"]],
        }
        for row in cells["pendingRows"]
    ],
    "resourceDetailIds": [],
    "concurrency": [
        "fire_liaoyuan 在制作中第 6 行。另一聊天正在处理旧项返修和离线编译，之后会写表。本单不领取。",
        "fire_heat_up、fire_burn 在制作中第 3、第 4 行。另一聊天正在做新版素材草稿。本单不领取。",
        "fire_scorch_field 在待验收第 5 行，并有资源明细第 7 行。另一聊天已完成迁移。本单不领取。",
    ],
}
for item in cells["detailRows"]:
    values = {column: cell.get("v") for column, cell in item["cells"].items()}
    intake["resourceDetailIds"].append({"row": item["row"], "id": values.get("A"), "index": values.get("B")})
(here / "intake.json").write_text(json.dumps(intake, ensure_ascii=False, indent=2), encoding="utf-8")
print(len(intake["pending"]), intake["workbook"]["sha256"])
