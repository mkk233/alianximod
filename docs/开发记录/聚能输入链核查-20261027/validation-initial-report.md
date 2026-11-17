# 聚能输入链验证

`compileJava --offline --no-daemon` 通过，退出码 0。JDK 17.0.6。完整输出在 `compileJava.log`。没有打包，没有进游戏。无属性法术资格仍未决定，整项不能称完成。

## 编译

命令在项目根执行，`GRADLE_USER_HOME=C:\Users\baishang\.gradle`：

`.\gradlew.bat compileJava --offline --no-daemon`

`BUILD SUCCESSFUL in 25s`。日志里的中文警告是 `ResourceLocation` 构造和 `setCanceled` 的过时提示，不是失败。没有第二轮 Gradle，没有 `build`、`jar`、`reobfJar`、`runClient`。`compileTestJava` 没有跑；已知的 `SkinAttachmentChecks` 旧失败没有修。

本轮没有改 `src` 下的 Java。`ReleaseInput.java` 未改，先前 77 条契约检查没有重跑。

## 定向检查

检查类用 `javac --release 17` 编到本目录 `checks-integration`。运行类路径依次是 `checks-integration`、`build\classes\java\main`、`build\tide-classpath.txt`。生产类来自这次 `compileJava` 的 `build\classes\java\main`，没有替身。

| 检查 | 断言 | 退出 | 日志 |
| --- | --- | --- | --- |
| `ReleaseEntityEnergyCheck` | 56，失败 0 | 0 | `check-entity.log` |
| `PrisonEnergyCheck` | 56，失败 0 | 0 | `check-prison.log` |
| `RouteReleaseInputCheck` | 28，失败 2 | 1 | `check-route.log` |
| `ReleaseContainerEnergyCheck` | 摘要没打印 | 1 | `check-container.log` |
| `WallHostExpiryCheck` | 摘要没打印 | 1 | `check-wall-host.log` |

实体检查自己写明：实体 tick、NBT 往返、客户端渲染未执行。水牢检查自己写明：真实药水、`onWetRemoved`、外部清除、预约第二次 `commit` 未执行，不能当成集成通过。

路由两条失败都是引用相等，不是倍率算错。`check-route-identity.log` 对同一生产方法测得：无聚能成员 `baseMultiplier=1.0`、`baseIdentity=false`；后一名成员 `secondMultiplier=1.0`、`secondSize=1.0`、`secondIdentity=false`；前一名仍是 `2.0`。`frozenChainInput` 在倍率恰好为 1 时执行 `new ReleaseInput(gatheredEnergy)`，只有剑术排除才返回 `ReleaseInput.BASE`。后一名在前一个正伤害攻击处停止回扫，聚能没有乘进去。这不是泄漏。本轮不改生产代码去迎合 `== BASE`。

容器和墙宿主第一次失败是 `BarrierBody` 继承 `Entity`，注册表未初始化（`Not bootstrapped`）。容器停在 `barrier` 里第一次调用 `BarrierBody.withinLife`；在此之前没有打印 `FAIL`，`field()` 没跑到。墙宿主停在 `sameAsWall` 加载 `BarrierBody`；在此之前的 `lifeEnded` 没有打印 `FAIL`。补了 `SharedConstants.tryDetectVersion` 再调 `Bootstrap.bootStrap` 后，停在 Forge `NetworkEvent` 缺少无参构造，这要 ModLauncher，脱离不了。这两项不能记通过，也还没有看到生产断言失败。日志是 `check-container-bootstrap.log`、`check-wall-host-bootstrap.log`。

射线和水环几何的生产方法这次不读 `ReleaseInput`，没有拿它们代替上面的检查。

## 审查结论

契约审查是通过。水牢提前拆牢那条，`review-prison-fix-final.md` 按成功提交路径静态判定通过；这次 56 条对上了支撑数字，真实药水回调仍未跑。实体审查早先的不通过就是这一条，随后的静态复查已盖住成功提交路径。容器审查任务要求审查者只给出结论、不写报告文件，本目录没有单独的容器终稿；容器检查又没跑完，所以容器没有执行通过的结论。

`SkillFollowCast` 按 `legacy-entry-decision.md` 更正：`src/main/java` 没有外部调用，是未使用旧入口，不是运行时漏接。`route-report.md` 已改掉“仍要改这个文件”的说法。真实父窗口是 `CompiledReleaseStep` 到 `CastExecutionSession`。旧 while 调度没有宣称已验证或已修。

## 仍待

无属性法术（`MAGIC` 且属性 `NONE`，以及同类无属性载体）是否允许聚能，仍等用户。当前非剑路径会乘扫到的链倍率。

路由引用相等要不要收成 `BASE` 单例，等主控分配。容器和墙宿主要在能启动 Forge 的进程里再跑。游戏内没有测。
