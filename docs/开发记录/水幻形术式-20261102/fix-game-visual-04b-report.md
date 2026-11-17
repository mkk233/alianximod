# 水幻形缺臂 04b

缺的是 `+X` 臂，浅的是 `+X` 腿。根因是皮肤顶点写进了水花贴图的批次。已经按这个改了提交顺序。待统一编译。未游戏验证。

## 缓冲实际怎么交

`RenderBuffers.bufferSource()` 的自定义 `RenderType` 不在固定缓冲表里，`getBuilderRaw` 都返回同一个 `BufferBuilder`。`getBuffer` 发现类型变了，就对上一个类型调用 `endBatch`：`RenderType.end` 立刻 `setupRenderState` 并 `BufferUploader.drawWithShader`。这段来自 1.20.1 映射源码 `MultiBufferSource.BufferSource`，没有在运行中的 JVM 里再执行一遍 `BufferBuilder`。

原来的 `drawPhase` 在任何一个顶点写入之前连续调用 `getBuffer(皮肤)` 和 `getBuffer(水花)`。第二次调用把还是空的皮肤批次提交掉，随后六个皮肤部件和水花都写进这个已经切到水花的 builder。真正绑上的是 `water_flakes.png`（32×32），不是 `steve_water_lines.png`。顶点位置仍是水线网格，所以外形还像镂空人；颜色和透明度来自水花贴图在这些 UV 上的纹素。

## 为什么正好是一条臂和一条腿

水花贴图 1024 个纹素里 660 个不透明。皮肤纹素中心 UV 落到这张 32×32 上的结果：

- `steve_arm_px`（bin 1，存储 +X）：571 个纹素全部落在透明纹素上，整条臂不画出来。
- `steve_arm_nx`：437 个落在不透明纹素上，134 个透明，这条臂还在。
- `steve_leg_px`：571 个里 562 个是 (186, 235, 255, 235)，浅色，没有皮肤上的深色 (22, 81, 115)。
- `steve_leg_nx`：563 个是 (46, 178, 214, 214)，仍是较深的青。

`+X` 臂和 `+X` 腿的皮肤 UV 都在 v=0.81–1.0。水花贴图这一带是透明或浅青。`-X` 一侧的 UV 在 v=0.25–0.5，采到的是不透明的中青。所以缺的臂和发白的腿是同一侧。`alignVanillaSteve` 的 `scale(-1)` 会把这一侧换到画面左或右，随身体朝向变。

00:25 日志里的包围盒仍然有效：顶点位置是对的，那次记录没有看到最终绑定的贴图。

## 改动

`SteveWaterLineVisual.drawPhase` 第 72–75 行先 `drawSkin`，皮肤顶点写完后再 `getBuffer` 水花并 `drawFlakes`。切换时 `endBatch` 提交的是已经写好的皮肤批次，贴图是 `steve_water_lines.png`。

`SteveWaterLineMesh` 第 87–135 行把绘制拆成按贴图种类各走一次，顶点格式、颜色和 UV 没变。

`CloneRenderer` 第 52–60 行去掉诊断挂钩。`WaterPhantomVisualDiagnostics` 已删除。确认模型和两张贴图没改。

名字门禁退出码 0。没有编译。未游戏验证。

本阶段额度已用完，返修结果未追加复审。
