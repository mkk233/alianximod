# 最后一轮实现材料 仅核对吸收提交返修

Cursor CLI --model grok-4.7-high-fast，继续同一只读review会话。所有作者已最终反馈并收束。禁止写文件、编译、测试、游戏和另派审查。设计2/2已用完；本单是实现第3/3轮所需最终证据，之后唯一gpt-6-astra作本轮裁决。不重审设计，不重新通读已检查领域主体。

读本目录review-decision-2.md、contact-repair-absorption-final.md，只对contact-evidence/repair-absorption/before与当前4文件的变化及必要调用检查：TechniqueHits、DomainContactBilling、LivingDomainDamageCommitMixin、PlayerDomainDamageCommitMixin。Forge字节码依据在repair-absorption/bytecode-absorption.txt，可核对必要方法，不重新读全套源码。

核对原唯一P2：两类actuallyHurt的前置吸收扣除在异常时是否有可靠提交证据。确认与该次setter对应的真实前后值及完成信号，零/等值/增加不算减少；帧、目标、来源和嵌套归属保持。异常时生命提交或吸收减少任一成立只提交一次，否则退款一次。正常返回继续hurtConfirmed布尔（最终零伤退款），原异常继续传播，无成功载荷。原已确认setHealth后信号、动态分派/调用次数及无跟踪行为不回退。

只列本次差异有证据的实际问题，不凭未知子类覆盖或任意第三方Mod设想扩充新问题。既有明确接受的正常零伤、scope行为与其他方案不重议。同步说明静态注入点/注册兼容是否因本次差异受影响，运行时未知不冒充已验证。

最终报告控制约800–1200中文字：是否原P2已修复的证据、若有剩余P1/P2给路径行号及场景、正常路径是否保持、未执行验证。不写长篇旧事实，不运行检查脚本或编译。
