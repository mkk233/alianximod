继续当前 A 公共组任务。先完整读取 shared-contract.md、route-plan-pending.md、containers-report.md、entities-report.md（均在本任务目录）及已有调查报告。GPT-6-astra 已批准下列方案，你在 Cursor 用 Grok 4.7 High Fast 实施；DeepSeek权限不可用且用户已授权此fallback。用户限定你改代码并编译，主控审查，不打包不启动游戏。保留工作区已有未提交更改，任务基线在 before/。

本阶段与资格无关的机械接线现在开始，尚未收到无属性MAGIC/NONE是否允许聚能的用户答案。不得自行决定该分支，不要放临时throw或默认BASE冒充完成；可保留既有资格入口/行为，仅落实确定的SWORD排除，并在报告明确MAGIC/NONE仍待决定。不要因等待该答案停止已授权的其余接线。

独占生产文件范围：ReleaseInput、CastFrame、network中的SkillUsePacket/SkillChainPlan/SkillSegmentRunner/CastExecutionSession/CompiledReleaseStep及必要编译计划结构；聚能注册和装配校验的必要文件；docs/底座功能点.md。只读其他组所有消费者文件，禁止修改其文件。按需rg精确定位，别读整仓，别重复研究已定架构。

按每个实际成员在释放计划建立时冻结该链能量输入，显式参数贯穿计划、会话、所有延迟lambda、复制/一分为三/双重、接触触发、普通与reserved容器入口。不要执行时重新扫描装配，不依赖ThreadLocal给容器偷传值。缺省旧入口BASE兼容可以保留，但每个链入口都必须显式输入。普通实体通过已绑定CastFrame.releaseInput进入TechniqueHitSpec。几何energyScale仍为定义ownEnergy×该input.sizeMultiplier，CastFrame.volumeScale另乘enchant radius；duration只乘该input，保留缺月自身1.35，不把自身倍率或附魔倍率当聚能。剑术（含有元素属性的剑术）聚能链倍率恒1，基础尺寸和原动画窗保持；新装配不能把聚能放在剑技槽，已有旧链也要服务端排除。不要改ChainRole.ATTACK，也不要术式id分支。

覆盖B报告列出的SkillUsePacket、SkillChainPlan.fireDualSequence、CastExecutionSession、SkillSegmentRunner全部入口，C的ConditionalEffect.attempt四参以及预约路径。别把参数存到EnchantmentSnapshot或修改操作号/预扣退款。费用/单次伤害/冷却/速度/索敌范围/维护周期不扩大。

必须加有意义的定向测试，验证实际计划/路由构建对BASE2/4的冻结、独立成员、双重/三重/延迟/接触输入沿用、剑术排除并保留ownEnergy。不做只测试自己拼出来字符串的伪覆盖，不为跑测试大面积暴露无关API。消费者侧测试已经由B/C写好，不改他们测试。最终统一编译等主控通知所有修改完成，本阶段不要跑Gradle。

更新旧docs/底座功能点.md里“不影响持续时间”的过时说明，按新用户设计：尺寸与有限持续时间消费链输入、费用伤害速度不乘；无属性资格此时明确待确认，不写成最终采用。

完成后写 route-report.md：精确改动文件/新接口、每条入口如何显式传入、还待用户回答位置、测试未执行状态、真实越界阻塞。不执行git提交，不打包，不启动游戏。