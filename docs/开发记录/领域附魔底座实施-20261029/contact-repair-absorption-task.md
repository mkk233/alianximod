# 实现第2轮裁决的吸收提交补齐

Cursor CLI --model grok-4.7-high-fast，继续原contact作者。完整读取本目录review-decision-2.md和review-repair-final.md，按唯一P2的具体接受条件返修。设计2/2、实现2/3，不另做设计审查或正式复审。用户费用仍每种基础附魔成功伤害1点，多种相加。

唯一可写源文件（均已由你独占）：
- D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/TechniqueHits.java
- D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainContactBilling.java
- D:/alianximod/src/main/java/com/bslx/XueXiMod/mixin/LivingDomainDamageCommitMixin.java
- D:/alianximod/src/main/java/com/bslx/XueXiMod/mixin/PlayerDomainDamageCommitMixin.java

证据仅写 docs/开发记录/领域附魔底座实施-20261029/contact-evidence/repair-absorption/。先存返修前快照，不覆盖旧证据。无需新增文件或注册，不改xueximod.mixins.json、MagicPool、ManaPaymentReceipt、TideMarkDamageScope、伤害公式或领域主体。已有改动保留。

已确认原P2剩余：LivingEntity/Player.actuallyHurt均在setHealth前真实扣吸收，之后onLivingDamage等可能抛出，原healthApplied=false导致已生效伤害退款。遵照裁决，把相关实际吸收写入的前后值及完成信号纳入本次受跟踪帧；只有值真实减少才标记。不要整次hurt两端粗比差值，不把等值setter、增加或未写入异常当已伤害，保持目标/来源/嵌套帧归属。

异常路径读取生命已提交或吸收已实际减少，任一成立提交一次，两者都成立仍一次；都未生效退款一次。正常返回继续原hurtConfirmed布尔语义，包括最终零伤退款，不因吸收标志改变正常路径。保留已有setHealth提交信号，原方法动态分派和调用次数不变。异常不发成功载荷、不补生命/吸收、不吞异常，RuntimeException/Error原样传播。

如确有必须扩展的路径，先返回具体原因而不越界；不重构全局事务。只补裁决这一项。

不编译、不运行测试、不新增测试、不启动游戏、不改制作表或提交。Java改动运行既定name-gate，只处理本次源文件命名，旧OriginalHumanoid诊断记录即可。静态核对四分支：吸收实际减少后异常、吸收未减少后异常、setHealth返回后异常、正常最终零伤；没有实际执行的验证如实标未执行。

最终报告尽量1000中文字以内：确认点/两个实体位置、四分支资金结果、改动文件与末次写入、基线位置、name-gate及未跑项，明确写入收束。不得声称新增复审通过。修完后待主会话统一编译，不再自行继续写。
