固定Cursor CLI --model grok-4.7-high-fast，原作者A返修。完整读review-1-final.md问题1、架构裁决的0贡献/原始结构约定。设计2/2已用尽，实现复核已完成1/3（需返修）；本单实现修复不增加正式复核轮次。不另作架构/正式复审，不编译/测试/游戏/改表/提交。
唯一允许生产写入：network/ContactCast.java、shu/technique/release/TechniqueHits.java（相对src/main/java/com/bslx/XueXiMod/），及必要A拥有的紧邻公共接口若真需要则最终准确列出，不能写C的EnchantmentSnapshot或B文件。保留本批原始pre-A，另存本次修前字节到pre-repair-1/A/并更新author-A-manifest.json。
落实问题1：
- ContactCast.refusal及snapshot的资格检查使用原始附魔加数/原始资格，不使用已配置contactDamage<=0来取消原本成立的接触或领域绑定。金额仍使用配置后的值，0贡献保留，条数/类型/幽冥状态不被清除。
- 跨作者合同：C同时会在自己拥有的EnchantmentSnapshot中提供 public static float rawDamageOf(Skill skill, List<Integer> indexes)，签名参数与已有damageOf相同，返回原公式未配置伤害合计，不读BalanceService。A使用这个方法作结构资格判断；已有damageOf仍返回已配置金额，不能换成raw作实际伤害。不自行修改C文件或读C的中间实现。
- TechniqueHits.hurt/hurtRoot对合法的已配置0贡献，与classifyRespecting采用同一种“成立但无正伤”的处理，不在进伤害前当失败；不能因此把非法目标、受保护目标或原权限拒绝误记成功。复用现有判定语义，原版正伤路径/无敌帧等行为保持。
仅修本轮明确问题；不得修未改OriginalHumanoid。完成后必要命名门禁，记录未跑编译/游戏，返回修改文件行号、0贡献状态流、是否有阻塞，并明确释放写入。只在最终报告反馈，不读B/C运行中的源码。
