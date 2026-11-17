沿review-implementation原task/session继续，固定Cursor CLI --model grok-4.7-high-fast，Ask只读。只有主会话收到本次I返修valid/reported_done/writesSettled=true并登记实现第3/3轮后才Resume。本单不改任何文件，不Gradle、不游戏、不spawn，不另开测试阶段。主会话已保存你的上轮完整结论review-implementation-round2.final.md。

读取TaskFile/本Prompt、记录.md最新状态、implementation-contract.md，以及原作者最新integration-repair-round2-delivery.md、integration-delivery.md。按本轮实际最终代码核实返修，不把作者报告当结论，不因为上轮已通过就套用到发生变化的路径。

先集中验证上轮七项：
1. 分身预约只归位一次，确实复用C选边，所有实际发布前调用validate，对需要移动者也覆盖当前位置偏差；失败取消预约并安全单人。
2. CONTACT触发point/direction/target真正进入每次子族显式几何，继承原接触次数与份额，不能把point只存字段不消费。
3. 领域协同费用由原报价/收据唯一承担，不是免费也不是重复扣；没有分身/预算不足/失败全单人时可选份额退原池，prepaid不能因所有值归零而没退款载荷；成功首双人仅提交一次，其后不退已提交费。自动轮不多装手动冷却。
4. record和每个目标flush都复查owner/session/world/父硬取消以及DamagePermission、同队、驯服动物、分身；旧调用默认行为不被改变，CloneSourceScope只同栈深度1。
5. 载体换手只终止未提交成员，已发/已付且owner仍合法的待结算保留；owner死亡/登出/换维/复制、世界卸载/stop/父cancel仍硬取消。延迟成员退款/账本清理不能相互误伤。
6. 双人暂存/发布失败清完整后同栈最多一次原单人完整发布；没有半提交、重复玩家体积、重复费用/预算或跨tick双人重试。
7. 等READY或确定单人后再RUNNING，两个演员同startTick/windup/recovery，分身mirrored与实际ID正确。无分身回旋即启，动作费RUNNING时提交，释放gate唯一；取消两个演员均清理，单斩不覆盖旋转。

再核实返修影响的三组入口与规则传播/资源账本/生命周期必要回归，尤其TechniqueChain新宿主规则归属不能吞协同或跨族继承。不要为增加轮次价值而整仓通读/另审无关旧功能。明确实际查过与无法静态验证事项，不把命名检查/编译当游戏效果。

这轮是本批最后一轮自动实现复核：设计1/2、实现3/3，完成后无论通过或需继续返修都停止本只读任务，不自动追加同阶段复审，不派其他会话重审。报告完整结论、必要缺陷/精确行号/原作者归属；若仍有缺陷供作者在既有授权内修复，不能以额度耗尽写成通过。

正文直接返回，不写文件。末尾唯一完整JSON：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"第3轮集中复核实际结论","changedFiles":[],"checks":[],"notRun":["未编译","未游戏验证"],"writesSettled":true,"blocker":null,"nextAction":"主会话仅按最终问题交原作者返修并转达结果；本批实现3/3已用完，不自动追加复审；写入收束满足条件后指定Cursor统一编译"}
<<<END_CURSOR_TASK_REPORT>>>
