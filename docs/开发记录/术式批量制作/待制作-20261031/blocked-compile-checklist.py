# -*- coding: utf-8 -*-
import json
from pathlib import Path

HERE = Path(r"D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031")
intake = json.loads((HERE / "intake.json").read_text(encoding="utf-8"))
rows = {item["id"]: item for item in intake["pending"]}
ORDER = [
    "fire_flame_trail",
    "fire_meteorite",
    "fire_shock_ring",
    "rule_surface_anchor",
    "fire_upwell",
    "rule_stagger",
    "rule_hit_continue",
    "fire_seed_sow",
    "fire_kindle",
    "fire_detonate",
    "fire_nether_gloom",
    "earth_break_ground",
    "fire_spark",
]
PREP = {
    "fire_flame_trail": "创造栏或给予测试法杖。装配一个会移动的攻击载体（火苗、火球或陨岩），再在同一链上装留焰。运行贴图为 assets/xueximod/textures/effect/flame_trail_v1/flame_trail.png。基础法杖仍应是 6 槽。",
    "fire_meteorite": "创造栏或给予测试法杖，并准备石镐。装配陨岩。运行方块贴图为 assets/xueximod/textures/block/meteorite_v1.png，不要用 64×1024 外观条带当六面。",
    "fire_shock_ring": "创造栏或给予测试法杖。装配热波。运行贴图为 assets/xueximod/textures/effect/shock_ring_v1/shock_ring.png。",
    "rule_surface_anchor": "创造栏或给予测试法杖。把地表定位放在热波、火苗或陨岩前面。运行贴图为 assets/xueximod/textures/effect/surface_anchor_v1/surface_anchor.png。",
    "fire_upwell": "创造栏或给予测试法杖。装配喷涌，后面接一个会移动的载体。喷涌没有独立新贴图，看载体自己的运动。",
    "rule_stagger": "创造栏或给予测试法杖。装配错时，后面接多份攻击。错时没有独立新贴图。",
    "rule_hit_continue": "创造栏或给予测试法杖。装配一个攻击父段，再接命中接续和一段子攻击。命中接续没有独立新贴图。",
    "fire_seed_sow": "创造栏或给予测试法杖。装配会命中的火攻击，并装上种火。运行贴图为 assets/xueximod/textures/effect/seed_sow_v1/seed_sow.png。",
    "fire_kindle": "创造栏或给予测试法杖。目标先有火种余额。装配引燃。运行贴图为 assets/xueximod/textures/effect/kindle_v1/kindle.png。",
    "fire_detonate": "创造栏或给予测试法杖。目标先有火种余额。装配引爆。运行贴图为 assets/xueximod/textures/effect/detonate_v1/detonate.png。",
    "fire_nether_gloom": "创造栏或给予测试法杖。装配带领域的宿主并装上幽冥。运行贴图为 assets/xueximod/textures/effect/nether_gloom_v1/nether_gloom.png。",
    "earth_break_ground": "创造栏或给予测试法杖。装配崩地，站在可破坏的普通方块上。运行贴图为 assets/xueximod/textures/effect/ground_dust_v1/ground_dust.png。没有坑形贴图。",
    "fire_spark": "创造栏或给予测试法杖。装配火苗。运行贴图为 assets/xueximod/textures/effect/fire_spark_v1/fire_spark.png。",
}
STEP = {
    "fire_flame_trail": "进入世界后释放带留焰的载体。让火径先擦到一个生物，再让载体本体碰到同一个生物。",
    "fire_meteorite": "进入世界后释放陨岩，看抛物线、落地中心和超时后的无伤残骸。再用石镐挖残骸方块。",
    "fire_shock_ring": "进入世界后释放热波，看环是否只判定一次，以及内外范围。",
    "rule_surface_anchor": "瞄准 32 格内的地面释放。再故意瞄准没有落点的天空，确认整组被拒绝。",
    "fire_upwell": "进入世界后释放喷涌。单份看竖直，多份看锥面。不要和垂落或风暴放在同一分支。",
    "rule_stagger": "进入世界后释放多份。看第 i 份是否在组开始后约 10i tick 才出生。",
    "rule_hit_continue": "进入世界后让父段打出免疫或正伤，看子段是否接上。再试取消或未打中，子段不应接上。",
    "fire_seed_sow": "进入世界后打中一个目标。看火种是否按基础 10 再乘产种倍率，并且同一次释放对同一目标只生产一次。",
    "fire_kindle": "进入世界后对有余额的目标挂上引燃。余额 100 时期限应为 140 tick，同时存在的计划最多 8 条。不要把档位 n 理解成 8。",
    "fire_detonate": "进入世界后打中有余额的目标。看是否按当时余额的 0.2×(1+当时热量) 结算，并且同一次只结算一次。",
    "fire_nether_gloom": "进入世界后打开领域。等满一个 20 tick 周期，看敌对目标是否得到基础 5 乘产种倍率。重叠时同一窗口只应发最高的一笔。看蓝染范围，并核对原版火焰粒子没有改色。",
    "earth_break_ground": "进入世界后释放崩地。看浅碗是否约半径 2、深度 2，并且不超过 32 块。容器、流体和受保护的块应跳过。",
    "fire_spark": "进入世界后释放火苗。看直线飞行、命中或撞块后消失，以及约 20 tick 的原版燃烧。命中应产基础 2 点火种。",
}
APPROVED = {
    "fire_flame_trail": "批准：魔力 2，冷却 5，复杂度 2，装配消耗 2，等级上限 20。寿命 40 tick，直径 0.5，视觉采样不超过 0.25 格，每载体最多 256 个表现点。链上角色不适用。本体和火径合计一次。",
    "fire_meteorite": "批准：魔力 48，冷却 40，复杂度 6，装配消耗 1，等级上限 20。物理 24，抛物线，速度 0.8，仰角 15 度，重力 0.08，有伤飞行 100 tick，碰撞 1 立方。硬度 3，抗爆 6，石镐及以上掉自身。",
    "fire_shock_ring": "批准：魔力 8，冷却 16，复杂度 2，装配消耗 1，等级上限 20。物理 4。无运动弹道。内半径 1.5，外半径 5，总高 3。无击退。视觉 12 tick，判定一次。",
    "rule_surface_anchor": "批准：魔力 0，冷却 0，复杂度 1，装配消耗 1，等级上限 20。释放种类是规则，链上角色是地表定位。视线 32 格。失败整组拒绝，不扣费，不冷却。",
    "fire_upwell": "批准：魔力 0，冷却 0，复杂度 2，装配消耗 1，等级上限 20。释放种类是规则，链上角色是喷涌改写，弹道是喷涌。初速 1，重力 0.08，锥面 25 度，运动最多 100 tick。",
    "rule_stagger": "批准：魔力 0，冷却 0，复杂度 2，装配消耗 1，等级上限 20。链上角色是错时。第 i 份在组开始后 10i tick 出生。",
    "rule_hit_continue": "批准：魔力 0，冷却 0，复杂度 3，装配消耗 1，等级上限 20。链上角色是命中接续。免疫成立可以接续，取消或不成立不接续。",
    "fire_seed_sow": "批准：魔力 2，冷却 5，复杂度 1，装配消耗 2，等级上限 20。基础产种 10，再乘炉心产种倍率后向下取整。",
    "fire_kindle": "批准：魔力 2，冷却 5，复杂度 2，装配消耗 2，等级上限 20。n=floor(余额/10)，余额 100 时 n=10、期限 140 tick。8 是同时存在的计划上限，不是档位上限。",
    "fire_detonate": "批准：魔力 2，冷却 5，复杂度 2，装配消耗 2，等级上限 20。火伤 0.2×当时余额×(1+当时热量)。",
    "fire_nether_gloom": "批准：魔力 2，冷却 5，复杂度 3，装配消耗 2，等级上限 20。每 20 tick 基础 5，乘领域主人当时产种倍率后取整。完整周期后首笔。同一窗口只发最高的一次。蓝火只是视觉。原版火焰粒子、燎系剑火、炉身、雾雨和重压水面不改色。超过 3072 片时这一发不染蓝。",
    "earth_break_ground": "批准：魔力 2，冷却 5，复杂度 3，装配消耗 2，等级上限 20。浅碗半径 2、深度 2、最多 32 块。只有土尘表现。",
    "fire_spark": "批准：魔力 2，冷却 5，复杂度 0，装配消耗 1，等级上限 20。火伤 1，直线，攻击，基础产种 2。速度 0.8，寿命 30 tick，碰撞 0.25 立方，原版燃烧 20 tick。",
}
lines = [
    "# 实测清单",
    "",
    "尚待编译。`compileJava` 没有执行。这不是编译通过，也不是编译器报错。现在不能做游戏验收。没有独立测试阶段。下面每项的用户反馈留空。",
    "",
    "预期里先保留领取时的完整功能描述、特效和备注，再附上后来批准的数值。不用一句话代替原文。",
    "",
]
for code in ORDER:
    item = rows[code]
    cells = item["cells"]
    name = cells["A"]["v"]
    lines.extend([
        "## " + name,
        "",
        "编号：" + code,
        "",
        "前置：" + PREP[code],
        "",
        "步骤：" + STEP[code],
        "",
        "预期：",
        "",
        "功能描述原文：",
        "",
        cells["N"]["v"],
        "",
        "特效原文：",
        "",
        cells.get("O", {}).get("v") or "（领取时没有特效格）",
        "",
        "备注原文：",
        "",
        cells.get("DV", {}).get("v") or "（领取时没有备注格）",
        "",
        APPROVED[code],
        "",
        "用户反馈：",
        "",
    ])
lines.extend([
    "## 测试法杖与基础法杖",
    "",
    "编号：test_staff（物品，不是术式行）；对照 basic_staff",
    "",
    "前置：创造栏取出，或执行 `/give @s xueximod:test_staff` 与 `/give @s xueximod:basic_staff`。模型文件是 `assets/xueximod/models/item/test_staff.json`。",
    "",
    "步骤：用测试法杖打开装配，放到第 32 格并重进世界再看这一格。再换基础法杖，确认槽位没有变成 32。",
    "",
    "预期：测试法杖 32 槽、备用魔力上限 1000、冷却缩短 50%、回魔总速度 16 倍（加成字段 15.0）。基础法杖保持 6 槽和原来的 0.20、0.10、100。空格不必填满。超长旧列表不能被静默删掉。",
    "",
    "用户反馈：",
    "",
])
(HERE / "实测清单.md").write_text("\n".join(lines), encoding="utf-8")
print("lines", len(lines))
