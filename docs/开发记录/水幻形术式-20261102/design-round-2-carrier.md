# 水幻形设计第2轮：载体门

日期：2026-11-02。本批设计2/2，实现0/3。只裁决新查实的MAGIC宿主与SWORD载荷装配/施放阻塞；原底座历史和本批第1轮玩法/伤害/付款/生命周期不重审、不重置。A/B/D已启动独立实现，本轮不修改它们的冻结接口。

## 最终事实

已完整读取并等最终反馈停写：implementation-contract-report.md、implementation-contract.md、read-carrier-gates-final.md。水幻形按已批准设计为MAGIC，断潮现为SWORD。法杖put/GUI类型门、StaffSkills.castRefusal（先于asSkill）、offerNow第二道SWORD检查全部拒绝。StaffSkills.add/asSkill不按SWORD丢条目，法杖存取保留原胚；GUI没有额外的法杖SWORD预滤。法杖packet只能末尾追加，GUI按gearIndex替换后把非空石紧凑成链。

另确认本捕获新路径不可直接沿用的写入风险：put先consume再add且忽略false；writeGear先清空再add失败break。本轮只要求新捕获编辑不得静默消耗/截断，不扩审或修复全部旧路径。

## 裁决

唯一载体是法杖槽0。两术式类型不变；宿主声明STAFF_ONLY和不可变SWORD载荷例外。完整捕获计划验证通过之后，仅已被消费的载荷原下标可略过法杖类型门，其余学习/解锁/原胚/容量/持剑需求/开发能力全部保留。不按ID开门，不开放任意MAGIC+SWORD，不把断潮改成MAGIC。

C增加纯ActorCaptureCarrierGate，与现有ActorCaptureCompiler共用完整结构判断。宿主前缀可以保存DRAFT但不可施放；先放宿主再加适配载荷。插入/替换验证实际完整候选，反序/额外/未适配/非NONE原胚拒绝且不消耗。删除/移动可无损保存不完整或反序的草稿，不能自动丢载荷、原胚或恢复执行资格；每次施放重新验证。未知条目不得先被asSkill丢掉后假成合法链。

StaffSkills.castRefusal和offerNow第二道SWORD门都按同一已消费下标例外检查；executeSkill/dispatcher在Batch/报价之前防御检查实际载体与完整PLAN。非法或手写NBT链收费前拒绝。普通法杖裸SWORD、普通剑技MAGIC拒绝及剑技同步过滤保持原样。

新捕获编辑采用完整候选及原子列表替换，失败不改变来源、法杖术式/原胚、菜单齿轮或携带栈，不覆盖法杖魔力等其它NBT。C只在相关法杖路径补这个提交边界，不另开全局库存重构。

## 精确C范围

根目录为D:/alianximod/src/main/java/com/bslx/XueXiMod/：

- `shu/technique/release/capture/`：ActorCaptureContract追加载体策略，新增ActorCaptureCarrierGate及只读结果类型；compiler/dispatcher补实际载体准入。
- `shu/technique/release/clone/water/WaterPhantomDefinition.java`：STAFF_ONLY及SWORD载荷例外声明。
- `item/StaffSkills.java`：castRefusal与捕获相关完整列表原子写入辅助；不全局放宽add/asSkill。
- `network/SkillAssemblyPacket.java`：法杖末尾追加的完整候选、本捕获成对写入、删除移动草稿状态。非法杖分支不放宽。
- `gui/SpellAssemblyMenu.java`：法杖替换后非空紧凑候选与写回一致性。剑技类型门/同步过滤不删；不改筛选按钮。
- `network/SkillUsePacket.java`：offerNow法杖SWORD门及最终执行前防御，配合StaffSkills的更早门。

无需改A/B记录、lease、付款、报价、体积或D协议。无需改TechniqueType、SkillCarrier全局准入、普通剑技同步或客户端筛选屏幕。

完整新增接口与边界已写入implementation-contract第9节和必要C接口说明；详细指导第4.1节与design-decision同步。C可以按结论实施，无待用户偏好问题，不再确认开工。

## 状态

设计2/2已用完，不自动追加设计复审。实现额度仍按本批记录继续。Steve标准六方块、直接真实Blockbench画材质的用户新指令保持；v1否决，v2未确认。视觉确认前C通用代码可做，正式PLAYER注册/专项声明/具体外观接线仍待确认版。

未实现Java、未编译、未操作MCP、未改表。本轮授权的四份文档写入完毕，设计者停止写入。
