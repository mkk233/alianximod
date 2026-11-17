我先读取这份复核任务说明，按其中授权范围完成只读检查并给出完整报告。任务是第 2/2 轮只读取证，范围限定 R1–R6。我先读三份既有记录，再对照源码和本地映射字节码核对。三份记录已读完。接下来对照源码和本地映射 jar，逐项核 R1–R6 的实际字节码与调用路径。映射 jar 和 Mixin 0.8.5 已定位。接下来用 javap 核对 `hurt` 里两次 `PUTFIELD` 的 owner、序号，以及 Redirect 能否拿到原方法参数。实现第 2/2 轮不通过。`lastHurt` 的序号和 else 分支对得上，但 `invulnerableTime` 的字段 owner 与映射字节码不一致；`require = 1` 会使整份 `FireResourceHurtMixin` 无法通过 Mixin 0.8.5 的注入校验。元素帧在正常返回、早退和嵌套路径上是平衡的，异常离开 `hurt` 时 `disarm` 清不掉已压入的调用帧。

本轮只读了 `review-round1.md`、`repair-round1-final.md`、`repair-r6-final.md` 和对应源码，并用 JDK 17 `javap` 核对了本地映射 jar 与 Mixin 0.8.5。没有改文件，没有跑 Gradle、测试或游戏。

映射 jar：

`C:\Users\baishang\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1\forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-recomp.jar`

Mixin：

`C:\Users\baishang\.gradle\caches\modules-2\files-2.1\org.spongepowered\mixin\0.8.5\9d1c0c3a304ae6697ecd477218fa61b850bf57fc\mixin-0.8.5.jar`

## 仍确定的问题

### R2：`invulnerableTime` 的 owner 对不上

`LivingEntity.hurt(DamageSource, float)` 里，`lastHurt` 有两次 `PUTFIELD`，`invulnerableTime` 有一次。常量池是：

- `#1585 = Fieldref LivingEntity.lastHurt:F`
- `#602 = Fieldref LivingEntity.invulnerableTime:I`

else 分支（`DamageSource.is` 为真时 `ifne 303`）的最小片段：

```
249: getfield  #602   // LivingEntity.invulnerableTime:I
264: invokevirtual    // DamageSource.is
267: ifne       303
272: getfield  #1585  // LivingEntity.lastHurt:F
280: ireturn          // 差额不足，直接返回
285: getfield  #1585
289: invokevirtual    // actuallyHurt
294: putfield  #1585  // lastHurt，本方法第 1 次 PUTFIELD，ordinal 0
303: aload_0
305: putfield  #1585  // lastHurt，第 2 次 PUTFIELD，ordinal 1
311: putfield  #602   // invulnerableTime，本方法唯一一次 PUTFIELD，ordinal 0
317: invokevirtual    // actuallyHurt
```

`lastHurt` 的注解 owner、名字、描述符 `F`、`PUTFIELD`、ordinal `1`、`require = 1` 与偏移 305 一致，打中的是 else，不是偏移 294 的差额写入。`Player.hurt` 末尾是 `invokespecial LivingEntity.hurt`，这两次写入不在 `Player.hurt` 自己的方法体里。

`invulnerableTime` 的名字、描述符 `I`、opcode 和 ordinal `0` 也指向偏移 311，但注解 owner 写成了 `Entity`：

```54:62:src/main/java/com/bslx/XueXiMod/mixin/FireResourceHurtMixin.java
    @Redirect(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Entity;invulnerableTime:I",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 0
            ),
            require = 1
    )
```

Mixin 0.8.5 的 `MemberInfo.matches` 对 owner 做字符串相等比较，不相等就返回 `NONE`。这条 Redirect 的匹配数是 0。`InjectionInfo` 在 `required` 不满足时抛出 “did not match the required number of targets” / “Critical injection failure”。`xueximod.mixins.json` 第 2 行是 `"required": true`，第 31 行登记了 `FireResourceHurtMixin`，第 54–56 行 `defaultRequire` 为 1。因此失败的是整份 Mixin 的应用校验，不是单独漏掉无敌时间、其余钩子照常生效。本轮没有启动游戏，没有实际崩溃栈。

