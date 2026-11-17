# 性能记录：释放链只读定位

模型固定 Cursor CLI --model grok-4.7-high-fast。当前是只读信息采集，不是正式正确性复审。你就是执行 Cursor，不再委托任何代理。
用户已授权只开始性能记录；不实施缓存、查询复用、C/JNI、异步或玩法优化。
只允许读取 D:/alianximod/src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java、shu/technique/chain、shu/technique/release（不包括 entity、hitbox、motion 子目录）及确实关联的 effects/LockTargetService.java；上述相对目录均在 D:/alianximod/src/main/java/com/bslx/XueXiMod 下。可读取 AGENTS.md 和 docs/底座功能点.md。
目标：找出链解析、释放、目标查询、几何/轨迹生成、延迟汇总伤害的可插桩位置，给主会话与唯一架构裁决代理使用。只能读指定代码与必要直接接口，不能整仓通读。
输出：真实路径与行号、方法名、同步/线程和服务端边界、嵌套/重入计时关系、现有 profiler 或日志设施、最小改动候选清单。明确哪些计时含子项、哪些无法分离，避免把总时长相加。注意附魔回调、延迟 Tally 与多重释放的不同生命周期。
禁止任何文件写入、实现、编译、启动游戏、打包、提交和制作表操作。仓库有大量用户未提交改动，不能 reset/clean/checkout。git 需要时用 git -c safe.directory=D:/alianximod，不改全局配置。
只返回最终定位报告（建议不超过 6500 汉字），不要先给建议让用户确认，不产生独立复审结论。报告写明没有改动。
