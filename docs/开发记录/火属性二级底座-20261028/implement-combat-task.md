# 作者C：火伤消费与燃烧调度

完整读取 implementation-contract.md 和主设计后按C归属实现。C唯一写 release/fire/**、同包tests、新增fire/steam伤害类型和标签。现有术式定义/表格/无敌帧均不能改。

实现A快照与B炉心接口上的统一FireDamage、立即消费引爆和多段燃烧计划服务、NBT暂停恢复调度与冲突预检、SteamBurst.release(Player)。提供显式元素来源上下文/tag分类给炉心火伤与水冰承伤；旧playerAttack不能按id猜属性。分量与同层恰好一次，蒸汽固定基础2排除攻击者增幅。

默认按同效果键+同施法者拒绝重复，并支持目标全局同键排他；快照值固定、新火种不续段；卸载保存剩余段/下一段tick，加载不补离线、不重新消费；每段失败也只消耗一段；目标死亡清除；owner缺席使用保存UUID和伤害快照，不错误归属。无原版着火、蔓延、方块/掉落物修改。

4tick伤害必须查本地Minecraft/Forge确认 DamageTypeTags.BYPASSES_COOLDOWN 或等价定向机制；新增专用damage type入该唯一必要tag，勿仿水牢四类绕过防御tag，不能清invulnerableTime。有伤害免疫按正常规则，不能把hurt false当成功重复补打。

Steam友敌过滤不能依赖目前恒false的Relations.ally，至少自我/同队/驯服主人与队友排除并复用可用关系策略；碰撞和抗性正常，不能承诺所有目标3格。只服务不做冷却术式。

分段计划应通用，未来三式配方仅可出现在纯检查输入中，不硬编码到运行模块。校验计划有限、数值有效、NBT长度/段数有合理安全上限并在报告注明，防巨大/损坏NBT拖垮server。消费前计划拒绝无副作用。不编译Gradle，不独立正式复审。最终交文件/API/限制/测试说明。
