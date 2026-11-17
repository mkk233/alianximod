固定Cursor CLI --model grok-4.7-high-fast，原作者C返修。完整读review-1-final.md问题2以及问题1的跨作者依赖。设计2/2已用尽，实现复核1/3需返修；本单落实明确修复，不另行设计/正式复审，不编译/测试/游戏/改表/提交。
仍仅写原C所有权路径：shu/technique/enchant/**、shu/technique/EnchantmentDefinitions.java、status/fire/**、shu/technique/release/fire/**、shu/technique/release/firecast/**、mixin/PlayerAttackMixin.java（均相对src/main/java/com/bslx/XueXiMod/）。不写A/B文件。只修review明确问题和下述必要接口，不扩大玩法。
- 火伤先完整settle为已配置金额，再校验，所有燃料draw/消耗或不可逆提交必须在该金额可被后续接受后。超出账本既有10000上限可以按未就绪拒绝，但不能先扣火种再拒绝；不要移除无关原权限/燃料/目标合法性条件。
- 配置产生的合法0贡献须保留为已结算0贡献；SharedSpec/挂载、脉冲、FireDamage及必要NBT校验的上下游不能再把它当成非法金额丢掉。负数、非有限数、超限、无合法燃料/目标等仍按原合法性拒绝。保留炉心采样时点与furnaceSettled，一次倍率；旧存档不追乘。
- 对合法0火伤采用与“命中成立但没有正伤”一致的结果，不能为了0绕开原目标/权限条件，不能因此让无效原计划成立。留意所有真实引爆/引燃/燎原/焚天/燎里/灼地/蒸汽相关消费点。
- 为A修ContactCast结构门，在EnchantmentSnapshot提供 public static float rawDamageOf(Skill skill, List<Integer> indexes)，参数类型/顺序与已有damageOf相同，返回原未配置magicDamage逐下标合计，不读BalanceService、不改费用公式。已有damageOf继续返回已配置金额。A将用raw方法判资格，配置后的金额仍用于命中。不读A的中间修改。
保留最初pre-C，另存本次修前字节pre-repair-1/C/，更新author-C-manifest.json。必要命名门禁不扩修OriginalHumanoid。最终报告校验在扣种前、0贡献贯穿处理、上限拒绝/存档兼容和公共raw方法的实际签名、改动路径、未解决/阻塞，待统一编译，释放全部写入。
