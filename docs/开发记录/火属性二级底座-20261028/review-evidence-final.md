我先读这份任务说明，确认授权范围后再按它产出完整报告。任务是本批只读取证：不改文件、不编译、不派生子代理。我先按规格和契约定位火种、炉心、伤害与 mixin 的真实源码。本地映射在 Forge 源码 jar 里，我只从中抽出 `hurt`、抗性吸收和效果更新这几段，用来核对冷却字段和 mixin 落点。本批只读取证结束。下面是供判断用的源码证据，没有改文件，没有跑 Gradle、测试或游戏，也不把结构测试当成集成通过。映射依据是本地 `forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-sources.jar`。

## 1. 火种预览、扣款、挂载

主线程门在 `FireSeedState`：`actionable` 要求服务端且 `server.isSameThread()`，否则 preview/consume 返回空、grant 返回 0，不写数据。

```94:104:src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedState.java
    private static boolean actionable(LivingEntity target) {
        return onServerMainThread(target) && target.isAlive() && target.getUUID() != null;
    }
    // onServerMainThread：null、客户端、server==null 或非同线程都是 false
```

`preview` 只 `plan`。`consume` 在 `plan` 成功后减余额并 `markConsumed`。`grant` 若该目标已消费或已生产则返回 0；`floor(P×M)` 仍为正才 `markProduced`，已满 100 时增加量可以是 0，机会仍占用。去重在 `FireCastContext` 的目标 UUID 集合上，不看 castId 相等。同 UUID 的新对象是另一本账（`FireCastContext` 11 行的契约）。`Integer.MAX_VALUE` 表示整额，其它非负取 `min(请求, 余额)`。

调度顺序在 `FireConsumption.schedule` 23–70 行：主线程与存活 → 读账本冲突或 `trackCount>=8` 则返回 → `preview` → `planner.plan` → `BurnSchedule.of` 与 `canSeal` → `consume` → 快照变化才重规划 → 再读账本 `admit` → `write`。`detonate` 73–103 行同样先 preview 和 `componentReady`，再 consume，然后 `strikeOnce`。

`BurnPlanner`/`StrikePlanner` 只在注释里要求纯函数。实现只捕获 `RuntimeException` 并当成计划失败，失败发生在扣款前。规划器若在同线程改余额或燃烧 NBT，保护不住：

- 规划器里再次 `consume`：内层已扣并标记，外层 `plan` 看到 `hasConsumed` 返回空，外层不再扣，也不挂载。
- 规划器里 `grant` 或写入冲突轨道：外层 `consume` 已成功；57–59 行或 64–67 行打错误日志并返回空，没有退款。

纯规划器下，预览与扣款读到同一余额，`furnaceBonus` 用的是同一个局部变量，`snapshotChanged` 为假，第二次 `admit` 与扣款前的 `canSeal` 条件相同，挂载失败这条走不到。扣款前校验的是合成伤害有限且为正（`FireLayer.acceptableDamage`，上限 10000），不查关系策略，也不预演 `hurt` 成败。`detonate` 在 `strikeOnce` 返回 false 时仍 `return Optional.of(taken)`（103 行）。规格允许扣款后的免疫不退款；调用方区分不出没打上。

本包没有 `BurnPlanner` 的生产实现。上述先扣后失败是静态缺口，触发条件是规划器或同期主线程回调改数据。按契约这是调用方违约；按规格「不能挂载则必须不扣」，违约时实现不退款。严重性建议：高，限于非纯回调。

## 2. 元素帧与加 F

`ElementDamageScope` 是线程上的 `ArrayDeque`，`open*` 用 `addFirst`，`current()` 取栈顶。帧上没有目标、`DamageSource` 或这次 `hurt` 的标识。`close` 只弹出自己的帧。

本批唯一开火范围处是 `FireDamage.strikeOnce` 48–49 行，包住整段 `target.hurt`。因此 `ForgeHooks.onLivingAttack`（映射 `LivingEntity` 1119 行）和随后的 `LivingHurtEvent` 都看得到这帧。水/冰的 `openWater`、`openIce`、`openWaterIce` 在 main 里没有调用点。`onHurt` 在帧为 null 时直接返回，旧攻击不会被猜成火水冰。

