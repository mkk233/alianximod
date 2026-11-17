我先读这份测试阶段写回准备任务，只按它授权的范围做。这单只准备迁移对照，不改表、也不判定能否迁页。我先读流程第 2、8 节和现有报告、语言与资源引用。语言里的六句和术式构造描述一致，贴图路径也有直接引用。我按这些事实写转换候选，不改表。六式的待验收转换对照已备好，六项现在都不能迁。现行表哈希仍是 `CE0AF918E0A99DC586232E76B156465B12AEDF8A5C0047CAD8268BEF6A3D899D`，与上次测试输入一致。测试正式结果核对仍是 0/2，尚未开始。

完整现行 N/O/DV、批准版本、视觉确认原文、资源映射和拟转换前后值在 `test-stage-transition-snapshot.json`。简短说明在 `test-stage-transition-prep-report.md`。`记录.md` 已追加。

拟 N 与 `zh_cn.json` 的描述句、`ModTechniques` 构造描述逐字相同，界面绘制的就是这句。六项都有现成的一句话，没有记成待确定。

| 编号 | 拟 N | 拟 O |
| --- | --- | --- |
| fire_liaoyuan | 铺开地表火层。首次踩中时烧掉该目标的全部火种 | `liaoyuan_v2` 的火浪、地火、炭纹三张贴图 |
| fire_liaoli | 持剑向前刺击。命中时烧掉火种，并打一刀物理 | `liaoli_v6` 的六张透明贴图 |
| fire_fentian | 球内有火种的目标一次烧完，并按档数分段 | `fentian_v2/conflagration.png` |
| fire_heat_up | 进入普通炉心。状态没变则不扣魔力 | `furnace_v1/heat_up.png` |
| fire_burn | 进入或升级超频炉心，并保留已有加成 | `furnace_v1/overdrive.png` |
| fire_cool | 主动冷却炉心，并放出一次蒸汽 | `furnace_v1/cool.png` |

批准版本仍是燎原 v2、燎里 v6、焚天 v2、炉心三式 v1。路径分别由 `FireVisualLayout`、`LiaoliV6Layout` 给出，并由 `FireVisualClient`、`LiaoliV6Renderer` 绘制。六式在资源状态明细里没有行。

测试准备发现 09:08 `FireHurtMixinPlugin` 和 `FireHurtBoundary` 的包位置变了，旧的 04:24 编译输入盖不住它们。主会话还在核实外部改动是否已经停写，统一编译和功能测试都还没启动。事实在 `test-stage-prep-report.md`，这里不判断那次改动是否正确。

这些前后值只是待核对候选。准备完资料也不会自动迁页。本单停止写入。未改工作簿，未做迁移候选，未改 src、正式资源或语言，未跑 Gradle，未启动游戏。
