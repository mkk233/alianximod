# -*- coding: utf-8 -*-
"""Assemble the delivery requirement snapshot. Does not touch the workbook."""
import hashlib
import json
from datetime import datetime
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029")
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
intake = json.loads((batch / "liaoli-hit-seed-intake.json").read_text(encoding="utf-8"))
pose = json.loads((batch / "liaoli-pose-transition-snapshot.json").read_text(encoding="utf-8"))
evidence = json.loads((batch / "liaoli-hit-seed-implement-evidence.json").read_text(encoding="utf-8"))
data = live.read_bytes()
stat = live.stat()
live_hash = hashlib.sha256(data).hexdigest().upper()
append_n = (
    "2026-10-30火苗外观已确认。用户原话：“确认，使用这版火苗”。"
    "正式图 docs/资源库/特效/火焰/火种常驻火苗_v1/fire_seed_flame.png，8帧各32×32，每帧100ms。"
    "上文“预览尚未确认”只保留为当时原文，以这次确认为准。不改伤害、消耗、魔力和冷却。"
)
append_o = (
    "火种火苗已确认。运行路径 assets/xueximod/textures/effect/fire_seed/fire_seed_flame.png，"
    "与归档 PNG 哈希相同。过程稿不是批准素材。"
)
append_dv = (
    "[设计反馈 2026-10-30] 用户确认火苗外观，原话“确认，使用这版火苗”。"
    "正式图 docs/资源库/特效/火焰/火种常驻火苗_v1/fire_seed_flame.png，"
    "运行副本 assets/xueximod/textures/effect/fire_seed/fire_seed_flame.png，SHA256 相同。"
    "源码作者已停写。尚未统一编译，不迁待验收，不是审核通过。"
    "本阶段额度已用完，返修结果未追加复审。"
    "详见 docs/开发记录/术式批量制作/火系六式-20261029/记录.md。"
)
snapshot = {
    "preparedOnly": True,
    "workbookWritten": False,
    "moved": False,
    "sheet": "制作中",
    "row": 6,
    "id": "fire_liaoli",
    "liveSha256": live_hash,
    "liveMtime": datetime.fromtimestamp(stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "liveBytes": len(data),
    "liveMatchesReturnSubmit": live_hash == "ECE5C0FAFAF440A70481463AC790F0E03BF83B33433AFF2A2495E78108EF78D3",
    "approvedOriginal": {
        "n": intake["restored"]["n"],
        "o": intake["restored"]["o"],
        "dv": intake["restored"]["dv"],
        "source": "liaoli-hit-seed-intake.json restored，即退回制作中时写入的最新已批准原文",
    },
    "appendedConfirmation": {"n": append_n, "o": append_o, "dv": append_dv},
    "n": intake["restored"]["n"] + "\n" + append_n,
    "o": intake["restored"]["o"] + "\n" + append_o,
    "dv": intake["restored"]["dv"] + "\n" + append_dv,
    "sharedFuel": "基础6点物理，另加0.36×S×(1+F)。S是命中当时共享余额。不改成0.2×S。",
    "approvedPose": pose["approvedPose"],
    "approvedFlame": {
        "quote": "确认，使用这版火苗",
        "choice": "所有带火种实体常驻显示（建议）",
        "archivePng": "docs/资源库/特效/火焰/火种常驻火苗_v1/fire_seed_flame.png",
        "sha256": "106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B",
        "frames": 8,
        "frameSize": "32x32",
        "atlas": "32x256",
        "frameMs": 100,
    },
    "userChanges": intake["userChanges"],
    "changedFiles": evidence["sourcesAfter"],
    "resourceMapping": [
        {"use": "未点燃剑", "path": "assets/xueximod/textures/effect/liaoli_v6/sword_unlit.png", "sha256": "2FB695A061875B6C88C102716F36E5B3791288F1EF9734E5EC7D847EFF7119B8"},
        {"use": "点燃剑", "path": "assets/xueximod/textures/effect/liaoli_v6/sword_hot.png", "sha256": "155FA83DFA7FD54F4F7040C3A2684F0DF622E17BE6DB76BB74FD328BAEAA42D6"},
        {"use": "背后点燃", "path": "assets/xueximod/textures/effect/liaoli_v6/back_ignite.png", "sha256": "A83D034983C36F4EEB1B492CB6B269937BBCBFFD0DEB4FB0729E1E031E250CB2"},
        {"use": "背后火流", "path": "assets/xueximod/textures/effect/liaoli_v6/back_flow.png", "sha256": "254E8F1F63BE6F13017AC192FD4AF496B3ADB960133E55F65535A802B95FA6E0"},
        {"use": "刺击回流", "path": "assets/xueximod/textures/effect/liaoli_v6/thrust_backflow.png", "sha256": "2D57C54F8D99AC3E451B35A70009D99B2885B924242349A6C1B6BFE120BD329F"},
        {"use": "后环", "path": "assets/xueximod/textures/effect/liaoli_v6/rear_ring.png", "sha256": "857C37C052D082548A93B0E00868375C973D33B2CDB2C0B477C46C384A1A0CDE"},
        {"use": "火种火苗", "path": "assets/xueximod/textures/effect/fire_seed/fire_seed_flame.png", "sha256": "106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B"},
        {"use": "剑阵姿态", "path": "docs/资源库/特效/火焰/燎里_剑阵姿态_v7/liaoli-dive-leg-array-forward.bbmodel", "sha256": None, "note": "未改，不进游戏贴图"},
    ],
    "proposedDisplay": {
        "n": "持剑向前刺击。命中时花掉当时剩余的火种，并打一刀物理",
        "o": [
            "assets/xueximod/textures/effect/liaoli_v6/sword_unlit.png",
            "assets/xueximod/textures/effect/liaoli_v6/sword_hot.png",
            "assets/xueximod/textures/effect/liaoli_v6/back_ignite.png",
            "assets/xueximod/textures/effect/liaoli_v6/back_flow.png",
            "assets/xueximod/textures/effect/liaoli_v6/thrust_backflow.png",
            "assets/xueximod/textures/effect/liaoli_v6/rear_ring.png",
            "assets/xueximod/textures/effect/fire_seed/fire_seed_flame.png",
            "docs/资源库/特效/火焰/燎里_剑阵姿态_v7/liaoli-dive-leg-array-forward.bbmodel",
        ],
        "reason": "本次未改语言资源。沿用既有游戏内一句。源码作者的多句拟显示N不采用。",
    },
}
out = batch / "liaoli-hit-seed-delivery-snapshot.json"
out.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "liveSha256": live_hash,
    "liveMatches": snapshot["liveMatchesReturnSubmit"],
    "nStarts": snapshot["n"][:12],
    "hasFormula": "0.36×S×(1+F)" in snapshot["n"],
    "hasConfirmation": "确认，使用这版火苗" in snapshot["n"],
    "sourceCount": len(evidence["sourcesAfter"]),
}, ensure_ascii=False))
