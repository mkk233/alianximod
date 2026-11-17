# -*- coding: utf-8 -*-
import json
from pathlib import Path

here = Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031")
read = json.loads((here / "实施-交付准备-读取.json").read_text(encoding="utf-8"))
if len(read["located"]) != 1:
    raise SystemExit(f"燎原位置不是唯一: {read['located']}")
found = read["located"][0]
cells = found["cells"]
snapshot = {
    "kind": "燎原返修交付准备快照",
    "sheetEdited": False,
    "compiled": False,
    "reviewed": False,
    "quota": "本阶段额度已用完，返修结果未追加复审",
    "designQuota": "2/2",
    "implementationQuota": "3/3",
    "quotaReset": False,
    "workbook": {
        "path": "docs/术式制作表.xlsx",
        "sha256": read["sha256"],
        "bytes": read["bytes"],
        "mtime": "2026-10-31 01:55:51.895",
        "counts": read["counts"],
        "uniqueIds": read["unique"],
        "statusIds": read["total"],
    },
    "location": {
        "sheet": found["sheet"],
        "row": found["row"],
        "id": cells["B"],
        "name": cells["A"],
        "making": read["making"],
        "pending": read["pending"],
    },
    "sheetOriginal": cells,
    "approvedDesign": {
        "unchanged": True,
        "note": "伤害、半径、接触和资源语义保持制作中当前N与数值列。本快照不回滚到初版档案，也不把作者实现差异改写成新设计。",
        "bodyBase": cells["E"],
        "bodyPeriod": cells["BV"],
        "fieldLife": cells["BW"],
        "radius": cells["AS"],
        "burnAmount": cells["EC"],
        "burnPeriod": cells["EG"],
        "burnFirstDelay": cells["EH"],
        "burnTrigger": cells["EB"],
        "cooldown": cells["F"],
    },
    "currentVisual": {
        "status": "已确认，旧过程视觉不是现行",
        "userReply": "可以，使用这版",
        "confirmedPreview": "docs/资源库/过程归档/燎原灰烬-20261031/mixed-surface-preview-v5.gif",
        "formalArchive": "docs/资源库/特效/火焰/燎原_v3/",
        "formalVersion": "v3",
        "processVersion": "v5",
        "history": [
            "过程初版底色含长裂缝。",
            "v2按用户要求加强火星，底纹仍沿用带裂缝的初版。",
            "v3去掉长裂缝。",
            "v4增加多层杂色，用户认为仍像砖块或大理石。",
            "v5改为草方块顶面那种16×16离散颗粒，四种底纹加共享透明余烬。用户确认采用这版。",
        ],
        "runtimeFiles": [
            "assets/xueximod/textures/effect/liaoyuan_v3/ground_ash.png",
            "assets/xueximod/textures/effect/liaoyuan_v3/ground_ash_b.png",
            "assets/xueximod/textures/effect/liaoyuan_v3/ground_ash_c.png",
            "assets/xueximod/textures/effect/liaoyuan_v3/ground_ash_d.png",
            "assets/xueximod/textures/effect/liaoyuan_v3/ground_embers.png",
        ],
        "keptV2": [
            "assets/xueximod/textures/effect/liaoyuan_v2/spread_wave.png",
            "assets/xueximod/textures/effect/liaoyuan_v2/ground_flame.png",
        ],
        "archiveHashes": {
            "ground_ash.png": "3A27AB77AB560BDAA10B91D1FA0BBEE9EEA2E7A2539D96B38EAF5D34E035A391",
            "ground_ash_b.png": "FDA27E3FE5AF8D2BF24F7C640F6FB029261ED1E60249C598B3D2E1BD92CD9650",
            "ground_ash_c.png": "E238680FE3196C8BDA7F23E18F3E864D6AA64EB9F82CB09182F29B8A308623A6",
            "ground_ash_d.png": "45DA398493CB87E0E5E5D74EA32272B6888A51ADEBC94FB9646467CF76C2E7FA",
            "ground_embers.png": "B8642145B0665A179A5C89C6A0CE0A270A5118254F2686E6BD16618A5B2C8F0F",
            "mixed-surface-preview.gif": "E60B25B40A129A574933A4636D2255B03DB4387F96B1B002AFA126283607D3A5",
        },
        "mapping": "待验收O尚未写。最终资源映射等源码作者接入贴图后的完整报告，当前不猜。",
    },
    "displayN": "待验收展示句尚未准备。等源码作者接入贴图的最终报告给出实际游戏内文案，当前不猜。",
    "authorFirstStage": {
        "session": "56a5db68-36cf-4f89-b30d-b07c214a5d68",
        "report": "docs/开发记录/燎原伤害定稿入表-20261031/实施-修复报告.md",
        "evidence": "docs/开发记录/燎原伤害定稿入表-20261031/实施-修复证据.json",
        "role": "只转记第一阶段最终反馈。不是新增正式复审。贴图接入正在进行，本快照不重读进行中的代码。",
        "knownLimits": [
            "作者报告：生成年龄0未铺开，最早年龄1才有有效接触。表内按年龄0接触写出的0、10至190，与该实现差一拍；作者称中心仍可打满20次，落在年龄1、11至191。",
            "作者报告：外围目标次数少于中心。",
            "作者报告：抗火等原版减伤和免疫仍然适用。",
            "作者报告：旧KnownLimitsCheck与FireVisualLayoutCheck仍期待半径6和本体2，未更新、未运行，不能当通过。",
            "作者报告：第一阶段没有把v3图复制进运行资源。当时代码只点了ground_ash.png和ground_embers.png。四种底纹的接入不在那份报告里。",
        ],
    },
    "prepared": [
        "制作中位置和源表哈希已重读。",
        "当前N/O/DV原文已收入本快照。",
        "已确认的v5灰烬归档、五张运行图哈希和保留的v2展开、附着火焰已登记。",
        "第一阶段作者反馈和已知限制已转记。",
    ],
    "missing": [
        "源码作者贴图接入的最终报告和完整资源映射。",
        "待验收用的实际游戏内展示句。",
        "本次编译。",
        "迁入待验收。",
        "用户游戏实测。",
        "追加正式复审。",
    ],
}
path = here / "实施-交付快照.json"
path.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8")
print(found["sheet"], found["row"], cells["B"], len(cells["N"]), len(cells["O"]), len(cells["DV"]))
