# 空间切割 v8 接入结果

2026-10-25。执行：Cursor Grok 4.7 High Fast，未委派。只改代码、贴图、索引并 `compileJava`。未 `build`、未打 jar、未启动游戏、未提交、未推送。游戏内三道斩击和时序没有实测。

## 落地方式

命中窗口保持 10 tick。`SpaceCutEffect` 的 `LIFETIME = 10`、`hitInterval = 0`、锚点、`STILL_AIM`、listener 和 `onExpire` 路径都没改。视觉另走可选的静止单次贴图：20 帧、每帧 1 tick，由客户端快照播完。实体在第 10 tick 按原逻辑移除后，快照继续到第 20 tick。没有新的服务端实体，没有第二份伤害，也没有把一张实体画成三道。

`space_cut` 仍走已有宿主组。出生时间在 `releaseActivation` 里写一次；`applySpec` 只抄帧数，不写出生时间。没有宿主组时，成功 `addFreshEntity` 之后才写。出生前客户端不画。

## 精确改动

新增：

- `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/StillSheetVisual.java`：默认 `NONE`。有效描述才启用，不看术式 id。
- `StillSheetClock.java`：`frame = floor((gameTime - birth + partialTick) / ticksPerFrame)`，范围 0..19。小于 0 不画，大于等于 20 到期，不取模。
- `StillSheetSnapshot.java`：一份只读姿态、`CohortVisual`、贴图、出生 gameTime、世界身份。
- `StillSheetCache.java`：按 UUID 只收一次。到期或切世界才删。正常移除不删。没有生成或命中方法。
- `StillSheetLayout.java`：只调用 `CohortLaneLayout.sheetWindow`，不再乘倍率。
- `src/main/java/com/bslx/XueXiMod/client/StillSheetMesh.java`：透明混合、`LEQUAL` 深度测试、只写颜色、`NO_CULL`、最近邻且无 mipmap。
- `StillSheetPlayback.java`：客户端 tick 扫描 `entitiesForRendering` 捕获；`AFTER_ENTITIES` 画完剩余帧。同一 UUID 不重播。切世界或断线清空。每个快照一张四边形，不画反向面。
- `src/test/java/com/bslx/XueXiMod/shu/technique/release/entity/StillSheetChecks.java`
- `src/main/resources/assets/xueximod/textures/entity/xueximod/dimension_willow_v8.png`：从归档原样复制，无 mcmeta。

修改：

- `SpaceCutEffect.java`：贴图改为 `xueximod:textures/entity/xueximod/dimension_willow_v8.png`，`StillSheetVisual.once(TEXTURE, 20, 1)`。宽 5、长 3、高 0、寿命 10、`hitInterval = 0`、宿主组 `SPACE_CUT_ID` 登记保留。
- `TechniqueHitSpec.java`：新增 `stillSheet`，默认 `NONE`。无宿主组且发布成功后调用 `stampStillBirth`。宿主组分支仍只 `stagePublished`。
- `TechniqueHitEntity.java`：同步帧数、每帧 tick、出生 gameTime（未出生为 -1）、显式取消标记。`releaseActivation` 末尾补一次盖章。`applySpec` 不写出生时间。`cancelStillSheet` 没有接到到期或命中。
- `TechniqueHitRenderer.java`：描述启用时 `shouldRender` 返回 false，`render` 直接返回，避免和快照叠两层。旧斩面、光束、水术式路径未改。
- `docs/资源库/README.md`、`docs/资源库/特效/次元斩/柳叶单斩_v8/README.md`：标明已接入 `space_cut`、`compileJava` 已通过、游戏内未验证。PSD 和历史版本保留。

写上述已有文件前已重读，并按相对路径备份到 `docs/资源库/过程归档/空间切割接入-20261025/baseline/`。

未改：`ReleaseDefinitions`、`CohortVisual`、`CohortLaneLayout`、`HostGroupContext`、`ReleaseBatch`、`TechniqueHitSettle`、Gradle 配置和全局环境。

出生时间异常超前超过 100 tick 的快照会清掉，避免缓存无界。正常激活从 age 0 开始，0 到 19 帧不走这条清理。

## 检查

`StillSheetChecks` 直接运行，退出码 0：

