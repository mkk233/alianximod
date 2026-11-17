# 补齐作者已报告的实际扣血确认窗口

Cursor CLI --model grok-4.7-high-fast，仍由原contact作者唯一实施。读你已结束的contact-repair-1-final.md及review-decision-1.md。延续同一个P2返修，设计2/2、实现1/3均不变，不进行新设计评审或新复审。前次报告明确未完全解决，不能把部分修补当完成。

你报告的精确缺口：本地 Forge1.20.1-47.3.33 的 LivingEntity.actuallyHurt 与 Player.actuallyHurt 在setHealth返回后、ENTITY_DAMAGE发出前仍有可能抛出的语句，较晚VanillaGameEvent不能作为全部扣血确认。按你的已报告必要路径，授权最小Mixin确认钩子并注册，取得实际扣血完成的信号，消除这个窗口。不要以更晚的公开事件或一个净health差值替代扣血完成证据。保留原异常传播、异常不发成功载荷、正常路径语义及原来源收据。

本次唯一作者允许写入的精确文件：
- src/main/java/com/bslx/XueXiMod/shu/technique/release/TechniqueHits.java
- src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainContactBilling.java
- src/main/java/com/bslx/XueXiMod/mixin/LivingDomainDamageCommitMixin.java（新文件，作用于作者报告的LivingEntity.actuallyHurt所需位置）
- src/main/java/com/bslx/XueXiMod/mixin/PlayerDomainDamageCommitMixin.java（新文件，作用于作者报告的Player.actuallyHurt所需位置）
- src/main/resources/xueximod.mixins.json（仅追加必要注册，保留其它既有改动）
- docs/开发记录/领域附魔底座实施-20261029/contact-evidence/repair-commit/（返修前快照、当地Forge源/字节码静态依据、文件清单）

全部相对路径基于D:/alianximod。若这两个新文件意外已经存在，先报告不要覆盖他人内容。GameEvent监听若被更准确钩子替代，可清理你上一次新增的无用路径，勿保留能误判的第二套确认。不修改其他Mixin、TideMarkDamageScope、MagicPool、ManaPaymentReceipt或全局伤害公式。其他源码作者都已收束，不存在并行文件作者；本单注册文件由你独占。

把实际Forge类中注入点的可用签名、setHealth前后顺序和方法owner核实清楚，保留最小证据。钩子只为已激活的本次跟踪伤害范围记录，匹配本次目标与来源，正确处理嵌套和finally清理，不污染不使用新入口的普通hurt/hurtConfirmed调用者。不要跳过/再执行原hurt或setHealth，不修改无敌帧，不补血回滚，不在异常时发载荷。

先保存返修前快照，不覆盖原始before和repair-1。无Gradle/编译授权、不启动游戏、不新增或运行测试。name-gate按原规则只处理此次改动文件；必要静态核对不是新复审。

最终完整反馈：是否已经解决该P2（如仍存在真实未解决窗口必须说清）；未生效/生效后异常的费用结果、真实确认点及来源匹配、原异常传播；改动和注册清单；静态证据；未运行验证；所有文件最后写入时间；写入已收束。不要把后续游戏未验证说成已验证。
