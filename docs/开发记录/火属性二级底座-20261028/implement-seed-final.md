我先读任务说明，按其中授权范围执行，并在结束时交完整报告。任务指定作者 A 只写火种资源。我先读实现合同和主设计，再按归属落地。合同和主设计已读完。接下来定位现有状态系统、资源注册和生命周期接点，只按火种归属实现。本地 Forge 源码已对上：死亡克隆不会带走属性状态，存活换维仍是同一实体。开始按这个边界写火种账本、快照和生命周期。火种模块已按作者 A 的范围落地。纯逻辑检查 244 条全部通过。没有跑 Gradle，也不把这次结果当成 Forge 集成已经通过。

## 改动

- `src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedMath.java`：钳制、档位、余数、整档扣除、`floor(P×M)`。
- `FireSeedArchive.java`：旧档案 BURST / SUSTAIN / SPREAD，只按完整档给收益。
- `FireCastContext.java`：一次逻辑施法一份账本，按目标 UUID 记下已生产、已消费。
- `FireSeedSnapshot.java`：不可变快照，档位和余数必须与投入点数一致。
- `FireSeedAccount.java`：单目标余额事务，不含实体和线程。
- `FireSeedState.java`：服务端入口，经 `AttributeStateService` 读写 `fire_seed`。
- `FireSeedLifecycle.java`：死亡清理、死亡克隆不继承、存活克隆和存活转化只补火种键。
- `ResourceStatuses.java`：登记 `xueximod:fire_seed`，只读数值、档位和无限时长。
- `src/test/java/com/bslx/XueXiMod/status/fire/seed/FireSeedCheck.java`：边界、共享余额、重复产消、余数和源码结构。

没有改术式、释放表、药水注册、语言文件或构建配置。没有火种 `MobEffect`。

## API

资源标识是 `xueximod:fire_seed`，持久键是属性状态复合标签里的 `fire_seed`，范围 0–100。0 会删掉这个键。

- `FireSeedState.value(LivingEntity)`：服务端主线程返回钳制后的余额，否则返回 0。
- `preview(target, cast, requested, furnaceBonus)`：不写余额，也不记入账本。
- `consume(...)`：只在投入点数大于 0 时扣款并标记该目标已消费。
- `grant(target, cast, baseAmount, productionMultiplier)`：返回实际增加值。
- `FireCastContext(UUID castId, UUID casterId)`：`castId` 必填，`casterId` 可以为空。复制体和延迟回调必须复用同一个对象。
- `requested == Integer.MAX_VALUE` 表示扣光当前余额；其他非负请求取 `min(余额, 请求)`。
- 查询视图是 `ResourceView(xueximod:fire_seed, S, floor(S/10), -1)`。`-1` 是 `FireSeedState.UNLIMITED_DURATION`，表示没有倒计时。判断有没有火种看数值，不能把时长小于等于 0 当成失效。

旧档案不自动附在 `consume` 上。调用方先取 `FireSeedMath.wholeTierSpend(余额)`，大于 0 再按这个点数扣。19 点因此扣 10、余额留 9；快照记录的是投入 10、1 档、余数 0。`Integer.MAX_VALUE` 才会把 19 点连余数一起扣完。收益用快照档位：引爆每档 0.04，持续每档加 10 tick，扩散每档加 0.10 格。

炉心倍率由调用方传入。本模块只做 `floor(P×M)`，不计算 1.12 或 2.2。快照里的炉心加成只保存，不改变扣点。

## 行为

- 基础量为 0，或取整后为 0：不增加，也不占用生产机会。
- 取整后大于 0，即使目标已经是 100、实际增加为 0，仍然占用这个目标的生产机会。
- 同一上下文里，该目标已经消费过，之后的生产直接拒绝。
- 先生产成功，之后仍可消费一次。同一次结算如果两件事都要做，调用方应先消费。
- 余额为 0、请求为 0、负数、非数或负数炉心：不扣、不标记。
- 去重只存在这份上下文对象上，没有按施法 UUID 建的全局表。标识相同的新对象是另一本账。
- 预览不回写。生产或消费发现存档越界时，会把持久值钳回 0–100。
- 非法参数不会写成负数或非数。火种不挂药水，牛奶清药水不会碰到它。

生命周期依据本地 1.20.1 Forge 源码：`ServerPlayer.restoreFrom` 只复制 `PlayerPersisted`，不会带走 `xueximod_attribute_states`。死亡且死亡事件未取消时清掉火种；死亡克隆再清一次新玩家身上的火种键。末地回主世界的存活克隆，以及 `Mob.convertTo` 触发的存活转化，只抄火种这一个整数。下界和末地传送仍是同一个 `ServerPlayer`，数据留在原对象上，这里不去改。

## 检查

纯 Java 模块用 `javac --release 17` 编译后运行了 `FireSeedCheck`：断言 244，失败 0，退出码 0。这次编译没有包含 `FireSeedState`、`FireSeedLifecycle` 和 `ResourceStatuses`。

`tools/check-name-gate.ps1` 退出码 1。本次改过的文件没有新违规。剩下的是未改动的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。

## 未测

没有跑 `compileJava`，没有游戏内验证。因此还没实测：实体存档与重载、真实死亡和克隆、生物转化、牛奶、越界 NBT 回写，以及非服务端主线程会被拒绝。燃烧计划的 NBT 由后续消费方保存快照字段；恢复时不要再次调用 `consume`。现有火术式没有接上产消。
