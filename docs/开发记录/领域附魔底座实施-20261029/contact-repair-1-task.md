# 实现第1轮裁决后的唯一异常结算返修

Cursor CLI --model grok-4.7-high-fast，继续原contact作者会话。按 D:/alianximod/docs/开发记录/领域附魔底座实施-20261029/review-decision-1.md 的唯一P2返修；先完整读该已结束裁决。设计仍2/2已用完，不再重审设计。实现已用1/3；你是返修作者，不自行开启复审，不改其他已核对路径。

用户授权价格仍每种基础附魔每次1点，多种相加。其他玩法不变。当前问题已在你的报告和审查中确认：confirmDamage扣血后抛出，赋值未完成，finally退款；禁止将所有异常直接视为未造成伤害，也禁止改成所有异常收费。

允许修改：
- D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainContactBilling.java
- D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/TechniqueHits.java（仅必要的确认结果辅助接口，其他调用者行为保持）
- D:/alianximod/docs/开发记录/领域附魔底座实施-20261029/contact-evidence/repair-1/（本次返修前快照、证据、文件清单）

其余生产代码只读，尤其MagicPool/ManaPaymentReceipt不改。若确须修改 TideMarkDamageScope 或现有Minecraft伤害确认Mixin才能在异常时得到真实已发生伤害结果，先只读定位并最终报告精确必要路径、当前钩子和原因，等待主会话明确追加文件所有权后才改那些文件。不得默认越界；这些文件未授权前，可完成范围内互不依赖部分但不能交付虚假的已解决。

目标保持既有支付契约：
1. 正常确认正伤（含致死）提交一次；取消/无敌帧/零伤/明确未生效退一次，不发载荷。
2. 异常退出时结算层仍保留并返回/提供本次已发生伤害的可靠确认，据此决定费用。正值LivingDamageEvent本身不是扣血已经完成的证据；简单仅比较前后health会漏absorption及同步回调，不以猜测代替确认。依据实际已有伤害机制作最小补充。
3. 不直接补血伪造回滚，不吞异常，不为异常路径发成功载荷；资金仍独立收据/原来源，不改全局退款规则。
4. 保持其它所有原本正常路径、非领域宿主、排序与领域生命周期。

对本次准备改的文件先留下返修前原始快照；不要覆盖初始before基线。Java改动按要求运行name-gate，只修本次文件问题。无gradlew/编译授权，不新增或运行独立测试、不运行游戏。必要定向静态检查可以做；若可控验证需要编译或游戏，报告未执行，不越过授权。裁决希望验证异常前后分支，不等于授予运行编译或游戏。

最终报告：是否完全修复或需要扩展的精确路径；已生效/未生效异常的确认依据、资金结果、异常传播；精确改动清单及末次写入时间；name-gate；未运行项；写入是否收束。不宣称已经新增复审通过，不重新改设计。
