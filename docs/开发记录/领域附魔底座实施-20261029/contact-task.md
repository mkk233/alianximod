# 领域基础附魔成功伤害计费 实现任务

模型 Cursor CLI --model grok-4.7-high-fast。工作区 D:/alianximod，遵守AGENTS.md与max-dispatch。阶段：按已批准方案实现，不新做设计评审。设计2/2已用完；实现0/3，待当前两路作者全部最终交回并收束后再集中审查。禁止自行派模型。保留工作区已有未提交修改，禁止reset/clean/checkout覆盖。

用户最新已授权『好了开始修改底座吧』。价格已明确答复：『先用每种基础附魔每次1点，多种相加，之后实测调数值』。本次执行已写方案第一阶段：领域限时、取消维护、对领域实际携带的基础附魔接触成功伤害收事件费。缺魔跳过本次收费伤害、领域继续存在；固定潮印光环不新增收费。保留现有生成费和开域附魔预扣。不开关、不做按键/管理界面、不做自动绑定垂丝、不改变同名共存。多附魔按冻结快照中不同基础附魔类型计数，每种1点；同一类型重复条目不因重复计数变多，不更改当前伤害快照的既有计算。每次成功接触伤害按其携带的基础附魔种类收费，金额独立命名参数便于以后调数值；不是把旧8点维护费搬过来，也不是所有领域统一收一次1点。

先完整读 docs/领域附魔限时与触发计费修改方案-2026-10-29.md、docs/开发记录/领域附魔方案-20261029/design-decision.md 及两个read-*-final.md里相关事实。最新开始实施与明确价格覆盖方案的仅文档状态和价格待定；其他建议按本任务给出的默认保持范围落实，不重设计。读代码用本模型，限相关路径和必要调用，不通读整仓。

唯一可写路径：
- src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainInstance.java
- src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainContact*.java（允许拟新增的领域专用计费/策略类）
- src/main/java/com/bslx/XueXiMod/shu/technique/release/contact/*.java（仅为此接入必要文件，不顺手整理）
- src/main/java/com/bslx/XueXiMod/shu/technique/enchant/EnchantmentSnapshot.java（如需暴露只读类型计数/标识；保持现有伤害和普通宿主语义）
- src/main/java/com/bslx/XueXiMod/shu/technique/release/TechniqueHits.java（仅在确有必要统一正伤确认时修改，优先复用现有hurtConfirmed）
- docs/开发记录/领域附魔底座实施-20261029/contact-evidence/（本作者原始快照、文件清单及证据）

另一作者独占 DomainDefinition.java、DomainService.java、WaterReleaseDefinition.java；你不得改这些文件，不依赖对方未声明的新API。DomainInstance归你清理旧维护时钟/接口，对方将移除DomainService维护调用。DomainDefinition构造访问可保留既有兼容API，不要强迫对方新增计费字段。计费参数放领域专用独立定义中，本次不改共享Config.java。MagicPool、ManaPaymentReceipt仅可读，不允许重写资金底座；若真正无法遵守现有已裁决退款契约，保留现场报告。

第一步为每个要改的既有文件完整备份到contact-evidence/before相同相对路径（很多代码未跟踪，不可只依赖git diff）；对新增文件另记清单。最终报告所有修改文件末次写入时间，交回后收束写入。

必须实现的行为：
1. 仅DomainInstance这类领域接触宿主启用事件计费。非领域的接触宿主、普通攻击、友方固定潮印光环、原生潮湿/水势/耗氧/封界都沿用原样。
2. 每目标每次领域接触先过现有合法性和几何过滤，再按冻结基础附魔种类报价，从领域owner实时可用魔力预留独立ManaPaymentReceipt。不能先伤害后扣费，也不能复用多年前结束的按键父收据。
3. 预留失败静默跳过本次伤害和其附魔/火种载荷，不关闭领域。每个事件至多commit/refund一次。确认正伤才提交，取消、无敌帧挡住、最终零伤则原路退款。致死也算成功伤害；载荷是否因目标死而不应用沿当前规则。异常路径也不能漏钱或免费伤害。
4. 复用现有hurtConfirmed的正伤确认及RESPECT_INVULNERABILITY；不得改无敌帧，不拆第二次hurt，不让领域接触拿领域光环递归增伤。非领域宿主不能被强制改成新确认/新收费规则。
5. 退款通过ManaPaymentReceipt原来源能力，不用只返玩家池的MagicPool.restore；继续支持法杖优先、玩家池补足。预留时读当下余额，所有领域事件服务端串行按确定顺序处理，同玩家按领域实例标识、目标标识等稳定顺序，不能依赖HashMap遍历或让多个事件共用扣前余额。保留合法多领域/多复制实例，不把它们全局去重为一份。
6. DomainInstance清理不再使用的维护时钟/接口，寿命、主人生命周期、contact.close、同步和原生行为保持；不开新持久化/NBT，不扩成触发术式调度器。

无编译授权：禁止gradlew/build/jar/reobf/runClient、游戏、制作表、提交、新增测试。Java修改后运行 powershell -NoProfile -File tools/check-name-gate.ps1，只处理本次自己修改文件；其他已存在诊断记录归属不越界。作者必要自检可做，不另起正式复审。

最终反馈：路径、基线/新增清单、关键方法行号；按事件的费用公式和资金/成功判断；非领域兼容性；确定排序键；已做name-gate和未跑验证；所有文件末次写入时间；『写入已收束，待统一编译』。不报游戏已验证，不需要流式进度。若模型不可用不降档。
