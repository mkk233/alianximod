我先完整读取这份实现复审任务说明，按其中授权范围执行，并在结束时交回完整终稿。任务是实现第 1/3 轮只读复审。我先核对批次目录、最终报告和源码时间戳，确认写入已停后再对最终代码做集中核对。所有权与批准快照已对齐。接下来对照合同和最终代码，先留下源码时间戳与哈希，再核对注册、法杖和关键结算路径。## 结论

**有阻断。** 13 项和测试法杖都已注册，能进刻印、装配和释放编译；基础法杖仍是 6 槽。阻断在四条已批准的执行路径上，不是缺登记。本轮没有编译，也没有进游戏。

源码在复审期间未改。结束时 `TechniqueHitSettle.java` 的 SHA256 仍是 `dcda2da953795405a667f9fdc471b03852453457a09dd575635b36add998a5a1`（2026-10-31 04:59:01），`BatchCastSession.java` 仍是 `30057ffdf005c6fe679672d41c9b8d2e9cc74c8c020893ac36328e10bf9b6625`（04:59:40），与作者 A 停写记录一致。陨石贴图 32×32，九张竖条均为 64×1024。

## 阻断

### 1. 留焰与本体不是一份命中

作者 **A**。`TechniqueHitSettle.sweepEarliest` 先结算再占钟。

```229:237:src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSettle.java
        com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict verdict =
                TechniqueHits.classifyRespecting(target, caster, hitEntity.settleDamage, hitEntity.settleTide);
        if (verdict == com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.INVALID_OR_CANCELLED) {
            hitEntity.rejectedTargets.add(target.getUUID());
            return sweepEarliest(hitEntity, level);
        }
        if (hitEntity.hitClock != null
                && !hitEntity.hitClock.tryMark(target.getUUID(), level.getGameTime(), hitEntity.hitInterval)) {
            return false;
        }
```

火苗、陨岩走这条最早扫掠。留焰在 `BatchFlameTrail.sweep`（约 125–128 行）先用同一只 `HitClock` 打出宿主伤害。之后本体再碰到该目标时，伤害已经打出，`tryMark` 失败只 `return false`：不加入拒绝名单，不撞毁，不接续，不产种。该目标仍是最早接触时，之后每一刻再打一次。

火球更直接。`FireOrbEffect.attachTrail`（71–90 行，作者 A）把同一只钟交给留焰，但 `tryHit`（44–54 行）只看 `hitOnce`，不看这只钟。留焰和火球各打一次。

触发：火苗、陨岩或火球带留焰，轨迹先擦到目标，本体随后再碰到同一目标。后果：违反「本体与火径合计一次」，并可能卡住接续和产种。

最小修复：在 `classifyRespecting` 之前用共享钟判断。已经占钟的目标跳过且不要再结算；扫掠应继续看其余候选，不能停在这个目标上每刻重打。`tryHit` 对带 `hitClock` 的火球同样先占钟。留焰侧不要改成第二套去重。

### 2. 热波当接续父段时，本组不再结束

作者 **A**。热波在 `execute` 里当次判定后立刻调用 `follow`（`BatchShockRingEffect` 69–70 行，作者 C，这次立即调用符合「判定一次」）。父段闩锁却在 `spawn` 返回之后才加：

```181:193:src/main/java/com/bslx/XueXiMod/network/BatchCastSession.java
            BirthLayout.install(slot);
            ReleaseOutcome outcome = spawn(birth.segment, birth.entityLock, birth.dropPath, frozenPoint, frozenDirection);
            if (outcome != null && outcome.committed()) {
                budget.commit(birth.hostPrice);
                if (birth.retainedPrice > 0) {
                    budget.commit(birth.retainedPrice);
                }
                // ...
                if (birth.child != null) {
                    openLatches += 1;
                    int timeoutTick = owner.getServer().getTickCount() + birth.lifetimeTicks;
                    ChainScheduler.schedule(owner.getServer(), timeoutTick, () -> expireLatch(birth));
                }
            }
```

