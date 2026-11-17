# 炉心入口定向只读采集

完整读取同目录 inspect-furnace.json 与本提示。固定 Cursor CLI --model grok-4.7-high-fast。本单只读，禁止改任何文件、编译、测试、提交、后台任务、子代理、其他模型。用 rg 定位，不通读整仓，不查看凭据或用户私密配置。

用户新授权：先不制作任何具体术式。开始做木属性二级资源底座，只有花瓣和岁华两种资源；用户猜测花谱可用现有炉心形式的药水状态，只需要以后做新术式而非另造状态底座。核实当前入口能否复用或最小泛化。

输入文档：`docs/木属性二级玩法设计.md`、`docs/火属性二级玩法设计.md`、`docs/底座功能点.md` 中相关段落、根 AGENTS.md。本单只采集源代码事实及最小修改点候选，不做正式架构正确性裁决；架构由主会话拿到全部事实后交唯一 gpt-6-astra。

源码查找范围：`src/main/java/com/bslx/XueXiMod`，仅搜索炉心、MobEffect、自身状态释放/SELF_STATUS/注册的符号；按实际引用收窄读取。重点 FurnaceService/FurnaceCoreEffect/FurnaceState/ModEffects 或实际对应类、ReleaseDefinitions、ReleaseDefinition、ChainRole/释放类型、SkillUsePacket/链校验与状态提交、炉心相关具体术式注册（只读，不制作）。可读相关现有检查文件，但不运行。

请最终提供：
1. 现行炉心从术式声明到施法检查、魔力/冷却、状态提交、MobEffect注册、tick/移除/死亡/登出/重登/换维的完整入口图，用文字和准确文件/行号，注明硬编码炉心的地方。
2. SELF_STATUS 是通用状态承载还是炉心专用？现有术式如何调用它？如果花谱30秒、六种来源可组合，哪些层已经能复用，哪些不能直接复用。不要误把炉心 heat/amplifier/过载等专属规则套到木系。
3. 不新增花谱术式的前提下，最小通用化可改哪些接口/文件；是否可只登记状态效果并通过未来术式调用；对既有炉心行为的回归风险。
4. 准确的 public API 签名与推荐阅读锚点，便于后续设计裁决；不要输出整文件。
5. 只读任务发现的与候选修改文件有关的既有脏改动/其他作者迹象。使用 `git -c safe.directory=D:/alianximod ...` 仅对任务范围查状态，不改全局 git 配置、不 reset/clean/checkout。不要擅自判断其他作者写入已结束。

报告控制在约2500-4000汉字，证据完整优先。返回的最终文本会由主会话保存为架构输入，不写报告文件。

最终末尾使用：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"已完成炉心入口只读事实采集","changedFiles":[],"checks":["实际定向读取范围"],"notRun":["未修改源码、未编译、未运行测试或游戏"],"writesSettled":true,"blocker":null,"nextAction":"交主会话汇总并交架构裁决"}
<<<END_CURSOR_TASK_REPORT>>>