```31:42:src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireRuntimeEvents.java
        if (frame.fire() && !frame.furnaceSettled()) {
            double bonus = casterBonus(victim, frame);
            double updated = ElementAdjustment.fireAmount(event.getAmount(), frame.fireBase(), bonus, false);
            event.setAmount((float) updated);
            frame.settleFurnace();
        }
        if ((frame.water() || frame.ice()) && !frame.vulnerabilityApplied()) {
            double multiplier = FurnaceService.waterIceMultiplier(victim);
            // ...
            frame.applyVulnerability();
        }
```

加 F 的式子是事件当前值加 `B×F`，B 是开帧时传入的 `fireBase`。`B×F` 不是正数、或已结算时，原样返回当前值。它不把金额改写成单独的 B，也不乘上已有修饰。同一帧里 `settleFurnace()` 之后再来的 `LivingHurtEvent` 不再加火，水冰同理只乘一次。新的 `open*` 压入新帧，不复制外层火水冰和已结算标志；内层关闭后外层还在。

本批调用都把 `furnaceSettled` 传成 true：引爆是 `strikeOnce(..., struck.base(), true)`（`FireConsumption` 102 行），定时段是 `strikeOnce(..., 0.0d, true)`（`BurnCarrier` 40 行）。事件因此不再加 F。定时段即使结算标志被提前置上，B 也是 0，`fireAmount` 仍返回当前值。密封伤害在 `BurnLedger.seal` 134–145 行已经按 `B×(1+A+F)` 算死。

未再开范围的嵌套 `hurt` 共用外层帧。`onHurt` 不看 `FireDamage.matches`。严重性建议：中。本批监听器不在火伤 `hurt` 里再打别人；荆棘、蒸汽或范围伤害若发生在该 `hurt` 内，会吃到外层帧。外层帧若一开始已结算，嵌套火加成会被跳过。

## 3. 燃烧 NBT、tick、死亡

编码在 `BurnCarrier.encode` 119–140 行：效果键、排他、cast、可选 caster、target、投入、档、余数、炉心、间隔、等待、游标、`damages`。`version` 必须为 1，否则 `read` 丢弃并在随后 `write` 清空。恢复时校验键、段数≤32、伤害可接受、游标、档余自洽；冲突或超过 8 条计入 `droppedOnRestore`。等待超出间隔会被改成间隔，不是丢弃。损坏快照不会重算已封伤害，所以之后新火种不改旧段。

`tick` 22–47 行：读出 → `advance`（等待>1 只减 1，否则打一段并后移）→ 逐段 `strikeOnce` → 仍存活则 `write` 内存账本。死亡、移除或 `LivingDeathEvent`（未取消）走 `clear`。这段 `write` 会盖住 strike 期间刚写入的新轨道；若 strike 里有人 `clear` 而实体仍活着，末尾 `write` 会把内存账本写回去。本批 `onHurt` 不挂载、不清除。严重性建议：中，现在没有批内触发；以后在火伤回调里 `schedule` 会丢新计划。

死亡克隆：`ServerPlayer.restoreFrom` 1216–1219 行只复制 `PlayerPersisted`，然后才 `onPlayerClone`。火种、燃烧、炉心都不在这个标签里。死亡克隆三条都清 successor，不从 original 拷贝。非死亡克隆才 `retainLivingClone` / `BurnCarrier.copy` / `FurnaceStore.copy`。转化只拷火种（`FireSeedLifecycle` 42–46 行），不拷燃烧。这是范围缺口，严重性建议低到中。

异步：火种写入口和 `FireConsumption.hostReady` 拒绝非主线程。`discard` 在非主线程是空操作，不会清掉。`BurnCarrier.write/clear` 本身不看线程。

## 4. 炉心、牛奶、mixin

公共修改是 `enterNormal`、`enterOverdrive`、`endPassive`、`cool`。`gate` 只拒绝 null 和客户端，不看 `isSameThread`。查询会 `reconcile`，缺运行态时按热量 0 重建并写 NBT。炉心包内没有主线程断言。严重性建议：低到中，正常事件在主线程；异步调用不会被拒绝。

