# 第二步：链结构与当次执行数据，只读定位

你是Cursor CLI --model grok-4.7-high-fast执行者，不再委托任何代理。本阶段仅采集事实，不做正式正确性复审，不写文件/实现/编译/运行游戏/测试/改表/提交。

用户授权随后实施“可缓存计划”与“当次释放数据”拆分。本次先只读定位，给唯一gpt-6-astra设计裁决使用。保留全部既有未提交改动与上一步性能插桩。不要重新通读整仓或逐行复述玩法。

只读范围：D:/alianximod/src/main/java/com/bslx/XueXiMod 下的 network/SkillUsePacket.java、CompiledReleaseStep.java、CastExecutionSession.java、SkillChainPlan.java、SkillSegmentRunner.java，以及 shu/technique/chain/TechniqueChain.java、ChainSteps.java（若位于其他子目录按精确文件名唯一定位）、shu/technique/release/ReleaseBatch.java、CastFrame.java。允许按直接调用符号读取确实必要的小接口，禁止展开所有effect/entity实现。

精确问题：
1. 每次释放重复解析/分类的入口、产物及消费路径（普通/双重/动作接触/条件释放/纯附魔/持续重复等）。是否已有CompiledReleaseStep等结构能复用，哪些字段混入动态对象？
2. 每个可缓存候选字段的依赖：仅术式ID与顺序/角色/静态定义，还是玩家属性、手持ItemStack/NBT、魔力水势/领域、随机、位置目标、当前tick、账本/冷却等？列方法/字段，不复述所有业务。
3. 现在动态值在“按下、实际段执行、接触/蓄力后、命中”中的哪个时点读取？哪些延迟闭包持有武器/施法者/列表，不能被跨次共享或提前快照？
4. 一个不可变结构计划+当次对象接入所需的最小接口边界、现有代码事实和各路径接入候选位置。可建议候选，不做正式设计裁决。
5. 上一步CHAIN_SPLIT/CHAIN_ROLE_SCAN等插桩在哪些真实解析点，缓存后应怎样保持计时口径清楚的事实。

工具读取限于具体方法/字段/调用片段，尽量合并定向rg，避免打印几千行整类。最终报告尽量3500字内，包含准确绝对路径/行号、静态/动态依赖清单、入口覆盖、未确定事实和可划分文件所有权。不要产生大段代码或全仓清单。结尾明确只读无改动。git如需使用，单次 -c safe.directory=D:/alianximod。
