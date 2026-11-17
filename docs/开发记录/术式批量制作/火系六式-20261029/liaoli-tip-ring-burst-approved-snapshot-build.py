# -*- coding: utf-8 -*-
"""Supplement the approved damage and ring text. Does not touch the workbook."""
import json
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029")
intake = json.loads((batch / "liaoli-tip-ring-burst-intake.json").read_text(encoding="utf-8"))
append_n = (
    "2026-10-30用户已批准，原话：“采用建议数值，按迎面相对速度计算（最接近原版长矛）”。"
    "这取代上文尚未批准、只记录待确认的那一句。\n"
    "v是沿本次刺击方向的实际相对接近速度，单位格/秒，负值截为0，再封顶40。"
    "物理伤害为6+1.5×v。火种火伤为S×(1+F)，F沿用原炉心，不重复乘。"
    "不计其他领域和减伤时，S100且静止为106，v20为136，v40为166。"
    "一次消耗当时全部火种、刺击最长40tick、魔力40、冷却300tick，以及火种严格大于50才出环，都保留。\n"
    "火环每道从实际手持剑尖发出，向剑柄和玩家身后连续后移并单调扩大，明显超过背后全剑阵和火舌范围后淡出。"
    "每道完整走完，不能取模回到起点，也不能被另一道共用的截止剪断。"
    "已经触发的视觉尾迹可以收尾，不延长伤害命中体。仍用rear_ring、已批准v7姿态、六张贴图和常驻火苗。"
)
append_o = (
    "三道环各自从实际手持剑尖出发，连续后移并单调变大，超过背后剑阵和火舌后淡出，每道走完，不中途跳回。"
    "仍用rear_ring。迎面相对速度只改变已批准的伤害，不另做新贴图。"
)
snapshot = {
    "workbookWritten": False,
    "sheet": "制作中",
    "row": 6,
    "id": "fire_liaoli",
    "liveSha256": "410458A81EB7F37B5E1016072162BD31263532A92D34F2F52AB39F2574220F2C",
    "liveMtime": "2026-10-30 02:51:01.753",
    "liveBytes": 171136,
    "supersedesPending": True,
    "userQuote": "采用建议数值，按迎面相对速度计算（最接近原版长矛）",
    "priorSheetText": {"n": intake["restored"]["n"], "o": intake["restored"]["o"]},
    "n": intake["restored"]["n"] + "\n" + append_n,
    "o": intake["restored"]["o"] + "\n" + append_o,
    "formula": {
        "v": "沿本次刺击方向的实际相对接近速度，格/秒，负值截0，再封顶40",
        "physical": "6+1.5×v",
        "fire": "S×(1+F)，F为原炉心，不重复乘",
        "nominal": {"s100v0": 106, "s100v20": 136, "s100v40": 166},
        "excludes": "不计其他领域和减伤",
    },
}
(batch / "liaoli-tip-ring-burst-approved-snapshot.json").write_text(
    json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8"
)
print("ok")