两个 Redirect 的显式参数本身符合 `PUTFIELD` 的基本形态：`(字段所在实例, 新值)V`。`lastHurt` 是 `(LivingEntity, float)`，`invulnerableTime` 是 `(LivingEntity, int)`，返回 `void`。这个 `LivingEntity` 与字节码 fieldref owner 一致，和注解里的 `Entity` 不一致。`@Inject` 的 `hurt(DamageSource, float)` 描述符与方法一致；`@At`、`@Redirect`、`@Inject`、`@Shadow` 都没有写 `remap = false`，默认 `remap = true`。

Mixin 0.8.5 的 jar 里没有 `@Local`。`RedirectInjector.injectAtPutField` 先用 `[字段 owner, 字段类型]` 做 `validateParams`；`captureTargetArgs > 0` 时再 `pushArgs` 压入原方法参数。所以 Redirect 可以把 `hurt` 的 `DamageSource` 和 `float` 追加在字段参数后面，不需要 `@Local`。当前两个 handler 没有追加这些参数，只读入口栈，这在签名上合法。栈方案是否正确见 R3。

最小修法：把无敌时间的 target 改成 `Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I`，opcode、ordinal `0`、`require = 1` 保持不变。handler 第一参数已经是 `LivingEntity`。

### R3：异常路径清不掉已领取的调用帧

正常返回、方法内早退、未标记嵌套、显式嵌套、同一对象再入，在钩子真的织入时是平衡的。

- `enterHurt` 用 `pollFirst` 领走队首 arm，未标记时帧是 `null`，显式中性也是 `null` 帧。
- `BeforeReturn.find` 只收集布尔方法的 `ireturn`（opcode 172），`hurt` 里的早退 `ireturn` 都会走到 `exitHurt`。
- `FireRuntimeEvents.onHurt` 第 23–29 行只看栈顶，并且 `call.target() == victim`、`call.source() == event.getSource()`。同一目标和同一个 `DamageSource` 再入时，内层是新的 `HurtCall`；外层帧不在栈顶。事件没有读取 `identity()`。
- 火帧的水和冰都是 false。`furnaceSettled == true` 时第 35–39 行不再加炉心。内层未标记帧是 `null`，第 24 行直接返回，不会消耗外层的 `vulnerabilityApplied`。

`@At("RETURN")` 盖不住抛出。`disarm` 只在 `!claimed` 时从 `WAITING` 删除：

```38:43:src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/ElementDamageScope.java
    public static void disarm(ElementArm arm) {
        if (arm == null || arm.claimed) {
            return;
        }
        WAITING.get().remove(arm);
    }
```

HEAD 一旦执行，arm 已经被 `pollFirst` 取走且 `claimed == true`。`finally` 里的 `disarm` 不再碰 `ACTIVE`。

`FireDamage.strikeOnce` 第 53–62 行、`SteamBurst.release` 第 56–65 行只在 `catch (RuntimeException)` 里调用一次 `exitHurt()`。单层 `RuntimeException`、且这次 HEAD 已经入栈、RETURN 没有执行时，这一次弹出是平衡的。

反例：`strikeOnce` 已经 `armFire`，`hurt` 的 HEAD 已经 `enterHurt`。此后在任何 `ireturn` 之前抛出非 `RuntimeException`。RETURN 注入不运行，`catch (RuntimeException)` 不运行，`disarm` 因为 `claimed` 直接返回。`ACTIVE` 顶部的火伤 `HurtCall` 留在这条服务器线程的 `ThreadLocal` 里。下一次正常的 `LivingEntity.hurt` 会再压一层，RETURN 只弹出新层，底下的火伤帧还在。若再往后一次 `strikeOnce` 或 `SteamBurst` 在进入 `LivingEntity.hurt` 之前就抛 `RuntimeException`，它们的 `catch` 会无条件 `exitHurt()`，弹出的是这层残留帧，或者弹出仍在进行的外层帧。

