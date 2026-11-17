沿impl-integration原task/session继续共享整合，固定Cursor CLI --model grok-4.7-high-fast，不spawn、不换模型。完整读本TaskFile/Prompt、AGENTS.md、新流程、max-dispatch，以及本批记录.md、implementation-contract.md、最新interfaces-delivery.md、effects-delivery.md、clone-delivery.md、visual-delivery.md和主会话本次追加的交接说明。
主会话只有收齐C/E/V有效最终回执、确认停写并更新TaskFile范围后才Resume本单。接口阶段和本阶段是同一task，不重置额度；设计1/2，实现1/3已经使用（接口交付文档检查），你不增加正式复审、不改额度、不改记录.md/TaskFile/启动器或.grok回执。用户已确认cooperative-slash-visual-v1-aligned并授权接入，不重复问同版许可。

本次目标：把合同的功能真正接到普通玩家装配/释放链、普通分身部署、手动/冲刺/领域三类入口和统一付款/账本/动作协议，交付可进入最终集中复核和统一编译的完整源码。不能只注册三项、留下抛异常的效果仍被旧路径调用就报告已实现；也不能以复杂为由砍掉双重、错时、命中接续或领域兼容。发现当前合同/接口确实无法实现时，明确报告具体缺口，不擅自降级玩法或隐藏不支持分支。

文件作者：
- 你唯一修改TaskFile列出的共享旧文件、cooperative公共API/计划/能力/注册，以及新共享运行时/编译/提交/份额/初始化辅助文件，和列出的语言、配置指南/本批整合交付。
- clone/core全部归C；cooperative/attack和action归E；客户端、动作运行表现、ModNetwork/CooperativeActionS2CPacket和已确认运行资源归V。即使他们已停止写入，你仍不得改他们的文件。需要返修则精确报告原作者；如新公共API修改，集中列出精确前后签名和消费者变化给主会话，不让消费者自行改API。
- 不碰已确认水幻形PNG/bin/散水/皮肤提交顺序，不重审旧批。保留所有未提交修改，禁止reset/clean/checkout，不修改制作表。不要通读整仓，仅读合同和真实调用点所需代码。
- 禁止gradlew/build/jar/reobf/runClient，不启动游戏。本单作者自检可做必要定向检查/命名门禁，不另设测试/正式复审；最终编译由后续指定独立编译者串行执行。

先据作者最终报告挂工厂和端口：
E已提供ObliqueSlashEffect.technique/releaseDefinition/capabilities/adapter/serverVolume/declareSpecialBalance/immediateGate，以及SpinActionEffect/SpinTimelineDefinition工厂和HorizontalSlashActorAdapter。其execute现在明确拒绝旧段循环，需要你确保新能力入口实际由FamilyExecutionService处理。不要把“返回而没有打出”或effect抛错当成功。专项在seal前恰好声明一次，避免通用motion重复lifetime键/给回旋造未消费速度键；damage host白名单只有真正攻击。
C/V具体工厂以它们最新报告为准。请把服务端生命周期、serverSession/clone token映射、演员publisher和跟踪快照接到真实入口/事件，不留必须手工调用的孤立工具类。正常class initialization顺序和专服安全要保持，不从服务端加载客户端类。

逐项完成合同语义：
1. 三项独立分类，斩击独用立即出刀、回旋无自身伤害可空放、协同无伤害/无专属斩光；横劈是第二真实适配。未适配的后续攻击在收费前明确拒绝，仅不含新前缀的旧玩法沿原路径。普通clone_body PLAYER独立法杖槽0部署/潜行撤回，无隐形召唤/不借水幻形载荷，唯一C付款入口。
2. 手动BatchReleaseCompiler、CONTACT冲刺带新斩击、BindingStructureCompiler/BoundMember/AutomaticRound三个入口实接。能力前缀跨ACTION保留原链下标、索敌/弹道/复制/双重/错时/附魔/接续和原报价；不返回null落旧循环，不按damage>0辨攻击，不以画面双人翻倍。新逻辑只影响含本批能力的执行族，旧无前缀路径不重写。
3. 运行时每MinecraftServer隔离、世界/owner/cast/token校验、最多80刻未提交、硬限额/checked展开、确定性member/copy/spawn/emit identity；全局实体倍率只展开一次，两个演员共享密集settlementOrdinal。每成员独立Tally、防异伤害最后值覆盖，目标共享64上限。
4. 发布桥使用E的真实显式几何和C的演员帧，不能再跑ambient CastFrame/隐式SkillEntityCount。不同链落点/交错先求解、再加演员眼点差。按合同处理冻结方向和配置，作者报告若有需要I处理的退化/旧box接口限制应落地，不把其文字建议当作修改已批准合同的授权。新斜斩薄盒厚度必须真实参与命中，不误用旧无厚度tiltedPlane测试；横劈适配保持旧配置与锚点。
5. 先单人合法，再可选预约分身；两tick有界安全归位失败正常单人。实例位姿偏差/世代/权限/安全复验，规则附加费可选，mana不足合作但足够单人则回退；不可让同一槽旧响应同时占用。成员双人暂存/许可/发布全有或全无，失败后单人也不能重复已有玩家体积。用共同预算、最多一具普通分身/一族，不碰水幻形响应。
6. 服务端6+6刻TIMED gate仅触发一次，CONTACT保持原冲刺合法接触次数并派生子族；出刀点发布几何、单斩立即姿态，回旋过程中不被solo覆盖；费用与冷却按真实提交项一并聚合，不随演员或回调重复。客户端包/TimeNotice不参与伤害触发。
7. 动作费RUNNING时提交、攻击份额成员发布时提交、合作费第一次双人发布时提交；未取份额原receipt原池退，禁止MagicPool.restore。取消/身体软失败/主人硬取消、登出/复制/换维/世界卸载/停服、载体失效、晚到回调全部闭合；已提交攻击不因换姿态倒扣，尚未结算的硬取消guard丢弃载荷。手动冷却和领域自动轮冷却分别遵循原职责。
8. Tally所有本批发射哪怕单copy也强制共享record，不走额外direct hurt；每次record/flush重验owner/目标权限，硬取消可清待结算/监听。协作结算作用域CloneSourceScope深度1只包最终当刻结算，不把ThreadLocal跨tick存活；不改旧无敌帧/全局分类。
9. 标准费用/冷却/伤害及专项参数按配置指南、服务端读取一次并冻结。运动/规则不能假挂damageMultiplier；配置0伤害仍ATTACK。修改docs/配置平衡指南.md时只写真正已消费的键和例子，不覆盖用户配置文件，不另开热重载。
10. 使用V已确认资产路径/表现工厂，正常tracking/自己收到、晚到快进、旧stop不清新；源/运行PNG哈希对应。整个bbmodel/青灰假人/占位剑不进运行。正式参数变更不得暗改美术观感，交回主会话处理。
11. 真实的准备/发布/拒绝/回退/emit性能统计挂既有计数器，不登记空消费者。预算超限先判断、费用前拒绝或双人降单人，不静默裁copy或全局倍率。
12. 术式石/学习/载体准入/语言说明实际可见；普通分身生成仍是独立部署，不放宽其它开发术式的权限。

