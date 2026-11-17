我先把这份任务说明完整读完，再按里面的范围做只读核对。用户决定是从装配链指定已适配的水系攻击。接下来只核对断潮、缺月的命中体字段，以及分身结束、收费和延迟票据。用户决定是从装配链指定已经适配的水系攻击，不走固定水刃分支。任务里的「首批断潮 + 缺月、触碰后先消身体、下一 tick 用独立票据反击」仍是未批准的理解问题。按现有源码，这两条命中体和分身队列都没有这套原始能力。

## 断潮与缺月怎样生成

两条都在效果的 `execute(ServerPlayer, Technique, ItemStack, TechniqueCastContext)` 里当场 `new TechniqueHitSpec()`，然后调用 `TechniqueHitSpec.spawn(ServerLevel, ServerPlayer, TechniqueHitSpec)`。没有 builder。

断潮（`TideSeverEffect.execute`，32–51 行）写成静止斩：`path = STILL_AIM`，`speed = 0`。纵深和高度来自专项 `beamLength`、`boxHeight`，缺省 5 和 3；宽度用释放定义厚度，登记值 0.4；寿命用定义的 6 刻。锚点是 `player.getEyePosition()`，方向是 `cast.getLook()`。贴图是 `xueximod:textures/entity/xueximod/horizontal_slash.png`。`listener.onHit` / `onExpire` 把 `cast.onFirstTrigger()` 最多叫一次。`requiresHeldSword()` 为假。

缺月（`WaterFormEffect.execute`，26–94 行）先按厚度 0.35、长度倍率 3.2、宽度倍率 0.7 写盒子，再因为 `swordForm()` 带弧带，写入 `ArcBandDraft.radialBlade(beat, arcBandRadius(), arcBandHalfWidth())`。半径和半宽的接口默认是 2.4 和 0.16（`ReleaseDefinition` 89–95 行）。拍号只在当前 `HostGroupContext.hasBeatCandidate()` 时取，否则是 0。`swordForm()` 同时打开持剑、宿主组提交、拍号循环、分道视觉和弹反。`WaterBeatBridge.capture(ServerPlayer, int)` 读主手和选中槽。

`spawnReported` 在生成前改这份 spec。`CastFrame.current()` 不为空时：

- `suppressRootSettle == false` 就把 `frame.damage`、附魔、`tally`、复制号、火种、燃烧、控制、灭火、击退、潮印抄进 `settle*`。调用方事先写的 `settleDamage` 会被盖掉。
- 体积倍率乘长宽高和模型，弧带除外。速度倍率大于 1 就乘速度。
- `overrideGeometry` 时按弹道改 `start`、`anchor`、`aim`、路径。弧带烘焙优先用帧原点，否则用 `spec.start`，再否则用循环外采样的施术者眼睛和视线（`bakeArc`，573–590 行）。没有自动来源时，采样就是 `caster.getEyePosition()` 和 `caster.getLookAngle()`。
- 帧为空时不抄上述结算和几何。`start`、`anchor`、`aim`、`lifetime` 保留调用方的值。弧带仍会采样施术者眼睛。

份数：有宿主组用 `hostGroup.spawnCount()`，否则 `SkillEntityCount.entityCount()`，默认 1。寿命经 `instanceLifetime(template, releaseInput, arcBand || fixedLifetime)`。弧带或 `fixedLifetime` 不吃释放时长倍率。缺月走弧带，断潮会吃当前帧的 `ReleaseInput`。

显式字段都是 public，没有权限字段。和几何、伤害、寿命直接相关的是：`start`、`anchor`、`aim`、`sourceOrigin`、`sourceDirection`、`launchAim`、`localOffset`、`settleDamage`（默认 NaN）、`settleTide`（默认 `NONE`）、`settleEnchantments`、`settleTally`、`respectLanding`（默认 false）、`suppressRootSettle`、`lifetime`、`fixedLifetime`、`automaticLifetime`、`endsWhenReleased`、`releaseSlot`、`listener`。

## 旧命中会接上什么

结算在命中体之后的 `TechniqueHitSettle.tryHit`，不在 `spawn` 里。

