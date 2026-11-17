# 六式待验收转换候选

本单只准备对照。六项都还不能迁。测试正式结果核对仍是 0/2，尚未开始。工作簿未改，没有迁移候选。

现行表 SHA256 仍是 `CE0AF918E0A99DC586232E76B156465B12AEDF8A5C0047CAD8268BEF6A3D899D`，与 `test-stage-input.json` 一致。六式仍在制作中第 3–8 行。完整现行 N/O/DV、批准版本、视觉确认原文、归档与游戏资源映射、拟转换前后值在 `test-stage-transition-snapshot.json`。

拟 N 取 `zh_cn.json` 的 `technique.xueximod.<编号>.desc`。这六句与 `ModTechniques` 构造描述逐字相同，界面通过 `Technique.getDescription()` 绘制。没有缺句，所以没有记成待确定。

拟 O 只写当前代码正在引用的资源标识：

| 编号 | 拟 N | 拟 O |
| --- | --- | --- |
| fire_liaoyuan | 铺开地表火层。首次踩中时烧掉该目标的全部火种 | `liaoyuan_v2` 的 `spread_wave.png`、`ground_flame.png`、`ground_embers.png` |
| fire_liaoli | 持剑向前刺击。命中时烧掉火种，并打一刀物理 | `liaoli_v6` 的六张透明贴图 |
| fire_fentian | 球内有火种的目标一次烧完，并按档数分段 | `fentian_v2/conflagration.png` |
| fire_heat_up | 进入普通炉心。状态没变则不扣魔力 | `furnace_v1/heat_up.png` |
| fire_burn | 进入或升级超频炉心，并保留已有加成 | `furnace_v1/overdrive.png` |
| fire_cool | 主动冷却炉心，并放出一次蒸汽 | `furnace_v1/cool.png` |

资源标识都带 `xueximod:` 前缀和游戏目录、归档目录。燎原、焚天、炉心三式由 `FireVisualLayout` 给出路径，`FireVisualClient` 绘制。燎里由 `LiaoliV6Layout` 给出路径，`LiaoliV6Renderer` 绘制。六式在资源状态明细里没有行。

这些前后值只是待核对候选。测试结果和正式结果核对逐项决定之后，才另派写回。准备这份资料不表示任何一项可以迁入待验收。
