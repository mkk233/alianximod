固定 Cursor CLI --model grok-4.7-high-fast。你是源码作者A，续办A2实际完整接入。读implementation-contract.md、架构裁决.md（含第2轮补充）、API合同.md及author-B1-final.md、author-C1-final.md。保留A1接口与清单；不要重新架构裁决。设计2/2已用尽，实现0/3尚未正式复审。主会话派发本单即A1已最终收束、B/C可依赖API合同。你与B/C按互斥文件并行，不读他们的中间代码。
独占允许路径（相对src/main/java/com/bslx/XueXiMod/）：
shu/balance/**、XueXiMod.java、shu/Technique.java、shu/TechniqueManager.java、shu/TechniqueIds.java、api/RegisterXueXiContentEvent.java、network/**、shu/technique/release/ReleaseRegistry.java、CastFrame.java、ReleaseInput.java、ReleaseBatch.java、FlowRoundLedger.java、TechniqueHits.java、shu/technique/release/entity/**、shu/technique/release/domain/DomainAuraResolver.java、gui/SpellAssemblyScreen.java、gui/ImprintTreeView.java。上述单文件CastFrame等均在shu/technique/release/下。日志/备份/author-A-manifest.json允许本批目录；不得改文档作者的规则/指南、B/C源码、制作表、Config.java或Gradle配置。
按架构完成：
- JSON模板与世界文件生命周期、schema/边界校验、未知项告警保留、缺字段默认、不重写作者值；服务端启动加载冻结、客户端仅接收权威快照、登出清理且不误清集成服务端，专服不能引用仅客户端类导致启动失败。
- 注册阶段统一B/C专项声明，按第2轮精确合同调用双方bootstrap；完成内置62项与外部术式的标准目录/封存检查，外部旧API兼容及新spec入口。不要按baseDamage>0给所有术式猜实伤：剑来/万剑归宗无通用damageMultiplier，C实际零底伤术式与水牢仍需真实伤害键。与B/C合同逐项一致。
- Technique原始baseDamage用于结构保持原样；费用原默认逻辑独立；实际冷却统一入口；配置感知的展示不能更改结构getter。补所有本范围内报价旁路，严格区分普通链预扣、reserved多重份额、水系开域/护体/屏障生成、水牢成功单价、自身状态、资源附魔、领域自动链/接触费。B1报告是这些分流的事实依据，避免同一键同时代表“0元宿主预扣”和“40元开域费”并被加两次。钱包/退款不二次计算倍率。
- 各网络段伤害入口使用C第2轮确定的原始拆分合同，宿主本体和其已有武器加成按宿主ID形成一笔，附魔共享附加与潮印按归属分别一次。CastFrame、spec、账本都只保存/合并已配置金额；公共hurt出口不再次乘。域光环按其来源形成贡献，不改变允许合并路径。
- 默认行为保持；不得为了省工作把配置写入静态final、不生成未接通键。需要跨作者改动在最终报告明确提出，不能跨写。公共API若需新增辅助可向后兼容追加，但不得更改已供B/C使用的签名/语义。
- 保留原所有未提交改动，逐文件追加pre-A备份与manifest（A1已有新文件不能误记原文件），跑命名门禁，只修自己本次文件问题。
不编译、不独立测试、不启动游戏、不提交。最终报告必须包括已完成/未覆盖、实际文件、全部配置键真实来源/消费者证据、B/C公共接入引用是否已按合同完成、命名门禁结果、待统一编译、释放全部写入权。不要以未运行编译当阻塞；真实无法完成的项明确报出。
派发补充：必须完整读取本批接口衔接.md；A1与设计第2轮之间的优先级及新增公共签名以该文档为准。
