固定 Cursor CLI --model grok-4.7-high-fast。你是后续源码作者B，本阶段 B1仅只读接入准备/依赖事实采集，不正式复审、不写文件、不编译、不测试、不派代理，不读写制作表。设计累计1/2，实现0/3。公共A1正在创建shu/balance接口，此时禁止读它的中间代码；后续收到最终API合同才实现。
读本目录架构裁决.md、implementation-contract.md、三份read-*-final.md。仅有针对性读取将归你负责的源码：
shu/ModTechniques.java、WaterTechniques.java；shu/technique/release/{ReleaseDefinition.java,ReleaseDefinitions.java,WaterReleaseDefinition.java,WeepingSettings.java}；release/selfstatus/**、release/batch/**、shu/technique/effects/**、carrier/**、shu/technique/sword/**、release/{barrier,guard,field,conditional,prison}/**、release/domain/**排除DomainAuraResolver.java。
以上路径均相对 src/main/java/com/bslx/XueXiMod/。其他文件可只读查调用但不展开无关实现。你不拥有任何shu/balance、network、enchant、release/fire、release/firecast、status/fire、EnchantmentDefinitions.java或Mixin文件。
仅返回后续实现所需的外部接口事实，别重复62项表：
1. B侧专项参数可放到哪些就近定义工厂声明，怎样由ModTechniques/WaterTechniques在最终注册后统一触发；列需A2调用的一个明确B bootstrap方法位置建议（实际实现后才能接）。
2. B与C的直接交界调用（Liaoli、火范围/持续、幽冥资源产种、附魔费用）列现有准确签名和调用文件，提示需要C提供什么合同，不能自行改C文件。
3. B的所有真正费用消费点列表，哪些需要统一raw费用来源避免二次倍率；B与A报价接口交界。
4. 发现超出已批准架构或本所有权不可完成的具体阻塞才指出；不要重新裁决整体方案。
最终stdout简明完整，暂不实现。后续会复用同会话给API合同与B2授权。
