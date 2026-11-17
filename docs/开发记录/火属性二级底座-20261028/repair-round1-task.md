# 第1轮集中返修：唯一整合作者

模型必须 Cursor CLI --model grok-4.7-high-fast。你是原作者C，会话30c8a3cf-0e38-4fe0-9005-34fd6b3038f4。本轮由你唯一整合R1–R6，三作者、统一编译者、只读取证均已结束，没有其它源码作者。主会话只更新文档/记录。

完整读取同目录 review-round1.md 与 compile-first-final.md。它们是唯一gpt-6-astra给出的实现1/2正式结论及编译事实；不重审设计，不另开正式复审，不spawn，不要求换模型。按已裁决最小修法落实所有6项，不继续讨论已确认问题。用户范围仍是火属性底层，不做任何术式。

本次写权限合并：status/fire/seed/**（仅确需R项关联才动）、status/fire/furnace/**、release/fire/**、对应本批tests；新增火伤专用mixin、现有xueximod.mixins.json；现有zh_cn.json/en_us.json只补本批死亡键。既有FurnaceResistanceMixin/FurnaceEffectAccess只在R项必需时修改；不能改其它旧攻击、ModTechniques、ReleaseDefinitions、表格、旧OriginalHumanoid命名问题、构建配置或AGENTS。保留已有所有用户改动，不reset/clean/checkout。

必须完成：
1. FurnaceEvents 3处静态Player重复模式匹配改直接局部取值，保持原逻辑。
2. 仅fire_resource源在LivingEntity.hurt相应else分支跳过lastHurt与invulnerableTime两次字段写入，保留原值和完整防御；定向PUTFIELD混入准确限定method/ordinal/require，直接取得hurt的DamageSource参数，不用易被嵌套覆盖的单值HEAD缓存。正常分支和其它来源不变，不清零无敌帧，不绕过护甲/抗性。注册新mixin。
3. 元素帧绑定目标、DamageSource和单次hurt身份。未声明元素的嵌套hurt不能继承外层，显式嵌套独立，返回恢复。仅比较source/target不足以区分同对象嵌套。确保蒸汽中性。可以按该明确修复需要收紧ElementDamageScope的API，所有本批调用/tests同步，最终报告准确签名供文档更新，不接旧术式。
4. BurnCarrier推进后的账本先提交，再分发伤害；伤害后不以旧快照覆写回调的新状态。回调schedule不丢，discard/死亡不复活，必要时世代标记阻止本次剩余伤害批处理，仍保首段延迟与新轨道同tick不执行。
5. 炉心所有能改NBT/效果/属性/伤害的入口及reconcile查询均检查server!=null && server.isSameThread；转换明确拒绝，查询不能后台reconcile，不自动调度后伪报成功。
6. fire_resource/steam_burst各基础、.player、.item共6键，中英文各补齐，Minecraft参数占位语义正确，保留已有语言。

增加必要的有意义定向回归检查，重点R2交错命中保持普通冷却/lastHurt、R3嵌套调用、R4回调schedule/discard、R5线程拒绝。不要复制实现写大量无价值测试；不能以源码字符串存在宣称Mixin运行时已经织入。

作者阶段不执行Gradle，不打包不启动游戏；可执行纯Java定向检查，返回每项真实结果。下一阶段另由已知编译会话固定JDK17与GRADLE_USER_HOME=C:/Users/baishang/.gradle统一编译。不要触发错误sandbox缓存的wrapper下载。

只修本轮6项并进行必要作者自检，不追加正式全包复审。最终交回：R1–R6对应文件/行、是否全部落地、接口签名变更、新检查入口及结果、无法实现项、未游戏验证；明确所有源码写入已结束。
