我先完整读取任务文件和提示文件，按它们写明的范围执行。任务只收集分身底座和可复用协作接口的既有事实。我先读本批记录和直接相关代码，不改文件、不编译。分身底座目前只有三类身体和一条可执行攻击。玩家平时只能通过法杖放出水幻形；普通分身和幻影分身基础都停在开发命令。没有短距归位动作，分身伤害也不进玩家的合击账本。

## 玩家实际能得到的分身

`TechniqueAvailability.PLAYER` 的只有 `water_phantom`（水幻形）。`clone_phantom`、`clone_body`、`clone_response_pulse` 注册为 `DEVELOPMENT_ONLY`。`TechniqueAccess.allowsPlayerUse` / `playerCatalog` / `skillAllowed` 会把这三项挡在玩家目录和技能之外。

水幻形入口是 `SkillUsePacket.routeCapture` → `ActorCaptureDispatcher.tryExecute(ServerPlayer, Skill, CapturedStructurePlan, CapturedInput)`。条件是法杖、槽 0、链上先是水幻形再接一段已适配攻击。当前 `ActorPayloadRegistry.registerBuiltins()` 只登记 `TideSeverActorAdapter`（`tide_sever`，version 1）。普通释放留下身体；潜行时 `immediate=true`，诱敌半径强制为 0，部署后立刻进入反击。同一主人同一服务器游戏刻只能部署一次（`CaptureAcceptanceLedger`）。

普通分身和幻影分身只从权限 2 的 `/clone spawn normal|phantom` 进入 `CloneService.spawn(ServerPlayer, CloneKind)`。效果执行器直接抛 `DEVELOPMENT_ENTRY_ONLY`。`spawn` 还会再核对这两项以及脉冲仍是 `DEVELOPMENT_ONLY`。

## 规格和响应预算

开发幻影：`ResponseBudget.ONE`，花掉后结束，触发 `MANUAL_AND_HURT`，结束策略 `BUDGET`，表现 `LEGACY_HUMANOID`，动作键 `clone_response_pulse`。开发普通分身：`REPEAT`，花掉后不结束，其余与幻影相同。两边生成报价默认魔力 10、冷却 20 刻、寿命 0（不自然到期）、生命 20、诱敌半径 8。脉冲报价默认魔力 2、冷却 20、固定伤害 2、射程 8；伤害只走一次 `BalanceService.damageContribution`。

水幻形身体按 `CloneKind.PHANTOM` 发布，但规格是另一套：动作键就是 `water_phantom`，预算 `ONE`，触发 `CONTACT_OR_HURT`，结束策略 `BREAK_ON_TRIGGER`，表现 `WATER_OUTLINE`，生命在部署时固定为 1。魔力在部署时一次收取「水幻形魔力 + 断潮魔力」；体积结算不再扣费。默认寿命 0、诱敌半径 8。

主人上限 4，含已脱离身体的水幻形租约；服务器上限 128。每具身体同时只能挂 1 条请求，服务器队列 128。每 tick 最多提交 32 次，开发队列先用，剩余才给水幻形票据。

## 调度、生命周期、重入

`CloneLifecycleEvents.onServerTick`：`START` 调用 `CloneRuntime.advanceTick()`，`END` 调用 `drain()`。`drain` 顺序是到期、主人巡查、冷却恢复、水幻形接触、开发响应队列、水幻形票据、体积、诱敌、清理空主人记录。请求在下一单调 tick 执行，寿命 2 tick；队头未到点就停，不跳过。主人巡查和诱敌按 `staggerSlot % 20` 错开。诱敌每 tick 最多查 4 具。

取消和结束：`dismiss` / `dismissAll`、登出 `OWNER_LOGOUT`、主人死亡 `OWNER_DEATH`、换维和玩家复制 `OWNER_DIMENSION`、实体离维 `UNLOAD`、世界卸载 `WORLD_UNLOAD`、停服 `SERVER_STOP`。存档读入的分身直接丢弃，不恢复。水幻形租约在登出、主人死亡、换维、世界卸载、停服、主动撤回时不保留；身体已经交成票据后，其它结束原因会把脱离单位继续算进上限。票据过期是 `TICKET_EXPIRED`。体积在主人死亡、换维、区域未加载时结束。

重入：`CloneSourceScope.blocksResponse()` 在来源为 `CLONE_RESPONSE` 或深度大于 0 时为真。开发 `perform` 和水幻形体积分次伤害都会打开深度 1 的该作用域。此时再 `request`、受击、`breakForResponse` 都会拒绝。`sourceDepth > 0` 同样拒绝。每具身体只有一个 `pending`。

