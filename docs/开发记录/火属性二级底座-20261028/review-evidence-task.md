# 本批实现第1轮集中复审的只读取证
固定模型 grok-4.7-high-fast。只读，不写任何源码/文档，不运行Gradle、游戏或测试，不spawn。三个作者写入均已收束，另有独立编译者只编译不改源码。不要查实时日志。最终报告供唯一gpt-6-astra作本轮判断，此取证不是新增轮次。
范围仅本批：status/fire/seed、status/fire/furnace、shu/technique/release/fire、新Furnace mixin、ModMobEffects、ResourceStatuses、mixins.json、对应伤害JSON与tags/语言；可按具体调用有限读取现有注册入口/属性服务/关系策略及本地Forge 1.20.1映射源码。不要整仓审计，不重新讨论设计数值，不要求旧未显式元素标注术式自动接入。规格 docs/火属性二级玩法设计.md，契约 docs/开发记录/火属性二级底座-20261028/implementation-contract.md。只采真实源码证据，作者自报不能替代。
请集中报告每项路径+准确行号+最小代码片段+实际控制流，重点：
1 火种preview/consume/grant主线程验证、账本去重、校验与实际扣款；消费调度预检→planner→consume→挂载之间可重入/数据变化/挂载失败路径，是否校验伤害权限，是否会先扣后失败；planner纯函数契约与实现保护不要混淆。
2 ElementDamageScope ThreadLocal frame是否绑定目标和DamageSource/单次hurt，嵌套伤害会否继承外层火水冰或已结算标志；列出open/fire事件/strikeOnce/steam调用片段。一次加F是基于事件当前值还是明确基础B，是否同层、重复或吞掉其它修饰。
3 BurnCarrier/Ledger实际NBT编码解码：快照是否真正保存/恢复/校验；target UUID/caster/cast、段数、等待、重复/冲突key、容量限制；是否tick期间事件重入会覆盖新挂载或清除；死亡和clone/存活转化如何处理，异步是否拒绝。
4 炉心所有公共修改方法是否服务端主线程；hidden effect命令升级与生命周期同步，forceAddEffect事件是否会递归/重置运行态，牛奶处理、clone复制、死亡取消事件、移除事件与实际是否移除一致。Resistance redirect具体目标/require/remap与源码目标存在证据；ModMobEffects实际注册、mixin加载清单。
5 专用fire_resource bypasses_cooldown触发原版hurt是否刷新invulnerableTime=20且lastHurt，影响普通攻击的具体最小例子。给不扩大其它伤害绕过、不直接清零全局字段的最小定向修法建议：能否在hurt内只针对该专用伤害跳过写 invulnerableTime/lastHurt，其它防御保留；需指出混入准确位置与可行性，不实施。
6 damage tags replace是否false且未覆盖既有成员；damage_type message_id与zh/en缺失键准确列出；steam不继承外层元素作用域是否有隔离。
对明确问题列触发实例与严重性建议；对没发现问题给简短证据，不凭结构测试宣称运行集成通过。区分静态确定问题、接口限制、游戏验证项。报告控制在8000中文内，必要核心片段完整，最终反馈之后退出。禁止写任何文件。
