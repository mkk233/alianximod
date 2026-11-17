我先读任务说明和调度规范，确认这次授权范围后再动手。这是只读定向核查：先确认没有其他源码写入或 Gradle 在跑，再对相关源码做哈希并顺着真实消费路径取证。进程是空的。消费入口已经对上 `FireSeedState.consume`，接下来核对账本、三式接线和设计里的扣款时机。相关源码的 class 不比源文件旧，也没有别的 Java 进程。接下来用现行账本类做余额复现，并重跑已有的纯账本检查。持续燃烧会实际扣火种。立即引爆会实际扣火种。两者都在成功提交时走 `FireSeedState.consume`，由 `FireSeedAccount.consume` 减目标余额，再写入实体上的 `fire_seed`。持续燃烧是挂载前扣一次，不是每段再扣。引爆的火伤来自这次实扣快照，没有“不扣款却按火种加伤”的分支。

这不是“目标燃烧期间禁止引爆、并暂停所有产种”。当前只有同一本施法账的一次消费，以及燎原按施法者、焚天按效果键的排他。燃烧进行中，新的施法上下文仍可补火种并立刻引爆。这是尚未授权的新策略差距，不是三式漏扣，也不表示现有账本测试全部失效。

完整报告在 `docs/开发记录/火种双路线消费核查-20261029/report.md`。结论如下。

## 真实扣款链

1. `FireConsumption.schedule`（23–45 行）或 `detonate`（74–88 行）先做宿主、冲突或分量预检。
2. 通过后调用 `FireSeedState.consume`（`FireConsumption.java` 46 行、89 行）。
3. `FireSeedState.consume`（41–50 行）把属性状态交给 `FireSeedAccount.consume`（41–48 行）减余额并 `markConsumed`，再 `persist`（88–91 行）。
4. 余额有变化时，`AttributeStateService.write` 写到实体持久数据 `xueximod_attribute_states` / `fire_seed`（`AttributeStateData.java` 8–33 行）。0–100 的成功扣除一定会变小，因此会写。拒绝时余额不变，正常值不写。
5. 持续燃烧再用这次快照挂上轨道（`FireConsumption.java` 50–70 行）。以后每段只打封存伤害（`BurnDispatch.java` 26–46 行，`BurnCarrier.java` 23–35 行），不再读、不再扣火种。
6. 引爆用这次快照打一次（93–104 行）。`strikeOnce` 失败也不退款。

预览不减余额、不标记、不写存储（`FireSeedState.java` 36–38 行）。`Integer.MAX_VALUE` 表示全部余额。燎原、焚天、燎里传的就是这个全部消费（`FireConsumerPlans.java` 18 行），不走整档。

## 余额

夹具对真实 `FireSeedAccount.consume` 的结果：

| 起点 | 整档扣掉 | 整档剩余 | 全部消费扣掉 | 全部消费剩余 |
| --- | --- | --- | --- | --- |
| 0 | 不扣、不标记 | 0 | 不扣、不标记 | 0 |
| 9 | 不扣、不标记 | 9 | 9 | 0 |
| 10 | 10 | 0 | 10 | 0 |
| 19 | 10 | 9 | 19 | 0 |
| 99 | 90 | 9 | 99 | 0 |
| 100 | 100 | 0 | 100 | 0 |

按当前三式，100 在 `consume` 返回时已经是 0。10 点燎原再跑完 4 段、80 步，余额仍是 0，段伤保持 2.2。100 点焚天 10 段、40 步打完，余额仍是 0。80 点引爆的火伤是 28.8，100 点是 36.0，都来自实扣点数。

## 拒绝和退款

账本上已执行且不扣款的情况：余额 0、请求 0、负数、非数或负炉心、9 点整档、0 点三式无计划、非法效果键、同对象第二次消费。19 点先扣 10 后剩 9，同一对象不能再拿这 9。第 9 条燃烧轨道挂不上，另一份 50 点不动。

