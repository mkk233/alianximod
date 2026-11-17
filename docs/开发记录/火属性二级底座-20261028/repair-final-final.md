先读最终返修任务说明，按授权范围处理。复审已经写明改法：无敌时间的字段所有者要改成 `LivingEntity`，冷却判断直接拿这次 `hurt` 的参数；元素帧则要按调用令牌做异常安全的进入和退出。我先核对本地映射字节码，再按这个边界改。本地类文件在工程的编译产物里。冷却重定向改为直接接收这次 `hurt` 的参数，元素帧改成带令牌的 `try/finally`。R2 和 R3 已按第 2 轮结论改完。本次源码写入已经结束。返修结果未追加复审。

## R2

`FireResourceHurtMixin` 仍只挂钩 `LivingEntity.hurt(DamageSource, float)`。

- `lastHurt` 保持 `LivingEntity`、`F`、`PUTFIELD`、ordinal 1、`require = 1`
- `invulnerableTime` 改为 `Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I`，`PUTFIELD`、ordinal 0、`require = 1`
- 两个重定向的参数是字段实例、新值，然后是这次 `hurt` 的 `DamageSource` 和 `float`。只有 `xueximod:fire_resource` 跳过这两次写入，其它来源照常写入。不清零，也不绕过防御
- 冷却判断不再读元素调用栈

## R3

无参 `exitHurt()` 已删除。现在按令牌结束：

- `HurtCall enterHurt(Object target, Object source)`
- `void exitHurt(HurtCall call)`：令牌不在栈上则不动；在栈上则只卸掉它和压在它上面的遗留，底下的外层保留
- `Boundary mark()`、`void restore(Boundary boundary)`：显式包装恢复进入前的深度

`FireHurtMixinPlugin` 在应用 `FireResourceHurtMixin` 之前，用现有 ASM 给这个 `hurt` 包一层真正的 `try/finally`（`xueximod.mixins.json` 已登记该插件）。进入时 `enterHurt`，正常返回和任何 `Throwable` 都 `exitHurt` 同一个令牌后再返回或继续抛出。原来的指令和原有异常表都还在，没有重写伤害计算。`HEAD`/`RETURN` 注入已去掉。

`FireDamage.strikeOnce` 和 `SteamBurst.release` 在调用 `hurt` 前 `mark()`，`finally` 里 `restore()`。`catch (RuntimeException)` 只记日志，不再盲弹。`super.hurt` 之前抛出的异常没有令牌，不会动外层。

## 检查入口

- `ElementHurtLifecycleCheck`：正常、早退、嵌套 `RuntimeException` 向外传、未包装内层被回调捕获、包装内层被捕获、`super` 前异常、非 `RuntimeException` 的 `Throwable`、边界恢复
- `FireHurtBytecodeCheck`：读复审指定的 `recomp.jar`，核对两处 `PUTFIELD` 的 owner、描述符和次数，并执行变换后的夹具（正常、早退、直接异常、`Throwable`、内层异常被捕获）
- `FireLayerCheck`、`FireCooldownCheck` 已改成新签名和新重定向

这几项在本会话里没有跑出结果。命令执行入口每次都被拒绝，`javac`、上述 `main` 和变量名门禁都没有启动，所以不能写断言数字，也不能把这次写成已经核对过 `recomp.jar`。运行脚本在 `build/run-fire-hurt-check.ps1`。没有跑 Gradle，没有打包，没有启动游戏，也没有织入 Mixin。
