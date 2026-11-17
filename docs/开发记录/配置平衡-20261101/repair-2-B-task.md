固定Cursor CLI --model grok-4.7-high-fast，原作者B执行review-2问题2、3。完整读review-2-final.md。设计2/2已用尽，实现复核2/3；本单只实现明确修复，不新设计/正式复审，不编译/测试/游戏/改表/提交。
仍独占原B路径，本轮重点shu/technique/sword/SwordEntityMotion.java、shu/technique/release/domain/**中ContactEnchantState/DomainContactBilling及紧邻B接触状态消费者（若实际类在其他原B目录按原所有权）；排除A的DomainAuraResolver。不得写A的TechniqueHits/network/ReleaseBatch/entity，C不写。
A将同时提供已固定接口，不读取A中间代码：
TechniqueHits.classifyHit、classifyRoot、classifyConfirmed：参数/重载分别与旧hurt、hurtRoot、hurtConfirmed对应；返回类型与现有classifyRespecting相同，具有continues()/grantsSeeds()。有限恰好0且权限成立返回ESTABLISHED_NO_DAMAGE；非法负数/非有限/权限失败无效。旧hurt*布尔只表示正伤接受(grantsSeeds)，0返回false。B必须按typed裁决消费，不能仍用bool判断0接续。
- SwordEntityMotion：合法0保留命中占位与原载荷/接续语义；FireSeedGrant.deposit等正伤限定副作用只跟grantsSeeds，不跟continues。正伤路径与原主人的伤害归属保持，读档不追乘。不要扩改原本就存在且与这次配置无关的载荷顺序。
- ContactEnchantState.open、领域collect/eligible等不能再把已确认原始资格的有限0金额当作无接触；不得放行负数/非有限/权限不成立或原始未获资格的无效接触。使用现有资格/快照来源，不把当前伤害0当结构判定。
- scan以及DomainContactBilling.apply/confirmTracked等全链路用continues决定接触成立、下发状态/载荷、正常周期和费用收据；只用grantsSeeds产种或记正伤。领域合法0不能先扣再因为“没有正伤”错误退款，也不能每tick重复收费；权限失败仍按原拒绝/退款语义。
- 原始资格由上游ContactCast.rawDamageOf已验证；若某入口还需要资格证据，用已有快照/来源接口明确核对，不自行把所有空快照也放开。缺少跨作者接口才最终报出，不跨写。
保存本次修前字节pre-repair-2/B/，最初pre-B与前次备份保留，更新author-B-manifest.json。必要命名门禁不扩修OriginalHumanoid。最终交回各路径typed裁决使用、接触0收费/状态/产种区分、实际改动与未完成/阻塞，待统一编译，全部写入释放。
