我先读取任务文件和提示文件，按它们写明的范围做实现复核。任务是只读实现复核。我先读合同、交付说明和流程约束，再按符号核对真实调用链。合同和交付边界已经对齐。接下来按调用链核对三条入口、付款、几何和客户端，不把作者说明当成代码证据。本轮只读复核结论是必须返修。手动单人斩击、旧横劈、近垂直刀向和 `moveLimit` 这几条能对上代码；协同站位、冲刺落点、领域协同费、结算权限和出刀后的载体取消还不能按合同交付。

## 必须返修

### 1. 协同站位会被第二次错误移动，出刀也不复验（I）

分身端口已经按合同选边并开始归位。`CloneServerPorts.stanceFor` 比较左右两侧，目的地是脚点加冻结水平前向乘 0.5，再加减 `sideOffset`（`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneServerPorts.java` 516–541、615–619）。`tryReserve` 把这份计划放进预约，`advance` 沿它走到 `READY`（59–111、115–174）。

执行族在第一次看到 `READY` 时又调用 `offerReposition`（`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/FamilyExecutionService.java` 505–507、544–561）。新目的地是 `player.position()` 加瞄准右向乘 `sideOffset`，没有前向 0.5，也不看预约已经选定的左右侧。`prepare` 发现旧计划已经到达后会忘掉它，再开一段新移动（`CloneServerPorts.java` 299–317）。

`CooperationActors.validate` 只在 `CloneServerPorts.java` 199–238 实现，全仓没有调用。`reservedEye` 只在预约当下就已经 `READY` 时写下（`FamilyExecutionService.java` 436–437），需要走动的分身不会留下这个眼点，520–523 的 1 格检查也就不会发生。

复现：法杖里放协同加斜斩，附近有一具可预约的普通分身。分身先走到合同站位，接着被拉向玩家脚点的瞄准右侧。斩击从偏掉的位置打出。第二段路径不安全时，已经成功的预约会退成单人。

### 2. 冲刺接触点没有进入斩击几何（I）

`CooperativeContactBridge` 把接触点、方向和目标放进 `TriggerGeometry`（`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/CooperativeContactBridge.java` 18–22）。`onContact` 只把方向交给发布（`FamilyExecutionService.java` 942–946）。`solve` 的锚点来自玩家眼睛、领域强制锚点或 32 格空间射线（733–762），没有读取 `TriggerGeometry.point()`。

复现：冲刺加斜斩，在离开眼睛的位置碰到实体。斩击仍锚在眼睛上，接触坐标被丢掉。

### 3. 领域自动轮不收协同费（I）

手动路径会读协同魔力并在 `open` 里一次扣下，这条是通的。领域轮不是。

协同释放的 `kind()` 返回 `SELF_STATUS`（`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/CooperativeContent.java` 106–107）。`TechniqueChain.waitsForHost` 不把 `RULE_COOPERATIVE` 当成等待宿主的修饰，它会单独成段（`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/chain/TechniqueChain.java` 50–62）。`SkillChainPlan.pressCommitCost` 遇到自身状态就跳过该段（`D:/alianximod/src/main/java/com/bslx/XueXiMod/network/SkillChainPlan.java` 72–74）。`AutomaticRound.priceQuote` 用的就是这个报价（`D:/alianximod/src/main/java/com/bslx/XueXiMod/network/AutomaticRound.java` 128–133）。`launchPrepaid` 再把动作、攻击和协同份额都写成 0（`D:/alianximod/src/main/java/com/bslx/XueXiMod/network/CooperativeChainCompiler.java` 373、408、421）。

复现：领域、绑定、协同、斜斩。自动轮只收斜斩魔力，仍会预约分身并打出双人斩，协同的 2 点魔力没有付款人。`SELF_STATUS` 可以保留来让手动显式耗魔生效，领域报价必须另外计入这一份，并且不能再扣第二次。

### 4. 结算守卫不复查攻击权限（I）

`CooperativeSettlementGuard.allows` 只拒绝主人 UUID 和任意 `CloneEntity`（`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/CooperativeSettlementGuard.java` 25–37）。`active` 只看开关。账本刷新只调用 `active`（`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseBatch.java` 412–416）。命中入口 `TechniqueHitSettle.canHit` 只排除旁观者和施法者本人（`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSettle.java` 28–35）。

普通分身脉冲的边界在 `CloneEntity` 482–495：`DamagePermission`、主人的驯服动物、同队。协同体积没有走这条边界，刷新时也不再查一次。

复现：协同斜斩扫过自己的狼或同队玩家。记录时被接受，约 6 刻后仍结算。

### 5. 出刀后换手会清掉已付款、尚未刷新的伤害（I）

每刻都把载体失效当成硬取消（`FamilyExecutionService.java` 497–499）。硬取消会关掉守卫并丢掉未刷新账本（888–912）。攻击份额在体积发出时已经 `commitRetained`（688–690），取消不会退这一份。

复现：斜斩已经打出，6 刻刷新前换掉主手。魔力留在已提交份额里，待结算伤害被丢弃。合同里的载体失效只取消尚未提交的成员。

