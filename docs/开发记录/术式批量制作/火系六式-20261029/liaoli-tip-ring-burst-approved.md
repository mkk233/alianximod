# 燎里伤害批准与实施所有权

只补记录。制作表未改，`fire_liaoli` 仍在制作中第 6 行。源表 SHA256 仍是 `410458A81EB7F37B5E1016072162BD31263532A92D34F2F52AB39F2574220F2C`，LastWriteTime 2026-10-30 02:51:01.753，171136 字节。不迁任何行。

2026-10-30 用户明确回答：“采用建议数值，按迎面相对速度计算（最接近原版长矛）”。这取代此前伤害和移速系数待确认的状态。表内那句待确认原文没有改写，补充正文在 `liaoli-tip-ring-burst-approved-snapshot.json`。

沿本次刺击方向的实际相对接近速度 v，单位是格/秒，负值截为 0，再封顶 40。物理伤害是 `6+1.5×v`。火种火伤是 `S×(1+F)`，F 沿用原炉心，不重复乘。不计其他领域和减伤时，100 火种且静止是 106，v 为 20 是 136，v 为 40 是 166。一次消耗、40 tick、魔力 40、冷却 300 tick，以及火种严格大于 50 才出环，都保留。

火环每道从实际手持剑尖发出，向剑柄和玩家身后连续后移并单调扩大，明显超过背后全剑阵和火舌后淡出。每道完整走完，不能取模回到起点，也不能被另一道共用的截止剪断。已经触发的视觉尾迹可以收尾，不延长伤害命中体。仍用 `rear_ring`、已批准 v7 姿态、六张贴图和常驻火苗。本次没有新像素素材。

源码唯一作者是 `23ef4660-da4b-47a8-9341-8ec236ec5cc3`，正在执行，源码停写前不得统一编译。可写范围以 `src/main/java/com/bslx/XueXiMod/` 为根：`visual/liaoli/LiaoliV6Layout.java`、`client/LiaoliV6Renderer.java`、`shu/technique/effects/LiaoliEffect.java`。需要时可以新增客户端火环尾迹或速度采样辅助类，并只在 `client/FireVisualClient.java` 注册和绘制该尾迹。`TechniqueHitEntity.java` 只补必要的视觉触发数据，不延长攻击窗口。`FireConsumerPlans.java` 只改本式计划。网络或事件类只许本式视觉尾迹的最小接线。该作者还写 `liaoli-tip-ring-burst-implement-report.md`、`liaoli-tip-ring-burst-implement-evidence.json` 和必要检查日志。

工作簿和记录由 `d6f84d0b-4ea1-4368-a576-8acc20cc2afd` 写。资源归档和索引归主会话。统一编译者 `b2d5b359-42e3-4a90-9d24-b26acec6d5f8` 尚未派发。

设计 2/2、实现 3/3 承接。本阶段额度已用完，返修结果未追加复审。没有独立测试。本单不看正在写的源码或实时日志，不等待源码作者。交付写回另单。

本单停止写入。未改制作表，未改 src，未跑 Gradle，未改资源库，未操作游戏，未提交。