- `settleDamage` 为 NaN：不调用 `settle`，没有伤害、领域和潮印。
- 有 `settleTally`：只 `record`，返回 `PENDING`，伤害留给汇总，不在这里打。
- 没有 tally、`respectLanding == false`、施术者还在：`TechniqueHits.classifyRoot`。它先 `DomainAuraResolver.merge(amount, player, ROOT_TECHNIQUE)`，命中时按施术者所在领域加一次最大附加，再 `classifyHit(..., BYPASS_INVULNERABILITY, tide)`，清掉无敌帧后 `playerAttack(player)`。潮印用抄来的 `settleTide`。
- `respectLanding == true`：`classifyRespecting`，保留无敌帧，不在这里做领域合并，仍把 `settleTide` 交给 `TideMarkDamageScope`。
- `HitPayloads.attach` 在帧上有治疗、牵引、潮湿、灭火或抬升时挂上。湿压在定义要求且帧存在时挂上。`listener.onHit` 和 `batchFollow.onVerdict` 是命中后续。一分为三的反馈击退走 `ChainScheduler.schedule`。
- 施术者 UUID 等于目标时 `canHit` 直接跳过。这条路径不调用 `DamagePermission.allows`。分身脉冲才调用它。
- 非自动寿命时，施术者为空或不活着，`releaseStopped` 结束命中体。缺月还要求主手仍是剑。

没有按来源或能力把派生命中体隔出旧链的入口。`suppressRootSettle` 只停抄 `settle*`，不停体积倍率、几何覆盖、宿主组、湿压和 `HeavyWater.commitFirst`。它也不防止调用方把已经乘过 `BalanceDisplay.damage` 的金额再乘一次。`classifyRoot` 本身不再乘伤害倍率，只加领域附加。帧存在且未抑制时，生成会加入当前 tally 和潮印。

因此不能把现有 `execute` 或 `spawn` 说成可由分身几何独立调用。

## 身体消掉之后，队列和收费还在不在

`CloneActionRequest` 只放在 `CloneRuntime` 的队列里，并记在 `CloneRuntimeState.pending`。它带句柄、代次、攻击者或视线，不带术式、资格、伤害或收据。

`endActor`（334–358 行）先把 `prepareCancelled` 设真，再 `cancelPending`，从主人索引和 `actors` 删掉，然后 `discard()`，`state.actor = null`。`cancelPending` 仅在 `pending != null` 且没有 `executing`、没有 `commitLine` 时，按 `requestSerial` 移出队列（371–376 行）。代次不会变成还能兑换的票；`find(actorId)` 随后是空。

`pumpQueue` 在服务器 tick 末尾跑。过期则 `CloneService.cancelQueued`；`executeTick` 还没到就停住整条队列。`perform` 用 `actors` 里的状态。状态没了、实体已移除或不活，在扣费前就失败。取消令牌是 `ServiceCancel`，只读 `state.prepareCancelled`。`prepare` 期间结束会让它变成已取消；跨过提交线之后，`cancelPending` 不再撤队列。

没有能脱离身体活到下一 tick 的行动票类型。`ManaPaymentReceipt` 只活在 `spawn` 或 `perform` 的局部变量里。`charge(player, cost, tellPlayer)`：费用小于等于 0 返回零额已提交收据且不扣；扣失败返回 null。`refund` 只退同一玩家、状态仍是 `COMMITTED` 的余额，退完变成 `ROLLED_BACK`。分身代码不调用 `retainShare`，也不把收据放进请求。

生成顺序是：校验、占位、读报价、分配代次、`charge`、放入世界、`publish`。未 `publish` 的失败在 `finally` 里释放占位并退款；世界放入失败或索引失败会先退一次。冷却只在发布成功后装上。代次即使失败也已经加一。响应费更晚，在 `prepare` 接受之后、提交线之前；提交前失败退款，提交后不退。

`CloneEntity` 没有重写碰撞、推动或 `playerTouch`。唯一捕获是 `hurt`：先 `super.hurt`。已经死亡就 `endLoaded(DEATH)` 并返回，不会 `onAcceptedHurt`。非致死且伤害被接受才入队。入队的请求仍指向这具身体；下一 tick 前如果 `endActor`，队列项被取消，不会留下独立反击。

## 设计者还要明确的事

- 用户决定只写「适配过的水系攻击」。断潮和缺月是否就是首批准入，以及准入写在哪，决定文件里没有。
- 链段冻结要保存术式、资格、已乘一次的伤害和几何。现有请求和 `CloneSpec` 都没有这些字段。
- 生成费和响应费现在是两次 `charge`。没有「部署时一次收两笔、身体先消失、票据再打」的收据。
- 身体销毁会取消未提交队列。若反击必须在身体消失后的下一 tick 发生，现有票据不够。
- 主人离线或换维会结束身体并取消未提交请求。没有一张可以单独作废的延迟票。
- 命中体要不要进当前帧的 tally、潮印、领域和无敌帧绕过。现有开关只有 `suppressRootSettle` 和 `respectLanding`，而且帧存在时还会改几何。
- 缺月沿用现有效果时仍看主手剑和宿主拍号。断潮锚点仍是施术者眼睛。这两处是否改为分身几何，要单独定。
