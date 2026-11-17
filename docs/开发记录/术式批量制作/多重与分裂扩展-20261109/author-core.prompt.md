模型：Cursor CLI --model grok-4.7-high-fast。首次Start写会话。完整读取TaskFile、本Prompt与本批记录。只按授权互斥路径实现，不派其他模型，不改启动器、运行记录或批次额度。
用户授权八项同底座只改数量：三重释放/四重释放/八重释放/十六重释放/穷尽释放；一分为二/五/八。原始详细需求与N/O/DV见本批领取回执及记录，实施前必须确认八项已在制作中，首次原始留档已成功；如果未满足立即交回，不先改源码。
读本批“只读事实.md”“入口与几何补充事实.md”“记录.md”设计2/2最终契约。设计额度2/2已用完，不再请求架构裁决或重审旧底座；在批准设计内查签名和实现可以继续。实现额度当前0/3，不自行计数或单项正式复审。全部作者收束后集中只读复核，不读其他作者运行中源码和diff。
保留所有现存未提交改动，不reset/clean/checkout覆盖。不打包/不启动游戏/不提交。本单无编译权限（包括compileJava），交付写“待统一编译”。不得改制作表、批次记录、其他作者交付文件。所有新重要平衡值继承同类默认，由BalanceCatalog已有标准键读取；不增加数量/时序等结构配置。
这批没有新美术，复用现有通用图标textures/gui/technique/other_none.png，宿主几何与现有表现不变，禁止为2/5/8重新设计布局。不要改ReleaseDefinition.java、SkillUsePacket.java、ChainRole.java、付款服务/执行协议，它们不在授权中且部分由木属性批次作者占用。
Java改动完成执行项目根 powershell -NoProfile -File tools/check-name-gate.ps1；只修本次动过文件报出的局部/参数/字段，报告遗留未动文件的结果，不能宣称全仓通过。默认无独立测试任务，不造无意义镜像测试；必要定向检查纳入本单交付，勿另开测试阶段。
真实接口若不足、需要越过本单权限或改付款/协议才能落地，交回具体缺口、绝对路径/行号、已完成与未完成，停止依赖部分；不能自由设计新底座，不能以拒绝新数量/丢语义回退作为完成。
最终交付写独立作者交付.md，列准确路径/行号、实现、必要检查、配置默认/单位/范围/实际消费与生效时机、资源映射、N/O原文对应游戏内描述候选、尚未运行编译/游戏、文件写入是否收束。最后标准 <<<CURSOR_TASK_REPORT>>> JSON，changedFiles真实，writesSettled=true，notRun写编译/游戏，nextAction等另一作者后集中复核。
## 本单唯一作者范围与目标（author-core）
仅写TaskFile的3份Java及核心作者交付.md：ModTechniques.java、release/ReleaseDefinitions.java、network/SkillChainPlan.java。适配作者并行写另外两编译器与一个薄helper，你不得改/读其运行中版本，不需要依赖它们的新接口。
在ModTechniques/ReleaseDefinitions最小增量登记八项：
rule_triple_release 三重释放 fixed(3)
rule_quadruple_release 四重释放 fixed(4)
rule_octuple_release 八重释放 fixed(8)
rule_sixteen_release 十六重释放 fixed(16)
rule_exhaustive_release 穷尽释放 remaining()
上五项ChainRole.RULE_DUAL，pullAfterA同双重为1，ReplicationSpec=null，同类空effect，冷却20tick。
rule_double_cast 一分为二 of(2)
rule_quintuple_cast 一分为五 of(5)
rule_octuple_cast 一分为八 of(8)
后三项ChainRole.RULE_TRIPLE，pullAfterA同一分为三为0，multicast=null，复用既有复制规则effect，冷却0tick。
全部通用/无属性/基础伤害0/等级上限20/规则自身默认耗魔0（成员照旧扣费）。先查ID不冲突，不覆盖现有常量/注册。不新增八套effect，不按术式ID加链结算分支。
游戏内description与待验收N严格一致：固定K“按多重释放规则，依次释放后续最多K个装配格的内容。”；穷尽“按多重释放规则，依次释放后续直到链尾的装配内容。”；分裂K“将后续首个接受复制的宿主复制为K份。”（K数字用2/5/8；多重3/4/8/16，句末句号统一。）
SkillChainPlan仅补两个必要固定乘3：scanModifiers RULE_TRIPLE分支与copiesBefore无MemberCastView fallback，读取对应ReleaseDefinition.replication().factor。若def或spec缺失，沿现有适用资格/合法默认路径处理，不能为未声明复制随意加3，也不改既有已声明三份默认。成员已带MemberCastView时直接用copyProduct一次，不额外再叠乘plan.copies。维持多重的共享parser/缓存/时序/报价语义。
配置兼容交付要列每项cooldownTicks/manaCost标准键（默认20或0/0，tick及魔力单位，范围以实际BalanceCatalog已声明为准，非零冷却不接受0的既有规则，世界/服务器重开生效、服务端快照同步客户端）。不要声明结构数量键、无消费者键，也不要把伤害参数充当复制资格。
本单不得改CooperativeChainCompiler/BatchReleaseCompiler/helper或任何表格。完成后待统一编译。

本批已保存实现前字节在 source-before/（相对路径映射见manifest.json）。这是用于区别本批改动与既有未提交内容的只读对照；不许覆盖/恢复源码，不许修改基线。实现前核对自己文件与manifest相符，若有他人新改动先对账交回。作者交付应准确列本批增量而不把整个git脏状态当自己改动。