`onParentVerdict`（207–211 行）在 `openLatches` 仍是 0 时就把闩锁减掉，并写入 `continued`。返回后闩锁变成 1。寿命回调看见 `continued` 已有记录就直接返回，不再减。`maybeFinish` 要求闩锁为 0，于是 `finish` 不再执行：余款不退、冷却不开始、`CastCooldown` 不释放、根上下文的会话持有不释放。

触发：热波后面接「命中接续」和任一子攻击，且热波至少打出免疫或正伤。陨岩、火苗的接续发生在飞行命中之后，这条时序本身是对的。

最小修复：有子段时，在调用 `spawn` 之前把该父份闩锁加一。`spawn` 内部已经接续的，回来后不要再加。

### 3. 引燃单次最多 10 档，不是 8 层

作者 **B**。`KindlePlan.specify` 34–36 行把 `n` 封在 `FireLimits.TIER_CAP`（`FireLimits.java` 16 行，值为 10）。余额 100 时 `n=10`，期限 `40+10×10=140` tick，段数 14。批准是 `n=floor(余额/10)` 且最多 8 层，8 层期限应为 120 tick。

`PLAN_CAP=8` 只限制目标身上同时有几条轨道。`FireConsumption.scheduleKindle` 51–52 行在已有 8 条时不挂、不占声明，这只覆盖「再挂一条」，不把这一条引燃从 10 档收成 8 档。首跳等待整段 10 tick、每段请求 10、伤害 `2×(1+启动 F)`、`F` 取 `FurnaceService.fireBonus`、不足按份额、余数走 `RemainderRotation`，这些与批准一致。

触发：目标火种余额至少 90，挂上一次引燃。后果：多 1 到 2 档，持续时间长于 120 tick。

最小修复：只把新引燃的档位封在 8。不要改 `TIER_CAP`，旧燃烧仍用 10。

### 4. 地表净空没有按热波真实外形量

作者 **A**。收费前拒绝本身是对的：`BatchCastSession.open` 91–96 行先 `SurfaceAnchor.resolve`，失败则在 `GroupBudget.reserve` 之前返回。量法不对。`BatchReleaseCompiler.halfExtent` 330–334 行用 `max(0.5, 厚度×0.5)`，子段再用 `child.radius×0.5`（379–382 行）。热波厚度和子半径都是外半径 5，净空半宽变成 2.5，高度也用这个数再加 0.125。批准是总高 3 的半高 1.5 加 0.125，并按最终出生几何的全部水平范围检查。

触发：地表定位加到热波上，或热波是子段。半径 2.5 到 5 之间有碰撞块时仍会通过并扣费。火苗还会被 `max(0.5, …)` 抬高，比 0.25 碰撞盒的半高更严。

最小修复：每个出生体按自己的半高加 0.125 做竖向净空，按最终水平半径做横向净空，再取整组最大值。热波用外半径 5、半高 1.5。

## 另外一处明确缺陷

非法杖载体换装配不会取消未出生份。作者 **A**。`BatchCastSession.stillValid` 322–339 行只在法杖上比对 `StaffSkills.signature`。剑槽签名是空字符串，主手物品种类没变就继续出生并提交。松键只停 `HoldRepeat`，不取消本组，这是对的。死亡、下线和换维会 `cancelOwned`。

触发：剑槽上的火苗加错时，延迟出生前改该槽装配。未出生份仍按旧计划打出。

最小修复：非法杖也保存释放当时的槽签名；签名变化时走现有取消，只停未出生份和未触发子段并退未提交金额。

## 已核对、不构成阻断

