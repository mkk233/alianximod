# -*- coding: utf-8 -*-
import json
from pathlib import Path

here = Path(r"D:\alianximod\docs\开发记录\燎原伤害定稿入表-20261031")
path = here / "实施-交付快照.json"
before = json.loads((here / "实施-待验收-转换前.json").read_text(encoding="utf-8"))
snapshot = json.loads(path.read_text(encoding="utf-8"))
snapshot["acceptanceConversion"] = {
    "submitted": True,
    "from": before["from"],
    "to": {"sheet": "待验收", "row": 6},
    "beforeN": before["n"],
    "beforeO": before["o"],
    "afterN": "铺开半径10格的贴地火场，展开前沿即可命中。本体基础8点、每10刻一次并受一次炉心加成；有火种时足额段为4×(1+0.1n+F)，每段请求10点火种",
    "afterO": [
        "xueximod:textures/effect/liaoyuan_v3/ground_ash.png",
        "xueximod:textures/effect/liaoyuan_v3/ground_ash_b.png",
        "xueximod:textures/effect/liaoyuan_v3/ground_ash_c.png",
        "xueximod:textures/effect/liaoyuan_v3/ground_ash_d.png",
        "xueximod:textures/effect/liaoyuan_v3/ground_embers.png",
        "xueximod:textures/effect/liaoyuan_v2/spread_wave.png",
        "xueximod:textures/effect/liaoyuan_v2/ground_flame.png",
    ],
    "kept": before["kept"],
    "baselineSha256": before["baselineSha256"],
    "liveSha256": "AC4BA99781DC6174B89344FC2CB2174E67B4A2AF4C0515BB50C4BE6905435F8F",
    "mtime": "2026-10-31 02:51:37.376",
    "bytes": 184918,
    "counts": {"已完成": 40, "待制作": 13, "制作中": 3, "待验收": 4},
    "uniqueIds": 60,
    "compileExitCode": 0,
    "gameTested": False,
    "quota": "本阶段额度已用完，返修结果未追加复审",
}
path.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8")
print("mapped", snapshot["acceptanceConversion"]["to"])
