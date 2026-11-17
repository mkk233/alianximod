# -*- coding: utf-8 -*-
"""Assemble a pending N/O conversion snapshot. Does not write the workbook or src."""
import hashlib
import json
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "术式批量制作" / "火系六式-20261029"
source = root / "docs" / "术式制作表.xlsx"
intake = json.loads((batch / "test-stage-input.json").read_text(encoding="utf-8"))
lang = json.loads((root / "src/main/resources/assets/xueximod/lang/zh_cn.json").read_text(encoding="utf-8"))
english = json.loads((root / "src/main/resources/assets/xueximod/lang/en_us.json").read_text(encoding="utf-8"))


def sha(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def visual_paragraph(note):
    marker = "[设计反馈 2026-10-29]"
    start = note.find(marker)
    if start < 0:
        raise SystemExit("missing 2026-10-29 feedback")
    rest = note[start:]
    end = rest.find("\n2026-10-29本轮执行结果")
    if end < 0:
        raise SystemExit("visual feedback is not followed by the execution result")
    return rest[:end]


def texture(archive_file, game_file, symbol, drawn_by):
    game = "src/main/resources/assets/xueximod/" + game_file
    return {
        "archiveFile": archive_file,
        "gameFile": game,
        "resourceId": "xueximod:" + game_file,
        "symbol": symbol,
        "drawnBy": drawn_by,
    }


profiles = {
    "fire_liaoyuan": {
        "approvedVersion": "燎原 v2。功能需求仍是现行 N 全文；这次批准的是视觉确认版，不改 N 里的数值和不改地形约束。",
        "archive": "docs/资源库/特效/火焰/燎原_v2/",
        "gameDir": "src/main/resources/assets/xueximod/textures/effect/liaoyuan_v2/",
        "textures": [
            texture("liaoyuan-spread-wave-v2-strip.png", "textures/effect/liaoyuan_v2/spread_wave.png", "FireVisualLayout.SPREAD_WAVE", "FireVisualClient"),
            texture("liaoyuan-ground-flame-v2-strip.png", "textures/effect/liaoyuan_v2/ground_flame.png", "FireVisualLayout.GROUND_FLAME", "FireVisualClient"),
            texture("liaoyuan-ground-embers-v2-strip.png", "textures/effect/liaoyuan_v2/ground_embers.png", "FireVisualLayout.GROUND_EMBERS", "FireVisualClient"),
        ],
    },
    "fire_liaoli": {
        "approvedVersion": "燎里 v6。功能需求仍是现行 N 全文；视觉确认版是背后八剑、身后起火、朝后双流和三道后移火环，透明片不新增攻击。",
        "archive": "docs/资源库/特效/火焰/燎里_v6/",
        "gameDir": "src/main/resources/assets/xueximod/textures/effect/liaoli_v6/",
        "textures": [
            texture("sword_unlit.png", "textures/effect/liaoli_v6/sword_unlit.png", "LiaoliV6Layout.SWORD_UNLIT", "LiaoliV6Renderer"),
            texture("sword_hot.png", "textures/effect/liaoli_v6/sword_hot.png", "LiaoliV6Layout.SWORD_HOT", "LiaoliV6Renderer"),
            texture("back_ignite.png", "textures/effect/liaoli_v6/back_ignite.png", "LiaoliV6Layout.BACK_IGNITE", "LiaoliV6Renderer"),
            texture("back_flow.png", "textures/effect/liaoli_v6/back_flow.png", "LiaoliV6Layout.BACK_FLOW", "LiaoliV6Renderer"),
            texture("thrust_backflow.png", "textures/effect/liaoli_v6/thrust_backflow.png", "LiaoliV6Layout.THRUST_STREAM", "LiaoliV6Renderer"),
            texture("rear_ring.png", "textures/effect/liaoli_v6/rear_ring.png", "LiaoliV6Layout.REAR_RING", "LiaoliV6Renderer"),
        ],
    },
    "fire_fentian": {
        "approvedVersion": "焚天 v2。功能需求仍是现行 N 全文；用户强调特效用在敌人身上，确认版按各目标实际计划段数播放。",
        "archive": "docs/资源库/特效/火焰/焚天_v2/",
        "gameDir": "src/main/resources/assets/xueximod/textures/effect/fentian_v2/",
        "textures": [
            texture("fentian-conflagration-v2-strip.png", "textures/effect/fentian_v2/conflagration.png", "FireVisualLayout.CONFLAGRATION", "FireVisualClient"),
        ],
    },
    "fire_heat_up": {
        "approvedVersion": "炉心三式 v1 中的升温。功能需求仍是现行 N 全文；确认的视觉只是普通炉心低密度暖光。",
        "archive": "docs/资源库/特效/火焰/炉心三式_v1/",
        "gameDir": "src/main/resources/assets/xueximod/textures/effect/furnace_v1/",
        "textures": [
            texture("heat_up-v1-strip.png", "textures/effect/furnace_v1/heat_up.png", "FireVisualLayout.HEAT_SHEET", "FireVisualClient"),
        ],
    },
    "fire_burn": {
        "approvedVersion": "炉心三式 v1 中的燃烧。功能需求仍是现行 N 全文；确认的视觉只是过载较强橙红反馈。",
        "archive": "docs/资源库/特效/火焰/炉心三式_v1/",
        "gameDir": "src/main/resources/assets/xueximod/textures/effect/furnace_v1/",
        "textures": [
            texture("overdrive-v1-strip.png", "textures/effect/furnace_v1/overdrive.png", "FireVisualLayout.OVERDRIVE_SHEET", "FireVisualClient"),
        ],
    },
    "fire_cool": {
        "approvedVersion": "炉心三式 v1 中的冷却。功能需求仍是现行 N 全文；确认的视觉只在成功主动 cool 时播放一次蒸汽。",
        "archive": "docs/资源库/特效/火焰/炉心三式_v1/",
        "gameDir": "src/main/resources/assets/xueximod/textures/effect/furnace_v1/",
        "textures": [
            texture("cool-v1-strip.png", "textures/effect/furnace_v1/cool.png", "FireVisualLayout.COOL_SHEET", "FireVisualClient"),
        ],
    },
}

constructors = {
    "fire_heat_up": "进入普通炉心。状态没变则不扣魔力",
    "fire_burn": "进入或升级超频炉心，并保留已有加成",
    "fire_cool": "主动冷却炉心，并放出一次蒸汽",
    "fire_liaoyuan": "铺开地表火层。首次踩中时烧掉该目标的全部火种",
    "fire_liaoli": "持剑向前刺击。命中时烧掉火种，并打一刀物理",
    "fire_fentian": "球内有火种的目标一次烧完，并按档数分段",
}

items = []
for item in intake["items"]:
    code = item["id"]
    profile = profiles[code]
    zh_key = f"technique.xueximod.{code}.desc"
    en_key = zh_key
    zh = lang[zh_key]
    en = english[en_key]
    if zh != constructors[code]:
        raise SystemExit(f"{code} language and constructor differ")
    proposed_o = "；".join(row["resourceId"] for row in profile["textures"])
    proposed_o += f"（游戏目录 {profile['gameDir']}；归档 {profile['archive']}）"
    items.append({
        "id": code,
        "name": item["name"],
        "sheet": item["sheet"],
        "row": item["row"],
        "mayMigrate": False,
        "candidateOnly": True,
        "n": item["n"],
        "o": item["o"],
        "dv": item["dv"],
        "approvedVersion": profile["approvedVersion"],
        "userVisualChange": visual_paragraph(item["dv"]),
        "language": {
            "zhKey": zh_key,
            "zh": zh,
            "enKey": en_key,
            "en": en,
            "constructorDescription": constructors[code],
            "zhMatchesConstructor": True,
            "drawnBy": "Technique.getDescription()，调用处包括 ShuManagementScreen、SkillAssemblyScreen、SpellAssemblyScreen、ImprintTreeView",
            "proposedNStatus": "语言有与构造描述相同的一句话",
        },
        "resources": {
            "archive": profile["archive"],
            "gameDir": profile["gameDir"],
            "resourceDetailRows": item["resourceDetails"],
            "textures": profile["textures"],
        },
        "conversion": {
            "status": "待核对候选",
            "nBefore": item["n"],
            "oBefore": item["o"],
            "nAfter": zh,
            "oAfter": proposed_o,
        },
    })

workbook_sha = sha(source)
document = {
    "purpose": "进入待验收前的 N/O 转换候选。不是迁移，不是通过结论，也不表示任何一项现在可以迁页。",
    "preparedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
    "mayMigrate": False,
    "testFormalReview": "0/2，尚未开始",
    "workbook": {
        "path": "docs/术式制作表.xlsx",
        "sha256": workbook_sha,
        "matchesIntake": workbook_sha == intake["workbook"]["sha256"],
        "intakeSha256": intake["workbook"]["sha256"],
        "modified": False,
    },
    "source": "docs/开发记录/术式批量制作/火系六式-20261029/test-stage-input.json",
    "items": items,
}
out = batch / "test-stage-transition-snapshot.json"
out.write_text(json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "workbookSha256": workbook_sha,
    "matchesIntake": document["workbook"]["matchesIntake"],
    "items": [
        {
            "id": item["id"],
            "row": item["row"],
            "nAfter": item["conversion"]["nAfter"],
            "oCount": len(item["resources"]["textures"]),
            "visualStarts": item["userVisualChange"][:24],
            "mayMigrate": item["mayMigrate"],
        }
        for item in items
    ],
}, ensure_ascii=False, indent=2))