最终写本批integration-delivery.md：每个玩家入口、服务/生命周期/工厂真实挂接点、三组主要装配与横劈复用、费用/权限/取消/复制/领域的实际实现路径、消费配置、精确改变文件、必要作者自检以及任何没做完/需要原作者返修的项。另写本批游戏实测说明.md，面向用户给实际获取/装配/释放方式和少量关键观察，不把未游戏验证写已通过。源码写完停止，等待集中复核/统一编译。

末尾唯一机器回执（完整JSON、标记独占一行，不伪造task/attempt/session/inputHash）：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"实际共享整合结果","changedFiles":[],"checks":[],"notRun":["未统一编译","未游戏验证","未新增正式复审"],"writesSettled":true,"blocker":null,"nextAction":"主会话收束本阶段并在剩余额度内集中复核，之后指定Cursor统一编译"}
<<<END_CURSOR_TASK_REPORT>>>
真实未完成用blocked/needs_input/failed并具体说明，不因局部代码完成就把完整功能称完成。

# V最终回执补充（2026-11-03）

V已valid/reported_done/writesSettled=true，最终说明visual-delivery.md，Result为visual.final.md。必须读取该最终文档后接线。`CooperativeActionPublisher.publisher()`返回ActorActionPublisher；V自己注册生命周期/开始跟踪补发；`ModNetwork.register`已登记通道17，不重复登记，不改V所有文件。

攻击实体必须同步新斩击的techniqueId=sword_oblique_slash或真实texture路径`xueximod:textures/entity/xueximod/oblique_slash_v1.png`，中心由服务端完成眼前1.5格，客户端不再加偏移。沿既有placeAt传方向，roll传刀面倾角；length纵深、width宽、height逻辑厚度。横劈保持旧表现。E presentationId=xueximod:oblique_slash与V常量xueximod:oblique_slash_v1的命名映射可在I自己持有的实体发布层明确处理，不能因此改动确认素材或消费者文件。

动作由冻结服务器startTick/windup/recovery发布；镜像演员启用mirrored轨道，短距归位来自C真实位置，不能再叠BB预览0.1s位移。普通分身保留原外观，水幻形不走本次协同。V未编译，仍需最终统一编译。

# C最终回执补充（2026-11-03）

C已valid/reported_done/writesSettled=true，最终说明clone-delivery.md、完整反馈clone.final.md。`CloneServerPorts.create(ServerSessionLookup)`同一实例提供CooperationActors、ShortReposition、CloneStandaloneDeployment；不要分开new多个实例。lookup.sessionOf(server)由I唯一分配UUID，lookup.cloneSessionToken(UUID)必须映射到该server的CloneRuntime.get(server).sessionToken()，不能自己hash/随机long。

普通CloneSpec开启ExternalCooperation.OPEN，旧构造默认CLOSED。clone_body登记开放PLAYER仅归I；开发命令权限仍保留2。部署唯一付款在C服务，I负责完整结构/载体/合法报价和立即return；潜行撤回不走付款路径。C已报告载体检查主手栈/slot但未调用CarrierWatch.observe（避免清掉水刃拍号），I应在自己拥有的入口完成合同所需generation合法性与凭据传递，不重复收款。C文档的其它风险/边界一并完整读取，不能把作者“待验证”说成已证实安全。

当前C/E/V均收束，I可以单作者共享整合。所有原作者文件禁止改动仍有效；需要原作者修复的具体项最终报告交回，由主会话沿原task/session继续，不新建作者。

## TaskFile目录授权的进一步单作者边界

cooperative目录只允许公共api/已有I类及新共享运行时、计划降低、付款、提交辅助；明确排除attack/和action/两个E目录。network目录只允许合同已点名的SkillAssemblyPacket、BindingStructureCompiler、BatchReleasePlan、BatchReleaseCompiler、BatchCastSession、AutomaticRound、AttackBudgetWindow、SkillUsePacket、SkillSegmentRunner、SkillSegmentMath、SkillChainPlan、UncommittedShare，以及确有需要的新Cooperative前缀辅助Java类；明确排除ModNetwork.java、CooperativeActionS2CPacket.java及其它无关已有类。hitbox目录只可增加显式OBB最小辅助或扩充SizedBoxHitbox显式新入口，保留旧入口行为。目录授权不转移C/E/V文件所有权。

