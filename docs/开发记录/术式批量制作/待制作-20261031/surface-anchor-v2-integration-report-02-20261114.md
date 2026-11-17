# 地表定位 v2 包围盒私有字段返修报告

- batchId：`待制作-20261031`
- taskId：`rule_surface_anchor_v2_integration`
- attemptId：`20261114-integrate-surface-anchor-v2-02`
- stage：`implementation`（本单返修后待统一编译）
- 操作角色：`implement`
- 真实直接父代理：主会话
- 关联 B：`rule_surface_anchor`（地表定位）
- 依赖：`20261114-integrate-surface-anchor-v2-01`、`20261114-compile-surface-anchor-v2-01`（均已收束关闭）
- 授权来源：用户 2026-11-14 原话「你先继续，我忘了地面定位之前你给出的版本我还没说采用，之前那个版本的效果可以接入」
- 额度：设计 `2/2`、实现 `3/3`。本阶段额度已用完，返修结果未追加复审，不重置计数。
- 编译：未跑。待统一编译。

## 修复

`BatchCarrierVisual.makeBoundingBox()` 第 86 行原先写的是 `this.dimensions.makeBoundingBox(this.position()).expandTowards(0.0D, 0.5D, 0.0D)`。`net.minecraft.world.entity.Entity` 的 `dimensions` 是 `private`。前序编译 `20261114-compile-surface-anchor-v2-01` 报：`BatchCarrierVisual.java:86: 错误: dimensions 在 Entity 中是 private 访问控制`。

现在改为 `super.makeBoundingBox().expandTowards(0.0D, 0.5D, 0.0D)`。`super.makeBoundingBox()` 是 `protected`，内部仍是 `this.dimensions.makeBoundingBox(this.position())`。地表定位仍只在原箱体上向上扩 `0.5`，顶面从 `+0.5` 到 `+1.0`，底面仍是锚点 `Y = 0`。

守卫未改：`this.entityData != null && this.textureKind() == BatchVisualTexture.SURFACE`。其余贴图仍走 `super.makeBoundingBox()`。无行内注释，无单字母变量。没有改其他方法。

## 文件

路径：`src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java`。

|时点|字节|SHA256|mtime|换行|
|---|---:|---|---|---|
|改前|21,502|`2BF108F5770E7509880CE9CECB847A3B890D7037FCBD4240758012A615F45EFD`|2026-11-14 04:35:32.426 +08:00|LF 557，CRLF 0，裸 CR 0|
|改后|21,477|`F60325E434A1A267798A6BE501D20F63348F917EA5264ADEF9031E7528C4BC48`|2026-11-14 04:48:11.855 +08:00|LF 557，CRLF 0，裸 CR 0|

改前字节和 SHA256 与任务单预期一致，也与编译回执记下的改后快照一致。改后少 25 字节，等于把 `this.dimensions.makeBoundingBox(this.position())` 换成 `super.makeBoundingBox()`。行数不变。

## 名称门禁

- 命令：`powershell -NoProfile -File tools/check-name-gate.ps1`。
- 直接运行退出码 `1`，末行「共 274 处」。
- 同一命令再捕获一次标准输出和标准错误：退出码 `1`，276 行，末行「共 274 处」，其中 `BatchCarrierVisual.java` 命中 `0` 处。
- 其余命中都在本单未改的既有文件。没有修那些历史命中。

## 状态拆分

- 功能：只把地表定位载体包围盒从私有字段访问改成调用 `super.makeBoundingBox()`。落点、失败拒绝、扣费和冷却未改。
- 美术：沿用 `20261114-art-surface-anchor-v2-01` 确认版。本单未改贴图、元数据或几何。
- 用户确认：2026-11-14 用户明确说「之前那个版本的效果可以接入」。本单是该接入的编译返修，不是一次新的效果确认。
- 正式接入：运行侧这一处调用已改。正式资源库归档不在本单写权限内，未做。
- 游戏实测：未做。
- 制作表、`记录.md`、`实测清单.md`、`docs/资源库/`：未写。
- 待统一编译。本单禁止跐 `gradlew` / `compileJava`，没有重跑编译，不把未编译记成已通过。

前序编译日志里，本单写范围内的错误只有 `BatchCarrierVisual.java:86`。同一份日志还有 `ReleaseDefinitions.java` 等写范围外报错。那些文件本单没改，不记成已修。

## 收束与额度

本单写入已收束。只写了 `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java` 和本回执。没有其他作者持有这两条路径。文件句柄在本 final 返回前关闭。

设计额度 2/2、实现额度 3/3 在开工前已用完。本阶段额度已用完，返修结果未追加复审，没有重置计数。

审批拒绝：无。编译：未跑，待统一编译。阻塞：无。

下一步：交回主会话。
