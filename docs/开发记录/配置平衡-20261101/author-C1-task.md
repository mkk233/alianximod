固定 Cursor CLI --model grok-4.7-high-fast。你是后续源码作者C，本阶段 C1仅只读接入准备/依赖事实采集，不正式复审、不写文件、不编译、不测试、不派代理，不读写制作表。设计累计1/2，实现0/3。公共A1正在创建shu/balance接口，禁止读它的中间代码；后续收到最终API合同才实现。
读本目录架构裁决.md、implementation-contract.md、三份read-*-final.md。只定向读将归你负责的源码（以下相对src/main/java/com/bslx/XueXiMod/）：
shu/technique/enchant/**、shu/technique/EnchantmentDefinitions.java、status/fire/**、shu/technique/release/fire/**、shu/technique/release/firecast/**、mixin/PlayerAttackMixin.java。已核实SteamBurst就在release/fire/下。本任务正确补全了裁决中的路径前缀，不扩大玩法范围。B拥有effects/**、batch/**、domain除DomainAuraResolver等，A拥有network/**、shu/balance/**，你不得改这些文件。
仅输出后续实现所需明确接口/事实，别重复62项表：
1. 为已批准“原公式先结算，再按来源缩放一次”找到火伤/炉心原公式的准确收束点，列现有函数签名及如何向B暴露已配置伤害接口/保持原方法兼容；保留所有已批准限制，不在总hurt出口再次统一倍率。
2. C专项参数登记适合由哪个统一bootstrap调用，给将供A/B接入的准确类/方法合同建议；不同模块不要重复声明同键。
3. EnchantBonus/WeaponEnchant给A段伤害组合需提供什么原始/已配置金额接口，潮印条数与有效伤害如何区分，以及旧NBT回退事实。
4. B/C跨界费用/产种/范围/时长实际调用点，列签名；任何已批准架构无法覆盖的问题才报告，不重新裁决整体方案。
最终stdout简明完整，暂不实现。后续复用同会话给API合同与C2授权。