死亡、旁观、客户端和非服务端主线程在 `FireConsumption.hostReady`（107–117 行）和 `FireSeedState.actionable`（94–104 行）于扣款前返回。这两条没有实体，没有跑进 `FireConsumption` 本身。

扣款成功后的免疫和死亡按设计不退款（`docs/火属性二级玩法设计.md` 48 行）。死亡只会把剩余火种写成 0，不会把已花掉的点数补回。

条件性缺口：`schedule` 在 46 行、`detonate` 在 89 行已经写存储之后，若最终计划或分量被拒绝，52–67 行和 95–100 行只返回空、不退款。当前 `spread`、`sky`、`thrust` 是纯计算，同线程上这条分支进不去。本轮没有把它跑成一次真实丢种。若以后要补，最小范围是这两处失败时加回本次投入并撤销消费标记。

## 同一本账和燃烧中再引爆

同一个 `FireCastContext` 对同一目标只能消费一次。先燃烧后引爆、先引爆后燃烧，第二次都是空，余额不变。只复用同一个 UUID 不够：两本新对象可以把 19 点先扣成 9，再扣成 0。

三式用的是 `ReleaseBatch.fireContext()` 这一个对象（`ReleaseBatch.java` 45–50 行）。燎原火场、焚天和燎里的迟到命中都抓住它。同一次释放里因此不能第二次消费。本轮没有跑一分为三的真实释放。

两个新上下文读的是同一份目标余额。夹具按 `FireSeedState` 的“每次从存储新建账本”边界：40 被第一本账扣成 0，第二本账读到 0，写入 12 后第三本账把 12 扣完。

已经挂着燎原时：新上下文可以补 30，第三个新上下文可以把这 30 引爆，原轨道的投入仍是 100。同施法者不能再挂一条燎原；另一施法者可以。焚天键和燎原键不互斥。引爆没有效果键，也不读燃烧账。产种暂停只对“这一本账已经消费或生产过该目标”有效。`FireSeedGrant` 每次新建账（17–22 行），所以点火、热光束、火球在燃烧期间仍可入账。

三式接线：

- 燎原：`GroundFireFields.java` 119–124 行调用 `schedule`，键 `xueximod:fire_liaoyuan`，不排他到整个目标。
- 焚天：`FentianEffect.java` 55–59 行调用 `schedule`，键 `xueximod:fire_fentian`，目标排他。
- 燎里：`LiaoliEffect.java` 51–58 行调用 `detonate`。

火种来源写的是 `grant`，不是消费入口。带火附魔额外 2（`EnchantBonus.java` 193 行），热光束每击 1（`HeatBeamEffect.java` 36 行），火球每实际目标 3（`FireOrbEffect.java` 43 行）。未抑制根结算时，帧上的 2 会再加到命中体点数上，所以带火附魔的火球是 5、热光束是 3。伤害被接受才 `deposit`。

燎里引爆失败后仍可能打物理刀，燎原附着失败时仍可能打固定 2 点。这两下都不随火种点数变化。

## 验证

定向 `javac --release 17` 退出 0（20:21:56–20:21:57）。六个入口都是退出 0：

- `DualRouteConsumeProbe`：上表、两种顺序、燃烧中补 30 再引爆、脉冲不改余额。
- `FireSeedCheck`：断言 251，失败 0。
- `SixFireFunctionalCheck`：通过。未跑实体命中、地火 tick、一分为三。
- `BurnLedgerCheck`：断言 121，失败 0。
- `BurnDispatchCheck`：断言 16，失败 0。
- `FireReleaseStructureCheck`：断言 41，失败 0。它只核对源码顺序，并把“消费写在最终挂载之前”当成应保持的顺序。

没有执行 `FireConsumption` 的实体调用、`AttributeStateData.write`、死亡、错误线程、无敌帧、地火 tick、`compileJava` 和游戏。范围内 main class 不比源文件旧，因此没有重编。57 个直接相关源码的 SHA-256 前后一致。生产源码和现有测试未改。结束时没有 `java`、`javaw` 或 `gradle` 进程。