`enter` 用 `forceAddEffect`。映射 `LivingEntity.forceAddEffect` 1004–1011 行是 `put` 替换实例：没有旧效果则 `onEffectAdded`，已有则 `onEffectUpdated`。两条都不发 `Added`，也不发 `Remove`。`onEffectUpdated` 1061–1066 行按新档先 `removeAttributeModifiers` 再 `addAttributeModifiers`。炉心的摘除只摘修饰器，`FurnaceStore.onAttached` 在标签已存在时保留热量并改档。普通升过载因此留住 F。`onAdded` 只 `normalize`，不再 `forceAddEffect`，这条替换路径不递归。

命令走 `addEffect` 971–986 行。`Applicable` 先于写入；非玩家或档不在 0..1 时 `FurnaceEvents.onApplicable` 118–119 行 `DENY`。`Added` 在 `update` 之前发出，`normalize` 把新旧实例都收成无限时长并拆掉隐藏效果。`MobEffectInstance.isShorterDurationThan` 128–129 行认定无限时长不算更短，所以随后的 `update` 不会把旧档收成隐藏。玩家 tick 顺序是 `ServerPlayer.tick` 489 行 `super.tick()` → `Player.tick` 218 行先发 START → `LivingEntity.baseTick` 440 行才 `tickEffects`。START 里 `reconcile` 再拆一次隐藏效果，发生在效果计时前。

牛奶：`IForgeMobEffect.getCurativeItems` 默认含奶桶。`FurnaceMobEffect.getCurativeItems` 64–66 行固定返回空表，`normalize` 54 行还把实例字段设成空表。`curePotionEffects` 3550 行要 `isCurativeItem` 才发 `Remove`。奶桶匹配不上，效果和运行态都不动，也不重加。

`removeEffect` 1032 行先发 `Remove(entity, effect)`，此时 `getEffectInstance()` 仍是身上的实例。未取消则 `FurnaceEvents.onRemove` 清运行态，随后 `onEffectRemoved` 摘属性。过期事件同样先清运行态再摘。死亡事件里 `removeEffect` 加清存档，不调用 `SteamBurst`。`cool` 仅在 `endPassive` 成功后调用一次蒸汽。

抗性 mixin 的目标在映射里成立：`getDamageAfterMagicAbsorb` 1669–1702 行只有一处 `hasEffect`（1673 行），和 `defaultRequire: 1` 一致。重定向把炉心玩家自己的抗性判定改成 false，后面的 `EnchantmentHelper.getDamageProtection` 1694 行仍执行。被打的是 mixin 的 `this`。`suppressesOwnResistance` 只在玩家且身上有炉心效果时为真。`mixins` 列表 29–30 行有 `FurnaceResistanceMixin` 和 `FurnaceEffectAccess`。`ModMobEffects` 20–21 行注册 `furnace_core`，`XueXiMod` 90 行登记事件总线。未启动游戏，不能证明运行时已经织入。

## 5. `fire_resource` 与无敌时间

`bypasses_cooldown.json` 是 `replace: false`，只追加 `xueximod:fire_resource`。映射 `LivingEntity.hurt` 1161–1174 行：

```1161:1174:（forge 1.20.1-47.3.33 parchment 源码）LivingEntity.java
if ((float)this.invulnerableTime > 10.0F && !pSource.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
    if (pAmount <= this.lastHurt) return false;
    this.actuallyHurt(pSource, pAmount - this.lastHurt);
    this.lastHurt = pAmount;
} else {
    this.lastHurt = pAmount;
    this.invulnerableTime = 20;
    this.actuallyHurt(pSource, pAmount);
    this.hurtDuration = 10;
    this.hurtTime = this.hurtDuration;
}
```

带该标签时条件为假，一定进 else。else 在 `actuallyHurt` 之前把 `lastHurt` 写成这次伤害，并把 `invulnerableTime` 写成 20。生物在 `baseTick` 414–415 行每 tick 减 1（服务器玩家在 `ServerPlayer.tick` 453–454 行减）。判定是 `>10`，所以刷新后的前 10 tick 会卡住普通攻击。

