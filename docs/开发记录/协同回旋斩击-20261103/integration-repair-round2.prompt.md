沿impl-integration原task/session返修，固定Cursor CLI --model grok-4.7-high-fast，不spawn、不换模型。读TaskFile/本Prompt、最新记录.md、implementation-contract.md、review-implementation-round2.final.md、你自己的integration-delivery.md。集中复核已结束，全部源码作者停写，现在只有你获本范围写入权；C/E/V仍冻结，不能改他们的文件。

本批设计1/2，实现2/3已使用。本单是原作者定向修复，不新增正式复审，不改额度/记录/启动器/制作表。没有Gradle/打包/游戏授权。保留所有未提交变化。原TaskFile范围继续有效，其中cooperative排除attack/action、network排除V和无关旧文件的边界不变。本次额外授权shu/technique/chain/TechniqueChain.java给你处理已有规则宿主职责；不顺带改其它旧规则。

本轮只读复核结论为必须返修，7项均归I。必须完整读取该报告的证据、位置和复现路径后修复，不只是修改交付文档声称支持。目标按原合同，不改变已经确认的视觉参数、动作曲线、魔力默认值或组合玩法。

1. 分身端口已经选择左右站位并负责两刻归位，不应再由FamilyExecutionService.offerReposition强行二次移向右侧。复用预约实际到位结果；每次成员实际出刀必须调用CooperationActors.validate(reservation,当前合法ownerFrame)，失效时退单人。所有需要移动的分身也必须执行玩家当前位置偏差校验，不能只有初始READY有reservedEye才检查。
2. CONTACT每次合法触发的TriggerGeometry.point和direction/target必须穿到该子族显式几何；保留落点/交错原职责和真实接触锚点，不再只用方向并遗失point。不得改变旧无新能力冲刺。
3. 领域协同费用必须有且仅有一个真实付款方。完整链与原自动轮报价/原receipt份额衔接，不能SELF_STATUS跳段而免费双人，也不能为了修复再charge一遍按键。预付路径可以不再收费，但可选协同份额仍要遵循原合同：没有成功双人发布时退原receipt原池；不能把份额全写0之后假称能退。保证动作/攻击/协同实际提交线、领域冷却职责仍正确。若需要新辅助类放在I所有范围，不改C/E/V。报告精确说明来源与退款归属。
4. CooperativeSettlementGuard.active真实复验serverSession、world、owner合法性、父族硬取消；allows每次record和flush都调用同一权限边界（DamagePermission、排除任意分身、主人驯服动物、同队）。ReleaseBatch刷新必须逐目标复验，不只active总开关。不要全局改变旧账本默认路径，无敌帧规则保持。
5. 载体失效只取消未提交成员并退这些份额，已经发布且合法owner仍有效的成员保留伤害与待刷新账本；出刀后换手不能付费却吞刀。主人死亡/登出/换维/复制、世界卸载/停服/父族取消仍是合同硬取消，应丢弃待结算；两种失效不能混为一个全族开关。
6. 双人暂存/发布失败后先清除全部双人暂存和未完成发布，再在同一调用内用原单人计划尝试一次完整发布；不下一刻又试双人，不重复已有玩家体积、不留半提交。费用与预算只按最后成功提交的成员提交。保留合同all-or-none和幂等语义，不仅靠异常后晚一刻删除。
7. 有预约时先PREPARED等分身READY或确定单人回退，再开始共同回旋RUNNING。开始时给玩家与分身发同一serverStartTick/windup/recovery，分身mirrored=true、使用实际actor id/entity id/generation/只读武器快照。没有分身/无协同的回旋不额外等两刻。出刀仍由服务端gate一次触发，单斩姿态不能覆盖尚未结束的回旋。失败/取消停止对应演员，保留客户端现有旧seq不清新约束。

完成后更新integration-delivery.md和游戏实测说明.md，使实际边界与本次代码一致；可在本批新写integration-repair-round2-delivery.md详细对照7项。报告真实改动文件路径（上次部分entity路径与真实路径不同），不报不存在路径。不改主会话记录。做作者必要定向检查与命名门禁，无编译；不要启动另一轮正式复审。

确实必须跨C/E/V文件或改变公共签名时先在最终回执精确交回缺口，不越权。无法满足的合同项使用blocked，不把局部修复称整套完成。

末尾唯一机器JSON：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"七项定向返修的实际结果","changedFiles":[],"checks":[],"notRun":["未统一编译","未游戏验证","未新增正式复审"],"writesSettled":true,"blocker":null,"nextAction":"主会话等待所有写入收束后按剩余第3轮实现额度决定集中复核，再统一编译"}
<<<END_CURSOR_TASK_REPORT>>>
