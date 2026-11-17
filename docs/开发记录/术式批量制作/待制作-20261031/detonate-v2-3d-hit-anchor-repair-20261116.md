# 引爆 v2-3D 命中锚点修正

- batchId：`待制作-20261031`
- taskId：`fire_detonate_v2_integration`
- attemptId：`20261116-impl-detonate-3d-02`
- 关联 B：`fire_detonate`
- 直接父会话：`01a1ce06-b681-7c90-b11d-260d3c169911`
- 绑定版本：`fire_detonate_v2_3d_candidate_20261116`
- 上一回执保留：`docs/开发记录/术式批量制作/待制作-20261031/detonate-v2-3d-integration-20261116.md`
- 本阶段额度已用完，返修结果未追加复审。设计 2/2、实现 3/3 不变。本轮只补挂点，不重审 26 团参数。

## 修正

上一轮 `showSpent` 固定取 `target.position()`，没有接收载体接触点。现只在 `FireSeedState.draw` 已经返回且 `spent > 0` 后播放。有限接触点按传入坐标冻结，不加 Y。

`TechniqueHitSettle.sweepEarliest` 已有 `contactPoint`：`contact.point()` 非空时就是扫描接触点；为空时沿用该方法原有的载体当前位置，不是新做的表面交点，也不是统一抬高。该值现经 `ResourceSettlement.afterVerdict` 和 `ResourceHitCommit.commit` 传给 `DetonatePlan.strike`。交接事件点、跟随、伤害、去重和消费都没有改。

外层文件必须改的原因：`DetonatePlan` 只被 `ResourceHitCommit` 调用，接触点不经这次提交就到不了视觉。`ResourceSettlement` 是扫描结算进入该提交的唯一入口。

## 位置来源

- 实际接触：最早接触扫描。陨石实体拦截会返回未处理并继续这条扫描，所以被直接扫中的目标使用同一接触点。
- 回退 `target.position()`：非扫描的命中盒、点名接触、弧带、持续流、陨石体积内其余目标、热波环、武器附魔、领域脉冲、原版近战并入、延迟汇总账。这些路径没有逐目标碰撞点。陨石体积中心和热波环心不冒充每个目标的碰撞位置。回退是目标当时脚底，不是精确碰撞位置。

## 剩余限制

非扫描命中仍无逐目标交点。扫描接触点为空时用的是既有载体位置。视觉比例、贴图和 26 团参数未改。未编译，未启动游戏。

## 检查与指纹

实际执行 `powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 1。本次四个文件为零处；其余为既有命名问题，未改。未跑 Gradle。无下属代理。

- `DetonatePlan.java`：`642E74317C4434D8097A88EC47EC79DF63114BE5C92F01B85E19A685FD4817A1`
- `ResourceHitCommit.java`：`8832A1F2A689DE0F6EC10A686C1545B6EB7265C66C94B480FB47FD0F07D7633E`
- `ResourceSettlement.java`：`320AFC05E55BD862E8A38A0895DEF8925CBE83B98F2E09AE2E0047E7B2E3399F`
- `TechniqueHitSettle.java`：`A70AD86E80CB9EB76C30E95B53D9B99ABAFA69A4EFD802E3054A0977FA0F8C40`

写入已收束。状态仍是待统一编译。
