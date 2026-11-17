# 法杖捕获链门：设计第2轮最小事实补查

固定 Cursor CLI `--model grok-4.7-high-fast`，纯只读 ask，不改任何文件、不复审全合同、不编译、不spawn、不操作MCP美术。A/B源码正在各自实现，不读其进行中内容。本查询只查C现有入口事实，供唯一设计者同一设计2/2轮定稿；不是新审查轮。

你此前implementation-contract第9节发现MAGIC水幻形+SWORD断潮被装配门拦截。设计者拟裁决：仅法杖载体接受，MAGIC宿主仍法术；SWORD只在完整捕获计划的已消费载荷下标获局部类型例外，普通剑技槽和同步跳过MAGIC不变。A/B接口不变。请最小补查以下真实签名和控制流（不自行裁决设计）：

1. StaffSkills.asSkill/castRefusal及法杖存取是否更早丢弃或拒绝SWORD，完整方法签名和具体文件行号。
2. SpellAssemblyMenu.rejectReason/mayPlace、SkillAssemblyPacket.put能否取得目标索引及插入/替换后的完整候选链，如何组成候选、原胚数据在何处；删除宿主或移动顺序时哪里同步、存储/编辑拒绝会否静默丢槽。
3. 法杖GUI可选列表是否还有前置type过滤；有则只报告明确路径/条件及可用上下文，没找到写没找到，不推断不存在。

不查伤害、生命周期、体积，不改类型/装配门、不写doc。最终一次性给可用事实、行号、必要短代码、只读停止声明。不要长篇历史复述。