最小例子，静态控制流，不是实测：僵尸 `invulnerableTime==0`。`fire_resource` 打 2：全额 `actuallyHurt(2)`，同时 `lastHurt=2`、`invulnerableTime=20`。随后普通近战 6：6>2，只结算 `actuallyHurt(4)`。若近战是 1：1<=2，1163 行直接 false。若近战先打出 7，4 tick 内再吃火伤 2：火伤仍全额，并把 `lastHurt` 从 7 改成 2、无敌时间重新写成 20。`is_fire` 的抗火在 1126 行就 return，发生在这次写入之前，抗火不会刷新冷却。护甲、魔抗、附魔、吸收仍在 `actuallyHurt` 1710–1713 行。

严重性建议：高。标签让火伤自己穿过冷却，else 同时改了全局冷却字段。

可行的最小修法，不实施：保留 `bypasses_cooldown`，这样仍走 else，全额进入 1172 行 `actuallyHurt`。只在 `hurt` 里对 `source.is(FireDamage.TYPE)` 跳过两处写入：1170 行 `lastHurt`（该方法里 else 这一次 PUTFIELD，if 分支 1167 行的那次不要动），以及 1171 行 `invulnerableTime`（`hurt` 内唯一一次写成 20；1818 行 `handleDamageEvent` 不要织）。跳过是不赋值，保留原来的值，不是写成 0。1173–1174 行的受击闪红不参与下次攻击判定。Mixin 0.8 可用 `@Redirect` 的 PUTFIELD，或 HEAD 记下本次 `DamageSource` 再在字段写入处决定是否落笔。护盾仍在 1137 行，防御仍在 `actuallyHurt`。未跑游戏，织入后的字节码需启动时再看。

## 6. 标签、语言、蒸汽隔离

`bypasses_cooldown.json` 与 `is_fire.json` 都是 `replace: false`，values 只有 `xueximod:fire_resource`。`bypasses_armor/effects/resistance/enchantments` 和 `always_hurts_ender_dragons` 仍只有 `xueximod:water_prison_pressure`。`steam_burst` 不在这些标签里。

`fire_resource.json` 与 `steam_burst.json` 的 `message_id` 分别是 `fire_resource`、`steam_burst`。`zh_cn.json` 和 `en_us.json` 都没有这些键：

- `death.attack.fire_resource`
- `death.attack.fire_resource.player`
- `death.attack.fire_resource.item`
- `death.attack.steam_burst`
- `death.attack.steam_burst.player`
- `death.attack.steam_burst.item`

两份语言里已有的死亡键只有 `death.attack.water_prison_pressure`。炉心键 `effect.xueximod.furnace_core` 中英都有。缺死亡键的严重性建议：低，死亡消息会显示原始键名。

`SteamBurst.release` 直接 `candidate.hurt(source, 2.0F)`，不开元素帧，不读炉心或火种。`cool` 成功退出后单独调用它时栈是空的，`onHurt` 看到 null 就返回。它没有压入一个空帧；若调用时栈上已有火或水冰帧，这次蒸汽 `hurt` 会使用那个帧。本批没有这种调用。直接冷却路径是隔离的。严重性建议：低，只在「作用域内再放蒸汽」时出现。

## 判断时可分开的三类

静态确定：非纯规划器会先扣后失败且不退款；元素帧不绑定目标和伤害源；tick 末尾 `write` 能盖住重入挂载；`bypasses_cooldown` 会把 `invulnerableTime` 写成 20 并改 `lastHurt`；死亡语言键缺失；命令非法档被 `Applicable` 拒绝；`forceAddEffect` 升级不发 `Remove`、不把热量清零；牛奶治愈列表为空。

接口限制：去重认账本对象不认相同 castId；规划器纯度只写在接口上；炉心公共写方法不拒绝非主线程；`BurnCarrier.write` 同样不拒绝；旧术式没有 `openWater`/`openIce` 调用，所以不会自动吃炉心。

游戏验证：mixin 是否真的织上、4 tick 段是否在目标身上落地、牛奶和死亡克隆的手感、抗性数值。这些都没有跑。
