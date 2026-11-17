# 第二步：装配、定义与缓存失效来源，只读定位

固定Cursor CLI --model grok-4.7-high-fast，不委托代理。只读事实采集，不做正式复审；禁止写文件、实现、编译、运行游戏、测试、制作表或提交。

用户下一步要实施可缓存术式链计划。需要确定缓存键和失效来源，不能凭槽号或玩家ID猜。根为D:/alianximod/src/main/java/com/bslx/XueXiMod。

范围：item/SkillCarrier.java、SkillStorageData.java、StaffData.java、StaffSkills.java；network/SkillAssemblyPacket.java与装配相关直接写入入口；shu/TechniqueManager.java、Technique.java、ModTechniques.java（只登记/变更方法）；shu/technique/release/ReleaseRegistry.java、ReleaseDefinitions.java（只定义变更/访问口）、TechniqueEffectRegistry.java、EnchantmentRegistry.java；event/PlayerConnectionHandler.java、ModEvents.java中的登录登出/服务器生命周期。按精确文件名定位实际位置。可沿装配/NBT写入的直接调用读取必要方法，但不要展开UI绘制、所有术式定义或效果实现。

需报告：
- 实际装配数据从哪里读、返回可变/不可变列表还是全新副本；术式顺序、槽位类型、步进模式、技能属性等哪些输入改变结构解析。NBT中具体相关键及写入口，包括服务端装配、换武器/法杖、命令、复制物品、读取存档，不要笼统宣称所有NBT都必须缓存。
- Technique、ReleaseDefinition、附魔/效果登记对象是否可变；注册/替换/重载是否有现成generation/revision/lifecycle。没有热重载接口也如实说明，不为未来假想接口扩大任务。
- 是否适合按纯内容键共享不可变计划，还是需玩家/槽持有；只给事实与约束，最终裁决由gpt-6-astra做。缓存绝不能持有玩家/Level/Entity/可变ItemStack/NBT/本次账本。
- 服务器启动/停止、玩家退出、装配修改清理的真实可用事件位置和现有注册方式；当前已有缓存/compiled plan设施，避免重复发明。
- 若要观察命中/未命中，可只读debug/performance/PerfMetric.java、PerfCounter.java及导出遍历方式，报告最低接入事实，不改它们。

使用定向rg/方法片段，避免整类长打印。最终尽量3500字内，带真实绝对路径/行号与静态依赖/变更入口，明确未知点和候选文件清单。不要通读整仓、不要输出整份schema。保留所有未提交改动。git如需用单次 -c safe.directory=D:/alianximod。结尾明确无改动。