```text
StillSheetChecks: 148 assertions
```

覆盖：未出生不显示；age 0/9/10/18/19/19.99/20/40 的帧号和清理；age 10 不删快照；同 UUID 刷新不改出生时间和姿态；同锚点三个 UUID 都保留；显式 `cancel` 只删一个；切世界和 `clear` 清空；5×3 同锚点三槽沿上下分开，槽间空隙 0.4，长边仍是 5，不缩成碎片；已散开的份与单份窗口相同，不二次分槽；UV 为 `(frame + localV) / 20`；播放类只有 4 个顶点、无反向面、无 `tickCount`、无 `getModelScale`、无 `tryHit` / `spawnReported`；效果类寿命仍是 10；`applySpec` 不盖出生章；结算类不调用取消。

PNG：归档与游戏内文件 SHA-256 都是 `99E2654E649AF63B48CBEEB2830563D4258F2D67C29AB4B0FE86E716AC2DE12B`。`System.Drawing` 读到 96×960、`Format32bppArgb`。IHDR 宽 96、高 960、位深 8、颜色类型 6。高 960 / 48 = 20 帧。

命名门禁退出码 1，只有两处，都在本次没改的文件：

```text
src/main/java/com/bslx/XueXiMod/animation/runtime/OriginalHumanoid.java:165 i
src/main/java/com/bslx/XueXiMod/animation/runtime/OriginalHumanoid.java:202 to
```

本次动过的 Java 没有报出来，所以没有改这两处。

## 编译

命令：

```text
gradlew.bat -g C:\Users\baishang\AppData\Local\Temp\cursor-sandbox-cache\a6dd7201ae0ea39bc33e5d675cf9f1cf\gradle compileJava --offline --no-daemon --console=plain
```

进程环境里的 `GRADLE_USER_HOME` 指向 `C:\Users\baishang\AppData\Local\Temp\cursor-sandbox-cache\b6d9258c7eb2e8915016e6f3c87bc5dc\gradle`。编译前后该目录都不存在。指定 `-g` 缓存里的 `caches\8.8\javaCompile\javaCompile.lock`、`fileHashes.lock`、`journal-1.lock` 在第一次编译时被写下（2026-10-25T20:12:23+08:00）。缓存没有被重定向到那条环境目录。

第一次编译失败：`StillSheetPlayback` 对已经是 `ClientLevel` 的 `Minecraft.level` 做了模式匹配，2 个错误。已改成空值判断。第二次：

```text
BUILD SUCCESSFUL in 18s
4 actionable tasks: 2 executed, 2 up-to-date
gradle-exit=0
```

仍有 16 条既有的过时 API 提示（`ResourceLocation` 构造、`setCanceled`），不是这次错误，也没有因此改那些文件。Gradle 因现有 `org.gradle.jvmargs` 提示会拉起一次性进程，并在构建结束停掉。没有改 Gradle 配置，没有开常驻 daemon。缺依赖的情况没有出现，因此没有改成联网。

## 游戏内尚未核对

编译通过不能当作三道视觉或游戏时序已经看到。还需要在客户端确认：

- 宿主组激活前不显示，激活后从第 0 帧开始。
- 实体第 10 tick 移除后，第 11 到 18 帧仍在，第 19 帧透明，第 20 tick 消失。
- 同锚点一分为三是三个实体、三道上下分开的整张柳叶，不是把一张图切成三段，也没有叠一层旧斩面。
- 已经散开的份不再被分槽推开。
- 命中窗口、`onExpire` 和后续链仍在第 10 tick，没有多打一份伤害。

## 最终代码核对

独立 Grok 4.7 审查及 GPT-6-astra 同步/生命周期专项核对通过，未发现阻断本次接入的实质缺陷。核对范围包括正式激活盖章、客户端事件注册、10 tick正常移除后续播、UUID去重、三份分槽及旧渲染抑制。独立报告见同目录 review-result.md。

非阻断建议：StillSheetMesh.of 每次绘制创建 RenderType，后续可按纹理缓存以减少分配开销。本次未因此扩大修改范围。编译结果沿用 Cursor 已完成的离线 BUILD SUCCESSFUL；未打包，游戏内效果由用户验证。