### 6. 双人发布失败后没有改发完整单人（I）

`publishMember` 在中途失败时丢弃已经进世界的体积并返回（`FamilyExecutionService.java` 680–685）。它不调用 `fallbackSolo`，也不在同一次调用里发布单人成员。服务端刻末才跑族逻辑（`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/CooperativeFamilyEvents.java` 22–27），所以这次丢弃赶在下一轮实体刻之前；下一刻仍按双人重试。

复现：双人第二具体积 `addFreshEntity` 失败。玩家那一份被丢掉，后续刻继续尝试双人，单人斩击不会补上。

### 7. 回旋只播给玩家，并且早于分身到位（I）

定时门一开始就进入 `RUNNING` 并播放回旋（`FamilyExecutionService.java` 443–448）。`playSpin` 只给玩家本人发一条 `xueximod:spin`，`mirrored` 固定为 false（765–777）。分身没有对应的开始包。玩家轨道在分身仍处于准备时就已经启动。

复现：协同、回旋、斜斩，分身需要两刻才走到站位。玩家先转，分身没有回旋轨道。双方应在分身 `READY` 后使用同一条冻结时间线；没有分身的回旋仍应马上开始。

## 已核对、未发现缺陷

- `FamilyExecutionRuntime.start` 直接拒绝（`FamilyExecutionService.java` 181–182）。手动释放、冲刺接触和领域轮都从 `open` 进入。
- 没有分身查询时先扣协同份额、预约失败再退，走 `UncommittedShare.cancel` 到 `ManaPaymentReceipt.refundShare`（`D:/alianximod/src/main/java/com/bslx/XueXiMod/network/UncommittedShare.java` 61–67）。协同钱不够但单人够时，扣款前就关掉协同（`CooperativeChainCompiler.java` 344–348）。
- 近垂直瞄准在准备前收成水平（`CooperativeChainCompiler.java` 499–507）。横劈近垂直仍朝正南（`SlashVolumeLayout.java` 141–148）。站位回退留在分身水平前向里，没有拿去改三维刀面倾角。
- 平衡键是 `selectionRadius`、`moveLimit`、`sideOffset`，由选演员、短距移动和站位消费。指南同一套键。
- 斜斩、回旋、协同、普通分身已登记；适配器只有斜斩和横劈。未适配攻击在扣费前拒绝。旧横劈、旧冲刺、跳跃不带新能力时编译器返回空，旧段继续。玩家路径不会走到会抛异常的旧 `execute`。
- 普通分身是玩家可用，法杖槽 0 单独部署，潜行只撤回开启外部协作的身体。部署付款在分身端口内。
- 每个攻击成员一本账，两名演员共用密集结算序号，刷新延迟 6 刻。`clearSettled` 保留已见轮次。来源作用域在同一次刷新里打开并关闭，深度 1。
- 斜斩有向盒用深度、刀宽和厚度三轴（`SizedBoxHitbox.java` 111–136）。动作包方向是发往客户端，姿态采样不写玩家朝向。

## 无法静态确认

没有编译，也没有进游戏。确认版 PNG 的字节没有在本轮重算。楼梯上表面、邻块是否间接加载区块、专服类加载器会不会提前解析 `handle` 方法体，都还不能从源码定成缺陷。

## 最小修复清单

全部交回 I：

1. 去掉第二次归位，或让它使用预约里已经选定的站位。出刀调用 `validate`，失败才退单人。
2. 接触子族把 `TriggerGeometry.point()` 写进显式几何原点。
3. 领域轮的原报价计入一次协同费；预付启动保持份额为 0，避免再扣。
4. 记录和刷新都复查会话、世界、主人，以及 `DamagePermission`、分身、驯服动物和同队。
5. 载体失效只取消未提交成员，已提交成员的待刷新账本继续结算。
6. 双人发布失败并丢弃暂存后，同一次调用改发完整单人。
7. 有分身的回旋等 `READY` 后，给玩家和分身同时发同一条冻结时间线。

<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"必须返修。协同站位被第二次错误移动且出刀未调用validate；冲刺接触点未进入斩击几何；领域自动轮因SELF_STATUS跳段不收协同费；结算守卫未复查DamagePermission、同队和驯服动物；出刀后换手会丢掉已付款未刷新伤害；双人发布失败不改发单人；回旋只播给玩家且早于分身READY。手动扣费退款、moveLimit、近垂直刀向和旧路径分流已核对。未编译，未进游戏。","changedFiles":[],"checks":["三条入口与start拒绝","协同预约站位与validate调用","接触几何","领域报价与预付份额","结算守卫与6刻刷新","载体取消","双人发布失败路径","回旋动作包","配置moveLimit与近垂直刀向","旧execute是否可达"],"notRun":["未编译","未游戏验证"],"writesSettled":true,"blocker":null,"nextAction":"由主会话按本轮缺陷交回原作者，不自动开始返修后复审"}
<<<END_CURSOR_TASK_REPORT>>>
