# 领域生命周期停维护 实现任务

模型 Cursor CLI --model grok-4.7-high-fast，必须显式保持此模型。工作区 D:/alianximod，遵守AGENTS.md与max-dispatch。执行任务：实现，不是新设计评审。现有方案的设计额度2/2已用完，不得追加设计审查；按已批准方案实现。实现额度当前0/3，待本阶段两路作者均结束后统一审查。禁止自行派模型。

用户已于2026-10-29说『好了开始修改底座吧』，随后确认领域基础附魔事件价格为『先用每种基础附魔每次1点，多种相加，之后实测调数值』。本次落实方案第一阶段，不做开关、不做自动绑定垂丝。沿用当前生成费、尺寸、寿命、主人死亡/下线/跨维关闭、原生湿度/水势/减速/耗氧/边界与固定潮印光环。取消领域存续维护；不影响护体、屏障、旧场地维护。接触计费由另一作者负责。

先完整读 docs/领域附魔限时与触发计费修改方案-2026-10-29.md 以及 docs/开发记录/领域附魔方案-20261029/read-domain-final.md 中与你有关的事实。方案中仅文档交付状态被最新实施指令覆盖。保留所有既有未提交内容，禁止reset、clean、checkout覆盖。

唯一文件所有权，可写仅：
- src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainDefinition.java
- src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainService.java
- src/main/java/com/bslx/XueXiMod/shu/technique/release/WaterReleaseDefinition.java
- docs/开发记录/领域附魔底座实施-20261029/lifecycle-evidence/（本作者原始文件快照、文件清单和证据；不得放游戏代码副本回src）

另一作者独占 DomainInstance.java、接触包与计费类；你不得改这些文件。你可读需要的相关代码，但不通读整仓。第一步将你要改的原始文件完整复制到lifecycle-evidence/before下，保留相对结构（大量相关代码未跟踪，git diff不足以作本次基线）。所有新增/修改源文件最终列完整路径及LastWriteTime，结束时明确释放写入所有权，未经新派发不继续写。

具体：
1. DomainService.tick 删除或停用维护扣款及维护不足关闭分支；保留先判断到期/主人有效性再做脉冲，关闭清理顺序不变。
2. DomainDefinition 的维护字段/构造器/工厂参数不再作为存续条件；在不影响非领域调用兼容性前提下清理或明确弃用。不要仅把8写0却保留到期收费驱动语义。构造器签名若被本次范围外代码调用，优先保持兼容而不越界改调用方。
3. WaterReleaseDefinition.domain 不再把领域维护费传为有效场地维护；普通FieldZones/护体/屏障维护保持原样，领域生成成本及显式成本的单次扣费路径保持现状。
4. DomainInstance中的旧维护时钟与接口由另一个作者清理，你的代码不再调用nextUpkeepAt等维护接口即可，不依赖对方新增API。不要改生成波次、复制数量、同名共存、几何、寿命缩放或触发/原生周期。

无编译授权：禁止gradlew、build、jar、reobf、runClient，不启动游戏、不改制作表、不提交、不新增测试。改Java后执行 powershell -NoProfile -File tools/check-name-gate.ps1，只修自己改过文件的命名问题；既有或别人文件的诊断只记录。必要静态搜索和作者自检可做，不作额外正式复审。

最终一次报告完整交回：改过路径、基线快照位置、具体变化、关键方法行号、name-gate结果（归属说明）、未跑验证、文件末次写入时间和『写入已收束，待统一编译』。不需要流式进度。遇到固定模型不可用或超出批准设计的真正冲突就保留现场报告，不降档不擅改方案。