最小修法：用一次调用令牌把入栈和出栈配成对。RETURN 和任意 `Throwable` 的清理都只弹出这个令牌，并且只弹一次。`disarm` 继续只负责还没被领取的 `WAITING`。

## 已解决

### R1

`FurnaceEvents` 三处已经直接取 `Player`：第 27 行 `event.player`，第 42 行 `event.getEntity()`，第 52 行 `event.getEntity()`。死亡和治疗的静态类型仍是 `LivingEntity`，所以那里的 `instanceof Player` 还在。本轮没有编译，不能代替另一会话的 `compileJava` 结果。

### R4

`BurnDispatch.tick` 第 38–45 行先 `advance` 再 `write`，然后才 `strike`。循环里不再写回这份旧账。`BurnCarrier.clear` 第 94–95 行删计划并把 `xueximod_fire_burn_gen` 加一。下一段开始时世代或存活检查失败就 `return`。

同步 `schedule` 在 `FireConsumption` 第 63–69 行重新 `read` 当前持久数据再 `write`。发生在提交之后，本 tick 的脉冲列表已经固定，新轨道不会被旧账盖掉，也不会在这一拍被推进。`discard` 或死亡清账之后，剩余段不会把旧计划写回去。

失败段在 `advance` 里已经把 `segmentCursor` 向前拨，`strikeOnce` 吞掉 `RuntimeException` 后循环继续。新轨道 `ticksUntilNext` 初始等于 `intervalTicks`，`advance` 只在等待不大于 1 时出脉冲，首段要走满间隔。空账 `write` 只删计划键，不加世代。

### R5

`FurnaceService.gate` 第 107–117 行：空玩家、客户端、非服务端主线程分别返回 `REJECT_NULL`、`REJECT_CLIENT`、`REJECT_THREAD`。`enterNormal`、`enterOverdrive`、`endPassive` 都走这个门；`cool` 只有结束成功才放蒸汽。`serverSnapshot` 第 120–123 行在非玩家或不在服务端线程时返回 `inactive()`（热量 0、档位空），不调用 `reconcile`。对应查询是火伤加成 0、火种倍率 1、未激活、水冰倍率 1。`FurnaceRuntime.reconcile` 第 12 行和 `advance` 第 43–45 行在写效果、属性和存档前同样拒绝。没有把工作排回主线程再报成功。

### R6

`zh_cn.json` 第 62–67 行、`en_us.json` 第 3–8 行各有 6 个键，`message_id` 分别是 `fire_resource` 和 `steam_burst`。

`DamageSource.getLocalizedDeathMessage` 的实参数是：

- 没有造成者、也没有击杀归属：基础键 1 个参数
- 没有造成者、但有击杀归属：`.player` 2 个参数
- 有造成者且主手有自定义名称：`.item` 3 个参数
- 有造成者但武器没有自定义名称：基础键 2 个参数

`TranslatableContents.getArgument` 只按模板里出现的 `%n$s` 取参数，下标是 `n - 1`，越界才抛 `TranslatableFormatException`。多出来的实参不会被读取。当前基础键只有 `%1$s`，`.player` 是 `%1$s` 和 `%2$s`，`.item` 是三个占位符。四种调用都不会因为个数对不上而露出原始模板。有造成者、武器也没有自定义名称时，原版把攻击者传给基础键，当前句子不显示这个攻击者。

## 未实测

没有织入运行、没有 `compileJava`、没有游戏。因此没有观察：Mixin 实际加载结果、4 tick 资源火伤和普通攻击交错后的 `lastHurt` 与 `invulnerableTime`、护甲、抗性、抗火、火焰保护、牛奶、隐藏效果、死亡或存活克隆、卸载存档，以及蒸汽击退距离。JDK 17 的 `javap` 已找到，路径是 `C:\Program Files\Java\jdk-17\bin\javap.exe`。上一轮复审已经用它核对过映射后的 `LivingEntity.hurt` 字节码。
