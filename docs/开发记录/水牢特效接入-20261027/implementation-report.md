我先读完整份任务说明，再按里面写明的范围和约束执行。任务指定我为唯一实现者，不派子代理。我先读设计报告和渲染器基线，再按已消解方案改那两个授权文件。设计已定：只改渲染器和一张公告板贴图。我按报告里点名的方法读现有渲染与材质，不扩到全仓。贴图先核对哈希和尺寸，确认目标路径没有别的文件，再按设计改渲染器。水牢公告板已接入。只改了渲染器和一张贴图，玩法、协议和寿命逻辑都没动。

**修改文件**
- `src/main/java/com/bslx/XueXiMod/client/PrisonShellRenderer.java`
- `src/main/resources/assets/xueximod/textures/entity/xueximod/water_prison_billboard.png`（写入前目标路径不存在）

**实现**
阶段仍是 `AFTER_ENTITIES`。材质只调用一次 `StillSheetMesh.of`，同一返回值用于 `getBuffer` 和 `endBatch`。每个牢 4 个顶点、白顶点、`uv2` 全亮，不提交背面，也不加 mcmeta。

包络以快照 `bounds` 为底。目标按 UUID 在 `entitiesForRendering()` 里查找（1.20.1 的 `Level.getEntities()` 是 `protected`，模组包不能调用）；找不到再用网络编号，且 `getEntity(int)` 的 UUID 必须一致，否则忽略。命中且存活时，用 `partialTick` 插值碰撞箱后与 `bounds` 取并集。未加载只画 `bounds`。UUID 已匹配但死亡或 `isRemoved()`，这一帧不画。第一人称且目标就是相机实体，或相机在包络内，不画。

面朝当前相机，含俯仰。右轴是左轴的反方向，宽高共用同一半边长。角点先在 double 里减相机，再 `transformPosition`，投影用 `projection.transform`。深度是 `(角点-相机)·look`；深度不大于 0 或 `clipW <= 1.0e-4` 的角丢掉。近平面为 `m32/(m22-1)`，不是有限正数时用 `0.05`。平面深度 `d = max(近平面+0.05, 最近深度-0.08)`。边长 `hypot(投影宽×96/70, 投影高×96/74)×1.04`。没有留在前方的角就不画。

帧：`age = (gameTime - birthTick) + partialTick`，小于 0 用第 0 帧，否则 `floor(age/2) mod 16`。贴图 96×1536，V 从 `frame/16` 到 `(frame+1)/16`，U 为 0 到 1。

**检查**
- 贴图 SHA256 `8ba44798140ec92fc45b826549cec03cf629bcb7bcd1d34dab750462101e5096`，与正式源一致，47640 字节，96×1536。
- `tools/check-name-gate.ps1` 退出码 1。本次文件无命名问题。基线仍有两处：`OriginalHumanoid.java:165` 的 `i`，`202` 的 `to`。未改该文件。

**未跑与风险**
未跑 `gradlew`，未进游戏。并集不会小于冻结壳；实体不在时不跟姿势；贴墙 0.08 格内可能盖住墙面；不写深度的水挡不住；多牢透明重叠不排序。

待统一编译；文件写入结束，可释放所有权。