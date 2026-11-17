模型：Cursor CLI --model grok-4.7-high-fast。沿原implementation-review/task/session7561e9f3-05c0-4aa7-ab45-daa1af63285d精确Resume。本单是本批最后一次实现正式复核，第3/3轮；设计2/2已用完。不写文件、不编译、不派新模型/复审、不改计数，不重审旧底座。
新输入是用户明确纠正后的v3：测试法杖原32格；剑术四个入口各独立8格；仅剑术禁止FIXED且fixedCount>=8（当前八重/十六重），REMAINING穷尽必须允许；复制8不禁。不要再作“所有载具8格”结论。旧载具事实中“remaining禁用”已明确失效。
前置：author-core载具A/B均最终reported_done/valid/writesSettled=true，其他本批源码作者早已收束，主会话确认目标无活跃作者后才派本单。先读本批最新用户v3与A/B交付必要小节、核心作者交付中A/B段；无需反复通读全部旧记录。
本轮仅新增载具限制与关联文本：
- 新shu/technique/release/SwordMulticastGate.java；
- gui/SpellAssemblyMenu.java、network/SkillAssemblyPacket.java、shu/ModTechniques.java相对carrier-before对应基线增量；
- network/SkillUsePacket.java、lang/zh_cn.json、en_us.json相对carrier-runtime-before基线增量。
旧六份共享逻辑第1轮已通过、中性标签第2轮已通过，不重复重验；只确认除8/16说明外旧共享逻辑未变、既有木系新接线没被覆盖。用每阶段基线而不是整个git脏diff。读取相关直接依赖中未被占用的签名即可，不整仓通读，不读运行中内容。
集中核对：
1. 纯谓词只在spec.kind()==FIXED且fixedCount>=8受限，先分kind再取fixedCount；REMAINING/null无多重、fixed3/4及replication2/5/8不得因此拒绝。不按当前ID列表、TechniqueType名、伤害值或复制因子猜测载具限制。
2. 真实SpellAssemblyMenu落子和SkillAssemblyPacket.put非法杖分支都用共同资格点，在消耗物品/保存变更前拒绝；法杖原学会/类型/slotCount资格保持，32格不被8格常量误伤。旧非法配置允许取出/修正，不静默删NBT。
3. SkillUsePacket.offerNow在当前选中剑入口Skill解码后、所有本次付款/冷却/出手安排前拒绝。覆盖四个入口和旧已保存链；只检查这条选中的链，不把其他三个入口上的非法链扩散成全剑禁用；StaffSkills.asSkill分支不套用剑术限制。空链/空手操纵等原资格和顺序保持。
4. 用户容量原语义：Skill.CHAIN_LIMIT=8只管每条剑链，实际菜单4×8；StaffData/测试法杖32保持，不扩容、不拼接四链。穷尽在两载具正常读当前链剩余；一分为八仍保留原宿主/载具资格。只读用户实测清单，指出若任何预期仍误写全载具8格或误禁穷尽。
5. 提示键gui.xueximod.sword.multicast_limit的菜单/包/施放调用和中英locale联通，中文/英文都明确fixed8+；其他翻译（含木系新键）、中性多重/复制短名保持。JSON可解析且不丢键。
6. 8/16 description精确增加不能用于剑术，其他六项保持；待验收N须与真实description逐字一致。无新结构配置、无角色变更、无新增付款/解析/调度机制。
7. 作者必要name-gate结果和范围保留。未跑游戏/最终新源码compileJava不得写已过。给出predicate真值与装配/施放边界的静态证据，说明静态限制。
输出完整一份结论与准确绝对路径/行号，若有问题指出最小修正及唯一author-core。额度用完不自动建议再开第四轮；若发现问题，交回对应作者按确定问题定向返修，之后编译与真实写回，修后未追加复审必须如实记录。不得用本轮替旧底座新增验收判断。
末尾标准 <<<CURSOR_TASK_REPORT>>>，changedFiles=[]、writesSettled=true，notRun编译/游戏，summary/nextAction分清本轮通过、返修或阻塞。复核本身可以reported_done但不能把有问题等同实现通过。