- 13 项都在 `ModTechniques.registerAll` 和 `ReleaseDefinitions.registerAll`。六个附魔是 `ChainRole.NOT_APPLICABLE`，魔力 2、直接伤害 0，不是假 ATTACK。地表定位、喷涌、错时、命中接续角色分别是 `SEEK_SURFACE`、`TRAJECTORY_UPWELL`、`RULE_STAGGER`、`RULE_HIT_CONTINUE`。火苗、热波、陨岩有载体效果；幽冥登记 `GloomDomainEffect`，不生成攻击体。
- 基础法杖 `StaffData(6, 0.20, 0.10, 100, null)`。测试法杖 `StaffData(32, 15.0, 0.50, 1000, null)`。回魔是 `REGEN_AMOUNT * (1+15)`，总倍率 16，不是 17。备用上限只在这把法杖上。剑技仍是 4 条、`Skill.CHAIN_LIMIT=8`。装配界面测试法杖 8 列，超 32 的旧列表进保留字段并拒绝施放，不删尾部。
- 新报价由 `declaresBatchQuote` 打开。父复制在关闭父段时清掉，不传给子。附魔价只进首份。三陨岩（48×3）加各一发热波（8×3），再加崩地、引爆、引燃各 2，代码路径是 174。未跑数值。出生才 `commit`，结束才 `settle` 原收据。取消令牌停未出生份。
- 一次释放一个 `FireCastContext`。`release` 只在持有数到 0 时摘掉。父子各一只命中钟。资源只在 `DAMAGE_ACCEPTED` 时提交。本体基数与种火基数相加后再 `floor(基数×seedMultiplier)`，火球旧产种点仍是 3，热光束仍是 1。
- 实体取消返回 `notHandled`，不扩圈、不占钟。`PENDING` 和取消在 `onResolved` 直接返回。免疫可以接续。超时走无伤残骸，不接子。本批出生是逐份提交，不把接续押在 6 tick 的 `Tally` 上。`whenComplete` 没有被批量会话注册；当前这 13 项的出生参数没有打开共享 Tally。
- 留焰直径 0.5、采样 0.25、上限 256、寿命 40，且不自己产种、不接续、不崩地。陨岩 24 / 0.8 / 上仰 15 度 / 重力 0.08 / 100 tick。火苗 1 火伤、产种基数 2、0.8、30 tick、0.25 碰撞、点燃 20 tick。热波物理 4、内 1.5 外 5、总高 3、显示 12 tick、打完恢复原速度、父目标放进中心排除。
- 喷涌单份竖直、多份 25 度，与抛物线、垂落、涡旋冲突则编译拒绝。错时 `10×序号`，寿命从出生计。接续一层，超过 64 实例拒绝。父附魔不进子段索引。
- 产种、引燃、引爆顺序在 `ResourceHitCommit`。引爆是 `0.2×当时余额×(1+当时 fireBonus)`，去重键独立。普通武器一次攻击提交后摘掉载荷，不 `adopt`。
- 幽冥满 20 tick 后基数 5 再乘产种倍率，同一窗口只入最高的一笔，不看接触伤害。蓝色只在 `NetherDomainTint.sample`。
- 崩地浅碗按 `0≤d≤2×(1-r²/4)` 生成 23 块，上限 32。破坏和安放分开；开关默认开，只挡破坏。取消的破坏事件不改块。主人不在线不冒充。陨石硬度 3、抗爆 6，石镐标签和自身掉落在生成资源里。残骸有独立标识，未加载不强载，20 tick 后再试。

## 13 项与测试法杖

| 项 | 映射 |
| --- | --- |
| 测试法杖 | 可交付。32 槽、字段 15.0、冷却 0.50、备用 1000，基础法杖未改 |
| 留焰 | 阻断。与火苗、陨岩、火球不能合成一份命中 |
| 火苗 | 单独可交付。带留焰时随阻断 1 |
| 陨岩 | 单独可交付。带留焰时随阻断 1。抛物线、中心排除、超时残骸已接上 |
| 热波 | 单独可交付。作为接续父段时随阻断 2 |
| 地表定位 | 收费前拒绝已接上。与热波组合的净空随阻断 4 |
| 喷涌、错时、命中接续 | 可交付。接续父段若是热波，父段会话被阻断 2 卡住 |
| 种火、引爆 | 可交付 |
| 引燃 | 阻断。单次档位到 10 |
| 幽冥、崩地 | 可交付 |

## 运行时仍未验证

未跑 `compileJava`，未进游戏，未做数值试算。下面这些只读代码看得到，还没有运行证据：174 的实际扣费和部分取消退款；32 槽重进后的最后一格；保护插件取消破坏或放置；残骸卸载再加载只安放一次；留焰修复后的同目标只伤一次；引燃 8 层在共享余额里的末跳和余数；幽冥重叠窗口只发最高一笔，以及远处客户端离开蓝区后恢复。不能把这些写成已经游戏验收。

本轮只读，没有写入源码。
