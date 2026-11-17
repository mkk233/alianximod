# 聚能输入链验证

## 工作区最终编译

菜单补丁进入工作区后，只跑了 `.\gradlew.bat compileJava --offline --no-daemon`。真实退出码 0。`BUILD SUCCESSFUL in 22s`，4 个任务里 2 个执行、2 个仍是最新。标准错误里的 20 条是既有过时 API 警告，不是失败。日志 `compileJava-workspace-final.log`，标准输出 `compileJava-workspace-final.out.txt`，标准错误 `compileJava-workspace-final.err.txt`，退出码 `compileJava-workspace-final.exit.txt`。没有 `build`、`jar`、`reobf`、`processResources`、`runClient`，没有打包，没有进游戏。源码没有再改。

这次编译包含 `SpellAssemblyMenu` 的两处修改：`staff == false` 时，`rejectReason` 直接拒绝 `TechniqueType.MAGIC`，`writeGear` 直接跳过。这两处不再看当前主手 `allowsMagic`。

这轮没有重跑检查。先前已经执行的逻辑断言是 `ReleaseInput` 77、实体 56、水牢 56、释放边界后的 `RouteReleaseInputCheck` 68，合计 257，失败 0。菜单改动不改变那 68 条，所以没有再跑。这不是全套测试通过。`ReleaseContainerEnergyCheck` 和 `WallHostExpiryCheck` 仍因缺少 ModLauncher 没有跑完，没有再试 Bootstrap。实体 tick、水牢真实药水和 `onWetRemoved`、容器和墙宿主的实际世界回调都没有运行。

当前只支持元素法术消费聚能，无属性法术不支持。一次聚能不能跨过下一次命中体积攻击。释放边界改完后的 `compileJava --offline --no-daemon` 通过，退出码 0，日志 `compileJava-boundary.log`。`RouteReleaseInputCheck` 重跑 68 条，失败 0，退出码 0，日志 `check-route-boundary.log`。没有打包，没有进游戏。容器和墙宿主仍未跑完，没有再试 Bootstrap。

## 资格收口

`frozenChainInput` 只有技为法术、成员为带属性的法术、定义为命中体积、且不是附魔时才扫描链倍率。空链、非法索引、空成员是基准倍率。零伤容器没有被正伤害过滤挡掉。聚能在 `ModTechniques` 注册为 `TechniqueType.MAGIC`，`ChainRole.ATTACK` 和释放种类没改。已有 `before` 快照没有覆盖。

剑技槽的现行入口是 `SpellAssemblyMenu.rejectReason`：法术且 `SkillCarrier.allowsMagic` 为假时返回「法术不能装入剑技槽」。`allowsMagic` 只对法杖为真。`GearSlot.mayPlace` 在原因为空时才放行，`set` 有原因就提示并返回。这次检查断言了注册类型是法术且 `canUseForSwordSkill` 为假。菜单要玩家背包，没有启动，所以菜单拒绝本身没有执行通过。旧 `SkillAssemblyPacket.put` 先做同一条法术检查，后面的 `suppliesChainEnergy` 分支新的法术聚能到不了。没有另加术式 id 判断。

`SkillFollowCast` 仍未修改。实体和水牢的生产代码这次没改，56 条没有重跑。`ReleaseInput` 77 条没有重跑。

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
| `RouteReleaseInputCheck`（资格收口前） | 28，失败 2 | 1 | `check-route.log` |
| `RouteReleaseInputCheck`（资格收口后） | 46，失败 0 | 0 | `check-route-final.log` |
| `RouteReleaseInputCheck`（释放边界后） | 68，失败 0 | 0 | `check-route-boundary.log` |
| `ReleaseContainerEnergyCheck` | 摘要没打印 | 1 | `check-container.log` |
| `WallHostExpiryCheck` | 摘要没打印 | 1 | `check-wall-host.log` |

实体检查自己写明：实体 tick、NBT 往返、客户端渲染未执行。水牢检查自己写明：真实药水、`onWetRemoved`、外部清除、预约第二次 `commit` 未执行，不能当成集成通过。

资格收口前的两条路由失败是引用相等，不是倍率算错。检查已改成比 `sizeMultiplier` 和倍率数值。第二名成员断言为 1，不复用第一名的 2。生产工厂在倍率恰好为 1 时仍 `new ReleaseInput(gatheredEnergy)`，没有为测试收成单例。收口后的 46 条已通过。

容器和墙宿主第一次失败是 `BarrierBody` 继承 `Entity`，注册表未初始化（`Not bootstrapped`）。容器停在 `barrier` 里第一次调用 `BarrierBody.withinLife`；在此之前没有打印 `FAIL`，`field()` 没跑到。墙宿主停在 `sameAsWall` 加载 `BarrierBody`；在此之前的 `lifeEnded` 没有打印 `FAIL`。补了 `SharedConstants.tryDetectVersion` 再调 `Bootstrap.bootStrap` 后，停在 Forge `NetworkEvent` 缺少无参构造，这要 ModLauncher，脱离不了。这两项不能记通过，也还没有看到生产断言失败。日志是 `check-container-bootstrap.log`、`check-wall-host-bootstrap.log`。

射线和水环几何的生产方法这次不读 `ReleaseInput`，没有拿它们代替上面的检查。

## 审查结论

契约审查是通过。水牢提前拆牢那条，`review-prison-fix-final.md` 按成功提交路径静态判定通过；这次 56 条对上了支撑数字，真实药水回调仍未跑。实体审查早先的不通过就是这一条，随后的静态复查已盖住成功提交路径。容器代码的独立静态审查已通过，主控已保存为 `review-cursor-containers-final.md`；容器和墙宿主的运行检查因缺少完整 Forge 启动环境未跑完，静态审查通过不代表运行检查通过。

`SkillFollowCast` 按 `legacy-entry-decision.md` 更正：`src/main/java` 没有外部调用，是未使用旧入口，不是运行时漏接。`route-report.md` 已改掉“仍要改这个文件”的说法。真实父窗口是 `CompiledReleaseStep` 到 `CastExecutionSession`。旧 while 调度没有宣称已验证或已修。

## 仍待

容器和墙宿主要在能启动 Forge 的进程里再跑。游戏内没有测。无属性法术当前不支持，不再等可选问题。基准倍率不要求对象单例。


## 最终复核

`review-last-increments-final.md` 已对聚能释放边界和菜单换手绕过两项增量给出 PASS。最终工作区 `compileJava --offline --no-daemon` 退出 0，日志 `compileJava-workspace-final.log`，包含菜单修复。已执行且针对当前未再变化逻辑的定向断言共 257 条、失败 0；这不是全套测试通过。未打包、未启动游戏。游戏内范围、画面、实际状态回调和菜单交互由用户实测。