## 可接攻击和朝向

动作接口是 `CloneAction.prepare(CloneActionContext)`，准备不扣费、不改次数。登记门禁是 `CloneActionRegistry.admit(actionKey, required)`。`CloneService.perform` 固定要求 `CloneActionCapabilities.full()`：动作原点、动作方向、主人资源、不碰主人身体、不碰主人手、抑制广播、延迟隔离，七项都要为真。当前唯一登记动作是 `ClonePulseAction`，键 `clone_response_pulse`。

`CloneService.request(ServerPlayer, CloneHandle, CloneTriggerInput)` 只对 `MANUAL_AND_HURT` 成立。水幻形是 `CONTACT_OR_HURT`，`CloneTriggerPolicy` 直接返回 `UNSUPPORTED_ACTION`。它的反击是 `CloneService.breakForResponse` → 票据 → `TideSeverActorAdapter.instantiate` → `ActorVolumeRuntime.submit`。选择现有身体只有 `list(ServerPlayer)`、`status`、`handleFor(ServerPlayer, UUID)`，按主人索引和 UUID，没有按距离自动挑选。

`CloneSpec.Station` 只有 `STATIONARY`。移动速度属性是 0，没有寻路目标。`moveTo` 只出现在生成。`CloneEntity.face(Vec3)` 只改水平角、俯仰、头和身体朝向。`ClonePositionPolicy.frame` 用当前眼睛位置加调用方传入的单位视线，或受击者中心方向，不移动身体。没有短距归位动作。

## 和玩家攻击结算的关系

开发脉冲提交一次 `TechniqueHits.classifyRespecting(target, owner, amount, TideMarkSuccess.NONE)`，伤害源是 `playerAttack(owner)`，金额是准备时冻结的。`CloneActionResult` 只记录裁决，不退款、不恢复次数。幻影这一次之后结束；普通分身进入冷却再回到 `READY`，不把伤害乘人数。

水幻形体积分次调用同一个 `classifyRespecting`，金额用部署时冻结的断潮快照。每个目标在该体积生命周期里只尝试一次，上限 64。不调用 `ReleaseBatch.openGroupTally()` 或 `Tally.record`。因此分身人数不会自动乘进合击账本；若玩家自己的攻击再打同一目标，就是另一次 `hurt`，受原版无敌帧约束。水幻形接触在 `invulnerableTime > 0` 时不触发破裂。

权限：`DamagePermission.allows(ServerPlayer, LivingEntity)` 排除自己、旁观、无敌和不能伤害的玩家。`CloneEntity.rejectsPulseTarget` 再排除分身、主人的驯服生物和同队。分身不能打分身，也不能以另一具分身作为受击来源。

## 姿态和视觉

服务端同步的是种类、主人、生命、响应、动作序号、世代、会话、表现键和部署标识。旧分身客户端用僵尸人体和僵尸贴图；`actionSequence` 变化时挥主手。水幻形不画僵尸，由 `CloneRenderer` 调用 `SteveWaterLineVisual.alignVanillaSteve` 再 `drawPhase(..., residencePhase)`。散开由 `ClonePresentation` 通知，客户端 `CloneAfterimagePlayback` / `CloneOutlineVisual` 用 `scatterPhase` 画，身体在非 `ACTIVE` 或散开隐藏时不画。

`SteveWaterLineVisual.drawPhase` 当前顺序是先 `drawSkin(SKIN_TYPE)`，再 `drawFlakes(FLAKE_TYPE)`。两种 `RenderType` 共用同一个 `BufferBuilder`，这个顺序不能倒回去。

## 必须交 Astra 决定的问题

1. 协同要选的是玩家可持有的水幻形，还是开发命令里的普通/幻影分身。现成查询没有“近处合适分身”。
2. 水幻形不能走 `CloneService.request`。指挥它要新调度，还是只复用 `list` / `handleFor` 和 `face`。指挥会消耗它仅有的一次破裂预算；现在没有“先试探、失败不占预算”的接口，`request` 一接受就进入 `RESERVED`。
3. 短距归位没有现成动作。新动作若要走现在的 `perform`，必须声明全部七项能力。是否允许只转分身、不转玩家相机，代码里也没有转玩家相机的分身接口。
4. 一次合击一次命中：现成分身伤害是独立的 `classifyRespecting`，不进 `ReleaseBatch.Tally`。要合进玩家那一记，还是允许各打各的并受无敌帧影响，需要定。
5. 协同、回旋、斩击的新参数名和生效时机。本任务不新增配置键。
